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
 * Class encapsulating the code for writing an instance of FTM
 */
public class WriterForFtm extends PeripheralWithState {

   /** Signals that use this writer */
   protected InfoTable fQuadSignals = new InfoTable("InfoQUAD");

   /** Signals that use this writer */
   protected InfoTable fFaultSignals = new InfoTable("InfoFAULT");

   /** Signals that use this writer */
   protected InfoTable fClkinSignals = new InfoTable("InfoCLKIN");

   public WriterForFtm(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "PWM, Input capture and Output compare";
   }

   @Override
   public int getSignalIndex(Signal signal) {
      Pattern p = Pattern.compile("CH(\\d+)");
      Matcher m = p.matcher(signal.getSignalName());
      if (m.matches()) {
         return Integer.parseInt(m.group(1));
      }
      final String quadNames[] = {"QD_PHA", "QD_PHB"};
      for (int signalName=0; signalName<quadNames.length; signalName++) {
         if (signal.getSignalName().matches(quadNames[signalName])) {
            return signalName;
         }
      }
      final String clockNames[] = {"CLKIN0", "CLKIN1"};
      for (int signalName=0; signalName<clockNames.length; signalName++) {
         if (signal.getSignalName().matches(clockNames[signalName])) {
            return signalName;
         }
      }
      final String faultNames[] = {"FLT0", "FLT1", "FLT2", "FLT3"};
      for (int signalName=0; signalName<faultNames.length; signalName++) {
         if (signal.getSignalName().matches(faultNames[signalName])) {
            return signalName;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + signal.getSignalName());
   }

   @Override
   protected void writeDeclarations() {
      
      super.writeDeclarations();
      
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
         String type = String.format("const %s<%d>", getClassBaseName()+getInstance()+"::"+"Channel", index);
         writeVariableDeclaration("", signal.getUserDescription(), cIdentifier, type, pin.getLocation());
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
         String cIdentifier = makeCIdentifier(cIdentifierPhaseA);
         String type = String.format("const FtmQuadDecoder"+getInstance());
         writeVariableDeclaration("", signalPhaseA.getUserDescription(), cIdentifier, type, pinPhaseA.getLocation()+", "+pinPhaseB.getLocation());
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
         final String clkinNames[] = {"CLKIN0", "CLKIN1"};
         for (signalIndex=0; signalIndex<clkinNames.length; signalIndex++) {
            if (signal.getSignalName().matches(clkinNames[signalIndex])) {
               infoTable = fClkinSignals;
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
   
}
