package net.sourceforge.usbdm.peripheralDatabase;

import java.util.Vector;

/**
 * Class providing access to device peripheral descriptions contained in SVD files
 *  
 * @author podonoghue
 */
public interface IPeripheralDescriptionProvider {
   /**
    * Get ID of this provider
    * 
    * @return Name as string
    */
   public String getId();
   /**
    * Get name of this provider (use as short description)
    * 
    * @return Name as string
    */
   public String getName();
   /**
    * Get long description
    * 
    * @return description as string
    */
   public String getDescription();
   /**
    * Get license applicable to the SVD files
    * 
    * @return License as string
    */
   public String getLicense();
   /**
    * Get names of devices described
    * 
    * @return Vector of device names
    */
   public Vector<String> getDeviceNames();
   /**
    * Get the device peripherals for the given device
    * 
    * @param deviceName Name of device
    * 
    * @return Device peripherals
    */
   public DevicePeripherals getDevicePeripherals(String deviceName);
   
   /**
    * Determine the base file name for the deviceName.<br>
    * This can be used to construct the name of either the header file or the SVD file.
    * 
    * @param deviceName
    * 
    * @return Filename if found e.g. MK11D5, or null if device is not found
    */
   String getMappedFilename(String deviceName);

   /**
    * Determine the mapped device name for the deviceName e.g. MK10DxxxM5
    * 
    * @param deviceName
    * 
    * @return Device name if found e.g. MK10DxxxM5, or null if device is not found
    */
   String getMappedDeviceName(String deviceName);
}
