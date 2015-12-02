import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


/**
 * Describes how a peripheral function is mapped to a pin<br>
 */
public class MappingInfo {
   
   /** Peripheral functions that are mapped */
   public ArrayList<PeripheralFunction> functions;
   
   /** Pin that functions are mapped to */
   public PinInformation pin;
   
   /** Pin multiplexor setting to map these functions on the pin */
   public MuxSelection mux;

   /**
    * Map from Pin to list of Functions
    */
   private static HashMap<PinInformation, HashMap<MuxSelection, MappingInfo>> pinMap = new HashMap<PinInformation, HashMap<MuxSelection, MappingInfo>>();
   
   /**
    * Map from Function to list of Pins
    */
   private static HashMap<PeripheralFunction, ArrayList<MappingInfo>> functionMap = new HashMap<PeripheralFunction, ArrayList<MappingInfo>>();

   /** Map from basenames to Map of pins having that facility */
   private static HashMap<String, HashSet<PinInformation>> functionsByBaseName = new HashMap<String, HashSet<PinInformation>>();
   
   /**
    * Reset state
    */
   public static void reset() {
      pinMap              = new HashMap<PinInformation, HashMap<MuxSelection, MappingInfo>>();
      functionMap         = new HashMap<PeripheralFunction, ArrayList<MappingInfo>>();
      functionsByBaseName = new HashMap<String, HashSet<PinInformation>>();
   }
   
   /**
    * Add info to map by function
    * 
    * @param info
    */
   private static void addToFunctionMap(PeripheralFunction function, MappingInfo info) {
//      System.err.println(String.format("addToFunctionMap() - F:%s, info:%s", function.toString(), info.toString()));
      ArrayList<MappingInfo> list = functionMap.get(function);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
         functionMap.put(function, list);
      }
      list.add(info);
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
   }
   
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
    * @param function            Function signal being mapped e.g. I2C2_SCL
    * @param pinInformation      Pin being mapped e.g. PTA (pin name not signal!)
    * @param functionSelector    Multiplexor setting that maps this signal to the pin
    * @return
    */
   public static MappingInfo createMapping(PeripheralFunction function, PinInformation pinInformation, MuxSelection functionSelector) {
//      System.err.println(String.format("S:%s, P:%s, M:%s", function.toString(), pinInformation.toString(), functionSelector.toString()));
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
      mapInfo.functions.add(function);
      addToFunctionMap(function, mapInfo);
      
      return mapInfo;
   }

   /**
    * Returns a list of mapped functions as a string e.g. <b><i>GPIOC_6/LLWU_P10</b></i>
    * 
    * @return list as string
    */
   public String getFunctionList() {
      StringBuffer name = new StringBuffer();
      for (PeripheralFunction function:functions) {
         if (name.length() != 0) {
            name.append("/");
         }
         name.append(function.getName());
      }
      return name.toString();
   }

   /**
    * 
    * @param baseName
    * @return
    */
   public static HashSet<PinInformation> getFunctionType(String baseName) {
      return functionsByBaseName.get(baseName);
   }
   
   public static void addFunctionType(String baseName, PinInformation pinInfo) {
      // Record pin as having this function
      HashSet<PinInformation> set = functionsByBaseName.get(baseName);
      if (set == null) {
         set = new HashSet<PinInformation>();
      }
      set.add(pinInfo);
//      System.err.println("Matches");
   }

};
