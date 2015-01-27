package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class PllClockValidate_CFV1 extends MyValidator {

   static final long PLL_IN_MINIMUM_FREQUENCY = 1000000;
   static final long PLL_IN_MAXIMUM_FREQUENCY = 2000000;
   
   static final long PLL_OUT_MINIMUM_FREQUENCY = 48000000;
   static final long PLL_OUT_MAXIMUM_FREQUENCY = 100000000;
   
   static final int  PRDIV_MIN = 1;
   static final int  PRDIV_MAX = 24;
   
   static final int  VDIV_MIN  = 4;
   static final int  VDIV_MAX  = 48;
   static final int  VDIV_STEP = 4;
   
   @Override
   public void validate(final TreeViewer viewer) throws Exception {
      super.validate(viewer);

      NumericOptionModelNode oscclk_clockNode       =  getNumericModelNode("oscclk_clock");
      NumericOptionModelNode pllTargetFrequencyNode =  getNumericModelNode("pllTargetFrequency");
      NumericOptionModelNode mcg_c1_rdivNode        =  getNumericModelNode("mcg_c1_rdiv");
      NumericOptionModelNode mcg_c3_vdivNode        =  getNumericModelNode("mcg_c3_vdiv");
      NumericOptionModelNode mcg_c3_pllsNode        =  getNumericModelNode("mcg_c3_plls");

      long mcg_c3_plls = mcg_c3_pllsNode.getValueAsLong();

      // Main clock used by FLL
      long oscclk_clock = oscclk_clockNode.getValueAsLong();

      long pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();

//      System.err.println(String.format("\nPllClockValidate.validate(): oscclk_clock = %d, pllTargetFrequency = %d", oscclk_clock, pllTargetFrequency));

      int  mcg_c1_rdiv = 0;
      int  mcg_c3_vdiv  = 0;

      boolean valid = false;
      
      Set<Long> pllFrequencies = new TreeSet<Long>(); 

      // Try each prescale value
      for (mcg_c1_rdiv = PRDIV_MIN; mcg_c1_rdiv <= PRDIV_MAX; mcg_c1_rdiv++) {
         double pllInFrequency = oscclk_clock/(1<<mcg_c1_rdiv);
         if (pllInFrequency>PLL_IN_MAXIMUM_FREQUENCY) {
            // Invalid as input to PLL
            continue;
         }
         if (pllInFrequency<(PLL_IN_MINIMUM_FREQUENCY)) {
            // Invalid as input to PLL
            break;
         }
         // Try each multiplier value
         for (mcg_c3_vdiv=VDIV_MIN; mcg_c3_vdiv<=VDIV_MAX; mcg_c3_vdiv+=VDIV_STEP) {
            long pllOutFrequency = Math.round(mcg_c3_vdiv*pllInFrequency);
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
         if (mcg_c3_plls == 1) {
            // PLL in use - mcg_c1_rdiv is shared by FLL & PLL 
            update(viewer, mcg_c1_rdivNode, new Long(mcg_c1_rdiv));
         }
         update(viewer, mcg_c3_vdivNode, new Long(mcg_c3_vdiv));
//         System.err.println(String.format("PllClockValidate.validate() Valid: rdiv=%d, vdiv=%d", mcg_c1_rdiv, mcg_c3_vdiv));
      }
      String pllTargetFrequencyMessage = null;
      if (!valid) {
         if (pllFrequencies.isEmpty()) {
            // Not OK to allow changing value
            pllTargetFrequencyNode.setEnabled(false);
            // No possible output frequencies indicates that the input frequency was not suitable
            pllTargetFrequencyMessage = String.format("PLL not usable with input clock frequency %d Hz", oscclk_clock);
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