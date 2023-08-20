package net.sourceforge.usbdm.deviceEditor.parsers;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceData;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class Expression implements IModelChangeListener {

   public enum Type {
      Double, Long, String, Boolean, DisabledValue,
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
   }

   static abstract class UnaryExpressionNode extends ExpressionNode {

      ExpressionNode fLeft;

      UnaryExpressionNode(ExpressionNode left, Type type) {
         super(type);
         fLeft  = left;
      }
      
      @Override
      public void collectVars(ArrayList<Variable> variablesFound) {
         fLeft.collectVars(variablesFound);
      }
      
      @Override
      public ExpressionNode prune() throws Exception {
         fLeft  = fLeft.prune();
         if (fLeft.isConstant()) {
            return wrapConstant(eval());
         }
         return this;
      }
   }

   static class VariableNode extends ExpressionNode {

      static enum Modifier {
         Value, Name, Code, Enum
      };

      final Variable fVar;
      final Modifier fModifier;

      VariableNode(Variable var, Modifier modifier, Type type) {
         super(type);
         fVar = var;
         fModifier = modifier;
      }

      @Override
      Object eval() throws Exception {
         if (fModifier != null) {
            ChoiceVariable cv = (ChoiceVariable)fVar;
            ChoiceData choiceData = cv.getCurrentChoice();
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
            }
         }
         else if (fVar instanceof ChoiceVariable) {
            return fVar.getValueAsLong();
         }
         if (fVar instanceof BooleanVariable) {
            return fVar.getValueAsBoolean();
         }
         if (fVar instanceof LongVariable) {
            return fVar.getValueAsLong();
         }
         if (fVar instanceof DoubleVariable) {
            return fVar.getValueAsDouble();
         }
         // Default to treating as string
         return fVar.getValueAsString();
      }

      @Override
      public void collectVars(ArrayList<Variable> variablesFound) {
         variablesFound.add(fVar);
      }

      @Override
      boolean isConstant() {
         return fVar.isConstant();
      }

      @Override
      public ExpressionNode prune() throws Exception {
         if (fVar.isConstant()) {
            return wrapConstant(eval());
         }
         return this;
      }
      

      @Override
      public String toString() {
         return this.getClass().getSimpleName()+"("+fVar.getName()+", "+fType+")";
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
         return !(Boolean)fLeft.eval();
      }
   }
   
   static class LongToDoubleNode extends UnaryExpressionNode {

      LongToDoubleNode(ExpressionNode left) {
         super(left, Type.Double);
      }

      @Override
      Object eval() throws Exception {
         return (double)(Long)fLeft.eval();
      }
   }
   
   static class MinusNode extends UnaryExpressionNode {

      MinusNode(ExpressionNode left) {
         super(left, left.fType);
      }

      @Override
      Object eval() throws Exception {
         Object result = fLeft.eval();
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
         return fLeft.eval();
      }
   }
   
   static class ComplementNode extends UnaryExpressionNode {

      ComplementNode(ExpressionNode left) {
         super(left, left.fType);
      }

      @Override
      Object eval() throws Exception {
         return ~(Long)(fLeft.eval());
      }
   }
   
   static class MultiplyNode extends BinaryExpressionNode {

      MultiplyNode(ExpressionNode left, ExpressionNode right) {
         super(left,right,left.fType);
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
      throw new Exception("Node not of expected type");
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
   }

   private void prelim() throws Exception {
      
      String expression = fExpressionStr;
      String parts[] = expression.split("#");
      if (parts.length>1) {
         String primaryVarStr = parts[0].trim();
         fPrimaryVar = fVarProvider.getVariable(primaryVarStr);
         expression  = parts[1];
      }
      parts = expression.split(",");
      if (parts.length>1) {
         fMessage = parts[1].trim();
      }
      else {
         fMessage = null;
      }
      
      ExpressionParser ep = new ExpressionParser(this, fVarProvider, ExpressionParser.Mode.Construct);
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
      if (fVariables.size()>1) {
         sb.append(" modified by ");
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
    * Get current value of expression
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
   
   public String getValueAsString() throws Exception {
      return (String)getValue();
   }

   public Boolean getValueAsBoolean() throws Exception {
      return (Boolean)getValue();
   }

   public Long getValueAsLong() throws Exception {
      return (Long)getValue();
   }

   public Double getValueAsDouble() throws Exception {
      return (Double)getValue();
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

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
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
            notifyExpressionChangeListeners();
         }
      } catch (Exception e) {
         // TODO Auto-generated catch block
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


}
