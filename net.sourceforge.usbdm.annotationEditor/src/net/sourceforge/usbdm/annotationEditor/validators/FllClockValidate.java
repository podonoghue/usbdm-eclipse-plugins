package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;
import net.sourceforge.usbdm.annotationEditor.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.Message;
import net.sourceforge.usbdm.annotationEditor.MyValidator;
import net.sourceforge.usbdm.annotationEditor.NumericOptionModelNode;

public class FllClockValidate extends MyValidator {

   private static final long FLL_CLOCK_RANGE1_MIN = 32000L;
   private static final long FLL_CLOCK_RANGE1_MAX = 40000L;
   
   private static final long FLL_CLOCK_RANGE2_MIN = 3000000L;
   private static final long FLL_CLOCK_RANGE2_MAX = 8000000L;
   
   private static final long FLL_CLOCK_RANGE3_MIN = 8000000L;
   private static final long FLL_CLOCK_RANGE3_MAX = 32000000L;
   
   private static final long FLL_CLOCK_NARROW_MIN = 32768L-100;
   private static final long FLL_CLOCK_NARROW_MAX = 32768L+100;
   
   private static final long FLL_CLOCK_WIDE_MIN = 31250L;
   private static final long FLL_CLOCK_WIDE_MAX = 39063L;
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode           =  getNumericModelNode("clock_mode");
      NumericOptionModelNode fllTargetFrequencyNode         =  getNumericModelNode("fllTargetFrequency");
      NumericOptionModelNode system_erc_clockNode           =  safeGetNumericModelNode("system_erc_clock");
      if (system_erc_clockNode == null) {
         // Try oscillator 0 clock
         system_erc_clockNode                               =  getNumericModelNode("oscclk0_clock");
      }
      if (system_erc_clockNode == null) {
         // Try oscillator clock
         system_erc_clockNode                               =  getNumericModelNode("oscclk_clock");
      }
      NumericOptionModelNode internalReferenceClockNode     =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode mcg_c1_frdivNode               =  getNumericModelNode("mcg_c1_frdiv");
      NumericOptionModelNode mcg_c1_irefsNode               =  getNumericModelNode("mcg_c1_irefs");
      NumericOptionModelNode mcg_c2_rangeNode               =  getNumericModelNode("mcg_c2_range0");
      BinaryOptionModelNode  mcg_c2_erefsNode               =  getBinaryModelNode("mcg_c2_erefs0");
      BinaryOptionModelNode  mcg_c4_dmx32Node               =  getBinaryModelNode("mcg_c4_dmx32");
      NumericOptionModelNode mcg_c4_drst_drsNode            =  getNumericModelNode("mcg_c4_drst_drs");

      // For reference on only!
      NumericOptionModelNode mcg_c7_oscselNode              =  safeGetNumericModelNode("mcg_c7_oscsel");

      // Name of External Reference Clock
      String system_erc_clock_name = "External clock or crystal";
      if (mcg_c7_oscselNode != null) {
         switch ((int)mcg_c7_oscselNode.getValueAsLong()) {
         case 0: // ERC = OSCCLK
            break;
         case 1: // ERC = OSC32KCLK
            system_erc_clock_name = "RTC External clock or crystal";
            break;
         case 2: // ERC = IRC48M
            system_erc_clock_name = "Internal 48MHz clock";
            break;
         default:
            throw new Exception("Illegal Clock source (mcg_c7_oscsel)");
         }
      }

      // Main clock used by FLL
      long system_erc_clock = system_erc_clockNode.getValueAsLong();

      //=========================================
      // Determine mcg_c2_range
      //    - Clock range of oscillator
      //    - Affects FLL prescale
      //
      long    mcg_c2_range         = -1;
      Message mcg_c2_erefs_message = null;

      if ((system_erc_clock >= FLL_CLOCK_RANGE1_MIN) && (system_erc_clock <= FLL_CLOCK_RANGE1_MAX)) {
         mcg_c2_range = 0;
      }
      else if ((system_erc_clock >= FLL_CLOCK_RANGE2_MIN) && (system_erc_clock <= FLL_CLOCK_RANGE2_MAX)) {
         mcg_c2_range = 1;
      }
      else if ((system_erc_clock >= FLL_CLOCK_RANGE3_MIN) && (system_erc_clock <= FLL_CLOCK_RANGE3_MAX)) {
         mcg_c2_range = 2;
      }
      if (mcg_c2_range < 0) {
         String msgText;
         // Not suitable frequency for FLL
         msgText = String.format("Frequency of %s (%d) is not suitable for use as FLL input\n", 
               system_erc_clock_name, system_erc_clock);
         msgText += String.format("Permitted ranges [%d-%d], [%d-%d] or [%d-%d]", 
               FLL_CLOCK_RANGE1_MIN, FLL_CLOCK_RANGE1_MAX, FLL_CLOCK_RANGE2_MIN, FLL_CLOCK_RANGE2_MAX, FLL_CLOCK_RANGE3_MIN, FLL_CLOCK_RANGE3_MAX);
         mcg_c2_erefs_message = new Message(msgText, mcg_c2_erefsNode.safeGetValue()?Severity.ERROR:Severity.WARNING);
         // Set compromise value
         if (system_erc_clock <= FLL_CLOCK_RANGE2_MIN) {
            mcg_c2_range = 0;
         }
         else if (system_erc_clock <= FLL_CLOCK_RANGE2_MAX) {
            mcg_c2_range = 1;
         }
         else {
            mcg_c2_range = 2;
         }
      }

