package net.sourceforge.usbdm.deviceEditor.model;

import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

/**
 * Model representing a peripheral in the Pin Mapping by Peripheral tree view
 */
public final class PeripheralModel extends CategoryModel {

   /**
    * Constructor <br>
    * Constructs model for signals associated with a peripheral
    * 
    * @param parent        Parent
    * @param peripheral    Peripheral used to locate signals model
    */
   public PeripheralModel(BaseModel parent, Peripheral peripheral) {
      super(parent, peripheral.getName(), peripheral.getDescription());
      peripheral.createSignalModels(this);
   }
   
}
