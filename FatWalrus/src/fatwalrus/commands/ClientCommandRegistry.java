/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.commands;

import fatwalrus.client.Client;
import java.util.Arrays;

/**
 *
 * @author Bob
 */
public class ClientCommandRegistry extends CommandRegistry {
    
    private final Client client;
    
    public ClientCommandRegistry(Client client) {
        commands.add("PONG");
        commands.add("ECHO");
        commands.add("CHECKTIME");
        commands.add("TIMECHECK");
        commands.add("SRVSTP");
        this.client = client;
    }
    
    @Override
    public void run(String commandString) {
        String command   = commandString.split("_")[0];
        String[] args    = commandString.split("_");
        
        if(commandString.contains("_"))
            args = Arrays.copyOfRange(args, 1, args.length);
        
        switch (command) {
            case "PONG":
                System.out.println("PONG");
                break;
            case "ECHO":
                System.out.println("RECEIVED: " + args[0]);
                break;
            case "CHECKTIME":
                break;                
            case "TIMECHECK":
                client.sendMessage("CHECKTIME".getBytes());
                break;
            case "SRVSTP":
                System.out.println("Server is stopping...");
                client.stop();
                client.onServerStop();
                break;
            default:
                System.out.println("ERROR: Improperly Registered Command: " + command);
                break;
        }
        
    }
    
}
