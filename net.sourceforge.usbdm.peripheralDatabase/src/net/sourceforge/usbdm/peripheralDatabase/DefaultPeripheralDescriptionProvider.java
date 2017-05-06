package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Default implementation of IPeripheralDescriptionProvider
 * 
 * @author podonoghue
 */
public class DefaultPeripheralDescriptionProvider implements IPeripheralDescriptionProvider {
   private DevicePeripheralsFactory fDevicePeripheralsFactory = null;
   private String license     = "No license set";
   private String description = "No description set";
   private String name        = "No name set";
   private String id          = "No ID set";

   /**
    * Constructor for device peripherals library
    * Expects 'data' directory to be within plug-in directory structure
    * 
    * @param context Context of plug-in
    */
   protected DefaultPeripheralDescriptionProvider(BundleContext context) {
      try {
         fDevicePeripheralsFactory = new DevicePeripheralsFactory(getDataPath(context));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Constructor for device peripherals library
    * Uses given path to data directory
    * 
    * @param dataPath Path to data directory
    */
   protected DefaultPeripheralDescriptionProvider(Path dataPath) {
      try {
         fDevicePeripheralsFactory = new DevicePeripheralsFactory(dataPath);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   /**
    * Constructor for device peripherals library
    * Uses default USBDM path to data directory
    */
   protected DefaultPeripheralDescriptionProvider() {
      try {
         fDevicePeripheralsFactory = new DevicePeripheralsFactory();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   @Override
   public DevicePeripherals getDevicePeripherals(String deviceName) {
      return fDevicePeripheralsFactory.getDevicePeripherals(deviceName);
   }

   /**
    * Set ID
    * 
    * @param id
    */
   public void setId(String id) {
      this.id = id;
   }

   /**
    * Set license
    * 
    * @param license
    */
   public void setLicense(String license) {
      this.license = license;
   }

   /**
    * Set description
    * 
    * @param description
    */
   public void setDescription(String description) {
      this.description = description;
   }

   /**
    * Set name
    * 
    * @param name
    */
   public void setName(String name) {
      this.name = name;
   }

   @Override
   public String getLicense() {
      return license;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getDescription() {
      return description;
   }

   /**
    * Get path to the plug-in data directory
    * 
    * @param context Context of plug-in to find path for
    * 
    * @return Path to data directory
    */
   protected Path getDataPath(BundleContext context) {
      Path path = null;
      if (context != null) {
         try {
            //            System.err.println("getDataPath() context = " + context);
            Bundle bundle = context.getBundle();
            //            System.err.println("getDataPath() bundle = " + bundle);
            URL folder = FileLocator.find(bundle, new org.eclipse.core.runtime.Path("data"), null);
            //            System.err.println("getDataPath() URL = " + folder);
            folder = FileLocator.resolve(folder);
            //            System.err.println("getDataPath() URL = " + folder);
            path = Paths.get(folder.toURI());
            //            System.err.println("getDataPath() path = " + path);
         } catch (IOException e) {
            e.printStackTrace();
         } catch (URISyntaxException e) {
            e.printStackTrace();
         }
      }
      if (path == null) {
         path = FileSystems.getDefault().getPath("data");
         //         System.err.println("getDataPath() default path = " + path);
      }
      return path.toAbsolutePath();
   }

   @Override
   public Vector<String> getDeviceNames() {
      return fDevicePeripheralsFactory.getDeviceNames();
   }

   @Override
   public String getId() {
      return id;
   }
}
