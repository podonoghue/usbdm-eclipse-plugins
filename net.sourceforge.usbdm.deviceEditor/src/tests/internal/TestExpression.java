package tests.internal;

import net.sourceforge.usbdm.deviceEditor.parsers.SimpleExpressionParser;

public class TestExpression {

   public static void main(String[] args) {
      
      SimpleExpressionParser parser = new SimpleExpressionParser("(%%+1)<<1", "256");
      try {
         System.err.println("Result = "+ parser.evaluate());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
