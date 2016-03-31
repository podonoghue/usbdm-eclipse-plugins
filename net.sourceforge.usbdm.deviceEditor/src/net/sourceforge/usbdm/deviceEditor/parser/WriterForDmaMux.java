package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInformation;
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
public class WriterForDmaMux extends WriterBase {

   public WriterForDmaMux(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
   }

   static final String ALIAS_BASE_NAME       = "vref_";
   static final String CLASS_BASE_NAME       = "Vref";
   static final String INSTANCE_BASE_NAME    = "vref";

   /* (non-Javadoc)
    * @see InstanceWriter#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String signalName, String alias) {
      return ALIAS_BASE_NAME+alias;
   }

   /* (non-Javadoc)
    * @see InstanceWriter#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getFunctions().get(fnIndex).getSignal();
      return INSTANCE_BASE_NAME+instance+"_"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * @throws Exception 
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getFunctionIndex(mappingInfo.getFunctions().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }
   
   @Override
   public void writeInfoClass(DeviceInformation deviceInformation, DocumentUtilities writer) throws IOException {
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
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
         " * Convenience class representing a VREF\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using Vref = const USBDM::Vref<VrefInfo>;\n"+
         " * @endcode\n"+
         " *\n"+
         " */\n";
   
   @Override
   public String getTemplate() {
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s = Vref<%sInfo>;\n\n",
            getClassName(), getClassName());
   }

   @Override
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return super.getAlias(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }

   @Override
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " + getDefinition(mappingInfo, fnIndex);
   }

   @Override
   public boolean useAliases(PinInformation pinInfo) {
      return false;
   }

   @Override
   public String getTitle() {
      return "Direct Memory Access (DMA)";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used Direct Memory Access (DMA)";
   }

}