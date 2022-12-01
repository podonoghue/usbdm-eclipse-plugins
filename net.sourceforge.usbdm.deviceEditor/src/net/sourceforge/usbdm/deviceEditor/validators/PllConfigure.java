package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;

public class PllConfigure {
   private final long PLL_IN_MIN;
   private final long PLL_IN_MAX;

   private final long PLL_OUT_MIN;
   private final long PLL_OUT_MAX;

   private final int  PRDIV_MIN;
   private final int  PRDIV_MAX;

   private final int  VDIV_MIN;
   private final int  VDIV_MAX;

   private final int  PLL_POST_DIV;
   
   /** Status of PLL i.e. whether a divider etc could be calculated */
   private Status pllStatus;

   /**
    * Validates PLL related variables
    * VDIV & PRDIV
    * 
    * @param pllOutMin
    * @param pllOutMax
    * @param pllInMin
    * @param pllInMax
    * @param prDivMin
    * @param prDivMax
    * @param vDivMin
    * @param vDivMax
    * @param pllPostDiv
    */
   public PllConfigure(
         long pllOutMin,
         long pllOutMax,
         long pllInMin,
         long pllInMax,
         long prDivMin,
         long prDivMax,
         long vDivMin,
         long vDivMax,
         long pllPostDiv) {
      
      PLL_OUT_MIN   = pllOutMin;
      PLL_OUT_MAX   = pllOutMax;
      PLL_IN_MIN    = pllInMin;
      PLL_IN_MAX    = pllInMax;
      PRDIV_MIN     = (int) prDivMin;
      PRDIV_MAX     = (int) prDivMax;
      VDIV_MIN      = (int) vDivMin;
      VDIV_MAX      = (int) vDivMax;
      PLL_POST_DIV  = (int) pllPostDiv;
   }

   protected void validate(Variable mcg_erc_clockVar,
         Variable pll0InputFrequencyVar,
         Variable pll0OutputFrequencyVar,
         Variable mcg_c5_prdiv0Var,
         Variable mcg_c6_vdiv0Var ) {

      // Main clock used by FLL
      long mcg_erc_clockFreq = mcg_erc_clockVar.getValueAsLong();

      long pllTargetFrequency = pll0OutputFrequencyVar.getRawValueAsLong();

//      System.err.println(String.format("\nPllClockValidater.validate(): mcg_erc_clock = %d, pllTargetFrequency = %d", mcg_erc_clock, pllTargetFrequency));

      int  mcg_prdiv = PRDIV_MIN;
      int  mcg_vdiv  = VDIV_MIN;

      boolean pllInputValid  = false;
      boolean pllOutputValid = false;

      Set<Long> pllFrequencies = new TreeSet<Long>();

      StringBuilder sb = new StringBuilder();
      long nearest_PllOutFrequency = Long.MAX_VALUE;

      Status mcgErcStatus = mcg_erc_clockVar.getStatus();
      if ((mcgErcStatus!= null) && mcgErcStatus.greaterThan(Severity.INFO)) {
         // Input clock is invalid
         pll0OutputFrequencyVar.setStatus(mcgErcStatus);
         pll0InputFrequencyVar.setStatus(mcgErcStatus);
         
         // Change nothing
         return;
      }
      // Try each prescale value
      for (int mcg_prdiv_probe = PRDIV_MIN; mcg_prdiv_probe <= PRDIV_MAX; mcg_prdiv_probe++) {
         if (sb.length()>0) {
            //            System.err.println(sb.toString());
            sb = new StringBuilder();
         }
         double pllInFrequency = mcg_erc_clockFreq/mcg_prdiv_probe;
         sb.append(String.format("(prdiv = %d, pllIn=%f) => ", mcg_prdiv_probe, pllInFrequency));
         if (pllInFrequency>PLL_IN_MAX) {
            // Invalid as input to PLL
            sb.append("too high");
            continue;
         }
         if (pllInFrequency<PLL_IN_MIN) {
            // Invalid as input to PLL
            sb.append("too low");
            break;
         }
         pllInputValid = true;
         // Try each multiplier value
         for (int mcg_vdiv_probe=VDIV_MIN; mcg_vdiv_probe<=VDIV_MAX; mcg_vdiv_probe++) {
            long pllOutFrequency = Math.round((mcg_vdiv_probe*pllInFrequency)/PLL_POST_DIV);
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
      mcg_c5_prdiv0Var.setValue(mcg_prdiv);
      mcg_c5_prdiv0Var.setStatus(new Status("Field value = 0b" + Integer.toBinaryString(mcg_prdiv-1), Severity.OK));
      mcg_c6_vdiv0Var.setValue(mcg_vdiv);
      mcg_c6_vdiv0Var.setStatus(new Status("Field value = 0b" + Integer.toBinaryString(mcg_vdiv-PLL_POST_DIV), Severity.OK));

      pll0InputFrequencyVar.setValue(mcg_erc_clockFreq/mcg_prdiv);
      pll0InputFrequencyVar.setOrigin(mcg_erc_clockVar.getOrigin()+"\n/mcg.c7.prdiv0");
      pll0OutputFrequencyVar.setOrigin(mcg_erc_clockVar.getOrigin()+"\n via PLL");

      if (!pllInputValid) {
         String msg = String.format("PLL not usable with input clock frequency %sHz\nRange: [%s,%s]",
               EngineeringNotation.convert(mcg_erc_clockFreq,3),
               EngineeringNotation.convert(PLL_IN_MIN,3),EngineeringNotation.convert(PLL_IN_MAX,3));
         Status status = new Status(msg, Severity.ERROR);
         pll0InputFrequencyVar.setStatus(status);
         pllStatus = status;
      }
      else {
         // PLL-in is valid
         pll0InputFrequencyVar.setStatus((Status)null);

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

   /**
    * Status of PLL i.e. whether a divider etc could be calculated
    * 
    * @return
    */
   public Status getPllStatus() {
      return pllStatus;
   }
}