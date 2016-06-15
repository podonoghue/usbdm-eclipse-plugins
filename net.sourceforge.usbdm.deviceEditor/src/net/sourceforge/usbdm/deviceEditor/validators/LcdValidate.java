package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.NumericListVariable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings
 
 * Used for:
 *     osc0
 *     osc0_div
 */
public class LcdValidate extends Validator {
   
   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/SIM/system_erclk32k_clock",
         "/MCG/system_mcgirclk_clock",
         "/OSC0/system_oscerclk_clock",
   };

   public LcdValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to validate LCD settings
    */
   @Override
   public void validate(Variable variable) {
      
      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      
      Variable       system_erclk32k_clockVar        =  getVariable("/SIM/system_erclk32k_clock");
      Variable       system_mcgirclk_clockVar        =  getVariable("/MCG/system_mcgirclk_clock");
      Variable       system_oscerclk_clockVar        =  getVariable("/OSC0/system_oscerclk_clock");
      Variable       lcd_gcr_clockVar                =  getVariable("lcd_gcr_clock");
      Variable       lcd_gcr_altdivVar               =  getVariable("lcd_gcr_altdiv");
      Variable       lcdClockVar                     =  getVariable("lcdClock");

      Variable       lcd_gcr_rvenVar                 =  getVariable("lcd_gcr_rven");
      Variable       lcd_gcr_rvtrimVar               =  getVariable("lcd_gcr_rvtrim");
      
      lcd_gcr_rvtrimVar.enable(lcd_gcr_rvenVar.getValueAsBoolean());
      
      Variable             lcd_gcr_dutyVar     =  getVariable("lcd_gcr_duty");
      NumericListVariable  backplanesVar       =  (NumericListVariable) getVariable("backplanes");
//      NumericListVariable  frontplanesVar      =  (NumericListVariable) getVariable("frontplanes");
      
      
      backplanesVar.setListSize((int)lcd_gcr_dutyVar.getValueAsLong()+1);
      
      double divider = 1<<(3*lcd_gcr_altdivVar.getValueAsLong());
            
      switch ((int)lcd_gcr_clockVar.getValueAsLong()) {
      default:
         lcd_gcr_clockVar.setValue(0);
      case 0: 
         lcd_gcr_altdivVar.enable(false);
         lcdClockVar.setValue(system_erclk32k_clockVar.getValueAsLong());
         lcdClockVar.setOrigin(system_erclk32k_clockVar.getOrigin());
         lcdClockVar.setStatus(system_erclk32k_clockVar.getFilteredStatus());
         break;
      case 1: 
         lcd_gcr_altdivVar.enable(true);
         lcdClockVar.setValue(system_mcgirclk_clockVar.getValueAsLong() / divider);
         lcdClockVar.setOrigin(system_mcgirclk_clockVar.getOrigin() + " / ALTDIV");
         lcdClockVar.setStatus(system_mcgirclk_clockVar.getFilteredStatus());
         break;
      case 2: 
         lcd_gcr_altdivVar.enable(true);
         lcdClockVar.setValue(system_oscerclk_clockVar.getValueAsLong() / divider);
         lcdClockVar.setOrigin(system_oscerclk_clockVar.getOrigin() + " / ALTDIV");
         lcdClockVar.setStatus(system_oscerclk_clockVar.getFilteredStatus());
         break;
      }
   }
}
