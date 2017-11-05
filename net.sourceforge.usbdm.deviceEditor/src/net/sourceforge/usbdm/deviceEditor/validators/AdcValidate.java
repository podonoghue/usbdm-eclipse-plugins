package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
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

      // Clock Mapping
      //=================
      final StringVariable   osc0_peripheralVar   = getStringVariable("/SIM/osc0_peripheral");
      final LongVariable     osc0_oscer_clockVar  = getLongVariable(osc0_peripheralVar.getValueAsString()+"/oscer_clock");

      // Variables
      //=================================
      DoubleVariable clockFrequencyVar    = (DoubleVariable) getVariable("clockFrequency");
      Variable adc_cfg1_adiclkVar         = getVariable("adc_cfg1_adiclk");
      Variable adc_cfg1_adivVar           = getVariable("adc_cfg1_adiv");
      Variable adcInternalClockVar        = getVariable("adcInternalClock");

      Variable system_bus_clockVar        = getVariable("/SIM/system_bus_clock");

      Variable adc_cfg1_adlpcVar          = getVariable("adc_cfg1_adlpc");
      Variable adc_cfg2_adhscVar          = getVariable("adc_cfg2_adhsc");
      Variable adc_cfg1_modeVar           = getVariable("adc_cfg1_mode");

      BooleanVariable adc_cfg1_adlsmpVar  = getBooleanVariable("adc_cfg1_adlsmp");
      ChoiceVariable  adc_cfg2_adlstsVar  = getChoiceVariable("adc_cfg2_adlsts");

      LongVariable    low_comparison_valueVar  = getLongVariable("low_comparison_value");
      LongVariable    high_comparison_valueVar = getLongVariable("high_comparison_value");
      LongVariable    adc_cv1Var = getLongVariable("adc_cv1");
      LongVariable    adc_cv2Var = getLongVariable("adc_cv2");

      ChoiceVariable  adc_sc2_compareVar = getChoiceVariable("adc_sc2_compare");

      LongVariable adc_sc2_acfeVar  = getLongVariable("adc_sc2_acfe");
      LongVariable adc_sc2_acfgtVar = getLongVariable("adc_sc2_acfgt");
      LongVariable adc_sc2_acrenVar = getLongVariable("adc_sc2_acren");

      int cv1 = 0;
      int cv2 = 0;

      int low  = (int)low_comparison_valueVar.getValueAsLong();
      int high = (int)high_comparison_valueVar.getValueAsLong();

      int compareChoice = (int)adc_sc2_compareVar.getValueAsLong();

      boolean adc_sc2_acfe  = true;
      boolean adc_sc2_acfgt = false;
      boolean adc_sc2_acren = false;

      switch (compareChoice) {
      case 0: // Disabled
         adc_sc2_acfe = false;
         break;
      case 1: // ADC < low(CV1)
         cv1 = low;
         adc_sc2_acfgt = false;  adc_sc2_acren = false;
         break;
      case 2: // ADC >= low(CV1)
         cv1 = low;
         adc_sc2_acfgt = true;   adc_sc2_acren = false;
         break;
      case 3: // (ADC<low(CV1)) or (high(CV2)<ADC)      CV1<CV2
         cv1 = low; cv2 = high;
         adc_sc2_acfgt = false;  adc_sc2_acren = true;
         break;
      case 4: // (low(CV2)<ADC<high(CV1))             CV1>CV2 <==> CV2<CV1
         cv2 = low; cv1 = high;
         adc_sc2_acfgt = false;  adc_sc2_acren = true;
         break;
      case 5: // (low(CV1)<=ADC<=high(CV2)            CV1<CV2
         cv1 = low; cv2 = high;
         adc_sc2_acfgt = true;  adc_sc2_acren = true;
         break;
      case 6: // (ADC<=low(CV2)) or (high(CV1<=ADC))    CV1>CV2 <==> CV2<CV1
         cv2 = low; cv1 = high;
         adc_sc2_acfgt = true;  adc_sc2_acren = true;
         break;
      }
      adc_cv1Var.enable(compareChoice>=1);
      adc_cv2Var.enable(compareChoice>=3);
      low_comparison_valueVar.enable(compareChoice>=1);
      high_comparison_valueVar.enable(compareChoice>=3);
      adc_cv1Var.setValue(cv1);
      adc_cv2Var.setValue(cv2);

      adc_sc2_acfeVar.setValue(adc_sc2_acfe);

      adc_cfg2_adlstsVar.enable(adc_cfg1_adlsmpVar.getValueAsBoolean());

      adc_sc2_acfeVar.setValue(adc_sc2_acfe);
      adc_sc2_acfgtVar.setValue(adc_sc2_acfgt);
      adc_sc2_acrenVar.setValue(adc_sc2_acren);

      // Varies with power settings etc
      adcInternalClockVar.setValue(ADC_CLOCK_VALUES[(int)(2*adc_cfg1_adlpcVar.getValueAsLong()+adc_cfg2_adhscVar.getValueAsLong())]);

      LongVariable system_irc48m_clockVar = safeGetLongVariable("/MCG/system_irc48m_clock");

      Variable clockSourceVar = null;
      double clockFrequency;

      switch((int)adc_cfg1_adiclkVar.getValueAsLong()) {
      case 0: 
         clockSourceVar = system_bus_clockVar;
         clockFrequency = system_bus_clockVar.getValueAsLong();
         break;
      case 1:
         /* 
          * TODO - better method of clock selection
          * ALTCLK2: Varies with device but assume irc48m if available else busClock/2
          */
         if (system_irc48m_clockVar != null) {
            clockSourceVar = system_irc48m_clockVar;
            clockFrequency = system_irc48m_clockVar.getValueAsLong();
         }
         else {
            clockSourceVar = system_bus_clockVar;
            clockFrequency = system_bus_clockVar.getValueAsLong()/2.0;
         }
         break;
      case 2:
         clockSourceVar = osc0_oscer_clockVar;
         clockFrequency = osc0_oscer_clockVar.getValueAsLong();
         break;
      default:
         adc_cfg1_adiclkVar.setValue(1);
      case 3:
         clockSourceVar = adcInternalClockVar;
         clockFrequency = adcInternalClockVar.getValueAsLong();
         break;
      }
      // Set MIN and MAX before updating value
      if (adc_cfg1_modeVar.getValueAsLong()>=2) {
         clockFrequencyVar.setMin(FADC_HIGH_RES_MIN);
         clockFrequencyVar.setMax(FADC_HIGH_RES_MAX);
      }
      else {
         clockFrequencyVar.setMin(FADC_LOW_RES_MIN);
         clockFrequencyVar.setMax(FADC_LOW_RES_MAX);
      }
      clockFrequency = clockFrequency/(1L<<adc_cfg1_adivVar.getValueAsLong());
      clockFrequencyVar.setValue(clockFrequency);
      clockFrequencyVar.setStatus(clockSourceVar.getFilteredStatus());
      clockFrequencyVar.setOrigin(clockSourceVar.getOrigin() + " divided by adc_cfg1_adiv");
   }

   @Override
   protected void createDependencies() throws Exception {
      // Clock Mapping
      //=================
      final String   osc0_peripheral = getStringVariable("/SIM/osc0_peripheral").getValueAsString();

      final String externalVariables[] = {
            "/MCG/system_irc48m_clock",
            "/SIM/system_bus_clock",
            osc0_peripheral+"/oscer_clock",
      };
      addToWatchedVariables(externalVariables);
   }

}