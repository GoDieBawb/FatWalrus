/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fatwalrus.commands;

import java.util.ArrayList;

/**
 *
 * @author Bob
 */
public abstract class CommandRegistry {
    
    protected ArrayList<String> commands = new ArrayList<>(); //List of Strings containing commands
    
    //Runs command string if registered with this command registry
    public abstract void run(String commandString);
    
    //Returns the list of commands
    public ArrayList<String> getCommands() {
        return commands;
    }
    
}
