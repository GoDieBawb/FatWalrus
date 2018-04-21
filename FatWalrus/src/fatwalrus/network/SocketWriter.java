/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

/**
 *
 * @author Bob
 */
public class SocketWriter implements Runnable {
    
    private final   DatagramSocket     socket;
    private final   ArrayList<byte[]>  messageQueue = new ArrayList();
    private final   Semaphore          lock = new Semaphore(1);
    private final   int                port;
    private final   InetAddress        ip;
    private boolean go = true;
    
    public SocketWriter(DatagramSocket socket, InetAddress ip, int port) {
        this.socket       = socket;
        this.port         = port;
        this.ip           = ip;
    }    

    public void write(byte[] message) {
        try {
            lock.acquire();
            messageQueue.add(message);
            lock.release();
        }
        catch (InterruptedException e) {
            lock.release();
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
        }
        
        return message;
        
    } 
    
    public void stop() {
        go = false;
    }    
    
    @Override
    public void run() {
        
        if (port == 0) return;
            
            try {
                
            byte[] sendData = readMessageQueue(messageQueue,lock);
            if (sendData == null) return;

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ip, port);
            socket.send(sendPacket);

        }

        catch(IOException e) {
            if (go)
            e.printStackTrace();
        }
            
    }
    
}
