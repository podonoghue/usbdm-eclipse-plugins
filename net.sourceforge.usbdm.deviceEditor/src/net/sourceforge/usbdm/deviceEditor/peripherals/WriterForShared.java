package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.HashMap;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of Shared
 */
/**
 * @author podonoghue
 *
 */
public class WriterForShared extends PeripheralWithState {

   public WriterForShared(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Not real hardware
      setSynthetic();

   }

   @Override
   public String getTitle() {
      return "Shared Resources";
   }

   @Override
   public String getGroupName() {
      return getBaseName().toUpperCase()+"_Group";
   }

   private HashMap<String, Integer> indexMap = new HashMap<String, Integer>();
   private int fIndex = 0;
   
   @Override
   public int getSignalIndex(Signal signal) {
      Integer index = indexMap.get(signal.getName());
      if (index == null) {
         index = fIndex++;
         indexMap.put(signal.getName(), index);
      }
      return index;
   }
}