package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
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
   public void validate() {
      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      // OSC
      //=================================
      Variable     clockFrequencyVar          =  getVariable("clockFrequency");
      Variable     lptmr_psr_pcsVar           =  getVariable("lptmr_psr_pcs");
      Variable     lptmr_psr_prescalerVar     =  getVariable("lptmr_psr_prescaler");
      Variable     periodVar                  =  getVariable("period");
//      Variable     system_mcgirclk_clockVar   =  getVariable("/MCG/system_mcgirclk_clock");
//      Variable     system_low_power_clockVar  =  getVariable("/MCG/system_low_power_clock");
//      Variable     system_erclk32k_clockVar   =  getVariable("/SIM/system_erclk32k_clock");
//      Variable     system_oscerclk_clockVar   =  getVariable("/OSC0/system_oscerclk_clock");
      
      double clockFrequency;
      
      switch((int)lptmr_psr_pcsVar.getValueAsLong()) {
      default:
         lptmr_psr_pcsVar.setValue(0);
      case 0: 
         clockFrequency = getVariable("/MCG/system_mcgirclk_clock").getValueAsLong();
         break;
      case 1:
         clockFrequency = getVariable("/MCG/system_low_power_clock").getValueAsLong();
         break;
      case 2:
         clockFrequency = getVariable("/SIM/system_erclk32k_clock").getValueAsLong();
         break;
      case 3:
         clockFrequency = getVariable("/OSC0/system_oscerclk_clock").getValueAsLong();
         break;
      }
      clockFrequency = clockFrequency/(1L<<(lptmr_psr_prescalerVar.getValueAsLong()+1));
      clockFrequencyVar.setValue(EngineeringNotation.convert(clockFrequency, 5));
   }

   @Override
   public boolean variableChanged(Variable variable) {
      validate();
      return false;
   }
}