package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Model describing the Device
 */
public final class DeviceInformationModel extends TreeViewModel {

   /**
    * Constructor
    * 
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
}
