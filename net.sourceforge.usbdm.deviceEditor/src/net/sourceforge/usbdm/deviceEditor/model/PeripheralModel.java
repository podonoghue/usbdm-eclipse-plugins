package net.sourceforge.usbdm.deviceEditor.model;

import java.util.TreeMap;

import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class PeripheralModel extends BaseModel {

   public PeripheralModel(BaseModel parent, Peripheral peripheral) {
      super(parent, peripheral.getName(), peripheral.getDescription());
   }

   /*
    * =============================================================================================
    */
   public static PeripheralModel createPeripheralModel(BaseModel parent, Peripheral peripheral) {
      PeripheralModel peripheralModel = new PeripheralModel(parent, peripheral);
      TreeMap<String, Signal> peripheralFunctions = peripheral.getFunctions();
      for (String peripheralFunctionName:peripheralFunctions.keySet()) {
         Signal peripheralFunction = peripheralFunctions.get(peripheralFunctionName);
         new SignalModel(peripheralModel, peripheralFunction);
      }
      return peripheralModel;
   }

}
