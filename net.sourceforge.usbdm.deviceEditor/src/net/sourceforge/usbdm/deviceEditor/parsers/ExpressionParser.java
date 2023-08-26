package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.BooleanNode;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.ExpressionNode;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

/**
 * Recursive descent expression parser <br>
 *
 * <li>identifier  : id_char+[.id_char*]["["expr"]"]
 * <li>number      : digit+[.digit*]
 * <li>value       : number | identifier
 * <li>factor      : [['+'|'-'|'~'|'!'] factor] | ['(' expr ')' | value]
 * <li>term        : factor [ ['*'|'/'|'%'] factor]*
 * <li>sum         : term [['+'|'-'] term]*
 * <li>shift       : sum [['<<'|'>>] sum]*
 * <li>compare     : shift [['<'|'<='|'>']'>='] shift]
 * <li>equal       : equal [['!='|'=='] equal]
 * <li>bitAnd      : compare ['&' compare]*
 * <li>xor         : bitAnd ['^' bitAnd]*
 * <li>bitOr       : xor ['^' xor]*
 * <li>logicalAnd  : bitOr ['&&' bitOr]*
 * <li>logicalOr   : logicalAnd ['^' logicalAnd]*
 * <li>ternary     : logicalOr ['?' expr ':' expr]
 * <li>expr        : ternary
 * 
 * @note It may be necessary to enclose variable names in braces as they may contain '/' e.g. (/xxx/yyy)
 */
