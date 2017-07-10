package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
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
         "/MCG/system_irc48m_clock",
         "/SIM/system_bus_clock",
         "/OSC0/system_oscerclk_clock",
         "/SIM/system_bus_clock",
   };

   public AdcValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine LPTMR settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {

      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      // Variables
      //=================================
      DoubleVariable clockFrequencyVar    = (DoubleVariable) getVariable("clockFrequency");
      Variable adc_cfg1_adiclkVar         = getVariable("adc_cfg1_adiclk");
      Variable adc_cfg1_adivVar           = getVariable("adc_cfg1_adiv");
      Variable adcInternalClockVar        = getVariable("adcInternalClock");

      Variable system_bus_clockVar        = getVariable("/SIM/system_bus_clock");
      Variable system_oscerclk_clockVar   = getVariable("/OSC0/system_oscerclk_clock");

      Variable adc_cfg1_adlpcVar          = getVariable("adc_cfg1_adlpc");
      Variable adc_cfg2_adhscVar          = getVariable("adc_cfg2_adhsc");
      Variable adc_cfg1_modeVar           = getVariable("adc_cfg1_mode");

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
         clockSourceVar = system_oscerclk_clockVar;
         clockFrequency = system_oscerclk_clockVar.getValueAsLong();
         break;
      default:
         adc_cfg1_adiclkVar.setValue(1);
      case 3:
         clockSourceVar = adcInternalClockVar;
         clockFrequency = adcInternalClockVar.getValueAsLong();
         break;
      }
      clockFrequency = clockFrequency/(1L<<adc_cfg1_adivVar.getValueAsLong());
      clockFrequencyVar.setValue(clockFrequency);
      clockFrequencyVar.setStatus(clockSourceVar.getFilteredStatus());
      clockFrequencyVar.setOrigin(clockSourceVar.getOrigin() + " divided by adc_cfg1_adiv");

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