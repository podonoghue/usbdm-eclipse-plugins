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
/**
 * @author podonoghue
 *
 */
public class WriterForLlwu extends Peripheral {

   static private final String ALIAS_PREFIX = "llwu_";

   public WriterForLlwu(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Low-leakage Wake-up Unit";
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
      Pattern p = Pattern.compile("P(\\d+)");
      Matcher m = p.matcher(function.getSignal());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      throw new RuntimeException("Signal "+function.getSignal()+" does not match expected pattern ");
   }
   
   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public boolean needPCRTable() {
      return true;
   }

}