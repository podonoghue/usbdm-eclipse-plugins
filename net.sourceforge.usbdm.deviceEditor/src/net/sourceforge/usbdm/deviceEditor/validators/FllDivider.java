package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

/**
 * Class to determine FLL divider settings
 */
class FllDivider {

   private static final long FLL_CLOCK_WIDE_MIN   = 31250L;
   private static final long FLL_CLOCK_WIDE_MAX   = 39063L;
   private static final long FLL_CLOCK_NARROW_MIN = 32768L-100;
   private static final long FLL_CLOCK_NARROW_MAX = 32768L+100;

   // Choose range and divisor based on suitable FLL input frequency
   private static final int[] LOW_RANGE_DIVISORS  = {
         1, 2, 4, 8, 16, 32, 64, 128
   };
   private static final int[] HIGH_RANGE_DIVISORS = {
         1*(1<<5), 2*(1<<5), 4*(1<<5), 8*(1<<5), 16*(1<<5), 32*(1<<5), 40*(1<<5), 48*(1<<5)
   };

   private double nearestError     = Double.MAX_VALUE;
   private double nearestFrequency = 0.0;
   private int    nearest_frdiv    = 0;
   private int    mcg_c1_frdiv_calc;
   private double fllInputFrequency_calc;

   /** Calculated MCG_C1_FRDIV value */
   public final int  mcg_c1_frdiv;

   /** MCG_C2_RANGE chosen based upon FLL input frequency */
   public final int mcg_c2_range;
   
   /** MCG_C4_DRST_DRS value corresponding to fllTargetFrequency */
   public final int mcg_c4_drst_drs;
   
   /**
    * Find suitable FLL divider (frdiv)
    * 
    * @param fllInputClock       Input clock to be divided
    * @param mcg_c4_dmx32Node    Clock mode (affects input range)
    * @param rangeDivisors       Possible dividers to select
    * 
    * @return True => Found suitable divider
    */
   boolean findDivider(long fllInputClock, boolean mcg_c4_dmx32Node, int[] rangeDivisors) { 

      long fllInMin;
      long fllInMax;
      
      if (mcg_c4_dmx32Node) {
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
    * @param mcg_c2_rangeIn         [in]     Range
    * @param mcg_c1_irefs           [in]     irefs value (affect clock source)
    * @param mcg_erc_clockNode      [in]     mcg_erc_clock variable
    * @param slow_irc_clock         [in]     Frequency of slow IRC
    * @param mcg_c7_oscsel          [in]     OSCSEL value used to constrain dividers
    * @param mcg_c4_dmx32Node       [in]     Affects input range accepted 
    * @param fllInputFrequencyNode  [in/out] Input to FLL
    * @param fllTargetFrequencyNode [out]    Output from FLL
    */
   public FllDivider(
         int mcg_c2_rangeIn, boolean mcg_c1_irefs, 
         final Variable mcg_erc_clockNode, 
         long  slow_irc_clock, 
         long  mcg_c7_oscsel, boolean mcg_c4_dmx32, 
         final Variable fllInputFrequencyNode, 
         final Variable fllTargetFrequencyNode) {

      boolean found = false;
      
      String  fllOrigin;
      Message status;
      
      long availableClock;
      if (mcg_c1_irefs) {
         // Slow internal clock selected
         fllOrigin      = "Slow internal reference clock";
         status         = null;
         availableClock = slow_irc_clock;
      }
      else {
         fllOrigin      = mcg_erc_clockNode.getOrigin();
         status         = mcg_erc_clockNode.getStatus();
         availableClock = mcg_erc_clockNode.getValueAsLong();
      }
      fllTargetFrequencyNode.setOrigin(fllOrigin+" via FLL");
      if (mcg_c1_irefs) {
         // No dividers - direct from Slow IRC
         found = true;
         found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
         if (mcg_c2_rangeIn<0) {
            mcg_c2_rangeIn = 0;
         }
      }
      else if (mcg_c7_oscsel == 1) {
         // Forced to LOW_RANGE_DIVISORS irrespective of range
         found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
         fllOrigin += " after scaling by (Low range FRDIV)";
         if (mcg_c2_rangeIn<0) {
            mcg_c2_rangeIn = 0;
         }
      }
      else {
         switch (mcg_c2_rangeIn) {
         case 0:
            found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            fllOrigin += " after scaling (Low range FRDIV)";
            break;
         case 1:
            found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            fllOrigin += " after scaling (High range FRDIV)";
            break;
         case 2:
            found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            fllOrigin += " after scaling (High range FRDIV)";
         case -1: 
            // Unconstrained - try both sets of dividers
            mcg_c2_rangeIn = 0;
            found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            if (!found) {
               mcg_c2_rangeIn = 1;
               found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            }
            fllOrigin += " after scaling (Low/High range FRDIV)";
            break;
         }
      }
      // Record range in use
      mcg_c2_range = mcg_c2_rangeIn;
      
      long inputFrequency;
      if (found) {
         inputFrequency  = Math.round(fllInputFrequency_calc);
         mcg_c1_frdiv       = mcg_c1_frdiv_calc;
      }
      else {
         inputFrequency  = Math.round(nearestFrequency);
         mcg_c1_frdiv       = nearest_frdiv;
         if (status == null) {
            String msgText = String.format("Unable to find suitable FLL divisor for input frequency of %s", 
                  EngineeringNotation.convert(availableClock, 3));
            status = new Message(msgText, Severity.WARNING);
         }
      }
      // Record FLL input details
      fllInputFrequencyNode.setValue(inputFrequency);
      fllInputFrequencyNode.setStatus(status);
      fllInputFrequencyNode.setOrigin(fllOrigin);
      
      // Determine possible output frequencies & check against desired value
      //=======================================================================
      long fllOutFrequency;
      if (mcg_c4_dmx32) {
         fllOutFrequency = inputFrequency * 732;
      }
      else {
         fllOutFrequency = inputFrequency * 640;
      }
      int mcg_c4_drst_drs_calc = -1;

      Long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();
      ArrayList<Long> fllFrequencies = new ArrayList<Long>(); 
      for (int probe=1; probe<=4; probe++) {
         fllFrequencies.add(fllOutFrequency*probe);
         // Accept value within ~10% of desired
         if (Math.abs((fllOutFrequency*probe) - fllTargetFrequency) < (fllTargetFrequency/10)) {
            mcg_c4_drst_drs_calc = probe-1;
         }         
      }
      StringBuilder sb = new StringBuilder();
      Severity severity;;
      if (mcg_c4_drst_drs_calc >= 0) {
         // Adjust rounded value
         fllTargetFrequency = fllOutFrequency*(mcg_c4_drst_drs_calc+1);
         severity = Severity.OK;
      }
      else {
         mcg_c4_drst_drs_calc = 0;
         sb.append("Not possible to generate desired FLL frequency from input clock\n");
         severity = Severity.WARNING;
      }
      boolean needComma = false;
      for (Long freq : fllFrequencies) {
         if (needComma) {
            sb.append(", ");
         }
         else {
            sb.append("Possible values (Hz) = ");
         }
         needComma = true;
         sb.append(EngineeringNotation.convert(freq, 5)+"Hz");
      }
      status = new Message (sb.toString(), severity);
      fllTargetFrequencyNode.setValue(fllTargetFrequency);
      fllTargetFrequencyNode.setStatus(status);
      
      mcg_c4_drst_drs = mcg_c4_drst_drs_calc;
   }
}