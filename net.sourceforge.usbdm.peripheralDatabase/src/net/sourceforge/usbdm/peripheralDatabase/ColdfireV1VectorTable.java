package net.sourceforge.usbdm.peripheralDatabase;

import java.io.Writer;

public class ColdfireV1VectorTable extends VectorTable {

   static final int VECTOR_OFFSET       = 0;
   static final int FIRST_IRQ_INDEX     = 48;
   static final int HARD_FAULT_NUMBER   = 0;

   public ColdfireV1VectorTable() {
      super(VECTOR_OFFSET, FIRST_IRQ_INDEX);
   }

   InterruptEntry defaultVectorTableEntries[] = {
         new InterruptEntry("InitialSP",                0,   null, "Initial SP"              ),
         new InterruptEntry("Reset",                    1,   null, "Initial PC"              ),
         new InterruptEntry("AccessError",              2,   null, "Access Error"            ),
         new InterruptEntry("AddressError",             3,   null, "Address Error"           ),
         new InterruptEntry("IllegalInstruction",       4,   null, "Illegal Instruction"     ),
         new InterruptEntry("DivideBy0",                5,   null, "Divide by Zero"          ),
         new InterruptEntry("PrivilegeViolation",       8,   null, "Privilege Violation"     ),
         new InterruptEntry("Trace",                    9,   null, "Trace"                   ),
         new InterruptEntry("UnimplementedLineA",      10,   null, "Unimplemented Line A"    ),
         new InterruptEntry("UnimplementedLineF",      11,   null, "Unimplemented Line F"    ),
         new InterruptEntry("NonPCBreakpoint",         12,   null, "Non PC Breakpoint"       ),         
         new InterruptEntry("PCBreakpoint",            13,   null, "PC Breakpoint"           ),            
         new InterruptEntry("FormatError",             14,   null, "Format Error"            ),             
         new InterruptEntry("Uninitialized",           15,   null, "Uninitialised Interrupt" ),  
         new InterruptEntry("SpuriousInt",             24,   null, "Spurious Interrupt"      ),             
         new InterruptEntry("AutoVector1",             25,   null, "Auto vector # 1"         ),             
         new InterruptEntry("AutoVector2",             26,   null, "Auto vector # 2"         ),             
         new InterruptEntry("AutoVector3",             27,   null, "Auto vector # 3"         ),             
         new InterruptEntry("AutoVector4",             28,   null, "Auto vector # 4"         ),             
         new InterruptEntry("AutoVector5",             29,   null, "Auto vector # 5"         ),             
         new InterruptEntry("AutoVector6",             30,   null, "Auto vector # 6"         ),             
         new InterruptEntry("AutoVector7",             31,   null, "Auto vector # 7"         ),             
         new InterruptEntry("Trap0",                   32,   null, "Trap # 0"                ),                   
         new InterruptEntry("Trap1",                   33,   null, "Trap # 1"                ),                   
         new InterruptEntry("Trap2",                   34,   null, "Trap # 2"                ),                   
         new InterruptEntry("Trap3",                   35,   null, "Trap # 3"                ),                   
         new InterruptEntry("Trap4",                   36,   null, "Trap # 4"                ),                   
         new InterruptEntry("Trap5",                   37,   null, "Trap # 5"                ),                   
         new InterruptEntry("Trap6",                   38,   null, "Trap # 6"                ),                   
         new InterruptEntry("Trap7",                   39,   null, "Trap # 7"                ),                   
         new InterruptEntry("Trap8",                   40,   null, "Trap # 8"                ),                   
         new InterruptEntry("Trap9",                   41,   null, "Trap # 9"                ),                   
         new InterruptEntry("Trap10",                  42,   null, "Trap # 10"               ),                  
         new InterruptEntry("Trap11",                  43,   null, "Trap # 11"               ),                  
         new InterruptEntry("Trap12",                  44,   null, "Trap # 12"               ),                  
         new InterruptEntry("Trap13",                  45,   null, "Trap # 13"               ),                  
         new InterruptEntry("Trap14",                  46,   null, "Trap # 14"               ),                  
         new InterruptEntry("Trap15",                  47,   null, "Trap # 15"               ),                                
   };

   /*
    * (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.VectorTable#addDefaultInterruptEntries()
    */
   @Override
   protected void addDefaultInterruptEntries() throws Exception {
      for (InterruptEntry i : defaultVectorTableEntries) {
         addEntry(i);
      }
   }

