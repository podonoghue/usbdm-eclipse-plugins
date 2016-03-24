package net.sourceforge.usbdm.configEditor.information;
import java.util.Map;
import java.util.TreeMap;
/**
 * Information about a package<br>
 * 
 * - Package name<br>
 * - Pins present in the package
 */
public class DevicePackage {

   final String fName;
   final TreeMap<String, String> fPins;

   DevicePackage(String packageName) {
      fName = packageName;
      fPins = new TreeMap<String, String>();
   }
   
   public String getName() {
      return fName;
   }
   
   public void addPin(PinInformation pin, String location) {
      fPins.put(pin.getName(), location);
   }
   
   public Map<String, String> getPins() {
      return fPins;
   }
   
   public String getLocation(String pin) {
      return fPins.get(pin);
   }
   
   public String getLocation(PinInformation pin) {
      return fPins.get(pin.getName());
   }
   
   @Override
   public String toString() {
      return "Package(" + getName() + ")";
   }
}
