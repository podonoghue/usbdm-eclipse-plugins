/*
 * sysinit.c
 *
 *  Created on: 07/12/2012
 *      Author: podonoghue
 */

#include "derivative.h"
#include "clock.h"

/* Unlocking Watchdog sequence words*/
#define KINETIS_WDOG_UNLOCK_SEQ_1   0xC520
#define KINETIS_WDOG_UNLOCK_SEQ_2   0xD928

/* Word to disable the Watchdog */
#define KINETIS_WDOG_DISABLED_CTRL  (WDOG_STCTRLH_WAITEN_MASK|WDOG_STCTRLH_STOPEN_MASK|WDOG_STCTRLH_ALLOWUPDATE_MASK|WDOG_STCTRLH_CLKSRC_MASK) // 0xD2

/* Actual Vector table */
extern int const __cs3_interrupt_vector_arm[];

void sysInit(void) {
   /* Set the interrupt vector table position */
   SCB_VTOR = (uint32_t)__cs3_interrupt_vector_arm;

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

   // Initialize clock
   initClock();
}
