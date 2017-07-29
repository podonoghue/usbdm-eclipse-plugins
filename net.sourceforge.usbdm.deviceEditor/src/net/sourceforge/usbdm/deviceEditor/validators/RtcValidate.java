package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     osc0
 *     osc0_div
 */
public class RtcValidate extends Validator {
   
   // Ranges for External Crystal
   private   static final long EXTERNAL_EXTAL_RANGE_MIN = 32000L;
   private   static final long EXTERNAL_EXTAL_RANGE_MAX = 40000L;

   /** External Crystal frequency error message */
   private   static final Status OSCCLK32K_CLOCK_WARNING_MSG = new Status(String.format(
      "External crystal frequency not suitable for RTCCLK32\n"+
      "Range [%sHz,%sHz]",           
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE_MAX, 3)),
      Severity.WARNING);

   public RtcValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine RTC oscillator settings
    * 
    * Outputs rtcclk_clock, rtcclk_gated_clock, 
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      // OSC
      //=================================
      Variable     rtc_input_freqVar               =  getVariable("rtc_input_freq");
      Variable     rtc_cr_osceVar                  =  getVariable("rtc_cr_osce");
      Variable     rtcclk_clockVar                 =  getVariable("rtcclk_clock");
      Variable     rtcclk_gated_clockVar           =  getVariable("rtcclk_gated_clock");
      Variable     rtc_cr_clkoVar                  =  getVariable("rtc_cr_clko");
      Variable     rtc_cr_scpVar                   =  getVariable("rtc_cr_scp");
      Variable     rtc_cr_umVar                    =  getVariable("rtc_cr_um");
      Variable     rtc_cr_supVar                   =  getVariable("rtc_cr_sup");
      Variable     rtc_cr_wpeVar                   =  getVariable("rtc_cr_wpe");
      Variable     irqHandlingMethodVar               =  getVariable("irqHandlingMethod");
      Variable     rtc_irqLevelVar                 =  getVariable("irqLevel");

//      Variable     sim_sopt1_osc32kselVar          =  getVariable("sim_sopt1_osc32ksel");
//      Variable     system_erclk32k_clockVar        =  getVariable("system_erclk32k_clock");
//      Variable     system_rtc_clkoutVar            =  getVariable("system_rtc_clkout");

      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //
      // Check suitability of OSC for OSC32KCLK
      long rtcclk_input_freq = rtc_input_freqVar.getValueAsLong();
      Status inputStatus = null;
      if ((rtcclk_input_freq < EXTERNAL_EXTAL_RANGE_MIN) || (rtcclk_input_freq > EXTERNAL_EXTAL_RANGE_MAX)) {
         inputStatus = OSCCLK32K_CLOCK_WARNING_MSG;
         rtcclk_clockVar.setOrigin("RTCCLK (invalid range)");
         rtcclk_input_freq = 0L;
      }
      else {
         inputStatus = null;
         rtcclk_clockVar.setOrigin("RTCCLK");
      }
      rtcclk_clockVar.setValue(rtcclk_input_freq);
      
      boolean rtc_cr_osce = rtc_cr_osceVar.getValueAsBoolean();
      if (rtc_cr_osce) {
         // Enabled
         rtcclk_clockVar.setStatus(inputStatus);
         rtc_input_freqVar.setStatus(inputStatus);
      }
      else {
         // Disabled
         rtcclk_clockVar.setStatus(new Status("RTCCLK Disabled by rtc_cr_osce", Severity.WARNING));
         rtc_input_freqVar.setStatus((Status)null);
      }
      rtcclk_clockVar.enable(rtc_cr_osce);
      rtc_cr_scpVar.enable(rtc_cr_osce);
      rtc_cr_clkoVar.enable(rtc_cr_osce);
      rtc_cr_umVar.enable(rtc_cr_osce);
      rtc_cr_supVar.enable(rtc_cr_osce);
      rtc_cr_wpeVar.enable(rtc_cr_osce);
      irqHandlingMethodVar.enable(rtc_cr_osce);
      rtc_irqLevelVar.enable(rtc_cr_osce);
      
      // RTC Clocks
      //==============================
      Long    rtcclk_clockValue  = rtcclk_clockVar.getValueAsLong();
      Status rtcclk_clockStatus = rtcclk_clockVar.getStatus();
      String  rtcclk_clockOrigin = rtcclk_clockVar.getOrigin();

      if (rtc_cr_clkoVar.getValueAsBoolean()) {
         rtcclk_gated_clockVar.setValue(rtcclk_clockValue);
         rtcclk_gated_clockVar.setStatus(rtcclk_clockStatus);
         rtcclk_gated_clockVar.setOrigin(rtcclk_clockOrigin);
         rtcclk_gated_clockVar.enable(rtc_cr_osce);
      }
      else {
         rtcclk_gated_clockVar.setValue(0L);
         rtcclk_gated_clockVar.setStatus(new Status("Disabled by rtc_cr_clko", Severity.WARNING));
         rtcclk_gated_clockVar.setOrigin(rtcclk_clockOrigin);
         rtcclk_gated_clockVar.enable(false);
      }
      
   }
}
