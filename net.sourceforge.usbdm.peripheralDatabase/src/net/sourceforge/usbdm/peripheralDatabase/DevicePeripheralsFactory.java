package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.jni.Usbdm;

public class DevicePeripheralsFactory {
   private Path                fFolderPath      = null;
   private DeviceFileList      fDeviceFileList  = null;

   private static final String USBDM_SVD_DEFAULT_PATH     = "DeviceData/Device.SVD/Internal";
   private static final String USBDM_SVD_DEFAULT_FILELIST = "DeviceList.xml";

   /**
    * Creates factory using given path to device folder
    * 
    * @param folderPath
    * @throws Exception 
    */
   public DevicePeripheralsFactory(Path folderPath) {
      fFolderPath = folderPath;
      checkForFileList();
   }

   /**
    * Creates factory using default path to device folder
    * 
    * @throws Exception 
    */
   public DevicePeripheralsFactory() {
      fFolderPath = getDefaultPath();
      checkForFileList();
   }

   /**
    * Checks for a device list associated with this factory
    * 
    * @throws Exception
    */
   private void checkForFileList() {
      fDeviceFileList = null;
      Path deviceList = fFolderPath.resolve(USBDM_SVD_DEFAULT_FILELIST);
      if (deviceList.toFile().exists()) {
         try {
            fDeviceFileList = new DeviceFileList(deviceList);
//            System.err.println("DevicePeripheralsFactory.checkForFileList() - found deviceList");
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Gets default path for device XML files
    * 
    * @return path or null if default cannot be found
    */
   private Path getDefaultPath() {
      Path usbdmResourcePath = Paths.get(Usbdm.getResourcePath().toOSString());
      Path path = null;
      if (usbdmResourcePath != null) {
         path = usbdmResourcePath.resolve(USBDM_SVD_DEFAULT_PATH);
      }
//      System.err.println("DevicePeripheralsFactory.getDefaultPath() xmlRootPath = \"" + path + "\"");
      return path;
   }

   /**
    *  Creates peripheral database for device
    * 
    *  @param name Name of SVD file e.g. "MK20D5", default extensions will be tried e.g. ".xml"
    *  
    *  @return device peripheral description or null on error
    */
   public DevicePeripherals getDevicePeripherals(Path name) {
      DevicePeripherals devicePeripherals = null;

      // Parse the XML file into the XML internal DOM representation
      try {
         for (String extension : new String[]{"", ".svd", ".xml", ".svd.xml"}) {
            // Resolve against default path
            Path filename = fFolderPath.resolve(name+extension);
            if (Files.isRegularFile(filename)) {
//               System.err.println("DevicePeripheralsFactory.getDevicePeripherals() - Trying \""+filename+"\" - found");
               devicePeripherals = new DevicePeripherals(filename);
               break;
            }
//            System.err.println("DevicePeripheralsFactory.getDevicePeripherals() - Trying \""+filename+"\" - not found");
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.err.println("DevicePeripheralsFactory.getDevicePeripherals() - Exception for device: " + name);
         System.err.println("DevicePeripheralsFactory.getDevicePeripherals() - Exception: reason: " + e.getMessage());
      }
      return devicePeripherals;
   }

   /**
    *  Creates peripheral database for device
    * 
    *  @param name Where to load database from. <br>
    *  <li> This may be the name of device e.g. "MK20D5" (Mapped names will also be tried)
    *  <li> Path of a device file (absolute or relative)
    *  
    *  @return device peripheral description or null on error
    */
   public DevicePeripherals getDevicePeripherals(String name) {
      boolean doneBoth = false;
      while (true) {
         DevicePeripherals devicePeripherals = null;
         // Try file list
         if (fDeviceFileList != null) {
            devicePeripherals = getDevicePeripherals(fDeviceFileList, name);
            if (devicePeripherals != null) {
               return devicePeripherals;
            }
         }
         // Try name as file name
         devicePeripherals = getDevicePeripherals(Paths.get(name));
         if (devicePeripherals != null) {
            return devicePeripherals;
         }
         if (doneBoth) {
            return null;
         }
         // Try again with mapped name
         name = getMappedSvdName(name);
         if (name == null) {
            return null;
         }
         doneBoth = true;
      }
   }

   /**
    *  Creates peripheral database for device
    * 
    *  @param deviceList   Device list file to use as index
    *  @param device       Device name as appears in deviceList file
    *  
    *  @return device peripheral description or null on error
    */
   public DevicePeripherals getDevicePeripherals(DeviceFileList deviceList, String device) {
      try {
         Path path = deviceList.getSvdFilename(device);
         if (path != null) {
//            System.err.println("DevicePeripheralsFactory.getDevicePeripherals() - Trying filelist \""+device+"\" - found");
            return getDevicePeripherals(path);
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.err.println("DevicePeripheralsFactory.getDevicePeripherals() - Exception for device: " + device);
         System.err.println("DevicePeripheralsFactory.getDevicePeripherals() - Exception: reason: " + e.getMessage());
      }
      return null;
   }

   private static class PatternPair {
      Pattern p;
      String  m;
      public PatternPair(Pattern p, String m) {
         this.p = p;
         this.m = m;
      }
   }
   private ArrayList<PatternPair> mappedNames = null;

   /**
    * Maps raw device name to generic name e.g. MK11DN512M5 -> MK11D5
    * 
    * @param originalName name to map
    * 
    * @return mapped name or null if not mapped
    */
   private String getMappedSvdName(String originalName) {
      if (mappedNames == null) {
         mappedNames = new ArrayList<PatternPair>();
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?\\d{2,}M(\\d+)$"), "$1$2"));  // MK11DN512M5 -> MK11D5
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?1M0M(\\d+)$"),     "$1$2"));  // MK10FN1M0M5 -> MK10D5
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?\\d{2,}Z$"),       "$1Z10")); // MK10DN512Z  -> MK10DZ10
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?\\d{2,}$"),        "$15"));   // MK10DN128   -> MK10D5
         mappedNames.add(new PatternPair(Pattern.compile("^([^\\d]+\\d+[D|F|Z])[N|X]?1M0$"),            "$112"));  // MK10FN1M0   -> MK10F12
      }
      for (PatternPair pair : mappedNames ) {
         Pattern p = pair.p;
         Matcher m = p.matcher(originalName);
         if (m.matches()) {
            return m.replaceAll(pair.m);
         }
      }
      return null;
   }

   /**
    * Get list of devices
    * 
    * @return Vector of device (or file) names
    * 
    * @throws Exception
    */
   public Vector<String> getDeviceNames() {
      Vector<String> files = new Vector<String>();

      if (fDeviceFileList != null) {
//         System.err.println("DevicePeripherals.createDatabase() - Using DeviceFileList: \n");
         files = fDeviceFileList.getDeviceList();
         Collections.sort(files);
         return files;
      }
      DirectoryStream<Path> stream = null;
      try {
         stream = Files.newDirectoryStream(fFolderPath, "*.svd.xml");
         Pattern p = Pattern.compile("(.*)\\.svd\\.xml");
//         System.err.println("DevicePeripherals.createDatabase() - Using Directory contents \n");
         for (Path entry: stream) {
            Matcher m = p.matcher(entry.getFileName().toString());
            if (m.matches()) {
               files.add(m.replaceAll("$1"));
            }
            else {
               files.add(entry.getFileName().toString());
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      if (stream != null) {
         try {
            stream.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      Collections.sort(files);
      return files;
   }
   
   /**
    * @param args
    */
   public static void main(String[] args) {
      System.err.println("Starting");
      DevicePeripheralsFactory factory;
      try {
         factory = new DevicePeripheralsFactory();
         String deviceName = "MK20DX128";
//         String deviceName = "FRDM_K22F";
         try {
            DevicePeripherals devicePeripherals = factory.getDevicePeripherals(deviceName);
            System.err.println("Found device " + deviceName + " => " + devicePeripherals.getDescription());
         } catch (Exception e) {
            e.printStackTrace();
         }
      } catch (Exception e1) {
         e1.printStackTrace();
      }
      System.err.println("Done");
      System.err.flush();
   }

}
