package tests.internal;

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
   }

}
