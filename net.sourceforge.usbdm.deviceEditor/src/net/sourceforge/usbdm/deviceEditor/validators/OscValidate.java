package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

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
public class OscValidate extends Validator {
   
   public static final String OSC_RANGE_KEY       = "range_out";
   
   /** Used to indicate range is unconstrained by oscillator */
   public static final int    UNCONSTRAINED_RANGE = 3; 
   
   // Ranges for External Crystal
   private   static final long EXTERNAL_EXTAL_RANGE1_MIN = 32000L;
   private   static final long EXTERNAL_EXTAL_RANGE1_MAX = 40000L;
   
   private   static final long EXTERNAL_EXTAL_RANGE2_MIN = 3000000L;
   private   static final long EXTERNAL_EXTAL_RANGE2_MAX = 8000000L;
   
   private   static final long EXTERNAL_EXTAL_RANGE3_MIN = 8000000L;
   private   static final long EXTERNAL_EXTAL_RANGE3_MAX = 32000000L;

   // Maximum External Clock (not Crystal)
   private   static final long EXTERNAL_CLOCK_MAX        = 50000000L;

   /** External Crystal frequency error message */
   private   static final String OSCCLK32K_CLOCK_MSG = String.format(
      "External crystal frequency not suitable for 32k Oscillator mode\n"+
      "Range [%sHz,%sHz]",           
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3));

   /** External crystal frequency error message */
   private   static final Status FLL_CLOCK_ERROR_MSG = new Status(String.format(
      "External crystal frequency not suitable for oscillator\n"+
      "Ranges [%sHz,%sHz], [%sHz,%sHz], [%sHz,%sHz]",           
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE2_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE2_MAX, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE3_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE3_MAX, 3)),
      Severity.WARNING);

   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/RTC/rtc_cr_osce",
   };
   
   /** External clock frequency error message */
   private   static final Status CLOCK_RANGE_ERROR_MSG = new Status(String.format(
      "External clock frequency is too high\nMax=%sHz",
      EngineeringNotation.convert(EXTERNAL_CLOCK_MAX, 3)), 
      Severity.WARNING);

   
   public OscValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
      if (safeGetVariable(OSC_RANGE_KEY) == null) {
         fPeripheral.addVariable(new LongVariable(OSC_RANGE_KEY, fPeripheral.makeKey(OSC_RANGE_KEY)));
      }
   }

   /**
    * Class to determine oscillator settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);

      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      
      // OSC
      //=================================
      Variable     osc_cr_erclkenVar               =  getVariable("osc_cr_erclken");
      Variable     erefs0Var                       =  getVariable("erefs0");
      Variable     hgo0Var                         =  getVariable("hgo0");
      Variable     osc_cr_scpVar                   =  getVariable("osc_cr_scp");
      Variable     osc_cr_erefstenVar              =  getVariable("osc_cr_erefsten");
      Variable     rangeOutVar                     =  getVariable(OSC_RANGE_KEY);
//      Variable     rangeVar                        =  getVariable("range");
      Variable     system_oscerclk_undiv_clockVar  =  safeGetVariable("system_oscerclk_undiv_clock");
      Variable     system_oscerclk_clockVar        =  null;
      if (system_oscerclk_undiv_clockVar == null) {
         system_oscerclk_undiv_clockVar            =  getVariable("system_oscerclk_clock");
      }
      else {
         system_oscerclk_clockVar                  =  getVariable("system_oscerclk_clock");
      }
      Variable     osc_div_erpsVar                 =  safeGetVariable("osc_div_erps");
      LongVariable osc32kclk_clockVar              =  getLongVariable("osc32kclk_clock");
      Variable     oscclk_clockVar                 =  getVariable("oscclk_clock");
      Variable     osc_input_freqVar               =  getVariable("osc_input_freq");
      
      Variable     rtcSharesPinsVar                =  safeGetVariable("/SIM/rtcSharesPins");
      Variable     rtc_cr_osceVar                  =  safeGetVariable("/RTC/rtc_cr_osce");

      long    oscclk_clock_freq    = osc_input_freqVar.getValueAsLong();
      
      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //
      
      // Check if RTC has control of OSC
      boolean rtcForcing = ((rtcSharesPinsVar != null) && rtc_cr_osceVar.getValueAsBoolean());
      
      // OSC mode if selected by erefs or RTC
      boolean erefs0 = erefs0Var.getValueAsBoolean() || rtcForcing;

      String  oscclk_clockOrg       = null;
      Status  oscclk_clockStatus    = null;

      String  rangeOrigin  = "Unused";
      int     range        = UNCONSTRAINED_RANGE;

      if (erefs0) {
         // Using oscillator - range is chosen to suit crystal frequency (or forced by RTC)
         if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE1_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE1_MAX)) {
            oscclk_clockOrg = "OSCCLK (low range oscillator)";
            rangeOrigin     = "Determined by Crystal Frequency";
            range           = 0;
         }
         else if (rtcForcing) {
            // Not suitable as OSC Crystal frequency
            oscclk_clockOrg     = "OSCCLK (invalid range for RTC clock)";
            oscclk_clockStatus  = new Status(OSCCLK32K_CLOCK_MSG, Severity.WARNING);
            range               = 0;
         }
         else if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE2_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE2_MAX)) {
            oscclk_clockOrg = "OSCCLK (high range oscillator)";
            rangeOrigin     = "Determined by Crystal Frequency";
            range           = 1;
         }
         else if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE3_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE3_MAX)) {
            oscclk_clockOrg = "OSCCLK (very high range oscillator)";
            rangeOrigin     = "Determined by Crystal Frequency";
            range           = 2;
         }
         else {
            // Not suitable as OSC Crystal frequency
            oscclk_clockOrg     = "OSCCLK (invalid range)";
            oscclk_clockStatus  = FLL_CLOCK_ERROR_MSG;
            range               = UNCONSTRAINED_RANGE;
         }
      }
      else {
         // Using external clock
         oscclk_clockOrg = "OSCCLK (External clock)";
         
         // Range has no effect on Oscillator
         range           = UNCONSTRAINED_RANGE;
         
         // Check suitable clock range
         if (oscclk_clock_freq>EXTERNAL_CLOCK_MAX) {
            // Not suitable as external clock
            oscclk_clockStatus = CLOCK_RANGE_ERROR_MSG;
         }
      }
      // Check suitability of OSC for OSC32KCLK
      Status  osc32kclk_clockStatus = null;
      String  osc32kclk_clockOrg    = oscclk_clockOrg;
      
      if ((oscclk_clock_freq < EXTERNAL_EXTAL_RANGE1_MIN) || (oscclk_clock_freq > EXTERNAL_EXTAL_RANGE1_MAX)) {
         osc32kclk_clockStatus = new Status(OSCCLK32K_CLOCK_MSG, Severity.WARNING);
         osc32kclk_clockOrg    = osc32kclk_clockOrg+"(invalid range)";
      }
      if (rtcForcing) {
         Status rtcInUseMessage = new Status("Feature is controlled by RTC which shares XTAL/EXTAL pins", Severity.INFO);
         erefs0Var.enable(false);
         erefs0Var.setStatus(rtcInUseMessage);
         
         osc_cr_scpVar.enable(false);
         osc_cr_scpVar.setStatus(rtcInUseMessage);
         
         hgo0Var.enable(false);
         hgo0Var.setStatus(rtcInUseMessage);
      }
      else {
         osc_input_freqVar.enable(true);
         osc_input_freqVar.setStatus((Status)null);
         
         erefs0Var.enable(true);
         erefs0Var.setStatus((Status)null);
         
         osc_cr_scpVar.enable(erefs0);
         osc_cr_scpVar.setStatus((Status)null);
         
         hgo0Var.enable(erefs0);
         hgo0Var.setStatus((Status)null);
      }      
      
      boolean oscclkOK = (oscclk_clockStatus==null) || oscclk_clockStatus.getSeverity().lessThan(Severity.WARNING);
      long checkedOscclk_clock_freq = oscclkOK?oscclk_clock_freq:0;

      oscclk_clockVar.setOrigin(oscclk_clockOrg);
      oscclk_clockVar.setStatus(oscclk_clockStatus);
      oscclk_clockVar.setValue(oscclkOK?oscclk_clock_freq:0);
      oscclk_clockVar.enable(oscclkOK);
      
      rangeOutVar.setValue(range);
      rangeOutVar.setOrigin(rangeOrigin);
      
      osc32kclk_clockVar.setValue((osc32kclk_clockStatus != null)?0:oscclk_clock_freq);
      osc32kclk_clockVar.setStatus(osc32kclk_clockStatus);
      osc32kclk_clockVar.setOrigin(osc32kclk_clockOrg);
//      osc32kclk_clockVar.enable(osc32kclk_clockStatus==null);

      // Determine OSCERCLK, OSCERCLK_UNDIV 
      //==================================
      if (osc_cr_erclkenVar.getValueAsBoolean()) {
         // Oscillator/clock enabled
         system_oscerclk_undiv_clockVar.setValue(checkedOscclk_clock_freq);
         system_oscerclk_undiv_clockVar.setStatus(oscclk_clockVar.getFilteredStatus());
         system_oscerclk_undiv_clockVar.setOrigin(oscclk_clockVar.getOrigin());
         system_oscerclk_undiv_clockVar.enable(true);
         osc_cr_erefstenVar.enable(true);
         long system_oscerclk = checkedOscclk_clock_freq;
         if (osc_div_erpsVar != null) {
            // If divider exists
            system_oscerclk /= 1<<osc_div_erpsVar.getValueAsLong();
            oscclk_clockOrg += "/osc_div_erps";
            system_oscerclk_clockVar.setValue(system_oscerclk);
            system_oscerclk_clockVar.setStatus(oscclk_clockVar.getFilteredStatus());
            system_oscerclk_clockVar.setOrigin(oscclk_clockVar.getOrigin());
            system_oscerclk_clockVar.enable(true);
            osc_div_erpsVar.enable(true);
         }
      }
      else {
         Status osc_crMessage = new Status("Disabled by osc_cr_erclken", Severity.OK);
         // Oscillator/clock disabled
//         system_oscerclk_undiv_clockVar.setValue(0);
         system_oscerclk_undiv_clockVar.setStatus(osc_crMessage);
         system_oscerclk_undiv_clockVar.setOrigin(oscclk_clockOrg);
         system_oscerclk_undiv_clockVar.enable(false);
         osc_cr_erefstenVar.enable(false);
         if (osc_div_erpsVar != null) {
//            system_oscerclk_clockVar.setValue(0);
            system_oscerclk_clockVar.setStatus(osc_crMessage);
            system_oscerclk_clockVar.setOrigin(oscclk_clockOrg);
            system_oscerclk_clockVar.enable(false);
            osc_div_erpsVar.enable(false);
         }
      }
   }
}