package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine tpm settings
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
//      DoubleVariable    clockPeriodVar             =  getDoubleVariable("clockPeriod");
      LongVariable      tpm_modVar                 =  getLongVariable("tpm_mod");
      DoubleVariable    tpm_modPeriodVar           =  getDoubleVariable("tpm_modPeriod");
      ChoiceVariable    tpm_sc_modeVar             =  getChoiceVariable("tpm_sc_mode");
      
      long clockFrequency = clockFrequencyVar.getValueAsLong();
      
      tpm_modPeriodVar.enable(clockFrequency != 0);

      if (clockFrequency != 0){
         long   tpm_mod       = tpm_modVar.getValueAsLong();
         double tpm_modPeriod = tpm_modPeriodVar.getValueAsDouble();

         double clockPeriod = 1.0/clockFrequency;
         
         long tpm_sc_mode = tpm_sc_modeVar.getValueAsLong();
         
         // These updates involves a loop so suppress initially
         if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
            if (variable != null) {
               if (variable.equals(tpm_modPeriodVar)) {
                  // Calculate rounded value
                  switch ((int)tpm_sc_mode) {
                  default:
                  case 0: // Left-aligned
                     tpm_mod        = Math.max(0, Math.round((tpm_modPeriod/clockPeriod)-1));
                     tpm_modVar.setValue(tpm_mod);
                     break;
                  case 1: // Centre-aligned
                     tpm_mod        = Math.max(0, Math.round((tpm_modPeriod/clockPeriod)/2));
                     tpm_modVar.setValue(tpm_mod);
                     break;
                  case 2:  // Free-running
                     tpm_modPeriodVar.setValue(clockPeriod*65535);
                  }
                  return;
               }
            }
         }
//         double tpm_modPeriodMax = clockPeriod * (tpm_sc_cpwms?(2*(65535.5)):((65536.5)));
//         tpm_modPeriodVar.setValue(tpm_modPeriod);
//         tpm_modPeriodVar.setMax(tpm_modPeriodMax);
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