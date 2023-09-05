package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

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

   private ChoiceVariable   oscillatorRangeVar             =  null;
   private LongVariable     osc_input_freqVar              =  null;

   // Indicates that the OSC must use a 32kHz xtal = low range only
   private boolean onlyLowFrequencySupported;

   // Indicates OSC0 operating mode
   private ChoiceVariable oscModeVar = null;

   private long OscMode_NotConfigured;

   private long OscMode_RTC_Controlled;

   public OscValidate(PeripheralWithState peripheral) {
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

      Status  osc_clockStatus  = null;

      int     oscillatorRange       = UNCONSTRAINED_RANGE;
      boolean oscillatorRangeEnable = false;
      Status  oscillatorRangeStatus = null;
      String  oscillatorRangeOrigin = "Unused";

      long    oscMode          = oscModeVar.getValueAsLong();
      long    osc_input_freq   = osc_input_freqVar.getValueAsLong();
      
      if (oscMode == OscMode_NotConfigured) {
      }
      else if (oscMode == OscMode_RTC_Controlled) {
         // RTC controlling XTAL pins

         // Using low range crystal
         osc_input_freqVar.setMin(EXTERNAL_EXTAL_RANGE1_MIN);
         osc_input_freqVar.setMax(EXTERNAL_EXTAL_RANGE1_MAX);

         // Using oscillator - range is forced by RTC
         oscillatorRange       = 0;
         oscillatorRangeOrigin = "Forced by RTC";
         oscillatorRangeStatus = new Status("Feature is controlled by RTC which shares XTAL/EXTAL pins", Severity.WARNING);

         if (!inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE1_MIN, EXTERNAL_EXTAL_RANGE1_MAX)) {
            // Not suitable as OSC Crystal frequency
            osc_clockStatus = OSCCLK32K_RANGE_ERROR_CLOCK_MSG;
         }
      }
      else {
         // OSC controlling XTAL pins

         boolean oscillatorInUse = oscModeVar.getValueAsLong() != 0L;
         
         if (oscillatorInUse) {
            
            // Complicated constraints
            osc_input_freqVar.setMin(EXTERNAL_EXTAL_RANGE1_MIN);
            osc_input_freqVar.setMax(EXTERNAL_EXTAL_RANGE3_MAX);
            
            oscillatorRangeEnable = true;
            
            // Using oscillator - range is chosen to suit crystal frequency (or forced by RTC)
            if (onlyLowFrequencySupported && !inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE1_MIN, EXTERNAL_EXTAL_RANGE1_MAX)) {
               osc_clockStatus  = OSCCLK32K_RANGE_ERROR_CLOCK_MSG;
            }
            if (onlyLowFrequencySupported || inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE1_MIN, EXTERNAL_EXTAL_RANGE1_MAX)) {
               oscillatorRangeOrigin      = "Determined by Crystal Frequency";
               oscillatorRange  = 0;
            }
            else if (inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE2_MIN, EXTERNAL_EXTAL_RANGE2_MAX)) {
               oscillatorRangeOrigin      = "Determined by Crystal Frequency";
               oscillatorRange  = 1;
            }
            else if (inRange(osc_input_freq, EXTERNAL_EXTAL_RANGE3_MIN, EXTERNAL_EXTAL_RANGE3_MAX)) {
               oscillatorRangeOrigin      = "Determined by Crystal Frequency";
               oscillatorRange  = 2;
            }
            else {
               // Not suitable as OSC Crystal frequency
               osc_clockStatus  = XTAL_CLOCK_RANGE_ERROR_MSG;
               oscillatorRange  = UNCONSTRAINED_RANGE;
            }
         }
         else {
            // Using external clock
            osc_input_freqVar.setMin(EXTERNAL_CLOCK_MIN);
            osc_input_freqVar.setMax(EXTERNAL_CLOCK_MAX);

            // Range has no effect on Oscillator
            oscillatorRange = UNCONSTRAINED_RANGE;

            // Check suitable clock range
            if (osc_input_freq>EXTERNAL_CLOCK_MAX) {
               // Not suitable as external clock
               osc_clockStatus = EXTERNAL_CLOCK_RANGE_ERROR_MSG;
            }
         }
      }

      osc_input_freqVar.setStatus(osc_clockStatus);
      
      boolean changed = false;
      changed = oscillatorRangeVar.enableQuietly(oscillatorRangeEnable)    || changed;
      changed = oscillatorRangeVar.setStatusQuietly(oscillatorRangeStatus) || changed;
      changed = oscillatorRangeVar.setValueQuietly(oscillatorRange)        || changed;
      changed = oscillatorRangeVar.setOriginQuietly(oscillatorRangeOrigin) || changed;
      if (changed) {
         oscillatorRangeVar.notifyListeners();
      }
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
     
      ArrayList<String> externalVariablesList = new ArrayList<String>();
      
      // Some device only support low range XTAL
      onlyLowFrequencySupported      =  safeGetVariable("/MCG/mcg_c2_range0[]") == null;

      // Constants
      OscMode_NotConfigured          =  getLongVariable("/OscMode_NotConfigured").getValueAsLong();
      OscMode_RTC_Controlled         =  getLongVariable("/OscMode_RTC_Controlled").getValueAsLong();
      
      // Inputs
      oscModeVar                     =  getChoiceVariable("/MCG/mcg_c2_oscmode", externalVariablesList);
      
      // Inout
      osc_input_freqVar              =  getLongVariable("osc_input_freq", externalVariablesList);

      // Output
      oscillatorRangeVar             =  getChoiceVariable("oscillatorRange");

      externalVariablesList.add("mcg_c2_oscmode");
      
      addToWatchedVariables(externalVariablesList);
      
      // Don't add default dependencies
      return false;
   }
}