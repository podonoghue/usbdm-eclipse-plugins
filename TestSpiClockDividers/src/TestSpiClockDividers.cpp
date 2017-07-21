//============================================================================
// Name        : TestSpiClockDividers.cpp
// Author      : 
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include <stdio.h>
#include <math.h>
#include <float.h>
#include <stdint.h>

#define SPI_CTAR_DBR_MASK                        (0x80000000U)                                       /*!< SPI0_CTAR.DBR Mask                      */
#define SPI_CTAR_DBR_SHIFT                       (31U)                                               /*!< SPI0_CTAR.DBR Position                  */
#define SPI_CTAR_DBR(x)                          (((uint32_t)(((uint32_t)(x))<<31U))&0x80000000UL)   /*!< SPI0_CTAR.DBR Field                     */
#define SPI_CTAR_PBR_MASK                        (0x30000U)                                          /*!< SPI0_CTAR.PBR Mask                      */
#define SPI_CTAR_PBR_SHIFT                       (16U)                                               /*!< SPI0_CTAR.PBR Position                  */
#define SPI_CTAR_PBR(x)                          (((uint32_t)(((uint32_t)(x))<<16U))&0x30000UL)      /*!< SPI0_CTAR.PBR Field                     */
#define SPI_CTAR_BR_MASK                         (0xFU)                                              /*!< SPI0_CTAR.BR Mask                       */
#define SPI_CTAR_BR_SHIFT                        (0U)                                                /*!< SPI0_CTAR.BR Position                   */
#define SPI_CTAR_BR(x)                           (((uint32_t)(((uint32_t)(x))<<0U))&0xFUL)           /*!< SPI0_CTAR.BR Field                      */

static const uint16_t pbrFactors[] = {2,3,5,7};
static const uint16_t brFactors[]  = {2,4,6,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768};

/**
 * Calculate communication speed factors for SPI
 *
 * @param[in]  clockFrequency => Clock frequency of SPI in Hz
 * @param[in]  frequency      => Communication frequency in Hz
 *
 * @return CTAR register value only including SPI_CTAR_BR, SPI_CTAR_PBR fields
 *
 * Note: Chooses the highest speed that is not greater than frequency.
 */
uint32_t calculateDividers(uint32_t clockFrequency, uint32_t frequency) {

   if (clockFrequency <= (2*frequency)) {
      // Use highest possible rate
      return SPI_CTAR_DBR_MASK;
   }
   int bestPBR = 3;
   int bestBR  = 7;
   int32_t bestDifference = 0x7FFFFFFF;
   for (int pbr = 3; pbr >= 0; pbr--) {
      for (int br = 15; br >= 0; br--) {
         uint32_t calculatedFrequency = clockFrequency/(pbrFactors[pbr]*brFactors[br]);
         int32_t difference = frequency-calculatedFrequency;
         if (difference < 0) {
            // Too high stop looking here
            break;
         }
         if (difference < bestDifference) {
            // New "best value"
            bestDifference = difference;
            bestBR  = br;
            bestPBR = pbr;
         }
      }
   }
   uint32_t clockFactors = SPI_CTAR_BR(bestBR)|SPI_CTAR_PBR(bestPBR);
   if ((clockFactors == 0) && (clockFrequency<=(2*frequency))) {
      // Use highest possible rate
      clockFactors = SPI_CTAR_DBR_MASK;
   }
   return clockFactors;
}
float calcFreq(float clockFrequency, uint32_t divider) {
   int dbr = (divider&SPI_CTAR_DBR_MASK)?2:1;
   int pbr = (divider&SPI_CTAR_PBR_MASK)>>SPI_CTAR_PBR_SHIFT;
   int br  = (divider&SPI_CTAR_BR_MASK)>>SPI_CTAR_BR_SHIFT;
   return dbr * clockFrequency/(pbrFactors[pbr]*brFactors[br]);
}

