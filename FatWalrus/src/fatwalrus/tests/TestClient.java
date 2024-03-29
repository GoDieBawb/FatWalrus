/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.tests;

import fatwalrus.client.Client;
import java.util.Random;

/**
 *
 * @author Bob
 */
public class TestClient {
    
    public static void main(String args[]) throws Exception {
        
        test();
        
    }    
    
    public static void test() throws Exception {
        
        Client client = new Client("127.0.1", 9876, true) {
            @Override
            public void onConnectionLost() {
                System.out.println("Lost Connection to Server!");
            }
        };
        client.start();
        
        String in = "";
        
        while (!in.equals("q") && client.isRunning()) {
            int del = new Random().nextInt((10 - 0) + 1) + 0;
            client.sendMessage("PING".getBytes());
            client.sendMessage("ECHO_testing this".getBytes());
            client.sendMessage("CHECKTIME".getBytes());
            Thread.sleep(del*100);
            int dis = new Random().nextInt((1000 - 0) + 1) + 0;
            if (dis==999) break;
        }
                
        System.out.println("Good Bye!"); 
        
    }

}
