/*
 * utilities-mk.h
 *
 *  Created on: May 13, 2013
 *      Author: PODonoghue
 */
#ifndef UTILTIES_H_
#define UTILTIES_H_

#include "derivative.h"

// Used to create port register names in a configurable fashion
//-------------------------------------------------------------
#define CONCAT2_(x,y) x ## y
#define CONCAT3_(x,y,z) x ## y ## z
#define CONCAT4_(w,x,y,z) w ## x ## y ## z

#define PCR(port,num)   CONCAT4_(PORT,port,_PCR,num)
#define PDOR(port)      CONCAT3_(GPIO,port,_PDOR)
#define PSOR(port)      CONCAT3_(GPIO,port,_PSOR)
#define PCOR(port)      CONCAT3_(GPIO,port,_PCOR)
#define PTOR(port)      CONCAT3_(GPIO,port,_PTOR)
#define PDIR(port)      CONCAT3_(GPIO,port,_PDIR)
#define PDDR(port)      CONCAT3_(GPIO,port,_PDDR)

#ifdef NVIC_ISER

#define NVIC_PRIO_BITS (4) /* 4 Bits used for the Priority Levels    */

/*!
 * Enable IRQ channel in NVIC
 *
 * @param irqNum - IRQ number (16-255)
 */
static inline void NVIC_EnableIrq(int irqNum) {
   NVIC_ISER((irqNum-16)/32) = NVIC_ISER_SETENA(1<<((irqNum-16)%32));
}

/*!
 * Disable IRQ channel in NVIC
 *
 * @param irqNum - IRQ number (16-255)
 */
static inline void NVIC_DisableIrq(int irqNum) {
   NVIC_ICER((irqNum-16)/32) = NVIC_ICER_CLRENA(1<<((irqNum-16)%32));
}

/*!
 * Set the priority of an interrupt.
 *
 * @param intIndex - Interrupt Index (16-255)
 * @param priority – Priority to set (0-15 for Kinetis MK)
 */
static inline void NVIC_SetIrqPriority(int intIndex,
                                       uint32_t priority) {
   NVIC_IP(intIndex-16) =
         ((priority << (8 - NVIC_PRIO_BITS)) & 0xff);
}
#endif

/*!
 *  Wait for interrupt event - used to reduce power
 */
#define __wait_for_interrupt() __asm__("wfi")

/*!
 *  Debug breakpoint
 */
#define __breakpoint() __asm__("bkpt")

/*!
 *  Enable interrupts
 */
#define __enable_interrupt() __asm__("cpsie I")

/*!
 *  Disable interrupts
 */
#define __disable_interrupt() __asm__("cpsid I")

/*!
 *  Enable fault interrupts
 */
#define __enable_fault_interrupt() __asm__("cpsie F")

/*!
 *  Disable fault interrupts
 */
#define __disable_fault_interrupt() __asm__("cpsid F")

#endif /* UTILTIES_H_ */
