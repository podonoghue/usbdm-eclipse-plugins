package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

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

   /** Calculated MCG_C1_FRDIV value */
   public final int     mcg_c1_frdiv;

   /** Message related to MCG_C1_FRDIV value */
   public final Message mcg_c1_frdiv_clockMessage;

   /** FLL Input frequency determined by range, divisor etc */
   public final double  fllInputFrequency;

   /** MCG_C2_VALUE chosen based upon FLL input frquency */
   public final int mcg_c2_range;
   
   /** FLL Target frequency arrived at */
   public final long fllTargetFrequency;
   
   /** Message describing fllTargetFrequency */
   public final Message fllTargetFrequencyMessage;

   /** MCG_C4_DRST_DRS value corresponding to fllTargetFrequency */
   public final int mcg_c4_drst_drs;
   
   private int     mcg_c1_frdiv_calc;
   private double  fllInputFrequency_calc;

   private double nearestError     = Double.MAX_VALUE;
   private double nearestFrequency = 0.0;
   private int    nearest_frdiv    = 0;

   /**
    * Find suitable FLL divider (frdiv)
    * 
    * @param fllInputClock Input clock to be divided
    * @param mcg_c4_dmx32Node 
    * @param rangeDivisors Possible dividers to select
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
    * @param mcg_c2_rangeIn         Range
    * @param mcg_c1_irefs           
    * @param mcg_erc_clock          Frequency of selected mcg_erc_clock
    * @param slow_irc_clock
    * @param mcg_c7_oscsel          OSCSEL value used to constrain dividers
    * @param mcg_c4_dmx32Node       Affects input range accepted 
    * @param fllTargetFrequencyIn
    */
   public FllDivider(int mcg_c2_rangeIn, boolean mcg_c1_irefs, long mcg_erc_clock, 
         long slow_irc_clock, long mcg_c7_oscsel, boolean mcg_c4_dmx32, long fllTargetFrequencyIn) {

      boolean found = false;
      
      String fllInputMessage = "Origin = External reference clock";
      long availableClock = mcg_erc_clock;
      if (mcg_c1_irefs) {
         // Slow internal clock selected
         fllInputMessage = "Origin = Internal reference clock";
         availableClock = slow_irc_clock;
      }
      if (mcg_c7_oscsel == 1) {
         // Forced to LOW_RANGE_DIVISORS irrespective of range
         found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
         fllInputMessage += " after scaling";
         if (mcg_c2_rangeIn<0) {
            mcg_c2_rangeIn = 0;
         }
      }
      else {
         switch (mcg_c2_rangeIn) {
         case 0:
            found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            break;
         case 1:
            found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            fllInputMessage += " after scaling";
            break;
         case 2:
            found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            fllInputMessage += " after scaling";
         case -1: 
            // Unconstrained - try both sets of dividers
            mcg_c2_rangeIn = 0;
            found = findDivider(availableClock, mcg_c4_dmx32, LOW_RANGE_DIVISORS);
            if (!found) {
               mcg_c2_rangeIn = 1;
               fllInputMessage += " after scaling";
               found = findDivider(availableClock, mcg_c4_dmx32, HIGH_RANGE_DIVISORS);
            }
            break;
         }
      }
      mcg_c2_range = mcg_c2_rangeIn;
      
      if (found) {
         mcg_c1_frdiv_clockMessage  = new Message(fllInputMessage, Severity.OK);
         fllInputFrequency          = fllInputFrequency_calc;
         mcg_c1_frdiv               = mcg_c1_frdiv_calc;
      }
      else {
         String msgText = String.format("Unable to find suitable FLL divisor for input frequency of %s\n", 
               EngineeringNotation.convert(mcg_erc_clock, 3));
         mcg_c1_frdiv_clockMessage  = new Message(msgText, Severity.WARNING);
         fllInputFrequency          = nearestFrequency;
         mcg_c1_frdiv               = nearest_frdiv;
      }
      
      // Determine possible output frequencies & check against desired value
      //=======================================================================
      long fllOutFrequency;
      if (mcg_c4_dmx32) {
         fllOutFrequency = Math.round(fllInputFrequency * 732.0);
      }
      else {
         fllOutFrequency = Math.round(fllInputFrequency * 640.0);
      }
      int mcg_c4_drst_drs_calc = -1;

      ArrayList<Long> fllFrequencies = new ArrayList<Long>(); 
      for (int probe=1; probe<=4; probe++) {
         fllFrequencies.add(fllOutFrequency*probe);
         // Accept value within ~10% of desired
         if (Math.abs((fllOutFrequency*probe) - fllTargetFrequencyIn) < (fllTargetFrequencyIn/10)) {
            mcg_c4_drst_drs_calc = probe-1;
         }         
      }

      StringBuilder sb = new StringBuilder();
      Severity severity = Severity.OK;
      
      if (mcg_c4_drst_drs_calc >= 0) {
         // Adjust rounded value
         fllTargetFrequency = (fllOutFrequency*(mcg_c4_drst_drs_calc+1));;
      }
      else {
         mcg_c4_drst_drs_calc = 0;
         sb.append("Not possible to generate desired FLL frequency from input clock. \n");
         severity = Severity.WARNING;
         fllTargetFrequency = fllTargetFrequencyIn;
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
      fllTargetFrequencyMessage = new Message(sb.toString(), severity);
      
      mcg_c4_drst_drs = mcg_c4_drst_drs_calc;

//      mcg_c4_drst_drsNode.setValue(mcg_c4_drst_drs_calc);
//      mcg_c4_drst_drsNode.enable(fll_enabled);
//      fllTargetFrequencyNode.setMessage(fllTargetFrequencyMessage);
   }
}