   static final String handlerTemplate          = "void %-40s WEAK_DEFAULT_HANDLER;\n";
   static final String vectorTableTypedef       =
         "typedef struct {\n"        +
         "   uint32_t *initialSP;\n" +
         "   intfunc  handlers[];\n" +
         "} VectorTable;\n\n";

   static final String vectorTableOpen     = 
         "extern VectorTable const __vector_table;\n\n"+
         "__attribute__ ((section(\".interrupt_vectors\")))\n"+
         "VectorTable const __vector_table = {\n"+
         "                                          /*  Exc# */\n"+
         "   &__StackTop,                           /*    0  Initial stack pointer                                                  */\n"+
         "   {\n"+
         "      __HardReset,                        /*    1  Reset Handler                                                          */\n";
   static final String vectorTableEntry    = 
         "      %-35s /* %4d  %-70s */\n";
   static final String vectorTableDeviceSeparator =
         "\n                                          /*   External Interrupts */\n";
   static final String vectorTableClose    = 
         "   }\n" +
         "};\n\n";
   
   /*
    * (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.VectorTable#writeCVectorTable(java.io.Writer)
    */
   @Override
   public void writeCVectorTable(Writer writer) throws Exception {

      // Add default entries
      if (addDefaultVectors) {
         addDefaultInterruptEntries();
         addDefaultVectors = false;
      }

      // Only list until ...
      int lastEntry = getLastUsedEntry();
      if (lastEntry<(FIRST_IRQ_INDEX+9)) {
         lastEntry = FIRST_IRQ_INDEX+9;
      }
      
      // Write out handler prototypes
      for (int index=2; index<=lastEntry; index++) {
         String handlerName = getHandlerName(index);
         // Exclude empty entries and the hard fault handler
         if ((handlerName != null) && (index != HARD_FAULT_NUMBER)) {
            writer.write(String.format(handlerTemplate, handlerName+"(void)"));
         }
      }
      writer.write('\n');
      
      // Write out vector table
      writer.write(vectorTableTypedef);
      writer.write(vectorTableOpen);
      
      for (int index=2; index<=lastEntry; index++) {
         if (index==FIRST_IRQ_INDEX) {
            writer.write(vectorTableDeviceSeparator);
         }
         String handlerName = getHandlerName(index);
         if (handlerName == null) {
            handlerName = DEFAULT_HANDLER_NAME;
         }
         writer.write(String.format(vectorTableEntry, handlerName+",", index, getHandlerDescription(index)));
      }
      writer.write(vectorTableClose);
   }
   
   /*
    * (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.VectorTable#writeCInterruptHeader(java.io.Writer)
    */
   @Override
   public void writeCInterruptHeader(Writer writer) throws Exception {

      if (addDefaultVectors) {
         addDefaultInterruptEntries();
         addDefaultVectors = false;
      }

      writer.write(INTERRUPT_BANNER);
      writer.write(INTERRUPT_PREAMBLE);
      writer.write(INTERRUPT_SEPARATOR);
      boolean separatorWritten = false;

      // Only list until ...
      int lastEntry = getLastUsedEntry();
      if (lastEntry<FIRST_IRQ_INDEX) {
         lastEntry = FIRST_IRQ_INDEX;
      }
      for (int index=1; index<=lastEntry; index++) {
         if (!separatorWritten && (index >= FIRST_IRQ_INDEX)) {
            writer.write(String.format(INTERRUPT_SPU_SEPARATOR, getCDescription()));
            separatorWritten = true;
         }
         String handlerName = getHandlerIrqEnum(index);
         if (handlerName != null) {
            writer.write(String.format(INTERRUPT_ENTRY_FORMAT, handlerName, index-VECTOR_OFFSET, index, getHandlerDescription(index)));
         }
      }
      writer.write(INTERRUPT_POSTAMBLE);
   
      writer.write(EXTERNAL_HANDLER_BANNER);

      for (int index=2; index<=lastEntry; index++) {
         String handlerName = getHandlerName(index);
         if (handlerName != null) {
            writer.write(String.format(EXTERNAL_HANDLER_TEMPLATE, handlerName+"(void);", getHandlerDescription(index)));
         }
      }
      writer.write('\n');
   }
}
