package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

public class NumericVariableModel extends VariableModel {

   private long fMin  = Long.MIN_VALUE;
   private long fMax  = Long.MAX_VALUE;
   private long fStep = 1;

   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param name          Display name
    * @param description   Display description
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
      return fMin;
   }

   /**
    * Set minimum permitted value
    * 
    * @param min Value to set
    */
   public void setMin(long min) {
      fMin = min;
   }
   
   /**
    * Get maximum permitted value
    * 
    * @return
    */
   public long getMax() {
      return fMax;
   }
   
   /**
    * Set maximum permitted value
    * 
    * @param max Value to set
    */
   public void setMax(long max) {
      fMax = max;
   }
   
   /**
    * Get step size of value<br>
    * i.e. the value must always be a multiple of this value
    * 
    * @return
    */
   public long getStep() {
      return fStep;
   }
   
   /**
    * Set step size
    * 
    * @param max Value to set
    */
   public void setStep(long step) {
      fStep = step;
   }
   
   /**
    * Checks if the value is valid for this variable
    * 
    * @return Description of error or null if valid
    */
   public String isValid(Long value) {
      if (value<fMin) {
         return "Value too small";
      }
      if (value>fMax) {
         return "Value too large";
      }
      long remainder = value % fStep;
      if (remainder != 0) {
         return "Value not a multiple of " + fStep;
      }
      return null;
   }

   @Override
   public String isValid(String value) {
      long lValue = 0;
      try {
         String s = value.toString().trim();
         if (s.startsWith("0b")) {
            lValue = Long.parseLong(s.substring(2, s.length()), 2);
         } else {
            lValue = Long.decode(s);
         }
      }
      catch (NumberFormatException e) {
         return "Illegal number";
      }
      return isValid(lValue);
   }

   @Override
   public String getValueAsString() {
      String value = super.getValueAsString();
      setMessage(isValid(value));
      if (fLogging) {
         System.err.println("getValueAsString() "+value);
      }
      return value;
   }

   @Override
   public void setValueAsString(String value) {
      setMessage(isValid(value));
      super.setValueAsString(value);
   }

   @Override
   Message getMessage() {
      Message msg = super.getMessage();
      if ((msg != null) && msg.greaterThan(Severity.WARNING)) {
         return msg;
      }
      String message = isValid(getValueAsString());
      if (message != null) {
         msg = new Message(message, this);
      }
      return msg;
   }
}
