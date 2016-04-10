package net.sourceforge.usbdm.deviceEditor.model;

public final class PeripheralConfigurationModel extends RootModel {

   public PeripheralConfigurationModel(ModelFactory modelFactory, String[] columnLabels, String title, String toolTip) {
      super(modelFactory, columnLabels, title, toolTip);
   }

   @Override
   protected void removeMyListeners() {
   }

}
