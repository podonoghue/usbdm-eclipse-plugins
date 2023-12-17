package tests.internal;

public class Testing {

   static String wrapText(String text, final int maxColumn) {
      int column = 0;
      StringBuilder lineBuffer = new StringBuilder();
      StringBuilder wordBuffer = new StringBuilder();
      
      boolean newLine = true;
      
      for (int chIndex=0; chIndex<text.length(); chIndex++) {
         char ch = text.charAt(chIndex);
         if (column>maxColumn) {
            lineBuffer.append("\n");
            column = wordBuffer.length();
            newLine = true;
         }
         else if (Character.isWhitespace(ch)) {
            if (wordBuffer.length()>0) {
               if (!newLine) {
                  lineBuffer.append(" ");
               }
               lineBuffer.append(wordBuffer);
               newLine = false;
               wordBuffer = new StringBuilder();
            }
            if (ch=='\n') {
               lineBuffer.append("\n");
               column = 0;
               newLine = true;
               continue;
            }
            if ((ch=='\t') && newLine) {
               lineBuffer.append("    ");
               continue;
            }
            continue;
         }
         wordBuffer.append(ch);
         column++;
      }
      if (wordBuffer != null) {
         if (!newLine) {
            lineBuffer.append(" ");
         }
         lineBuffer.append(wordBuffer);
      }
      return lineBuffer.toString();
   }
   
   public static void main(String[] args) {
      
      String testString1 = ""
            + "Canterbury raised some\n   eye brows"
            + " when they signed a   host of utility players"
            + "             but the Bulldogs are adamant the strategy can\thelp end the    club's longest "
            + "finals drought in almost 70 years.\n"
            + "\t- xxx\n"
            + "\t- yyy\n";
      System.out.println(wrapText(testString1, 40));
      String testString2 = ""
            + "Used to identify peripheral interrupt";
      System.out.println(wrapText(testString2, 40));
   }

}
