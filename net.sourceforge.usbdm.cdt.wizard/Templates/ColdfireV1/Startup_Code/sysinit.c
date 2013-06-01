/*
 * sysinit.c
 *
 * Generic system initialization for Coldfire V1
 *
 *  Created on: 07/12/2012
 *      Author: podonoghue
 */

#include "derivative.h"
#include "stdint.h"

#define $(targetDeviceSubFamily)

$(cDeviceParameters)

void sysInit(void) {
   // This is generic initialization code
   // It may not be correct for a specific target

#ifdef SOPT1
   // Disable watch-dog
   SOPT1 = 0x00;
#endif
#ifdef SIM_COPC
   // Disable watch-dog
   SIM_COPC = 0x00;
#endif
}
