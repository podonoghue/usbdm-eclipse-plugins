package tests.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
   
   static void regexTest(String text, String regex, String replacement) {
      System.err.println("==========================================");
      System.err.println("text        = '" + text + "'");
      System.err.println("regex       = '" + regex + "'");
      System.err.println("replacement = '" + replacement + "'");
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(text);
      String result = "Failed match";
      if (m.matches()) {
         result = text;
         if (replacement != null) {
            result = m.replaceAll(replacement);
         }
      }
      System.err.println("result     => '" + result + "'");
   }
   
   static void doWrapTests() {
      
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
   
   static class RegexTest {
      final String regex;
      final String text;
      final String replacement;
      RegexTest(String text, String regex, String replacement) {
         this.text        = text;
         this.regex       = regex;
         this.replacement = replacement;
      }
   }
   
   static void doRegexTests() {
      RegexTest regexTests[] = {
            new RegexTest("SPI0_PCS3|TestSpi|PTC4|All in|Spi3In",   "^SPI0_(PCS\\d)\\|(.*?)\\|(.*?)\\|(.*?)$",   "$1 (also known as $2) is mapped to $3" ),
            new RegexTest("TSI0_CH0|Touch 1|xx|Touch 1 (x)|Touchy", "^TSI0_(CH\\d)\\|(.*?)\\|(.*?)\\|(.*?)$",    "$1" ),
            new RegexTest("TSI0_CH0|Touch 1|xx|Touch 1 (x)|Touchy", "^(?:.*?\\|){4}+(.*)$", "$1"),
            
            
      };
      for (RegexTest test : regexTests) {
         regexTest(test.text, test.regex, test.replacement);
      }
   }
   
   public static void main(String[] args) {
      doRegexTests();
   }
}
