package Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;

import net.sourceforge.usbdm.peripheralDatabase.DeviceFileList;
import net.sourceforge.usbdm.peripheralDatabase.DeviceFileList.DeviceSvdInfo;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.ModeControl;
import net.sourceforge.usbdm.peripheralDatabase.PeripheralDatabaseMerger;

public class CreatePeripheralDatabase {
   private static final  Path PACKAGE_FOLDER    = Paths.get("../../usbdm-eclipse-makefiles-build/PackageFiles");
   private static final  Path MAIN_FOLDER       = PACKAGE_FOLDER.resolve("Stationery/Device.SVD");
   //   @SuppressWarnings("unused")
   //   private static final  Path headerReducedMergedOptimisedManualFolder     = PACKAGE_FOLDER.resolve("Stationery/Project_Headers");
   //
   //   private static final  Path usbdmFolder            = MAIN_FOLDER.resolve("Internal");
   //   //   @SuppressWarnings("unused")
   //   private static final  Path usbdmFolder_Check      = MAIN_FOLDER.resolve("Internal.Check");
   //
   //   private static final  Path usbdmHeaderFolder_Check   = MAIN_FOLDER.resolve("Internal_header.Check");
   //
   //   @SuppressWarnings("unused")
   //   private static final  Path freescaleFolder        = MAIN_FOLDER.resolve("Freescale");
   //   @SuppressWarnings("unused")
   //   private static final  Path freescaleFolder_Check   = MAIN_FOLDER.resolve("Freescale.Check");

   private static final  String DEVICE_LIST_FILENAME        = "DeviceList.xml";
   private static final  String CMSIS_SCHEMA_FILENAME       = "CMSIS-SVD_Schema_1_1.xsd";
   private static final  String DEVICE_LIST_SCHEMA_FILENAME = "DeviceListSchema.dtd";

   private static String firstFileToProcess = null;
   private static String firstFileToReject  = null;
   private static String filesToReject      = null;

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

   /**
    * Class to keep track of files to process
    */
   static class FileFilter {
      private Pattern startPattern;
      private Pattern endPattern;
      private boolean include;

      /**
       * Constructor
       * 
       * @param startPattern Regex matching first file to process
       * @param endPattern   Regex matching first file not to process
       * 
       * NOTES:
       *    if startPattern == null files are processed from first.
       *    if endPattern   == null then files are processed until end
       */
      public FileFilter(String startPattern, String endPattern) {
         if (startPattern == null) {
            this.startPattern = null;
            include = true;
         }
         else {
            this.startPattern = Pattern.compile(startPattern);
            include = false;
         }
         if (endPattern == null) {
            this.endPattern   = null;
         }
         else {
            this.endPattern   = Pattern.compile(endPattern);
         }
      }
      public boolean skipFile(String filename) {
         if (include) {
            if ((endPattern != null) && endPattern.matcher(filename).matches()) {
               include = false;
               endPattern = null;
            }
         }
         else {
            if ((startPattern != null) && startPattern.matcher(filename).matches()) {
               include = true;
               startPattern = null;
            }
         }
         //         if (!include) {
         //            System.err.println("Skipping " + filename);
         //         }
         return !include;
      }
   }
   
