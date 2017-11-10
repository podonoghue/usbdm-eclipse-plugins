/*
 Change History
+===================================================================================
| Revision History
+===================================================================================
| 19 Jan 15 | Some name changes to avoid MACRO clashes                    4.10.6.250
| 16 Nov 13 | Added subfamily field                                       4.10.6.100
+===================================================================================
 */
package net.sourceforge.usbdm.peripheralDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

/**
 * Class holding information about device peripherals created from SVD file
 */
public class DevicePeripherals extends ModeControl {

   private String                       name;
   private String                       version;
   private String                       description;
   private long                         addressUnitBits;
   private long                         width;
   private long                         resetValue;
   private long                         resetMask;
   private List<Peripheral>             peripherals;
   private AccessType                   accessType;
   private Cpu                          cpu;

   private boolean                      sorted;
   private ArrayList<String>            equivalentDevices;
   private VectorTable                  vectorTable;
   private String                       vendor;
   private String                       license;


   @Override
   public String toString() {
      return "[DevicePeripherals: " + getDescription() + "]";
   }

   /**
    * Create DevicePeripherals using given path to SVD file
    * 
    * @param path  Path to SVD file
    * 
    * @throws Exception 
    */
   public DevicePeripherals(Path path) throws UsbdmException {
      name              = "";
      version           = "0.0";
      description       = "";
      addressUnitBits   =  8;
      width             = 32;
      resetValue        =  0L;
      resetMask         =  0xFFFFFFFFL;
      peripherals       = new ArrayList<Peripheral>();
      accessType        = AccessType.ReadWrite;
      cpu               = null;
      sorted            = false;
      equivalentDevices = new ArrayList<String>();
      vectorTable       = null;

      try {
         SVD_XML_Parser.parseDocument(path, this);
      } catch (Exception e) {
         throw new UsbdmException("Failed to parse SVD file "+path, e);
      }
   }

   public void addPeripheral(Peripheral peripheral) {
      peripherals.add(peripheral);
      sorted = false;
   }


   public void addInterruptEntry(InterruptEntry entry) throws Exception {
      getVectorTable().addEntry(entry);
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getDescription() {
      return description;
   }

   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }

   public void setDescription(String description) {
      this.description = getSanitizedDescription(description.trim());
   }

   public long getAddressUnitBits() {
      return addressUnitBits;
   }

   public void setAddressUnitBits(long addressUnitBits) {
      this.addressUnitBits = addressUnitBits;
   }

   public long getWidth() {
      return width;
   }

   public void setWidth(long width) {
      this.width = width;
   }

   public void setName(String name) {
      this.name = name;
      if (isHackKnownValues()) {
         if ((cpu.getName() == null) || (cpu.getName().length() == 0)) {
            if (name.startsWith("MKL") || name.startsWith("MKE") || name.startsWith("MKX")) {
               cpu.setName("CM0");
            }
            else if (name.startsWith("MK")) {
               cpu.setName("CM4");
            }
            else {
               cpu.setName("CM3");
            }
         }
         if ((cpu.getNvicPrioBits() == 0)) {
            if (name.startsWith("MKL") || name.startsWith("MKE") || name.startsWith("MKX")) {
               cpu.setNvicPrioBits(2);
            }
            else if (name.startsWith("MK")) {
               cpu.setNvicPrioBits(4);
            }
         }
         if (name.matches("^MK\\d*F.*")) {
            cpu.fpuPresent = true;
         }
      }
   }

   public String getName() {
      return name;
   }

   public List<Peripheral> getPeripherals() {
      return peripherals;
   }

   public AccessType getAccessType() {
      return accessType;
   }

   public void setAccessType(AccessType accessType) {
      this.accessType = accessType;
   }

   /**
    * @return the resetValue
    */
   public long getResetValue() {
      return resetValue;
   }

   /**
    * @param resetValue the resetValue to set
    */
   public void setResetValue(long resetValue) {
      this.resetValue = resetValue;
   }

   /**
    * @return the resetMask
    */
   public long getResetMask() {
      return resetMask;
   }

   /**
    * @param resetMask the resetMask to set
    */
   public void setResetMask(long resetMask) {
      this.resetMask = resetMask;
   }


   /**
    * Set vendor ID string
    * 
    * @param vendor
    */
   public void setVendor(String vendor) {
      this.vendor = vendor;
   }

