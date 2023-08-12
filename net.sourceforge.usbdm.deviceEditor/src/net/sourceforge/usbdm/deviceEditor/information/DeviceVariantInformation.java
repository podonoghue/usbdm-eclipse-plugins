package net.sourceforge.usbdm.deviceEditor.information;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.parsers.XmlDocumentUtilities;

/**
 * Information about a device variant
 * 
 * <li> Device name
 * <li> Manual name
 * <li> Package
 * <li> Generic device name
 */
public class DeviceVariantInformation {
   /** Variant name e.g. MK28FN2M0VMI15, MK20DN32VLH5, FRDM_K20D50M */
   private String            fPreciseName;
   
   /** Package information for variant e.g. LQFP_64, QFN_32, FRDM_K28F */
   private DevicePackage     fPackage;
   
   /** Manual for variant e.g. K20P64M50SF0RM, K28P210M150SF5RM */
   private String            fManual;

   /** Device name e.g. MK28FN2M0M15 - Used to obtain SVD and device information */
   private String            fDeviceName;
   
   /**
    * Constructor
    * 
    * @param preciseName      Device variant e.g. MK28FN2M0VMI15, MK20DN32VLH5, FRDM_K20D50M - Key
    * @param manual           Manual reference e.g. K20P64M50SF0RM
    * @param devicePackage    Package information including pin information
    * @param deviceName       Device name as in USBDM device files e.g. MK28FN2M0M15 - Used to obtain SVD and device information
    */
   public DeviceVariantInformation(String preciseName, String manual, DevicePackage devicePackage, String deviceName) {
      fPreciseName   = preciseName;
      fPackage       = devicePackage;
      fManual        = manual;
      fDeviceName    = deviceName;
   }
   
   /**
    * Get Precise device name e.g. MKE04Z8VTG4, MK20DN32VLH5, FRDM_K20D50M
    * 
    * @return
    */
   public String getPreciseName() {
      return fPreciseName;
   }
   
   /**
    * Get package information e.g. LQFP_64, QFN_32
    * 
    * @return
    */
   public DevicePackage getPackage() {
      return fPackage;
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
    * Get device name as in USBDM device files e.g. MK28FN2M0M15 - Used to obtain SVD and device information
    * @return
    */
   public String getDeviceName() {
      return fDeviceName;
   }

   @Override
   public String toString() {
      return "DeviceVariantInformation["+fPreciseName+", "+fManual+", "+fPackage+", "+fDeviceName+"]";
   }
   
   public void writeXml(XmlDocumentUtilities documentUtilities) throws IOException {
      documentUtilities.openTag("device");
      documentUtilities.writeAttribute("variant",     fPreciseName);
      documentUtilities.writeAttribute("manual",      fManual);
      documentUtilities.writeAttribute("package",     fPackage.getName());
      documentUtilities.writeAttribute("deviceName",  fDeviceName);
      documentUtilities.closeTag();
   }

}
