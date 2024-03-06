package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.PinListExpansion;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModelInterface;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.CommaListNode.Visitor;
import net.sourceforge.usbdm.deviceEditor.parsers.ExpressionParser.Mode;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

public class Expression implements IModelChangeListener {

   public enum Type {
      Double, Long, String, Boolean, DisabledValue, List, Set, Unknown,
   }
   
   /**
    * Evaluates an argument
    * 
    * @param arg     Argument to evaluate
    * @param type    Expected type arg
    * @return        Evaluated result or null if arg is null
    * 
    * @throws Exception if arg is not a constant or of unexpected type
    */
   static Object evalConstantArg(ExpressionNode arg, Type type) throws Exception {
      
      if (arg == null) {
         return null;
      }
      if (arg.fType != type) {
         throw new Exception(type.toString() + " argument expected, found '" + arg.fType + "'");
      }
      if (!arg.isConstant()) {
         throw new Exception("Constant argument expected");
      }
      return arg.eval();
   }
   
   /**
    * Evaluates an argument
    * 
    * @param arg     Argument to evaluate
    * @param type    Expected type arg
    * @return        Evaluated result or null if arg is null
    * 
    * @throws Exception if arg is not a constant or of unexpected type
    */
   static Object evalRequiredConstantArg(ExpressionNode arg, Type type) throws Exception {
      Object result = evalConstantArg(arg, type);
      if (result == null) {
         throw new Exception("Missing argument of type '" + type.toString() + "'");
      }
      return result;
   }
   
   static abstract class ExpressionNode {
      abstract Object eval() throws Exception;
      
      public final Type fType;
      
      public ExpressionNode(Type type) {
         fType = type;
      }
      
      boolean isConstant() {
         return false;
      }

      /**
       * Prune expression tree by doing constant folding
       * 
       * @return Pruned tree (may be 'this' unchanged)
       * 
       * @throws Exception
       */
      public ExpressionNode prune() throws Exception {
         return this;
      }

      /**
       * Collect a list of the variables used in expression
       * 
       * @param variablesFound  List to add found variables to
       * @throws Exception
       */
      public void collectVars(ArrayList<Variable> variablesFound) throws Exception {
      }
      
      @Override
      public String toString() {
         return this.getClass().getSimpleName()+"("+fType+")";
      }
      
   }

   static abstract class BinaryExpressionNode extends ExpressionNode {

      ExpressionNode fLeft;
      ExpressionNode fRight;

      BinaryExpressionNode(ExpressionNode left, ExpressionNode right, Type type) {
         super(type);
         fLeft  = left;
         fRight = right;
      }
      
      @Override
      public ExpressionNode prune() throws Exception {
         fLeft  = fLeft.prune();
         fRight = fRight.prune();
         if (fLeft.isConstant() && fRight.isConstant()) {
            return wrapConstant(eval());
         }
         return this;
      }
      
      @Override
      public void collectVars(ArrayList<Variable> variablesFound) throws Exception {
         fLeft.collectVars(variablesFound);
         fRight.collectVars(variablesFound);
      }
      
      @Override
      boolean isConstant() {
         return fLeft.isConstant() && fRight.isConstant();
      }
      
      @Override
      public String toString() {
         return this.getClass().getSimpleName()+"("+fLeft.toString()+","+fRight.toString()+","+fType+")";
      }

   }

   static abstract class UnaryExpressionNode extends ExpressionNode {

      ExpressionNode fArg;

      UnaryExpressionNode(ExpressionNode arg, Type type) {
         super(type);
         fArg  = arg;
      }
      
      @Override
      public ExpressionNode prune() throws Exception {
         fArg  = fArg.prune();
         if (fArg.isConstant()) {
            return wrapConstant(eval());
         }
         return this;
      }

      @Override
      public void collectVars(ArrayList<Variable> variablesFound) throws Exception {
         fArg.collectVars(variablesFound);
      }
      
      @Override
      boolean isConstant() {
         return fArg.isConstant();
      }
   }

   static abstract class FunctionNode extends ExpressionNode {

      ExpressionNode fArg;

      FunctionNode(String functionName, ExpressionNode arg, Type type) {
         super(type);
         fArg  = arg;
      }
      
      @Override
      public void collectVars(ArrayList<Variable> variablesFound) throws Exception {
         fArg.collectVars(variablesFound);
      }
      
      @Override
      public ExpressionNode prune() throws Exception {
         fArg  = fArg.prune();
         if (fArg.isConstant()) {
            return wrapConstant(fArg.eval());
         }
         return this;
      }
   }

   static class VariableNode extends ExpressionNode implements IExpressionChangeListener {
      static final int MAX_DIMENSION = 4;
      
      static enum Modifier {
         Value, Name, Code, Enum, Size,
      };

      /** Cached variable */
      protected Variable fVar;
      
      /** Modified for variable access */
      private final Modifier fModifier;
      
      /** Owner of the expression for change notification */
      private final Expression fOwner;
      
      /** Name of variable without index */
      private String fVarName;
      
      /** Expression for index */
      private Expression fIndex;

//    private final Variable[]       fVars = new Variable[MAX_DIMENSION];

//      private       Object           fValue = null;
      

      VariableNode(Expression owner, String varName, Type type, Modifier modifier, Expression index) {
         super(type);
         
         fOwner      = owner;
         fVarName    = varName;
         fIndex      = index;
         fModifier   = modifier;
         if (index != null) {
            try {
               index.addListener(this);
            } catch (Exception e) {
               System.err.println("Failed to add listeners for index changes to expresssion");
               e.printStackTrace();
            }
         }
      }

      /**
       * Create a node to represent a simple variable
       * 
       * @param var        Variable to wrap
       * @param modifier   Access modifier for variable
       * 
       * @return           Wrapped variable
       * 
       * @throws Exception
       */
      /**
       * 
       * @param owner    The expression owning this variable reference
       * @param varName  The variable name
       * @param modifier Modifier e.g. name, code etc (Field)
       * @param index    Index for indexed variable. null if not indexed
       * 
       * @return
       * @throws Exception
       */
      public static ExpressionNode create(Expression owner, String varName, String modifier, Expression index) throws Exception {
         
//         if (varName.contains("ftm_cnsc_secondOutput")) {
//            System.err.println("VariableNode.create("+varName+")");
//         }
         String name = varName;
         if (index != null) {
            // Use zero index to allow safe access to array variable
            int ind = 0;
            // Use constant index if provided in preference
            if (index.isConstant()) {
               ind = index.getValueAsLong().intValue();
            }
            name = name+"["+ind+"]";
         }
         // Get variable to determine type
         // This may fail if the variable does not exist yet
         Variable var = owner.fVarProvider.safeGetVariable(name);
//         if (name.contains("tsi_pen_pen")) {
//            System.err.println("Found it");
//         }
//         if (var == null) {
//            System.err.println("Warning - Unable to access var '"+varName+"', assuming string type");
//         }
         if (modifier != null) {
            if ("name".equalsIgnoreCase(modifier)) {
               // .name  => Name from choice
               return new VariableNode(owner, varName, Type.String, Modifier.Name, index);
            }
            else if ("code".equalsIgnoreCase(modifier)) {
               // .code  => Code from choice
               return new VariableNode(owner, varName, Type.String, Modifier.Code, index);
            }
            else if ("enum".equalsIgnoreCase(modifier)) {
               // .code  => Code from choice
               return new VariableNode(owner, varName, Type.String, Modifier.Enum, index);
            }
            else if ("size".equalsIgnoreCase(modifier)) {
               // .size  => Number of choices
               return new VariableNode(owner, varName, Type.Long, Modifier.Size, index);
            }
            else {
               throw new Exception("Unexpected field for '" + var + "'");
            }
         }
         if (var instanceof ChoiceVariable) {
            // 'value' for a choice is the index
            return new VariableNode(owner, varName, Type.Long, null, index);
         }
         if (var instanceof BooleanVariable) {
            return new VariableNode(owner, varName, Type.Boolean, null, index);
         }
         if (var instanceof LongVariable) {
            return new VariableNode(owner, varName, Type.Long, null, index);
         }
         if (var instanceof DoubleVariable) {
            return new VariableNode(owner, varName, Type.Double, null, index);
         }
         if (var instanceof StringVariable) {
            return new VariableNode(owner, varName, Type.String, null, index);
         }
         if (var != null) {
            // Can't happen!!
            throw new Exception("Did not find type for " + var);
         }
         System.err.println("Warning, using default String type for non-existent variable '" + name + "'");
         return new VariableNode(owner, varName, Type.String, null, index);
      }
      
