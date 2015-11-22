import java.util.ArrayList;
import java.util.HashMap;

public class Aliases {
   
   /**
    * Map of all device pins to Aliases<br>
    * 1-to-Many i.e. a given pin may have multiple aliases
    */
   static HashMap<PinInformation, Aliases> aliasesByPin = new HashMap<PinInformation, Aliases>();

   /**
    * Reset internal state
    */
   static void reset() {
      aliasesByPin = new HashMap<PinInformation, Aliases>();
   }
   
   /**
    * List of aliases for this pin
    */
   ArrayList<String> aliasList = new ArrayList<String>();
   
   /**
    * Associated pin
    */
   PinInformation    pin;
   
   /**
    * Constructor
    * 
    * @param pin
    */
   private Aliases(PinInformation pin) {
      this.pin = pin;
   }
   
   /**
    * Add an alias for given pin
    * 
    * @param pin        Pin being aliased
    * @param newAlias   Name of alias
    * @return
    */
   static Aliases addAlias(PinInformation pin, String newAlias) {
      Aliases aliases = aliasesByPin.get(pin);
      if (aliases == null) {
         aliases = new Aliases(pin);
         aliasesByPin.put(pin, aliases);
      }
      aliases.aliasList.add(newAlias);
      return aliases;
   }
   
   /**
    * Get Aliases for this pin
    * 
    * @param pin  Pin to look for
    * 
    * @return  Aliases or null if none
    */
   static Aliases getAlias(PinInformation pin) {
      return aliasesByPin.get(pin);
   }  
   
   /**
    * Gets list of alias for this pin as a comma separated string.
    * 
    * @param pinName
    * @return String of form alias1,alias2 ... or null if no aliases defined
    */
   public static String getAliasList(PinInformation pinInformation) {
      Aliases aliases = getAlias(pinInformation);
      if (aliases == null) {
         return null;
      }
      StringBuilder b = new StringBuilder();
      boolean firstTime = true;
      for(String s:aliases.aliasList) {
         if (!firstTime) {
            b.append(", ");
         }
         firstTime = false;
         b.append(s);
      }
      return b.toString();
   }

}
