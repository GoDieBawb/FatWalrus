/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.input;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

/**
 *
 * @author Bob
 */
public class InputListener implements Runnable {
    
    private final   ArrayList<byte[]> messageQueue;
    private final   Semaphore         lock;
    private final   Scanner           scanner;
    private boolean go = true;
    
    public InputListener(ArrayList<byte[]>  messageQueue, Semaphore lock) {
    
        scanner           = new Scanner(System.in);
        this.messageQueue = messageQueue;
        this.lock         = lock;
        
    }
    
    public void stop() {
        go = false;
    }        
    
    @Override
    public void run() {
    
        while (go) {
        
            String input = scanner.nextLine();
            if (input.equals("q")) {
                go = false;
            }
            
            try {
                lock.acquire();
                messageQueue.add(input.getBytes());
                lock.release();
            }
                        
            catch(InterruptedException e) {
            }
            
        }
        
    }
    
}
