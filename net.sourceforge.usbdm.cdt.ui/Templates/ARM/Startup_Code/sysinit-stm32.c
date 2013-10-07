/*
 * sysinit-stm.c
 *
 * Generic system initialization for STM32xxx family
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

/* This definition is overridden if Standard Peripheral Library is used */
__attribute__((__weak__))
void SystemInit() {
   /* Set the interrupt vector table position */
   SCB_VTOR = (uint32_t)__vector_table;

   /* Enable trapping of divide by zero and unaligned access */
   SCB_CCR |= SCB_CCR_DIV_0_TRP_MASK|SCB_CCR_UNALIGN_TRP_MASK;
}

/* This definition is overridden if Clock initialisation is provided */
__attribute__((__weak__))
void clock_initialise() {
}

/* This definition is overridden if UART initialisation is provided */
__attribute__((__weak__))
void uart_initialise(int baudRate) {
   (void)baudRate;
}

void sysInit(void) {
   /* This is generic initialization code */
   /* It may not be correct for a specific target */

   /* Use SPL initialization - if present */
   SystemInit();
   
   /* Use FPU initialisation - if present */
   fpu_init();
   
   /* Use Clock initialisation - if present */
   clock_initialise();

   /* Use UART initialisation - if present */
   uart_initialise(19200);
}

void _exit(int i) {
   (void)i;
   while (1) {
      asm("bkpt #0");
   }
}

