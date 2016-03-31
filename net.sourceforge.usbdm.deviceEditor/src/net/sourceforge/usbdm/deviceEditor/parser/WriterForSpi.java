package net.sourceforge.usbdm.deviceEditor.parser;

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
public class WriterForSpi extends WriterBase {

   static final String ALIAS_BASE_NAME       = "spi_";
   static final String CLASS_BASE_NAME       = "Spi";
   static final String INSTANCE_BASE_NAME    = "spi";

   public WriterForSpi(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
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
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      String instance  = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getFunctionIndex(mappingInfo.getFunctions().get(fnIndex)));
      return "const " + DeviceInfo.NAME_SPACE + "::PcrTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }
//   /* (non-Javadoc)
//    * @see InstanceWriter#needPcrTable()
//    */
//   @Override
//   public boolean needPeripheralInformationClass() throws Exception {
//      boolean required = fOwner.getFunctions().size() > 0;
//      if (!required) {
//         if ((fOwner.getClockReg() != null) || (fOwner.getClockMask() != null)) {
//            throw new Exception("Unexpected clock information for non-present peripheral " + fOwner.peripheralName);
//         }
//      }
//      return required;
//   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      final String signalNames[] = {"SCK", "SIN|MISO", "SOUT|MOSI", "PCS0|PCS|SS", "PCS1", "PCS2", "PCS3", "PCS4", "PCS5"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignal().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + function.getSignal());
   }
   
   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing an SPI pin\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using spi0_PCS0 = const USBDM::Spi0Pin<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam spiPinNum    SPI pin number (index into SpiInfo[])\n"+
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
   public String getExtraDefinitions() {
      String name = getClassName();
      StringBuffer buff = new StringBuffer();
      for (int index=0; index<=5; index++) {
         buff.append(String.format("using %s_PCS%s = USBDM::PcrTable_T<USBDM::%sInfo, %s>;\n", name, index, name, index+3));
      }
      return buff.toString();
   }

   @Override
   public String getTitle() {
      return "Serial Peripheral Interface";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used for SPI functions";
   }
}