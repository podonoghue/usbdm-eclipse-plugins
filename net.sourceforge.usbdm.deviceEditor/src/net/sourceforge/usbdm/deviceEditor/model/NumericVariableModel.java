package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.Variable.Units;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

public class NumericVariableModel extends VariableModel {

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
   public NumericVariableModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, provider, key, description);
   }
   
   /**
    * Get minimum permitted value
    * 
    * @return
    */
   public long getMin() {
      return fVariable.getMin();
   }

   /**
    * Get maximum permitted value
    * 
    * @return
    */
   public long getMax() {
      return fVariable.getMax();
   }
   
   /**
    * Get step size of value<br>
    * i.e. the value must always be a multiple of this value
    * 
    * @return
    */
   public long getStep() {
      return fVariable.getStep();
   }
   
   /**
    * Get offset size for value<br>
    * i.e. offset to add when applying this value in template
    * 
    * @return
    */
   public long getOffset() {
      return fVariable.getOffset();
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
   
   /**
    * Checks if the value is valid for this variable
    * 
    * @return Description of error or null if valid
    */
   private String isValid(Long value) {
      if (value<getMin()) {
         return "Value too small";
      }
      if (value>getMax()) {
         return "Value too large";
      }
      long remainder = value % getStep();
      if (remainder != 0) {
         return "Value not a multiple of " + getStep();
      }
      return null;
   }

   public String isValid(String value) {
//      System.err.println("NumericVariableModel.isValid("+fName+", "+value+")");
      long lValue = 0;
      try {
         lValue = Math.round(EngineeringNotation.parse(value));
      }
      catch (NumberFormatException e) {
         return "Illegal number";
      }
      return isValid(lValue);
   }

   @Override
   public void setValueAsString(String sValue) {
//      System.err.println("NumericVariableModel.setValueAsString("+fName+", "+sValue+")");
      Long value;
      if (fVariable.getUnits() == Units.Hz) {
         value = Math.round(EngineeringNotation.parse(sValue));
      }      
      else {
         value = Long.parseLong(sValue);
      }
      value += getOffset();
      super.setValueAsString(Long.toString(value));
   }

   @Override
   public String getValueAsString() {
      return getValueAsString(toLong(super.getValueAsString())-fVariable.getOffset());
   }

   /**
    * Converts the given string into a form appropriate for this  model
    * 
    * @param value Value to format
    * 
    * @return String in appropriate form e.g. 24.56MHz
    */
   public String getValueAsString(Long value) {
      if (fVariable.getUnits() == Units.Hz) {
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
      boolean firstOne = true;
      
      if (getMin() != Long.MIN_VALUE) {
         sb.append("\n");
         firstOne = false;
         sb.append("min="+getValueAsString(getMin())+" ");
      }
      if (getMax() != Long.MAX_VALUE) {
         if (firstOne) {
            sb.append("\n");
         }
         firstOne = false;
         sb.append("max="+getValueAsString(getMax())+" ");
      }
      if (getStep() != 1) {
         if (firstOne) {
            sb.append("\n");
         }
         firstOne = false;
         sb.append("step="+getValueAsString(getStep())+" ");
      }
      return sb.toString();
   }
   
}
