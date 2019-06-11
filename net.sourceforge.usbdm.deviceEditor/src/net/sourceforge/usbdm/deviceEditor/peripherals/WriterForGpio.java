package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of GPIO
 */
public class WriterForGpio extends PeripheralWithState {

   public WriterForGpio(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return"Digital Input/Output";
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal = getSignalIndex(mappingInfo.getSignals().get(fnIndex));
      return String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassBaseName()+getInstance(), signal);
   }

   @Override
   public boolean needPCRTable() {
      return false;
   }

//   @Override
//   public String getPcrDefinition() {
//      return String.format(
//            "   //! Value for PCR (including MUX value)\n"+
//            "   static constexpr uint32_t %s  = GPIO_DEFAULT_PCR;\n\n", DEFAULT_PCR_VALUE_NAME
//            );
//   }

   @Override
   public int getSignalIndex(Signal function) {
      // No tables for GPIO
      return Integer.parseInt(function.getSignalName());
//      Pattern p = Pattern.compile("(\\d+).*");
//      Matcher m = p.matcher(function.getSignalName());
//      if (!m.matches()) {
//         throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
//      }
//      int signalIndex = Integer.parseInt(m.group(1));
//      return signalIndex;
   }

   @Override
   public void modifyVectorTable(VectorTable vectorTable) {
      for (IrqVariable var : irqVariables) {
         modifyVectorTable(vectorTable, var, "Port");
      }
   }
   
}