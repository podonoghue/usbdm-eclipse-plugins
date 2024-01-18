package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine oscillator settings

 * Used for:
 * Sim_xxx
 */
public class SimValidate extends IndexedValidator {

   private final long MAX_RUN_CORE_CLOCK_FREQ;
   private final long MAX_RUN_BUS_CLOCK_FREQ;
   private final long MAX_RUN_FLASH_CLOCK_FREQ;
   private final long MAX_RUN_FLEXBUS_CLOCK_FREQ;

   private final long MAX_VLPR_CORE_CLOCK_FREQ;
   private final long MAX_VLPR_BUS_CLOCK_FREQ;
   private final long MAX_VLPR_FLASH_CLOCK_FREQ;
   private final long MAX_VLPR_FLEXBUS_CLOCK_FREQ;

   private final long MAX_HSRUN_CORE_CLOCK_FREQ;
   private final long MAX_HSRUN_BUS_CLOCK_FREQ;
   private final long MAX_HSRUN_FLASH_CLOCK_FREQ;
   private final long MAX_HSRUN_FLEXBUS_CLOCK_FREQ;
   
   public SimValidate(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();

      MAX_RUN_CORE_CLOCK_FREQ       = (Long)it.next();
      MAX_RUN_BUS_CLOCK_FREQ        = (Long)it.next();
      MAX_RUN_FLEXBUS_CLOCK_FREQ    = (Long)it.next();
      MAX_RUN_FLASH_CLOCK_FREQ      = (Long)it.next();

      MAX_VLPR_CORE_CLOCK_FREQ      = (Long)it.next();
      MAX_VLPR_BUS_CLOCK_FREQ       = (Long)it.next();
      MAX_VLPR_FLEXBUS_CLOCK_FREQ   = (Long)it.next();
      MAX_VLPR_FLASH_CLOCK_FREQ     = (Long)it.next();
      
      MAX_HSRUN_CORE_CLOCK_FREQ     = (values.size()>8)?(Long)it.next():0;
      MAX_HSRUN_BUS_CLOCK_FREQ      = (values.size()>8)?(Long)it.next():0;
      MAX_HSRUN_FLEXBUS_CLOCK_FREQ  = (values.size()>8)?(Long)it.next():0;
      MAX_HSRUN_FLASH_CLOCK_FREQ    = (values.size()>8)?(Long)it.next():0;
   }

   static class StringPair {
      final String left;
      final String right;

      StringPair(String left, String right) {
         this.left  = left;
         this.right = right;
      }
   };

