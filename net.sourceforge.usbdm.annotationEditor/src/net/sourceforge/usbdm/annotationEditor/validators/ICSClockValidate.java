package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class ICSClockValidate extends MyValidator {

   private static final long FLL_CLOCK_RANGE1_MIN = 31250L;
   private static final long FLL_CLOCK_RANGE1_MAX = 39063L;
   
   private static final long FLL_CLOCK_RANGE2_MIN =  4000000L;
   private static final long FLL_CLOCK_RANGE2_MAX = 20000000L;
   
   private static final long FLL_CLOCK_WIDE_MIN = 31250L;
   private static final long FLL_CLOCK_WIDE_MAX = 39063L;
   
   private static long FLL_FACTOR;
   
   // Backwards compatible
   public ICSClockValidate() {
      this(1024);
   }
   
   public ICSClockValidate(long fllFactor) {
      FLL_FACTOR = fllFactor;
   }
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode           =  getNumericModelNode("clock_mode");
      NumericOptionModelNode fllTargetFrequencyNode         =  getNumericModelNode("fllTargetFrequency");
      NumericOptionModelNode oscclk_clockNode               =  getNumericModelNode("oscclk_clock");
      NumericOptionModelNode internalReferenceClockNode     =  getNumericModelNode("system_slow_irc_clock");
      BinaryOptionModelNode  osc_cr_oscosNode               =  getBinaryModelNode("osc_cr_oscos");
      NumericOptionModelNode osc_cr_rangeNode               =  getNumericModelNode("osc_cr_range");
      NumericOptionModelNode ics_c1_rdivNode                =  getNumericModelNode("ics_c1_rdiv");
      NumericOptionModelNode ics_c1_irefsNode               =  getNumericModelNode("ics_c1_irefs");
      
      // Main clock used by FLL
      long oscclk_clock = oscclk_clockNode.getValueAsLong();

      //=========================================
      // Determine osc_cr_range
      //    - Clock range of oscillator
      //    - Affects FLL prescale
      //
      long   osc_cr_range         = -1;
      String osc_cr_oscos_message = null;

      if ((oscclk_clock >= FLL_CLOCK_RANGE1_MIN) && (oscclk_clock <= FLL_CLOCK_RANGE1_MAX)) {
         osc_cr_range = 0;
      }
      else if ((oscclk_clock >= FLL_CLOCK_RANGE2_MIN) && (oscclk_clock <= FLL_CLOCK_RANGE2_MAX)) {
         osc_cr_range = 1;
      }
      
      if (osc_cr_range < 0) {
         if (osc_cr_oscosNode.safeGetValue()) {
            // External crystal selected but not suitable frequency
            osc_cr_oscos_message = "Frequency of the External Crystal is not suitable for use with the Oscillator\n";
            osc_cr_oscos_message += String.format("Permitted ranges [%d-%d] and [%d-%d]", FLL_CLOCK_RANGE1_MIN, FLL_CLOCK_RANGE1_MAX, FLL_CLOCK_RANGE2_MIN, FLL_CLOCK_RANGE2_MAX);
         }
         // Set compromise value
         if (oscclk_clock <= FLL_CLOCK_RANGE2_MIN) {
            osc_cr_range = 0;
         }
         else {
            osc_cr_range = 1;
         }
      }

      //=================
      // Determine External FLL reference after dividers
      //
      double externalfllInputFrequencyAfterDivider = 0.0;
      if (osc_cr_range == 0) {
         externalfllInputFrequencyAfterDivider = oscclk_clock;
      }
      else {
         externalfllInputFrequencyAfterDivider = oscclk_clock / (1<<5);
      }
      
      String ics_c1_rdivMessage = null;
      int    ics_c1_rdiv        = 0;

      // Assume no errors
      boolean validFllInputClock = true;
      
      if      (((externalfllInputFrequencyAfterDivider/1)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/1)<=FLL_CLOCK_WIDE_MAX)) {
         ics_c1_rdiv =  0; externalfllInputFrequencyAfterDivider /= 1;
      }
      else if (((externalfllInputFrequencyAfterDivider/2)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/2)<=FLL_CLOCK_WIDE_MAX)) {
         ics_c1_rdiv =  1; externalfllInputFrequencyAfterDivider /= 2;
      }
      else if (((externalfllInputFrequencyAfterDivider/4)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/4)<=FLL_CLOCK_WIDE_MAX)) {
         ics_c1_rdiv =  2; externalfllInputFrequencyAfterDivider /= 4;
      }
      else if (((externalfllInputFrequencyAfterDivider/8)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/8)<=FLL_CLOCK_WIDE_MAX)) {
         ics_c1_rdiv =  3; externalfllInputFrequencyAfterDivider /= 8;
      }
      else if (((externalfllInputFrequencyAfterDivider/16)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/16)<=FLL_CLOCK_WIDE_MAX)) {
         ics_c1_rdiv =  4; externalfllInputFrequencyAfterDivider /= 16;
      }
      else if (((externalfllInputFrequencyAfterDivider/32)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/32)<=FLL_CLOCK_WIDE_MAX)) {
         ics_c1_rdiv =  5; externalfllInputFrequencyAfterDivider /= 32;
      }
      else if (((externalfllInputFrequencyAfterDivider/64)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/64)<=FLL_CLOCK_WIDE_MAX)  && (osc_cr_range  == 0)) {
         ics_c1_rdiv =  6; externalfllInputFrequencyAfterDivider /= 64;
      }
      else if (((externalfllInputFrequencyAfterDivider/128)>=FLL_CLOCK_WIDE_MIN) && ((externalfllInputFrequencyAfterDivider/128)<=FLL_CLOCK_WIDE_MAX) && (osc_cr_range == 0)) {
         ics_c1_rdiv =  7; externalfllInputFrequencyAfterDivider /= 128;
      }
      else {
         validFllInputClock = false;
         ics_c1_rdivMessage = String.format("Unable to find suitable divider for external reference clock frequency = %d Hz", oscclk_clock);
         ics_c1_rdiv        =  7;
      }
      System.err.println("FllClockValidate.validate() externalfllInputFrequencyAfterDivider = " + externalfllInputFrequencyAfterDivider);

      //=================
      // Determine FLL input frequency.  From:
      //  - External reference (after dividers)
      //  - Internal (slow) reference
      String fllTargetFrequencyMessage = null;
      double fllInputFrequency = 0;
      
      if (ics_c1_irefsNode.getValueAsLong() == 0) {
         // Using external reference clock
         if (validFllInputClock) {
            fllInputFrequency = externalfllInputFrequencyAfterDivider;
         }
      }
      else {
         // Using internal (low frequency) reference
         validFllInputClock = true;
         fllInputFrequency = internalReferenceClockNode.getValueAsLong();
      }
      
      if (!validFllInputClock) {
         fllTargetFrequencyMessage = "FLL not usable with input clock frequency";
         fllTargetFrequencyNode.setEnabled(false);

         // Check if trying to use FLL 
         if (primaryClockModeNode.getValueAsLong() == ClockValidate_KLxx.ClockModes.FEEClock.ordinal()) {
            // Done inside an IF as another validator may set this error message
            setValid(viewer, primaryClockModeNode, "FLL input reference is out of acceptable ranges. FLL may not be used");
         }
      }
      else {
         System.err.println("FllClockValidate.validate() fllInputFrequency = " + fllInputFrequency);
         fllTargetFrequencyNode.setEnabled(true);

         //=================
         // Determine possible output frequencies & check against desired value
         //
         long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();
         long fllOutFrequency    = Math.round(fllInputFrequency * FLL_FACTOR);
         ArrayList<Long> fllFrequencies = new ArrayList<Long>(); 
         fllFrequencies.add(fllOutFrequency*1);
         if (fllOutFrequency != fllTargetFrequency) {
            StringBuilder buff = new StringBuilder("Not possible to generate desired FLL frequency from input clock. \nPossible values (Hz) = ");
            boolean needComma = false;
            for (Long freq : fllFrequencies) {
               if (needComma) {
                  buff.append(", ");
               }
               needComma = true;
               buff.append(String.format("%d", freq));
            }
            fllTargetFrequencyMessage = buff.toString();
         }
         System.err.println("FllClockValidate.validate() fllOutFrequency = " + fllOutFrequency);
      }
      
      update(viewer, ics_c1_rdivNode, ics_c1_rdiv);
      update(viewer, osc_cr_rangeNode, osc_cr_range);
      setValid(viewer, ics_c1_rdivNode, ics_c1_rdivMessage);
      setValid(viewer, osc_cr_oscosNode, osc_cr_oscos_message);
      setValid(viewer, fllTargetFrequencyNode, fllTargetFrequencyMessage);
   }

}
