package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.io.Writer;

public abstract class ArmVectorTable extends VectorTable {

   static final int VECTOR_OFFSET      = 16;
   static final int FIRST_IRQ_INDEX    = VECTOR_OFFSET;
   static final int HARD_FAULT_NUMBER  = 3;

   public ArmVectorTable() {
      super(VECTOR_OFFSET, FIRST_IRQ_INDEX);
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
         "                                     /*  Exc# Irq# */\n"+
         "   &__StackTop,                      /*    0   -16  Initial stack pointer                                                            */\n"+
         "   {\n"+
         "      __HardReset,                   /*    1   -15  Reset Handler                                                                    */\n";
   static final String vectorTableEntry    = 
         "      %-30s /* %4d, %4d  %-80s */\n";
   static final String vectorTableDeviceSeparator =
         "\n                                     /* External Interrupts */\n";
   static final String vectorTableClose    = 
         "   }\n" +
         "};\n\n";
   
   /*
    * (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.VectorTable#writeCVectorTable(java.io.Writer)
    */
   @Override
   public void writeCVectorTable(Writer writer) throws IOException {

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
            if (index<FIRST_IRQ_INDEX) {
               handlerName = "0";
            }
            else {
               handlerName = DEFAULT_HANDLER_NAME;
            }
         }
         writer.write(String.format(vectorTableEntry, handlerName+",", index, index-VECTOR_OFFSET, getHandlerDescription(index)));
      }
      writer.write(vectorTableClose);
   }
   
   /*
    * (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.VectorTable#writeCInterruptHeader(java.io.Writer)
    */
   @Override
   public void writeCInterruptHeader(Writer writer) throws IOException {

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
         String handlerName = getHandlerIrqNumber(index);
         if (handlerName != null) {
            writer.write(String.format(INTERRUPT_ENTRY_FORMAT, handlerName, index-VECTOR_OFFSET, index, getHandlerDescription(index)));
         }
      }
      writer.write(INTERRUPT_POSTAMBLE);
   
      writer.write(EXTERNAL_HANDLER_BANNER);

      for (int index=2; index<=lastEntry; index++) {
         String handlerName = getHandlerName(index);
         if (handlerName != null) {
            writer.write(String.format(EXTERNAL_HANDLER_TEMPLATE, handlerName+"(void)"));
         }
      }
      writer.write('\n');
   }
}
