package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.SelectionModel;

public abstract class SimpleSelectionModel extends SelectionModel {

   private final IModelEntryProvider fProvider;
   private final String             fKey;
   private final String[]           fValues;
   
   private int fDefaultSelection = 0;

   /**
    * Gets array of choices to display to user
    * 
    * @return
    */
   protected abstract String[] getChoicesArray();
   /**
    * Get array of values that correspond to choices
    * 
    * @return
    */
   protected abstract String[] getValuesArray();
   
   /**
    * 
    * @param parent     
    * @param provider
    * @param key
    */
   public SimpleSelectionModel(BaseModel parent, IModelEntryProvider provider, String key, String description) {
      super(parent, key, description);
      
      fProvider   = provider;
      fKey        = key;
      
      fChoices     = getChoicesArray();
      fValues      = getValuesArray();
      
      fDefaultSelection = findValue(fValues[fValues.length-1]);
      fSelection        = fDefaultSelection;

      String sel = fProvider.getValueAsString(fKey);
      if (sel != null) {
         fSelection = findValue(sel);
      }
   }

   /**
    * Finds the given value in VALUES
    * 
    * @param value Value to look for
    * 
    * @return Selection index or -1 if not found
    */
   int findValue(String value) {
      for (int index=0; index<fValues.length; index++) {
         if (fValues[index].equalsIgnoreCase(value)) {
            return index;
         }
      }
      return -1;
   }

   @Override
   public void setValueAsString(String value) {
      super.setValueAsString(value);
      if (fSelection >= (fValues.length-1)) {
         // Handle default choice
         fSelection = fDefaultSelection;
      }
      fProvider.setValue(fKey, fValues[fSelection]);
   }

   @Override
   protected void removeMyListeners() {
   }

}