package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;

/**
 * Class to determine FLL divider settings
 */
class FllConfigure {

   // Range for FLL input - Wide
   private static final long FLL_CLOCK_WIDE_MIN   = 31250L;
   private static final long FLL_CLOCK_WIDE_MAX   = 39063L;
   
   // Range for FLL input - Narrow
   private static final long FLL_CLOCK_NARROW_MIN = 32768L-100;
   private static final long FLL_CLOCK_NARROW_MAX = 32768L+100;

   // Low range FLL divisors
   private static final int[] LOW_RANGE_DIVISORS  = {
         1, 2, 4, 8, 16, 32, 64, 128
   };

   // High range FLL divisors
   private static final int[] HIGH_RANGE_DIVISORS = {
         // 32     64        128       256       512        1024       1280       1536
         1*(1<<5), 2*(1<<5), 4*(1<<5), 8*(1<<5), 16*(1<<5), 32*(1<<5), 40*(1<<5), 48*(1<<5)
   };

   // FLL multiplication factor for narrow range
   private static final long FLL_NARROW_FACTOR = 732;

   // FLL multiplication factor for wide range
   private static final long FLL_WIDE_FACTOR   = 640;

   private double nearestError     = Double.MAX_VALUE;
   private double nearestFrequency = 0.0;
   private int    nearest_frdiv    = 0;
   private int    mcg_c1_frdiv_calc;
   private double fllInputFrequency_calc;

   /** Calculated MCG_C1_FRDIV value */
   public final int  mcg_c1_frdiv;

   /** MCG_C4_DRST_DRS value corresponding to fllTargetFrequency */
   public final int mcg_c4_drst_drs;
   
   /** Returns the status of the FLL i.e. whether input and output values are valid */
   private Status fllStatus;
   
