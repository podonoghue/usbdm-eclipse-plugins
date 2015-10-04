import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Peripheral {
   
   /**
    * Map of all Peripherals
    */
   private static HashMap<String, Peripheral> map = new HashMap<String, Peripheral>(); 
         
   /**
    * Reset internal state
    */
   public static void reset() {
      map = new HashMap<String, Peripheral>(); 
   }
   
   /**
    * Get list of all peripheral names
    * 
    * @return Sorted list
    */
   public static ArrayList<String> getList() {
      ArrayList<String> ar = new ArrayList<String>(map.keySet());
      Collections.sort(ar);
      return ar;
   }

   /**
    * Add peripheral
    * 
    * @param name Name of peripheral e.g. FTM0
    * 
    * @return Peripheral
    * 
    * @throws Exception
    */
   public static Peripheral addPeripheral(String name) throws Exception {
//      System.err.println(String.format("addPeripheral(%s)", name));
      Peripheral p = map.get(name);
      if (p == null) {
         p = new Peripheral(name);
         map.put(name, p);
      }
      return p;
   }

   /**
    * Add peripheral
    * 
    * @param baseName2
    * @param baseName3
    * @return
    * @throws Exception 
    */
   public static Peripheral addPeripheral(String baseName, String signal) throws Exception {
//      System.err.println(String.format("addPeripheral(%s)", baseName+signal));
      String name = baseName+signal;
      Peripheral p = map.get(name);
      if (p == null) {
         p = new Peripheral(baseName, signal);
         map.put(name, p);
      }
      return p;
   }

   /**
    * 
    * @param name
    * @return
    * @throws Exception
    */
   public static Peripheral getPeripheral(String name) throws Exception {
      Peripheral p = map.get(name);
//      if (p == null) {
//         throw new Exception("Failed to find Peripheral \'" + name + "\'");
//      }
      return p;
   }
   
   /**
    * List of patterns for peripherals
    */
   private static final Pattern patterns[] = {
         Pattern.compile("^\\s*(PORT)([A-Z])\\s*$"),
         Pattern.compile("^\\s*(I2C)(\\d*)\\s*$"),
         Pattern.compile("^\\s*(I2S)(\\d*)\\s*$"),
         Pattern.compile("^\\s*([^\\d]*)(\\d*)\\s*$")
   };

   /** Base name of the peripheral e.g. FTM0_CH6 = FTM, PTA3 = PT */
   String fBaseName;
   
   /** Instance name/number of the peripheral instance e.g. FTM0_CH6 = 0, PTA3 = A */
   String fInstance;
   
   /** Name e.g. FTM0 => FTM0 */
   String fName;

   /** Clock register e.g. SIM->SCGC6 */
   String fClockReg = null;

   /** Clock register mask e.g. ADC0_CLOCK_REG */
   String fClockMask = null;

   /**
    * Create peripheral
    * 
    * @param name
    * @throws Exception
    */
   private Peripheral(String name) throws Exception {
//      System.err.println(String.format("Peripheral(%s)", name));
      Matcher matcher = null;
      for (Pattern pattern:patterns) {
         matcher = pattern.matcher(name);
         if (matcher.matches()) {
            break;
         }
         matcher = null;
      }
      if (matcher == null) {
         throw new Exception("Failed to match Peripheral name \'" + name + "\'");
      }
      this.fName = name;
      fBaseName  = matcher.group(1);
      fInstance  = matcher.group(2);
   }

   /**
    * Create peripheral
    * 
    * @param fName
    * @throws Exception
    */
   private Peripheral(String baseName, String signal) throws Exception {
      this.fName      = baseName+signal;
      this.fBaseName  = baseName;
      this.fInstance  = signal;
   }
   
   public void setClockInfo(String clockReg, String clockMask) {
      fClockMask = clockMask;
      fClockReg   = clockReg;
   }
   
   @Override
   public String toString() {
      return fName;
   }

}
