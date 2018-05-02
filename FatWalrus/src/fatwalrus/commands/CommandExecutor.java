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
    
    private final HashMap<String, CommandRegistry> commands;
    
    public CommandExecutor() {
        commands = new HashMap<>();
    }
    
    public void Execute(String commandString) {
        
        if (commandString.equals(""))
            return;
        
        String command = commandString.split("_")[0];
        
        if (commands.containsKey(command)) {
            commands.get(command).run(commandString);
        }
        
        else {
            System.out.println("ERROR: Bad command string: " + commandString);
        }
        
    }
    
    public void registerCommands(CommandRegistry cr) {
        
        cr.getCommands().forEach((command) -> {
            
            if (commands.containsKey(command)) {
                System.out.println("ERROR: Could not register command: " + command + " Already exists!");
            } 
            
            else {
                commands.put(command, cr);
            }
            
        });
        
    }
    
}
