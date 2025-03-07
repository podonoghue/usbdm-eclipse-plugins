package net.sourceforge.usbdm.configEditor.parser;

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
public class WriterForUart extends WriterBase {

   static final String ALIAS_BASE_NAME       = "uart_";
   static final String CLASS_BASE_NAME       = "Uart";
   static final String INSTANCE_BASE_NAME    = "uart";

   public WriterForUart(DeviceFamily deviceFamily) {
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
      int signal       = getFunctionIndex(mappingInfo.functions.get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, fOwner.getBaseName(), signal));
      return sb.toString();
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
      final String signalNames[] = {"TX", "RX", "RTS_b|RTS", "CTS_b|CTS", "COL|COL_b"};
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
         " * using uart0_PCS0 = const USBDM::Uart0Pin<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam uartPinNum    SPI pin number (index into UartInfo[])\n"+
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
      return "Uart_Group";
   }

   @Override
   public String getGroupTitle() {
      return "UART, Universal Asynchonous Receiver/Transmitter";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Pins used for UART functions";
   }
}