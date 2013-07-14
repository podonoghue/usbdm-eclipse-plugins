/*
 * start.S
 *
 * Generic start up code for Coldfire V1
 *
 * Copyright 2013 Peter O'Donoghue
 *
 * License GPL
 *
 * Based upon description in Codesourcery document Getting_Started.pdf
 *
 */

/*
 * Hard Reset Phase
 */
    .section .text.cs3.reset,"ax",%progbits
    .globl __cs3_reset, __cs3_start_asm
    .type __cs3_reset, %function
__cs3_reset:
    jmp    __cs3_start_asm

/*
 * Assembly Initialization Phase
 */
    .section .text.cs3.init,"ax",%progbits
    .globl __cs3_stack,__cs3_interrupt_vector_coldfire
    .globl _start, __cs3_start_asm
    .type __cs3_start_asm, %function
_start:
__cs3_start_asm:
   move.l #__cs3_stack,%sp

   /* Set the interrupt vector table position */
   move.l  #__cs3_interrupt_vector_coldfire,%d0
   movec.l %d0,%VBR

   jmp   __cs3_start_c
