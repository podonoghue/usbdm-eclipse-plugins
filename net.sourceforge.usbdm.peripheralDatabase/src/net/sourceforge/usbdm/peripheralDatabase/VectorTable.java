package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

public class VectorTable {
   private InterruptEntry[]   interrupts        = new InterruptEntry[256];
   private ArrayList<String>  usedBy            = new ArrayList<String>();
   private String             name              = null;
   private boolean            addDefaultVectors = true;
   private String             description       = "";
   private int                lastUsedEntry     = 0;
   private final int          VECTOR_OFFSET     = 16;
   
   /**
    * @return the lastUsedEntry
    */
   public int getLastUsedEntry() {
      return lastUsedEntry;
   }

   public InterruptEntry[] getEntries() {
      return interrupts;
   }

   public void addEntry(InterruptEntry entry) {
      interrupts[entry.getNumber()+VECTOR_OFFSET] = entry;
      if ((entry.getNumber()+VECTOR_OFFSET)>lastUsedEntry) {
         lastUsedEntry = entry.getNumber()+VECTOR_OFFSET;
      }
   }

   public void addUsedBy(String deviceName) {
      usedBy.add(deviceName);
   }

   public void clearUsedBy() {
      usedBy = new ArrayList<String>();
   }

   public ArrayList<String> getUsedBy() {
      return usedBy;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the description
    */
   public String getDescription() {
      return description;
   }

   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(description);
   }

