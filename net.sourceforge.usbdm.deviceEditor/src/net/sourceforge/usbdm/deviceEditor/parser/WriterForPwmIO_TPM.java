package net.sourceforge.usbdm.deviceEditor.parser;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;

/**
 * Class encapsulating the code for writing an instance of PwmIO (TPM)
 */
public class WriterForPwmIO_TPM extends WriterBase {


   static final String ALIAS_BASE_NAME       = "tpm_";
   static final String CLASS_BASE_NAME       = "Tpm";
   static final String INSTANCE_BASE_NAME    = "tpm";
   
   public WriterForPwmIO_TPM(DeviceInfo deviceInfo, Peripheral peripheral) {
      super(deviceInfo, peripheral);
   }
   /* (non-Javadoc)
    * @see WriterForDigitalIO#getAliasName(java.lang.String)
    */
   @Override
   public String getAliasName(String signalName, String alias) {
//      System.err.println(String.format("getAliasName(%s,%s)", signalName, alias));
      if (signalName.matches(".*ch\\d+")) {
         return ALIAS_BASE_NAME+alias;
      }
      return null;
   }

   /* (non-Javadoc)
    * @see WriterForDigitalIO#getInstanceName(MappingInfo, int)
    */
   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getFunctions().get(fnIndex).getSignal().replaceAll("CH", "ch");
      return INSTANCE_BASE_NAME+instance+"_"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Tpm<b><i>1</b></i>&lt;<i><b>17</i></b>>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    */
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      String instance  = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      int    signal    = getFunctionIndex(mappingInfo.getFunctions().get(fnIndex));
      return String.format("const %s::%s%s<%d>", DeviceInfo.NAME_SPACE, CLASS_BASE_NAME, instance, signal);
   }

   @Override
   public boolean useGuard() {
      return true;
   }

   static final int QUAD_INDEX  = 8;
   static final int CLOCK_INDEX = 10;
   static final int FAULT_INDEX = 12;
   
   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      Pattern p = Pattern.compile("CH(\\d+)");
      Matcher m = p.matcher(function.getSignal());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      final String quadNames[] = {"QD_PHA", "QD_PHB"};
      for (int signal=0; signal<quadNames.length; signal++) {
         if (function.getSignal().matches(quadNames[signal])) {
            return QUAD_INDEX+signal;
         }
      }
      final String clockNames[] = {"CLKIN0", "CLKIN1"};
      for (int signal=0; signal<clockNames.length; signal++) {
         if (function.getSignal().matches(clockNames[signal])) {
            return CLOCK_INDEX+signal;
         }
      }
      final String faultNames[] = {"FLT0", "FLT1", "FLT2", "FLT3"};
      for (int signal=0; signal<faultNames.length; signal++) {
         if (function.getSignal().matches(faultNames[signal])) {
            return FAULT_INDEX+signal;
         }
      }
      throw new RuntimeException("function '" + function.getSignal() + "' does not match expected pattern");
   }

   static final String TEMPLATE_DOCUMENTATION = 
   "/**\n"+
   " * Convenience templated class representing a TPM\n"+
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
   public String getTemplate() {
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s = TmrBase_T<%sInfo, channel>;\n\n",
            getClassName(), getClassName(), getPeripheralName());
   }

   @Override
   public String getInfoConstants() {
      StringBuffer sb = new StringBuffer();
      sb.append(super.getInfoConstants());
      sb.append(String.format(
            "   //! Base value for tmr->SC register\n"+
            "   static constexpr uint32_t scValue  = %s;\n\n",
            getPeripheralName()+"_SC"));
      sb.append(String.format(
            "   //! Indexes of special functions in PcrInfo[] table\n"+
            "   static constexpr int QUAD_INDEX  = %d;\n" +
            "   static constexpr int CLOCK_INDEX = %d;\n" +
            "   static constexpr int FAULT_INDEX = %d;\n" +
            "\n",
            QUAD_INDEX, CLOCK_INDEX, FAULT_INDEX));
      InfoTable functions = fPeripheralFunctions;
      int lastChannel = -1;
      for (int index=0; index<functions.table.size(); index++) {
         if (index >= QUAD_INDEX) {
            break;
         }
         if (functions.table.get(index) != null) {
            lastChannel = index;
         }
      }
      sb.append(String.format(
            "   static constexpr int NUM_CHANNELS  = %d;\n" +
            "\n",
            lastChannel+1));
      return sb.toString();
   }

   @Override
   public String getGroupName() {
      return "PwmIO_Group";
   }

   @Override
   public String getTitle() {
      return "Input capture, Output compare";
   }

   @Override
   public String getGroupBriefDescription() {
      return "Allows use of port pins as PWM outputs";
   }

   @Override
   public void writeWizard(DocumentUtilities headerFile) throws IOException {
      headerFile.writeWizardSectionOpen("Clock settings for " + getPeripheralName());
      headerFile.writeWizardOptionSelectionPreamble(
            String.format("%s_SC.CMOD ================================\n//", getPeripheralName()), 
            0,
            null,
            String.format("%s_SC.CMOD Clock source", getPeripheralName()),
            String.format("Selects the clock source for the %s module. [%s_SC.CMOD]", getPeripheralName(), getPeripheralName()));
      headerFile.writeWizardOptionSelectionEnty("0", "Disabled");
      headerFile.writeWizardOptionSelectionEnty("1", "Internal clock");
      headerFile.writeWizardOptionSelectionEnty("2", "External clock");
      headerFile.writeWizardOptionSelectionEnty("3", "Reserved");
      headerFile.writeWizardDefaultSelectionEnty("1");
      headerFile.writeWizardOptionSelectionPreamble(
            String.format("%s_SC.PS ================================\n//",getPeripheralName()),
            1,
            null,
            String.format("%s_SC.PS Clock prescaler", getPeripheralName()),
            String.format("Selects the prescaler for the %s module. [%s_SC.PS]", getPeripheralName(), getPeripheralName()));
      headerFile.writeWizardOptionSelectionEnty("0", "Divide by 1");
      headerFile.writeWizardOptionSelectionEnty("1", "Divide by 2");
      headerFile.writeWizardOptionSelectionEnty("2", "Divide by 4");
      headerFile.writeWizardOptionSelectionEnty("3", "Divide by 8");
      headerFile.writeWizardOptionSelectionEnty("4", "Divide by 16");
      headerFile.writeWizardOptionSelectionEnty("5", "Divide by 32");
      headerFile.writeWizardOptionSelectionEnty("6", "Divide by 64");
      headerFile.writeWizardOptionSelectionEnty("7", "Divide by 128");
      headerFile.writeWizardDefaultSelectionEnty("0");
      headerFile.writeOpenNamespace(DeviceInfo.NAME_SPACE);
      headerFile.writeConstexpr(16, getPeripheralName()+"_SC", "(TPM_SC_CMOD(0x1)|TPM_SC_PS(0x0))");
      headerFile.writeCloseNamespace();
      headerFile.write("\n");
      headerFile.writeWizardSectionClose();
      //      headerFile.write( String.format(optionSectionClose));
   }

}
