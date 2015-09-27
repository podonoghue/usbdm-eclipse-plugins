import java.util.HashMap;

/**
 * Describes a peripheral function that may be mapped to a pin<br>
 * e.g. FTM0_CH6 => MappingInfo(FTM, 0, 6)<br>
 * e.g. PTA3     => MappingInfo(PT, A, 3)
 */
class PeripheralFunction {
   public   String baseName;    // Base name of the peripheral e.g. FTM0_CH6 => FTM, PTA3 => PT
   public   String name;        // Name/Number of the peripheral e.g. FTM0_CH6 => 0, PTA3 => A
   public   String channel;     // Channel or pin operation e.g. FTM0_CH6 => 6, PTA3 => 3, SPI0_SCK => SCK
   
   /**
    * Map of all peripheral functions created
    */
   static HashMap<String, PeripheralFunction> functions = new HashMap<String, PeripheralFunction>();
   
   /**
    * Get map of peripheral functions created
    * 
    * @return map 
    */
   public static HashMap<String, PeripheralFunction> getFunctions() {
      return functions;
   }

   /**
    * Create peripheral function for mapping to a pin<br>
    * e.g. FTM0_CH6(fn4) => MappingInfo(FTM, 0, 6, 4)
    * 
    * @param baseName   Base name of the peripheral e.g. FTM0_CH6 => FTM, PTA3 => PT
    * @param name       Number/name of the peripheral e.g. FTM0_CH6 => 0, PTA3 => A
    * @param channel    Channel/pin number/operation e.g. FTM0_CH6 => 6, PTA3 => 3, SPI0_SCK => SCK
    */
   private PeripheralFunction(String baseName, String name, String channel) {
      System.err.println(String.format("b=%s, n=%s, ch=%s", baseName, name, channel));
      this.baseName = baseName;
      this.name     = name;
      this.channel  = channel;
   }
   
   /**
    * Factory to create peripheral function for mapping to a pin<br>
    * e.g. FTM0_CH6(fn4) => MappingInfo(FTM, 0, 6, 4)
    * 
    * @note The function is added to a internal list for re-use<br>
    * If it already exists then a previous instance is returned.
    * 
    * @param baseName   Base name of the peripheral e.g. FTM0_CH6 => FTM, PTA3 => PT
    * @param name       Number/name of the peripheral e.g. FTM0_CH6 => 0, PTA3 => A
    * @param channel    Channel/pin number/operation e.g. FTM0_CH6 => 6, PTA3 => 3, SPI0_SCK => SCK
    */
   static public PeripheralFunction getPeripheralFunction(String baseName, String name, String channel) {
      String key = makeKey(baseName, name, channel);
      PeripheralFunction function = functions.get(key);
      if (function == null) {
         function = new PeripheralFunction(baseName, name, channel);
         functions.put(key, function);
      }
      return function;
   }
   
   /**
    * Makes a key for the peripheral function<br>
    * e.g. makeKey(FTM, 0, 6) => FTM0_6
    * 
    * @return name created
    */
   static public String makeKey(String baseName, String name, String channel) {
      return baseName+name+"_"+channel;
   }
   
   /**
    * Gets the unique key for this peripheral function
    * 
    * @return name created
    */
   public String getKey() {
      return makeKey(baseName, baseName, channel);
   }
   
   /**
    * Create descriptive name<br>
    * e.g. MappingInfo(FTM, 0, 6) => FTM0_6
    * 
    * @return name created
    */
   public String getName() {
      return baseName+name+"_"+channel;
   }
   
   public String toString() {
      return getName();
   }
}
