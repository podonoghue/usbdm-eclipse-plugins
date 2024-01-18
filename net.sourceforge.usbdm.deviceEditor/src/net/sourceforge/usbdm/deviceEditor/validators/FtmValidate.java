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

   int MODE_None;
   int MODE_Left;
   int MODE_Centre;
   int MODE_Quad;
   
   public FtmValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   /**
    * Class to determine FTM settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable, int properties) throws Exception {

      super.validate(variable, properties);

      //=================================

      ChoiceVariable    modeVar           =  getChoiceVariable("mode");

      int mode = (int) modeVar.getValueAsLong();
      if ((mode == MODE_None) || (mode == MODE_Quad)) {
         return;
      }

      DoubleVariable    clockVar          =  getDoubleVariable("clock");
      LongVariable      modVar            =  getLongVariable("ftm_mod_mod");
      DoubleVariable    modPeriodVar      =  getDoubleVariable("ftm_modPeriod");
      BooleanVariable   sc_cpwmsVar       =  getBooleanVariable("ftm_sc_cpwms");

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
                     DoubleVariable ftm_cnvEventTime_independentVar  = getDoubleVariable("ftm_cnvEventTime_independent["+channel+"]");
                     if (variable.equals(ftm_cnvEventTime_independentVar)) {
                        // Target
                        LongVariable ftm_cnv_independentVar = getLongVariable("ftm_cnv_independent["+channel+"]");

                        if (!ftm_cnvEventTime_independentVar.isEnabled()||!ftm_cnv_independentVar.isEnabled()) {
                           // Ignore if disabled to preserve value
                           continue;
                        }
                        // Calculate rounded value for event time in ticks
                        double cnvEventTime = ftm_cnvEventTime_independentVar.getValueAsDouble();
                        if (!Double.isFinite(cnvEventTime)) {
                           // Don't propagate invalid calculation
                           continue;
                        }
                        long cnv = Math.max(0, Math.round((cnvEventTime/clockPeriod)));
                        ftm_cnv_independentVar.setValue(cnv);
//                        System.err.println("ftm_cnv_independent="+ftm_cnv_independentVar+", \n"
//                              + "ftm_cnvEventTime_independent= "+ftm_cnvEventTime_independentVar);
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
            "ftm_mod_mod",
            "ftm_modPeriod",
            "ftm_sc_cpwms",
            "ftm_cnvEventTime_independent[0]",
            "ftm_cnvEventTime_independent[1]",
            "ftm_cnvEventTime_independent[2]",
            "ftm_cnvEventTime_independent[3]",
            "ftm_cnvEventTime_independent[4]",
            "ftm_cnvEventTime_independent[5]",
            "ftm_cnvEventTime_independent[6]",
            "ftm_cnvEventTime_independent[7]",
      };
      addToWatchedVariables(externalVariables);
      
      MODE_None   = (int) getLongVariable("/FTM0/None").getValueAsLong();
      MODE_Left   = (int) getLongVariable("/FTM0/Left").getValueAsLong();
      MODE_Centre = (int) getLongVariable("/FTM0/Centre").getValueAsLong();
      MODE_Quad   = (int) getLongVariable("/FTM0/Quad").getValueAsLong();
      
      // Don't add default dependencies
      return false;
   }

}