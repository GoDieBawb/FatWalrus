/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 *
 * @author Bob
 */
public class ConnectionChecker implements Runnable {
    
    private final Semaphore                         conLock;
    private final HashMap<String, ClientConnection> connections;
    private final int                               timeout;
    private boolean go = true;
    
    public ConnectionChecker(HashMap connections, Semaphore conLock, int timeout) {
        this.connections = connections;
        this.conLock     = conLock;
        this.timeout     = timeout;
    }
    
    public void stop() {
        go = false;
    }
    
    @Override
    public void run() {
        
        while (go) {
            checkConnections();
            try {Thread.sleep(1000);} catch(InterruptedException e){}
        }
        
    }
    
    private void checkConnections() {
        
        try {
            
            conLock.acquire();

            ArrayList<String> removes = new ArrayList();

            connections.entrySet().forEach((cc) -> {

                if (checkForTimeout(cc.getValue())) { 
                    cc.getValue().disconnect();
                    System.out.println(cc.getKey() + " has timed out!");
                }

                if (cc.getValue().hasDisconnected()) {
                    removes.add(cc.getKey());
                }

            });

            removes.forEach((key) -> {
                connections.get(key).stop();
                connections.remove(key);
            });

            conLock.release();
            
        }
        
        catch(InterruptedException e) {
            e.printStackTrace();
            conLock.release();
        }
        
        
    }

    
    private boolean checkForTimeout(ClientConnection cc) {
        
        long receiveDelay = System.currentTimeMillis()-cc.getLlastConnect();

        if (receiveDelay > timeout*1000) {
            return true;
        }

        else if (receiveDelay > timeout*750 && cc.timeWarning() == 1) {
            cc.sendMessage("TIMECHECK".getBytes());
            cc.timeWarn();
        }

        else if (receiveDelay > timeout*500 && cc.timeWarning() == 0) {
            cc.sendMessage("TIMECHECK".getBytes());
            cc.timeWarn();
        }
        
        return false;
        
    }    
    
    
}
