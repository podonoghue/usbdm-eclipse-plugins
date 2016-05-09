package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

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
public class WriterForMisc extends Peripheral {

   public WriterForMisc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return getClassName().toUpperCase() + " (Miscellaneous)";
   }

   @Override
   public String getAliasName(String signalName, String alias) {
      return getClassName()+alias;
   }

   @Override
   public void writeInfoClass(DocumentUtilities pinMappingHeaderFile) throws IOException {
      if ((getClockMask() == null) && (getClockReg() == null)) {
         return;
      }
      super.writeInfoClass(pinMappingHeaderFile);
   }

   @Override
   public String getGroupName() {
      return getBaseName().toUpperCase()+"_Misc_Group";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return getClassName()+instance+"_"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getSignalIndex(mappingInfo.getSignals().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"CLKIN(0?)", "CLKIN1"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignalName().matches(signalNames[signal])) {
            return signal;
         }
      }
      return -1;
   }
   
   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) throws IOException {
      return null;
   }

}