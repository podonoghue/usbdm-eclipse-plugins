package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.editor.BaseLabelProvider;
import net.sourceforge.usbdm.deviceEditor.editor.ModifierEditorInterface;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Settings;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.SignalModel;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;

/**
 * Class encapsulating the code for writing an instance of SPI
 */
public class WriterForSpi extends PeripheralWithState {

   /** Key used to save/restore identifier used for code generation */
   private final String POLARITY_KEY  = "$peripheral$"+getName()+"_polarity";

   int fPcsPolarity = 0;


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
   
   class FormatInfo {
      String left;
      String right;
      
      FormatInfo(String left, String right) {
         this.left  = left;
         this.right = right;
      }
   };
   
   /**
    * Generate an enum and add to builder
    * 
    * @param index   Index of PCS
    * @param signal  Signal associated with PCS
    * @param format  Format for enum
    * @param usedIds Used identifiers for clashes
    * @param sb      Builder
    */
   void generatePcsEnum(int index, Signal signal, FormatInfo format, HashSet<String> usedIds, StringBuilder sb) {

         if (index < PCS_FIRST_INDEX) {
            return;
         }
         if (index > PCS_LAST_INDEX) {
            return;
         }
         if (signal == null) {
            return;
         }
         MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
         if (mappingInfo == MappingInfo.UNASSIGNED_MAPPING) {
            return;
         }
         Pin pin = mappingInfo.getPin();
         if (pin == Pin.UNASSIGNED_PIN) {
            return;
         }
         if (!pin.isAvailableInPackage()) {
            return;
         }

         String lineFormat   = "static constexpr %-"+(format.left.length()+10)+"s = %-"+(format.right.length())+"s %s\n";
         
         // Canonical PCS name
         String mapName         = String.format(format.right, index-PCS_FIRST_INDEX);

         String trailingComment = "///< " + signal.getUserDescription() + " (" + pin.getNameWithLocation()  + ")";

         String pcsPinName      = String.format(format.left, prettyPinName(pin.getName()));

         String cIdentifier     = makeCTypeIdentifier(signal.getCodeIdentifier());
         String pcsUserName             = "";
         if (!cIdentifier.isBlank()) {
            pcsUserName          = String.format(format.left, cIdentifier);
         }

         String prefix = "";
         if (!usedIds.add(pcsPinName)) {
            prefix = "// ";
         }
         sb.append(prefix+String.format(lineFormat, pcsPinName, mapName+";", trailingComment));
         
         if (!pcsUserName.isBlank()) {
            sb.append(String.format(lineFormat, pcsUserName, mapName+";", trailingComment));
         }
   
   }
   /**
    * Generate set of enum symbols and Types for CMP inputs that are mapped to pins.
    * 
    * <pre>
    *   // In spi.h
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

      StringBuilder enumStringBuilder       = new StringBuilder();
      
      for (InfoTable signalTable:signalTables) {
         int index = -1;
         FormatInfo format = new FormatInfo(
               "SpiPeripheralSelect SpiPeripheralSelect_%s",
               "SpiPeripheralSelect_%s"
               );
         for (Signal signal:signalTable.table) {
            index++;
            generatePcsEnum(index, signal, format, usedIdentifiers, enumStringBuilder);
         }
      }
      StringBuilder pcsSb = new StringBuilder();
      boolean firstValue = true;
      for (InfoTable signalTable:signalTables) {
         int index = -1;
         FormatInfo format = new FormatInfo(
               "SpiPeripheralSelectPolarity SpiPeripheralSelectPolarity_%s_ActiveLow",
               "SpiPeripheralSelectPolarity_%s_ActiveLow"
               );
         for (Signal signal:signalTable.table) {
            index++;
            if (isActiveLow(signal)) {
               generatePcsEnum(index, signal, format, usedIdentifiers, enumStringBuilder);
               if (!firstValue) {
                  pcsSb.append(",\n      ");
               }
               pcsSb.append("SpiPeripheralSelectPolarity_"+(index-PCS_FIRST_INDEX)+"_ActiveLow");
               firstValue=false;
            }
         }
      }
      String pcsPolarity = "SpiPeripheralSelectPolarity_All_ActiveHigh";
      if (!pcsSb.isEmpty()) {
         pcsPolarity = pcsSb.toString();
      }
      String key = "/"+getName()+"/spi_mcr_pcsisValue";
      Variable spi_mcr_pcsisValueVar = safeGetVariable(key);
      spi_mcr_pcsisValueVar.setValueQuietly(pcsPolarity);
      // Create or replace Input Mapping variable as needed
      fDeviceInfo.addOrUpdateStringVariable("Input Mapping", makeKey("InputMapping"), enumStringBuilder.toString(), true);
   }

   /**
    * Get inforTable index of PCS pin
    * 
    * @param signal Signal to look for
    * 
    * @return index or -1 if not a PCS signal
    */
   private int getPcsPinIndex(Signal signal) {
      int index = fInfoTable.table.indexOf(signal);
      if (index<PCS_FIRST_INDEX) {
         return -1;
      }
      if (index>PCS_LAST_INDEX) {
         return -1;
      }
      return index;
   }
   
