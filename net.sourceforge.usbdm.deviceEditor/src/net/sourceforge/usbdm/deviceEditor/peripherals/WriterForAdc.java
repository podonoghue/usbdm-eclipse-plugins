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
   final int DM_INDEX   = 36;
   final int SEB_INDEX  = 40;

   /** Signals that use this writer */
//   protected InfoTable fDmFunctions = new InfoTable("InfoDM");

   /** Signals that use this writer */
//   protected InfoTable fDpFunctions = new InfoTable("InfoDP");

   /** Signals that use this writer */
//   protected InfoTable fBChannelFunctions = new InfoTable("InfoBChannels");

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

//   /**
//    * Write declarations for differential channels and (differential) PGA channels
//    *
//    * @param infoTable
//    */
//   private void writeDifferentialDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo, InfoTable dpInfoTable, InfoTable dmInfoTable) {
//
//      // Differential channels (including Pga) - recognised by having the same code name for DP and DM
//      for (int index=0; index<dpInfoTable.table.size(); index++) {
//         Signal dpSignal = dpInfoTable.table.get(index);
//         Signal dmSignal = dmInfoTable.table.get(index);
//         if ((dpSignal == null) || (dmSignal == null)) {
//            continue;
//         }
//         Pin dpPin = dpSignal.getFirstMappedPinInformation().getPin();
//         Pin dmPin = dmSignal.getFirstMappedPinInformation().getPin();
//         if ((dpPin == Pin.UNASSIGNED_PIN) || (dmPin == Pin.UNASSIGNED_PIN)) {
//            continue;
//         }
//         if (!dpPin.isAvailableInPackage() || !dmPin.isAvailableInPackage()) {
//            continue;
//         }
//         String dpIdentifier = dpSignal.getCodeIdentifier();
//         String dmIdentifier = dmSignal.getCodeIdentifier();
////         if (isPgaSignal(dpSignal) && dmIdentifier.isBlank()) {
////            // Assume pga_se or nothing - already handled
////            continue;
////         }
//         if (dpIdentifier.isBlank() && dmIdentifier.isBlank()) {
//            // Nothing
//            continue;
//         }
//         boolean unMatchedNames = !dpIdentifier.equals(dmIdentifier);
//         if (unMatchedNames) {
//            // Try to reconcile names - e.g. fredP vs fredM => fred
//            String pName = dpIdentifier.replaceAll("^(.*)[Pp]", "$1");
//            String mName = dmIdentifier.replaceAll("^(.*)[Mm]", "$1");
//            unMatchedNames = !pName.equals(mName);
//            if (!unMatchedNames) {
//               dpIdentifier = pName;
//               dmIdentifier = mName;
//            }
//         }
//         String cIdentifier = unMatchedNames?"error_"+dpIdentifier:dpIdentifier;
//         String dpDescription = dpSignal.getUserDescription();
//         String dmDescription = dmSignal.getUserDescription();
//         String type;
//         String error = "";
//         String trailingComment = dpPin.getNameWithLocation()+", "+dmPin.getNameWithLocation();
//         String description = dpDescription;
//         if (!dmDescription.equalsIgnoreCase(dpDescription)) {
//            if (!dpDescription.isBlank()) {
//               description += ", ";
//            }
//            description += dmDescription;
//         }
//         if (unMatchedNames) {
//            error = "Differential channel has unmatched input names '" + dpIdentifier + "', '" + dmIdentifier + "'";
//         }
//         if (isPgaSignal(dpSignal)) {
//            description = description + " (Differential programmable gain amplifier)";
//            type = String.format("%s", getClassBaseName()+getInstance()+"::"+"PgaDiffChannel");
//         }
//         else {
//            description = description + " (Differential)";
//            type = String.format("%s<%s>", getClassBaseName()+getInstance()+"::"+"DiffChannel", "AdcChannelNum_Diff"+index);
//         }
//         String constType = "const "+ type;
//         if (dpSignal.getCreateInstance() || dmSignal.getCreateInstance()) {
//            writeVariableDeclaration(hardwareDeclarationInfo, error, description, cIdentifier, constType, trailingComment);
//         }
//         else {
//            writeTypeDeclaration(hardwareDeclarationInfo, error, description, cIdentifier, type, trailingComment);
//         }
//      }
//   }

   @Override
   protected void writeDeclarations(HardwareDeclarationInfo hardwareDeclarationInfo) {

      super.writeDeclarations(hardwareDeclarationInfo);

      // Single-ended channels (including Pga recognised by having no code name DM)
      writeDeclarations(hardwareDeclarationInfo, fInfoTable);

      // Single-ended B channels
//      writeSingleEndedDeclarations(hardwareDeclarationInfo, fBChannelFunctions);

      // Differential channels (including Pga recognised by having the same code name for DP and DM)
//      writeDifferentialDeclarations(hardwareDeclarationInfo, fDpFunctions, fDmFunctions);
   }

   @Override
   public int getSignalIndex(Signal function) {
      final Pattern pUsual = Pattern.compile("^(SE|DM|DP)(\\d+)(a|b)?$");
      final Pattern pPga   = Pattern.compile("^PGA(\\d+)_DP$");
      String signalName = function.getSignalName();
      Matcher m = pUsual.matcher(signalName);
      int index = 0;
      if (m.matches()) {
         String type = m.group(1);
         index = Integer.parseInt(m.group(2));
         String suffix = m.group(3);

         if ("SE".equals(type)) {
            if ((suffix != null) && suffix.equalsIgnoreCase("b")) {
               index += SEB_INDEX-4; // -4 as SE4b-SE7b
            }
            // else no adjustment
         }
         else if ("DM".equals(type)) {
            index += DM_INDEX;
         }
         else if ("DP".equals(type)) {
            index += DP_INDEX;
         }
      }
      else {
         Matcher mPga = pPga.matcher(function.getSignalName());
         if (!mPga.matches()) {
            throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
         }
         index = PGA_INDEX;
      }
      return index;
   }

   @Override
   public boolean isPcrTableNeeded() {
      boolean required =
            (fInfoTable.table.size()
//                  fBChannelFunctions.table.size() +
//                  fDpFunctions.table.size() +
//                  fDmFunctions.table.size()
                  ) > 0;
                  return required;
   }

   @Override
   protected void addSignalToTable(Signal function) {
      final int PGA_INDEX = 2;

      InfoTable fFunctions = null;

      Pattern p = Pattern.compile("PGA(\\d+)(_(DM|DP))?");
      Matcher m = p.matcher(function.getName());
      if (m.matches()) {
         InfoTable table = null;
         // PGA input - may has DM/DP suffix
         //            System.out.println("Found " + function);
//         if (m.group(3) != null) {
//            // Has DM/DP suffix
//            String signalType = m.group(3);
//            if (signalType.equalsIgnoreCase("DM")) {
//               // Add entry in DM table
//               table = fDmFunctions;
//            }
//            else if (signalType.equalsIgnoreCase("DP")) {
//               // Add entry in DP table
//               table = fDpFunctions;
//            }
//         }
//         else {
            // Add entry in SE table
            table = fInfoTable;
//         }
         // Add entry in SE table
         if (PGA_INDEX>=table.table.size()) {
            table.table.setSize(PGA_INDEX+1);
         }
         table.table.setElementAt(function, PGA_INDEX);
         return;
      }
      
      p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b|A|B)?");
      m = p.matcher(function.getSignalName());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignalName() + " does not match expected pattern");
      }
      int signalIndex = getSignalIndex(function);
//      String signalType = m.group(1);
//      String suffix = m.group(3);
//      if (signalType.equalsIgnoreCase("SE")) {
//         if ((suffix != null) && (suffix.equalsIgnoreCase("b"))) {
//            fFunctions = fBChannelFunctions;
//         }
//         else {
            fFunctions = super.fInfoTable;
//         }
//      }
//      else if (signalType.equalsIgnoreCase("DM")) {
//         fFunctions = fDmFunctions;
//      }
//      else if (signalType.equalsIgnoreCase("DP")) {
//         fFunctions = fDpFunctions;
//      }
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
//         rv.add(fDpFunctions);
//         rv.add(fDmFunctions);
//         rv.add(fBChannelFunctions);
         return rv;
      }
      
      @Override
      public InfoTable getUniqueSignalTable() {
        System.err.println("There is more than one signal table");
        return fInfoTable;
      }

   }