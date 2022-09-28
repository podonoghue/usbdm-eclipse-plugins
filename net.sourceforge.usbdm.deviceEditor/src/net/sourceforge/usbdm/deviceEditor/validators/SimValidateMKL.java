package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 * Sim_xxx
 */
public class SimValidateMKL extends PeripheralValidator {

   private final long MAX_RUN_CORE_CLOCK_FREQ;
   private final long MAX_RUN_BUS_CLOCK_FREQ;
   private final long MAX_VLPR_CORE_CLOCK_FREQ;
   private final long MAX_VLPR_BUS_CLOCK_FREQ;

//   private ChoiceVariable  sim_copc_coptVar            = null;
//   private BooleanVariable sim_copc_copwVar            = null;
//   private BooleanVariable sim_copc_copdbgenVar        = null;
//   private BooleanVariable sim_copc_copstpenwVar       = null;
//   private ChoiceVariable  sim_copc_copclkselVar       = null;
   
   public SimValidateMKL(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      
      MAX_RUN_CORE_CLOCK_FREQ       = (Long)it.next();
      MAX_RUN_BUS_CLOCK_FREQ        = (Long)it.next();
      
      MAX_VLPR_CORE_CLOCK_FREQ      = (Long)it.next();
      MAX_VLPR_BUS_CLOCK_FREQ       = (Long)it.next();
   }

   /**
    * 
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {

      validateNonindexedVariables(variable);

      for(fIndex=0; fIndex<fDimension; fIndex++) {
         validateIndexVariables(variable);
      }
      fIndex = 0;
   }

   static class StringPair {
      final String left;
      final String right;
      
      StringPair(String left, String right) {
         this.left  = left;
         this.right = right;
      }
    };
    
    static class VariablePair {
       final Variable       left;
       final LongVariable   right;
       
       VariablePair(Variable left, LongVariable right) {
          this.left  = left;
          this.right = right;
       }
     };
     
   /**
    * 
    * @param variable
    * @throws Exception
    */
   public void validateNonindexedVariables(Variable variable) throws Exception {
      super.validate(variable);

//      // Windowed mode only available when the COP clock is bus clock
//      if (sim_copc_coptVar != null) {
//         sim_copc_copwVar.enable((sim_copc_coptVar.getValueAsLong() >= 4));
//         if (sim_copc_copdbgenVar != null) {
//            sim_copc_copdbgenVar.enable((sim_copc_coptVar.getValueAsLong() != 0));
//            sim_copc_copstpenwVar.enable((sim_copc_coptVar.getValueAsLong() != 0));
//            sim_copc_copclkselVar.enable((sim_copc_coptVar.getValueAsLong() != 0));
//         }
//      }
   }

   void validateUsbfsClock(LongVariable system_peripheral_clockVar) throws Exception {

      // USB FS Clock source select
      //============================
      ChoiceVariable sim_sopt2_usbsrcVar = safeGetChoiceVariable("sim_sopt2_usbsrc");
      if (sim_sopt2_usbsrcVar == null) {
         return;
      }
      ChoiceVariable sim_clkdiv2_usbVar = safeGetChoiceVariable("sim_clkdiv2_usb");

      if (sim_clkdiv2_usbVar != null) {
         // USB divider CLKDIV2 exists

         int usbCalcValue = -1;
         if (sim_sopt2_usbsrcVar.getValueAsLong() == 0) {
            // Using USB CLKIN pin
            sim_clkdiv2_usbVar.enable(false);
            sim_clkdiv2_usbVar.setOrigin("Not used with external clock");
            sim_clkdiv2_usbVar.setLocked(false);
         }
         else {
            // Using internal clock

            // Try to auto calculate divisor
            long clock = system_peripheral_clockVar.getValueAsLong();
            for (int  usbdiv=0; usbdiv<=7; usbdiv++) {
               for (int  usbfrac=0; usbfrac<=1; usbfrac++) {
                  long testValue = Math.round(clock*(usbfrac+1.0)/(usbdiv+1.0));
                  if (testValue == 48000000) {
                     usbCalcValue = (usbdiv<<1) + usbfrac;
                     break;
                  }
               }
               if (usbCalcValue>=0) {
                  break;
               }
            }
            sim_clkdiv2_usbVar.enable(true);
            if (usbCalcValue>=0) {
               sim_clkdiv2_usbVar.setSubstitutionValue(usbCalcValue);
               sim_clkdiv2_usbVar.setOrigin("Automatically calculated from input clock");
               sim_clkdiv2_usbVar.setLocked(true);
            }
            else {
               sim_clkdiv2_usbVar.setOrigin("Manually selected");
               sim_clkdiv2_usbVar.setLocked(false);
            }
         }
      }
   }