   @Override
   public void extractHardwareInformation(Peripheral dbPortPeripheral) {
      extractAllRegisterFields(dbPortPeripheral);
   }

   boolean isActiveLow(Signal signal) {
      int index = getPcsPinIndex(signal);
      if (index<0) {
         return false;
      }
      return (fPcsPolarity&(1<<index)) != 0;
   }

   boolean setActiveLow(Signal signal, Boolean value) {
      int index = getPcsPinIndex(signal);
      if (index<0) {
         return false;
      }
      boolean currentValue = (fPcsPolarity & (1<<index)) != 0;
      if (currentValue == value) {
         return false;
      }
      fPcsPolarity ^= (1<<index);
      setDirty(true);
      return true;
   }
   
   @Override
   public void saveSettings(Settings settings) {
      super.saveSettings(settings);
      if (fPcsPolarity != 0) {
         settings.put(POLARITY_KEY, Integer.toString(fPcsPolarity));
      }
   }

   @Override
   public void loadSettings(Settings settings) {
      super.loadSettings(settings);
      String value = settings.get(POLARITY_KEY);
      if (value != null) {
         fPcsPolarity = Integer.parseInt(value);
      }
   }

   /**
    * Editor support for GPIO polarity
    */
   public class ModifierEditingSupport implements ModifierEditorInterface {

      @Override
      public boolean canEdit(SignalModel model) {
         if (getPcsPinIndex(model.getSignal()) < 0) {
            return false;
         }
         return model.getSignal().getMappedPin() != Pin.UNASSIGNED_PIN;
      }

      @Override
      public CellEditor getCellEditor(TreeViewer viewer) {
         return new CheckboxCellEditor(viewer.getTree());
      }

      @Override
      public Object getValue(SignalModel model) {
         if (getPcsPinIndex(model.getSignal()) < 0) {
            return null;
         }
         if (model.getSignal().getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         return isActiveLow(model.getSignal());
      }

      @Override
      public String getText(SignalModel model) {
         if (getPcsPinIndex(model.getSignal()) < 0) {
            return null;
         }
         if (model.getSignal().getMappedPin() == Pin.UNASSIGNED_PIN) {
            return null;
         }
         return isActiveLow(model.getSignal())?"ActiveLow":"ActiveHigh";
      }

      @Override
      public boolean setValue(SignalModel model, Object value) {
         if (getPcsPinIndex(model.getSignal()) < 0) {
            return false;
         }
         return setActiveLow(model.getSignal(), (Boolean)value);
      }

      @Override
      public Image getImage(SignalModel model) {
         if (getPcsPinIndex(model.getSignal()) < 0) {
            return null;
         }
       return isActiveLow(model.getSignal())?BaseLabelProvider.checkedImage:BaseLabelProvider.uncheckedImage;
      }
      
      @Override
      public String getModifierHint(SignalModel model) {
         if (getPcsPinIndex(model.getSignal()) < 0) {
            return null;
         }
         return "Polarity of PCS signal";
      }
   }

   private ModifierEditingSupport modifierEditingSupport =  new ModifierEditingSupport();
   
   @Override
   public ModifierEditorInterface getModifierEditor() {
      return modifierEditingSupport;
   }
   
   
}