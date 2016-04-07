package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of PwmIO (TPM)
 */
public class WriterForPwmIO_TPM extends Peripheral {

   static final String ALIAS_PREFIX          = "tpm_";
   
   /** Functions that use this writer */
   protected InfoTable fQuadFunctions = new InfoTable("InfoQUAD");
         
   /** Functions that use this writer */
   protected InfoTable fFaultFunctions = new InfoTable("InfoFAULT");
         
   /** Functions that use this writer */
   protected InfoTable fClkinFunctions = new InfoTable("InfoCLKIN");
         
   public WriterForPwmIO_TPM(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Input capture, Output compare";
   }

   @Override
   public String getAliasName(String signalName, String alias) {
      if (signalName.matches(".*ch\\d+")) {
         return ALIAS_PREFIX+alias;
      }
      return null;
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignal().replaceAll("CH", "ch");
      return getClassName()+instance+"_"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal = getFunctionIndex(mappingInfo.getSignals().get(fnIndex));
      return String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal);
   }

   @Override
   public int getFunctionIndex(Signal function) {
      Pattern p = Pattern.compile("CH(\\d+)");
      Matcher m = p.matcher(function.getSignal());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      final String quadNames[] = {"QD_PHA", "QD_PHB"};
      for (int signal=0; signal<quadNames.length; signal++) {
         if (function.getSignal().matches(quadNames[signal])) {
            return signal;
         }
      }
      final String clockNames[] = {"CLKIN0", "CLKIN1"};
      for (int signal=0; signal<clockNames.length; signal++) {
         if (function.getSignal().matches(clockNames[signal])) {
            return signal;
         }
      }
      final String faultNames[] = {"FLT0", "FLT1", "FLT2", "FLT3"};
      for (int signal=0; signal<faultNames.length; signal++) {
         if (function.getSignal().matches(faultNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("function '" + function.getSignal() + "' does not match expected pattern");
   }

   @Override
   public boolean needPCRTable() {
      boolean required = 
            (fPeripheralFunctions.table.size() +
             fQuadFunctions.table.size() + 
             fFaultFunctions.table.size()) > 0;
      return required;
   }

   static final String TEMPLATE_DOCUMENTATION = 
   "/**\n"+
   " * Convenience template class representing a TPM\n"+
   " *\n"+
   " * Example\n"+
   " * @code\n"+
   " * // Instantiate the tpm channel (for TPM0 CH6)\n"+
   " * const USBDM::Tpm0<6>   tpm0_ch6;\n"+
   " *\n"+
   " * // Initialise PWM with initial period and alignment\n"+
   " * tpm0_ch6.setPwmOutput(200, USBDM::ftm_leftAlign);\n"+
   " *\n"+
   " * // Change period (in ticks)\n"+
   " * tpm0_ch6.setPeriod(500);\n"+
   " *\n"+
   " * // Change duty cycle (in percent)\n"+
   " * tpm0_ch6.setDutyCycle(45);\n"+
   " * @endcode\n"+
   " *\n"+
   " * @tparam channel    Timer channel\n"+
   " */\n";
   @Override
   public String getCTemplate() {
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s = TmrBase_T<%sInfo, channel>;\n\n",
            getClassName(), getClassName(), getName());
   }

   @Override
   protected void addFunctionToTable(Signal function) {
      InfoTable fFunctions = null;

      int signalIndex = -1;

      Pattern p = Pattern.compile(".*CH(\\d+)");
      Matcher m = p.matcher(function.getSignal());
      if (m.matches()) {
         fFunctions = fPeripheralFunctions;
         signalIndex = Integer.parseInt(m.group(1));
      }
      if (fFunctions == null) {
         final String quadNames[] = {"QD_PHA", "QD_PHB"};
         for (signalIndex=0; signalIndex<quadNames.length; signalIndex++) {
            if (function.getSignal().endsWith(quadNames[signalIndex])) {
               fFunctions = fQuadFunctions;
               break;
            }
         }
      }
      if (fFunctions == null) {
         final String faultNames[] = {"FLT0", "FLT1", "FLT2", "FLT3"};
         for (signalIndex=0; signalIndex<faultNames.length; signalIndex++) {
            if (function.getSignal().endsWith(faultNames[signalIndex])) {
               fFunctions = fFaultFunctions;
               break;
            }
         }
      }
      if (fFunctions == null) {
         final String clkinNames[] = {"CLKIN0", "CLKIN1"};
         for (signalIndex=0; signalIndex<clkinNames.length; signalIndex++) {
            if (function.getSignal().matches(clkinNames[signalIndex])) {
               fFunctions = fClkinFunctions;
               break;
            }
         }
      }
      if (fFunctions == null) {
         throw new RuntimeException("function '" + function.getSignal() + "' does not match expected pattern");
      }
      if (signalIndex>=fFunctions.table.size()) {
         fFunctions.table.setSize(signalIndex+1);
      }
      if ((fFunctions.table.get(signalIndex) != null) && 
            (fFunctions.table.get(signalIndex) != function)) {
         throw new RuntimeException("Multiple functions mapped to index = "+signalIndex+"\n new = " + function + ",\n old = " + fFunctions.table.get(signalIndex));
      }
      fFunctions.table.setElementAt(function, signalIndex);
   }

   @Override
   public ArrayList<InfoTable> getFunctionTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fPeripheralFunctions);
      rv.add(fFaultFunctions);
      rv.add(fQuadFunctions);
      rv.add(fClkinFunctions);
      return rv;
   }

   int ftm_sc_clks = 0x01;
   int ftm_sc_ps   = 0x01;
   
   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      StringBuffer sb = new StringBuffer();
      super.writeInfoConstants(pinMappingHeaderFile);
      sb.append(String.format(
            "   //! Base value for tmr->SC register\n"+
            "   static constexpr uint32_t scValue  = TPM_SC_CMOD(0x%02X)|TPM_SC_PS(0x%02X);\n\n",
            ftm_sc_clks, ftm_sc_ps));
      InfoTable functions = fPeripheralFunctions;
      int lastChannel = -1;
      for (int index=0; index<functions.table.size(); index++) {
         if (functions.table.get(index) != null) {
            lastChannel = index;
         }
      }
      sb.append(String.format(
            "   static constexpr int NUM_CHANNELS  = %d;\n" +
            "\n",
            lastChannel+1));
      pinMappingHeaderFile.write(sb.toString());
   }
   
}
