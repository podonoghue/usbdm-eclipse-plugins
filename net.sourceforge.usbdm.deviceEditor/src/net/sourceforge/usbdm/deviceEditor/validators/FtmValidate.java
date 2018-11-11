package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
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
    * Class to determine LPTMR settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      //=================================
      
      DoubleVariable    clockFrequencyVar          =  getDoubleVariable("clockFrequency");
      DoubleVariable    clockPeriodVar             =  getDoubleVariable("clockPeriod");
      ChoiceVariable    ftm_sc_clksVar             =  getChoiceVariable("ftm_sc_clks");
      ChoiceVariable    ftm_sc_psVar               =  getChoiceVariable("ftm_sc_ps");
      LongVariable      ftm_modVar                 =  getLongVariable("ftm_mod");
      DoubleVariable    ftm_mod_periodVar          =  getDoubleVariable("ftm_mod_period");
      BooleanVariable   ftm_sc_cpwmsVar            =  getBooleanVariable("ftm_sc_cpwms");
      
      Variable clockSourceVar = null;
      
      switch((int)ftm_sc_clksVar.getValueAsLong()) {
      case 0: 
         clockSourceVar = new LongVariable("Disabled", "/Ftm/Disabled");
         clockSourceVar.setOrigin("Disabled");
         clockSourceVar.setValue(0);
         break;
      default:
         ftm_sc_clksVar.setValue(1);
      case 1:
         clockSourceVar = getParameterSelectedVariable("/SIM/ftm"+getPeripheral().getInstance()+"_input1_clock", "/SIM/system_bus_clock");
         break;
      case 2:
         clockSourceVar = getParameterSelectedVariable("/SIM/ftm"+getPeripheral().getInstance()+"_input2_clock", "/MCG/system_mcgffclk_clock");
         break;
      case 3:
         clockSourceVar = getParameterSelectedVariable("/SIM/ftm"+getPeripheral().getInstance()+"_input3_clock", "ftmExternalClock");
         break;
      }
      double clockFrequency = clockSourceVar.getValueAsDouble();
      String clockOrigin    = clockSourceVar.getOrigin();

      clockFrequency = clockFrequency/(1L<<ftm_sc_psVar.getValueAsLong());
      
      clockFrequencyVar.setValue(clockFrequency);
      clockFrequencyVar.setOrigin("frequency("+clockOrigin + ")/prescaler");
      clockFrequencyVar.setStatus(clockSourceVar.getFilteredStatus());

      clockPeriodVar.setOrigin("period("+clockOrigin + ")*prescaler");
      clockPeriodVar.setStatus(clockSourceVar.getFilteredStatus());
      
      clockFrequencyVar.enable(clockFrequency != 0);
      clockPeriodVar.enable(clockFrequency != 0);
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
      final String[] externalVariables = {
            "/MCG/system_mcgffclk_clock",
            "/SIM/system_bus_clock",
      };
      addToWatchedVariables(externalVariables);
   }

}