package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.io.Writer;

import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry.Mode;

public abstract class ArmVectorTable extends VectorTable {

   static final int VECTOR_OFFSET      = 16;
   static final int FIRST_IRQ_INDEX    = VECTOR_OFFSET;
   static final int HARD_FAULT_NUMBER  = 3;

   public ArmVectorTable() {
      super(VECTOR_OFFSET, FIRST_IRQ_INDEX);
   }

   static final String resetHandlerPrototype = 
         "#ifdef __cplusplus\n"+
               "extern \"C\" {\n"+
               "#endif\n"+
               "// Reset handler must have C linkage\n"+
               "void Reset_Handler(void);\n"+
               "#ifdef __cplusplus\n"+
               "}\n"+
               "#endif\n";

   static final String handlerTemplate          = "void %-40s WEAK_DEFAULT_HANDLER;\n";
   static final String vectorTableTypedef       =
         "typedef struct {\n"        +
               "   uint32_t *initialSP;\n" +
               "   intfunc  handlers[%d];\n" +
               "} VectorTable;\n\n";

   static final String vectorTableOpen     = 
         "extern VectorTable const __vector_table;\n\n"+
               "__attribute__ ((section(\".interrupt_vectors\")))\n"+
               "VectorTable const __vector_table = {\n"+
               "                                               /*  Exc# Irq# */\n"+
               "   &__StackTop,                                /*    0   -16  Initial stack pointer                                                            */\n"+
               "   {\n";
   static final String vectorTableEntry    = 
         "      %-40s /* %4d, %4d  %-80s */\n";
   static final String vectorTableDeviceSeparator =
         "\n                                               /* External Interrupts */\n";
   static final String vectorTableClose    = 
         "   }\n" +
               "};\n\n";

   /**
    * Write Vector table entry
    * 
    * @param writer     Write to use
    * @param handlerName
    * @param index
    * @throws IOException
    */
   private void writeVectorTableEntry(Writer writer, String handlerName, int index) throws IOException {
      final String vectorTableEntry    = 
            "      %-40s /* %4d, %4d  %-80s */\n";
      writer.write(String.format(vectorTableEntry, handlerName+",", index, index-VECTOR_OFFSET, getHandlerDescription(index)));
   }

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

      writer.write(resetHandlerPrototype);

      // Only list until ...
      int lastEntry = getLastUsedEntry();
      if (lastEntry<(FIRST_IRQ_INDEX+9)) {
         lastEntry = FIRST_IRQ_INDEX+9;
      }

      // Write out handler prototypes
      for (int index=2; index<=lastEntry; index++) {
         if ((interrupts[index] == null) || (interrupts[index].getHandlerMode() == Mode.ClassMethod)) {
            // No prototype if entry is empty or using a C++ class member handler
            continue;
         }
         String handlerName = getHandlerName(index);
         // Exclude empty entries and the hard fault handler
         if ((handlerName != null) && (index != HARD_FAULT_NUMBER)) {
            writer.write(String.format(handlerTemplate, handlerName+"(void)"));
         }
      }
      writer.write('\n');

      // Write out vector table
      writer.write(String.format(vectorTableTypedef, lastEntry));
      writer.write(vectorTableOpen);

      for (int index=1; index<=lastEntry; index++) {
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
         writeVectorTableEntry(writer, handlerName, index);
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

      writeGroupPreamble(writer, "Interrupt_vector_numbers", "Interrupt vector numbers", "Vector numbers required for NVIC functions");

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

      writeGroupPostamble(writer, "Interrupt_vector_numbers");

      writeGroupPreamble(writer, "Interrupt_handler_prototypes", "Interrupt handler prototypes", "Prototypes for interrupt handlers");
      writer.write(EXTERNAL_HANDLER_BANNER);

      for (int index=2; index<=lastEntry; index++) {
         String handlerName = getHandlerName(index);
         if (handlerName != null) {
            writer.write(String.format(EXTERNAL_HANDLER_TEMPLATE, handlerName+"(void);", getHandlerDescription(index)));
         }
      }
      writer.write('\n');

      writeGroupPostamble(writer, "Interrupt_handler_prototypes");
   }
}
