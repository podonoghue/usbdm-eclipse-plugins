import java.util.ArrayList;
import java.util.HashMap;

/**
 * Information about a pin<br>
 * - Peripheral functions mapped to that pin
 */
public class PinInformation {
   /** Name of the pin, usually the port name e.g. PTA1 */
   public String name;
   
   /** Description of peripheral functions that may be mapped to this pin */
   StringBuilder  description = new StringBuilder();

   /**
    * Get description of functions mapped to this pin
    * 
    * @return Description
    */
   String getDescription() {
      if (description.length() == 0) {
         return name;
      }
      return name + " = " + description.toString();
   }
   
   /** s
    * Functions associated with this pin arranged by function base name<br> 
    * e.g. multiple FTMs may be associated with a pin 
    */
   public HashMap<String, ArrayList<MappingInfo>> mappedPins = new HashMap<String, ArrayList<MappingInfo>>();

   /**
    * Create empty pin function for given pin
    * 
    * @param name Name of the pin, usually the port name e.g. PTA1
    */
   public PinInformation(String name) {
      this.name  = name;
   }
   
   /**
    * Gets sub-list of peripheral functions mapped to this pin
    * 
    * @param   baseName
    * 
    * @return  List (may be empty, never null)
    */
   public ArrayList<MappingInfo> createMappingList(String baseName) {
      ArrayList<MappingInfo> list = mappedPins.get(baseName);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
         mappedPins.put(baseName, list);
      }
      return list;
   }
   
   /**
    * Adds a peripheral function to the pin
    * 
    * @param function Function to add
    * @param mux      Mux setting for this function
    * 
    * @return  The sub-list of <b>similar</b> functions mapped to this pin
    */
   ArrayList<MappingInfo> addPeripheralFunction(PeripheralFunction function, int mux) {
      ArrayList<MappingInfo> elements = createMappingList(function.baseName);
      elements.add(new MappingInfo(function, mux));
      if (description.length() > 0) {
         description.append(",");
      }
      description.append(function.getName());
      return elements;
   }
   
   /**
    * Gets sub-list of peripheral functions mapped to this pin
    * 
    * @param   baseName
    * 
    * @return  List (never null)
    */
   public ArrayList<MappingInfo> getMappingList(String baseName) {
      ArrayList<MappingInfo> list = mappedPins.get(baseName);
      if (list == null) {
         list = new ArrayList<MappingInfo>();
      }
      return list;
   }
}
