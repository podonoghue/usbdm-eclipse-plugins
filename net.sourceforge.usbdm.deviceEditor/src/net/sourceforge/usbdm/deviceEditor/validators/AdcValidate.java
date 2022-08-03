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

   private LongVariable    system_adc_clockVar      = null;
   private Variable        adc_cfg1_adivVar         = null;
   private Variable        adcInternalClockVar      = null;
   private Variable        adc_cfg1_adlpcVar        = null;
   private Variable        adc_cfg2_adhscVar        = null;
   private Variable        adc_cfg1_modeVar         = null;
   private LongVariable    low_comparison_valueVar  = null;
   private LongVariable    high_comparison_valueVar = null;
   private LongVariable    adc_cv1Var               = null;
   private LongVariable    adc_cv2Var               = null;
   private ChoiceVariable  adc_sc2_compareVar       = null;
   private LongVariable    clockFrequencyVar        = null;
   
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

      int low  = (int)low_comparison_valueVar.getValueAsLong();
      int high = (int)high_comparison_valueVar.getValueAsLong();

      int compareChoice = (int)adc_sc2_compareVar.getValueAsLong();

      int cv1 = low;
      int cv2 = high;
      if ((compareChoice == 4)||(compareChoice == 5)) {
         // Values are swapped for these two cases
         cv1 = high;
         cv2 = low;
      }

      adc_cv1Var.enable(compareChoice>=1);
      adc_cv2Var.enable(compareChoice>=3);
      low_comparison_valueVar.enable(compareChoice>=1);
      high_comparison_valueVar.enable(compareChoice>=3);
      adc_cv1Var.setValue(cv1);
      adc_cv2Var.setValue(cv2);

      // Varies with power settings etc
      adcInternalClockVar.setValue(ADC_CLOCK_VALUES[(int)(2*adc_cfg1_adlpcVar.getValueAsLong()+adc_cfg2_adhscVar.getValueAsLong())]);

      // Set MIN and MAX before updating value
      if (adc_cfg1_modeVar.getValueAsLong()>=2) {
         clockFrequencyVar.setMin(FADC_HIGH_RES_MIN);
         clockFrequencyVar.setMax(FADC_HIGH_RES_MAX);
      }
      else {
         clockFrequencyVar.setMin(FADC_LOW_RES_MIN);
         clockFrequencyVar.setMax(FADC_LOW_RES_MAX);
      }
      long clockFrequency = system_adc_clockVar.getValueAsLong();
      clockFrequency = clockFrequency/(1L<<adc_cfg1_adivVar.getValueAsLong());
      clockFrequencyVar.setValue(clockFrequency);
      clockFrequencyVar.setStatus(system_adc_clockVar.getFilteredStatus());
      clockFrequencyVar.setOrigin(system_adc_clockVar.getOrigin() + " divided by adc_cfg1_adiv");
   }

   @Override
   protected void createDependencies() throws Exception {
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      adc_cfg1_adivVar          = getVariable("adc_cfg1_adiv");
      adcInternalClockVar       = getVariable("adcInternalClock");
      adc_cfg1_adlpcVar         = getVariable("adc_cfg1_adlpc");
      adc_cfg2_adhscVar         = getVariable("adc_cfg2_adhsc");
      adc_cfg1_modeVar          = getVariable("adc_cfg1_mode");
                                
      low_comparison_valueVar   = getLongVariable("low_comparison_value");
      high_comparison_valueVar  = getLongVariable("high_comparison_value");
      adc_cv1Var                = getLongVariable("adc_cv1");
      adc_cv2Var                = getLongVariable("adc_cv2");
                                
      adc_sc2_compareVar        = getChoiceVariable("adc_sc2_compare");
                                
      clockFrequencyVar         = getLongVariable("clockFrequency");
      
      system_adc_clockVar = createLongVariableReference(
            "/SIM/system_adc"+getPeripheral().getInstance()+"_clock", variablesToWatch);
      
      addToWatchedVariables(variablesToWatch);
   }

}