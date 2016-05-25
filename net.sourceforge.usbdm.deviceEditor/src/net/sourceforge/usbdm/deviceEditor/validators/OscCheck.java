package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;

/**
 * Class to determine oscillator settings
 */
class OscCheck {
   
   // Ranges for External crystal
   private   static final long EXTERNAL_EXTAL_RANGE1_MIN = 32000L;
   private   static final long EXTERNAL_EXTAL_RANGE1_MAX = 40000L;
   
   private   static final long EXTERNAL_EXTAL_RANGE2_MIN = 3000000L;
   private   static final long EXTERNAL_EXTAL_RANGE2_MAX = 8000000L;
   
   private   static final long EXTERNAL_EXTAL_RANGE3_MIN = 8000000L;
   private   static final long EXTERNAL_EXTAL_RANGE3_MAX = 32000000L;

   // Maximum External Clock (not crystal)
   private   static final long EXTERNAL_CLOCK_MAX        = 50000000L;

   /** External crystal frequency error message */
   private   static final Message OSCCLK32K_CLOCK_ERROR_MSG = new Message(String.format(
      "External crystal frequency not suitable for OSCCLK32\n"+
      "Range [%sHz,%sHz]",           
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3)),
      Severity.ERROR);

   /** External crystal frequency error message */
   private   static final Message FLL_CLOCK_ERROR_MSG = new Message(String.format(
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
   private   static final Message CLOCK_RANGE_ERROR_MSG = new Message(String.format(
      "External clock frequency is too high\nMax=%sHz",
      EngineeringNotation.convert(EXTERNAL_CLOCK_MAX, 3)), 
      Severity.ERROR);

   /** External clock/oscillator frequency */
   public final long    oscclk_clock;
   
   /** Indicates the suitability of the OSCCLK */
   public final Message oscclk_clockStatus;
   
   /** Indicating the origin of the OSCCLK */
   public final String oscclk_clockOrigin;
   
   /** External clock/oscillator frequency */
   public final long    osc32kclk_clock;
   
   /** Indicates the suitability of the OSCCLK */
   public final Message osc32kclk_clockStatus;
   
   /** Indicating the origin of the OSC32KCLK */
   public final String osc32kclk_clockOrigin;
   
   /** Calculated MCG_CR2.RANGE value for oscillator, -1 if not used */ 
   public final int     mcg_c2_range;
   
   /**
    * Class to determine oscillator settings
    *  
    * @param pMcg_c2_erefs0      User selected clock/oscillator setting
    * @param oscclk_clock_freq   User selected frequency of external clock or crystal
    */
   OscCheck(boolean mcg_c2_erefs0, long oscclk_clock_freq) {
      
      Message oscclk_clockMsg    = null;
      String  oscclk_clockOrg    = null;
      
      Message osc32kclk_clockMsg = null;
      String  osc32kclk_clockOrg = "OSC32KCLK";

      int     range              = -1;

      // Check suitability of OSC for OSC32KCLK
      if ((oscclk_clock_freq < EXTERNAL_EXTAL_RANGE1_MIN) || (oscclk_clock_freq > EXTERNAL_EXTAL_RANGE1_MAX)) {
         osc32kclk_clockMsg = OSCCLK32K_CLOCK_ERROR_MSG;
         osc32kclk_clockOrg = "OSC32KCLK (invalid range)";
      }
      // Using oscillator - range is chosen to suit Crystal frequency
      if (mcg_c2_erefs0) {
         // Using oscillator - range is chosen to suit Crystal frequency
         if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE1_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE1_MAX)) {
            oscclk_clockOrg = "OSCCLK (low range oscillator)";
            range = 0;
         }
         else if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE2_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE2_MAX)) {
            oscclk_clockOrg = "OSCCLK (high range oscillator)";
            range = 1;
         }
         else if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE3_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE3_MAX)) {
            oscclk_clockOrg = "OSCCLK (very high range oscillator)";
            range = 2;
         }
         else {
            // Not suitable as OSC crystal frequency
            oscclk_clockOrg = "OSCCLK (invalid range)";
            oscclk_clockMsg    = FLL_CLOCK_ERROR_MSG;
         }
      }
      else {
         // Using external clock - check suitable range
         oscclk_clockOrg = "OSCCLK (External clock)";
         
         // Check suitable range
         if (oscclk_clock_freq>EXTERNAL_CLOCK_MAX) {
            // Not suitable as external clock
            oscclk_clockMsg = CLOCK_RANGE_ERROR_MSG;
         }
      }
      if (osc32kclk_clockMsg != null) {
         osc32kclk_clock       = 0;
      }
      else {
         osc32kclk_clock        = oscclk_clock_freq;
      }
      osc32kclk_clockOrigin = osc32kclk_clockOrg;
      osc32kclk_clockStatus = osc32kclk_clockMsg;

      if (oscclk_clockMsg != null) {
         oscclk_clock           = 0;
      }
      else {
         oscclk_clock           = oscclk_clock_freq;
      }
      oscclk_clockOrigin = oscclk_clockOrg;
      oscclk_clockStatus = oscclk_clockMsg;
      mcg_c2_range        = range;
   }
}