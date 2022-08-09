package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.DeviceDatabase;
import net.sourceforge.usbdm.deviceDatabase.MemoryRegion;
import net.sourceforge.usbdm.deviceDatabase.MemoryRegion.MemoryRange;
import net.sourceforge.usbdm.deviceDatabase.MemoryType;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

public class DeviceLinkerInformation {

   private final static String MAP_PREFIX = 
         "MEMORY\n" + //$NON-NLS-1$
               "{\n";         //$NON-NLS-1$

   private final static String MAP_SUFFIX = 
         "}\n\n";         //$NON-NLS-1$

   private final static String MEM_FORMAT       = "  %-14s %-5s : ORIGIN = 0x%08X, LENGTH = 0x%08X\n";
   private final static String MEM_FORMAT_FLASH = "  %-14s %-5s : ORIGIN = 0x%08X + BOOT_LOADER_SIZE, LENGTH = 0x%08X - BOOT_LOADER_SIZE\n";

   private final static String MEM_DOCUMENTATION = 
         "/*\n"                             +
               "    <o>  %-6s address <constant>\n" +
               "    <o1> %-6s size    <constant>\n"  +
               " */\n";

   private final static String LINKER_FLEXNVM_REGION =
         "   /* flexNVM flash region */\n"+
               "   .flexNVM (NOLOAD) :\n" + 
               "   {\n" + 
               "      . = ALIGN(4);\n" + 
               "      PROVIDE(__FlexNvmStart = .);\n"+
               "      KEEP(*(.flexNVM))\n" + 
               "      PROVIDE(__FlexNvmEnd = .);\n" + 
               "   } > flexNVM\n\n"; //$NON-NLS-1$

   private final static String LINKER_FLEXRAM_REGION =
         "   /* FlexRAM region for non-volatile variables */\n"+
               "   .flexRAM (NOLOAD) :\n" + 
               "   {\n" + 
               "      . = ALIGN(4);\n" + 
               "      PROVIDE(__FlexRamStart = .);\n"+
               "      KEEP(*(.flexRAM))\n" +
               "      PROVIDE(__FlexRamEnd = .);\n" + 
               "   } > flexRAM\n\n"; //$NON-NLS-1$

   private final static String DEFAULT_RAM_REGION        = "ram";
   private final static String DEFAULT_RAM_HIGH_REGION   = "ram_high";
   private final static String DEFAULT_RAM_LOW_REGION    = "ram_low";

   private static String getRangeSuffix(int count) {
      if (count == 0) {
         return "   "; //$NON-NLS-1$
      }
      else if (count <= 9) {
         return "_" + Integer.toString(count) + " "; //$NON-NLS-1$ //$NON-NLS-2$
      }
      else {
         return "_" + Integer.toString(count); //$NON-NLS-1$
      }
   }

   /**
    * Produce a merge list of memory regions
    * 
    * @param regions Regions to check and merge
    * 
    * @return Merged regions
    */
   private static ArrayList<MemoryRange> coalesce(ArrayList<MemoryRange> regions) {
      if (regions.size() <= 1) {
         // Return unchanged
         return regions;
      }
      ArrayList<MemoryRange> newRegions = new ArrayList<MemoryRange>();
      Collections.sort(regions);
      long start = regions.get(0).start;
      long end   = regions.get(0).end;
      for(int sub=1; sub<regions.size(); sub++) {
         if ((end+1) == regions.get(sub).start) {
            // Merge with previous region
            end = regions.get(sub).end;
         }
         else {
            // Finish this region
            newRegions.add(new MemoryRange(start, end));
            start = regions.get(sub).start;
            end   = regions.get(sub).end;
         }
      }
      // Add the last region being processed
      newRegions.add(new MemoryRange(start, end));
      
      return newRegions;
   }

