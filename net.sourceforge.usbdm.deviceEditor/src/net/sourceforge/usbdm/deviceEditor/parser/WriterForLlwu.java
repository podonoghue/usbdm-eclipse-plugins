package net.sourceforge.usbdm.deviceEditor.parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public class WriterForLlwu extends WriterBase {

   public WriterForLlwu(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
      // TODO Auto-generated constructor stub
   }

   /* (non-Javadoc)
    * @see InstanceWriter#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String signalName, String alias) {
      return getClassName()+alias;
   }

   /* (non-Javadoc)
    * @see InstanceWriter#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getFunctions().get(fnIndex).getSignal();
      return getClassName()+instance+"_"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    */
   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }
   
   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      Pattern p = Pattern.compile("P(\\d+)");
      Matcher m = p.matcher(function.getSignal());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Signal "+function.getSignal()+" does not match expected pattern ");
   }
   
   @Override
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public boolean useAliases(PinInformation pinInfo) {
      return false;
   }

   @Override
   public boolean needPCRTable() {
      return false;
   }

   @Override
   public String getTitle() {
      return "Low-leakage Wake-up Unit";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used for Low-leakage Wake-up Unit";
   }
}