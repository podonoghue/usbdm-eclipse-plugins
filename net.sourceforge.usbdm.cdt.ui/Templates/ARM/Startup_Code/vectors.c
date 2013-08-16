/*
 *  Vectors.c
 *
 *  Generic vectors and security for Kinetis
 *
 *  Created on: 07/12/2012
 *      Author: podonoghue
 */
#include <stdint.h>
#include <string.h>
#include "derivative.h"

#define $(targetDeviceSubFamily)

/*
 * Security information
 */
typedef struct {
    uint8_t  backdoorKey[8];
    uint32_t fprot;
    uint8_t  fsec;
    uint8_t  fopt;
    uint8_t  feprot;
    uint8_t  fdprot;
} SecurityInfo;

__attribute__ ((section(".security_information")))
const SecurityInfo securityInfo = {
    /* backdoor */ {0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF,0xFF},
    /* fprot    */ 0xFFFFFFFF,
    /* fsec     */ 0xFE,
    /* fopt     */ 0xFF,
    /* feprot   */ 0xFF,
    /* fdprot   */ 0xFF,
};

/*
 * Vector table related
 */
typedef void( *const intfunc )( void );

#define WEAK_DEFAULT_HANDLER __attribute__ ((__weak__, alias("Default_Handler")))

#ifndef SCB_ICSR
#define SCB_ICSR (*(volatile uint32_t*)(0xE000ED04))
#endif

__attribute__((__interrupt__))
void Default_Handler(void) {

   uint32_t vectorNum = SCB_ICSR;

   (void)vectorNum;

   while (1) {
      asm("bkpt #0");
   }
}

typedef struct {
   unsigned int r0;
   unsigned int r1;
   unsigned int r2;
   unsigned int r3;
   unsigned int r12;
   unsigned int lr;
   unsigned int pc;
   unsigned int psr;
} ExceptionFrame;

typedef struct {
   unsigned int scb_hfsr;
   unsigned int scb_cfsr;
   unsigned int scb_bfar;
} ExceptionInfo;

#ifdef DEVICE_SUBFAMILY_CortexM0
/*  Low-level exception handler
 *
 *  Interface from asm to C.
 *  Passes address of exception handler to C-level handler
 *
 *  See http://www.freertos.org/Debugging-Hard-Faults-On-Cortex-M-Microcontrollers.html
 */
__attribute__((__naked__, __weak__, __interrupt__))
void HardFault_Handler(void) {
   __asm volatile (
          "       mov r0,lr                                     \n"
          "       mov r1,#4                                     \n"
          "       and r0,r1                                     \n"
          "       bne skip1                                     \n"
          "       mrs r0,msp                                    \n"
          "       b   skip2                                     \n"
          "skip1:                                               \n"
          "       mrs r0,psp                                    \n"
          "skip2:                                               \n"
          "       nop                                           \n"
          "       ldr r2, handler_addr_const                    \n"
          "       bx r2                                         \n"
          "       handler_addr_const: .word _HardFault_Handler  \n"
      );
}
#endif
#if defined(DEVICE_SUBFAMILY_CortexM3) || defined(DEVICE_SUBFAMILY_CortexM3F) || \
    defined(DEVICE_SUBFAMILY_CortexM4) || defined(DEVICE_SUBFAMILY_CortexM4F)

/*  Low-level exception handler
 *
 *  Interface from asm to C.
 *  Passes address of exception handler to C-level handler
 *
 *  See http://www.freertos.org/Debugging-Hard-Faults-On-Cortex-M-Microcontrollers.html
 */
__attribute__((__naked__, __weak__, __interrupt__))
void HardFault_Handler(void) {
        __asm volatile ( "  tst   lr, #4              \n");  // Check mode
        __asm volatile ( "  ite   eq                  \n");  // Get active SP
        __asm volatile ( "  mrseq r0, msp             \n");
        __asm volatile ( "  mrsne r0, psp             \n");
//        __asm volatile ( "  ldr   r1,[r0,#24]         \n");  // PC
//        __asm volatile ( "  push  {r1}                \n");  // Dummy ?
        __asm volatile ( "  bl    _HardFault_Handler  \n");  // Go to C handler
}
#endif

