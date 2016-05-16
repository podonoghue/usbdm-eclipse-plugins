package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public abstract class BaseClockValidator extends Validator {

   // Ranges for External crystal
   protected static final long EXTERNAL_EXTAL_RANGE1_MIN = 32000L;
   protected static final long EXTERNAL_EXTAL_RANGE1_MAX = 40000L;
   
   protected static final long EXTERNAL_EXTAL_RANGE2_MIN = 3000000L;
   protected static final long EXTERNAL_EXTAL_RANGE2_MAX = 8000000L;
   
   protected static final long EXTERNAL_EXTAL_RANGE3_MIN = 8000000L;
   protected static final long EXTERNAL_EXTAL_RANGE3_MAX = 32000000L;

   // Maximum External Clock (not crystal)
   protected static final long EXTERNAL_CLOCK_MAX        = 50000000L;
   
   protected static final long FLL_CLOCK_WIDE_MIN = 31250L;
   protected static final long FLL_CLOCK_WIDE_MAX = 39063L;

   public enum ClockMode {ClockMode_None, ClockMode_FEI, ClockMode_FEE, ClockMode_FBI, ClockMode_FBE, ClockMode_PBE,  ClockMode_PEE, ClockMode_BLPI, ClockMode_BLPE}

   protected BaseClockValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

   static class FllDivider {

      /** External crystal frequency error message */
      protected static final Message FLL_CLOCK_ERROR_MSG = new Message(String.format(
         "External crystal frequency not suitable for oscillator\n"+
         "Ranges [%2.2f,%2.2f] kHz, [%2.2f,%2.2f] MHz, [%2.2f,%2.2f] MHz",           
         EXTERNAL_EXTAL_RANGE1_MIN/1000.0,    EXTERNAL_EXTAL_RANGE1_MAX/1000.0,
         EXTERNAL_EXTAL_RANGE2_MIN/1000000.0, EXTERNAL_EXTAL_RANGE2_MAX/1000000.0,
         EXTERNAL_EXTAL_RANGE3_MIN/1000000.0, EXTERNAL_EXTAL_RANGE3_MAX/1000000.0),
         Severity.ERROR);

      /** External clock frequency error message */
      protected static final Message CLOCK_RANGE_ERROR_MSG = new Message(String.format(
         "External clock frequency is too high\nMax=%2.2f MHz",
         EXTERNAL_CLOCK_MAX/1000000.0), 
         Severity.ERROR);

      // Choose range and divisor based on suitable FLL input frequency
      private static final int[] LOW_RANGE_DIVISORS  = {1, 2, 4, 8, 16, 32, 64, 128};
      private static final int[] HIGH_RANGE_DIVISORS = {1, 2, 4, 8, 16, 32, 40, 48};

      public final int     mcg_c1_frdiv;
      public final Message mcg_c1_frdiv_clockMessage;

      public final int     mcg_c2_range;
      public final Message oscclk_clockMessage;
      public final double  fllInputFrequency;

      private int     _mcg_c1_frdiv;
      private double  _fllInputFrequency;

      private double nearestError     = Double.MAX_VALUE;
      private double nearestFrequency = 0.0;
      private int    nearest_frdiv    = 0;

      /**
       * Find suitable FLL divider (frdiv)
       * @param fllInputClock
       * @param rangeDivisors
       * @return
       */
      boolean findDivider(long fllInputClock, int[] rangeDivisors) { 
         boolean found = false;
         for (_mcg_c1_frdiv=0; _mcg_c1_frdiv<rangeDivisors.length; _mcg_c1_frdiv++) {
            _fllInputFrequency = fllInputClock/rangeDivisors[_mcg_c1_frdiv];
            if (FLL_CLOCK_WIDE_MIN>_fllInputFrequency) {
               if ((FLL_CLOCK_WIDE_MIN-_fllInputFrequency) < nearestError) {
                  nearestFrequency = _fllInputFrequency;
                  nearestError     = FLL_CLOCK_WIDE_MIN-_fllInputFrequency;
                  nearest_frdiv    = _mcg_c1_frdiv;
                  System.err.println(String.format("+%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
               }
            }
            else if (_fllInputFrequency>FLL_CLOCK_WIDE_MAX) {
               if ((_fllInputFrequency-FLL_CLOCK_WIDE_MAX) < nearestError) {
                  nearestFrequency = _fllInputFrequency;
                  nearestError     = _fllInputFrequency-FLL_CLOCK_WIDE_MAX;
                  nearest_frdiv    = _mcg_c1_frdiv;
                  System.err.println(String.format("-%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
               }
            }
            else {
               nearestFrequency = _fllInputFrequency;
               nearestError     = 0.0;
               nearest_frdiv    = _mcg_c1_frdiv;
               System.err.println(String.format("=%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
               found = true;
               break;
            }
         }
         return found;
      }

      public FllDivider(long system_erc_clock, long mcg_c2_erefs0, long oscclk_clock) {

         //=========================================
         // Check input clock/oscillator ranges
         //   - Determine mcg_c2_range
         //   - Affects FLL prescale
         //
         int range;
         Message clockMessage = null;
         boolean found = false;
         if (mcg_c2_erefs0 != 0) {
            // Using oscillator - range is determine by Crystal frequency
            if ((system_erc_clock >= EXTERNAL_EXTAL_RANGE1_MIN) && (system_erc_clock <= EXTERNAL_EXTAL_RANGE1_MAX)) {
               found = findDivider(system_erc_clock, LOW_RANGE_DIVISORS);
               range = 0;
            }
            else if ((system_erc_clock >= EXTERNAL_EXTAL_RANGE2_MIN) && (system_erc_clock <= EXTERNAL_EXTAL_RANGE2_MAX)) {
               found = findDivider(system_erc_clock/(1<<5), HIGH_RANGE_DIVISORS);
               range = 1;
            }
            else if ((system_erc_clock >= EXTERNAL_EXTAL_RANGE3_MIN) && (system_erc_clock <= EXTERNAL_EXTAL_RANGE3_MAX)) {
               found = findDivider(system_erc_clock/(1<<5), HIGH_RANGE_DIVISORS);
               range = 2;
            }
            else {
               clockMessage = FLL_CLOCK_ERROR_MSG;
               findDivider(system_erc_clock/(1<<5), LOW_RANGE_DIVISORS);
               range = 0;
            }
         }
         else {
            // Using external clock - try all possibilities
            found = findDivider(system_erc_clock, LOW_RANGE_DIVISORS);
            range = 0;
            if (!found) {
               range = 1;
               found = findDivider(system_erc_clock/(1<<5), HIGH_RANGE_DIVISORS);
            }
            if ((mcg_c2_erefs0 == 0) && (oscclk_clock>EXTERNAL_CLOCK_MAX)) {
               clockMessage = CLOCK_RANGE_ERROR_MSG;
            }
         }
         if (found) {
            mcg_c1_frdiv_clockMessage  = null;
            fllInputFrequency          = _fllInputFrequency;
            mcg_c1_frdiv               = _mcg_c1_frdiv;
         }
         else {
            String msgText = String.format("Unable to find suitable FLL divisor for input frequency of %s\n", system_erc_clock);
            mcg_c1_frdiv_clockMessage  = new Message(msgText, Severity.WARNING);
            fllInputFrequency          = nearestFrequency;
            mcg_c1_frdiv               = nearest_frdiv;
         }
         mcg_c2_range        = range;
         oscclk_clockMessage = clockMessage;
      }
   }

   /**
    * =============================================================
    */
   private static       boolean  busy           = false;
   private static       boolean  recursed       = false;
   private static final int      MAX_ITERATION  = 100;

   @Override
   public void variableChanged(Variable variable) {
      
      if (busy) {
         recursed = true;
         return;
      }
      busy = true;
      int iterationCount = 0;
      do {
         recursed = false;
         validate();
         if (iterationCount++>MAX_ITERATION) {
            System.err.println("BaseClockValidator() Iteration limit reached");
            break;
         }
      } while (recursed);
      busy = false;
   }

}
