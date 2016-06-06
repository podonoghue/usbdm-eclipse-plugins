package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     osc0
 *     osc0_div
 */
public class LptmrValidate extends Validator {
   
   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/MCG/system_mcgirclk_clock",
         "/MCG/system_low_power_clock",
         "/SIM/system_erclk32k_clock",
         "/OSC0/system_oscerclk_clock",
   };

   public LptmrValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine LPTMR settings
    */
   @Override
   public void validate(Variable variable) {
      
      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      // OSC
      //=================================
      DoubleVariable    clockFrequencyVar          =  (DoubleVariable) getVariable("clockFrequency");
      DoubleVariable    clockPeriodVar             =  (DoubleVariable) getVariable("clockPeriod");
      Variable          lptmr_psr_pcsVar           =  getVariable("lptmr_psr_pcs");
      Variable          lptmr_psr_prescalerVar     =  getVariable("lptmr_psr_prescaler");
      Variable          lptmr_cmrVar               =  getVariable("lptmr_cmr");
      Variable          lptmr_psr_pbypVar          =  getVariable("lptmr_psr_pbyp");
//      Variable     system_mcgirclk_clockVar   =  getVariable("/MCG/system_mcgirclk_clock");
//      Variable     system_low_power_clockVar  =  getVariable("/MCG/system_low_power_clock");
//      Variable     system_erclk32k_clockVar   =  getVariable("/SIM/system_erclk32k_clock");
//      Variable     system_oscerclk_clockVar   =  getVariable("/OSC0/system_oscerclk_clock");
      
      if (variable == lptmr_cmrVar) {
         
      }
      Variable clockSource = null;
      
      double clockFrequency;
      
      switch((int)lptmr_psr_pcsVar.getValueAsLong()) {
      default:
         lptmr_psr_pcsVar.setValue(0);
      case 0: 
         clockSource = getVariable("/MCG/system_mcgirclk_clock");
         break;
      case 1:
         clockSource = getVariable("/MCG/system_low_power_clock");
         break;
      case 2:
         clockSource = getVariable("/SIM/system_erclk32k_clock");
         break;
      case 3:
         clockSource = getVariable("/OSC0/system_oscerclk_clock");
         break;
      }
      clockFrequency = clockSource.getValueAsLong();
      if (lptmr_psr_pbypVar.getValueAsBoolean()) {
         lptmr_psr_prescalerVar.enable(false);
         lptmr_psr_prescalerVar.setOrigin("Disabled by lptmr_psr_pbyp");
      }
      else {
         lptmr_psr_prescalerVar.enable(true);
         clockFrequency = clockFrequency/(1L<<(lptmr_psr_prescalerVar.getValueAsLong()+1));
         lptmr_psr_prescalerVar.setOrigin(null);
      }
      clockFrequencyVar.setValue(clockFrequency);
      clockFrequencyVar.setOrigin(clockSource.getOrigin());
      clockFrequencyVar.setStatus(clockSource.getStatus());
      clockPeriodVar.setOrigin(clockSource.getOrigin());
      clockPeriodVar.setStatus(clockSource.getStatus());
      if (clockFrequency == 0) {
         clockFrequencyVar.enable(false);
         clockPeriodVar.enable(false);
         clockPeriodVar.setValue(0.0);
      }
      else {
         clockFrequencyVar.enable(true);
         clockPeriodVar.enable(true);
         clockPeriodVar.setValue(1/clockFrequency);
      }
   }

}