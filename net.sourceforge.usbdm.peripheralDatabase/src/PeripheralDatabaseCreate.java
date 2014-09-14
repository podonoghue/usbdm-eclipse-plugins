import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
import net.sourceforge.usbdm.peripheralDatabase.SVD_XML_Parser;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class PeripheralDatabaseCreate {

   static IPath mainFolder                                        = new Path("C:/Users/podonoghue/Development/USBDM/Arm_Devices/Generated");
   static IPath svdRaw                                            = mainFolder.append("svdRaw");
                                                                  
   static IPath headerFolder                                      = mainFolder.append("Header");
   static IPath headerReducedMergedOptimisedFolder                = mainFolder.append("HeaderReducedMergedOptimised");
   static IPath headerReducedMergedOptimisedManualFolder          = mainFolder.append("HeaderReducedMergedOptimisedManual");
   static IPath headerReducedMergedOptimisedManualIterationFolder = mainFolder.append("HeaderReducedMergedOptimisedManualIteration");
   static IPath headerReducedMergedOptimisedManualExpandedFolder  = mainFolder.append("HeaderReducedMergedOptimisedManualExpanded");
   
   static IPath svdReducedFolder                                  = mainFolder.append("svdReduced");
   static IPath svdReducedMergedFolder                            = mainFolder.append("svdReducedMerged");
   static IPath svdReducedMergedOptimisedFolder                   = mainFolder.append("svdReducedMergedOptimised");
   static IPath svdReducedMergedOptimisedManualFolder             = mainFolder.append("svdReducedMergedOptimisedManual");
   static IPath svdReducedMergedOptimisedManualCheckFolder        = mainFolder.append("svdReducedMergedOptimisedManual.Check");
   static IPath svdReducedMergedOptimisedManualExpandedFolder     = mainFolder.append("svdReducedMergedOptimisedManualExpanded");
                                                                  
   static IPath freescaleSvdFolder                                = mainFolder.append("Freescale");
   static IPath freescaleSvdTestFolder                            = mainFolder.append("FreescaleTest");
   static IPath freescaleHeaderFilesFolder                        = mainFolder.append("FreescaleHeaderFiles");
                                                                  
   static IPath stmicroSvdFolder                                  = mainFolder.append("STMicro");
   static IPath stmicroSvdExpandedFolder                          = mainFolder.append("STMicroExpanded");
   static IPath stmicroSvdMergedFolder                            = mainFolder.append("STMicroMerged");
   static IPath stmicroSvdMerged2Folder                           = mainFolder.append("STMicroMerged2");
   static IPath stMicroHeaderFilesFolder                          = mainFolder.append("STMicroHeaderFiles");
                                                                  
   static IPath freescaleFolder                                   = mainFolder.append("Freescale");
   static IPath freescaleSortedFolder                             = mainFolder.append("FreescaleSorted");
   static IPath freescaleCommonFolder                             = mainFolder.append("FreescaleCommon");
   static IPath freescaleReducedFolder                            = mainFolder.append("FreescaleReduced");

   
   static final String deviceListFilename       = "DeviceList.xml";
   static final String cmsisSchemaFilename      = "CMSIS-SVD_Schema_1_1.xsd";
   static final String deviceListSchemaFilename = "DeviceListSchema.dtd";

   static final String onlyFileToProcess = null;
// static final String onlyFileToProcess = "^(MKM).*";
//   static final String onlyFileToProcess = "^(MKE|MKL).*(64).*";
//   static final String onlyFileToProcess = "^(MKE).*";
//   static final String onlyFileToProcess = "^(MK[26][42]).*";
// static final String onlyFileToProcess = "^(MK20).*";
//    static final String onlyFileToProcess = "^(MK.4).*";
// static final String onlyFileToProcess = "^(STM).*";
//   static final String onlyFileToProcess = "^(MK22).*";
   
//   static final String onlyFileToProcess = "MK20D5.svd.xml";

   static void copyFile(IPath source, IPath destination) throws IOException {
      System.out.println("Copying "+source.toOSString()+" -> \n        "+destination.toOSString());
      Files.copy(new java.io.File(source.toPortableString()).toPath(), 
                 new java.io.File(destination.toPortableString()).toPath(), 
                 StandardCopyOption.REPLACE_EXISTING); 
   }
   
   /*
    * *************************************************************************************************************************************
    * *************************************************************************************************************************************
    */
   
   /**
    *    Merges multiple SVD files and produces merged SVD files with common peripherals extracted
    *    The original files may already be merged.
    *    
    *    sourceFolderPath  +-> destinationFolderPath : (extracts common peripherals across devices)
    *    
    *    @param sourceFolderPath      - Folder of SVD files to merge
    *    @param destinationFolderPath - Destination folder (which is created) with sub-folder of peripherals
    *    @param optimise              - Whether to apply optimisations when creating SVD files
    * 
    *    @throws IOException 
    *    
    */
   public static void mergeFiles(IPath sourceFolderPath, IPath destinationFolderPath, boolean optimise) throws IOException {

      if (destinationFolderPath.toFile().exists()) {
         System.out.flush();
         System.err.println("Destination already exists " + destinationFolderPath.toOSString());
         return;
      }

      File[] listOfFiles = sourceFolderPath.toFile().listFiles();

      if (listOfFiles == null) {
         System.out.flush();
         System.err.println("Source doesn't exist " + sourceFolderPath.toOSString());
         return;
      }
      // Using complete file name so no automatic extension
      SVD_XML_Parser.setXmlExtension("");

      destinationFolderPath.toFile().mkdir();
      
      PeripheralDatabaseMerger merger = new PeripheralDatabaseMerger();

      merger.setXmlExtension(".svd.xml");
      merger.setXmlRootPath(destinationFolderPath);
      
      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setFreescaleMode(optimise);
      ModeControl.setMapFreescaleCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(optimise);

      System.out.println("Writing files to : \""+destinationFolderPath.toString()+"\"");

      try {
         int deviceCount = 500;
         for (File file : listOfFiles) {
            if (deviceCount-- == 0) {
               break;
            }
            if (file.isFile()) {
               String fileName = file.getName();
               if ((onlyFileToProcess != null) && (!fileName.matches(onlyFileToProcess))) {
                  continue;
               }
               if (fileName.endsWith(".svd.xml")) {
                  System.out.println("Merging SVD file : \""+file.toString()+"\"");

                  // Read device peripheral database
                  DevicePeripherals devicePeripherals = SVD_XML_Parser.createDatabase(new Path(file.getPath()));
                  if (optimise) {
                     devicePeripherals.optimise();
                  }
                  // Create merged SVD file
                  merger.writeDeviceToSVD(devicePeripherals);
               }
            }
         }
         merger.writePeripheralsToSVD();
         merger.writeVectorTablesToSVD();
      } catch (Exception e) {
         e.printStackTrace();
      }
      IPath sourceDeviceList      = sourceFolderPath.append(deviceListFilename);
      IPath destinationDeviceList = destinationFolderPath.append(deviceListFilename);
      if (sourceDeviceList.toFile().exists()) {
         copyFile(sourceDeviceList, destinationDeviceList);
         copyFile(mainFolder.append(deviceListSchemaFilename), destinationFolderPath.append(deviceListSchemaFilename));
      }
      copyFile(mainFolder.append(cmsisSchemaFilename), destinationFolderPath.append(cmsisSchemaFilename));
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
    */
   public static void createHeaderFiles(IPath sourceFolderPath, IPath destinationFolderPath, boolean optimise) {

      if (destinationFolderPath.toFile().exists()) {
         System.out.flush();
         System.err.println("Destination already exists " + destinationFolderPath.toOSString());
         return;
      }
      // Using complete file name so no automatic extension
      SVD_XML_Parser.setXmlExtension("");

      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setFreescaleMode(optimise);
      ModeControl.setMapFreescaleCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(false);

      destinationFolderPath.toFile().mkdir();

      int deviceCount = 5000;

      File[] listOfFiles = sourceFolderPath.toFile().listFiles();

      for (File svdSourceFile : listOfFiles) {
         if (deviceCount-- == 0) {
            break;
         }
         if (svdSourceFile.isFile()) {
            String fileName = svdSourceFile.getName();
            if ((onlyFileToProcess != null) && !fileName.matches(onlyFileToProcess)) {
               continue;
            }
            if (fileName.endsWith(".svd.xml")) {
               System.out.println("Processing File : \""+svdSourceFile.getName()+"\"");
               try {
                  // Read device description
                  DevicePeripherals devicePeripherals = SVD_XML_Parser.createDatabase(new Path(svdSourceFile.getPath()));

                  if (optimise) {
                     // Optimise peripheral database
                     devicePeripherals.optimise();
                  }
                  devicePeripherals.sortPeripherals();
                  
                  // Create header file
                  IPath headerFilePath = destinationFolderPath.append(devicePeripherals.getName()).addFileExtension("h");
                  System.out.println("Creating : \""+headerFilePath.toOSString()+"\"");
                  devicePeripherals.writeHeaderFile((Path)headerFilePath);

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
   public static void createReducedDeviceList(IPath sourceFolderPath, IPath destinationFolderPath) throws Exception {

      if (destinationFolderPath.toFile().exists()) {
         System.out.flush();
         System.err.println("Destination already exists " + destinationFolderPath.toOSString());
         return;
      }

      ArrayList<DevicePeripherals> deviceList = new ArrayList<DevicePeripherals>();

      // Using complete file name so no automatic extension
      SVD_XML_Parser.setXmlExtension("");

      // Set optimisations
      ModeControl.setExtractComplexStructures(false);
      ModeControl.setExtractDerivedPeripherals(false);
      ModeControl.setExtractSimpleRegisterArrays(false);
      ModeControl.setFreescaleMode(false);
      ModeControl.setMapFreescaleCommonNames(false);
      ModeControl.setGenerateFreescaleRegisterMacros(false);
      ModeControl.setRegenerateAddressBlocks(false);
      ModeControl.setExpandDerivedPeripherals(true);
      ModeControl.setExpandDerivedRegisters(false);
      int maxDevices = 500;
      
      destinationFolderPath.toFile().mkdir();
      
      File[] listOfFiles = sourceFolderPath.toFile().listFiles();

      // Create deviceList from unique devices
      //
      for (File svdSourceFile : listOfFiles) {
         // Create database of all devices
         if (svdSourceFile.isFile()) {
            String fileName = svdSourceFile.getName();
            if ((onlyFileToProcess != null) && !fileName.matches(onlyFileToProcess)) {
               continue;
            }
            if (fileName.endsWith(".svd.xml")) {
//               System.out.println("Processing File : \""+svdSourceFile.getName()+"\"");
               try {
                  // Read device description
                  DevicePeripherals device = SVD_XML_Parser.createDatabase(new Path(svdSourceFile.getPath()));
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
         device.writeSVD(destinationFolderPath.append(mappedDestinationFileName).addFileExtension("svd.xml"));
      }
      sortDeviceNames(devicePairList);
      
      // Create deviceList.xml
      File deviceListFile = destinationFolderPath.append(deviceListFilename).toFile();
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
      copyFile(mainFolder.removeLastSegments(1).append(cmsisSchemaFilename),      destinationFolderPath.append(cmsisSchemaFilename));
      copyFile(mainFolder.removeLastSegments(1).append(deviceListSchemaFilename), destinationFolderPath.append(deviceListSchemaFilename));
   }

   /**
    * Tests access to reduced SVD file set (using deviceList.xml)
    * 
    * @param deviceListPath
    * @throws Exception
    */
   static void checkDeviceList(IPath deviceListPath) throws Exception {
      
      DeviceFileList deviceFileList = DeviceFileList.createDeviceFileList(deviceListPath);
      if (!deviceFileList.isValid()) {
         return;
      }
      String svdFilename = deviceFileList.getSvdFilename("MK20DX128M5");
      System.err.println(String.format("Test : %-20s => \"%s\"", "MK20DX128M5", svdFilename));
      System.err.println(String.format("Test : %-20s => \"%s\"", "MK10DN64",    deviceFileList.getSvdFilename("MK10DN64")));
   }
   
   /**
    * Produces a set of header files from SVD files (based on deviceList.xml)
    * Each header file represent a device family with similar peripherals
    * 
    * @param sourceFolderPath       Should contain "DeviceList.xml" file
    * @param destinationFolderPath  Where to write header files to
    * 
    * @throws Exception
    */
   static void createHeaderFilesFromList(IPath sourceFolderPath, IPath destinationFolderPath, boolean optimise) throws Exception {
      
      if (destinationFolderPath.toFile().exists()) {
         System.out.flush();
         System.err.println("Destination already exists " + destinationFolderPath.toOSString());
         return;
      }

      // Using complete file name so no automatic extension
      SVD_XML_Parser.setXmlExtension("");

      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setFreescaleMode(optimise);
      ModeControl.setMapFreescaleCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(optimise);

      destinationFolderPath.toFile().mkdir();

      DeviceFileList deviceFileList = DeviceFileList.createDeviceFileList(sourceFolderPath.append(deviceListFilename));
      if (!deviceFileList.isValid()) {
         return;
      }
      // Get full list of devices
      ArrayList<Pair> list = deviceFileList.getArrayList();

      // Map of already copied files to prevent multiple copying
      HashSet<String> copiedFiles       = new HashSet<String>();

      for (int index = 0; index < list.size(); index++) {
         Pair pair = list.get(index);
         if ((onlyFileToProcess != null) && !pair.deviceName.matches(onlyFileToProcess)) {
            continue;
         }
         System.out.println("Processing File : \""+pair.mappedDeviceName+"\"");
         try {
            // Don't produce the same file!
            if (copiedFiles.contains(pair.mappedDeviceName)) {
               continue;
            }
            copiedFiles.add(pair.mappedDeviceName);
            
            // Read device description
            DevicePeripherals devicePeripherals = SVD_XML_Parser.createDatabase(sourceFolderPath.append(pair.mappedDeviceName).addFileExtension("svd.xml"));

            if (optimise) {
               // Optimise peripheral database
               devicePeripherals.optimise();
            }
            devicePeripherals.sortPeripherals();
            
            devicePeripherals.setName(pair.mappedDeviceName);
            
            devicePeripherals.addEquivalentDevice(pair.deviceName);
            for (int index2 = index+1; index2 < list.size(); index2++) {
               if (pair.mappedDeviceName.equalsIgnoreCase(list.get(index2).mappedDeviceName)) {
                  devicePeripherals.addEquivalentDevice(list.get(index2).deviceName);
               }
            }
            // Create header file
            IPath headerFilePath = destinationFolderPath.append(devicePeripherals.getName()).addFileExtension("h");
            System.out.println("Creating : \""+headerFilePath.toOSString()+"\"");
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
   static void createExpandedSvdFilesFromList(IPath sourceFolderPath, IPath destinationFolderPath, boolean optimise) throws Exception {
      
      if (destinationFolderPath.toFile().exists()) {
         System.out.flush();
         System.err.println("Destination already exists " + destinationFolderPath.toOSString());
         return;
      }

      // Using complete file name so no automatic extension
      SVD_XML_Parser.setXmlExtension("");

      // Set optimisations
      ModeControl.setExtractComplexStructures(optimise);
      ModeControl.setExtractDerivedPeripherals(optimise);
      ModeControl.setExtractSimpleRegisterArrays(optimise);
      ModeControl.setFreescaleMode(optimise);
      ModeControl.setMapFreescaleCommonNames(optimise);
      ModeControl.setGenerateFreescaleRegisterMacros(optimise);
      ModeControl.setRegenerateAddressBlocks(optimise);
//      ModeControl.setExtractCommonPrefix(optimise);

      destinationFolderPath.toFile().mkdir();

      DeviceFileList deviceFileList = DeviceFileList.createDeviceFileList(sourceFolderPath.append(deviceListFilename));
      if (!deviceFileList.isValid()) {
         return;
      }
      ArrayList<Pair> list = deviceFileList.getArrayList();
      
      for (Pair pair : list) {
         if ((onlyFileToProcess != null) && !pair.deviceName.matches(onlyFileToProcess)) {
            continue;
         }
         System.out.println("Processing File : \""+pair.mappedDeviceName+"\"");
         try {
            // Read device description
            DevicePeripherals devicePeripherals = SVD_XML_Parser.createDatabase(sourceFolderPath.append(pair.mappedDeviceName).addFileExtension("svd.xml"));

            // Optimise peripheral database
            devicePeripherals.optimise();

            devicePeripherals.setName(pair.deviceName);
            
            // Create SVD file
            IPath svdFilePath = destinationFolderPath.append(devicePeripherals.getName()).addFileExtension("svd.xml");
            System.out.println("Creating : \""+svdFilePath.toOSString()+"\"");
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
   
   /*
    ARM_Devices +-> svdRaw +-> ARMHeaders (via SVDConv.exe)
                           |
                           +-> Header
                           |
                           +-> HeaderOptimised
                           |
                           +-> svdOptimised  +-> HeaderOptimisedx2 (should = HeaderOptimised)
                                             |
                                             +-> svdOptimisedx2 (should == svdOptimised) 
                                             |
                                             +-> svdOptimisedx3 + (extracts common peripherals across devices)
                                                                |
                                                                +-> HeaderOptimisedx3 (should = HeaderOptimised)
                                                                |
                                                                +-> svdOptimisedx4Manual + (manual clean up)
                                                                                         |
                                                                                         +-> headerOptimisedx4Folder
    ARM_Devices +-> svdRaw +-> ARMHeaders (via SVDConv.exe)
                           |
                           +-> Header
                           |
                           +-> svdReduced  + (extracts common devices)
                                           |
                                           +-> svdReducedMerged (extracts common peripherals across devices)
                                           |
                                           +-> svdReducedMergedOptimised + (extracts common peripherals across devices + optimises)
                                                                         |
                                                                         +-> headerReducedMergedOptimisedFolder + (Reference header files)
                                                                         |
                                                                         +-> svdReducedMergedOptimisedManualFolder + (manual optimisation)
                                                                                                                   |
                                                                                                                   +-> svdReducedMergedOptimisedManualTest (Iteration)
                                                                                                                   |
                                                                                                                   +-> headerReducedMergedOptimisedManual
    */
   
   /**
    * @param args
    */
   @SuppressWarnings("unused")
   public static void main(String[] args) {
      System.err.println("Starting");

      try {
         if (false) {
            // Create final header files & SVD Test
            createHeaderFilesFromList(freescaleSvdFolder, freescaleHeaderFilesFolder, true);
            mergeFiles(freescaleSvdFolder, freescaleSvdTestFolder, true);
         }
         else if (false) {
            // Manual optimisation
            ModeControl.setExpandDerivedRegisters(false);
//            mergeFiles(svdReducedMergedOptimisedManualFolder, svdReducedMergedOptimisedManualIterationFolder, true);                      // svdReducedMerged +-> svdReducedMergedIteration (should be unchanged)
            createHeaderFilesFromList(svdReducedMergedOptimisedManualFolder, headerReducedMergedOptimisedManualFolder, true);             // svdReducedMerged +-> headerReducedMerged (== headerReduced)
            //createExpandedSvdFilesFromList(svdReducedMergedOptimisedManualFolder, svdReducedMergedOptimisedManualExpandedFolder, false);  // svdReducedMerged     +-> header  (for reference)
            //createHeaderFilesFromList(svdReducedMergedOptimisedManualIterationFolder, headerReducedMergedOptimisedManualIterationFolder, true);             // svdReducedMerged +-> headerReducedMerged (== headerReduced)
         }
         else if (true) {
            // Expand SVD
            ModeControl.setExpandDerivedRegisters(false);
            mergeFiles(               svdReducedMergedOptimisedManualFolder, svdReducedMergedOptimisedManualCheckFolder, true);                      // svdReducedMerged +-> svdReducedMergedIteration (should be unchanged)
            createHeaderFilesFromList(svdReducedMergedOptimisedManualFolder, headerReducedMergedOptimisedManualFolder, true);             // svdReducedMerged +-> headerReducedMerged (== headerReduced)
//            createExpandedSvdFilesFromList(svdReducedMergedOptimisedManualFolder, svdReducedMergedOptimisedManualExpandedFolder, false);  // svdReducedMerged     +-> header  (for reference)
//            createHeaderFilesFromList(svdReducedMergedOptimisedManualIterationFolder, headerReducedMergedOptimisedManualIterationFolder, true); // svdReducedMerged +-> headerReducedMerged (== headerReduced)
         }
         else {
            // Playing with Freescale files
            ModeControl.setExpandDerivedRegisters(false);
            createReducedDeviceList(freescaleFolder,        freescaleSortedFolder);
            mergeFiles(freescaleSortedFolder,  freescaleCommonFolder,  true);
            mergeFiles(freescaleCommonFolder,  freescaleReducedFolder,  true);
            
//
//            
//            mergeFiles(freescaleFolder,        freescaleSortedFolder,  false);
//            mergeFiles(freescaleSortedFolder,  freescaleCommonFolder,  true);
//            mergeFiles(freescaleCommonFolder,  freescaleReducedFolder, true);
//            createHeaderFilesFromList(svdReducedMergedOptimisedManualFolder, headerReducedMergedOptimisedManualFolder, true);             // svdReducedMerged +-> headerReducedMerged (== headerReduced)
            //createExpandedSvdFilesFromList(svdReducedMergedOptimisedManualFolder, svdReducedMergedOptimisedManualExpandedFolder, false);  // svdReducedMerged     +-> header  (for reference)
            //createHeaderFilesFromList(svdReducedMergedOptimisedManualIterationFolder, headerReducedMergedOptimisedManualIterationFolder, true);             // svdReducedMerged +-> headerReducedMerged (== headerReduced)
         }

         
         //         createReducedDeviceList(stmicroSvdFolder, stmicroSvdExpandedFolder);
         //         ModeControl.setStripWhiteSpace(true);
         //         mergeFiles(stmicroSvdExpandedFolder, stmicroSvdMergedFolder, true);
         //         mergeFiles(stmicroSvdMergedFolder, stmicroSvdMerged2Folder, true);
         //         createHeaderFilesFromList(stmicroSvdMergedFolder, stMicroHeaderFilesFolder, true);


         //         createHeaderFiles(svdRaw, headerFolder, false);                        // svdRaw     +-> header  (for reference)
         //         createReducedDeviceList(svdRaw, svdReducedFolder);                     // svdRaw     +-> svdReduced (extracts common devices)
         //         mergeFiles(svdReducedFolder, svdReducedMergedFolder, false);           // svdReduced +-> svdReducedMerged (extracts common peripherals across devices)
         //         mergeFiles(svdReducedFolder, svdReducedMergedOptimisedFolder, true);   // svdReduced +-> svdReducedMerged (extracts common peripherals across devices + optimisation)
         //         createHeaderFilesFromList(svdReducedMergedOptimisedFolder, headerReducedMergedOptimisedFolder, true);  // svdReducedMerged +-> headerReducedMerged (== headerReduced)
         //
         //         // Manual optimisation
         //         ModeControl.setExpandDerivedRegisters(false);
         //         mergeFiles(svdReducedMergedOptimisedManualFolder, svdReducedMergedOptimisedManualIterationFolder, true);                      // svdReducedMerged +-> svdReducedMergedIteration (should be unchanged)
         //         createHeaderFilesFromList(svdReducedMergedOptimisedManualFolder, headerReducedMergedOptimisedManualFolder, true);             // svdReducedMerged +-> headerReducedMerged (== headerReduced)
         //         createExpandedSvdFilesFromList(svdReducedMergedOptimisedManualFolder, svdReducedMergedOptimisedManualExpandedFolder, false);  // svdReducedMerged     +-> header  (for reference)
         //         createHeaderFilesFromList(svdReducedMergedOptimisedManualIterationFolder, headerReducedMergedOptimisedManualIterationFolder, true);             // svdReducedMerged +-> headerReducedMerged (== headerReduced)
      } catch (Exception e) {
         e.printStackTrace();
      }
      System.out.flush();
      System.out.println("Done");
   }
}
