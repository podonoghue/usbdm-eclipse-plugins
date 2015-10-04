import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Describes how a peripheral function is mapped to a pin<br>
 */
public class MappingInfo {
   
   /** Peripheral function that is mapped */
   public PeripheralFunction function;
   
   /** Peripheral function that is mapped */
   public PinInformation pin;
   
   /** Pin multiplexor setting to map this function on the pin */
   public int mux;

   /** List of mux values for which this peripheral should be enabled */
   boolean[] muxValues;

   /**
    * Map from Pin to list of Functions
    */
   private static HashMap<PinInformation, ArrayList<MappingInfo>> pinMap      = new HashMap<PinInformation, ArrayList<MappingInfo>>();
   
   /**
    * Map from Function to list of Pins
    */
   private static HashMap<PeripheralFunction, ArrayList<MappingInfo>> functionMap = new HashMap<PeripheralFunction, ArrayList<MappingInfo>>();
   
   /**
    * Add info to map by pin
    * 
    * @param info
    */
   private static void addToPinMap(MappingInfo info) {
      ArrayList<MappingInfo> list = pinMap.get(info.pin);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
         pinMap.put(info.pin, list);
      }
      list.add(info);
   }
   
   /**
    * Add info to map by function
    * 
    * @param info
    */
   private static void addToFunctionMap(MappingInfo info) {
      ArrayList<MappingInfo> list = functionMap.get(info.pin);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
         functionMap.put(info.function, list);
      }
      list.add(info);
   }

   /**
    * Reset state
    */
   public static void reset() {
      pinMap      = new HashMap<PinInformation, ArrayList<MappingInfo>>();
      functionMap = new HashMap<PeripheralFunction, ArrayList<MappingInfo>>();
   }
   
   /**
    * Get list of pin mappings associated with given function
    * 
    * @param function 
    * 
    * @return
    */
   static ArrayList<MappingInfo> getPins(PeripheralFunction function) {
      return functionMap.get(function);
   }

   /**
    * Get list of pin mappings associated with given pin
    * 
    * @param pin 
    * 
    * @return
    */
   static ArrayList<MappingInfo> getFunctions(PinInformation pin) {
      return pinMap.get(pin);
   }

   /**
    * Associates a peripheral function and a pin<br>
    * 
    * @param function   Peripheral function
    * @param pin        Pin
    * @param mux        Pin multiplexor setting to select associated function on the pin
    * @throws Exception 
    */
   public MappingInfo(PeripheralFunction function, PinInformation pin, int mux) throws Exception {
//      System.err.println(String.format("f=%s, p=%s, mux=%s", function.getName(), pin.getName(), mux));
      this.function = function;
      this.pin      = pin;
      this.mux      = mux;
      addToPinMap(this);
      addToFunctionMap(this);
      function.addPinMapping(this);
      pin.addPeripheralFunctionMapping(this);
      muxValues   = new boolean[16];
      Arrays.fill(muxValues, false);
      addMuxValue(mux);
   }
   
   /**
    * Add mux value for this template
    * 
    * @param muxValue
    * @throws Exception
    */
   public void addMuxValue(int muxValue) throws Exception {
      if ((muxValue<0)||(muxValue>=muxValues.length)) {
         throw new Exception("Mux value out of range");
      }
      muxValues[muxValue] = true;
   }

   @Override
   public String toString() {
      return String.format("Mapping(%s => %s @ %s)", function.getName(), pin.getName(), mux);
   }

};
