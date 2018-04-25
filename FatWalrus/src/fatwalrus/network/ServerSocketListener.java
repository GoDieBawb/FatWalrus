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
    
    private final Server             server;
    private final DatagramSocket     socket;
    private final PublicKey          publicKey;
    private final PrivateKey         privateKey;
    private final Semaphore          conLock;
    private final HashMap<String, ClientConnection> connections;
    private boolean go = true;
    
    public ServerSocketListener(Server server, HashMap<String, ClientConnection> connections, KeyGenerator kg, Semaphore conLock) {
        this.server       = server;
        this.socket       = server.getSocket();
        this.connections  = connections;
        this.publicKey    = kg.getPublicKey();
        this.privateKey   = kg.getPrivateKey();
        this.conLock      = conLock;
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

                InetAddress ip   = receivePacket.getAddress();
                int         port = receivePacket.getPort();
                String      key  = ip + ":" + port;
                
                ClientConnection cc;
                
                conLock.acquire();
                
                if (connections.containsKey(key)) {
                    cc = connections.get(key);
                }
                
                else {
                    
                    if (port == -1) continue;
                    
                    cc = new ClientConnection(server, ip, port, privateKey);
                    if (Server.IS_ENCRYPTED)
                        cc.sendMessage(publicKey.getEncoded());
                    connections.put(key, cc);
                    
                }
                conLock.release();
                
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
