package net.sourceforge.usbdm.deviceEditor.model;

public final class DeviceModel extends RootModel {

   /**
    * Constructor
    * 
    * @param modelFactory  Factory owning model
    * @param columnLabels  Labels to use for columns
    * @param string2 
    * @param string 
    */
   public DeviceModel(ModelFactory modelFactory, String[] columnLabels, String title, String toolTip) {
      super(modelFactory, columnLabels, title, toolTip);
   }

   @Override
   public String getValueAsString() {
      return "";
   }

   protected Message checkConflicts() {
      getModelFactory().checkConflicts();
      return new Message("");
   }

   @Override
   protected void removeMyListeners() {
   }
}
