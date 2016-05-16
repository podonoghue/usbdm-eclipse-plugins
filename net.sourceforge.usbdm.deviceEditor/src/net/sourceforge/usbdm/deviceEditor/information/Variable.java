package net.sourceforge.usbdm.deviceEditor.information;

import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

public class Variable extends ObservableModel {
   private final   String  fName;
   private         String  fValue;
   private         Message fMessage = null;
   
   // Minimum permitted value
   private long fMin    = Long.MIN_VALUE;
   
   // Maximum permitted value
   private long fMax    = Long.MAX_VALUE;

   // Step size value
   private long fStep   = 1;
   
   // Offset used when applying value
   private long fOffset = 0;

   
   Variable(String name, String value) {
      fName  = name;
      fValue = value;
   }

   /**
    * @return the Name
    */
   public String getName() {
      return fName;
   }

   /**
    * @return the Value
    */
   public String getValue() {
      return fValue;
   }

   /**
    * @param value The value to set
    */
   public void setValue(String value) {
      System.err.println("Variable.setValue("+fName+", "+value+")");
      if (fValue.equals(value)) {
         return;
      }
      fValue = value;
      notifyListeners();
   }

   @Override
   public String toString() {
      return "Variable(Name=" + fName + ", value=" + fValue + ")";
   }

   /**
    * Get the variable value interpreted as a Long
    * 
    * @return
    */
   public long getValueAsLong() {
      return Long.decode(fValue);
   }

   public void setValue(long value) {
      setValue(Long.toString(value));
   }

   public void setMessage(String message) {
      if ((fMessage != null) && (message != null) && fMessage.equals(message)) {
         // No significant change
         return;
      }
      if (message == null) {
         setMessage((Message)null);
      }
      else {
         setMessage(new Message(message));
      }
   }

   public void setMessage(Message message) {
      if ((fMessage == null) && (message == null)) {
         // No change
         return;
      }
      if ((fMessage != null) && (message != null) && fMessage.equals(message)) {
         // No significant change
         return;
      }
      fMessage = message;
      notifyListeners();
   }
   
   public Message getMessage() {
      return fMessage;
   }

   public void setMin(long min) {
      fMin = min;
   }

   public void setMax(long max) {
      fMax = max;
   }

   public void setStep(long step) {
      fStep = step;
   }

   public void setOffset(long offset) {
      fOffset = offset;
   }

   public long getMin() {
      return fMin;
   }

   public long getMax() {
      return fMax;
   }

   public long getStep() {
      return fStep;
   }

   public long getOffset() {
      return fOffset;
   }

   public boolean getValueAsBoolean() {
      return (fValue.equalsIgnoreCase("1") || 
            fValue.equalsIgnoreCase("true")|| 
            fValue.equalsIgnoreCase("active")|| 
            fValue.equalsIgnoreCase("enabled"));
   }

   public void setBinaryValue(Boolean b) {
      setValue(b.toString());
   }

}