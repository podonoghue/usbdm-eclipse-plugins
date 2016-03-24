package net.sourceforge.usbdm.configEditor.information;

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
   
   /** Base name of the peripheral e.g. FTM0_CH6 = FTM, PTA3 = PT */
   String fBaseName;
   
   /** Instance name/number of the peripheral instance e.g. FTM0_CH6 = 0, PTA3 = A */
   private String fInstance;
   
   /** Name e.g. FTM0 => FTM0 */
   private String fName;

   /** Clock register e.g. SIM->SCGC6 */
   private String fClockReg = null;

   /** Clock register mask e.g. ADC0_CLOCK_REG */
   private String fClockMask = null;

   /**
    * Create peripheral
    * 
    * @param fName
    */
   Peripheral(String baseName, String instance) {
      this.setfName(baseName+instance);
      this.fBaseName  = baseName;
      this.setfInstance(instance);
   }
   
   public void setClockInfo(String clockReg, String clockMask) {
      setfClockMask(clockMask);
      setfClockReg(clockReg);
   }
   
   @Override
   public String toString() {
      return getfName();
   }

   public String getInstance() {
      return getfInstance();
   }

   public String getfClockReg() {
      return fClockReg;
   }

   public void setfClockReg(String fClockReg) {
      this.fClockReg = fClockReg;
   }

   public String getfName() {
      return fName;
   }

   public void setfName(String fName) {
      this.fName = fName;
   }

   public String getfClockMask() {
      return fClockMask;
   }

   public void setfClockMask(String fClockMask) {
      this.fClockMask = fClockMask;
   }

   public String getfInstance() {
      return fInstance;
   }

   public void setfInstance(String fInstance) {
      this.fInstance = fInstance;
   }
}
