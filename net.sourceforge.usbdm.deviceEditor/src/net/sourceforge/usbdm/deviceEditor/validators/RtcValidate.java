package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
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
public class RtcValidate extends PeripheralValidator {
   
   // Ranges for External Crystal
   static final long EXTERNAL_EXTAL_RANGE_MIN = 32000L;
   static final long EXTERNAL_EXTAL_RANGE_MAX = 40000L;

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
      
      // RTC
      //=================================
      LongVariable     rtc_input_freqVar     =  getLongVariable("rtc_input_freq");
      LongVariable     rtcclk_clockVar       =  getLongVariable("rtcclk_clock");
      LongVariable     rtcclk_gated_clockVar =  getLongVariable("rtcclk_gated_clock");
      BooleanVariable  rtc_cr_osceVar        =  getBooleanVariable("rtc_cr_osce");
      BooleanVariable  rtc_cr_clkoVar        =  getBooleanVariable("rtc_cr_clko");
      ChoiceVariable   rtc_cr_scpVar         =  getChoiceVariable("rtc_cr_scp");
      Variable         rtc_cr_umVar          =  getVariable("rtc_cr_um");
      Variable         rtc_cr_supVar         =  getVariable("rtc_cr_sup");
      Variable         rtc_cr_wpeVar         =  getVariable("rtc_cr_wpe");
      LongVariable     rtc_1hz_clockVar      =  getLongVariable("rtc_1hz_clock");

      //=========================================
      // Check input clock/oscillator ranges
      //
      long   rtcclk_input_freq = rtc_input_freqVar.getValueAsLong();
      Status status            = null;
      String origin            = "RTCCLK";
      long   rtcClockFrequency = rtcclk_input_freq;
      
      if ((rtcclk_input_freq < RtcValidate.EXTERNAL_EXTAL_RANGE_MIN) || (rtcclk_input_freq > RtcValidate.EXTERNAL_EXTAL_RANGE_MAX)) {
         status = OSCCLK32K_CLOCK_WARNING_MSG;
         origin = "RTCCLK (invalid range)";
         rtcClockFrequency = 0L;

         rtc_cr_osceVar.enable(false);
         rtc_cr_osceVar.setStatus(status);
         rtc_input_freqVar.setStatus(status);
      }
      else {
         rtc_cr_osceVar.enable(true);
         rtc_cr_osceVar.setStatus((Status)null);
         rtc_input_freqVar.setStatus((Status)null);

         if (!rtc_cr_osceVar.getValueAsBoolean()) {
            status = new Status("Disabled by rtc_cr_osce", Severity.WARNING);
            origin = ("RTCCLK (disabled)");
            rtcClockFrequency = 0L;
         }
      }
      rtcclk_clockVar.setValue(rtcClockFrequency);
      rtcclk_clockVar.setStatus(status);
      rtcclk_clockVar.setOrigin(origin);
      rtc_1hz_clockVar.setValue((rtcClockFrequency>0)?1:0);
      rtc_1hz_clockVar.setStatus(status);
      rtc_1hz_clockVar.setOrigin(origin);

      //=========================================
      // Check if enabled
      //
      boolean rtc_cr_osce = rtc_cr_osceVar.isEnabled() && rtc_cr_osceVar.getValueAsBoolean();
      rtc_cr_scpVar.enable(rtc_cr_osce);
      rtc_cr_umVar.enable(rtc_cr_osce);
      rtc_cr_supVar.enable(rtc_cr_osce);
      rtc_cr_wpeVar.enable(rtc_cr_osce);
      rtc_cr_clkoVar.enable(rtc_cr_osce);
      rtcclk_clockVar.enable(rtc_cr_osce);
      rtc_1hz_clockVar.enable(rtc_cr_osce);
      
      // RTC Clocks
      //==============================
      
      if (rtc_cr_clkoVar.isEnabled() && rtc_cr_clkoVar.getValueAsBoolean()) {
         rtcclk_gated_clockVar.setValue(rtcClockFrequency);
         rtcclk_gated_clockVar.setStatus(status);
         rtcclk_gated_clockVar.setOrigin(origin);
         rtcclk_gated_clockVar.enable(rtc_cr_osce);
      }
      else {
         rtcclk_gated_clockVar.setValue(0L);
         rtcclk_gated_clockVar.setStatus(new Status("Disabled by rtc_cr_clko", Severity.WARNING));
         rtcclk_gated_clockVar.setOrigin(origin);
         rtcclk_gated_clockVar.enable(false);
      }
   }
}
