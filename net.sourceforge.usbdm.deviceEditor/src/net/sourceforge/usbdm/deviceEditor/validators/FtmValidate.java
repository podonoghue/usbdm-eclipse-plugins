package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine ftm settings
 */
public class FtmValidate extends PeripheralValidator {

   public FtmValidate(PeripheralWithState peripheral) {
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

      ChoiceVariable    modeVar           =  getChoiceVariable("mode");

      int mode = (int) modeVar.getValueAsLong();
      if (mode == 2) {
         // Quad-decoder
         return;
      }

      DoubleVariable    clockVar          =  getDoubleVariable("clock");
      LongVariable      modVar            =  getLongVariable("ftm_mod");
      DoubleVariable    modPeriodVar      =  getDoubleVariable("ftm_modPeriod");
      BooleanVariable   sc_cpwmsVar       =  getBooleanVariable("ftm_sc_cpwms");

      LongVariable      NumChannelsVar    = getLongVariable("NumChannels");
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
                     DoubleVariable cnvEventTimeVar  = getDoubleVariable("ftm_cnvEventTime["+channel+"]");
                     if (variable.equals(cnvEventTimeVar)) {
                        // Target
                        LongVariable cnvVar = getLongVariable("ftm_cnv["+channel+"]");

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
            "ftm_mod",
            "ftm_modPeriod",
            "ftm_sc_cpwms",
            "ftm_cnvEventTime[0]",
            "ftm_cnvEventTime[1]",
            "ftm_cnvEventTime[2]",
            "ftm_cnvEventTime[3]",
            "ftm_cnvEventTime[4]",
            "ftm_cnvEventTime[5]",
            "ftm_cnvEventTime[6]",
            "ftm_cnvEventTime[7]",
      };
      addToWatchedVariables(externalVariables);

      // Don't add default dependencies
      return false;
   }

}