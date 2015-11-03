import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes a peripheral function that may be mapped to a pin<br>
 * Includes:
 * <li>peripheral
 * <li>signal
 */
class PeripheralFunction {
   /**
    * Map of all peripheral functions created<br>
    * May be searched by key derived from function name
    */
   private static HashMap<String, PeripheralFunction> functions = new HashMap<String, PeripheralFunction>();
   
   /**
    * Map of peripheral functions associated with a baseName<br>
    * May be searched by baseName string
    */
   private static HashMap<String, HashMap<String, PeripheralFunction>> functionsByBaseName = new HashMap<String, HashMap<String, PeripheralFunction>>();

   public static final PeripheralFunction DISABLED = new PeripheralFunction("Disabled", "", "", "");
   
   /**
    * Comparator for port names e.g. PTA13 c.f. PTB12
    * Treats the number separately as a number.
    */
   private static final Comparator<String> portNameComparator = new Comparator<String>() {
      @Override
      public int compare(String arg0, String arg1) {
         Pattern p = Pattern.compile("([^\\d]*)(\\d*)(.*)");
         Matcher m0 = p.matcher(arg0);
         Matcher m1 = p.matcher(arg1);
         if (m0.matches() && m1.matches()) {
            String t0 = m0.group(1);
            String n0 = m0.group(2);
            String s0 = m0.group(3);
            String t1 = m1.group(1);
            String n1 = m1.group(2);
            String s1 = m1.group(3);
            int r = t0.compareTo(t1);
            if (r == 0) {
               int no0 = -1, no1 = -1;
               if (n0.length() > 0) {
                  no0 = Integer.parseInt(n0);
               }
               if (n1.length() > 0) {
                  no1 = Integer.parseInt(n1);
               }
               r = -no1 + no0;
            }
            if (r == 0) {
               Pattern pp = Pattern.compile("([^\\d]*)(\\d*)(.*)");
               Matcher mm0 = pp.matcher(s0);
               Matcher mm1 = pp.matcher(s1);
               if (mm0.matches() && mm1.matches()) {
                  String tt0 = mm0.group(1);
                  String nn0 = mm0.group(2);
                  String tt1 = mm1.group(1);
                  String nn1 = mm1.group(2);
                  r = tt0.compareTo(tt1);
                  if (r == 0) {
                     int no0 = -1, no1 = -1;
                     if (nn0.length() > 0) {
                        no0 = Integer.parseInt(nn0);
                     }
                     if (nn1.length() > 0) {
                        no1 = Integer.parseInt(nn1);
                     }
                     r = -no1 + no0;
                  }
               }
               else {
                  r = s0.compareTo(s1);
               }
            }
            return r;
         }
         return arg0.compareTo(arg1);
      }
   };

   /**
    * Reset shared internal state
    */
   public static void reset() {
      functions            = new HashMap<String, PeripheralFunction>();
      functionsByBaseName  = new HashMap<String, HashMap<String, PeripheralFunction>>();
      peripheralFunctions  = null;
   }

   /** Map of pins that this peripheral function may be mapped to */
//   private ArrayList<PinInformation> mappablePins = new ArrayList<PinInformation>();
   
//   private int fPreferredPinIndex = 0;

   /** Peripheral that signal belongs to */
   public Peripheral fPeripheral;
   
   /** Peripheral signal name number e.g. FTM0_CH6 = 6, PTA3 = 3, SPI0_SCK = SCK */
   public   String fSignal;

   /** Name of peripheral function e.g. FTM0_CH3 */
   private String fName;

   private boolean fIncluded;     

   /**
    * Get map of all peripheral functions
    * 
    * @return map 
    */
   public static HashMap<String, PeripheralFunction> getFunctions() {
      return functions;
   }

   static ArrayList<String> peripheralFunctions = null;
   
   /**
    * Get list of all peripheral functions sorted by name
    * 
    * @return list
    */
   public static ArrayList<String> getPeripheralFunctionsAsList() {
      if (peripheralFunctions == null) {
         peripheralFunctions = new ArrayList<String>(PeripheralFunction.getFunctions().keySet());
         Collections.sort(peripheralFunctions, portNameComparator);
      }
      return peripheralFunctions;
   }
   
   /**
    * Get map of peripheral functions associated with the given baseName<br>
    * e.g. "FTM" with return all the FTM peripheral functions
    * 
    * @param baseName Base name to search for e.g. FTM, ADC etc
    * 
    * @return  Map or null if none exists for baseName
    */
   public static HashMap<String, PeripheralFunction> getFunctionsByBaseName(String baseName) {
      return functionsByBaseName.get(baseName);
   }

