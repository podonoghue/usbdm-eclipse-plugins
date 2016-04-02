package net.sourceforge.usbdm.deviceEditor.model;

public class SelectionModel extends BaseModel {

   /** List of selection values */
   protected String[] fValues = null;

   /** Current selection index */
   protected int fSelection = 0;
   
   public SelectionModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   /**
    * Get list of selection values
    * 
    * @return
    */
   public String[] getValues() {
      return fValues;
   }

   @Override
   public boolean canEdit() {
      return fValues.length>1;
   }

   @Override
   public boolean isLocked() {
      return !canEdit();
   }

   @Override
   public String getValueAsString() {
      try {
         return fValues[fSelection];
      }
      catch (Exception e) {
         return "Opps";
      }
   }
   
   public void setValue(String value) {
      if (value != null) {
         for (int index=0; index<fValues.length; index++) {
            if (fValues[index].equals(value)) {
               fSelection = index;
               return;
            }
         }
      }
      // Invalid - reset to first element
      fSelection = 0;
      return;
   }

   public boolean isReset() {
      return false;
   }
   
}
