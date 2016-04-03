package net.sourceforge.usbdm.deviceEditor.information;

/**
 * Information about a device variant
 * 
 * <li> Device name
 * <li> Package
 * <li> Manual name
 */
public class DeviceVariantInformation {
   /** Variant name */
   String            fName;
   
   /** Package for variant */
   DevicePackage     fPackage;
   
   /** Manual for variant */
   String            fManual;
   
   /**
    * Constructor
    * 
    * @param variantName             Device name
    * @param devicePackage    Package
    * @param manual           Manual reference
    */
   public DeviceVariantInformation(String variantName, DevicePackage devicePackage, String manual) {
      fName    = variantName;
      fPackage = devicePackage;
      fManual  = manual;
   }
   
   /**
    * Get Variant name
    * 
    * @return
    */
   public String getName() {
      return fName;
   }
   
   /**
    * Get manual name
    * 
    * @return
    */
   public String getManual() {
      return fManual;
   }
   
   /**
    * Get package
    * 
    * @return
    */
   public DevicePackage getPackage() {
      return fPackage;
   }
   
   @Override
   public String toString() {
      return "Device("+fName+", "+fPackage+", "+fManual+")";
   }
}
