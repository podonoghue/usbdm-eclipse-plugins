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
public class FtmValidate extends Validator {
   
   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/MCG/system_mcgffclk_clock",
         "/SIM/system_bus_clock",
   };

   public FtmValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
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
      Variable          ftm_sc_clksVar             =  getVariable("ftm_sc_clks");
      Variable          ftm_sc_psVar               =  getVariable("ftm_sc_ps");
      
      Variable clockSource = null;
      
      double clockFrequency;
      
      switch((int)ftm_sc_clksVar.getValueAsLong()) {
      case 0: 
         clockSource = null;
         break;
      default:
         ftm_sc_clksVar.setValue(1);
      case 1:
         clockSource = getVariable("/SIM/system_bus_clock");
         break;
      case 2:
         clockSource = getVariable("/MCG/system_mcgffclk_clock");
         break;
      case 3:
         clockSource = getVariable("ftmExternalClock");
         break;
      }
      if (clockSource == null){
         clockFrequencyVar.setValue(0.0);
         clockFrequencyVar.enable(false);
         clockFrequencyVar.setOrigin("Disabled");
         clockPeriodVar.enable(false);
         clockPeriodVar.setValue(0.0);
         clockPeriodVar.setOrigin("Disabled");
      }
      else {
         clockFrequency = clockSource.getValueAsLong();
         clockFrequency = clockFrequency/(1L<<ftm_sc_psVar.getValueAsLong());
         clockFrequencyVar.setValue(clockFrequency);
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
         clockFrequencyVar.setOrigin(clockSource.getOrigin());
         clockFrequencyVar.setStatus(clockSource.getStatus());
         clockPeriodVar.setOrigin(clockSource.getOrigin());
         clockPeriodVar.setStatus(clockSource.getStatus());
      }
   }

}