   /**
    * Write a set of memory region descriptions
    * 
    * @param memoryRanges     Memory ranges to add
    * @param userNameBase     Base name for display e.g. "Flex RAM"
    * @param linkerNameBase   Base name for liner use e.g. "flexRAM"
    * @param attributes       Attributes for region e.g. "rw"
    * 
    * @return the description
    */
   private static String writeRegions(final ArrayList<MemoryRange> memoryRanges, String userNameBase, String linkerNameBase, String attributes) {
      StringBuilder memoryMap = new StringBuilder();
      int suffix  = 0;
      String userName   = userNameBase;
      String linkerName = linkerNameBase;
      for(MemoryRange region:memoryRanges) {
         String currentUserName    = region.getName();
         String currentLinkerName  = region.getName();
         if (currentUserName == null) {
            currentUserName = userName;
         }
         if (currentLinkerName == null) {
            currentLinkerName = linkerName;
         }
         memoryMap.append(String.format(MEM_DOCUMENTATION, currentUserName, currentUserName));
         if (userNameBase.equals("FLASH")) {
            memoryMap.append(String.format(MEM_FORMAT_FLASH, currentLinkerName, "("+attributes+")", region.start, region.end-region.start+1));
         }
         else {
            memoryMap.append(String.format(MEM_FORMAT, currentLinkerName, "("+attributes+")", region.start, region.end-region.start+1));
         }
         suffix++;
         userName   = userNameBase   + suffix; 
         linkerName = linkerNameBase + suffix; 
      }
      return memoryMap.toString();
   }

   /**
    * Updates or creates (if necessary) a variable
    * 
    * @param paramMap      Map to add variable to 
    * @param newVariable   New variable
    * @param derived       Whether the variable is derived i.e. calculated rather than user controlled 
    */
   private static void updateVariable(VariableMap paramMap, Variable newVariable, boolean derived) {
      Variable variable = paramMap.safeGet(newVariable.getKey());
      if (variable != null) {
         // Existing variable
         if (derived || variable.isDefault()) {
            // Update value rather than create variable
            variable.setValue(newVariable.getValueAsString());
            variable.setDefault(newVariable.getValueAsString());
         }
      }
      else {
         // New variable
         variable = newVariable;
         paramMap.put(newVariable.getKey(), newVariable);
         variable.setDefault(newVariable.getValueAsString());
      }
      variable.setDerived(derived);
      if (variable instanceof LongVariable) {
         // Use hex for numbers
         ((LongVariable) variable).setRadix(16);
      }
   }
   
