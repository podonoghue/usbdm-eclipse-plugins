package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
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
/**
 * @author peter
 *
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


   // Constants
   private boolean          rtcSharesPins         = false;
   
   // Read-write variables
   private BooleanVariable  rtc_cr_osceVar        = null;
   private BooleanVariable  rtc_cr_clkoVar        = null;

   /** /OSC0/osc_input_freq or /SIM/rtc_input_freq */
   private LongVariable     osc_input_freqVar     = null; 
   /** /OSC0/osc32k_clock or /SIM/rtc32k_clock */
   private LongVariable     osc_clockVar          = null;
   /** /RTC/osc_input_freq */
   private LongVariable     rtc_osc_input_freqVar = null;
   /** /RTC/osc_clock */
   private LongVariable     rtc_osc_clockVar      = null;

   // Write-only variables
   private ChoiceVariable   rtc_cr_scpVar         = null;
   private Variable         rtc_cr_umVar          = null;
   private Variable         rtc_cr_supVar         = null;
   private Variable         rtc_cr_wpeVar         = null;
   private LongVariable     rtc_1hz_clockVar      = null;
   private LongVariable     rtcclk_gated_clockVar = null;

   
   /**
    * @param peripheral Associated peripheral
    * @param values     Not used
    */
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


      Status   status   = null;
      String   origin   = getPeripheral().getName();

      long     osc_input_freq;
      if (rtc_osc_input_freqVar.equals(variable)) {
         osc_input_freq = rtc_osc_input_freqVar.getValueAsLong();
         osc_input_freqVar.setValue(osc_input_freq);
      }
      else {
         osc_input_freq = osc_input_freqVar.getValueAsLong();
         rtc_osc_input_freqVar.setValue(osc_input_freq);
      }
      rtc_osc_clockVar.setValue(osc_clockVar.getValueAsLong());
      rtc_osc_clockVar.setStatus(osc_clockVar.getStatus());
      rtc_osc_clockVar.setOrigin(osc_clockVar.getOrigin());
      
      //=========================================
      // Check input clock/oscillator ranges
      //
      long   rtcClockFrequency = osc_input_freq;

      if ((osc_input_freq < RtcValidate.EXTERNAL_EXTAL_RANGE_MIN) || (osc_input_freq > RtcValidate.EXTERNAL_EXTAL_RANGE_MAX)) {
         status = OSCCLK32K_CLOCK_WARNING_MSG;
         origin = origin+" (invalid RTC frequency)";
         rtcClockFrequency = 0L;
         rtc_cr_osceVar.setValue(false);
         rtc_cr_osceVar.setStatus(status);
         rtc_osc_input_freqVar.setStatus(status);
      }
      else {
         rtc_cr_osceVar.clearStatus();
         rtc_osc_input_freqVar.clearStatus();
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
      rtc_1hz_clockVar.enable(rtc_cr_osce);
      
      if (!rtcSharesPins) {
         // If shared then let MCG control enable 
         osc_clockVar.enable(rtc_cr_osce);
      }      
      if (!rtcSharesPins || rtc_cr_osce) {
         // Only update if currently controlled by RTC 
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
      rtcclk_gated_clockVar = getLongVariable("rtcclk_gated_clock");

      rtc_cr_clkoVar.enable(rtc_cr_osce);

      if (rtc_cr_osce && rtc_cr_clkoVar.getValueAsBoolean()) {
         rtcclk_gated_clockVar.setValue(rtcClockFrequency);
         rtcclk_gated_clockVar.setStatus(status);
         rtcclk_gated_clockVar.setOrigin(origin);
         rtcclk_gated_clockVar.enable(rtc_cr_osce);
      }
      else {
         rtcclk_gated_clockVar.setValue(0L);
         rtcclk_gated_clockVar.setStatus(new Status("Disabled by rtc_cr_clko", Severity.WARNING));
         rtcclk_gated_clockVar.setOrigin(origin+" (disabled)");
         rtcclk_gated_clockVar.enable(false);
      }
   }
   
   @Override
   protected void createDependencies() throws Exception {
      ArrayList<String> externalVariablesList = new ArrayList<String>(); 
      
      StringVariable rtcSharesPinsVar = safeGetStringVariable("/SIM/rtcSharesPins");
      rtcSharesPins =  (rtcSharesPinsVar != null) && rtcSharesPinsVar.getValueAsBoolean();
      
      rtc_cr_osceVar           = safeGetBooleanVariable("rtc_cr_osce");
      rtc_cr_clkoVar           = getBooleanVariable("rtc_cr_clko");
      
      StringVariable osc0_peripheralVar = getStringVariable("/SIM/osc0_peripheral");
      String osc0_peripheral = null;
      osc0_peripheral = osc0_peripheralVar.getValueAsString();
      if (rtcSharesPins) {
         // RTC uses main oscillator (OSC0) XTAL/EXTAL pins
         //===================================================
         externalVariablesList.add(osc0_peripheral+"/osc_input_freq");
         externalVariablesList.add(osc0_peripheral+"/osc_clock");

         osc_input_freqVar      = getLongVariable(osc0_peripheral+"/osc_input_freq");
         osc_clockVar           = getLongVariable(osc0_peripheral+"/osc32k_clock");
         
         rtc_cr_osceVar.setToolTip(
               "Enable "+osc0_peripheral.substring(1)+" as 32kHz RTC oscillator\n"+
               "Note: this disables "+osc0_peripheral.substring(1)+" control by MCG");
      }
      else {
         // RTC uses separate XTAL32/EXTAL32 pins
         //===================================================
         externalVariablesList.add("/SIM/rtc_input_freq");
         externalVariablesList.add("/SIM/rtc32k_clock");

         osc_input_freqVar =  getLongVariable("/SIM/rtc_input_freq");
         osc_clockVar      =  getLongVariable("/SIM/rtc32k_clock");

         rtc_cr_osceVar.setToolTip("Enable 32kHz RTC oscillator");
      }
      rtc_osc_input_freqVar = getLongVariable("osc_input_freq");
      rtc_osc_input_freqVar.setDescription(osc_input_freqVar.getDescription());
      rtc_osc_input_freqVar.setToolTip(osc_input_freqVar.getToolTip());
      if (rtcSharesPins) {
         rtc_osc_input_freqVar.setMin(osc_input_freqVar.getMin());
         rtc_osc_input_freqVar.setMax(osc_input_freqVar.getMax());
      }
      
      rtc_osc_clockVar = getLongVariable("osc_clock");
      rtc_osc_clockVar.setDescription(osc_clockVar.getDescription());
      rtc_osc_clockVar.setToolTip(osc_clockVar.getToolTip());
      
      // Write-only variables
      rtc_cr_scpVar     = getChoiceVariable("rtc_cr_scp");
      rtc_cr_umVar      = getVariable("rtc_cr_um");
      rtc_cr_supVar     = getVariable("rtc_cr_sup");
      rtc_cr_wpeVar     = getVariable("rtc_cr_wpe");
      rtc_1hz_clockVar  = getLongVariable("rtc_1hz_clock");

      rtcclk_gated_clockVar = getLongVariable("rtcclk_gated_clock");

      String[] externalVariables = externalVariablesList.toArray(new String[externalVariablesList.size()]);
      addToWatchedVariables(externalVariables);
   }
}
