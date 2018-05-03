/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.commands;

import fatwalrus.server.Server;
import java.util.Arrays;

/**
 *
 * @author Bob
 */
public class ServerCommandRegistry extends CommandRegistry {
    
    private final Server server; //The server to which this is registered to
    
    //Register commands on construct and set server value
    public ServerCommandRegistry(Server server) {
        this.server = server;
        commands.add("BRDCST");
        commands.add("SEND");
    }
    
    //Runs command string if registered with this command registry
    @Override
    public void run(String commandString) {
        
        String   command = commandString.split("_")[0]; //Get actual command
        String[] args    = commandString.split("_"); //list of command arguments
        
        //Remove command itself from args list if args exist
        if(commandString.contains("_")) args = Arrays.copyOfRange(args, 1, args.length);
        
        switch(command) {
            
            //SEND: Sends a message to the client id specified in args[0]
            case "SEND":
                sendMessage(args[0], commandString.split("_", 3)[2]);
                break;
            //BRDCST: Sends a message to all clients connected to the server    
            case "BRDCST":
                broadcastMessage(commandString.replaceAll("BRDCST_", "").getBytes());
                break;
            //If command is in the commands list but has no execution case it is improperly registered    
            default:
                System.out.println("ERROR: Improperly Registered Command: " + command);
                break;
                
        }
        
    }
    
    //Sends a message to a specific client
    private void sendMessage(String clientID, String message) {
        
        try {
            server.getConLock().acquire();
            server.getConnections().get(clientID).sendMessage(message.getBytes());
        }
        
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        server.getConLock().release();
        
    }
    
    //Sends a message to all clients connected to the server 
    private void broadcastMessage(byte[] message) {
        
        try {
            server.getConLock().acquire();
            server.getConnections().entrySet().forEach((cc) -> {
                cc.getValue().sendMessage(message);
            });
        }
        
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        server.getConLock().release();
        
    }       
    
}