      private Variable getVar() throws Exception {
         
         Variable var = fVar;
         if (var == null) {
            String name = fVarName;
            if (fIndex != null) {
               name = name+"["+fIndex.getValueAsLong()+"]";
            }
            var = fOwner.fVarProvider.getVariable(name);

//            if ((fIndex != null) && fIndex.isConstant()) {
//               System.err.println("Caching indexed variable lookup '" + fVarName + "[" + fIndex.getExpressionStr() + "]'");
//            }
            // If there is no index or index is unchanging then we can cache the variable lookup
            if ((fIndex == null)||fIndex.isConstant()) {
//               if ((fIndex != null) && (var.getName().contains("ftm_cnsc_mode"))) {
//                  System.err.println("Pruning variable with constant index '" + var.getName() );
//               }
               fVar = var;
               fIndex = null;
            }
         }
         return var;
      }
      
//      private Variable getIndexedVar(int index) throws Exception {
//
//         String name = fVarName+"["+index+"]";
//         return fOwner.fVarProvider.getVariable(name);
//      }
      
      @Override
      public void expressionChanged(Expression expression) {
         // Index changed indicate var is stale
         fVar = null;
         fOwner.notifyExpressionChangeListeners();
      }

      @Override
      public void collectVars(ArrayList<Variable> variablesFound) throws Exception {
         Variable var = getVar();
         if (!variablesFound.contains(var))
            variablesFound.add(var);
      }

      @Override
      boolean isConstant() {
         try {
            return ((fIndex==null)||fIndex.isConstant()) && getVar().isConstant();
         } catch (Exception e) {
         }
         return false;
      }

      @Override
      public ExpressionNode prune() throws Exception {
         if ((fIndex != null)&&(fIndex.isConstant())) {
            // Do indexing once only
            getVar();
         }
         if (isConstant()) {
            if (fIndex != null) {
               System.err.println("Pruning constant indexed variable '" + fVarName + "[" + fIndex.getExpressionStr() +"]' to " + eval().toString() );
            }
//            else {
//               System.err.println("Pruning constant variable '" + fVarName + "' to " + eval().toString() );
//            }
            return wrapConstant(eval());
         }
         return this;
      }
      

      @Override
      public String toString() {
         if (fVar != null) {
            return this.getClass().getSimpleName()+"("+fVar.getName()+", "+fType+")";
         }
         return this.getClass().getSimpleName()+"("+fVarName+", "+fType+")";
      }

      @Override
      Object eval() throws Exception {
         Variable var = getVar();
         if (fModifier != null) {
            if (!(var instanceof VariableWithChoices)) {
               throw new Exception("Expected choice variable '" + var + "'");
            }
            VariableWithChoices cv = (VariableWithChoices)var;
            ChoiceData choiceData = cv.getCurrentChoice();
            if (fModifier == Modifier.Size) {
               Long temp = (long) cv.getChoiceCount();
               return temp;
            }
            if (choiceData == null) {
               return "Nothing selected in choice";
            }
            switch (fModifier) {
            case Code:
               // .code  => Code from choice
               return choiceData.getCodeValue();
            case Enum:
               // .enum  => Enum name from choice
               return choiceData.getEnumName();
            case Name:
               // .name  => Name from choice
               return choiceData.getName();
            case Value:
               // .value => Value from choice
               return choiceData.getValue();
            default:
               return "Impossible";
            }
         }
         else if (var instanceof ChoiceVariable) {
            return var.getValueAsLong();
         }
         if (var instanceof BooleanVariable) {
            return var.getValueAsBoolean();
         }
         if (var instanceof LongVariable) {
            return var.getValueAsLong();
         }
         if (var instanceof DoubleVariable) {
            return var.getValueAsDouble();
         }
         // Default to treating as string
         return var.getValueAsString();
      }

      public Boolean exists() {
         String name = fVarName;
         if (fIndex != null) {
            name = name+"[0]";
         }
         return fOwner.fVarProvider.safeGetVariable(name) != null;
      }
      
   }

   static class DisabledValueNode extends ExpressionNode {

      DisabledValueNode() {
         super(Type.DisabledValue);
      }

      @Override
      Object eval() {
         return false;
      }
      
      @Override
      boolean isConstant() {
         return true;
      }
   }

   static class BooleanNode extends ExpressionNode {

      final Boolean fValue;

      BooleanNode(Boolean value) {
         super(Type.Boolean);
         fValue = value;
      }

      @Override
      Object eval() {
         return fValue;
      }
      
      @Override
      boolean isConstant() {
         return true;
      }

      @Override
      public String toString() {
         return fValue.toString();
      }
   }

   static class DoubleConstantNode extends ExpressionNode {

      final Double fValue;

      DoubleConstantNode(double value) {
         super(Type.Double);
         fValue = value;
      }

      @Override
      Object eval() {
         return fValue;
      }
      
      @Override
      boolean isConstant() {
         return true;
      }

      @Override
      public String toString() {
         return fValue.toString();
      }
   }

   static class LongConstantNode extends ExpressionNode {

      final Long fValue;

      LongConstantNode(long value) {
         super(Type.Long);
         fValue = value;
      }

      @Override
      Object eval() {
         return fValue;
      }
      
      @Override
      boolean isConstant() {
         return true;
      }

      @Override
      public String toString() {
         return fValue.toString();
      }
   }

   static class StringConstantNode extends ExpressionNode {

      final String fValue;

      StringConstantNode(String value) {
         super(Type.String);
         fValue = value;
      }

      @Override
      String eval() {
         return fValue;
      }
      
      @Override
      boolean isConstant() {
         return true;
      }

      @Override
      public String toString() {
         return "\""+fValue+"\"";
      }
   }

   static class BooleanConstantNode extends ExpressionNode {

      final Boolean fValue;

      BooleanConstantNode(Boolean value) {
         super(Type.Boolean);
         fValue = value;
      }

      @Override
      Boolean eval() {
         return fValue;
      }
      
      @Override
      boolean isConstant() {
         return true;
      }

      @Override
      public String toString() {
         return "\""+fValue+"\"";
      }
   }

   static class NotNode extends UnaryExpressionNode {

      NotNode(ExpressionNode left) {
         super(left, Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         return !(Boolean)fArg.eval();
      }
   }
   
   static class CastToLongNode extends UnaryExpressionNode {

      CastToLongNode(ExpressionNode arg) throws Exception {
         super(arg, Type.Long);
         
         switch(arg.fType) {
         default:
            throw new Exception("Expression cannot be cast to 'Long'");
            
         case Double:
         case Long:
            break;
         }
      }

      @Override
      Object eval() throws Exception {
         switch(fArg.fType) {
         default:
            throw new Exception("Invalid cast");
            
         case Double:
            break;
            
         case Long:
            break;
         }
         Double res = (double) fArg.eval();
         return res;
      }
      
      /**
       * Promote arg to Long type
       * 
       * @param arg  Expression  to promote
       * 
       * @return  Promoted arg, or arg unchanged, if already correct type
       * 
       * @throws Exception
       */
      static ExpressionNode promoteIfNeeded(ExpressionNode arg) throws Exception {
         if (arg.fType == Type.Long) {
            return arg;
         }
         return new CastToLongNode(arg);
      }
   }
   
   static class OrdinalNode extends UnaryExpressionNode {

      OrdinalNode(ExpressionNode arg) throws Exception {
         super(arg, Type.Long);
         
         switch(arg.fType) {
         default:
            throw new Exception("Ordinal is not supported for expression");
            
         case Long:
         case String:
         case Boolean:
            break;
         }
      }

      @Override
      Object eval() throws Exception {
         switch(fArg.fType) {
         default:
            throw new Exception("Invalid cast");
            
         case Long:
            return fArg.eval();
            
         case String: {
            String s = (String) fArg.eval();
            if (s.length()!=1) {
               throw new Exception("Ordinal only available for 1 character strings");
            }
            return Character.getNumericValue(s.charAt(0));
         }
         case Boolean: {
            Boolean b = (Boolean) fArg.eval();
            return b?0L:1L;
         }
         }
      }
      
      /**
       * Promote arg to Long type
       * 
       * @param arg  Expression  to promote
       * 
       * @return  Promoted arg, or arg unchanged, if already correct type
       * 
       * @throws Exception
       */
      static ExpressionNode promoteIfNeeded(ExpressionNode arg) throws Exception {
         if (arg.fType == Type.Long) {
            return arg;
         }
         return new CastToLongNode(arg);
      }
   }
   
   static class CastToDoubleNode extends UnaryExpressionNode {

      /**
       * Cast a Long or Double ExpressionNode to Double ExpressionNode
       * 
       * @param arg
       * @throws Exception
       */
      CastToDoubleNode(ExpressionNode arg) throws Exception {
         super(arg, Type.Double);
         if ((arg.fType != Expression.Type.Long) && (arg.fType != Expression.Type.Double)) {
            throw new Exception("Expression cannot be promoted to 'Double'");
         }
      }

      @Override
      Object eval() throws Exception {
         Object res = fArg.eval();
         if (res instanceof Double) {
            return res;
         }
         double lRes = (Long) res;
         return lRes;
      }
      
