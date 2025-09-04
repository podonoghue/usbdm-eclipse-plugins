package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate LPTMR settings
 */
public class LptmrValidate extends PeripheralValidator {

   public LptmrValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   private DoubleVariable counterFrequencyVar;
   private DoubleVariable lptmr_cmrPeriodVar;
   private DoubleVariable lptmr_cmrFrequencyVar;
   private LongVariable   lptmr_cmr_compareVar;
   private long           maximumCountValue;
   
   /**
    * Class to determine LPTMR settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable, int properties) throws Exception {
      
      super.validate(variable, properties);

//      System.err.println("LptmrValidate.validate("+variable+")");
      
      double counterFrequency = counterFrequencyVar.getValueAsDouble();
      double clockPeriod    = (counterFrequency==0)?0:(1/counterFrequency);
      
      double maximumPeriod = clockPeriod*(maximumCountValue+1);
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
            if (cmrFrequency <= (counterFrequency/maximumCountValue)) {
               lptmr_cmr = maximumCountValue;
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
      
      ArrayList<String> externalVariablesList = new ArrayList<String>();
      
      counterFrequencyVar        =  getDoubleVariable("counterFrequency",   externalVariablesList);
      lptmr_cmrPeriodVar         =  getDoubleVariable("lptmr_cmrPeriod" ,   externalVariablesList);
      lptmr_cmrFrequencyVar      =  getDoubleVariable("lptmr_cmrFrequency", externalVariablesList);
      lptmr_cmr_compareVar       =  getLongVariable("lptmr_cmr_compare",    externalVariablesList);
      
      BooleanVariable counterIs32bitVar          =  safeGetBooleanVariable("counterIs32bit");
      maximumCountValue = (counterIs32bitVar!=null)?4294967296L:65535L;
      
      addToWatchedVariables(externalVariablesList);
      
      // Don't add default dependencies
      return false;
   }
}