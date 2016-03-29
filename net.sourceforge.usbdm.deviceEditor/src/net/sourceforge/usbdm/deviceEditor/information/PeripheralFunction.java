package net.sourceforge.usbdm.deviceEditor.information;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * Describes a peripheral function that may be mapped to a pin<br>
 * Includes:
 * <li>name e.g. FTM3_CH2
 * <li>signal e.g. FTM3_CH2 => CH2
 * <li>peripheral owning this function e.g. FTM3
 */
public class PeripheralFunction implements Comparable<PeripheralFunction> {

   /**
    * Pin comparator
    */
   public static Comparator<String> comparator = Utils.comparator;

   private static class PinMappingComparator implements Comparator<MappingInfo>{

      @Override
      public int compare(MappingInfo o1, MappingInfo o2) {
         int rc = PinInformation.comparator.compare(o1.getPin().getName(), o2.getPin().getName());
         if (rc == 0) {
            rc = o1.getMux().ordinal() - o2.getMux().ordinal();
            if (rc == 0) {
               rc = o1.getFunctionList().compareTo(o2.getFunctionList());
            }
         }
         return rc;
      }
   }

   /** Comparator for Pin mappings to sort by pin */
   private static PinMappingComparator pinMappingComparator = new PinMappingComparator();
   
   /**
    * Disabled function
    */
   public static final PeripheralFunction DISABLED = new PeripheralFunction("Disabled", null, "");

   /*
    * ======================================================================================================
    */
   
   
   /** Peripheral that signal belongs to */
   private final Peripheral fPeripheral;

   /** Peripheral signal name number e.g. PTA3 = 3, FTM0_CH6 = CH6, SPI0_SCK = SCK */
   private final String fSignal;

   /** Name of peripheral function e.g. FTM0_CH3 */
   private final String fName;

   /** Indicates whether to include this function in output */
   private boolean fIncluded;

   /** Function template applicable to this function (if any) */
   private PeripheralTemplateInformation fTemplate = null;     

   /** Map of pins that this peripheral function may be mapped to */
   private TreeSet<MappingInfo> fPinMappings = new TreeSet<MappingInfo>(pinMappingComparator);

   /** Reset mapping for this function */
   private MappingInfo fResetMapping = new MappingInfo(PinInformation.DISABLED_PIN, MuxSelection.disabled);

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
    * @return the signal
    */
   public String getSignal() {
      return fSignal;
   }

   /**
    * Add a pin that this function may be mapped to
    * 
    * @param mapInfo
    */
   public void addMapping(MappingInfo mapInfo) {
      fPinMappings.add(mapInfo);
   }

   /**
    * Get ordered set of possible pin mappings for peripheral function
    * 
    * @return
    */
   public TreeSet<MappingInfo> getPinMapping() {
      return fPinMappings;
   }

   public void setResetPin(MappingInfo mapping) {
      if (this == DISABLED) {
         // Ignore resets to Disabled
         return;
      }
      if ((fResetMapping.getMux() != MuxSelection.disabled) && (fResetMapping != mapping)) {
         throw new RuntimeException("Multiple reset pin mappings for " + getName());
      }
      fResetMapping = mapping;
   }

   public MappingInfo getResetMapping() {
      return fResetMapping;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("PF    = "+fName);
      sb.append("\n  P   = "+fPeripheral);
      sb.append("\n  RM  = "+fResetMapping);
      for(MappingInfo mapping:fPinMappings) {
         sb.append("\n  PMi = "+mapping);
      }
      return sb.toString();
   }

}