/**
 * Calculate Delay factors
 * Used for ASC, DT and CSSCK
 *
 * @param[in]  delay          => Desired delay in seconds
 * @param[in]  clockFrequency => Clock frequency of SPI in Hz
 * @param[out] bestPrescale   => Best prescaler value (0=>/1, 1=>/3, 2=/5, 3=>/7)
 * @param[out] bestDivider    => Best divider value (N=>/(2**(N+1)))
 *
 * Note: Determines bestPrescaler and bestDivider for the smallest delay that is not less than delay.
 */
void calculateDelay(float clockFrequency, float delay, int &bestPrescale, int &bestDivider) {
   const float clockPeriod = (1/clockFrequency);
   float bestDifference = FLT_MAX;

   bestPrescale = 0;
   bestDivider  = 0;
   for (int prescale = 3; prescale >= 0; prescale--) {
      for (int divider = 15; divider >= 0; divider--) {
         float calculatedDelay = clockPeriod*((prescale<<1)+1)*(1UL<<(divider+1));
         float difference = calculatedDelay - delay;
         if (difference < 0) {
            // Too short - stop looking here
            break;
         }
         if (difference < bestDifference) {
            // New "best delay"
            bestDifference = difference;
            bestPrescale = prescale;
            bestDivider  = divider;
         }
      }
   }
}

float calcDelay(float clockFrequency, int prescale, int divider) {
   const float clockPeriod = (1/clockFrequency);
   return clockPeriod*((prescale<<1)+1)*(1UL<<(divider+1));
}

static float ns = 1e-9;
static float us = 1e-6;
static float ms = 1e-3;
static float  s = 1.0;

static float MHz = 1e6;
static float kHz = 1e3;
static float Hz  = 1.0;

float clockFrequency;
float delay;
int bestPrescale;
int bestDivider;

const char *format(float d) {

   static char buf[100];

   float multipliers[] = {1e9, 1e6, 1e3, 1e0};
   const char *suffixes[] = {"ns", "us", "ms", "s"};

   for (unsigned index=0; index<(sizeof(suffixes)/sizeof(suffixes[0])); index++) {
      int value = round(d*multipliers[index]*100);
      if (value<1*100) {
         return "0.0ns";
      }
      if (value<999*100) {
         snprintf(buf, sizeof(buf), "%d.%2.2d%s", value/100, value%100, suffixes[index]);
         return buf;
      }
   }

   if (d<1e-6) {
      snprintf(buf, sizeof(buf), "%5.2fns", d*1e9);
   }
   else if (d<1e-3) {
      snprintf(buf, sizeof(buf), "%5.2fus", d*1e6);
   }
   else if (d<1) {
      snprintf(buf, sizeof(buf), "%5.2fms", d*1e3);
   }
   else {
      snprintf(buf, sizeof(buf), "%5.2fs", d);
   }
   return buf;
}

int main() {

   double delays[] = {0*ns, 10*ns, 50*ns, 100*ns, 150*ns, 200*ns, 300*ns,
         1*us, 1.5*us, 3*us, 10*us, 15*us, 55*us, 100*us, 128*us, 342*us,
         1*ms};

   for (unsigned index=0; index<(sizeof(delays)/sizeof(delays[0])); index++) {
      calculateDelay((float)delays[index], 48000000.0f, bestPrescale, bestDivider);
      float delay = calcDelay(48000000.0f, bestPrescale, bestDivider);

      printf("%10s, bestPrescale=%5d, bestPrescale=%5d ", format(delays[index]), bestPrescale, bestPrescale);
      printf("==> %10s\n", format(delay));
   }

   double frequencies[] = {22*Hz, 48*MHz, 33*MHz, 24*MHz, 22*MHz, 12*MHz, 10*MHz, 5*MHz, 2*MHz, 1.5*MHz,
      900*kHz, 700*kHz, 450*kHz, 300*kHz, 120*kHz, 93*kHz, 22*kHz, 1.2*kHz,
      900*Hz, 200*Hz, 22*Hz};

   for (unsigned index=0; index<(sizeof(frequencies)/sizeof(frequencies[0])); index++) {
      uint32_t result = calculateDividers(48000000, frequencies[index]);
      printf("%10d=>0x%08X => %f\n", (int)round(frequencies[index]), result, calcFreq(48000000, result));
   }

	return 0;
}
