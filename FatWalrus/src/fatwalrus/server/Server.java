/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.server;

import fatwalrus.commands.CommandExecutor;
import fatwalrus.commands.CommandRegistry;
import fatwalrus.commands.ServerCommandRegistry;
import fatwalrus.network.ClientConnection;
import fatwalrus.network.ServerSocketListener;
import fatwalrus.encryption.KeyGenerator;
import fatwalrus.network.ConnectionHandler;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;


/**
 *
 * 
 * 
 * @author Bob
 */
public class Server implements Runnable {

    public static boolean IS_ENCRYPTED; //Determines whether the connection is encrypted
    
    private final KeyGenerator         kg        = new KeyGenerator(2048); //Encryption key generator
    private final Semaphore            conLock   = new Semaphore(1); //Semaphore for connections map
    private final Semaphore            recLock   = new Semaphore(1); //Semaphore for received Queue
    private final ArrayList<byte[]>    recQueue  = new ArrayList<>(); //Queue of received messages
    private final CommandExecutor      executor  = new CommandExecutor(); //Executes registered commands
    private boolean                    isRunning = false; //Whether the server is running or not
    private final DatagramSocket       socket; //The server's socket
    private final ServerSocketListener sl; //Listens for messages on the socket
    private final ConnectionHandler    cl; //Handles client disconnects
    private final int                  port; //The port the server will listen on
    private int                        timeout  = 10; //Timeout in seconds before client is disconnected
    
    private final HashMap<String, ClientConnection> connections = new HashMap<>(); //Map of Client id's to connections
    
    //Construct server and set final fields
    public Server(int port, boolean isEncrypted) throws Exception {
        
        //Determine if encrypted
        IS_ENCRYPTED = isEncrypted; 
        
         //Generate encryption keys
        kg.createKeys();
        
        //Set fields
        this.port = port;
        socket    = new DatagramSocket(port);
        sl        = new ServerSocketListener(this, connections, kg, conLock); 
        cl        = new ConnectionHandler(connections, conLock, timeout);
        
        //Register basic server commands
        executor.registerCommands(new ServerCommandRegistry(this));
        
    }
    
    //Starts the server
    public void start() {

        //Determine the server to be running
        isRunning = true;
        
        //Create and start threads for Runnables
        Thread lt = new Thread(sl);
        Thread ct = new Thread(cl);
        
        lt.start();
        ct.start();
        
    }
    
    //Stops the server
    public void stop() {
        
        //Broadcast message that the server is stopping
        broadcastMessage("SRVSTP");

        //Wait for final send/receive
        try {
            Thread.sleep(1000);
        } 
        catch(InterruptedException e) {
            e.printStackTrace();
        }
        
        //Stop Runnables
        sl.stop();
        cl.stop();
        
         
        //Close socket and determine stopped
        socket.close();
        isRunning = false;
        System.out.println("Stopped");
        
    }
    
    //Safely runs a command
    public void runCommand(String command) {
        addToReceived(command.getBytes());
    }
    
    //Safely calls the send command to send a message to a specific client
    public void sendMessage(String clientID, String message) {
        String command = "SEND_" + clientID + "_" + message;
        addToReceived(command.getBytes());
    }
    
    //Safely calls the broadcast command to send a message to all clients
    public void broadcastMessage(String message) {
        String command = "BRDCST_" + message;
        addToReceived(command.getBytes());
    }     
    
    //When server thread is started check to see if any pending commands
    @Override
    public void run() {
        checkReceived();
    }
    
    //Adds a message to the received queue and starts a thread to act on it
    private void addToReceived(byte[] message) {
        try {
            recLock.acquire();
            recQueue.add(message);
        }
                
        catch (Exception e) {
            if (isRunning) e.printStackTrace();
        }
        
        recLock.release();
        new Thread(this).start();
        
    }
    
    //Get first message in queue and execute command
    private void checkReceived() {
        
        byte[] received = readMessageQueue(recQueue, recLock);
        
        if (received == null) return;
            
        String message = new String(received);
        executor.Execute(message);
        
    }    
    
    //Synchrnonized read of a Queue of byte arrays
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
    
    //Registers a CommandRegistry with the server
    public void registerServerCommands(CommandRegistry commandRegistry) {
        executor.registerCommands(commandRegistry);
        onServerCommandsRegistered(commandRegistry);
    }
    
    //Registers a CommandRegistry to already connected clients
    public void registerClientCommands(CommandRegistry commandRegistry) {
        
        connections.entrySet().forEach((cc) -> {
            cc.getValue().registerCommands(commandRegistry);
            onClientConnectionCommandsRegistered(cc.getValue(), commandRegistry);
        });
        
    }
    
    //Overridable method to handle when client connects
    public void onClientConnected(ClientConnection cc) {}
    //Overridable method to handle when client disconnects
    public void onClientDisconnected(ClientConnection cc, String reason) {}    
    //Overridable method to handle when client connection commands are registered
    public void onClientConnectionCommandsRegistered(ClientConnection cc, CommandRegistry cr) {}
    //Overridable method to handle when server commands are registered
    public void onServerCommandsRegistered(CommandRegistry cr) {}
    
    //Returns the server's socket
    public DatagramSocket getSocket() {
        return socket;
    }
    
    //Returns whether the server is running or not
    public boolean isRunning() {
        return isRunning;
    }
    
    //Returns the map of the client ids and connections
    public HashMap<String, ClientConnection> getConnections() {
        return connections;
    }
    
    //Gets the Semaphore for the connections HashMap
    public Semaphore getConLock() {
        return conLock;
    }
    
    //Sets the timeout before client is kicked
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    //Returns the timeout value before client is kicked
    public int getTimeout() {
        return timeout;
    }    
   
}
