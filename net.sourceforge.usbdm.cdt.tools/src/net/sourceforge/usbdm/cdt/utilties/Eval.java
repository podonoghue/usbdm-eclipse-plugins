package net.sourceforge.usbdm.cdt.utilties;

public class Eval {
   private final String fExpression;
   private       int   fIndex;
   private       int   fLength;
   
   private Eval(String expression) {
      fExpression = expression.trim();
      fIndex      = 0;
      fLength     = fExpression.length();
   }

   private void skipSpace() {
      while ((fIndex<fLength) && Character.isSpaceChar(fExpression.charAt(fIndex))) {
         fIndex++;
      }
   }
   
   private int parseInt() throws Exception {
      int value     = 0;
      boolean valid = false;
      while ((fIndex<fLength) && Character.isDigit(fExpression.charAt(fIndex))) {
         valid = true;
         value = value*10 + (fExpression.charAt(fIndex)-'0');
         fIndex++;
      }
      if (!valid) {
         throw new Exception("Invalid expression '"+ fExpression + "'");
      }
      return value;
   }
   
   int eval() throws Exception {
      skipSpace();
      int result = parseInt();
      skipSpace();
      while (fIndex<fLength) {
         char operator = fExpression.charAt(fIndex);
         fIndex++;
         skipSpace();
         int op2 = parseInt();
         skipSpace();
         switch(operator) {
         case '/' : result = result / op2;
         case '*' : result = result * op2;
         }
      }
      return result;
   }
   
   /**
    * Evaluate a simple expression
    * 
    * @param expression
    * @return
    * @throws Exception
    */
   public static int eval(String expression) throws Exception {
      Eval evaluator = new Eval(expression);
      return evaluator.eval();
   }
}
