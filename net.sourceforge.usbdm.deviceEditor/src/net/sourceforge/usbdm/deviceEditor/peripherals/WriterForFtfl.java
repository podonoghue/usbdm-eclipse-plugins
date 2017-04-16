package net.sourceforge.usbdm.deviceEditor.peripherals;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

/**
 * Class encapsulating the code for writing an instance of LLWU
 */
public class WriterForFtfl extends PeripheralWithState {

   public WriterForFtfl(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

//   /**
//    * Return version name of peripheral<br>
//    * Defaults to name based on peripheral e.g. Ftm
//    */
//   public String getVersion() {
//      return ((fVersion!=null) && !fVersion.isEmpty())?fVersion:getClassBaseName().toLowerCase();
//   }


   @Override
   public String getTitle() {
      return "Flash Memory Module";
   }

//   public void writeExtraInfo(DocumentUtilities pinMappingHeaderFile) throws IOException {
//      System.err.println("ParamMap = " + getParamMap());
//      System.err.println("Version = " + getPeripheralModelName());
//      
//   }

}