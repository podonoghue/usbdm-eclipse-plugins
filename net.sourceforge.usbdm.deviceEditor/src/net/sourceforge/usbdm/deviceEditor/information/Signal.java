package net.sourceforge.usbdm.deviceEditor.information;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.MappingInfo.Origin;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.peripherals.Peripheral;
import net.sourceforge.usbdm.deviceEditor.peripherals.WriterForGpio;

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

   /** Indicates the signal is a power signal e.g. VDD, VSS */ 
   private final boolean fIsPowerSignal;
   
   /** Set of pin mappings for this signal */
   private TreeSet<MappingInfo> fPinMappings = new TreeSet<MappingInfo>(new PinMappingComparator());

   /** Reset mapping for this signal */
   private MappingInfo fResetMapping = MappingInfo.UNASSIGNED_MAPPING;

   /** Status indicating if multiple pins are mapped to this signal */ 
   private Status fStatus = null;

   /** Status of associated signals i.e. if multiple the mapped pins is also mapped to another signal */ 
   private Status fAssociatedStatus = null;

   /** User description of pin use */
   private String fUserDescription = "";

   /** User identifier to use in code generation */
   private String fCodeIdentifier = "";
   
   /** Indicates that code for a user instance of the signal class should be created */ 
   private boolean fCreateInstance;

   private static final String getCreateInstanceKey(String name) {
      return "$signal$"+name+"_createInstance";
   }

   private static final String getUserDescriptionKey(String name) {
      return "$signal$"+name+"_descriptionSetting";
   }

   private static final String getCodeIndentifierKey(String name) {
      return "$signal$"+name+"_codeIdentifier";
   }

   private static final String getPolarityIndentifierKey(String name) {
      return "$signal$"+name+"_polarity";
   }
 
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
      
      String t = signal.toUpperCase();
      fIsPowerSignal = t.startsWith("VDD") || t.startsWith("VSS") || t.startsWith("VCC") || t.startsWith("GND");
   }


   /**
    * Set editor dirty via deviceInfo
    */
   void setDirty(boolean dirty) {
      if (fPeripheral != null) {
         fPeripheral.setDirty(dirty);
      }
   }
   
   /** 
    * Set identifier to use in code generation
    */
   public void setCodeIdentifier(String codeIdentifier) {
      if (this == DISABLED_SIGNAL) {
         return;
      }
      if ((fCodeIdentifier != null) && (fCodeIdentifier.compareTo(codeIdentifier) == 0)) {
         return;
      }
      fCodeIdentifier = codeIdentifier;
      setDirty(true);
      notifyListeners();
      getMappedPin().modelElementChanged(this);
   }

   /** 
    * Get identifier to use in code generation
    */
   public String getCodeIdentifier() {
      if (this == DISABLED_SIGNAL) {
         return "DISABLED_SIGNAL!";
      }
      return fCodeIdentifier;
   }
   
   /** 
    * Set description of pin use 
    */
   public void setUserDescription(String userDescription) {
      if (this == DISABLED_SIGNAL) {
         return;
      }
      if ((fUserDescription != null) && (fUserDescription.compareTo(userDescription) == 0)) {
         return;
      }
      fUserDescription  = userDescription;

      // Update watchers of active mapping
      for (MappingInfo mappingInfo: fPinMappings) {
         if (mappingInfo.isSelected()) {
            mappingInfo.notifyListeners();
         }
      }
      setDirty(true);
      notifyListeners();
   }

   /**
    * Get user description for pin.
    * 
    * @return
    */
   public String getUserDescription( ) {
      return fUserDescription;
   }
   
   /**
    * Load pin settings from settings object
    * 
    * @param settings Settings object
    */
   public void loadSettings(Settings settings) {
      if (this == DISABLED_SIGNAL) {
         return;
      }
      if (settings.get(getCreateInstanceKey(fName)) != null) {
         setCreateInstance(true);
      }
      String value = settings.get(getCodeIndentifierKey(fName));
      if (value != null) {
         setCodeIdentifier(value);
      }
      value = settings.get(getUserDescriptionKey(fName));
      if (value != null) {
         setUserDescription(value);
      }
      if (fPeripheral instanceof WriterForGpio) {
         // Migrate old setting
         value = settings.get(getPolarityIndentifierKey(fName));
         if (value != null) {
            WriterForGpio gpio = (WriterForGpio)fPeripheral;
            gpio.setActiveLow(this, Boolean.parseBoolean(value));
         }
      }
   }

   /**
    * Save signal settings to settings object
    * 
    * @param settings Settings object
    */
   public void saveSettings(Settings settings) {
      if (this == DISABLED_SIGNAL) {
         return;
      }
      if (getCreateInstance()) {
         settings.put(getCreateInstanceKey(fName), "true");
      }
      String ident = getCodeIdentifier();
      if ((ident != null) && !ident.isEmpty()) {
         settings.put(getCodeIndentifierKey(fName), ident);
      }
      String desc = getUserDescription();
      if ((desc != null) && !desc.isEmpty()) {
         settings.put(getUserDescriptionKey(fName), desc);
      }
   }

   /**
    * Indicates this signal is a power signal e.g. Vcc or Vdd
    * 
    * @return True if power signal
    */
   public boolean isPowerSignal() {
      return fIsPowerSignal;
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
      mapInfo.getPin().addListener(this);
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
      
      if (isPowerSignal()) {
         // OK to map multiple pins to power
         return null;
      }
      
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
    * Get status of this signal and mapped pin
    * (Cached value)
    * 
    * @return Status 
    */
   Status getSignalStatus() {
      return fStatus;
   }
   
   /**
    * Get status of associated pins
    * 
    * @return
    */
   Status getAssociatedStatus() {
      Pin pin = getMappedPin();
      return pin.getPinStatus();
   }
   
   /**
    * Get status of this signal and mapped pin
    * 
    * @return Status 
    */
   public Status getStatus() {
   
      if (fStatus != null) {
         // signal -> multiple pins
         return fStatus;
      }
      return getAssociatedStatus();
   }
   
   /**
    * Map the function to a pin using the given mapping
    * 
    * @param mappingInfo Mapping being selected for signal
    */
   public void setMappedPin(MappingInfo mappingInfo) {
      for (MappingInfo mapping:fPinMappings) {
         if (mapping == mappingInfo) {
            continue;
         }
         mapping.select(Origin.signal, false);
      }
      mappingInfo.select(Origin.signal, true);
      setDirty(true);
      notifyListeners();
   }

   /**
    * Disconnect Signal as listener for changes on pin multiplexing
    */
   public void disconnectListeners() {
   }
   
   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof MappingInfo) {
         fStatus = checkMappingConflicted();
         notifyListeners();
      }
      if (model instanceof Pin) {
         Status status = getAssociatedStatus();
         if (fAssociatedStatus != status) {
            fAssociatedStatus = status;
            notifyListeners();
         }
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

   /**
    * Sets whether code for a user instance of the peripheral class should be created
    * 
    * @param value true to create instance
    */
   public void setCreateInstance(boolean value) {
      if (fCreateInstance != value) {
         fCreateInstance = value;
         setDirty(true);
         notifyListeners();
      }
   }

   /**
    * Indicates whether code for a instance or type declaration should be created in user code
    * 
    * @return true => Instance, false => Type
    */
   public Boolean getCreateInstance() {
      return fCreateInstance && getPeripheral().canCreateInstance(this);
   }

   /**
    * Indicates whether code for a user instance of the signal related class can be created
    * 
    * @return true to indicate an instance can be created
    */
   public boolean canCreateInstance() {
      return getPeripheral().canCreateInstance(this);
   }

   /**
    * Indicates whether code for a user instance of the signal related class can be created
    * 
    * @return true to indicate an instance can be created
    */
   public boolean canCreateType() {
      return getPeripheral().canCreateType(this);
   }
   
 }
