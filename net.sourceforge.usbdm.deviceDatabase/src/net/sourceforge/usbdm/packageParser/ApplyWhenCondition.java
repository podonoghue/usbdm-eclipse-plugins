package net.sourceforge.usbdm.packageParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Button;

import net.sourceforge.usbdm.deviceDatabase.Device;

public class ApplyWhenCondition {
   public enum Type {deviceNameIs, deviceFamilyIs, deviceSubfamilyIs, 
                     deviceNameMatches, deviceFamilyMatches, deviceSubfamilyMatches,
                     hardwareIs, hardwareMatches, 
                     variableRef,
                     preclusion, requirement,
                     or, and, not, emptyTrue};
   public enum Condition {
      isDefined,
      isTrue,
      lessThan,
      lessThanOrEqual,
      equal,
      greaterThan,
      greaterThanOrEqual,
      matches,
   };
   protected ApplyWhenCondition.Type       fType;
   protected ArrayList<ApplyWhenCondition> fOperands;
   protected String                        fValue;
   protected String                        fVariableName;
   protected boolean                       fDefaultValue;
   protected Condition                     fCondition;
   private   boolean                       fVerbose;
           
   public static ApplyWhenCondition trueCondition  = new ApplyWhenCondition();
   
   protected ApplyWhenCondition() {
      fType         = Type.emptyTrue;
      fDefaultValue = true;
      fValue        = "true";
   }
   
   public ApplyWhenCondition(ApplyWhenCondition.Type operator, ArrayList<ApplyWhenCondition> operands) 
         throws Exception {
      fType = operator;
      fOperands = operands;
      if (operator == Type.not) {
         if (operands.size() > 1) {
            throw new Exception("ApplyWhenCondition(operator, operands) - Too many operands for NOT operation");
         }
      }
      else if ((operator != Type.and) && (operator != Type.or)) {
         throw new Exception("ApplyWhenCondition(operator, operands) - Must be NOT, OR or AND operator");
      }
   }
   public ApplyWhenCondition(ApplyWhenCondition.Type operator, String value) throws Exception {
      fType = operator;
      fValue = value;
      if ((operator != Type.deviceNameIs) && 
          (operator != Type.deviceFamilyIs) && 
          (operator != Type.deviceSubfamilyIs) &&
          (operator != Type.deviceNameMatches) && 
          (operator != Type.deviceFamilyMatches) && 
          (operator != Type.deviceSubfamilyMatches) &&
          (operator != Type.hardwareIs) && 
          (operator != Type.hardwareMatches) && 
          (operator != Type.requirement) && 
          (operator != Type.preclusion) && 
          (operator != Type.hardwareIs)
          ) {
         throw new Exception("ApplyWhenCondition(operator, value) - Must be unary type operator");
      }
   }
   /**
    * Constructs a condition that evaluates a variable
    * 
    * @param operator      must be Type.variableIs
    * @param variable      Variable to check
    * @param defaultValue  Default value for condition if variable not found
    * @param condition     Indicates condition to check  <,<=,>,>=,==
    * @param value         Value to compare variable to 
    * 
    * @throws Exception
    */
   public ApplyWhenCondition(
         Type              operator, 
         String            variable, 
         boolean           defaultValue,
         Condition         condition,
         String            value) throws Exception {
      fType          = operator;
      fVariableName  = variable;
      fDefaultValue  = defaultValue;
      fCondition     = condition;
      fValue         = value;
      if (operator != Type.variableRef) {
         throw new Exception("ApplyWhenCondition(operator, value) - Must be 'variableRef' type");
      }
      if ((fCondition != Condition.isTrue) && (fCondition != Condition.isDefined) && (fValue == null)) {
         throw new Exception("ApplyWhenCondition must have value unless \'isTrue\'");
      }
   }

   /**
    * Evaluates a condition only involving variables
    * 
    * @param variableMap : a map of variables that may be used in conditions
    * 
    * @return true/false result of evaluation
    */
   public boolean applies(Map<String, String> variableMap) {
      return evaluateCondition(null, variableMap, null);
   }
   