   /**
    * Adds the device linker and memory map information to the paramMap
    * 
    * @param deviceName  Name of device to get memory map for
    * @param paramMap    Map to add information to
    * @throws Exception 
    */
   public static void addLinkerMemoryMap(String deviceName, VariableMap paramMap) throws Exception {

      DeviceDatabase deviceDatabase = DeviceDatabase.getDeviceDatabase(TargetType.T_ARM);
      Device device = deviceDatabase.getDevice(deviceName);    
      
      if (device == null) {
         throw new Exception("Unable to find device data for '"+deviceName+"'");
      }
      int ioRangeCount    = 0;
      int romCount        = 0;
      int unknownCount    = 0;
      long ramSize        = 0x100;
      long flashSize      = 0x1000;
      long flexRamSize    = 0;
      long flexNvmSize    = 0;

      StringBuilder memoryMap = new StringBuilder(String.format(
            "/*\n" +
                  " * Memory Map generated by USBDM for %s\n" +
                  " */\n" +
                  " \n",
                  device.getName())); //$NON-NLS-1$

      memoryMap.append(MAP_PREFIX);
      ArrayList<MemoryRange> ramRegions     = new ArrayList<MemoryRange>();
      ArrayList<MemoryRange> flashRegions   = new ArrayList<MemoryRange>();
      ArrayList<MemoryRange> flexNvmRegions = new ArrayList<MemoryRange>();
      ArrayList<MemoryRange> flexRamRegions = new ArrayList<MemoryRange>();
      for (Iterator<MemoryRegion> it = device.getMemoryRegionIterator();
            it.hasNext();) {
         MemoryRegion memoryRegion = it.next();
         for ( Iterator<MemoryRange> it1 = memoryRegion.iterator();
               it1.hasNext();) {
            MemoryRange memoryRange = it1.next();
            String name   = "";
            String access = "";
            MemoryType memType = memoryRegion.getMemoryType();
            switch (memType) {
            case MemRAM   :
               ramRegions.add(memoryRange);
               continue;
            case MemFLASH : 
               flashRegions.add(memoryRange);
               continue;
            case MemFlexRAM : 
               flexRamRegions.add(memoryRange);
               continue;
            case MemFlexNVM : 
               flexNvmRegions.add(memoryRange);
               continue;
            case MemIO    : 
               name   = String.format("io%s", getRangeSuffix(ioRangeCount++));
               access = "(rw)";
               break;
            case MemROM:
               name   = String.format("rom%s", getRangeSuffix(romCount++));
               access = "(rx)";
               break;
            case MemDFlash:
            case MemEEPROM:
            case MemInvalid:
            case MemPFlash:
            case MemPRAM:
            case MemPROM:
            case MemXRAM:
            case MemXROM:
            default:
               name   = memType.xmlName+String.format("%s", getRangeSuffix(unknownCount++));
               access = "(r)";
               break;
            }
            if (memoryRange.getName() != null) {
               // Use supplied name
               name = memoryRange.getName();
            }
            memoryMap.append(String.format(MEM_FORMAT, name, access, memoryRange.start, memoryRange.end-memoryRange.start+1));
         }
      }
      // Don't coalesce SRAMU/SRAML as alignment issue at boundary
      
      flexRamRegions = coalesce(flexRamRegions);
      memoryMap.append(writeRegions(flexRamRegions, "Flex RAM", "flexRAM", "rw"));
      
      flexNvmRegions = coalesce(flexNvmRegions);
      memoryMap.append(writeRegions(flexNvmRegions, "Flex NVM", "flexNVM", "rx"));

      flashRegions = coalesce(flashRegions);
      memoryMap.append(writeRegions(flashRegions, "FLASH", "flash", "rx"));

      if (flashRegions.size() == 0) {
         // Should never happen!
         System.err.println("Error = flashSize == 0!");
         flashSize = 0;
      }
      else {
         // 1st FLASH region
         flashSize = (flashRegions.get(0).end-flashRegions.get(0).start+1);
      }

      // Don't coalesce RAM regions as there are alignment issues at boundaries.
      // Just choose the largest as the default RAM region

      ramSize        = 0;
      MemoryRange ramRegion = null;
      MemoryRange ramHigh   = null;
      MemoryRange ramLow    = null;

      // Use largest RAM region as default RAM
      for(MemoryRange region:ramRegions) {
         if ((region.getName() != null) && (region.getName().equals("bitband"))) {
            // This is not real RAM!
            continue;
         }
         long t = (region.end-region.start+1);
         if (t>=ramSize) {
            ramSize   = t;
            ramRegion = region;
         }
         if (DEFAULT_RAM_HIGH_REGION.equalsIgnoreCase(region.getName())) {
            ramHigh = region;
         }
         if (DEFAULT_RAM_LOW_REGION.equalsIgnoreCase(region.getName())) {
            ramLow = region;
         }
      }
      if (ramRegion == null) {
         // Should never happen!
         System.err.println("Error no RAM region found");
      }
      else {
         memoryMap.append(writeRegions(ramRegions, "RAM", "ram", "rwx"));
         memoryMap.append(MAP_SUFFIX);
      }

      if (!DEFAULT_RAM_REGION.equalsIgnoreCase(ramRegion.getName())) {
         // Add alias for main RAM region (if needed)
         memoryMap.append(String.format("REGION_ALIAS(\"%s\",\"%s\");\n", DEFAULT_RAM_REGION,   ramRegion.getName()));
      }
      
      if ((ramHigh != null) && (ramLow != null)) {
         // Assume separate regions for Stack and Heap.
         updateVariable(paramMap, new LongVariable(null, "/LINKER/stackSize",     "0x"+Long.toHexString((ramHigh.end-ramHigh.start+1)/2)), false);
         updateVariable(paramMap, new LongVariable(null, "/LINKER/heapSize",      "0x"+Long.toHexString((ramLow.end-ramLow.start+1)/2)),   false);
         updateVariable(paramMap, new LongVariable(null, "/LINKER/ramLowerSize",  "0x"+Long.toHexString(ramLow.end-ramLow.start+1)),       true);
         updateVariable(paramMap, new LongVariable(null, "/LINKER/ramUpperSize",  "0x"+Long.toHexString(ramHigh.end-ramHigh.start+1)),     true);
      }
      else  {
         // Assume single RAM region
         updateVariable(paramMap, new LongVariable(null, "/LINKER/stackSize",     "0x"+Long.toHexString(ramSize/4)), false);
         updateVariable(paramMap, new LongVariable(null, "/LINKER/heapSize",      "0x"+Long.toHexString(ramSize/4)), false);
         updateVariable(paramMap, new LongVariable(null, "/LINKER/ramLowerSize",  "0x"+Long.toHexString(0)),         true);
         updateVariable(paramMap, new LongVariable(null, "/LINKER/ramUpperSize",  "0x"+Long.toHexString(0)),         true);
      }
      updateVariable(paramMap, new StringVariable(null, "/LINKER/information",  memoryMap.toString()),             true);
      updateVariable(paramMap, new LongVariable(  null, "/LINKER/flashSize",    "0x"+Long.toHexString(flashSize)), true);
      updateVariable(paramMap, new LongVariable(  null, "/LINKER/ramSize",      "0x"+Long.toHexString(ramSize)),   true);

      StringBuilder sb = new StringBuilder();
      if (flexRamRegions.size()>0) {
         sb.append(LINKER_FLEXRAM_REGION);
         flexRamSize = flexRamRegions.get(0).end-flexRamRegions.get(0).start+1;
      }
      if (flexNvmRegions.size()>0) {
         sb.append(LINKER_FLEXNVM_REGION);
         flexNvmSize = flexNvmRegions.get(0).end-flexNvmRegions.get(0).start+1;
      }
      updateVariable(paramMap, new StringVariable(null, "/LINKER/extraRegions", sb.toString()), true);

      updateVariable(paramMap, new LongVariable(  null, "/LINKER/flexRamSize",      "0x"+Long.toHexString(flexRamSize)),   true);
      updateVariable(paramMap, new LongVariable(  null, "/LINKER/flexNvmSize",      "0x"+Long.toHexString(flexNvmSize)),   true);

      String subFamily = device.getFamily();
      
      String scriptFilename   = "ARMLtd_GCC-rom.ld";
      String mapFilename      = "MemoryMap.ld";
      String vectorsFilename  = "vectors.cpp";
      
      if (subFamily.equalsIgnoreCase("CortexM0") || subFamily.equalsIgnoreCase("CortexM0plus")) {
         scriptFilename   = "ARMLtd_GCC-rom-mkl.ld";
         mapFilename      = "MemoryMap-mkl.ld";
         vectorsFilename  = "vectors-cm0.cpp";
      }
      else if (subFamily.equalsIgnoreCase("CortexM4")||subFamily.equalsIgnoreCase("CortexM4F")) {
         scriptFilename   = "ARMLtd_GCC-rom-mk.ld";
         mapFilename      = "MemoryMap-mk.ld";
         vectorsFilename  = "vectors-cm4.cpp";
      }
      updateVariable(paramMap, new StringVariable(null, "/LINKER/scriptFilename",   scriptFilename),  true);
      updateVariable(paramMap, new StringVariable(null, "/LINKER/mapFilename",      mapFilename),     true);
      updateVariable(paramMap, new StringVariable(null, "/LINKER/vectorsFilename",  vectorsFilename), true);
   }

}

