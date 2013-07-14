/*
 * sysinit.c
 *
 * Generic system initialization for Kinetis
 *
 *  Created on: 07/12/2012
 *      Author: podonoghue
 */

#include <stdint.h>
#include "derivative.h"

#define $(targetDeviceSubFamily)

$(cDeviceParameters)

/* Actual Vector table */
extern int const __vector_table[];

#ifndef SCB_VTOR
#define SCB_VTOR (*(uint32_t *)0xE000ED08)
#endif

#if defined(DEVICE_SUBFAMILY_CortexM0) && !defined(SIM_COPC)
   /* Defaults for MKL devices */

   /* COP timer register */
   #define SIM_COPC (*(uint32_t *)0x40048100)
#endif

#if defined(DEVICE_SUBFAMILY_CortexM4) || defined(DEVICE_SUBFAMILY_CortexM4F)
#if !defined(WDOG_UNLOCK)
   /* Defaults for MK devices */

   /* Watchdog unlock register */
   #define WDOG_BASE_ADDR (0x40052000)
   #define WDOG_STCTRLH  (*(uint16_t *)(WDOG_BASE_ADDR+0x00))
   #define WDOG_UNLOCK   (*(uint16_t *)(WDOG_BASE_ADDR+0x0E))
#endif
   /* Unlocking Watchdog sequence words*/
   #define KINETIS_WDOG_UNLOCK_SEQ_1   0xC520
   #define KINETIS_WDOG_UNLOCK_SEQ_2   0xD928

   /* Word to disable the Watchdog */
   #define KINETIS_WDOG_DISABLED_CTRL  (0xD2)

#ifndef SCB_CCR
   #define SCB_CCR                  (*(uint32_t *)(0xE000ED14))
   #define SCB_CCR_DIV_0_TRP_MASK   (1<<4)
   #define SCB_CCR_UNALIGN_TRP_MASK (1<<3)
#endif
#endif

#if defined(DEVICE_SUBFAMILY_CortexM4F)
void fpu_init() {
   asm (
      "  .equ CPACR, 0xE000ED88     \n"
      "                             \n"
      "  LDR.W R0, =CPACR           \n"  // CPACR address
      "  LDR R1, [R0]               \n"  // Read CPACR
      "  ORR R1, R1, #(0xF << 20)   \n"  // Enable CP10 and CP11 coprocessors
      "  STR R1, [R0]               \n"  // Write back the modified value to the CPACR
      "  DSB                        \n"  // Wait for store to complete"
      "  ISB                        \n"  // Reset pipeline now the FPU is enabled
   );
}
#endif

void sysInit(void) {
   /* This is generic initialization code */
   /* It may not be correct for a specific target */

   /* Set the interrupt vector table position */
   SCB_VTOR = (uint32_t)__vector_table;

#ifdef WDOG_UNLOCK
   // Disable watch-dog
   WDOG_UNLOCK  = KINETIS_WDOG_UNLOCK_SEQ_1;
   WDOG_UNLOCK  = KINETIS_WDOG_UNLOCK_SEQ_2;
   WDOG_STCTRLH = KINETIS_WDOG_DISABLED_CTRL;
#endif

#ifdef SIM_COPC
   // Disable watch-dog
   SIM_COPC = 0x00;
#endif

#if defined(DEVICE_SUBFAMILY_CortexM4) || defined(DEVICE_SUBFAMILY_CortexM4F)
   // Enable trapping of divide by zero and unaligned access
   SCB_CCR |= SCB_CCR_DIV_0_TRP_MASK|SCB_CCR_UNALIGN_TRP_MASK;
#endif

#if defined(DEVICE_SUBFAMILY_CortexM4F)
   fpu_init();
#endif

//   clock_initialise();
}

void _exit(int i) {
   (void)i;
   while (1) {
      asm("bkpt #0");
   }
}

