package net.sourceforge.usbdm.peripheralDatabase;


public class ArmCM0VectorTable extends ArmVectorTable {

   static final int VECTOR_OFFSET      = 16;
   static final int FIRST_IRQ_INDEX    = VECTOR_OFFSET;
   static final int HARD_FAULT_NUMBER  = 3;

   public ArmCM0VectorTable() {
      super();
   }

   InterruptEntry defaultVectorTableEntries[] = {
         new InterruptEntry("Reset",             -15,   null,         "Reset Vector, invoked on Power up and warm reset"),
         new InterruptEntry("NonMaskableInt",    -14,   "NMI",        "Non maskable Interrupt, cannot be stopped or preempted"),
         new InterruptEntry("HardFault",         -13,   null,         "Hard Fault, all classes of Fault"),
         new InterruptEntry("SVCall",             -5,   "SVC",        "System Service Call via SVC instruction"),
         new InterruptEntry("PendSV",             -2,   null,         "Pendable request for system service"),
         new InterruptEntry("SysTick",            -1,   null,         "System Tick Timer"),
   };

   /*
    * (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.VectorTable#addDefaultInterruptEntries()
    */
   @Override
   protected void addDefaultInterruptEntries() {
      for (InterruptEntry i : defaultVectorTableEntries) {
         addEntry(i);
      }
   }
}
