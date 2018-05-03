/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.network;

import fatwalrus.commands.ClientConnectionCommandRegistry;
import fatwalrus.commands.CommandExecutor;
import fatwalrus.commands.CommandRegistry;
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
    
    private final Server             server; //The server that clients are connected to
    private final ArrayList<byte[]>  recQueue  = new ArrayList<>(); //Queue of received messages
    private final Semaphore          recLock   = new Semaphore(1); //Semaphore for received messages
    private final CommandExecutor    executor  = new CommandExecutor(); //Executes commands
    private final SocketWriter       sw; //Writes data to socket
    private final String             id; //The ID of the client connection
    private PublicKey                clientPublicKey; //The client's public key
    private PrivateKey               privateKey; //The server's private key
    private boolean                  connectionEstablished; //Whether the connection has been established
    private long                     lastConnect; //The time of the last message received
    private int                      timeWarn; //Value of timeout warning
    private boolean                  hasDisconnected; //Whether the client has disconnected
    private String                   disconnectReason; //The reason for the client's disconnection
    
    //Set final fields on construct
    public ClientConnection(Server server, InetAddress ip, int port, PrivateKey privateKey) {
        this.server     = server;
        sw              = new SocketWriter(server.getSocket(), ip, port);
        this.privateKey = privateKey;
        id              = ip.toString() + ":" + port;
        //Register basic client connection commands
        executor.registerCommands(new ClientConnectionCommandRegistry(this));
    }
    
    //Registers a CommandRegistry with the CommandExecutor
    public void registerCommands(CommandRegistry commandRegistry) {
        executor.registerCommands(commandRegistry);
    }
    
    //Runs a new thread to check the received queue
    @Override public void run() {
        checkReceived();
    }
    
    //Checks for received messages and acts on them
    private void checkReceived() {

        byte[] received = readMessageQueue(recQueue, recLock);
        
        if (received == null) return;
        
        //If connection has not been established then attempt connection
        if (!connectionEstablished()) {
            attemptConnection(received);
        }       
        
        //If connection has been established execute the message as a command
        else {
            
            String commandString = new String(received);
            executor.Execute(commandString);
            
        }
        
    }
    
    //Attempts to complete the handshake with the client
    private void attemptConnection(byte[] received) {
        
        //If connection is encrypted
        if (Server.IS_ENCRYPTED) {

            //Attempt to receive keys
            if (receivePublicKey(received) != null) {
                clientPublicKey = receivePublicKey(received);
            }

            else if (receivePrivateKey(received) != null) {
                System.out.println("RECEIVED PRIVATE KEY");
            }

            //If key has been received check for confirmation
            if (clientPublicKey != null) {

                String message = "";

                try {
                    message = new Decryptor().decryptText(new String(received), privateKey);
                }

                catch(Exception e) {}

                //If CONFIRM received determine connection has been established
                if (message.startsWith("CONFIRM"))
                    establishConnection();   

            }

        }

        //If not encrypted attempt plain text handshake
        else {

            //If message starts with HELLO then send back the handshake
            if (new String(received).startsWith("HELLO"))
                sendMessage(received);
            //If CONFIRM received determine the connection has been established
            if (new String(received).startsWith("CONFIRM"))
                establishConnection();           

        }
            
    }
    
    //Synchronized read of a list of byte arrays
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
    
    //Returns the client connection id
    public String getId() {
        return id;
    }
    
    //Receives a message from the client
    public void receiveMessage(byte[] message) {
        
        //Update last receive
        updateConnect();
        
        try {
            
            recLock.acquire();

            //If encrypted then decrypt message
            if (connectionEstablished && Server.IS_ENCRYPTED)
                message = new Decryptor().decryptText(new String(message), privateKey).getBytes();

            //Add message to received queue
            recQueue.add(message);
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        recLock.release();
        //Start a new thread to execute the received message
        new Thread(this).start();
        
    }
    
    //Sends a message to the client
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
    
    //Returns the received message queue
    public ArrayList<byte[]> getIncoming() {
        return recQueue;
    }
    
    //Returns the received message semaphre
    public Semaphore getRecLock() {
        return recLock;
    }
    
    //Sets the client public key
    public void setClientPublicKey(PublicKey key) {
        this.clientPublicKey = key;
    }
    
    //Returns the client's public key
    public PublicKey getClientPublicKey() {
        return clientPublicKey;
    }
    
    //Updates the last received message time to now and resets the timeout warning
    public void updateConnect() {
        lastConnect = System.currentTimeMillis();
        timeWarn    = 0;
    }
    
    //Returns the time of the last message received from the client
    public long getLlastConnect() {
        return lastConnect;
    }
    
    //Increments the timeout warning
    public void timeWarn() {
        timeWarn++;
    }
    
    //Returns the value of the timeout warning
    public int timeWarning() {
        return timeWarn;
    }
    
    //Stops the ClientConnection
    public void stop() {
        sw.stop();
    }
    
    //Determine that the connection has been established
    public void establishConnection() {
        
        connectionEstablished = true;
        server.onClientConnected(this);
        
        if (Server.IS_ENCRYPTED)
            System.out.println("ENCRYPTED CONNECTION ESTABLISHED");
        else
            System.out.println("CONNECTION ESTABLISHED");
        
    }
    
    public boolean connectionEstablished() {
        return connectionEstablished;
    }
    
    //Disconnect the client and call onDisconnect
    public void disconnect(String reason) {
        sw.stop();
        disconnectReason = reason;  
        hasDisconnected  = true;      
        server.onClientDisconnected(this, disconnectReason);
    }
    
    //Return whether the client has disconnected
    public boolean hasDisconnected() {
        return hasDisconnected;
    }
    
    //Receive a public key
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
    
    //Receive a private key
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
