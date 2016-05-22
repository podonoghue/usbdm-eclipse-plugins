package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Model describing the device pins organised by peripheral or pin category
 */
public final class DevicePinsModel extends TreeViewModel {

   /**
    * Constructor
    * 
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DevicePinsModel(String[] columnLabels, String title, String toolTip) {
      super(columnLabels, title, toolTip);
   }

   @Override
   public String getValueAsString() {
      return "";
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public EditorPage createEditorPage() {
      return new TreeEditorPage();
   }
}
