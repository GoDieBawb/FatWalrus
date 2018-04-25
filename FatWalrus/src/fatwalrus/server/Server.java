/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.server;

import fatwalrus.commands.CommandExecutor;
import fatwalrus.commands.CommandRegistry;
import fatwalrus.encryption.Decryptor;
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
    private int                        timeout  = 30;
    
    private final HashMap<String, ClientConnection> connections = new HashMap();
    
    public Server(int port, boolean isEncrypted) throws Exception {
        
        IS_ENCRYPTED = isEncrypted;
        
        kg.createKeys();
        socket = new DatagramSocket(port);
        sl     = new ServerSocketListener(this, connections, kg, conLock);
        cl     = new ConnectionHandler(connections, conLock, timeout);
        
    }
    
    public void start() {
        
        System.out.println("Starting Server...");

        isRunning = true;
        
        Thread lt = new Thread(sl);
        Thread ct = new Thread(cl);
        
        lt.start();
        ct.start();
        
    }
    
    public void stop() {
        
        System.out.println("Stopping Server...");
        broadcastMessage("KICK");
        try {Thread.sleep(1000);} catch(InterruptedException e){}
        sl.stop();
        cl.stop();
        socket.close();
        isRunning = false;
        
    }
    
    public void sendMessage(String clientID, String message) {
        
        try {
            
            conLock.acquire();
            connections.get(clientID).sendMessage(message.getBytes());
            conLock.release();
            
        }
        
        catch (InterruptedException e) {
            conLock.release();
        }
        
    }
    
    public void broadcastMessage(String message) {
        
        try {
            conLock.acquire();
            connections.entrySet().forEach((cc) -> {
                cc.getValue().sendMessage(message.getBytes());
                cc.getValue().stop();
            });
            conLock.release();
        }
        catch (InterruptedException e) {
            conLock.release();
        }
        
    }     
    
    @Override
    public void run() {
        checkReceived();
    }
    
    public void receiveMessage(byte[] message) {
        
        try {
            
            recLock.acquire();

            if (IS_ENCRYPTED)
                message = new Decryptor().decryptText(new String(message), kg.getPrivateKey()).getBytes();

            recQueue.add(message);
            recLock.release();
            
        }
        
        catch (Exception e) {
            recLock.release();
            if (isRunning) e.printStackTrace();
        }
        
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
    }
    
    public void registerClientCommands(CommandRegistry commandRegistry) {
        
        connections.entrySet().forEach((cc) -> {
            cc.getValue().registerCommands(commandRegistry);
        });
        
    }
    
    public void onClientConnected(ClientConnection cc) {}
    
    public void onClientDisconnected(ClientConnection cc, String reason) {}    
    
    public DatagramSocket getSocket() {
        return socket;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public HashMap getConnections() {
        return connections;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getTimeout() {
        return timeout;
    }    
   
}