   public void setDescription(String description) {
      this.description = description;
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof VectorTable)) {
         return false;
      }
      VectorTable o = (VectorTable) other;
      return Arrays.equals(interrupts, o.interrupts);
   }

   private final int HARD_FAULT_NUMBER = -13;
   
   InterruptEntry defaultEntries[] = {
         new InterruptEntry("Reset",             -15,   null,         "Reset Vector, invoked on Power up and warm reset"),
         new InterruptEntry("NonMaskableInt",    -14,   "NMI",        "Non maskable Interrupt, cannot be stopped or preempted"),
         new InterruptEntry("HardFault",         -13,   null,         "Hard Fault, all classes of Fault"),
         new InterruptEntry("MemoryManagement",  -12,   "MemManage",  "Memory Management, MPU mismatch, including Access Violation and No Match"),
         new InterruptEntry("BusFault",          -11,   null,         "Bus Fault, Pre-Fetch-, Memory Access Fault, other address/memory related Fault"),
         new InterruptEntry("UsageFault",        -10,   null,         "Usage Fault, i.e. Undef Instruction, Illegal State Transition"),
         new InterruptEntry("SVCall",             -5,   "SVC",        "System Service Call via SVC instruction"),
         new InterruptEntry("DebugMonitor",       -4,   "DebugMon",   "Debug Monitor"),
         new InterruptEntry("PendSV",             -2,   null,         "Pendable request for system service"),
         new InterruptEntry("SysTick",            -1,   null,         "System Tick Timer"),
   };

   private void addDefaultInterruptEntries() {
      for (InterruptEntry i : defaultEntries) {
         addEntry(i);
      }
   }
   
   private static final String deviceListPreamble = 
         "<!--\n"
       + "Devices using this vector table: \n";
 
   private static final String deviceListPostamble = 
         "-->\n";
 
   /**
    * Writes the Vectors out to a SVD file
    * 
    * @param writer
    * @param standardFormat
    */
   public void writeSVDInterruptEntries(PrintWriter writer, boolean standardFormat) {
      if (!standardFormat) {
         writer.print("&"+PeripheralDatabaseMerger.VECTOR_TABLE_ENTITY+";\n");
         return;
      }
      if (getUsedBy().size()>0) {
         writer.print(deviceListPreamble);
         for (String deviceName : getUsedBy()) {
            writer.println(String.format( "   %s", deviceName));
         }
         writer.print(deviceListPostamble);
      }
      final String indenter = RegisterUnion.getIndent(6);
      writer.print(                       indenter+"<"+SVD_XML_Parser.INTERRUPTS_TAG+">\n");
      if ((getName() != null) && (name.length() > 0)) {
         writer.print(String.format(      indenter+"   <name>%s</name>\n", getName()));
      }
      if ((getDescription() != null) && (getDescription().length()>0)) {
         writer.print(String.format(      indenter+"   <description>%s</description>\n", getDescription()));
      }
      for (InterruptEntry entry : getEntries()) {
         if (entry != null) {
            int vectorNumber = entry.getNumber();
            if (vectorNumber<0) {
               continue;
            }
            String name        = String.format(entry.getName(), vectorNumber);
            String description = String.format(entry.getDescription(), vectorNumber);
            writer.print(                 indenter+"   <"+SVD_XML_Parser.INTERRUPT_TAG+">\n");
            writer.print(String.format(   indenter+"      <name>%s</name>\n", name));
            writer.print(String.format(   indenter+"      <description>%s</description>\n", SVD_XML_BaseParser.escapeString(description)));
            writer.print(String.format(   indenter+"      <value>%s</value>\n", vectorNumber));
            writer.print(                 indenter+"   </"+SVD_XML_Parser.INTERRUPT_TAG+">\n");
         }
      }
      writer.print(                       indenter+"</"+SVD_XML_Parser.INTERRUPTS_TAG+">\n\n");
   }

   static final String interruptBanner          = "/* -------------------------  Interrupt Number Definition  ------------------------ */\n\n";
   static final String interruptPreamble        = "typedef enum {\n";
   static final String interruptCortexSeparator = "/* --------------------  Cortex-M Processor Exceptions Numbers  ------------------- */\n";
   static final String interruptEntryFormat     = "  %-29s = %3d,   /*!< %3d %-80s */\n";
   static final String interruptCPUSeparator    = "/* ----------------------   %-40s ---------------------- */\n";
   static final String interruptPostamble       = "} IRQn_Type;\n\n";

   static final String externHandlerBanner      = "/* -------------------------  Exception Handlers  ------------------------ */\n";
   static final String externHandlerTemplate    = "extern void %s;\n";

   /** Writes a C-code fragment that is suitable for inclusion in a C header file
    * It defines the vectors numbers an enum and provides prototypes
    * for the interrupt handlers matching the vector table created by writeCVectorTable().
    * 
    * @param writer  Where to write the fragment
    * 
    * @throws IOException
    */
   public void writeCInterruptHeader(Writer writer) throws IOException {

      if (addDefaultVectors) {
         addDefaultInterruptEntries();
         addDefaultVectors = false;
      }

      writer.write(interruptBanner);
      writer.write(interruptPreamble);
      writer.write(interruptCortexSeparator);
      boolean separatorWritten = false;
      for (int index=1; index<=getLastUsedEntry(); index++) {
         InterruptEntry entry = interrupts[index];
         if (!separatorWritten && (index >= VECTOR_OFFSET)) {
            writer.write(String.format(interruptCPUSeparator, getCDescription()));
            separatorWritten = true;
         }
         if (entry != null) {
            writer.write(String.format(interruptEntryFormat, entry.getName()+"_IRQn", index-VECTOR_OFFSET, index, entry.getCDescription()));
         }
      }
      writer.write(interruptPostamble);
   
      writer.write(externHandlerBanner);
      String suffix = exceptionHandlerNameSuffix;
      for (int index=2; index<=getLastUsedEntry()+10; index++) {
         InterruptEntry entry = interrupts[index];
         if (index==VECTOR_OFFSET) {
            suffix=interruptHandlerNameSuffix;
         }
         if (entry != null) {
            writer.write(String.format(externHandlerTemplate, entry.getHandlerName()+suffix+"(void)"));
         }
      }
      writer.write('\n');
      
   }

   static final String handlerTemplate          = "void %-40s WEAK_DEFAULT_HANDLER;\n";
   static final String vectorTableTypedef       =
         "typedef struct {\n"        +
         "   uint32_t *initialSP;\n" +
         "   intfunc  handlers[];\n" +
         "} VectorTable;\n\n";

   static final String vectorTableOpen     = 
         "__attribute__ ((section(\".interrupt_vectors\")))\n"+
         "VectorTable const __vector_table = {\n"+
         "                                     /*  Vec Irq */\n"+
         "   &__StackTop,                      /*   0  -16  Initial stack pointer                                                            */\n"+
         "   {\n"+
         "      __HardReset,                   /*   1  -15  Reset Handler                                                                    */\n";
   static final String vectorTableEntry    = 
         "      %-30s /* %3d, %-3d  %-80s */\n";
   static final String vectorTableDeviceSeparator =
         "\n                                     /* External Interrupts */\n";
   static final String vectorTableClose    = 
         "   }\n" +
         "};\n\n";
   
   static final String exceptionHandlerNameSuffix = "_Handler";
   static final String interruptHandlerNameSuffix = "_IRQHandler";
   
   /**
    * Writes a C-code fragment the is suitable for creating a vector table for the device.
    * 
    * @param writer  Where to write the fragment
    * 
    * @throws IOException
    */
   public void writeCVectorTable(Writer writer) throws IOException {

      // Add default entries
      if (addDefaultVectors) {
         addDefaultInterruptEntries();
         addDefaultVectors = false;
      }
      // Write out handler prototypes
      String suffix = exceptionHandlerNameSuffix;
      for (int index=2; index<=getLastUsedEntry()+10; index++) {
         InterruptEntry entry = interrupts[index];
         if (index==VECTOR_OFFSET) {
            suffix = interruptHandlerNameSuffix;
         }
         if ((entry != null) && (entry.getNumber() != HARD_FAULT_NUMBER)) {
            writer.write(String.format(handlerTemplate, entry.getHandlerName()+suffix+"(void)"));
         }
      }
      writer.write('\n');
      
      // Write out vector table
      writer.write(vectorTableTypedef);
      writer.write(vectorTableOpen);
      suffix = exceptionHandlerNameSuffix;
      for (int index=2; index<=255; index++) {
         if (index==VECTOR_OFFSET) {
            suffix = interruptHandlerNameSuffix;
            writer.write(vectorTableDeviceSeparator);
         }
         InterruptEntry entry = interrupts[index];
         if (entry == null) {
            if (index<=15) {
               writer.write(String.format(vectorTableEntry, "0,              ", index, index-VECTOR_OFFSET, "Reserved"));
            }
            else {
               writer.write(String.format(vectorTableEntry, "Default_Handler,", index, index-VECTOR_OFFSET, "Reserved"));
            }
         }
         else {
            writer.write(String.format(vectorTableEntry, entry.getHandlerName()+suffix+",", index, index-VECTOR_OFFSET, entry.getCDescription()));
         }
      }
      writer.write(vectorTableClose);
   }

   public InterruptEntry getEntry(int number) {
      return interrupts[number+VECTOR_OFFSET];
   }

}
