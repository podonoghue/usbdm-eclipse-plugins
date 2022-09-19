package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 *     adc0_diff
 */
public class AdcValidate extends PeripheralValidator {

   // Input clock ranges < 13-bit resolution
   private static final long FADC_LOW_RES_MIN  =  1000000;
   private static final long FADC_LOW_RES_MAX  = 18000000;
   
   // Input clock ranges 16-bit resolution
   private static final long FADC_HIGH_RES_MIN =  2000000;
   private static final long FADC_HIGH_RES_MAX = 12000000;

   // Internal clock speeds
   private static final long FADC_LP1_HS0_MAX = 2400000;
   private static final long FADC_LP1_HS1_MAX = 4000000;
   private static final long FADC_LP0_HS0_MAX = 5200000;
   private static final long FADC_LP0_HS1_MAX = 6200000;

   private Variable        adcInternalClockVar      = null;
   private Variable        adc_cfg1_adlpcVar        = null;
   private Variable        adc_cfg2_adhscVar        = null;
   private Variable        adc_cfg1_modeVar         = null;
   private LongVariable    adc_cv1Var               = null;
   private LongVariable    adc_cv2Var               = null;
   private ChoiceVariable  adc_sc2_compareVar       = null;
   private LongVariable    adcClockFrequencyVar        = null;
   
   private static long ADC_CLOCK_VALUES[] = {FADC_LP0_HS0_MAX, FADC_LP0_HS1_MAX, FADC_LP1_HS0_MAX, FADC_LP1_HS1_MAX};

   public AdcValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine LPTMR settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {

      super.validate(variable);

      int compareChoice = (int)adc_sc2_compareVar.getValueAsLong();

      adc_cv1Var.enable(compareChoice>=1);
      adc_cv2Var.enable(compareChoice>=3);

      // Internal clock frequency varies with ADC power settings etc
      int index = (int)(2*adc_cfg1_adlpcVar.getValueAsLong()+adc_cfg2_adhscVar.getValueAsLong());
      adcInternalClockVar.setValue(ADC_CLOCK_VALUES[index]);

      // Update MIN and MAX
      if (adc_cfg1_modeVar.getValueAsLong()>=2) {
         adcClockFrequencyVar.setMin(FADC_HIGH_RES_MIN);
         adcClockFrequencyVar.setMax(FADC_HIGH_RES_MAX);
      }
      else {
         adcClockFrequencyVar.setMin(FADC_LOW_RES_MIN);
         adcClockFrequencyVar.setMax(FADC_LOW_RES_MAX);
      }
   }

   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();
      
      // Variable to watch
//      ArrayList<String> variablesToWatch = new ArrayList<String>();

      adcInternalClockVar       = getVariable("adcInternalClock");
      adc_cfg1_adlpcVar         = getVariable("adc_cfg1_adlpc");
      adc_cfg2_adhscVar         = getVariable("adc_cfg2_adhsc");
      adc_cfg1_modeVar          = getVariable("adc_cfg1_mode");
                                
      adc_cv1Var                = getLongVariable("adc_cv1");
      adc_cv2Var                = getLongVariable("adc_cv2");
                                
      adc_sc2_compareVar        = getChoiceVariable("adc_sc2_compare");
                                
      adcClockFrequencyVar      = getLongVariable("adcClockFrequency");
      
//      addToWatchedVariables(variablesToWatch);
   }

}