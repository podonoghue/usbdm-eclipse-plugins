package net.sourceforge.usbdm.peripheralDatabase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.jni.UsbdmException;

/**
 * Class to identify a SVD file 
 */
public class SVDIdentifier {
   private String        fProviderId = null;
   private String        fDeviceName = null;
   private Path          fPath = null;
   /**
    * Cached value
    */
   private DevicePeripherals   fDevicePeripherals = null;
   
   /**
    * Create empty SVDIdentifier
    */
   public SVDIdentifier() {
   }
   /**
    * Create SVDIdentifier from Provider ID and device name
    * 
    * @param id            Unique identifier for provider 
    * @param deviceName    Name of device (or regex)
    */
   public SVDIdentifier(String id, String deviceName) {
      this.fProviderId = id;
      this.fDeviceName = deviceName;
   }
   /**
    * Create SVDIdentifier from path to SVD file
    * 
    * @param id            Unique identifier for provider 
    * @param fDeviceName    Name of device (or regex)
    */
   public SVDIdentifier(Path path) {
      this.fProviderId = "";
      this.fPath       = path;
      this.fDeviceName = null;
   }
   /**
    * Create identifier from string representation of SVDID
    * 
    * @param identification
    * 
    * @throws Exception on illegal format
    */
   public SVDIdentifier(String identification) throws Exception {
      Pattern pPath = Pattern.compile("\\[SVDIdentifier:path=(.*)\\]$");
      Matcher mPath = pPath.matcher(identification);
      if (mPath.matches()) {
         // Uses path
         this.fProviderId = "";
         this.fPath       = Paths.get(mPath.group(1));
         this.fDeviceName = null;
         return;
      }
      Pattern pProvider = Pattern.compile("\\[SVDIdentifier:([^:]*):([^:]*).*\\]");
      Matcher mProvider = pProvider.matcher(identification);
      if (mProvider.matches()) {
         // Identifies provider
         this.fProviderId = mProvider.group(1);
         this.fDeviceName = mProvider.group(2);
         this.fPath       = null;
         return;
      }
      throw new Exception("Invalid SVDID string: '"+identification+"'");
   }
   
   /**
    * Get provider id
    * 
    * @return
    */
   public String getproviderId() {
      return fProviderId;
   }
   /**
    * Get string representation 
    */
   @Override
   public String toString() {
      if (fPath != null) {
         return "[SVDIdentifier:path=" + fPath + "]";
      }
      return "[SVDIdentifier:" + fProviderId + ":" + fDeviceName + "]";
   }
   /**
    * Get device name
    * 
    * @return
    * 
    * @throws UsbdmException 
    */
   public String getDeviceName() throws UsbdmException {
      if (fDeviceName == null) {
         fDeviceName = getDevicePeripherals().getName();
      }
      return fDeviceName;
   }
   /**
    * Set device name
    * 
    * @param deviceName
    */
   public void setDeviceName(String deviceName) {
      this.fDeviceName = deviceName;
   }
   /**
    * Get path to SVD (if present)
    * 
    * @return
    */
   public Path getPath() {
      return fPath;
   }
   
   /**
    * Get Device Peripherals for this SVD
    * 
    * @return Device Peripherals
    * 
    * @throws UsbdmException If can't locate device peripherals etc
    */
   public DevicePeripherals getDevicePeripherals() throws UsbdmException {
      if (fDevicePeripherals == null) {
         DevicePeripheralsProviderInterface devicePeripheralsProviderInterface = new DevicePeripheralsProviderInterface();
         fDevicePeripherals = devicePeripheralsProviderInterface.getDevice(this);
      }
      return fDevicePeripherals;
   }

   /**
    * Checks if the SVDIdentifier is valid i.e. refers to an existing device
    * @return
    */
   public boolean isValid() {
      try {
         return (getDevicePeripherals() != null);
      } catch (Exception e) {
         e.printStackTrace();
         System.err.println(e.getMessage());
      }
      return false;
   }
}
  
