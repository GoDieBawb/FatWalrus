/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.network;

import fatwalrus.commands.ClientConnectionCommandRegistry;
import fatwalrus.commands.CommandExecutor;
import fatwalrus.commands.CommandRegistry;
import java.net.DatagramSocket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import fatwalrus.encryption.Decryptor;
import fatwalrus.encryption.Encryptor;
import fatwalrus.server.Server;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 *
 * @author Bob
 */
public class ClientConnection implements Runnable {
     
    private final ArrayList<byte[]>  recQueue  = new ArrayList();
    private final Semaphore          recLock   = new Semaphore(1);
    private final CommandExecutor    executor  = new CommandExecutor();
    private final SocketWriter       sw;
    private final String             id;
    private PublicKey                clientPublicKey;
    private PrivateKey               privateKey;
    private boolean                  connectionEstablished;
    private long                     lastConnect;
    private int                      timeWarn;
    private boolean                  hasDisconnected;
    
    public ClientConnection(DatagramSocket socket, InetAddress ip, int port, PrivateKey privateKey) {
        sw              = new SocketWriter(socket, ip, port);
        this.privateKey = privateKey;
        id              = ip.toString() + ":" + port;
        executor.registerCommands(new ClientConnectionCommandRegistry(this));
    }
    
    public void registerCommands(CommandRegistry commandRegistry) {
        executor.registerCommands(commandRegistry);
    }
    
    @Override public void run() {
        checkReceived();
    }
    
    private void checkReceived() {

        byte[] received = readMessageQueue(recQueue, recLock);
        
        if (received == null) return;
        
        if (!connectionEstablished()) {
            attemptConnection(received);
        }       
        
        else {
            
            String commandString = new String(received);
            executor.Execute(commandString);
            
        }
        
    }
    
    private void attemptConnection(byte[] received) {
        
        if (Server.IS_ENCRYPTED) {

            if (receivePublicKey(received) != null) {
                clientPublicKey = receivePublicKey(received);
            }

            else if (receivePrivateKey(received) != null) {
                System.out.println("RECEIVED PRIVATE KEY");
            }

            if (clientPublicKey != null) {

                String message = "";

                try {
                    message = new Decryptor().decryptText(new String(received), privateKey);
                }

                catch(Exception e) {}

                if (message.startsWith("CONFIRM"))
                    establishConnection();   

            }

        }

        else {

            if (new String(received).startsWith("HELLO"))
                sendMessage(received);
            if (new String(received).startsWith("CONFIRM"))
                establishConnection();           

        }
            
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
            e.printStackTrace();
        }
        
        return message;
        
    }    
    
    public String getId() {
        return id;
    }
    
    public void receiveMessage(byte[] message) {
        
        updateConnect();
        
        try {
            
            recLock.acquire();

            if (connectionEstablished && Server.IS_ENCRYPTED)
                message = new Decryptor().decryptText(new String(message), privateKey).getBytes();

            recQueue.add(message);
            recLock.release();
            
        }
        catch (Exception e) {
            e.printStackTrace();
            recLock.release();
        }
        
        new Thread(this).start();
        
    }
    
    public void sendMessage(byte[] message) {
        
        try { 

            if (connectionEstablished && Server.IS_ENCRYPTED)
                message = new Encryptor().encryptText(new String(message), clientPublicKey).getBytes();

            sw.write(message);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    public ArrayList<byte[]> getIncoming() {
        return recQueue;
    }
    
    public Semaphore getRecLock() {
        return recLock;
    }
    
    public void setClientPublicKey(PublicKey key) {
        this.clientPublicKey = key;
    }
    
    public PublicKey getClientPublicKey() {
        return clientPublicKey;
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
    
    public void stop() {
        sw.stop();
    }
    
    public void establishConnection() {
        
        connectionEstablished = true;
        
        if (Server.IS_ENCRYPTED)
            System.out.println("ENCRYPTED CONNECTION ESTABLISHED");
        else
            System.out.println("CONNECTION ESTABLISHED");
        
    }
    
    public boolean connectionEstablished() {
        return connectionEstablished;
    }
    
    public void disconnect() {
        hasDisconnected = true;
    }
    
    public boolean hasDisconnected() {
        return hasDisconnected;
    }
    
    private PublicKey receivePublicKey(byte[] keyBytes) {
        
        try {
            X509EncodedKeySpec ks                = new X509EncodedKeySpec(keyBytes);
            KeyFactory         kf                = KeyFactory.getInstance("RSA");
            PublicKey          key               = kf.generatePublic(ks);
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
    
}