      /**
       * Promote arg to Double type
       * 
       * @param arg  Expression  to promote
       * 
       * @return  Promoted arg, or arg unchanged, if already correct type
       * 
       * @throws Exception
       */
      static ExpressionNode promoteIfNeeded(ExpressionNode arg) throws Exception {
         if (arg.fType == Type.Double) {
            return arg;
         }
         return new CastToDoubleNode(arg);
      }
   }
   
   static class PrettyNode extends UnaryExpressionNode {

      /**
       * Prettify a string e.g. cmd => Cmd
       * 
       * @param arg
       * @throws Exception
       */
      PrettyNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if (arg.fType != Expression.Type.String) {
            throw new Exception("Expression cannot be prettified");
         }
      }

      @Override
      Object eval() throws Exception {
         String s = ((String) fArg.eval()).strip();
         String res = s;
         if (s.length()>2) {
            if (s.matches("[A-Z0-9]+")) {
               // All upper-case
               res = Character.toUpperCase(s.charAt(0))+s.substring(1).toLowerCase();
            }
            else if (s.matches("[a-z0-9]+")) {
               // All lower-case
               res = Character.toUpperCase(s.charAt(0))+s.substring(1).toLowerCase();
            }
            else {
               // Mixed case
               res = Character.toUpperCase(s.charAt(0))+s.substring(1);
            }
         }
//         System.err.println("prettify: '"+s+"' => '"+res+"'");
         return res;
      }
   }
   
   static class UppercaseNode extends UnaryExpressionNode {

      /**
       * Uppercase a string e.g. cmd => CMD
       * 
       * @param arg
       * @throws Exception
       */
      UppercaseNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if (arg.fType != Expression.Type.String) {
            throw new Exception("Expression cannot be Uppercased");
         }
      }

