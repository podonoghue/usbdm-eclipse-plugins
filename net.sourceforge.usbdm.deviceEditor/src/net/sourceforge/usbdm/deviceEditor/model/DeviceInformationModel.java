package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Model describing the Device
 */
public final class DeviceInformationModel extends TreeViewModel {

   /**
    * Constructor
    * 
    * @param modelFactory  Factory owning model
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DeviceInformationModel(String[] columnLabels, String title, String toolTip) {
      super(columnLabels, title, toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public EditorPage createEditorPage() {
      return new TreeEditorPage();
   }
   
}
