/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.tests;

import fatwalrus.server.Server;
import java.util.Scanner;

/**
 *
 * @author Bob
 */
public class TestServer {
    
    public static void main(String args[]) throws Exception {
        
        Server server = new Server(9876, true);
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
