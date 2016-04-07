package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForLptmr extends Peripheral {

   public WriterForLptmr(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignal();
      return getClassName()+instance+"_"+signal;
   }

   @Override
   public String getTitle() {
      return "Low Power Timer";
   }

   @Override
   public int getFunctionIndex(Signal function) {
      final String signalNames[] = {"ALT0", "ALT1", "ALT2", "ALT3"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignal().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal "+function.getSignal()+" does not match expected pattern ");
   }
   
   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

}