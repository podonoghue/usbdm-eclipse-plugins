package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PinInformation;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForMisc extends WriterBase {

   public WriterForMisc(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
   }

   /* (non-Javadoc)
    * @see InstanceWriter#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String signalName, String alias) {
      return fPeripheral.getClassName()+alias;
   }

   /* (non-Javadoc)
    * @see InstanceWriter#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getFunctions().get(fnIndex).getSignal();
      return fPeripheral.getClassName()+instance+"_"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getFunctionIndex(mappingInfo.getFunctions().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      return -1;
   }
   
   @Override
   public String getTitle() {
      return getClassName().toUpperCase() + " (Miscellaneous)";
   }

   @Override
   public String getGroupBriefDescription() {
      return fPeripheral.getName()+"Miscellaneous Pins";
   }

   @Override
   public boolean useAliases(PinInformation pinInfo) {
      return false;
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.deviceEditor.parser.WriterBase#getDefinition(net.sourceforge.usbdm.deviceEditor.information.MappingInfo, int)
    */
   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) throws IOException {
      return null;
   }

}