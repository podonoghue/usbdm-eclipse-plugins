package net.sourceforge.usbdm.deviceEditor.parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForCmp extends Peripheral {
   
   static final String ALIAS_PREFIX       = "cmp_";

   public WriterForCmp(String basename, String instance, PeripheralTemplateInformation template, DeviceInfo deviceInfo) {
      super(basename, instance, template, deviceInfo);
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
   
}