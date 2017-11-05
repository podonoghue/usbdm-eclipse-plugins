package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.Vector;

import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate Segment LCD settings
 
 * Used for:
 *     lcd_mkl
 */
public class LcdValidate extends PeripheralValidator {
   
   private static final Status UNMAPPED_PIN_STATUS = new Status("Not all signals are mapped to pins", Severity.WARNING);

   public LcdValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);
   }

   /**
    * Class to validate LCD settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      super.validate(variable);
      
      String         osc0_peripheral           =  getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      LongVariable   osc0_oscer_clockVar       =  getLongVariable(osc0_peripheral+"/oscer_clock");
      
      LongVariable   system_erclk32k_clockVar  =  getLongVariable("/SIM/system_erclk32k_clock");
      LongVariable   system_mcgirclk_clockVar  =  getLongVariable("/MCG/system_mcgirclk_clock");
      ChoiceVariable lcd_gcr_clockVar          =  getChoiceVariable("lcd_gcr_clock");
      ChoiceVariable lcd_gcr_altdivVar         =  getChoiceVariable("lcd_gcr_altdiv");
      DoubleVariable lcdClockVar               =  getDoubleVariable("lcdClock");

      BooleanVariable lcd_gcr_rvenVar          =  getBooleanVariable("lcd_gcr_rven");
      ChoiceVariable lcd_gcr_rvtrimVar         =  getChoiceVariable("lcd_gcr_rvtrim");
      
      lcd_gcr_rvtrimVar.enable(lcd_gcr_rvenVar.getValueAsBoolean());
      
      ChoiceVariable   lcd_gcr_dutyVar   =  getChoiceVariable("lcd_gcr_duty");
      PinListVariable  backplanesVar     =  (PinListVariable) getVariable("backplanes");
      PinListVariable  frontplanesVar    =  (PinListVariable) getVariable("frontplanes");
      
      Vector<Signal> table = getPeripheral().getSignalTables().get(0).table;

      Status unmappedBackplanesMessage = null;
      int backPlaneValues[] = backplanesVar.getValues();
      for (int pinNum:backPlaneValues) {
         Signal entry = table.get(pinNum);
         if ((entry == null) || (entry.getMappedPin().getPin() == Pin.UNASSIGNED_PIN)) {
            unmappedBackplanesMessage = UNMAPPED_PIN_STATUS;
            break;
         }
      }
      backplanesVar.setStatus(unmappedBackplanesMessage);
      
      Status unmappedFrontplanesMessage = null;
      int frontPlaneValues[] = frontplanesVar.getValues();
      for (int pinNum:frontPlaneValues) {
         Signal entry = table.get(pinNum);
         if ((entry == null) || (entry.getMappedPin().getPin() == Pin.UNASSIGNED_PIN)) {
            unmappedFrontplanesMessage = UNMAPPED_PIN_STATUS;
            break;
         }
      }
      frontplanesVar.setStatus(unmappedFrontplanesMessage);
      
      // Number of back-planes is determined by duty-cycle
      backplanesVar.setMinListLength(0);
      backplanesVar.setListLength((int)lcd_gcr_dutyVar.getValueAsLong()+1);

      // Number of front-planes is determined by pins left over from back-planes
      frontplanesVar.setMinListLength(0);
      frontplanesVar.setMaxListLength(63-((int)lcd_gcr_dutyVar.getValueAsLong()+1));

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
         lcdClockVar.setValue(osc0_oscer_clockVar.getValueAsLong() / divider);
         lcdClockVar.setOrigin(osc0_oscer_clockVar.getOrigin() + " / ALTDIV");
         lcdClockVar.setStatus(osc0_oscer_clockVar.getFilteredStatus());
         break;
      }
   }

   @Override
   protected void createDependencies() throws Exception {
      // Clock Mapping
      //=================
      final String   osc0_peripheral    = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      
      final String[] externalVariables = {
            "/SIM/system_erclk32k_clock",
            "/MCG/system_mcgirclk_clock",
            osc0_peripheral+"/oscer_clock",
      };
      addToWatchedVariables(externalVariables);
   }

}
