/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Added interfaceType field                                                         | V4.10.6.250
===============================================================================================================
*/

package net.sourceforge.usbdm.peripherals.model;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;

/**
 * Model for a device tree item (ROOT element)
 *
 */
public class DeviceModel extends BaseModel {
   
   InterfaceType interfaceType = null;

   public DeviceModel(String deviceName) {
      super(null, deviceName, deviceName);
      new BaseModel(this, "Loading...", "Loading...") {};
   }

   /**
    * Sets all peripherals 'needsUpdate' status
    * 
    * @param needsUpdate
    */
   public void setNeedsUpdate(boolean needsUpdate) {
      for (Object peripheralModel : getChildren()) {
         if (peripheralModel instanceof PeripheralModel) {
            ((PeripheralModel)peripheralModel).setNeedsUpdate(needsUpdate);
         }
      }
   }
   
   /**
    * Resets the model register values to their expected reset values  
    */
   public void loadResetValues() {
      for (Object peripheralModel : getChildren()) {
         if (peripheralModel instanceof PeripheralModel) {
            ((PeripheralModel)peripheralModel).loadResetValues();
         }
      }
   }
   
   /**
    * Indicates that the current value of all peripheral registers are to be used as the reference
    * for determining changed values. 
    */
   public void setChangeReference() {
      for (Object peripheralModel : getChildren()) {
         if (peripheralModel instanceof PeripheralModel) {
            ((PeripheralModel)peripheralModel).setChangeReference();
         }
      }
   }

   public void setInterfaceType(InterfaceType interfaceType) {
      this.interfaceType = interfaceType;
   }

   public InterfaceType getInterfaceType() {
      return interfaceType;
   }

}