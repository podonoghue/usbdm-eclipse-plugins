package net.sourceforge.usbdm.deviceDatabase;

import java.util.ArrayList;

public class ApplyWhenCondition {
   public enum Type {deviceNameIs, deviceFamilyIs, deviceSubfamilyIs, 
                     deviceNameMatches, deviceFamilyMatches, deviceSubfamilyMatches, or, and, not};
   ApplyWhenCondition.Type                          fType;
   ArrayList<ApplyWhenCondition> fOperands;
   String                        fValue;
   
   public ApplyWhenCondition(ApplyWhenCondition.Type operator, ArrayList<ApplyWhenCondition> operands) 
         throws Exception {
      fType = operator;
      fOperands = operands;
      if ((operator != Type.and) && (operator != Type.or)) {
         throw new Exception("ApplyWhenCondition(operator, operands) - Must be binary operator");
      }
   }
   public ApplyWhenCondition(ApplyWhenCondition.Type operator, ApplyWhenCondition operand) throws Exception {
      fType = operator;
      fOperands = new ArrayList<ApplyWhenCondition>();
      fOperands.add(operand);
      if (operator != Type.not) {
         throw new Exception("ApplyWhenCondition(operator, operand) - Must be not operator");
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
          (operator != Type.deviceSubfamilyMatches)) {
         throw new Exception("ApplyWhenCondition(operator, value) - Must be unary type operator");
      }
   }
   
   public boolean appliesTo(Device device) {
      boolean rv;
      switch (fType) {
      case and:
         rv = true;
         for (ApplyWhenCondition operand:fOperands) {
            rv = rv && operand.appliesTo(device);
         }
         return rv;
      case or:
         rv = false;
         for (ApplyWhenCondition operand:fOperands) {
            rv = rv || operand.appliesTo(device);
         }
         return rv;
      case not:
         return !appliesTo(device);
      case deviceFamilyIs:
         return (device.getFamily() != null) && device.getFamily().equals(fValue);
      case deviceNameIs:
         return (device.getName() != null) && device.getName().equals(fValue);
      case deviceSubfamilyIs:
         return (device.getSubFamily() != null) && device.getSubFamily().equals(fValue);
      case deviceFamilyMatches:
         return (device.getFamily() != null) && device.getFamily().matches(fValue);
      case deviceNameMatches:
         return (device.getName() != null) && device.getName().matches(fValue);
      case deviceSubfamilyMatches:
         return (device.getSubFamily() != null) && device.getSubFamily().matches(fValue);
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
         return ("dfi=="+fValue);
      case deviceNameIs:
         return ("dni=="+fValue);
      case deviceSubfamilyIs:
         return ("dsfi=="+fValue);
      case deviceFamilyMatches:
         return ("dfi~="+fValue);
      case deviceNameMatches:
         return ("dni~="+fValue);
      case deviceSubfamilyMatches:
         return ("dsfi~="+fValue);
      }
      return super.toString();
   }
}