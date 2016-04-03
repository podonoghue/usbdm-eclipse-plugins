package net.sourceforge.usbdm.deviceEditor.information;
import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.DeviceFamily;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.Mode;

/**
 * Represents a peripheral.<br>
 * Includes
 * <li>name e.g. FTM0
 * <li>base-name e.g. FTM0 => FTM
 * <li>instance e.g. FTM0 => 0
 * <li>clock mask
 * <li>clock register
 */
public class PeripheralTemplateInformation {

   /** Device information */
   private final DeviceInfo fDeviceInfo;
   
   /** Template that is used to select functions applicable to this template */
   private final Pattern fMatchPattern;
   
   /** Pattern for extracting the name e.g. Adc */
   private final String fnamePattern;
   
   /** Pattern for extracting the base name of C peripheral instance e.g. adc */
   private final String fInstancePattern;
   
   /** Pattern for extracting the signal */
   private final String fSignalPattern;
   
   /** Constructor for derived Peripheral<br>
    * <b><i>String Peripheral(String basename, String instance, PeripheralTemplateInformation template, DeviceInfo deviceInfo)</b></i> 
    */
   Constructor<?> fConstructor;

   /**
    * Create function template
    * 
    * @param deviceInfo             Device information handle
    * @param deviceFamily           Device family
    * @param namePattern            Base name of C peripheral class e.g. FTM2 => Ftm
    * @param peripheralBasename     Base name of peripheral e.g. FTM2 => FTM
    * @param instancePattern        Instance name e.g. FTM2 => "2"
    * @param matchTemplate          Pattern to select use of this template e.g. "FTM\\d+_CH\\d+"
    * @param matchTemplate2 
    * @param instanceWriter         Detailed instanceWriter to use
    * 
    * @throws Exception 
    */

   public PeripheralTemplateInformation(
         DeviceInfo     deviceInfo, 
         DeviceFamily   deviceFamily,
         String         namePattern, 
         String         signalPattern, 
         String         instancePattern, 
         String         matchTemplate, 
         Class<?>       instanceWriterClass) throws Exception {
      
      fDeviceInfo       = deviceInfo;
      fnamePattern      = namePattern;
      fInstancePattern  = instancePattern;
      fSignalPattern    = signalPattern;
      fConstructor      = instanceWriterClass.getConstructor(
            /* basename   */ String.class, 
            /* instance   */ String.class, 
            /* template   */ PeripheralTemplateInformation.class, 
            /* deviceinfo */ DeviceInfo.class );
      if (matchTemplate != null) {
         fMatchPattern       = Pattern.compile(matchTemplate);
      }
      else {
         fMatchPattern       = null;
      }
   }

