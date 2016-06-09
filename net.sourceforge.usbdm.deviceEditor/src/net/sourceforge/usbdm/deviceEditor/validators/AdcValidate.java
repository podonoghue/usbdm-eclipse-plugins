package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     adc0_diff
 */
public class AdcValidate extends Validator {
   
   private static final long FADC_LOW_RES_MIN  =  1000000;
   private static final long FADC_LOW_RES_MAX  = 18000000;
   private static final long FADC_HIGH_RES_MIN =  2000000;
   private static final long FADC_HIGH_RES_MAX = 12000000;
   
   private static final long FADC_LP1_HS0_MAX = 2400000;
   private static final long FADC_LP1_HS1_MAX = 4000000;
   private static final long FADC_LP0_HS0_MAX = 5200000;
   private static final long FADC_LP0_HS1_MAX = 6200000;
   
   private static long ADC_CLOCK_VALUES[] = {FADC_LP0_HS0_MAX, FADC_LP0_HS1_MAX, FADC_LP1_HS0_MAX, FADC_LP1_HS1_MAX};
   
   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/SIM/system_bus_clock",
         "/OSC0/system_oscerclk_clock",
         "/MCG/system_mcgffclk_clock",
         "/SIM/system_bus_clock",
   };

   public AdcValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine LPTMR settings
    */
   @Override
   public void validate(Variable variable) {
      
      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      // OSC
      //=================================
      DoubleVariable    clockFrequencyVar          =  (DoubleVariable) getVariable("clockFrequency");
      Variable          adc_cfg1_adiclkVar         =  getVariable("adc_cfg1_adiclk");
      Variable          adc_cfg1_adivVar           =  getVariable("adc_cfg1_adiv");
      Variable          adcInternalClockVar        =  getVariable("adcInternalClock");

      Variable          system_bus_clockVar        =  getVariable("/SIM/system_bus_clock");
      Variable          system_oscerclk_clockVar   =  getVariable("/OSC0/system_oscerclk_clock");

      Variable          adc_cfg1_adlpcVar          =  getVariable("adc_cfg1_adlpc");
      Variable          adc_cfg2_adhscVar          =  getVariable("adc_cfg2_adhsc");
      Variable          adc_cfg1_modeVar           =  getVariable("adc_cfg1_mode");

      adcInternalClockVar.setValue(ADC_CLOCK_VALUES[(int)(2*adc_cfg1_adlpcVar.getValueAsLong()+adc_cfg2_adhscVar.getValueAsLong())]);
      
      Variable clockSource = null;
      double clockFrequency;
      
      switch((int)adc_cfg1_adiclkVar.getValueAsLong()) {
      case 0: 
         clockSource = system_bus_clockVar;
         clockFrequency = system_bus_clockVar.getValueAsLong();
         break;
      default:
         adc_cfg1_adiclkVar.setValue(1);
      case 1:
         clockSource = system_bus_clockVar;
         clockFrequency = system_bus_clockVar.getValueAsLong()/2.0;
         break;
      case 2:
         clockSource = system_oscerclk_clockVar;
         clockFrequency = system_oscerclk_clockVar.getValueAsLong();
         break;
      case 3:
         clockSource = adcInternalClockVar;
         clockFrequency = adcInternalClockVar.getValueAsLong();
         break;
      }
      clockFrequency = clockFrequency/(1L<<adc_cfg1_adivVar.getValueAsLong());
      clockFrequencyVar.setValue(clockFrequency);
      clockFrequencyVar.setStatus(clockSource.getFilteredStatus());
      clockFrequencyVar.setOrigin(clockSource.getOrigin());
      
      if (adc_cfg1_modeVar.getValueAsLong()>=2) {
         clockFrequencyVar.setMin(FADC_HIGH_RES_MIN);
         clockFrequencyVar.setMax(FADC_HIGH_RES_MAX);
      }
      else {
         clockFrequencyVar.setMin(FADC_LOW_RES_MIN);
         clockFrequencyVar.setMax(FADC_LOW_RES_MAX);
      }
   }

}