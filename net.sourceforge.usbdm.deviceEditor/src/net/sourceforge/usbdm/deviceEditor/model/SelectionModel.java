package net.sourceforge.usbdm.deviceEditor.model;

public abstract class SelectionModel extends EditableModel {

   /** List of selection values */
   protected String[] fChoices = null;

   /** Current selection index */
   protected int fSelection = 0;
   
   public SelectionModel(BaseModel parent, String name, String description) {
      super(parent, name, description);
   }

   /**
    * Get array of selection choices
    * 
    * @return The array of choices displayed to user
    */
   public String[] getChoices() {
      return fChoices;
   }

   @Override
   public boolean canEdit() {
      return fChoices.length>1;
   }

   @Override
   public boolean isLocked() {
      return !canEdit();
   }

   /**
    * Set the selected value from choices
    * 
    * @param selection Index of choice
    */
   void setSelection(int selection) {
      if (selection<fChoices.length) {
         fSelection = selection;
      }
   }
   
   @Override
   public String getValueAsString() {
      try {
         return fChoices[fSelection];
      }
      catch (Exception e) {
         return "Opps";
      }
   }
   
   @Override
   public void setValueAsString(String value) {
      fSelection = findChoice(value);
      if (fSelection<0) {
         // Invalid - reset to first element
         fSelection = 0;
      }
      return;
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

}