   /**
    * Find suitable FLL divider (frdiv)
    * 
    * @param fllInputClock       Input clock to be divided
    * @param mcg_c4_dmx32Var     Clock mode (affects input range)
    * @param rangeDivisors       Possible dividers to select
    * 
    * @return True => Found suitable divider
    */
   boolean findDivider(long fllInputClock, boolean mcg_c4_dmx32Var, int[] rangeDivisors) { 

      long fllInMin;
      long fllInMax;
      
      if (mcg_c4_dmx32Var) {
         fllInMin = FLL_CLOCK_NARROW_MIN;
         fllInMax = FLL_CLOCK_NARROW_MAX;
      }
      else {
         fllInMin = FLL_CLOCK_WIDE_MIN;
         fllInMax = FLL_CLOCK_WIDE_MAX;
      }

      boolean found = false;
      for (mcg_c1_frdiv_calc=0; mcg_c1_frdiv_calc<rangeDivisors.length; mcg_c1_frdiv_calc++) {
         
         fllInputFrequency_calc = ((double)fllInputClock)/rangeDivisors[mcg_c1_frdiv_calc];
         
         if (fllInMin>fllInputFrequency_calc) {
            if ((fllInMin-fllInputFrequency_calc) < nearestError) {
               nearestFrequency = fllInputFrequency_calc;
               nearestError     = fllInMin-fllInputFrequency_calc;
               nearest_frdiv    = mcg_c1_frdiv_calc;
               //                  System.err.println(String.format("+%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            }
         }
         else if (fllInputFrequency_calc>fllInMax) {
            if ((fllInputFrequency_calc-fllInMax) < nearestError) {
               nearestFrequency = fllInputFrequency_calc;
               nearestError     = fllInputFrequency_calc-fllInMax;
               nearest_frdiv    = mcg_c1_frdiv_calc;
               //                  System.err.println(String.format("-%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            }
         }
         else {
            nearestFrequency = fllInputFrequency_calc;
            nearestError     = 0.0;
            nearest_frdiv    = mcg_c1_frdiv_calc;
            //               System.err.println(String.format("=%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            found = true;
            break;
         }
      }
      return found;
   }

   /**
    * Determines the FLL divider values
    * 
    * @param osc_osc_cr_erclkenVar      [in]     Indicates if Oscillator is in use
    * @param osc_oscillatorRangeVar     [in]     Range in from oscillator
    * @param mcg_c2_rangeVar            [out]    Range out after modification by FLL
    * @param mcg_c1_irefs               [in]     irefs value (affects clock source)
    * @param mcg_erc_clockVar           [in]     mcg_erc_clock source
    * @param system_slow_irc_clock      [in]     Frequency of slow IRC
    * @param mcg_c7_oscsel              [in]     OSCSEL value used to constrain dividers
    * @param mcg_c4_dmx32Var            [in]     Affects input range accepted 
    * @param fll_enabledVar             [in]     Indicates if FLL is in use
    * @param fllInputFrequencyVar       [in/out] Input to FLL
    * @param fllOutputFrequencyVar      [out]    Output from FLL
    * @param system_mcgffclk_clockVar   [out]    MCGFFCLK 
    * @param drst_drs_max               [in]     Maximum value for mcg_c4_drst_drs
    */
   public FllConfigure(
         final Variable osc_osc_cr_erclkenVar, 
         final Variable osc_oscillatorRangeVar, 
         final Variable mcg_c2_rangeVar, 
         boolean        mcg_c1_irefs, 
         final Variable mcg_erc_clockVar, 
         long           system_slow_irc_clock, 
         long           mcg_c7_oscsel, 
         boolean        mcg_c4_dmx32, 
         final Variable fll_enabledVar,
         final Variable fllInputFrequencyVar, 
         final Variable fllOutputFrequencyVar, 
         final Variable system_mcgffclk_clockVar, 
         long           drst_drs_max) {

      // Tentative range - may be overridden by FLL constraints
      int     osc0_range       = (osc_oscillatorRangeVar== null)?0:(int)osc_oscillatorRangeVar.getValueAsLong();
      String  osc0_rangeOrigin = (osc_oscillatorRangeVar== null)?"":osc_oscillatorRangeVar.getOrigin();
      
      String  fllOrigin;
      String  fllInputOrigin;

      long availableClock;
      if (mcg_c1_irefs) {
         // Slow IRC selected [32kHz]
         fllOrigin      = "Slow internal reference clock";
         fllInputOrigin = fllOrigin;
         fllStatus      = null;
         availableClock = system_slow_irc_clock;
      }
      else {
         // ERCLK selected [OSCCLK, RTCCLK, IRC48MCLK]
         fllOrigin      = mcg_erc_clockVar.getOrigin();
         fllInputOrigin = fllOrigin+" / FRDIV";
         fllStatus      = mcg_erc_clockVar.getStatus();
         availableClock = mcg_erc_clockVar.getValueAsLong();
      }
      fllInputFrequencyVar.setOrigin(fllInputOrigin);
      system_mcgffclk_clockVar.setOrigin(fllInputOrigin);
      
      fllOrigin = fllOrigin+" via FLL";

      if ((fllStatus != null) && (fllStatus.getSeverity().greaterThan(Severity.OK))) {
         // Invalid input
         fllStatus = new Status(fllStatus.getSimpleText()+": Invalid FLL input", Severity.WARNING);
         fllInputFrequencyVar.setStatus(fllStatus);
         fllInputFrequencyVar.setValue(0);
         system_mcgffclk_clockVar.setStatus(fllStatus);
         system_mcgffclk_clockVar.setValue(0);
         // Use mcg_c2_rangeIn unless invalid
         mcg_c4_drst_drs = 0;
         mcg_c1_frdiv    = 0;
         mcg_c2_rangeVar.setValue(osc0_range);
         mcg_c2_rangeVar.setOrigin(osc0_rangeOrigin);
         fllOutputFrequencyVar.setOrigin(fllOrigin+"(invalid FLL input)");
         return;
      }

      // Find input range & divisor
      //==============================
      boolean found = false;
      boolean osc_osc_cr_erclken = (osc_osc_cr_erclkenVar == null) || osc_osc_cr_erclkenVar.getValueAsBoolean();
      if (mcg_c1_irefs) {
         // [Slow IRC] - No dividers, Range value unconstrained by FLL
         found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
         // Use mcg_c2_rangeIn unless invalid
      }
      else if (mcg_c7_oscsel == 1) {
         // [RTCCLK] - Forced to LOW_RANGE_DIVISORS,  Range value unconstrained FLL
         found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
         fllOrigin += " after scaling by (Low range FRDIV)";
      }
      else if ((mcg_c7_oscsel != 0) && !osc_osc_cr_erclken) {
         // ![OSCCLK] and not enabled for peripherals 
         // Unconstrained - try both sets of dividers
         // Use whichever mcg_c2_rangeIn works
         osc0_range = 0;
         found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
         if (!found) {
            osc0_range = 1;
            found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
         }
         fllOrigin        += " after scaling (Low/High range FRDIV)";
         osc0_rangeOrigin  = "Determined by FLL";
      }
      else {
         switch (osc0_range) {
         // Use osc0_range unless invalid
         case 0:
            found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            fllOrigin += " after scaling (Low range FRDIV)";
            break;
         case 1:
         case 2:
            found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            fllOrigin += " after scaling (High range FRDIV)";
            break;
         default: 
            // Unconstrained - try both sets of dividers
            // Use whichever  mcg_c2_rangeIn works
            osc0_range = 0;
            found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            if (!found) {
               osc0_range = 1;
               found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            }
            fllOrigin        += " after scaling (Low/High range FRDIV)";
            osc0_rangeOrigin  = "Determined by FLL";
            break;
         }
      }
      // Record range in use
      mcg_c2_rangeVar.setValue(osc0_range);
      mcg_c2_rangeVar.setOrigin(osc0_rangeOrigin);
      
      if (!found) {
         // No suitable divisor - Set invalid and use defaults
         fllInputFrequencyVar.setValue(Math.round(nearestFrequency));
         String msgText = String.format("Unable to find suitable FLL divisor for input frequency of %s", 
               EngineeringNotation.convert(availableClock, 3));
         fllStatus = new Status(msgText, Severity.WARNING);
         fllInputFrequencyVar.setStatus(fllStatus);
         mcg_c1_frdiv    = nearest_frdiv;
         mcg_c4_drst_drs = 0;
         system_mcgffclk_clockVar.setValue(Math.round(nearestFrequency));
         system_mcgffclk_clockVar.setStatus((Status)null);
         fllOutputFrequencyVar.setStatus(fllStatus);
         fllOutputFrequencyVar.setOrigin(fllOrigin+"(invalid input frequency)");
         return;
      }

      long inputFrequency  = Math.round(fllInputFrequency_calc);
      mcg_c1_frdiv         = mcg_c1_frdiv_calc;

      // Record FLL input details
      system_mcgffclk_clockVar.setValue(inputFrequency);
      system_mcgffclk_clockVar.setStatus((Status)null);

      fllInputFrequencyVar.setValue(inputFrequency);
      fllInputFrequencyVar.setStatus((Status)null);

      // Determine possible output frequencies & check against desired value
      //=======================================================================
      int mcg_c4_drst_drs_calc = -1;

      long fllOutFrequency = inputFrequency * (mcg_c4_dmx32?FLL_NARROW_FACTOR:FLL_WIDE_FACTOR);

      Long fllTargetFrequency = fllOutputFrequencyVar.getRawValueAsLong();

      ArrayList<Long> fllFrequencies = new ArrayList<Long>(); 
      for (int probe=0; probe<=drst_drs_max; probe++) {
         fllFrequencies.add(fllOutFrequency*(probe+1));
         // Accept value within ~10% of desired
         if (Math.abs((fllOutFrequency*(probe+1)) - fllTargetFrequency) < (fllTargetFrequency/50)) {
            mcg_c4_drst_drs_calc = probe;
         }         
      }
      StringBuilder sb       = new StringBuilder();
      Severity      severity = Severity.INFO;
      if (mcg_c4_drst_drs_calc >= 0) {
         // Adjust rounded value
         fllTargetFrequency = fllOutFrequency*(mcg_c4_drst_drs_calc+1);
      }
      else {
         mcg_c4_drst_drs_calc = 0;
         sb.append("Not possible to generate desired FLL frequency from input clock\n");
         fllOrigin = fllOrigin+"(invalid output frequency)";
         severity = Severity.WARNING;
      }
      boolean needComma = false;
      for (Long freq : fllFrequencies) {
         if (needComma) {
            sb.append(", ");
         }
         else {
            sb.append("Possible values = ");
         }
         needComma = true;
         sb.append(EngineeringNotation.convert(freq, 5)+"Hz");
      }
      fllStatus = new Status (sb.toString(), severity);
      fllOutputFrequencyVar.setValue(fllTargetFrequency);
      fllOutputFrequencyVar.setStatus(fllStatus);
      fllOutputFrequencyVar.setOrigin(fllOrigin);
      mcg_c4_drst_drs = mcg_c4_drst_drs_calc;
   }

   /**
    * Returns the status of the FLL i.e. range of values, whether input and output values are valid
    * 
    * @return Message 
    */
   public Status getFllStatus() {
      return fllStatus;
   }
}