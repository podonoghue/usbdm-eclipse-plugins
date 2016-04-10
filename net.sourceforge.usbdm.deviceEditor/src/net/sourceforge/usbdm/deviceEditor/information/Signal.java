package net.sourceforge.usbdm.deviceEditor.information;
import java.util.Comparator;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo.Origin;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Describes a peripheral signal that may be mapped to a pin<br>
 * Includes:
 * <li>name e.g. FTM3_CH2
 * <li>signal e.g. FTM3_CH2 => CH2
 * <li>peripheral owning this signal e.g. FTM3
 */
public class Signal extends ObservableModel implements Comparable<Signal>, IModelChangeListener {

   /**
    * Pin comparator
    */
   public static Comparator<String> comparator = Utils.comparator;

   private static class PinMappingComparator implements Comparator<MappingInfo>{

      @Override
      public int compare(MappingInfo o1, MappingInfo o2) {
         int rc = Pin.comparator.compare(o1.getPin().getName(), o2.getPin().getName());
         if (rc == 0) {
            rc = o1.getMux().ordinal() - o2.getMux().ordinal();
            if (rc == 0) {
               rc = o1.getSignalList().compareTo(o2.getSignalList());
            }
         }
         return rc;
      }
   }

   /** Comparator for Pin mappings to sort by pin */
   private static PinMappingComparator pinMappingComparator = new PinMappingComparator();
   
   /**
    * Disabled signal
    */
   public static final Signal DISABLED_SIGNAL = new Signal("Disabled", null, "");

   /*
    * ======================================================================================================
    */
   
   
   /** Peripheral that signal belongs to */
   private final Peripheral fPeripheral;

   /** Peripheral signal name number e.g. PTA3 = 3, FTM0_CH6 = CH6, SPI0_SCK = SCK */
   private final String fSignal;

   /** Name of peripheral signal e.g. FTM0_CH3 */
   private final String fName;

   /** Indicates whether to include this signal in output */
   private boolean fIncluded;

   /** Template applicable to this signal (if any) */
//   private PeripheralTemplateInformation fTemplate = null;     

   /** Map of pins that this peripheral signal may be mapped to */
   private TreeSet<MappingInfo> fPinMappings = new TreeSet<MappingInfo>(pinMappingComparator);

   /** Reset mapping for this signal */
   private MappingInfo fResetMapping = new MappingInfo(Pin.DISABLED_PIN, MuxSelection.disabled);

   private MappingInfo fCurrentMapping = MappingInfo.DISABLED_MAPPING;

   /**
    * 
    * @param name          Name of peripheral signal e.g. FTM0_CH3 
    * @param peripheral    Peripheral that signal belongs to 
    * @param signal        Peripheral signal name or number e.g. PTA3 = 3, FTM0_CH6 = CH6, SPI0_SCK = SCK 
    */
   Signal(String name, Peripheral peripheral, String signal) {
      fName       = name;
      fPeripheral = peripheral;
      fSignal     = signal;
   }

   /**
    * Connect Signal as listener for changes on pin multiplexing
    */
   public void connectListeners() {
      for (MappingInfo mappingInfo:fPinMappings) {
         mappingInfo.addListener(this);
      }
   }
   
   /**
    * Connect Signal as listener for changes on pin multiplexing
    */
   public void disconnectListeners() {
      for (MappingInfo mappingInfo:fPinMappings) {
         mappingInfo.removeListener(this);
      }
   }
   
   void setIncluded(boolean include) {
      fIncluded = include;
   }

   public boolean isIncluded() {
      return fIncluded;
   }

   /**
    * Create descriptive name<br>
    * e.g. MappingInfo(FTM, 0, 6) = FTM0_6
    * 
    * @return name created
    */
   public String getName() {
      return fName;
   }

   @Override
   public int compareTo(Signal o) {
      return comparator.compare(getName(), o.getName());
   }

