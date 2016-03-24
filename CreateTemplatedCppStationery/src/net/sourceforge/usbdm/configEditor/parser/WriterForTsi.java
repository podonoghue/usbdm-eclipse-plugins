package net.sourceforge.usbdm.configEditor.parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo;
import net.sourceforge.usbdm.configEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.configEditor.information.MappingInfo;
import net.sourceforge.usbdm.configEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.configEditor.information.PinInformation;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForTsi extends WriterBase {

   static final String ALIAS_BASE_NAME       = "tsi_";
   static final String CLASS_BASE_NAME       = "Tsi";
   static final String INSTANCE_BASE_NAME    = "tsi";

   public WriterForTsi(DeviceFamily deviceFamily) {
      super(deviceFamily);
   }

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
      String instance = mappingInfo.functions.get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.functions.get(fnIndex).getSignal();
      return INSTANCE_BASE_NAME+instance+"_"+signal;
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
      String instance  = mappingInfo.functions.get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getFunctionIndex(mappingInfo.functions.get(fnIndex)));
      return "const " + DeviceInfo.NAME_SPACE + "::PcrTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }
   /* (non-Javadoc)
    * @see InstanceWriter#needPcrTable()
    */
   @Override
   public boolean needPeripheralInformationClass() {
      return true;
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
//    System.err.println("function.fSignal = "+function.fSignal);
    Pattern p = Pattern.compile("CH(\\d+)");
    Matcher m = p.matcher(function.getSignal());
    if (m.matches()) {
       return Integer.parseInt(m.group(1));
    }
    throw new RuntimeException("function '" + function.getSignal() + "' does not match expected pattern");
 }
   
   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing an SPI pin\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using tsi0_PCS0 = const USBDM::Tsi0Pin<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam tsiPinNum    SPI pin number (index into TsiInfo[])\n"+
         " */\n";

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
   public String getGroupName() {
      return "TSI_Group";
   }

   @Override
   public String getGroupTitle() {
      return "TSI, Low-leakage Wake-up Unit";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used for Low-leakage Wake-up Unit";
   }

}