      @Override
      Object eval() throws Exception {
         String s = (String) fArg.eval();
         return s.toUpperCase();
      }
   }
   
   static class ReplaceAllNode extends UnaryExpressionNode {

      /**
       * Does Pattern.compile(text).matcher(regex).replaceAll(replacement)
       * 
       * @param arg     Arguments - must be ("text","regex","replacement")
       * @throws Exception
       */
      ReplaceAllNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if (!(arg instanceof CommaListNode)) {
            throw new Exception("Expected (\"text\",\"regex\",\"replacement\")");
         }
         CommaListNode cln = (CommaListNode) arg;
         ExpressionNode[] args = cln.getExpressionList();
         if (args.length != 3) {
            throw new Exception("Wrong number of arguments, expected (\"text\",\"regex\",\"replacement\")");
         }
         if ((args[0].fType != Type.String)||(args[1].fType != Type.String)||(args[2].fType != Type.String)) {
            throw new Exception("Arguments have wrong type, expected (\"text\",\"regex\",\"replacement\")");
         }
      }

      @Override
      Object eval() throws Exception {

         Object resArray[] = (Object[]) fArg.eval();
         String res = Pattern.compile(resArray[1].toString()).matcher(resArray[0].toString()).replaceAll(resArray[2].toString());
//         System.err.println("Pattern.compile(\""+resArray[1].toString()+"\").matcher(\""+resArray[0].toString()+"\").replaceAll(\""+resArray[2].toString()+"\")"+
//               "=>\""+res+"\"");
         return res;
      }
   }
   
   static class FormatNode extends UnaryExpressionNode {

      /**
       * Uppercase a string e.g. cmd => CMD
       * 
       * @param arg
       * @throws Exception
       */
      FormatNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
      }

      @Override
      Object eval() throws Exception {
         Object res = fArg.eval();
         if (!(res instanceof Object[])) {
            throw new Exception("Wrong argument type to FormatNode, arg = " + fArg);
         }
         Object resArray[] = (Object[]) res;
         if (!(resArray[0] instanceof String)) {
            throw new Exception("Wrong argument[0] type to FormatNode, arg = " + resArray[0]);
         }
         return String.format(resArray[0].toString(), resArray[1]);
      }
   }
   
   static class LowercaseNode extends UnaryExpressionNode {

      /**
       * LowercaseNode a string e.g. cMd => cmd
       * 
       * @param arg
       * @throws Exception
       */
      LowercaseNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if (arg.fType != Type.String) {
            throw new Exception("Expression cannot be Lowercased");
         }
      }

      @Override
      Object eval() throws Exception {
         String s = (String) fArg.eval();
         return s.toLowerCase();
      }
   }
   
   static class CastToCharacterStringNode extends UnaryExpressionNode {

      /**
       * Cast a Long ExpressionNode to a single character String ExpressionNode e.g. 30 => "0"
       * 
       * @param arg
       * @throws Exception
       */
      CastToCharacterStringNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if (arg.fType != Expression.Type.Long) {
            throw new Exception("Expression cannot be promoted to 'Character String'");
         }
      }

      @Override
      Object eval() throws Exception {
         long l = (long) fArg.eval();
         int i = (int) l;
         return Character.toString((char)i);
      }
   }

   static class ToStringNode extends UnaryExpressionNode {

      /**
       * Cast a Long ExpressionNode to a single character String ExpressionNode e.g. 30 => "0"
       * 
       * @param arg
       * @throws Exception
       */
      ToStringNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if ((arg.fType != Expression.Type.Long)&&(arg.fType != Expression.Type.Boolean)) {
            throw new Exception("Expression cannot be converted to String");
         }
      }

      @Override
      Object eval() throws Exception {
         Object res = fArg.eval();
         return res.toString();
      }
   }

   static class ExpandPinListNode extends UnaryExpressionNode {
      
      /**
       * Cast a Long ExpressionNode to a single character String ExpressionNode e.g. 30 => "0"
       * 
       * @param arg
       * @param delimiter
       * @throws Exception
       */
      ExpandPinListNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if (arg instanceof CommaListNode) {
            CommaListNode argList = (CommaListNode)arg;
            ExpressionNode[] args = argList.fList;
            if (args.length>2) {
               throw new Exception("Too many arguments for function");
            }
            if (!args[1].isConstant()) {
               throw new Exception("2nd argument must be a constant");
            }
            if (args[0].fType != Type.String) {
               throw new Exception("1st argument must be a string");
            }
            if (args[1].fType != Type.String) {
               throw new Exception("2nd argument must be a string");
            }
         }
         else if (arg.fType != Expression.Type.String) {
            throw new Exception("Argument must be a string");
         }
      }

      @Override
      Object eval() throws Exception {
         if (fArg instanceof CommaListNode) {
            CommaListNode argList = (CommaListNode)fArg;
            ExpressionNode[] args = argList.fList;
            String s = String.join((String)args[1].eval(), PinListExpansion.expandPinList((String)args[0].eval(), ","));
            s = s.replace("\\n", "\n");
            return s.replace("\\t", "   ");
         }
         else {
            String s = (String) fArg.eval();
            return String.join(",", PinListExpansion.expandPinList(s, ","));
         }
      }
   }
   
   static class SignalDescriptionNode extends ExpressionNode {

      final Signal fSignal;
      final String fRegex;
      final String fReplacement;
      
      private SignalDescriptionNode(Signal signal, String regex, String replacement) {
         super(Type.String);
         fSignal      = signal;
         fRegex       = regex;
         fReplacement = replacement;
      }

      /**
       * Create 'live' Expression node describing the signal
       * String is of format<br>
       * <b>"signal-name,user-description or code-identifier,mapped-pin-name|combined-description"</b>
       * Example: <b>"TSIO0_CH0|Touch 1|PTB3|Touch1(PTB3)"</b>
       * 
       * @param peripheral Peripheral owning the signal
       * @param arg        Constant expression evaluating to the index of the signal in info table
       * 
       * @throws Exception
       */
      /**
       * Get a information about signal and mapped pin. <br>
       * Returns a string of format<br>
       * <b>"signal-name,user-description or code-identifier,mapped-pin-name|combined-description"</b>
       * Example: <b>"TSIO0_CH0|Touch 1|PTB3|Touch1(PTB3)"</b>
       * 
       * @return
       */
      
      private SignalDescriptionNode(Peripheral peripheral, ExpressionNode arg) throws Exception {
         super(Type.String);

         if (arg == null) {
            throw new Exception("Expected index for signal (an integer)");
         }
         if (arg.fType != Type.List) {
            
            // Must be a single index (Long)
            Long value = (Long) evalConstantArg(arg, Type.Long);
            
            fSignal      = peripheral.getSignalFromIndex((int)value.longValue());
            fRegex       = null;
            fReplacement = null;
            return;
         }
         ExpressionNode[] args = ((CommaListNode)arg).getExpressionList();
         if (args.length != 3) {
            // Must be an index (Long) and a String replacement pattern
            throw new Exception("Expected 3 arguments 'index,regex,replacement'");
         }
         fSignal      = peripheral.getSignalFromIndex((int)((Long) evalConstantArg(args[0], Type.Long)).longValue());
         fRegex       = (String) evalConstantArg(args[1], Type.String);
         fReplacement = (String) evalConstantArg(args[2], Type.String);
         
      }
      
      /**
       * Get signal being monitored
       * 
       * @return Signal or null if not found when constructed
       */
      public Signal getSignal() {
         return fSignal;
      }
      
      @Override
      Object eval() throws Exception {

         String text = fSignal.getMapDescription();
         if (fRegex != null) {
            Pattern pattern = Pattern.compile(fRegex);
            Matcher matcher = pattern.matcher(text);
            if (!matcher.matches()) {
               return "regex failed match";
            }
            text = matcher.replaceAll(fReplacement);
         }
         return text;
      }

      /**
       * Create Expression node providing information about a signal.
       * 
       * @param fProvider  Peripheral to locate signals
       * @param arg        Constant expression evaluating to<br>
       * <li> Long index of the signal in info table
       * <li> (optional) regex for pattern matching (String)
       * <li> (optional) replacement pattern for use with regex to modify result (String)<br>
       * <br>
       * If arg is null then no modification is done.<br>
       * Examples<br>
       * Signal description: <b>"signal-name,user-description or code-identifier,mapped-pin-name|combined-description|code-identifier"</b><br>
       * Signal example:     <b>"TSIO0_CH0|Touch 1|PTB3|Touch1(PTB3)|TouchInput"</b><br>
       * Example filter:     <b>"^(.*?)\|(.*?)\|(.*?)\|(.*?)\|(.*?)$"</b><br>
       * Example replacement: <b>"$1 mapped to $3"</b><br>
       * 
       * @return  SignalDescriptionNode if signal found <br>
       *          StringConstantNode("Signal not found") if signal not found
       * 
       * @throws Exception
       */
      public static ExpressionNode createNodeFromIndex(Peripheral peripheral, ExpressionNode arg) throws Exception {

         Signal signal;
         String regex       = null;
         String replacement = null;
         
         if (arg == null) {
            throw new Exception("Expected index for signal (an integer)");
         }
         do {
            if (arg.fType != Type.List) {

               // Must be a single index (Long)
               Long value = (Long) evalConstantArg(arg, Type.Long);

               signal      = peripheral.getSignalFromIndex((int)value.longValue());
               continue;
            }
            ExpressionNode[] args = ((CommaListNode)arg).getExpressionList();
            if (args.length != 3) {
               // Must be an index (Long), regex (String) and replacement pattern (String)
               throw new Exception("Expected 3 arguments 'index,regex,replacement'");
            }
            signal      = peripheral.getSignalFromIndex((int)((Long) evalConstantArg(args[0], Type.Long)).longValue());
            regex       = (String) evalConstantArg(args[1], Type.String);
            replacement = (String) evalConstantArg(args[2], Type.String);
            continue;
         } while(false);
         
         if (signal != null) {
            return new SignalDescriptionNode(signal, regex, replacement);
         }
         else {
            return new StringConstantNode("Signal not found");
         }
      }
      
      /**
       * Create Expression node providing information about a signal.
       * 
       * @param fProvider  Peripheral to locate signals
       * @param arg        Constant expression evaluating to<br>
       * <li> Name of signal (a String)
       * <li> (optional) regex for pattern matching (String)
       * <li> (optional) replacement pattern for use with regex to modify result (String)<br>
       * <br>
       * If arg is null then no modification is done.<br>
       * Examples<br>
       * Signal description: <b>"signal-name,user-description or code-identifier,mapped-pin-name|combined-description|code-identifier"</b><br>
       * Signal example:     <b>"TSIO0_CH0|Touch 1|PTB3|Touch1(PTB3)|TouchInput"</b><br>
       * Example filter:     <b>"^(.*?)\|(.*?)\|(.*?)\|(.*?)\|(.*?)$"</b><br>
       * Example replacement: <b>"$1 mapped to $3"</b><br>
       * 
       * @return  SignalDescriptionNode if signal found <br>
       *          StringConstantNode("Signal not found") if signal not found
       * 
       * @throws Exception
       */
      public static ExpressionNode createNodeFromName(Peripheral peripheral, ExpressionNode arg) throws Exception {
         Signal signal;
         String regex       = null;
         String replacement = null;
         
         if (arg == null) {
            throw new Exception("Expected index for signal (an integer)");
         }
         do {
            if (arg.fType != Type.List) {
               // Must be a single name (String)
               signal      = peripheral.getSignal((String) evalConstantArg(arg, Type.String));
               continue;
            }
            ExpressionNode[] args = ((CommaListNode)arg).getExpressionList();
            if (args.length != 3) {
               // Must be an name (String), regex (String) and a replacement pattern (String)
               throw new Exception("Expected 3 arguments 'name,regex,replacement'");
            }
            signal      = peripheral.getSignal((String) evalConstantArg(args[0], Type.String));
            regex       = (String) evalConstantArg(args[1], Type.String);
            replacement = (String) evalConstantArg(args[2], Type.String);
            continue;
         } while(false);
         
         if (signal != null) {
            return new SignalDescriptionNode(signal, regex, replacement);
         }
         else {
            return new StringConstantNode("Signal not found");
         }
      }
   }
   
   static class PinMappingNode extends ExpressionNode {

      Signal fSignal;
      
      PinMappingNode(Signal signal) {
         super(Type.String);
         fSignal = signal;
      }

      @Override
      Object eval() throws Exception {
         Pin pin = null;
         if (fSignal != null) {
            pin = fSignal.getMappedPin();
         }
         String pinName = "";
         if (pin != Pin.UNASSIGNED_PIN) {
            pinName = pin.getName();
         }
         return pinName;
      }
   }
   
   static class MappedPinsListNode extends ExpressionNode {

      final Peripheral fPeripheral;
      final String     fRegex;
      final String     fReplacement;

      /**
       * Create Expression node providing information about mapped signals.
       * 
       * @param fProvider  Peripheral to locate signals
       * @param arg        Constant expression evaluating to<br>
       * <li> (optional) String regex for filtering on pin name
       * <li> (optional) String replacement pattern for use with regex to produce modified pin information
       * 
       * If no arg is null then no filtering is done and all mapped pins are returned.
       * 
       * @return MappedPinsListNode providing a comma-separated String result when evaluated
       * 
       * @throws Exception
       */
      MappedPinsListNode(Peripheral peripheral, ExpressionNode arg) throws Exception {
         super(Type.String);
         
         fPeripheral     = peripheral;
         
         if (arg == null) {
            // Unfiltered list
            fRegex       = null;
            fReplacement = null;
            return;
         }
         if (arg.fType != Type.List) {
            // Filtered list
            fRegex       = (String) evalConstantArg(arg, Type.String);
            fReplacement = null;
            return;
         }
         ExpressionNode[] args = ((CommaListNode)arg).getExpressionList();
         if (args.length != 2) {
            throw new Exception("Expected 2 arguments, 'regex,replacement'");
         }
         // Filtered list with modified values
         fRegex       = (String) evalConstantArg(args[0], Type.String);
         fReplacement = (String) evalConstantArg(args[1], Type.String);
      }

      @Override
      Object eval() throws Exception {

         Pin[] mappedPins = fPeripheral.getMappedPins();
         StringBuilder sb = new StringBuilder();

         Pattern pattern = null;
         if (fRegex != null) {
            pattern = Pattern.compile(fRegex);
         }
         for (Pin pin:mappedPins) {
            if (pin == null) {
               continue;
            }
            String pinName = pin.getName();
            if (pattern != null) {
               Matcher matcher = pattern.matcher(pinName);
               if (!matcher.matches()) {
                  continue;
               }
               if (fReplacement != null) {
                  pinName = matcher.replaceAll(fReplacement);
               }
            }
            if (!sb.isEmpty()) {
               sb.append(",");
            }
            sb.append(pinName);
         }
         return sb.toString();
      }
   }
   
   static class SignalsListNode extends ExpressionNode {

      final Peripheral fPeripheral;
      final String     fRegex;
      final String     fReplacement;
      
      /**
       * Create Expression node providing information about signals.
       * 
       * @param fProvider  Peripheral to locate signals
       * @param arg        Constant expression evaluating to<br>
       * <li> (optional) String regex for filtering on mapped signal description
       * <li> (optional) String replacement pattern for use with regex to produce modify result<br>
       * <br>
       * If arg is null then no filtering is done and all mapped signals are returned.<br>
       * Examples<br>
       * Signal description: <b>"signal-name,user-description or code-identifier,mapped-pin-name|combined-description|code-identifier"</b><br>
       * Signal example:     <b>"TSIO0_CH0|Touch 1|PTB3|Touch1(PTB3)|TouchInput"</b><br>
       * Example filter:     <b>"^(.*?)\|(.*?)\|(.*?)\|(.*?)\|(.*?)$"</b><br>
       * Example replacement: <b>"$1 mapped to $3"</b><br>
       * 
       * @return SignalsListNode providing a comma-separated String result when evaluated
       * 
       * @throws Exception
       */
      SignalsListNode(Peripheral peripheral, ExpressionNode arg) throws Exception {
         super(Type.String);
         fPeripheral     = peripheral;
         if (arg == null) {
            fRegex       = null;
            fReplacement = null;
            return;
         }
         if (arg.fType != Type.List) {
            fRegex       = (String) evalConstantArg(arg, Type.String);
            fReplacement = null;
            return;
         }
         ExpressionNode[] args = ((CommaListNode)arg).getExpressionList();
         if (args.length != 2) {
            throw new Exception("Expected 2 arguments");
         }
         fRegex       = (String) evalConstantArg(args[0], Type.String);
         fReplacement = (String) evalConstantArg(args[1], Type.String);
      }
      
      @Override
      String eval() throws Exception {
         
         Signal[] signals = fPeripheral.getMappedSignals();
         StringBuilder sb = new StringBuilder();
         
         Pattern pattern = null;
         if (fRegex != null) {
            pattern = Pattern.compile(fRegex);
         }
         for (Signal signal:signals) {
            if (signal == null) {
               continue;
            }
            // Description: <b>"signal-name,user-description or code-identifier,mapped-pin-name|combined-description|code-identifier"</b>
            // Example:     <b>"TSIO0_CH0|Touch 1|PTB3|Touch1(PTB3)|TouchInput"</b>
            // Example filter: "^(.*?)\|(.*?)\|(.*?)\|(.*?)\|(.*?)$"
            // Example replacement: "$1 mapped to $3"
            String description = signal.getMapDescription();
            if (pattern != null) {
               Matcher matcher = pattern.matcher(description);
               if (!matcher.matches()) {
                  continue;
               }
               if (fReplacement != null) {
                  description = matcher.replaceAll(fReplacement);
               }
            }
            if (!sb.isEmpty()) {
               sb.append(",");
            }
            sb.append(description);
         }
//         System.err.println("SignalsListNode.eval() => '"+sb.toString()+"'");
         return sb.toString();
      }
   }
   
   static class VectorsListNode extends ExpressionNode {

      final Peripheral fPeripheral;
      final String     fRegex;
      final String     fReplacement;
      
      /**
       * Create Expression node providing information about signals.
       * 
       * @param fProvider  Peripheral to locate vectors
       * @param arg        Constant expression evaluating to<br>
       * <li> (optional) String regex for filtering on vector description
       * <li> (optional) String replacement pattern for use with regex to produce modify result<br>
       * <br>
       * If arg is null then no filtering is done and all vectors are returned.<br>
       * Examples<br>
       * Information <b>"name|index|C-description|handler-name|peripherals[,peripheral]"</b><br>
       * Example:    <b>"PORTCD|3|Port interrupt||PORTC,PORTD"</b><br>
       * Example pattern: <b>"^(.*?)\|(.*?)\|(.*?)\|(.*?)\|(.*?)$"</b><br>
       * Example replacement: <b>"$1 used by $5"</b><br>
       * 
       * @return SignalsListNode providing a comma-separated String result when evaluated
       * 
       * @throws Exception
       */
      VectorsListNode(Peripheral peripheral, ExpressionNode arg) throws Exception {
         super(Type.String);
         fPeripheral     = peripheral;
         if (arg == null) {
            fRegex       = null;
            fReplacement = null;
            return;
         }
         if (arg.fType != Type.List) {
            fRegex       = (String) evalConstantArg(arg, Type.String);
            fReplacement = null;
            return;
         }
         ExpressionNode[] args = ((CommaListNode)arg).getExpressionList();
         if (args.length != 2) {
            throw new Exception("Expected 2 arguments");
         }
         fRegex       = (String) evalConstantArg(args[0], Type.String);
         fReplacement = (String) evalConstantArg(args[1], Type.String);
      }
      
      @Override
      String eval() throws Exception {

         Pattern pattern = null;
         if (fRegex != null) {
            pattern = Pattern.compile(fRegex);
         }
         VectorTable vectorTable = fPeripheral.getDeviceInfo().getDevicePeripherals().getVectorTable();
         StringBuilder sb = new StringBuilder();
         for (InterruptEntry entry:vectorTable.getEntries()) {
            if (entry == null) {
               continue;
            }
            String description = entry.getInformation(pattern, fReplacement);
            if (description == null) {
               continue;
            }
            if (!sb.isEmpty()) {
               sb.append(';');
            }
            sb.append(description);
         }
         return sb.toString();
      }
   }
   
   static class UnaryMinusNode extends UnaryExpressionNode {

      UnaryMinusNode(ExpressionNode left) {
         super(left, left.fType);
      }

      @Override
      Object eval() throws Exception {
         Object result = fArg.eval();
         if (result instanceof Double) {
            return -(Double)result;
         } else {
            return -(Long)result;
         }
      }
   }
   
   static class UnaryPlusNode extends UnaryExpressionNode {

      UnaryPlusNode(ExpressionNode left) {
         super(left, left.fType);
      }

      @Override
      Object eval() throws Exception {
         return fArg.eval();
      }
   }
   
   static class ComplementNode extends UnaryExpressionNode {

      ComplementNode(ExpressionNode left) {
         super(left, left.fType);
      }

      @Override
      Object eval() throws Exception {
         return ~(Long)(fArg.eval());
      }
   }
   
   static class MultiplyNode extends BinaryExpressionNode {

      MultiplyNode(ExpressionNode left, ExpressionNode right) throws Exception {
         super(left,right,left.fType);
         if (left.fType != right.fType) {
            throw new Exception("Incompatible operand to '*'");
         }
         if ((left.fType != Type.Double) && (left.fType != Type.Long)) {
            throw new Exception("Illegal operand to '*'");
         }
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) {
            return (Double)leftOperand * (Double)rightOperand;
         } else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
            return (Long)leftOperand * (Long)rightOperand;
         } else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
            return (Long)leftOperand * (Double)rightOperand;
         } else {
            return (Double)leftOperand * (Long)rightOperand;
         }
      }
   }

   static class DivideNode extends BinaryExpressionNode {

      DivideNode(ExpressionNode left, ExpressionNode right) {
         super(left,right, left.fType);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         
         if ((leftOperand instanceof Double) && (rightOperand instanceof Double)) {
            Double right = (Double)rightOperand;
            if (right == 0.0) {
               return Double.POSITIVE_INFINITY;
            }
            return (Double)leftOperand / right;
         } else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
            Long right = (Long)rightOperand;
            if (right == 0) {
               return Double.POSITIVE_INFINITY;
            }
            return (Long)leftOperand / right;
         } else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
            Double right = (Double)rightOperand;
            if (right == 0.0) {
               return Double.POSITIVE_INFINITY;
            }
            return (Long)leftOperand / right;
         } else {
            Long right = (Long)rightOperand;
            if (right == 0) {
               return Double.POSITIVE_INFINITY;
            }
            return (Double)leftOperand / right;
         }
      }
   }

   static class ModuloNode extends BinaryExpressionNode {

      ModuloNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Long);
      }

      @Override
      Object eval() throws Exception {
         return (Long)fLeft.eval() % (Long)fRight.eval();
      }
   }

   static class AddNode extends BinaryExpressionNode {

      AddNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,left.fType);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         switch (fLeft.fType) {
         case Double:
            return (Double)leftOperand + (Double)rightOperand;
         case Long:
            return (Long)leftOperand + (Long)rightOperand;
         case String:
            return (String)leftOperand + (String)rightOperand;
         default:
            throw new Exception("Impossible type!");
         }
      }
   }

   static class SubtractNode extends BinaryExpressionNode {

      SubtractNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,left.fType);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         switch (fLeft.fType) {
         case Double:
            return (Double)leftOperand - (Double)rightOperand;
         case Long:
            return (Long)leftOperand - (Long)rightOperand;
         default:
            throw new Exception("Impossible type!");
         }
      }
   }

   static class LeftShiftNode extends BinaryExpressionNode {

      LeftShiftNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Long);
      }

      @Override
      Object eval() throws Exception {
         return (Long)fLeft.eval() << (Long)fRight.eval();
      }
   }

   static class RightShiftNode extends BinaryExpressionNode {

      RightShiftNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Long);
      }

      @Override
      Object eval() throws Exception {
         return (Long)fLeft.eval() >> (Long)fRight.eval();
      }
   }

   static class LessThanNode extends BinaryExpressionNode {

      LessThanNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         switch (fLeft.fType) {
         case Double:
            return (Double)leftOperand < (Double)rightOperand;
         case Long:
            return (Long)leftOperand < (Long)rightOperand;
         case String:
            return ((String)leftOperand).compareTo((String)rightOperand)<0;
         default:
            throw new Exception("Impossible type!");
         }
      }
   }

   static class LessThanOrEqualNode extends BinaryExpressionNode {

      LessThanOrEqualNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         switch (fLeft.fType) {
         case Double:
            return (Double)leftOperand <= (Double)rightOperand;
         case Long:
            return (Long)leftOperand <= (Long)rightOperand;
         case String:
            return ((String)leftOperand).compareTo((String)rightOperand)<=0;
         default:
            throw new Exception("Impossible type!");
         }
      }
   }

   static class GreaterThanNode extends BinaryExpressionNode {

      GreaterThanNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         switch (fLeft.fType) {
         case Double:
            return (Double)leftOperand > (Double)rightOperand;
         case Long:
            return (Long)leftOperand > (Long)rightOperand;
         case String:
            return ((String)leftOperand).compareTo((String)rightOperand)>0;
         default:
            throw new Exception("Impossible type!");
         }
      }
   }

   static class GreaterThanOrEqualNode extends BinaryExpressionNode {

      GreaterThanOrEqualNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         Object rightOperand = fRight.eval();
         switch (fLeft.fType) {
         case Double:
            return (Double)leftOperand >= (Double)rightOperand;
         case Long:
            return (Long)leftOperand >= (Long)rightOperand;
         case String:
            return ((String)leftOperand).compareTo((String)rightOperand)>=0;
         default:
            throw new Exception("Impossible type!");
         }
      }
   }

   static class EqualNode extends BinaryExpressionNode {

      EqualNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         if (fRight instanceof CommaListNode) {
            CommaListNode set = (CommaListNode) fRight;
            Visitor inSet = new Visitor() {
               Object  value = leftOperand;
               Boolean result = false;

               @Override
               void visit(ExpressionNode node) throws Exception {
                  if (node.eval().equals(value)) {
                     result = true;
                  }

               }

               @Override
               Object getResult() {
                  return result;
               }
            };
            set.forEach(inSet);
            return inSet.getResult();
         }
         else {
            Object rightOperand = fRight.eval();
            switch (fLeft.fType) {
            case Boolean:
               return (Boolean)leftOperand == (Boolean)rightOperand;
            case Double:
               return (Double)leftOperand == (Double)rightOperand;
            case Long:
               return (Long)leftOperand == (Long)rightOperand;
            case String:
               return ((String)leftOperand).compareTo((String)rightOperand) == 0;
            default:
               throw new Exception("Impossible type!");
            }
         }
      }
   }

   static class NotEqualNode extends BinaryExpressionNode {

      NotEqualNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
         if (fRight instanceof CommaListNode) {
            CommaListNode set = (CommaListNode) fRight;
            Visitor inSet = new Visitor() {
               Object  value = leftOperand;
               Boolean result = false;

               @Override
               void visit(ExpressionNode node) throws Exception {
                  if (node.eval().equals(value)) {
                     result = true;
                  }
               }

               @Override
               Object getResult() {
                  return result;
               }
            };
            set.forEach(inSet);
            return !(Boolean)(inSet.getResult());
         }
         else {
            Object rightOperand = fRight.eval();
            switch (fLeft.fType) {
            case Double:
               return (Double)leftOperand != (Double)rightOperand;
            case Long:
               return (Long)leftOperand != (Long)rightOperand;
            case String:
               return ((String)leftOperand).compareTo((String)rightOperand)!=0;
            default:
               throw new Exception("Impossible type!");
            }
         }
      }
   }

   static class BitAndNode extends BinaryExpressionNode {

      BitAndNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Long);
      }

      @Override
      Object eval() throws Exception {
         return (Long)fLeft.eval() & (Long)fRight.eval();
      }
   }

   static class BitOrNode extends BinaryExpressionNode {

      BitOrNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Long);
      }

      @Override
      Object eval() throws Exception {
         return (Long)fLeft.eval() | (Long)fRight.eval();
      }
   }

   static class BitXorNode extends BinaryExpressionNode {

      BitXorNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Long);
      }

      @Override
      Object eval() throws Exception {
         return (Long)fLeft.eval() ^ (Long)fRight.eval();
      }
   }

   static class LogicalAndNode extends BinaryExpressionNode {

      LogicalAndNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         return (Boolean)fLeft.eval() && (Boolean)fRight.eval();
      }

      @Override
      public ExpressionNode prune() throws Exception {
         fLeft  = fLeft.prune();
         
         if (fLeft.isConstant()) {
            if ((Boolean)fLeft.eval()) {
               // Node value is determined from right node alone
//               System.err.println("Pruning && (LHS=T) -> RHS=" + fRight);
               return fRight.prune();
            }
            // Node is always false
//            System.err.println("Pruning && (LHS=F) -> false, discarding RHS=" + fRight);
            return new BooleanNode(false);
         }
         fRight = fRight.prune();
         if (fRight.isConstant()) {
            if ((Boolean)fRight.eval()) {
               // Node value is determined from left node alone
//               System.err.println("Pruning && (RHS=T) -> LHS=" + fLeft);
               return fLeft;
            }
            // Node is always false
//            System.err.println("Pruning && (RHS=F) -> false, discarding LHS=" + fLeft);
            return new BooleanNode(false);
         }
         return this;
      }

   }

   static class LogicalOrNode extends BinaryExpressionNode {

      LogicalOrNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         return (Boolean)fLeft.eval() || (Boolean)fRight.eval();
      }

      @Override
      public ExpressionNode prune() throws Exception {
         fLeft  = fLeft.prune();
         
         if (fLeft.isConstant()) {
            if ((Boolean)fLeft.eval()) {
               // Node is always true
//               System.err.println("Pruning || (LHS=T) -> true, discarding RHS=" + fRight);
               return new BooleanNode(true);
            }
            // Node value is determined from right node alone
//            System.err.println("Pruning || (LHS=F) -> RHS=" + fRight);
            return fRight.prune();
         }
         fRight = fRight.prune();
         if (fRight.isConstant()) {
            if ((Boolean)fRight.eval()) {
               // Node is always true
//               System.err.println("Pruning || (RHS-T) -> true, discarding LHS=" + fLeft);
               return new BooleanNode(true);
            }
            // Node value is determined from left node alone
//            System.err.println("Pruning || (RHS=F) -> LHS=" + fLeft);
            return fLeft;
         }
         return this;
      }
   }

   static class TernaryNode extends BinaryExpressionNode {
      
      ExpressionNode fCondition;
      
      TernaryNode(ExpressionNode condition, ExpressionNode left, ExpressionNode right) {
         super(left,right,left.fType);
         fCondition = condition;
      }

      @Override
      Object eval() throws Exception {
         return ((Boolean)fCondition.eval())?fLeft.eval():fRight.eval();
      }

      @Override
      public ExpressionNode prune() throws Exception {
         fCondition  = fCondition.prune();
         
         if (fCondition.isConstant()) {
            return ((boolean) fCondition.eval())?fLeft.prune():fRight.prune();
         }
         fLeft       = fLeft.prune();
         fRight      = fRight.prune();
         return this;
      }

      @Override
      public void collectVars(ArrayList<Variable> variablesFound) throws Exception {
         super.collectVars(variablesFound);
         fCondition.collectVars(variablesFound);
      }

      @Override
      boolean isConstant() {
         return fCondition.isConstant() && super.isConstant();
      }
      
      @Override
      public String toString() {
         return this.getClass().getSimpleName()+"("+fCondition.toString()+"?"+fLeft.toString()+":"+fRight.toString()+","+fType+")";
      }

   }

