package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
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
      
      LongVariable pllTargetFrequencyNode = getLongVariable("pllTargetFrequency");
      pllTargetFrequencyNode.setMin(pllOutMin);
      pllTargetFrequencyNode.setMax(pllOutMax);
      
      LongVariable mcg_c5_prdiv0Node      = getLongVariable("mcg_c5_prdiv0");
      mcg_c5_prdiv0Node.setMin(prDivMin);
      mcg_c5_prdiv0Node.setMax(prDivMax);
      
      LongVariable mcg_c6_vdiv0Node       = getLongVariable("mcg_c6_vdiv0");
      mcg_c6_vdiv0Node.setMin(vDivMin);
      mcg_c6_vdiv0Node.setMax(vDivMax);
   }
   
   @Override
   protected void validate() {
      Variable system_erc_clockNode   = getVariable("mcg_erc_clock");
      Variable pllTargetFrequencyNode = getVariable("pllTargetFrequency");
      Variable mcg_c5_prdiv0Node      = getVariable("mcg_c5_prdiv0");
      Variable mcg_c6_vdiv0Node       = getVariable("mcg_c6_vdiv0");

      // Main clock used by FLL
      long mcg_erc_clock = system_erc_clockNode.getValueAsLong();

      long pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();

//      System.err.println(String.format("\nPllClockValidater.validate(): mcg_erc_clock = %d, pllTargetFrequency = %d", mcg_erc_clock, pllTargetFrequency));

      int  mcg_prdiv = 0;
      int  mcg_vdiv  = 0;

      boolean valid = false;
      
      Set<Long> pllFrequencies = new TreeSet<Long>(); 

      StringBuilder sb = new StringBuilder();
      long nearestPllOutFrequency = Long.MAX_VALUE;
      
      // Try each prescale value
      for (int mcg_prdiv_probe = PRDIV_MIN; mcg_prdiv_probe <= PRDIV_MAX; mcg_prdiv_probe++) {
         if (sb.length()>0) {
//            System.err.println(sb.toString());
            sb = new StringBuilder();
         }
         double pllInFrequency = mcg_erc_clock/mcg_prdiv_probe;
         sb.append(String.format("(prdiv = %d, pllIn=%f) => ", mcg_prdiv_probe, pllInFrequency));
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
         for (int mcg_vdiv_probe=VDIV_MIN; mcg_vdiv_probe<=VDIV_MAX; mcg_vdiv_probe++) {
            long pllOutFrequency = Math.round((mcg_vdiv_probe*pllInFrequency)/PLL_POST_DIV);
            sb.append(pllOutFrequency);
            if (pllOutFrequency<PLL_OUT_MINIMUM_FREQUENCY) {
               sb.append("<, ");
               continue;
            }
            if (pllOutFrequency>PLL_OUT_MAXIMUM_FREQUENCY) {
               sb.append(">, ");
               break;
            }
            sb.append("*,");
            pllFrequencies.add(pllOutFrequency);
            
            // Accept value within ~2.5% of desired
            if (Math.abs(pllOutFrequency - pllTargetFrequency) < (pllTargetFrequency/40)) {
               sb.append("=");
               if (Math.abs(pllOutFrequency-pllTargetFrequency)<Math.abs(nearestPllOutFrequency-pllTargetFrequency))  {
                  nearestPllOutFrequency = pllOutFrequency;
                  mcg_prdiv = mcg_prdiv_probe;
                  mcg_vdiv  = mcg_vdiv_probe;
               }
               valid = true;
            }         
         }
         if (sb.length()>0) {
            sb = new StringBuilder();
         }
      }
      if (valid) {
         // Valid - update
         mcg_c5_prdiv0Node.setValue(mcg_prdiv);
         mcg_c6_vdiv0Node.setValue(mcg_vdiv);
         if (pllTargetFrequency != nearestPllOutFrequency) {
            pllTargetFrequency = nearestPllOutFrequency;
            pllTargetFrequencyNode.setValue(pllTargetFrequency);
         }
//         System.err.println(String.format("PllClockValidater.validater(): Valid - prdiv=%d, vdiv=%d", mcg_prdiv, mcg_vdiv));
      }
      sb = new StringBuilder();
      Message.Severity severity = Severity.OK;
      if (!valid) {
         if (pllFrequencies.isEmpty()) {
            // No possible output frequencies indicates that the input frequency was not suitable
            sb.append(String.format("PLL not usable with input clock frequency %d Hz\n", mcg_erc_clock));
         }
         else {
            sb.append("Not possible to generate desired PLL frequency from input clock\n");
         }
//         System.err.println("PllClockValidater.validater(): "+sb.toString());
      }
      sb.append("Possible values (Hz) = \n");
      boolean needComma = false;
      int lineCount = -1;
      for (Long freq : pllFrequencies) {
         if (needComma) {
            sb.append(", ");
         }
         if (lineCount++>=10) {
            sb.append("\n");
            lineCount = 0;
         }
         needComma = true;
         sb.append(EngineeringNotation.convert(freq, 5)+"Hz");
      }
      Message pllTargetFrequencyMessage = new Message(sb.toString(), severity);
//      System.err.println(String.format("PllClockValidater.validater(): "+pllTargetFrequencyMessage.getMessage()));

      pllTargetFrequencyNode.setMessage(pllTargetFrequencyMessage);
   }
}