   static Path[] sortedFileList(Path folderPath, final DirectoryStream.Filter<Path> filter) throws IOException {
      
      DirectoryStream<Path> folderStream = null;
      if (filter == null) {
         folderStream = Files.newDirectoryStream(folderPath.toAbsolutePath());
      }
      else {
         folderStream = Files.newDirectoryStream(folderPath.toAbsolutePath(), filter);
      }
      
      ArrayList<Path> unsortedPaths =  new ArrayList<Path>();
      for (Path filePath : folderStream) {
         unsortedPaths.add(filePath);
      }
      Path[] sortedPaths = new Path[unsortedPaths.size()];
      sortedPaths = unsortedPaths.toArray(sortedPaths);
      
      Arrays.sort(sortedPaths, new Comparator<Path>() {

         @Override
         public int compare(Path arg0, Path arg1) {
            String f0 = arg0.getFileName().toString();
            String f1 = arg1.getFileName().toString();
            Pattern deviceNamePattern = Pattern.compile("([a-z|A-Z]*)(.*)");
            Matcher m0 = deviceNamePattern.matcher(f0);
            Matcher m1 = deviceNamePattern.matcher(f1);
            int res = 0;
//          System.err.println("m0.g1="+m0.group(1)+", m1.g1=" + m1.group(1));
            if (!m0.matches() || !m1.matches()) {
               System.err.println("no match for" + f0 + "," +  f1);
               res = f0.compareTo(f1);
            }
            else {
               res = m0.group(1).compareTo(m1.group(1));
               if (res == 0) {
                  res = m0.group(2).compareTo(m1.group(2));
               }
            }
            return res;
         }
         
      });
      return sortedPaths;
   }
   
