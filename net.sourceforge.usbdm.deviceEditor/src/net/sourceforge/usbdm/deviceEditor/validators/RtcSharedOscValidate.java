package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     osc0
 *     osc0_div
 */
public class RtcSharedOscValidate extends Validator {
   
   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/SIM/system_erclk32k_clock",
   };
   
   public RtcSharedOscValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine RTC oscillator settings
    * 
    * Outputs rtcclk_clock, rtcclk_gated_clock, 
    */
   @Override
   public void validate(Variable variable) {
      
      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      // RTC input clock
      //=====================
      Variable system_erclk32k_clockVar = getVariable("/SIM/system_erclk32k_clock");
      
      // RTC
      //=================================
      Variable     rtc_cr_osceVar                  =  getVariable("rtc_cr_osce");
      Variable     rtc_cr_clkoVar                  =  getVariable("rtc_cr_clko");
      Variable     rtc_cr_scpVar                   =  getVariable("rtc_cr_scp");
      Variable     rtc_cr_umVar                    =  getVariable("rtc_cr_um");
      Variable     rtc_cr_supVar                   =  getVariable("rtc_cr_sup");
      Variable     rtc_cr_wpeVar                   =  getVariable("rtc_cr_wpe");

      Variable     rtc_1hz_clockVar                =  getVariable("rtc_1hz_clock");

      Variable     rtc_irqHandlerVar               =  getVariable("irqHandler");
      Variable     rtc_irqLevelVar                 =  getVariable("irqLevel");

      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //
      // Check suitability of OSC for OSC32KCLK
      
      boolean rtc_cr_osce = rtc_cr_osceVar.getValueAsBoolean();
      rtc_cr_scpVar.enable(rtc_cr_osce);
      rtc_cr_umVar.enable(rtc_cr_osce);
      rtc_cr_supVar.enable(rtc_cr_osce);
      rtc_cr_wpeVar.enable(rtc_cr_osce);
      rtc_irqHandlerVar.enable(rtc_cr_osce);
      rtc_irqLevelVar.enable(rtc_cr_osce);
      
      // RTC Clocks
      //==============================
      
      if (rtc_cr_clkoVar.getValueAsBoolean()) {
         rtc_1hz_clockVar.enable(true);
         rtc_1hz_clockVar.setValue((system_erclk32k_clockVar.getValueAsLong()>0)?1:0);
         rtc_1hz_clockVar.setStatus(system_erclk32k_clockVar.getFilteredStatus());
      }
      else {
         rtc_1hz_clockVar.enable(false);
         rtc_1hz_clockVar.setValue(0);
         rtc_1hz_clockVar.setStatus(new Message("Disabled by rtc_cr_clko", Severity.WARNING));
      }
   }
}