   /**
    * Updates
    * - sim_sopt2_pllfllsel[x]
    * - system_peripheral_clock[x]
    * - system_core_clock[x]
    * - system_bus_clock[x]
    * - system_flexbus_clock[x]
    * - system_flash_clock[x]
    * - sim_clkdiv1_outdiv1[x]
    * - sim_clkdiv1_outdiv2[x]
    * - sim_clkdiv1_outdiv3[x]
    * - sim_clkdiv1_outdiv4[x]
    * 
    * @param variable
    * @throws Exception
    */
   void validateIndexVariables(Variable variable) throws Exception {

      // Determine peripheralClock
      LongVariable  peripheralClockVar = getLongVariable("system_peripheral_clock");

      // Validate USB clock
      validateUsbfsClock(peripheralClockVar);

      //======================================
      final LongVariable   system_core_clockVar         = getLongVariable("system_core_clock");
      final LongVariable   system_bus_clockVar          = getLongVariable("system_bus_clock");

      final LongVariable   sim_clkdiv1_outdiv1Var       = getLongVariable("sim_clkdiv1_outdiv1");
      final LongVariable   sim_clkdiv1_outdiv4Var       = getLongVariable("sim_clkdiv1_outdiv4");

      final ChoiceVariable smc_pmctrl_runmVar           = getChoiceVariable("/SMC/smc_pmctrl_runm");

      long maxCoreClockFreq      = 0;
      long maxBusClockFreq       = 0;

      switch(Integer.parseInt(smc_pmctrl_runmVar.getSubstitutionValue())) {
      case 0: // RUN mode
         maxCoreClockFreq     = MAX_RUN_CORE_CLOCK_FREQ;
         maxBusClockFreq      = MAX_RUN_BUS_CLOCK_FREQ;
         break;
      case 2: // VLPR mode
         maxCoreClockFreq     = MAX_VLPR_CORE_CLOCK_FREQ;
         maxBusClockFreq      = MAX_VLPR_BUS_CLOCK_FREQ;
         break;
      }
      
      if ((variable == null) || (variable == smc_pmctrl_runmVar)) {
         system_core_clockVar.setMax(maxCoreClockFreq);
         system_bus_clockVar.setMax(maxBusClockFreq);
      }
      
      // Core & System Clock
      //===========================================
      // Attempt to find acceptable divisor
      final LongVariable   system_mcgoutclk_clockVar    =  getLongVariable("/MCG/system_mcgoutclk_clock");

      long inputFrequency = system_mcgoutclk_clockVar.getValueAsLong();
      final FindDivisor coreDivisor = new FindDivisor(maxCoreClockFreq, inputFrequency, system_core_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return frequency<=maximum;
         }
      };
      Severity      severity = Severity.OK;
      StringBuilder sb       = new StringBuilder();

      if ((variable == system_core_clockVar) &&
            (getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed)) {
         // Clock variable changed - replace with nearest value if found
         if (coreDivisor.divisor == 0) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(coreDivisor.divisors);
//         if (coreDivisor.nearestTargetFrequency == 0) {
//            System.err.println("Setting 'system_core_clock' to zero");
//         }
         system_core_clockVar.setValue(coreDivisor.nearestTargetFrequency);
         system_core_clockVar.setStatus(new Status(sb.toString(), severity));
         sim_clkdiv1_outdiv1Var.setValue(coreDivisor.divisor);
      }
      else {
         // Clock variable not changed - just validate
         if ((coreDivisor.divisor == 0) ||
               (system_core_clockVar.getValueAsLong() != (coreDivisor.nearestTargetFrequency))) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(coreDivisor.divisors);
         system_core_clockVar.setStatus(new Status(sb.toString(), severity));
         sim_clkdiv1_outdiv1Var.setValue(coreDivisor.divisor);
      }

