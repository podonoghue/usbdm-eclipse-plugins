/*
 * main.cpp
 *
 *  Created on: 04/12/2012
 *      Author: podonoghue
 */

#include "derivative.h"


// Used to create port register names
//--------------------------------------------------------
#define CONCAT2_(x,y) x ## y
#define CONCAT3_(x,y,z) x ## y ## z
#define CONCAT4_(w,x,y,z) w ## x ## y ## z

#define PCR(reg,num)   CONCAT4_(PORT,reg,_PCR,num)
#define PDOR(reg)      CONCAT3_(GPIO,reg,_PDOR)
#define PSOR(reg)      CONCAT3_(GPIO,reg,_PSOR)
#define PCOR(reg)      CONCAT3_(GPIO,reg,_PCOR)
#define PTOR(reg)      CONCAT3_(GPIO,reg,_PTOR)
#define PDIR(reg)      CONCAT3_(GPIO,reg,_PDIR)
#define PDDR(reg)      CONCAT3_(GPIO,reg,_PDDR)

//=================================================================================
// LED Port bit masks
//
#define GREEN_LED_NUM         19
#define GREEN_LED_REG         B
#define GREEN_LED_MASK        (1<<GREEN_LED_NUM)
#define GREEN_LED_PCR         PCR(GREEN_LED_REG,GREEN_LED_NUM)
#define GREEN_LED_PDOR        PDOR(GREEN_LED_REG)
#define GREEN_LED_PSOR        PSOR(GREEN_LED_REG)  // Data set
#define GREEN_LED_PCOR        PCOR(GREEN_LED_REG)  // Data clear
#define GREEN_LED_PTOR        PTOR(GREEN_LED_REG)  // Data toggle
#define GREEN_LED_PDIR        PDIR(GREEN_LED_REG)  // Data input
#define GREEN_LED_PDDR        PDDR(GREEN_LED_REG)  // Data direction

#define GREEN_LED_ON()        (GREEN_LED_PCOR = GREEN_LED_MASK)
#define GREEN_LED_OFF()       (GREEN_LED_PSOR = GREEN_LED_MASK)
#define GREEN_LED_TOGGLE()    (GREEN_LED_PTOR = GREEN_LED_MASK)

#define RED_LED_NUM           18
#define RED_LED_REG           B
#define RED_LED_MASK          (1<<RED_LED_NUM)
#define RED_LED_PCR           PCR(RED_LED_REG,RED_LED_NUM)
#define RED_LED_PDOR          PDOR(RED_LED_REG)
#define RED_LED_PSOR          PSOR(RED_LED_REG)  // Data set
#define RED_LED_PCOR          PCOR(RED_LED_REG)  // Data clear
#define RED_LED_PTOR          PTOR(RED_LED_REG)  // Data toggle
#define RED_LED_PDIR          PDIR(RED_LED_REG)  // Data input
#define RED_LED_PDDR          PDDR(RED_LED_REG)  // Data direction

#define RED_LED_ON()          (RED_LED_PCOR = RED_LED_MASK)
#define RED_LED_OFF()         (RED_LED_PSOR = RED_LED_MASK)
#define RED_LED_TOGGLE()      (RED_LED_PTOR = RED_LED_MASK)

void initLEDs(void) {
   GREEN_LED_OFF();
   RED_LED_OFF();
   GREEN_LED_PDDR |= GREEN_LED_MASK;
   GREEN_LED_PCR   = PORT_PCR_MUX(1)|PORT_PCR_DSE_MASK|PORT_PCR_PE_MASK|PORT_PCR_PS_MASK;
   RED_LED_PDDR   |= RED_LED_MASK;
   RED_LED_PCR     = PORT_PCR_MUX(1)|PORT_PCR_DSE_MASK|PORT_PCR_PE_MASK|PORT_PCR_PS_MASK;
}

void initPorts() {
   // Enable all port clocks
   SIM_SCGC5 |=   SIM_SCGC5_PORTA_MASK
                | SIM_SCGC5_PORTB_MASK
                | SIM_SCGC5_PORTC_MASK
                | SIM_SCGC5_PORTD_MASK
                | SIM_SCGC5_PORTE_MASK;

}

void delay(void) {
   volatile unsigned long i;
   for (i=800000; i>0; i--) {
      asm("nop");
   }
}

int count = 0;

int main(void) {
   initPorts();
   initLEDs();
   GREEN_LED_ON();
   for(;;) {
      GREEN_LED_TOGGLE();
      delay();
      RED_LED_TOGGLE();
      delay();
   }
}


