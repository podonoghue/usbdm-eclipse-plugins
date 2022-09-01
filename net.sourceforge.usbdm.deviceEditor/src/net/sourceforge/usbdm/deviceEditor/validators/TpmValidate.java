package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     osc0
 *     osc0_div
 */
public class TpmValidate extends PeripheralValidator {
   
   public TpmValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine TPM settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      //=================================
       
      LongVariable      clockFrequencyVar          =  getLongVariable("clockFrequency");
      DoubleVariable    clockPeriodVar             =  getDoubleVariable("clockPeriod");
      LongVariable      tpm_modVar                 =  getLongVariable("tpm_mod");
      DoubleVariable    tpm_mod_periodVar          =  getDoubleVariable("tpm_mod_period");
      BooleanVariable   tpm_sc_cpwmsVar            =  getBooleanVariable("tpm_sc_cpwms");
      
//      String clockOrigin    = clockFrequencyVar.getOrigin();
      long clockFrequency = clockFrequencyVar.getValueAsLong();
      
//      clockPeriodVar.setOrigin("period(" + clockOrigin + ")");
//      clockPeriodVar.setStatus(clockFrequencyVar.getFilteredStatus());
      
//      clockFrequencyVar.enable(clockFrequency != 0);
//      clockPeriodVar.enable(clockFrequency != 0);
      tpm_mod_periodVar.enable(clockFrequency != 0);

      if (clockFrequency != 0){
         long tpm_mod = tpm_modVar.getValueAsLong();

         double clockPeriod = 1.0/clockFrequency;
         clockPeriodVar.setValue(clockPeriod);
         
         boolean tpm_sc_cpwms = tpm_sc_cpwmsVar.getValueAsBoolean();
         
         double tpm_mod_period = clockPeriod * (tpm_sc_cpwms?(2*(tpm_mod)):((tpm_mod+1)));
         
         if (variable != null) {
            // Update selectively
            if (variable.equals(tpm_mod_periodVar)) {
               tpm_mod_period = tpm_mod_periodVar.getValueAsDouble();
               // Calculate rounded value
               if (tpm_sc_cpwms) {
                  tpm_mod        = Math.max(0, Math.round((tpm_mod_period/clockPeriod)/2));
               }
               else {
                  tpm_mod        = Math.max(0, Math.round((tpm_mod_period/clockPeriod)-1));
               }
               tpm_mod_period = clockPeriod * (tpm_sc_cpwms?(2*(tpm_mod)):((tpm_mod+1)));
               // Update
               tpm_modVar.setValue(tpm_mod);
            }
         }
         double tpm_mod_periodMax = clockPeriod * (tpm_sc_cpwms?(2*(65535.5)):((65536.5)));
         tpm_mod_periodVar.setValue(tpm_mod_period);
         tpm_mod_periodVar.setMax(tpm_mod_periodMax);
      }
   }

   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();

      final String[] externalVariables = {
            "/SIM/system_tpm_clock"
      };
      addToWatchedVariables(externalVariables);
   }

}