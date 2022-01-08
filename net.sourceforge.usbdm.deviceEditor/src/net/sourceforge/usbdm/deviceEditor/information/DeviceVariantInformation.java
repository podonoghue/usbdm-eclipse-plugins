package net.sourceforge.usbdm.deviceEditor.information;

/**
 * Information about a device variant
 * 
 * <li> Device name
 * <li> Package
 * <li> Manual name
 */
public class DeviceVariantInformation {
   /** Variant name e.g. MK20DN32VLH5, FRDM_K20D50M */
   String            fVariantName;
   
   /** Package for variant e.g. LQFP_64, QFN_32 */
   DevicePackage     fPackage;
   
   /** Manual for variant e.g. K20P64M50SF0RM */
   String            fManual;
   
   /**
    * Constructor
    * 
    * @param variantName      Device variant name e.g. MK20DN32VLH5, FRDM_K20D50M
    * @param devicePackage    Package e.g. LQFP_64, QFN_32
    * @param manual           Manual reference e.g. K20P64M50SF0RM
    */
   public DeviceVariantInformation(String variantName, DevicePackage devicePackage, String manual) {
      fVariantName   = variantName;
      fPackage       = devicePackage;
      fManual        = manual;
   }
   
   /**
    * Get Variant name e.g. MK20DN32VLH5, FRDM_K20D50M
    * 
    * @return
    */
   public String getName() {
      return fVariantName;
   }
   
   /**
    * Get manual name e.g. K20P64M50SF0RM
    * 
    * @return
    */
   public String getManual() {
      return fManual;
   }
   
   /**
    * Get package e.g. LQFP_64, QFN_32
    * 
    * @return
    */
   public DevicePackage getPackage() {
      return fPackage;
   }
   
   @Override
   public String toString() {
      return "DeviceVariantInformation["+fVariantName+", "+fPackage+", "+fManual+"]";
   }
}
