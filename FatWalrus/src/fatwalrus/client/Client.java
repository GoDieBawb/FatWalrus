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
import fatwalrus.encryption.Decryptor;
import fatwalrus.encryption.Encryptor;
import fatwalrus.encryption.KeyGenerator;
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
    private final ArrayList<byte[]>    recQueue  = new ArrayList();
    private final CommandExecutor      executor  = new CommandExecutor();
    private       boolean              isStopped;
    private       boolean              connectionEstablished;
    private       PublicKey            serverPublicKey;
    private final ClientSocketListener sl;
    private final SocketWriter         sw;
    private       String               handshake;
    private final String               id;
    
    public Client(String ip, int port, boolean isEncrypted) throws Exception {
        
        IS_ENCRYPTED = isEncrypted;
        
        System.out.println("Starting Client...");
        
        executor.registerCommands(new ClientCommandRegistry(this));
        
        kg.createKeys();        
        
        sl = new ClientSocketListener(this, socket);
        sw = new SocketWriter(socket, InetAddress.getByName(ip), port);
        id = ip + ":" + port;
        
    }
    
    public void start() throws Exception {
      
        Thread lt = new Thread(sl);
        Thread ct = new Thread(this);
        
        lt.start();
        ct.start();
        
    }
    
    public void stop() {
        
        sendMessage("DISCONNECT".getBytes());
        
        try {
            Thread.sleep(500);
        }
        
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        

        sl.stop();
        sw.stop();
        socket.close();
        isStopped = true;
        
    }
    
    @Override
    public void run() {
        
        try {

            checkReceived();

            if (!connectionEstablished) {

                if (IS_ENCRYPTED) {
                    sendPublicKey();
                }

                else {
                    sendHandshake();
                }

            }
            
        }
        
        catch(Exception e) {
            if (!isStopped) e.printStackTrace();
        }
        
    }
    
    private void checkReceived() {
        
        byte[] received = readMessageQueue(recQueue, recLock);
        
        if (received == null) return;
        
        if (!connectionEstablished) {
            attemptConnection(received);
        }
        
        else {
            
            String message = new String(received);
            executor.Execute(message);

        }
        
    }
    
    private void attemptConnection(byte[] received) {
        
        if (IS_ENCRYPTED) {

            if (receivePublicKey(received) != null) {
                System.out.println("ENCRYPTED CONNECTION ESTABLISHED");
                serverPublicKey = receivePublicKey(received);
                connectionEstablished = true;
                sendMessage("CONFIRM".getBytes());
            }

        }

        else {

            if (new String(received).equals(handshake)) {
                System.out.println("CONNECTION ESTABLISHED");
                connectionEstablished = true;
                sendMessage("CONFIRM".getBytes());
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
        
        try {
            
            recLock.acquire();

            if (connectionEstablished && IS_ENCRYPTED)
                message = new Decryptor().decryptText(new String(message), kg.getPrivateKey()).getBytes();

            recQueue.add(message);
            recLock.release();
            
        }
        
        catch (Exception e) {
            recLock.release();
            if (!isStopped) e.printStackTrace();
        }
        
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
            if (!isStopped) e.printStackTrace();
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
    
    public boolean isStopped() {
        return isStopped;
    }
   
}