//   static class SetNode extends ExpressionNode {
//      ExpressionNode[] fList;
//
//      SetNode(ExpressionNode[] expression) {
//         super(Type.List);
//         fList = expression;
//      }
//
//      @Override
//      Object eval() throws Exception {
//         Object[] result = new Object[fList.length];
//         for (int index=0; index<fList.length; index++) {
//            result[index] = fList[index].eval();
//         }
//         return result;
//      }
//
//      @Override
//      public void collectVars(ArrayList<Variable> variablesFound) {
//         for (ExpressionNode exp:fList) {
//            exp.collectVars(variablesFound);
//         }
//      }
//
//      @Override
//      public ExpressionNode prune() throws Exception {
//         for (int index=0; index<fList.length; index++) {
//            fList[index] = fList[index].prune();
//         }
//         return this;
//      }
//
//      @Override
//      public String toString() {
//         StringBuilder description = new StringBuilder();
//         description.append(this.getClass().getSimpleName()).append("(");
//         for (ExpressionNode exp:fList) {
//            description.append(exp.toString());
//         }
//         description.append(")");
//         return description.toString();
//      }
//
//      @Override
//      boolean isConstant() {
//         boolean isConstant = true;
//         for (int index=0; (index<fList.length)&&isConstant; index++) {
//            isConstant = isConstant && fList[index].isConstant();
//         }
//         return isConstant;
//      }
//
//   }
   
   static class CommaListNode extends ExpressionNode {
      ExpressionNode[] fList;
      
      CommaListNode(ExpressionNode[] expressions) {
         super(Type.List);
         fList = expressions;
      }
      
      /**
       * Get list of expressions
       * 
       * @return
       */
      public ExpressionNode[] getExpressionList() {
         return fList;
      }
      
      @Override
      Object eval() throws Exception {
         Object[] result = new Object[fList.length];
         for (int index=0; index<fList.length; index++) {
            result[index] = fList[index].eval();
         }
         return result;
      }

      @Override
      public void collectVars(ArrayList<Variable> variablesFound) throws Exception {
         for (ExpressionNode exp:fList) {
            exp.collectVars(variablesFound);
         }
      }
      
      @Override
      public ExpressionNode prune() throws Exception {
         for (int index=0; index<fList.length; index++) {
            fList[index] = fList[index].prune();
         }
         return this;
      }
      
      @Override
      public String toString() {
         StringBuilder description = new StringBuilder();
         description.append(this.getClass().getSimpleName()).append("(");
         for (ExpressionNode exp:fList) {
            description.append(exp.toString());
         }
         description.append(")");
         return description.toString();
      }

      @Override
      boolean isConstant() {
         boolean isConstant = true;
         for (int index=0; (index<fList.length)&&isConstant; index++) {
            isConstant = isConstant && fList[index].isConstant();
         }
         return isConstant;
      }

      public static abstract class Visitor {
         abstract void visit(ExpressionNode node) throws Exception ;

         Object getResult() {
            return null;
         }
         
      };
      
      /**
       * Visit each expression in list
       * 
       * @param visitor Visitor
       * @throws Exception
       */
      public void forEach(Visitor visitor) throws Exception {
         for (ExpressionNode node:fList) {
            visitor.visit(node);
         }
      }

   }

   /**
    * Class to accumulate variable changes
    */
   public static class VariableUpdateInfo {
      public Object   value        = null;
      public Status   status       = null;
      public String   origin       = null;
      public boolean  enable       = true;
      public boolean  doFullUpdate = false;
      public int      properties   = 0;
   };

   /** Variable provider for variable in expression */
   private final VariableProvider fVarProvider;
   
   /** Original expression */
   private final String   fExpressionStr;
      
   /** Parsed expressions */
   private ExpressionNode fExpression;
   
   /** Message associated with expression */
   private String         fMessage;
   
   /** Primary variable if provided */
   private Variable       fPrimaryVar;
   
   /** Indicates the expression is constant */
   private Boolean fIsConstant;
   
   /** Cached value for expression */
   private Object fCurrentValue;

   /** Disables caching of expression value i.e. expression is always re-evaluated when used */
   private boolean neverCacheValue = false;
   
   /** Listeners that need to be notified of expression changes */
   private ArrayList<IExpressionChangeListener> fListeners = new ArrayList<IExpressionChangeListener>();

   /** Variable used by expression */
   private ArrayList<Variable> fVariables;

   /** Controls operation of parser */
   private Mode fMode;

   /**
    * Wraps a constant value in a Expression node
    * 
    * @param constantValue Value to wrap
    * 
    * @return  Wrapped value
    * 
    * @throws Exception
    */
   static ExpressionNode wrapConstant(Object constantValue) throws Exception {
      
      if (constantValue instanceof Long) {
         return new LongConstantNode((Long)constantValue);
      }
      if (constantValue instanceof Double) {
         return new DoubleConstantNode((Double)constantValue);
      }
      if (constantValue instanceof String) {
         return new StringConstantNode((String)constantValue);
      }
      if (constantValue instanceof Boolean) {
         return new BooleanNode((Boolean)constantValue);
      }
      throw new Exception("Node not of expected type" + constantValue.getClass().toString());
   }
   
   /**
    * Create expression with optional message and primary variable from string of form:
    * "primaryVar#expression,message"
    * 
    * @param expression    Expression+message as string
    * @param fVarProvider  Provider for variables used in expression (mat be null if none)
    * @param mode          Mode of evaluation i.e. whether variable value or existence only is used
    * 
    * @throws Exception
    */
   public Expression(String expression, VariableProvider provider, Mode mode) throws Exception {
      fExpressionStr = expression;
      fVarProvider   = provider;
      fMode          = mode;
//      if (expression.contains("_irqCount+1")) {
//         System.err.println("Found it "+toString());
//      }
   }

   /**
    * Create expression with optional message and primary variable from string of form:
    * "primaryVar#expression,message"
    *
    * @param expression Expression+message as string
    * @param fVarProvider
    * @throws Exception
    */
   public Expression(String expression, VariableProvider provider) throws Exception {
      this(expression, provider, Mode.Dynamic);
   }
   
