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
      ArrayList<String> patterns = findAllPatterns(input);
      for (String p : patterns) {
         String replaceWith = map.get(p);
         if (replaceWith == null) {
            continue;
         }
         input = input.replaceAll("\\$\\("+p+"\\)", Matcher.quoteReplacement(replaceWith));
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
      String input = "$(This) is a $(test) with some $(not replaced) ";
      
      System.out.println("Input = " + input);

      map.put("This", "THIS");
      map.put("test", "TEST");
      ArrayList<String> x = findAllPatterns(input);
      for (String s : x) {
         System.out.println("Pattern = " + s);
      }

      String output = substitute(input, map);
      System.out.println("Output = " + output);
      
      System.out.println("Done");
   }
   
}
