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
 * Class encapsulating the code for writing an instance of FTM or TPM
 */
public class WriterForFtm extends PeripheralWithState {

   /** Signals that use this writer */
   protected InfoTable fQuadSignals = new InfoTable("InfoQUAD");

   /** Signals that use this writer */
   protected InfoTable fFaultSignals = new InfoTable("InfoFAULT");

   /** Signals that use this writer */
   protected InfoTable fClkinSignals = new InfoTable("InfoCLKIN");

   protected String clockSignalNames[] = {"CLKIN0", "CLKIN1"};

   public WriterForFtm(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);

      // Can create type instances of this peripheral
      fCanCreateInstance = true;
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;

      // Can create instances for signals belonging to this peripheral
      fCanCreateSignalInstance = true;
   }

   @Override
   public String getTitle() {
      return "PWM, Input capture and Output compare";
   }

   @Override
   protected void writeDeclarations() {
      
      super.writeDeclarations();
      
      for (int index=0; index<fInfoTable.table.size(); index++) {
         if (index>7) {
            // Maximum of 8 signals/channels. Other signals are special purpose
            break;
         }
         Signal signal = fInfoTable.table.get(index);
         if (signal == null) {
            continue;
         }
         Pin pin = signal.getFirstMappedPinInformation().getPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            continue;
         }
         String cIdentifier = signal.getCodeIdentifier();
         if ((cIdentifier == null) || cIdentifier.isBlank()) {
            continue;
         }
         if (!pin.isAvailableInPackage()) {
            continue;
         }
         String trailingComment  = pin.getNameWithLocation();
         String type = String.format("%s<%d>", getClassBaseName()+getInstance()+"::"+"Channel", index);
         String constType = "const "+ type;
         if (signal.getCreateInstance()) {
            writeVariableDeclaration("", signal.getUserDescription(), cIdentifier, constType, trailingComment);
         }
         else {
            writeTypeDeclaration("", signal.getUserDescription(), cIdentifier, type, trailingComment);
         }
      }
      
      if (fQuadSignals.table.size() >= 2) {
         do {
         Signal signalPhaseA = fQuadSignals.table.get(0);
         Signal signalPhaseB = fQuadSignals.table.get(1);
         if ((signalPhaseA == null) || (signalPhaseB == null)) {
            continue;
         }
         String cIdentifierPhaseA = signalPhaseA.getCodeIdentifier();
         String cIdentifierPhaseB = signalPhaseA.getCodeIdentifier();
         if (cIdentifierPhaseA.isBlank() || !(cIdentifierPhaseA.equals(cIdentifierPhaseB))) {
            continue;
         }
         Pin pinPhaseA = signalPhaseA.getFirstMappedPinInformation().getPin();
         Pin pinPhaseB = signalPhaseB.getFirstMappedPinInformation().getPin();
         if ((pinPhaseA == Pin.UNASSIGNED_PIN) || (pinPhaseB == Pin.UNASSIGNED_PIN)) {
            continue;
         }
         String trailingComment  = pinPhaseA.getNameWithLocation()+", "+pinPhaseB.getNameWithLocation();
         String cIdentifier = makeCIdentifier(cIdentifierPhaseA);
         String type = String.format("FtmQuadDecoder"+getInstance());
         String constType = "const "+ type;
         writeTypeDeclaration("", signalPhaseA.getUserDescription(), cIdentifier, type, trailingComment);
         if (signalPhaseA.getCreateInstance() || signalPhaseB.getCreateInstance()) {
            writeVariableDeclaration("", signalPhaseA.getUserDescription(), cIdentifier, constType, trailingComment);
         }
         } while (false);
      }
   }
   
   @Override
   public boolean isPcrTableNeeded() {
      boolean required = 
            super.isPcrTableNeeded() ||
           (fQuadSignals.table.size() + 
            fFaultSignals.table.size()) > 0;
      return required;
   }

   @Override
   protected void addSignalToTable(Signal signal) {
      InfoTable infoTable = null;

      int signalIndex = -1;

      Pattern p = Pattern.compile(".*CH(\\d+)");
      Matcher m = p.matcher(signal.getSignalName());
      if (m.matches()) {
         infoTable = fInfoTable;
         signalIndex = Integer.parseInt(m.group(1));
      }
      if (infoTable == null) {
         final String quadNames[] = {"QD_PHA", "QD_PHB"};
         for (signalIndex=0; signalIndex<quadNames.length; signalIndex++) {
            if (signal.getSignalName().endsWith(quadNames[signalIndex])) {
               infoTable = fQuadSignals;
               break;
            }
         }
      }
      if (infoTable == null) {
         final String faultNames[] = {"FLT0", "FLT1", "FLT2", "FLT3"};
         for (signalIndex=0; signalIndex<faultNames.length; signalIndex++) {
            if (signal.getSignalName().endsWith(faultNames[signalIndex])) {
               infoTable = fFaultSignals;
               break;
            }
         }
      }
      if (infoTable == null) {
         // Look for shared clock inputs
         for (signalIndex=0; signalIndex<clockSignalNames.length; signalIndex++) {
            if (signal.getSignalName().matches(clockSignalNames[signalIndex])) {
               infoTable = fInfoTable;
               signalIndex += 8;
               break;
            }
         }
      }
      if (infoTable == null) {
         throw new RuntimeException("Signal '" + signal.getSignalName() + "' does not match expected pattern");
      }
      if (signalIndex>=infoTable.table.size()) {
         infoTable.table.setSize(signalIndex+1);
      }
      if ((infoTable.table.get(signalIndex) != null) && 
            (infoTable.table.get(signalIndex) != signal)) {
         throw new RuntimeException("Multiple signals mapped to index = "+signalIndex+"\n new = " + signal + ",\n old = " + infoTable.table.get(signalIndex));
      }
      infoTable.table.setElementAt(signal, signalIndex);
   }

   @Override
   public ArrayList<InfoTable> getSignalTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fInfoTable);
      rv.add(fFaultSignals);
      rv.add(fQuadSignals);
      rv.add(fClkinSignals);
      return rv;
   }
   
   @Override
   public InfoTable getUniqueSignalTable() {
      System.err.println("There is more than one signal table");
      return fInfoTable;
    }

   @Override
   public void addLinkedSignals() {
      for (String signalName:clockSignalNames) {
         Signal signal = fDeviceInfo.getSignals().get(getBaseName()+"_"+signalName);
         if (signal == null) {
            continue;
         }
         addSignal(signal);
      }
   }

   @Override
   protected boolean okTemplate(String key) {
      // Filter Quadrature decoders where there are no Quad inputs
      if (key.equalsIgnoreCase("/FTM/quadDeclarations")) {
         return fQuadSignals.table.size()>0;
      }
      if (key.equalsIgnoreCase("/TPM/quadDeclarations")) {
         return fQuadSignals.table.size()>0;
      }
      return super.okTemplate(key);
   }

}
