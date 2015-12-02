import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Class representing information about a function available on a pin
 */
class FunctionTemplateInformation {

   /**
    * List of templates
    */
   static private ArrayList<FunctionTemplateInformation> list = new ArrayList<FunctionTemplateInformation>();

   static void reset() {
      list = new ArrayList<FunctionTemplateInformation>();
   }

   /**
    * Get list of templates for given basename
    * 
    * @return list
    */
   static FunctionTemplateInformation getTemplate(String baseName) {
      for (FunctionTemplateInformation item:list) {
         if (item.baseName == baseName) {
            return item;
         }
      }
      return null;
   }

   /**
    * Get list of all templates
    * 
    * @return
    */
   static ArrayList<FunctionTemplateInformation> getList() {
      return list;
   }

   /**
    * Get PCR initialisation string for given pin e.g. for <b><i>PTB4</b></i>
    * 
    * @param pin The pin being configured
    * 
    * @return
    */
   static PinInformation getPCRInformation(PinInformation pin) throws IOException {
      
      HashSet<PinInformation> set = MappingInfo.getFunctionType("GPIO");
      boolean noDigitalIO = (set != null) && set.contains(pin);
      if (noDigitalIO) {
         // No PCR register - Only analogue function on pin
         return null;
      }
      return pin;
   }

   /**
    * Get PCR initialisation string for given pin e.g. for <b><i>PTB4</b></i>
    * <pre>
    * "PORT<b><i>B</b></i>_CLOCK_MASK,  PORT<b><i>B</b></i>_BasePtr+offsetof(PCR[<b><i>4</b></i>]),  "
    * </pre>
    * 
    * @param pin The pin being configured
    * 
    * @return
    */
   static String getPCRInitString(PinInformation pin) throws IOException {
      pin = getPCRInformation(pin);

      if (pin == null) {
         return "0, 0";
      }
      String portClockMask    = pin.getClockMask(); // PORTA_CLOCK_MASK
      String pcrRegister      = pin.getPCRasInt();
      if (portClockMask == null) {
         return "0, 0, ";
      }
      return String.format("%-17s %-42s", portClockMask+",", pcrRegister+",");
   }

   final String baseName;
   final String peripheralName;
   final String groupName;
   final String groupTitle;
   final String groupBriefDescription;
   final InstanceWriter instanceWriter;
   final Pattern matchPattern;

   /**
    * 
    * @param baseName               Name to use as basename of these functions e.g. GPIO
    * @param groupName              e.g. "DigitalIO_Group"
    * @param groupTitle             e.g. "Digital Input/Output"
    * @param groupBriefDescription  e.g. "Allows use of port pins as simple digital inputs or outputs"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param instanceWriter         Detailed instanceWriter to use
    */
   public FunctionTemplateInformation(
         String baseName, String peripheralName, 
         String groupName, String groupTitle, 
         String groupBriefDescription, 
         String matchTemplate, 
         InstanceWriter instanceWriter) {
      this.baseName              = baseName;
      this.peripheralName        = peripheralName;
      this.groupName             = groupName;
      this.groupTitle            = groupTitle;
      this.groupBriefDescription = groupBriefDescription;
      this.instanceWriter        = instanceWriter;
      this.matchPattern          = Pattern.compile(matchTemplate);
      list.add(this);
   }
}