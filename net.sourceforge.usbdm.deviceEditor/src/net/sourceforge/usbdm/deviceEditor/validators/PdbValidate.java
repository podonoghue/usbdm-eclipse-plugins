package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine PDB settings

 * Used for:
 *     pdb0
 */
public class PdbValidate extends PeripheralValidator {

   public PdbValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   int     NumChannels              = 0;
   int     NumPreTriggers           = 0;
   int     NumDacIntervalTriggers   = 0;
   int     NumPulseOutputs          = 0;
   
   double  pdb_clock_period      = 0.0;
   double  max_interval          = 0.0;
   
   boolean modChanged      = true;
   long    pdb_mod;
   double  pdb_mod_period;
   
   /**
    * Validate the PDB counter settings
    * 
    * @param  variable   Variable that triggered change (may be null)
    * 
    * @throws Exception
    */
   public void doCounterValidate(Variable variable) throws Exception {
      // In/Out
      LongVariable     pdb_modVar                 = getLongVariable("pdb_mod");
      LongVariable     pdb_idlyVar                = getLongVariable("pdb_idly");

      DoubleVariable   pdb_mod_periodVar          = getDoubleVariable("pdb_mod_period");
      DoubleVariable   pdb_idly_delayVar          = getDoubleVariable("pdb_idly_delay");
      DoubleVariable   pdb_clock_periodVar        = getDoubleVariable("pdb_clock_period");

      pdb_clock_period  = pdb_clock_periodVar.getValueAsDouble();
      pdb_mod           = pdb_modVar.getValueAsLong();
      pdb_mod_period    = pdb_mod_periodVar.getValueAsDouble();

      long pdb_idly         = pdb_idlyVar.getValueAsLong();
      double pdb_idly_delay = pdb_idly_delayVar.getValueAsDouble();

      max_interval = 0.0;
      if (variable == pdb_clock_periodVar) {
         // Update max interval
         max_interval = pdb_clock_period*(pdb_modVar.getMax()+1);
         
         pdb_mod_periodVar.setMax(max_interval);
         pdb_idly_delayVar.setMax(max_interval);
      }
      
      boolean guiUpdateAllowed = getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed;

      if (guiUpdateAllowed) {
         if (variable == pdb_mod_periodVar) {
            // Recalculate pdb_mod and pdb_mod_period (to allow for rounding)
            pdb_mod        = Math.max(0, Math.round((pdb_mod_period/pdb_clock_period)-1));
            pdb_mod_period = (pdb_mod+1)*pdb_clock_period;

            pdb_modVar.setValue(pdb_mod);
            pdb_mod_periodVar.setValue(pdb_mod_period);
         }
         if (variable == pdb_idly_delayVar) {
            // Recalculate pdb_idly_delayVar
            pdb_idly       = Math.max(0, Math.round((pdb_idly_delay/pdb_clock_period)-1));
            pdb_idly_delay = (pdb_idly+1)*pdb_clock_period;

            pdb_idlyVar.setValue(pdb_idly);
            pdb_idly_delayVar.setValue(pdb_idly_delay);
         }
      }
   }
   
   /**
    * Validate a PDB channel settings
    * 
    * @param  variable   Variable that triggered change (may be null)
    * @param channel     The channel to validate e.g. "0", "1" etc
    * 
    * @throws Exception
    */
   void doChannelValidate(Variable variable, int channel) throws Exception {

      boolean guiUpdateAllowed = getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed;

      for (int pretrigger=0; pretrigger<NumPreTriggers; pretrigger++) {
         DoubleVariable   pdb_chX_dlyM_delayVar   = getDoubleVariable("pdb_ch"+channel+"_dly"+ pretrigger+"_delay");
         if (max_interval > 0.0) {
            pdb_chX_dlyM_delayVar.setMax(max_interval);
         }
         if (guiUpdateAllowed) {
            ChoiceVariable   pdb_chN_c1_ptM          = getChoiceVariable("pdb_ch"+channel+"_c1_pt" + pretrigger);
            LongVariable     pdb_chX_dlyMVar         = getLongVariable("pdb_ch"+channel+"_dly" + pretrigger);
            if ((pdb_chN_c1_ptM.getValueAsLong() == 2) && (variable == pdb_chX_dlyM_delayVar)) {
               double pdb_chX_dlyM_delay = pdb_chX_dlyM_delayVar.getRawValueAsDouble();

               // Recalculate pdb_chX_dlyM and pdb_chX_dlyM_delay
               Long pdb_chX_dlyM  = Math.max(0, Math.round((pdb_chX_dlyM_delay/pdb_clock_period)-1));
               pdb_chX_dlyM_delay = (pdb_chX_dlyM+1)*pdb_clock_period;

               pdb_chX_dlyMVar.setValue(pdb_chX_dlyM);
               pdb_chX_dlyM_delayVar.setValue(pdb_chX_dlyM_delay);
            }
         }
      }
   }
   
