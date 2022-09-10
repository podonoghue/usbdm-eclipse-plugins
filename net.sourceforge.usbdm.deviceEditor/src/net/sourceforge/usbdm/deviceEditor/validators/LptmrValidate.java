package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
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
      
      // Variables
      //=================================
      DoubleVariable    counterFrequencyVar        =  getDoubleVariable("counterFrequency");
      DoubleVariable    maximumPeriodVar           =  getDoubleVariable("maximumPeriod");
      
      LongVariable      inputClockFrequencyVar     =  getLongVariable("inputClockFrequency");

      DoubleVariable    filterDurationVar          =  getDoubleVariable("filterDuration");
      
      ChoiceVariable    lptmr_psr_prescalerVar     =  getChoiceVariable("lptmr_psr_prescaler");
      ChoiceVariable    lptmr_psr_glitchFilterVar  =  getChoiceVariable("lptmr_psr_glitchFilter");
      BooleanVariable   lptmr_csr_tmsVar           =  getBooleanVariable("lptmr_csr_tms");
      Variable          lptmr_csr_tpsVar           =  getVariable("lptmr_csr_tps");
      Variable          lptmr_csr_tppVar           =  getVariable("lptmr_csr_tpp");

      LongVariable      lptmr_cmrVar               =  getLongVariable("lptmr_cmr_compare");
      DoubleVariable    lptmr_cmrPeriodVar         =  getDoubleVariable("lptmr_cmrPeriod");
      DoubleVariable    lptmr_cmrFrequencyVar      =  getDoubleVariable("lptmr_cmrFrequency");

      // Enable/disable parameters that depend on mode
      boolean isPulseMode = lptmr_csr_tmsVar.getValueAsBoolean();

      // Available in timer mode
      lptmr_psr_prescalerVar.enable(!isPulseMode);
      lptmr_cmrPeriodVar.enable(!isPulseMode);
      lptmr_cmrFrequencyVar.enable(!isPulseMode);
      maximumPeriodVar.enable(!isPulseMode);
      counterFrequencyVar.enable(!isPulseMode);

      // Available in pulse counting mode
      lptmr_psr_glitchFilterVar.enable(isPulseMode);
      lptmr_csr_tpsVar.enable(isPulseMode);
      lptmr_csr_tppVar.enable(isPulseMode);
      filterDurationVar.enable(isPulseMode);
      
      long inputClockFrequency = inputClockFrequencyVar.getValueAsLong();

      // For Timer mode
      double counterFrequency = inputClockFrequency;
      if (lptmr_psr_prescalerVar.getValueAsLong() != 0) {
         // Clock divider used
         long divider = (1L<<lptmr_psr_prescalerVar.getValueAsLong());
         counterFrequency = inputClockFrequency/divider;
         counterFrequencyVar.setOrigin(inputClockFrequencyVar.getOrigin() + "\n/"+divider+"[selected by lptmr_psr_prescaler]");
      }
      else {
         // Clock divider bypassed
         counterFrequencyVar.setOrigin(inputClockFrequencyVar.getOrigin());
      }

      // For Pulse counting mode
      long  filterDurationInTicks = 0;
      if (lptmr_psr_glitchFilterVar.getValueAsLong() != 0) {
         // Clock divider used
         filterDurationInTicks = 1L<<(lptmr_psr_glitchFilterVar.getValueAsLong());
      }
      if (inputClockFrequency != 0) {
         filterDurationVar.setValue((double)filterDurationInTicks/inputClockFrequency);
      }
      
      counterFrequencyVar.setStatus(inputClockFrequencyVar.getFilteredStatus());
      counterFrequencyVar.setValue(counterFrequency);
      double clockPeriod    = (counterFrequency==0)?0:(1/counterFrequency);
      
      double maximumPeriod = clockPeriod*65536;
      maximumPeriodVar.setValue(maximumPeriod);
      lptmr_cmrPeriodVar.setMax(maximumPeriod);
      
      long lptmr_cmr = lptmr_cmrVar.getValueAsLong();

      if (lptmr_cmrPeriodVar.equals(variable)) {
         // lptmr_cmrPeriod -> cmr
         // Calculate rounded value
         lptmr_cmr = Math.round(lptmr_cmrPeriodVar.getValueAsDouble()*counterFrequency);

         // Update to rounded value
         lptmr_cmrVar.setValue(lptmr_cmr);

         // Need to show effect of rounding
         lptmr_cmrPeriodVar.setValue(clockPeriod*lptmr_cmr);
         lptmr_cmrFrequencyVar.setValue(counterFrequency/lptmr_cmr); // cmr==0 produced infinity which is OK!
      }
      else if (lptmr_cmrFrequencyVar.equals(variable)) {
         // lptmr_cmrFrequency -> cmr
         // Calculate rounded value
         Double cmrFrequency = lptmr_cmrFrequencyVar.getValueAsDouble();
         if (cmrFrequency <= (counterFrequency/65535)) {
            lptmr_cmr = 65535;
         }
         else {
            lptmr_cmr = Math.round(counterFrequency/cmrFrequency);
         }
         // Update
         lptmr_cmrVar.setValue(lptmr_cmr);

         // Need to show effect of rounding
         lptmr_cmrPeriodVar.setValue(clockPeriod*lptmr_cmr);
         lptmr_cmrFrequencyVar.setValue(counterFrequency/lptmr_cmr); // cmr==0 produced infinity which is OK!
      }
      else {
         // Default update cmr -> ...
         lptmr_cmrPeriodVar.setValue(clockPeriod*lptmr_cmr);
         lptmr_cmrFrequencyVar.setValue(counterFrequency/lptmr_cmr); // cmr==0 produced infinity which is OK!
      }
   }
   
   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();
      
//      addToWatchedVariables(externalVariables);
   }

}