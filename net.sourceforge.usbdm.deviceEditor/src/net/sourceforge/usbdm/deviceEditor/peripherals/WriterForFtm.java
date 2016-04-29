package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

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
      createValue(FTM_SC_CLKS_KEY,     "1", "FTM_SC.CLKS Clock source");
      createValue(FTM_SC_PS_KEY,       "0", "FTM_SC.PS Clock prescaler");
      createValue(FTM_IRQ_HANDLER_KEY, "0", "Handler for IRQ", 0, 1);
      createValue(FTM_IRQ_LEVEL_KEY,   "0", "IRQ Level in NVIC [0-15]", 0, 15);
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
      return String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName()+"Channel", signal);
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
   private static final String FTM_IRQ_HANDLER_KEY = "FTM_IRQ_HANDLER";
   private static final String FTM_IRQ_LEVEL_KEY   = "FTM_IRQ_LEVEL";
   
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
      BinaryModel model;
      
      model = new BinaryModel(models[0], this, FTM_IRQ_HANDLER_KEY, "");
      model.setName(fVariableMap.get(FTM_IRQ_HANDLER_KEY).name);
      model.setToolTip("The interrupt handler may be a static member function or\n"+
            "may be set by use of the setCallback() method");
      model.setValue0("No handler installed", "0");
      model.setValue1("Handler installed",    "1");
      
      VariableModel vModel = new VariableModel(models[0], this, FTM_IRQ_LEVEL_KEY, "");
      vModel.setName(fVariableMap.get(FTM_IRQ_LEVEL_KEY).name);
      vModel.setToolTip("Sets the priority level used to configure the NVIC");

      return models;
   }

   @Override
   public VariableInfo getVariableInfo(String key) {
      return fVariableMap.get(key);
   }

   @Override
   public void getVariables(Map<String, String> variableMap, VectorTable vectorTable) {
      final String headerFileName = getBaseName().toLowerCase()+".h";
      super.getVariables(variableMap, vectorTable);
      boolean handlerSet = false;
      for (InterruptEntry entry:vectorTable.getEntries()) {
         if ((entry != null) && entry.getName().startsWith(fName)) {
            if (getVariableInfo(FTM_IRQ_HANDLER_KEY).value.equals("1")) {
               entry.setHandlerName(DeviceInfo.NAME_SPACE+"::"+getClassName()+"::irqHandler");
               entry.setClassMemberUsedAsHandler(true);
               handlerSet = true;
            }
         }
      }
      if (handlerSet) {
         String headers = variableMap.get("VectorsIncludeFiles");
         if (!headers.contains(headerFileName)) {
            variableMap.put("VectorsIncludeFiles", headers + "#include \""+headerFileName+"\"\n");
         }
      }
   }

}