   /**
    * Validate a DAC settings
    * 
    * @param  variable   Variable that triggered change (may be null)
    * @param  dacNum     The DAC to validate e.g. 0, 1 etc
    * 
    * @throws Exception
    */
   void doDacValidate(Variable variable, int dacNum) throws Exception {

      boolean guiUpdateAllowed = getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed;

      DoubleVariable      pdb_intX_delayVar        = getDoubleVariable("pdb_int"+dacNum+"_delay");
      if (max_interval > 0.0) {
         pdb_intX_delayVar.setMax(max_interval);
      }
      if (guiUpdateAllowed) {
         ChoiceVariable      pdb_intcX_triggerModeVar = getChoiceVariable("pdb_intc"+dacNum+"_triggerMode");
         LongVariable        pdb_intXVar              = getLongVariable("pdb_int"+dacNum);
         
         if ((pdb_intcX_triggerModeVar.getValueAsLong() == 1) && (variable == pdb_intX_delayVar)) {
            double pdb_intX_delay = pdb_intX_delayVar.getRawValueAsDouble();

            // Recalculate pdb_intX and pdb_intX_delay
            Long pdb_intX  = Math.max(0, Math.round((pdb_intX_delay/pdb_clock_period)-1));
            pdb_intX_delay = (pdb_intX+1)*pdb_clock_period;
   
            pdb_intXVar.setValue(pdb_intX);
            pdb_intX_delayVar.setValue(pdb_intX_delay);
         }
      }
   }
   
   /**
    * Validate a PDB pulse output settings
    * 
    * @param  variable   Variable that triggered change (may be null)
    * @param  pulseOutputNum    The pulse output to validate e.g. 0, 1 etc
    * 
    * @throws Exception
    */
   void doPulseValidate(Variable variable, int pulseOutputNum) throws Exception {

      boolean guiUpdateAllowed = getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed;

      BooleanVariable pdb_poen_enY = getBooleanVariable("pdb_poen_en"+pulseOutputNum);
      
      for (int edgeNum=1; edgeNum<=2; edgeNum++) {
         DoubleVariable   pdb_poY_dlyE_delayVar = getDoubleVariable("pdb_po"+pulseOutputNum+"_dly"+ edgeNum+"_delay");
         if (max_interval > 0.0) {
            pdb_poY_dlyE_delayVar.setMax(max_interval);
         }
         if (guiUpdateAllowed) {
            LongVariable     pdb_poY_dlyEVar = getLongVariable("pdb_po"+pulseOutputNum+"_dly" + edgeNum);
            if (pdb_poen_enY.getValueAsBoolean() && (variable == pdb_poY_dlyE_delayVar)) {
               double pdb_poY_dlyE_delay = pdb_poY_dlyE_delayVar.getRawValueAsDouble();

               // Recalculate pdb_poY_dlyE and pdb_poY_dlyE_delay
               Long pdb_poY_dlyE  = Math.max(0, Math.round((pdb_poY_dlyE_delay/pdb_clock_period)-1));
               pdb_poY_dlyE_delay = (pdb_poY_dlyE+1)*pdb_clock_period;

               pdb_poY_dlyEVar.setValue(pdb_poY_dlyE);
               pdb_poY_dlyE_delayVar.setValue(pdb_poY_dlyE_delay);
            }
         }
      }
   }
   
   /**
    * Class to determine PDB settings
    * 
    * Updates pdb_clock_periodVar, pdb_clock_frequencyVar, pdb_mod_period, pdb_idly_delay
    * 
    * @throws Exception
    */
   @Override
   public void validate(Variable variable, int properties) throws Exception {
      
      super.validate(variable, properties);
      
      // Validate the shared counter
      doCounterValidate(variable);
      
      // Validate each channel
      for (int index=0; index<NumChannels; index++) {
         doChannelValidate(variable, index);
      }
      // Validate DAC triggers
      for (int index=0; index<NumDacIntervalTriggers; index++) {
         doDacValidate(variable, index);
      }
      // Validate Pulse outputs
      for (int index=0; index<NumPreTriggers; index++) {
         doPulseValidate(variable, index);
      }
   }
   
   @Override
   protected boolean createDependencies() throws Exception {

      NumChannels             = (int)getLongVariable("NumChannels").getValueAsLong();
      NumPreTriggers          = (int)getLongVariable("NumPreTriggers").getValueAsLong();
      NumDacIntervalTriggers  = (int)getLongVariable("NumDacIntervalTriggers").getValueAsLong();
      NumPulseOutputs         = (int)getLongVariable("NumPulseOutputs").getValueAsLong();
      
      final String[] externalVariables = {
            "/SIM/system_bus_clock[]",
      };
      addToWatchedVariables(externalVariables);
      
      // Don't add default dependencies
      return false;
   }
}
