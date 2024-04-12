package net.sourceforge.usbdm.peripheralDatabase;


public class ArmCM4VectorTable extends ArmVectorTable {

   static final int VECTOR_OFFSET      = 16;
   static final int FIRST_IRQ_INDEX    = VECTOR_OFFSET;
   static final int HARD_FAULT_NUMBER  = 3;

   public ArmCM4VectorTable() {
      super();
   }

   InterruptEntry defaultVectorTableEntries[] = {
         new InterruptEntry("Reset",      -15,   null,   "Reset Vector, invoked on Power up and warm reset"),
         new InterruptEntry("NMI",        -14,   null,   "Non maskable Interrupt, cannot be stopped or preempted"),
         new InterruptEntry("HardFault",  -13,   null,   "Hard Fault, all classes of Fault"),
         new InterruptEntry("MemManage",  -12,   null,   "Memory Management, MPU mismatch, including Access Violation and No Match"),
         new InterruptEntry("BusFault",   -11,   null,   "Bus Fault, Pre-Fetch-, Memory Access Fault, other address/memory related Fault"),
         new InterruptEntry("UsageFault", -10,   null,   "Usage Fault, i.e. Undef Instruction, Illegal State Transition"),
         new InterruptEntry("SVC",         -5,   null,   "System Service Call via SVC instruction"),
         new InterruptEntry("DebugMon",    -4,   null,   "Debug Monitor"),
         new InterruptEntry("PendSV",      -2,   null,   "Pendable request for system service"),
         new InterruptEntry("SysTick",     -1,   null,   "System Tick Timer"),
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
}
