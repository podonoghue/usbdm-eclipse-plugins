package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of I2C
 */
public class WriterForI2c extends PeripheralWithState {

   public WriterForI2c(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can (usually do) create instances of this class
      fCanCreateInstance = true;
      
      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "Inter-Integrated-Circuit Interface";
   }

   @Override
   public int getSignalIndex(Signal signal) {
      String signalNames[] = {"SCL", "SDA", "4WSCLOUT", "4WSDAOUT", "SCLS", "SDAS", "HREQ"};
      return getSignalIndex(signal, signalNames);
   }

   @Override
   public String getPcrValue(Signal y) {
      return "USBDM::I2C_DEFAULT_PCR";
   }
   
   @Override
   public void validateMappedPins() {
      super.validateMappedPins();
      if (fStatus != null) {
         return;
      }
      // Warn if SCL and SDA signals not mapped
      validateMappedPins(new int[]{0,1}, getSignalTables().get(0).table);
   }

   @Override
   public long getPcrForcedBitsMask(Signal signal) {
      
      // Must use open-drain for SDA & SCL - force ODE
      int index = fInfoTable.indexOf(signal);
      return ((index>=0) && (index<2))?MappingInfo.PORT_PCR_ODE_MASK:0;
   }

   @Override
   public long getPcrForcedBitsValueMask(Signal signal) {
      
      // Must use open-drain for SDA & SCL - ODE = 1
      int index = fInfoTable.indexOf(signal);
      return ((index>=0) && (index<2))?MappingInfo.PORT_PCR_ODE_MASK:0;
   }
   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      
//      final String format = ""
//            + "   // I2C %s Pin Info table index\n"
//            + "   static constexpr unsigned %sInfoIndex = %s;\n\n";
//      pinMappingHeaderFile.write(String.format(format, "SCL (clock)", "scl", "0"));
//      pinMappingHeaderFile.write(String.format(format, "SDA (clock)", "sda", "1"));
      
   }

}