import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripheralDatabase.DeviceFileList;
import net.sourceforge.usbdm.peripheralDatabase.DeviceFileList.Pair;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.ModeControl;
import net.sourceforge.usbdm.peripheralDatabase.PeripheralDatabaseMerger;

import org.eclipse.core.runtime.IPath;

public class CreatePeripheralDatabase {
   private static final  Path PACKAGE_FOLDER    = Paths.get("C:/Users/podonoghue/Documents/Development/USBDM/usbdm-eclipse-makefiles-build/PackageFiles");
   private static final  Path MAIN_FOLDER                                  = PACKAGE_FOLDER.resolve("DeviceData/Device.SVD");
//   @SuppressWarnings("unused")
   private static final  Path headerReducedMergedOptimisedManualFolder     = PACKAGE_FOLDER.resolve("Stationery/Project_Headers");
   @SuppressWarnings("unused")
private static final  Path freescaleFolder        = MAIN_FOLDER.resolve("Freescale");
   private static final  Path usbdmFolder            = MAIN_FOLDER.resolve("Internal");
//   @SuppressWarnings("unused")
   private static final  Path usbdmCheckFolder       = MAIN_FOLDER.resolve("Internal.Check");
   @SuppressWarnings("unused")
private static final  Path freescaleCheckFolder   = MAIN_FOLDER.resolve("Freescale.Check");
   
   private static final  String DEVICE_LIST_FILENAME        = "DeviceList.xml";
   private static final  String CMSIS_SCHEMA_FILENAME       = "CMSIS-SVD_Schema_1_1.xsd";
   private static final  String DEVICE_LIST_SCHEMA_FILENAME = "DeviceListSchema.dtd";

   private static String onlyFileToProcess = null;

   static void copyFile(IPath source, IPath destination) throws IOException {
      System.err.println("Copying "+source.toOSString()+" -> \n        "+destination.toOSString());
      Files.copy(new java.io.File(source.toPortableString()).toPath(), 
            new java.io.File(destination.toPortableString()).toPath(), 
            StandardCopyOption.REPLACE_EXISTING); 
   }
   /*
    * *************************************************************************************************************************************
    * *************************************************************************************************************************************
    */

