package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;

/**
 * Model describing the device pins organised by pin category
 */
public final class DevicePinsModel extends TreeViewModel implements IPage {

   static final private String[] PERIPHERAL_COLUMN_LABELS  = {"Peripheral.Signal", "Mux:Pin",     "Description"};

   /**
    * Constructor
    * @param parent 
    * 
    * @param columnLabels  Labels to use for columns
    * @param title 
    * @param toolTip 
    */
   public DevicePinsModel(BaseModel parent, DeviceInfo fDeviceInfo) {
      super(parent, "Peripheral View", "Pin mapping organized by peripheral", PERIPHERAL_COLUMN_LABELS);

      for (String pName:fDeviceInfo.getPeripherals().keySet()) {
         Peripheral peripheral = fDeviceInfo.getPeripherals().get(pName);
         if (peripheral.hasMappableSignals()) {
            new PeripheralModel(this, peripheral);
         }
      }
   }
   @Override
   public String getValueAsString() {
      return "";
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public IEditorPage createEditorPage() {
      return new TreeEditorPage();
   }
   @Override
   public String getPageName() {
      return "Peripheral View";
   }
   @Override
   public void updatePage() {
      update();
   }
   @Override
   public TreeViewModel getModel() {
      return this;
   }
}
