//============================================================================
// Name        : TestClockTransitions.cpp
// Author      : 
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include <stdio.h>
#include <stdint.h>
#include <string>
using namespace std;

/**
 * Peripheral information for MCG, Multipurpose Clock Generator
 */
class McgInfo {
public:
   enum ClockMode {
      ClockMode_None,
      ClockMode_FEI,
      ClockMode_FEE,
      ClockMode_FBI,
      ClockMode_BLPI,
      ClockMode_FBE,
      ClockMode_BLPE,
      ClockMode_PBE,
      ClockMode_PEE,
   };

   //! Clock Mode
   static constexpr ClockMode clockMode = McgInfo::ClockMode_None;

};

static constexpr uint8_t clockTransitionTable[8][8] = {
   /*  from                 to =>   ClockMode_FEI,           ClockMode_FEE,           ClockMode_FBI,           ClockMode_BLPI,          ClockMode_FBE,           ClockMode_BLPE,          ClockMode_PBE,           ClockMode_PEE */
   /* ClockMode_FEI,  */ { McgInfo::ClockMode_FEI,  McgInfo::ClockMode_FEE,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE, },
   /* ClockMode_FEE,  */ { McgInfo::ClockMode_FEI,  McgInfo::ClockMode_FEE,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE, },
   /* ClockMode_FBI,  */ { McgInfo::ClockMode_FEI,  McgInfo::ClockMode_FEE,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_BLPI, McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE, },
   /* ClockMode_BLPI, */ { McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI, },
   /* ClockMode_FBE,  */ { McgInfo::ClockMode_FEI,  McgInfo::ClockMode_FEE,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBI,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_BLPE, McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE, },
   /* ClockMode_BLPE, */ { McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_BLPE, McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE, },
   /* ClockMode_PBE,  */ { McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_FBE,  McgInfo::ClockMode_BLPE, McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PEE, },
   /* ClockMode_PEE,  */ { McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PBE,  McgInfo::ClockMode_PEE, },
};

/** Current clock mode (FEI out of reset) */
McgInfo::ClockMode currentClockMode = McgInfo::ClockMode_FEI;
static int test = 0;

const char* getClockName(McgInfo::ClockMode mode) {
   static const char* names[] = {
      "None",
      "FEI",
      "FEE",
      "FBI",
      "BLPI",
      "FBE",
      "BLPE",
      "PBE",
      "PEE",
   };
   return names[mode];
}

/**
 * Transition from current clock mode to mode given
 *
 * @param to CLock mode to transition to
 */
int clockTransition(McgInfo::ClockMode to) {
   int transitionCount = 0;
   fprintf(stderr, "%3d: (%5s=>%5s): ", test++, getClockName(currentClockMode), getClockName(to));
   fprintf(stderr, "%5s", getClockName(currentClockMode));
   while (currentClockMode != to) {
      transitionCount++;
      if (transitionCount>6) {
         return -1;
      }
      McgInfo::ClockMode next = (McgInfo::ClockMode)clockTransitionTable[currentClockMode-1][to-1];
      switch (next) {
      case McgInfo::ClockMode_None:
      case McgInfo::ClockMode_FEI:
         break;
      case McgInfo::ClockMode_FEE:
         break;
      case McgInfo::ClockMode_FBI:
         break;
      case McgInfo::ClockMode_BLPI:
         break;
      case McgInfo::ClockMode_FBE:
         break;
      case McgInfo::ClockMode_BLPE:
         break;
      case McgInfo::ClockMode_PBE:
         break;
      case McgInfo::ClockMode_PEE:
         break;
      }
      currentClockMode = next;
      fprintf(stderr, ", %5s", getClockName(currentClockMode));
   }
   fprintf(stderr, "\n");
   return 0;
}


int main() {
   for (int from=1; from<=McgInfo::ClockMode_PEE; from++) {
      currentClockMode = (McgInfo::ClockMode)from;
      for (int to=1; to<=McgInfo::ClockMode_PEE; to++) {
         currentClockMode = (McgInfo::ClockMode)from;
         int rc = clockTransition((McgInfo::ClockMode)to);
         if (rc<0) {
            fprintf(stderr, " ==> Failed\n");
         }
      }
   }

   return 0;
}
