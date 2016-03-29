package net.sourceforge.usbdm.deviceEditor.model;

import java.util.TreeMap;

import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;

public class PeripheralModel extends BaseModel {

   public PeripheralModel(BaseModel parent, Peripheral peripheral) {
      super(parent, peripheral.getName(), peripheral.getDescription());
   }

   /*
    * =============================================================================================
    */
   public static PeripheralModel createPeripheralModel(BaseModel parent, Peripheral peripheral) {
      PeripheralModel peripheralModel = new PeripheralModel(parent, peripheral);
      TreeMap<String, PeripheralFunction> peripheralFunctions = peripheral.getFunctions();
      for (String peripheralFunctionName:peripheralFunctions.keySet()) {
         PeripheralFunction peripheralFunction = peripheralFunctions.get(peripheralFunctionName);
         new FunctionModel(peripheralModel, peripheralFunction);
      }
      return peripheralModel;
   }

}
