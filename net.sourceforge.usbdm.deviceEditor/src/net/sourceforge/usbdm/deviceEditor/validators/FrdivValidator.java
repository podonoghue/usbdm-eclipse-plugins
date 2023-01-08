package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine FLL divider settings
 */
public class FrdivValidator extends IndexedValidator {

   /** Used to indicate range is unconstrained by oscillator */
   public static final int    UNCONSTRAINED_RANGE = 3;
   
   // Range for FLL input - Use wide range as FLL will validate final value
   private static final long FLL_CLOCK_WIDE_MIN   = 31250L;
   private static final long FLL_CLOCK_WIDE_MAX   = 39063L;
   
   // Low range FLL divisors
   private static final int[] LOW_RANGE_DIVISORS  = {
         1, 2, 4, 8, 16, 32, 64, 128
   };

   // High range FLL divisors
   private static final int[] HIGH_RANGE_DIVISORS = {
         // 32     64        128       256       512        1024       1280       1536
         1*(1<<5), 2*(1<<5), 4*(1<<5), 8*(1<<5), 16*(1<<5), 32*(1<<5), 40*(1<<5), 48*(1<<5)
   };

   /** Calculated MCG_C1_FRDIV value */
   private int  mcg_c1_frdiv;

   public FrdivValidator(PeripheralWithState peripheral, Integer dimension) {
      super(peripheral, dimension);
   }

/**
    * Find suitable FLL divider (FRDIV)
    * 
    * @param fllInputClock       Input clock to be divided
    * @param mcg_c4_dmx32Var     Input range optimised for 32.768kHz
    * @param rangeDivisors       Possible dividers to select
    * 
    * @note Updates mcg_c1_frdiv with best divider found
    * 
    * @return True => Found suitable divider
    */
   private boolean findFllDivider(long fllInputClock, boolean mcg_c4_dmx32Var, int[] rangeDivisors) {

      double nearestError        = Double.MAX_VALUE;
      int    nearest_frdiv       = 0;

      boolean found = false;
      
      for (int mcg_c1_frdiv_calc=0; mcg_c1_frdiv_calc<rangeDivisors.length; mcg_c1_frdiv_calc++) {
         
         double fllInputFrequency_calc = ((double)fllInputClock)/rangeDivisors[mcg_c1_frdiv_calc];
         
         if (FLL_CLOCK_WIDE_MIN>fllInputFrequency_calc) {
            // Below range
            if ((FLL_CLOCK_WIDE_MIN-fllInputFrequency_calc) < nearestError) {
               // Keep updated value even if out of range
               nearestError          = FLL_CLOCK_WIDE_MIN-fllInputFrequency_calc;
               nearest_frdiv         = mcg_c1_frdiv_calc;
//                  System.err.println(String.format("+%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            }
         }
         else if (fllInputFrequency_calc>FLL_CLOCK_WIDE_MAX) {
            // Above range
            if ((fllInputFrequency_calc-FLL_CLOCK_WIDE_MAX) < nearestError) {
               // Keep updated value even if out of range
               nearestError          = fllInputFrequency_calc-FLL_CLOCK_WIDE_MAX;
               nearest_frdiv         = mcg_c1_frdiv_calc;
//                  System.err.println(String.format("-%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            }
         }
         else {
            // In range
            nearestError          = 0.0;
            nearest_frdiv         = mcg_c1_frdiv_calc;
//               System.err.println(String.format("=%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            found = true;
            break;
         }
      }
      mcg_c1_frdiv = nearest_frdiv;
      
      return found;
   }

   /**
    * Determines FRDIV and MCG_C3_RANGE0
    */
   @Override
   protected void validate(Variable variable, int index) throws Exception {

      final BooleanVariable mcg_c1_irefsVar    = getBooleanVariable("mcg_c1_irefs[]");
      final ChoiceVariable  mcg_c1_frdivVar    = getChoiceVariable("mcg_c1_frdiv[]");
      
      // Oscillator range determined from crystal
      final Variable oscillatorRangeVar = getVariable("/OSC0/oscillatorRange");
      int oscillatorRange = (int) oscillatorRangeVar.getValueAsLong();

      ChoiceVariable mcg_c2_rangeVar    = getChoiceVariable("mcg_c2_range0[]");
      
      // Defaults - controlled by OSC0
      Integer        mcg_c2_range       = (oscillatorRange==UNCONSTRAINED_RANGE)?0:oscillatorRange;
      String         mcg_c2_rangeOrigin = "Determined by OSC0";

      if (mcg_c1_irefsVar.getValueAsBoolean()) {
         // Pass through range from OSC as no sensible user value
         mcg_c2_rangeVar.setValue(mcg_c2_range);
         mcg_c2_rangeVar.setOrigin(mcg_c2_rangeOrigin);
         mcg_c2_rangeVar.setLocked(true);
         return;
      }
      
      final LongVariable   mcg_erc_clockVar       = getLongVariable("mcg_erc_clock[]");
      final long           mcg_erc_clock          = mcg_erc_clockVar.getValueAsLong();
      final Status         mcg_erc_clockStatus    = mcg_erc_clockVar.getStatus();
      
      if ((mcg_erc_clockStatus != null) && (mcg_erc_clockStatus.getSeverity().greaterThan(Severity.INFO))) {
         
         // ERC invalid so FRDIV is invalid as well
         mcg_c1_frdivVar.setStatus(mcg_erc_clockStatus);

         // Pass through range from OSC as no sensible user value
         mcg_c2_rangeVar.setValue(mcg_c2_range);
         mcg_c2_rangeVar.setOrigin(mcg_c2_rangeOrigin);
         mcg_c2_rangeVar.setLocked(true);
         return;
      }
      
      final BooleanVariable fll_enabledVar = getBooleanVariable("fll_enabled[]");
      boolean frdivInUse  = fll_enabledVar.getValueAsBoolean();
      
      mcg_c1_frdivVar.setLocked(frdivInUse);
      
      if (!frdivInUse) {
         
         // FRDIV not used by FLL - don't update value
         mcg_c1_frdivVar.clearStatus();
         mcg_c1_frdivVar.setOrigin("");
         
         if (oscillatorRange != UNCONSTRAINED_RANGE) {
            // Range is controlled by OSC
            mcg_c2_rangeVar.setValue(mcg_c2_range);
            mcg_c2_rangeVar.setOrigin(mcg_c2_rangeOrigin);
            mcg_c2_rangeVar.setLocked(true);
         }
         else {
            // Range can be set by user (to suite MCGFFCLK needs)
            mcg_c2_rangeVar.setOrigin(null);
            mcg_c2_rangeVar.setLocked(false);
         }
         return;
      }
      
      // Find input range & divisor
      //==============================
      int mcg_c7_oscsel = 0;
      
      final ChoiceVariable  mcg_c7_oscselVar  = safeGetChoiceVariable("mcg_c7_oscsel[]");
      if (mcg_c7_oscselVar != null) {
         mcg_c7_oscsel = (int) mcg_c7_oscselVar.getValueAsLong();
      }
      final BooleanVariable mcg_c4_dmx32Var = getBooleanVariable("mcg_c4_dmx32[]");
      boolean mcg_c4_dmx32 = mcg_c4_dmx32Var.getValueAsBoolean();
      
      boolean acceptableFrdivFound = false;
      
      if (mcg_c7_oscsel == 1) {
         // ERC = RTCCLK - Forced to LOW_RANGE_DIVISORS, mcg_c2_range has no effect on FRDIV
         acceptableFrdivFound = findFllDivider(mcg_erc_clock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);

         if (oscillatorRange == UNCONSTRAINED_RANGE) {
            // Range has no effect on FRDIV or OSC
            mcg_c2_rangeOrigin  = "Unused";
         }
      }
      else {
         // Use osc0_range unless unconstrained
         switch (oscillatorRange) {
         case 0:
            acceptableFrdivFound = findFllDivider(mcg_erc_clock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            // Use value to suit oscillator
            mcg_c2_range        = oscillatorRange;
            mcg_c2_rangeOrigin  = "Determined by OSC0";
            break;
         case 1:
         case 2:
            acceptableFrdivFound = findFllDivider(mcg_erc_clock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            // Use value to suit oscillator
            mcg_c2_range        = oscillatorRange;
            mcg_c2_rangeOrigin  = "Determined by OSC0";
            break;
         case UNCONSTRAINED_RANGE:
         default:
            // Unconstrained - try both sets of dividers
            // Use whichever mcg_c2_range works for FLL
            mcg_c2_range = 0;
            acceptableFrdivFound = findFllDivider(mcg_erc_clock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            if (!acceptableFrdivFound) {
               mcg_c2_range = 1;
               acceptableFrdivFound = findFllDivider(mcg_erc_clock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            }
            mcg_c2_rangeOrigin  = "Determined by FLL";
            break;
         }
      }
      mcg_c1_frdivVar.setOrigin("Determined by FLL input constraints");
      if (acceptableFrdivFound) {
         mcg_c1_frdivVar.setValue(mcg_c1_frdiv);
         mcg_c1_frdivVar.clearStatus();
      }
      else {
         mcg_c1_frdivVar.setStatus("Unable to find suitable value");
      }
      // Record range in use
      mcg_c2_rangeVar.setLocked(true);
      mcg_c2_rangeVar.setValue(mcg_c2_range);
      mcg_c2_rangeVar.setOrigin(mcg_c2_rangeOrigin);
   }

   @Override
   protected boolean createDependencies() throws Exception {
      super.createDependencies();
      
      // Variables to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      variablesToWatch.add("/OSC0/oscillatorRange");
      
      variablesToWatch.add("fll_enabled[]");
      variablesToWatch.add("mcg_c1_irefs[]");
      variablesToWatch.add("mcg_erc_clock[]");
      variablesToWatch.add("mcg_c7_oscsel[]");
      variablesToWatch.add("mcg_c4_dmx32[]");
      variablesToWatch.add("mcgClockMode[]");

      variablesToWatch.add("mcg_c1_frdiv[]");

      addSpecificWatchedVariables(variablesToWatch);
      
      return false;
   }
}