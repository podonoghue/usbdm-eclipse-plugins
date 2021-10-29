package net.sourceforge.usbdm.deviceEditor.information;
import java.util.Map;
import java.util.TreeMap;
/**
 * Information about a package<br>
 * 
 * - Package name<br>
 * - Map from pin names to locations
 */
public class DevicePackage {

   final String fName;
   
   /** Map from pinName to Location */
   final TreeMap<String, String> fPins;

   DevicePackage(String packageName) {
      fName = packageName;
      fPins = new TreeMap<String, String>();
   }
   
   /**
    * Get name of package
    * 
    * @return Name
    */
   public String getName() {
      return fName;
   }

   /**
    * Add pin mapping to package
    * 
    * @param pin        Pin being mapped
    * @param location   Location on package
    */
   public void addPin(Pin pin, String location) {
       fPins.put(pin.getName(), location);
   }
   
   /**
    * Get map 
    * @return
    */
   public Map<String, String> getPins() {
      return fPins;
   }
   
   /**
    * Get location of pin from pin name
    * 
    * @param pin  Pin to look for
    * 
    * @return  Pin location
    */
   public String getLocation(String pin) {
      return fPins.get(pin);
   }
   
   /**
    * Get location of pin
    * 
    * @param pin  Pin to look for
    * 
    * @return  Pin location or null if not found in this package
    */
   public String getLocation(Pin pin) {
      return fPins.get(pin.getName());
   }
   
   @Override
   public String toString() {
      return "Package(" + getName() + ")";
   }
}
