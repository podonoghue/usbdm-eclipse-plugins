package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.validators.ClockValidate_CFV1.ClockModes;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class FllClockValidate_CFV1 extends MyValidator {

   private static final long FLL_CLOCK_RANGE1_MIN =  32000L;
   private static final long FLL_CLOCK_RANGE1_MAX = 100000L;
   
   private static final long FLL_CLOCK_RANGE2_MIN =  1000000L;
   private static final long FLL_CLOCK_RANGE2_MAX = 16000000L;
   
   private static final long FLL_CLOCK_NARROW_MIN = 32768L-100;
   private static final long FLL_CLOCK_NARROW_MAX = 32768L+100;
   
   private static final long FLL_CLOCK_WIDE_MIN = 31250L;
   private static final long FLL_CLOCK_WIDE_MAX = 39063L;
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode           =  getNumericModelNode("clock_mode");
      NumericOptionModelNode fllTargetFrequencyNode         =  getNumericModelNode("fllTargetFrequency");
      NumericOptionModelNode oscclk_clockNode               =  getNumericModelNode("oscclk_clock");
      NumericOptionModelNode internalReferenceClockNode     =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode mcg_c1_rdivNode                =  getNumericModelNode("mcg_c1_rdiv");
      NumericOptionModelNode mcg_c1_irefsNode               =  getNumericModelNode("mcg_c1_irefs");
      NumericOptionModelNode mcg_c2_rangeNode               =  getNumericModelNode("mcg_c2_range");
      BinaryOptionModelNode  mcg_c2_erefsNode               =  getBinaryModelNode("mcg_c2_erefs0");
      NumericOptionModelNode mcg_c3_pllsNode                =  getNumericModelNode("mcg_c3_plls");
      BinaryOptionModelNode  mcg_c3_div32Node               =  getBinaryModelNode("mcg_c3_div32");
      BinaryOptionModelNode  mcg_c4_dmx32Node               =  getBinaryModelNode("mcg_c4_dmx32");
      NumericOptionModelNode mcg_c4_drsNode                 =  getNumericModelNode("mcg_c4_drs");
      
      long clk = primaryClockModeNode.getValueAsLong();
      if (clk > ClockModes.values().length) {
         throw new Exception("Illegal Clock mode");
      }
      ClockModes primaryClockMode = ClockModes.values()[(int)primaryClockModeNode.getValueAsLong()];
      
      long mcg_c3_plls = mcg_c3_pllsNode.getValueAsLong();
      
      // Main clock used by FLL
      long oscclk_clock = oscclk_clockNode.getValueAsLong();

      //=========================================
      // Determine mcg_c2_range
      //    - Clock range of oscillator
      //    - Affects FLL prescale
      //
      long   mcg_c2_range         = -1;
      String mcg_c2_erefs_message = null;

      if ((oscclk_clock >= FLL_CLOCK_RANGE1_MIN) && (oscclk_clock <= FLL_CLOCK_RANGE1_MAX)) {
         mcg_c2_range = 0;
      }
      else if ((oscclk_clock >= FLL_CLOCK_RANGE2_MIN) && (oscclk_clock <= FLL_CLOCK_RANGE2_MAX)) {
         mcg_c2_range = 1;
      }
      if (mcg_c2_range < 0) {
         if (mcg_c2_erefsNode.safeGetValue()) {
            // External crystal selected but not suitable frequency
            mcg_c2_erefs_message = "Frequency of the External Crystal is not suitable for use with the Oscillator\n";
            mcg_c2_erefs_message += String.format("Permitted ranges [%d-%d] or [%d-%d]", 
                  FLL_CLOCK_RANGE1_MIN, FLL_CLOCK_RANGE1_MAX, FLL_CLOCK_RANGE2_MIN, FLL_CLOCK_RANGE2_MAX);
         }
         // Set compromise value
         if (oscclk_clock <= FLL_CLOCK_RANGE2_MIN) {
            mcg_c2_range = 0;
         }
         else {
            mcg_c2_range = 1;
         }
      }

      //=================
      // Determine External FLL reference after dividers
      //
      double  externalfllInputFrequencyAfterDivider = 0.0;
      String  mcg_c1_rdivMessage = null;
      int     mcg_c1_rdiv        = 0;
      boolean mcg_c3_div32       = false;

      // Assume no suitable value possible
      boolean validFllInputClock = false;
      
      for (mcg_c1_rdiv=0; mcg_c1_rdiv<8; mcg_c1_rdiv++) {
         // Each possible divider
         externalfllInputFrequencyAfterDivider = ((double)oscclk_clock)/(1<<mcg_c1_rdiv);
         validFllInputClock = ((externalfllInputFrequencyAfterDivider>=FLL_CLOCK_WIDE_MIN) && (externalfllInputFrequencyAfterDivider<=FLL_CLOCK_WIDE_MAX));
         if (!validFllInputClock && (mcg_c2_range == 1)) {
            // Try with extra divider
            externalfllInputFrequencyAfterDivider = ((double)oscclk_clock)/(1<<(mcg_c1_rdiv+5));
            validFllInputClock = ((externalfllInputFrequencyAfterDivider>=FLL_CLOCK_WIDE_MIN) && (externalfllInputFrequencyAfterDivider<=FLL_CLOCK_WIDE_MAX));
            mcg_c3_div32 = true;
         }
         if (validFllInputClock) {
            break;
         }
      }
      if (!validFllInputClock) {
         mcg_c1_rdivMessage = String.format("Unable to find suitable divider for external reference clock frequency = %d Hz", oscclk_clock);
         mcg_c1_rdiv  = 7;
         mcg_c3_div32 = false;
      }
         
//      System.err.println("FllClockValidate.validate() externalfllInputFrequencyAfterDivider = " + externalfllInputFrequencyAfterDivider);

      //=================
      // Determine FLL input frequency.  From:
      //  - External reference (after dividers)
      //  - Internal (slow) reference
      String fllTargetFrequencyMessage = null;
      double fllInputFrequency = 0;
      if (mcg_c1_irefsNode.getValueAsLong() == 0) {
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
      
      // Default values
      long mcg_c4_drs = 0;
      String mcg_c4_dmx32NodeMessage = null;

      if (validFllInputClock) {
         // Check if using narrowed bandwidth
         if ((mcg_c4_dmx32Node.safeGetValue()) &&
               ((fllInputFrequency < FLL_CLOCK_NARROW_MIN) || (fllInputFrequency > FLL_CLOCK_NARROW_MAX))) {
            // Internal reference selected with narrow FLL bandwidth
            mcg_c4_dmx32NodeMessage = String.format("FLL reference clock must be 32.768 kHz when (MCG_C4_DMX32 = 1) (currently = %3.3f kHz)", 
                  ((double)fllInputFrequency)/1000.0);
            validFllInputClock = false;
         }
      }
      
      if (!validFllInputClock) {
         fllTargetFrequencyMessage = "FLL not usable with input clock frequency";
         fllTargetFrequencyNode.setEnabled(false);

         // Check if trying to use FLL 
         if (primaryClockMode == ClockModes.FEEClock) {
            // Done inside an IF as another validator may set this error message
            setValid(viewer, primaryClockModeNode, "FLL input reference is out of acceptable ranges. FLL may not be used");
         }
      }
      else {
//         System.err.println("FllClockValidate.validate() fllInputFrequency = " + fllInputFrequency);
         fllTargetFrequencyNode.setEnabled(true);

         //=================
         // Determine possible output frequencies & check against desired value
         //
         long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();

         long fllOutFrequency;
         if (mcg_c4_dmx32Node.safeGetValue()) {
            fllOutFrequency = Math.round(fllInputFrequency * 608.0);
         }
         else {
            fllOutFrequency = Math.round(fllInputFrequency * 512.0);
         }
         mcg_c4_drs = -1;
         long probe = 0;
         ArrayList<Long> fllFrequencies = new ArrayList<Long>(); 
         for (probe=1; probe<=3; probe++) {
            fllFrequencies.add(fllOutFrequency*probe);
            if ((fllOutFrequency*probe) == fllTargetFrequency) {
               mcg_c4_drs = probe-1;
            }         
         }
         if (mcg_c4_drs < 0) {
            mcg_c4_drs = 0;
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
//         System.err.println("FllClockValidate.validate() fllOutFrequency = " + fllOutFrequency);
      }
      if (mcg_c3_plls == 0) {
         // FLL Selected
         update(viewer, mcg_c1_rdivNode,  mcg_c1_rdiv);
         setValid(viewer, mcg_c1_rdivNode,  mcg_c1_rdivMessage);
      }
      update(viewer, mcg_c2_rangeNode, mcg_c2_range);
      update(viewer, mcg_c3_div32Node, mcg_c3_div32);
      update(viewer, mcg_c4_drsNode,   mcg_c4_drs);

      setValid(viewer, mcg_c2_erefsNode, mcg_c2_erefs_message);
      setValid(viewer, mcg_c4_dmx32Node, mcg_c4_dmx32NodeMessage);
      setValid(viewer, fllTargetFrequencyNode, fllTargetFrequencyMessage);
   }

}
