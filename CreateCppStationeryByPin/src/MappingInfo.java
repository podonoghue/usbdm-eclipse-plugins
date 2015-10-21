import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Describes how a peripheral function is mapped to a pin<br>
 */
public class MappingInfo {
   
   /** Peripheral function that is mapped */
   public ArrayList<PeripheralFunction> functions;
   
   /** Pin that function is mapped to */
   public PinInformation pin;
   
   /** Pin multiplexor setting to map this function on the pin */
   public MuxSelection mux;

   /** List of mux values for which this peripheral should be enabled */
   boolean[] muxValues;

   /**
    * Map from Pin to list of Functions
    */
   private static HashMap<PinInformation, HashMap<MuxSelection, MappingInfo>> pinMap      = new HashMap<PinInformation, HashMap<MuxSelection, MappingInfo>>();
   
   /**
    * Map from Function to list of Pins
    */
   private static HashMap<PeripheralFunction, ArrayList<MappingInfo>> functionMap = new HashMap<PeripheralFunction, ArrayList<MappingInfo>>();
   
   /**
    * Add info to map by function
    * 
    * @param info
    */
   private static void addToFunctionMap(MappingInfo info) {
      ArrayList<PeripheralFunction> functions = info.functions;
      for (PeripheralFunction f:functions) {
         ArrayList<MappingInfo> list = functionMap.get(f);
         if (list == null) {
            list = new ArrayList<MappingInfo>();
            functionMap.put(f, list);
         }
         list.add(info);
      }
   }

   /**
    * Reset state
    */
   public static void reset() {
      pinMap      = new HashMap<PinInformation, HashMap<MuxSelection, MappingInfo>>();
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
   static HashMap<MuxSelection, MappingInfo> getFunctions(PinInformation pin) {
      return pinMap.get(pin);
   }

   /**
    * Associates a peripheral function and a pin<br>
    * 
    * @param function   Peripheral function
    * @param pin        Pin
    * @param functionSelector        Pin multiplexor setting to select associated function on the pin
    */
   private MappingInfo(PinInformation pin, MuxSelection functionSelector)  {
//      System.err.println(String.format("f=%s, p=%s, mux=%s", function.getName(), pin.getName(), mux));
      this.functions = new ArrayList<PeripheralFunction>();
      this.pin       = pin;
      this.mux       = functionSelector;
      muxValues   = new boolean[16];
      Arrays.fill(muxValues, false);
//      if (mux>0) {
//         addMuxValue(mux);
//      }
   }
   
//   /**
//    * Add mux value for this template
//    * 
//    * @param muxValue
//    * @throws Exception
//    */
//   public void addMuxValue(int muxValue) throws Exception {
//      if ((muxValue<0)||(muxValue>=muxValues.length)) {
//         throw new Exception("Mux value out of range");
//      }
//      muxValues[muxValue] = true;
//   }
//
   @Override
   public String toString() {
      return String.format("Mapping(%s => %s @ %s)", pin.getName(), functions.toString(), mux);
   }

   /**
    * Create new Pin mapping<br>
    * 
    * Mapping is added to pin map<br>
    * Mapping is added to function map<br>
    * 
    * @param signal              Function signal being mapped e.g. I2C2_SCL
    * @param pinInformation      Pin being mapped e.g. PTA (pin name not signal!)
    * @param functionSelector    Multiplexor setting that maps this signal to the pin
    * @return
    */
   public static MappingInfo createMapping(PeripheralFunction signal, PinInformation pinInformation, MuxSelection functionSelector) {
      System.err.println(String.format("S:%s, P:%s, M:%s", signal.toString(), pinInformation.toString(), functionSelector.toString()));
      HashMap<MuxSelection, MappingInfo> list = pinMap.get(pinInformation);
      if (list == null) {
         list = new HashMap<MuxSelection, MappingInfo>();
         pinMap.put(pinInformation, list);
      }
      MappingInfo mapInfo = list.get(functionSelector);
      if (mapInfo == null) {
         mapInfo = new MappingInfo(pinInformation, functionSelector);
         list.put(functionSelector, mapInfo);
      }
      mapInfo.functions.add(signal);
      addToFunctionMap(mapInfo);
      
      return mapInfo;
   }

};
