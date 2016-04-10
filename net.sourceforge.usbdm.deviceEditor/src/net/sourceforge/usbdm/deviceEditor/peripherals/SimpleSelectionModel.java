package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.ModelEntryProvider;
import net.sourceforge.usbdm.deviceEditor.model.SelectionModel;

public abstract class SimpleSelectionModel extends SelectionModel {

   private final ModelEntryProvider fProvider;
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
   
   public SimpleSelectionModel(BaseModel parent, ModelEntryProvider provider, String key) {
      super(parent, key, provider.getVariableInfo(key).description);

      fProvider   = provider;
      fKey        = key;

      fChoices     = getChoicesArray();
      fValues      = getValuesArray();
      
      fDefaultSelection = findValue(fValues[fValues.length-1]);
//            findValue(provider.getVariableInfo(key).defaultValue);
      fSelection        = fDefaultSelection;

      String sel = fProvider.getValueAsString(fKey);
      if (sel != null) {
         fSelection = findValue(sel);
      }
      setToolTip("Selects the clock source for the module.");
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
