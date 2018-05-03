/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.network;

import fatwalrus.encryption.KeyGenerator;
import fatwalrus.server.Server;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 *
 * @author Bob
 */
public class ServerSocketListener implements Runnable {
    
    private final Server             server; //Server that this listener belongs to
    private final DatagramSocket     socket; //Socket to be listened on
    private final PublicKey          publicKey; //The server's public key
    private final PrivateKey         privateKey; //The server's private key
    private final Semaphore          conLock; //Semaphore for connections HashMap
    private final HashMap<String, ClientConnection> connections; //Contains connection id and connection
    private boolean go = true; //Determines whether to keep listening
    
    //Pass in parameters and set final fields on construct
    public ServerSocketListener(Server server, HashMap<String, ClientConnection> connections, KeyGenerator kg, Semaphore conLock) {
        this.server       = server;
        this.socket       = server.getSocket();
        this.connections  = connections;
        this.publicKey    = kg.getPublicKey();
        this.privateKey   = kg.getPrivateKey();
        this.conLock      = conLock;
    }
    
    //Stop listening
    public void stop() {
        go = false;
    }
    
    //Listens and addresses multiple connections to the server
    @Override
    public void run() {
        
        //Set basic receive data buffer size
        byte[] receiveData  = new byte[2048];
        
        //While the server is still running
        while (go) {

            //Construct packet to be received
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                //Wait to receive packet
                socket.receive(receivePacket);
            }
            catch (Exception e) {
                if (go)
                    e.printStackTrace();
            }
            
            try {

                //Get connection information from packet and construct ID
                InetAddress ip   = receivePacket.getAddress();
                int         port = receivePacket.getPort();
                String      key  = ip + ":" + port;
                
                //Declare Client Connection
                ClientConnection cc;
                
                conLock.acquire();
                
                //If connection already exists get existing connection
                if (connections.containsKey(key)) {
                    cc = connections.get(key);
                }
                
                //If client does not exist construct ClientConnection and add to Connection map
                else {
                    
                    if (port == -1) continue;
                    
                    cc = new ClientConnection(server, ip, port, privateKey);
                    if (Server.IS_ENCRYPTED)
                        cc.sendMessage(publicKey.getEncoded());
                    connections.put(key, cc);
                    
                }
                conLock.release();
                
                //Create formatted byte array and send message to client handler
                byte[] recMess = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());
                cc.receiveMessage(recMess);
                
            }
            
            catch(Exception e) {
                if (go)
                    e.printStackTrace();
            }
            
        }
        
    }
    
}
