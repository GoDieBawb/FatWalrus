/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.network;

import fatwalrus.client.Client;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/**
 *
 * @author Bob
 */
public class ClientSocketListener implements Runnable {
    
    private final   DatagramSocket  socket; //Client's socket
    private final   Client          client; //Client itself
    private boolean                 go = true; //Determines whether to continue loop
 
    //Set finals fields on construct
    public ClientSocketListener(Client client, DatagramSocket socket) {
        this.client = client;
        this.socket = socket;
    }
    
    //Stop Socket Listener
    public void stop() {
        go = false;
    }
    
    //Starts listening for messages on the socket
    @Override
    public void run() {
        
        //Set basic receive data buffer size
        byte[] receiveData  = new byte[2048];
        
        //While client is running
        while (go) {

            //Construct new receive packet
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                //Wait for packet received
                socket.receive(receivePacket);
            }
            catch (Exception e) {
                if (go)
                    e.printStackTrace();
            }
            
            try {
                //Create formatted byte array and send message to client
                byte[] recMess = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                client.receiveMessage(recMess);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
        }
        
    }
    
}
