package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine MCG settings

 * Used for:
 *     mcg_mk_ics48mml
 *     mcg_mk
 */
public class PllValidator extends IndexedValidator {

   private final long  PLL_IN_MIN;
   private final long  PLL_IN_MAX;

   private final long  PLL_OUT_MIN;
   private final long  PLL_OUT_MAX;

   private final long  PRDIV_MIN;
   private final long  PRDIV_MAX;

   private final long  VDIV_MIN;
   private final long  VDIV_MAX;

   private final long  PLL_POST_DIV;

   public PllValidator(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      PLL_IN_MIN   = (Long)it.next();
      PLL_IN_MAX   = (Long)it.next();
      PLL_OUT_MIN  = (Long)it.next();
      PLL_OUT_MAX  = (Long)it.next();
      PRDIV_MIN    = (Long)it.next();
      PRDIV_MAX    = (Long)it.next();
      VDIV_MIN     = (Long)it.next();
      VDIV_MAX     = (Long)it.next();
      PLL_POST_DIV = (Long)it.next();

      addVariable(new LongVariable(null, "/MCG/pll_vdiv_min",     Long.toString(VDIV_MIN)));
      addVariable(new LongVariable(null, "/MCG/pll_post_divider", Long.toString(PLL_POST_DIV)));

      try {
         // Update PRDIV0 and VDIV0 limits
         for (int clockIndex=0; clockIndex<dimension; clockIndex++) {
            setClockIndex(clockIndex);
            LongVariable mcg_c5_prdiv0Var = getLongVariable("mcg_c5_prdiv0");
            mcg_c5_prdiv0Var.setOffset(-PRDIV_MIN);
            mcg_c5_prdiv0Var.setMin(PRDIV_MIN);
            mcg_c5_prdiv0Var.setMax(PRDIV_MAX);

            LongVariable mcg_c6_vdiv0Var = getLongVariable("mcg_c6_vdiv0");
            mcg_c6_vdiv0Var.setOffset(-VDIV_MIN);
            mcg_c6_vdiv0Var.setMin(VDIV_MIN);
            mcg_c6_vdiv0Var.setMax(VDIV_MAX);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * 
    * @throws Exception
    */
   @Override
   protected void validate(Variable variable, int index) throws Exception {

//      final LongVariable   mcg_erc_clockVar       = getLongVariable("mcg_erc_clock");
      final LongVariable   pll0InputFrequencyVar  = getLongVariable("pll0InputFrequency");
      final LongVariable   pll0OutputFrequencyVar = getLongVariable("pll0OutputFrequency");
//      final LongVariable   usb1pfdclk_ClockVar    = safeGetLongVariable("/SIM/usb1pfdclk_Clock");
//      final ChoiceVariable mcg_c7_oscselVar       = safeGetChoiceVariable("mcg_c7_oscsel");
      
//      long mcg_erc_clockFreq  = mcg_erc_clockVar.getValueAsLong();
      long pllTargetFrequency = pll0OutputFrequencyVar.getRawValueAsLong();

      BooleanVariable pll0EnabledVar = getBooleanVariable("pll0Enabled");
      boolean pll0Enabled = pll0EnabledVar.getValueAsBoolean();
      
      pll0InputFrequencyVar.enable(pll0Enabled);
      pll0OutputFrequencyVar.enable(pll0Enabled);

      if (!pll0Enabled) {
         pll0InputFrequencyVar.clearStatus();
         pll0OutputFrequencyVar.clearStatus();
         return;
      }

//      Status mcgErcStatus = mcg_erc_clockVar.getStatus();
//      if ((mcgErcStatus!= null) && mcgErcStatus.greaterThan(Severity.INFO)) {
//         // Input clock is invalid
//         pll0OutputFrequencyVar.setStatus(mcgErcStatus);
//         pll0InputFrequencyVar.setStatus(mcgErcStatus);
//
//         // Change nothing
//         return;
//      }
      long  mcg_prdiv = PRDIV_MIN;
      long  mcg_vdiv  = VDIV_MIN;

      boolean pllInputValid  = false;
      boolean pllOutputValid = false;

      Set<Long> pllFrequencies = new TreeSet<Long>();

      StringBuilder sb = new StringBuilder();
      long nearest_PllOutFrequency = Long.MAX_VALUE;

      long pllInputFrequency = pll0InputFrequencyVar.getValueAsLong();
      
      // Try each prescale value
      for (long mcg_prdiv_probe = PRDIV_MIN; mcg_prdiv_probe <= PRDIV_MAX; mcg_prdiv_probe++) {
         if (sb.length()>0) {
            //            System.err.println(sb.toString());
            sb = new StringBuilder();
         }
         double pllDividedFrequency = pllInputFrequency/mcg_prdiv_probe;
         sb.append(String.format("(prdiv = %d, pllIn=%f) => ", mcg_prdiv_probe, pllDividedFrequency));
         if (pllDividedFrequency>PLL_IN_MAX) {
            // Invalid as input to PLL
            sb.append("too high");
            continue;
         }
         if (pllDividedFrequency<PLL_IN_MIN) {
            // Invalid as input to PLL
            sb.append("too low");
            break;
         }
         pllInputValid = true;
         // Try each multiplier value
         for (long mcg_vdiv_probe=VDIV_MIN; mcg_vdiv_probe<=VDIV_MAX; mcg_vdiv_probe++) {
            long pllOutFrequency = Math.round((mcg_vdiv_probe*pllDividedFrequency)/PLL_POST_DIV);
            sb.append(pllOutFrequency);
            if (pllOutFrequency<PLL_OUT_MIN) {
               sb.append("<, ");
               continue;
            }
            if (pllOutFrequency>PLL_OUT_MAX) {
               sb.append(">, ");
               break;
            }
            sb.append("*,");
            pllFrequencies.add(pllOutFrequency);

            // Best so far
            if (Math.abs(pllOutFrequency-pllTargetFrequency)<Math.abs(nearest_PllOutFrequency-pllTargetFrequency))  {
               nearest_PllOutFrequency = pllOutFrequency;
               mcg_prdiv = mcg_prdiv_probe;
               mcg_vdiv  = mcg_vdiv_probe;
            }
            // Accept value within ~2.5% of desired
            if (Math.abs(pllOutFrequency - pllTargetFrequency) < (pllTargetFrequency/50)) {
               sb.append("=");
               pllOutputValid = true;
            }
         }
         if (sb.length()>0) {
            sb = new StringBuilder();
         }
      }
      
      // Update with 'best value' - irrespective of whether they are acceptable
      final LongVariable mcg_c5_prdiv0Var       = getLongVariable("mcg_c5_prdiv0");
      final LongVariable mcg_c6_vdiv0Var        = getLongVariable("mcg_c6_vdiv0");

      mcg_c5_prdiv0Var.setValue(mcg_prdiv);
      mcg_c5_prdiv0Var.setStatus(new Status("Field value = 0b" + Long.toBinaryString(mcg_prdiv-1), Severity.OK));
      mcg_c6_vdiv0Var.setValue(mcg_vdiv);
      mcg_c6_vdiv0Var.setStatus(new Status("Field value = 0b" + Long.toBinaryString(mcg_vdiv-PLL_POST_DIV), Severity.OK));

//      pll0InputFrequencyVar.setOrigin(mcg_erc_clockVar.getOrigin()+"\n/mcg.c7.prdiv0");
//      pll0OutputFrequencyVar.setOrigin(mcg_erc_clockVar.getOrigin()+"\n via PLL");

      Status pllStatus = null;
      if (!pllInputValid) {
         String msg = String.format("PLL not usable with input clock frequency %sHz\nRange: [%s,%s]",
               EngineeringNotation.convert(pllInputFrequency,3),
               EngineeringNotation.convert(PLL_IN_MIN,3),EngineeringNotation.convert(PLL_IN_MAX,3));
         Status status = new Status(msg, Severity.ERROR);
//         pll0InputFrequencyVar.setStatus(status);
         pllStatus = status;
      }
      else {
         // PLL-in is valid
//         pll0InputFrequencyVar.setStatus((Status)null);

         // Check PLL out
         StringBuilder status = new StringBuilder();
         Status.Severity severity = Severity.OK;
         if (!pllOutputValid) {
            // PLL Output invalid
            status.append("Not possible to generate desired PLL frequency from input clock\n");
            severity = Severity.ERROR;
         }
         else {
            // PLL Output valid
            if (pllTargetFrequency != nearest_PllOutFrequency) {
               // Update PLL as it was approximated
               pllTargetFrequency = nearest_PllOutFrequency;
               pll0OutputFrequencyVar.setValue(pllTargetFrequency);
//               System.err.println("PLL = " + pllTargetFrequency);
            }
         }
         status.append("Possible values = \n");
         boolean needComma = false;
         int lineCount = -1;
         for (Long freq : pllFrequencies) {
            if (needComma) {
               status.append(", ");
            }
            if (lineCount++>=10) {
               status.append("\n");
               lineCount = 0;
            }
            needComma = true;
            status.append(EngineeringNotation.convert(freq, 3)+"Hz");
         }
         pllStatus = new Status(status.toString(), severity);
      }
      pll0OutputFrequencyVar.setStatus(pllStatus);
   }

   @Override
   protected boolean createDependencies() throws Exception {
      super.createDependencies();

      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      variablesToWatch.add("pll0Enabled");
      variablesToWatch.add("pll0InputFrequency");
      variablesToWatch.add("pll0OutputFrequency");
      
      addSpecificWatchedVariables(variablesToWatch);

      return false;
   }
}
