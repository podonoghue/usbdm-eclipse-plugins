package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

public class DoubleVariableModel extends VariableModel {

   /**
    * 
    * @param parent        Parent model
    * @param provider      Associated variable provider
    * @param key           Variable key
    * @param description   Description for model
    * 
    * @note Added as child of parent if not null
    */
   public DoubleVariableModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }
   
   @Override
   public DoubleVariable getVariable() {
      return (DoubleVariable)super.getVariable();
   }

   public String isValid(String value) {
      return getVariable().isValid(value);
   }

   @Override
   public void setValueAsString(String sValue) {
      getVariable().setValue(sValue);
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
   private String getValueAsString(Double value) {
      return getVariable().getValueAsString(value);
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
      
      if (getVariable().getMin() != Double.NEGATIVE_INFINITY) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("min="+getValueAsString(getVariable().getMin())+" ");
      }
      if (getVariable().getMax() != Double.POSITIVE_INFINITY) {
         if (newLineNeeded) {
            sb.append("\n");
            newLineNeeded = false;
         }
         sb.append("max="+getValueAsString(getVariable().getMax())+" ");
      }
      return (sb.length() == 0)?null:sb.toString();
   }
   
}
