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
public class FtmValidate extends PeripheralValidator {
   
   public FtmValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine FTM settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      //=================================
      
      LongVariable      clockFrequencyVar          =  getLongVariable("clockFrequency");
      DoubleVariable    clockPeriodVar             =  getDoubleVariable("clockPeriod");
      LongVariable      ftm_modVar                 =  getLongVariable("ftm_mod");
      DoubleVariable    ftm_mod_periodVar          =  getDoubleVariable("ftm_mod_period");
      BooleanVariable   ftm_sc_cpwmsVar            =  getBooleanVariable("ftm_sc_cpwms");
      
//      String clockOrigin    = clockFrequencyVar.getOrigin();
      long clockFrequency = clockFrequencyVar.getValueAsLong();
      
//      clockPeriodVar.setOrigin("period(" + clockOrigin + ")");
//      clockPeriodVar.setStatus(clockFrequencyVar.getFilteredStatus());
      
//      clockFrequencyVar.enable(clockFrequency != 0);
//      clockPeriodVar.enable(clockFrequency != 0);
      ftm_mod_periodVar.enable(clockFrequency != 0);

      if (clockFrequency != 0){
         long ftm_mod = ftm_modVar.getValueAsLong();

         double clockPeriod = 1.0/clockFrequency;
         clockPeriodVar.setValue(clockPeriod);
         
         boolean ftm_sc_cpwms = ftm_sc_cpwmsVar.getValueAsBoolean();
         
         double ftm_mod_period = clockPeriod * (ftm_sc_cpwms?(2*(ftm_mod)):((ftm_mod+1)));
         
         if (variable != null) {
            // Update selectively
            if (variable.equals(ftm_mod_periodVar)) {
               ftm_mod_period = ftm_mod_periodVar.getValueAsDouble();
               // Calculate rounded value
               if (ftm_sc_cpwms) {
                  ftm_mod        = Math.max(0, Math.round((ftm_mod_period/clockPeriod)/2));
               }
               else {
                  ftm_mod        = Math.max(0, Math.round((ftm_mod_period/clockPeriod)-1));
               }
               ftm_mod_period = clockPeriod * (ftm_sc_cpwms?(2*(ftm_mod)):((ftm_mod+1)));
               // Update
               ftm_modVar.setValue(ftm_mod);
            }
         }
         double ftm_mod_periodMax = clockPeriod * (ftm_sc_cpwms?(2*(65535.5)):((65536.5)));
         ftm_mod_periodVar.setValue(ftm_mod_period);
         ftm_mod_periodVar.setMax(ftm_mod_periodMax);
      }
   }

   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();

      final String[] externalVariables = {
            "/MCG/system_mcgffclk_clock",
            "/SIM/system_bus_clock",
      };
      addToWatchedVariables(externalVariables);
   }

}