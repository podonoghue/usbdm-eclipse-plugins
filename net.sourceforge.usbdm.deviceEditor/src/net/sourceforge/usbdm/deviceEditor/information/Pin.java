package net.sourceforge.usbdm.deviceEditor.information;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo.Origin;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModel;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;

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
   /** Device info owning this pin */
   private final DeviceInfo fDeviceInfo;

   /** Name of the pin, usually the port name e.g. PTA1 */
   private final String fName;

   /** Port instance e.g. PTA3 => A (only if has PCR) */
   private String fPortInstance = null;

   /** Pin instance e.g. PTA3 => 3 (only if has PCR) */
   private String fPortPin = null;

   /** Map of signals mappable to this pin ordered by mux value */
   private Map<MuxSelection, MappingInfo> fMappableSignals = new TreeMap<MuxSelection, MappingInfo>();

   private MuxSelection fResetMuxValue = MuxSelection.unassigned;

   /** PCR value (excluding MUX) */
   private long fProperties = 0;

   /** Status indicating if multiple Signals are mapped to this pin */ 
   private Status fStatus = null;

   /** Status of associated signals i.e. if multiple the mapped signal is also mapped to another pin */ 
   private Status fAssociatedStatus = null;

   private String fMappedSignalsCodeIdentifiers="";

   private String fMappedSignalsUserDescriptions="";

   /** 
    * Split a description or code identifier. <br>
    * Semicolons may be escaped with '\' <br>
    * Names are always non-null but may be empty<br>
    * Example: <br>
    * <pre>
    *   "first;second"  => "first"         "second"       
    *   "first\;first"  => "first;first"   ""             
    *   "first"         => "first"         ""             
    *   "first\;"       => "first;"        ""             
    *   ";second"       => ""              "second"       
    *   "\;first"       => ";first"        ""             
    *   ""              => ""              ""             
    * </pre>   
    * 
    * @param names Names to split
    * @return  Array of split names
    */
   static String[] splitNames(String names) {
      String[] res = {"", ""};
      boolean escaped  = false;
      boolean complete = false;
      StringBuilder sb = new StringBuilder();
      for (int index=0; index<names.length(); index++) {
         char ch = names.charAt(index);
         if (complete) {
            res[1] = names.substring(index).trim();
            break;
         }
         if (escaped) {
            sb.append(ch);
            continue;
         }
         escaped = false;
         if (ch == '\\') {
            escaped = true;
            continue;
         }
         if (ch == ';') {
            complete = true;
            continue;
         }
         sb.append(ch);
      }
      res[0]   = sb.toString().trim();
      return res;
   }
   
   /**
    * Create empty pin function for given pin
    * @param deviceInfo 
    * 
    * @param fVariantName Name of the pin, usually the port name e.g. PTA1
    */
   Pin(DeviceInfo deviceInfo, String name) {
      if ((deviceInfo == null) && !"Unassigned".equals(name)) {
         System.err.print("deviceInfo is null!!");
      }
      fDeviceInfo = deviceInfo;
      fName  = name;
      Pattern p = Pattern.compile("^\\s*PT(.)(\\d*)\\s*$");
      Matcher m = p.matcher(name);
      if (m.matches()) {
         fPortInstance = m.group(1);
         fPortPin      = m.group(2);
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
    * @return Pin name or null if not found in current package
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
    * Get name of the pin with package location
    * 
    * <pre>
    * e.g "PTC6(p51)"
    * </pre>
    * 
    * @return String suitable as trailing comment
    */
   public String getNameWithLocation() {
      String trailingComment = getName();
      String location = getLocation();
      if ((location != null) && !location.isBlank()) {
         trailingComment = trailingComment+"("+location+")";
      }
      return trailingComment;
   }

   @Override
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Pin("+fName+",\n   R:"+fResetMuxValue+",\n   C:");
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
      if (this == UNASSIGNED_PIN) {
         return;
      }
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
    * Sets the reset mapping peripheral signals for the pin
    * 
    * @param resetSignals  Name of signals to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception if pin already has reset pin mapping or value not in available pin mappings
    */
   public void setResetSignals(DeviceInfo factory, final String resetSignals) {
      if (this == UNASSIGNED_PIN) {
         return;
      }
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
   }

   @Override
   public int compareTo(Pin o) {
      return comparator.compare(fName, o.fName);
   }

   /**
    * Get map of signals mappable to this pin indexed by MUX value
    * 
    * @return
    */
   public Map<MuxSelection, MappingInfo> getMappableSignals() {
      return fMappableSignals;
   }

   /**
    * Returns the <b>single</b> signal mapped to this pin.<br>
    * If more than a single mapping is active then null is returned.
    * 
    * @return Mapped signal or null if multiple mapped.
    */
   public Signal getUniqueMappedSignal() {
      ArrayList<MappingInfo> mappedSignals = getMappedSignals();
      if (mappedSignals.size() == 1) {
         ArrayList<Signal> signals = mappedSignals.get(0).getSignals();
         if (signals.size() == 1) {
            return signals.get(0);
         }
      }
      return null;
   }
   
   /**
    * Get user description from associated signals.
    * Cached.
    * 
    * @return
    */
   public String getMappedSignalsUserDescriptions( ) {
      return fMappedSignalsUserDescriptions;
   }
   
   /**
    * Determine user description from associated signals.
    * 
    * @return
    */
   public String determineMappedSignalsUserDescriptions( ) {
      StringBuilder sb = new StringBuilder();
      boolean doSeparator = false;
      for (MuxSelection muxSel:fMappableSignals.keySet()) {
         MappingInfo mappinfo = fMappableSignals.get(muxSel);
         if (mappinfo.isSelected()) {
            String description = mappinfo.getMappedSignalsUserDescriptions();
            if (description.isBlank()) {
               continue;
            }
            if (doSeparator) {
               sb.append("/");
            }
            sb.append(description);
            doSeparator = true;
         }
      }
      return sb.toString();
   }

   /**
    * Get user description (from mapped signals).
    * Cached.
    * 
    * @return
    */
   public String getMappedSignalsCodeIdentifiers() {
      return fMappedSignalsCodeIdentifiers;
   }
   
   /**
    * Determine user description for mapped signals.
    * 
    * @return
    */
   public String determineMappedSignalsCodeIdentifiers() {
      StringBuilder sb = new StringBuilder();
      boolean doSeparator = false;
      for (MuxSelection muxSelection:fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxSelection);
         if (!mappingInfo.isSelected()) {
            continue;
         }
         for (Signal signal:mappingInfo.getSignals()) {
            if ((signal.getCodeIdentifier() == null) || signal.getCodeIdentifier().isBlank()) {
               continue;
            }
            if (doSeparator) {
               sb.append('/');
            }
            sb.append(signal.getCodeIdentifier());
            doSeparator = true;
         }
      }
      return sb.toString();
   }

   /**
    * Get the currently mapped signal(s) for this pin.
    * 
    * @return List of mapped signals (which may be empty)
    */
   public ArrayList<MappingInfo> getMappedSignals() {
      ArrayList<MappingInfo> rv = new ArrayList<MappingInfo>();
      for (MuxSelection muxSelection:fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxSelection);
         if (mappingInfo.isSelected()) {
            rv.add(mappingInfo);
         }
      }
      return rv;
   }

   /**
    * Checks if more than one signal is mapped to this pin
    * 
    * @return Status describing conflict or null if no conflict
    */
   public Status checkMappingConflicted() {
      StringBuilder sb = new StringBuilder();
      int mappingsFound = 0;
      
      sb.append("(");
      for (MuxSelection muxKey : fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxKey);
         if (mappingInfo.isSelected()) {
            if (mappingsFound>0) {
               sb.append(", ");
            }
            sb.append(mappingInfo.getSignalList());
            mappingsFound++;
         }
      }
      if (mappingsFound <= 1) {
         return null;
      }
      sb.append(")");
      return new Status(sb.toString(), Severity.WARNING, "Multiple signals are mapped to pin '" + getName() + "'");
   }
   
   /**
    * Get status of this pin
    * 
    * @return Status 
    */
   public Status getPinStatus() {
      return fStatus;
   }
   
   /**
    * Get status of associated signals
    * 
    * @return
    */
   Status getAssociatedSignalsStatus() {
      Status status = null;
      for (MappingInfo mappingInfo:getMappedSignals()) {
         ArrayList<Signal> signals = mappingInfo.getSignals();
         if (!signals.isEmpty()) {
            status = signals.get(0).getSignalStatus();
            if (status != null) {
               break;
            }
         }
      }
      return status;
   }
   
   /**
    * Get status of this pin and mapped signal
    * 
    * @return Status 
    */
   public Status getStatus() {
      if (fStatus != null) {
         // signal -> multiple pins
         return fStatus;
      }
      fAssociatedStatus = getAssociatedSignalsStatus();
      return fAssociatedStatus;
   }
   
   /**
    * Checks for a selected mux setting
    * 
    * @return Mux setting found or <b>MuxSelection.unassigned</b> if none
    */
   public MappingInfo findMappedSetting() {
      for (MuxSelection muxKey : fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxKey);
         if (mappingInfo.isSelected()) {
            return mappingInfo;
         }
      }
      return MappingInfo.UNASSIGNED_MAPPING;
   }
   
   /** 
    * Set current pin multiplexor setting.
    * 
    * Listening signals are modified 
    * 
    * @param newMuxValue Multiplexor value to set
    */
   public void setMuxSelection(MuxSelection newMuxValue) {
      //      System.err.println("Pin("+fName+")::setMuxSelection("+newMuxValue+")");
      if (this == UNASSIGNED_PIN) {
         return;
      }
      MappingInfo activatedMuxSetting = fMappableSignals.get(newMuxValue);
      
      // Update selection
      boolean changed = false;
      for (MuxSelection muxKey : fMappableSignals.keySet()) {
         MappingInfo mappingInfo = fMappableSignals.get(muxKey);
         if (mappingInfo == activatedMuxSetting) {
            changed = changed || mappingInfo.select(Origin.pin, true);
         }
         else {
            changed = changed || mappingInfo.select(Origin.pin, false);
         }
      }
      notifyListeners();
      setDirty(changed);
   }

   /**
    * Add a signal mapping to this pin
    * 
    * @param signal      Signal to add
    * @param muxValue    Mux selection to select this signal on pin
    * @return
    */
   public MappingInfo addSignal(Signal signal, MuxSelection muxValue) {
      MappingInfo mapInfo = fMappableSignals.get(muxValue);
      if (mapInfo == null) {
         // Create new mapping
         mapInfo = new MappingInfo(this, muxValue);
         fMappableSignals.put(muxValue, mapInfo);
      }
      mapInfo.addSignal(signal);
      signal.addMappedPin(mapInfo);
      if (muxValue == MuxSelection.fixed) {
         fResetMuxValue = MuxSelection.fixed;
         setMuxSelection(MuxSelection.fixed);
      }
      signal.addListener(this);
      return mapInfo;
   }

   private  final String getOldMuxKey() {
      return "$signal$"+fName+"_muxSetting";
   }

   private  final String getMuxKey() {
      return "$pin$"+fName+"_muxSetting";
   }

