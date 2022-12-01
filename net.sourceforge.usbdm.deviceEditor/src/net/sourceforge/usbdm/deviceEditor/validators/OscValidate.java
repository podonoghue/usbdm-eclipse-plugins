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
   private   static final long EXTERNAL_CLOCK_MIN        = 1L;
   private   static final long EXTERNAL_CLOCK_MAX        = 50000000L;

   /** External Crystal frequency error message */
   private   static final Status OSCCLK32K_RANGE_ERROR_CLOCK_MSG = new Status(String.format(
         "External crystal frequency not suitable for 32k Oscillator mode\n"+
               "Range [%sHz,%sHz]",
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3)),
         Severity.ERROR);

   /** External crystal frequency error message */
   private   static final Status XTAL_CLOCK_RANGE_ERROR_MSG = new Status(String.format(
         "External crystal frequency not suitable for oscillator\n"+
               "Permitted ranges [%sHz,%sHz], [%sHz,%sHz]",
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE2_MIN, 3),
               EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE3_MAX, 3)),
         Severity.ERROR);

   /** External clock frequency error message */
   private static final Status EXTERNAL_CLOCK_RANGE_ERROR_MSG = new Status(String.format(
         "External clock frequency is too high\nMax=%sHz",
         EngineeringNotation.convert(EXTERNAL_CLOCK_MAX, 3)),
         Severity.ERROR);

   private ChoiceVariable   oscModeVar                     =  null;
   private ChoiceVariable   osc_cr_scpVar                  =  null;
   private Variable         oscillatorRangeVar             =  null;
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
    * Check if a value is within a range
    * 
    * @param value
    * @param min
    * @param max
    * 
    * @return
    */
   static boolean inRange(long value, long min, long max) {
      return (value>=min) && (value<= max);
   }
   
   /**
    * Class to determine oscillator settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {

      super.validate(variable);

      // Check if RTC has control of oscillator pins
      boolean rtcForcing    = rtcSharesPins && rtc_cr_osceVar.getValueAsBoolean();
      
      String  rangeOrigin      = "Unused";
      int     range            = UNCONSTRAINED_RANGE;

      long    osc_input_freq   = osc_input_freqVar.getValueAsLong();
      long    osc_clockFreq    = 0;
      
      String  osc_clockOrg     = getPeripheral().getName();
      Status  osc_clockStatus  = null;
      
      if (rtcForcing) {
         // RTC controlling XTAL pins

         // Using external clock
         osc_input_freqVar.setMin(EXTERNAL_EXTAL_RANGE1_MIN);
         osc_input_freqVar.setMax(EXTERNAL_EXTAL_RANGE1_MAX);

         Status rtcInUseMessage = new Status("Feature is controlled by RTC which shares XTAL/EXTAL pins", Severity.WARNING);

         oscModeVar.enable(false);
         oscModeVar.setStatus(rtcInUseMessage);

         osc_clockFreq = osc32k_clockVar.getValueAsLong();

         osc_clockVar.setOrigin(osc32k_clockVar.getOrigin());
         osc_clockVar.setStatus(osc32k_clockVar.getStatus());
         osc_clockVar.setValue(osc_clockFreq);
         
         oscillatorRangeVar.enable(false);
         oscillatorRangeVar.setStatus(rtcInUseMessage);

         // Using oscillator - range is forced by RTC
         range            = 0;
         rangeOrigin      = "Forced by RTC";

         osc_clockOrg += " Forced by RTC";
         
         if (!inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE1_MIN, EXTERNAL_EXTAL_RANGE1_MAX)) {
            // Not suitable as OSC Crystal frequency
            osc_clockOrg     += " (invalid range)";
            osc_clockStatus = OSCCLK32K_RANGE_ERROR_CLOCK_MSG;
         }
         else {
            osc_clockOrg     += " (low range)";
         }
         osc_cr_scpVar.enable(false);
         osc_cr_scpVar.setStatus(rtcInUseMessage);
      }
      else {
         // OSC controlling XTAL pins

         oscModeVar.enable(true);
         oscModeVar.clearStatus();

         boolean oscillatorInUse = oscModeVar.getValueAsLong() != 0L;
         
         oscillatorRangeVar.enable(true);
         oscillatorRangeVar.clearStatus();

         if (oscillatorInUse) {
            // Complicated constraints
            osc_input_freqVar.setMin(EXTERNAL_EXTAL_RANGE1_MIN);
            osc_input_freqVar.setMax(EXTERNAL_EXTAL_RANGE3_MAX);
            
            // Using oscillator - range is chosen to suit crystal frequency (or forced by RTC)
            if (forceLowFrequencyRange && !inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE1_MIN, EXTERNAL_EXTAL_RANGE1_MAX)) {
               osc_clockOrg     += " (invalid range)";
               osc_clockStatus  = OSCCLK32K_RANGE_ERROR_CLOCK_MSG;
            }
            if (forceLowFrequencyRange || inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE1_MIN, EXTERNAL_EXTAL_RANGE1_MAX)) {
               osc_clockOrg += " (low range)";
               rangeOrigin      = "Determined by Crystal Frequency";
               range            = 0;
            }
            else if (inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE2_MIN, EXTERNAL_EXTAL_RANGE2_MAX)) {
               osc_clockOrg += " (high range)";
               rangeOrigin      = "Determined by Crystal Frequency";
               range            = 1;
            }
            else if (inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE3_MIN, EXTERNAL_EXTAL_RANGE3_MAX)) {
               osc_clockOrg += " (very high range)";
               rangeOrigin      = "Determined by Crystal Frequency";
               range            = 2;
            }
            else {
               // Not suitable as OSC Crystal frequency
               osc_clockOrg     += " (invalid range)";
               osc_clockStatus  = XTAL_CLOCK_RANGE_ERROR_MSG;
               range               = UNCONSTRAINED_RANGE;
            }
         }
         else {
            // Using external clock
            osc_input_freqVar.setMin(EXTERNAL_CLOCK_MIN);
            osc_input_freqVar.setMax(EXTERNAL_CLOCK_MAX);

            osc_clockOrg += " (External clock)";

            // Range has no effect on Oscillator
            range           = UNCONSTRAINED_RANGE;

            // Check suitable clock range
            if (osc_input_freq>EXTERNAL_CLOCK_MAX) {
               // Not suitable as external clock
               osc_clockStatus = EXTERNAL_CLOCK_RANGE_ERROR_MSG;
            }
         }
         osc_cr_scpVar.enable(oscillatorInUse);
         osc_cr_scpVar.clearStatus();
      }

      boolean osc_clockOK = (osc_clockStatus==null) || osc_clockStatus.getSeverity().lessThan(Severity.WARNING);

      osc_input_freqVar.setStatus(osc_clockStatus);

      if (!rtcForcing) {
         osc_clockFreq = osc_clockOK?osc_input_freq:0;
         osc_clockVar.setOrigin(osc_clockOrg);
         osc_clockVar.setStatus(osc_clockStatus);
         osc_clockVar.setValue(osc_clockFreq);
      }
      
      oscillatorRangeVar.setValue(range);
      oscillatorRangeVar.setOrigin(rangeOrigin);
   }
   
   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();
      
      ArrayList<String> externalVariablesList = new ArrayList<String>();
      
      // Some device only support low range XTAL
      forceLowFrequencyRange = safeGetVariable("/MCG/mcg_c2_range0") == null;
      
      oscModeVar                     =  getChoiceVariable("/MCG/oscMode");
      externalVariablesList.add(oscModeVar.getKey());
      osc_cr_scpVar                  =  getChoiceVariable("osc_cr_scp");
      oscillatorRangeVar             =  getVariable("oscillatorRange");
      osc32k_clockVar                =  getLongVariable("osc32k_clock");
      osc_clockVar                   =  getLongVariable("osc_clock");
      osc_input_freqVar              =  getLongVariable("osc_input_freq");

      rtcSharesPins                  =  safeGetStringVariable("/SIM/rtc_shared") != null;

      rtc_cr_osceVar                 =  safeCreateBooleanVariableReference("/RTC/rtc_cr_osce", externalVariablesList);
      
      addToWatchedVariables(externalVariablesList);
   }
}