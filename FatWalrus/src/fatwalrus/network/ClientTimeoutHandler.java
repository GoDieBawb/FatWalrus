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
    
    private final Client  client; //Client the handler belongs to
    private       boolean go = true; //Determines whether the runnable should continue to loop
    
    //Set final fields
    public ClientTimeoutHandler(Client client) {
        this.client = client;
    }
    
    //Stops the timeout handler
    public void stop() {
        go = false;
    }
    
    //Starts checking for client timing out
    @Override
    public void run() {
    
        //While client is running
        while (go) {
            
            //Wait 3 seconds
            try {Thread.sleep(3000);}
            catch (InterruptedException e) {}
            
            //Check when the last message received from the server was
            long receiveDelay = System.currentTimeMillis()-client.getLlastConnect();
            
            //If more than 5 seconds have passed request a timecheck and increment and warn client
            if (receiveDelay > 5*1000) {
                client.sendMessage("TIMECHECK".getBytes());
                client.timeWarn();
            }
            
            //If client has made more than 5 attempts to reconnect determine connection lost
            if (client.timeWarning() > 5) {
                client.stop();
                client.onConnectionLost();
            }
            
        }
        
    }
    
}
