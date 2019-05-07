package net.sourceforge.usbdm.deviceEditor.information;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;

/**
 * Information about a pin<br>
 * <li>Pin name
 * <li>Peripheral signals mapped to that pin
 */
public class Pin extends ObservableModel implements Comparable<Pin>, IModelChangeListener {

   /** Pin used to denote a disabled mapping */
   public final static Pin UNASSIGNED_PIN = new Pin(null, "Unassigned");

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

   /** Map of signals mapped to this pin ordered by mux value */
   private Map<MuxSelection, MappingInfo> fMappableSignals = new TreeMap<MuxSelection, MappingInfo>();

   private MuxSelection fResetMuxValue = MuxSelection.unassigned;

   /** User description of pin use */
   private String fPinUseDescription = "";

   /** Current multiplexor setting */
   private MuxSelection fMuxValue = MuxSelection.unassigned;

   /** Device info owning this pin */
   private DeviceInfo fDeviceInfo;

   /** PCR value (excluding MUX) */
   private long fProperties = 0;

   /**
    * Create empty pin function for given pin
    * @param deviceInfo 
    * 
    * @param fVariantName Name of the pin, usually the port name e.g. PTA1
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
    * Connect Pin as listener for changes on pin multiplexing
    */
   public void connectListeners() {
      for (MuxSelection muxValue:fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxValue);
         mappingInfo.addListener(this);
      }
   }

   /**
    * Disconnect Pin as listener for changes on pin multiplexing
    */
   public void disconnectListeners() {
      for (MuxSelection muxValue:fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxValue);
         mappingInfo.removeListener(this);
      }
   }

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
    * Get Port base pointer e.g. PORTC_BasePtr
    * 
    * @return String or null if no associated PORT
    */
   public String getPortBasePtr() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("PORT%s_BasePtr", fPortInstance);
   }

   /**
    * Get Port e.g. PORTC
    * 
    * @return String or null if no associated PORT
    */
   public String getPort() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("PORT%s", fPortInstance);
   }

   /**
    * Get Port information name e.g. "PortBInfo"
    * 
    * @return String or null if no associated PORT
    */
   public String getPortInfo() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("Port%sInfo", fPortInstance);
   }

   //   /**
   //    * Get PCR register address as integer e.g. PORTC_BasePtr+offsetof(PORT_Type,PCR[2])
   //    * 
   //    * @return
   //    */
   //   public String getPCRasInt() {
   //      if (fPortInstance == null) {
   //         return null;
   //      }
   //      return String.format("%s+offsetof(PORT_Type,PCR[%s])", getPORTBasePtr(), fPortPin);
   //   }

   //   /**
   //    * Get clock mask e.g. PORTA_CLOCK_MASK
   //    * 
   //    * @return
   //    */
   //   public String getClockMask() {
   //      if (fPortInstance == null) {
   //         return null;
   //      }
   //      return String.format("PORT%s_CLOCK_MASK", fPortInstance);
   //   }

   /**
    * Get GPIO base pointer e.g. GPIOA_BasePtr
    * 
    * @return String or null if no associated GPIO
    */
   public String getGpioBasePtr() {
      if (fPortInstance == null) {
         return null;
      }
      return String.format("GPIO%s_BasePtr", fPortInstance);
   }

   /**
    * Get bit number of associated PORT/GPIO
    * 
    * @return String or null if no associated port
    */
   public String getGpioBitNum() {
      if (fPortInstance == null) {
         return null;
      }
      return fPortPin;
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
         location = fDeviceInfo.getVariant().getPackage().getLocation(this);
      }
      return location;
   }

   /**
    * Indicates if the pin is available in the package
    * 
    * @return
    */
   public boolean isAvailableInPackage() {
      if (this == UNASSIGNED_PIN) {
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
      if ((location == null) || (location.length() == 0)) {
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
      sb.append("Pin("+fName+",\n   R:"+fResetMuxValue+",\n   C:"+fMuxValue);
      for (MuxSelection muxSelection:fMappableSignals.keySet()) {
         sb.append(",\n   "+fMappableSignals.get(muxSelection).toString());
      }
      sb.append(")");
      return sb.toString();
   }

   /**
    * Sets the reset pin mapping
    * 
    * @param mux reset peripheral function on this pin<br>
    * 
    * @throws RuntimeException If pin already has default or new default not found as available pin mapping
    */
   public void setResetValue(MuxSelection mux) {
      if ((fResetMuxValue != mux) && (fResetMuxValue != MuxSelection.unassigned)) {
         throw new RuntimeException("Pin "+fName+" already has reset value "+fResetMuxValue);
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
    * Sets the reset peripheral signals for the pin
    * 
    * @param resetSignals  Name of signals to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception If pin already has reset or value not available pin mapping
    */
   public void setResetSignals(DeviceInfo factory, final String resetSignals) {
      for (MuxSelection muxValue:fMappableSignals.keySet()) {
         MappingInfo mappingInfo= fMappableSignals.get(muxValue);
         if (mappingInfo.getSignalList().equalsIgnoreCase(resetSignals) && (mappingInfo.getMux() != MuxSelection.unassigned)) {
            setResetValue(mappingInfo.getMux());
            break;
         }
      }
      if ((fResetMuxValue == MuxSelection.unassigned) && !resetSignals.equalsIgnoreCase("Disabled")) {
         throw new RuntimeException("Reset function "+resetSignals+" not found as option for pin " + fName);
      }

      //      // Disabled is not necessarily in list of mappings
      //      // If necessary create mux0=disabled if free
      //      if ((fResetMuxValue == MuxSelection.unassigned) && resetSignals.equalsIgnoreCase("Disabled")) {
      //         if (fMappedSignals.get(MuxSelection.mux0) == null) {
      //            factory.createMapping(Signal.DISABLED_SIGNAL, this, MuxSelection.mux0);
      //            setResetValue(MuxSelection.mux0);
      //         }
      //      }
      //      if (fResetMuxValue == MuxSelection.unassigned) {
      //         throw new RuntimeException("Reset function "+resetSignals+" not found as option for pin " + fName);
      //      }
   }

   //   /**
   //    * Get PCR initialisation string e.g. for <b><i>PTB4</b></i>
   //    * <pre>
   //    * "PORTB_CLOCK_MASK, PORTB_BasePtr,  GPIOB_BasePtr,  4, "
   //    * OR
   //    * "0, 0, 0, 0, "
   //    * </pre>
   //    * 
   //    * @param pin The pin being configured
   //    * 
   //    * @return
   //    * @throws Exception 
   //    */
   //   String getPCRInitString() throws Exception {
   //      String portClockMask = getClockMask();
   //      if (portClockMask == null) {
   //         // No PCR - probably an analogue pin
   //         return "0, 0, 0, 0, ";
   //      }
   //      String portAddress      = getPortBasePtr();
   //      String gpioRegister     = getGpioBasePtr();
   //      String gpioBitNum       = getGpioBitNum();
   //
   //      return String.format("%-17s %-15s %-15s %-4s", portClockMask+",", portAddress+",", gpioRegister+",", gpioBitNum+",");
   //   }

   @Override
   public int compareTo(Pin o) {
      return comparator.compare(fName, o.fName);
   }

   /**
    * Get map of signals mappable to this pin ordered by MUX value
    * 
    * @return
    */
   public Map<MuxSelection, MappingInfo> getMappableSignals() {
      return fMappableSignals;
   }

   /** 
    * Set description of pin use 
    */
   public void setPinUseDescription(String pinUseDescription) {
      fPinUseDescription = pinUseDescription;

      // Update watchers of active mapping
      MappingInfo mappingInfo = getMappedSignal();
      mappingInfo.notifyListeners();

      fDeviceInfo.setDirty(true);
      notifyListeners();
   }

   /** 
    * Get description of pin use 
    */
   public String getPinUseDescription() {
      return fPinUseDescription;
   }

   /**
    * Get the currently mapped signal for this pin
    * 
    * @return Mapped signal or <b>MappingInfo.DISABLED_MAPPING</b> if none
    */
   public MappingInfo getMappedSignal() {
      MappingInfo rv = fMappableSignals.get(fMuxValue);
      if (rv == null) {
         rv = MappingInfo.UNASSIGNED_MAPPING;
      }
      return rv;
   }

   /**
    * Sets the signal mapped to this pin
    * 
    * @param mappingInfo
    */
   public void setMappedSignal(MappingInfo mappingInfo) {
      setMuxSelection(mappingInfo.getMux());
   }

   /** 
    * Set current pin multiplexor setting 
    * 
    * @param newMuxValue Multiplexor value to set
    */
   public void setMuxSelection(MuxSelection newMuxValue) {
      //      System.err.println("Pin("+fName+")::setMuxSelection("+newMuxValue+")");
      if (this == UNASSIGNED_PIN) {
         return;
      }
      if (newMuxValue == fMuxValue) {
         return;
      }
      if ((fMuxValue == MuxSelection.fixed) && (newMuxValue != MuxSelection.fixed)) {
         System.err.println("Attempting to map fixed signal - ignored");
         return;
      }
      MappingInfo newMappingInfo = null;
      for (MuxSelection muxValue:fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxValue);
         if (muxValue == newMuxValue) {
            newMappingInfo = mappingInfo;
            continue;
         }
         if (muxValue == MuxSelection.fixed) {
            System.err.println("Attempting to unmap fixed signal");
            continue;
         }
         // Unmap existing mapping from this pin
         mappingInfo.select(null, false);
      }
      fMuxValue = newMuxValue;
      if (newMappingInfo != null) {
         newMappingInfo.select(null, true);
      }
      fDeviceInfo.setDirty(true);
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
         fResetMuxValue = MuxSelection.fixed;
         setMuxSelection(MuxSelection.fixed);
      }
      MappingInfo mapInfo = fMappableSignals.get(muxValue);
      if (mapInfo == null) {
         // Create new mapping
         mapInfo = new MappingInfo(this, muxValue);
         fMappableSignals.put(muxValue, mapInfo);
      }
      mapInfo.addSignal(signal);
      signal.addMappedPin(mapInfo);
      return mapInfo;
   }

   public static final String getMuxKey(String name) {
      return "$signal$"+name+"_muxSetting";
   }

   public static final String getDescriptionKey(String name) {
      return "$signal$"+name+"_descriptionSetting";
   }

   public static final String getPCRKey(String name) {
      return "$signal$"+name+"_pcrSetting";
   }

   /**
    * Load pin settings from settings object
    * 
    * @param settings Settings object
    */
   public void loadSettings(Settings settings) {
      String value = settings.get(getMuxKey(fName));
      if (value != null) {
         MuxSelection muxValue = MuxSelection.valueOf(value);
         setMuxSelection(muxValue);
      }
      value = settings.get(getDescriptionKey(fName));
      if (value != null) {
         setPinUseDescription(value);
      }
      value = settings.get(getPCRKey(fName));
      if (value != null) {
         setProperties(Long.parseLong(value, 16));
      }
   }

   /**
    * Save pin settings to settings object
    * 
    * @param settings Settings object
    */
   public void saveSettings(Settings settings) {
      if ((fMuxValue != MuxSelection.unassigned) && (fMuxValue != MuxSelection.fixed)) {
         settings.put(getMuxKey(fName), fMuxValue.name());
      }
      String desc = getPinUseDescription();
      if ((desc != null) && !desc.isEmpty()) {
         settings.put(getDescriptionKey(fName), getPinUseDescription());
      }
      if (getProperties() != 0) {
         settings.put(getPCRKey(fName), Long.toHexString(getProperties()));
      }
   }

   @Override
   public void modelElementChanged(ObservableModel model) {
      //      System.err.println("Pin["+fName+"].modelElementChanged("+model+")");
      if (model instanceof MappingInfo) {
         MappingInfo mappingInfo = (MappingInfo) model;
         if (mappingInfo.isSelected()) {
            // Signal has been mapped to this pin
            fMuxValue = mappingInfo.getMux();
            fDeviceInfo.setDirty(true);
         }
         else {
            // Pin may have been unmapped
            if (fMuxValue == mappingInfo.getMux()) {
               fMuxValue = MuxSelection.unassigned;
               fDeviceInfo.setDirty(true);
            }
         }
         if (fMuxValue == null) {
            throw new RuntimeException("Impossible mapping");
         }
      }
      //      System.err.println("Pin("+fName+").modelElementChanged("+fMuxValue+") - Changed");
      notifyListeners();
   }

   public void setPort(Signal signal) throws Exception {
      Pattern p = Pattern.compile("^\\s*GPIO(.)_(\\d*)\\s*$");
      Matcher m = p.matcher(signal.getName());
      if (m.matches()) {
         if (fDeviceInfo.getDeviceFamily() != DeviceFamily.mke) {
            String portInstance = m.group(1);
            String portPin      = m.group(2);
            if (fPortInstance == null) {
               fPortInstance = portInstance;
               fPortPin      = portPin;
            }
            else {
               if (!fPortInstance.equals(portInstance) || !fPortPin.equals(portPin)) {
                  throw new Exception("Pin associated with multiple PORTs, 1st="+fPortInstance+" , 2nd="+portPin);
               }
            }
         }
         else {
            /* 
             * MKE unusual port mapping
             * GPIOA = [PTD, PTC, PTB, PTA]
             * GPIOB = [PTH, PTG, PTF, PTE]
             */

            if (m.matches()) {
               int portPinNumber = Integer.parseUnsignedInt(m.group(2));
               String portInstance = m.group(1);
               String portPin      = Integer.toString(portPinNumber%8);
               final String[] APorts = {"A", "B", "C", "D"}; 
               final String[] BPorts = {"E", "F", "G", "H"}; 
               final String[] CPorts = {"I", "X", "X", "X"}; 
               switch(m.group(1)) {
               case "A" : 
                  portInstance = APorts[portPinNumber/8];
                  break;
               case "B" : 
                  portInstance = BPorts[portPinNumber/8];
                  break;
               case "C" : 
                  portInstance = CPorts[portPinNumber/8];
                  break;
               }
               if (fPortInstance == null) {
                  fPortInstance = portInstance;
                  fPortPin      = portPin;
               }
               else {
                  if (!fPortInstance.equals(portInstance) || !fPortPin.equals(portPin)) {
                     throw new Exception("Pin associated with multiple PORTs, 1st="+fPortInstance+" , 2nd="+portPin);
                  }
               }
            }
         }
      }
   }

   /**
    * Class representing Pin Interrupt/DMA functions
    */
   public enum PinIntDmaValue {
      disabled(   0,  "Disabled"),
      dmaRising(  1,  "DMA rising edge"),
      dmaFalling( 2,  "DMA falling edge"),
      dmaEither(  3,  "DMA either edge"),
      intLow(     8,  "INT when low"),
      intRising(  9,  "INT rising edge"),
      intFalling( 10, "INT falling edge"),
      intEither(  11, "INT either edge"),
      intHigh(    12, "INT when high");

      static final String[] choices = {
            disabled.getName(),
            dmaRising.getName(),
            dmaFalling.getName(),
            dmaEither.getName(),
            intLow.getName(),
            intRising.getName(),
            intFalling.getName(),
            intEither.getName(),
            intHigh.getName(),
      };

      private final int     value;
      private final String  name;

      private PinIntDmaValue(int value, String name) {
         this.value  = value;
         this.name   = name;
      }
      /**
       * Maps an integer into a PinIntDmaValue value
       * 
       * @param value Value to map
       * 
       * @return Corresponding PinIntDmaValue value
       */
      public static PinIntDmaValue valueOf(int value) {
         switch(value) {
         case 0  : return disabled;
         case 1  : return dmaRising;
         case 2  : return dmaFalling;
         case 3  : return dmaEither;
         case 8  : return intLow;
         case 9  : return intRising;
         case 10 : return intFalling;
         case 11 : return intEither;
         case 12 : return intHigh;
         default : return disabled;
         }
      }

      /**
       * Maps an integer into a PinIntDmaValue value
       * 
       * @param value Value to map
       * 
       * @return Corresponding PinIntDmaValue value
       */
      public static PinIntDmaValue getNameFromDescription(String description) {
         for (int index=0; index<choices.length; index++) {
            if (choices[index].equalsIgnoreCase(description)) {
               return PinIntDmaValue.values()[index];
            }
         }
         throw new RuntimeException("No matching enum for " + description);
      }
      public String getName() {
         return name;
      }

      public static String[] getChoices() {
         return choices;
      }

      public int getValue() {
         return value;
      }
   }

   /**
    * Get Pin Interrupt/DMA functions
    * 
    * @return function
    */
   public PinIntDmaValue getInterruptDmaSetting() {
      return PinIntDmaValue.valueOf((int)getProperty(PORT_PCR_IRQC_MASK, PORT_PCR_IRQC_SHIFT));
   }

   /**
    * Set Pin Interrupt/DMA functions
    * 
    * @param value Function to set
    */
   public void setInterruptDmaSetting(PinIntDmaValue value) {
      setProperty(PORT_PCR_IRQC_MASK, PORT_PCR_IRQC_SHIFT, value.getValue());
   }

   /**
    * Class representing Pin Interrupt/DMA functions
    */
   public enum PinPullValue {
      none(  0,  "None"),
      down(  2,  "Down"),
      up(    3,  "Up");

      static final String[] choices = {
            none.getName(),
            down.getName(),
            up.getName(),
      };

      private final int     value;
      private final String  name;

      private PinPullValue(int value, String name) {
         this.value  = value;
         this.name   = name;
      }
      /**
       * Maps an integer into a PinIntDmaValue value
       * 
       * @param value Value to map
       * 
       * @return Corresponding PinIntDmaValue value
       */
      public static PinPullValue valueOf(int value) {
         switch(value) {
         case 0  : return none;
         case 2  : return down;
         case 3  : return up;
         default : return none;
         }
      }

      /**
       * Maps an integer into a PinIntDmaValue value
       * 
       * @param value Value to map
       * 
       * @return Corresponding PinIntDmaValue value
       */
      public static PinIntDmaValue getNameFromDescription(String description) {
         for (int index=0; index<choices.length; index++) {
            if (choices[index].equalsIgnoreCase(description)) {
               return PinIntDmaValue.values()[index];
            }
         }
         throw new RuntimeException("No matching enum for " + description);
      }
      public String getName() {
         return name;
      }

      public static String[] getChoices() {
         return choices;
      }

      public int getValue() {
         return value;
      }
   }

   /**
    * Get Pin Interrupt/DMA functions
    * 
    * @return function
    */
   public PinPullValue getPullSetting() {
      return PinPullValue.valueOf((int)getProperty(PORT_PCR_PULL_MASK, PORT_PCR_PULL_SHIFT));
   }

   /**
    * Set Pin Interrupt/DMA functions
    * 
    * @param value Function to set
    */
   public void setPullSetting(PinPullValue value) {
      setProperty(PORT_PCR_PULL_MASK, PORT_PCR_PULL_SHIFT, value.getValue());
   }

   public final static long PORT_PCR_PULL_SHIFT  =  0;                             
   public final static long PORT_PCR_PULL_MASK   =  (0x03L << PORT_PCR_PULL_SHIFT);  
   public final static long PORT_PCR_SRE_SHIFT   =  2;                             
   public final static long PORT_PCR_SRE_MASK    =  (0x01L << PORT_PCR_SRE_SHIFT); 
   public final static long PORT_PCR_PFE_SHIFT   =  4;                             
   public final static long PORT_PCR_PFE_MASK    =  (0x01L << PORT_PCR_PFE_SHIFT); 
   public final static long PORT_PCR_ODE_SHIFT   =  5;                             
   public final static long PORT_PCR_ODE_MASK    =  (0x01L << PORT_PCR_ODE_SHIFT); 
   public final static long PORT_PCR_DSE_SHIFT   =  6;                             
   public final static long PORT_PCR_DSE_MASK    =  (0x01L << PORT_PCR_DSE_SHIFT); 
   public final static long PORT_PCR_MUX_SHIFT   =  8;                             
   public final static long PORT_PCR_MUX_MASK    =  (0x07L << PORT_PCR_MUX_SHIFT); 
   public final static long PORT_PCR_LK_SHIFT    =  15;                            
   public final static long PORT_PCR_LK_MASK     =  (0x01L << PORT_PCR_LK_SHIFT);  
   public final static long PORT_PCR_IRQC_SHIFT  =  16;                            
   public final static long PORT_PCR_IRQC_MASK   =  (0x0FL << PORT_PCR_IRQC_SHIFT); 
   public final static long PORT_PCR_ISF_SHIFT   =  24;                            
   public final static long PORT_PCR_ISF_MASK    =  (0x01L << PORT_PCR_ISF_SHIFT); 
   public final static long PORT_PCR_MASK        =  
         PORT_PCR_PULL_MASK|PORT_PCR_SRE_MASK|PORT_PCR_PFE_MASK|PORT_PCR_ODE_MASK|
         PORT_PCR_DSE_MASK|PORT_PCR_LK_MASK|PORT_PCR_IRQC_MASK; 

   /**
    * Get PCR value (excluding MUX)
    * 
    * @return PCR value (excluding MUX)
    */
   public long getProperties() {
      return fProperties & PORT_PCR_MASK;
   }

   /**
    * Set PCR value (excluding MUX)
    * 
    * @param properties PCR value (excluding MUX)
    */
   public boolean setProperties(long properties) {
      properties &= PORT_PCR_MASK;
      if (fProperties == properties) {
         return false;
      }
      fProperties = properties;
      return true;
   }

   public long getProperty(long mask, long offset) {
      return (getProperties()&mask)>>offset;
   }

   public boolean setProperty(long mask, long offset, long property) {
      return setProperties((getProperties()&~mask)|((property<<offset)&mask));
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      notifyStructureChangeListeners();
   }

   @Override
   public void elementStatusChanged(ObservableModel model) {
      notifyStatusListeners();
   }

   public long getPcrValue() {
      return getProperties() | ((getMuxValue().value<<PORT_PCR_MUX_SHIFT)&PORT_PCR_MUX_MASK);
   }

   public String getPcrValueAsString() {
      long pcrValue = getProperties() | ((getMuxValue().value<<PORT_PCR_MUX_SHIFT)&PORT_PCR_MUX_MASK);
      return getPcrValueAsString(pcrValue);
   }

   public static String getPcrValueAsString(long pcrValue) {
      StringBuilder sb = new StringBuilder();
      sb.append("PORT_PCR_MUX(" +((pcrValue & PORT_PCR_MUX_MASK) >> PORT_PCR_MUX_SHIFT)+")|");
      sb.append("PORT_PCR_DSE(" +((pcrValue & PORT_PCR_DSE_MASK) >> PORT_PCR_DSE_SHIFT)+")|");
      sb.append("PORT_PCR_IRQC("+((pcrValue & PORT_PCR_IRQC_MASK)>> PORT_PCR_IRQC_SHIFT)+")|");
      sb.append("PORT_PCR_ISF(" +((pcrValue & PORT_PCR_ISF_MASK) >> PORT_PCR_ISF_SHIFT)+")|");
      sb.append("PORT_PCR_LK("  +((pcrValue & PORT_PCR_LK_MASK) >> PORT_PCR_LK_SHIFT)+")|");
      sb.append("PORT_PCR_ODE(" +((pcrValue & PORT_PCR_ODE_MASK) >> PORT_PCR_ODE_SHIFT)+")|");
      sb.append("PORT_PCR_PFE(" +((pcrValue & PORT_PCR_PFE_MASK) >> PORT_PCR_PFE_SHIFT)+")|");
      sb.append("PORT_PCR_SRE(" +((pcrValue & PORT_PCR_SRE_MASK) >> PORT_PCR_SRE_SHIFT)+")|");
      sb.append("PORT_PCR_PE("  +((pcrValue & PORT_PCR_PULL_MASK) >> PORT_PCR_MUX_SHIFT)+")|");
      sb.append("PORT_PCR_PS("  +((pcrValue & PORT_PCR_PULL_MASK) >> PORT_PCR_MUX_SHIFT)+")");

      return sb.toString();
   }
}
