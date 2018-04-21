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
    
    private final   DatagramSocket  socket;
    private final   Client          client;
    private boolean                 go = true;
    
    public ClientSocketListener(Client client, DatagramSocket socket) {
        this.client = client;
        this.socket = socket;
    }
    
    public void stop() {
        go = false;
    }
    
    @Override
    public void run() {
        
        byte[] receiveData  = new byte[2048];
        
        while (go) {

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                socket.receive(receivePacket);
            }
            catch (Exception e) {
                if (go)
                    e.printStackTrace();
            }
            
            try {
                byte[] recMess = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                client.receiveMessage(recMess);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            
        }
        
    }
    
}
