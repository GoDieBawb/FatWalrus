/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.client;

import fatwalrus.commands.ClientCommandRegistry;
import fatwalrus.network.SocketWriter;
import fatwalrus.network.ClientSocketListener;
import fatwalrus.commands.CommandExecutor;
import fatwalrus.commands.CommandRegistry;
import fatwalrus.encryption.Decryptor;
import fatwalrus.encryption.Encryptor;
import fatwalrus.encryption.KeyGenerator;
import fatwalrus.network.ClientTimeoutHandler;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Semaphore;


/**
 *
 * @author Bob
 */
public class Client implements Runnable {
    
    public static boolean IS_ENCRYPTED;
    
    private final KeyGenerator         kg        = new KeyGenerator(2048);
    private final DatagramSocket       socket    = new DatagramSocket();
    private final Semaphore            recLock   = new Semaphore(1);
    private final ArrayList<byte[]>    recQueue  = new ArrayList<>();
    private final CommandExecutor      executor  = new CommandExecutor();
    private       boolean              connectionEstablished;
    private       PublicKey            serverPublicKey;
    private final ClientTimeoutHandler to;
    private final ClientSocketListener sl;
    private final SocketWriter         sw;
    private       String               handshake;
    private final String               id;
    private       boolean              isRunning = false;
    private       long                 lastConnect;
    private       int                  timeWarn;
    
    public Client(String ip, int port, boolean isEncrypted) throws Exception {
        
        IS_ENCRYPTED = isEncrypted;
        
        executor.registerCommands(new ClientCommandRegistry(this));
        
        kg.createKeys();        
        
        to = new ClientTimeoutHandler(this);
        sl = new ClientSocketListener(this, socket);
        sw = new SocketWriter(socket, InetAddress.getByName(ip), port);
        id = ip + ":" + port;
        
    }
    
    public void start() throws Exception {
      
        System.out.println("Starting Client on address: " + id + "...");
        isRunning = true;
        
        Thread tt = new Thread(to);
        Thread lt = new Thread(sl);
        Thread ct = new Thread(this);
        
        tt.start();
        lt.start();
        ct.start();
        
    }
    
    public void stop() {
        
        System.out.println("Stopping Client...");
        sendMessage("DISCONNECT".getBytes());
        
        try {
            Thread.sleep(500);
        }
        
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        to.stop();
        sl.stop();
        sw.stop();
        socket.close();
        
        isRunning = false;
        
        onDisconnect();
        
    }
    
    @Override
    public void run() {

        if (!connectionEstablished) {
            attemptConnection();
        }
        checkReceived();
            
        
    }
    
    private void checkReceived() {
        
        byte[] received = readMessageQueue(recQueue, recLock);
        
        if (received == null) return;
        
        if (!connectionEstablished) {
            attemptEstablish(received);
        }
        
        else {
            
            String message = new String(received);
            executor.Execute(message);

        }
        
    }
    
    private void attemptConnection() {
        
        int attempts = 0;

        while (!connectionEstablished && isRunning) {

            try {
                
                if (IS_ENCRYPTED) {
                    sendPublicKey();
                }

                else {
                    sendHandshake();
                }

                attempts++;

                if (attempts > 5) {
                    System.out.println("Connection Failed!");
                    stop();
                    onConnectFailed();
                    return;
                }

                checkReceived();


                Thread.sleep(1000);
                
            }

            catch (Exception e) {
                e.printStackTrace();
            }

        }
        
    }
    
    private void attemptEstablish(byte[] received) {
        
        if (IS_ENCRYPTED) {

            if (receivePublicKey(received) != null) {
                System.out.println("ENCRYPTED CONNECTION ESTABLISHED");
                serverPublicKey = receivePublicKey(received);
                connectionEstablished = true;
                sendMessage("CONFIRM".getBytes());
                onConnect();
            }

        }

        else {

            if (new String(received).equals(handshake)) {
                System.out.println("CONNECTION ESTABLISHED");
                connectionEstablished = true;
                sendMessage("CONFIRM".getBytes());
                onConnect();
            }

        }
        
    }
    
    public void sendMessage(byte[] message) {
        
        try { 

            if (connectionEstablished && IS_ENCRYPTED)
                message = new Encryptor().encryptText(new String(message), serverPublicKey).getBytes();

            sw.write(message);
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }    
    
    public void receiveMessage(byte[] message) {
        
        updateConnect();
        
        try {
            
            recLock.acquire();

            if (connectionEstablished && IS_ENCRYPTED)
                message = new Decryptor().decryptText(new String(message), kg.getPrivateKey()).getBytes();

            recQueue.add(message);
            
        }
        
        catch (Exception e) {
            if (isRunning) e.printStackTrace();
        }
        
        recLock.release();
        
        new Thread(this).start();
        
    }    
    
    private byte[] readMessageQueue(ArrayList<byte[]> messageQueue, Semaphore lock) {
        
        byte[] message = null;
        
        try {
            lock.acquire();
            if (!messageQueue.isEmpty()) {
                message = messageQueue.get(0);
                messageQueue.remove(0);
            }
            lock.release();
        }
        
        catch(Exception e) {
            if (isRunning) e.printStackTrace();
        }
        
        return message;
        
    }
    
    private void sendHandshake() {
        handshake = "HELLO " + new Random().nextInt((999 - 0) + 1) + 0;
        sw.write(handshake.getBytes());
    }
    
    private void sendPublicKey() throws Exception {
        byte[]         sendData     = kg.getPublicKey().getEncoded();
        sw.write(sendData);
    }
    
    private PublicKey receivePublicKey(byte[] keyBytes) {
        
        try {
            X509EncodedKeySpec ks  = new X509EncodedKeySpec(keyBytes);
            KeyFactory         kf  = KeyFactory.getInstance("RSA");
            PublicKey          key = kf.generatePublic(ks);
            return key;
        }
        
        catch(Exception e) {
            return null;
        }
        
    }
    
    private PrivateKey receivePrivateKey(byte[] keyBytes) {
        
        try {
            PKCS8EncodedKeySpec spec  = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory          kf    = KeyFactory.getInstance("RSA");
            PrivateKey          key   = kf.generatePrivate(spec);
            return key;
        }
        
        catch(Exception e) {
            return null;
        }
        
    }    
    
    private void sendPrivateKey() throws Exception {
        byte[]         sendData     = kg.getPrivateKey().getEncoded();
        sw.write(sendData);
    }
    
    public void updateConnect() {
        lastConnect = System.currentTimeMillis();
        timeWarn    = 0;
    }
    
    public long getLlastConnect() {
        return lastConnect;
    }
    
    public void timeWarn() {
        timeWarn++;
    }
    
    public int timeWarning() {
        return timeWarn;
    }
        
    
    public void onConnect() {
    }
    
    public void onDisconnect() {
    }
    
    public void onConnectFailed() {
    }
    
    public void onConnectionLost() {
    }
    
    public void onServerStop() {
    }
    
    public String getID() {
        return id;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
   
    public void RegisterCommands(CommandRegistry cr) {
        executor.registerCommands(cr);
    }
    
}
