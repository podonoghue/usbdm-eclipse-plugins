package net.sourceforge.usbdm.configEditor.information;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Information about a pin<br>
 * <li>Pin name
 * <li>Peripheral functions mapped to that pin
 */
public class PinInformation implements Comparable<PinInformation>{
   /**
    * Pin used to denote a disabled mapping
    */
   public final static PinInformation DISABLED_PIN = new PinInformation("Disabled");
   
   /**
    * Comparator for port names e.g. PTA13 c.f. PTB12
    * Treats the number separately as a number.
    */
   public static Comparator<String> comparator = new Comparator<String>() {
      @Override
      public int compare(String arg0, String arg1) {
         if ((arg0.length()==0) && (arg1.length()==0)) {
            return 0;
         }
         if (arg0.length()==0) {
            return -1;
         }
         if (arg1.length()==0) {
            return 1;
         }
         Pattern p = Pattern.compile("([^\\d]*)(\\d*)(.*)");
         Matcher m0 = p.matcher(arg0);
         Matcher m1 = p.matcher(arg1);
         if (m0.matches() && m1.matches()) {
            String t0 = m0.group(1);
            String t1 = m1.group(1);
            int r = t0.compareTo(t1);
            if (r == 0) {
               // Treat as numbers
               String n0 = m0.group(2);
               String n1 = m1.group(2);
               int no0 = -1, no1 = -1;
               if (n0.length() > 0) {
                  no0 = Integer.parseInt(n0);
               }
               if (n1.length() > 0) {
                  no1 = Integer.parseInt(n1);
               }
               r = -no1 + no0;

               if (r == 0) {
                  String s0 = m0.group(3);
                  String s1 = m1.group(3);
                  r = compare(s0, s1);
               }
            }
            return r;
         }
         return arg0.compareTo(arg1);
      }
   };

   /** Port instance e.g. PTA3 => A */
   private String fPortInstance = null;

   /** Pin instance e.g. PTA3 => 3 */
   private String fPortPin = null;
   
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
   
   /** Name of the pin, usually the port name e.g. PTA1 */
   private String fName;
   
   /** Description of peripheral functions that may be mapped to this pin */
   private StringBuilder  fDescription = new StringBuilder();

   /**
    * Peripheral functions associated with this pin<br> 
    */
   private  ArrayList<PeripheralFunction> fMappedPins = new ArrayList<PeripheralFunction>();

   /**
    * Peripheral functions associated with this pin arranged by function base name<br> 
    * e.g. multiple FTMs may be associated with a pin 
    */
   private HashMap<String, ArrayList<MappingInfo>> fMappedPinsByFunction = new HashMap<String, ArrayList<MappingInfo>>();

   /**
    * Reset pin
    */
   private MuxSelection fResetFunction = null;

   /**
    * Default functions on this pin
    */
   private MuxSelection fDefaultFunction = null;
   
   /**
    * Create empty pin function for given pin
    * 
    * @param fName Name of the pin, usually the port name e.g. PTA1
    */
   PinInformation(String name) {
      this.fName  = name;
      Pattern p = Pattern.compile("^\\s*PT(.)(\\d*)\\s*$");
      Matcher m = p.matcher(name);
      if (m.matches()) {
         fPortInstance = m.group(1);
         fPortPin      = m.group(2);
      }
      fResetFunction = null;
   }
   
   /**
    * Get pin name
    * 
    * @return
    */
   public String getName() {
      return fName;
   }
   
   /**
    * Get description of functions mapped to this pin
    * 
    * @return Description
    */
   public String getDescription() {
      if (fDescription.length() == 0) {
         return fName;
      }
      return fName + " = " + fDescription.toString();
   }
   
   /**
    * Gets list of peripheral functions mapped to this pin
    * 
    * @param   fBaseName
    * 
    * @return  List (may be empty, never null)
    */
   public ArrayList<PeripheralFunction> getMappedPeripheralFunctions() {
      return fMappedPins;
   }
   
   /**
    * Gets sub-list of peripheral functions mapped to this pin
    * 
    * @param   baseName
    * 
    * @return  List (may be empty, never null)
    */
   public ArrayList<MappingInfo> createMappingList(String baseName) {
      ArrayList<MappingInfo> list = fMappedPinsByFunction.get(baseName);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
         fMappedPinsByFunction.put(baseName, list);
      }
      return list;
   }
   