   /**
    * Get peripheral function from name 
    * 
    * @param name e.g. FTM0_CH6, PTA3
    * 
    * @return function if found or null if not present
    * @throws Exception 
    */
   public static PeripheralFunction find(String name) throws Exception {
      return findPeripheralFunction(name, false);
   }
   
   /**
    * Get peripheral function from key 
    * 
    * @param name e.g. FTM0_CH6, PTA3
    * 
    * @return function if found or null if not present
    * @throws Exception 
    */
   public static PeripheralFunction lookup(String name) throws Exception {
      return functions.get(name);
   }
   
   /**
    * Create peripheral function from components<br>
    * e.g. FTM0_CH6 = <b>new</b> PeripheralFunction(FTM, 0, 6)
    * 
    * @param baseName   Base name of the peripheral e.g. FTM0_CH6 = FTM, PTA3 = PT
    * @param fInstance   Number/name of the peripheral e.g. FTM0_CH6 = 0, PTA3 = A
    * @param signal     Channel/pin number/operation e.g. FTM0_CH6 = 6, PTA3 = 3, SPI0_SCK = SCK
    * @throws Exception 
    */
   private PeripheralFunction(String name, String baseName, String instance, String signal) {
//      System.err.println(String.format("PeripheralFunction(b=%s, n=%s, ch=%s)", baseName, instance, signal));
      fName       = name;
      fPeripheral = Peripheral.addPeripheral(baseName, instance);
      fSignal     = signal;

//      mappablePins.add(PinInformation.DISABLED_PIN);
      
      // Add to basename map
      HashMap<String, PeripheralFunction> map = functionsByBaseName.get(baseName);
      if (map == null) {
         map = new HashMap<String, PeripheralFunction>();
         functionsByBaseName.put(baseName, map);
      }
      map.put(baseName, this);
//      System.err.println(getName());
   }
   
   /**
    * Create peripheral function from a peripheral name<br>
    * e.g. "FTM0_CH6" = <i>PeripheralFunction</i>(FTM, 0, 6)
    * 
    * @param function   Name of peripheral function to process e.g. FTM0_CH6
    *                      
    * @return Created function if matches an expected pattern and is not marked as useful
    * 
    * @throws Exception if function does fit expected form
    * 
    * @note If the function already exists then the previous instance is returned.
    */
   public static PeripheralFunction createPeripheralFunction(String function) throws Exception {
      return findPeripheralFunction(function, true);
   }
   
   /**
    * Find peripheral function from a peripheral name<br>
    * e.g. "FTM0_CH6" = <i>PeripheralFunction</i>(FTM, 0, 6)
    * 
    * @param function   Name of peripheral function to process e.g. FTM0_CH6
    *                      
    * @return Function if found, null otherwise
    * 
    * @throws Exception if function does fit expected form
    */
   public static PeripheralFunction findPeripheralFunction(String function) throws Exception {
      return findPeripheralFunction(function, false);
   }
   