/******************************************************************************/
/* Exception frame without floating-point storage
 * hard fault handler in C,
 * with stack frame location as input parameter
 *
 * @param exceptionFrame address of exception frame
 *
 */
void _HardFault_Handler(volatile ExceptionFrame *exceptionFrame) {
   (void)exceptionFrame;
#ifdef SCB_HFSR
   char reason[200] = "";
   volatile ExceptionInfo exceptionInfo = {0};
   exceptionInfo.scb_hfsr = SCB_HFSR;
   (void)exceptionInfo.scb_hfsr;
   if ((exceptionInfo.scb_hfsr&SCB_HFSR_FORCED_MASK) != 0) {
      // Forced
      exceptionInfo.scb_cfsr = SCB_CFSR;

      if (SCB_CFSR&SCB_CFSR_BFARVALID_MASK) {
         exceptionInfo.scb_bfar = SCB_BFAR;
      }
      /* CFSR Bit Fields */
      if (SCB_CFSR&SCB_CFSR_DIVBYZERO_MASK  ) { strcat(reason, "Divide by zero,"); }
      if (SCB_CFSR&SCB_CFSR_UNALIGNED_MASK  ) { strcat(reason, "Unaligned access,"); }
      if (SCB_CFSR&SCB_CFSR_NOCP_MASK       ) { strcat(reason, "No co-processor"); }
      if (SCB_CFSR&SCB_CFSR_INVPC_MASK      ) { strcat(reason, "Invalid PC (on return),"); }
      if (SCB_CFSR&SCB_CFSR_INVSTATE_MASK   ) { strcat(reason, "Invalid state (EPSR.T/IT,"); }
      if (SCB_CFSR&SCB_CFSR_UNDEFINSTR_MASK ) { strcat(reason, "Undefined Instruction,"); }
      if (SCB_CFSR&SCB_CFSR_BFARVALID_MASK  ) { strcat(reason, "BFAR contents valid,"); }
      if (SCB_CFSR&SCB_CFSR_LSPERR_MASK     ) { strcat(reason, "Bus fault on FP state save,"); }
      if (SCB_CFSR&SCB_CFSR_STKERR_MASK     ) { strcat(reason, "Bus fault on exception entry,"); }
      if (SCB_CFSR&SCB_CFSR_UNSTKERR_MASK   ) { strcat(reason, "Bus fault on exception return,"); }
      if (SCB_CFSR&SCB_CFSR_IMPRECISERR_MASK) { strcat(reason, "Imprecise data access error,"); }
      if (SCB_CFSR&SCB_CFSR_PRECISERR_MASK  ) { strcat(reason, "Precise data access error,"); }
      if (SCB_CFSR&SCB_CFSR_IBUSERR_MASK    ) { strcat(reason, "Bus fault on instruction pre-fetch,"); }
      if (SCB_CFSR&SCB_CFSR_MMARVALID_MASK  ) { strcat(reason, "MMAR contents valid,"); }
      if (SCB_CFSR&SCB_CFSR_MLSPERR_MASK    ) { strcat(reason, "MemManage fault on FP state save,"); }
      if (SCB_CFSR&SCB_CFSR_MSTKERR_MASK    ) { strcat(reason, "MemManage fault on exception entry,"); }
      if (SCB_CFSR&SCB_CFSR_MUNSTKERR_MASK  ) { strcat(reason, "MemManage fault on exception return,"); }
      if (SCB_CFSR&SCB_CFSR_DACCVIOL_MASK   ) { strcat(reason, "MemManage access violation on data access,"); }
      if (SCB_CFSR&SCB_CFSR_IACCVIOL_MASK   ) { strcat(reason, "MemManage access violation on instruction fetch,"); }
   }
#endif
   while (1) {
      asm("bkpt #0");
   }
}

void __HardReset(void) __attribute__((__interrupt__));
extern uint32_t __StackTop;

/*
 * Each vector is assigned an unique name.  This is then 'weakly' assigned to the
 * default handler.
 * To install a handler, create a function with the name shown and it will override
 * the weak default.
 */
