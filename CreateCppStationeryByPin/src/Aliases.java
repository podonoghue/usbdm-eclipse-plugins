import java.util.ArrayList;
import java.util.HashMap;

public class Aliases {
   static HashMap<PinInformation, Aliases> aliasesByPin = new HashMap<PinInformation, Aliases>();
   
   static void reset() {
      aliasesByPin = new HashMap<PinInformation, Aliases>();
   }
   
   ArrayList<String> aliasList = new ArrayList<String>();
   PinInformation    pin;
   
   private Aliases(PinInformation pin) {
      this.pin = pin;
   }
   
   static Aliases addAlias(PinInformation pin, String newAlias) {
      Aliases aliases = aliasesByPin.get(pin);
      if (aliases == null) {
         aliases = new Aliases(pin);
         aliasesByPin.put(pin, aliases);
      }
      aliases.aliasList.add(newAlias);
      return aliases;
   }
   
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
         return "";
      }
      StringBuilder b = new StringBuilder();
      boolean firstTime = true;
      for(String s:aliases.aliasList) {
         if (!firstTime) {
            b.append(",");
         }
         firstTime = false;
         b.append(s);
      }
      return b.toString();
   }
   

}
