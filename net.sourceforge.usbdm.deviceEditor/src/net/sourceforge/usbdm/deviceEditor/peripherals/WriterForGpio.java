package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of GPIO
 */
public class WriterForGpio extends PeripheralWithState {

   public WriterForGpio(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return"Digital Input/Output";
   }

   @Override
   public String getAliasName(String signalName, String alias) {
      return getBaseName().toLowerCase()+"_"+alias;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int          signal = getSignalIndex(mappingInfo.getSignals().get(fnIndex));
      StringBuffer sb     = new StringBuffer();
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
            "   static constexpr uint32_t %s  = GPIO_DEFAULT_PCR;\n\n", DEFAULT_PCR_VALUE_NAME
            );
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("(\\d+).*");
      Matcher m = p.matcher(function.getSignalName());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
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
      
      writeInfoTemplate(pinMappingHeaderFile);
   }

   @Override
   public void modifyVectorTable(VectorTable vectorTable) {
      for (IrqVariable var : irqVariables) {
         modifyVectorTable(vectorTable, var, "Port");
      }
   }
   
}