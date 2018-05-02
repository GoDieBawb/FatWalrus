/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.network;

import fatwalrus.client.Client;

/**
 *
 * @author Bob
 */
public class ClientTimeoutHandler implements Runnable {
    
    private final Client  client;
    private       boolean go = true;
    
    public ClientTimeoutHandler(Client client) {
        this.client = client;
    }
    
    public void stop() {
        go = false;
    }
    
    @Override
    public void run() {
    
        while (go) {
            
            try {Thread.sleep(3000);}
            catch (InterruptedException e) {}
            
            long receiveDelay = System.currentTimeMillis()-client.getLlastConnect();
            
            if (receiveDelay > 5) {
                client.sendMessage("TIMECHECK".getBytes());
                client.timeWarn();
            }
            
            if (client.timeWarning() > 5) {
                client.stop();
                client.onConnectionLost();
            }
            
        }
        
    }
    
}
