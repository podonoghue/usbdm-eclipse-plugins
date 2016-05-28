package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

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
public class RtcValidate extends BaseClockValidator {
   
   /** Used for preliminary range determination */
   public static final String OSC_RANGE_KEY = "oscRange";
   
   // Ranges for External Crystal
   private   static final long EXTERNAL_EXTAL_RANGE_MIN = 32000L;
   private   static final long EXTERNAL_EXTAL_RANGE_MAX = 40000L;

   /** External Crystal frequency error message */
   private   static final Message OSCCLK32K_CLOCK_WARNING_MSG = new Message(String.format(
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
    */
   @Override
   public void validate() {
      
      // OSC
      //=================================
      Variable     rtc_cr_osceNode                  =  getVariable("rtc_cr_osce");
      Variable     rtcclk_clockNode                 =  getVariable("rtcclk_clock");
      Variable     rtc_cr_clkoNode                  =  getVariable("rtc_cr_clko");
      Variable     rtc_cr_scpNode                   =  getVariable("rtc_cr_scp");
      Variable     rtc_cr_umNode                    =  getVariable("rtc_cr_um");
      Variable     rtc_cr_supNode                   =  getVariable("rtc_cr_sup");
      Variable     rtc_cr_wpeNode                   =  getVariable("rtc_cr_wpe");
      Variable     rtc_IRQ_HANDLERNode              =  getVariable("IRQ_HANDLER");
      
//      Variable     sim_sopt1_osc32kselNode          =  getVariable("sim_sopt1_osc32ksel");
//      Variable     system_erclk32k_clockNode        =  getVariable("system_erclk32k_clock");
//      Variable     sim_sopt2_rtcclkoutselNode       =  getVariable("sim_sopt2_rtcclkoutsel");
//      Variable     system_rtc_clkoutNode            =  getVariable("system_rtc_clkout");

      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //
      boolean rtc_cr_osce = rtc_cr_osceNode.getValueAsBoolean();
      if (rtc_cr_osceNode.getValueAsBoolean()) {
         // Enabled
         // Check suitability of OSC for OSC32KCLK
         long rtcclk_clock_freq = rtcclk_clockNode.getValueAsLong();
         if ((rtcclk_clock_freq < EXTERNAL_EXTAL_RANGE_MIN) || (rtcclk_clock_freq > EXTERNAL_EXTAL_RANGE_MAX)) {
            rtcclk_clockNode.setOrigin("RTCCLK (invalid range)");
            rtcclk_clockNode.setStatus(OSCCLK32K_CLOCK_WARNING_MSG);
         }
         else {
            rtcclk_clockNode.setOrigin("RTCCLK");
            rtcclk_clockNode.setStatus((Message)null);
         }
      }
      else {
         // Disabled
         rtcclk_clockNode.setOrigin("RTCCLK (disabled)");
         rtcclk_clockNode.setStatus(new Message("RTCCLK Disabled by rtc_cr_osce", Severity.WARNING));
      }
      rtcclk_clockNode.enable(rtc_cr_osce);
      rtc_cr_scpNode.enable(rtc_cr_osce);
      rtc_cr_clkoNode.enable(rtc_cr_osce);
      rtc_cr_umNode.enable(rtc_cr_osce);
      rtc_cr_supNode.enable(rtc_cr_osce);
      rtc_cr_wpeNode.enable(rtc_cr_osce);
      rtc_IRQ_HANDLERNode.enable(rtc_cr_osce);
   }
}
