package net.sourceforge.usbdm.deviceEditor.information;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Information about a pin<br>
 * <li>Pin name
 * <li>Peripheral signals mapped to that pin
 */
public class Pin extends ObservableModel implements Comparable<Pin>, IModelChangeListener {
   
   /** Pin used to denote a disabled mapping */
   public final static Pin DISABLED_PIN = new Pin(null, "Disabled");
   
   /** Pin comparator */
   public static Comparator<String> comparator = Utils.comparator;
   
   /*
    * ==================================================================================
    */
   /** Port instance e.g. PTA3 => A (only if has PCR) */
   private String fPortInstance = null;

   /** Pin instance e.g. PTA3 => 3 (only if has PCR) */
   private String fPortPin = null;
   
   /** Name of the pin, usually the port name e.g. PTA1 */
   private String fName;
   
//   /** Description of peripheral signals that may be mapped to this pin */
//   private StringBuilder  fDescription = new StringBuilder();

//   /**
//    * Peripheral signals associated with this pin<br> 
//    */
//   private  ArrayList<Peripheralsignal> fMappedPins = new ArrayList<Peripheralsignal>();

   /** Map of signals mapped to this pin ordered by mux value */
   private Map<MuxSelection, MappingInfo> fMappedSignals = new TreeMap<MuxSelection, MappingInfo>();

//   /**
//    * Peripheral signals associated with this pin arranged by signal base name<br> 
//    * e.g. multiple FTMs may be associated with a pin 
//    */
//   private HashMap<String, ArrayList<MappingInfo>> fMappedPinsBysignal = new HashMap<String, ArrayList<MappingInfo>>();

   /** Multiplexor value at reset */
   private MuxSelection fResetMuxValue = MuxSelection.unused;

   /** Default multiplexor value */
   private MuxSelection fDefaultMuxValue = MuxSelection.unused;

   /** User description of pin use */
   private String fPinUseDescription = "";

   /** Current multiplexor setting */
   private MuxSelection fMuxValue = MuxSelection.unused;
   
   /** Device info owning this pin */
   private DeviceInfo fDeviceInfo;

   /**
    * Get PCR register e.g. PORTA->PCR[3]
    * 
    * @return
    */
   public String getPCR() {
      if (fPortInstance == null) {
         return "0";
      }
      return String.format("&PORT%s->PCR[%s]", fPortInstance, fPortPin);
   }
   
   /**
    * Get Gpio class declaration e.g. GpioA<3>
    * 
    * @return
    */
   public String getGpioClass() {
      return String.format("Gpio%s<%s>", fPortInstance, fPortPin);
   }
   