public class ExpressionParser {
   
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
      Construct,                 ///< Construct expression tree and add listeners
   }

   /** Index into current expression being parsed */
   private int    fIndex;
   
   /** Current expression being evaluated */
   private String fExpressionString;

   /** Provider for variables */
   private VariableProvider fProvider;
   
   /** Non-constant variable found in expression */
   private ArrayList<Variable> fCollectedVariables = null;

   // Operating mode
   private Mode fMode = Mode.EvaluateFully;

   // Listener for changes in variables
   private Expression fListener;

   /**
    * Get next character<br>
    * This first advances the character pointer.
    * 
    * @return Character found or null if at end of buffer
    */
   private Character getNextCh() {
      fIndex++;
      if (fIndex>=fExpressionString.length()) {
         return null;
      }
      return fExpressionString.charAt(fIndex);
   }
   
   /**
    * Get next non-whitespace character<br>
    * This first advances the character pointer.
    * 
    * @return Character found or null if at end of buffer
    */
   private Character getNextNonWhitespaceCh() {
      fIndex++;
      return skipSpace();
   }
   
   /**
    * Skip whitespace and return first non-whitespace<br>
    * This does <b>not</b> advance the character point first.
    * 
    * @return Character found or null if at end of expression
    */
   private Character skipSpace() {
      while ((fIndex<fExpressionString.length()) && Character.isWhitespace(fExpressionString.charAt(fIndex))) {
         fIndex++;
      }
      if (fIndex>=fExpressionString.length()) {
         return null;
      }
      return fExpressionString.charAt(fIndex);
   }
   
   /**
    * Find end of variable index i.e. closing ']' <br>
    * Does handle nested indices<br>
    * Doesn't handle quoting.
    * 
    * @return index of closing ']'
    * 
    * @throws Exception
    */
   int findEndOfIndex() throws Exception {
      int nesting = 0;
      Character ch;
      
      do {
         ch = getNextCh();
         if (ch == null) {
            throw new Exception("']' expected");
         }
         if (ch == '[') {
            nesting++;
         }
         if (ch == ']') {
            if (nesting > 0) {
               nesting--;
               // Make sure we don't exit the loop
               ch = ' ';
            }
         }

      } while ((nesting>0) || (']' != ch));
      
      return fIndex;
   }
   
   /**
    * Peek at next character without advancing
    * 
    * @return Next character or null if at end of expression
    */
   private Character peek() {
      if ((fIndex+1)>=fExpressionString.length()) {
         return null;
      }
      return fExpressionString.charAt(fIndex+1);
   }

   /**
    * Accepts identifier : alpha [alpha|digit|'/'|'['|']']+
    * 
    * @return identifier value or null
    * 
    * @throws Exception
    */
   private ExpressionNode getIdentifier() throws Exception {
      StringBuilder sb = new StringBuilder();
      
      Character ch = skipSpace();

      if (ch == null) {
         return null;
      }
      boolean forceEvaluate = (fMode != Mode.CheckIdentifierExistance);
      if (ch == '@') {
         forceEvaluate = true;
         ch = getNextCh();
      }
      while(fIndex<fExpressionString.length()) {
         ch = fExpressionString.charAt(fIndex);
         if ((ch == '/') || Character.isJavaIdentifierPart(ch)) {
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
         return new Expression.BooleanNode(true);
      }
      if ("false".equalsIgnoreCase(key)) {
         return new Expression.BooleanNode(false);
      }
      if ("disabled".equalsIgnoreCase(key)) {
         return new Expression.DisabledValueNode();
      }
      // Check for index
      ch = skipSpace();
      Expression index = null;
      if ((ch != null) && (ch == '[')) {
         ch = getNextNonWhitespaceCh();
         if (ch ==']') {
            if (forceEvaluate) {
               // [] should only be used in existence checks
               System.err.println("Empty index used in evaluated variable'"+fExpressionString+"'");
            }
            index = new Expression("0", fProvider);
         }
         else {
            int startOfIndex = fIndex;
            int endOfIndex   = findEndOfIndex();
            
            // Parse sub-expression
            index = new Expression(fExpressionString.substring(startOfIndex, endOfIndex), fProvider, fMode);
            
            // Check type is correct for index
            Object ind = index.getValue();
            if (!(ind instanceof Long)) {
               throw new Exception("Invalid index type");
            }
         }
         ch = getNextNonWhitespaceCh();
      }
      
      // Check for modifier
      String modifier = null;
      ch = skipSpace();
      if ((ch != null) && (ch == '.')) {
         // Discard '.'
         ch = getNextCh();
         StringBuilder modifierSb = new StringBuilder();
         while ((ch != null) && Character.isJavaIdentifierPart(ch)) {
            modifierSb.append(ch);
            ch = getNextCh();
         }
         modifier = modifierSb.toString();
      }
      if (fProvider == null) {
         throw new Exception("Provider used but not provided");
      }
      key = fProvider.makeKey(key);

      Variable var;
      if (index != null) {
         var = fProvider.safeGetVariable(key+"["+index.getValueAsLong()+"]");
      }
      else {
         var = fProvider.safeGetVariable(key);
      }
      
      if ((var != null) && !var.isConstant()) {
         if (!fCollectedVariables.contains(var)) {
            fCollectedVariables.add(var);
         }
      }
      switch(fMode) {
      case CheckIdentifierExistance:
         if (!forceEvaluate) {
            return new BooleanNode(var != null);
         }
      default:
      case Construct:
         if (var == null) {
            throw new Exception("Failed to find variable '" + key + "'");
         }
         if (!var.isConstant()) {
            var.addListener(fListener);
         }
      case CollectIdentifiers:
      case EvaluateFully:
         if (var == null) {
            throw new Exception("Failed to find variable '" + key + "'");
         }
//         if (index != null) {
//            System.err.println("Found it" + index);
//         }
         return Expression.VariableNode.create(fListener, key, modifier, index);
      }
   }
   
   /**
    * Accepts number : digit+[.digit+]
    * 
    * @return number value or null
    *
    * @throws Exception
    */
   private ExpressionNode getNumber() throws Exception {
      Character ch = skipSpace();

      if ((ch == null)||!Character.isDigit(ch)) {
         return null;
      }
      StringBuilder sb = new StringBuilder();

    // Assume number
      boolean isDouble = false;
      while(fIndex<fExpressionString.length()) {
         ch = fExpressionString.charAt(fIndex);
         if (!Character.isDigit(ch) && !Character.isAlphabetic(ch) && (ch != '_') && (ch != '.')) {
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
         return new Expression.DoubleNode(EngineeringNotation.parse(val));
      }
      return new Expression.LongNode(EngineeringNotation.parseAsLong(val));
   }
   
   /**
    * Accepts number : " char+[.char+] "
    * 
    * @return number value or null
    *
    * @throws Exception
    */
   private ExpressionNode getString() throws Exception {
      Character ch = skipSpace();

      if ((ch == null) || (ch != '"')) {
         return null;
      }
      fIndex++;
      StringBuilder sb = new StringBuilder();
      
      // Collect string
      boolean foundTerminator = false;
      while(fIndex<fExpressionString.length()) {
         ch = fExpressionString.charAt(fIndex);
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
      return new Expression.StringNode(sb.toString());
   }
   
   /**
    * Accepts value : number | identifier
    * 
    * @return value of number or identifier
    *
    * @throws Exception
    */
   private ExpressionNode getValue() throws Exception {

      ExpressionNode result = getNumber();
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

   private boolean isBoolean(ExpressionNode target) {
      return target.fType == Expression.Type.Boolean;
   }
   
   private boolean isInteger(ExpressionNode target) {
      return target.fType == Expression.Type.Long;
   }
   
   private boolean isFloatOrInteger(ExpressionNode target) {
      return (target.fType == Expression.Type.Long)||(target.fType == Expression.Type.Double);
   }
   
//   private boolean isFloat(ExpressionNode target) {
//      return (target.fType == Expression.Type.Double);
//   }

   private boolean isString(ExpressionNode target) {
      return (target.fType == Expression.Type.String);
   }
   
   /**
    * Accepts factor : [['+'|'-'|'~'|'!'] factor] | ['(' expr ')' | value]
    * 
    * @return value of factor
    *
    * @throws Exception
    */
   private ExpressionNode parseFactor() throws Exception {

      ExpressionNode result;
      Character ch = skipSpace();

      if (ch == null) {
         throw new Exception("Unexpected end of expression");
      }
      if ("+-~!".indexOf(ch)>=0) {
         boolean okResult = false;
         
         fIndex++;
         result = parseFactor();
         switch (ch) {
            case '+' :
               okResult = isFloatOrInteger(result);
               break;
            case '-' :
               okResult = isFloatOrInteger(result);
               result = new Expression.MinusNode(result);
               break;
            case '!' :
               okResult = isBoolean(result);
               result = new Expression.NotNode(result);
               break;
            case '~' :
               okResult = isInteger(result);
               result = new Expression.ComplementNode(result);
               break;
         }
         if (!okResult) {
            throw new Exception("Unexpected data type for operand in Factor");
         }
      }
      else if (ch == '(') {
         fIndex++;
         result = parseSubExpression();
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
    * Promote Long node to Double
    * If not a Long node then left unchanged
    * 
    * @param node Node to promote
    * 
    * @return  Promoted node (may be unchanged)
    */
   private ExpressionNode promoteLongNode(ExpressionNode node) {
      if (node.fType != Expression.Type.Long) {
         return node;
      }
      return new Expression.LongToDoubleNode(node);
   }
   
   /**
    * Accepts term : factor [ ['*'|'/'|'%'] factor]*
    * 
    * @return value of term
    *
    * @throws Exception
    */
   private ExpressionNode parseTerm() throws Exception {

      ExpressionNode leftOperand = parseFactor();
      ExpressionNode rightOperand = null;
      do {
         Character ch = skipSpace();
         if (ch == null) {
            return leftOperand;
         }
         boolean failed = false;
         if (ch == '*') {
            fIndex++;
            failed = true;
            if (isFloatOrInteger(leftOperand)) {
               rightOperand = parseFactor();
               if (leftOperand.fType != rightOperand.fType) {
                  leftOperand  = promoteLongNode(leftOperand);
                  rightOperand = promoteLongNode(rightOperand);
               }
               if (leftOperand.fType == rightOperand.fType) {
                  leftOperand = new Expression.MultiplyNode(leftOperand, rightOperand);
                  failed = false;
               }
            }
         }
         else if (ch == '/') {
            fIndex++;
            failed = true;
            if (isFloatOrInteger(leftOperand)) {
               rightOperand = parseFactor();
               if (leftOperand.fType != rightOperand.fType) {
                  leftOperand  = promoteLongNode(leftOperand);
                  rightOperand = promoteLongNode(rightOperand);
               }
               if (leftOperand.fType == rightOperand.fType) {
                  leftOperand = new Expression.DivideNode(leftOperand, rightOperand);
                  failed = false;
               }
            }
         }
         else if (ch == '%') {
            fIndex++;
            failed = true;
            if (isInteger(leftOperand)) {
               rightOperand = parseFactor();
               if (isInteger(rightOperand)) {
                  leftOperand = new Expression.ModuloNode(leftOperand, rightOperand);
                  failed = false;
               }
            }
         }
         else {
            return leftOperand;
         }
         if (failed) {
            throw new Exception("Unexpected data type for operand in Term");
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
   private ExpressionNode parseSum() throws Exception {
      
      ExpressionNode leftOperand = parseTerm();

      do {
         Character ch = skipSpace();
         if (ch == null) {
            return leftOperand;
         }
         boolean failed = false;
         if (ch == '+') {
            fIndex++;
            failed = true;
            if (isFloatOrInteger(leftOperand)||isString(leftOperand)) {
               ExpressionNode rightOperand = parseTerm();
               if (leftOperand.fType != rightOperand.fType) {
                  leftOperand  = promoteLongNode(leftOperand);
                  rightOperand = promoteLongNode(rightOperand);
               }
               if (leftOperand.fType == rightOperand.fType) {
                  leftOperand = new Expression.AddNode(leftOperand, rightOperand);
                  failed = false;
               }
            }
         }
         else if (ch == '-') {
            fIndex++;
            failed = true;
            if (isFloatOrInteger(leftOperand)) {
               ExpressionNode rightOperand = parseTerm();
               if (leftOperand.fType != rightOperand.fType) {
                  leftOperand  = promoteLongNode(leftOperand);
                  rightOperand = promoteLongNode(rightOperand);
               }
               if (leftOperand.fType == rightOperand.fType) {
                  leftOperand = new Expression.SubtractNode(leftOperand, rightOperand);
                  failed = false;
               }
            }
         }
         else {
            return leftOperand;
         }
         if (failed) {
            throw new Exception("Unexpected data type for operand Sum");
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
   private ExpressionNode parseShift() throws Exception {
      
      ExpressionNode leftOperand = parseSum();

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
            ExpressionNode rightOperand = parseSum();

            if ((isInteger(leftOperand)) && (isInteger(rightOperand))) {
               leftOperand = new Expression.LeftShiftNode(leftOperand, rightOperand);
            } else {
               failed = true;
            }
         }
         else if (ch == '>') {
            ExpressionNode rightOperand = parseSum();

            if ((isInteger(leftOperand)) && (isInteger(rightOperand))) {
               leftOperand = new Expression.RightShiftNode(leftOperand, rightOperand);
            } else {
               failed = true;
            }
         }
         else {
            return leftOperand;
         }
         if (failed) {
            throw new Exception("Unexpected data type for operand in Shift");
         }
      } while (true);
   }
   
   /**
    * Accepts  compare : shift [['<'|'<='|'>']'>='] shift]
    * 
    * @return value of shift
    *
    * @throws Exception
    */
   private ExpressionNode parseCompare() throws Exception {
      
      enum OpType      {lessThan, lessThanOrEqual, greaterThan, greaterThanOrEqual};
      
      ExpressionNode leftOperand = parseShift();
      do {
         Character ch = skipSpace();
         if ((ch == null) || ((ch != '<') && (ch != '>'))) {
            return leftOperand;
         }
         Character ch2 = getNextCh();
         if (ch2 == '=') {
            getNextCh();
         }
         if (!isFloatOrInteger(leftOperand)) {
            throw new Exception("Expected float or integer in Comparison");
         }
         ExpressionNode rightOperand = parseShift();
         
         if (leftOperand.fType != rightOperand.fType) {
            leftOperand  = promoteLongNode(leftOperand);
            rightOperand = promoteLongNode(rightOperand);
         }
         if (leftOperand.fType != rightOperand.fType) {
            throw new Exception("Incompatible operands in Comparison");
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
            leftOperand = new Expression.GreaterThanNode(leftOperand, rightOperand);
            break;
         case greaterThanOrEqual:
            leftOperand = new Expression.GreaterThanOrEqualNode(leftOperand, rightOperand);
            break;
         case lessThan:
            leftOperand = new Expression.LessThanNode(leftOperand, rightOperand);
            break;
         case lessThanOrEqual:
            leftOperand = new Expression.LessThanOrEqualNode(leftOperand, rightOperand);
            break;
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
   private ExpressionNode parseEquality() throws Exception {
      
      ExpressionNode leftOperand = parseCompare();

      do {
         Character ch = skipSpace();
         if ((ch == null) || ((ch != '!') && (ch != '=')) || (peek() == null) || (peek() != '=')) {
            return leftOperand;
         }
         fIndex++;
         fIndex++;
         ExpressionNode rightOperand = parseCompare();
         if (leftOperand.fType != rightOperand.fType) {
            leftOperand  = promoteLongNode(leftOperand);
            rightOperand = promoteLongNode(rightOperand);
         }
         if (leftOperand.fType != rightOperand.fType) {
            throw new Exception("Incompatible operands in Equality");
         }
         if (ch == '=') {
            leftOperand = new Expression.EqualNode(leftOperand,rightOperand);
         } else {
            leftOperand = new Expression.NotEqualNode(leftOperand,rightOperand);
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
   private ExpressionNode parseBitAnd() throws Exception {
      
      ExpressionNode leftOperand = parseEquality();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '&')||((peek() != null)&&(peek() == '&'))) {
            return leftOperand;
         }
         fIndex++;
         ExpressionNode rightOperand = parseEquality();

         if ((isInteger(leftOperand)) && (isInteger(rightOperand))) {
            leftOperand = new Expression.BitAndNode(leftOperand, rightOperand);
         }
         else {
            throw new Exception("Unexpected data type for operand in bit-AND");
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
   private ExpressionNode parseXor() throws Exception {
      
      ExpressionNode leftOperand = parseBitAnd();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '^')) {
            return leftOperand;
         }
         fIndex++;
         ExpressionNode rightOperand = parseBitAnd();

         if ((isInteger(leftOperand)) && (isInteger(rightOperand))) {
            leftOperand = new Expression.BitXorNode(leftOperand, rightOperand);
         }
         else {
            throw new Exception("Unexpected data type for operand in Xor");
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
   private ExpressionNode parseBitOr() throws Exception {
      
      ExpressionNode leftOperand = parseXor();

      do {
         Character ch = skipSpace();
         if ((ch == null) || (ch != '|')||((peek() != null) && (peek() == '|'))) {
            return leftOperand;
         }
         fIndex++;
         ExpressionNode rightOperand = parseXor();

         if ((isInteger(leftOperand)) && (isInteger(rightOperand))) {
            leftOperand = new Expression.BitOrNode(leftOperand, rightOperand);
         }
         else {
            throw new Exception("Unexpected data type for operand in bit-OR");
         }
      } while (true);
   }
   
   /**
    * Accepts bitOr ['&&' bitOr]*
    * 
    * @return value of logicalAnd
    *
    * @throws Exception
    */
   private ExpressionNode parseLogicalAnd() throws Exception {
      
      ExpressionNode leftOperand = parseBitOr();

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

         if (!(isBoolean(leftOperand))) {
            throw new Exception("Unexpected data type for operand in Logical-AND");
         }
         ExpressionNode rightOperand = parseBitOr();
         if (!(isBoolean(rightOperand))) {
            throw new Exception("Unexpected data type for operand in Logical-AND");
         }
         leftOperand = new Expression.LogicalAndNode(leftOperand, rightOperand);
      } while (true);
   }
   
   /**
    * Accepts logicalOr : logicalAnd ['||' logicalAnd]*
    * 
    * @return value of logicalOr
    *
    * @throws Exception
    */
   private ExpressionNode parselogicalOr() throws Exception {
      
      ExpressionNode leftOperand = parseLogicalAnd();

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
         
         if (!isBoolean(leftOperand)) {
            throw new Exception("Unexpected data type for operand in Logical-OR");
         }
         ExpressionNode rightOperand = parseLogicalAnd();
         if (!(isBoolean(rightOperand))) {
            throw new Exception("Unexpected data type for operand in Logical-OR");
         }
         leftOperand = new Expression.LogicalOrNode(leftOperand, rightOperand);
      } while (true);
   }
   
   /**
    * Accepts ternary : shift ['?' expr ':' expr]
    * 
    * @return value of ternary
    *
    * @throws Exception
    */
   private ExpressionNode parseTernary() throws Exception {
      
      ExpressionNode control = parselogicalOr();

      Character ch = skipSpace();
      if ((ch == null) || (ch != '?')) {
         return control;
      }
      if (!isBoolean(control)) {
         throw new Exception("Unexpected data type for operand in Ternary");
      }
      fIndex++;
      ExpressionNode trueExp = parseSubExpression();
      ch = skipSpace();
      if (ch != ':') {
         throw new Exception("':' expected");
      }
      fIndex++;
      ExpressionNode falseExp = parseSubExpression();
      if (falseExp.fType != trueExp.fType) {
         throw new Exception("Incompatible operands");
      }
      return new Expression.TernaryNode(control, trueExp, falseExp);
   }
   
   /**
    * Accepts expr : ternary
    * 
    * @return value of expr
    *
    * @throws Exception
    */
   private ExpressionNode parseSubExpression() throws Exception {
      return parseTernary();
   }

//   /**
//    * Accepts expr : ternary
//    *
//    * @return value of expr
//    *
//    * @throws Exception
//    */
//   private ExpressionNode parseIndexExpression() throws Exception {
//      // Save current variables found
//      ArrayList<Variable> currentVariables = fCollectedVariables;
//      fCollectedVariables = new ArrayList<Variable>();
//      ExpressionNode index = parseTernary();
//      fCollectedVariables = currentVariables;
//      return index;
//   }

   private String getDiagnostic() {
      return String.format("\nInput     '%s" + "'" +
            "\n           %"+(fIndex+1)+"s", fExpressionString, "^")+
             ".....";
   }
   
   /**
    * Constructor
    * Create parser
    * @param expression
    * 
    * @param varProvider    Variable provider
    * @param variableNames  List of variable names for substitution e.g. var1Name, var2Name
    * @param mode Controls  Controls operation of parser
    * 
    *  <li>CollectIdentifiers        => Only parse expression to collect identifiers
    *  <li>CheckIdentifierExistance  => Treat identifiers as true/false => present/absent (must be boolean expression)
    *  <li>EvaluateFully             => Fully evaluate expression
    */
   public ExpressionParser(Expression expression, VariableProvider varProvider, Mode mode) {
      fMode              = mode;
      fProvider          = varProvider;
      fExpressionString  = null;
      fListener          = expression;
   }

   /**
    * Parse expression supplied
    * 
    * @param expression Expression to evaluate
    * 
    * @return  Result as an Expression tree
    * 
    * @throws Exception
    * 
    * @note A null expression is viewed as 'true'
    */
   ExpressionNode parseExpression(String expression) throws Exception {
      if (expression == null) {
         return new Expression.BooleanNode(true);
      }
//      if (expression.contains("mcg_sc_fcrdiv")) {
//         System.err.println("Found it " + expression);
//      }
      fCollectedVariables = new ArrayList<Variable>();
      fExpressionString = expression;
      fIndex = 0;
      try {
         ExpressionNode exp = parseSubExpression();
         if ((fIndex) != fExpressionString.length()) {
            throw new Exception("Unexpected characters at end of expression, found='");
         }
         return exp;
      } catch (Exception e) {
         String diagnostic = getDiagnostic()+"....." + e.getMessage()+"\n";
         throw new Exception(diagnostic, e);
      }
   }

   /**
    * Get list of non-constant variables collected while parsing expression
    * 
    * @return List
    */
   public ArrayList<Variable> getCollectedVariables() {
      return fCollectedVariables;
   }
   
}
