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
    
    private final KeyGenerator         kg        = new KeyGenerator(2048); //Encryption Key Generator
    private final DatagramSocket       socket    = new DatagramSocket(); //Socket
    private final Semaphore            recLock   = new Semaphore(1); //Semaphore for Receive Queue
    private final ArrayList<byte[]>    recQueue  = new ArrayList<>(); //Queue for received messages
    private final CommandExecutor      executor  = new CommandExecutor(); //Contains command registries
    private       boolean              connectionEstablished; //Whether handshake has been completed
    private       PublicKey            serverPublicKey; //The key received from the server if encrypted
    private final ClientTimeoutHandler to; //Runnable checks to see if still connected to server
    private final ClientSocketListener sl; //Listens for messages sent to the socket
    private final SocketWriter         sw; //Writes messages to the sockets
    private       String               handshake; //The string that is sent to and returned from the server
    private final String               id; //The Client ID Format <ip>:port
    private       boolean              isRunning = false; //Whether the client is currently running
    private       long                 lastConnect; //The last time the client received a message
    private       int                  timeWarn; //Timeout warning is incrememented if server is unresponsive
    
    //Client is constructed with target ip, port and boolean to determine if encrypted connection
    public Client(String ip, int port, boolean isEncrypted) throws Exception {
        
        //Set encrypted
        IS_ENCRYPTED = isEncrypted;
        
        //Register base client commands
        executor.registerCommands(new ClientCommandRegistry(this));
        
        //Create the encryption keys
        kg.createKeys();        
        
        //Construct Runnables for threads
        to = new ClientTimeoutHandler(this);
        sl = new ClientSocketListener(this, socket);
        sw = new SocketWriter(socket, InetAddress.getByName(ip), port);
        
        //Set id
        id = ip + ":" + port;
        
    }
    
    //Start the client
    public void start() throws Exception {
      
        //Client is now running
        isRunning = true;
        
        //Construct Threads containing Runnables
        Thread tt = new Thread(to);
        Thread lt = new Thread(sl);
        Thread ct = new Thread(this);
        
        //Start threads
        tt.start();
        lt.start();
        ct.start();
        
    }
    
    //Stop the client
    public void stop() {
        
        //Sends the disconnect message to the server
        sendMessage("DISCONNECT".getBytes());
        
        //Waits to allow final send/receive
        try {
            Thread.sleep(500);
        }
        
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        //Stop Runnables
        to.stop();
        sl.stop();
        sw.stop();
        
        //Close Socket
        socket.close();
        
        //Server is not running
        isRunning = false;
        
        //Run Overridable onDisconnect()
        onDisconnect();
        
    }
    
    //Run Client Thread
    @Override
    public void run() {

        //If not yet connected attempt connection establishment
        if (!connectionEstablished) {
            attemptConnection();
        }
        //Check Received Messages
        checkReceived();
            
    }
    
    //Checks Messages in the Received Queue
    private void checkReceived() {
        
        //Synchronized read of Received Queue
        byte[] received = readMessageQueue(recQueue, recLock);
        
        //Return on null
        if (received == null) return;
        
        //If received message but not connected attempt to establish with received message
        if (!connectionEstablished) {
            attemptEstablish(received);
        }
        
        //If connected send the received message to the command executor
        else {
            
            String message = new String(received);
            executor.Execute(message);

        }
        
    }
    
    //Attempt to establish a connection with the server
    private void attemptConnection() {
        
        //Number of connection attempts
        int attempts = 0;

        //White no connection and Client Remains Running
        while (!connectionEstablished && isRunning) {

            try {
                
                //If encrypted send the public key
                if (IS_ENCRYPTED) {
                    sendPublicKey();
                }

                //If not encrypted send the handshake string
                else {
                    sendHandshake();
                }

                //increment attempts
                attempts++;

                //If more than five attempts the connection has failed
                if (attempts > 5) {
                    System.out.println("Connection Failed!");
                    stop();
                    onConnectFailed();
                    return;
                }

                //Check for response
                checkReceived();

                //Wait one second before trying again
                Thread.sleep(1000);
                
            }

            catch (Exception e) {
                e.printStackTrace();
            }

        }
        
    }
    
    //If connection not yet established check received for connection confirmation
    private void attemptEstablish(byte[] received) {
        
        //If encrypted the client will expect to receive the server's public key
        if (IS_ENCRYPTED) {

            if (receivePublicKey(received) != null) {
                System.out.println("ENCRYPTED CONNECTION ESTABLISHED");
                serverPublicKey = receivePublicKey(received);
                connectionEstablished = true;
                sendMessage("CONFIRM".getBytes());
                onConnect();
            }

        }

        //If server is not encrypted it will expect the handshake to be returned
        else {

            if (new String(received).equals(handshake)) {
                System.out.println("CONNECTION ESTABLISHED");
                connectionEstablished = true;
                sendMessage("CONFIRM".getBytes());
                onConnect();
            }

        }
        
    }
    
    //Send a message from the client to the server
    public void sendMessage(byte[] message) {
        
        try { 

            //If encrypted then encrypt the message
            if (connectionEstablished && IS_ENCRYPTED)
                message = new Encryptor().encryptText(new String(message), serverPublicKey).getBytes();

            //Write the message to socket writer
            sw.write(message);
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }    
    
    //Receive message from server
    public void receiveMessage(byte[] message) {
        
        //Update the time of the last received message
        updateConnect();
        
        try {
            
            recLock.acquire();

            //If encrypted then decrypt the received message
            if (connectionEstablished && IS_ENCRYPTED)
                message = new Decryptor().decryptText(new String(message), kg.getPrivateKey()).getBytes();

            //Add the message to the received queue
            recQueue.add(message);
            
        }
        
        catch (Exception e) {
            if (isRunning) e.printStackTrace();
        }
        
        recLock.release();
        
        //Start a new client thread to act on the new received message
        new Thread(this).start();
        
    }    
    
    //Synchronized read of a Message Queue returns first in line
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
    
    //Generate and send the handshake to the server
    private void sendHandshake() {
        //The handshake is the word "HELLO" followed by a random int less than 1000
        handshake = "HELLO " + new Random().nextInt((999 - 0) + 1) + 0;
        sw.write(handshake.getBytes());
    }
    
    //Send the client's public key to the server
    private void sendPublicKey() throws Exception {
        byte[]         sendData     = kg.getPublicKey().getEncoded();
        sw.write(sendData);
    }
    
    //Receive, construct and return a public key
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
    
    //Receive, construct and return a private key
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
    
    //Send the private client's private key to the server
    private void sendPrivateKey() throws Exception {
        byte[]         sendData     = kg.getPrivateKey().getEncoded();
        sw.write(sendData);
    }
    
    //Updates last connect to current time and resets the timeout warning
    public void updateConnect() {
        lastConnect = System.currentTimeMillis();
        timeWarn    = 0;
    }
    
    //Returns the time of the last message received from the server
    public long getLlastConnect() {
        return lastConnect;
    }
    
    //Increments the timeout warning
    public void timeWarn() {
        timeWarn++;
    }
    
    //Returns the current value of the client's timeout warning
    public int timeWarning() {
        return timeWarn;
    }
        
    //OVerridable method for handling  on client connecting
    public void onConnect() {
    }
    //OVerridable method for handling  on client disconnecting
    public void onDisconnect() {
    }
    //OVerridable method for handling  on client failing connection
    public void onConnectFailed() {
    }
    //OVerridable method for handling  on client losing connection
    public void onConnectionLost() {
    }
    //OVerridable method for handling client on server stopping
    public void onServerStop() {
    }
    
    //Returns the client ID
    public String getID() {
        return id;
    }
    
    //Returns whether the client is running
    public boolean isRunning() {
        return isRunning;
    }
   
    //Registers the CommandRegistry parameter with the Client's command executor
    public void RegisterCommands(CommandRegistry cr) {
        executor.registerCommands(cr);
    }
    
}
