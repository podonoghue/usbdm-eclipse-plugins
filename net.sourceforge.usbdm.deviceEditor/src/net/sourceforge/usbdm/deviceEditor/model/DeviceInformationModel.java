package net.sourceforge.usbdm.deviceEditor.model;

public final class DeviceInformationModel extends RootModel {

   /**
    * Constructor
    * 
    * @param modelFactory  Factory owning model
    * @param columnLabels  Labels to use for columns
    * @param string2 
    * @param string 
    */
   public DeviceInformationModel(ModelFactory modelFactory, String[] columnLabels, String title, String toolTip) {
      super(modelFactory, columnLabels, title, toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }
}
