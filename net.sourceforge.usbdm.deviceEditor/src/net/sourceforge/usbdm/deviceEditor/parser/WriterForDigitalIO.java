package net.sourceforge.usbdm.deviceEditor.parser;

import java.io.IOException;
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
public class WriterForDigitalIO extends Peripheral {

   static final String ALIAS_PREFIX       = "gpio_";

   public WriterForDigitalIO(String basename, String instance, PeripheralTemplateInformation template, DeviceInfo deviceInfo) {
      super(basename, instance, template, deviceInfo);
   }

   @Override
   public String getTitle() {
      return"Digital Input/Output";
   }

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
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getFunctionIndex(mappingInfo.getFunctions().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }

   @Override
   public boolean needPCRTable() {
      return false;
   }

   @Override
   public String getPcrDefinition() {
      return String.format(
            "   //! Value for PCR (including MUX value)\n"+
            "   static constexpr uint32_t pcrValue  = GPIO_DEFAULT_PCR;\n\n"
            );
   }

   static final String TEMPLATE_DOCUMENTATION_1 = 
     "/**\n"+
     " * @brief Convenience template for %s. See @ref Gpio_T\n"+
     " *\n"+
     " * <b>Usage</b>\n"+
     " * @code\n"+
     " * // Instantiate for bit 3 of %s\n"+
     " * %s<3> %s3\n"+
     " *\n"+
     " * // Set as digital output\n"+
     " * %s3.setOutput();\n"+
     " *\n"+
     " * // Set pin high\n"+
     " * %s3.set();\n"+
     " *\n"+
     " * // Set pin low\n"+
     " * %s3.clear();\n"+
     " *\n"+
     " * // Toggle pin\n"+
     " * %s3.toggle();\n"+
     " *\n"+
     " * // Set pin to boolean value\n"+
     " * %s3.write(true);\n"+
     " *\n"+
     " * // Set pin to boolean value\n"+
     " * %s3.write(false);\n"+
     " *\n"+
     " * // Set as digital input\n"+
     " * %s3.setInput();\n"+
     " *\n"+
     " * // Read pin as boolean value\n"+
     " * bool x = %s3.read();\n"+
     " * @endcode\n"+
     " *\n"+
     " * @tparam bitNum        Bit number in the port\n"+
     " */\n";
   static final String TEMPLATE_DOCUMENTATION_2 = 
     "/**\n"+
     " * @brief Convenience template for %s fields. See @ref Field_T\n"+
     " *\n"+
     " * <b>Usage</b>\n"+
     " * @code\n"+
     " * // Instantiate for bit 6 down to 3 of %s\n"+
     " * %sField<6,3> %s6_3\n"+
     " *\n"+
     " * // Set as digital output\n"+
     " * %s6_3.setOutput();\n"+
     " *\n"+
     " * // Write value to field\n"+
     " * %s6_3.write(0x53);\n"+
     " *\n"+
     " * // Clear all of field\n"+
     " * %s6_3.bitClear();\n"+
     " *\n"+
     " * // Clear lower two bits of field\n"+
     " * %s6_3.bitClear(0x3);\n"+
     " *\n"+
     " * // Set lower two bits of field\n"+
     " * %s6_3.bitSet(0x3);\n"+
     " *\n"+
     " * // Set as digital input\n"+
     " * %s6_3.setInput();\n"+
     " *\n"+
     " * // Read pin as int value\n"+
     " * int x = %s6_3.read();\n"+
     " * @endcode\n"+
     " *\n"+
     " * @tparam left          Bit number of leftmost bit in port (inclusive)\n"+
     " * @tparam right         Bit number of rightmost bit in port (inclusive)\n"+
     " */\n";
//   template<int left, int right, uint32_t defPcrValue=GPIO_DEFAULT_PCR> using GpioAField = Field_T<GpioAInfo, left, right, defPcrValue>;

   @Override
   public String getCTemplate() {  
      StringBuffer buff = new StringBuffer();
      buff.append(TEMPLATE_DOCUMENTATION_1.replaceAll("%s", getClassName()));
      buff.append(String.format(
            "template<uint8_t bitNum> using %s = Gpio_T<%sInfo, bitNum>;\n\n",
            getClassName(), getClassName()));
      buff.append(TEMPLATE_DOCUMENTATION_2.replaceAll("%s", getClassName()));
      buff.append(String.format(
            "template<int left, int right> using %sField = Field_T<%sInfo, left, right>;\n\n",
            getClassName(), getClassName()));
      return buff.toString();
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
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      StringBuffer sb = new StringBuffer();
      
      // Base address
      sb.append(String.format(
            "   //! PORT Hardware base pointer\n"+
            "   static constexpr uint32_t pcrAddress   = %s\n\n",
            getName().replaceAll("GPIO", "PORT")+"_BasePtr;"
            ));

      // Base address
      sb.append(String.format(
            "   //! GPIO Hardware base pointer\n"+
            "   static constexpr uint32_t gpioAddress   = %s\n\n",
            getName().replaceAll("PORT", "GPIO")+"_BasePtr;"
            ));

      sb.append(getPcrDefinition());
      
      if (getClockMask() != null) {
         sb.append(String.format(
               "   //! Clock mask for peripheral\n"+
               "   static constexpr uint32_t clockMask = %s;\n\n",
               getClockMask()));
      }
      if (getClockReg() != null) {
         sb.append(String.format(
               "   //! Address of clock register for peripheral\n"+
               "   static constexpr uint32_t clockReg  = %s;\n\n",
               "SIM_BasePtr+offsetof(SIM_Type,"+getClockReg()+")"));
      }
      if (getIrqNumsAsInitialiser() != null) {
         sb.append(String.format(
               "   //! Number of IRQs for hardware\n"+
               "   static constexpr uint32_t irqCount  = %s;\n\n",
               getIrqCount()));
         sb.append(String.format(
               "   //! IRQ numbers for hardware\n"+
               "   static constexpr IRQn_Type irqNums[]  = {%s};\n\n",
               getIrqNumsAsInitialiser()));
      }
      pinMappingHeaderFile.write(sb.toString());
   }

}