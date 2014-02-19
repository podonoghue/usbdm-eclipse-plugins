package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class PllClockValidate extends MyValidator {

   static final long PLL_IN_MINIMUM_FREQUENCY = 2000000;
   static final long PLL_IN_MAXIMUM_FREQUENCY = 4000000;
   
   static final long PLL_OUT_MINIMUM_FREQUENCY = 48000000;
   static final long PLL_OUT_MAXIMUM_FREQUENCY = 100000000;
   
   static final int  PRDIV_MIN = 1;
   static final int  PRDIV_MAX = 24;
   
   static final int  VDIV_MIN = 24;
   static final int  VDIV_MAX = 55;
   
   @Override
   public void validate(final TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode pllTargetFrequencyNode =  getNumericModelNode("pllTargetFrequency");
      NumericOptionModelNode system_erc_clockNode   =  safeGetNumericModelNode("system_erc_clock");
      if (system_erc_clockNode == null) {
         // Default to oscillator clock
         system_erc_clockNode                       =  getNumericModelNode("oscclk_clock");
      }
      NumericOptionModelNode mcg_c5_prdivNode       =  getNumericModelNode("mcg_c5_prdiv");
      NumericOptionModelNode mcg_c6_vdivNode        =  getNumericModelNode("mcg_c6_vdiv");

      // Main clock used by FLL
      long system_erc_clock = system_erc_clockNode.getValueAsLong();

      long pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();

      mcg_c5_prdivNode.setModifiable(false);
      mcg_c6_vdivNode.setModifiable(false);
      
      System.err.println(String.format("\nPllClockValidate.validate(): system_erc_clock = %d, pllTargetFrequency = %d", system_erc_clock, pllTargetFrequency));

      int  mcg_c5_prdiv = 0;
      int  mcg_c6_vdiv  = 0;

      boolean valid = false;
      
      Set<Long> pllFrequencies = new TreeSet<Long>(); 

      // Try each prescale value
      for (mcg_c5_prdiv = PRDIV_MIN; mcg_c5_prdiv <= PRDIV_MAX; mcg_c5_prdiv++) {
         double pllInFrequency = system_erc_clock/mcg_c5_prdiv;
         if (pllInFrequency>PLL_IN_MAXIMUM_FREQUENCY) {
            // Invalid as input to PLL
            continue;
         }
         if (pllInFrequency<(PLL_IN_MINIMUM_FREQUENCY)) {
            // Invalid as input to PLL
            break;
         }
         // Try each multiplier value
         for (mcg_c6_vdiv=VDIV_MIN; mcg_c6_vdiv<=VDIV_MAX; mcg_c6_vdiv++) {
            long pllOutFrequency = (system_erc_clock*mcg_c6_vdiv)/mcg_c5_prdiv;
            if (pllOutFrequency<PLL_OUT_MINIMUM_FREQUENCY) {
               continue;
            }
            if (pllOutFrequency>PLL_OUT_MAXIMUM_FREQUENCY) {
               continue;
            }
            pllFrequencies.add(pllOutFrequency);
//            System.err.println(String.format("PllClockValidate.validate(): Trying prdiv=%d, vdiv=%d, pllIn=%d, pllOut=%d", prdiv, vdiv, pllInFrequency, pllOutFrequency));
            if (pllOutFrequency == pllTargetFrequency) {
               valid = (pllOutFrequency*mcg_c5_prdiv) == (system_erc_clock*mcg_c6_vdiv);
            }
            if (valid) {
               break;
            }
         }
         if (valid) {
            break;
         }
      }
      if (valid) {
         // Valid - update
         update(viewer, mcg_c5_prdivNode, new Long(mcg_c5_prdiv));
         update(viewer, mcg_c6_vdivNode, new Long(mcg_c6_vdiv));
         System.err.println(String.format("PllClockValidate.validate() Valid: prdiv=%d, vdiv=%d", mcg_c5_prdiv, mcg_c6_vdiv));
      }
      String pllTargetFrequencyMessage = null;
      if (!valid) {
         if (pllFrequencies.isEmpty()) {
            // Not OK to allows changing value
            pllTargetFrequencyNode.setEnabled(false);
            // No possible output frequencies indicates that the input frequency was not suitable
            pllTargetFrequencyMessage = "PLL not usable with input clock frequency";
         }
         else {
            // OK to allows changing value
            pllTargetFrequencyNode.setEnabled(true);
            StringBuilder buff = new StringBuilder("Not possible to generate PLL frequency from input clock. Possible values = \n");
            boolean needComma = false;
            int lineCount = -1;
            for (Long freq : pllFrequencies) {
               if (needComma) {
                  buff.append(", ");
               }
               if (lineCount++>=10) {
                  buff.append("\n");
                  lineCount = 0;
               }
               needComma = true;
               buff.append(String.format("%d", freq));
            }
            pllTargetFrequencyMessage = buff.toString();
         }
      }
      else {
         // OK to allows changing value
         pllTargetFrequencyNode.setEnabled(true);
      }
      setValid(viewer, pllTargetFrequencyNode, pllTargetFrequencyMessage);
   }

}