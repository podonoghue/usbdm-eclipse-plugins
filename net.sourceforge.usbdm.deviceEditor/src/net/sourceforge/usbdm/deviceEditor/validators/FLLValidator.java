package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class FLLValidator extends BaseClockValidator {

   public FLLValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

   private static final long FLL_CLOCK_NARROW_MIN = 32768L-100;
   private static final long FLL_CLOCK_NARROW_MAX = 32768L+100;
   
   @Override
   protected void validate() {

      // Warning level to use (depend on whether FLL is enabled)
      boolean  fll_enabled                     = getVariable("fll_enabled").getValueAsBoolean();
      Severity warningLevel                    = fll_enabled?Severity.WARNING:Severity.OK;
      
      // Internal
      Variable internalReferenceClockNode     =  getVariable("system_slow_irc_clock");
      
      Variable system_erc_clockNode           =  getVariable("mcg_erc_clock");
      if (system_erc_clockNode == null) {
         // Try oscillator 0 clock
         system_erc_clockNode                 =  getVariable("oscclk0_clock");
      }
      if (system_erc_clockNode == null) {
         // Try oscillator clock
         system_erc_clockNode                 =  getVariable("oscclk_clock");
      }
      Variable mcg_c2_erefsNode               =  getVariable("mcg_c2_erefs0");
      
      // FLL
      Variable clock_modeNode                 =  getVariable("clock_mode");
      Variable mcg_c2_rangeNode               =  getVariable("mcg_c2_range0");
      Variable mcg_c1_frdivNode               =  getVariable("mcg_c1_frdiv");
      Variable system_mcgffclk_clockNode      =  getVariable("system_mcgffclk_clock");

      Variable mcg_c4_dmx32Node               =  getVariable("mcg_c4_dmx32");
      Variable mcg_c4_drst_drsNode            =  getVariable("mcg_c4_drst_drs");

      Variable fllTargetFrequencyNode         =  getVariable("fllTargetFrequency");

      Variable mcg_c1_irefsNode               =  getVariable("mcg_c1_irefs");

      Variable system_mcgout_clockNode        =  getVariable("system_mcgout_clock");
      
      Variable mcg_c7_oscselNode              =  safeGetVariable("mcg_c7_oscsel");

      ClockMode clockMode;
      try {
         clockMode = ClockMode.valueOf(clock_modeNode.getSubstitutionValue());
      } catch (Exception e) {
         System.err.println(e.getMessage());
         clockMode = ClockMode.ClockMode_None;
//         clock_modeNode.setValue(clockMode.toString());
      }

      boolean mcg_c1_irefs = false;
      switch(clockMode) {
      case ClockMode_None:
      case ClockMode_FEI:
      case ClockMode_FBI:
      case ClockMode_BLPI:
         mcg_c1_irefs = true;
         break;
      
      case ClockMode_BLPE:
      case ClockMode_FBE:
      case ClockMode_FEE:
      case ClockMode_PBE:
      case ClockMode_PEE:
      default:
         break;
      }
      
      boolean fllInUse = false;
      switch(clockMode) {
      case ClockMode_FEI:
      case ClockMode_FEE:
      fllInUse = true;
      break;

      case ClockMode_None:
      case ClockMode_FBI:
      case ClockMode_BLPI:
      case ClockMode_BLPE:
      case ClockMode_FBE:
      case ClockMode_PBE:
      case ClockMode_PEE:
      default:
         break;
      }
      mcg_c1_irefsNode.setValue(mcg_c1_irefs);

      // Main clock used by FLL
      long mcg_erc_clock = system_erc_clockNode.getValueAsLong();

      //=========================================
      // Determine mcg_c2_range
      //    - Clock range of oscillator
      //    - Affects FLL prescale
      //
      long    mcg_c2_range         = mcg_c2_rangeNode.getValueAsLong();
      Message mcg_c2_erefs_message = null;

      BaseClockValidator.FllDivider check = new FllDivider(
            mcg_erc_clock, 
            mcg_c2_erefsNode.getValueAsBoolean(), 
            mcg_c7_oscselNode.getValueAsLong(),
            system_erc_clockNode.getValueAsLong());
      mcg_c1_frdivNode.setMessage(check.mcg_c1_frdiv_clockMessage);
      mcg_c1_frdivNode.setValue(check.mcg_c1_frdiv);
      
//      System.err.println("FllClockValidate.validate() externalfllInputFrequencyAfterDivider = " + externalfllInputFrequencyAfterDivider);
      boolean validFllInputClock;
      double fllInputFrequency;

      StringBuilder fllMessage = new StringBuilder();
      if (mcg_c1_irefs) {
         // Using slow internal reference clock
         validFllInputClock = true;
         fllInputFrequency = internalReferenceClockNode.getValueAsLong();
         fllMessage.append("Origin = Slow IRC");
      }
      else {
         // Using external reference clock
         validFllInputClock = (check.mcg_c1_frdiv_clockMessage == null);
         fllInputFrequency = check.fllInputFrequency;
         fllMessage.append("Origin = External reference clock after scaling");
      }
      long system_mcgout_clock = system_mcgout_clockNode.getValueAsLong();
      if (fllInputFrequency>(system_mcgout_clock/8.0)) {
         // Too high a frequency - disabled
         system_mcgffclk_clockNode.setValue(0);
         fllMessage.append("\nDisabled as freq>(MCGOUTCLK/8)");
      }
      else {
         system_mcgffclk_clockNode.setValue(Math.round(fllInputFrequency));
      }
//      system_mcgffclk_clockNode.setMessage(new Message(fllMessage.toString(), Severity.OK));
      // Default values
      long mcg_c4_drst_drs = 0;
      Message mcg_c4_dmx32NodeMessage = null;

      if (validFllInputClock) {
         // Check if using narrowed bandwidth
         if ((mcg_c4_dmx32Node.getValueAsBoolean()) &&
               ((fllInputFrequency < FLL_CLOCK_NARROW_MIN) || (fllInputFrequency > FLL_CLOCK_NARROW_MAX))) {
            // Internal reference selected with narrow FLL bandwidth
            mcg_c4_dmx32NodeMessage = new Message (
               "FLL reference clock must be 32.768 kHz: Currently = "+
               EngineeringNotation.convert(fllInputFrequency, 5)+"Hz", warningLevel);
            validFllInputClock = false;
         }
      }
      boolean fllOutputValid = validFllInputClock;
      Message fllTargetFrequencyMessage = null;
      if (!validFllInputClock) {
         fllTargetFrequencyMessage = new Message("FLL not usable with input clock frequency: "+
                 EngineeringNotation.convert(fllInputFrequency, 5)+"Hz", warningLevel);
      }
      else {
//         System.err.println("FllClockValidate.validate() fllInputFrequency = " + fllInputFrequency);
//         fllTargetFrequencyNode.setEnabled(true);

         //=================
         // Determine possible output frequencies & check against desired value
         //
         long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();

         long fllOutFrequency;
         if (mcg_c4_dmx32Node.getValueAsBoolean()) {
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
            // Accept value within ~10% of desired
            if (Math.abs((fllOutFrequency*probe) - fllTargetFrequency) < (fllTargetFrequency/10)) {
               mcg_c4_drst_drs = probe-1;
            }         
         }

         StringBuilder sb = new StringBuilder();
         Severity severity = Severity.OK;
         if (mcg_c4_drst_drs >= 0) {
            if (fllTargetFrequency != (fllOutFrequency*(mcg_c4_drst_drs+1))) {
               // Adjust rounded value
               fllTargetFrequency = (fllOutFrequency*(mcg_c4_drst_drs+1));
               fllTargetFrequencyNode.setValue(fllTargetFrequency);
//               fllTargetFrequencyNode.setMessage(new Message("Value rounded to nearest possible value", Severity.OK));
            }
         }
         else {
            fllOutputValid = false;
            mcg_c4_drst_drs = 0;
            sb.append("Not possible to generate desired FLL frequency from input clock. \n");
            severity = warningLevel;
         }
         boolean needComma = false;
         for (Long freq : fllFrequencies) {
            if (needComma) {
               sb.append(", ");
            }
            else {
               sb.append("Possible values (Hz) = ");
            }
            needComma = true;
            sb.append(EngineeringNotation.convert(freq, 5)+"Hz");
         }
         fllTargetFrequencyMessage = new Message(sb.toString(), severity);
//         System.err.println("FllClockValidate.validate() fllOutFrequency = " + fllOutFrequency);
      }
      mcg_c2_rangeNode.setValue(mcg_c2_range);
      mcg_c4_drst_drsNode.setValue(mcg_c4_drst_drs);
      mcg_c2_erefsNode.setMessage(mcg_c2_erefs_message);
      mcg_c4_dmx32Node.setMessage(mcg_c4_dmx32NodeMessage);
      fllTargetFrequencyNode.setMessage(fllTargetFrequencyMessage);
   }
}
