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
 *
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

      // Indicates RTC uses main oscillator XTAL/EXTAL pins
      Variable rtcSharesPinsVar = safeGetVariable("/SIM/rtcSharesPins");
      boolean  rtcSharesPins    = (rtcSharesPinsVar != null) && rtcSharesPinsVar.getValueAsBoolean();

      super.validate(variable);

      // RTC
      //=================================
      BooleanVariable  rtc_cr_osceVar    = safeGetBooleanVariable("rtc_cr_osce");
      if (rtc_cr_osceVar == null) {
         return;
      }
      ChoiceVariable   rtc_cr_scpVar     = getChoiceVariable("rtc_cr_scp");
      Variable         rtc_cr_umVar      = getVariable("rtc_cr_um");
      Variable         rtc_cr_supVar     = getVariable("rtc_cr_sup");
      Variable         rtc_cr_wpeVar     = getVariable("rtc_cr_wpe");
      LongVariable     rtc_1hz_clockVar  = getLongVariable("rtc_1hz_clock");

      long             osc_input_freq = 0;
      LongVariable     osc_input_freqVar = null;
      LongVariable     osc_clockVar      = null;

      Status           status            = null;
      String           origin            = "RTCCLK";

      if (rtcSharesPins) {
         // RTC uses main oscillator XTAL/EXTAL pins
         //===================================================
         String osc0_peripheral = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
         osc_input_freqVar      = getLongVariable(osc0_peripheral+"/osc_input_freq");
         osc_clockVar           = getLongVariable(osc0_peripheral+"/osc_clock");
         origin                 = "RTCCLK";
         rtc_cr_osceVar.setToolTip(
               "Enable main oscillator as 32kHz RTC oscillator\n"+
               "Note: this disables OSC control by MCG");
      }
      else {
         // RTC uses separate XTAL32/EXTAL32 pins
         //===================================================
         osc_input_freqVar =  getLongVariable("osc_input_freq");
         osc_clockVar      =  getLongVariable("osc_clock");
         origin            = "RTCCLK";
         rtc_cr_osceVar.setToolTip("Enable 32kHz RTC oscillator");
         
         // Warn if SCL and SDA signals not mapped
         validateMappedPins(new int[]{0,1}, getPeripheral().getSignalTables().get(0).table);
      }
      osc_input_freq = osc_input_freqVar.getValueAsLong();

      //=========================================
      // Check input clock/oscillator ranges
      //
      long   rtcClockFrequency = osc_input_freq;

      if ((osc_input_freq < RtcValidate.EXTERNAL_EXTAL_RANGE_MIN) || (osc_input_freq > RtcValidate.EXTERNAL_EXTAL_RANGE_MAX)) {
         status = OSCCLK32K_CLOCK_WARNING_MSG;
         origin = origin+" (invalid range)";
         rtcClockFrequency = 0L;
         rtc_cr_osceVar.setValue(false);
         rtc_cr_osceVar.setStatus(status);
         if (!rtcSharesPins) { 
            osc_input_freqVar.setStatus(status);
         }
      }
      else {
         rtc_cr_osceVar.setStatus((Status)null);
         if (!rtcSharesPins) { 
            osc_input_freqVar.setStatus((Status)null);
         }
         if (!rtc_cr_osceVar.getValueAsBoolean()) {
            status = new Status("Disabled by rtc_cr_osce", Severity.WARNING);
            origin = origin+" (disabled)";
            rtcClockFrequency = 0L;
         }
      }
      //=========================================
      // Check and propagate enabled
      //
      boolean rtc_cr_osce = rtc_cr_osceVar.isEnabled() && rtc_cr_osceVar.getValueAsBoolean();
      rtc_cr_scpVar.enable(rtc_cr_osce);
      rtc_cr_umVar.enable(rtc_cr_osce);
      rtc_cr_supVar.enable(rtc_cr_osce);
      rtc_cr_wpeVar.enable(rtc_cr_osce);
      osc_clockVar.enable(rtc_cr_osce);
      rtc_1hz_clockVar.enable(rtc_cr_osce);

      if (!rtcSharesPins || rtc_cr_osce) {
         // Only update if owned by RTC 
         osc_clockVar.setValue(rtcClockFrequency);
         osc_clockVar.setStatus(status);
         osc_clockVar.setOrigin(origin);
      }
      rtc_1hz_clockVar.setValue((rtcClockFrequency>0)?1:0);
      rtc_1hz_clockVar.setStatus(status);
      rtc_1hz_clockVar.setOrigin(origin);

      // RTC Clocks
      //==============================
      // Check if gating option
      BooleanVariable  rtc_cr_clkoVar        = getBooleanVariable("rtc_cr_clko");
      LongVariable     rtcclk_gated_clockVar = getLongVariable("rtcclk_gated_clock");

      rtc_cr_clkoVar.enable(rtc_cr_osce);

      if (rtc_cr_clkoVar.isEnabled() && rtc_cr_clkoVar.getValueAsBoolean()) {
         rtcclk_gated_clockVar.setValue(rtcClockFrequency);
         rtcclk_gated_clockVar.setStatus(status);
         rtcclk_gated_clockVar.setOrigin(origin);
         rtcclk_gated_clockVar.enable(rtc_cr_osce);
      }
      else {
         rtcclk_gated_clockVar.setValue(0L);
         rtcclk_gated_clockVar.setStatus(new Status("Disabled by rtc_cr_clko", Severity.WARNING));
         rtcclk_gated_clockVar.setOrigin("RTCCLK (disabled)");
         rtcclk_gated_clockVar.enable(false);
      }
   }
   
   @Override
   protected void createDependencies() throws Exception {
      ArrayList<String> externalVariablesList = new ArrayList<String>(); 
      
      externalVariablesList.add("/SIM/system_erclk32k_clock");
      if (safeGetVariable("/SIM/rtcSharesPins") != null) {
         final String osc0_peripheral = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
         externalVariablesList.add(osc0_peripheral+"/osc_input_freq");
      }
      String[] externalVariables = externalVariablesList.toArray(new String[externalVariablesList.size()]);
      addToWatchedVariables(externalVariables);
   }
}
