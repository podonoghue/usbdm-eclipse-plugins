package net.sourceforge.usbdm.peripherals.model;

/**
 * Model for a device tree item (ROOT element)
 *
 */
public class DeviceModel extends BaseModel {
   
   public DeviceModel(String deviceName) {
      super(null, deviceName, deviceName);
   }

   /**
    * Sets all peripherals 'needsUpdate' status
    * 
    * @param needsUpdate
    */
   public void setNeedsUpdate(boolean needsUpdate) {
      for (Object peripheralModel : getChildren()) {
         ((PeripheralModel)peripheralModel).setNeedsUpdate(needsUpdate);
      }
   }
   
   /**
    * Resets the model register values to their expected reset values  
    */
   public void loadResetValues() {
      for (Object peripheralModel : getChildren()) {
         ((PeripheralModel)peripheralModel).loadResetValues();
      }
   }
   
   /**
    * Indicates that the current value of all peripheral registers are to be used as the reference
    * for determining changed values. 
    */
   public void setChangeReference() {
      for (Object peripheralModel : getChildren()) {
         ((PeripheralModel)peripheralModel).setChangeReference();
      }
   }
}