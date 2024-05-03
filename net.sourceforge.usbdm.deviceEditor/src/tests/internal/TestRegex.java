package tests.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegex {

   static String cleanUp(String contents) {
      contents = contents.trim();        // Discard leading and trailing white space
      contents = contents.
            replaceAll("\n\\s*", "\n").  // Discard white space after newline
            replaceAll("\\\\t", "   ").  // Expand tab sequence to 3 spaces "\t"
            replaceAll("\\\\n", "\n");   // Expand newline sequence "\n"
      return contents;
   }
   public static void main(String[] args) {
      String[] tests = {
            " \n\t    hello there \n" +
            "                \t  \\tBye Bye   x\\nx  \n" +
            "  ",
      };
      
      for (String test:tests) {
         System.out.print("'" + test + "' => \n'" + cleanUp(test) + "'");
      }
      System.err.println();
      
      
      
      String text = "\\t   /// SIM CLKDIV1 System Clock Divider Register 1 %initExpression-\n\n ";
      String pattern = Pattern.quote("%initExpression");
//      String pattern = "%xxx(\\W).*";
//      String text    = " %xxx z%xxx xx%xxx ";
      
      Matcher m = Pattern.compile(pattern).matcher(text);
      String msg = "Pattern.compile("+pattern+").matcher("+text+");";
      if (m.matches()) {
         System.err.println(msg + " => matches");
         System.err.println("Group 1 = '" + m.group(1) + "'");
         System.err.println("Group 2 = '" + m.group(2) + "'");
      }
      else {
         System.err.println(msg + " => doesn't match");
      }
      String pattern2 = Pattern.quote("%initExpression")+"(\\W)";
      System.err.println("Result = " + text.replaceAll(pattern2, "aaa$1"));
   }

}
