package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.annotationEditor.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;
import net.sourceforge.usbdm.annotationEditor.NumericOptionModelNode;

public class Pll1ClockValidate_MKxxM12 extends MyValidator {

   static final long PLL_IN_MINIMUM_FREQUENCY = 8000000;
   static final long PLL_IN_MAXIMUM_FREQUENCY = 16000000;
   
   static final long PLL_OUT_MINIMUM_FREQUENCY = 90000000;
   static final long PLL_OUT_MAXIMUM_FREQUENCY = 180000000;
   
   static final int  PRDIV_MIN = 1;
   static final int  PRDIV_MAX = 8;
   
   static final int  VDIV_MIN = 16;
   static final int  VDIV_MAX = 47;
   
   @Override
   public void validate(final TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode oscclk0_clockNode      =  getNumericModelNode("oscclk0_clock");
      NumericOptionModelNode oscclk1_clockNode      =  getNumericModelNode("oscclk1_clock");
      NumericOptionModelNode pllTargetFrequencyNode =  getNumericModelNode("pll1TargetFrequency");
      BinaryOptionModelNode  mcg_pllrefselNode      =  getBinaryModelNode("mcg_c11_pllrefsel1");
      NumericOptionModelNode mcg_prdivNode          =  getNumericModelNode("mcg_c11_prdiv1");
      NumericOptionModelNode mcg_vdivNode           =  getNumericModelNode("mcg_c12_vdiv1");

      // Main clock used by FLL
      long system_erc_clock;
      if (mcg_pllrefselNode.safeGetValue()) {
         system_erc_clock = oscclk1_clockNode.getValueAsLong();
      }
      else {
         system_erc_clock = oscclk0_clockNode.getValueAsLong();
      }
      long pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();

//      System.err.println(String.format("\nPllClockValidate.validate(): system_erc_clock = %d, pllTargetFrequency = %d", system_erc_clock, pllTargetFrequency));

      int  mcg_prdiv = 0;
      int  mcg_vdiv  = 0;

      boolean valid = false;
      
      Set<Long> pllFrequencies = new TreeSet<Long>(); 

      // Try each prescale value
      for (mcg_prdiv = PRDIV_MIN; mcg_prdiv <= PRDIV_MAX; mcg_prdiv++) {
         double pllInFrequency = system_erc_clock/mcg_prdiv;
         if (pllInFrequency>PLL_IN_MAXIMUM_FREQUENCY) {
            // Invalid as input to PLL
            continue;
         }
         if (pllInFrequency<(PLL_IN_MINIMUM_FREQUENCY)) {
            // Invalid as input to PLL
            break;
         }
         // Try each multiplier value
         for (mcg_vdiv=VDIV_MIN; mcg_vdiv<=VDIV_MAX; mcg_vdiv++) {
            long pllOutFrequency = Math.round(mcg_vdiv*(pllInFrequency/2.0));
            if (pllOutFrequency<PLL_OUT_MINIMUM_FREQUENCY) {
               continue;
            }
            if (pllOutFrequency>PLL_OUT_MAXIMUM_FREQUENCY) {
               continue;
            }
            pllFrequencies.add(pllOutFrequency);
//            System.err.println(String.format("PllClockValidate.validate(): Trying prdiv=%d, vdiv=%d, pllIn=%d, pllOut=%d", prdiv, vdiv, pllInFrequency, pllOutFrequency));
            valid =  pllOutFrequency == pllTargetFrequency;
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
         update(viewer, mcg_prdivNode, new Long(mcg_prdiv));
         update(viewer, mcg_vdivNode, new Long(mcg_vdiv));
//         System.err.println(String.format("PllClockValidate.validate() Valid: prdiv=%d, vdiv=%d", mcg_prdiv, mcg_vdiv));
      }
      String pllTargetFrequencyMessage = null;
      if (!valid) {
         if (pllFrequencies.isEmpty()) {
            // Not OK to allow changing value
            pllTargetFrequencyNode.setEnabled(false);
            // No possible output frequencies indicates that the input frequency was not suitable
            pllTargetFrequencyMessage = String.format("PLL not usable with input clock frequency %d Hz", system_erc_clock);
         }
         else {
            // OK to allows changing value
            pllTargetFrequencyNode.setEnabled(true);
            StringBuilder buff = new StringBuilder("Not possible to generate desired PLL frequency from input clock. Possible values (Hz) = \n");
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