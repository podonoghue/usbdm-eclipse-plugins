package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of Control
 */
/**
 * @author podonoghue
 *
 */
public class WriterForControl extends PeripheralWithState {

   public WriterForControl(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      setSynthetic();
      
      // Can create type declarations for signals belonging to this peripheral (PCR)
      fcanCreateSignalType = true;
   }

   @Override
   protected void writeDeclarations() {
      writeSignalPcrDeclarations();
   }
   
   @Override
   public String getTitle() {
      return "Control";
   }

   @Override
   public void writeInfoConstants(final DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      super.writeDefaultPinInstances(pinMappingHeaderFile);
   }
   
   @Override
   public String getGroupName() {
      return "Control_Group";
   }

   private HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
   private int fIndex = 0;
   
   @Override
   public int getSignalIndex(Signal function) {
      Integer index = indexMap.get(function.getName());
      if (index == null) {
         index = fIndex++;
         indexMap.put(function.getName(), index);
      }
      return index;
      
//      return super.getSignalIndex(function, signalNames);
   }
}