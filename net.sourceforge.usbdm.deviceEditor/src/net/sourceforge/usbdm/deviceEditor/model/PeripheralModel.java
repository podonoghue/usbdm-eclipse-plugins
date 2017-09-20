package net.sourceforge.usbdm.deviceEditor.model;

import java.util.TreeMap;

import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Model representing a peripheral in the Pin Mapping by Peripheral tree view
 */
public final class PeripheralModel extends CategoryModel {

   /**
    * Constructor <br>
    * Constructs peripheral and associated signals
    * 
    * @param parent        Parent
    * @param peripheral    Peripheral associated with this model
    */
   public PeripheralModel(BaseModel parent, Peripheral peripheral) {
      super(parent, peripheral.getName(), peripheral.getDescription());
      TreeMap<String, Signal> peripheralSignals = peripheral.getSignals();
      for (String signalName:peripheralSignals.keySet()) {
         Signal signal = peripheralSignals.get(signalName);
         if (signal.isAvailableInPackage()) {
            new SignalModel(this, signal);
         }
      }
   }
   
}
