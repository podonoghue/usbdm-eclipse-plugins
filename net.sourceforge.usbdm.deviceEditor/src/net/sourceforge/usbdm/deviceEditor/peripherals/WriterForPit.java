package net.sourceforge.usbdm.deviceEditor.peripherals;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of PIT
 */
public class WriterForPit extends PeripheralWithState {

   static final String ALIAS_PREFIX       = "pit_";

   /** Key for PIT_LDVAL */
   private static final String PIT_LDVAL_KEY = "PIT_LDVAL";
   
   public WriterForPit(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(PIT_LDVAL_KEY, "2000", "Reload value");
   }

   @Override
   public String getTitle() {
      return "Programmable Interrupt Timer";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignal();
      return getClassName()+instance+"_"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getFunctionIndex(mappingInfo.getSignals().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }

   @Override
   public int getFunctionIndex(Signal function) {
      final String signalNames[] = {"OUT"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignal().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal "+function.getSignal()+" does not match expected pattern ");
   }
   
   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience class representing a PIT\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using Pit = const USBDM::Pit<PitInfo>;\n"+
         " * @endcode\n"+
         " *\n"+
         " */\n";
   
   @Override
   public String getCTemplate() {
      return null;
//      return TEMPLATE_DOCUMENTATION + String.format(
//            "template<uint8_t channel> using %s = %s<%sInfo>;\n\n",
//            getClassName(), getClassName(), getClassName());
   }

   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return super.getAliasDeclaration(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }

   @Override
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " + getDefinition(mappingInfo, fnIndex);
   }

}