package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.PinListExpansion;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.parsers.ExpressionParser.Mode;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class Expression implements IModelChangeListener {

   public enum Type {
      Double, Long, String, Boolean, DisabledValue, List
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
       * Collect a list of the variables used in expression
       * 
       * @param variablesFound  List to add found variables to
       */
      public void collectVars(ArrayList<Variable> variablesFound) {
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
      public void collectVars(ArrayList<Variable> variablesFound) {
         fLeft.collectVars(variablesFound);
         fRight.collectVars(variablesFound);
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
      public String toString() {
         return this.getClass().getSimpleName()+"("+fLeft.toString()+","+fRight.toString()+","+fType+")";
      }

      @Override
      boolean isConstant() {
         return fLeft.isConstant() && fRight.isConstant();
      }
      
   }

   static abstract class UnaryExpressionNode extends ExpressionNode {

      ExpressionNode fArg;

      UnaryExpressionNode(ExpressionNode left, Type type) {
         super(type);
         fArg  = left;
      }
      
      @Override
      public void collectVars(ArrayList<Variable> variablesFound) {
         fArg.collectVars(variablesFound);
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
      public void collectVars(ArrayList<Variable> variablesFound) {
         fArg.collectVars(variablesFound);
      }
      
      @Override
      public ExpressionNode prune() throws Exception {
         fArg  = fArg.prune();
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
      private final String fVarName;
      
      /** Expression for index */
      private final Expression fIndex;

//    private final Variable[]       fVars = new Variable[MAX_DIMENSION];

//      private       Object           fValue = null;
      

      VariableNode(Expression owner, String varName, Type type, Modifier modifier, Expression index) {
         super(type);
         
         fOwner      = owner;
         fVarName    = varName;
         fIndex      = index;
         fModifier   = modifier;
         if (index != null) {
            index.addListener(this);
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
      public static VariableNode create(Expression owner, String varName, String modifier, Expression index) throws Exception {
         
         String name = varName;
         if (index != null) {
            name = name+"["+index.getValueAsLong()+"]";
         }
         Variable var = owner.fVarProvider.getVariable(name);
               
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
         return new VariableNode(owner, varName, Type.String, null, index);
      }
      
      private Variable getVar() throws Exception {
         Variable var = fVar;
//         if (var == null) {
            String name = fVarName;
            if (fIndex != null) {
               name = name+"["+fIndex.getValueAsLong()+"]";
            }
            var = fOwner.fVarProvider.getVariable(name);
            
//            // If index is unchanging then we can cache the variable
//            if ((fIndex == null)||fIndex.isConstant()) {
//               fVar = var;
//            }
//         }
         return var;
      }
      
      @Override
      public void expressionChanged(Expression expression) {
         // Index changed indicate var is stale
         fVar = null;
         fOwner.notifyExpressionChangeListeners();
      }

      @Override
      public void collectVars(ArrayList<Variable> variablesFound) {
         try {
            variablesFound.add(getVar());
         } catch (Exception e) {
         }
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
         if (isConstant()) {
            if (fIndex != null) {
               System.err.println("Pruning with index " + fIndex.getExpressionStr());
            }
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

   static class DoubleNode extends ExpressionNode {

      final Double fValue;

      DoubleNode(double value) {
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

   static class LongNode extends ExpressionNode {

      final Long fValue;

      LongNode(long value) {
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

   static class StringNode extends ExpressionNode {

      final String fValue;

      StringNode(String value) {
         super(Type.String);
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
   
   static class ExpandPinListNode extends UnaryExpressionNode {

      /**
       * Cast a Long ExpressionNode to a single character String ExpressionNode e.g. 30 => "0"
       * 
       * @param arg
       * @throws Exception
       */
      ExpandPinListNode(ExpressionNode arg) throws Exception {
         super(arg, Type.String);
         if (arg.fType != Expression.Type.String) {
            throw new Exception("Expression has wrong type for expansion");
         }
      }

      @Override
      Object eval() throws Exception {
         String s = (String) fArg.eval();
         return String.join(",", PinListExpansion.expandPinList(s, ","));
      }
   }
   
//   static class CastToVariableNode extends UnaryExpressionNode {
//
//      private final VariableProvider fProvider;
//
//      /**
//       * Cast a String ExpressionNode to a Variable ExpressionNode
//       *
//       * @param arg
//       * @throws Exception
//       */
//      CastToVariableNode(VariableProvider provider, ExpressionNode arg) throws Exception {
//         super(arg, Type.String);
//         if (arg.fType != Expression.Type.String) {
//            throw new Exception("Expression cannot be used as name of variable");
//         }
//         fProvider = provider;
//      }
//
//      @Override
//      Object eval() throws Exception {
//         String varName = (String) fArg.eval();
//         Variable var = fProvider.getVariable(varName);
//         return var.getSubstitutionValue();
//      }
//   }
//
   static class MinusNode extends UnaryExpressionNode {

      MinusNode(ExpressionNode left) {
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
   
   static class PlusNode extends UnaryExpressionNode {

      PlusNode(ExpressionNode left) {
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
            return (Double)leftOperand / (Double)rightOperand;
         } else if ((leftOperand instanceof Long) && (rightOperand instanceof Long)) {
            return (Long)leftOperand / (Long)rightOperand;
         } else if ((leftOperand instanceof Long) && (rightOperand instanceof Double)) {
            return (Long)leftOperand / (Double)rightOperand;
         } else {
            return (Double)leftOperand / (Long)rightOperand;
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

   static class NotEqualNode extends BinaryExpressionNode {

      NotEqualNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,Type.Boolean);
      }

      @Override
      Object eval() throws Exception {
         Object leftOperand  = fLeft.eval();
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
         fRight = fRight.prune();
         
         if (fLeft.isConstant()) {
            if ((Boolean)fLeft.eval()) {
               // Node value is determined from right node alone
               return fRight;
            }
            // Node is always false
            return new BooleanNode(false);
         }
         if (fRight.isConstant()) {
            if ((Boolean)fRight.eval()) {
               // Node value is determined from left node alone
               return fLeft;
            }
            // Node is always false
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
         fRight = fRight.prune();
         
         if (fLeft.isConstant()) {
            if ((Boolean)fLeft.eval()) {
               // Node is always true
               return new BooleanNode(true);
            }
            // Node value is determined from right node alone
            return fRight;
         }
         if (fRight.isConstant()) {
            if ((Boolean)fRight.eval()) {
               // Node is always true
               return new BooleanNode(true);
            }
            // Node value is determined from left node alone
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
      public void collectVars(ArrayList<Variable> variablesFound) {
         super.collectVars(variablesFound);
         fCondition.collectVars(variablesFound);
      }
      
      @Override
      public ExpressionNode prune() throws Exception {
         fCondition  = fCondition.prune();
         fLeft       = fLeft.prune();
         fRight      = fRight.prune();
         
         if (fCondition.isConstant() && fLeft.isConstant() && fRight.isConstant()) {
            return wrapConstant(eval());
         }
         return this;
      }
      @Override
      public String toString() {
         return this.getClass().getSimpleName()+"("+fCondition.toString()+"?"+fLeft.toString()+":"+fRight.toString()+","+fType+")";
      }

   }

   static class CommaListNode extends ExpressionNode {
      ExpressionNode[] fList;
      
      CommaListNode(ExpressionNode[] expressions) {
         super(Type.List);
         fList = expressions;
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
      public void collectVars(ArrayList<Variable> variablesFound) {
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

   }

   /**
    * Class to accumulate variable changes
    */
   public static class VariableUpdateInfo {
      public Object  value   = null;
      public Status  status  = null;
      public String  origin  = null;
      public boolean enable  = true;
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
   
   private Object fCurrentValue;

   private ArrayList<IExpressionChangeListener> fListeners = new ArrayList<IExpressionChangeListener>();

   private ArrayList<Variable> fVariables;

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
         return new LongNode((Long)constantValue);
      }
      if (constantValue instanceof Double) {
         return new DoubleNode((Double)constantValue);
      }
      if (constantValue instanceof String) {
         return new StringNode((String)constantValue);
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
    * @param expression Expression+message as string
    * @param fVarProvider
    * @throws Exception
    */
   public Expression(String expression, VariableProvider provider) throws Exception {
      fExpressionStr = expression;
      fVarProvider   = provider;
      fMode          = ExpressionParser.Mode.Construct;
   }

   /**
    * Create expression with optional message and primary variable from string of form:
    * "primaryVar#expression,message"
    * 
    * @param expression Expression+message as string
    * @param fVarProvider
    * @throws Exception
    */
   public Expression(String expression, VariableProvider provider, Mode mode) throws Exception {
      fExpressionStr = expression;
      fVarProvider   = provider;
      fMode          = mode;
   }

   private void prelim() throws Exception {
//      if (this.fExpressionStr.contains("ACMP0_IN%i")) {
//         System.err.println("Found it ");
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
      }
      if (parts.length>2) {
         fMessage = parts[2].trim();
      }
      
      ExpressionParser ep = new ExpressionParser(this, fVarProvider, fMode);
      fExpression = ep.parseExpression(parts[0].trim());
      fExpression = fExpression.prune();
      fVariables = ep.getCollectedVariables();
      if ((fPrimaryVar == null) && (fVariables.size() == 1)) {
         fPrimaryVar = fVariables.get(0);
      }
      fVariables.remove(fPrimaryVar);
   }

   /**
    * Forced evaluation of expression
    * 
    * @return
    * @throws Exception
    */
   private Object evaluate() throws Exception {
      if (fExpression == null) {
         prelim();
      }
      return fExpression.eval();
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
         if (fVariables.size()>0) {
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
      if (fExpression == null) {
         prelim();
      }
      return fExpression.isConstant();
   }

   /**
    * Get value of expression
    * 
    * @return Current value
    * @throws Exception
    */
   public Object getValue() throws Exception {
      if (fCurrentValue == null) {
         fCurrentValue = evaluate();
      }
      return fCurrentValue;
   }

   /**
    * Get value of expression as String
    * 
    * @return
    * @throws Exception
    */
   public String getValueAsString() throws Exception {
      return (String)getValue();
   }

   /**
    * Get value of expression as Boolean
    * 
    * @return
    * @throws Exception
    */
   public Boolean getValueAsBoolean() throws Exception {
      return (Boolean)getValue();
   }

   /**
    * Get value of expression as Long
    * 
    * @return
    * @throws Exception
    */
   public Long getValueAsLong() throws Exception {
      return (Long)getValue();
   }

   /**
    * Get value of expression as Double
    * 
    * @return
    * @throws Exception
    */
   public Double getValueAsDouble() throws Exception {
      return (Double)getValue();
   }

   /**
    * Get value of expression
    * 
    * @return
    * @throws Exception
    */
   public static Object getValue(String expression, VariableProvider provider) throws Exception {
      Expression exp = new Expression(expression, provider, Mode.EvaluateFully);
      return exp.getValue();
   }

   /**
    * Get value of expression as String
    * 
    * @return
    * @throws Exception
    */
   public static String getValueAsString(String expression, VariableProvider provider) throws Exception {
      Expression exp = new Expression(expression, provider, Mode.EvaluateFully);
      return (String) exp.getValue();
   }

   /**
    * Get value of expression as Boolean
    * 
    * @return
    * @throws Exception
    */
   public static Boolean getValueAsBoolean(String expression, VariableProvider provider) throws Exception {
      Expression exp = new Expression(expression, provider, Mode.EvaluateFully);
      return (Boolean) exp.getValue();
   }

   /**
    * Get value of expression as Long
    * 
    * @return
    * @throws Exception
    */
   public static Long getValueAsLong(String expression, VariableProvider provider) throws Exception {
      Expression exp = new Expression(expression, provider, Mode.EvaluateFully);
      return (Long) exp.getValue();
   }

   /**
    * Get value of expression as Double
    * 
    * @return
    * @throws Exception
    */
   public static Double getValueAsDouble(String expression, VariableProvider provider) throws Exception {
      Expression exp = new Expression(expression, provider, Mode.EvaluateFully);
      return (Double) exp.getValue();
   }

   /**
    * Get value of expression as Boolean<br>
    * Variable values are not used.  If a variable exists then it evaluates as <b>true</b>, otherwise <b>false</b>.<br>
    * An empty expression evaluates as <b>true</b>;
    * 
    * @return
    * @throws Exception
    */
   public static Boolean checkCondition(String expression, VariableProvider provider) throws Exception {
      if (expression == null) {
         return true;
      }
//      if (expression.contains("mcg_sc_fcrdiv")) {
//         System.err.println("Found it " + expression);
//      }
      Expression exp = new Expression(expression, provider, Mode.CheckIdentifierExistance);
      return (Boolean) exp.getValue();
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

//   static class Pair {
//      final IExpressionChangeListener listener;
//      final Expression                expression;
//
//      Pair(IExpressionChangeListener listener, Expression expression) {
//         this.listener   = listener;
//         this.expression = expression;
//      }
//   }
//
//   static ConcurrentLinkedQueue<Pair> listenersBeingNotfied =
//         new ConcurrentLinkedQueue<Pair>();
//
//   static boolean queueIsFree = true;
//
//   enum QueueAction {Obtain, Release};
//
//   /**
//    *
//    * @param request
//    *
//    * @return true if queue ownership obtained, false otherwise
//    */
//   static synchronized boolean changeQueueOwnership(QueueAction action) {
//      switch (action) {
//      case Obtain:
//         if (queueIsFree) {
//            queueIsFree = false;
//            return true;
//         }
//         return false;
//      case Release:
//         queueIsFree = true;
//         return false;
//      }
//      return false;
//   }
//
//   void notifyIfNeeded() {
//      // Add to listeners needing notification
//      for (IExpressionChangeListener x:fListeners) {
//         listenersBeingNotfied.add(new Pair(x, this));
//      }
//      // Check if we need to do the notification
//      if (changeQueueOwnership(QueueAction.Obtain)) {
//         do {
//            Pair item = listenersBeingNotfied.poll();
//            if (item == null) {
//               break;
//            }
//            try {
//               item.listener.expressionChanged(item.expression);
//            } catch (Exception e) {
//               e.printStackTrace();
//            }
//         } while(true);
//         changeQueueOwnership(QueueAction.Release);
//      }
//   }
   
   
   @Override
   public void modelElementChanged(ObservableModel observableModel) {
//      if (fExpressionStr.contains("(/SMC/smc_pmctrl_runm[1]==VLPR)")) {
//         System.err.println("Found it");
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

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
   }
   
   /**
    * Add listener for expression changes
    * Only adds listener if the expression is dynamic
    * 
    * @param listener Listener to add<br>
    *        Adding the same listener multiple times is ignored
    * 
    * @return <b>true</b> if listener added
    */
   public boolean addListener(IExpressionChangeListener listener) {
      try {
         if (!isConstant()) {
            if (!fListeners.contains(listener)) {
               fListeners.add(listener);
               return true;
            }
         }
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      return false;
   }

   /**
    * Get original expression
    * 
    * @return
    */
   public String getExpressionStr() {
      return fExpressionStr;
   }

   @Override
   public String toString() {
      return "Expression("+fExpressionStr+")";
   }

   public static Object evaluate(String expressionString, VariableProvider provider) throws Exception {
      Expression exp = new Expression(expressionString, provider, Mode.EvaluateFully);
      return exp.getValue();
   }

}
