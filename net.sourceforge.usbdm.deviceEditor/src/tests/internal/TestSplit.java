package tests.internal;

import java.util.ArrayList;

public class TestSplit {

   static String[] splitValues(String value, char delimiter) throws Exception {
      
      enum TokenState { Normal, Quoted, DoubleQuoted, LeadingSpaces, TrailingSpaces };

      ArrayList<String> valueList = new ArrayList<String>();

      StringBuilder sb          = new StringBuilder();
      StringBuilder spaceBuffer = new StringBuilder();

      TokenState tokenState = TokenState.LeadingSpaces;

      for (int index=0; index<value.length(); index++) {
         char ch = value.charAt(index);
         
//         System.err.println("ch           = '" + ch + "'");
//         System.err.println("sb           = '" + sb.toString() + "'");
//         System.err.println("spaceBuffer  = '" + spaceBuffer.toString() + "'");
//         System.err.println("tokenState   = " + tokenState.toString());
//         System.err.println("valueList    = " + Arrays.toString(valueList.toArray(new String[valueList.size()])));
         
         if (tokenState == TokenState.LeadingSpaces) {
            if (Character.isWhitespace(ch)) {
               // Discard leading spaces
               continue;
            }
            tokenState = TokenState.Normal;
            // Fall through
         }
         
         if (tokenState == TokenState.TrailingSpaces) {
            if (Character.isSpaceChar(ch)) {
               // Accumulate trailing spaces
               spaceBuffer.append(ch);
               continue;
            }
            tokenState = TokenState.Normal;
            if (ch != delimiter) {
               // Put back the trailing spaces unless end of token
               sb.append(spaceBuffer.toString());
            }
            spaceBuffer = new StringBuilder();
            // Fall through
         }
         
         switch(tokenState) {
         
         case LeadingSpaces:
         case TrailingSpaces:
            // Can't happen
            break;
            
         case Normal:
            if (ch == '\'') {
               tokenState = TokenState.Quoted;
               sb.append(ch);
               continue;
            }
            if (ch == '"') {
               tokenState = TokenState.DoubleQuoted;
               sb.append(ch);
               continue;
            }
            if (Character.isSpaceChar(ch)) {
               // Accumulate trailing spaces
               spaceBuffer.append(ch);
               tokenState = TokenState.TrailingSpaces;
               continue;
            }
            if (ch == delimiter) {
               // Process text so far
               String t = sb.toString();
               valueList.add(t);
               sb = new StringBuilder();
               tokenState = TokenState.LeadingSpaces;
               continue;
            }
            sb.append(ch);
            break;
         case Quoted:
            if (ch == '\'') {
               tokenState = TokenState.Normal;
               sb.append(ch);
               continue;
            }
            sb.append(ch);
            break;
         case DoubleQuoted:
            if (ch == '"') {
               tokenState = TokenState.Normal;
               sb.append(ch);
               continue;
            }
            sb.append(ch);
            break;
         }
      }
      if ((tokenState == TokenState.Quoted)||(tokenState == TokenState.DoubleQuoted)) {
         throw new Exception("Unexpected state at end of processing text '"+tokenState.toString()+"'");
      }
      else {
         String t = sb.toString();
         valueList.add(t);
      }
      return valueList.toArray(new String[valueList.size()]);
   }

   static void runKeyTests() throws Exception {
      String keyTests[] = {
            "   middle  : Right:Justified:Left   :\" colon : included \": \"     \" ::",
      };
      for (String test:keyTests) {
         System.out.println("===============================");
         System.out.println("\""+test + "\" => ");
         String splits[] = splitValues(test, ':');
         for (String split:splits) {
            System.out.println("==>\""+split + "\"");
         }
      }
   }
   
   static void runValueTests() throws Exception {
      String valueTests[] = {
            "   middle  : Right:Justified:Left   :\" colon : included \": \"     \" ::\n"+
            "        middle  : Right:Justified:Left   :\" colon : included \": \"     \" ::   ",
            
            "uart   : Uart   : UART;\n"
            + "                        lpuart : Lpuart : LPUART",
            "uart   : Uart   : UART;\n"
            + "                        lpuart : Lpuart : LPUART",
      };
      for (String test:valueTests) {
         System.out.println("===============================");
         System.out.println("\""+test + "\" => ");
         String splits[] = splitValues(test, ';');
         for (String split:splits) {
            System.out.println("==>\""+split + "\"");
            String splits2[] = splitValues(split, ':');
            for (String split2:splits2) {
               System.out.println("  ==>\""+split2 + "\"");
               
            }
            
         }
      }
   }
   public static void main(String[] args) throws Exception {
//      runKeyTests();
      runValueTests();
   }

}
