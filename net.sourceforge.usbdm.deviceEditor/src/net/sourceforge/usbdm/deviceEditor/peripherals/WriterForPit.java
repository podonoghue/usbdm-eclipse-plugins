package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of PIT
 */
public class WriterForPit extends PeripheralWithState {

   public WriterForPit(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Programmable Interrupt Timer";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"OUT"};
      return getSignalIndex(function, signalNames);
   }
   
   @Override
   public void modifyVectorTable(VectorTable vectorTable) {
      IrqVariable var = (IrqVariable) safeGetVariable(makeKey("irqHandler"));
      super.modifyVectorTable(vectorTable, var, getClassName());
      for (int i=0; i<4; i++) {
         var = (IrqVariable) safeGetVariable(makeKey("irqHandlerChannel"+i));
         super.modifyVectorTable(vectorTable, var, getClassName());
      }
   }

}