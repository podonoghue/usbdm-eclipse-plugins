package net.sourceforge.usbdm.deviceEditor.information;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.DialogSettings;

import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Information about a pin<br>
 * <li>Pin name
 * <li>Peripheral functions mapped to that pin
 */
public class PinInformation extends ObservableModel implements Comparable<PinInformation> {
   
   /** Pin used to denote a disabled mapping */
   public final static PinInformation DISABLED_PIN = new PinInformation(null, "Disabled");
   
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
   
//   /** Description of peripheral functions that may be mapped to this pin */
//   private StringBuilder  fDescription = new StringBuilder();

//   /**
//    * Peripheral functions associated with this pin<br> 
//    */
//   private  ArrayList<PeripheralFunction> fMappedPins = new ArrayList<PeripheralFunction>();

   /** Map of functions mapped to this pin ordered by mux value */
   private Map<MuxSelection, MappingInfo> fMappedFunctions = new TreeMap<MuxSelection, MappingInfo>();

//   /**
//    * Peripheral functions associated with this pin arranged by function base name<br> 
//    * e.g. multiple FTMs may be associated with a pin 
//    */
//   private HashMap<String, ArrayList<MappingInfo>> fMappedPinsByFunction = new HashMap<String, ArrayList<MappingInfo>>();

   /** Function mapped at reset */
   private MuxSelection fResetFunction = MuxSelection.unused;

   /** Default functions */
   private MuxSelection fDefaultFunction = MuxSelection.unused;

   /** Used description of pin use */
   private String fPinUseDescription = "";

