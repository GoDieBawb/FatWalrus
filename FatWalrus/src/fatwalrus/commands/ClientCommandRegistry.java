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
    
    private final Client client; //The client the registry belongs to
    
    //Register commands on construct and set client value
    public ClientCommandRegistry(Client client) {
        commands.add("PONG");
        commands.add("ECHO");
        commands.add("CHECKTIME");
        commands.add("TIMECHECK");
        commands.add("SRVSTP");
        this.client = client;
    }
    
    //Runs command string if registered with this command registry
    @Override
    public void run(String commandString) {
        
        //Commands are '_' separated lists
        String command   = commandString.split("_")[0]; //Get actual command
        String[] args    = commandString.split("_"); //list of command arguments
        
        //Remove command itself from args list if args exist
        if(commandString.contains("_")) args = Arrays.copyOfRange(args, 1, args.length);
        
        switch (command) {
            
            //PONG: print line Pong 
            case "PONG":
                System.out.println("PONG");
                break;
            //ECHO: print out the first argument received    
            case "ECHO":
                System.out.println("RECEIVED: " + args[0]);
                break;
            //CHECKTIME: Do nothing    
            case "CHECKTIME":
                break;     
            //TIMECHECK: Sends the message CHECKTIME to the server    
            case "TIMECHECK":
                client.sendMessage("CHECKTIME".getBytes());
                break;
            //SRVSTP: Alerts the client the server has stopped and stops client and runs onServerStop()    
            case "SRVSTP":
                System.out.println("Server is stopping...");
                client.stop();
                client.onServerStop();
                break;
            //If command is in the commands list but has no execution case it is improperly registered  
            default:
                System.out.println("ERROR: Improperly Registered Command: " + command);
                break;
                
        }
        
    }
    
}
