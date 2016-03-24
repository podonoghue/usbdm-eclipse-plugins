package net.sourceforge.usbdm.configEditor.parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.configEditor.information.DeviceInfo;
import net.sourceforge.usbdm.configEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.configEditor.information.MappingInfo;
import net.sourceforge.usbdm.configEditor.information.PeripheralFunction;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForPort extends WriterBase {

   static final String ALIAS_BASE_NAME       = "port_";
   static final String CLASS_BASE_NAME       = "Port";
   static final String INSTANCE_BASE_NAME    = "port";

   public WriterForPort(DeviceFamily deviceFamily) {
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
      return false;
   }

   @Override
   public String getPcrValue() {
      return String.format(
            "   //! Base value for PCR (including MUX value)\n"+
            "   static constexpr uint32_t pcrValue  = GPIO_DEFAULT_PCR;\n\n"
            );
   }

   static final String TEMPLATE_DOCUMENTATION = 
     "/**                                                             \n"+
     " * @brief Convenience template for GPIOA. See @ref Gpio_T  \n"+
     " *                                                              \n"+
     " * <b>Usage</b>                                                 \n"+
     " * @code                                                        \n"+
     " * // Instantiate for bit 3 of port                             \n"+
     " * GpioA<3> Gpio3                                               \n"+
     " *                                                              \n"+
     " * // Set as digital output                                     \n"+
     " * Gpio3.setOutput();                                           \n"+
     " *                                                              \n"+
     " * // Set pin high                                              \n"+
     " * Gpio3.set();                                                 \n"+
     " *                                                              \n"+
     " * // Set pin low                                               \n"+
     " * Gpio3.clear();                                               \n"+
     " *                                                              \n"+
     " * // Toggle pin                                                \n"+
     " * Gpio3.toggle();                                              \n"+
     " *                                                              \n"+
     " * // Set pin to boolean value                                  \n"+
     " * Gpio3.write(true);                                           \n"+
     " *                                                              \n"+
     " * // Set pin to boolean value                                  \n"+
     " * Gpio3.write(false);                                          \n"+
     " *                                                              \n"+
     " * // Set as digital input                                      \n"+
     " * Gpio3.setInput();                                            \n"+
     " *                                                              \n"+
     " * // Read pin as boolean value                                 \n"+
     " * bool x = Gpio3.read();                                       \n"+
     " * @endcode                                                     \n"+
     " *                                                              \n"+
     " * @tparam bitNum        Bit number in the port                 \n"+
     " */                                                             \n";
   
   /* (non-Javadoc)
    * @see InstanceWriter#getTemplate(FunctionTemplateInformation)
    */
   @Override
   public String getTemplate() {   
      return TEMPLATE_DOCUMENTATION+String.format(
            "template<uint8_t bitNum> using %s = Gpio_T<%sInfo, bitNum>;\n\n",
            fOwner.getBaseName(), fOwner.getBaseName());
   }

   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      Pattern p = Pattern.compile("(\\d+).*");
      Matcher m = p.matcher(function.getSignal());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignal() + " does not match expected pattern");
      }
      int signalIndex = Integer.parseInt(m.group(1));
      return signalIndex;
   }

   @Override
   public String getInfoConstants() {
      StringBuffer buff = new StringBuffer();
      
      // Base address
      buff.append(String.format(
            "   //! PORT Hardware base pointer\n"+
            "   static constexpr uint32_t pcrAddress   = %s\n\n",
            fOwner.getPeripheralName()+"_BasePtr;"
            ));

      // Base address
      buff.append(String.format(
            "   //! GPIO Hardware base pointer\n"+
            "   static constexpr uint32_t gpioAddress   = %s\n\n",
            fOwner.getPeripheralName().replaceAll("PORT", "GPIO")+"_BasePtr;"
            ));

      buff.append(getPcrValue());
      
      if (fOwner.getClockMask() != null) {
         buff.append(String.format(
               "   //! Clock mask for peripheral\n"+
               "   static constexpr uint32_t clockMask = %s;\n\n",
               fOwner.getClockMask()));
      }
      if (fOwner.getClockReg() != null) {
         buff.append(String.format(
               "   //! Address of clock register for peripheral\n"+
               "   static constexpr uint32_t clockReg  = %s;\n\n",
               "SIM_BasePtr+offsetof(SIM_Type,"+fOwner.getClockReg()+")"));
      }
      if (fOwner.getIrqNumsAsInitialiser() != null) {
         buff.append(String.format(
               "   //! Number of IRQs for hardware\n"+
               "   static constexpr uint32_t irqCount  = %s;\n\n",
               fOwner.getIrqCount()));
         buff.append(String.format(
               "   //! IRQ numbers for hardware\n"+
               "   static constexpr IRQn_Type irqNums[]  = {%s};\n\n",
               fOwner.getIrqNumsAsInitialiser()));
      }
      return buff.toString();
   }

   @Override
   public String getGroupName() {
      throw new RuntimeException("Unexpected call to empty function");
   }

   @Override
   public String getGroupTitle() {
      throw new RuntimeException("Unexpected call to empty function");
   }

   @Override
   public String getGroupBriefDescription() {
      throw new RuntimeException("Unexpected call to empty function");
   }
   
}