//   static String stripSpaces(String s) {
//      boolean stripped = false;
//      Character escape = null;
//      StringBuilder sb = new StringBuilder();
//      for (int index=0; index<s.length(); index++) {
//
//         if ("'\"".indexOf(s.charAt(index)) >= 0) {
//            escape = s.charAt(index);
//         }
//      }
//      if (stripped) {
//         return sb.toString();
//      }
//      return s;
//   }
   
   private void prelim() throws Exception {
//      if (fExpressionStr.matches(".*\\|\\|\\(ftm_cnsc_mode\\[0.*")) {
//         System.err.println("Found it prelim("+fExpressionStr+")");
//      }
      
      String expression = fExpressionStr;

      // Check for expression#primaryVar#message
      String parts[] = expression.split("#");
      if (parts.length>1) {
         expression  = parts[0].trim();
         String primaryVarStr = parts[1].trim();
         if (!primaryVarStr.isBlank()) {
            fPrimaryVar = fVarProvider.getVariable(primaryVarStr);
         }
         if (parts.length>2) {
            fMessage = parts[2].trim();
         }
      }
      
      // Parse expression
      ExpressionParser ep = new ExpressionParser(this, fVarProvider, fMode);
      fExpression = ep.parseExpression(parts[0].trim());
      
      // Prune constant nodes
      fExpression = fExpression.prune();
      
      if (fMode == Mode.Dynamic) {
         
         // Collect variables
         fVariables = new ArrayList<Variable>();
         fExpression.collectVars(fVariables);
         
         // Listen to variables
         for (Variable var:fVariables) {
            var.addListener(this);
         }
         // Update primary variable
         if ((fPrimaryVar == null) && (fVariables.size() == 1)) {
            fPrimaryVar = fVariables.get(0);
         }
         fVariables.remove(fPrimaryVar);
      }

   }

   /**
    * Forced evaluation of expression
    * 
    * @return
    * @throws Exception
    */
   private Object evaluate() throws Exception {
      try {
         if (fExpression == null) {
            // Parse expression
            prelim();
         }
         return fExpression.eval();
      } catch (Exception e) {
         throw new Exception("Failed to evaluate expression '" + fExpressionStr + "'", e);
      }
   }

   /**
    * Get primary variable associated with string (for origin etc)
    * 
    * @return Primary variable associated with express (if available)
    */
   public Variable getPrimaryVar() {
      return fPrimaryVar;
   }

   /**
    * Get message for origin purposes when expression used as ref=...
    * If no explicit message available then a message is constructed from the expression.
    * 
    * @param choiceVar If expression is associated with a Choice then this is used to add information to message
    * 
    * @return
    */
   public String getOriginMessage() {
      
      if (fMessage != null) {
         return fMessage;
      }
      StringBuilder sb = new StringBuilder();
      
      
      if (fPrimaryVar != null) {
         // Get status and enable from primary variable
         if (fPrimaryVar.isNamedClock()) {
            // Don't propagate origin past a clock variable
            sb.append(fPrimaryVar.getDescription());
         }
         else {
            sb.append(fPrimaryVar.getOrigin());
         }
         if (fVariables.size()>0) {
            sb.append("\nModified by ");
         }
      }
      else {
         if ((fVariables != null) && fVariables.size()>0) {
            sb.append("Calculated from ");
         }
         else {
            if(fExpression.fType == Type.DisabledValue) {
               sb.append("Disabled");
            }
         }
      }
      
      boolean commaNeeded = false;
      if (fVariables.size()>0) {
         for(Variable var:fVariables) {
            if (commaNeeded) {
               sb.append(", ");
            }
            sb.append(var.getName());
            commaNeeded = true;
         }
      }
      return sb.toString();
   }

   /**
    * Get message associated with expression
    * If no explicit message available then a message is constructed from the expression.
    * 
    * @param defaultLeader
    * 
    * @return
    */
   public String getMessage(String defaultLeader) {
      if (fMessage != null) {
         return fMessage;
      }
      StringBuilder sb = new StringBuilder();
      sb.append(defaultLeader);
      
      Variable primaryVariable = getPrimaryVar();
      if (primaryVariable != null) {
         sb.append(primaryVariable.getName());
      }
      
      boolean commaNeeded = false;
      if ((fVariables!=null) && (fVariables.size()>1)) {
         if (primaryVariable != null) {
            sb.append(" modified by ");
         }
         for(Variable var:fVariables) {
            if (commaNeeded) {
               sb.append(", ");
            }
            sb.append(var.getName());
            commaNeeded = true;
         }
      }
      return sb.toString();
   }

   /**
    * Check if expression is constant e.g. "Disabled"
    * 
    * @return true if constant
    * @throws Exception
    */
   public boolean isConstant() throws Exception {
      
      if (fIsConstant == null) {
         if (fExpression == null) {
            prelim();
         }
         fIsConstant = fExpression.isConstant();
      }
      return fIsConstant;
   }

   /**
    * Get value of expression
    * 
    * @return Current value of expression
    * 
    * @throws Exception
    */
   public Object getValue() throws Exception {
      InitPhase initPhase = fVarProvider.getDeviceInfo().getInitialisationPhase();
      if ((fCurrentValue == null) || neverCacheValue || (initPhase.isEarlierThan( InitPhase.VariableAndGuiPropagationAllowed))) {
         // Always evaluate if needed or when loading settings
         fCurrentValue = evaluate();
      }
      return fCurrentValue;
   }

   /**
    * Get value of expression as String
    * 
    * @return Current value of expression
    * 
    * @throws Exception
    */
   public String getValueAsString() throws Exception {
      return castResult(String.class);
   }

   /**
    * Get value of expression as Boolean
    * 
    * @return Current value of expression
    * 
    * @throws Exception
    */
   public Boolean getValueAsBoolean() throws Exception {
      return castResult(Boolean.class);
   }

   /**
    * Get value of expression as Long
    * 
    * @return Current value of expression
    * 
    * @throws Exception
    */
   public Long getValueAsLong() throws Exception {
      return castResult(Long.class);
   }

   /**
    * Get value of expression as Double
    * 
    * @return Current value of expression
    * 
    * @throws Exception
    */
   public Double getValueAsDouble() throws Exception {
      return castResult(Double.class);
   }

   /*
    * Static methods
    */
   /**
    * Get value of expression<br>
    * This constructs an expression and immediately evaluates it.
    * 
    * @return Current value of expression
    * 
    * @throws Exception
    */
   public static Object getValue(String expression, VariableProvider provider) throws Exception {
      return new Expression(expression, provider, Mode.EvaluateImmediate).getValue();
   }

   @SuppressWarnings("unchecked")
   private static <T> T castResult(Class<T> toClass, String expression, VariableProvider provider) throws Exception {
      Object res = getValue(expression, provider);
      if (!toClass.isInstance(res)) {
         throw new Exception("Expected "+toClass.getSimpleName()+" result for expression '"+expression+"'");
      }
      return (T) res;
   }
   
   @SuppressWarnings("unchecked")
   private <T> T castResult(Class<T> toClass) throws Exception {
      Object res = getValue();
      if ((toClass == Double.class) && (res instanceof Long)) {
         Double t = ((Long)res).doubleValue();
         return (T)t;
      }
      if (!toClass.isAssignableFrom(res.getClass())) {
         throw new Exception("Expected "+toClass.getSimpleName()+" result for expression '"+fExpressionStr+"'");
      }
      return (T) res;
   }
   
   /**
    * Get value of expression as String<br>
    * This constructs an expression and immediately evaluates it.
    * 
    * @return
    * @throws Exception
    */
   public static String getValueAsString(String expression, VariableProvider provider) throws Exception {
      return castResult(String.class, expression, provider);
   }

   /**
    * Get value of expression as Boolean<br>
    * This constructs an expression and immediately evaluates it.
    * 
    * @return
    * @throws Exception
    */
   public static Boolean getValueAsBoolean(String expression, VariableProvider provider) throws Exception {
      return castResult(Boolean.class, expression, provider);
   }

   /**
    * Get value of expression as Long<br>
    * This constructs an expression and immediately evaluates it.
    * 
    * @return
    * @throws Exception
    */
   public static Long getValueAsLong(String expression, VariableProvider provider) throws Exception {
      return castResult(Long.class, expression, provider);
   }

   /**
    * Get value of expression as Double<br>
    * This constructs an expression and immediately evaluates it.
    * 
    * @return
    * @throws Exception
    */
   public static Double getValueAsDouble(String expression, VariableProvider provider) throws Exception {
      return castResult(Double.class, expression, provider);
   }

   /**
    * Get value of expression as Boolean<br>
    * This constructs an expression and immediately evaluates it.<br>
    * Variable values are <b>not used</b> unless prefixed with <b>@</b>.<br>
    * If a variable exists then it evaluates as <b>true</b>, otherwise <b>false</b>.<br>
    * An empty expression evaluates as <b>true</b>;
    * 
    * @return
    * @throws Exception
    */
   public static Boolean checkCondition(String expression, VariableProvider provider) throws Exception {
      if (expression == null) {
         return true;
      }
//      if (expression.contains("/I2S0/_irqCount")) {
//         System.err.println("Found it "+expression);
//      }
      Expression exp = new Expression(expression, provider, Mode.CheckIdentifierExistance);
      try {
         return (Boolean) exp.getValue();
      } catch (ClassCastException e) {
         throw new Exception("Expected boolean result for condition '"+expression+"'", e);
      }
   }

   public void removeAllListeners() {
      fListeners = new ArrayList<IExpressionChangeListener>();
   }

   public void removeListener(IExpressionChangeListener listener) {
      fListeners.remove(listener);
   }

   public void notifyExpressionChangeListeners() {
      for (IExpressionChangeListener listener:fListeners) {
         listener.expressionChanged(this);
      }
   }

   /**
    * Add listener for expression changes<br>
    * Only adds listener if the expression is not constant
    * 
    * @param listener Listener to add<br>
    *        Adding the same listener multiple times is ignored
    * 
    * @return <b>true</b> if listener added
    */
   public boolean addListener(IExpressionChangeListener listener) throws Exception {
      try {
         if (!isConstant()) {
            if (!fListeners.contains(listener)) {
               fListeners.add(listener);
               return true;
            }
         }
      } catch (Exception e) {
         throw new Exception("Failed to add listeners for expression " + fExpressionStr, e);
      }
      return false;
   }

   /**
    * Get original expression string
    * 
    * @return
    */
   public String getExpressionStr() {
      return fExpressionStr;
   }

   @Override
   public String toString() {
      return "Expression(\""+fExpressionStr+"\", "+fMode+", "+fVarProvider+")";
   }

   @Override
   public void modelElementChanged(ObservableModelInterface observableModel, int properties) {
      
//      if (fExpressionStr.matches(".*ftm_cnsc_mode\\[0.*")) {
//         System.err.println("Found it modelElementChanged"+fExpressionStr+")");
//      }
      try {
         Object newValue = evaluate();
         if (newValue == fCurrentValue) {
            // No change to propagate
            return;
         }
         boolean changed = true;
         if ((fCurrentValue != null) && (fExpression.fType == Type.Double)) {
            if (Double.isNaN((Double)fCurrentValue)&&Double.isNaN((Double)newValue)) {
               // Ignore NAN => NAN
               changed = false;
            }
            if (Double.isInfinite((Double)fCurrentValue)&&Double.isInfinite((Double)newValue)) {
               // Ignore Infinity => Infinity etc.
               changed = false;
            }
            if (Double.isFinite((Double)fCurrentValue)&&Double.isFinite((Double)newValue)) {
               Double change = ((Double)newValue-(Double)fCurrentValue)/(Double)fCurrentValue;
               changed =  (Math.abs(change)>0.000000001);
            }
         }
         if (changed) {
            fCurrentValue = newValue;
//            notifyIfNeeded();
            notifyExpressionChangeListeners();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Causes the expression to be re-evaluated on every use
    */
   public void setNeverCacheValue() {
      neverCacheValue = true;
   }

   /**
    * Checks if the expression should be re-evaluated on every use
    */
   public boolean isNeverCached() {
      return neverCacheValue;
   }

   public static ExpressionNode createConstantNode(Object res) throws Exception {
      if (res instanceof String) {
         return new StringConstantNode((String)res);
      }
      if (res instanceof Long) {
         return new LongConstantNode((Long)res);
      }
      if (res instanceof Double) {
         return new DoubleConstantNode((Double)res);
      }
      if (res instanceof Boolean) {
         return new BooleanConstantNode((Boolean)res);
      }
      throw new Exception("Unexpected constant type");
   }

}
