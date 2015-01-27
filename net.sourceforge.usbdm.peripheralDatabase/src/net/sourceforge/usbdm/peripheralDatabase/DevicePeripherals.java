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

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

   public DevicePeripherals() {
      name              = "";
      version           = "0.0";
      description       = "";
      addressUnitBits   =  8;
      width             = 32;
      resetValue        =  0L;
      resetMask         =  0xFFFFFFFFL;
      peripherals       = new ArrayList<Peripheral>();
      accessType        = AccessType.ReadWrite;
      cpu               = new Cpu();
      sorted            = false;
      equivalentDevices = new ArrayList<String>();
      vectorTable       = null;
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
    * Get vector table
    * An empty vector is created if needed.
    * 
    * @return the vectorTable
    * @throws Exception 
    */
   public VectorTable getVectorTable() throws Exception {
      if (vectorTable == null) {
         vectorTable = VectorTable.factory(getCpu().getName());
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
         boolean debug1 = false; //peripheral.getName().matches("ADC0");
//         if (debug1) {
//            System.out.println("DevicePeripherals.extractDerivedPeripherals() checking \""+peripheral.getName()+"\"");
//         }
         // Check if a compatible peripheral has already been created in this device
         for (int index2=index1+1; index2<peripherals.size(); index2++) {
            Peripheral checkPeripheral = peripherals.get(index2);
            boolean debug2 = debug1 && checkPeripheral.getName().matches("CAU");
            if (debug2) {
               System.out.println("DevicePeripherals.extractDerivedPeripherals() checking \""+checkPeripheral.getName()+"\" ?= \""+peripheral.getName()+"\"");
            }
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
    * This includes :
    *   * Peripherals that can be derived from earlier peripherals
    *   * Common prefixes on register names
    *   * Creation of register arrays (simple arrays of registers)
    *   * Creation of register clusters (used to represent complex arrays of multiple registers)
    * @throws Exception 
    */
   public void optimise() throws Exception {
      for (Peripheral peripheral : peripherals) {
         peripheral.optimise();
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
         "<device schemaVersion=\"1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" xs:noNamespaceSchemaLocation=\"CMSIS-SVD_Schema_1_1.xsd\">\n"
         ;

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
         ArrayList<InterruptEntry> interruptEntry = peripheral.getInterruptEntries();
         peripheral.clearInterruptEntries();
         for (InterruptEntry i : interruptEntry) {
            if (getVectorTable().getEntry(i.getIndexNumber()) != null) {
               System.err.println("Interrupt vector already allocated");
               System.err.println(String.format("name=%s, no=%d d=%s", i.getName(), i.getIndexNumber(), i.getDescription()));
               i = getVectorTable().getEntry(i.getIndexNumber());
               System.err.println(String.format("name=%s, no=%d d=%s", i.getName(), i.getIndexNumber(), i.getDescription()));
               //               throw new Exception("Interrupt vector already allocated");
            }
            else {
               getVectorTable().addEntry(i);
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
    *   
    *  @param writer         The destination for the XML
    *  @param standardFormat Flag that controls whether the peripherals are expanded in-line or by the use of ENTITY references
    *                        It also suppresses some other size optimisations 
    *  @throws Exception 
    *  
    *  @note If expansion is not done then it is assumed that the needed ENTITY lines have already be written to the writer
    *  @note In any case an XML header should already be written to the file.
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat) throws Exception {
      writer.print(DEVICE_PREAMBLE_1_1);
      writer.println(String.format("   <name>%s</name>", SVD_XML_BaseParser.escapeString(getName())));
      writer.println(String.format("   <version>%s</version>", ((getVersion()==null)?"0.0":getVersion())));
      writer.println(String.format("   <description>%s</description>", SVD_XML_BaseParser.escapeString(getDescription())));
      cpu.writeSVD(writer, standardFormat, this);
      writer.println(String.format("   <addressUnitBits>%d</addressUnitBits>", getAddressUnitBits()));
      writer.println(String.format("   <width>%d</width>", getWidth()));
      writer.println("   <peripherals>");
      sortPeripheralsByName();
      for (Peripheral peripheral : peripherals) {
         if (standardFormat) {
            peripheral.writeSVD(writer, standardFormat, this);
         }
         else {
            if (peripheral.getDerivedFrom() == null) {
               writer.print(String.format("&%s;\n", peripheral.getName()));
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
      getVectorTable().writeSVDInterruptEntries(writer, standardFormat);
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
      } catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         if (writer != null) {
            writer.close();
         }
      }
   }

   static final String NVIC_PRIO_FORMAT = 
         "#define __NVIC_PRIO_BITS  (%d)  /*!< Number of Bits used for Priority Levels    */\n";

   static final String PERIPHERAL_MEMORY_MAP_COMMENT = 
         "\n"
               +"/* ================================================================================ */\n"
               +"/* ================              Peripheral memory map             ================ */\n"
               +"/* ================================================================================ */\n"
               +"\n"
               ;

   static final String PERIPHERAL_INSTANCE_INTRO     = "\n/* %s - Peripheral instance base addresses */\n";
   static final String BASE_ADDRESS_FORMAT           = "#define %-30s 0x%08XUL\n";
   static final String FREESCALE_BASE_ADDRESS_FORMAT = "#define %-30s %s\n";

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
   static final String FREESCALE_PERIPHERAL_DECLARATION_FORMAT = "#define %-30s ((%s *) %s)\n";

   static final String CORE_HEADER_FILE_INCLUSION = 
         "#include %-20s   /*!< Processor and core peripherals */\n"+
               "#include %-20s   /*!< Device specific configuration file */\n\n";

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

   boolean isPeripheralExcludedFromHeaderFile(String name) {
      if (excludedPeripherals == null) {
         excludedPeripherals = new HashSet<String>();
         excludedPeripherals.add("AIPS");
         excludedPeripherals.add("AXBS");
         excludedPeripherals.add("AIPS0");
         excludedPeripherals.add("AXBS0");
         excludedPeripherals.add("AIPS1");
         excludedPeripherals.add("AXBS1");
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
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM");
      Date date = new Date();

      writer.print(String.format(HEADER_FILE_INTRO, 
            getName(), getName(), getEquivalentDevicesList(), getVersion(), dateFormat.format(date), getName().toUpperCase(), getName().toUpperCase()));
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
      //      sortPeripherals();
      if (isFreescaleMode()) {
         // Structs for each peripheral
         writer.print(String.format(HEADER_FILE_ANONYMOUS_UNION_PREAMBLE));
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            // typedef defining registers for each peripheral
            peripheral.writeHeaderFileTypedef(writer, this);

            // #define macros for each peripheral register field
            peripheral.writeHeaderFileFieldMacros(writer, this);
            
            // #define defining peripheral location
            writer.print(String.format(PERIPHERAL_INSTANCE_INTRO, peripheral.getName()));
            writer.print(String.format(BASE_ADDRESS_FORMAT, peripheral.getName()+PTR_BASE, peripheral.getBaseAddress()));
            writer.print(String.format(FREESCALE_PERIPHERAL_DECLARATION_FORMAT, peripheral.getName(), peripheral.getSafeHeaderStructName(), peripheral.getName()+PTR_BASE));
            writer.print(String.format(FREESCALE_BASE_ADDRESS_FORMAT, peripheral.getName()+FREESCALE_PTR_BASE, "("+peripheral.getName()+")"));

            if (isGenerateFreescaleRegisterMacros()) {
               // #define macros for each peripheral register 
               peripheral.writeHeaderFileRegisterMacro(writer);
            }
         }
         writer.print(String.format(HEADER_FILE_ANONYMOUS_UNION_POSTAMBLE));
      }
      else {
         // Structs for each peripheral
         writer.print(String.format(HEADER_FILE_ANONYMOUS_UNION_PREAMBLE));
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            // typedef defining registers for each peripheral
            peripheral.writeHeaderFileTypedef(writer, this);

            // #define macros for each peripheral register field
            peripheral.writeHeaderFileFieldMacros(writer, this);

            if (isGenerateFreescaleRegisterMacros()) {
               // #define macros for each peripheral register 
               peripheral.writeHeaderFileRegisterMacro(writer);
            }
         }
         writer.print(String.format(HEADER_FILE_ANONYMOUS_UNION_POSTAMBLE));

         // Memory map
         writer.print(PERIPHERAL_MEMORY_MAP_COMMENT);
         // #define address of each peripheral
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            writer.print(String.format(BASE_ADDRESS_FORMAT, peripheral.getName()+PTR_BASE, peripheral.getBaseAddress()));
         }
         // Peripheral definitions
         writer.print(PERIPHERAL_DECLARATION_INTRO);
         // #define each peripheral
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            writer.print(String.format(PERIPHERAL_DECLARATION_FORMAT, peripheral.getName(), "volatile "+peripheral.getSafeHeaderStructName()+TYPE_DEF_SUFFIX, peripheral.getName()+PTR_BASE));
         }
      }
      writer.print(String.format(CPP_CLOSING));
      writer.print(String.format(HEADER_FILE_POSTAMBLE, getName().toUpperCase()));
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
   public static void createHeaderFile(String deviceName, Path headerFilePath) {
      // Read device description
      DevicePeripherals devicePeripherals = DevicePeripherals.createDatabase(deviceName);
      System.out.println("Creating : \""+headerFilePath+"\"");
      try {
         devicePeripherals.writeHeaderFile(headerFilePath);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static final String DEVICELIST_FILENAME      = "DeviceList";

   /**
    *  Creates peripheral database for device
    * 
    *  @param device Name of SVD file or device name e.g. "MKL25Z128M5" or family name e.g. "MK20D5"
    *  
    *  @return device peripheral description or null on error
    */
   public static DevicePeripherals createDatabase(String device) {
      SVD_XML_Parser database = new SVD_XML_Parser();

      DevicePeripherals devicePeripherals = null;

      // Parse the XML file into the XML internal DOM representation
      Document dom;
      try {
         // Try name as given (may be full path)
         //         System.err.println("DevicePeripherals.createDatabase() - Trying \""+filePath+"\"");
         dom = SVD_XML_BaseParser.parseXmlFile(device);
         if (dom == null) {
            // Try name with default extension
            //            System.err.println("DevicePeripherals.createDatabase() - Trying \""+filePath+".svd"+"\"");
            dom = SVD_XML_BaseParser.parseXmlFile(device+".svd");
         }
         if (dom == null) {
            // Try name with default extension
            //            System.err.println("DevicePeripherals.createDatabase() - Trying \""+filePath+".xml"+"\"");
            dom = SVD_XML_BaseParser.parseXmlFile(device+".xml");
         }
         if (dom == null) {
            // Retry with mapped name
            //            System.err.println("DevicePeripherals.createDatabase() - Trying DeviceFileList: \n");
            DeviceFileList deviceFileList = DeviceFileList.createDeviceFileList(DEVICELIST_FILENAME);
            if (deviceFileList != null) {
               String mappedFilename = deviceFileList.getSvdFilename(device);
               //               System.err.println("DevicePeripherals.createDatabase() - Trying DeviceFileList: \""+mappedFilename+"\"");
               if (mappedFilename != null) {
                  dom = SVD_XML_BaseParser.parseXmlFile(mappedFilename);
               }
            }
         }
         if (dom != null) {
            // Get the root element
            Element documentElement = dom.getDocumentElement();

            //  Process XML contents and generate Device description
            devicePeripherals = database.parseDocument(documentElement);
         }
      } catch (Exception e) {
         System.err.println("DevicePeripherals.createDatabase() - Exception while parsing: " + device);
         System.err.println("DevicePeripherals.createDatabase() - Exception: reason: " + e.getMessage());
//         e.printStackTrace();
      }
      return devicePeripherals;
   }

   /**
    *  Creates peripheral database for device
    * 
    *  @param path Path to SVD file describing device peripherals
    *  
    *  @return device peripheral description or null on error
    */
   public static DevicePeripherals createDatabase(Path path) {
      return createDatabase(path.toAbsolutePath().toString());
   }

}
