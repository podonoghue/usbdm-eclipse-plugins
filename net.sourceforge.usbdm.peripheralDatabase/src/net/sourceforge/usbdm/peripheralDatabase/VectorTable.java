package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public abstract class VectorTable extends ModeControl {
   
   protected InterruptEntry[]   interrupts        = new InterruptEntry[256];
   protected ArrayList<String>  usedBy            = new ArrayList<String>();
   protected boolean            addDefaultVectors = true;
   protected int                lastUsedEntry     = 0;
   private   final int          vectorOffset;
   protected final int          firstIrqIndex;
   protected String             name              = null;
   protected String             description       = "";
   
   /** List of files to #include at top of file */ 
   protected HashSet<String>  fIncludeFiles      = new HashSet<String>();

   protected static final String DEFAULT_HANDLER_NAME       = "Default_Handler";
   protected static final String EXCEPTION_HANDLER_SUFFIX   = "_Handler";
   protected static final String EXCEPTION_IRQ_SUFFIX       = "_IRQHandler";
   protected static final String EXCEPTION_NUMBER_SUFFIX    = "_IRQn";
   
   public static VectorTable factory(String string) throws Exception {
      
      if (string.equals("CFV1")) {
         return new ColdfireV1VectorTable();
      }
      if (string.equals("CFV2")) {
         return new ColdfireV2VectorTable();
      }
      if (string.startsWith("CM4")) {
         return new ArmCM4VectorTable();
      }
      if (string.startsWith("CM3")) {
         return new ArmCM4VectorTable();
      }
      if (string.startsWith("CM0")) {
         return new ArmCM0VectorTable();
      }
      throw new Exception("Unrecognized CPU type");
   }
   
   /**
    * Create vector table
    * 
    * @param vectorOffset Offset applied to map vector number to vector table entries (ARM)
    * @param firstIrqIndex Index number of first entry to have "_IRQHandler suffix"
    */
   public VectorTable(int vectorOffset, int firstIrqIndex) {
      this.vectorOffset    = vectorOffset;
      this.firstIrqIndex   = firstIrqIndex;
   }

   /**
    * @return the lastUsedEntry
    */
   public int getLastUsedEntry() {
      return lastUsedEntry;
   }

   /**
    * Returns the array of interrupt entries
    * 
    * @return
    */
   public InterruptEntry[] getEntries() {
      return interrupts;
   }

   /**
    * Add interrupt entry
    * 
    * @param entry
    */
   public void addEntry(InterruptEntry entry) {
      interrupts[entry.getIndexNumber()+vectorOffset] = entry;
      if ((entry.getIndexNumber()+vectorOffset)>lastUsedEntry) {
         lastUsedEntry = entry.getIndexNumber()+vectorOffset;
      }
   }

   /**
    * Add device using this vector table
    * 
    * @param deviceName
    */
   public void addUsedBy(String deviceName) {
      usedBy.add(deviceName);
   }

   /**
    * Clear list of devices using thie vector table
    */
   public void clearUsedBy() {
      usedBy = new ArrayList<String>();
   }

   /**
    * Get list of devices using this vector table
    * @return
    */
   public ArrayList<String> getUsedBy() {
      return usedBy;
   }

   /**
    * Get name
    * 
    * @return
    */
   public String getName() {
      return name;
   }

   /**
    * Set name of vector table
    * 
    * @param name
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * @return the description
    */
   public String getDescription() {
      return description;
   }

   /**
    * Get name as safe C name
    * 
    * @return
    */
   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(description);
   }

   /**
    * Get description
    * 
    * @param description
    */
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

   /**
    * Gets interrupt entry for given number
    * 
    * @param number
    * @return
    */
   public InterruptEntry getEntry(int number) {
      return interrupts[number+vectorOffset];
   }

   /**
    * Get name of handler for vector table index<br>
    * e.g. FormatError_Handler, QSPI_IRQHandler
    * 
    * @param index
    * 
    * @return
    */
   public String getHandlerName(int index) {
      if (interrupts[index] == null) {
         return null;
      }
      // Try arbitrary handler name
      String handlerName = interrupts[index].getHandlerName();
      if (handlerName != null) {
         return handlerName;
      }
      // Construct default from name
      handlerName = interrupts[index].getName();
      if (handlerName == null) {
         throw new RuntimeException("Irq entry without name!");
      }
      if (index<firstIrqIndex) {
         return handlerName = handlerName+EXCEPTION_HANDLER_SUFFIX;
      }
      else {
         return handlerName = handlerName+EXCEPTION_IRQ_SUFFIX;
      }
   }
   

   /**
    * Get name of the handler number<br>
    * e.g. AccessError_IRQn
    * 
    * @param index
    * 
    * @return
    */
   public String getHandlerIrqNumber(int index) {
      if (interrupts[index] != null) {
         return interrupts[index].getName()+EXCEPTION_NUMBER_SUFFIX;
      }
      return null;
   }
   /**
    * Get description of interrupt handler for vector table index
    * 
    * @param index
    * @return
    */
   public String getHandlerDescription(int index) {
      if (interrupts[index] != null) {
         return interrupts[index].getCDescription();
      }
      return "";
   }
   
   /**
    * Add default vector table entries
    * 
    */
   protected abstract void addDefaultInterruptEntries();
   
   /*
    * ====================================================================================================
    */
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
      // List devices using this table
      if (getUsedBy().size()>0) {
         writer.print(deviceListPreamble);
         for (String deviceName : getUsedBy()) {
            writer.println(String.format( "   %s", deviceName));
         }
         writer.print(deviceListPostamble);
      }
      final int indent = 0;
      final String indenter = RegisterUnion.getIndent(indent);
      writer.print(                       indenter+"<"+SVD_XML_Parser.INTERRUPTS_TAG+">\n");
      if ((getName() != null) && (name.length() > 0)) {
         writer.print(String.format(      indenter+"   <name>%s</name>\n", getName()));
      }
      else {
         ArrayList<String> usedBy = getUsedBy();
         if (usedBy.size()>0) {
            writer.print(String.format(      indenter+"   <name>%s</name>\n", usedBy.get(0)+"_VectorTable"));
         }
      }
      if ((getDescription() != null) && (getDescription().length()>0)) {
         writer.print(String.format(      indenter+"   <description>%s</description>\n", getDescription()));
      }
      else {
         ArrayList<String> usedBy = getUsedBy();
         if (usedBy.size()>0) {
            writer.print(String.format(      indenter+"   <description>%s</description>\n", usedBy.get(0)+" VectorTable"));
         }
      }
      for (InterruptEntry entry : getEntries()) {
         if (entry != null) {
            int vectorNumber = entry.getIndexNumber();
            if (vectorNumber<0) {
               continue;
            }
            entry.writeSVD(writer, indent+3, true);
//            String name        = String.format(entry.getName(), vectorNumber);
//            String description = String.format(entry.getDescription(), vectorNumber);
//            writer.print(                 indenter+"   <"+SVD_XML_Parser.INTERRUPT_TAG+">\n");
//            writer.print(String.format(   indenter+"      <name>%s</name>\n", name));
//            writer.print(String.format(   indenter+"      <description>%s</description>\n", SVD_XML_BaseParser.escapeString(description)));
//            writer.print(String.format(   indenter+"      <value>%s</value>\n", vectorNumber));
//            writer.print(                 indenter+"   </"+SVD_XML_Parser.INTERRUPT_TAG+">\n");
         }
      }
      writer.print(                       indenter+"</"+SVD_XML_Parser.INTERRUPTS_TAG+">\n\n");
   }

   static final String INTERRUPT_BANNER            = "/* -------------------------  Interrupt Number Definition  ------------------------ */\n\n";
   static final String INTERRUPT_PREAMBLE          = 
           "/**\n" +
            " * Interrupt vector numbers\n" +
            " */\n" +
            "typedef enum {\n";
   static final String INTERRUPT_SEPARATOR         = "/* ------------------------  Processor Exceptions Numbers  ------------------------- */\n";
   static final String INTERRUPT_ENTRY_FORMAT      = "  %-29s = %3d,   /**< %3d %-80s */\n";
   static final String INTERRUPT_SPU_SEPARATOR     = "/* ----------------------   %-40s ---------------------- */\n";
   static final String INTERRUPT_POSTAMBLE         = "} IRQn_Type;\n\n";

   static final String EXTERNAL_HANDLER_BANNER     = "/* -------------------------  Exception Handlers  ------------------------ */\n";
   static final String EXTERNAL_HANDLER_TEMPLATE   = "extern void %-32s   /**< %-80s */\n";

   /** 
    * Writes a C-code fragment that is suitable for inclusion in a C header file<br>
    * It defines the vectors numbers as an enum and provides prototypes<br>
    * for the interrupt handlers matching the vector table created by writeCVectorTable().
    * 
    * @param writer  Where to write the fragment
    * 
    * @throws IOException
    * @throws Exception 
    */
   public abstract void writeCInterruptHeader(Writer writer) throws Exception;
   
   /**
    * Writes a C-code fragment the is suitable for creating a vector table for the device.
    * 
    * @param writer  Where to write the fragment
    * 
    * @throws IOException
    */
   public abstract void writeCVectorTable(Writer writer) throws IOException;

   /**
    * Create a vector table suitable for use in C code
    * 
    * @return String containing the Vector Table
    */
   public String getCVectorTableEntries() {
      StringWriter writer = new StringWriter();
      try {
         writeCVectorTable(writer);
      } catch (IOException e) {
         writer.write("Error Creating Vector table, reason:"+e.getMessage());
      }
      return writer.toString();
   }

   /**
    * Add to set of files to #include at top of vector file 
    *
    * @param headerFileName
    */
   public void addIncludeFile(String headerFileName) {
      fIncludeFiles.add(headerFileName);
   }
   
   /**
    * Get set of files to #include at top of vector file 
    * 
    * @return Set of include files
    */
   public String getCIncludeFiles() {
      StringBuffer sb = new StringBuffer();
      for (String includeFile:fIncludeFiles) {
         sb.append(String.format("#include \"%s\"\n", includeFile));
      }
      return sb.toString();
   }

   /**
    * Get relative vector table path
    * 
    * @return
    * @throws Exception
    */
   public String getRelativePath() throws Exception {
      return PeripheralDatabaseMerger.VECTOR_FOLDER+"/"+getName()+PeripheralDatabaseMerger.XML_EXTENSION;
   }
}
