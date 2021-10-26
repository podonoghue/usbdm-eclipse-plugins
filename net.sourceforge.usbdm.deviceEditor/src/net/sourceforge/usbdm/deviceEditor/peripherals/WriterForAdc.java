package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
public class WriterForAdc extends PeripheralWithState {      

   /** Signals that use this writer */
   protected InfoTable fDmFunctions = new InfoTable("InfoDM");

   /** Signals that use this writer */
   protected InfoTable fDpFunctions = new InfoTable("InfoDP");

   /** Signals that use this writer */
//   protected InfoTable fPgaFunctions = new InfoTable("InfoPGA");

   public WriterForAdc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
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
      return super.getAliasName(signalName, alias);
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      Signal signal = mappingInfo.getSignals().get(fnIndex);
      if (!signal.getSignalName().matches("^(SE)(\\d+)(a|b)?$")) {
         // Only single-ended channels can be declared
         return null;
      }
      int signalIndex = getSignalIndex(mappingInfo.getSignals().get(fnIndex));
      return String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassBaseName()+getInstance()+"::"+"Channel", signalIndex);
   }

   @Override
   public int getSignalIndex(Signal function) {
      Pattern p = Pattern.compile("^(SE|DM|DP)(\\d+)(a|b)?$");
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
                  fDmFunctions.table.size() /*+
                  fPgaFunctions.table.size()*/) > 0;
      return required;
   }

   @Override
   protected void addSignalToTable(Signal function) {
      final int DP_INDEX = 2;
      
      InfoTable fFunctions = null;

      Pattern p = Pattern.compile("PGA(\\d+)_(DM|DP)");
      Matcher m = p.matcher(function.getName());
      if (m.matches()) {
         p = Pattern.compile("(DM|DP)");
         m = p.matcher(function.getSignalName());
         if (m.matches()) {
//            System.out.println("Found " + function);
            String signalType = m.group(1);
            if (signalType.equalsIgnoreCase("DM")) {
               if (DP_INDEX>=fDmFunctions.table.size()) {
                  fDmFunctions.table.setSize(DP_INDEX+1);
               }
               fDmFunctions.table.setElementAt(function, DP_INDEX);
            }
            else if (signalType.equalsIgnoreCase("DP")) {
               if (DP_INDEX>=fDpFunctions.table.size()) {
                  fDpFunctions.table.setSize(DP_INDEX+1);
               }
               fDpFunctions.table.setElementAt(function, DP_INDEX);
               if (DP_INDEX>=super.fInfoTable.table.size()) {
                  super.fInfoTable.table.setSize(DP_INDEX+1);
               }
               super.fInfoTable.table.setElementAt(function, DP_INDEX);
            }
            return;
         }
      }
      p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      m = p.matcher(function.getSignalName());
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
//      rv.add(fPgaFunctions);
      return rv;
   }

}