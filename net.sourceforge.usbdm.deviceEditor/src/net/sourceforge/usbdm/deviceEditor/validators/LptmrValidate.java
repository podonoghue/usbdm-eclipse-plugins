package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
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
      
      DoubleVariable    filterDurationVar          =  getDoubleVariable("filterDuration");
      
      ChoiceVariable    lptmr_psr_prescalerVar     =  getChoiceVariable("lptmr_psr_prescaler");
      ChoiceVariable    lptmr_psr_glitchFilterVar  =  getChoiceVariable("lptmr_psr_glitchFilter");
      BooleanVariable   lptmr_csr_tmsVar           =  getBooleanVariable("lptmr_csr_tms");
      Variable          lptmr_csr_tpsVar           =  getVariable("lptmr_csr_tps");
      Variable          lptmr_csr_tppVar           =  getVariable("lptmr_csr_tpp");

      LongVariable      lptmr_cmr_compareVar       =  getLongVariable("lptmr_cmr_compare");
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
      
      double counterFrequency = counterFrequencyVar.getValueAsDouble();
      double clockPeriod    = (counterFrequency==0)?0:(1/counterFrequency);
      
      double maximumPeriod = clockPeriod*65536;
      lptmr_cmrPeriodVar.setMax(maximumPeriod);
      lptmr_cmrPeriodVar.setMin(2*clockPeriod);
      lptmr_cmrFrequencyVar.setMax(1/(2*clockPeriod));
      lptmr_cmrFrequencyVar.setMin(1/maximumPeriod);
      
      
      long lptmr_cmr = lptmr_cmr_compareVar.getValueAsLong();

      // These updates involves a loop so suppress initially
      if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
         
         if (lptmr_cmrPeriodVar.equals(variable)) {
            // lptmr_cmrPeriod -> cmr
            // Calculate rounded value
            lptmr_cmr = Math.round(lptmr_cmrPeriodVar.getValueAsDouble()*counterFrequency)-1;

            // Update to rounded value
            lptmr_cmr_compareVar.setValue(lptmr_cmr);
         }
         else if (lptmr_cmrFrequencyVar.equals(variable)) {
            // lptmr_cmrFrequency -> cmr
            // Calculate rounded value
            Double cmrFrequency = lptmr_cmrFrequencyVar.getValueAsDouble();
            if (cmrFrequency <= (counterFrequency/65535)) {
               lptmr_cmr = 65535;
            }
            else {
               lptmr_cmr = Math.round(counterFrequency/cmrFrequency)-1;
            }
            // Update
            lptmr_cmr_compareVar.setValue(lptmr_cmr);
         }
      }
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
      return super.createDependencies();
      // No external dependencies
   }
}