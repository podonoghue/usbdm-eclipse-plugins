package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
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
public class SimValidateMKL_lite extends PeripheralValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_BUS_CLOCK_FREQ;
//   private final long MAX_FLASH_CLOCK_FREQ;
//   private final long MAX_FLEXBUS_CLOCK_FREQ;

   private final static String[] externalVariables = {
         "/OSC0/system_oscerclk_clock",
         "/OSC0/osc32kclk_clock",
         "/MCG/system_low_power_clock",
         "/MCG/system_mcgpclk_clock",
         "/MCG/system_mcgoutclk_clock",
         "/MCG/system_mcgirclk_clock",
         "/MCG/system_irc48m_clock",
         "/MCG/system_usb_clkin_clock",
         "/RTC/rtcclkin_clock",
         "/RTC/rtc_1hz_clock",
   };

   public SimValidateMKL_lite(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      MAX_CORE_CLOCK_FREQ     = (Long)it.next();
      MAX_BUS_CLOCK_FREQ      = (Long)it.next();
//      MAX_FLASH_CLOCK_FREQ    = (Long)it.next();
//      MAX_FLEXBUS_CLOCK_FREQ  = (Long)it.next();

      for(int index=0; index<dimension; index++) {
         fIndex = index;
         try {
            LongVariable system_core_clockVar = getLongVariable("system_core_clock");
            system_core_clockVar.setMin(0);
            system_core_clockVar.setMax(MAX_CORE_CLOCK_FREQ);

            LongVariable system_bus_clockVar = getLongVariable("system_bus_clock");
            system_bus_clockVar.setMin(0);
            system_bus_clockVar.setMax(MAX_BUS_CLOCK_FREQ);

//            LongVariable system_flash_clockVar = getLongVariable("system_flash_clock");
//            system_flash_clockVar.setMin(0);
//            system_flash_clockVar.setMax(MAX_FLASH_CLOCK_FREQ);
//			
//            LongVariable system_flexbus_clockVar = safeGetLongVariable("system_flexbus_clock");
//            if (system_flexbus_clockVar != null) {
//               system_flexbus_clockVar.setMin(0);
//               system_flexbus_clockVar.setMax(MAX_FLEXBUS_CLOCK_FREQ);
//            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      fIndex = 0;

   }

   private static interface LpClockSelector {
      public void lpClockSelect(String sourceVar, String clockVar) throws Exception;
   }

   /**
    * Class to determine oscillator settings
    * @throws Exception 
    */
   @Override
   public void validate(Variable variable) throws Exception {
      addToWatchedVariables(externalVariables);
      for(fIndex=0; fIndex<fDimension; fIndex++) {
         validateIndexVariables(variable);
      }
      fIndex = 0;
      validateNonindexedVariables(variable);
   }      
   
   /**
    * Updates
    *  - srcVar
    *  - clockVar
    *  - system_erclk32k_clockVar
    *  - sim_sopt1_osc32kselVar
    *  - sim_sopt2_rtcclkoutselVar
    *  - rtc_clkoutVar
    *  - system_usbfs_clockVar
    *  
    * @param variable
    * @throws Exception
    */
   public void validateNonindexedVariables(Variable variable) throws Exception {
      super.validate(variable);

      //OSC
      //=================
      final LongVariable     system_oscerclk_clockVar        =  getLongVariable("/OSC0/system_oscerclk_clock");
      final LongVariable     osc32kclk_clockVar              =  getLongVariable("/OSC0/osc32kclk_clock");

      // MCG
      //=================
      final LongVariable     system_low_power_clockVar       =  getLongVariable("/MCG/system_low_power_clock");
      final LongVariable     system_mcgpclk_clockVar         =  getLongVariable("/MCG/system_mcgpclk_clock");
      final LongVariable     system_mcgirclk_clockVar        =  getLongVariable("/MCG/system_mcgirclk_clock");
      final LongVariable     system_usb_clkin_clockVar       =  safeGetLongVariable("/MCG/system_usb_clkin_clock");

      // RTC
      //=================
      final LongVariable     rtcclkin_clockVar               =  safeGetLongVariable("/RTC/rtcclkin_clock");
      final LongVariable     rtc_1hz_clockVar                =  safeGetLongVariable("/RTC/rtc_1hz_clock");
      final LongVariable     rtc_clkoutVar                   =  safeGetLongVariable("rtc_clkout");

      // Check if CLKDIV3 Present
      //=====================================
      final Long   pllPostDiv3Value;
      final String pllPostDiv3Origin;

      final Variable system_peripheral_postdivider_clockVar = safeGetVariable("system_peripheral_postdivider_clock");
      if (system_peripheral_postdivider_clockVar != null) {
         // After divider
         pllPostDiv3Value  = system_peripheral_postdivider_clockVar.getValueAsLong();
         pllPostDiv3Origin = system_peripheral_postdivider_clockVar.getOrigin();
      }
      else {
         // Direct (no divider)
         pllPostDiv3Value  = system_mcgpclk_clockVar.getValueAsLong();
         pllPostDiv3Origin = system_mcgpclk_clockVar.getOrigin();
      }

      /**
       * Clock selector used for LPUARTs, TPMs and FlexIO
       */
      LpClockSelector clockSelector = new LpClockSelector() {
         @Override
         public void lpClockSelect(String sourceVar, String clockVarId) throws Exception {

            // Clock source select (if present)
            //===================================
            Variable srcVar = safeGetVariable(sourceVar);
            if (srcVar != null) {
               Variable clockVar = getVariable(clockVarId);
               switch ((int)srcVar.getValueAsLong()) {
               default:
                  srcVar.setValue(0);
               case 0: // Disabled
                  clockVar.setValue(0);
                  clockVar.setStatus((Status)null);
                  clockVar.setOrigin("Disabled");
                  break;
               case 1: // Peripheral Clock / CLKDIV3
                  clockVar.setValue(pllPostDiv3Value);
                  clockVar.setStatus(system_mcgpclk_clockVar.getStatus());
                  clockVar.setOrigin(pllPostDiv3Origin);
                  break;
               case 2: // OSCERCLK
                  clockVar.setValue(system_oscerclk_clockVar.getValueAsLong());
                  clockVar.setStatus(system_oscerclk_clockVar.getStatus());
                  clockVar.setOrigin(system_oscerclk_clockVar.getOrigin());
                  break;
               case 3: // MCGIRCLK
                  clockVar.setValue(system_mcgirclk_clockVar.getValueAsLong());
                  clockVar.setStatus(system_mcgirclk_clockVar.getStatus());
                  clockVar.setOrigin(system_mcgirclk_clockVar.getOrigin());
                  break;
               }
            }
         }
      };

      // Determine ERCLK32K
      //==================================
      LongVariable system_erclk32k_clockVar = getLongVariable("system_erclk32k_clock");
      ChoiceVariable sim_sopt1_osc32kselVar = getChoiceVariable("sim_sopt1_osc32ksel");
      
      switch ((int)sim_sopt1_osc32kselVar.getValueAsLong()) {
      case 0: // System oscillator (OSC32KCLK)
         system_erclk32k_clockVar.setValue(osc32kclk_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(osc32kclk_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(osc32kclk_clockVar.getStatus());
         break;
      default:
         sim_sopt1_osc32kselVar.setValue(2);
      case 2: // RTC CLK_IN
         system_erclk32k_clockVar.setValue(rtcclkin_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(rtcclkin_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(rtcclkin_clockVar.getStatus());
         break;
      case 3: // LPO 1 kHz
         system_erclk32k_clockVar.setValue(system_low_power_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(system_low_power_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(system_low_power_clockVar.getStatus());
         break;
      }

      // RTC Clock out pin select 
      //============================
      BooleanVariable sim_sopt2_rtcclkoutselVar = getBooleanVariable("sim_sopt2_rtcclkoutsel");
         
      switch ((int)sim_sopt2_rtcclkoutselVar.getValueAsLong()) {
      default:
         sim_sopt2_rtcclkoutselVar.setValue(0);
      case 0: // RTC seconds clock = 1Hz
         rtc_clkoutVar.setValue(rtc_1hz_clockVar.getValueAsLong());
         rtc_clkoutVar.setStatus(rtc_1hz_clockVar.getStatus());
         rtc_clkoutVar.setOrigin(rtc_1hz_clockVar.getOrigin());
         break;
      case 1: // OSCERCLK
         rtc_clkoutVar.setValue(system_oscerclk_clockVar.getValueAsLong());
         rtc_clkoutVar.setStatus(system_oscerclk_clockVar.getStatus());
         rtc_clkoutVar.setOrigin(system_oscerclk_clockVar.getOrigin());
         break;
      }

      // UART0 Clock source select (if present)
      //==========================================
      clockSelector.lpClockSelect("sim_sopt2_uart0src", "system_uart0_clock");

      // LPUARTx Clock source select (if present)
      //==========================================
      final String[] lpUartInstances = {"", "0", "1", "2"};
      for (String lpUartInstance:lpUartInstances) {
         clockSelector.lpClockSelect("sim_sopt2_lpuart"+lpUartInstance+"src", "system_lpuart"+lpUartInstance+"_clock");
      }
	  
      // TPMx Clock source select (if present)
      //==========================================
      final String[] tpmInstances = {"", "0", "1", "2"};
      for (String tpmInstance:tpmInstances) {
         clockSelector.lpClockSelect("sim_sopt2_tpm"+tpmInstance+"src", "system_tpm"+tpmInstance+"_clock");
      }

      // FLEXIO Clock source select (if present)
      //==========================================
      clockSelector.lpClockSelect("sim_sopt2_flexiosrc", "system_flexio_clock");
	  
      // USB FS Clock source select 
      //============================
      ChoiceVariable sim_sopt2_usbsrcVar = safeGetChoiceVariable("sim_sopt2_usbsrc");
      if (sim_sopt2_usbsrcVar != null) {
         LongVariable system_usbfs_clockVar = getLongVariable("system_usbfs_clock");
         if (sim_sopt2_usbsrcVar.getValueAsLong() == 0) {
            // Using USB_CLKIN
            system_usbfs_clockVar.setValue(system_usb_clkin_clockVar.getValueAsLong());
            system_usbfs_clockVar.setStatus(system_usb_clkin_clockVar.getStatus());
            system_usbfs_clockVar.setOrigin(system_usb_clkin_clockVar.getOrigin());
         }
         else {
            // MCGPCLK
            system_usbfs_clockVar.setValue(system_mcgpclk_clockVar.getValueAsLong());
            system_usbfs_clockVar.setStatus(system_mcgpclk_clockVar.getStatus());
            system_usbfs_clockVar.setOrigin(system_mcgpclk_clockVar.getOrigin());
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
      
      //======================================
      final LongVariable   system_core_clockVar         = getLongVariable("system_core_clock");
      final LongVariable   system_bus_clockVar          = getLongVariable("system_bus_clock");

      final LongVariable   sim_clkdiv1_outdiv1Var       = getLongVariable("sim_clkdiv1_outdiv1");
      final LongVariable   sim_clkdiv1_outdiv4Var       = getLongVariable("sim_clkdiv1_outdiv4");

      // Core & System Clock
      //===========================================
      // Attempt to find acceptable divisor
      final LongVariable     system_mcgoutclk_clockVar       =  getLongVariable("/MCG/system_mcgoutclk_clock");

      long inputFrequency = system_mcgoutclk_clockVar.getValueAsLong();
      final FindDivisor coreDivisor = new FindDivisor(inputFrequency, system_core_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return frequency<=MAX_CORE_CLOCK_FREQ;
         }
      };
      Severity      severity = Severity.OK;
      StringBuilder sb       = new StringBuilder();
      if (variable == system_core_clockVar) {
         // Clock variable changed - replace with nearest value if found
         if (coreDivisor.divisor == 0) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(coreDivisor.divisors);
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
      inputFrequency     = coreFrequency;
      final FindDivisor flashDivisor = new FindDivisor(inputFrequency, system_bus_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=MAX_BUS_CLOCK_FREQ) &&
                  (frequency<=coreFrequency);
         }
      };
      severity = Severity.OK;
      sb       = new StringBuilder();
      if (variable == system_bus_clockVar) {
         // Clock variable changed - replace with nearest value if found
         if (flashDivisor.divisor == 0) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(flashDivisor.divisors);
         system_bus_clockVar.setValue(flashDivisor.nearestTargetFrequency);
         system_bus_clockVar.setStatus(new Status(sb.toString(), severity));
         sim_clkdiv1_outdiv4Var.setValue(flashDivisor.divisor);
      }
      else {
         // Clock variable not changed - just validate
         if ((flashDivisor.divisor == 0) || 
             (system_bus_clockVar.getValueAsLong() != (flashDivisor.nearestTargetFrequency))) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(flashDivisor.divisors);
         system_bus_clockVar.setStatus(new Status(sb.toString(), severity));
         sim_clkdiv1_outdiv4Var.setValue(flashDivisor.divisor);
      }
   }

   private abstract static class FindDivisor {

      public final long   nearestTargetFrequency;
      public final int    divisor;
      public final String divisors;

      /**
       * Creates table of acceptable frequencies and determines the nearest to target frequency
       * 
       * @param inputFrequency   Input frequency being divided
       * @param targetFrequency  Desired frequency
       */
      public FindDivisor(long inputFrequency, long targetFrequency) {
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
         nearestTargetFrequency = Math.round(nearestValue);
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

}