/*
 Change History
+===================================================================================
| Revision History
+===================================================================================
| 16 Nov 13 | Added subfamily field                                       4.10.6.100
+===================================================================================
 */
package net.sourceforge.usbdm.peripheralDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

import org.eclipse.core.runtime.IPath;

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
      vectorTable       = new VectorTable();
   }

  
   public void addPeripheral(Peripheral peripheral) {
      peripherals.add(peripheral);
      sorted = false;
   }

   public void addInterruptEntry(InterruptEntry entry) {
      vectorTable.addEntry(entry);
   }
   
   public VectorTable getInterruptEntries() {
      return vectorTable;
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
    * @return the vectorTable
    */
   public VectorTable getVectorTable() {
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
   
   static final String xmlPreamble = 
       "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

   static final String devicePreamble1_0 = 
         "<device schemaVersion=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" xs:noNamespaceSchemaLocation=\"CMSIS-SVD_Schema_1_0.xsd\">\n"
         ;
   
   static final String devicePreamble1_1 = 
         "<device schemaVersion=\"1.1\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" xs:noNamespaceSchemaLocation=\"CMSIS-SVD_Schema_1_1.xsd\">\n"
         ;
   
   static final String devicePostamble = "</device>";

   /**
    *   Writes the Device description to file in SVF format
    *   
    *  @param filePath The destination for the XML
    * @throws Exception 
    */
   public void writeSVD(IPath filePath) throws Exception {
      File svdFile = filePath.toFile();
      PrintWriter writer = null;
      try {
         writer = new PrintWriter(svdFile);
         writer.print(xmlPreamble);
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
            if (vectorTable.getEntry(i.getNumber()) != null) {
               System.err.println("Interrupt vector already allocated");
               System.err.println(String.format("name=%s, no=%d d=%s", i.getName(), i.getNumber(), i.getDescription()));
               i = vectorTable.getEntry(i.getNumber());
               System.err.println(String.format("name=%s, no=%d d=%s", i.getName(), i.getNumber(), i.getDescription()));
//               throw new Exception("Interrupt vector already allocated");
            }
            else {
               vectorTable.addEntry(i);
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
    * @throws Exception 
    *  
    *  @note If expansion is not done then it is assumed that the needed ENTITY lines have already be written to the writer
    *  @note In any case an XML header should already be written to the file.
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat) throws Exception {
         writer.print(devicePreamble1_1);
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
               writer.print(String.format("&%s;\n", peripheral.getName()));
            }
         }
         writer.println("   </peripherals>");

         // Consolidate vector table
         collectVectors();
         
         writer.println("   <vendorExtensions>");
         vectorTable.writeSVDInterruptEntries(writer, standardFormat);
         writer.println("   </vendorExtensions>");
         
         writer.print(devicePostamble);
         writer.flush();
   }

   static final String headerFilePreamble =
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
   
   static final String commonIncludes = 
         "#include <stdint.h>\n"
         ;
   
   static final String cppOpening = 
            "#ifdef __cplusplus\n"
          + "extern \"C\" {\n"
          + "#endif\n"
          + "\n"
          ;
   

   static final String commonDefinitions = 
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
   
   static final String headerFileAnonymousUnionsPreamble =
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

   static final String headerFileAnonymousUnionsPostamble =
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
   
   static final String headerFileDeviceSpecificPeripheralSeparator =
       "\n\n"
      + "/* ================================================================================ */\n"
      +"/* ================       Device Specific Peripheral Section       ================ */\n"
      +"/* ================================================================================ */\n\n\n"
      ;


   static final String cppClosing =
         "\n"
       + "#ifdef __cplusplus\n"
       + "}\n"
       + "#endif\n"
       + "\n"
       ;

   static final String headerFilePostamble =
         "\n"
       + "#endif  /* MCU_%s */\n"
       + "\n"
       ;

   /**
    *  Creates a header file from the device description
    *   
    *  @param  filePath  The destination for the data
    *  
    *  @throws Exception 
    */
   public void writeHeaderFile(IPath filePath) throws Exception {
      File headerFile = filePath.toFile();
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

   final String nvicPrioFormat = 
         "#define __NVIC_PRIO_BITS  (%d)  /*!< Number of Bits used for Priority Levels    */\n";

   final String peripheralMemoryMapComment = 
         "\n"
        +"/* ================================================================================ */\n"
        +"/* ================              Peripheral memory map             ================ */\n"
        +"/* ================================================================================ */\n"
        +"\n"
        ;
  
  final String baseAddressFormat = "#define %-30s 0x%08XUL\n";
  
  final String peripheraDeclarationComment = 
        "\n"
       +"/* ================================================================================ */\n"
       +"/* ================             Peripheral declarations            ================ */\n"
       +"/* ================================================================================ */\n"
       +"\n"
       ;
 
  final String processorAndCoreBanner = 
  "/* ================================================================================ */\n" +
  "/* ================      Processor and Core Peripheral Section     ================ */\n" +
  "/* ================================================================================ */\n\n";


   final String peripheralDeclarationFormat = 
         "#define %-30s ((%-20s *) %s)\n";
 
   final String cortexHeaderFileInclusion = 
         "#include <%s>   /*!< Cortex-M processor and core peripherals                              */\n\n";
   
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
   
   /**
    *   Writes the Device description to a header file
    *   
    *  @param writer         The destination for the data
    *  
    *  @throws Exception 
    *  
    */
   @SuppressWarnings("unused")
   private void writeHeaderFile(PrintWriter writer) throws Exception {
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM");
      Date date = new Date();
      
      writer.print(String.format(headerFilePreamble, 
            getName(), getName(), getEquivalentDevicesList(), getVersion(), dateFormat.format(date), getName().toUpperCase(), getName().toUpperCase()));

      writer.print(String.format(commonIncludes));
      
      writer.print(String.format(cppOpening));

      ModeControl.resetMacroCache();
      
      if (vectorTable == null) {
         vectorTable = new VectorTable();
      }
//      vectorTable.writeCVectorTable(writer);
      vectorTable.writeCInterruptHeader(writer);
      
      writer.print(processorAndCoreBanner);
      
      cpu.writeCHeaderFile(writer);

      writer.print(String.format(cortexHeaderFileInclusion, cpu.getHeaderFileName()));

      writer.print(String.format(commonDefinitions));

      writer.print(String.format(headerFileDeviceSpecificPeripheralSeparator));

      sortPeripheralsByName();
//      sortPeripherals();
      
      if (false) {
         // Structs for each peripheral
         writer.print(String.format(headerFileAnonymousUnionsPreamble));
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            // typedef defining registers for each peripheral
            peripheral.writeHeaderFileTypedef(writer, this);
            //            writer.print(String.format(peripheralDeclarationFormat, peripheral.getName(), "volatile "+peripheral.getName()+"_Type", peripheral.getName()+"_BASE_PTR"));
            if (isGenerateFreescaleRegisterMacros()) {
               peripheral.writeHeaderFileRegisterMacro(writer);
            }
         }
         writer.print(String.format(headerFileAnonymousUnionsPostamble));

         // #define macros for each peripheral
         for (Peripheral peripheral : peripherals) {
            if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
               continue;
            }
            // #define macros for each peripheral register field
            peripheral.writeHeaderFileFieldMacros(writer, this);
         }
      }
      else {
         // Structs for each peripheral
         writer.print(String.format(headerFileAnonymousUnionsPreamble));
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
         writer.print(String.format(headerFileAnonymousUnionsPostamble));
      }

      // Memory map
      writer.print(peripheralMemoryMapComment);
      // #define address of each peripheral
      for (Peripheral peripheral : peripherals) {
         if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
            continue;
         }
         writer.print(String.format(baseAddressFormat, peripheral.getName()+"_BASE_PTR", peripheral.getBaseAddress()));
      }

      // Peripheral definitions
      writer.print(peripheraDeclarationComment);
      // #define each peripheral
      for (Peripheral peripheral : peripherals) {
         if (isPeripheralExcludedFromHeaderFile(peripheral.getName())) {
            continue;
         }
         writer.print(String.format(peripheralDeclarationFormat, peripheral.getName(), "volatile "+peripheral.getName()+"_Type", peripheral.getName()+"_BASE_PTR"));
      }

      writer.print(String.format(cppClosing));
      
      writer.print(String.format(headerFilePostamble, getName().toUpperCase()));
   }
   
   public String getCVectorTableEntries() {
      StringWriter writer = new StringWriter();
      try {
         vectorTable.writeCVectorTable(writer);
      } catch (IOException e) {
         writer.write("Error Creating Vector table, reason:"+e.getMessage());
      }
      return writer.toString();
   }
   
   /**
    * 
    * @param deviceName
    * @param headerFilePath
    */
   public static void createHeaderFile(String deviceName, IPath headerFilePath) {
      // Read device description
      DevicePeripherals devicePeripherals = SVD_XML_Parser.createDatabase(deviceName);
      System.out.println("Creating : \""+headerFilePath.toOSString()+"\"");
      try {
         devicePeripherals.writeHeaderFile(headerFilePath);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
