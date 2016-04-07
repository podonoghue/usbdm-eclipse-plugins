package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForCmp extends Peripheral {
   
   static final String ALIAS_PREFIX       = "cmp_";

   public WriterForCmp(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Analogue Comparator";
   }

   @Override
   public String getAliasName(String signalName, String alias) {
      return ALIAS_PREFIX+alias;
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignal();
      return getClassName()+instance+"_"+signal;
   }
 
   @Override
   public int getFunctionIndex(Signal function) {
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
   
}