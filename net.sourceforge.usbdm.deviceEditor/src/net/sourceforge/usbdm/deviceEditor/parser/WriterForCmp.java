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
public class WriterForCmp extends WriterBase {

   public WriterForCmp(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
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
 
   @Override
   public int getFunctionIndex(PeripheralFunction function) {
    Pattern p = Pattern.compile("IN(\\d+)");
    Matcher m = p.matcher(function.getSignal());
    if (m.matches()) {
       return Integer.parseInt(m.group(1));
    }
    final String signalNames[] = {"OUT"};
    for (int signal=0; signal<signalNames.length; signal++) {
       if (function.getSignal().matches(signalNames[signal])) {
          return 8+signal;
       }
    }
    throw new RuntimeException("function '" + function.getSignal() + "' does not match expected pattern");
 }
   
   @Override
   public boolean useAliases(PinInformation pinInfo) {
      return false;
   }

   @Override
   public String getTitle() {
      return "Analogue Comparator";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used for Analogue Comparator";
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      // TODO Auto-generated method stub
      return null;
   }
}