//   private  final String getOldUserDescriptionKey() {
//      return "$signal$"+fName+"_descriptionSetting";
//   }

   private  final String getPCRKey() {
      return "$signal$"+fName+"_pcrSetting";
   }

   /**
    * Load pin settings from settings object
    * 
    * @param settings Settings object
    */
   public void loadSettings(Settings settings) {
      if (this == UNASSIGNED_PIN) {
         return;
      }
      String oldMuxValue = settings.get(getOldMuxKey());
      if (oldMuxValue != null) {
         MuxSelection muxValue = MuxSelection.valueOf(oldMuxValue);
         setMuxSelection(muxValue);
      }
      for (MuxSelection muxSelection:fMappableSignals.keySet()) {
         String muxValue = settings.get(getMuxKey()+"_" +muxSelection.getShortName());
         if (muxValue != null) {
            fMappableSignals.get(muxSelection).select(Origin.pin, true);
         }
      }
      String pcrValue = settings.get(getPCRKey());
      if (pcrValue != null) {
         setProperties(Long.parseLong(pcrValue, 16));
      }
      /**
       * Migrate old settings
       */
      for (MuxSelection muxSelection:fMappableSignals.keySet()) {
         String muxValue = settings.get(getMuxKey()+"_" +muxSelection.toString());
         if (muxValue != null) {
            fMappableSignals.get(muxSelection).select(Origin.pin, true);
         }
      }
//      Map<MuxSelection, MappingInfo> mappedSignals = getMappableSignals();
//      if (mappedSignals.isEmpty()) {
//         // Only migrate if mapped to a signal
//         return;
//      }
//      final String descriptionValue = settings.get(getOldUserDescriptionKey());
//      if (descriptionValue != null) {
//         mappedSignals.get(MuxSelection.mux0).getSignals().forEach(new Consumer<Signal>() {
//            @Override
//            public void accept(Signal signal) {
//               signal.setUserDescription(descriptionValue);
//            }
//         });
//      }
   }

   /**
    * Save pin settings to settings object
    * 
    * @param settings Settings object
    */
   public void saveSettings(Settings settings) {
      if (this == UNASSIGNED_PIN) {
         return;
      }
//      if ((fMuxValue != MuxSelection.unassigned) && (fMuxValue != MuxSelection.fixed)) {
//         settings.put(getOldMuxKey(fName), fMuxValue.name());
//      }
      for (MuxSelection muxSelection:fMappableSignals.keySet()) {
         if (muxSelection == MuxSelection.fixed) {
            continue;
         }
         if (muxSelection == MuxSelection.unassigned) {
            continue;
         }
         if (fMappableSignals.get(muxSelection).isSelected()) {
            settings.put(getMuxKey()+"_" +muxSelection.getShortName(), "selected");
         }
      }
//      String desc = getUserDescription();
//      if ((desc != null) && !desc.isEmpty()) {
//         settings.put(getUserDescriptionKey(fName), desc);
//      }
//      String ident = getCodeIdentifier();
//      if ((ident != null) && !ident.isEmpty()) {
//         settings.put(getCodeIndentifierKey(fName), ident);
//      }
      if (getProperties() != 0) {
         settings.put(getPCRKey(), Long.toHexString(getProperties()));
      }
   }

   /**
    * Set editor dirty via deviceInfo
    */
   void setDirty(boolean dirty) {
      if (fDeviceInfo != null) {
         fDeviceInfo.setDirty(dirty);
      }
   }
   
   @Override
   public void modelElementChanged(ObservableModel model) {
      //      System.err.println("Pin["+fName+"].modelElementChanged("+model+")");
      boolean changed = false;
      if (model instanceof MappingInfo) {
         changed = true;
         fStatus = checkMappingConflicted();
      }
      if (model instanceof Signal) {
         Status status = getAssociatedSignalsStatus();
         if (fAssociatedStatus != status) {
            fAssociatedStatus = status;
            changed = true;
         }
         String mappedSignalsCodeIdentifiers = determineMappedSignalsCodeIdentifiers();
         if (fMappedSignalsCodeIdentifiers != mappedSignalsCodeIdentifiers) {
            fMappedSignalsCodeIdentifiers = mappedSignalsCodeIdentifiers;
            changed = true;
         }
         String mappedSignalsUserDescriptions = determineMappedSignalsUserDescriptions();
         if (fMappedSignalsUserDescriptions != mappedSignalsUserDescriptions) {
            fMappedSignalsUserDescriptions = mappedSignalsUserDescriptions;
            changed = true;
         }
      }
      if (changed) {
         notifyListeners();
      }
      //      System.err.println("Pin("+fName+").modelElementChanged("+fMuxValue+") - Changed");
   }

   public void setPort(Signal signal) throws Exception {
      if (this == UNASSIGNED_PIN) {
         return;
      }
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
       * @param fValue Value to map
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
      if (this == UNASSIGNED_PIN) {
         return;
      }
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
       * @param fValue Value to map
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
      if (this == UNASSIGNED_PIN) {
         return;
      }
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
   // This is a dummy mask used internally
//   public final static long PORT_POLARITY_SHIFT  =  30;                            
//   public final static long PORT_POLARITY_MASK   =  (0x01L << PORT_POLARITY_SHIFT); 
   public final static long PCR_MASK        =  
         PORT_PCR_PULL_MASK|PORT_PCR_SRE_MASK|PORT_PCR_PFE_MASK|PORT_PCR_ODE_MASK|
         PORT_PCR_DSE_MASK|PORT_PCR_LK_MASK|PORT_PCR_IRQC_MASK; 
   public final static long PROPERTIES_MASK        = PCR_MASK; 

   /**
    * Get PCR value (excluding MUX)
    * 
    * @return PCR value (excluding MUX)
    */
   public long getProperties() {
      return fProperties & PROPERTIES_MASK;
   }

   /**
    * Set PCR value (excluding MUX)
    * 
    * @param properties PCR value (excluding MUX)
    */
   public boolean setProperties(long properties) {
      if (this == UNASSIGNED_PIN) {
         return false;
      }
      properties &= PROPERTIES_MASK;
      if (fProperties == properties) {
         return false;
      }
      fProperties = properties;
      setDirty(true);
      return true;
   }

   /**
    * Get property (field from getProperties())
    * 
    * @param mask    Mask to extract field 
    * @param offset  Offset to shift after extraction
    * 
    * @return Extracted field from property
    */
   public long getProperty(long mask, long offset) {
      return (getProperties()&mask)>>offset;
   }

   public boolean setProperty(long mask, long offset, long property) {
      if (this == UNASSIGNED_PIN) {
         return false;
      }
      return setProperties((getProperties()&~mask)|((property<<offset)&mask));
   }

   @Override
   public void modelStructureChanged(ObservableModel model) {
      if (this == UNASSIGNED_PIN) {
         return;
      }
      notifyStructureChangeListeners();
   }

   @Override
   public void elementStatusChanged(ObservableModel model) {
      if (this == UNASSIGNED_PIN) {
         return;
      }
      notifyStatusListeners();
   }

   /**
    * Get PCR value for pin combined with mux value given
    * 
    * @param muxValue Mux value to combine with PCR values for pin
    * 
    * @return PCR value
    */
   public long getPcrValue(MuxSelection muxValue) {
      return (getProperties()&PCR_MASK) | ((muxValue.value<<PORT_PCR_MUX_SHIFT)&PORT_PCR_MUX_MASK);
   }

//   public String getPcrValueAsString() {
//      long pcrValue = getProperties() | ((getMuxValue().value<<PORT_PCR_MUX_SHIFT)&PORT_PCR_MUX_MASK);
//      return getPcrValueAsString(pcrValue);
//   }
//
//   public static String getPcrValueAsString(long pcrValue) {
//      StringBuilder sb = new StringBuilder();
//      sb.append("PORT_PCR_MUX(" +((pcrValue & PORT_PCR_MUX_MASK) >> PORT_PCR_MUX_SHIFT)+")|");
//      sb.append("PORT_PCR_DSE(" +((pcrValue & PORT_PCR_DSE_MASK) >> PORT_PCR_DSE_SHIFT)+")|");
//      sb.append("PORT_PCR_IRQC("+((pcrValue & PORT_PCR_IRQC_MASK)>> PORT_PCR_IRQC_SHIFT)+")|");
//      sb.append("PORT_PCR_ISF(" +((pcrValue & PORT_PCR_ISF_MASK) >> PORT_PCR_ISF_SHIFT)+")|");
//      sb.append("PORT_PCR_LK("  +((pcrValue & PORT_PCR_LK_MASK) >> PORT_PCR_LK_SHIFT)+")|");
//      sb.append("PORT_PCR_ODE(" +((pcrValue & PORT_PCR_ODE_MASK) >> PORT_PCR_ODE_SHIFT)+")|");
//      sb.append("PORT_PCR_PFE(" +((pcrValue & PORT_PCR_PFE_MASK) >> PORT_PCR_PFE_SHIFT)+")|");
//      sb.append("PORT_PCR_SRE(" +((pcrValue & PORT_PCR_SRE_MASK) >> PORT_PCR_SRE_SHIFT)+")|");
//      sb.append("PORT_PCR_PE("  +((pcrValue & PORT_PCR_PULL_MASK) >> PORT_PCR_MUX_SHIFT)+")|");
//      sb.append("PORT_PCR_PS("  +((pcrValue & PORT_PCR_PULL_MASK) >> PORT_PCR_MUX_SHIFT)+")");
//
//      return sb.toString();
//   }
}
