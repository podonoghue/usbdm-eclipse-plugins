package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
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

   public WriterForAdc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Analogue Input";
   }

   @Override
   protected void writeDeclarations() {
      
      super.writeDeclarations();
      
      // PGA channels appear as ADC channel #2
      final int PGA_INDEX = 2;
      
      // Differential channels (including Pga) - recognised by having the same code name for DP and DM 
      for (int index=0; index<fDpFunctions.table.size(); index++) {
         Signal dpSignal = fDpFunctions.table.get(index);
         Signal dmSignal = fDmFunctions.table.get(index);
         if ((dpSignal == null) || (dmSignal == null)) {
            continue;
         }
         Pin dpPin = dpSignal.getFirstMappedPinInformation().getPin();
         Pin dmPin = dmSignal.getFirstMappedPinInformation().getPin();
         if ((dpPin == Pin.UNASSIGNED_PIN) || (dmPin == Pin.UNASSIGNED_PIN)) {
            continue;
         }
         String cIdentifier = dpSignal.getCodeIdentifier();
         
         // Only considered a differential channel if DP and DM signals have same non-blank code identifier
         if (cIdentifier.isBlank() || !cIdentifier.equalsIgnoreCase(dmSignal.getCodeIdentifier())) {
            continue;
         }
         String description = dpSignal.getUserDescription();
         String location = dpPin.getLocation()+","+dmPin.getLocation();
         String type;
         if (index == PGA_INDEX) {
            description = description + " (Programmable gain amplifier differential input)";
            type = String.format("const %s", getClassBaseName()+getInstance()+"::"+"PgaChannel");
         }
         else {
            description = description + " (Differential input)";
            type = String.format("const %s<%d>", getClassBaseName()+getInstance()+"::"+"DiffChannel", index);
         }
         writeVariableDeclaration("", description, cIdentifier, type, location);
      }
      // Single-ended channels
      for (int index=0; index<fInfoTable.table.size(); index++) {
         Signal signal = fInfoTable.table.get(index);
         if (signal == null) {
            continue;
         }
         Pin pin = signal.getFirstMappedPinInformation().getPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            continue;
         }
         String cIdentifier = signal.getCodeIdentifier();
         if (cIdentifier.isBlank()) {
            continue;
         }
         String description = signal.getUserDescription();
         String type;
         if (index == PGA_INDEX) {
            cIdentifier = cIdentifier+"Pga";
            description = description + " (Programmable gain amplifier)";
            type = String.format("const %s", getClassBaseName()+getInstance()+"::"+"PgaChannel");
         }
         else {
            type = String.format("const %s<%d>", getClassBaseName()+getInstance()+"::"+"Channel", index);
         }
         writeVariableDeclaration("", description, cIdentifier, type, pin.getLocation());
      }
   }
   
   @Override
   public int getSignalIndex(Signal function) {
      final Pattern pUsual = Pattern.compile("^(SE|DM|DP)(\\d+)(a|b)?$");
      final Pattern pPga   = Pattern.compile("^PGA(\\d+)_DP$");
      Matcher m = pUsual.matcher(function.getSignalName());
      int index = 0;
      if (m.matches()) {
         index = Integer.parseInt(m.group(2));
         if ((m.group(3) != null) && m.group(3).equalsIgnoreCase("a")) {
            index += 32;
         }
      }
      else {
         Matcher mPga = pPga.matcher(function.getSignalName());
         if (!mPga.matches()) {
            throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
         }
         index = 2;
      }
      return index;
   }

   @Override
   public boolean isPcrTableNeeded() {
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