   /**
    * Updates sim_clkdiv2_usb[]
    * 
    * @param system_peripheral_clockVar
    * @throws Exception
    */
   private void validateUsbfsClock(LongVariable system_peripheral_clockVar) throws Exception {

      // USB FS Clock source select
      //============================
      ChoiceVariable sim_sopt2_usbsrcVar = safeGetChoiceVariable("sim_sopt2_usbsrc[]");
      if (sim_sopt2_usbsrcVar == null) {
         return;
      }
      ChoiceVariable sim_clkdiv2_usbVar = safeGetChoiceVariable("sim_clkdiv2_usb[]");

      if (sim_clkdiv2_usbVar != null) {
         // USB divider CLKDIV2 exists

         long perClock = system_peripheral_clockVar.getValueAsLong();

         if (sim_sopt2_usbsrcVar.getValueAsLong() == 0) {
            // Using USB CLKIN pin
            sim_clkdiv2_usbVar.enable(false);
            sim_clkdiv2_usbVar.setOrigin("Not used with external USB clock");
            sim_clkdiv2_usbVar.setLocked(false);
         }
         else if (perClock==0) {
            sim_clkdiv2_usbVar.enable(false);
            sim_clkdiv2_usbVar.setOrigin(system_peripheral_clockVar.getOrigin());
            sim_clkdiv2_usbVar.setLocked(false);
         }
         else {
            // Using internal clock

            // Try to auto calculate divisor
            int usbCalcValue = -1;
            for (int  usbdiv=0; usbdiv<=7; usbdiv++) {
               for (int  usbfrac=0; usbfrac<=1; usbfrac++) {
                  long testValue = Math.round(perClock*(usbfrac+1.0)/(usbdiv+1.0));
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
    * Checks
    * - system_peripheral_clock[]
    * - system_core_clock[]
    * - system_bus_clock[]
    * - system_flexbus_clock[]
    * - system_flash_clock[]
    * 
    * Updates
    * - sim_clkdiv2_usb[]
    * - sim_clkdiv1_outdiv1[]
    * - sim_clkdiv1_outdiv2[]
    * - sim_clkdiv1_outdiv3[]
    * - sim_clkdiv1_outdiv4[]
    * 
    * @param variable
    * @throws Exception
    */
   /**
    * 
    * @throws Exception
    */
   @Override
   public void validate(Variable variable, int properties, int index) throws Exception {

      if ((properties&PROPERTY_VALUE) == 0) {
         return;
      }
      if ((variable != null) && variable.isLogging()) {
         System.err.println("validate("+variable+", "+IModelChangeListener.getPropertyNames(properties)+")");
      }
      // Determine peripheralClock
      LongVariable  peripheralClockVar = getLongVariable("system_peripheral_clock[]");

      // Validate USB clock
      validateUsbfsClock(peripheralClockVar);

      //======================================
      final LongVariable   system_core_clockVar         = getLongVariable("system_core_clock[]");
      final LongVariable   system_bus_clockVar          = getLongVariable("system_bus_clock[]");
      final LongVariable   system_flexbus_clockVar      = safeGetLongVariable("system_flexbus_clock[]");
      final LongVariable   system_flash_clockVar        = getLongVariable("system_flash_clock[]");

      final ChoiceVariable   sim_clkdiv1_outdiv1Var     = getChoiceVariable("sim_clkdiv1_outdiv1[]");
      final ChoiceVariable   sim_clkdiv1_outdiv2Var     = getChoiceVariable("sim_clkdiv1_outdiv2[]");
      final ChoiceVariable   sim_clkdiv1_outdiv3Var     = safeGetChoiceVariable("sim_clkdiv1_outdiv3[]");
      final ChoiceVariable   sim_clkdiv1_outdiv4Var     = getChoiceVariable("sim_clkdiv1_outdiv4[]");

      final ChoiceVariable smc_pmctrl_runmVar           = getChoiceVariable("/SMC/smc_pmctrl_runm[]");

      long maxCoreClockFreq      = 0;
      long maxBusClockFreq       = 0;
      long maxFlexbusClockFreq   = 0;
      long maxFlashClockFreq     = 0;

      switch(Integer.parseInt(smc_pmctrl_runmVar.getSubstitutionValue())) {
      case 0: // RUN mode
         maxCoreClockFreq     = MAX_RUN_CORE_CLOCK_FREQ;
         maxBusClockFreq      = MAX_RUN_BUS_CLOCK_FREQ;
         maxFlexbusClockFreq  = MAX_RUN_FLEXBUS_CLOCK_FREQ;
         maxFlashClockFreq    = MAX_RUN_FLASH_CLOCK_FREQ;
         break;
      case 2: // VLPR mode
         maxCoreClockFreq     = MAX_VLPR_CORE_CLOCK_FREQ;
         maxBusClockFreq      = MAX_VLPR_BUS_CLOCK_FREQ;
         maxFlexbusClockFreq  = MAX_VLPR_FLEXBUS_CLOCK_FREQ;
         maxFlashClockFreq    = MAX_VLPR_FLASH_CLOCK_FREQ;
         break;
      case 3: // HSRUN mode
         maxCoreClockFreq     = MAX_HSRUN_CORE_CLOCK_FREQ;
         maxBusClockFreq      = MAX_HSRUN_BUS_CLOCK_FREQ;
         maxFlexbusClockFreq  = MAX_HSRUN_FLEXBUS_CLOCK_FREQ;
         maxFlashClockFreq    = MAX_HSRUN_FLASH_CLOCK_FREQ;
         break;
      }

      if ((variable == null) || (variable == smc_pmctrl_runmVar)) {
         // Update initially or if RUN mode changes
         system_core_clockVar.setMax(maxCoreClockFreq);
         system_bus_clockVar.setMax(maxBusClockFreq);
         if (system_flexbus_clockVar != null) {
            system_flexbus_clockVar.setMax(maxFlexbusClockFreq);
         }
         system_flash_clockVar.setMax(maxFlashClockFreq);
      }
      if (getDeviceInfo().getInitialisationPhase() == InitPhase.VariablePropagationSuspended) {
         return;
      }
      // Permit GUI derived updates?
      boolean doGuiUpdates = getDeviceInfo().getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed;
      
      // All clocks are directly derived from this value
      final LongVariable system_mcgoutclk_clockVar = getLongVariable("/MCG/system_mcgoutclk_clock[]");
      
      // Attempt to find core clock acceptable divisor
      final FindDivisor coreDivisor = new FindDivisor(maxCoreClockFreq, system_mcgoutclk_clockVar, system_core_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return frequency<=maximum;
         }
      };
      // Core Clock
      //===========================================
      if (system_core_clockVar.isEnabled() && ((variable==null)||(variable==system_core_clockVar))) {
         // Core clock changed
         Severity severity = Severity.OK;
         if (coreDivisor.failed()) {
            severity = Severity.ERROR;
         }
         else if (system_core_clockVar.getValueAsLong() != coreDivisor.nearestTargetFrequency) {
            system_core_clockVar.setValue(coreDivisor.nearestTargetFrequency);
         }
         system_core_clockVar.setStatus(new Status(coreDivisor.divisors, severity));
         if (!coreDivisor.failed() && doGuiUpdates && (variable == system_core_clockVar)) {
            // Target clock manually changed - update divisor
            sim_clkdiv1_outdiv1Var.setValue(coreDivisor.divisor-1);
         }
      }
      // Bus Clock
      //===========================================
      // Bus clock changed
      // Attempt to find acceptable divisor
      final FindDivisor busDivisor = new FindDivisor(maxBusClockFreq, system_mcgoutclk_clockVar, system_bus_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=maximum) &&
                  ((divisor % coreDivisor.divisor) == 0) &&  // Even multiple
                  ((divisor/coreDivisor.divisor)<=8);        // Differ from core < 8
         }
      };
      {
         Severity      severity = Severity.OK;
         StringBuilder sb       = new StringBuilder();
         if (busDivisor.failed() || (system_bus_clockVar.getValueAsLong() != busDivisor.nearestTargetFrequency)) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(busDivisor.divisors);
         system_bus_clockVar.setStatus(new Status(sb.toString(), severity));
      }
      if (!busDivisor.failed() && doGuiUpdates && (variable == system_bus_clockVar)) {
         // Target clock manually changed - update divisors
         sim_clkdiv1_outdiv2Var.setValue(busDivisor.divisor-1);
         system_bus_clockVar.setValue(busDivisor.nearestTargetFrequency);
      }
      // Flexbus Clock
      //===========================================
      if (sim_clkdiv1_outdiv3Var != null) {
         // Attempt to find acceptable divisor
         final FindDivisor flexDivisor = new FindDivisor(maxFlexbusClockFreq, system_mcgoutclk_clockVar, system_flexbus_clockVar.getValueAsLong()) {
            @Override
            boolean okValue(int divisor, double frequency) {
               return (frequency<=maximum) &&
                     (frequency<=busDivisor.nearestTargetFrequency) &&
                     ((divisor % coreDivisor.divisor) == 0) && // Even multiple
                     ((divisor/coreDivisor.divisor)<=8);       // Differ from core < 8
            }
         };
         {
            Severity      severity = Severity.OK;
            StringBuilder sb       = new StringBuilder();
            if (flexDivisor.failed() || (system_flexbus_clockVar.getValueAsLong() != flexDivisor.nearestTargetFrequency)) {
               severity = Severity.ERROR;
               sb.append("Illegal Frequency\n");
            }
            sb.append(flexDivisor.divisors);
            system_flexbus_clockVar.setStatus(new Status(sb.toString(), severity));
         }
         if (!flexDivisor.failed() && doGuiUpdates && (variable == system_flexbus_clockVar)) {
            // Target clock manually changed - update divisor
            sim_clkdiv1_outdiv3Var.setValue(flexDivisor.divisor-1);
            system_flexbus_clockVar.setValue(flexDivisor.nearestTargetFrequency);
         }
      }
      // Flash Clock
      //===========================================
      // Attempt to find acceptable divisor
      final FindDivisor flashDivisor = new FindDivisor(maxFlashClockFreq, system_mcgoutclk_clockVar, system_flash_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=maximum) &&
                  (frequency<=busDivisor.nearestTargetFrequency) &&
                  ((divisor % coreDivisor.divisor) == 0) &&    // Even multiple
                  ((divisor/coreDivisor.divisor)<=8);   // Differ from core < 8

         }
      };
      {
         Severity      severity = Severity.OK;
         StringBuilder sb       = new StringBuilder();
         if (flashDivisor.failed() || (system_flash_clockVar.getValueAsLong() != flashDivisor.nearestTargetFrequency)) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(flashDivisor.divisors);
         system_flash_clockVar.setStatus(new Status(sb.toString(), severity));
      }
      boolean doUpdate = !flashDivisor.failed() && doGuiUpdates && (variable == system_flash_clockVar);
      if (doUpdate) {
         // Target clock changed - validate
         sim_clkdiv1_outdiv4Var.setValue(flashDivisor.divisor-1);
         system_flash_clockVar.setValue(flashDivisor.nearestTargetFrequency);
      }
      
//      System.err.println("validate() - exit");
   }

