package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

public class SelectionVariableModel extends VariableModel {

   /** Array of values corresponding to displayed choices */
   private String[]  fValues = null;
   
   /** List of displayed choices */
   protected String[] fChoices = null;

   /**
    * Create model
    * 
    * @param parent        Parent model
    * @param provider      Provider that owns the variable
    * @param key           Key used to access the variable
    * @param description   Description for the display
    */
   public SelectionVariableModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, provider, key, description);
   }

   /**
    * Get array of selection choices
    * 
    * @return The array of choices displayed to user
    */
   public String[] getChoices() {
      return fChoices;
   }

   /** 
    * Set choices displayed
    * 
    * @param choices
    */
   public void setChoices(String[] choices) {
      fChoices = choices;
   }
   
   /** 
    * Set values corresponding to choices displayed
    * 
    * @param values
    */
   public void setValues(String[] values) {
      fValues = values;
   }
   
   /**
    * Finds the given choice in fChoices
    * 
    * @param value Choice to look for
    * 
    * @return Selection index or -1 if not found
    */
   protected int findChoice(String choice) {
      for (int index=0; index<fChoices.length; index++) {
         if (fChoices[index].equalsIgnoreCase(choice)) {
            return index;
         }
      }
      return -1;
   }
   /**
    * Finds the given value in fValues
    * 
    * @param value Value to look for
    * 
    * @return Selection index or -1 if not found
    */
   protected int findValue(String value) {
      for (int index=0; index<fValues.length; index++) {
         if (fValues[index].equalsIgnoreCase(value)) {
            return index;
         }
      }
      return -1;
   }

   public void setValueAsString(String value) {
      int selection = findChoice(value);
      if (selection<0) {
         // Invalid - reset to first element
         selection = 0;
      }
      super.setValueAsString(fValues[selection]);
   }

   @Override
   public String getValueAsString() {
      return fChoices[findValue(super.getValueAsString())];
   }
   
   @Override
   protected void removeMyListeners() {
   }

}
