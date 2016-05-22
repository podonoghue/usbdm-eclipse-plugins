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

   public enum ClockMode {ClockMode_None, ClockMode_FEI, ClockMode_FEE, ClockMode_FBI,
      ClockMode_FBE, ClockMode_PBE,  ClockMode_PEE, ClockMode_BLPI, ClockMode_BLPE}

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
//                  System.err.println(String.format("+%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
               }
            }
            else if (_fllInputFrequency>FLL_CLOCK_WIDE_MAX) {
               if ((_fllInputFrequency-FLL_CLOCK_WIDE_MAX) < nearestError) {
                  nearestFrequency = _fllInputFrequency;
                  nearestError     = _fllInputFrequency-FLL_CLOCK_WIDE_MAX;
                  nearest_frdiv    = _mcg_c1_frdiv;
//                  System.err.println(String.format("-%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
               }
            }
            else {
               nearestFrequency = _fllInputFrequency;
               nearestError     = 0.0;
               nearest_frdiv    = _mcg_c1_frdiv;
//               System.err.println(String.format("=%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
               found = true;
               break;
            }
         }
         return found;
      }

      public FllDivider(long system_erc_clock, boolean mcg_c2_erefs0, long mcg_c7_oscsel, long oscclk_clock) {

         //=========================================
         // Check input clock/oscillator ranges
         //   - Determine mcg_c2_range
         //   - Affects FLL prescale
         //
         int range = 0;
         Message clockMessage = null;
         boolean found = false;
         int[] divisors = null;
         
         if (mcg_c2_erefs0) {
            // Using oscillator - range is determine by Crystal frequency
            if ((oscclk_clock >= EXTERNAL_EXTAL_RANGE1_MIN) && (oscclk_clock <= EXTERNAL_EXTAL_RANGE1_MAX)) {
               range = 0;
               divisors = LOW_RANGE_DIVISORS;
            }
            else if ((oscclk_clock >= EXTERNAL_EXTAL_RANGE2_MIN) && (oscclk_clock <= EXTERNAL_EXTAL_RANGE2_MAX)) {
               range = 1;
               divisors = HIGH_RANGE_DIVISORS;
            }
            else if ((oscclk_clock >= EXTERNAL_EXTAL_RANGE3_MIN) && (oscclk_clock <= EXTERNAL_EXTAL_RANGE3_MAX)) {
               range = 2;
               divisors = HIGH_RANGE_DIVISORS;
            }
            else {
               // Not suitable for OSC
               clockMessage = FLL_CLOCK_ERROR_MSG;
               
               // Set a more useful divisor even though none are suitable
               findDivider(system_erc_clock/(1<<5), LOW_RANGE_DIVISORS);
            }
         }
         else {
            // Using external clock
            if (oscclk_clock>EXTERNAL_CLOCK_MAX) {
               clockMessage = CLOCK_RANGE_ERROR_MSG;
            }
         }
         if (mcg_c7_oscsel == 1) {
            // Forced to LOW_RANGE_DIVISORS irrespective of range
            divisors = LOW_RANGE_DIVISORS;
         }
         if (clockMessage == null) {
            // Clock OK - try to find divisor
            if (divisors == LOW_RANGE_DIVISORS) {
               found = findDivider(system_erc_clock, LOW_RANGE_DIVISORS);
            }
            else if (divisors == HIGH_RANGE_DIVISORS) {
               found = findDivider(system_erc_clock/(1<<5), HIGH_RANGE_DIVISORS);
            }
            else {
               // No divider set - try all possible ranges divisors
               found = findDivider(system_erc_clock, LOW_RANGE_DIVISORS);
               if (found) {
                  range = 0;
               }
               else {
                  found = findDivider(system_erc_clock/(1<<5), HIGH_RANGE_DIVISORS);
                  if (found) {
                     range = 1;
                  }
               }
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

   private String getSimpleClassName() {
      String s = getClass().toString();
      int index = s.lastIndexOf(".");
      return s.substring(index+1, s.length());
   }
   
   /**
    * =============================================================
    */
   private        boolean  busy           = false;
   private        boolean  recursed       = false;
   private  final int      MAX_ITERATION  = 100;

   @Override
   public boolean variableChanged(Variable variable) {
//      System.err.println(getSimpleClassName()+".variableChanged("+variable+")");
      if (busy) {
         recursed = true;
//         System.err.println(getSimpleClassName()+".variableChanged("+variable+"):Recursed");
//         new Throwable().printStackTrace(System.err);
         return true;
      }
      busy = true;
      int iterationCount = 0;
      do {
         recursed = false;
         validate();
//         System.err.println(getSimpleClassName()+".variableChanged("+variable+") Iterating " + iterationCount);
         if (iterationCount++>MAX_ITERATION) {
            System.err.println(getSimpleClassName()+".variableChanged("+variable+") Iteration limit reached");
            break;
         }
      } while (recursed);
      busy = false;
      return false;
   }
   
   /**
    * Check if a value is within a range
    * 
    * @param value   Value to check
    * @param min     Smallest allowed value
    * @param max     Largest allowed value
    * 
    * @return        true is value in [min,max]
    */
   boolean checkRange(long value, long min, long max) {
      return (value>=min) && (value<=max);
   }

}
