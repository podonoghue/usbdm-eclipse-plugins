package net.sourceforge.usbdm.deviceEditor.model;

/**
 * Model describing the Device
 */
public final class DeviceInformationModel extends RootModel {

   /**
    * Constructor
    * 
    * @param modelFactory  Factory owning model
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DeviceInformationModel(ModelFactory modelFactory, String[] columnLabels, String title, String toolTip) {
      super(modelFactory, columnLabels, title, toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }
}
