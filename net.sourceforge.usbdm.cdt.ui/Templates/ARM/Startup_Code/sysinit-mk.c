/*
 * sysinit-mk.c
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

/* This definition is overridden if FPU is present */
__attribute__((__weak__))
void fpu_init() {
}

#ifndef SCB_VTOR
#define SCB_VTOR (*(uint32_t *)0xE000ED08)
#endif

#ifndef SCB_CCR
   #define SCB_CCR                  (*(uint32_t *)(0xE000ED14))
   #define SCB_CCR_DIV_0_TRP_MASK   (1<<4)
   #define SCB_CCR_UNALIGN_TRP_MASK (1<<3)
#endif

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

/* This definition is overridden if Clock initialisation is provided */
__attribute__((__weak__))
void clock_initialise() {
}

void sysInit(void) {
   /* This is generic initialization code */
   /* It may not be correct for a specific target */

   /* Set the interrupt vector table position */
   SCB_VTOR = (uint32_t)__vector_table;

   // Disable watch-dog
   WDOG_UNLOCK  = KINETIS_WDOG_UNLOCK_SEQ_1;
   WDOG_UNLOCK  = KINETIS_WDOG_UNLOCK_SEQ_2;
   WDOG_STCTRLH = KINETIS_WDOG_DISABLED_CTRL;

   // Enable trapping of divide by zero and unaligned access
   SCB_CCR |= SCB_CCR_DIV_0_TRP_MASK|SCB_CCR_UNALIGN_TRP_MASK;

   /* Use FPU initialization - if present */
   fpu_init();
   
   /* Use Clock initialization - if present */
   clock_initialise();
}

void _exit(int i) {
   (void)i;
   while (1) {
      asm("bkpt #0");
   }
}

