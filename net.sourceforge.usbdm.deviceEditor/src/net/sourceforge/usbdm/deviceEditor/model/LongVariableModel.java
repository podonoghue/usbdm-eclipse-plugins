package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable.Units;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

public class LongVariableModel extends VariableModel {

   private static final int SIGNIFICANT_DIGITS = 5;
   
   /**
    * 
    * @param parent        Parent model
    * @param provider      Associated variable provider
    * @param key           Variable key
    * @param description   Description for model
    * 
    * @note Added as child of parent if not null
    */
   public LongVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }
   
   @Override
   public LongVariable getVariable() {
      return (LongVariable)super.getVariable();
   }

   /**
    * Convert string to long
    * 
    * @param value
    * @return
    */
   static long toLong(String value) {
//      if (fVariable.getUnits() == Units.Hz) {
      long lValue = 0;
      String s = value.toString().trim();
      if (s.startsWith("0b")) {
         lValue = Long.parseLong(s.substring(2, s.length()), 2);
      } else {
         lValue = Long.decode(s);
      }
      return lValue;
   }
   
   public String isValid(String value) {
      return getVariable().isValid(value);
   }

   @Override
   public void setValueAsString(String sValue) {
//      System.err.println("NumericVariableModel.setValueAsString("+fName+", "+sValue+")");
      Long value;
      if (getVariable().getUnits() == Units.Hz) {
         value = Math.round(EngineeringNotation.parse(sValue));
      }      
      else {
         value = Long.parseLong(sValue);
      }
      getVariable().setValue(value);
   }

   @Override
   public String getValueAsString() {
      return getVariable().getValueAsString();
   }

   /**
    * Converts the given string into a form appropriate for this  model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   public String getValueAsString(Long value) {
      if (getVariable().getUnits() == Units.Hz) {
         return EngineeringNotation.convert(value, SIGNIFICANT_DIGITS)+"Hz";
      }      
      return Long.toString(value);
   }

   @Override
   Message getMessage() {
      Message msg = super.getMessage();
      if ((msg != null) && msg.greaterThan(Severity.WARNING)) {
         return msg;
      }
      String message = isValid(getValueAsString());
      if (message != null) {
         msg = new Message(message);
      }
      return msg;
   }

   @Override
   public String getToolTip() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.getToolTip());
      boolean newLineNeeded = sb.length()>0;
      
      if (getVariable().getMin() != Long.MIN_VALUE) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("min="+getValueAsString(getVariable().getMin())+" ");
      }
      if (getVariable().getMax() != Long.MAX_VALUE) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("max="+getValueAsString(getVariable().getMax())+" ");
      }
      if (getVariable().getStep() != 1) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("step="+getValueAsString(getVariable().getStep())+" ");
      }
      return (sb.length() == 0)?null:sb.toString();
   }
   
}
