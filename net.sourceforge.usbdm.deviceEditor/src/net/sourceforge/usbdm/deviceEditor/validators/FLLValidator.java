package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class FLLValidator extends BaseClockValidator {

   public FLLValidator(PeripheralWithState peripheral) {
      super(peripheral);
   }

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
         mcg_c2_erefs_message = new Message(msgText, mcg_c2_erefsNode.getValueAsBoolean()?Severity.ERROR:Severity.WARNING);
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

      //=================================================
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

      Message mcg_c1_frdivMessage  = null;
      int     mcg_c1_frdiv         = 0;

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
         mcg_c1_frdivMessage = new Message(
               String.format("Unable to find suitable divider for external reference clock frequency = %d Hz", system_erc_clock),
               Severity.WARNING);
         mcg_c1_frdiv =  7;
      }
      mcg_c1_frdivNode.setValue(mcg_c1_frdiv);
      mcg_c1_frdivNode.setMessage(mcg_c1_frdivMessage);

//      System.err.println("FllClockValidate.validate() externalfllInputFrequencyAfterDivider = " + externalfllInputFrequencyAfterDivider);

      //=================
      // Determine FLL input frequency.  From:
      //  - External reference (after dividers)
      //  - Internal (slow) reference
      Message fllTargetFrequencyMessage = null;
      double fllInputFrequency = 0;
      if (!irefs) {
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
      
      if (!validFllInputClock) {
         fllTargetFrequencyMessage = new Message("FLL not usable with input clock frequency", Severity.WARNING);
//         fllTargetFrequencyNode.setEnabled(false);

         // Check if trying to use FLL 
         if (ClockMode.valueOf(primaryClockModeNode.getValue()) == ClockMode.ClockMode_FEE) {
            // Done inside an IF as another validator may set this error message
            primaryClockModeNode.setMessage("FLL input reference is out of acceptable ranges. FLL may not be used");
         }
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
            fllTargetFrequencyMessage = new Message(buff.toString(), Severity.WARNING);
         }
//         System.err.println("FllClockValidate.validate() fllOutFrequency = " + fllOutFrequency);
      }
      mcg_c2_rangeNode.setValue(mcg_c2_range);
      mcg_c4_drst_drsNode.setValue(mcg_c4_drst_drs);
      mcg_c2_erefsNode.setMessage(mcg_c2_erefs_message);
      mcg_c4_dmx32Node.setMessage(mcg_c4_dmx32NodeMessage);
      fllTargetFrequencyNode.setMessage(fllTargetFrequencyMessage);
   }
}
