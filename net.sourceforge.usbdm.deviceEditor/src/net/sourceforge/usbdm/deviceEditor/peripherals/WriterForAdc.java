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
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForAdc extends PeripheralWithState {      

   static private final String ALIAS_PREFIX = "adc_";
   
   /** Functions that use this writer */
   protected InfoTable fDmFunctions = new InfoTable("InfoDM");
         
   /** Functions that use this writer */
   protected InfoTable fDpFunctions = new InfoTable("InfoDP");
         
   public WriterForAdc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      loadModels();
   }

   @Override
   public String getTitle() {
      return "Analogue Input";
   }

   @Override
   public String getAliasName(String signalName, String alias) {
      Pattern p = Pattern.compile(".*(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(signalName);
      if (!m.matches()) {
         throw new RuntimeException("Function " + signalName +" does not match expected pattern");
      }
      String signalType = m.group(1);
      if (signalType.equalsIgnoreCase("SE")) {
         return ALIAS_PREFIX+alias;
      }
      else if (signalType.equalsIgnoreCase("DM")) {
         return null;
      }
      else if (signalType.equalsIgnoreCase("DP")) {
         return null;
      }
      return null;
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return getClassName()+instance+"_se"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal = getSignalIndex(mappingInfo.getSignals().get(fnIndex));
      return String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName()+"Channel", signal);
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(function.getSignalName());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
      }
      int index = Integer.parseInt(m.group(2));
      if ((m.group(3) != null) && m.group(3).equalsIgnoreCase("a")) {
         index += 32;
      }
      return index;
   }
   
   @Override
   public boolean needPCRTable() {
      boolean required = 
            (fInfoTable.table.size() +
                  fDpFunctions.table.size() + 
                  fDmFunctions.table.size()) > 0;
      return required;
   }

   @Override
   protected void addSignalToTable(Signal function) {
      InfoTable fFunctions = null;
      
      Pattern p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(function.getSignalName());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
      }
      int signalIndex = getSignalIndex(function);
      String signalType = m.group(1);
      if (signalType.equalsIgnoreCase("SE")) {
         fFunctions = super.fInfoTable;
      }
      else if (signalType.equalsIgnoreCase("DM")) {
         fFunctions = fDmFunctions;
      }
      else if (signalType.equalsIgnoreCase("DP")) {
         fFunctions = fDpFunctions;
      }
      if (fFunctions == null) {
         throw new RuntimeException("Illegal function " + function.toString());
      }
      if (signalIndex>=fFunctions.table.size()) {
         fFunctions.table.setSize(signalIndex+1);
      }
      if ((fFunctions.table.get(signalIndex) != null) && 
            (fFunctions.table.get(signalIndex) != function)) {
         throw new RuntimeException("Multiple functions mapped to index new = " + function + ", old = " + fFunctions.table.get(signalIndex));
      }
      fFunctions.table.setElementAt(function, signalIndex);
   }

   @Override
   public ArrayList<InfoTable> getSignalTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fInfoTable);
      rv.add(fDpFunctions);
      rv.add(fDmFunctions);
      return rv;
   }

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      StringBuffer sb = new StringBuffer();
      sb.append(substitute(fData.fTemplate));
      pinMappingHeaderFile.write(sb.toString());
   }

   @Override
   public BaseModel[] getModels(BaseModel parent) {
      fData.fModels[0].setParent(parent);
      return fData.fModels;
   }

   @Override
   public void getVariables(Map<String, String> variableMap, VectorTable vectorTable) {
      super.getVariables(variableMap, vectorTable);
      final String headerFileName = getBaseName().toLowerCase()+".h";
      boolean handlerSet = false;
      for (InterruptEntry entry:vectorTable.getEntries()) {
         if ((entry != null) && entry.getName().startsWith(fName)) {
            if (isCTrueValue("IRQ_HANDLER")) {
               entry.setHandlerName(DeviceInfo.NAME_SPACE+"::"+getClassName()+"::irqHandler");
               entry.setClassMemberUsedAsHandler(true);
               handlerSet = true;
            }
         }
      }
      if (handlerSet) {
         String headers = variableMap.get("VectorsIncludeFiles");
         if (!headers.contains(headerFileName)) {
            // Add include file
            variableMap.put("VectorsIncludeFiles", headers + "#include \""+headerFileName+"\"\n");
         }
      }
   }

}