   static void mergeFiles(Path svdSourceFolderPath, final DirectoryStream.Filter<Path> directoryFilter, PeripheralDatabaseMerger merger) throws Exception {
      FileFilter fileFilter = new FileFilter(firstFileToProcess, firstFileToReject);
      Pattern rejectPattern = null;
      if (filesToReject != null) {
         rejectPattern = Pattern.compile(filesToReject);
      }
      int deviceCount = 500;
      
      Path[] files = sortedFileList(svdSourceFolderPath, directoryFilter);
      
      for (Path filePath : files) {
         if (deviceCount-- == 0) {
            break;
         }
         if (Files.isRegularFile(filePath)) {
            String fileName = filePath.getFileName().toString();
            if (fileFilter.skipFile(fileName)) {
               continue;
            }
            if ((rejectPattern != null) && rejectPattern.matcher(fileName).matches()) {
               continue;
            }
            if (fileName.endsWith(".svd.xml") || (fileName.endsWith(".svd"))) {
               System.out.println("Merging SVD file : "+filePath.getFileName());

               // Read device peripheral database
               DevicePeripherals devicePeripherals = new DevicePeripherals(filePath);
               devicePeripherals.optimise();
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
    *    @param removeFolder        - Delete destination folder
    * 
    *    @throws IOException
    * 
    */
   public static void mergeFiles(Path svdSourceFolderPath, Path svdOutputFolderPath, boolean removeFolder) throws IOException {

      if (!Files.isDirectory(svdSourceFolderPath)) {
         System.err.println("Source doesn't exist " + svdSourceFolderPath.toAbsolutePath()+"\"\n");
         return;
      }

      System.err.println("Processing files from : \""+svdSourceFolderPath.getFileName()+"\"\n");
      
      if (Files.exists(svdOutputFolderPath)) {
         if (!removeFolder) {
            System.err.println("Destination already exists " + svdOutputFolderPath.toString());
         }
         else {
            System.err.println("Destination already exists -  deleting \'" + svdOutputFolderPath.getFileName() + "\'\n");
            removeDirectoryTree(svdOutputFolderPath);
         }
      }
      if (!Files.exists(svdOutputFolderPath)) {
         Files.createDirectory(svdOutputFolderPath);
      }

      System.err.println("Writing SVD files to  : \""+svdOutputFolderPath.getFileName()+"\"");

      PeripheralDatabaseMerger merger = new PeripheralDatabaseMerger();

      merger.setXmlRootPath(svdOutputFolderPath.toFile());

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
         mergeFiles(svdSourceFolderPath, nonColdfireDirectoryFilter, merger);
         mergeFiles(svdSourceFolderPath, coldfireDirectoryFilter,    merger);
         merger.writePeripheralsToSVD();
         merger.writeVectorTablesToSVD();
      } catch (Exception e) {
         e.printStackTrace();
      }
      if (Files.exists(svdSourceFolderPath.resolve(DEVICE_LIST_FILENAME), LinkOption.NOFOLLOW_LINKS)) {
         Files.copy(svdSourceFolderPath.resolve(DEVICE_LIST_FILENAME),        svdOutputFolderPath.resolve(DEVICE_LIST_FILENAME),       StandardCopyOption.REPLACE_EXISTING);
         Files.copy(svdSourceFolderPath.resolve(DEVICE_LIST_SCHEMA_FILENAME), svdOutputFolderPath.resolve(DEVICE_LIST_SCHEMA_FILENAME), StandardCopyOption.REPLACE_EXISTING);
         Files.copy(svdSourceFolderPath.resolve(CMSIS_SCHEMA_FILENAME),       svdOutputFolderPath.resolve(CMSIS_SCHEMA_FILENAME),      StandardCopyOption.REPLACE_EXISTING);
      }
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
    *  @param removeFolder           - Delete destination folder
    * @throws Exception
    */
   public static void createHeaderFiles(Path sourceFolderPath, Path destinationFolderPath, boolean removeFolder) throws Exception {
      FileFilter fileFilter = new FileFilter(firstFileToProcess, firstFileToReject);

      if (Files.exists(destinationFolderPath)) {
         if (!removeFolder) {
            System.err.println("Destination already exists \"" + destinationFolderPath.getFileName()+"\"\n");
         }
         else {
            System.err.println("Destination already exists -  deleting \"" + destinationFolderPath.getFileName()+"\"\n");
            removeDirectoryTree(destinationFolderPath);
         }
      }
      if (!Files.exists(destinationFolderPath)) {
         Files.createDirectory(destinationFolderPath);
      }

      System.err.println("\nWriting header files to  : \""+destinationFolderPath.getFileName()+"\"");

      Path[] files = sortedFileList(sourceFolderPath, null);
      
      for (Path svdSourceFile : files) {
         if (Files.isRegularFile(svdSourceFile)) {
            String fileName = svdSourceFile.getFileName().toString();
            if (fileFilter.skipFile(fileName)) {
               continue;
            }
            if (fileName.endsWith(".svd.xml")) {
               // Read device description
               DevicePeripherals devicePeripherals = new DevicePeripherals(svdSourceFile);

               // Optimise peripheral database
               devicePeripherals.optimise();
               devicePeripherals.sortPeripherals();

               // Create header file
               Path headerFilePath = destinationFolderPath.resolve(devicePeripherals.getName().toString()+".h");
               
               System.out.print(String.format("Processing File : %-20s => %-20s\n", fileName, headerFilePath.getFileName()));
               
               devicePeripherals.writeHeaderFile(headerFilePath);
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
   private static void sortDeviceNames(ArrayList<Pair> devicePairList) {
      Collections.sort(devicePairList, new Comparator<Pair>() {
         @Override
         public int compare(Pair pair1, Pair pair2) {
            int compare = (pair1.fileName.compareTo(pair2.fileName));
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

   static class Pair {
      String deviceName;
      String fileName;
      
      Pair(String deviceName, String fileName) {
         this.deviceName = deviceName;
         this.fileName   = fileName;
      }
   };
   
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
      FileFilter fileFilter = new FileFilter(firstFileToProcess, firstFileToReject);

      if (Files.exists(destinationFolderPath)) {
         System.err.println("Destination already exists -  deleting \"" + destinationFolderPath.getFileName()+"\"\n");
         removeDirectoryTree(destinationFolderPath);
      }
      if (!Files.exists(destinationFolderPath)) {
         Files.createDirectory(destinationFolderPath);
      }
      ArrayList<DevicePeripherals> deviceList = new ArrayList<DevicePeripherals>();

      // Set optimizations
      ModeControl.setExtractComplexStructures(false);
      ModeControl.setExtractDerivedPeripherals(false);
      ModeControl.setExtractSimpleRegisterArrays(false);
      ModeControl.setMapFreescalePeriperalCommonNames(false);
      ModeControl.setGenerateFreescaleRegisterMacros(false);
      ModeControl.setRegenerateAddressBlocks(false);
      ModeControl.setExpandDerivedPeripherals(true);
      ModeControl.setExpandDerivedRegisters(false);
      int maxDevices = 500;

      Path[] files = sortedFileList(sourceFolderPath, null);

      for (Path svdSourceFile : files) {
         // Create database of all devices
         if (Files.isRegularFile(svdSourceFile)) {
            String fileName = svdSourceFile.getFileName().toString();
            if (fileFilter.skipFile(fileName)) {
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
      ArrayList<Pair> devicePairList    = new ArrayList<Pair>();

      for (DevicePeripherals device : deviceList) {
         String deviceName = device.getName();
         // Add reference to self
         for (String equivalentDevice : device.getEquivalentDevices()) {
            // Add references from other devices
            devicePairList.add(new Pair(equivalentDevice, deviceName));
         }
         device.writeSVD(destinationFolderPath.resolve(deviceName+".svd.xml"));
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
            writer.print(String.format(deviceEntry, "<device name=\""+pair.deviceName+"\">", pair.fileName));
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
      Files.copy(MAIN_FOLDER.resolve(CMSIS_SCHEMA_FILENAME),       destinationFolderPath.resolve(CMSIS_SCHEMA_FILENAME),       StandardCopyOption.REPLACE_EXISTING);
      Files.copy(MAIN_FOLDER.resolve(DEVICE_LIST_SCHEMA_FILENAME), destinationFolderPath.resolve(DEVICE_LIST_SCHEMA_FILENAME), StandardCopyOption.REPLACE_EXISTING);
   }

   /**
    * Delete directory
    * 
    * @param directoryPath
    */
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
      FileFilter fileFilter = new FileFilter(firstFileToProcess, firstFileToReject);

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
      ModeControl.setMapFreescalePeriperalCommonNames(true);
      ModeControl.setGenerateFreescaleRegisterMacros(false);
      ModeControl.setUseNamesInFieldMacros(true);

      // Don't optimise
      ModeControl.setExtractComplexStructures(false);
      ModeControl.setExtractDerivedPeripherals(false);
      ModeControl.setExtractSimpleRegisterArrays(false);
      ModeControl.setRegenerateAddressBlocks(false);

      destinationFolderPath.toFile().mkdir();

      DeviceFileList deviceFileList = new DeviceFileList(sourceFolderPath.resolve(DEVICE_LIST_FILENAME));

      // Get full list of devices
      ArrayList<DeviceSvdInfo> list = deviceFileList.getArrayList();

      // Map of already copied files to prevent multiple copying
      HashSet<String> copiedFiles = new HashSet<String>();

      for (int index = 0; index < list.size(); index++) {
         DeviceSvdInfo pair = list.get(index);
         if (fileFilter.skipFile(pair.deviceName)) {
            continue;
         }
         System.err.println("Processing File : \""+pair.deviceName+"\"");
         try {
            // Don't produce the same file!
            if (copiedFiles.contains(pair.svdName)) {
               continue;
            }
            copiedFiles.add(pair.svdName);

            // Read device description
            DevicePeripherals devicePeripherals = new DevicePeripherals(sourceFolderPath.resolve(pair.svdName+".svd.xml"));

            devicePeripherals.sortPeripherals();

            devicePeripherals.setName(pair.svdName);

            devicePeripherals.addEquivalentDevice(pair.deviceName);
            for (int index2 = index+1; index2 < list.size(); index2++) {
               if (pair.svdName.equalsIgnoreCase(list.get(index2).svdName)) {
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
      FileFilter fileFilter = new FileFilter(firstFileToProcess, firstFileToReject);

      if (Files.exists(destinationFolderPath)) {
         System.err.println("Destination already exists -  deleting \"" + destinationFolderPath.getFileName()+"\"\n");
         removeDirectoryTree(destinationFolderPath);
      }
      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setMapFreescalePeriperalCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(optimise);
      //      ModeControl.setExtractCommonPrefix(optimise);

      destinationFolderPath.toFile().mkdir();

      DeviceFileList deviceFileList = new DeviceFileList(sourceFolderPath.resolve(DEVICE_LIST_FILENAME));
      ArrayList<DeviceSvdInfo> list = deviceFileList.getArrayList();

      for (DeviceSvdInfo pair : list) {
         if (fileFilter.skipFile(pair.deviceName)) {
            continue;
         }
         System.err.println("Processing File : \""+pair.svdName+"\"");
         try {
            // Read device description
            DevicePeripherals devicePeripherals = new DevicePeripherals(sourceFolderPath.resolve(pair.svdName+".svd.xml"));

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
    * Generates new SVD and header file for the usual minor changes.
    * This includes merging peripherals
    * 
    * Source "Internal"
    * Destinations "Internal.Check", "Internal_header.Check"
    */
   static void doUsualRegeneration() {
      final  Path usbdmFolder               = MAIN_FOLDER.resolve("Internal");
      final  Path usbdmFolder_Check         = MAIN_FOLDER.resolve("Internal.Check");
      final  Path usbdmHeaderFolder_Check   = MAIN_FOLDER.resolve("InternalHeader.Check");

      try {
         // Generate merged version of SVD files for testing (should be unchanging)
         ModeControl.setRegenerateAddressBlocks(true);
         ModeControl.setExtractSimilarFields(true);
         ModeControl.setExtractComplexStructures(true);
         ModeControl.setExtractDerivedPeripherals(true);
         ModeControl.setExtractSimpleRegisterArrays(true);
         ModeControl.setMapFreescalePeriperalCommonNames(true);
         ModeControl.setFoldRegisters(true);
         ModeControl.setCollectVectors(true);
         
         mergeFiles(usbdmFolder,     usbdmFolder_Check, true);

         // Turn of optimisation when generating header files
         ModeControl.setRegenerateAddressBlocks(false);
         ModeControl.setExtractSimilarFields(false);
         ModeControl.setExtractComplexStructures(false);
         ModeControl.setExtractDerivedPeripherals(false);
         ModeControl.setExtractSimpleRegisterArrays(false);
         ModeControl.setMapFreescalePeriperalCommonNames(false);
         ModeControl.setFoldRegisters(false);
         ModeControl.setUseNamesInFieldMacros(true);

         // Header file generation options (use defaults)
//         ModeControl.setGenerateFreescaleRegisterMacros(false);
//         ModeControl.setFreescaleFieldNames(true);
//         ModeControl.setUseShiftsInFieldMacros(false);
//         ModeControl.setUseBytePadding(true);
         createHeaderFiles(usbdmFolder, usbdmHeaderFolder_Check, true);

      } catch (Exception e) {
         e.printStackTrace();
      }
      System.err.flush();
      System.err.println("Done");

   }

   /**
    * Generates new SVD and header file from original unmerged SVD files
    * This includes merging peripherals
    * 
    * Source "Raw"
    * Destinations "Raw.Check", "Raw_header.Check"
    */
   static void doInitialRegeneration() {
      final  Path sourceFolder                 = MAIN_FOLDER.resolve("Raw");
      final  Path usbdmFolder_1st_Stage        = MAIN_FOLDER.resolve("Raw.1st_Stage_Commoned");
      final  Path usbdmFolder_2nd_Stage        = MAIN_FOLDER.resolve("Raw.2nd_Stage_Folded");
      final  Path usbdmHeaderFolder_2nd_Stage  = MAIN_FOLDER.resolve("Raw_header.2nd_Stage");
      final  Path usbdmFolder_3rd_Stage        = MAIN_FOLDER.resolve("Raw.3rd_Stage_Flattened");

      try {
         // Generate merged version of SVD files for testing (should be unchanging)
//         ModeControl.setRegenerateAddressBlocks(true);
         ModeControl.setExtractSimilarFields(false);
         ModeControl.setExtractComplexStructures(false);
         ModeControl.setExtractDerivedPeripherals(true);
         ModeControl.setExtractSimpleRegisterArrays(false);
         ModeControl.setMapFreescalePeriperalCommonNames(true);
         ModeControl.setFoldRegisters(false);
         ModeControl.setFlattenArrays(true);
         ModeControl.setCollectVectors(true);
         mergeFiles(sourceFolder,     usbdmFolder_1st_Stage, true);
         
         ModeControl.setExtractSimilarFields(true);
         ModeControl.setExtractComplexStructures(true);
         ModeControl.setExtractDerivedPeripherals(true);
         ModeControl.setExtractSimpleRegisterArrays(true);
         ModeControl.setMapFreescalePeriperalCommonNames(true);
         ModeControl.setFoldRegisters(true);
         ModeControl.setFlattenArrays(false);
         mergeFiles(usbdmFolder_1st_Stage,     usbdmFolder_2nd_Stage, true);
         
         // Turn off optimisation when generating header files
         ModeControl.setRegenerateAddressBlocks(false);
         ModeControl.setExtractSimilarFields(false);
         ModeControl.setExtractComplexStructures(false);
         ModeControl.setExtractDerivedPeripherals(false);
         ModeControl.setExtractSimpleRegisterArrays(false);
         ModeControl.setMapFreescalePeriperalCommonNames(false);
         ModeControl.setFoldRegisters(false);

         // Header file generation options
         ModeControl.setGenerateFreescaleRegisterMacros(false);
         ModeControl.setFreescaleFieldNames(true);
         ModeControl.setUseNamesInFieldMacros(true);
         ModeControl.setUseBytePadding(true);
         createHeaderFiles(usbdmFolder_2nd_Stage, usbdmHeaderFolder_2nd_Stage, true);

         createReducedDeviceList(usbdmFolder_2nd_Stage,     usbdmFolder_3rd_Stage);
         
      } catch (Exception e) {
         e.printStackTrace();
      }
      System.err.flush();
      System.err.println("Done");

   }

   static void doFactoring() {
      //    @SuppressWarnings("unused")
      //    private static final  Path headerReducedMergedOptimisedManualFolder     = PACKAGE_FOLDER.resolve("Stationery/Project_Headers");
      //
      final  Path usbdmFolder          = MAIN_FOLDER.resolve("Internal");
      final  Path stage1Folder         = MAIN_FOLDER.resolve("1.stage1Folder");
      final  Path stage2Folder         = MAIN_FOLDER.resolve("2.stage2Folder");
      final  Path stage3Folder         = MAIN_FOLDER.resolve("3.stage3Folder");
      final  Path resultFolder         = MAIN_FOLDER.resolve("9.resultFolder");
      final  Path resultHeaderFolder   = MAIN_FOLDER.resolve("9.resultHeaderFolder");

      try {
         // Generate merged version of SVD files for testing (should be unchanging eventually)
         //         ModeControl.setExpandDerivedRegisters(false);
         //         ModeControl.setFlattenArrays(true);
         //         ModeControl.setRenameSimSources(true);
         //         mergeFiles(freescaleFolder, freescaleFolder_Check, true, true);
         // Create Header files from SVD
         //         createHeaderFilesFromList(usbdmFolder, headerReducedMergedOptimisedManualFolder, false);
         //         createHeaderFiles(freescaleFolder,       freescaleHeaderFolder,       false, true);
         //         createHeaderFiles(freescaleFolder_Check, freescaleHeaderFolder_Check, false, true);

         ModeControl.setCollectVectors(true);

         ModeControl.setExtractSimilarFields(true);
         ModeControl.setExtractDerivedPeripherals(true);
         ModeControl.setMapFreescalePeriperalCommonNames(true);
         ModeControl.setFoldRegisters(true);
         ModeControl.setRenameSimSources(true);
         ModeControl.setMapRegisterNames(true);
         
         // Regenerate with expanded registers to allow merging of overlapping STRUCTS
         ModeControl.setFlattenArrays(true);
         mergeFiles(usbdmFolder, stage1Folder, true);

         // Merge overlapping STRUCTS
         ModeControl.setExtractComplexStructures(true);
         
         // Create Simple arrays
         ModeControl.setFlattenArrays(false);
         ModeControl.setIgnoreResetValuesInEquivalence(true);
         ModeControl.setIgnoreAccessTypeInEquivalence(true);
         ModeControl.setExtractSimpleRegisterArrays(true);
         mergeFiles(stage1Folder, stage2Folder, true);

         // Update the memory blocks
         ModeControl.setRegenerateAddressBlocks(true);
         mergeFiles(stage2Folder, stage3Folder, true);

         // Generate the header files
         mergeFiles(stage3Folder, resultFolder, true);

         // Turn off further optimisations when loading SVD
         ModeControl.setRegenerateAddressBlocks(false);
         ModeControl.setExtractSimilarFields(false);
         ModeControl.setExtractComplexStructures(false);
         ModeControl.setExtractDerivedPeripherals(false);
         ModeControl.setExtractSimpleRegisterArrays(false);
         ModeControl.setMapFreescalePeriperalCommonNames(false);
         ModeControl.setFoldRegisters(false);

         // Header file generation options
         ModeControl.setFreescaleFieldNames(true);
         ModeControl.setGenerateFreescaleRegisterMacros(false);
         ModeControl.setUseNamesInFieldMacros(true);
         ModeControl.setUseBytePadding(true);
         createHeaderFiles(stage3Folder, resultHeaderFolder, false);

      } catch (Exception e) {
         e.printStackTrace();
      }
      System.err.flush();
      System.err.println("Done");
   }
   
   /**
    * Generates new SVD and header file for the usual minor changes.
    * This includes merging peripherals
    * 
    * Source "Internal"
    * Destinations "Internal.Check", "Internal_header.Check"
    */
   static void doHeaderFiles() {
      final  Path usbdmFolder               = MAIN_FOLDER.resolve("Internal");
      final  Path usbdmHeaderFolder_Check   = MAIN_FOLDER.resolve("InternalHeader.Check");

      try {
         // Turn of optimisation when generating header files
         ModeControl.setRegenerateAddressBlocks(false);
         ModeControl.setExtractSimilarFields(false);
         ModeControl.setExtractComplexStructures(false);
         ModeControl.setExtractDerivedPeripherals(false);
         ModeControl.setExtractSimpleRegisterArrays(false);
         ModeControl.setMapFreescalePeriperalCommonNames(false);
         ModeControl.setFoldRegisters(false);

         // Header file generation options
         ModeControl.setGenerateFreescaleRegisterMacros(false);
         ModeControl.setFreescaleFieldNames(true);
         ModeControl.setUseNamesInFieldMacros(true);
         ModeControl.setUseBytePadding(true);
         createHeaderFiles(usbdmFolder, usbdmHeaderFolder_Check, true);

      } catch (Exception e) {
         e.printStackTrace();
      }
      System.err.flush();
      System.err.println("Done");
   }

   /**
    * @param args
    * @throws IOException
    */
   public static void main(String[] args) throws IOException {
      
//    firstFileToProcess = ("^STM*.*");
//    firstFileToReject  = ("^STM*");
//    firstFileToProcess = ("^MK22F51212.*");
//    firstFileToReject  = ("^MK22FA.*");

//    firstFileToProcess = ("^MKL82Z7.*");
    System.err.println("Main Folder : \""+MAIN_FOLDER.toRealPath()+"\"\n");

//      doHeaderFiles();
//      doInitialRegeneration();
      doUsualRegeneration();
//      try {
//         createReducedDeviceList(MAIN_FOLDER.resolve("Raw"), MAIN_FOLDER.resolve("Raw.expanded"));
//      } catch (Exception e) {
//         e.printStackTrace();
//      }
   }
}
