package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;

/**
 * Class encapsulating the code for writing an instance of PwmIO (FTM)
 */
public class WriterForFTM extends PeripheralWithState {

   private static final String ALIAS_PREFIX        = "ftm_";

   /** Functions that use this writer */
   protected InfoTable fQuadFunctions = new InfoTable("InfoQUAD");

   /** Functions that use this writer */
   protected InfoTable fFaultFunctions = new InfoTable("InfoFAULT");

   /** Functions that use this writer */
   protected InfoTable fClkinFunctions = new InfoTable("InfoCLKIN");

   public WriterForFTM(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(FTM_SC_CLKS_KEY, "1", "FTM_SC.CLKS Clock source");
      createValue(FTM_SC_PS_KEY,   "0", "FTM_SC.PS Clock prescaler");
   }

   @Override
   public String getTitle() {
      return "PWM, Input capture and Output compare";
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
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName().replaceAll("CH", "ch");
      return getClassName()+instance+"_"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal = getSignalIndex(mappingInfo.getSignals().get(fnIndex));
      return String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal);
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("CH(\\d+)");
      Matcher m = p.matcher(function.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      final String quadNames[] = {"QD_PHA", "QD_PHB"};
      for (int signal=0; signal<quadNames.length; signal++) {
         if (function.getSignalName().matches(quadNames[signal])) {
            return signal;
         }
      }
      final String clockNames[] = {"CLKIN0", "CLKIN1"};
      for (int signal=0; signal<clockNames.length; signal++) {
         if (function.getSignalName().matches(clockNames[signal])) {
            return signal;
         }
      }
      final String faultNames[] = {"FLT0", "FLT1", "FLT2", "FLT3"};
      for (int signal=0; signal<faultNames.length; signal++) {
         if (function.getSignalName().matches(faultNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("function '" + function.getSignalName() + "' does not match expected pattern");
   }

   @Override
   public boolean needPCRTable() {
      boolean required = 
            (fInfoTable.table.size() +
             fQuadFunctions.table.size() + 
             fFaultFunctions.table.size()) > 0;
                  return required;
   }

   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
               " * Convenience template class representing a FTM\n"+
               " *\n"+
               " * Example\n"+
               " * @code\n"+
               " * // Instantiate the ftm channel (for FTM0 CH6)\n"+
               " * const USBDM::Ftm0<6>   ftm0_ch6;\n"+
               " *\n"+
               " * // Initialise PWM with initial period and alignment\n"+
               " * ftm0_ch6.setPwmOutput(200, USBDM::ftm_leftAlign);\n"+
               " *\n"+
               " * // Change period (in ticks)\n"+
               " * ftm0_ch6.setPeriod(500);\n"+
               " *\n"+
               " * // Change duty cycle (in percent)\n"+
               " * ftm0_ch6.setDutyCycle(45);\n"+
               " * @endcode\n"+
               " *\n"+
               " * @tparam channel    Timer channel\n"+
               " */\n";
   
   @Override
   public String getCTemplate() {
      return TEMPLATE_DOCUMENTATION + String.format(
            "template<uint8_t channel> using %s = Tmr_T<%sInfo, channel>;\n\n",
            getClassName(), getClassName(), getName());
   }

   @Override
   protected void addSignalToTable(Signal function) {
      InfoTable fFunctions = null;

      int signalIndex = -1;

      Pattern p = Pattern.compile(".*CH(\\d+)");
      Matcher m = p.matcher(function.getSignalName());
      if (m.matches()) {
         fFunctions = fInfoTable;
         signalIndex = Integer.parseInt(m.group(1));
      }
      if (fFunctions == null) {
         final String quadNames[] = {"QD_PHA", "QD_PHB"};
         for (signalIndex=0; signalIndex<quadNames.length; signalIndex++) {
            if (function.getSignalName().endsWith(quadNames[signalIndex])) {
               fFunctions = fQuadFunctions;
               break;
            }
         }
      }
      if (fFunctions == null) {
         final String faultNames[] = {"FLT0", "FLT1", "FLT2", "FLT3"};
         for (signalIndex=0; signalIndex<faultNames.length; signalIndex++) {
            if (function.getSignalName().endsWith(faultNames[signalIndex])) {
               fFunctions = fFaultFunctions;
               break;
            }
         }
      }
      if (fFunctions == null) {
         final String clkinNames[] = {"CLKIN0", "CLKIN1"};
         for (signalIndex=0; signalIndex<clkinNames.length; signalIndex++) {
            if (function.getSignalName().matches(clkinNames[signalIndex])) {
               fFunctions = fClkinFunctions;
               break;
            }
         }
      }
      if (fFunctions == null) {
         throw new RuntimeException("function '" + function.getSignalName() + "' does not match expected pattern");
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
   public ArrayList<InfoTable> getSignalTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fInfoTable);
      rv.add(fFaultFunctions);
      rv.add(fQuadFunctions);
      rv.add(fClkinFunctions);
      return rv;
   }

   static final String TEMPLATE = 
         "   //! Default value for tmr->SC register\n"+
         "   static constexpr uint32_t scValue  = FTM_SC_CLKS(${FTM_SC_CLKS})|FTM_SC_PS(${FTM_SC_PS});\n\n";

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      StringBuffer sb = new StringBuffer();
      sb.append(substitute(TEMPLATE, fVariableMap));
      pinMappingHeaderFile.write(sb.toString());
   }

   private static final String FTM_SC_CLKS_KEY     = "FTM_SC_CLKS";
   private static final String FTM_SC_PS_KEY       = "FTM_SC_PS";

   @Override
   public BaseModel[] getModels(BaseModel parent) {
      BaseModel models[] = {
            new CategoryModel(parent, getName(), getDescription()),
      };

      new SimpleSelectionModel(models[0], this, FTM_SC_CLKS_KEY, "[FTM_SC_CLKS]") {
         {
            setName("Clock source");
            setToolTip("Selects the clock source for the module");
         }
         @Override
         protected String[] getChoicesArray() {
            final String SELECTION_NAMES[] = {
                  "Disabled",
                  "System clock",
                  "Fixed frequency clock",
                  "External clock",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0", "1", "2", "3",
                  "1", // Default
            };
            return VALUES;
         }
      };

      new SimpleSelectionModel(models[0], this, FTM_SC_PS_KEY, "[FTM_SC_PS]") {
         {
            setName("Clock prescaler");
            setToolTip("Selects the prescaler for the module");
         }
         @Override
         protected String[] getChoicesArray() {
            final String SELECTION_NAMES[] = {
                  "Divide by 1",
                  "Divide by 2",
                  "Divide by 4",
                  "Divide by 8",
                  "Divide by 16",
                  "Divide by 32",
                  "Divide by 64",
                  "Divide by 128",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0", "1", "2", "3", "4", "5", "6", "7",
                  "0", // Default
            };
            return VALUES;
         }
      };
      return models;
   }

   @Override
   public VariableInfo getVariableInfo(String key) {
      return fVariableMap.get(key);
   }


}
