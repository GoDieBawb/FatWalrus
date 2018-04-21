/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.commands;

import fatwalrus.network.ClientConnection;
import java.util.Arrays;

/**
 *
 * @author Bob
 */
public class ClientConnectionCommandRegistry extends CommandRegistry {
    
    private final ClientConnection cc;
    
    public ClientConnectionCommandRegistry(ClientConnection cc) {
        commands.add("DISCONNECT");
        commands.add("CHECKTIME");
        commands.add("ECHO");
        commands.add("PING");
        this.cc = cc;
    }
    
    @Override
    public void run(String commandString) {
        
        String   command = commandString.split("_")[0];
        String[] args    = commandString.split("_");
        args             = Arrays.copyOfRange(args, 1, args.length);
        
        switch(command) {
            
            case "DISCONNECT":
                System.out.println("Client: " + cc.getId() + " Disconnected!");
                cc.disconnect();
                break;
            case "CHECKTIME":
                break;
            case "ECHO":
                System.out.println("ECHOING!");
                cc.sendMessage(("ECHO_" + args[0]).getBytes());
                break;
            case "PING":
                cc.sendMessage("PONG".getBytes());
                break;
            default:
                System.out.println("ERROR: Improperly Registered Command: " + command);
                break;
                
        }
        
    }
    
}