   /**
    * Evaluates a condition involving devices and variables
    * 
    * @param device      : the device to use in evaluating device related conditions
    * @param variableMap : a map of variables that may be used in conditions
    * 
    * @return true/false result of evaluation
    */
   public boolean appliesTo(Device device, Map<String, String> variableMap) {
      return evaluateCondition(device, variableMap, null);
   }

   /**
    * Evaluates a condition controlling whether a button is enabled
    * 
    * @param device      : the device to use in evaluating device related conditions
    * @param variableMap : a map of variables that may be used in conditions
    * @param buttonMap   : a map of buttons that may be used in conditions
    * 
    * @return true/false result of evaluation
    */
   public boolean enabled(Device device, Map<String, String> variableMap, HashMap<String, Button> buttonMap) throws Exception {
      boolean result = evaluateCondition(device, variableMap, buttonMap);
      if (fVerbose) {
         System.err.println(String.format("ApplyWhenCondition.enabled => %s", result ));
      }
      return result;
   }
   
   /**
    * 
    * @param device
    * @param variableMap
    * @param buttonMap
    * @return
    */
   public boolean evaluateCondition(Device device, Map<String, String> variableMap, HashMap<String, Button> buttonMap) {
      Button button = null;
      if (fVerbose) {
         System.err.println(String.format("ApplyWhenCondition.evaluateCondition(%s)", toString() ));
      }
      switch (fType) {
      case and:
         // Short-circuit evaluation
         for (ApplyWhenCondition operand:fOperands) {
            if (!operand.evaluateCondition(device, variableMap, buttonMap)) {
               return false;
            };
         }
         return true;
      case or:
         // Short-circuit evaluation
         for (ApplyWhenCondition operand:fOperands) {
            if (operand.evaluateCondition(device, variableMap, buttonMap)) {
               return true;
            }
         }
         return false;
      case not:
         return !fOperands.get(0).evaluateCondition(device, variableMap, buttonMap);
      case deviceFamilyIs:
         return (device!= null) && (device.getFamily() != null) && device.getFamily().equals(fValue);
      case deviceFamilyMatches:
         return (device!= null) && (device.getFamily() != null) && device.getFamily().matches(fValue);
      case deviceNameIs:
         return (device!= null) && (device.getName() != null) && device.getName().equals(fValue);
      case deviceNameMatches:
         return (device!= null) && (device.getName() != null) && device.getName().matches(fValue);
      case deviceSubfamilyIs:
         return (device!= null) && (device.getSubFamily() != null) && device.getSubFamily().equals(fValue);
      case deviceSubfamilyMatches:
         return (device!= null) && (device.getSubFamily() != null) && device.getSubFamily().matches(fValue);
      case hardwareIs:
         return (device!= null) && (device.getHardware() != null) && device.getHardware().equals(fValue);
      case hardwareMatches:
         return (device!= null) && (device.getHardware() != null) && device.getHardware().matches(fValue);
      case variableRef: {
         if (variableMap == null) {
            throw new RuntimeException("Evaluation of 'variableRef' without variable map - " + fVariableName);
         }
         String variableValue = variableMap.get(fVariableName);
         if (fCondition == Condition.isDefined) {
            return variableValue != null;
         }
         if (variableValue == null) {
            if (fCondition == Condition.isTrue) {
               if (fVerbose) {
                  System.err.println("Cannot locate variable '"+fVariableName+"' when evaluating requirement: " + fCondition + ", assumed " + fDefaultValue);
               }
               return fDefaultValue; // Assume defaultValue for simple test on missing variable
            }
            System.err.println("Cannot locate variable '"+fVariableName+"' when evaluating requirement: " + fCondition + ", assumed " + fDefaultValue);
            throw new RuntimeException("Error evaluating " + fCondition);
         }
         Long variableNumericValue  = null;
         Long conditionNumericValue = null;
         if ((fCondition != Condition.isTrue) && (fCondition != Condition.matches)&& (fCondition != Condition.isDefined)) {
            try {
               variableNumericValue = Long.decode(variableValue);
            }
            catch (Exception e) {
               throw new RuntimeException("Variable value is illegal in ApplyWhenCondition - " + fVariableName + ", " + fCondition);
            }
            try {
               conditionNumericValue = Long.decode(fValue);
            }
            catch (Exception e) {
               throw new RuntimeException("Condition value is illegal in ApplyWhenCondition - " + fVariableName + ", " + fCondition);
            }
         }
         switch (fCondition) {
         case isDefined:
            return (variableValue != null) && !variableMap.isEmpty();
         case isTrue:
            return Boolean.valueOf(variableValue);
         case matches:
            return variableValue.matches(fValue);
         case equal:
            return variableNumericValue.compareTo(conditionNumericValue) == 0;
         case greaterThan:
            return variableNumericValue.compareTo(conditionNumericValue) > 0;
         case greaterThanOrEqual:
            return variableNumericValue.compareTo(conditionNumericValue) >= 0;
         case lessThan:
            return variableNumericValue.compareTo(conditionNumericValue) < 0;
         case lessThanOrEqual:
            return variableNumericValue.compareTo(conditionNumericValue) <= 0;
         }
         }
      case preclusion:
         button = buttonMap.get(fValue);
         return (button == null) || !button.isEnabled() || !button.getSelection();
      case requirement:
         button = buttonMap.get(fValue);
         return (button != null) && button.isEnabled() && button.getSelection();
      case emptyTrue:
         return true;
      default:
         break;
      }
      return false;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      StringBuffer sb;
      boolean isFirst = true;
      switch (fType) {
      case and:
         sb = new StringBuffer();
         sb.append('(');
         for (ApplyWhenCondition operand:fOperands) {
            if (!isFirst) {
               sb.append("&&");
            }
            isFirst = false;
            sb.append(operand.toString());
         }
         sb.append(')');
         return sb.toString();
      case or:
         sb = new StringBuffer();
         sb.append('(');
         for (ApplyWhenCondition operand:fOperands) {
            if (!isFirst) {
               sb.append("||");
            }
            isFirst = false;
            sb.append(operand.toString());
         }
         sb.append(')');
         return sb.toString();
      case not:
         return "!"+fOperands.get(0);
      case deviceFamilyIs:
         return ("df=="+fValue);
      case deviceFamilyMatches:
         return ("df~="+fValue);
      case deviceNameIs:
         return ("dn=="+fValue);
      case deviceNameMatches:
         return ("dn~="+fValue);
      case deviceSubfamilyIs:
         return ("dsf=="+fValue);
      case deviceSubfamilyMatches:
         return ("dsf~="+fValue);
      case hardwareIs:
         return ("hw=="+fValue);
      case hardwareMatches:
         return ("hw~="+fValue);
      case variableRef:
         return ("ref("+fCondition+"|"+fVariableName+"|"+fValue+")");
      case emptyTrue:
         return "TRUE";
      case preclusion:
         return ("pre("+fValue+")");
      case requirement:
         return ("req(button."+fValue+")");
      default:
         break;
      }
      return super.toString() + "opps!!";
   }
   
   protected ArrayList<String> getVariables(ArrayList<String> variables) {
      switch (fType) {
      case and:
      case or:
         for (ApplyWhenCondition operand:fOperands) {
            operand.getVariables(variables);
         }
         return variables;
      case not:
      case deviceFamilyIs:
      case deviceNameIs:
      case deviceSubfamilyIs:
      case deviceFamilyMatches:
      case deviceNameMatches:
      case deviceSubfamilyMatches:
      case hardwareIs:
      case hardwareMatches:
         return variables;
      case variableRef:
         if (fVariableName != null) {
            variables.add(fVariableName);
         }
         return variables;
      default:
         break;
      }
      return variables;
   }

   /**
    * Creates a list of all variables used by the expression
    * Note: there may be duplicates!
    * 
    * @return ArrayList<String> with names
    */
   public ArrayList<String> getVariables() {
      return getVariables(new ArrayList<String>());
   }
   
   public boolean getfDefaultValue() {
      return fDefaultValue;
   }

   public void setVerbose(boolean b) {
                            fVerbose = b;
   }

}