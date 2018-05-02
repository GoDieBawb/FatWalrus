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
    
    protected ArrayList<String> commands = new ArrayList<>();
    
    public abstract void run(String commandString);
    
    public ArrayList<String> getCommands() {
        return commands;
    }
    
}