   /**
    * Get Base pointer for Port e.g. PORTC_BasePtr
    * 
    * @return
    */
   public String getPORTBasePtr() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("PORT%s_BasePtr", fPortInstance);
   }
   
   /**
    * Get PCR register address as integer e.g. PORTC_BasePtr+offsetof(PORT_Type,PCR[2])
    * 
    * @return
    */
   public String getPCRasInt() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("%s+offsetof(PORT_Type,PCR[%s])", getPORTBasePtr(), fPortPin);
   }
   
   /**
    * Get clock mask e.g. PORTA_CLOCK_MASK
    * 
    * @return
    */
   public String getClockMask() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("PORT%s_CLOCK_MASK", fPortInstance);
   }
   
   /**
    * Get clock mask e.g. PORTA_CLOCK_MASK
    * 
    * @return
    */
   public String getGpioReg() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("GPIO%s_BasePtr", fPortInstance);
   }
   
   /**
    * Get clock mask e.g. PORTA_CLOCK_MASK
    * 
    * @return
    */
   public String getGpioBitNum() {
      if (fPortInstance == null) {
         return null;
      }
      return fPortPin;
   }
   
   /**
    * Create empty pin function for given pin
    * @param deviceInfo 
    * 
    * @param fName Name of the pin, usually the port name e.g. PTA1
    */
   Pin(DeviceInfo deviceInfo, String name) {
      fDeviceInfo = deviceInfo;
      this.fName  = name;
      Pattern p = Pattern.compile("^\\s*PT(.)(\\d*)\\s*$");
      Matcher m = p.matcher(name);
      if (m.matches()) {
         fPortInstance = m.group(1);
         fPortPin      = m.group(2);
      }
   }
   
   /**
    * Get name of the pin, usually the port name e.g. PTA1 
    * 
    * @return Pin name
    */
   public String getName() {
      return fName;
   }
   
   /**
    * Get location of the pin e.g. p23 
    * 
    * @return Pin name
    */
   public String getLocation() {
      String location = null;
      if (fDeviceInfo != null) {
         location = fDeviceInfo.getDeviceVariant().getPackage().getLocation(this);
      }
      return location;
   }
   
   /**
    * Indicates if the pin is available in the package
    * 
    * @return
    */
   public boolean isAvailableInPackage() {
      if (this == DISABLED_PIN) {
         // The disabled pin is always available
         return true;
      }
      return getLocation() != null;
   }

   /**
    * Get name of the pin with package location e.g. PTA1(p36) 
    * 
    * @return Pin name
    */
   public String getNameWithLocation() {
      String location = getLocation();
      if (location == null) {
         return fName;
      }
      else {  
         return fName + " ("+location+")";
      }
   }
   
   /**
    * Get description of functions mapped to this pin
    * 
    * @return Description
    */
   public String getDescription() {
      return "";
   }
    
   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Pin("+fName+",\n   R:"+fResetMuxValue+",\n   D:"+fDefaultMuxValue+",\n   C:"+fMuxValue);
      for (MuxSelection muxSelection:fMappedSignals.keySet()) {
         sb.append(",\n   "+fMappedSignals.get(muxSelection).toString());
      }
      sb.append(")");
      return sb.toString();
   }

   /**
    * Sets the reset pin mapping
    * 
    * @param mux reset peripheral function on this pin
    * 
    * @throws RuntimeException If pin already has default or new default not found as available pin mapping
    */
   public void setResetValue(MuxSelection mux) {
      if ((mux !=fResetMuxValue) && (fResetMuxValue != MuxSelection.unused)) {
         throw new RuntimeException("Pin "+getName()+" already has reset value "+fResetMuxValue);
      }
      fResetMuxValue = mux;
   }

   /**
    * Returns the reset pin mapping
    * 
    * @return reset peripheral function on this pin
    */
   public MuxSelection getResetValue() {
      return fResetMuxValue;
   }

   /**
    * Sets the default pin mapping
    * 
    * @param mux default peripheral function on this pin
    * 
    * @throws RuntimeException If pin already has default or new default not found as available pin mapping
    */
   public void setDefaultValue(MuxSelection mux) {
      if ((mux != fDefaultMuxValue) && (fDefaultMuxValue != MuxSelection.unused)) {
         throw new RuntimeException("Pin "+getName()+" already has default value "+fDefaultMuxValue);
      }
      if (fMuxValue == MuxSelection.unused) {
         fMuxValue = mux;
      }
      fDefaultMuxValue = mux;
   }

   /**
    * Returns the default pin mapping
    * 
    * @return default peripheral function on this pin
    */
   public MuxSelection getDefaultValue() {
      if (fMuxValue == MuxSelection.unused) {
         fMuxValue = fResetMuxValue;
      }
      return fDefaultMuxValue;
   }

   /**
    * Sets the default peripheral functions for the pin
    * 
    * @param defaultPeripheralName  Name of peripherals to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception If pin already has default or new default not found as available pin mapping
    */
   public void setDefaultPeripheralFunctions(DeviceInfo factory, final String defaultPeripheralName) {
      if (fDefaultMuxValue != MuxSelection.unused) {
         throw new RuntimeException("Pin "+getName()+" already has default value "+fDefaultMuxValue);
      }
      Map<MuxSelection, MappingInfo> functionMappings = getMappedSignals();
      
      for (MuxSelection functionMappingIndex:functionMappings.keySet()) {
         MappingInfo mappingInfo= functionMappings.get(functionMappingIndex);
         if (mappingInfo.getSignalList().equalsIgnoreCase(defaultPeripheralName) && (mappingInfo.getMux() != MuxSelection.reset)) {
            setDefaultValue(mappingInfo.getMux());
            break;
         }
      }
      if (fDefaultMuxValue == null) {
         throw new RuntimeException("Peripheral "+defaultPeripheralName+" not found as option for pin " + getName());
      }
   }

   /**
    * Sets the reset peripheral functions for the pin
    * 
    * @param resetFunctionName  Name of peripheral functions to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception If pin already has default or new default not found as available pin mapping
    */
   public void setResetPeripheralFunctions(DeviceInfo factory, final String resetFunctionName) {
      // Should be one of the mappings given (or disabled which defaults to mux 0)
      Map<MuxSelection, MappingInfo> functionMappings = getMappedSignals();
      for (MuxSelection functionMappingIndex:functionMappings.keySet()) {
         MappingInfo mappingInfo= functionMappings.get(functionMappingIndex);
         if ((mappingInfo.getMux() != MuxSelection.reset && mappingInfo.getSignalList().equalsIgnoreCase(resetFunctionName))) {
            setResetValue(mappingInfo.getMux());
            break;
         }
      }
      // Disabled is not necessarily in list of mappings
      // If necessary create mux0=disabled if free
      if ((fResetMuxValue == MuxSelection.unused) && resetFunctionName.equalsIgnoreCase(MuxSelection.disabled.name())) {
         if (functionMappings.get(MuxSelection.mux0) == null) {
            factory.createMapping(Signal.DISABLED_SIGNAL, this, MuxSelection.mux0);
            setResetValue(MuxSelection.mux0);
         }
      }
      if (fResetMuxValue == MuxSelection.unused) {
         throw new RuntimeException("Function "+resetFunctionName+" not found as option for pin " + getName());
      }
   }

   /**
    * Get PCR initialisation string e.g. for <b><i>PTB4</b></i>
    * <pre>
    * "PORTB_CLOCK_MASK, PORTB_BasePtr,  GPIOB_BasePtr,  4, "
    * OR
    * "0, 0, 0, 0, "
    * </pre>
    * 
    * @param pin The pin being configured
    * 
    * @return
    * @throws Exception 
    */
   String getPCRInitString() throws Exception {
      String portClockMask = getClockMask();
      if (portClockMask == null) {
         // No PCR - probably an analogue pin
         return "0, 0, 0, 0, ";
      }
      String pcrRegister      = getPORTBasePtr();
      String gpioRegister     = getGpioReg();
      String gpioBitNum       = getGpioBitNum();

      return String.format("%-17s %-15s %-15s %-4s", portClockMask+",", pcrRegister+",", gpioRegister+",", gpioBitNum+",");
   }

   @Override
   public int compareTo(Pin o) {
      return comparator.compare(getName(), o.getName());
   }

   /**
    * Get map of signals mapped to this pin ordered by mux value
    * 
    * @return
    */
   public Map<MuxSelection, MappingInfo> getMappedSignals() {
      return fMappedSignals;
   }

   /** 
    * Set description of pin use 
    */
   public void setPinUseDescription(String pinUseDescription) {
      fPinUseDescription = pinUseDescription;
      notifyListeners();
   }

   /** 
    * Get description of pin use 
    */
   public String getPinUseDescription() {
      return fPinUseDescription;
   }

   /**
    * Connect Pin as listener for changes on pin multiplexing
    */
   public void connectListeners() {
      for (MuxSelection muxValue:fMappedSignals.keySet()) {
         MappingInfo mappingInfo = fMappedSignals.get(muxValue);
         mappingInfo.addListener(this);
      }
   }
   
   /** 
    * Set current pin multiplexor setting 
    * 
    * @param newMuxValue Multiplexor value to set
    */
   public void setMuxSelection(MuxSelection newMuxValue) {
      System.err.println("Pin("+fName+")::setMuxSelection("+newMuxValue+")");
      if (this == DISABLED_PIN) {
         return;
      }
      fMappedSignals.get(fMuxValue).select(MappingInfo.Origin.pin, false);
      fMuxValue = newMuxValue;
      fMappedSignals.get(newMuxValue).select(MappingInfo.Origin.pin, true);
   }

   /** 
    * Get current pin multiplexor value 
    */
   public MuxSelection getMuxValue() {
      return fMuxValue;
   }

   /**
    * Add a signal mapping to this pin
    * 
    * @param signal      Signal to add
    * @param muxValue    Mux selection to select this signal on pin
    * @return
    */
   public MappingInfo addSignal(Signal signal, MuxSelection muxValue) {
      if (muxValue == MuxSelection.fixed) {
         fResetMuxValue    = MuxSelection.fixed;
         fDefaultMuxValue  = MuxSelection.fixed;
      }
      MappingInfo mapInfo = fMappedSignals.get(muxValue);
      if (mapInfo == null) {
         // Create new mapping
         mapInfo = new MappingInfo(this, muxValue);
         fMappedSignals.put(muxValue, mapInfo);
      }
      mapInfo.addSignal(signal);
      signal.addMapping(mapInfo);
      return mapInfo;
   }

   /** Key for mux selection persistence */
   public static final String MUX_SETTINGS_KEY = "_muxSetting"; 
   
   /** Key for description selection persistence */
   public static final String DESCRIPTION_SETTINGS_KEY = "_descriptionSetting"; 
   
   /**
    * Load pin settings from settings object
    * 
    * @param settings Settings object
    */
   public void loadSettings(DialogSettings settings) {
      String value = settings.get(fName+MUX_SETTINGS_KEY);
      if (value != null) {
         MuxSelection muxValue = MuxSelection.valueOf(value);
         setMuxSelection(muxValue);
      }
      value = settings.get(fName+DESCRIPTION_SETTINGS_KEY);
      if (value != null) {
         setPinUseDescription(value);
      }
   }

   /**
    * Save pin settings to settings object
    * 
    * @param settings Settings object
    */
   public void saveSettings(DialogSettings settings) {
      if (fMuxValue != fDefaultMuxValue) {
         settings.put(fName+MUX_SETTINGS_KEY, fMuxValue.name());
      }
      String desc = getPinUseDescription();
      if ((desc != null) && !desc.isEmpty()) {
         settings.put(fName+DESCRIPTION_SETTINGS_KEY, getPinUseDescription());
      }
   }

   /**
    * Get the currently mapped signal for this pin
    * 
    * @return Mapped signal or null if none
    */
   public MappingInfo getMappedSignal() {
      return fMappedSignals.get(fMuxValue);
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      if (model instanceof MappingInfo) {
         for (MuxSelection mux: fMappedSignals.keySet()) {
            MappingInfo mappingInfo = fMappedSignals.get(mux);
            if (mappingInfo.isSelected()) {
               fMuxValue = mux;
            }
         }
         notifyListeners();
      }
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      notifyStructureChangeListeners();
   }
}
