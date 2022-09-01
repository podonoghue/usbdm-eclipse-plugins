package net.sourceforge.usbdm.deviceEditor.parsers;

public class SimpleExpressionParser {
   
   /** Index into current expression being parsed */
   private int    index;
   
   /** Current expression being evaluated */
   private String fExpression;
   
   private Character skipSpace() {
      while ((index<fExpression.length()) && Character.isWhitespace(fExpression.charAt(index))) {
         index++;
      }
      if (index>=fExpression.length()) {
         return null;
      }
      return fExpression.charAt(index);
   }
   
   private Double getNumber() throws Exception {
      
      skipSpace();
      
      StringBuilder sb = new StringBuilder();

      while(index<fExpression.length()) {
         char ch = fExpression.charAt(index);
         if (!Character.isDigit(ch) && (ch != '.')) {
            break;
         }
         sb.append(ch);
         index++;
      };
      if (sb.length() == 0) {
         throw new Exception("Digit expected at '"+fExpression.substring(index)+"' in '"+fExpression+"'");
      }
      return Double.parseDouble(sb.toString());
   }

   private Double evaluateFactor() throws Exception {
      
      Double result;

      Character ch = skipSpace();
      
      if (ch == null) {
         throw new Exception("Unexpected end of expression'"+fExpression+"'");
      }
      if (ch == '(') {
         index++;
         result = evaluateSum();
         if (skipSpace() != ')') {
            throw new Exception("Expected ')' in '"+fExpression.substring(index)+"'");
         }
         index++;
      }
      else {
         result = getNumber();
      }
      return result;
   }
   
   private Double evaluateTerm() throws Exception {
      
      Double result = evaluateFactor();
      
      Character ch = skipSpace();
      
      do {
         ch = skipSpace();
         if (ch == null) {
            break;
         }
         if (ch == '*') {
            index++;
            result = result * evaluateFactor();
         }
         else if (ch == '/') {
            index++;
            result = result / evaluateFactor();
         }
         else {
            break;
         }
      } while (true);
      return result;
   }
   
   private Double evaluateSum() throws Exception {
      
      Double result;

      Character ch = skipSpace();
      if (ch == null) {
         throw new Exception("Unexpected end of expression'"+fExpression+"'");
      }
      
      if ("+-".indexOf(ch)>0) {
         index++;
         result = evaluateTerm();
         if (ch == '-') {
            result = -result;
         }
      }
      else {
         result = evaluateTerm();
      }
      
      do {
         ch = skipSpace();
         if (ch == null) {
            break;
         }
         if (ch == '+') {
            index++;
            result = result + evaluateTerm();
         }
         else if (ch == '-') {
            index++;
            result = result - evaluateTerm();
         }
         else {
            break;
         }
      } while (true);
      return result;
   }
   
   private Double evaluateExpression() throws Exception {
      
      Double result = evaluateSum();

      do {
         Character ch = skipSpace();
         if (ch == null) {
            break;
         }
         if (ch == '<') {
            index++;
            if (ch != '<') {
               throw new Exception("Expected '<' in '"+fExpression.substring(index)+"'");
            }
            index++;
            result = (double) (result.longValue() << evaluateSum().longValue());
         }
         else if (ch == '>') {
            index++;
            if (ch != '>') {
               throw new Exception("Expected '>' in '"+fExpression.substring(index)+"'");
            }
            index++;
            result = (double) (result.longValue() >> evaluateSum().longValue());
         }
         else {
            break;
         }
      } while (true);
      return result;
   }
   
   /**
    * Constructor
    * 
    * @param expression Expression to evaluate
    */
   public SimpleExpressionParser(String expression, String value) {
      
      fExpression = expression.replace("%%", "("+value+")");
   }

   /**
    * Evaluate expression supplied in constructor
    * 
    * @return
    * 
    * @throws Exception
    */
   public Double evaluate() throws Exception {
      return evaluateExpression();
   }
   
}
