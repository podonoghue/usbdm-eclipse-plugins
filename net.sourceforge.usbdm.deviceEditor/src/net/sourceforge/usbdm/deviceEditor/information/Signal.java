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

   /** Signal name number e.g. PTA3 = 3, FTM0_CH6 = CH6, SPI0_SCK = SCK */
   private final String fSignalName;

   /** Name of signal e.g. FTM0_CH3 */
   private final String fName;

   /** Set of pin mappings for this signal */
   private TreeSet<MappingInfo> fPinMappings = new TreeSet<MappingInfo>(pinMappingComparator);

   /** Reset mapping for this signal */
   private MappingInfo fResetMapping = new MappingInfo(Pin.DISABLED_PIN, MuxSelection.disabled);

   /**
    * 
    * @param name          Name of signal e.g. FTM0_CH3 
    * @param peripheral    Peripheral that signal belongs to 
    * @param signal        Signal name or number e.g. PTA3 = 3, FTM0_CH6 = CH6, SPI0_SCK = SCK 
    */
   Signal(String name, Peripheral peripheral, String signal) {
      fName       = name;
      fPeripheral = peripheral;
      fSignalName = signal;
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
   
   /**
    * Get name e.g. FTM0_6, GPIOA_4
    * 
    * @return name created
    */
   public String getName() {
      return fName;
   }

   /**
    * Get signal name or number without prefix e.g. GPIOA_4 = 4, FTM0_CH6 = CH6, SPI0_SCK = SCK 
    * 
    * @return Name
    */
   public String getSignalName() {
      return fSignalName;
   }

   @Override
   public int compareTo(Signal o) {
      return comparator.compare(getName(), o.getName());
   }

   /**
    * Get peripheral that owns this signal
    * 
    * @return Owner
    */
   public Peripheral getPeripheral() {
      return fPeripheral;
   }

   /**
    * Add a pin that this signal may be mapped to
    * 
    * @param mapInfo
    */
   public void addMappedPin(MappingInfo mapInfo) {
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
    * Get ordered set of possible pin mappings for this signal 
    * 
    * @return
    */
   public TreeSet<MappingInfo> getPinMapping() {
      return fPinMappings;
   }

   /**
    * Set the reset pin mapping for this signal
    * 
    * @param mapping
    */
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

   /**
    * Get the reset pin mapping for this signal
    * 
    * return mapping
    */
   public MappingInfo getResetMapping() {
      return fResetMapping;
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Signal("+fName+")");
      sb.append("\n  fP   = "+fPeripheral);
      sb.append("\n  fRM  = "+fResetMapping);
      for(MappingInfo mapping:fPinMappings) {
         sb.append("\n  fPMi = "+mapping);
      }
      return sb.toString();
   }

   /**
    * Indicates if the signal is available in the current package<br>
    * i.e. is it possible to map this function to a pin with a location
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
    * Get current pin mapping for this signal
    * 
    * @return
    */
   public MappingInfo getMappedPin() {
      for (MappingInfo mappingInfo:fPinMappings) {
         if (mappingInfo.isSelected()) {
            return mappingInfo;
         }
      }
      return MappingInfo.DISABLED_MAPPING;
   }
   
   /**
    * Map the function to a pin using the given mapping
    * 
    * @param mappingInfo
    */
   public void setMappedPin(MappingInfo mappingInfo) {
      System.err.println("Signal.setPin("+mappingInfo+")");
      for (MappingInfo mapping:fPinMappings) {
         if (mapping == mappingInfo) {
            continue;
         }
         mapping.select(Origin.signal, false);
      }
      mappingInfo.select(Origin.signal, true);
      notifyListeners();
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof MappingInfo) {
         notifyListeners();
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      // Not used
   }

}