   /** Current pin multiplexor setting */
   private MuxSelection fMuxSelection = MuxSelection.unused;
   
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
   PinInformation(DeviceInfo deviceInfo, String name) {
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
    * Get name of the pin, usually the port name e.g. PTA1 
    * 
    * @return Pin name
    */
   public String getNameWithLocation() {
      String location = "";
      if (fDeviceInfo != null) {
         location = " ("+fDeviceInfo.getDeviceVariant().getPackage().getLocation(this)+")";
         
      }
      return fName+location;
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
      sb.append("Pin("+fName+",\n   R:"+fResetFunction+",\n   D:"+fDefaultFunction+",\n   C:"+fMuxSelection);
      for (MuxSelection muxSelection:fMappedFunctions.keySet()) {
         sb.append(",\n   "+fMappedFunctions.get(muxSelection).toString());
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
      if ((mux !=fResetFunction) && (fResetFunction != MuxSelection.unused)) {
         throw new RuntimeException("Pin "+getName()+" already has reset value "+fResetFunction);
      }
      fResetFunction = mux;
   }

   /**
    * Returns the reset pin mapping
    * 
    * @return reset peripheral function on this pin
    */
   public MuxSelection getResetValue() {
      return fResetFunction;
   }

   /**
    * Sets the default pin mapping
    * 
    * @param mux default peripheral function on this pin
    * 
    * @throws RuntimeException If pin already has default or new default not found as available pin mapping
    */
   public void setDefaultValue(MuxSelection mux) {
      if ((mux != fDefaultFunction) && (fDefaultFunction != MuxSelection.unused)) {
         throw new RuntimeException("Pin "+getName()+" already has default value "+fDefaultFunction);
      }
      if (fMuxSelection == MuxSelection.unused) {
         fMuxSelection = mux;
      }
      fDefaultFunction = mux;
   }

   /**
    * Returns the default pin mapping
    * 
    * @return default peripheral function on this pin
    */
   public MuxSelection getDefaultValue() {
      if (fMuxSelection == MuxSelection.unused) {
         fMuxSelection = fResetFunction;
      }
      return fDefaultFunction;
   }

   /**
    * Sets the default peripheral functions for the pin
    * 
    * @param defaultPeripheralName  Name of peripherals to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception If pin already has default or new default not found as available pin mapping
    */
   public void setDefaultPeripheralFunctions(DeviceInfo factory, final String defaultPeripheralName) {
      if (fDefaultFunction != MuxSelection.unused) {
         throw new RuntimeException("Pin "+getName()+" already has default value "+fDefaultFunction);
      }
      Map<MuxSelection, MappingInfo> functionMappings = getMappedFunctions();
      
      for (MuxSelection functionMappingIndex:functionMappings.keySet()) {
         MappingInfo mappingInfo= functionMappings.get(functionMappingIndex);
         if (mappingInfo.getFunctionList().equalsIgnoreCase(defaultPeripheralName) && (mappingInfo.getMux() != MuxSelection.reset)) {
            setDefaultValue(mappingInfo.getMux());
            break;
         }
      }
      if (fDefaultFunction == null) {
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
      Map<MuxSelection, MappingInfo> functionMappings = getMappedFunctions();
      for (MuxSelection functionMappingIndex:functionMappings.keySet()) {
         MappingInfo mappingInfo= functionMappings.get(functionMappingIndex);
         if ((mappingInfo.getMux() != MuxSelection.reset && mappingInfo.getFunctionList().equalsIgnoreCase(resetFunctionName))) {
            setResetValue(mappingInfo.getMux());
            break;
         }
      }
      // Disabled is not necessarily in list of mappings
      // If necessary create mux0=disabled if free
      if ((fResetFunction == MuxSelection.unused) && resetFunctionName.equalsIgnoreCase(MuxSelection.disabled.name())) {
         if (functionMappings.get(MuxSelection.mux0) == null) {
            factory.createMapping(PeripheralFunction.DISABLED, this, MuxSelection.mux0);
            setResetValue(MuxSelection.mux0);
         }
      }
      if (fResetFunction == MuxSelection.unused) {
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
   public int compareTo(PinInformation o) {
      return comparator.compare(getName(), o.getName());
   }

   /**
    * Get map of functions mapped to this pin ordered by mux value
    * 
    * @return
    */
   public Map<MuxSelection, MappingInfo> getMappedFunctions() {
      return fMappedFunctions;
   }

   /** Set description of pin use */
   public void setPinUseDescription(String pinUseDescription) {
      fPinUseDescription = pinUseDescription;
      notifyListeners();
   }

   /** Get description of pin use */
   public String getPinUseDescription() {
      return fPinUseDescription;
   }

   /** Set current pin multiplexor setting */
   public void setMuxSelection(MuxSelection muxValue) {
      MuxSelection oldValue = fMuxSelection;
      fMuxSelection = muxValue;
      if (fMuxSelection != oldValue) {
         notifyListeners();
      }
   }

   /** Get current pin multiplexor setting */
   public MuxSelection getMuxSelection() {
      return fMuxSelection;
   }

   /**
    * Add a function mapping to this pin
    * 
    * @param function            Function to add
    * @param functionSelector    Mux selection to select this function on pin
    * @return
    */
   public MappingInfo addFunction(PeripheralFunction function, MuxSelection functionSelector) {
      if (functionSelector == MuxSelection.fixed) {
         fResetFunction    = MuxSelection.fixed;
         fDefaultFunction  = MuxSelection.fixed;
      }
      MappingInfo mapInfo = fMappedFunctions.get(functionSelector);
      if (mapInfo == null) {
         // Create new mapping
         mapInfo = new MappingInfo(this, functionSelector);
         fMappedFunctions.put(functionSelector, mapInfo);
      }
      mapInfo.getFunctions().add(function);
      function.addMapping(mapInfo);
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
      if (fMuxSelection != fDefaultFunction) {
         settings.put(fName+MUX_SETTINGS_KEY, fMuxSelection.name());
      }
      String desc = getPinUseDescription();
      if ((desc != null) && !desc.isEmpty()) {
         settings.put(fName+DESCRIPTION_SETTINGS_KEY, getPinUseDescription());
      }
   }

}
