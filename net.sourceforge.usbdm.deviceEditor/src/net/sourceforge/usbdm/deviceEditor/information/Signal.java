package net.sourceforge.usbdm.deviceEditor.information;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo.Origin;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;

/**
 * Describes a peripheral signal that may be mapped to a pin<br>
 * Includes:
 * <li>name e.g. FTM3_CH2
 * <li>signal e.g. FTM3_CH2 => CH2
 * <li>peripheral owning this signal e.g. FTM3
 */
public class Signal extends ObservableModel implements Comparable<Signal>, IModelChangeListener {

   /**
    * Pin mapping comparator
    */
   public static Comparator<String> comparator = Utils.comparator;

   private static class PinMappingComparator implements Comparator<MappingInfo>{

      @Override
      public int compare(MappingInfo o1, MappingInfo o2) {
         int rc = o1.getMux().ordinal() - o2.getMux().ordinal();
         if (rc == 0) {
            rc = Pin.comparator.compare(o1.getPin().getName(), o2.getPin().getName());
            if (rc == 0) {
               rc = o1.getSignalList().compareTo(o2.getSignalList());
            }
         }
         return rc;
      }
   }

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
   private TreeSet<MappingInfo> fPinMappings = new TreeSet<MappingInfo>(new PinMappingComparator());

   /** Reset mapping for this signal */
   private MappingInfo fResetMapping = MappingInfo.UNASSIGNED_MAPPING;

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

   public boolean isPowerSignal() {
      return (fName.startsWith("VDD")) || (fName.startsWith("VSS")) ;
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
            if (!isPowerSignal()) {
               throw new RuntimeException("Can't add more pins to a signal with fixed pin mapping " + fName + ", " + mapInfo);
            }
         }
      }
      else {
         if (fPinMappings.isEmpty()) {
            // Add disabled setting
            fPinMappings.add(MappingInfo.UNASSIGNED_MAPPING);
            
            // Add reset mapping
            fPinMappings.add(fResetMapping);
         }
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
      if (isPowerSignal()) {
         return;
      }
      if ((fResetMapping.getMux() != MuxSelection.unassigned) && (fResetMapping != mapping)) {
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
      sb.append("\n  fPeripheral   = "+fPeripheral);
      sb.append("\n  fResetMapping   = "+fResetMapping);
      for(MappingInfo mapping:fPinMappings) {
         sb.append("\n  fPinMappings(i) = "+mapping);
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
      // Check all mappings looking for a valid one
      for (MappingInfo info:fPinMappings) {
         Pin pin = info.getPin();
         // Exclude disabled pin
         if ((pin != Pin.UNASSIGNED_PIN) && pin.isAvailableInPackage()) {
            return true;
         }
      }
      return false;
   }

   /**
    * Get current pin mapping information for this signal.
    * If more than one pin is mapped it returns the mapping for the first pin found. 
    * 
    * @return Pin mapping information for pin found or MappingInfo.UNASSIGNED_MAPPING
    */
   public MappingInfo getFirstMappedPinInformation() {
      for (MappingInfo mappingInfo:fPinMappings) {
         if (mappingInfo.isSelected()) {
            return mappingInfo;
         }
      }
      return MappingInfo.UNASSIGNED_MAPPING;
   }
   
   /**
    * Get current pin mapping information for this signal
    * 
    * @return Set of mapped pins.  The set may be empty if no pins mapped.
    */
   public List<MappingInfo> getMappedPinInformation() {
      ArrayList<MappingInfo> rv = new ArrayList<MappingInfo>();
      for (MappingInfo mappingInfo:fPinMappings) {
         if (mappingInfo.isSelected()) {
            rv.add(mappingInfo);
         }
      }
      return rv;
   }
   
   /**
    * Get current pin mapped for this signal
    * 
    * @return Mapped pin (may be Pin.UNASSIGNED_PIN)
    */
   public Pin getMappedPin() {
      MappingInfo mappedPins = getFirstMappedPinInformation();
      return mappedPins.getPin();
   }
   
   /**
    * Checks if this signal is mapped to more than one pin.
    * 
    * @return String describing conflict or null if no conflict
    */
   public Status checkMappingConflicted() {
      StringBuilder sb = new StringBuilder();
      int mappingsFound = 0;
      
      sb.append(getName() + " =>> (");
      for (MappingInfo mappingInfo : fPinMappings) {
         if (mappingInfo.isSelected()) {
            if (mappingsFound>0) {
               sb.append(", ");
            }
            sb.append(mappingInfo.getPin().getNameWithLocation());
            mappingsFound++;
         }
      }
      if (mappingsFound <= 1) {
         return null;
      }
      sb.append(")");
      return new Status(sb.toString(), "Signal '"+getName() + "' is mapped to multiple pins");
   }
   
   /**
    * Map the function to a pin using the given mapping
    * 
    * @param mappingInfo
    */
   public void setMappedPin(MappingInfo mappingInfo) {
//      System.err.println("Signal.setPin("+mappingInfo+")");
//      Pin pin = mappingInfo.getPin();
//      if (pin.isMappingConflicted() && pin == Pin.UNASSIGNED_PIN) {
//         
//      }
      for (MappingInfo mapping:fPinMappings) {
         if (mapping == mappingInfo) {
            continue;
         }
         mapping.select(Origin.signal, false);
      }
      mappingInfo.select(Origin.signal, true);
      
      Pin pin = mappingInfo.getPin();
      
      Map<MuxSelection, MappingInfo> pinMapping = pin.getMappableSignals();
      for (MuxSelection muxSel: pinMapping.keySet()) {
         MappingInfo pinInfo = pinMapping.get(muxSel);
         pinInfo.getPin().modelElementChanged(mappingInfo);
      }
      notifyListeners();
   }

   /**
    * Connect listeners
    * <li>Any pin mappable to this signal 
    */
   public void connectListeners() {
      for (MappingInfo mappingInfo : fPinMappings) {
         Pin pin = mappingInfo.getPin();
         pin.addListener(this);
//         // XXXX Delete me!
//         if (fName.equalsIgnoreCase("GPIOA_4") || pin.getName().equalsIgnoreCase("GPIOA_4")) {
//            System.err.println("Signal(GPIOA_4).connectListeners(Pin("+pin.getName()+"))");
//         }
      }
   }
   
   /**
    * Disconnect Signal as listener for changes on pin multiplexing
    */
   public void disconnectListeners() {
   }
   
   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof MappingInfo) {
         MappingInfo mappingInfo = (MappingInfo) model;
         // XXX Delete me!
         System.err.println("Signal("+fName+").modelElementChanged(MappingInfo("+mappingInfo.getPin()+"))");
         
         notifyListeners();
      }
      if (model instanceof Pin) {
         Pin pin = (Pin) model;
         // XXX Delete me!
         System.err.println("Signal("+fName+").modelElementChanged(Pin("+pin.getName()+"))");
         notifyListeners();
//         
//         // If this pin is mapped notify
//         if (pin.getMappedSignals().getSignals().contains(this)) {
//            notifyListeners();
//         }
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
      // Not used
   }

   @Override
   public void elementStatusChanged(ObservableModel observableModel) {
      notifyStatusListeners();
   }

}
