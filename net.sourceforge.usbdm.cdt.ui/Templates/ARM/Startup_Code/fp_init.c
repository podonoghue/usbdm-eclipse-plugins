/*
 * fp_init.c
 *
 * Initialisation of FP unit
 *
 *  Created on: 07/12/2012
 *      Author: podonoghue
 */

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

