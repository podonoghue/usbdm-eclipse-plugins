package net.sourceforge.usbdm.annotationEditor.validators;

import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class PllClockValidate extends MyValidator {

   private final long PLL_IN_MINIMUM_FREQUENCY;
   private final long PLL_IN_MAXIMUM_FREQUENCY;
   
   private final long PLL_OUT_MINIMUM_FREQUENCY;
   private final long PLL_OUT_MAXIMUM_FREQUENCY;
   
   private final int  PRDIV_MIN;
   private final int  PRDIV_MAX;
   
   private final int  VDIV_MIN;
   private final int  VDIV_MAX;
   
   private final int  PLL_POST_DIV;
   
   public PllClockValidate(long pllOutMin, long pllOutMax, long pllInMin, long pllInMax, long prDivMin, long prDivMax, long vDivMin, long vDivMax, long pllPostDiv) {
      PLL_OUT_MINIMUM_FREQUENCY = pllOutMin;
      PLL_OUT_MAXIMUM_FREQUENCY = pllOutMax;
      PLL_IN_MINIMUM_FREQUENCY  = pllInMin;
      PLL_IN_MAXIMUM_FREQUENCY  = pllInMax;
      PRDIV_MIN                 = (int) prDivMin;
      PRDIV_MAX                 = (int) prDivMax;
      VDIV_MIN                  = (int) vDivMin;
      VDIV_MAX                  = (int) vDivMax;
      PLL_POST_DIV              = (int) pllPostDiv;
   }
   
   public PllClockValidate(long pllOutMin, long pllOutMax) {
      this (pllOutMin, pllOutMax, 2000000, 4000000, 1, 25, 24, 55, 1);
   }
   
   @Deprecated
   public PllClockValidate() {
      this(48000000, 100000000);
   }
   
   @Override
   public void validate(final TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode system_erc_clockNode   =  safeGetNumericModelNode("system_erc_clock");
      NumericOptionModelNode pllTargetFrequencyNode =  getNumericModelNode("pllTargetFrequency");
      NumericOptionModelNode mcg_prdivNode          =  getNumericModelNode("mcg_c5_prdiv0");
      NumericOptionModelNode mcg_vdivNode           =  getNumericModelNode("mcg_c6_vdiv0");
      if (system_erc_clockNode == null) {
         // Default to oscillator clock
         system_erc_clockNode                       =  getNumericModelNode("oscclk_clock");
      }

      // Main clock used by FLL
      long system_erc_clock = system_erc_clockNode.getValueAsLong();

      long pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();

//      System.err.println(String.format("\nPllClockValidate.validate(): system_erc_clock = %d, pllTargetFrequency = %d", system_erc_clock, pllTargetFrequency));

      int  mcg_prdiv = 0;
      int  mcg_vdiv  = 0;

      boolean valid = false;
      
      Set<Long> pllFrequencies = new TreeSet<Long>(); 

      // Try each prescale value
      for (mcg_prdiv = PRDIV_MIN; mcg_prdiv <= PRDIV_MAX; mcg_prdiv++) {
         double pllInFrequency = system_erc_clock/mcg_prdiv;
         System.err.print(String.format("(prdiv = %d, pllIn=%f) => ", mcg_prdiv, pllInFrequency));
         if (pllInFrequency>PLL_IN_MAXIMUM_FREQUENCY) {
            // Invalid as input to PLL
            System.err.println("too high");
            continue;
         }
         if (pllInFrequency<PLL_IN_MINIMUM_FREQUENCY) {
            // Invalid as input to PLL
            System.err.println("too low");
            break;
         }
         // Try each multiplier value
         for (mcg_vdiv=VDIV_MIN; mcg_vdiv<=VDIV_MAX; mcg_vdiv++) {
            long pllOutFrequency = Math.round((mcg_vdiv*pllInFrequency)/PLL_POST_DIV);
            System.err.print(pllOutFrequency);
            if (pllOutFrequency<PLL_OUT_MINIMUM_FREQUENCY) {
               System.err.print("-, ");
               continue;
            }
            if (pllOutFrequency>PLL_OUT_MAXIMUM_FREQUENCY) {
               System.err.print("+, ");
               break;
            }
            System.err.print("*,");
            pllFrequencies.add(pllOutFrequency);
//            System.err.println(String.format("PllClockValidate.validate(): Trying prdiv=%d, vdiv=%d, pllIn=%d, pllOut=%d", prdiv, vdiv, pllInFrequency, pllOutFrequency));
            valid =  pllOutFrequency == pllTargetFrequency;
            if (valid) {
               System.err.print("=");
               break;
            }
         }
         System.err.println();
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