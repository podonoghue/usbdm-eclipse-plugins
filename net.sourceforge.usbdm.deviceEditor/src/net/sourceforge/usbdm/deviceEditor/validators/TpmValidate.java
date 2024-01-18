package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
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
   public void validate(Variable variable, int properties) throws Exception {

      super.validate(variable, properties);

      int MODE_None   = (int) getLongVariable("None").getValueAsLong();
//      int MODE_Left   = (int) getLongVariable("/TPM0/Left").getValueAsLong();
//      int MODE_Centre = (int) getLongVariable("/TPM0/Centre").getValueAsLong();
      int MODE_Quad   = (int) getLongVariable("Quad").getValueAsLong();

      //=================================

      ChoiceVariable    modeVar           =  getChoiceVariable("mode");

      int mode = (int) modeVar.getValueAsLong();
      if ((mode == MODE_None) || (mode == MODE_Quad)) {
         return;
      }

      DoubleVariable    clockVar          =  getDoubleVariable("clock");
      LongVariable      modVar            =  getLongVariable("tpm_mod_mod");
      DoubleVariable    modPeriodVar      =  getDoubleVariable("tpm_modPeriod");
      BooleanVariable   sc_cpwmsVar       =  getBooleanVariable("tpm_sc_cpwms");

      LongVariable      NumChannelsVar    = getLongVariable("_channelCount");
      int               NumChannels       = (int)NumChannelsVar.getValueAsLong();

      double clockFrequency = clockVar.getValueAsDouble();

      if (clockFrequency != 0){
         long   mod       = modVar.getValueAsLong();
         double modPeriod = modPeriodVar.getValueAsDouble();

         if (!Double.isFinite(modPeriod)) {
            // Don't propagate if invalid calculation of period
            return;
         }
         double clockPeriod = 1.0/clockFrequency;

         boolean sc_cpwms = sc_cpwmsVar.getValueAsBoolean();

         // These updates involves a loop so suppress initially
         if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
            if (variable != null) {
               if (variable.equals(modPeriodVar)) {
                  // modPeriod => mod
                  // Calculate rounded value
                  if (sc_cpwms) {
                     // Centre-aligned
                     mod = Math.max(0, Math.round((modPeriod/clockPeriod)/2));
                     modVar.setValue(mod);
                  }
                  else {
                     // Left-aligned
                     mod = Math.max(0, Math.round((modPeriod/clockPeriod)-1));
                     modVar.setValue(mod);
//                   System.err.println("ftm_modPeriod="+ftm_modPeriod+"ftm_modPeriod= "+ftm_modPeriod);
                  }
                  return;
               }
               else {
                  // cnvEventTimeVar[] -> cnv[]
                  for (int channel=0; channel<NumChannels; channel++) {
                     DoubleVariable cnvEventTimeVar  = getDoubleVariable("tpm_cnvEventTime_independent["+channel+"]");
                     if (variable.equals(cnvEventTimeVar)) {
                        // Target
                        LongVariable cnvVar = getLongVariable("tpm_cnv_independent["+channel+"]");

                        if (!cnvEventTimeVar.isEnabled()||!cnvVar.isEnabled()) {
                           // Ignore if disabled to preserve value
                           continue;
                        }
                        // Calculate rounded value for event time in ticks
                        double cnvEventTime = cnvEventTimeVar.getValueAsDouble();
                        if (!Double.isFinite(cnvEventTime)) {
                           // Don't propagate invalid calculation
                           continue;
                        }
                        long cnv = Math.max(0, Math.round((cnvEventTime/clockPeriod)));
//                        System.err.println("ftm_cnv="+ftm_cnv+", ftm_cnvEventTime= "+ftm_cnvEventTime);
                        cnvVar.setValue(cnv);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected boolean createDependencies() throws Exception {

      final String[] externalVariables = {
            "mode",
            "clock",
            "tpm_mod_mod",
            "tpm_modPeriod",
            "tpm_sc_cpwms",
            "tpm_cnvEventTime_independent[0]",
            "tpm_cnvEventTime_independent[1]",
            "tpm_cnvEventTime_independent[2]",
            "tpm_cnvEventTime_independent[3]",
            "tpm_cnvEventTime_independent[4]",
            "tpm_cnvEventTime_independent[5]",
            "tpm_cnvEventTime_independent[6]",
            "tpm_cnvEventTime_independent[7]",
      };
      addToWatchedVariables(externalVariables);

      // Don't add default dependencies
      return false;
   }

}