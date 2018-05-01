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
    
    private final Server server;
    
    public ServerCommandRegistry(Server server) {
        this.server = server;
        commands.add("BRDCST");
        commands.add("SEND");
    }
    
    @Override
    public void run(String commandString) {
        
        String   command = commandString.split("_")[0];
        String[] args    = commandString.split("_");
        args             = Arrays.copyOfRange(args, 1, args.length);
        
        switch(command) {
            case "SEND":
                sendMessage(args[0], commandString.split("_", 3)[2]);
                break;
            case "BRDCST":
                broadcastMessage(commandString.replaceAll("BRDCST_", "").getBytes());
                break;
            default:
                System.out.println("ERROR: Improperly Registered Command: " + command);
                break;
        }
        
    }
    
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