      // Bus & Flash Clock
      //===========================================
      // Attempt to find acceptable divisor
      final long coreFrequency = system_core_clockVar.getValueAsLong();
      final FindDivisor busDivisor = new FindDivisor(maxBusClockFreq, coreFrequency, system_bus_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=maximum) &&
                  (frequency<=coreFrequency);
         }
      };
      severity = Severity.OK;
      sb       = new StringBuilder();
      if (variable == system_bus_clockVar) {
         // Clock variable changed - replace with nearest value if found
         if (busDivisor.divisor == 0) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(busDivisor.divisors);
         system_bus_clockVar.setValue(busDivisor.nearestTargetFrequency);
         system_bus_clockVar.setStatus(new Status(sb.toString(), severity));
         sim_clkdiv1_outdiv4Var.setValue(busDivisor.divisor);
      }
      else {
         // Clock variable not changed - just validate
         if ((busDivisor.divisor == 0) ||
               (system_bus_clockVar.getValueAsLong() != (busDivisor.nearestTargetFrequency))) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(busDivisor.divisors);
         system_bus_clockVar.setStatus(new Status(sb.toString(), severity));
         sim_clkdiv1_outdiv4Var.setValue(busDivisor.divisor);
      }
   }

   private abstract static class FindDivisor {

      public final long   nearestTargetFrequency;
      public final int    divisor;
      public final String divisors;
      public final long   maximum;
      /**
       * Creates table of acceptable frequencies and determines the nearest to target frequency
       * 
       * @param inputFrequency   Input frequency being divided
       * @param targetFrequency  Desired frequency
       */
      public FindDivisor(long maximum, long inputFrequency, long targetFrequency) {
         this.maximum = maximum;
         HashSet<String> divisorSet= new HashSet<String>();
         double nearestValue   = Double.MAX_VALUE;
         int    nearestDivisor = 0;
         StringBuilder sb = new StringBuilder();
         sb.append("Possible values:");
         int values = 0;
         for (int divisor=16; divisor>0; divisor--) {
            double frequency = inputFrequency/divisor;
            if (!okValue(divisor, frequency)) {
               continue;
            }
            if (values++ == 7) {
               sb.append("\n");
            }
            String value = EngineeringNotation.convert(frequency, 3);
            if (divisorSet.add(value)) {
               sb.append(" "+value+"Hz");
               if ((Math.abs(frequency-targetFrequency))<(Math.abs(nearestValue-targetFrequency))) {
                  nearestValue = frequency;
                  nearestDivisor = divisor;
               }
            }
         }
         if (divisorSet.isEmpty()) {
            nearestTargetFrequency = 0;
            sb.append(" No suitable values found");
         }
         else {
            nearestTargetFrequency = Math.round(nearestValue);
         }
         divisor  = nearestDivisor;
         divisors = sb.toString();
      }

      /**
       * Used to accept or reject proposed target frequencies/divisors
       * 
       * @param divisor    Current divisor being considered
       * @param frequency  Current frequency being considered
       * @return
       */
      abstract boolean okValue(int divisor, double frequency);
   }

   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();
      
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

//      sim_copc_coptVar            = safeGetChoiceVariable("sim_copc_copt");
//      if (sim_copc_coptVar != null) {
//         sim_copc_copwVar            = safeGetBooleanVariable("sim_copc_copw");
//         sim_copc_copdbgenVar        = safeGetBooleanVariable("sim_copc_copdbgen");
//         sim_copc_copstpenwVar       = safeGetBooleanVariable("sim_copc_copstpen");
//         sim_copc_copclkselVar       = safeGetChoiceVariable("sim_copc_copclksel");
//      }
      
      addToWatchedVariables(variablesToWatch);
      
      final String[] externalVariables = {
            "/MCG/system_mcgirclk_clock",
            "/MCG/system_mcgfllclk_clock",
            "/MCG/system_mcgpllclk_clock",
            "/MCG/system_mcgoutclk_clock",
            "/MCG/usb1pfdclk_Clock",
            "/SMC/smc_pmctrl_runm",
      };
      addToWatchedVariables(externalVariables);
   }
}