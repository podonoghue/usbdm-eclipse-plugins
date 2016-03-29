package net.sourceforge.usbdm.deviceEditor.information;

import java.util.TreeMap;

/**
 * Represents a peripheral.<br>
 * Includes
 * <li>name e.g. FTM0
 * <li>base-name e.g. FTM0 => FTM
 * <li>instance e.g. FTM0 => 0
 * <li>clock mask
 * <li>clock register
 */
public class Peripheral {
   
   /** Name e.g. FTM0 => FTM0 */
   private final String fName;

   /** Base name of the peripheral e.g. FTM0_CH6 = FTM, PTA3 = PT */
   private final String fBaseName;
   
   /** Instance name/number of the peripheral instance e.g. FTM0_CH6 = 0, PTA3 = A */
   private final String fInstance;
   
   /** Description of peripheral */
   private final String fdescription;

   /** Clock register e.g. SIM->SCGC6 */
   private String fClockReg = null;

   /** Clock register mask e.g. ADC0_CLOCK_REG */
   private String fClockMask = null;

   /** List of all functions on this peripheral */
   private TreeMap<String, PeripheralFunction> fFunctions = new TreeMap<String, PeripheralFunction>(PeripheralFunction.comparator);
   
   /**
    * Create peripheral
    * 
    * @param baseName      Base name e.g. FTM3 => FTM
    * @param instance      Instance e.g. FTM3 => 3
    * @param description   Description of peripheral
    */
   Peripheral(String baseName, String instance, String description) {
      fName          = baseName+instance;
      fBaseName      = baseName;
      fInstance      = instance;
      fdescription   = description;
   }
   
   public void setClockInfo(String clockReg, String clockMask) {
      setClockMask(clockMask);
      setClockReg(clockReg);
   }
   
   @Override
   public String toString() {
      return fName;
   }

   public String getInstance() {
      return fInstance;
   }

   public String getClockReg() {
      return fClockReg;
   }

   public void setClockReg(String fClockReg) {
      this.fClockReg = fClockReg;
   }

   public String getName() {
      return fName;
   }

   public String getClockMask() {
      return fClockMask;
   }

   public void setClockMask(String fClockMask) {
      this.fClockMask = fClockMask;
   }

   public String getBasename() {
      return fBaseName;
   }

   public void addFunction(PeripheralFunction peripheralFunction) {
      fFunctions.put(peripheralFunction.getName(), peripheralFunction);
   }
   
   public TreeMap<String, PeripheralFunction> getFunctions() {
      return fFunctions;
   }

   public String getDescription() {
      return fdescription;
   }
}
