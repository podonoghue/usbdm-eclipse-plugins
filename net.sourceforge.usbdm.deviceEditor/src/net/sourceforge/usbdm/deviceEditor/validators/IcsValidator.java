package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine FLL divider settings
 */
public class IcsValidator extends IndexedValidator {

   // Range for FLL input - Use wide range as FLL will validate final value
   private static final long FLL_CLOCK_MIN   = 31250L;
   private static final long FLL_CLOCK_MAX   = 39063L;
   
   // Low range FLL divisors
   private static final int[] LOW_RANGE_DIVISORS  = {
         1, 2, 4, 8, 16, 32, 64, 128
   };

   // High range FLL divisors
   private static final int[] HIGH_RANGE_DIVISORS = {
         // 32     64        128       256       512        1024
         1*(1<<5), 2*(1<<5), 4*(1<<5), 8*(1<<5), 16*(1<<5), 32*(1<<5)
   };

   /** Calculated MCG_C1_RDIV value */
   private int ics_c1_rdiv;

   public IcsValidator(PeripheralWithState peripheral, Integer dimension) {
      super(peripheral, dimension);
   }

/**
    * Find suitable FLL divider (RDIV)
    * 
    * @param fllInputClock       Input clock to be divided
    * @param ics_c4_dmx32Var     Input range optimised for 32.768kHz
    * @param rangeDivisors       Possible dividers to select
    * 
    * @note Updates ics_c1_rdiv with best divider found
    * 
    * @return True => Found suitable divider
    */
   private boolean findFllDivider(long fllInputClock, int[] rangeDivisors) {

      double nearestError        = Double.MAX_VALUE;
      int    nearest_rdiv       = 0;

      boolean found = false;
      
      for (int ics_c1_rdiv_calc=0; ics_c1_rdiv_calc<rangeDivisors.length; ics_c1_rdiv_calc++) {
         
         double fllInputFrequency_calc = ((double)fllInputClock)/rangeDivisors[ics_c1_rdiv_calc];
         
         if (FLL_CLOCK_MIN>fllInputFrequency_calc) {
            // Below range
            if ((FLL_CLOCK_MIN-fllInputFrequency_calc) < nearestError) {
               // Keep updated value even if out of range
               nearestError         = FLL_CLOCK_MIN-fllInputFrequency_calc;
               nearest_rdiv         = ics_c1_rdiv_calc;
//                  System.err.println(String.format("+%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            }
         }
         else if (fllInputFrequency_calc>FLL_CLOCK_MAX) {
            // Above range
            if ((fllInputFrequency_calc-FLL_CLOCK_MAX) < nearestError) {
               // Keep updated value even if out of range
               nearestError         = fllInputFrequency_calc-FLL_CLOCK_MAX;
               nearest_rdiv         = ics_c1_rdiv_calc;
//                  System.err.println(String.format("-%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            }
         }
         else {
            // In range
            nearestError         = 0.0;
            nearest_rdiv         = ics_c1_rdiv_calc;
//               System.err.println(String.format("=%5.2f %5.2f %2d", nearestFrequency, nearestError, nearest_frdiv));
            found = true;
            break;
         }
      }
      ics_c1_rdiv = nearest_rdiv;
      return found;
   }

   /**
    * Determines RDIV and MCG_C3_RANGE0
    */
   @Override
   protected void validate(Variable variable, int properties, int index) throws Exception {

      final ChoiceVariable  ics_c1_rdivVar = getChoiceVariable("ics_c1_rdiv[]");
      
      // Oscillator range determined from crystal
      final Variable ocs_cr_rangeVar = getVariable("/OSC0/osc_cr_range");
      Integer osc_cr_range = (int) ocs_cr_rangeVar.getValueAsLong();

      final LongVariable   ics_erc_clockVar       = getLongVariable("ics_erc_clock[]");
      final long           ics_erc_clock          = ics_erc_clockVar.getValueAsLong();
      final Status         ics_erc_clockStatus    = ics_erc_clockVar.getStatus();
      
      if ((ics_erc_clockStatus != null) && (ics_erc_clockStatus.getSeverity().greaterThan(Severity.INFO))) {
         // ERC invalid so RDIV is invalid as well
         ics_c1_rdivVar.setValue(0);
         ics_c1_rdivVar.setStatus(ics_erc_clockStatus);
         return;
      }
      
//      final BooleanVariable fll_enabledVar = getBooleanVariable("fll_enabled[]");
//      boolean frdivInUse  = fll_enabledVar.getValueAsBoolean();
      
//      ics_c1_rdivVar.setLocked(frdivInUse);
//
//      if (!frdivInUse) {
//
//         // RDIV not used by FLL - don't update value
//         ics_c1_rdivVar.clearStatus();
//         ics_c1_rdivVar.setOrigin("");
//         return;
//      }
      
      // Find input range & divisor
      //==============================
      boolean acceptableFrdivFound = false;
      
      // Use osc0_range unless unconstrained
      switch (osc_cr_range) {
      default:
      case 0:
         acceptableFrdivFound = findFllDivider(ics_erc_clock, LOW_RANGE_DIVISORS);
         break;
      case 1:
         acceptableFrdivFound = findFllDivider(ics_erc_clock, HIGH_RANGE_DIVISORS);
         break;
      }
      if (acceptableFrdivFound) {
         ics_c1_rdivVar.setValue(ics_c1_rdiv+1);
         ics_c1_rdivVar.clearStatus();
      }
      else {
         ics_c1_rdivVar.setValue(0);
         ics_c1_rdivVar.setStatus("Unable to find suitable divider");
      }
   }

   @Override
   protected boolean createDependencies() throws Exception {
      
      // Variables to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

//      variablesToWatch.add("/OSC0/oscillatorRange");
      variablesToWatch.add("/OSC0/osc_cr_range");
      
//      variablesToWatch.add("fll_enabled[]");
      variablesToWatch.add("icsClockMode[]");
//      variablesToWatch.add("ics_c1_irefs[]");
      variablesToWatch.add("ics_erc_clock[]");
//      variablesToWatch.add("ics_c1_rdiv[]");

      addSpecificWatchedVariables(variablesToWatch);
      
      // Don't add default dependencies
      return false;
   }
}