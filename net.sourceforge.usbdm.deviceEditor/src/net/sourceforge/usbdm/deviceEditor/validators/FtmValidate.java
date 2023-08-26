package net.sourceforge.usbdm.deviceEditor.validators;

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
      
      LongVariable      clockFrequencyVar          =  getLongVariable("clockFrequency");
      LongVariable      ftm_modVar                 =  getLongVariable("ftm_mod");
      LongVariable      ftm_cntinVar               =  getLongVariable("ftm_cntin");
      DoubleVariable    ftm_modPeriodVar           =  getDoubleVariable("ftm_modPeriod");
      ChoiceVariable    ftm_sc_modeVar             =  getChoiceVariable("ftm_sc_mode");
      
      LongVariable      NumChannelsVar    = getLongVariable("NumChannels");
      int               NumChannels       = (int)NumChannelsVar.getValueAsLong();
      
      long clockFrequency = clockFrequencyVar.getValueAsLong();
      
      ftm_modPeriodVar.enable(clockFrequency != 0);

      if (clockFrequency != 0){
         long   ftm_mod       = ftm_modVar.getValueAsLong();
         long   ftm_cntin     = ftm_cntinVar.getValueAsLong();
         double ftm_modPeriod = ftm_modPeriodVar.getValueAsDouble();
         
         if (!Double.isFinite(ftm_modPeriod)) {
            // Don't propagate if invalid calculation of period
            return;
         }
         double clockPeriod = 1.0/clockFrequency;
         
         long ftm_sc_mode = ftm_sc_modeVar.getValueAsLong();
         
         ftm_modVar.setMin((ftm_cntin==0)?0:ftm_cntin+1);
         ftm_cntinVar.setMax((ftm_mod==0)?0:ftm_mod-1);
         
         // These updates involves a loop so suppress initially
         if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
            if (variable != null) {
               if (variable.equals(ftm_modPeriodVar)) {
                  // Calculate rounded value
                  switch ((int)ftm_sc_mode) {
                  default:
                  case 0: // Left-aligned
                     ftm_mod = Math.max(0, Math.round((ftm_modPeriod/clockPeriod)-1)) + ftm_cntin;
                     ftm_modVar.setValue(ftm_mod);
                     break;
                  case 1: // Centre-aligned
                     ftm_mod = Math.max(0, Math.round((ftm_modPeriod/clockPeriod)/2)) + ftm_cntin;
                     ftm_modVar.setValue(ftm_mod);
                     break;
                  case 2:  // Free-running
                     ftm_modPeriodVar.setValue(clockPeriod*65535);
                  }
                  return;
               }
               for (int channel=0; channel<NumChannels; channel++) {
                  DoubleVariable ftm_cnvEventTimeVar  = getDoubleVariable("ftm_cnvEventTime["+channel+"]");
                  if (variable.equals(ftm_cnvEventTimeVar)) {
                     // Target
                     LongVariable ftm_cnvVar = getLongVariable("ftm_cnv["+channel+"]");

                     if (!ftm_cnvEventTimeVar.isEnabled()||!ftm_cnvVar.isEnabled()) {
                        // Ignore if disabled to preserve value
                        continue;
                     }
                     // Calculate rounded value for event time in ticks
                     double ftm_cnvEventTime = ftm_cnvEventTimeVar.getValueAsDouble();
                     long ftm_cnv = Math.max(0, Math.round((ftm_cnvEventTime/clockPeriod)));
                     ftm_cnvVar.setValue(ftm_cnv);
                  }
               }
            }
         }
//         ftm_modPeriod = clockPeriod * (ftm_sc_cpwms?(2*(ftm_mod)):((ftm_mod+1)));
//         System.err.println("ftm_modPeriod = " + ftm_modPeriod);
//
//         double ftm_modPeriodMax = clockPeriod * (ftm_sc_cpwms?(2*(65535-ftm_cntin)):((65535-ftm_cntin+1)));
//         ftm_modPeriodVar.setValue(ftm_modPeriod);
//         ftm_modPeriodVar.setMax(ftm_modPeriodMax);
      }
   }

   @Override
   protected boolean createDependencies() throws Exception {

      final String[] externalVariables = {
            "clockFrequency",
            "ftm_mod",
            "ftm_cntin",
            "ftm_modPeriod",
            "ftm_sc_mode",
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