   /**
    * Find or Create peripheral function<br>
    * e.g. findPeripheralFunction("FTM0_CH6") = <i>PeripheralFunction</i>(FTM, 0, 6)
    * 
    * @param name Name of peripheral function to process e.g. FTM0_CH6
    * @param create       If true then the peripheral function will be created if it does not already exist
    *                      
    * @return Created function if found
    * 
    * @throws Exception if function does fit expected form
    */
   private static PeripheralFunction findPeripheralFunction(String name, boolean create) throws Exception {
      final class PinDescription {
         Pattern pattern;
         Boolean include;
         @SuppressWarnings("unused")
         PinDescription(String regex) {
            this.pattern = Pattern.compile(regex);
            this.include = false;
         }
         PinDescription(String regex, boolean include) {
            this.pattern = Pattern.compile(regex);
            this.include = include;
         }
      }
      final PinDescription[] pinNamePatterns = {
            new PinDescription("^\\s*(Disabled)()()\\s*$", true),
            new PinDescription("^\\s*(PT)([A-Z])(\\d+)\\s*$", true),
            new PinDescription("^\\s*(GPIO)([A-Z])_(\\d+)\\s*$", true),
            new PinDescription("^\\s*(ADC)(\\d+)_(?:SE|DM|DP)(\\d+)[ab]?\\s*$", true),
            new PinDescription("^\\s*(FTM)(\\d+)_CH(\\d+)\\s*$", true),
            new PinDescription("^\\s*(TPM)(\\d+)_CH(\\d+)\\s*$", true),
            new PinDescription("^\\s*(TPM)(\\d+)_(CLKIN\\d+)\\s*$", true),
            new PinDescription("^\\s*(SDHC)(\\d+)_((CLKIN)|(D\\d)|(CMD)|(DCLK))\\s*$", true),
            new PinDescription("^\\s*(SPI)(\\d+)_(SOUT|SIN|SCK|(PCS\\d+)|MOSI|MISO|SS_B)\\s*$", true),
            new PinDescription("^\\s*(I2C)(\\d+)_((SDA)|(SCL|4WSCLOUT|4WSDAOUT))\\s*$", true),
            new PinDescription("^\\s*(I2S)(\\d+)_(TX_BCLK|TXD[0-1]|RXD[0-1]|TX_FS|RX_BCLK|MCLK|RX_FS|TXD1)\\s*$", true),
            new PinDescription("^\\s*(LPTMR)(\\d+)_ALT(\\d+)\\s*$", true),
            
            new PinDescription("^\\s*(TSI)(\\d+)_CH(\\d+)\\s*$", true),
            new PinDescription("^\\s*(UART)(\\d+)_(CTS_b|RTS_b|COL_b|RX|TX)\\s*$", true),
            new PinDescription("^\\s*(LPUART)(\\d+)_(CTS_b|RTS_b|COL_b|RX|TX)\\s*$", true),
            new PinDescription("^\\s*(A?CMP)(\\d+)_((IN\\d*)|(OUT\\d*))\\s*$", true),
            new PinDescription("^\\s*(JTAG)()_(TCLK|TDI|TDO|TMS|TRST_b)\\s*$", true),
            new PinDescription("^\\s*(SWD)()_(CLK|DIO|IO)\\s*$", true),
            new PinDescription("^\\s*(EZP)()_(CLK|DI|DO|CS_b)\\s*$", true),
            new PinDescription("^\\s*(TRACE)()_(SWO)\\s*$", true),
            new PinDescription("^\\s*(LLWU)()_P(\\d+)\\s*$", true),
            new PinDescription("^\\s*(NMI)()_b()\\s*$", true),
            new PinDescription("^\\s*(USB)(\\d*)_(CLKIN|SOF_OUT)\\s*$", true),
            new PinDescription("^\\s*(FTM)(\\d+)_(QD_PHA|QD_PHB|FLT2|CLKIN[0-1]|FLT[0-9])\\s*$", true),
            new PinDescription("^\\s*(E?XTAL)(\\d+)()\\s*$", true),
            new PinDescription("^\\s*(EWM)()_(IN|OUT_b|OUT)\\s*$", true),
            new PinDescription("^\\s*(PDB)(\\d+)_(EXTRG)\\s*$", true),
            new PinDescription("^\\s*(CMT)(\\d*)_(IRO)\\s*$", true),
            new PinDescription("^\\s*(RTC)(\\d*)_(CLKOUT|CLKIN)\\s*$", true),
            new PinDescription("^\\s*(DAC)(\\d+)_(OUT)\\s*$", true),
            new PinDescription("^\\s*(VREF)(\\d+)_(OUT)\\s*$", true),
            new PinDescription("^\\s*(CLKOUT)()()\\s*$", true),
            new PinDescription("^\\s*(TRACE)()_(CLKOUT|D[0-3])\\s*$", true),
            new PinDescription("^\\s*(CLKOUT32K)()()\\s*$", true),
            new PinDescription("^\\s*(R?MII)(\\d+)_(RXCLK|RXER|RXD[0-4]|CRS_DV|RXDV|TXEN|TXD[0-4]|TXCLK|CRS|TXER|COL|MDIO|MDC)\\s*$", true),
            new PinDescription("^\\s*(CAN)(\\d+)_(TX|RX)\\s*$", true),
            new PinDescription("^\\s*(FB)()_((AD?(\\d+))|OE_b|RW_b|CS[0-5]_b|TSIZ[0-1]|BE\\d+_\\d+_BLS\\d+_\\d+_b|TBST_b|TA_b|ALE|TS_b)\\s*$", true),
            new PinDescription("^\\s*(ENET)(\\d+)_(1588_TMR[0-3]|CLKIN|1588_CLKIN)\\s*$", true),
            new PinDescription("^\\s*(KBI)(\\d+)_(P\\d+)\\s*$", true),
            new PinDescription("^\\s*(IRQ)()()\\s*$", true),
            new PinDescription("^\\s*(RESET_[b|B])()()\\s*$", true),
            new PinDescription("^\\s*(BUSOUT)()()\\s*$", true),
            new PinDescription("^\\s*(RTCCLKOUT)()()\\s*$", true),
            new PinDescription("^\\s*(AFE)()_(CLK)\\s*$", true),
            new PinDescription("^\\s*(NMI_B)()()\\s*$", true),
            new PinDescription("^\\s*(EXTRG)()_(IN)\\s*$", true),
            new PinDescription("^\\s*(CMP)(\\d)(OUT|P[0-9])\\s*$", true),
            new PinDescription("^\\s*(TCLK)(\\d+)()\\s*$", true),
            new PinDescription("^\\s*(PWT)()_(IN\\d+)\\s*$", true),
            new PinDescription("^\\s*(LCD)()_(P\\d+)\\s*$", true),
            new PinDescription("^\\s*(LCD)()(\\d+)\\s*$", true),
            new PinDescription("^\\s*(QT)(\\d+)()\\s*$", true),
            new PinDescription("^\\s*(audioUSB)()_(SOF_OUT)\\s*$", true),
            new PinDescription("^\\s*(PXBAR)()_((IN\\d+)|(OUT\\d+))\\s*$", true),
            new PinDescription("^\\s*(SCI)(\\d+)_(RTS|CTS|TxD|RxD)\\s*$", true),
            new PinDescription("^\\s*(LGPIOI)()_(M\\d+)\\s*$", true),
            new PinDescription("^\\s*(SDA)()((DM|DP)[0-3])\\s*$", true),
      };
      
      PeripheralFunction peripheralFunction = null;
      if (name.equalsIgnoreCase("Disabled")) {
         return DISABLED;
      }
      boolean patternMatched = false;
      for (PinDescription pinNamePattern:pinNamePatterns) {
         Matcher matcher = pinNamePattern.pattern.matcher(name);
         if (!matcher.matches()) {
            continue;
         }
         patternMatched = true;
         peripheralFunction = findPeripheralFunction(name, matcher.group(1), matcher.group(2), matcher.group(3), true);
         if (peripheralFunction != null) {
            peripheralFunction.setIncluded(pinNamePattern.include);
         }
      }
      if (!patternMatched) {
         throw new Exception("Failed to find pattern that matched peripheral function: \'" + name + "\'");
      }
      return peripheralFunction;
   }
   
