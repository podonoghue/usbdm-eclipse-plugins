package net.sourceforge.usbdm.deviceEditor.validators;

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
   
   public TpmValidate(PeripheralWithState peripheral) {
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
       
      LongVariable      clockFrequencyVar = getLongVariable("clockFrequency");
      LongVariable      tpm_modVar        = getLongVariable("tpm_mod");
      DoubleVariable    tpm_modPeriodVar  = getDoubleVariable("tpm_modPeriod");
      ChoiceVariable    tpm_sc_modeVar    = getChoiceVariable("tpm_sc_mode");
      
      LongVariable      NumChannelsVar    = getLongVariable("NumChannels");
      int               NumChannels       = (int)NumChannelsVar.getValueAsLong();
      
      long clockFrequency = clockFrequencyVar.getValueAsLong();
      
      tpm_modPeriodVar.enable(clockFrequency != 0);

      if (clockFrequency != 0){
         long   tpm_mod       = tpm_modVar.getValueAsLong();
         double tpm_modPeriod = tpm_modPeriodVar.getValueAsDouble();
         if (!Double.isFinite(tpm_modPeriod)) {
            // Don't propagate if invalid calculation of period
            return;
         }
         double clockPeriod = 1.0/clockFrequency;
         
         long tpm_sc_mode = tpm_sc_modeVar.getValueAsLong();
         
         // These updates involves a loop so suppress initially
         if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
            if (variable != null) {
               if (variable.equals(tpm_modPeriodVar)) {
                  // Calculate rounded value for mod value in ticks
                  switch ((int)tpm_sc_mode) {
                  default:
                  case 0: // Left-aligned
                     tpm_mod = Math.max(0, Math.round((tpm_modPeriod/clockPeriod)-1));
                     tpm_modVar.setValue(tpm_mod);
                     break;
                  case 1: // Centre-aligned
                     tpm_mod = Math.max(0, Math.round((tpm_modPeriod/clockPeriod)/2));
                     tpm_modVar.setValue(tpm_mod);
                     break;
                  case 2:  // Free-running
                     tpm_modPeriodVar.setValue(clockPeriod*65535);
                  }
                  return;
               }
               for (int channel=0; channel<NumChannels; channel++) {
                  DoubleVariable tpm_cnvEventTimeVar  = getDoubleVariable("tpm_cnvEventTime["+channel+"]");
                  if (variable.equals(tpm_cnvEventTimeVar)) {
                     // Target
                     LongVariable tpm_cnvVar = getLongVariable("tpm_cnv["+channel+"]");

                     if (!tpm_cnvEventTimeVar.isEnabled()||!tpm_cnvVar.isEnabled()) {
                        // Ignore if disabled to preserve value
                        continue;
                     }
                     // Calculate rounded value for event time in ticks
                     double tpm_cnvEventTime = tpm_cnvEventTimeVar.getValueAsDouble();
                     long tpm_cnv = Math.max(0, Math.round((tpm_cnvEventTime/clockPeriod)));
                     tpm_cnvVar.setValue(tpm_cnv);
                  }
               }
            }
         }
//         double tpm_modPeriodMax = clockPeriod * (tpm_sc_cpwms?(2*(65535.5)):((65536.5)));
//         tpm_modPeriodVar.setValue(tpm_modPeriod);
//         tpm_modPeriodVar.setMax(tpm_modPeriodMax);
      }
   }

   @Override
   protected boolean createDependencies() throws Exception {

      final String[] externalVariables = {
            "clockFrequency",
            "tpm_mod",
            "tpm_modPeriod",
            "tpm_sc_mode",
            "tpm_cnvEventTime[0]",
            "tpm_cnvEventTime[1]",
            "tpm_cnvEventTime[2]",
            "tpm_cnvEventTime[3]",
            "tpm_cnvEventTime[4]",
            "tpm_cnvEventTime[5]",
            "tpm_cnvEventTime[6]",
            "tpm_cnvEventTime[7]",
      };
      addToWatchedVariables(externalVariables);
      
      // Don't add default dependencies
      return false;
   }

}