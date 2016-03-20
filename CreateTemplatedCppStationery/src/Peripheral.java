import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

//   /**
//    * Add peripheral
//    * 
//    * @param name Name of peripheral e.g. FTM0
//    * 
//    * @return Peripheral
//    * 
//    * @throws Exception
//    */
//   public static Peripheral addPeripheral(String name) throws Exception {
////      System.err.println(String.format("addPeripheral(%s)", name));
//      Peripheral p = map.get(name);
//      if (p == null) {
//         p = new Peripheral(name);
//         map.put(name, p);
//      }
//      return p;
//   }

   /**
    * Add peripheral
    * 
    * @param baseName
    * @param signal
    * 
    * @return
    */
   public static Peripheral addPeripheral(String baseName, String signal) {
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
    * @param fName
    */
   private Peripheral(String baseName, String signal) {
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
