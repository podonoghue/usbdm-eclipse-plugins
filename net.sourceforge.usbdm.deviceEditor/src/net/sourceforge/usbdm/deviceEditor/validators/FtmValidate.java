package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
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
//      DoubleVariable    clockPeriodVar             =  getDoubleVariable("clockPeriod");
      LongVariable      ftm_modVar                 =  getLongVariable("ftm_mod");
      LongVariable      ftm_cntinVar               =  getLongVariable("ftm_cntin");
      DoubleVariable    ftm_modPeriodVar           =  getDoubleVariable("ftm_modPeriod");
      BooleanVariable   ftm_sc_cpwmsVar            =  getBooleanVariable("ftm_sc_cpwms");
      
      long clockFrequency = clockFrequencyVar.getValueAsLong();
      
      ftm_modPeriodVar.enable(clockFrequency != 0);

      if (clockFrequency != 0){
         long   ftm_mod       = ftm_modVar.getValueAsLong();
         long   ftm_cntin     = ftm_cntinVar.getValueAsLong();
         double ftm_modPeriod = ftm_modPeriodVar.getValueAsDouble();

         double clockPeriod = 1.0/clockFrequency;
//         clockPeriodVar.setValue(clockPeriod);
         
         boolean ftm_sc_cpwms = ftm_sc_cpwmsVar.getValueAsBoolean();
         
         ftm_modVar.setMin((ftm_cntin==0)?0:ftm_cntin+1);
         ftm_cntinVar.setMax((ftm_mod==0)?0:ftm_mod-1);
         
         // These updates involves a loop so suppress initially
         if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
            if (variable != null) {
               if (variable.equals(ftm_modPeriodVar)) {
                  // Calculate rounded value
                  if (ftm_sc_cpwms) {
                     ftm_mod        = Math.max(0, Math.round((ftm_modPeriod/clockPeriod)/2)) + ftm_cntin;
                  }
                  else {
                     ftm_mod        = Math.max(0, Math.round((ftm_modPeriod/clockPeriod)-1)) + ftm_cntin;
                  }
                  // Update
                  ftm_modVar.setValue(ftm_mod);
                  if (ftm_mod <= ftm_cntin) {
                     ftm_modVar.setValue(ftm_cntin+1);
                  }
                  return;
               }
            }
         }
         ftm_modPeriod = clockPeriod * (ftm_sc_cpwms?(2*(ftm_mod)):((ftm_mod+1)));
//         System.err.println("ftm_modPeriod = " + ftm_modPeriod);
         
         double ftm_modPeriodMax = clockPeriod * (ftm_sc_cpwms?(2*(65535-ftm_cntin)):((65535-ftm_cntin+1)));
//         ftm_modPeriodVar.setValue(ftm_modPeriod);
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