/*
 * 
 * Based on org.eclipse.cdt.managedbuilder.internal.core.ManagedCommandLineInfo
 * 
 */

package net.sourceforge.usbdm.cdt.tools;

import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;

public class ManagedCommandLineInfo implements IManagedCommandLineInfo {

    String commandLine;
    String commandLinePattern;
    String commandName;
    String flags;
    String outputFlag;
    String outputPrefix;
    String output;
    String inputs;

    ManagedCommandLineInfo(String commandLine,  String commandLinePattern,
                           String commandName,  String flags,      String outputFlag,
                           String outputPrefix, String output, String inputs) {
        this.commandLine         = commandLine;            
        this.commandLinePattern  = commandLinePattern; 
        this.commandName         = commandName;        
        this.flags               = flags;              
        this.outputFlag          = outputFlag;         
        this.outputPrefix        = outputPrefix;       
        this.output              = output;         
        this.inputs              = inputs;     
    }

    @Override
    public String getCommandLine() {
        return commandLine;
    }

    @Override
    public String getCommandLinePattern() {
        return commandLinePattern;
    }

    @Override
    public String getCommandName() {
//       System.err.println("ManagedCommandLineInfo.getCommandName() ==> \'"+commandName+"\'");
        return commandName;
    }

    @Override
    public String getFlags() {
        return flags;
    }

    @Override
    public String getInputs() {
        return inputs;
    }

    @Override
    public String getOutput() {
        return output;
    }

    @Override
    public String getOutputFlag() {
        return outputFlag;
    }

    @Override
    public String getOutputPrefix() {
        return outputPrefix;
    }

}
