package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class PllClockValidater extends BaseClockValidator {
   private final long PLL_IN_MINIMUM_FREQUENCY;
   private final long PLL_IN_MAXIMUM_FREQUENCY;
   
   private final long PLL_OUT_MINIMUM_FREQUENCY;
   private final long PLL_OUT_MAXIMUM_FREQUENCY;
   
   private final int  PRDIV_MIN;
   private final int  PRDIV_MAX;
   
   private final int  VDIV_MIN;
   private final int  VDIV_MAX;
   
   private final int  PLL_POST_DIV;
   
   /**
    * Validates PLL related variables
    * VDIV & PRDIV
    * 
    * @param peripheral
    * @param pllOutMin
    * @param pllOutMax
    * @param pllInMin
    * @param pllInMax
    * @param prDivMin
    * @param prDivMax
    * @param vDivMin
    * @param vDivMax
    * @param pllPostDiv
    */
   public PllClockValidater(PeripheralWithState peripheral, long pllOutMin, long pllOutMax, long pllInMin, long pllInMax, long prDivMin, long prDivMax, long vDivMin, long vDivMax, long pllPostDiv) {
      super(peripheral);
      PLL_OUT_MINIMUM_FREQUENCY = pllOutMin;
      PLL_OUT_MAXIMUM_FREQUENCY = pllOutMax;
      PLL_IN_MINIMUM_FREQUENCY  = pllInMin;
      PLL_IN_MAXIMUM_FREQUENCY  = pllInMax;
      PRDIV_MIN                 = (int) prDivMin;
      PRDIV_MAX                 = (int) prDivMax;
      VDIV_MIN                  = (int) vDivMin;
      VDIV_MAX                  = (int) vDivMax;
      PLL_POST_DIV              = (int) pllPostDiv;
      Variable pllTargetFrequencyNode = fPeripheral.getVariable("pllTargetFrequency");
      pllTargetFrequencyNode.setMin(pllOutMin);
      pllTargetFrequencyNode.setMax(pllOutMax);
   }
   
   @Override
   protected void validate() {
      Variable system_erc_clockNode   = fPeripheral.getVariable("system_erc_clock");
      Variable pllTargetFrequencyNode = fPeripheral.getVariable("pllTargetFrequency");
      Variable mcg_c5_prdiv0Node      = fPeripheral.getVariable("mcg_c5_prdiv0");
      Variable mcg_c6_vdiv0Node       = fPeripheral.getVariable("mcg_c6_vdiv0");

      if (system_erc_clockNode == null) {
         // Default to oscillator clock
         system_erc_clockNode =  fPeripheral.getVariable("oscclk_clock");
      }

      // Main clock used by FLL
      long system_erc_clock = system_erc_clockNode.getValueAsLong();

      long pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();

      System.err.println(String.format("\nPllClockValidater.validater(): system_erc_clock = %d, pllTargetFrequency = %d", system_erc_clock, pllTargetFrequency));

      int  mcg_prdiv = 0;
      int  mcg_vdiv  = 0;

      boolean valid = false;
      
      Set<Long> pllFrequencies = new TreeSet<Long>(); 

      StringBuffer sb = new StringBuffer();
      
      // Try each prescale value
      for (mcg_prdiv = PRDIV_MIN; mcg_prdiv <= PRDIV_MAX; mcg_prdiv++) {
         if (sb.length()>0) {
//            System.err.println(sb.toString());
            sb = new StringBuffer();
         }
         double pllInFrequency = system_erc_clock/mcg_prdiv;
         sb.append(String.format("(prdiv = %d, pllIn=%f) => ", mcg_prdiv, pllInFrequency));
         if (pllInFrequency>PLL_IN_MAXIMUM_FREQUENCY) {
            // Invalid as input to PLL
            sb.append("too high");
            continue;
         }
         if (pllInFrequency<PLL_IN_MINIMUM_FREQUENCY) {
            // Invalid as input to PLL
            sb.append("too low");
            break;
         }
         // Try each multiplier value
         for (mcg_vdiv=VDIV_MIN; mcg_vdiv<=VDIV_MAX; mcg_vdiv++) {
            long pllOutFrequency = Math.round((mcg_vdiv*pllInFrequency)/PLL_POST_DIV);
            sb.append(pllOutFrequency);
            if (pllOutFrequency<PLL_OUT_MINIMUM_FREQUENCY) {
               System.err.print("-, ");
               continue;
            }
            if (pllOutFrequency>PLL_OUT_MAXIMUM_FREQUENCY) {
               System.err.print("+, ");
               break;
            }
            sb.append("*,");
            pllFrequencies.add(pllOutFrequency);
//            System.err.println(String.format("PllClockValidate.validate(): Trying prdiv=%d, vdiv=%d, pllIn=%d, pllOut=%d", prdiv, vdiv, pllInFrequency, pllOutFrequency));
            valid =  pllOutFrequency == pllTargetFrequency;
            if (valid) {
               sb.append("=");
               break;
            }
         }
         if (valid) {
            break;
         }
         if (sb.length()>0) {
//            System.err.println(sb.toString());
            sb = new StringBuffer();
         }
      }
      if (valid) {
         // Valid - update
         mcg_c5_prdiv0Node.setValue(mcg_prdiv-PRDIV_MIN);
         mcg_c6_vdiv0Node.setValue(mcg_vdiv-VDIV_MIN);
         System.err.println(String.format("PllClockValidater.validater(): Valid - prdiv=%d, vdiv=%d", mcg_prdiv, mcg_vdiv));
      }
      Message pllTargetFrequencyMessage = null;
      if (!valid) {
         if (pllFrequencies.isEmpty()) {
            // No possible output frequencies indicates that the input frequency was not suitable
            pllTargetFrequencyMessage = new Message(String.format("PLL not usable with input clock frequency %d Hz", system_erc_clock), Severity.WARNING);
            System.err.println(String.format("PllClockValidater.validater(): "+pllTargetFrequencyMessage.getMessage()));
         }
         else {
            // OK to allows changing value
//            pllTargetFrequencyNode.setEnabled(true);
            StringBuilder buff = new StringBuilder("Not possible to generate desired PLL frequency from input clock.\n Possible values (Hz) = \n");
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
            pllTargetFrequencyMessage = new Message(buff.toString(), Severity.WARNING);
            System.err.println(String.format("PllClockValidater.validater(): "+pllTargetFrequencyMessage.getMessage()));
         }
      }
      else {
         // OK to allows changing value
//         pllTargetFrequencyNode.setEnabled(true);
      }
      pllTargetFrequencyNode.setMessage(pllTargetFrequencyMessage);
   }
}