   /**
    * Create peripheral from template
    *    
    * @param basename   Base name of peripheral e.g. FTM3 => FTM
    * @param instance   Instance of peripheral e.g. FTM3 => 3
    * 
    * @return Peripheral created
    */
   Peripheral createPeripheral(String basename, String instance) {
      Peripheral peripheral = null;
      try {
         peripheral = (Peripheral) fConstructor.newInstance(basename, instance, this, fDeviceInfo);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
      return peripheral;
   }
   
   /**
    * Checks if the template matches this <b>peripheral</b> name<br>
    * If so, the peripheral is created
    * 
    * @param name    Name of peripheral e.g. FTM3
    * 
    * @return Peripheral or null if template not applicable
    */
   public Peripheral createPeripheral(String name, Mode mode) {
      Matcher matcher = matcher(name);
      if ((matcher == null) || !matcher.matches()) {
         return null;
      }
      String basename = matcher.replaceAll(fnamePattern);
      matcher.reset();
      String instance = matcher.replaceAll(fInstancePattern);
      matcher.reset();
      
      return fDeviceInfo.createPeripheral(basename, instance, this, mode);
   }
   
   /**
    * Checks if the template matches this <b>function</b> name<br>
    * If so, both the function and peripheral are created as needed
    * 
    * @param name    Name of function e.g. FTM3_CH2
    * 
    * @return PeripheralFunction or null if template not applicable
    */
   public PeripheralFunction createFunction(String name) {         
      Matcher matcher = matcher(name);
      if ((matcher == null) || !matcher.matches()) {
         return null;
      }
      
      String basename = matcher.replaceAll(fnamePattern);
      matcher.reset();
      String instance = matcher.replaceAll(fInstancePattern);
      matcher.reset();
      String signal = matcher.replaceAll(fSignalPattern);
      matcher.reset();
      
      // Create peripheral
      fDeviceInfo.createPeripheral(basename, instance, this, Mode.ignore);
      
      // Create function
      return fDeviceInfo.createPeripheralFunction(name, basename, instance, signal);
   }
   
   /**
    * Get PCR initialisation string for given pin e.g. for <b><i>PTB4</b></i>
    * <pre>
    * "PORTB_CLOCK_MASK, PORTB_BasePtr,  GPIOB_BasePtr,  4, "
    * OR
    * "0, 0, 0, 0, "
    * </pre>
    * 
    * @param pin The pin being configured
    * 
    * @return
    * @throws Exception 
    */
   public static String getPCRInitString(PinInformation pin) {
      if (pin == null) {
         throw new RuntimeException("Pin may not be null");
      }
      String portClockMask = pin.getClockMask();
      if (portClockMask == null) {
         // No PCR - probably an analogue pin
         return "0, 0, 0, 0, ";
      }
      String pcrRegister      = pin.getPORTBasePtr();
      String gpioRegister     = pin.getGpioReg();
      String gpioBitNum       = pin.getGpioBitNum();
      
      return String.format("%-17s %-15s %-15s %-4s", portClockMask+",", pcrRegister+",", gpioRegister+",", gpioBitNum+",");
   }

//   /**
//    * Gets the numeric index of the function\n
//    * e.g. FTM3_Ch2 => 2 etc.
//    * 
//    * @param function   Function to look up
//    * @return  Index, -1 is returned if template doesn't match
//    * 
//    * @throws Exception If template matches peripheral but unexpected function 
//    */
//   public int getFunctionIndex(PeripheralFunction function) {
//      if (!getMatchPattern().matcher(function.getName()).matches()) {
//         return -1;
//      }
//      return getPeripheral(null).getFunctionIndex(function);
//   }

//   public boolean useAliases(PinInformation pinInfo) {
//      return getPeripheral(null).useAliases(pinInfo);
//   }

   /**
    * Gets the template match function
    * 
    * @param function PeripheralFunction to match
    * 
    * @return Non-null if the Matcher exists
    */
   private Matcher matcher(String name) {
      if (getMatchPattern() == null) {
         return null;
      }
      return getMatchPattern().matcher(name);
   }
   /**
    * Checks if the template matches the function
    * 
    * @param function PeripheralFunction to match
    * 
    * @return True if the template is applicable to this function 
    */
   public boolean matches(PeripheralFunction function) {
      if (function == PeripheralFunction.DISABLED) {
         return false;
      }
      Matcher matcher = matcher(function.getName());
      if (!matcher.matches()) {
         return false;
      }
      String instance = matcher.replaceAll(fInstancePattern);
      return function.getPeripheral().getInstance().equals(instance);
   }

   @Override
   public String toString() {
      return "Template("+fMatchPattern.toString() + ")";
   }

   public Pattern getMatchPattern() {
      return fMatchPattern;
   }

   /**
    * @return Base name of C peripheral class e.g. Ftm
    */
   public String getClassBasename() {
      return fnamePattern;
   }

   /**
    * @return Base name of C peripheral alias e.g. adc_ 
    */
   public String getAliasBaseName() {
      return "AAAA"; //fAliasBaseName;
   }

   /**
    * @return Base name of C peripheral instance e.g. adc 
    */
   public String getInstanceBaseName() {
      return fInstancePattern;
   }

   /**
    * @return Base name of peripheral e.g. FTM2 => FTM 
    */
   public String getPeripheralBasename() {
      return "PPPP"; //fPeripheralBasename;
   }

}