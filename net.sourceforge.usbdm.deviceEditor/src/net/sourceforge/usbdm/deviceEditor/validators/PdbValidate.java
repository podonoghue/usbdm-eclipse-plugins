package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
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

   public PdbValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   boolean clockChanged    = true;
   double  pdb_frequency   = 0.0;
   double  pdb_period      = 0.0;
   String  pdbClockOrigin  = "Unknown";
   
   boolean modChanged      = true;
   long    pdb_mod;
   double  pdb_mod_period;
   
   /**
    * Validate the PDB clock settings
    *  
    * @param  variable   Variable that triggered change (may be null)
    * 
    * @throws Exception
    */
   void doClockValidate(Variable variable) throws Exception {
      // In
      Variable         clockVar                   = getVariable("/SIM/system_bus_clock");
      Variable         pdb_sc_prescalerVar        = getVariable("pdb_sc_prescaler");
      Variable         pdb_sc_multVar             = getVariable("pdb_sc_mult");

      // Out
      DoubleVariable   pdb_periodVar              = getDoubleVariable("pdb_period");
      DoubleVariable   pdb_frequencyVar           = getDoubleVariable("pdb_frequency");

      pdbClockOrigin = "PDB Clock";
      
      clockChanged = (variable == null) || (
            (variable.equals(clockVar)) ||
            (variable.equals(pdb_sc_prescalerVar)) ||
            (variable.equals(pdb_sc_multVar))
            );         
      
      long busFrequency      = clockVar.getValueAsLong();
      long pdb_sc_prescaler  = pdb_sc_prescalerVar.getValueAsLong();
      long pdb_sc_mult       = pdb_sc_multVar.getValueAsLong();
 
      // MULT divider values
      final long multValues[] = {1,10,20,40};
      
      pdb_frequency = pdb_frequencyVar.getValueAsDouble();
      pdb_period    = pdb_periodVar.getValueAsDouble();

      if (clockChanged) {
         // Update everything
         pdb_frequency = busFrequency/((1<<pdb_sc_prescaler)*multValues[(int)pdb_sc_mult]);
         if (pdb_frequency == 0) {
            // For safety
            pdb_period = 1;
         }
         else {
            pdb_period = 1/pdb_frequency;
         }
         pdb_frequencyVar.setValue(pdb_frequency);
         pdb_frequencyVar.setOrigin(pdbClockOrigin+" frequency / (prescaler * divider)");
         pdb_periodVar.setValue(pdb_period);
         pdb_periodVar.setOrigin(pdbClockOrigin+" period * prescaler * divider");
      }
   }

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

      pdb_mod        = pdb_modVar.getValueAsLong();
      pdb_mod_period = pdb_mod_periodVar.getValueAsDouble();
      
      long pdb_idly         = pdb_idlyVar.getValueAsLong();
      double pdb_idly_delay = pdb_idly_delayVar.getValueAsDouble();
      
      if (clockChanged) {
         // Update everything
         pdb_mod_period = (pdb_mod+1)*pdb_period;
         pdb_mod_periodVar.setValue(pdb_mod_period);
         pdb_mod_periodVar.setOrigin(pdbClockOrigin+" period * PDB modulo");
         pdb_mod_periodVar.setMax((pdb_modVar.getMax()+1.5)*pdb_period);
         pdb_idly_delayVar.setValue((pdb_idly+1)*pdb_period);
         pdb_idly_delayVar.setOrigin(pdbClockOrigin+" period * PDB idly");
         modChanged = true;
      }
      else if (variable != null) {
         // Update selectively
         if (variable.equals(pdb_modVar)) {
            pdb_mod_period = (pdb_mod+1)*pdb_period;
            pdb_mod_periodVar.setValue(pdb_mod_period);
            modChanged = true;
         }
         else if (variable.equals(pdb_mod_periodVar)) {
            // Calculate rounded value
            pdb_mod        = Math.max(0, Math.round((pdb_mod_period/pdb_period)-1));
            pdb_mod_period = (pdb_mod+1)*pdb_period;
            // Update
            pdb_modVar.setValue(pdb_mod);
            // Need to show effect of rounding
            pdb_mod_periodVar.setValue(pdb_mod_period);
            modChanged = true;
         }
         else if (variable.equals(pdb_idlyVar)) {
            pdb_idly_delayVar.setValue((pdb_idly+1)*pdb_period);
         }
         else if (variable.equals(pdb_idly_delayVar)) {
            // Calculate rounded value
            pdb_idly = Math.max(0, Math.round((pdb_idly_delay/pdb_period)-1));
            // Update
            pdb_idlyVar.setValue(pdb_idly);
            // Need to show effect of rounding
            pdb_idly_delayVar.setValue((pdb_idly+1)*pdb_period);
         }
      }
      if (modChanged) {
         pdb_idly_delayVar.setMax((pdb_mod+1.5)*pdb_period);
         pdb_idlyVar.setMax(pdb_mod);
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
   void doChannelValidate(Variable variable, String channel) throws Exception {
      // In/Out
      LongVariable     pdb_chX_dly0Var         = safeGetLongVariable("pdb_ch"+channel+"_dly0");
      if (pdb_chX_dly0Var == null) {
         // Channel doesn't exit
         return;
      }
      LongVariable     pdb_chX_dly1Var         = getLongVariable("pdb_ch"+channel+"_dly1");
      LongVariable     pdb_chX_c1_tosVar       = getLongVariable("pdb_ch"+channel+"_c1_tos");
      LongVariable     pdb_chX_c1_enVar        = getLongVariable("pdb_ch"+channel+"_c1_en");

      // In/Out
      DoubleVariable   pdb_chX_dly0_delayVar   = getDoubleVariable("pdb_ch"+channel+"_dly0_delay");
      DoubleVariable   pdb_chX_dly1_delayVar   = getDoubleVariable("pdb_ch"+channel+"_dly1_delay");

      boolean pt0Enable = (pdb_chX_c1_enVar.getValueAsLong()&(1<<0)) != 0;
      boolean pt1Enable = (pdb_chX_c1_enVar.getValueAsLong()&(1<<1)) != 0;
      
      boolean dly0Enable = pt0Enable && ((pdb_chX_c1_tosVar.getValueAsLong()&(1<<0)) != 0);
      boolean dly1Enable = pt1Enable && ((pdb_chX_c1_tosVar.getValueAsLong()&(1<<1)) != 0);
      
      // Do enable/disable first
      pdb_chX_dly0Var.enable(dly0Enable);
      pdb_chX_dly0_delayVar.enable(dly0Enable);
      pdb_chX_dly1Var.enable(dly1Enable);
      pdb_chX_dly1_delayVar.enable(dly1Enable);
      
      // Get current values
      long   pdb_chX_dly0       = pdb_chX_dly0Var.getRawValueAsLong();
      double pdb_chX_dly0_delay = pdb_chX_dly0_delayVar.getRawValueAsDouble();
      long   pdb_chX_dly1       = pdb_chX_dly1Var.getRawValueAsLong();
      double pdb_chX_dly1_delay = pdb_chX_dly1_delayVar.getRawValueAsDouble();
      
      if (clockChanged) {
         pdb_chX_dly0_delayVar.setOrigin(pdbClockOrigin+" period * pdb_ch"+channel+"_dly0");
         pdb_chX_dly1_delayVar.setOrigin(pdbClockOrigin+" period * pdb_ch"+channel+"_dly1");
      }
      if (modChanged) {
         pdb_chX_dly0Var.setMax(pdb_mod);
         pdb_chX_dly1Var.setMax(pdb_mod);
      }
      if (variable != null) {
         if (variable.equals(pdb_chX_dly0_delayVar)) {
            // Calculate rounded value
            pdb_chX_dly0 = Math.max(0, Math.round((pdb_chX_dly0_delay/pdb_period)-1));
            // Update
            pdb_chX_dly0Var.setValue(pdb_chX_dly0);
         }
         else if (variable.equals(pdb_chX_dly1_delayVar)) {
            // Calculate rounded value
            pdb_chX_dly1 = Math.max(0, Math.round((pdb_chX_dly1_delay/pdb_period)-1));
            // Update
            pdb_chX_dly1Var.setValue(pdb_chX_dly1);
         }
      }
      pdb_chX_dly0_delayVar.setValue(pdb_period*(pdb_chX_dly0+1));
      pdb_chX_dly1_delayVar.setValue(pdb_period*(pdb_chX_dly1+1));
      pdb_chX_dly0_delayVar.setMax((pdb_mod+1.5)*pdb_period);
      pdb_chX_dly1_delayVar.setMax((pdb_mod+1.5)*pdb_period);
   }
   
   /**
    * Validate a DAC settings
    *  
    * @param  variable   Variable that triggered change (may be null)
    * @param  channel    The DAC to validate e.g. 0, 1 etc
    * 
    * @throws Exception
    */
   void doDacValidate(Variable variable, int channel) throws Exception {
      
      // In/Out
      BooleanVariable     pdb_intXc_toeVar         = safeGetBooleanVariable("pdb_int"+channel+"c_toe");
      if (pdb_intXc_toeVar == null) {
         // Dac trigger doesn't exit
         return;
      }
      BooleanVariable     pdb_intXc_extVar        = getBooleanVariable("pdb_int"+channel+"c_ext");
      LongVariable        pdb_intX_intVar         = getLongVariable("pdb_int"+channel+"_int");

      // Out/Out
      DoubleVariable      pdb_intX_int_delayVar   = getDoubleVariable("pdb_int"+channel+"_int_delay");
      
      boolean triggerEnable = pdb_intXc_toeVar.getRawValueAsBoolean();
      
      // Do enable/disable first
      pdb_intXc_extVar.enable(triggerEnable);
      pdb_intX_intVar.enable(triggerEnable);
      pdb_intX_int_delayVar.enable(triggerEnable);
      
      // Get current values
      long   pdb_intX_int       = pdb_intX_intVar.getRawValueAsLong();
      double pdb_intX_int_delay = pdb_intX_int_delayVar.getRawValueAsDouble();
      
      if (clockChanged) {
         pdb_intX_int_delayVar.setOrigin(pdbClockOrigin+" period * pdb_int"+channel+"_int");
      }
      if (variable != null) {
         if (variable.equals(pdb_intX_int_delayVar)) {
            // Calculate rounded value
            pdb_intX_int = Math.max(0, Math.round((pdb_intX_int_delay/pdb_period)-1));
            // Update
            pdb_intX_intVar.setValue(pdb_intX_int);
            // Need to show effect of rounding
         }
      }
      pdb_intX_intVar.setMax(pdb_mod);
      pdb_intX_int_delayVar.setMax((pdb_mod+1.5)*pdb_period);
      pdb_intX_int_delayVar.setValue(pdb_period*(pdb_intX_int+1));
   }
   
   /**
    * Validate a PDB pulse output settings
    *  
    * @param  variable   Variable that triggered change (may be null)
    * @param  channel    The pulse output to validate e.g. 0, 1 etc
    * 
    * @throws Exception
    */
   void doPulseValidate(Variable variable, int channel) throws Exception {
      
      // In/Out
      LongVariable     pdb_poX_dly_dly1Var      = safeGetLongVariable("pdb_po"+channel+"_dly_dly1");
      if (pdb_poX_dly_dly1Var == null) {
         // Channel doesn't exit
         return;
      }
      LongVariable     pdb_poX_dly_dly2Var      = getLongVariable("pdb_po"+channel+"_dly_dly2");

      // Out/Out
      DoubleVariable   pdb_poX_dly_dly1_delayVar   = getDoubleVariable("pdb_po"+channel+"_dly_dly1_delay");
      DoubleVariable   pdb_poX_dly_dly2_delayVar   = getDoubleVariable("pdb_po"+channel+"_dly_dly2_delay");
      
      LongVariable     pdb_poenVar      = getLongVariable("pdb_poen");
      boolean dlyEnable = (pdb_poenVar.getRawValueAsLong()&(1<<channel)) != 0;
      
      // Do enable/disable first
      pdb_poX_dly_dly1Var.enable(dlyEnable);
      pdb_poX_dly_dly1_delayVar.enable(dlyEnable);
      pdb_poX_dly_dly2Var.enable(dlyEnable);
      pdb_poX_dly_dly2_delayVar.enable(dlyEnable);
      
      // Get current values
      long   pdb_poX_dly_dly1       = pdb_poX_dly_dly1Var.getRawValueAsLong();
      double pdb_poX_dly_dly1_delay = pdb_poX_dly_dly1_delayVar.getRawValueAsDouble();
      long   pdb_poX_dly_dly2       = pdb_poX_dly_dly2Var.getRawValueAsLong();
      double pdb_poX_dly_dly2_delay = pdb_poX_dly_dly2_delayVar.getRawValueAsDouble();
      
      if (clockChanged) {
         pdb_poX_dly_dly1_delayVar.setOrigin(pdbClockOrigin+" period * pdb_po"+channel+"_dly_dly1");
         pdb_poX_dly_dly2_delayVar.setOrigin(pdbClockOrigin+" period * pdb_po"+channel+"_dly_dly2");
      }
      if (variable != null) {
         if (variable.equals(pdb_poX_dly_dly1_delayVar)) {
            // Calculate rounded value
            pdb_poX_dly_dly1 = Math.max(0, Math.round((pdb_poX_dly_dly1_delay/pdb_period)-1));
            // Update
            pdb_poX_dly_dly1Var.setValue(pdb_poX_dly_dly1);
         }
         else if (variable.equals(pdb_poX_dly_dly2_delayVar)) {
            // Calculate rounded value
            pdb_poX_dly_dly2 = Math.max(0, Math.round((pdb_poX_dly_dly2_delay/pdb_period)-1));
            // Update
            pdb_poX_dly_dly2Var.setValue(pdb_poX_dly_dly2);
         }
      }
      pdb_poX_dly_dly1Var.setMax(pdb_mod);
      pdb_poX_dly_dly1_delayVar.setMax((pdb_mod+1.5)*pdb_period);
      pdb_poX_dly_dly2Var.setMax(pdb_mod);
      pdb_poX_dly_dly2_delayVar.setMax((pdb_mod+1.5)*pdb_period);
      pdb_poX_dly_dly1_delayVar.setValue(pdb_period*(pdb_poX_dly_dly1+1));
      pdb_poX_dly_dly2_delayVar.setValue(pdb_period*(pdb_poX_dly_dly2+1));
   }
   
   /**
    * Class to determine PDB settings
    * 
    * Updates pdb_periodVar, pdb_frequencyVar, pdb_mod_period, pdb_idly_delay
    *  
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      // Validate the clock
      doClockValidate(variable);
      // Validate the shared counter
      doCounterValidate(variable);
      // Validate each channel
      doChannelValidate(variable, "0");
      doChannelValidate(variable, "1");
      doChannelValidate(variable, "2");
      doChannelValidate(variable, "3");
      // Validate DAC triggers
      doDacValidate(variable, 0);
      doDacValidate(variable, 1);
      doDacValidate(variable, 2);
      doDacValidate(variable, 3);
      // Validate Pulse outputs
      doPulseValidate(variable, 0);
      doPulseValidate(variable, 1);
      doPulseValidate(variable, 2);
      doPulseValidate(variable, 3);
   }
   
   @Override
   protected void createDependencies() throws Exception {
      final String[] externalVariables = {
            "/SIM/system_bus_clock",
      };
      addToWatchedVariables(externalVariables);
   }
}
