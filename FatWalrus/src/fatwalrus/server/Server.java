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

    public static boolean IS_ENCRYPTED;
    
    private final KeyGenerator         kg        = new KeyGenerator(2048);
    private final Semaphore            conLock   = new Semaphore(1);
    private final Semaphore            recLock   = new Semaphore(1);
    private final ArrayList<byte[]>    recQueue  = new ArrayList();
    private final CommandExecutor      executor  = new CommandExecutor();
    private boolean                    isRunning = false;
    private final DatagramSocket       socket;
    private final ServerSocketListener sl;
    private final ConnectionHandler    cl;
    private final int                  port;
    private int                        timeout  = 10;
    
    private final HashMap<String, ClientConnection> connections = new HashMap();
    
    public Server(int port, boolean isEncrypted) throws Exception {
        
        IS_ENCRYPTED = isEncrypted;
        
        kg.createKeys();
        this.port = port;
        socket    = new DatagramSocket(port);
        sl        = new ServerSocketListener(this, connections, kg, conLock);
        cl        = new ConnectionHandler(connections, conLock, timeout);
        executor.registerCommands(new ServerCommandRegistry(this));
        
    }
    
    public void start() {
        
        System.out.println("Starting Server on port " + port + "...");

        isRunning = true;
        
        Thread lt = new Thread(sl);
        Thread ct = new Thread(cl);
        
        lt.start();
        ct.start();
        
    }
    
    public void stop() {
        
        System.out.println("Stopping Server...");
        
        broadcastMessage("SRVSTP");

        try {
            Thread.sleep(1000);
        } 
        catch(InterruptedException e) {
            e.printStackTrace();
        }
        
        sl.stop();
        cl.stop();
        
         
        socket.close();
        isRunning = false;
        System.out.println("Stopped");
        
    }
    
    public void runCommand(String command) {
        addToReceived(command.getBytes());
    }
    
    public void sendMessage(String clientID, String message) {
        String command = "SEND_" + clientID + "_" + message;
        addToReceived(command.getBytes());
    }
    
    public void broadcastMessage(String message) {
        String command = "BRDCST_" + message;
        addToReceived(command.getBytes());
    }     
    
    @Override
    public void run() {
        checkReceived();
    }
    
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
    
    private void checkReceived() {
        
        byte[] received = readMessageQueue(recQueue, recLock);
        
        if (received == null) return;
            
        String message = new String(received);
        executor.Execute(message);
        
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
    
    public void registerServerCommands(CommandRegistry commandRegistry) {
        executor.registerCommands(commandRegistry);
        onServerCommandsRegistered(commandRegistry);
    }
    
    public void registerClientCommands(CommandRegistry commandRegistry) {
        
        connections.entrySet().forEach((cc) -> {
            cc.getValue().registerCommands(commandRegistry);
            onClientConnectionCommandsRegistered(cc.getValue(), commandRegistry);
        });
        
    }
    
    public void onClientConnected(ClientConnection cc) {}
    
    public void onClientDisconnected(ClientConnection cc, String reason) {}    
    
    public void onClientConnectionCommandsRegistered(ClientConnection cc, CommandRegistry cr) {}
    
    public void onServerCommandsRegistered(CommandRegistry cr) {}
    
    public DatagramSocket getSocket() {
        return socket;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public HashMap<String, ClientConnection> getConnections() {
        return connections;
    }
    
    public Semaphore getConLock() {
        return conLock;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getTimeout() {
        return timeout;
    }    
   
}
