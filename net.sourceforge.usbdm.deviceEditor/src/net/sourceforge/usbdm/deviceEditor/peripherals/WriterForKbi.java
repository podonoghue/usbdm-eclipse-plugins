package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.HardwareDeclarationInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of OSC
 */
public class WriterForKbi extends PeripheralWithState {

   public WriterForKbi(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Instance has internal state
      clearConstType();
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;
   }

   @Override
   public String getTitle() {
      return "Keyboard Interrupts";
   }

   @Override
   public int getSignalIndex(Signal signal) {
      Pattern p = Pattern.compile("P(\\d+)");
      Matcher m = p.matcher(signal.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Signal does not match expected pattern " + signal.getSignalName());
   }
   
   @Override
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {
      
      super.writeDeclarations(hardwareDeclarationInfo);
      
      for (int index=0; index<fInfoTable.table.size(); index++) {
         Signal signal = fInfoTable.table.get(index);
         if (signal == null) {
            continue;
         }
         Pin pin = signal.getFirstMappedPinInformation().getPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            continue;
         }
         String cIdentifier = signal.getCodeIdentifier();
         if ((cIdentifier == null) || cIdentifier.isBlank()) {
            continue;
         }
         if (!pin.isAvailableInPackage()) {
            continue;
         }
         String trailingComment  = pin.getNameWithLocation();
         String type = "Kbi"+getInstance()+"PinIndex";
         String pinName = type + "_" + prettyPinName(pin.getName());
         writeConstexprValue(hardwareDeclarationInfo, signal.getUserDescription(), cIdentifier, type, pinName, trailingComment);
      }
   }

}