package tests.internal;

import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser;

public class TestExpression {

   public static void main(String[] args) {
      
      String tests[] = {
            "0b101",
            "0x23",
            "\"hello\"+\"There\"",
            "false||false",
            "true||false",
            "false||true",
            "true||true",
            "false&&false",
            "true&&false",
            "false&&true",
            "true&&true",
            "!false",
            "!true",
            "false?1:2",
            "true?1:2",
            "-(10+1)<<1",
            "12*-12",
            "12*-12.5",
            "12.5*-12",
            "12.5*-12.5",
            "120%9",
            "120/9",
            "120.5/9",
            "120/9.5",
            "120.5/9.5",
            "0x23",
            "0b101",
            "~0x23",
            "~0b101&0b111",
            "!!!false",
            "!!!true",
            "true?1.0:1.5",
            "false?false?1:2:5",
            "false?(false?1:2):5",
            "false?true?1:2:5",
            "false?(true?1:2):5",
            "true?false?1:2:5",
            "true?(false?1:2):5",
            "true?true?1:2:5",
            "true?(true?1:2):5",
            "83==23",
            "83==23.5",
            "83.5==23",
            "83.5==23.5",
            "23==23",
            "23==23.0",
            "23.0==23",
            "23.5==23.5",
      };
      for (String expression:tests) {
         try {
            SimpleExpressionParser parser = new SimpleExpressionParser(null, "", expression);
            Object result = parser.evaluate();
            System.err.println("Evaluate '"+expression+"' => "+ result + " (" + result.getClass() + ")");
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }
      }
   }

}
