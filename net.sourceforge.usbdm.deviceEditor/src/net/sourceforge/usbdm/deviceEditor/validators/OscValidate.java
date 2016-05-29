package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     osc0
 *     osc0_div
 */
public class OscValidate extends BaseClockValidator {
   
   /** Used for preliminary range determination */
   public static final String OSC_RANGE_KEY = "/_Temp_/OSC/oscRange";
   
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
   private   static final Message OSCCLK32K_CLOCK_WARNING_MSG = new Message(String.format(
      "External crystal frequency not suitable for OSCCLK32\n"+
      "Range [%sHz,%sHz]",           
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MIN, 3),
      EngineeringNotation.convert(EXTERNAL_EXTAL_RANGE1_MAX, 3)),
      Severity.WARNING);

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
      Severity.WARNING);

   /** External clock frequency error message */
   private   static final Message CLOCK_RANGE_ERROR_MSG = new Message(String.format(
      "External clock frequency is too high\nMax=%sHz",
      EngineeringNotation.convert(EXTERNAL_CLOCK_MAX, 3)), 
      Severity.WARNING);

   
   public OscValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
      
      fPeripheral.addVariable(new LongVariable(OSC_RANGE_KEY, fPeripheral.makeKey(OSC_RANGE_KEY)));
   }

   /**
    * Class to determine oscillator settings
    */
   @Override
   public void validate() {
      
      // OSC
      //=================================
      Variable     osc_cr_erclkenNode               =  getVariable("osc_cr_erclken");
      Variable     erefs0Node                       =  getVariable("erefs0");
      Variable     hgo0Node                         =  getVariable("hgo0");
      Variable     osc_cr_scpNode                   =  getVariable("osc_cr_scp");
//    Variable     osc_cr_erefstenNode              =  getVariable("osc_cr_erefsten");
      Variable     rangeNode                        =  getVariable(OSC_RANGE_KEY);
      Variable     system_oscerclk_undiv_clockNode  =  safeGetVariable("system_oscerclk_undiv_clock");
      Variable     system_oscerclk_clockNode        =  null;
      if (system_oscerclk_undiv_clockNode == null) {
         system_oscerclk_undiv_clockNode            = getVariable("system_oscerclk_clock");
      }
      else {
         system_oscerclk_clockNode                  =  getVariable("system_oscerclk_clock");
      }
      Variable     osc_div_erpsNode                 =  safeGetVariable("osc_div_erps");
      Variable     osc32kclk_clockNode              =  getVariable("osc32kclk_clock");
      Variable     oscclk_clockNode                 =  getVariable("oscclk_clock");
      
      boolean  erefs0 = erefs0Node.getValueAsBoolean();
      long     oscclk_clock  = oscclk_clockNode.getValueAsLong();

      hgo0Node.enable(erefs0);
      osc_cr_scpNode.enable(erefs0);
      rangeNode.enable(erefs0);
      
      //==========================================
      
      Message oscclk_clockStatus    = null;
      String  oscclk_clockOrg    = null;
      
      Message osc32kclk_clockStatus = null;
      String  osc32kclk_clockOrg = "OSC32KCLK";
      
      int     range        = -1;
      String  rangeOrigin  = null;
      
      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //
      long oscclk_clock_freq = oscclk_clockNode.getValueAsLong();
      // Check suitability of OSC for OSC32KCLK
      if ((oscclk_clock_freq < EXTERNAL_EXTAL_RANGE1_MIN) || (oscclk_clock_freq > EXTERNAL_EXTAL_RANGE1_MAX)) {
         osc32kclk_clockStatus = OSCCLK32K_CLOCK_WARNING_MSG;
         osc32kclk_clockOrg = "OSC32KCLK (invalid range)";
      }
      // Using oscillator - range is chosen to suit crystal frequency
      if (erefs0) {
         // Using oscillator - range is chosen to suit crystal frequency
         if ((oscclk_clock_freq >= EXTERNAL_EXTAL_RANGE1_MIN) && (oscclk_clock_freq <= EXTERNAL_EXTAL_RANGE1_MAX)) {
            oscclk_clockOrg = "OSCCLK (low range oscillator)";
            rangeOrigin     = "Determined by Crystal Frequency";
            range           = 0;
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
            range           = -1;
         }
      }
      else {
         // Using external clock
         oscclk_clockOrg = "OSCCLK (External clock)";
         
         // Range has no effect on Oscillator
         range           = -1;
         
         // Check suitable clock range
         if (oscclk_clock_freq>EXTERNAL_CLOCK_MAX) {
            // Not suitable as external clock
            oscclk_clockStatus = CLOCK_RANGE_ERROR_MSG;
         }
      }
      oscclk_clockNode.setOrigin(oscclk_clockOrg);
      oscclk_clockNode.setStatus(oscclk_clockStatus);
      
      erefs0Node.setStatus(oscclk_clockStatus);
      rangeNode.setValue(range);
      rangeNode.setOrigin(rangeOrigin);
      
      // Determine OSCERCLK, OSCERCLK_UNDIV 
      //==================================
      if (osc_cr_erclkenNode.getValueAsBoolean()) {
         // Oscillator/clock enabled
         system_oscerclk_undiv_clockNode.setValue(oscclk_clock);
         system_oscerclk_undiv_clockNode.setStatus(oscclk_clockStatus);
         system_oscerclk_undiv_clockNode.setOrigin(oscclk_clockOrg);
         system_oscerclk_undiv_clockNode.enable(true);
         long system_oscerclk = oscclk_clock;
         if (osc_div_erpsNode != null) {
            // If divider exists
            system_oscerclk /= 1<<osc_div_erpsNode.getValueAsLong();
            oscclk_clockOrg += "/osc_div_erps";
            system_oscerclk_clockNode.setValue(system_oscerclk);
            system_oscerclk_clockNode.setStatus(oscclk_clockStatus);
            system_oscerclk_clockNode.setOrigin(oscclk_clockOrg);
            system_oscerclk_clockNode.enable(true);
         }
         osc32kclk_clockNode.setValue((osc32kclk_clockStatus != null)?0:oscclk_clock_freq);
         osc32kclk_clockNode.setStatus(osc32kclk_clockStatus);
         osc32kclk_clockNode.setOrigin(osc32kclk_clockOrg);
         osc32kclk_clockNode.enable(true);
      }
      else {
         Message osc_crMessage = new Message("Disabled by osc_cr_erclken", Severity.OK);
         // Oscillator/clock disabled
//         system_oscerclk_undiv_clockNode.setValue(0);
         system_oscerclk_undiv_clockNode.setStatus(osc_crMessage);
         system_oscerclk_undiv_clockNode.setOrigin(oscclk_clockOrg);
         system_oscerclk_undiv_clockNode.enable(false);
         if (osc_div_erpsNode != null) {
//            system_oscerclk_clockNode.setValue(0);
            system_oscerclk_clockNode.setStatus(osc_crMessage);
            system_oscerclk_clockNode.setOrigin(oscclk_clockOrg);
            system_oscerclk_clockNode.enable(false);
         }
         osc32kclk_clockNode.setStatus(osc_crMessage);
         osc32kclk_clockNode.setOrigin(osc32kclk_clockOrg);
         osc32kclk_clockNode.enable(false);
      }
   }
}