package net.sourceforge.usbdm.cmsis.view;

import java.util.ArrayList;

public class ProcessModel {
   
   public enum ControlBlockTypeEnum {
      TCB(  0, "TCB"),
      MCB(  1, "MCB"),
      SCB(  2, "SCB"),
      MUCB( 3, "MUCB"),
      HCB(  4, "HCB"),
      ;
      
      final int      value;
      final String   name;
      
      ControlBlockTypeEnum(int value, String name) {
         this.value = value;
         this.name  = name;
      }
      
      public String toString() {
         return name;
      }
   }
   
   public enum ProcessStateEnum {
      INACTIVE(   0, "INACTIVE"),
      READY(      1, "READY"),
      RUNNING(    2, "RUNNING"),
      WAIT_DLY(   3, "WAIT_DLY"),
      WAIT_ITV(   4, "WAIT_ITV"),
      WAIT_OR(    5, "WAIT_OR"),
      WAIT_AND(   6, "WAIT_AND"),
      WAIT_SEM(   7, "WAIT_SEM"),
      WAIT_MBX(   8, "WAIT_MBX"),
      WAIT_MUT(   9, "WAIT_MUT"),
      ;
      
      final int      value;
      final String   name;
      
      ProcessStateEnum(int value, String name) {
         this.value = value;
         this.name  = name;
      }
      
      public String toString() {
         return name;
      }
   }

   
   /* General part: identical for all implementations.                          */
   ControlBlockTypeEnum  cb_type;                 /* Control Block Type                      */
   ProcessStateEnum      state;                   /* Task state                              */
   byte                  prio;                    /* Execution priority                      */
   byte                  task_id;                 /* Task ID value for optimized TCB access  */
   long                  p_lnk;                   /* Link pointer for ready/sem. wait list   */
   long                  p_rlnk;                  /* Link pointer for sem./mbx lst backwards */
   long                  p_dlnk;                  /* Link pointer for delay list             */
   long                  p_blnk;                  /* Link pointer for delay list backwards   */
   int                   delta_time;              /* Time until time out                     */
   int                   interval_time;           /* Time interval for periodic waits        */
   int                   events;                  /* Event flags                             */
   int                   waits;                   /* Wait flags                              */
   /* Hardware dependent part: specific for CM processor                        */
   byte                  stack_frame;             /* Stack frame: 0=Basic, 1=Extended        */
   byte                  reserved;
   int                   priv_stack;              /* Private stack size, 0= system assigned  */
   long                  tsk_stack;               /* Current task Stack pointer (R13)        */
   long                  stack;                   /* Pointer to Task Stack memory block      */

   /* Task entry point used for uVision debugger                                */
   long     ptask;                   /* Task entry address                      */
   
   private ArrayList<ProcessModel> children;
   
   public ProcessModel() {
      this(10);
   }
   
   public ProcessModel(int numchildren) {
      this.cb_type = ControlBlockTypeEnum.TCB;
      this.state   = ProcessStateEnum.INACTIVE;
      this.children = new ArrayList<ProcessModel>();
      for (int sub=0; sub < numchildren; sub++) {
         children.add(new ProcessModel(0));
      }
   }
   
   public ProcessModel[] getChildren() {
      return children.toArray(new ProcessModel[children.size()]);
   }
}

