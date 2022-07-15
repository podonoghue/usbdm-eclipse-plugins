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

   /** /OSC0/osc_input_freq (only used of shared RTC/OSC0 share crystal pins */
   private LongVariable     osc0_input_freqVar    = null;
   
   /** /OSC0/osc32k_clock (only used of shared RTC/OSC0 share crystal pins */
   private LongVariable     osc0_osc32k_clockVar  = null;
   
   /** /RTC/osc_input_freq - may be linked by software to /OSC0/osc_input_freq */
   private LongVariable     rtc_osc_input_freqVar = null;
   
   /** /RTC/osc_clock - may be linked by software to /OSC0/osc_clock */
   private LongVariable     rtc_osc_clockVar      = null;
   
   // Write-only variables
   private ChoiceVariable   rtc_cr_scpVar         = null;
   private LongVariable     rtc_1hz_clockVar      = null;
   private LongVariable     rtcclk_gated_clockVar = null;
   
   /** ERCLK32K is used as RTC clock when using shared OSC0 */
   private LongVariable     system_erclk32k_clockVar = null;
   
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
      Status   status    = null;
      String   oscOrigin = "Independent RTC oscillator";

      // Initially assume using separate oscillator
      long oscFrequency = rtc_osc_input_freqVar.getValueAsLong();

      if (rtcSharesPins) {
         
         // Link rtc_osc_input_freqVar <=> osc0_input_freqVar
         if (rtc_osc_input_freqVar.equals(variable)) {
            osc0_input_freqVar.setValue(oscFrequency);
         }
         else if (osc0_input_freqVar.equals(variable)) {
            oscFrequency = osc0_input_freqVar.getValueAsLong();
            rtc_osc_input_freqVar.setValue(oscFrequency);
         }
         rtc_osc_clockVar.setValue(osc0_osc32k_clockVar.getValueAsLong());
         rtc_osc_clockVar.setStatus(osc0_osc32k_clockVar.getStatus());
      }
      
      //=========================================
      // Check input clock/oscillator ranges
      //
      boolean rtc_cr_osce = false;
      if ((oscFrequency < RtcValidate.EXTERNAL_EXTAL_RANGE_MIN) || (oscFrequency > RtcValidate.EXTERNAL_EXTAL_RANGE_MAX)) {
         // Frequency not suitable as RTC clock
         status = OSCCLK32K_CLOCK_WARNING_MSG;
         oscOrigin = oscOrigin+" (invalid RTC frequency)";
         oscFrequency = 0L;
         rtc_cr_osceVar.setValue(false);
         rtc_cr_osceVar.setStatus(status);
         rtc_osc_input_freqVar.setStatus(status);
      }
      else {
         rtc_cr_osceVar.clearStatus();
         rtc_osc_input_freqVar.clearStatus();
         
         rtc_cr_osce = rtc_cr_osceVar.isEnabled() && rtc_cr_osceVar.getValueAsBoolean();
         if (!rtc_cr_osce) {
            status = new Status("Disabled by rtc_cr_osce", Severity.WARNING);
            oscOrigin = oscOrigin+" (disabled)";
            oscFrequency = 0L;
         }
      }
      
      // Enable/disable controls as needed
      rtc_cr_scpVar.enable(rtc_cr_osce);

      if (!rtcSharesPins) {
         // osc_clockVar is independent
         rtc_osc_clockVar.enable(rtc_cr_osce);
         rtc_osc_clockVar.setValue(oscFrequency);
         rtc_osc_clockVar.setStatus(status);
         rtc_osc_clockVar.setOrigin(oscOrigin);
      }
      
      // Determine clock source for RTC
      long   rtcClockFrequency;
      String rtcOrigin;
      
      if (rtcSharesPins) {
         // RTC is driven by ERCLK32K
         rtcClockFrequency = system_erclk32k_clockVar.getValueAsLong();
         rtcOrigin         = system_erclk32k_clockVar.getOrigin();
      }
      else {
         // RTC is driven by RTC oscillator
         rtcClockFrequency = oscFrequency;
         rtcOrigin         = oscOrigin;
         
         // RTC 32kHz Clock output - only exists if RTC doesn't share OSC0 pins
         // Otherwise OSC032KCLK is used 
         //==============================
         // Check gating option
         rtc_cr_clkoVar.enable(rtc_cr_osce && !rtcSharesPins);
         
         if (!rtc_cr_osce) {
            rtcclk_gated_clockVar.setValue(0L);
            rtcclk_gated_clockVar.setStatus(new Status("Disabled by rtc_cr_osce", Severity.WARNING));
            rtcclk_gated_clockVar.setOrigin("RTC (disabled by rtc_cr_osce)");
            rtcclk_gated_clockVar.enable(false);
         }
         else if (!rtc_cr_clkoVar.getValueAsBoolean()) {
            rtcclk_gated_clockVar.setValue(0L);
            rtcclk_gated_clockVar.setStatus(new Status("Disabled by rtc_cr_clko", Severity.WARNING));
            rtcclk_gated_clockVar.setOrigin("RTC (disabled by rtc_cr_clko)");
            rtcclk_gated_clockVar.enable(false);
         }
         else {
            rtcclk_gated_clockVar.setValue(rtcClockFrequency);
            rtcclk_gated_clockVar.setStatus(status);
            rtcclk_gated_clockVar.setOrigin(rtcOrigin);
            rtcclk_gated_clockVar.enable(rtc_cr_osce);
         }
      }
      rtc_1hz_clockVar.enable(rtc_cr_osce || !rtcSharesPins);
      rtc_1hz_clockVar.setValue((rtcClockFrequency/32768.0));
      rtc_1hz_clockVar.setStatus(status);
      rtc_1hz_clockVar.setOrigin(rtcOrigin+" via RTC divider");

   }
   
   @Override
   protected void createDependencies() throws Exception {
      ArrayList<String> externalVariablesList = new ArrayList<String>(); 
      
      StringVariable rtcSharesPinsVar = safeGetStringVariable("/SIM/rtcSharesPins");
      rtcSharesPins =  (rtcSharesPinsVar != null) && rtcSharesPinsVar.getValueAsBoolean();
      
      rtc_osc_input_freqVar = getLongVariable("osc_input_freq");
      rtc_cr_osceVar        = getBooleanVariable("rtc_cr_osce");
      rtc_osc_clockVar      = getLongVariable("osc_clock");
      
      // Only used if independent
      rtc_cr_clkoVar        = getBooleanVariable("rtc_cr_clko");
      rtcclk_gated_clockVar = getLongVariable("rtcclk_gated_clock");

      if (rtcSharesPins) {
         // RTC uses main oscillator (OSC0) XTAL/EXTAL pins
         //===================================================
         StringVariable osc0_peripheralVar = getStringVariable("/SIM/osc0_peripheral");
         String osc0_peripheralName        = osc0_peripheralVar.getValueAsString();

         osc0_input_freqVar   = createLongVariableReference(osc0_peripheralName+"/osc_input_freq", externalVariablesList);
         osc0_osc32k_clockVar = createLongVariableReference(osc0_peripheralName+"/osc32k_clock",      externalVariablesList);
         
         rtc_osc_clockVar.setDescription(osc0_osc32k_clockVar.getDescription());
         rtc_osc_clockVar.setToolTip(osc0_osc32k_clockVar.getToolTip());
         
         rtc_cr_osceVar.setToolTip(
               "Enable "+osc0_peripheralName.substring(1)+" as 32kHz RTC oscillator\n"+
               "Note: this disables "+osc0_peripheralName.substring(1)+" control by MCG");
         
         rtc_osc_input_freqVar.setDescription(osc0_input_freqVar.getDescription());
         rtc_osc_input_freqVar.setToolTip(osc0_input_freqVar.getToolTip());
         rtc_osc_input_freqVar.setMin(osc0_input_freqVar.getMin());
         rtc_osc_input_freqVar.setMax(osc0_input_freqVar.getMax());
         rtc_osc_input_freqVar.setDerived(true);

         // Hide variables associated with RTC OSC
         rtc_cr_clkoVar.enable(false);
         rtc_cr_clkoVar.setHidden(true);
         rtcclk_gated_clockVar.enable(false);
         rtcclk_gated_clockVar.setHidden(true);
      }
      else {
         // RTC uses separate XTAL32/EXTAL32 pins
         //===================================================
         rtc_cr_osceVar.setToolTip("Enable independent 32kHz RTC oscillator");
         rtc_osc_input_freqVar.setMin(EXTERNAL_EXTAL_RANGE_MIN);
         rtc_osc_input_freqVar.setMax(EXTERNAL_EXTAL_RANGE_MAX);
      }
      
      // Write-only variables
      rtc_cr_scpVar     = getChoiceVariable("rtc_cr_scp");
      rtc_1hz_clockVar  = getLongVariable("rtc_1hz_clock");

      system_erclk32k_clockVar = createLongVariableReference("/SIM/system_erclk32k_clock", externalVariablesList);
      
      String[] externalVariables = externalVariablesList.toArray(new String[externalVariablesList.size()]);
      addToWatchedVariables(externalVariables);
   }
}
