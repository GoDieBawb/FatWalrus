/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.commands;

import java.util.HashMap;

/**
 *
 * @author Bob
 */
public class CommandExecutor {
    
    private final HashMap<String, CommandRegistry> commands; //Contains commands and their registries
    
    //Construct commands hashmap in constructor
    public CommandExecutor() {
        commands = new HashMap<>();
    }
    
    //Attempt to execute a command string
    public void Execute(String commandString) {
        
        //If received string is empty return
        if (commandString.equals(""))
            return;
        
        //Commands are '_' separated lists
        String command = commandString.split("_")[0]; //Get actual command
        
        //If command is registered to a registry execute that command via that command registry
        if (commands.containsKey(command)) {
            commands.get(command).run(commandString);
        }
        
        //If command is unrecognized the command has not been registered
        else {
            System.out.println("ERROR: Bad command string: " + commandString);
        }
        
    }
    
    //Registeres a CommandRegistry with this executor
    public void registerCommands(CommandRegistry cr) {
        
        //Iterate over CommandRegistry's commands
        cr.getCommands().forEach((command) -> {
            
            //If command already exists it cannot be registered
            if (commands.containsKey(command)) {
                System.out.println("ERROR: Could not register command: " + command + " Already exists!");
            } 
            
            //Register the command with its command registry
            else {
                commands.put(command, cr);
            }
            
        });
        
    }
    
}
