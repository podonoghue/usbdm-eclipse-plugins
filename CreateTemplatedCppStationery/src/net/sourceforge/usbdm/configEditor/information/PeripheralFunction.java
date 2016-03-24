package net.sourceforge.usbdm.configEditor.information;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes a peripheral function that may be mapped to a pin<br>
 * Includes:
 * <li>name e.g. FTM3_CH2
 * <li>signal e.g. FTM3_CH2 => CH2
 * <li>peripheral owning this function e.g. FTM3
 */
public class PeripheralFunction implements Comparable<PeripheralFunction> {

   /**
    * Comparator for port names e.g. PTA13 c.f. PTB12
    * Treats the number separately as a number.
    */
   public static final Comparator<String> comparator = new Comparator<String>() {
      @Override
      public int compare(String arg0, String arg1) {
         if ((arg0.length()==0) && (arg1.length()==0)) {
            return 0;
         }
         if (arg0.length()==0) {
            return -1;
         }
         if (arg1.length()==0) {
            return 1;
         }
         Pattern p = Pattern.compile("([^\\d]*)(\\d*)(.*)");
         Matcher m0 = p.matcher(arg0);
         Matcher m1 = p.matcher(arg1);
         if (m0.matches() && m1.matches()) {
            String t0 = m0.group(1);
            String t1 = m1.group(1);
            int r = t0.compareTo(t1);
            if (r == 0) {
               // Treat as numbers
               String n0 = m0.group(2);
               String n1 = m1.group(2);
               int no0 = -1, no1 = -1;
               if (n0.length() > 0) {
                  no0 = Integer.parseInt(n0);
               }
               if (n1.length() > 0) {
                  no1 = Integer.parseInt(n1);
               }
               r = -no1 + no0;

               if (r == 0) {
                  String s0 = m0.group(3);
                  String s1 = m1.group(3);
                  r = compare(s0, s1);
               }
            }
            return r;
         }
         return arg0.compareTo(arg1);
      }
   };

   /** Map of pins that this peripheral function may be mapped to */

   /** Peripheral that signal belongs to */
   private Peripheral fPeripheral;

   /** Peripheral signal name number e.g. PTA3 = 3, FTM0_CH6 = CH6, SPI0_SCK = SCK */
   private String fSignal;

   /** Name of peripheral function e.g. FTM0_CH3 */
   private String fName;

   /** Indicates whether to include this function in output */
   private boolean fIncluded;

   /** Function template applicable to this function (if any) */
   private PeripheralTemplateInformation fTemplate;     

   /**
    * Disabled function
    */
   public static final PeripheralFunction DISABLED = new PeripheralFunction("Disabled", null, "");

   /**
    * Create peripheral function from components<br>
    * e.g. FTM0_CH6 = <b>new</b> PeripheralFunction(FTM, 0, 6)
    * 
    * @param baseName   Base name of the peripheral e.g. FTM0_CH6 = FTM, PTA3 = PT
    * @param fInstance  Number/name of the peripheral e.g. FTM0_CH6 = 0, PTA3 = A
    * @param signal     Channel/pin number/operation e.g. FTM0_CH6 = 6, PTA3 = 3, SPI0_SCK = SCK
    * @throws Exception 
    */


   /**
    * 
    * @param name          Name of peripheral function e.g. FTM0_CH3 
    * @param peripheral    Peripheral that signal belongs to 
    * @param signal        Peripheral signal name or number e.g. PTA3 = 3, FTM0_CH6 = CH6, SPI0_SCK = SCK 
    */
   PeripheralFunction(String name, Peripheral peripheral, String signal) {
      fName       = name;
      fPeripheral = peripheral;
      fSignal     = signal;
      fTemplate   = null;
   }

   void setTemplate(PeripheralTemplateInformation functionTemplateInformation) {
      fTemplate = functionTemplateInformation;
   }

   public PeripheralTemplateInformation getTemplate() {
      return fTemplate;
   }

   void setIncluded(boolean include) {
      fIncluded = include;
   }

   public boolean isIncluded() {
      return fIncluded;
   }

   /**
    * Create descriptive name<br>
    * e.g. MappingInfo(FTM, 0, 6) = FTM0_6
    * 
    * @return name created
    */
   public String getName() {
      return fName;
      //      return fBaseName+fInstance+"_"+fSignal;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   public String toString() {
      return getName();
   }

   @Override
   public int compareTo(PeripheralFunction o) {
      return comparator.compare(getName(), o.getName());
   }

   /**
    * @return the Peripheral
    */
   public Peripheral getPeripheral() {
      return fPeripheral;
   }

   /**
    * @param peripheral the peripheral to set
    */
   public void setPeripheral(Peripheral peripheral) {
      this.fPeripheral = peripheral;
   }

   /**
    * @return the signal
    */
   public String getSignal() {
      return fSignal;
   }

   /**
    * @param signal the signal to set
    */
   public void setSignal(String signal) {
      this.fSignal = signal;
   }


}