//   /**
//    * Adds a mapping of a peripheral function to the pin
//    * 
//    * @param mappingInfo        Mapping to add
//    * 
//    * @throws Exception 
//    */
//   public void addPeripheralFunctionMapping(MappingInfo mappingInfo) {
//      ArrayList<PeripheralFunction> peripheralFunction = mappingInfo.functions;
//      ArrayList<MappingInfo> elements = createMappingList(peripheralFunction.fPeripheral.fBaseName);
//      elements.add(mappingInfo);
//      mappedPins.add(peripheralFunction);
//      if (fDescription.length() > 0) {
//         fDescription.append(",");
//      }
//      fDescription.append(peripheralFunction.getName());
//      fPinNames = null;
//   }

   /**
    * Gets sub-list of peripheral functions mapped to this pin that have this basename
    * 
    * @param   baseName
    * 
    * @return  List (never null)
    */
   public ArrayList<MappingInfo> getMappingList(String baseName) {
      ArrayList<MappingInfo> list = fMappedPinsByFunction.get(baseName);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
      }
      return list;
   }
   
//   /**
//    * Get index of pin that this peripheral function is mapped to
//    * 
//    * @return index of function if mapped, -1 if not mapped to this pin
//    */
//   public int getMappedPinIndex(PeripheralFunction peripheralFunction) {
//      for (int index=0; index<7; index++) {
//         if (mappedPins[index] == peripheralFunction) {
//            return index;
//         }
//      }
//      return -1;
//   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   public String toString() {
      return getDescription();
   }

   /**
    * Sets the reset pin mapping
    * 
    * @param mux reset peripheral function on this pin
    */
   public void setResetValue(MuxSelection mux) {
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
    */
   public void setDefaultValue(MuxSelection mux) {
      fDefaultFunction = mux;
   }

   /**
    * Returns the default pin mapping
    * 
    * @return default peripheral function on this pin
    */
   public MuxSelection getDefaultValue() {
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
      if (getDefaultValue() != null) {
         throw new RuntimeException("Pin "+getName()+" already has default value "+getDefaultValue());
      }
      Map<MuxSelection, MappingInfo> functionMappings = factory.getFunctions(this);
      if (functionMappings != null) {
         functionMappings.forEach(new BiConsumer<MuxSelection, MappingInfo>() {
            @Override
            public void accept(MuxSelection muxSelection, MappingInfo mappingInfo) {
               if (mappingInfo.getFunctionList().equalsIgnoreCase(defaultPeripheralName) && (mappingInfo.mux != MuxSelection.reset)) {
                  setDefaultValue(mappingInfo.mux);
               }
            }
         } );
      }
      if (getDefaultValue() == null) {
         throw new RuntimeException("Peripheral "+defaultPeripheralName+" not found as option for pin " + getName());
      }
   }

   /**
    * Sets the default peripheral functions for the pin
    * 
    * @param defaultPeripheralName  Name of peripherals to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception If pin already has default or new default not found as available pin mapping
    */
   public void setDefaultPeripheralFunctions(MuxSelection sel) {
      if (getDefaultValue() != null) {
         throw new RuntimeException("Pin "+getName()+" already has default value "+getDefaultValue());
      }
      setDefaultValue(sel);
   }
   
   /**
    * Sets the reset peripheral functions for the pin
    * 
    * @param resetPeripheralName  Name of peripherals to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception If pin already has default or new default not found as available pin mapping
    */
   public void setResetPeripheralFunctions(DeviceInfo factory, final String resetPeripheralName) throws Exception {
      if (getResetValue() != null) {
         throw new RuntimeException("Pin "+getName()+" already has reset value "+getDefaultValue());
      }
      Map<MuxSelection, MappingInfo> functionMappings = factory.getFunctions(this);
      functionMappings.forEach(new BiConsumer<MuxSelection, MappingInfo>() {
         @Override
         public void accept(MuxSelection muxSelection, MappingInfo mappingInfo) {
           if (mappingInfo.getFunctionList().equalsIgnoreCase(resetPeripheralName)) {
              setResetValue(mappingInfo.mux);
           }
         }
      } );
      if (getResetValue() == null) {
         throw new Exception("Peripheral "+resetPeripheralName+" not found as option for pin " + getName());
      }
   }

   /**
    * Sets the reset peripheral functions for the pin
    * 
    * @param resetPeripheralName  Name of peripherals to look for e.g. <b><i>GPIOE_1/LLWU_P0</i></b>
    * 
    * @throws Exception If pin already has default or new default not found as available pin mapping
    */
   public void setResetPeripheralFunctions(MuxSelection sel) {
      if (getResetValue() != null) {
         throw new RuntimeException("Pin "+getName()+" already has reset value "+getResetValue());
      }
      setResetValue(sel);
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


}