   static void mergeFiles(Path svdSourceFolderPath, final DirectoryStream.Filter<Path> directoryFilter, PeripheralDatabaseMerger merger, boolean optimise) throws Exception {
      int deviceCount = 500;
      DirectoryStream<Path> svdSourceFolderStream = Files.newDirectoryStream(svdSourceFolderPath.toAbsolutePath(), directoryFilter);
      for (Path filePath : svdSourceFolderStream) {
         if (deviceCount-- == 0) {
            break;
         }
         if (Files.isRegularFile(filePath)) {
            String fileName = filePath.getFileName().toString();
            if ((onlyFileToProcess != null) && (!fileName.matches(onlyFileToProcess))) {
               return;
            }
            if (fileName.endsWith(".svd.xml")) {
               System.err.println("Merging SVD file : \""+filePath.toString()+"\"");

               // Read device peripheral database
               DevicePeripherals devicePeripherals = new DevicePeripherals(filePath);
               if (optimise) {
                  devicePeripherals.optimise();
               }
               // Create merged SVD file
               merger.writeDeviceToSVD(devicePeripherals);
            }
         }
      }
   }
   /**
    *    Merges multiple SVD files and produces SVD files with common peripherals extracted
    *    The original files may already be merged.
    *    
    *    sourceFolderPath  +-> destinationFolderPath : (extracts common peripherals across devices)
    *    
    *    @param svdSourceFolderPath - Folder of SVD files to merge
    *    @param svdOutputFolderPath - Destination folder (which is created) with sub-folder of peripherals
    *    @param optimise            - Whether to apply optimisations when creating SVD files
    * 
    *    @throws IOException 
    *    
    */
   public static void mergeFiles(Path svdSourceFolderPath, Path svdOutputFolderPath, boolean optimise, boolean removeFolder) throws IOException {

      if (Files.exists(svdOutputFolderPath)) {
         if (!removeFolder) {
            System.err.println("Destination already exists " + svdOutputFolderPath.toString());
         }
         else {
            System.err.println("Destination already exists -  deleting " + svdOutputFolderPath.toString());
            removeDirectoryTree(svdOutputFolderPath);
         }
      }
      if (!Files.isDirectory(svdSourceFolderPath)) {
         System.err.println("Source doesn't exist " + svdSourceFolderPath.toString());
         return;
      }
      svdOutputFolderPath.toFile().mkdir();

      PeripheralDatabaseMerger merger = new PeripheralDatabaseMerger();

      merger.setXmlExtension(".svd.xml");
      merger.setXmlRootPath(svdOutputFolderPath.toFile());

      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setFreescaleFieldNames(optimise);
      ModeControl.setMapFreescaleCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(optimise);
      ModeControl.setFoldRegisters(optimise);

      System.err.println("Writing files to : \""+svdOutputFolderPath.toString()+"\"");

      DirectoryStream.Filter<Path> coldfireDirectoryFilter = new DirectoryStream.Filter<Path>() {
         @Override
         public boolean accept(Path path) throws IOException {
            return path.getFileName().toString().matches("MCF.*");
         }
      };

      DirectoryStream.Filter<Path> nonColdfireDirectoryFilter = new DirectoryStream.Filter<Path>() {
         @Override
         public boolean accept(Path path) throws IOException {
            return !path.getFileName().toString().matches("MCF.*");
         }
      };

      try {
         mergeFiles(svdSourceFolderPath, nonColdfireDirectoryFilter, merger, optimise);
         mergeFiles(svdSourceFolderPath, coldfireDirectoryFilter,    merger, optimise);
         merger.writePeripheralsToSVD();
         merger.writeVectorTablesToSVD();
      } catch (Exception e) {
         e.printStackTrace();
      }
      Files.copy(svdSourceFolderPath.resolve(DEVICE_LIST_FILENAME),        svdOutputFolderPath.resolve(DEVICE_LIST_FILENAME),       StandardCopyOption.REPLACE_EXISTING);
      Files.copy(svdSourceFolderPath.resolve(DEVICE_LIST_SCHEMA_FILENAME), svdOutputFolderPath.resolve(DEVICE_LIST_SCHEMA_FILENAME), StandardCopyOption.REPLACE_EXISTING);
      Files.copy(svdSourceFolderPath.resolve(CMSIS_SCHEMA_FILENAME),       svdOutputFolderPath.resolve(CMSIS_SCHEMA_FILENAME),      StandardCopyOption.REPLACE_EXISTING);
   }

   /*
    * *************************************************************************************************************************************
    * *************************************************************************************************************************************
    */

