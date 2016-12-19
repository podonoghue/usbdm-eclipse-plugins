package Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TryRegex {

   static void testPattern(Pattern pattern, String s) {
      Matcher m = pattern.matcher(s);
      System.err.println("Testing \'"+pattern.pattern() + "\', against \'" + s + "\'");
      if (!m.matches()) {
         System.err.println("No matches");
      } 
      else {
         for (int index=1; index<=m.groupCount(); index++) {
            System.err.println(String.format("$%d = \'%s\'", index, m.group(index)));
         }
      }
   }
   public static void main(String[] args) {
      final Pattern pattern = Pattern.compile("(.+?)([0-9|A-F|a-f]+)$");
      
      testPattern(pattern, "");
      testPattern(pattern, "DIRECT");
      testPattern(pattern, "DIRECT1");
      testPattern(pattern, "DIRECT10");
   }

}