      //=================
      // Determine External FLL reference after dividers
      //
      double externalfllInputFrequencyAfterDivider = 0.0;
      if (mcg_c2_range == 0) {
         externalfllInputFrequencyAfterDivider = system_erc_clock;
      }
      else {
         externalfllInputFrequencyAfterDivider = system_erc_clock / (1<<5);
      }
//      System.err.println("FllClockValidate.validate() externalfllInputFrequencyAfterDivider (after predivider) = " + externalfllInputFrequencyAfterDivider);

      String mcg_c1_frdivMessage  = null;
      int    mcg_c1_frdiv         = 0;

      // Assume no errors
      boolean validFllInputClock  = true;
      
      if      (((externalfllInputFrequencyAfterDivider/1)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/1)<=FLL_CLOCK_WIDE_MAX)) {
         mcg_c1_frdiv =  0; externalfllInputFrequencyAfterDivider /= 1;
      }
      else if (((externalfllInputFrequencyAfterDivider/2)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/2)<=FLL_CLOCK_WIDE_MAX)) {
         mcg_c1_frdiv =  1; externalfllInputFrequencyAfterDivider /= 2;
      }
      else if (((externalfllInputFrequencyAfterDivider/4)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/4)<=FLL_CLOCK_WIDE_MAX)) {
         mcg_c1_frdiv =  2; externalfllInputFrequencyAfterDivider /= 4;
      }
      else if (((externalfllInputFrequencyAfterDivider/8)>=FLL_CLOCK_WIDE_MIN)   && ((externalfllInputFrequencyAfterDivider/8)<=FLL_CLOCK_WIDE_MAX)) {
         mcg_c1_frdiv =  3; externalfllInputFrequencyAfterDivider /= 8;
      }
      else if (((externalfllInputFrequencyAfterDivider/16)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/16)<=FLL_CLOCK_WIDE_MAX)) {
         mcg_c1_frdiv =  4; externalfllInputFrequencyAfterDivider /= 16;
      }
      else if (((externalfllInputFrequencyAfterDivider/32)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/32)<=FLL_CLOCK_WIDE_MAX)) {
         mcg_c1_frdiv =  5; externalfllInputFrequencyAfterDivider /= 32;
      }
      else if (((externalfllInputFrequencyAfterDivider/64)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/64)<=FLL_CLOCK_WIDE_MAX)  && (mcg_c2_range == 0)) {
         mcg_c1_frdiv =  6; externalfllInputFrequencyAfterDivider /= 64;
      }
      else if (((externalfllInputFrequencyAfterDivider/40)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/40)<=FLL_CLOCK_WIDE_MAX)  && (mcg_c2_range != 0)) {
         mcg_c1_frdiv =  6; externalfllInputFrequencyAfterDivider /= 40;
      }
      else if (((externalfllInputFrequencyAfterDivider/128)>=FLL_CLOCK_WIDE_MIN) && ((externalfllInputFrequencyAfterDivider/128)<=FLL_CLOCK_WIDE_MAX) && (mcg_c2_range == 0)) {
         mcg_c1_frdiv =  7; externalfllInputFrequencyAfterDivider /= 128;
      }
      else if (((externalfllInputFrequencyAfterDivider/48)>=FLL_CLOCK_WIDE_MIN)  && ((externalfllInputFrequencyAfterDivider/48)<=FLL_CLOCK_WIDE_MAX)  && (mcg_c2_range != 0)) {
         mcg_c1_frdiv =  7; externalfllInputFrequencyAfterDivider /= 48;
      }
      else {
         validFllInputClock = false;
         mcg_c1_frdivMessage = String.format("Unable to find suitable divider for external reference clock frequency = %d Hz", system_erc_clock);
         mcg_c1_frdiv =  7;
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
      long mcg_c4_drst_drs = 0;
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
         if (primaryClockModeNode.getValueAsLong() == ClockValidate_KLxx.ClockModes.FEEClock.ordinal()) {
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
            fllOutFrequency = Math.round(fllInputFrequency * 732.0);
         }
         else {
            fllOutFrequency = Math.round(fllInputFrequency * 640.0);
         }
         mcg_c4_drst_drs = -1;
         long probe = 0;
         ArrayList<Long> fllFrequencies = new ArrayList<Long>(); 
         for (probe=1; probe<=4; probe++) {
            fllFrequencies.add(fllOutFrequency*probe);
            if ((fllOutFrequency*probe) == fllTargetFrequency) {
               mcg_c4_drst_drs = probe-1;
            }         
         }
         if (mcg_c4_drst_drs < 0) {
            mcg_c4_drst_drs = 0;
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
      update(viewer, mcg_c1_frdivNode, mcg_c1_frdiv);
      update(viewer, mcg_c2_rangeNode, mcg_c2_range);
      update(viewer, mcg_c4_drst_drsNode, mcg_c4_drst_drs);
      setValid(viewer, mcg_c1_frdivNode, mcg_c1_frdivMessage);
      setValid(viewer, mcg_c2_erefsNode, mcg_c2_erefs_message);
      setValid(viewer, mcg_c4_dmx32Node, mcg_c4_dmx32NodeMessage);
      setValid(viewer, fllTargetFrequencyNode, fllTargetFrequencyMessage);
   }

}
