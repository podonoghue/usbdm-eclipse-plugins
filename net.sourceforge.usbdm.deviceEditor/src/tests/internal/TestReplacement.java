package tests.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StringPair {
   String key;
   String value;
   StringPair(String key, String value) {
      this.key   = key;
      this.value  = value;
   }
};

public class TestReplacement {

   /**
    * Apply a set of template substitutions of form <b>%name</b> in template text
    * 
    * @param text          Text to modify
    * @param substitutions Substitutions to do
    * 
    * @return Modified test
    */
    static String doTemplateSubstitutions(String text, List<StringPair> substitutions) {
      
      if (text == null) {
         return null;
      }
      if (substitutions == null) {
         return text;
      }
      for (StringPair p:substitutions) {
         if (p.key==null) {
            System.err.println("key is null, value = "+p.value);
         }
         if (p.value==null) {
            System.err.println("value is null, res = "+p.key);
         }
         String pattern     = p.key;
         String replacement = p.value;
         if ((pattern.startsWith("$("))||(pattern.startsWith("%("))) {
            // Pattern is delimited - no extra checks
            pattern = Pattern.quote(p.key);
            replacement = Matcher.quoteReplacement(replacement);
         }
         else {
            // Assume pattern is a word.
            // Add delimiters to avoid matching shorter words
            pattern = Pattern.quote(p.key)+"(\\W|_|$)";
            replacement = Matcher.quoteReplacement(replacement)+"$1";
         }
         System.err.println("'"+ pattern + "' => '" + replacement + "'");

         Matcher m = Pattern.compile(pattern).matcher(text);
         boolean doneReplacement = false;
         StringBuffer sb = new StringBuffer();
         while (m.find()) {
             m.appendReplacement(sb, replacement);
             doneReplacement = true;
         }
         if (doneReplacement) {
            m.appendTail(sb);
            text = sb.toString();
         }
      }
      return text;
   }

   public static void main(String[] args) {
      
      String text =
              "      \t/**\n"
            + "      \t * Basic enable of $(_Class) %symbolicExpression; \n"
            + "      \t * Includes enabling clock and configuring all mapped pins if mapPinsOnEnable is selected in configuration\n"
            + "      \t */\n"
            + "      \tstatic void $(_namePrefix)enable() {\n";
      
      ArrayList<StringPair> substitutions =  new ArrayList<StringPair>();
      substitutions.add(new StringPair("$(_namePrefix)","_"));
      substitutions.add(new StringPair("$(_Class)","Spi0"));
      substitutions.add(new StringPair("%symbolicExpression","true"));
      
      
      
      
      for(StringPair sp:substitutions) {
         System.err.println("'"+sp.key + "' => '" + sp.value + "'");
      }
      System.err.println("Original Text = \n'" + text + "'");

      text = doTemplateSubstitutions(text,substitutions);
      
      System.err.println("New Text = \n'" + text + "'");
   }

}
