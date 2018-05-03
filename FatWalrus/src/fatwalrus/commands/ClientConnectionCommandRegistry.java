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
    
    private final ClientConnection cc; //The client connection to which this is registred
    
    //Register commands on construct and set ClientConnection value
    public ClientConnectionCommandRegistry(ClientConnection cc) {
        commands.add("DISCONNECT");
        commands.add("CHECKTIME");
        commands.add("TIMECHECK");
        commands.add("ECHO");
        commands.add("PING");
        this.cc = cc;
    }
    
    //Runs command string if registered with this command registry
    @Override
    public void run(String commandString) {
        
        //Commands are '_' separated lists
        String   command = commandString.split("_")[0]; //Get actual command
        String[] args    = commandString.split("_"); //list of command arguments
        
        //Remove command itself from args list if args exist
        if(commandString.contains("_")) args = Arrays.copyOfRange(args, 1, args.length);
        
        switch(command) {
            
            //DISCONNECT: Disconnects the clients connection and sets the reason to leaving
            case "DISCONNECT":
                cc.disconnect("Leaving");
                break;
            //CHECKTIME: Do nothing    
            case "CHECKTIME":
                break;
            //TIMECHECK: Send the command CHECKTIME to the client    
            case "TIMECHECK":
                cc.sendMessage("CHECKTIME".getBytes());
                break;     
            //ECHO: Returns the first argument of the command string    
            case "ECHO":
                System.out.println("ECHOING!");
                cc.sendMessage(("ECHO_" + args[0]).getBytes());
                break;
            //PING: Send the command PONG to the client    
            case "PING":
                cc.sendMessage("PONG".getBytes());
                break;
            //If command is in the commands list but has no execution case it is improperly registered      
            default:
                System.out.println("ERROR: Improperly Registered Command: " + command);
                break;
                
        }
        
    }
    
}