   public void setLicense(String license) {
      this.license = license;
   }

   public String getVendor() {
      return vendor;
   }

   public String getLicense() {
      return license;
   }

   /**
    * Get vector table<br>
    * An empty vector is created if needed.
    * 
    * @return the vectorTable
    * @throws Exception 
    */
   public VectorTable getVectorTable() throws Exception {
      if (vectorTable == null) {
         vectorTable = VectorTable.factory(getCpu().getName());
         vectorTable.setName(getName()+"_VectorTable");
      }
      return vectorTable;
   }

   /**
    * @param vectorTable the vectorTable to set
    */
   public void setVectorTable(VectorTable vectorTable) {
      this.vectorTable = vectorTable;
   }


   /** 
    * Get list of equivalent devices
    * 
    * @return
    */
   public ArrayList<String> getEquivalentDevices() {
      return equivalentDevices;
   }

   /**
    * Set a device as equivalent
    * 
    * @param device
    */
   public void addEquivalentDevice(String device) {
      this.equivalentDevices.add(device);
   }

   public Peripheral findPeripheral(String name) {
      for( Peripheral peripheral : peripherals) {
         if (name.equals(peripheral.getName())) {
            return peripheral;
         }
      }
      return null;
   }

   /**
    * Sort the peripherals list by address
    */
   public void sortPeripherals() {
      if (sorted) {
         return;
      }
      Collections.sort(peripherals, new Comparator<Peripheral>() {
         @Override
         public int compare(Peripheral peripheral1, Peripheral peripheral2) {
            if (peripheral2.getBaseAddress() < peripheral1.getBaseAddress()) {
               return 1;
            }
            else if (peripheral2.getBaseAddress() > peripheral1.getBaseAddress()) {
               return -1;
            }
            return (peripheral2.getName().compareTo(peripheral1.getName()));
         }
      });
      sorted = true;
   }

   /**
    * Sort the peripherals by Name
    */
   public void sortPeripheralsByName() {
      Collections.sort(peripherals, new Comparator<Peripheral>() {
         @Override
         public int compare(Peripheral peripheral1, Peripheral peripheral2) {
            return (peripheral1.getName().compareTo(peripheral2.getName()));
         }
      });
      sorted = false;
   }

   /**
    * Check for peripherals in this device that may be 'derived' from other peripherals
    */
   private void extractDerivedPeripherals() {
      //      sortPeripheralsByName();
      for (int index1=0; index1<peripherals.size(); index1++) {
         Peripheral peripheral = peripherals.get(index1);
         //         boolean debug1 = false; //peripheral.getName().matches("ADC0");
         //         if (debug1) {
         //            System.out.println("DevicePeripherals.extractDerivedPeripherals() checking \""+peripheral.getName()+"\"");
         //         }
         // Check if a compatible peripheral has already been created in this device
         for (int index2=index1+1; index2<peripherals.size(); index2++) {
            Peripheral checkPeripheral = peripherals.get(index2);
            //            boolean debug2 = debug1 && checkPeripheral.getName().matches("CAU");
            //            if (debug2) {
            //               System.out.println("DevicePeripherals.extractDerivedPeripherals() checking \""+checkPeripheral.getName()+"\" ?= \""+peripheral.getName()+"\"");
            //            }
            if (checkPeripheral.getDerivedFrom() != null) {
               continue;
            }
            if (checkPeripheral.equivalentStructure(peripheral)) {
               checkPeripheral.setDerivedFrom(peripheral);
               // Similar peripheral already referenced
               //               System.out.println("DevicePeripherals.extractDerivedPeripherals() found \""+checkPeripheral.getName()+"\" == \""+peripheral.getName()+"\"");
            }
         }
      }
      //      sortPeripherals();
   }

