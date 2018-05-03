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
    
    private final   DatagramSocket     socket; //The socket that is being written to
    private final   ArrayList<byte[]>  messageQueue = new ArrayList<>(); //Queue of messages to be sent
    private final   Semaphore          lock = new Semaphore(1); //Lock for message queue
    private final   int                port; //Port to write to
    private final   InetAddress        ip; //Ip to write to
    private boolean go = true;
    
    //Set final fields on construct
    public SocketWriter(DatagramSocket socket, InetAddress ip, int port) {
        this.socket       = socket;
        this.port         = port;
        this.ip           = ip;
    }    

    //Writes a message to the SocketWriter's message queue and starts a new thread
    public void write(byte[] message) {
        try {
            lock.acquire();
            messageQueue.add(message);
            lock.release();
        }
        catch (InterruptedException e) {
            lock.release();
            e.printStackTrace();
        }
        //Write the message on a new thread
        new Thread(this).start();
    }
    
    //Stops the socket writer
    public void stop() {
        go = false;
    }    
    
    //Starts a thread and writes the first message in the queue to the socket and removes it
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
    
    //Synchronized read from the writer's message queue
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
            lock.release();
            e.printStackTrace();
        }
        
        return message;
        
    }     
    
}
