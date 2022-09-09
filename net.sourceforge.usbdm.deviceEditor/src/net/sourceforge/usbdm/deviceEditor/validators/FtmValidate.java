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
      DoubleVariable    ftm_modPeriodVar           =  getDoubleVariable("ftm_modPeriod");
      BooleanVariable   ftm_sc_cpwmsVar            =  getBooleanVariable("ftm_sc_cpwms");
      
      long clockFrequency = clockFrequencyVar.getValueAsLong();
      
      ftm_modPeriodVar.enable(clockFrequency != 0);

      if (clockFrequency != 0){
         long   ftm_mod       = ftm_modVar.getValueAsLong();
         double ftm_modPeriod = ftm_modPeriodVar.getValueAsDouble();

         double clockPeriod = 1.0/clockFrequency;
         clockPeriodVar.setValue(clockPeriod);
         
         boolean ftm_sc_cpwms = ftm_sc_cpwmsVar.getValueAsBoolean();
         
         if (variable != null) {
            // Update selectively
            if (variable.equals(ftm_modPeriodVar)) {
               // Calculate rounded value
               if (ftm_sc_cpwms) {
                  ftm_mod        = Math.max(0, Math.round((ftm_modPeriod/clockPeriod)/2));
               }
               else {
                  ftm_mod        = Math.max(0, Math.round((ftm_modPeriod/clockPeriod)-1));
               }
               ftm_modPeriod = clockPeriod * (ftm_sc_cpwms?(2*(ftm_mod)):((ftm_mod+1)));
               // Update
               ftm_modVar.setValue(ftm_mod);
               return;
            }
            if (variable.equals(ftm_modVar)) {
               // Calculate rounded value
               if (ftm_sc_cpwms) {
                  ftm_modPeriod  = 2*ftm_mod*clockPeriod;
               }
               else {
                  ftm_modPeriod  = (ftm_mod+1)*clockPeriod;
               }
               // Update
               ftm_modPeriodVar.setValue(ftm_modPeriod);
               return;
            }
         }
         ftm_modPeriod = clockPeriod * (ftm_sc_cpwms?(2*(ftm_mod)):((ftm_mod+1)));
         double ftm_modPeriodMax = clockPeriod * (ftm_sc_cpwms?(2*(65535.5)):((65536.5)));
         ftm_modPeriodVar.setValue(ftm_modPeriod);
         ftm_modPeriodVar.setMax(ftm_modPeriodMax);
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