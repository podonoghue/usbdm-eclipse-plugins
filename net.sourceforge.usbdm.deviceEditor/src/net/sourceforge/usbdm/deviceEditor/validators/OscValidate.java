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
public class OscValidate extends PeripheralValidator {

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
   private   static final String OSCCLK32K_RANGE_ERROR_CLOCK_MSG = String.format(
         "External crystal frequency not suitable for 32k Oscillator mode\n"+
               "Range [%sHz,%sHz]",
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3));

   /** External crystal frequency error message */
   private   static final Status XTAL_CLOCK_RANGE_ERROR_MSG = new Status(String.format(
         "External crystal frequency not suitable for oscillator\n"+
               "Ranges [%sHz,%sHz], [%sHz,%sHz], [%sHz,%sHz]",
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE2_MIN, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE2_MAX, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE3_MIN, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE3_MAX, 3)),
         Severity.ERROR);

   /** External clock frequency error message */
   private static final Status EXTERNAL_CLOCK_RANGE_ERROR_MSG = new Status(String.format(
         "External clock frequency is too high\nMax=%sHz",
         EngineeringNotation.convert(EXTERNAL_CLOCK_MAX, 3)),
         Severity.ERROR);

   private BooleanVariable  osc_cr_erclkenVar              =  null;
   private BooleanVariable  mcg_c2_erefs0Var               =  null;
   private BooleanVariable  mcg_c2_hgo0Var                 =  null;
   private ChoiceVariable   osc_cr_scpVar                  =  null;
   private Variable         osc_cr_erefstenVar             =  null;
   private Variable         oscillatorRangeVar             =  null;
   private LongVariable     system_oscer_undiv_clockVar    =  null;
   private LongVariable     system_oscer_clockVar          =  null;
   private ChoiceVariable   osc_div_erpsVar                =  null;
   private LongVariable     osc32k_clockVar                =  null;
   private LongVariable     osc_clockVar                   =  null;
   private LongVariable     osc_input_freqVar              =  null;

   // Check if RTC has control of oscillator pins
   private boolean  rtcSharesPins           = false;
   private BooleanVariable rtc_cr_osceVar   = null;

   // Indicates that the OSC must use a 32kHz xtal = low range only
   private boolean forceLowFrequencyRange;

   public OscValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine oscillator settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {

      super.validate(variable);

      // Check if RTC has control of oscillator pins
      boolean rtcForcing = rtcSharesPins && rtc_cr_osceVar.getValueAsBoolean();
      
      String  rangeOrigin        = "Unused";
      int     range              = UNCONSTRAINED_RANGE;

      long    osc_input_freq     = osc_input_freqVar.getValueAsLong();
      
      String  oscclk_clockOrg    = getPeripheral().getName();
      Status  oscclk_clockStatus = null;
      
      if (rtcForcing) {
         // RTC controlling XTAL pins
         Status rtcInUseMessage = new Status("Feature is controlled by RTC which shares XTAL/EXTAL pins", Severity.WARNING);

//         osc_cr_erefstenVar.enable(false);
//         osc_cr_erefstenVar.setStatus(rtcInUseMessage);

//         mcg_c2_erefs0Var.setDisabledValue(true);
//         mcg_c2_erefs0Var.setLocked(true);
//         mcg_c2_erefs0Var.setValue(1);
         mcg_c2_erefs0Var.enable(false);
         mcg_c2_erefs0Var.setStatus(rtcInUseMessage);

         osc_clockVar.setOrigin(osc32k_clockVar.getOrigin());
         osc_clockVar.setStatus(osc32k_clockVar.getStatus());
         osc_clockVar.setValue(osc32k_clockVar.getValueAsLong());
         osc_clockVar.enable(true);
         
         oscillatorRangeVar.enable(false);
         oscillatorRangeVar.setStatus(rtcInUseMessage);

         // Using oscillator - range is forced by RTC
         range            = 0;
         rangeOrigin      = "Forced by RTC";

         oscclk_clockOrg += " Forced by RTC";

         if ((osc_input_freq < EXTERNAL_EXTAL_RANGE1_MIN) || (osc_input_freq > EXTERNAL_EXTAL_RANGE1_MAX)) {

            // Not suitable as OSC Crystal frequency
            oscclk_clockOrg     += " (invalid range)";
            oscclk_clockStatus = new Status(OSCCLK32K_RANGE_ERROR_CLOCK_MSG, Severity.WARNING);
         }
         else {
            oscclk_clockOrg     += " (low range)";
         }

         osc_cr_scpVar.enable(false);
         osc_cr_scpVar.setStatus(rtcInUseMessage);
         if (mcg_c2_hgo0Var != null) {
            mcg_c2_hgo0Var.enable(false);
            mcg_c2_hgo0Var.setStatus(rtcInUseMessage);
         }
      }
      else {

         // OSC controlling XTAL pins

         osc_cr_erefstenVar.enable(osc_cr_erclkenVar.getValueAsBoolean());
         osc_cr_erefstenVar.clearStatus();

         mcg_c2_erefs0Var.enable(true);
         mcg_c2_erefs0Var.clearStatus();

         boolean oscillatorInUse = mcg_c2_erefs0Var.getValueAsBoolean();
         
         oscillatorRangeVar.enable(true);
         oscillatorRangeVar.clearStatus();

         if (oscillatorInUse) {
            // Using oscillator - range is chosen to suit crystal frequency (or forced by RTC)
            if (forceLowFrequencyRange) {
               LongVariable osc_input_freqVar = safeGetLongVariable("osc_input_freq");
               osc_input_freqVar.setMin(EXTERNAL_EXTAL_RANGE1_MIN);
               osc_input_freqVar.setMax(EXTERNAL_EXTAL_RANGE1_MAX);
            }
            if (forceLowFrequencyRange ||
                  ((osc_input_freq >= EXTERNAL_EXTAL_RANGE1_MIN) && (osc_input_freq <= EXTERNAL_EXTAL_RANGE1_MAX))) {
               oscclk_clockOrg += " (low range)";
               rangeOrigin      = "Determined by Crystal Frequency";
               range            = 0;
            }
            else if ((osc_input_freq >= EXTERNAL_EXTAL_RANGE2_MIN) && (osc_input_freq <= EXTERNAL_EXTAL_RANGE2_MAX)) {
               oscclk_clockOrg += " (high range)";
               rangeOrigin      = "Determined by Crystal Frequency";
               range            = 1;
            }
            else if ((osc_input_freq >= EXTERNAL_EXTAL_RANGE3_MIN) && (osc_input_freq <= EXTERNAL_EXTAL_RANGE3_MAX)) {
               oscclk_clockOrg += " (very high range)";
               rangeOrigin      = "Determined by Crystal Frequency";
               range            = 2;
            }
            else {
               // Not suitable as OSC Crystal frequency
               oscclk_clockOrg     += " (invalid range)";
               oscclk_clockStatus  = XTAL_CLOCK_RANGE_ERROR_MSG;
               range               = UNCONSTRAINED_RANGE;
            }
         }
         else {
            // Using external clock
            LongVariable osc_input_freqVar = safeGetLongVariable("osc_input_freq");
            osc_input_freqVar.setMin(EXTERNAL_EXTAL_RANGE1_MIN);
            osc_input_freqVar.setMax(EXTERNAL_CLOCK_MAX);

            oscclk_clockOrg += " (External clock)";

            // Range has no effect on Oscillator
            range           = UNCONSTRAINED_RANGE;

            // Check suitable clock range
            if (osc_input_freq>EXTERNAL_CLOCK_MAX) {
               // Not suitable as external clock
               oscclk_clockStatus = EXTERNAL_CLOCK_RANGE_ERROR_MSG;
            }
         }
         osc_cr_scpVar.enable(oscillatorInUse);
         osc_cr_scpVar.clearStatus();

         if (mcg_c2_hgo0Var != null) {
            mcg_c2_hgo0Var.enable(oscillatorInUse);
            mcg_c2_hgo0Var.clearStatus();
         }
      }

      boolean oscclkOK = (oscclk_clockStatus==null) || oscclk_clockStatus.getSeverity().lessThan(Severity.WARNING);

      osc_clockVar.setOrigin(oscclk_clockOrg);
      osc_clockVar.setStatus(oscclk_clockStatus);
      osc_clockVar.setValue(oscclkOK?osc_input_freq:0);
      osc_clockVar.enable(oscclkOK);

      // Check suitability of OSC for OSC32KCLK
      //=========================================
      // Initially assume suitable
      long    osc32kclk_clockFreq   = osc_clockVar.getValueAsLong();
      Status  osc32kclk_clockStatus = osc_clockVar.getStatus();
      String  osc32kclk_clockOrg    = osc_clockVar.getOrigin();
      
      if ((osc32kclk_clockFreq < EXTERNAL_EXTAL_RANGE1_MIN) || (osc32kclk_clockFreq > EXTERNAL_EXTAL_RANGE1_MAX)) {
         if ((osc32kclk_clockStatus==null)||osc32kclk_clockStatus.lessThan(Severity.ERROR)) {
            osc32kclk_clockStatus = new Status(OSCCLK32K_RANGE_ERROR_CLOCK_MSG, Severity.WARNING);
         }
         osc32kclk_clockOrg  = osc32kclk_clockOrg+"(invalid range for 32kHz clock)";
      }
      osc32k_clockVar.setValue((osc32kclk_clockStatus != null)?0:osc32kclk_clockFreq);
      osc32k_clockVar.setStatus(osc32kclk_clockStatus);
      osc32k_clockVar.setOrigin(osc32kclk_clockOrg);
   
      oscillatorRangeVar.setValue(range);
      oscillatorRangeVar.setOrigin(rangeOrigin);

      // Determine OSCERCLK, OSCERCLK_UNDIV
      //==================================
      long    osc_clockFreq   = osc_clockVar.getValueAsLong();
      String  osc_clockOrg    = osc_clockVar.getOrigin();

      if (osc_cr_erclkenVar.getValueAsBoolean()) {
         // Oscillator/clock enabled
         system_oscer_undiv_clockVar.setValue(osc_clockFreq);
         system_oscer_undiv_clockVar.setStatus(osc_clockVar.getFilteredStatus());
         system_oscer_undiv_clockVar.setOrigin(osc_clockVar.getOrigin());
         system_oscer_undiv_clockVar.enable(true);
         long system_oscerclk = osc_clockFreq;
         if (osc_div_erpsVar != null) {
            // If divider exists
            system_oscerclk /= 1<<osc_div_erpsVar.getValueAsLong();
            osc_clockOrg += "/osc_div_erps";
            system_oscer_clockVar.setValue(system_oscerclk);
            system_oscer_clockVar.setStatus(osc_clockVar.getFilteredStatus());
            system_oscer_clockVar.setOrigin(osc_clockVar.getOrigin());
            system_oscer_clockVar.enable(true);
            osc_div_erpsVar.enable(true);
         }
      }
      else {
         Status osc_crMessage = new Status("Disabled by osc_cr_erclken", Severity.OK);
         osc_clockOrg = osc_clockOrg + "\n[Disabled by osc_cr_erclken]";
         // Oscillator/clock disabled
         //         system_oscerclk_undiv_clockVar.setValue(0);
         system_oscer_undiv_clockVar.setStatus(osc_crMessage);
         system_oscer_undiv_clockVar.setOrigin(osc_clockOrg);
         system_oscer_undiv_clockVar.enable(false);
         if (osc_div_erpsVar != null) {
            //            system_oscer_clockVar.setValue(0);
            system_oscer_clockVar.setStatus(osc_crMessage);
            system_oscer_clockVar.setOrigin(osc_clockOrg);
            system_oscer_clockVar.enable(false);
            osc_div_erpsVar.enable(false);
         }
      }
   }
   
   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();
      
      ArrayList<String> externalVariablesList = new ArrayList<String>();
      
      // Some device only support low range XTAL
      forceLowFrequencyRange = safeGetVariable("/MCG/mcg_c2_range0") == null;
      
      // Some device only support low power OSC mode
      mcg_c2_hgo0Var         = safeGetBooleanVariable("/MCG/mcg_c2_hgo0");
      
      if (mcg_c2_hgo0Var != null) {
         mcg_c2_hgo0Var = createBooleanVariableReference("/MCG/mcg_c2_hgo0", externalVariablesList);
      }
      mcg_c2_erefs0Var               =  createBooleanVariableReference("/MCG/mcg_c2_erefs0", externalVariablesList);
      osc_cr_erclkenVar              =  getBooleanVariable("osc_cr_erclken");
      osc_cr_scpVar                  =  getChoiceVariable("osc_cr_scp");
      osc_cr_erefstenVar             =  getVariable("osc_cr_erefsten");
      oscillatorRangeVar             =  getVariable("oscillatorRange");
      system_oscer_undiv_clockVar    =  safeGetLongVariable("oscer_undiv_clock");
      system_oscer_clockVar          =  null;
      if (system_oscer_undiv_clockVar == null) {
         system_oscer_undiv_clockVar =  getLongVariable("oscer_clock");
      }
      else {
         system_oscer_clockVar       =  getLongVariable("oscer_clock");
      }
      osc_div_erpsVar                =  safeGetChoiceVariable("osc_div_erps");
      osc32k_clockVar                =  getLongVariable("osc32k_clock");
      osc_clockVar                   =  getLongVariable("osc_clock");
      osc_input_freqVar              =  getLongVariable("osc_input_freq");

      rtcSharesPins                  =  safeGetStringVariable("/SIM/rtc_shared") != null;

      rtc_cr_osceVar                 =  safeCreateBooleanVariableReference("/RTC/rtc_cr_osce", externalVariablesList);
      
      String[] externalVariables     = externalVariablesList.toArray(new String[externalVariablesList.size()]);

      addToWatchedVariables(externalVariables);
   }
}