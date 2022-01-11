package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class encapsulating the code for writing an instance of SPI
 */
public class WriterForSpi extends PeripheralWithState {
   
   public WriterForSpi(String basename, String instance, DeviceInfo deviceInfo) throws IOException, UsbdmException {
      super(basename, instance, deviceInfo);
      
      // Can (usually do) create instances of this class 
      fCanCreateInstance = true;
      
      // Can create type declarations for signals belonging to this peripheral
      fcanCreateSignalType = true;

      // Instance has internal state
      clearConstType();
   }

   @Override
   public String getTitle() {
      return "Serial Peripheral Interface";
   }

   // First PCS index in Info Table
   final int PCS_FIRST_INDEX = 3;
   
   // Last PCS index in Info Table
   final int PCS_LAST_INDEX  = 8;
   
   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"SCK", "SIN|MISO", "SOUT|MOSI", "PCS0|PCS|SS|SS_b", "PCS1", "PCS2", "PCS3", "PCS4", "PCS5"};
      return getSignalIndex(function, signalNames);
   }
   
   @Override
   public void validateMappedPins() {
      super.validateMappedPins();
      if (fStatus != null) {
         return;
      }
      // Warn if MISO, MOSI and SCK signals not mapped
      validateMappedPins(new int[]{0,1,2}, getSignalTables().get(0).table);
   }
   
   static final String PIN_FORMAT   = "static constexpr SpiPeripheralSelect %-20s = %-25s %s\n";
   
   /**
    * Generate set of enum symbols and Types for CMP inputs that are mapped to pins.
    * 
    * <pre>
    *   // In tsi.h
    *   Input_Ptc6                = 6,       ///< Mapped pin PTC6 (p51)
    *   Input_MyComparatorInput   = 6,       ///< Mapped pin PTC6 (p51)
    *   
    *   // In hardware.h
    *   /// User comment
    *   typedef const Cmp0::Pin<Cmp0::Input_Ptc6>  MyComparatorInput;    // PTC6 (p51)
    * </pre> 
    * 
    * @param documentUtilities
    * @throws IOException
    */
   @Override
   protected void writeDeclarations() {

      super.writeDeclarations();

      ArrayList<InfoTable> signalTables = getSignalTables();
      HashSet<String> usedIdentifiers = new HashSet<String>();

      StringBuffer inputsStringBuilder       = new StringBuffer();
      for (InfoTable signalTable:signalTables) {
         int index = -1;
         for (Signal signal:signalTable.table) {
            index++;
            if (index < PCS_FIRST_INDEX) {
               continue;
            }
            if (index > PCS_LAST_INDEX) {
               continue;
            }
            if (signal == null) {
               continue;
            }
            MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
            if (mappingInfo == MappingInfo.UNASSIGNED_MAPPING) {
               continue;
            }
            Pin pin = mappingInfo.getPin();
            if (pin == Pin.UNASSIGNED_PIN) {
               continue;
            }
            if (!pin.isAvailableInPackage()) {
               continue;
            }

            String enumName    = "Pcs_";

            String trailingComment = "///< " + signal.getUserDescription() + " (" + pin.getNameWithLocation()  + ")";
            String pcsPinName      = enumName+prettyPinName(pin.getName());
            String cIdentifier     = makeCTypeIdentifier(signal.getCodeIdentifier());
            String mapName         = "SpiPeripheralSelect_"+(index-PCS_FIRST_INDEX);
            String pcsUserName     = "";
            if (!cIdentifier.isBlank()) {
               pcsUserName =  enumName+cIdentifier;
            }

            boolean inUse = !usedIdentifiers.add(pcsPinName);
            String format = PIN_FORMAT;
            if (inUse) {
               format = "// "+format; 
            }
            inputsStringBuilder.append(String.format(format, pcsPinName, mapName+";", trailingComment));
            if (!pcsUserName.isBlank()) {
               inUse = !usedIdentifiers.add(pcsUserName);
               if (inUse) {
                  pcsUserName = "// "+pcsUserName; 
               }
               inputsStringBuilder.append(String.format(format, pcsUserName, mapName+";", trailingComment));
            }
         }
         // Create or replace Input Mapping variable as needed
         fDeviceInfo.addOrUpdateStringVariable("Input Mapping", makeKey("InputMapping"), inputsStringBuilder.toString(), true);
      }
   }
}