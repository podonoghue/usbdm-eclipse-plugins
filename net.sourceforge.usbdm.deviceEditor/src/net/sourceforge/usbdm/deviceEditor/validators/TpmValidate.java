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
public class TpmValidate extends PeripheralValidator {
   
   private final static String[] externalVariables = {
         "/SIM/system_tpm_clock"
   };

   public TpmValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine LPTMR settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      addToWatchedVariables(externalVariables);
      
      // OSC
      //=================================
      
      Variable          tpmExternalClockVar        =  getVariable("tpmExternalClock");
      Variable          system_tpm_clockVar        =  getVariable("/SIM/system_tpm_clock");
      DoubleVariable    clockFrequencyVar          =  (DoubleVariable) getVariable("clockFrequency");
      DoubleVariable    clockPeriodVar             =  (DoubleVariable) getVariable("clockPeriod");
      DoubleVariable    maximumPeriodVar           =  (DoubleVariable) getVariable("maximumPeriod");
      Variable          tpm_sc_cmodVar             =  getVariable("tpm_sc_cmod");
      Variable          tpm_sc_psVar               =  getVariable("tpm_sc_ps");
      
      Variable clockSource = null;
      
      double clockFrequency;
      
      switch((int)tpm_sc_cmodVar.getValueAsLong()) {
      case 0: 
         clockSource = null;
         break;
      default:
         tpm_sc_cmodVar.setValue(1);
      case 1:
         clockSource = system_tpm_clockVar;
         break;
      case 2:
         clockSource = tpmExternalClockVar;
         break;
      case 3:
         clockSource = null;
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
         clockFrequency = clockFrequency/(1L<<tpm_sc_psVar.getValueAsLong());
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
         maximumPeriodVar.setValue(clockPeriodVar.getValueAsDouble()*65536);
         clockFrequencyVar.setOrigin(clockSource.getOrigin());
         clockFrequencyVar.setStatus(clockSource.getFilteredStatus());
         clockPeriodVar.setOrigin(clockSource.getOrigin());
         clockPeriodVar.setStatus(clockSource.getFilteredStatus());
      }
   }

}