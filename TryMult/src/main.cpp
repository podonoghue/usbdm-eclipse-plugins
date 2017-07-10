/*
 * main.cpp
 *
 *  Created on: 10Jul.,2017
 *      Author: podonoghue
 */

#include <stdio.h>
#include <stdint.h>
#include <math.h>

#define E_NO_ERROR  0
#define E_TOO_SMALL 1
#define E_TOO_LARGE 2

int setErrorCode(int err) {
   return err;
}

static int getPdbDividers(float period, uint32_t &multValue, int &prescaleValue, uint32_t &mod) {

   // Multiplier factors for prescale divider
   static const int   multFactors[] = {1,10,20,40};

//   float inputClock = Info::getInputClockFrequency();
   float inputClock = 48e6;

   // No MOD value found so far
   mod = 0;

   // Try each divider multiplier
   for (unsigned trialMultValue=0; trialMultValue<(sizeof(multFactors)/sizeof(multFactors[0])); trialMultValue++) {
      int multfactor = multFactors[trialMultValue];

      // Try prescalers from smallest to largest
      // Find first prescaler for which a suitable modulo exists
      int prescaleFactor=1;
      for (unsigned trialPrescaleValue=0; trialPrescaleValue<=7; trialPrescaleValue++) {
         float clock = inputClock/(multfactor*prescaleFactor);
         uint32_t trialMod = round(period*clock)-1;
//         printf("multfactor=%2d, prescaleFactor = %3d, mod=%8d, period=%f\n", multfactor, prescaleFactor, trialMod, period);
         if (trialMod <= 0) {
            // Too short a period
            return E_TOO_SMALL;
         }
         if (trialMod <= 65535) {
            if (trialMod>mod) {
               // Better value - save
               prescaleValue = trialPrescaleValue;
               multValue     = trialMultValue;
               mod           = trialMod;
            }
            break;
         }
         prescaleFactor <<= 1;
      }
   }
   // Too long a period
   return setErrorCode((mod==0)?E_TOO_LARGE:E_NO_ERROR);
}

void testPdb() {
   static const float periods[] = {2.93e-5,1.3e-4,1.2e-3,1.2,5.0,6.99};

   int prescaleValue;
   uint32_t multValue;
   uint32_t modValue;
   for (unsigned periodIndex=0; periodIndex<(sizeof(periods)/sizeof(periods[0])); periodIndex++) {
      float period = periods[periodIndex];
      if (getPdbDividers(period, multValue, prescaleValue, modValue) == E_NO_ERROR) {
         printf("Found: multValue=%d, prescaleValue=%6d, modValue=%6d, period=%.5e\n", multValue, prescaleValue, modValue, period);
      }
      else {
         printf("Failed\n");
      }
   }
}


/**
 * Set period
 *
 * @param[in] period Period in seconds as a float
 *
 * @note Adjusts Timer pre-scaler to appropriate value.
 *       This will affect all channels of the timer.
 *
 * @return E_NO_ERROR  => success
 * @return E_TOO_SMALL => failed to find suitable values
 * @return E_TOO_LARGE => failed to find suitable values
 */
static int setPeriod(float period) {
   float inputClock = 48e6;
   int prescaleFactor=1;
   int prescalerValue=0;
   while (prescalerValue<=7) {
      float    clock = inputClock/prescaleFactor;
      uint32_t mod   = round(period*clock);
      if (mod < Info::minimumResolution) {
         // Too short a period for 1% resolution
         return setErrorCode(E_TOO_SMALL);
      }
      if (mod <= 65535) {
         // Clear SC so immediate effect on prescale change
         uint32_t sc = tmr->SC&~FTM_SC_PS_MASK;
         tmr->SC     = 0;
         __DSB();
         tmr->MOD    = mod;
         tmr->SC     = sc|FTM_SC_PS(prescalerValue);
         return E_NO_ERROR;
      }
      prescalerValue++;
      prescaleFactor <<= 1;
   }
   // Too long a period
   return setErrorCode(E_TOO_LARGE);
}



int main() {
   testPdb();
}


