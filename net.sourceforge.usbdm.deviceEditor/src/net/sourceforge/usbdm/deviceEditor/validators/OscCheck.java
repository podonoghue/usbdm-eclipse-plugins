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
      "Origin: OSCCLK32\n"+
      "External crystal frequency not suitable for OSCCLK32\n"+
      "Range [%sHz,%sHz]",           
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3)),
      Severity.ERROR);

   /** External crystal frequency error message */
   private   static final Message FLL_CLOCK_ERROR_MSG = new Message(String.format(
      "Origin: OSCCLK\n"+
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
      "Origin: OSCCLK\n"+
      "External clock frequency is too high\nMax=%sHz",
      EngineeringNotation.convert(EXTERNAL_CLOCK_MAX, 3)), 
      Severity.ERROR);

   /** External clock/oscillator frequency */
   public final long    oscclk_clock;
   
   /** Message indicating the suitability of the OSCCLK */
   public final Message oscclk_clockMessage;
   
   /** External clock/oscillator frequency */
   public final long    osc32kclk_clock;
   
   /** Message indicating the suitability of the OSCCLK */
   public final Message osc32kclk_clockMessage;
   
   /** Calculated MCG_CR2.RANGE value for oscillator, -1 if not used */ 
   public final int     mcg_c2_range;
   
   /**
    * Class to determine oscillator settings
    *  
    * @param pMcg_c2_erefs0      User selected clock/oscillator setting
    * @param oscclk_clock_freq   User selected frequency of external clock or crystal
    */
   OscCheck(boolean mcg_c2_erefs0, long oscclk_clock_freq) {
      
      Message oscclk_clockMsg    = new Message("Origin: OSCCLK",    Severity.OK);
      Message osc32kclk_clockMsg = new Message("Origin: OSC32KCLK", Severity.OK);
      int     range              = -1;
      
      // Check suitability of OSC for OSC32KCLK
      if ((oscclk_clock_freq < EXTERNAL_EXTAL_RANGE1_MIN) || (oscclk_clock_freq > EXTERNAL_EXTAL_RANGE1_MAX)) {
         osc32kclk_clockMsg = OSCCLK32K_CLOCK_ERROR_MSG;
      }
      // Using oscillator - range is chosen to suit Crystal frequency
      if (mcg_c2_erefs0) {
         // Using oscillator - range is chosen to suit Crystal frequency
         if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE1_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE1_MAX)) {
            range = 0;
         }
         else if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE2_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE2_MAX)) {
            range = 1;
         }
         else if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE3_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE3_MAX)) {
            range = 2;
         }
         else {
            // Not suitable as OSC crystal frequency
            oscclk_clockMsg    = FLL_CLOCK_ERROR_MSG;
         }
      }
      else {
         // Using external clock - check suitable range
         if (oscclk_clock_freq>EXTERNAL_CLOCK_MAX) {
            // Not suitable as external clock
            oscclk_clockMsg = CLOCK_RANGE_ERROR_MSG;
         }
      }
      if (osc32kclk_clockMsg.getSeverity().greaterThan(Severity.OK)) {
         osc32kclk_clock        = 0;
      }
      else {
         osc32kclk_clock        = oscclk_clock_freq;
      }
      osc32kclk_clockMessage = osc32kclk_clockMsg;

      if (oscclk_clockMsg.getSeverity().greaterThan(Severity.OK)) {
         oscclk_clock           = 0;
      }
      else {
         oscclk_clock           = oscclk_clock_freq;
      }
      oscclk_clockMessage = oscclk_clockMsg;
      mcg_c2_range        = range;
   }
}