   /**
    *    Produces multiple header files from multiple device SVD files
    *    Optimisations may be applied to the SVD file before creating the header file.
    *    Header file name is based on device name in SVD file - not the source file name
    *    
    *    sourceFolderPath/*.svd.xml +-> destinationPath/*.h
    *    
    *  @param sourceFolderPath       - Folder containing SVD files (must have .svd.xml extension, otherwise ignored)
    *  @param destinationFolderPath  - Folder to write created header file to
    * @throws IOException 
    */
   public static void createHeaderFiles(Path sourceFolderPath, Path destinationFolderPath, boolean optimise) throws IOException {

      if (Files.exists(destinationFolderPath)) {
         System.err.flush();
         System.err.println("Destination already exists " + destinationFolderPath);
         return;
      }
      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setFreescaleFieldNames(optimise);
      ModeControl.setMapFreescaleCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(false);

      Files.createDirectory(destinationFolderPath);

      DirectoryStream<Path> sourceFolderStream = Files.newDirectoryStream(sourceFolderPath);

      int deviceCount = 5000;

      for (Path svdSourceFile : sourceFolderStream) {
         if (deviceCount-- == 0) {
            break;
         }
         if (Files.isRegularFile(svdSourceFile)) {
            String fileName = svdSourceFile.getFileName().toString();
            if ((onlyFileToProcess != null) && !fileName.matches(onlyFileToProcess)) {
               System.err.println(String.format("\'%s\' <> \'%s\'", fileName, onlyFileToProcess));
               continue;
            }
            if (fileName.endsWith(".svd.xml")) {
               System.err.println("Processing File : \""+fileName+"\"");
               try {
                  // Read device description
                  DevicePeripherals devicePeripherals = new DevicePeripherals(svdSourceFile);

                  if (optimise) {
                     // Optimise peripheral database
                     devicePeripherals.optimise();
                  }
                  devicePeripherals.sortPeripherals();

                  // Create header file
                  Path headerFilePath = destinationFolderPath.resolve(devicePeripherals.getName().toString()+".h");
                  System.err.println("Creating : \""+headerFilePath+"\"");
                  devicePeripherals.writeHeaderFile(headerFilePath);

               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
      }
   }

   /*
    * *************************************************************************************************************************************
    * *************************************************************************************************************************************
    */

   private final static String xmlPreamble = 
         "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"+
               "<!DOCTYPE DeviceList SYSTEM \"DeviceListSchema.dtd\" >\n" +
               "\n"+
               "<root version=\"4.10.6\">\n"+
               "   <description>Lookup table for mapping complete device name to the SVD file that describes the device</description>\n";

   private final static String openDeviceList   = "   <deviceList>\n";
   private static final String deviceEntry      = "      %-30s<svdFileName>%s</svdFileName> </device>\n";
   private final static String closeDeviceList  = "   </deviceList>\n";
   private final static String xmlPostamble     = "</root>\n";

   /**
    * Sorts list of device names
    */
   private static void sortDeviceNames(ArrayList<Pair> usedBy) {
      Collections.sort(usedBy, new Comparator<Pair>() {
         @Override
         public int compare(Pair pair1, Pair pair2) {
            int compare = (pair1.mappedDeviceName.compareTo(pair2.mappedDeviceName));
            if (compare != 0) {
               return compare;
            }
            String s1 = pair1.deviceName;
            String s2 = pair2.deviceName;
            Pattern p1 = Pattern.compile("^(.*[(DX)|(DN)|(FX)|(FN)|Z])(\\d+)(.*)$");
            Pattern p2 = Pattern.compile("^(.*[(DX)|(DN)|(FX)|(FN)|Z])(\\d+)(.*)$");
            Matcher m1 = p1.matcher(s1);
            Matcher m2 = p2.matcher(s2);
            String p1a = m1.replaceAll("$1");
            String p1b = m1.replaceAll("$2");
            String p1c = m1.replaceAll("$3");
            String p2a = m2.replaceAll("$1");
            String p2b = m2.replaceAll("$2");
            String p2c = m2.replaceAll("$3");
            compare = (p1a.compareTo(p2a));
            if (compare != 0) {
               return compare;
            }
            try {
               compare = Integer.parseInt(p1b) - Integer.parseInt(p2b);
               if (compare != 0) {
                  return compare;
               }
            } catch (NumberFormatException e) {
               System.err.println("sortUsedBy() s1 = "+s1+", s2 = "+s2);
               System.err.println("sortUsedBy() P1 = "+p1a+":"+p1b+", p2 = "+p2a+":"+p2b);
               e.printStackTrace();
            }
            return (p1c.compareTo(p2c));
         }
      });
   }

   /**
    * Creates a minimal set of device SVD files by looking for equivalent devices.
    * A separate XML file is created that maps a device to a SVD file (deviceList.xml).
    * Note: The target directory will end up with expanded SVD files!
    * Note: Source directory may contain reduced SVD files.
    * 
    * @param sourceFolderPath       Directory containing SVD files to process (all files ending in ".svd.xml").
    * @param destinationFolderPath  Where to write destination files.
    * 
    * @throws Exception 
    */
   public static void createReducedDeviceList(Path sourceFolderPath, Path destinationFolderPath) throws Exception {

      if (Files.exists(destinationFolderPath)) {
         System.err.flush();
         System.err.println("Destination already exists " + destinationFolderPath);
         return;
      }

      ArrayList<DevicePeripherals> deviceList = new ArrayList<DevicePeripherals>();

      // Set optimisations
      ModeControl.setExtractComplexStructures(false);
      ModeControl.setExtractDerivedPeripherals(false);
      ModeControl.setExtractSimpleRegisterArrays(false);
      ModeControl.setFreescaleFieldNames(false);
      ModeControl.setMapFreescaleCommonNames(false);
      ModeControl.setGenerateFreescaleRegisterMacros(false);
      ModeControl.setRegenerateAddressBlocks(false);
      ModeControl.setExpandDerivedPeripherals(true);
      ModeControl.setExpandDerivedRegisters(false);
      int maxDevices = 500;

      Files.createDirectory(destinationFolderPath);

      DirectoryStream<Path> sourceFolderStream = Files.newDirectoryStream(sourceFolderPath);

      //
      for (Path svdSourceFile : sourceFolderStream) {
         // Create database of all devices
         if (Files.isRegularFile(svdSourceFile)) {
            String fileName = svdSourceFile.getFileName().toString();
            if ((onlyFileToProcess != null) && !fileName.matches(onlyFileToProcess)) {
               System.err.println(String.format("\'%s\' <> \'%s\'", fileName, onlyFileToProcess));
               continue;
            }
            if (fileName.endsWith(".svd.xml")) {
               //               System.err.println("Processing File : \""+svdSourceFile.getName()+"\"");
               try {
                  // Read device description
                  DevicePeripherals device = new DevicePeripherals(svdSourceFile);
                  // Don't optimise as we want flat files for editing/checking
                  //                  device.optimise();
                  boolean foundEquivalent = false;
                  for (DevicePeripherals searchPeripheral : deviceList) {
                     if (searchPeripheral.equivalentStructure(device)) {
                        searchPeripheral.addEquivalentDevice(device.getName());
                        foundEquivalent = true;
                     }
                  }
                  if (!foundEquivalent) {
                     deviceList.add(device);
                     System.err.println("New       "+device.getName());
                  }
                  else {
                     System.err.println("Equivalent "+device.getName());
                  }
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
            if (maxDevices--<0) {
               break;
            }
         }
      }
      System.err.println();


      // Copy devices to target directory (expanded!)
      // Produce sorted list of device->svd file
      // List of copied devices to check for mapping clashes
      HashSet<String> copiedMappedFiles = new HashSet<String>();
      ArrayList<Pair> devicePairList    = new ArrayList<Pair>();

      for (DevicePeripherals device : deviceList) {
         String deviceName                = device.getName();
         String mappedDestinationFileName = deviceName;//DeviceFileList.getMappedSvdName(device.getName());
         if (copiedMappedFiles.contains(mappedDestinationFileName)) {
            throw new Exception(String.format("Mapped name collision %s => %s", deviceName, mappedDestinationFileName));
         }
         // Add reference to self
         devicePairList.add(new Pair(deviceName, mappedDestinationFileName));
         device.setName(mappedDestinationFileName);
         for (String equivalentDevice : device.getEquivalentDevices()) {
            // Add references from other devices
            devicePairList.add(new Pair(equivalentDevice, mappedDestinationFileName));
         }
         device.writeSVD(destinationFolderPath.resolve(mappedDestinationFileName+".svd.xml"));
      }
      sortDeviceNames(devicePairList);

      // Create deviceList.xml
      File deviceListFile = destinationFolderPath.resolve(DEVICE_LIST_FILENAME).toFile();
      PrintWriter writer = null;
      try {
         writer = new PrintWriter(deviceListFile);
         writer.print(xmlPreamble);
         writer.print(openDeviceList);

         for (Pair pair : devicePairList) {
            writer.print(String.format(deviceEntry, "<device name=\""+pair.deviceName+"\">", pair.mappedDeviceName));
         }
         writer.print(closeDeviceList);
         writer.print(xmlPostamble);
      } catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         if (writer != null) {
            writer.close();
         }
      }
      Files.copy(MAIN_FOLDER.getParent().resolve(CMSIS_SCHEMA_FILENAME),       destinationFolderPath.resolve(CMSIS_SCHEMA_FILENAME),       StandardCopyOption.REPLACE_EXISTING);
      Files.copy(MAIN_FOLDER.getParent().resolve(DEVICE_LIST_SCHEMA_FILENAME),  destinationFolderPath.resolve(DEVICE_LIST_SCHEMA_FILENAME),  StandardCopyOption.REPLACE_EXISTING);
   }

   /**
    * Tests access to reduced SVD file set (using deviceList.xml)
    * 
    * @param deviceListPath
    * @throws Exception
    */
   static void checkDeviceList(Path deviceListPath) throws Exception {

      DeviceFileList deviceFileList = new DeviceFileList(deviceListPath);
      Path svdFilename = deviceFileList.getSvdFilename("MK20DX128M5");
      System.err.println(String.format("Test : %-20s => \"%s\"", "MK20DX128M5", svdFilename));
      System.err.println(String.format("Test : %-20s => \"%s\"", "MK10DN64",    deviceFileList.getSvdFilename("MK10DN64")));
   }

   static void removeDirectoryTree(Path directoryPath) {
      try {
         Files.walkFileTree(directoryPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
               //               System.err.println("Deleting file: " + file);
               Files.delete(file);
               return FileVisitResult.CONTINUE;
            }

         });
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Produces a set of header files from SVD files (based on deviceList.xml)
    * Each header file represent a device family with similar peripherals
    * 
    * @param sourceFolderPath       Should contain "DeviceList.xml" file
    * @param destinationFolderPath  Where to write header files to
    * @param removeFolder           Destination folder will be deleted (otherwise files just overwrite/accumulate)
    * 
    * @throws Exception
    */
   static void createHeaderFilesFromList(Path sourceFolderPath, Path destinationFolderPath, boolean removeFolder) throws Exception {

      if (Files.exists(destinationFolderPath)) {
         if (!removeFolder) {
            System.err.println("Destination already exists " + destinationFolderPath);
         }
         else {
            System.err.println("Destination already exists -  deleting " + destinationFolderPath);
            removeDirectoryTree(destinationFolderPath);
         }
      }
      // Set options
      ModeControl.setFreescaleFieldNames(true);
      ModeControl.setMapFreescaleCommonNames(true);
      ModeControl.setGenerateFreescaleRegisterMacros(false);
      ModeControl.setUseShiftsInFieldMacros(true);
      
      // Don't optimise
      ModeControl.setExtractComplexStructures(false);
      ModeControl.setExtractDerivedPeripherals(false);
      ModeControl.setExtractSimpleRegisterArrays(false);
      ModeControl.setRegenerateAddressBlocks(false);

      destinationFolderPath.toFile().mkdir();

      DeviceFileList deviceFileList = new DeviceFileList(sourceFolderPath.resolve(DEVICE_LIST_FILENAME));
      
      // Get full list of devices
      ArrayList<Pair> list = deviceFileList.getArrayList();

      // Map of already copied files to prevent multiple copying
      HashSet<String> copiedFiles = new HashSet<String>();

      for (int index = 0; index < list.size(); index++) {
         Pair pair = list.get(index);
         if ((onlyFileToProcess != null) && !pair.deviceName.matches(onlyFileToProcess)) {
            continue;
         }
         System.err.println("Processing File : \""+pair.deviceName+"\"");
         try {
            // Don't produce the same file!
            if (copiedFiles.contains(pair.mappedDeviceName)) {
               continue;
            }
            copiedFiles.add(pair.mappedDeviceName);

            // Read device description
            DevicePeripherals devicePeripherals = new DevicePeripherals(sourceFolderPath.resolve(pair.mappedDeviceName+".svd.xml"));

            devicePeripherals.sortPeripherals();

            devicePeripherals.setName(pair.mappedDeviceName);

            devicePeripherals.addEquivalentDevice(pair.deviceName);
            for (int index2 = index+1; index2 < list.size(); index2++) {
               if (pair.mappedDeviceName.equalsIgnoreCase(list.get(index2).mappedDeviceName)) {
                  devicePeripherals.addEquivalentDevice(list.get(index2).deviceName);
               }
            }
            // Create header file
            Path headerFilePath = destinationFolderPath.resolve(devicePeripherals.getName()+".h");
            System.err.println("Creating : \""+headerFilePath+"\"");
            devicePeripherals.writeHeaderFile(headerFilePath);

         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Produces expanded (single file per target) SVD files (based on deviceList.xml)
    * 
    * @param sourceFolderPath       Should contain "DeviceList.xml" file
    * @param destinationFolderPath  Where to write expanded files to
    * 
    * @throws Exception
    */
   static void createExpandedSvdFilesFromList(Path sourceFolderPath, Path destinationFolderPath, boolean optimise) throws Exception {

      if (Files.exists(destinationFolderPath)) {
         System.err.flush();
         System.err.println("Destination already exists " + destinationFolderPath);
         return;
      }
      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setFreescaleFieldNames(optimise);
      ModeControl.setMapFreescaleCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(optimise);
//      ModeControl.setExtractCommonPrefix(optimise);

      destinationFolderPath.toFile().mkdir();

      DeviceFileList deviceFileList = new DeviceFileList(sourceFolderPath.resolve(DEVICE_LIST_FILENAME));
      ArrayList<Pair> list = deviceFileList.getArrayList();

      for (Pair pair : list) {
         if ((onlyFileToProcess != null) && !pair.deviceName.matches(onlyFileToProcess)) {
            continue;
         }
         System.err.println("Processing File : \""+pair.mappedDeviceName+"\"");
         try {
            // Read device description
            DevicePeripherals devicePeripherals = new DevicePeripherals(sourceFolderPath.resolve(pair.mappedDeviceName+".svd.xml"));

            // Optimise peripheral database
            devicePeripherals.optimise();

            devicePeripherals.setName(pair.deviceName);

            // Create SVD file
            Path svdFilePath = destinationFolderPath.resolve(devicePeripherals.getName()+".svd.xml");
            System.err.println("Creating : \""+svdFilePath+"\"");
            devicePeripherals.writeSVD(svdFilePath);

         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   /*
    * *************************************************************************************************************************************
    * *************************************************************************************************************************************
    */

   /**
    * @param args
    */
   public static void main(String[] args) {
      System.err.println("Starting");
//    onlyFileToProcess = "^MCF51.*";
//       onlyFileToProcess = "^MK2.*";
//       onlyFileToProcess = "^MKL.*";
//       onlyFileToProcess = "^MK10DX128M7*";
//       onlyFileToProcess = "^MK20DX128M5*";
//       onlyFileToProcess = "^MCF51J.*";
//       onlyFileToProcess = "^MK10DX128M5$";
//       onlyFileToProcess = "^(MKM).*";
//       onlyFileToProcess = "^MKE.*";
//      onlyFileToProcess = "^MK.*";

      try {
         // Generate merged version of SVD files for testing (should be unchanging eventually)
         ModeControl.setExpandDerivedRegisters(false);
//         ModeControl.setFlattenArrays(true);
//         mergeFiles(usbdmFolder,     usbdmCheckFolder, true, true);
//         mergeFiles(freescaleFolder, freescaleCheckFolder, true, false);
         // Create Header files from SVD
         createHeaderFilesFromList(usbdmFolder, headerReducedMergedOptimisedManualFolder, false);
      } catch (Exception e) {
         e.printStackTrace();
      }
      System.err.flush();
      System.err.println("Done");
   }
}
