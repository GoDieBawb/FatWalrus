/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.tests;

import fatwalrus.network.ClientConnection;
import fatwalrus.server.Server;
import java.util.Scanner;

/**
 *
 * @author Bob
 */
public class TestServer {
    
    public static void main(String args[]) throws Exception {
        
        Server server = new Server(9876, true) {
        
            @Override
            public void onClientDisconnected(ClientConnection cc, String reason) {
                System.out.println(cc.getId() + " Has Disconntected: " + reason);
            }
        };
        server.start();
        
        String in = "";
        
        while (!in.equals("q")) {
            if (in.equals("q")) continue;
            in = new Scanner(System.in).nextLine();
        }
        
        server.stop();
        System.out.println("Good Bye!");
        
    } 
    
}
