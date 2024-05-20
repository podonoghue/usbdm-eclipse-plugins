package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriteFamilyCpp.HardwareDeclarationInfo;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
public class WriterForAdc extends PeripheralWithState {

   final int PGA_INDEX  = 2;
   final int DP_INDEX   = 32;
   final int SEB_INDEX  = DP_INDEX+4;
   final int DM_INDEX   = DP_INDEX+8;

   public WriterForAdc(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);

      // Can create instances for signals belonging to this peripheral
      fCanCreateInstance = true;

      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;

      // Can create instances for signals belonging to this peripheral
      fCanCreateSignalInstance = true;
   }

   @Override
   public String getTitle() {
      return "Analogue Input";
   }

   /**
    * Checks if a signal is a PGA input
    * 
    * @param signal
    * 
    * @return
    */
   boolean isPgaSignal(Signal signal) {
      return (signal != Signal.DISABLED_SIGNAL) && signal.getName().startsWith("PGA");
   }

   /**
    * Checks if a signal is a Positive differential input
    * 
    * @param signal
    * 
    * @return
    */
   boolean isPositiveDiffSignal(Signal signal) {
      return (signal != Signal.DISABLED_SIGNAL) && signal.getName().matches("^ADC\\d_DP\\d$");
   }

   /**
    * Checks if a signal is a Positive differential input
    * 
    * @param signal
    * 
    * @return
    */
   boolean isAChannelSignal(Signal signal) {
      return (signal != Signal.DISABLED_SIGNAL) && signal.getName().matches("^ADC\\d_SE\\da$");
   }

   /**
    * Checks if a signal is a Positive differential input
    * 
    * @param signal
    * 
    * @return
    */
   boolean isBChannelSignal(Signal signal) {
      return (signal != Signal.DISABLED_SIGNAL) && signal.getName().matches("^ADC\\d_SE\\db$");
   }

   /**
    * Checks if a signal is a Positive differential input
    * 
    * @param signal
    * 
    * @return
    */
   boolean isNegativeDiffSignal(Signal signal) {
      return (signal != Signal.DISABLED_SIGNAL) && signal.getName().matches("^ADC\\d_DM\\d$");
   }

   /**
    * Write declarations for single-ended channels and single-ended use of (differential) PGA channels.
    * Note single-ended use of differential channels are also done as they have their own SE signal alias.
    * 
    * @param infoTable
    */
   private void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo, InfoTable infoTable) {
      // Single-ended channels
      for (int index=0; index<infoTable.table.size(); index++) {
         Signal signal = infoTable.table.get(index);
         if (signal == null) {
            continue;
         }
         Pin pin = signal.getFirstMappedPinInformation().getPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            continue;
         }
         if (!pin.isAvailableInPackage()) {
            continue;
         }
         String cIdentifier = signal.getCodeIdentifier();
         if (cIdentifier.isBlank()) {
            continue;
         }
         String trailingComment  = pin.getNameWithLocation();
         String description      = signal.getUserDescription();
         String type;
         if (isPgaSignal(signal)) {
            description = description + " (Programmable gain amplifier)";
            type = String.format("%s", getClassBaseName()+getInstance()+"::"+"PgaChannel");
         }
         else if (isPositiveDiffSignal(signal)) {
            description = description + " (Differential)";
            type = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"DiffChannel",
                  "AdcChannelNum_Diff"+(index-DP_INDEX));
         }
         else if (isAChannelSignal(signal)) {
            type = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"Channel",
                  "AdcChannelNum_Se"+(index)+"a");
         }
         else if (isBChannelSignal(signal)) {
            type = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"Channel",
                  "AdcChannelNum_Se"+(index-SEB_INDEX+4)+"b");
         }
         else if (isNegativeDiffSignal(signal)) {
            continue;
         }
         else {
            type = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"Channel",
                  "AdcChannelNum_Se"+(index));
         }
         String constType = "const "+ type;
         String[] cIdentifiers = cIdentifier.split("/");
         for (String cIdent:cIdentifiers) {
            if (signal.getCreateInstance()) {
               writeVariableDeclaration(hardwareDeclarationInfo, "", description, cIdent, constType, trailingComment);
            }
            else {
               writeTypeDeclaration(hardwareDeclarationInfo, "", description, cIdent, type, trailingComment);
            }
         }
      }
   }

   @Override
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {

      super.writeDeclarations(hardwareDeclarationInfo);

      // Single-ended channels (including Pga recognised by having no code name DM)
      writeDeclarations(hardwareDeclarationInfo, fInfoTable);
   }

   @Override
   public int getSignalIndex(Signal function) {

      final Pattern pUsual = Pattern.compile("^(SE|DM|DP)(\\d+)(a|b)?$");
      final Pattern pPga   = Pattern.compile("^PGA(\\d+)_DP$");
      Matcher m = pUsual.matcher(function.getSignalName());
      if (m.matches()) {
         String type = m.group(1);
         int index = Integer.parseInt(m.group(2));
         String suffix = m.group(3);

         if ("SE".equals(type)) {
            if ((suffix != null) && suffix.equalsIgnoreCase("b")) {
               return index+SEB_INDEX-4; // -4 as SE4b-SE7b
            }
            // else no adjustment
            return index;
         }
         else if ("DM".equals(type)) {
            return index+DM_INDEX;
         }
         return index+DP_INDEX;
      }
      Matcher mPga = pPga.matcher(function.getSignalName());
      if (!mPga.matches()) {
         return PGA_INDEX;
      }
      throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
   }

   @Override
   public boolean isPcrTableNeeded() {
      return fInfoTable.table.size() > 0;
   }

   @Override
   protected void addSignalToTable(Signal function) {

      Pattern p = Pattern.compile("PGA(\\d+)(_(DM|DP))?");
      Matcher m = p.matcher(function.getName());
      if (m.matches()) {
         // Add entry in SE table
         if (PGA_INDEX>=fInfoTable.table.size()) {
            fInfoTable.table.setSize(PGA_INDEX+1);
         }
         fInfoTable.table.setElementAt(function, PGA_INDEX);
         return;
      }
      p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b|A|B)?");
      m = p.matcher(function.getSignalName());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
      }
      int signalIndex = getSignalIndex(function);
      if (fInfoTable == null) {
         throw new RuntimeException("Illegal function " + function.toString());
      }
      if (signalIndex>=fInfoTable.table.size()) {
         fInfoTable.table.setSize(signalIndex+1);
      }
      if ((fInfoTable.table.get(signalIndex) != null) &&
            (fInfoTable.table.get(signalIndex) != function)) {
         throw new RuntimeException("Multiple functions mapped to index new = " + function + ", old = " + fInfoTable.table.get(signalIndex));
      }
      fInfoTable.table.setElementAt(function, signalIndex);
   }

   @Override
   public ArrayList<InfoTable> getSignalTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fInfoTable);
      return rv;
   }

   @Override
   public InfoTable getUniqueSignalTable() {
      System.err.println("There is more than one signal table");
      return fInfoTable;
   }

}