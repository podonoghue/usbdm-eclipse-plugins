package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 *
 * Used for:
 *     osc0
 *     osc0_div
 */
/**
 * @author peter
 *
 */
public class RtcValidate extends PeripheralValidator {

   // Constants
   private boolean          rtcSharesPins         = false;
   
   /** /OSC0/osc_input_freq (only used if shared RTC/OSC0 share crystal pins */
   private LongVariable     osc0_input_freqVar    = null;

   /** /OSC0/osc32k_clock (only used of shared RTC/OSC0 share crystal pins */
   private LongVariable     osc0_osc32k_clockVar  = null;

   /** /RTC/osc_input_freq - may be linked by software to /OSC0/osc_input_freq */
   private LongVariable     rtc_osc_input_freqVar = null;

   /** /RTC/osc_clock - may be linked by software to /OSC0/osc_clock */
   private LongVariable     rtc_osc_clockVar      = null;

   /**
    * @param peripheral Associated peripheral
    * @param values     Not used
    */
   public RtcValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to determine RTC oscillator settings
    * 
    * Outputs rtcclk_clock, rtcclk_gated_clock,
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {

      if (rtcSharesPins) {

         long oscFrequency = rtc_osc_input_freqVar.getValueAsLong();

         // Link rtc_osc_input_freqVar <=> osc0_input_freqVar
         if (rtc_osc_input_freqVar.equals(variable)) {
            osc0_input_freqVar.setValue(oscFrequency);
         }
         else {
            oscFrequency = osc0_input_freqVar.getValueAsLong();
            rtc_osc_input_freqVar.setValue(oscFrequency);
         }
         // Copy osc0_osc32k_clockVar => rtc_osc_clockVar
         rtc_osc_clockVar.setValue(osc0_osc32k_clockVar.getValueAsLong());
         rtc_osc_clockVar.setStatus(osc0_osc32k_clockVar.getStatus());
         rtc_osc_clockVar.setOrigin(osc0_osc32k_clockVar.getOrigin());
      }
   }
   
   @Override
   protected boolean createDependencies() throws Exception {
      super.createDependencies();
      
      ArrayList<String> externalVariablesList = new ArrayList<String>();
      
      rtcSharesPins         = safeGetStringVariable("/SIM/rtc_shared") != null;

      rtc_osc_input_freqVar = createLongVariableReference("osc_input_freq", externalVariablesList);
      rtc_osc_clockVar      = createLongVariableReference("osc_clock", externalVariablesList);

      if (rtcSharesPins) {
         // RTC uses main oscillator (OSC0) XTAL/EXTAL pins
         //===================================================
         StringVariable osc0_peripheralVar = getStringVariable("/SIM/osc0_peripheral");
         String osc0_peripheralName        = osc0_peripheralVar.getValueAsString();

         osc0_input_freqVar   = createLongVariableReference(osc0_peripheralName+"/osc_input_freq", externalVariablesList);
         osc0_osc32k_clockVar = createLongVariableReference(osc0_peripheralName+"/osc32k_clock",   externalVariablesList);

         rtc_osc_clockVar.setDescription(osc0_osc32k_clockVar.getDescription());
         rtc_osc_clockVar.setToolTip(osc0_osc32k_clockVar.getToolTip());
         rtc_osc_clockVar.setName(osc0_osc32k_clockVar.getName());

//         rtc_cr_osceVar.setToolTip(
//               "Enable "+osc0_peripheralName.substring(1)+" as 32kHz RTC oscillator\n"+
//               "Note: this disables "+osc0_peripheralName.substring(1)+" control by MCG");

         rtc_osc_input_freqVar.setDescription(osc0_input_freqVar.getDescription());
         rtc_osc_input_freqVar.setToolTip(osc0_input_freqVar.getToolTip());
         rtc_osc_input_freqVar.setMin(osc0_input_freqVar.getMin());
         rtc_osc_input_freqVar.setMax(osc0_input_freqVar.getMax());
         rtc_osc_input_freqVar.setOrigin(osc0_input_freqVar.getOrigin());
         rtc_osc_input_freqVar.setDerived(true);
      }
      
      addSpecificWatchedVariables(externalVariablesList);
      
      return false;
   }
}
