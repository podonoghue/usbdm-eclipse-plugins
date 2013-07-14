/*
 ============================================================================
 Name        : main.cpp
 Author      : $(author)
 Version     :
 Copyright   : $(copyright)
 Description : Basic CPP Main
 ============================================================================
 */
#include "derivative.h"

volatile int count = 0;

/*
 * Example use of interrupt handler
 *
 * See vectors.c for interrupt names
 */
extern "C" __attribute__((__interrupt__))
void AccessError_Handler(void) {
   for(;;) {
      asm("halt");
   }
}

int main(void) {

   for(count =0; count < 100; count++) {
      asm("nop");
   }
   // Generate Access error (interrupt handler demonstration)
   // (*(unsigned int *) 101) = 100;

   for(;;) {
      asm("nop");
   }
}