   private abstract static class FindDivisor {

      public final long   nearestTargetFrequency;
      public final int    divisor;
      public final String divisors;
      public final long   maximum;
      
      private final boolean valuesNotFound;
      
      /**
       * Creates table of acceptable frequencies and determines the nearest to target frequency
       * 
       * @param inputFrequency   Input frequency being divided
       * @param targetFrequency  Desired frequency
       */
      public FindDivisor(long maximum, LongVariable inputFrequencyVar, long targetFrequency) {
         this.maximum = maximum;
         HashSet<String> divisorSet= new HashSet<String>();
         double nearestValue   = Double.MAX_VALUE;
         int    nearestDivisor = 0;
         StringBuilder sb = new StringBuilder();
         int values = 0;
         Status status = inputFrequencyVar.getStatus();
         if (!inputFrequencyVar.isEnabled()) {
            nearestTargetFrequency = 0;
            valuesNotFound = true;
            sb.append("Disabled by "+inputFrequencyVar.getName());
         }
         else if ((status != null) && status.greaterThan(Status.Severity.INFO)) {
            nearestTargetFrequency = 0;
            sb.append(status.getSimpleText());
            valuesNotFound = true;
         }
         else {
            Long inputFrequency = inputFrequencyVar.getRawValueAsLong();
            for (int divisor=16; divisor>0; divisor--) {
               double frequency = inputFrequency/divisor;
               if ((frequency < 1.0) || !okValue(divisor, frequency)) {
                  continue;
               }
               String value = EngineeringNotation.convert(frequency, 3);
               if (divisorSet.add(value)) {
                  if (values == 0) {
                     sb.append("Possible values:");
                  }
                  if (values++ == 7) {
                     sb.append("\n");
                  }
                  sb.append(" "+value+"Hz");
                  if ((Math.abs(frequency-targetFrequency))<(Math.abs(nearestValue-targetFrequency))) {
                     nearestValue = frequency;
                     nearestDivisor = divisor;
                  }
               }
            }
            valuesNotFound = divisorSet.isEmpty();
            if (valuesNotFound) {
               nearestTargetFrequency = 0;
               sb.append(" No suitable values found");
            }
            else {
               nearestTargetFrequency = Math.round(nearestValue);
            }
         }
         divisor  = nearestDivisor;
         divisors = sb.toString();
      }

      /**
       * Indicates that no acceptable divisors were found
       * 
       * @return True => failed
       */
      public boolean failed() {
         return this.valuesNotFound;
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
   protected boolean createDependencies() throws Exception {

      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      addToWatchedVariables(variablesToWatch);
      final String[] externalVariables = {
            "/MCG/system_mcgoutclk_clock[]",
            "/SMC/smc_pmctrl_runm[]",
            "system_peripheral_clock[]",
            "system_core_clock[]",
            "system_bus_clock[]",
            "system_flexbus_clock[]",
            "system_flash_clock[]",
            "sim_sopt2_usbsrc[]",
      };
      addToWatchedVariables(externalVariables);
      
      // Don't add default dependencies
      return false;
   }
}