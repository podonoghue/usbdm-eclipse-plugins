package net.sourceforge.usbdm.deviceEditor.model;

import java.util.TreeMap;

import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public final class PeripheralModel extends BaseModel {

   public PeripheralModel(BaseModel parent, Peripheral peripheral) {
      super(parent, peripheral.getName(), peripheral.getDescription());
   }

   /*
    * =============================================================================================
    */
   public static PeripheralModel createPeripheralModel(BaseModel parent, Peripheral peripheral) {
      
      PeripheralModel peripheralModel = new PeripheralModel(parent, peripheral);
      TreeMap<String, Signal> peripheralSignals = peripheral.getSignals();
      for (String signalName:peripheralSignals.keySet()) {
         Signal signal = peripheralSignals.get(signalName);
         if (signal.isAvailableInPackage()) {
            new SignalModel(peripheralModel, signal);
         }
      }
      return peripheralModel;
   }

   @Override
   protected void removeMyListeners() {
   }

}
