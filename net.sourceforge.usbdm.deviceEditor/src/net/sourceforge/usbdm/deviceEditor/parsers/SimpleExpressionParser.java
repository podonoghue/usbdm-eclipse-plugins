package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Recursive descent expression parser
 *
 * number      : digit+[.digit*]
 * value       : number | identifier
 * factor      : [['+'|'-'|'~'|'!'] factor] | ['(' expr ')' | value]
 * term        : factor [ ['*'|'/'|'%'] factor]*
 * sum         : term [['+'|'-'] term]*
 * shift       : sum [['<<'|'>>] sum]*
 * compare     : shift [['<'|'<='|'>']'>='] shift]
 * equal       : equal [['!='|'=='] equal]
 * bitAnd      : compare ['&' compare]*
 * xor         : bitAnd ['^' bitAnd]*
 * bitOr       : xor ['^' xor]*
 * logicalAnd  : bitOr ['&&' bitOr]*
 * logicalOr   : logicalAnd ['^' logicalAnd]*
 * ternary     : logicalOr ['?' expr ':' expr]
 * expr        : ternary
 * 
 * @note It may be necessary to enclose variable names in braces as they may contain '/' e.g. (/xxx/yyy)
 */
public class SimpleExpressionParser {
   
   /**
    * Controls operation of parser
    * 
    *  <li>CollectIdentifiers        => Only parse expression to collect identifiers
    *  <li>CheckIdentifierExistance  => Treat identifiers as true/false => present/absent (must be boolean expression)
    *  <li>EvaluateFully             => Fully evaluate expression
    */
   public enum Mode {
      CollectIdentifiers,        ///< Only parse expression to collect identifiers
      CheckIdentifierExistance,  ///< Treat identifiers as true/false => present/absent (must be boolean expression)
      EvaluateFully,             ///< Fully evaluate expression
   }

   /** Index into current expression being parsed */
   private int    fIndex;
   
   /** Current expression being evaluated */
   private String fExpression;

   /** Provider for variables */
   private VariableProvider fProvider;
   
   private boolean fClockDependent = false;
   
   ArrayList<String> fCollectedIdentifiers = null;

   // Operating mode
   private Mode fMode = Mode.EvaluateFully;
   
   private Character getNextCh() {
      fIndex++;
      if (fIndex>=fExpression.length()) {
         return null;
      }
      return fExpression.charAt(fIndex);
   }
   
   private Character skipSpace() {
      while ((fIndex<fExpression.length()) && Character.isWhitespace(fExpression.charAt(fIndex))) {
         fIndex++;
      }
      if (fIndex>=fExpression.length()) {
         return null;
      }
      return fExpression.charAt(fIndex);
   }
   
   private Character peek() {
      if ((fIndex+1)>=fExpression.length()) {
         return null;
      }
      return fExpression.charAt(fIndex+1);
   }

   /**
    * Accepts identifier : alpha [alpha|digit|'/'|'['|']']+
    * 
    * @return identifier value or null
    * 
    * @throws Exception
    */
   private Object getIdentifier() throws Exception {
      StringBuilder sb = new StringBuilder();
      
      Character ch = skipSpace();

      if (ch == null) {
         return null;
      }
      while(fIndex<fExpression.length()) {
         ch = fExpression.charAt(fIndex);
         if (("/[]0123456789".indexOf(ch) >= 0) || Character.isJavaIdentifierPart(ch)) {
            sb.append(ch);
            fIndex++;
            continue;
         }
         break;
      }
      if (sb.length() == 0) {
         return null;
      }
      String key = sb.toString();
      if ("true".equalsIgnoreCase(key)) {
         return true;
      }
      if ("false".equalsIgnoreCase(key)) {
         return false;
      }
      key = makeClockSpecificVarKey(key);
      
      if (!fCollectedIdentifiers.contains(key)) {
         fCollectedIdentifiers.add(key);
      }
      
      key = fProvider.makeKey(key);

      Variable var = fProvider.safeGetVariable(key);

      switch(fMode) {
      case CheckIdentifierExistance:
         return var != null;
      default:
      case CollectIdentifiers:
      case EvaluateFully:
         if (var == null) {
            throw new Exception("Failed to find variable '" + key + "'");
         }
         if (var instanceof BooleanVariable) {
            return Boolean.parseBoolean(var.getNativeValue().toString());
         }
         if (var instanceof LongVariable) {
            return Long.parseLong(var.getNativeValue().toString());
         }
         if (var instanceof DoubleVariable) {
            return Double.parseDouble(var.getNativeValue().toString());
         }
         if (var instanceof ChoiceVariable) {
            return var.getValueAsLong();
         }
         // Default to treating as string
         return var.getValueAsString();
      }
   }
   
   /**
    * Accepts number : digit+[.digit+]
    * 
    * @return number value or null
    *
    * @throws Exception
    */
   private Object getNumber() throws Exception {
      Character ch = skipSpace();

      if ((ch == null)||!Character.isDigit(ch)) {
         return null;
      }
      StringBuilder sb = new StringBuilder();

    // Assume number
      boolean isDouble = false;
      while(fIndex<fExpression.length()) {
         ch = fExpression.charAt(fIndex);
         if (!Character.isDigit(ch) && (ch != '.') && (ch != 'x') && (ch != 'b')) {
            break;
         }
         if (ch == '.') {
            isDouble = true;
         }
         sb.append(ch);
         fIndex++;
      };
      String val = sb.toString();
      if (isDouble) {
         return Double.parseDouble(val);
      }
      if (val.startsWith("0b") || val.startsWith("0B")) {
         return Long.parseLong(val.substring(2), 2);
      }
      return Long.decode(sb.toString());
   }
   
   /**
    * Accepts number : digit+[.digit+]
    * 
    * @return number value or null
    *
    * @throws Exception
    */
   private Object getString() throws Exception {
      Character ch = skipSpace();

      if ((ch == null) || (ch != '"')) {
         return null;
      }
      fIndex++;
      StringBuilder sb = new StringBuilder();
      
      // Collect string
      boolean foundTerminator = false;
      while(fIndex<fExpression.length()) {
         ch = fExpression.charAt(fIndex);
         if (ch == '"') {
            foundTerminator = true;
            break;
         }
         sb.append(ch);
         fIndex++;
      };
      if (!foundTerminator) {
         throw new Exception("Unterminated string");
      }
      fIndex++;
      return sb.toString();
   }
   
   /**
    * Accepts value : number | identifier
    * 
    * @return value of number or identifier
    *
    * @throws Exception
    */
   private Object getValue() throws Exception {

      Object result = getNumber();
      if (result != null) {
         return result;
      }
      result = getIdentifier();
      if (result != null) {
         return result;
      }
      result = getString();
      if (result != null) {
         return result;
      }
      throw new Exception("Number or Identifier expected");
   }

   /**
    * Accepts factor : [['+'|'-'|'~'|'!'] factor] | ['(' expr ')' | value]
    * 
    * @return value of factor
    *
    * @throws Exception
    */
   private Object evaluateFactor() throws Exception {

      Object result;

      Character ch = skipSpace();

      if (ch == null) {
         throw new Exception("Unexpected end of expression");
      }
      if ("+-~!".indexOf(ch)>0) {
         fIndex++;
         result = evaluateFactor();
         switch (ch) {
            case '+' :
               if (result instanceof Double) {
                  return result;
               } else if (result instanceof Long) {
                  return result;
               }
               break;
            case '-' :
               if (result instanceof Double) {
                  return -(Double)result;
               } else if (result instanceof Long) {
                  return -(Long)result;
               }
               break;
            case '!' :
               if (result instanceof Boolean) {
                  return !(Boolean)result;
               }
               break;
            case '~' :
               if (result instanceof Long) {
                  return ~(Long)result;
               }
               break;
         }
         throw new Exception("Unexpected data type for operand");
      }

      if (ch == '(') {
         fIndex++;
         result = evaluateExpression();
         ch = skipSpace();
         if ((ch == null) || (ch != ')')) {
            throw new Exception("Expected ')'");
         }
         fIndex++;
      }
      else {
         result = getValue();
      }
      return result;
   }
   
   /**
    * Accepts term : factor [ ['*'|'/'|'%'] factor]*
    * 
    * @return value of term
    *
    * @throws Exception
    */
   private Object evaluateTerm() throws Exception {
      
      Object leftOperand = evaluateFactor();
      
      do {
         Character ch = skipSpace();
         if (ch == null) {
            return leftOperand;
         }
         boolean failed = false;
         if (ch == '*') {
            fIndex++;
            Object rightOperand = evaluateFactor();

            if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) {
               leftOperand = (Double)leftOperand * (Double)rightOperand;
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
               leftOperand = (Long)leftOperand * (Long)rightOperand;
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
               leftOperand = (Long)leftOperand * (Double)rightOperand;
            } else if ((leftOperand instanceof Double) && (rightOperand instanceof Long)) {
               leftOperand = (Double)leftOperand * (Long)rightOperand;
            } else {
               failed = true;
            }
         }
         else if (ch == '/') {
            fIndex++;
            Object rightOperand = evaluateFactor();

            if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) {
               leftOperand = (Double)leftOperand / (Double)rightOperand;
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
               // Don't promote Long but do a check for truncation in result
               leftOperand = (Long)leftOperand / (Long)rightOperand;
//             Double t = (double)(long)leftOperand / (double)(long)rightOperand;
//               if (Math.round(t) != (long)leftOperand) {
//                  System.err.println("Warning truncating division in '"+fExpression+"', "+Math.round(t)+" vs "+(long)leftOperand);
//               }
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
               leftOperand = (Long)leftOperand / (Double)rightOperand;
            } else if ((leftOperand instanceof Double) && (rightOperand instanceof Long)) {
               leftOperand = (Double)leftOperand / (Long)rightOperand;
            } else {
               failed = true;
            }
         }
         else if (ch == '%') {
            fIndex++;
            Object rightOperand = evaluateFactor();

            if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
               leftOperand = (Long)leftOperand % (Long)rightOperand;
            } else {
               failed = true;
            }
         }
         else {
            return leftOperand;
         }
         if (failed) {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   /**
    * Accepts sum : term [['+'|'-'] term]*
    * 
    * @return value of sum
    *
    * @throws Exception
    */
   private Object evaluateSum() throws Exception {
      
      Object leftOperand = evaluateTerm();

      do {
         Character ch = skipSpace();
         if (ch == null) {
            return leftOperand;
         }
         boolean failed = false;
         if (ch == '+') {
            fIndex++;
            Object rightOperand = evaluateTerm();

            if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) {
               leftOperand = (Double)leftOperand + (Double)rightOperand;
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
               leftOperand = (Long)leftOperand + (Long)rightOperand;
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
               leftOperand = (Long)leftOperand + (Double)rightOperand;
            } else if ((leftOperand instanceof Double) && (rightOperand instanceof Long)) {
               leftOperand = (Double)leftOperand + (Long)rightOperand;
            } else if ((leftOperand instanceof String) && (rightOperand instanceof String)) {
               leftOperand = (String)leftOperand + (String)rightOperand;
            } else {
               failed = true;
            }
         }
         else if (ch == '-') {
            fIndex++;
            Object rightOperand = evaluateTerm();

            if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) {
               leftOperand = (Double)leftOperand - (Double)rightOperand;
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
               leftOperand = (Long)leftOperand - (Long)rightOperand;
            } else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
               leftOperand = (Long)leftOperand - (Double)rightOperand;
            } else if ((leftOperand instanceof Double) && (rightOperand instanceof Long)) {
               leftOperand = (Double)leftOperand - (Long)rightOperand;
            } else {
               failed = true;
            }
         }
         else {
            return leftOperand;
         }
         if (failed) {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   /**
    * Accepts shift : sum [['<<'|'>>] sum]
    * 
    * @return value of shift
    *
    * @throws Exception
    */
   private Object evaluateShift() throws Exception {
      
      Object leftOperand = evaluateSum();

      do {
         boolean failed = false;
         Character ch = skipSpace();
         if ((ch == null) || ((ch != '<') && (ch != '>')) || (peek() == null) || (peek() != ch)) {
            return leftOperand;
         }
         // either '<<' or '>>'
         fIndex++;
         fIndex++;
         if (ch == '<') {
            Object rightOperand = evaluateSum();

            if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
               leftOperand = (Long)leftOperand << (Long)rightOperand;
            } else {
               failed = true;
            }
         }
         else if (ch == '>') {
            Object rightOperand = evaluateSum();

            if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
               leftOperand = (Long)leftOperand >> (Long)rightOperand;
            } else {
               failed = true;
            }
         }
         else {
            return leftOperand;
         }
         if (failed) {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   enum OperandType {longOp, doubleOp, stringOp, booleanOp};
   enum OpType      {lessThan, lessThanOrEqual, greaterThan, greaterThanOrEqual};
   
   /**
    * Accepts  compare : shift [['<'|'<='|'>']'>='] shift]
    * 
    * @return value of shift
    *
    * @throws Exception
    */
   private Object evaluateCompare() throws Exception {
      
      Object leftOperand = evaluateShift();
      OperandType operandType;
      do {
         Character ch = skipSpace();
         if ((ch == null) || ((ch != '<') && (ch != '>'))) {
            return leftOperand;
         }
         Character ch2 = getNextCh();
         if (ch2 == '=') {
            getNextCh();
         }
         Object rightOperand = evaluateShift();
         
         if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) {
            operandType = OperandType.doubleOp;
         }
         else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
            leftOperand  = (double) (long) leftOperand;
            operandType = OperandType.doubleOp;
         }
         else if ((leftOperand instanceof Double) && (rightOperand instanceof Long)) {
            rightOperand = (double) (long) rightOperand;
            operandType = OperandType.doubleOp;
         }
         else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
            operandType = OperandType.longOp;
         }
         else {
            throw new Exception("Incompatible operands");
         }
         OpType opType;
         if (ch == '<') {
            if ((ch2 != null) &&(ch2 == '=')) {
               // '<='
               opType = OpType.lessThanOrEqual;
            }
            else {
               // '<'
               opType = OpType.lessThan;
            }
         } else {
            if ((ch2 != null) &&(ch2 == '=')) {
               // '>='
               opType = OpType.greaterThanOrEqual;
            }
            else {
               // '>'
               opType = OpType.greaterThan;
            }
         }
         switch (opType) {
         case greaterThan:
            switch (operandType) {
            case doubleOp:
               leftOperand = (Double)leftOperand > (Double)rightOperand;
               continue;
            case longOp:
               leftOperand = (Long)leftOperand > (Long)rightOperand;
               continue;
            default:
               break;
            }
            break;
         case greaterThanOrEqual:
            switch (operandType) {
            case doubleOp:
               leftOperand = (Double)leftOperand >= (Double)rightOperand;
               continue;
            case longOp:
               leftOperand = (Long)leftOperand >= (Long)rightOperand;
               continue;
            default:
               break;
            }
            break;
         case lessThan:
            switch (operandType) {
            case doubleOp:
               leftOperand = (Double)leftOperand < (Double)rightOperand;
               continue;
            case longOp:
               leftOperand = (Long)leftOperand < (Long)rightOperand;
               continue;
            default:
               break;
            }
            break;
         case lessThanOrEqual:
            switch (operandType) {
            case doubleOp:
               leftOperand = (Double)leftOperand <= (Double)rightOperand;
               continue;
            case longOp:
               leftOperand = (Long)leftOperand <= (Long)rightOperand;
               continue;
            default:
               break;
            }
            throw new Exception("Impossible situation");
         }
         
      } while (true);
   }
   
   /**
    * Accepts equality : shift [['!='|'=='] shift]*
    * 
    * @return value of comparison
    *
    * @throws Exception
    */
   private Object evaluateEquality() throws Exception {
      
      Object leftOperand = evaluateCompare();

      do {
         Character ch = skipSpace();
         if ((ch == null) || ((ch != '!') && (ch != '=')) || (peek() == null) || (peek() != '=')) {
            return leftOperand;
         }
         fIndex++;
         fIndex++;
         Object rightOperand = evaluateCompare();
         if (!((leftOperand instanceof Double) && (rightOperand instanceof Long)) &&
             !((leftOperand instanceof Long) && (rightOperand instanceof Double)) &&
             (leftOperand.getClass() != rightOperand.getClass())) {
            throw new Exception("Incompatible operands");
         }
         if ((leftOperand instanceof Double) && (rightOperand instanceof Long)) {
            rightOperand = (double) (long) rightOperand;
         }
         if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
            leftOperand  = (double) (long) leftOperand;
         }
         if (ch == '=') {
            leftOperand = leftOperand.equals(rightOperand);
         } else {
            leftOperand = leftOperand != rightOperand;
         }
      } while (true);
   }
   
   /**
    * Accepts bitAnd : shift ['&' shift]*
    * 
    * @return value of bitOr
    *
    * @throws Exception
    */
   private Object evaluateBitAnd() throws Exception {
      
      Object leftOperand = evaluateEquality();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '&')||((peek() != null)&&(peek() == '&'))) {
            return leftOperand;
         }
         fIndex++;
         Object rightOperand = evaluateEquality();

         if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
            leftOperand = (Long)leftOperand & (Long)rightOperand;
         }
         else {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   /**
    * Accepts xor : bitAnd ['^' bitAnd]*
    * 
    * @return value of shift
    *
    * @throws Exception
    */
   private Object evaluateXor() throws Exception {
      
      Object leftOperand = evaluateBitAnd();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '^')) {
            return leftOperand;
         }
         fIndex++;
         Object rightOperand = evaluateBitAnd();

         if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
            leftOperand = (Long)leftOperand ^ (Long)rightOperand;
         }
         else {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   /**
    * Accepts bitOr : xor ['|' xor]*
    * 
    * @return value of bitOr
    *
    * @throws Exception
    */
   private Object evaluateBitOr() throws Exception {
      
      Object leftOperand = evaluateXor();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '|')||((peek() != null) && (peek() == '|'))) {
            return leftOperand;
         }
         fIndex++;
         Object rightOperand = evaluateXor();

         if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
            leftOperand = (Long)leftOperand | (Long)rightOperand;
         }
         else {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   /**
    * Accepts bitOr ['&&' bitOr]*
    * 
    * @return value of bitOr
    *
    * @throws Exception
    */
   private Object evaluateLogicalAnd() throws Exception {
      
      Object leftOperand = evaluateBitOr();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '&')) {
            return leftOperand;
         }
         fIndex++;
         ch = skipSpace();
         if ((ch == null) || (ch != '&')) {
            throw new Exception("Expected '&'");
         }
         fIndex++;
         Object rightOperand = evaluateBitOr();

         if ((leftOperand instanceof Boolean) && (rightOperand instanceof Boolean)) {
            leftOperand = (Boolean)leftOperand && (Boolean)rightOperand;
         }
         else {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   /**
    * Accepts logicalOr : logicalAnd ['||' logicalAnd]*
    * 
    * @return value of bitOr
    *
    * @throws Exception
    */
   private Object evaluatelogicalOr() throws Exception {
      
      Object leftOperand = evaluateLogicalAnd();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '|')) {
            return leftOperand;
         }
         fIndex++;
         ch = skipSpace();
         if ((ch == null) || (ch != '|')) {
            throw new Exception("Expected '|'");
         }
         fIndex++;
         Object rightOperand = evaluateLogicalAnd();

         if ((leftOperand instanceof Boolean) && (rightOperand instanceof Boolean)) {
            leftOperand = (Boolean)leftOperand || (Boolean)rightOperand;
         }
         else {
            throw new Exception("Unexpected data type for operand");
         }
      } while (true);
   }
   
   /**
    * Accepts ternary : shift ['?' expr ':' expr]
    * 
    * @return value of ternary
    *
    * @throws Exception
    */
   private Object evaluateTernary() throws Exception {
      
      Object control = evaluatelogicalOr();

      Character ch = skipSpace();
      if ((ch == null) || (ch != '?')) {
         return control;
      }
      if (!(control instanceof Boolean)) {
         throw new Exception("Unexpected data type for operand");
      }
      fIndex++;
      Object trueExp = evaluateExpression();
      ch = skipSpace();
      if (ch != ':') {
         throw new Exception("':' expected");
      }
      fIndex++;
      Object falseExp = evaluateExpression();
      if (falseExp.getClass() != trueExp.getClass()) {
         throw new Exception("Incompatible operands");
      }
      if ((Boolean) control) {
         return trueExp;
      }
      return falseExp;
   }
   
   /**
    * Accepts expr : ternary
    * 
    * @return value of expr
    *
    * @throws Exception
    */
   private Object evaluateExpression() throws Exception {
      return evaluateTernary();
   }

   /**
    * Check if variable key depends on current clock selection
    * 
    * @param varKey Key for variable
    * 
    * @return true if dependent, false otherwise
    */
   static boolean isClockDependent(String varKey) {
      return varKey.endsWith("[]");
   }
   
   /**
    * Converts a variable key into a clock specific key e.g. varkey[] => varKey[activeClockSelection]
    * Also converts the key to absolute using the provider.
    * 
    * @param varKey     Key to convert
    * 
    * @return  Converted key or original if not indexed key
    */
   String makeClockSpecificVarKey(String varKey) {
      if (isClockDependent(varKey)) {
         fClockDependent = true;
         varKey = varKey.substring(0,varKey.length()-2)+"["+fProvider.getDeviceInfo().getActiveClockSelection()+"]";
      }
      return varKey;
   }

   /**
    * Constructor
    * Create parser
    * 
    * @param varProvider    Variable provider
    * @param variableNames  List of variable names for substitution e.g. var1Name, var2Name
    * @param mode Controls  Controls operation of parser
    * 
    *  <li>CollectIdentifiers        => Only parse expression to collect identifiers
    *  <li>CheckIdentifierExistance  => Treat identifiers as true/false => present/absent (must be boolean expression)
    *  <li>EvaluateFully             => Fully evaluate expression
    */
   public SimpleExpressionParser(VariableProvider varProvider, Mode mode) {
      fMode        = mode;
      fProvider    = varProvider;
      fExpression  = null;
   }

   /**
    * Evaluate expression supplied
    * 
    * @param expression Expression to evaluate
    * 
    * @return  Expression result
    * 
    * @throws Exception
    * 
    * @note A null expression is viewed as 'true'
    */
   public Object evaluate(String expression) throws Exception {
      if (expression == null) {
         return true;
      }
      fCollectedIdentifiers = new ArrayList<String>();
      fExpression = expression;
      fIndex = 0;
      try {
         Object result = evaluateExpression();
         if ((fIndex) != fExpression.length()) {
            throw new Exception("Unexpected characters at end of expression");
         }
         return result;
      } catch (Exception e) {
         String diagnostic =
               String.format("\nInput     '%s" + "'" +
                             "\n           %"+(fIndex+1)+"s", fExpression, "^")+
                              "....." + e.getMessage()+"\n";
         throw new Exception(diagnostic, e);
      }
   }

   /**
    * Immediately evaluate expression
    * 
    * @param expression          Expression to evaluate
    * @param variableProvider    Provider for variables used
    * @param mode                Mode for evaluation
    * 
    * @return  Result of evaluation
    * 
    * @throws Exception On lots of situations
    * 
    * @note A null expression is viewed as 'true'
    */
   public static Object evaluate(String expression, VariableProvider variableProvider, Mode mode) throws Exception {
      SimpleExpressionParser parser = new SimpleExpressionParser(variableProvider, mode);
      return parser.evaluate(expression);
   }
   
   public boolean isClockDependent() {
      return fClockDependent;
   }

   /**
    * Get list of identifiers collected while parsing expression
    * 
    * @return List
    */
   public ArrayList<String> getCollectedIdentifiers() {
      return fCollectedIdentifiers;
   }
   
}
