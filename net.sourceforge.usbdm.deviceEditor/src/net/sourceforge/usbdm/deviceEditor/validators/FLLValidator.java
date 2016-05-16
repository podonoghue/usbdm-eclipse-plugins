package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class FLLValidator extends BaseClockValidator {

//   static class FllPair {
//      final int  mcg_c1_frdiv;
//      final long inputFrequency;
//      
//      FllPair(int  mcg_c1_frdiv, long inputFrequency) {
//         this.mcg_c1_frdiv = mcg_c1_frdiv;
//         this.inputFrequency = inputFrequency;
//      }
//   }
   
   public FLLValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

   private static final long FLL_CLOCK_NARROW_MIN = 32768L-100;
   private static final long FLL_CLOCK_NARROW_MAX = 32768L+100;
   
   @Override
   protected void validate() {

      // External
      Variable internalReferenceClockNode     =  fPeripheral.getVariable("system_slow_irc_clock");
      Variable system_erc_clockNode           =  fPeripheral.getVariable("system_erc_clock");
      if (system_erc_clockNode == null) {
         // Try oscillator 0 clock
         system_erc_clockNode                 =  fPeripheral.getVariable("oscclk0_clock");
      }
      if (system_erc_clockNode == null) {
         // Try oscillator clock
         system_erc_clockNode                 =  fPeripheral.getVariable("oscclk_clock");
      }
      Variable mcg_c2_erefsNode               =  fPeripheral.getVariable("mcg_c2_erefs0");
      
      // FLL
      Variable primaryClockModeNode           =  fPeripheral.getVariable("clock_mode");
      Variable mcg_c2_rangeNode               =  fPeripheral.getVariable("mcg_c2_range0");
      Variable mcg_c1_frdivNode               =  fPeripheral.getVariable("mcg_c1_frdiv");
      Variable system_mcgffclk_clockNode      =  fPeripheral.getVariable("system_mcgffclk_clock");

      Variable mcg_c4_dmx32Node               =  fPeripheral.getVariable("mcg_c4_dmx32");
      Variable mcg_c4_drst_drsNode            =  fPeripheral.getVariable("mcg_c4_drst_drs");

      Variable fllTargetFrequencyNode         =  fPeripheral.getVariable("fllTargetFrequency");



      
      // For reference on only!
      Variable mcg_c7_oscselNode              =  fPeripheral.getVariable("mcg_c7_oscsel");

      ClockMode clockMode = ClockMode.valueOf(primaryClockModeNode.getValue());
      
      boolean irefs = false;
      switch(clockMode) {
      case ClockMode_None:
      case ClockMode_FEI:
      case ClockMode_FBI:
      case ClockMode_BLPI:
         irefs = true;
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
      
      // Name of External Reference Clock
      String system_erc_clock_name = "External clock or crystal";
      if (mcg_c7_oscselNode != null) {
         switch ((int)mcg_c7_oscselNode.getValueAsLong()) {
         default:
         case 0: // ERC = OSCCLK
            break;
         case 1: // ERC = OSC32KCLK
            system_erc_clock_name = "RTC External clock or crystal";
            break;
         case 2: // ERC = IRC48M
            system_erc_clock_name = "Internal 48MHz clock";
            break;
         }
      }
      System.err.println("FLLValidator() ERC = " + system_erc_clock_name);
      
      // Main clock used by FLL
      long system_erc_clock = system_erc_clockNode.getValueAsLong();

      //=========================================
      // Determine mcg_c2_range
      //    - Clock range of oscillator
      //    - Affects FLL prescale
      //
      long    mcg_c2_range         = mcg_c2_rangeNode.getValueAsLong();
      Message mcg_c2_erefs_message = null;

      BaseClockValidator.FllDivider check = new FllDivider(system_erc_clock, mcg_c2_erefsNode.getValueAsLong(), system_erc_clockNode.getValueAsLong());
      mcg_c1_frdivNode.setMessage(check.mcg_c1_frdiv_clockMessage);
      mcg_c1_frdivNode.setValue(check.mcg_c1_frdiv);
      
//      System.err.println("FllClockValidate.validate() externalfllInputFrequencyAfterDivider = " + externalfllInputFrequencyAfterDivider);
      boolean validFllInputClock;
      double fllInputFrequency;
      
      if (!irefs) {
         // Using external reference clock
         validFllInputClock = (check.mcg_c1_frdiv_clockMessage == null);
         fllInputFrequency = check.fllInputFrequency;
      }
      else {
         // Using internal (low frequency) reference
         validFllInputClock = true;
         fllInputFrequency = internalReferenceClockNode.getValueAsLong();
      }
      system_mcgffclk_clockNode.setValue(Math.round(fllInputFrequency));
      
      // Default values
      long mcg_c4_drst_drs = 0;
      Message mcg_c4_dmx32NodeMessage = null;

      if (validFllInputClock) {
         // Check if using narrowed bandwidth
         if ((mcg_c4_dmx32Node.getValueAsBoolean()) &&
               ((fllInputFrequency < FLL_CLOCK_NARROW_MIN) || (fllInputFrequency > FLL_CLOCK_NARROW_MAX))) {
            // Internal reference selected with narrow FLL bandwidth
            mcg_c4_dmx32NodeMessage = new Message (
                  String.format("FLL reference clock must be 32.768 kHz when (MCG_C4_DMX32 = 1) (currently = %3.3f kHz)", 
                  ((double)fllInputFrequency)/1000.0), Severity.WARNING);
            validFllInputClock = false;
         }
      }
      boolean fllOutputValid = validFllInputClock;
      Message fllTargetFrequencyMessage = null;
      if (!validFllInputClock) {
         fllTargetFrequencyMessage = new Message("FLL not usable with input clock frequency", Severity.WARNING);
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
            if ((fllOutFrequency*probe) == fllTargetFrequency) {
               mcg_c4_drst_drs = probe-1;
            }         
         }
         if (mcg_c4_drst_drs < 0) {
            fllOutputValid = false;
            mcg_c4_drst_drs = 0;
            StringBuilder sb = new StringBuilder("Not possible to generate desired FLL frequency from input clock. \nPossible values (Hz) = ");
            boolean needComma = false;
            for (Long freq : fllFrequencies) {
               if (needComma) {
                  sb.append(", ");
               }
               needComma = true;
               sb.append(String.format("%d", freq));
            }
            fllTargetFrequencyMessage = new Message(sb.toString(), Severity.WARNING);
         }
//         System.err.println("FllClockValidate.validate() fllOutFrequency = " + fllOutFrequency);
      }
      // Check if trying to use FLL when not available
      if (fllInUse && !fllOutputValid) {
         primaryClockModeNode.setMessage("FLL incorrectly configured, FLL may not be used");
      }
      mcg_c2_rangeNode.setValue(mcg_c2_range);
      mcg_c4_drst_drsNode.setValue(mcg_c4_drst_drs);
      mcg_c2_erefsNode.setMessage(mcg_c2_erefs_message);
      mcg_c4_dmx32Node.setMessage(mcg_c4_dmx32NodeMessage);
      fllTargetFrequencyNode.setMessage(fllTargetFrequencyMessage);
   }
}
