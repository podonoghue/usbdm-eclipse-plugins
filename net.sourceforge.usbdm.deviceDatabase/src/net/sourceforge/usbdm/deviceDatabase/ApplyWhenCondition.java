package net.sourceforge.usbdm.deviceDatabase;

import java.util.ArrayList;
import java.util.Map;

public class ApplyWhenCondition {
   public enum Type {deviceNameIs, deviceFamilyIs, deviceSubfamilyIs, 
                     deviceNameMatches, deviceFamilyMatches, deviceSubfamilyMatches,
                     hardwareIs, hardwareMatches, 
                     requirement,
                     or, and, not};
   public enum Condition {
      isTrue,
      lessThan,
      lessThanOrEqual,
      equal,
      greaterThan,
      greaterThanOrEqual,
   };
   private ApplyWhenCondition.Type       fType;
   private ArrayList<ApplyWhenCondition> fOperands;
   private String                        fValue;
   private ProjectVariable               fVariable;
   private boolean                       fDefaultValue;
   private Condition                     fCondition;
                        
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
          (operator != Type.hardwareMatches)
          ) {
         throw new Exception("ApplyWhenCondition(operator, value) - Must be unary type operator");
      }
   }
   /**
    * Constructs a condition that evaluates a variable
    * 
    * @param operator      must be Type.requirement
    * @param variable      Variable to check
    * @param defaultValue  Default value for condition if variable not found
    * @param condition     Indicates condition to check  <,<=,>,>=,==
    * @param value         Value to compare variable to 
    * 
    * @throws Exception
    */
   public ApplyWhenCondition(
         Type              operator, 
         ProjectVariable   variable, 
         boolean           defaultValue,
         Condition         condition,
         String            value) throws Exception {
      fType          = operator;
      fVariable      = variable;
      fDefaultValue  = defaultValue;
      fCondition     = condition;
      fValue         = value;
      if ((fCondition != Condition.isTrue) && (fValue == null)) {
         throw new Exception("ApplyWhenCondition must have value unless \'isTrue\'");
      }
      if (operator != Type.requirement) {
         throw new Exception("ApplyWhenCondition(operator, value) - Must be 'requirement' type operator");
      }
   }

   public boolean appliesTo(Device device, Map<String, String> variableMap) throws Exception {
      boolean rv;
      switch (fType) {
      case and:
         rv = true;
         for (ApplyWhenCondition operand:fOperands) {
            rv = rv && operand.appliesTo(device, variableMap);
         }
         return rv;
      case or:
         rv = false;
         for (ApplyWhenCondition operand:fOperands) {
            rv = rv || operand.appliesTo(device, variableMap);
         }
         return rv;
      case not:
         return !fOperands.get(0).appliesTo(device, variableMap);
      case deviceFamilyIs:
         return (device.getFamily() != null) && device.getFamily().equals(fValue);
      case deviceFamilyMatches:
         return (device.getFamily() != null) && device.getFamily().matches(fValue);
      case deviceNameIs:
         return (device.getName() != null) && device.getName().equals(fValue);
      case deviceNameMatches:
         return (device.getName() != null) && device.getName().matches(fValue);
      case deviceSubfamilyIs:
         return (device.getSubFamily() != null) && device.getSubFamily().equals(fValue);
      case deviceSubfamilyMatches:
         return (device.getSubFamily() != null) && device.getSubFamily().matches(fValue);
      case hardwareIs:
         return (device.getHardware() != null) && device.getHardware().equals(fValue);
      case hardwareMatches:
         return (device.getHardware() != null) && device.getHardware().matches(fValue);
      case requirement: {
         if (variableMap == null) {
            throw new Exception("Evaluation of requirement without variable map");
         }
         if (fVariable == null) {
            return fDefaultValue;
         }
         String conditionString = variableMap.get(fVariable.getId());
         if (conditionString == null) {
            throw new Exception("Cannot locate variable \'"+fVariable.getId()+"\' when evaluating requirement");
         }
         Long variableValue;
         try {
            variableValue = Long.decode(conditionString);
         }
         catch (Exception e) {
            variableValue = null;
         }
         Long conditionValue;
         try {
            conditionValue = Long.decode(fValue);
         }
         catch (Exception e) {
            conditionValue = null;
         }
         if (fCondition != Condition.isTrue) {
            if (variableValue == null) {
               throw new Exception("Variable value is illegal in ApplyWhenCondition");
            }
            if (conditionValue == null) {
               throw new Exception("Condition value is illegal in ApplyWhenCondition");
            }
         }
         switch (fCondition) {
         case isTrue:
            return Boolean.valueOf(conditionString);
         case equal:
            return variableValue.compareTo(conditionValue) == 0;
         case greaterThan:
            return variableValue.compareTo(conditionValue) > 0;
         case greaterThanOrEqual:
            return variableValue.compareTo(conditionValue) >= 0;
         case lessThan:
            return variableValue.compareTo(conditionValue) < 0;
         case lessThanOrEqual:
            return variableValue.compareTo(conditionValue) <= 0;
         }
         }
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
      case requirement:
         return ("req("+fValue+")");
      default:
         break;
      }
      return super.toString();
   }
   
   private ArrayList<ProjectVariable> getVariables(ArrayList<ProjectVariable> variables) {
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
      case requirement:
         if (fVariable != null) {
            variables.add(fVariable);
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
   public ArrayList<ProjectVariable> getVariables() {
      return getVariables(new ArrayList<ProjectVariable>());
   }
   
   public boolean getfDefaultValue() {
      return fDefaultValue;
   }
}