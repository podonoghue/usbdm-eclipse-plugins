/*
 * Basic ManagedCommandLineGenerator
 * 
 *  Applies variable substitution
 * 
 *  Based on org.eclipse.cdt.managedbuilder.internal.core.ManagedCommandLineGenerator
 */

package net.sourceforge.usbdm.cdt.tools;


import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * @author podonoghue
 *
 */
public class ManagedCommandLineGenerator implements IManagedCommandLineGenerator {

   private class RelacementPair {
      public String key;
      public String value;
      public RelacementPair(String key, String value) {
         this.key   = key;
         this.value = value;
      }
   };

   static final String OPEN_VAR       = "${";
   static final String CLOSE_VAR      = "}";
   static final String DOUBLE_QUOTE   = "\"";
   static final String WHITE_SPACE    = " ";

   /**
    * Creates a variable reference from a string i.e. surround with "${...}"
    * 
    * @param token to quote
    * 
    * @return quoted token
    */
   private String makeVariable(String variableName) {
      return OPEN_VAR+variableName+CLOSE_VAR; //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Optionally quote a token.
    * The token is quoted if it is not a variable reference i.e. doesn't start with "$("
    * 
    * @param token to quote
    * 
    * @return quoted token
    */
   private String quoteToken(String token) {
      if ((token == null) || (token.length() == 0) || (token.indexOf("$(") == 0)) { //$NON-NLS-1$
         return token;
      }
      return DOUBLE_QUOTE+token+DOUBLE_QUOTE;
   }

   private static final String DEFAULT_PATTERN = "${COMMAND} ${FLAGS} ${OUTPUT_FLAG} ${OUTPUT_PREFIX}${OUTPUT} ${INPUTS}"; //$NON-NLS-1$

   @Override
   public IManagedCommandLineInfo generateCommandLineInfo(
         ITool tool,
         String     commandName,
         String[]   flags,
         String     outputFlag,
         String     outputPrefix,
         String     output,
         String[]   inputResources,
         String     commandLinePattern) {

      if ((commandLinePattern == null) || (commandLinePattern.length() == 0)) {
         commandLinePattern = DEFAULT_PATTERN;
      }
//      System.err.println("ManagedCommandLineGenerator.generateCommandLineInfo(output=\'"+output.toString()+"\')");
      output = quoteToken(output);

      String inputsStr = stringArrayToString(inputResources, false);
      String flagsStr  = stringArrayToString(flags, false);

//      System.err.println("ManagedCommandLineGenerator.generateCommandLineInfo(inputsStr=\'"+inputsStr+"\')");
//      System.err.println("ManagedCommandLineGenerator.generateCommandLineInfo(flagsStr =\'"+flagsStr+"\')");
//      System.err.println("ManagedCommandLineGenerator.generateCommandLineInfo(output   =\'"+output+"\')");
      
      RelacementPair[] replacementPair = {
            new RelacementPair("COMMAND", 		 commandName),
            new RelacementPair("FLAGS",   		 flagsStr),
            new RelacementPair("OUTPUT_FLAG", 	 outputFlag),
            new RelacementPair("OUTPUT_PREFIX",  outputPrefix),
            new RelacementPair("OUTPUT", 		    output),
            new RelacementPair("INPUTS", 		    inputsStr),
      };
      
      String command = commandLinePattern;
      for (RelacementPair pair:replacementPair) {
//            System.err.println("/'" + makeVariable(pair.key)+ "/'==>/'" + pair.value + "/'");
         command = command.replace(makeVariable(pair.key), pair.value);
         command = command.replace(makeVariable(pair.key).toLowerCase(), pair.value);
      }
      try {
         IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
//          System.err.println("ManagedCommandLineGenerator.generateCommandLineInfo() Before newCommandName = \'" + newCommandName);
         commandName = manager.performStringSubstitution(commandName);
      } catch (CoreException e1) {
         e1.printStackTrace();
      }
//      System.err.println("ManagedCommandLineGenerator.generateCommandLineInfo() commandName = \'"+commandName+"\'");
      return new ManagedCommandLineInfo(command.trim(), commandLinePattern, commandName, flagsStr,
            outputFlag, outputPrefix, output, inputsStr);
   }

   protected String stringArrayToString(String[] str, boolean quoteStrings) {
      if ((str == null) || str.length == 0) {
         return new String();
      }
      StringBuilder buffer = new StringBuilder();
      for (String s : str) {
         if (quoteStrings) {
            s = quoteToken(s);
         }
         buffer.append(s);
         buffer.append(WHITE_SPACE);
      }
      return buffer.toString();
   }
}
