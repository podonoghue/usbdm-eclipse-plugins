package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MacroSubstitute {

   /**
    * Replaces macros e.g. $(key) with values from a map
    * 
    * @param input String to replace macros in
    * @param map   Map of key->value pairs for substitution
    * 
    * @return      String with substitutions (or original if none)
    */
   public static String substitute(String input, Map<String,String> map) {
      
      if (input == null) {
         return null;
      }
      if (map == null) {
         return input;
      }
      ArrayList<String> patterns = findAllPatterns(input);
      Pattern variablePattern = Pattern.compile("([^:]+):(.*)");
      for (String pattern : patterns) {
         // p is the middle part of the pattern 
    	 // e.g. $(pattern) => pattern, $(pattern:default) => pattern:default
         Matcher matcher = variablePattern.matcher(pattern);
         String key = pattern;
    	 String defaultValue = null;
         if (matcher.find()) {
    		key          = matcher.group(1);
    		defaultValue = matcher.group(2);
//    		System.out.println(String.format("p=\'%s\', d=\'%s\'", pattern, defaultValue));
    	 }
         String replaceWith = map.get(key);
         if (replaceWith == null) {
//        	 System.out.println("Using default \'" + defaultValue + "\'");
        	 replaceWith = defaultValue;
         }
         if (replaceWith == null) {
             continue;
         }
         input = input.replaceAll("\\$\\("+pattern+"\\)", Matcher.quoteReplacement(replaceWith));
      }
      return input;
   }
   
   /**
    * Finds all $(..) patterns in string
    * 
    * @param input
    * @return array of names within the $(...)
    */
   public static ArrayList<String> findAllPatterns(String input) {
      ArrayList<String> patterns = new ArrayList<String>();
      
      Pattern pattern = Pattern.compile("\\$\\([^\\)]+\\)");
      Matcher matcher = pattern.matcher(input);
      if (matcher.find(0)) {
         do {
            patterns.add(input.substring(matcher.start()+2, matcher.end()-1));
         } while (matcher.find());
      }
      return patterns;
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      System.out.println("Starting");
      Map<String,String> map = new HashMap<String,String>();
      String input = 
		  "$(NoDef1)        => \'NoDefault1\'\n" +
		  "$(NoDef2)        => \'NoDef2\' (with prefix)\n" +
		  "$(Def1:XXXX)     => \'Default1-found\'\n" +
		  "$(Def2:Default2-used) => \'Default2-used\'\n";
      
      System.out.println("Input = \n" + input);

      map.put("NoDef1", "NoDefault1");
      map.put("Def1",   "Default1-found");
      
      ArrayList<String> x = findAllPatterns(input);
      for (String s : x) {
         System.out.println("Pattern = " + s);
      }

      String output = substitute(input, map);
      System.out.println("\nOutput = \n" + output);
      
      System.out.println("Done");
   }
   
}