   /**
    * @return the Peripheral
    */
   public Peripheral getPeripheral() {
      return fPeripheral;
   }

   /**
    * @return the signal
    */
   public String getSignal() {
      return fSignal;
   }

   /**
    * Add a pin that this signal may be mapped to
    * 
    * @param mapInfo
    */
   public void addMapping(MappingInfo mapInfo) {
      if (this == DISABLED_SIGNAL) {
//       throw new RuntimeException("Adding mapping to disabled pin");
         return;
      }
      if (mapInfo.getMux() == MuxSelection.fixed) {
         if (!fPinMappings.isEmpty()) {
            throw new RuntimeException("Can't add more pins to a fixed signal " + fName + ", " + mapInfo);
         }
         fPinMappings.add(mapInfo);
         return;
      }
      if (fPinMappings.isEmpty()) {
         // Add disabled setting
         fPinMappings.add(MappingInfo.DISABLED_MAPPING);
         fPinMappings.add(fResetMapping);
      }
      fPinMappings.add(mapInfo);
   }

   /**
    * Get ordered set of possible pin mappings for peripheral signal
    * 
    * @return
    */
   public TreeSet<MappingInfo> getPinMapping() {
      return fPinMappings;
   }

   public void setResetPin(MappingInfo mapping) {
      if (this == DISABLED_SIGNAL) {
         // Ignore resets to Disabled
         return;
      }
      if ((fResetMapping.getMux() != MuxSelection.disabled) && (fResetMapping != mapping)) {
         throw new RuntimeException("Multiple reset pin mappings for " + getName());
      }
      fResetMapping = mapping;
   }

   public MappingInfo getResetMapping() {
      return fResetMapping;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("PF    = "+fName);
      sb.append("\n  P   = "+fPeripheral);
      sb.append("\n  RM  = "+fResetMapping);
      for(MappingInfo mapping:fPinMappings) {
         sb.append("\n  PMi = "+mapping);
      }
      return sb.toString();
   }

   /**
    * Indicates if the signal is available in the current package
    * 
    * @return
    */
   public boolean isAvailableInPackage() {
      for (MappingInfo info:fPinMappings) {
         Pin pin = info.getPin();
         // Exclude disabled pin
         if ((pin != Pin.DISABLED_PIN) && pin.isAvailableInPackage()) {
            return true;
         }
      }
      return false;
   }

   /**
    * Map the function to a pin using the given mapping
    * 
    * @param mappingInfo
    */
   public void setPin(MappingInfo mappingInfo) {
      if (fCurrentMapping == mappingInfo) {
         // Already mapped - No change
         return;
      }
      fCurrentMapping.select(Origin.signal, false);
      mappingInfo.select(Origin.signal, true);
      
//      if (fMappedPin.getPin() != null) {
//         // Unmap existing pin
//         fMappedPin.getPin().setMuxSelection(MuxSelection.disabled);
//      }
//      fMappedPin = mappingInfo;
//      if (fMappedPin.getPin() != null) {
//         // Map new pin
//         fMappedPin.getPin().setMuxSelection(mappingInfo.getMux());
//      }
//      notifyListeners();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      
      if (model instanceof MappingInfo) {
         MappingInfo mappingInfo = (MappingInfo) model;
         if (mappingInfo.isSelected()) {
            // Signal mapped value
            if (fCurrentMapping == mappingInfo) {
               // Already mapped - no change
               return;
            }
            fCurrentMapping = mappingInfo;
            // New pin has been mapped
         }
         else {
            // Signal unmapped from pin
            if (mappingInfo != fCurrentMapping) {
               // Already unmapped - no change
               return;
            }
            // Currently pin has been unmapped
            fCurrentMapping = MappingInfo.DISABLED_MAPPING;
         }
         notifyListeners();
      }
   }

   /**
    * Get current pin mapping
    * 
    * @return
    */
   public MappingInfo getCurrentMapping() {
      return fCurrentMapping;
   }
   
   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

}