   private void setIncluded(boolean include) {
      this.fIncluded = include;
   }

   public boolean isIncluded() {
      return fIncluded;
   }

   /**
    * Find or Create peripheral function
    * e.g. findPeripheralFunction(FTM,0,6) = <i>PeripheralFunction</i>(FTM, 0, 6)
    * 
    * @param baseName      e.g. FTM0_CH6 = FTM
    * @param instanceNum   e.g. FTM0_CH6 = 0
    * @param signalName    e.g. FTM0_CH6 = 6
    * @param create        If true then the peripheral function will be created if it does not already exist
    *                      
    * @return Peripheral function if found or created, null otherwise
    * @throws Exception 
    */
   private static PeripheralFunction findPeripheralFunction(String name, String baseName, String instanceNum, String signalName, boolean create) throws Exception {
      
      PeripheralFunction peripheralFunction = functions.get(name);
      if ((peripheralFunction == null) && create) {
         peripheralFunction = new PeripheralFunction(name, baseName, instanceNum, signalName);
         functions.put(name, peripheralFunction);
         HashMap<String, PeripheralFunction> sorted = functionsByBaseName.get(baseName);
         if (sorted == null) {
            sorted = new HashMap<String, PeripheralFunction>();
         }
         sorted.put(name, peripheralFunction);
         Peripheral.addPeripheral(baseName+instanceNum);
      }
//      System.err.println(peripheralFunction.toString());
      return peripheralFunction;
   }

   /**
    * Create descriptive name<br>
    * e.g. MappingInfo(FTM, 0, 6) = FTM0_6
    * 
    * @return name created
    */
   public String getName() {
      return fName;
//      return fBaseName+fInstance+"_"+fSignal;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   public String toString() {
      return getName();
   }

   /**
    * A string listing all peripheral functions
    *  
    * @return
    */
   public static String listFunctions() {
      StringBuffer buff = new StringBuffer();
      buff.append("(");
      for (String f:functions.keySet()) {
         buff.append(f+",");
      }
      buff.append(")");
      return buff.toString();
   }
 
 }
