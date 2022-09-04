package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate LPTMR settings
 */
public class LptmrValidate extends PeripheralValidator {

   public LptmrValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine LPTMR settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);

//      System.err.println("LptmrValidate.validate("+variable+")");
      
      final String   osc0_peripheral  = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      
      // Variables
      //=================================
      DoubleVariable    clockFrequencyVar          =  getDoubleVariable("clockFrequency");
      DoubleVariable    clockPeriodVar             =  getDoubleVariable("clockPeriod");
      DoubleVariable    maximumPeriodVar           =  getDoubleVariable("maximumPeriod");

      Variable          lptmr_psr_pcsVar           =  getVariable("lptmr_psr_pcs");
      Variable          lptmr_psr_prescalerVar     =  getVariable("lptmr_psr_prescaler");
      Variable          lptmr_psr_glitchFilterVar  =  getVariable("lptmr_psr_glitchFilter");
      BooleanVariable   lptmr_csr_tmsVar           =  getBooleanVariable("lptmr_csr_tms");
      Variable          lptmr_csr_tpsVar           =  getVariable("lptmr_csr_tps");
      Variable          lptmr_csr_tppVar           =  getVariable("lptmr_csr_tpp");

      LongVariable      lptmr_cmrVar               =  getLongVariable("lptmr_cmr_compare");
      DoubleVariable    lptmr_cmrPeriodVar         =  getDoubleVariable("lptmr_cmrPeriod");
      DoubleVariable    lptmr_cmrFrequencyVar      =  getDoubleVariable("lptmr_cmrFrequency");

      // Enable/disable parameters that depend on mode
      boolean lptmr_csr_tms = lptmr_csr_tmsVar.getValueAsBoolean();
      lptmr_csr_tpsVar.enable(lptmr_csr_tms);
      lptmr_csr_tppVar.enable(lptmr_csr_tms);
      lptmr_cmrPeriodVar.enable(!lptmr_csr_tms);
      lptmr_cmrFrequencyVar.enable(!lptmr_csr_tms);
      lptmr_psr_glitchFilterVar.enable(lptmr_csr_tms);
      lptmr_psr_prescalerVar.enable(!lptmr_csr_tms);
      maximumPeriodVar.enable(!lptmr_csr_tms);
      lptmr_cmrPeriodVar.enable(!lptmr_csr_tms);
      lptmr_cmrFrequencyVar.enable(!lptmr_csr_tms);
      
      Variable clockSourceVar = null;
      
      switch((int)lptmr_psr_pcsVar.getValueAsLong()) {
      default:
         lptmr_psr_pcsVar.setValue(0);
      case 0: 
         clockSourceVar = getVariable("/MCG/system_mcgirclk_clock[0]");
         break;
      case 1:
         clockSourceVar = getVariable("/PMC/system_low_power_clock");
         break;
      case 2:
         clockSourceVar = getVariable("/SIM/system_erclk32k_clock");
         break;
      case 3:
         clockSourceVar = getVariable(osc0_peripheral+"/oscer_clock");
         break;
      }
      boolean clockChanged = (variable == null) ||    // Initial setup
            (variable == lptmr_csr_tmsVar) ||         // Switch divider used
            (variable == lptmr_psr_pcsVar) ||         // Clock source selection change
            (variable == clockSourceVar) ||           // Change in the currently selected clock source
            (variable == lptmr_psr_glitchFilterVar)|| // Prescaler changed
            (variable == lptmr_psr_prescalerVar);     // Prescaler changed

      boolean isPulseMode = lptmr_csr_tmsVar.getValueAsBoolean();
      
      Variable prescaleOrGlitch = isPulseMode?lptmr_psr_glitchFilterVar:lptmr_psr_prescalerVar;
      boolean pbypass           = (prescaleOrGlitch.getValueAsLong() & 0b100) != 0;
      
      if (variable == prescaleOrGlitch) {
         // Update bypass affected things
         if (pbypass) {
            // Clock divider bypassed
            clockFrequencyVar.setOrigin(clockSourceVar.getOrigin());
            clockPeriodVar.setOrigin(clockSourceVar.getOrigin());
         }
         else {
            // Clock divider used
            clockFrequencyVar.setOrigin(clockSourceVar.getOrigin() + " frequency divided by lptmr_psr_prescaler");
            clockPeriodVar.setOrigin(clockSourceVar.getOrigin() + " period multiplied by lptmr_psr_prescaler");
         }
      }
      // Current values
      double clockFrequency = clockSourceVar.getValueAsLong();
      if (!pbypass) {
         // Clock divider used
         clockFrequency = clockFrequency/(1L<<(prescaleOrGlitch.getValueAsLong()+1));
      }
      double clockPeriod    = (clockFrequency==0)?0:(1/clockFrequency);

      clockFrequencyVar.setStatus(clockSourceVar.getFilteredStatus());
      if (clockChanged) {
         // Update clockFrequency, clockPeriod
         clockFrequencyVar.setValue(clockFrequency);
         clockPeriodVar.setStatus(clockSourceVar.getStatus());
         if (clockFrequency == 0) {
            clockFrequencyVar.enable(false);
            clockPeriodVar.enable(false);
            clockPeriod = 0.0;
            clockPeriodVar.setValue(clockPeriod);
         }
         else {
            clockFrequencyVar.enable(true);
            clockPeriodVar.enable(true);
            clockPeriod = 1/clockFrequency;
            clockPeriodVar.setValue(clockPeriod);
         }
      }
      
      double maximumPeriod = clockPeriod*65536;
      maximumPeriodVar.setValue(maximumPeriod);
      lptmr_cmrPeriodVar.setMax(maximumPeriod);
      long lptmr_cmr      = lptmr_cmrVar.getValueAsLong();

      if (clockChanged) {
         Double cmrFrequency = clockFrequency/lptmr_cmr; // cmr==0 produced infinity which is OK!
         Double cmrPeriod    = clockPeriod*lptmr_cmr;
         lptmr_cmrPeriodVar.setValue(cmrPeriod);
         lptmr_cmrFrequencyVar.setValue(cmrFrequency);
      }
      else if (variable != null) {
         if (variable.equals(lptmr_cmrVar)) {
            Double cmrFrequency = clockFrequency/lptmr_cmr; // cmr==0 produced infinity which is OK!
            Double cmrPeriod    = clockPeriod*lptmr_cmr;
            lptmr_cmrPeriodVar.setValue(cmrPeriod);
            lptmr_cmrFrequencyVar.setValue(cmrFrequency);
         }
         else if (variable.equals(lptmr_cmrPeriodVar)) {
            // Calculate rounded value
            lptmr_cmr = Math.round(lptmr_cmrPeriodVar.getValueAsDouble()*clockFrequency);
            Double cmrFrequency = clockFrequency/lptmr_cmr; // cmr==0 produced infinity which is OK!
            Double cmrPeriod    = clockPeriod*lptmr_cmr;
            // Update
            lptmr_cmrVar.setValue(lptmr_cmr);
            // Need to show effect of rounding
            lptmr_cmrPeriodVar.setValue(cmrPeriod);
            lptmr_cmrFrequencyVar.setValue(cmrFrequency);
         }
         else if (variable.equals(lptmr_cmrFrequencyVar)) {
            // Calculate rounded value
            Double cmrFrequency = lptmr_cmrFrequencyVar.getValueAsDouble();
            if (cmrFrequency<=(clockFrequency/65535)) {
               lptmr_cmr = 65535;
            }
            else {
               lptmr_cmr = Math.round(clockFrequency/cmrFrequency);
            }
            cmrFrequency = clockFrequency/lptmr_cmr; // cmr==0 produced infinity which is OK!
            Double cmrPeriod    = clockPeriod*lptmr_cmr;
            // Update
            lptmr_cmrVar.setValue(lptmr_cmr);
            // Need to show effect of rounding
            lptmr_cmrPeriodVar.setValue(cmrPeriod);
            lptmr_cmrFrequencyVar.setValue(cmrFrequency);
         }
      }
   }
   
   @Override
   protected void createDependencies() throws Exception {
      final String osc0_peripheral = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      
      final String externalVariables[] = {
            "/MCG/system_mcgirclk_clock",
            "/PMC/system_low_power_clock",
            "/SIM/system_erclk32k_clock",
            osc0_peripheral+"/oscer_clock",
      };

      addToWatchedVariables(externalVariables);
   }

}