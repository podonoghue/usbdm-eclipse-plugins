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
extern int const __cs3_interrupt_vector_arm[];

#ifndef SCB_VTOR
#define SCB_VTOR (*(uint32_t *)0xE000ED08)
#endif

#if defined(DEVICE_SUBFAMILY_CortexM0) && !defined(SIM_COPC)
   /* Defaults for MKL devices */

   /* COP timer register */
   #define SIM_COPC (*(uint32_t *)0x40048100)
#endif

#if defined(DEVICE_SUBFAMILY_CortexM4)
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

void sysInit(void) {
   /* This is generic initialization code */
   /* It may not be correct for a specific target */

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

#ifdef DEVICE_SUBFAMILY_CortexM4
   // Enable trapping of divide by zero and unaligned access
   SCB_CCR |= SCB_CCR_DIV_0_TRP_MASK|SCB_CCR_UNALIGN_TRP_MASK;
#endif
}
