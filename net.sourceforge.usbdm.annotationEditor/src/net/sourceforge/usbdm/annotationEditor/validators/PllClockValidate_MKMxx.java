package net.sourceforge.usbdm.annotationEditor.validators;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class PllClockValidate_MKMxx extends MyValidator {

   private static final long PLL_CLOCK_WIDE_MIN = 31250L;
   private static final long PLL_CLOCK_WIDE_MAX = 39063L;
   private static final long PLL_FACTOR         = 375L;

   public PllClockValidate_MKMxx() {
   }
   
   @Override
   public void validate(final TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode osc32kclk_clockNode       =  getNumericModelNode("osc32kclk_clock");
      NumericOptionModelNode system_slow_irc_clockNode =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode system_erc_clockNode      =  getNumericModelNode("system_erc_clock");
      NumericOptionModelNode pll32krefselNode          =  getNumericModelNode("mcg_c7_pll32krefsel");
      NumericOptionModelNode pllTargetFrequencyNode    =  getNumericModelNode("pllTargetFrequency");
      
      // Main clock used by PLL
      long pll_input_clock = 0;
      
      switch ((int)pll32krefselNode.getValueAsLong()) {
      case 0 :
         pll_input_clock = osc32kclk_clockNode.getValueAsLong();
         break;
      case 1 :
         pll_input_clock = system_slow_irc_clockNode.getValueAsLong();
         break;
      case 2 :
         pll_input_clock = system_erc_clockNode.getValueAsLong();
         break;
      default :
         break;
      }

      boolean valid = (PLL_CLOCK_WIDE_MIN<=pll_input_clock) && (pll_input_clock<=PLL_CLOCK_WIDE_MAX);
      
      long   pll_output_clock          = 0;
      String pllTargetFrequencyMessage = null;
      if (valid) {
         // Valid - update
         System.err.println(String.format("PllClockValidate.validate() Valid"));
         pll_output_clock = pll_input_clock * PLL_FACTOR;
         System.err.println(String.format("\nPllClockValidate.validate(): Valid, pll_input_clock = %d, pllTargetFrequency = %d", 
               pll_input_clock, pll_output_clock));
      }
      else {
         // Not valid input frequency
         pllTargetFrequencyMessage = String.format("Invalid input frequency for PLL (%d)", pll_input_clock);
         System.err.println(String.format("PllClockValidate.validate(): Invalid, pll_input_clock = %d, pllTargetFrequency = %d", 
               pll_input_clock, pll_output_clock));
      }
      setValid(viewer, pllTargetFrequencyNode, pllTargetFrequencyMessage);
      update(viewer, pllTargetFrequencyNode, pll_output_clock);
   }

}