   /**
    * Optimise the peripherals associated with this device.
    * 
    * This includes :<br>
    *   <li>Peripherals that can be derived from earlier peripherals<br>
    *   <li>Common prefixes on register names<br>
    *   <li>Creation of register arrays (simple arrays of registers)<br>
    *   <li>Creation of register clusters (used to represent complex arrays of multiple registers)<br>
    * @throws Exception 
    */
   public void optimise() throws Exception {
      sortPeripheralsByName();
      for (Peripheral peripheral : peripherals) {
         try {
            peripheral.optimise();
         } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
            System.err.println("Peripheral: " + peripheral.getName());
            throw e;
         }
      }
      if (isExtractDerivedPeripherals()) {
         extractDerivedPeripherals();
      }
   }

   public boolean equivalentStructure(DevicePeripherals other) {
      boolean rv = (this.getAccessType()      == other.getAccessType()) &&
            (this.getAddressUnitBits() == other.getAddressUnitBits()) && 
            (this.getWidth()           == other.getWidth()) && 
            this.cpu.equals(other.cpu);
      if (!rv) {
         return false;
      }
      if (getPeripherals().size() != other.getPeripherals().size()) {
         return false;
      }
      sortPeripherals();
      other.sortPeripherals();
      for (int index=0; index<getPeripherals().size(); index++) {
         if (!getPeripherals().get(index).equivalent(other.getPeripherals().get(index))) {
            return false;
         }
      }
      return true;
   }

   public void report() throws Exception {
      System.out.println("Device \"" + getName() + "\" = ");
      System.out.println("    version = " + getVersion());
      System.out.println("    description = " + getDescription());
      System.out.println("    addressUnitBits = " + getAddressUnitBits());
      System.out.println("    width = " + getWidth());

      for (Peripheral peripheral : peripherals) {
         peripheral.report();
      }
   }

   //   private void addDefaultInterruptEntries() {
   //      if (interruptList == null) {
   //         interruptList = new VectorTable();
   //      }
   //      
   //      addInterruptEntry(new InterruptEntry("Reset",            -15, "Reset Vector, invoked on Power up and warm reset"));
   //      addInterruptEntry(new InterruptEntry("NonMaskableInt",   -14, "Non maskable Interrupt, cannot be stopped or preempted"));
   //      addInterruptEntry(new InterruptEntry("HardFault",        -13, "Hard Fault, all classes of Fault"));
   //      addInterruptEntry(new InterruptEntry("MemoryManagement", -12, "Memory Management, MPU mismatch, including Access Violation and No Match"));
   //      addInterruptEntry(new InterruptEntry("BusFault",         -11, "Bus Fault, Pre-Fetch-, Memory Access Fault, other address/memory related Fault"));
   //      addInterruptEntry(new InterruptEntry("UsageFault",       -10, "Usage Fault, i.e. Undef Instruction, Illegal State Transition"));
   //      addInterruptEntry(new InterruptEntry("SVCall",            -5, "System Service Call via SVC instruction"));
   //      addInterruptEntry(new InterruptEntry("DebugMonitor",      -4, "Debug Monitor"));
   //      addInterruptEntry(new InterruptEntry("PendSV",            -2, "Pendable request for system service"));
   //      addInterruptEntry(new InterruptEntry("SysTick",           -1, "System Tick Timer"));
   //   }

   static final String XML_PREAMBLE = 
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

   static final String DEVICE_PREAMBLE_1_0 = 
         "<device schemaVersion=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" xs:noNamespaceSchemaLocation=\"CMSIS-SVD_Schema_1_0.xsd\">\n"
         ;

   static final String DEVICE_PREAMBLE_1_1 = 
         "<device\n" +
               "   schemaVersion=\"1.1\"\n" +
               "   xmlns:xi=\"http://www.w3.org/2001/XInclude\"\n" +
               "   xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" xs:noNamespaceSchemaLocation=\"CMSIS-SVD_Schema_1_1.xsd\"\n" +
               ">\n";

   static final String DEVICE_POSTAMBLE = "</device>";

   /**
    *   Writes the Device description to file in SVF format
    *   
    *  @param path The destination for the XML
    * @throws Exception 
    */
   public void writeSVD(Path path) throws Exception {
      File svdFile = path.toFile();
      PrintWriter writer = null;
      try {
         writer = new PrintWriter(svdFile);
         writer.print(XML_PREAMBLE);
         writeSVD(writer, true);
         writer.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }
      if (writer != null) {
         writer.close();
      }
   }

   void collectVectors() throws Exception {
      for (Peripheral peripheral : peripherals) {
         ArrayList<InterruptEntry> interruptEntries = peripheral.getInterruptEntries();
         if (interruptEntries == null) {
            return;
         }
         peripheral.clearInterruptEntries();
         for (InterruptEntry interruptEntry : interruptEntries) {
            if (getVectorTable().getEntry(interruptEntry.getIndexNumber()) != null) {
               InterruptEntry j = getVectorTable().getEntry(interruptEntry.getIndexNumber());
               if (interruptEntry.getName().equals(j.getName())) {
                  continue;
               }
               System.err.println("Interrupt vector already allocated");
               System.err.println(String.format("name=%s, no=%d d=%s", interruptEntry.getName(), interruptEntry.getIndexNumber(), interruptEntry.getDescription()));
               System.err.println(String.format("name=%s, no=%d d=%s", j.getName(), j.getIndexNumber(), j.getDescription()));
               //               throw new Exception("Interrupt vector already allocated");
            }
            else {
               getVectorTable().addEntry(interruptEntry);
            }
         }
      }
   }

   /**
    * @return the cpu
    */
   public Cpu getCpu() {
      return cpu;
   }

   /**
    * @param cpu the cpu to set
    */
   public void setCpu(Cpu cpu) {
      this.cpu = cpu;
   }

   /**
    *   Writes the Device description to file in a modified SVF format
    *  @param peripheralDatabaseMerger 
    *   
    *  @param writer         The destination for the XML
    *  @param standardFormat Flag that controls whether the peripherals are expanded in-line or by the use of ENTITY references
    *                        It also suppresses some other size optimisations 
    *  @throws Exception 
    *  
    *  @note The XML header should already be written to the file.
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat) throws Exception {
      final int indent = 6;
      writeSVD(writer, standardFormat, indent);
   }

   /**
    *   Writes the Device description to file in a modified SVF format
    *  @param peripheralDatabaseMerger 
    *   
    *  @param writer         The destination for the XML
    *  @param standardFormat Flag that controls whether the peripherals are expanded in-line or by the use of ENTITY references
    *                        It also suppresses some other size optimisations 
    *  @throws Exception 
    *  
    *  @note The XML header should already be written to the file.
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat, int indent) throws Exception {
      writer.print(DEVICE_PREAMBLE_1_1);
      writer.println(String.format("   <name>%s</name>", SVD_XML_BaseParser.escapeString(getName())));
      writer.println(String.format("   <version>%s</version>", ((getVersion()==null)?"0.0":getVersion())));
      writer.println(String.format("   <description>%s</description>", SVD_XML_BaseParser.escapeString(getDescription())));
      cpu.writeSVD(writer, standardFormat, this);
      String headerDefinitionPrefix = getHeaderDefinitionsPrefix();
      if (headerDefinitionPrefix.length() > 0) {
         writer.println(String.format("   <headerDefinitionsPrefix>%s</headerDefinitionsPrefix>", getHeaderDefinitionsPrefix()));
      }
      writer.println(String.format("   <addressUnitBits>%d</addressUnitBits>", getAddressUnitBits()));
      writer.println(String.format("   <width>%d</width>", getWidth()));
      writer.println("   <peripherals>");
      sortPeripheralsByName();
      for (Peripheral peripheral : peripherals) {
         if (standardFormat) {
            peripheral.writeSVD(writer, standardFormat, this, indent);
         }
         else {
            if (peripheral.getDerivedFrom() == null) {
               writer.print(String.format("      <xi:include href=\"%s\"/>\n", peripheral.getRelativePath()));
            }
            else {
               StringWriter sWriter = new StringWriter();
               PrintWriter pWriter = new PrintWriter(sWriter);
               peripheral.writeSVD(pWriter, false, null);
               writer.print(sWriter.toString());
            }
         }
      }
      writer.println("   </peripherals>");

      // Consolidate vector table
      collectVectors();

      writer.println("   <vendorExtensions>");
      writer.print(String.format("   <xi:include href=\"%s\"/>\n", getVectorTable().getRelativePath()));
      writer.println("   </vendorExtensions>");

      writer.print(DEVICE_POSTAMBLE);
      writer.flush();
   }

   static final String HEADER_FILE_INTRO =
         "/****************************************************************************************************//**\n"
               + " * @file     %s.h\n"
               + " *\n"
               + " * @brief    CMSIS Cortex-M Peripheral Access Layer Header File for %s.\n"
               + " *           Equivalent: %s\n"
               + " *\n"
               + " * @version  V%s\n"
               + " * @date     %s\n"
               + " *\n"
               + " *******************************************************************************************************/\n"
               + "\n"
               + "#ifndef MCU_%s\n"
               + "#define MCU_%s\n"
               + "\n"
               ;

   static final String COMMON_INCLUDES = 
         "#include <stdint.h>\n"
         ;

   static final String CPP_OPENING = 
         "#ifdef __cplusplus\n"
               + "extern \"C\" {\n"
               + "#endif\n"
               + "\n"
               ;


   static final String COMMON_DEFINITIONS = 
         "#ifndef __IO\n"
               + "#define __IO volatile \n"
               + "#endif\n\n"
               + "#ifndef __I\n"
               + "#define __I volatile const\n"
               + "#endif\n\n"
               + "#ifndef __O\n"
               + "#define __O volatile\n"
               + "#endif\n"
               ;

   static final String HEADER_FILE_ANONYMOUS_UNION_PREAMBLE =
         "\n"
               + "/* -------------------  Start of section using anonymous unions  ------------------ */\n"
               +"#if defined(__CC_ARM)\n"
               +"  #pragma push\n"
               +"  #pragma anon_unions\n"
               +"#elif defined(__ICCARM__)\n"
               +"  #pragma language=extended\n"
               +"#elif defined(__GNUC__)\n"
               +"  /* anonymous unions are enabled by default */\n"
               +"#elif defined(__TMS470__)\n"
               +"/* anonymous unions are enabled by default */\n"
               +"#elif defined(__TASKING__)\n"
               +"  #pragma warning 586\n"
               +"#else\n"
               +"  #warning Not supported compiler type\n"
               +"#endif\n"
               ;

   static final String HEADER_FILE_ANONYMOUS_UNION_POSTAMBLE =
         "/* --------------------  End of section using anonymous unions  ------------------- */\n"
               +"#if defined(__CC_ARM)\n"
               +"  #pragma pop\n"
               +"#elif defined(__ICCARM__)\n"
               +"  /* leave anonymous unions enabled */\n"
               +"#elif defined(__GNUC__)\n"
               +"  /* anonymous unions are enabled by default */\n"
               +"#elif defined(__TMS470__)\n"
               +"  /* anonymous unions are enabled by default */\n"
               +"#elif defined(__TASKING__)\n"
               +"  #pragma warning restore\n"
               +"#else\n"
               +"  #warning Not supported compiler type\n"
               +"#endif\n"
               ;

   static final String HEADER_FILE_DEVICE_SPECIFIC_PERIPHERAL_SEPARATOR =
         "\n\n"
               + "/* ================================================================================ */\n"
               +"/* ================       Device Specific Peripheral Section       ================ */\n"
               +"/* ================================================================================ */\n\n\n"
               ;


   static final String CPP_CLOSING =
         "\n"
               + "#ifdef __cplusplus\n"
               + "}\n"
               + "#endif\n"
               + "\n"
               ;

   static final String HEADER_FILE_POSTAMBLE =
         "\n"
               + "#endif  /* MCU_%s */\n"
               + "\n"
               ;

   /**
    *  Creates a header file from the device description
    *   
    *  @param  headerFilePath  The destination for the data
    *  
    *  @throws Exception 
    */
   public void writeHeaderFile(Path headerFilePath) throws Exception {
      File headerFile = headerFilePath.toFile();
      PrintWriter writer = null;
      try {
         writer = new PrintWriter(headerFile);
         writeHeaderFile(writer);
      }
      finally {
         if (writer != null) {
            writer.close();
         }
      }
   }

   static final String NVIC_PRIO_FORMAT = 
         "#define __NVIC_PRIO_BITS  (%d)  /**< Number of Bits used for Priority Levels    */\n";

   static final String PERIPHERAL_MEMORY_MAP_COMMENT = 
         "\n"
               +"/* ================================================================================ */\n"
               +"/* ================              Peripheral memory map             ================ */\n"
               +"/* ================================================================================ */\n"
               +"\n"
               ;

   static final String PERIPHERAL_INSTANCE_INTRO     = "\n/* %s - Peripheral instance base addresses */\n";
   static final String BASE_ADDRESS_FORMAT           = "#define %-30s 0x%08XUL //!< Peripheral base address\n";
   static final String FREESCALE_BASE_ADDRESS_FORMAT = "#define %-30s %s //!< Freescale style base pointer\n";

   static final String PERIPHERAL_DECLARATION_INTRO = 
         "\n"
               +"/* ================================================================================ */\n"
               +"/* ================             Peripheral declarations            ================ */\n"
               +"/* ================================================================================ */\n"
               +"\n"
               ;

   static final String PROCESSOR_AND_CORE_PERIPHERAL_INTRO = 
         "/* ================================================================================ */\n" +
               "/* ================      Processor and Core Peripheral Section     ================ */\n" +
               "/* ================================================================================ */\n\n";


   static final String PERIPHERAL_DECLARATION_FORMAT           = "#define %-30s ((%-20s *) %s)\n";
   static final String FREESCALE_PERIPHERAL_DECLARATION_FORMAT = "#define %-30s ((%s *) %s) //!< Freescale base pointer\n";

   static final String CORE_HEADER_FILE_INCLUSION = 
         "#include %-20s   /* Processor and core peripherals     */\n"+
               "#include %-20s   /* Device specific configuration file */\n\n";

   private String getEquivalentDevicesList() {
      StringBuffer s = new StringBuffer();
      boolean firstname = true;
      for (String name : equivalentDevices) {
         if (!firstname) {
            s.append(", ");
         }
         firstname = false;
         s.append(name);
      }
      return s.toString();

   }

   HashSet<String> excludedPeripherals = null;
   
   private String  headerDefinitionsPrefix;
   private boolean useHeaderDefinitionsPrefix;

   /**
    * Indicates id a peripheral is excluded from the generated header files
    * @param name
    * @return
    */
   boolean isPeripheralExcludedFromHeaderFile(String name) {
      //TODO Where peripherals are excluded when generating header files
      // Some of these are in the CMSIS headers
      if (excludedPeripherals == null) {
         excludedPeripherals = new HashSet<String>();
         excludedPeripherals.add("AIPS");
         //       excludedPeripherals.add("AXBS");
         //       excludedPeripherals.add("AXBS0");
         //       excludedPeripherals.add("AXBS1");
         excludedPeripherals.add("AIPS0");
         excludedPeripherals.add("AIPS1");
         excludedPeripherals.add("DWT");
         excludedPeripherals.add("ITM");
         excludedPeripherals.add("NVIC");
         excludedPeripherals.add("SCB");
         excludedPeripherals.add("SysTick");
         excludedPeripherals.add("CoreDebug");
         excludedPeripherals.add("FPU");
      }
      return excludedPeripherals.contains(name);
   }

   static final String FREESCALE_PTR_BASE = "_BASE_PTR";

   static final String TYPE_DEF_SUFFIX    = "_TypeDef";
   static final String PTR_BASE           = "_BasePtr";

   /**
    *   Writes the Device description to a header file
    *   
    *  @param writer         The destination for the data
    *  
    *  @throws Exception 
    *  
    */
   public void writeHeaderFile(PrintWriter writer) throws Exception {
      final String periphGroupSuffix = "Peripheral_access_layer";
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM");
      Date date = new Date();

      setUseHeaderDefinitionsPrefix(true);

      writer.print(String.format(HEADER_FILE_INTRO, 
            getName(), 
            getName(), getEquivalentDevicesList(), getVersion(), dateFormat.format(date), 
            getName().toUpperCase(), getName().toUpperCase()));
      writer.print(String.format(COMMON_INCLUDES));
      writer.print(String.format(CPP_OPENING));
      ModeControl.clearMacroCache();
      Peripheral.clearTypedefsTable();
      getVectorTable().writeCInterruptHeader(writer);
      writer.print(PROCESSOR_AND_CORE_PERIPHERAL_INTRO);
      cpu.writeCHeaderFile(writer);
      writer.print(String.format(CORE_HEADER_FILE_INCLUSION, "\"" + cpu.getHeaderFileName() + "\"", "\"system.h\""));
      writer.print(String.format(COMMON_DEFINITIONS));
      writer.print(String.format(HEADER_FILE_DEVICE_SPECIFIC_PERIPHERAL_SEPARATOR));

      sortPeripheralsByName();
      writer.print(String.format(HEADER_FILE_ANONYMOUS_UNION_PREAMBLE));
      writeGroupPreamble(writer, periphGroupSuffix, "Device Peripheral Access Layer", "C structs allowing access to peripheral registers");
      if (useFreescaleFieldNames()) {
         // Structs for each peripheral
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            peripheral.setOwner(this);
            writeGroupPreamble(writer, peripheral.getGroupName()+"_"+periphGroupSuffix, peripheral.getGroupName()+" Peripheral Access Layer", "C Struct for "+peripheral.getGroupName());

            // typedef defining registers for each peripheral
            peripheral.writeHeaderFileTypedef(writer, this);

            // #define macros for each peripheral register field
            peripheral.writeHeaderFileFieldMacros(writer, this);

            // #define defining peripheral location
            writer.print(String.format(PERIPHERAL_INSTANCE_INTRO, peripheral.getName()));
            writer.print(String.format(BASE_ADDRESS_FORMAT, peripheral.getName()+PTR_BASE, peripheral.getBaseAddress()));
            writer.print(String.format(FREESCALE_PERIPHERAL_DECLARATION_FORMAT, peripheral.getName(), peripheral.getSafeHeaderStructName(), peripheral.getName()+PTR_BASE));
            writer.print(String.format(FREESCALE_BASE_ADDRESS_FORMAT, peripheral.getName()+FREESCALE_PTR_BASE, "("+peripheral.getName()+")"));
            
            // #define list of Interrupt numbers
            peripheral.writeHeaderFileInterruptList(writer);
            peripheral.writeHeaderFileMiscellaneous(writer);

            if (isGenerateFreescaleRegisterMacros()) {
               // #define macros for each peripheral register 
               peripheral.writeHeaderFileRegisterMacro(writer);
            }
            writeGroupPostamble(writer, peripheral.getGroupName()+"_"+periphGroupSuffix);
            peripheral.setOwner(null);
         }
         writer.print(String.format(HEADER_FILE_ANONYMOUS_UNION_POSTAMBLE));
      }
      else {
         // Structs for each peripheral
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            peripheral.setOwner(this);
            writeGroupPreamble(writer, peripheral.getGroupName()+"_"+periphGroupSuffix, peripheral.getGroupName()+" Peripheral Access Layer", "C Struct for "+peripheral.getGroupName());
            // typedef defining registers for each peripheral
            peripheral.writeHeaderFileTypedef(writer, this);

            // #define macros for each peripheral register field
            peripheral.writeHeaderFileFieldMacros(writer, this);
            
            // #define list of Interrupt numbers
            peripheral.writeHeaderFileInterruptList(writer);
            peripheral.writeHeaderFileMiscellaneous(writer);
            
            if (isGenerateFreescaleRegisterMacros()) {
               // #define macros for each peripheral register 
               peripheral.writeHeaderFileRegisterMacro(writer);
            }
            writeGroupPostamble(writer, peripheral.getGroupName()+"_"+periphGroupSuffix);
            peripheral.setOwner(null);
         }
         writer.print(String.format(HEADER_FILE_ANONYMOUS_UNION_POSTAMBLE));

         // Memory map
         writer.print(PERIPHERAL_MEMORY_MAP_COMMENT);
         // #define address of each peripheral
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            peripheral.setOwner(this);
            writer.print(String.format(BASE_ADDRESS_FORMAT, peripheral.getName()+PTR_BASE, peripheral.getBaseAddress()));
            peripheral.setOwner(null);
         }
         // Peripheral definitions
         writer.print(PERIPHERAL_DECLARATION_INTRO);
         // #define each peripheral
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            peripheral.setOwner(this);
            writer.print(String.format(PERIPHERAL_DECLARATION_FORMAT, peripheral.getName(), "volatile "+peripheral.getSafeHeaderStructName()+TYPE_DEF_SUFFIX, peripheral.getName()+PTR_BASE));
            peripheral.setOwner(null);
         }
      }
      writeGroupPostamble(writer, periphGroupSuffix);

      writer.print(String.format(CPP_CLOSING));
      writer.print(String.format(HEADER_FILE_POSTAMBLE, getName().toUpperCase()));
      
      setUseHeaderDefinitionsPrefix(false);
   }
   /**
    * Create a vector table suitable for use in C code
    * 
    * @return String containing the Vector Table
    * @throws Exception 
    */
   public String getCVectorTableEntries() throws Exception {
      return getVectorTable().getCVectorTableEntries();
   }

   /**
    * 
    * @param deviceName
    * @param headerFilePath
    */
   public void createHeaderFile(String deviceName, Path headerFilePath) {
      // Read device description
      System.out.println("Creating : \""+headerFilePath+"\"");
      try {
         writeHeaderFile(headerFilePath);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Set header file prefix
    * 
    * @param headerDefinitionsPrefix Header definitions prefix
    */
   public void setHeaderDefinitionsPrefix(String headerDefinitionsPrefix) {
      this.headerDefinitionsPrefix = headerDefinitionsPrefix;
   }

   /**
    * Get header file prefix
    * 
    * return Header definitions prefix
    */
   public String getHeaderDefinitionsPrefix() {
      if (headerDefinitionsPrefix == null) {
         return "";
      }
      return headerDefinitionsPrefix;
   }

   /**
    * Indicates whether the peripheral name is prefixed with the header file prefix
    * 
    * @return
    */
   public boolean isUseHeaderDefinitionsPrefix() {
      return useHeaderDefinitionsPrefix;
   }

   /**
    * Set whether to prefix peripheral name with the header file prefix
    * 
    * @param useHeaderDefinitionsPrefix
    */
   public void setUseHeaderDefinitionsPrefix(boolean useHeaderDefinitionsPrefix) {
      this.useHeaderDefinitionsPrefix = useHeaderDefinitionsPrefix;
   }

   //   /**
   //    *  Creates peripheral database for device
   //    * 
   //    *  @param device Name of SVD file or device name e.g. "MKL25Z128M5" or family name e.g. "MK20D5"
   //    *  
   //    *  @return device peripheral description or null on error
   //    */
   //   public static DevicePeripherals createDatabase(Path path) {
   //      SVD_XML_Parser database = new SVD_XML_Parser();
   //
   //      DevicePeripherals devicePeripherals = null;
   //
   //      // Parse the XML file into the XML internal DOM representation
   //      Document dom;
   //      try {
   //         // Try name as given (may be full path)
   ////         System.err.println("DevicePeripherals.createDatabase() - Trying \""+device+"\"");
   //         dom = SVD_XML_BaseParser.parseXmlFile(path);
   //         if (dom == null) {
   //            // Try name with default extension
   ////            System.err.println("DevicePeripherals.createDatabase() - Trying \""+device+".svd"+"\"");
   //            dom = SVD_XML_BaseParser.parseXmlFile(device+".svd");
   //         }
   //         if (dom == null) {
   //            // Try name with default extension
   ////            System.err.println("DevicePeripherals.createDatabase() - Trying \""+device+".xml"+"\"");
   //            dom = SVD_XML_BaseParser.parseXmlFile(device+".xml");
   //         }
   //         if (dom == null) {
   //            // Retry with mapped name
   ////            System.err.println("DevicePeripherals.createDatabase() - Trying DeviceFileList: \n");
   //            DeviceFileList deviceFileList = DeviceFileList.createDeviceFileList(DEVICELIST_FILENAME);
   //            if (deviceFileList != null) {
   //               String mappedFilename = deviceFileList.getSvdFilename(device);
   ////               System.err.println("DevicePeripherals.createDatabase() - Trying Mapped name: \""+mappedFilename+"\"");
   //               if (mappedFilename != null) {
   //                  dom = SVD_XML_BaseParser.parseXmlFile(mappedFilename);
   //               }
   //            }
   //         }
   //         if (dom == null) {
   //            System.err.println("DevicePeripherals.createDatabase() - Failed for \""+device+"\"");
   //         }
   //         else {
   //            // Get the root element
   //            Element documentElement = dom.getDocumentElement();
   //
   //            //  Process XML contents and generate Device description
   //            devicePeripherals = database.parseDocument(documentElement);
   //         }
   //      } catch (Exception e) {
   //         System.err.println("DevicePeripherals.createDatabase() - Exception while parsing: " + device);
   //         System.err.println("DevicePeripherals.createDatabase() - Exception: reason: " + e.getMessage());
   ////         e.printStackTrace();
   //      }
   //      return devicePeripherals;
   //   }

}