void NMI_Handler(void)            WEAK_DEFAULT_HANDLER;
void MemManage_Handler(void)      WEAK_DEFAULT_HANDLER;
void BusFault_Handler(void)       WEAK_DEFAULT_HANDLER;
void UsageFault_Handler(void)     WEAK_DEFAULT_HANDLER;
void SVC_Handler(void)            WEAK_DEFAULT_HANDLER;
void DebugMon_Handler(void)       WEAK_DEFAULT_HANDLER;
void PendSV_Handler(void)         WEAK_DEFAULT_HANDLER;
void SysTick_Handler(void)        WEAK_DEFAULT_HANDLER;

void Int0_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int1_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int2_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int3_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int4_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int5_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int6_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int7_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int8_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int9_Handler(void)           WEAK_DEFAULT_HANDLER;
void Int10_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int11_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int12_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int13_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int14_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int15_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int16_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int17_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int18_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int19_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int20_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int21_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int22_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int23_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int24_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int25_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int26_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int27_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int28_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int29_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int30_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int31_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int32_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int33_Handler(void)          WEAK_DEFAULT_HANDLER;
void Int34_Handler(void)          WEAK_DEFAULT_HANDLER;

typedef struct {
   uint32_t *initialSP;
   intfunc  handlers[];
} VectorTable;

__attribute__ ((section(".interrupt_vectors")))
VectorTable const __vector_table = {
    &__StackTop,                      /* Vec #0   Initial stack pointer                        */
    {
          __HardReset,              /* Vec #1   Reset Handler                                */
          NMI_Handler,              /* Vec #2   NMI Handler                                  */
(intfunc) HardFault_Handler,        /* Vec #3   Hard Fault Handler                           */
          MemManage_Handler,        /* Vec #4   MPU Fault Handler                            */
          BusFault_Handler,         /* Vec #5   Bus Fault Handler                            */
          UsageFault_Handler,       /* Vec #6   Usage Fault Handler                          */
          Default_Handler,          /* Vec #7   Reserved                                     */
          Default_Handler,          /* Vec #8   Reserved                                     */
          Default_Handler,          /* Vec #9   Reserved                                     */
          Default_Handler,          /* Vec #10  Reserved                                     */
          SVC_Handler,              /* Vec #11  SVCall Handler                               */
          DebugMon_Handler,         /* Vec #12  Debug Monitor Handler                        */
          Default_Handler,          /* Vec #13  Reserved                                     */
          PendSV_Handler,           /* Vec #14  PendSV Handler                               */
          SysTick_Handler,          /* Vec #15  SysTick Handler                              */

                                    /* External Interrupts */
          Int0_Handler,             /* Int #0  */
          Int1_Handler,             /* Int #1  */
          Int2_Handler,             /* Int #2  */
          Int3_Handler,             /* Int #3  */
          Int4_Handler,             /* Int #4  */
          Int5_Handler,             /* Int #5  */
          Int6_Handler,             /* Int #6  */
          Int7_Handler,             /* Int #7  */
          Int8_Handler,             /* Int #8  */
          Int9_Handler,             /* Int #9  */
          Int10_Handler,            /* Int #10 */
          Int11_Handler,            /* Int #11 */
          Int12_Handler,            /* Int #12 */
          Int13_Handler,            /* Int #13 */
          Int14_Handler,            /* Int #14 */
          Int15_Handler,            /* Int #15 */
          Int16_Handler,            /* Int #16 */
          Int17_Handler,            /* Int #17 */
          Int18_Handler,            /* Int #18 */
          Int19_Handler,            /* Int #19 */
          Int20_Handler,            /* Int #20 */
          Int21_Handler,            /* Int #21 */
          Int22_Handler,            /* Int #22 */
          Int23_Handler,            /* Int #23 */
          Int24_Handler,            /* Int #24 */
          Int25_Handler,            /* Int #25 */
          Int26_Handler,            /* Int #26 */
          Int27_Handler,            /* Int #27 */
          Int28_Handler,            /* Int #28 */
          Int29_Handler,            /* Int #29 */
          Int30_Handler,            /* Int #30 */
          Int31_Handler,            /* Int #31 */
          Int32_Handler,            /* Int #32 */
          Int33_Handler,            /* Int #33 */
          Int34_Handler,            /* Int #34 */
    }
};
