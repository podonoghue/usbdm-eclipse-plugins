package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
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
public class SimValidate extends PeripheralValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_BUS_CLOCK_FREQ;
   private final long MAX_FLASH_CLOCK_FREQ;
   private final long MAX_FLEXBUS_CLOCK_FREQ;

   public SimValidate(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      MAX_CORE_CLOCK_FREQ     = (Long)it.next();
      MAX_BUS_CLOCK_FREQ      = (Long)it.next();
      MAX_FLASH_CLOCK_FREQ    = (Long)it.next();
      MAX_FLEXBUS_CLOCK_FREQ  = (Long)it.next();

      for(int index=0; index<dimension; index++) {
         fIndex = index;
         try {
            LongVariable system_core_clockVar = getLongVariable("system_core_clock");
            system_core_clockVar.setMin(0);
            system_core_clockVar.setMax(MAX_CORE_CLOCK_FREQ);

            LongVariable system_bus_clockVar = getLongVariable("system_bus_clock");
            system_bus_clockVar.setMin(0);
            system_bus_clockVar.setMax(MAX_BUS_CLOCK_FREQ);

            LongVariable system_flash_clockVar = getLongVariable("system_flash_clock");
            system_flash_clockVar.setMin(0);
            system_flash_clockVar.setMax(MAX_FLASH_CLOCK_FREQ);

            LongVariable system_flexbus_clockVar = safeGetLongVariable("system_flexbus_clock");
            if (system_flexbus_clockVar != null) {
               system_flexbus_clockVar.setMin(0);
               system_flexbus_clockVar.setMax(MAX_FLEXBUS_CLOCK_FREQ);
            }
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

      // Clock Mapping
      //=================
      final String           osc0_peripheral              = getStringVariable("osc0_peripheral").getValueAsString();
      final LongVariable     osc0_oscer_clockVar          = getLongVariable(osc0_peripheral+"/oscer_clock");
      final LongVariable     osc0_osc32k_clockVar         = getLongVariable(osc0_peripheral+"/osc32k_clock");

      final StringVariable   osc32k_peripheralVar         = safeGetStringVariable("/SIM/osc32k_peripheral");
      LongVariable     rtcclk_gated_clockVar  = null;
      LongVariable     rtc_1hz_clockVar       = null;
      LongVariable     rtc_clkoutVar          = null;
      if (osc32k_peripheralVar != null) {
         final String           osc32k_peripheral            = osc32k_peripheralVar.getValueAsString();
          rtcclk_gated_clockVar        = safeGetLongVariable(osc32k_peripheral+"/rtcclk_gated_clock");
          rtc_1hz_clockVar             = safeGetLongVariable(osc32k_peripheral+"/rtc_1hz_clock");
          rtc_clkoutVar                = safeGetLongVariable("rtc_clkout");
      }
      // MCG
      //=================
      final LongVariable     system_low_power_clockVar    =  getLongVariable("/PMC/system_low_power_clock");
      final LongVariable     system_mcgirclk_clockVar     =  getLongVariable("/MCG/system_mcgirclk_clock");
      final LongVariable     system_usb_clkin_clockVar    =  safeGetLongVariable("/MCG/system_usb_clkin_clock");

      final LongVariable     peripheralClockVar           =  getLongVariable("system_peripheral_clock");


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
         pllPostDiv3Value  = peripheralClockVar.getValueAsLong();
         pllPostDiv3Origin = peripheralClockVar.getOrigin();
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
                  clockVar.setStatus(peripheralClockVar.getStatus());
                  clockVar.setOrigin(pllPostDiv3Origin);
                  break;
               case 2: // OSCERCLK
                  clockVar.setValue(osc0_oscer_clockVar.getValueAsLong());
                  clockVar.setStatus(osc0_oscer_clockVar.getStatus());
                  clockVar.setOrigin(osc0_oscer_clockVar.getOrigin());
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
         system_erclk32k_clockVar.setValue(osc0_osc32k_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(osc0_osc32k_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(osc0_osc32k_clockVar.getStatus());
         break;
      case 2: // RTC 32.768kHz oscillator
         system_erclk32k_clockVar.setValue(rtcclk_gated_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(rtcclk_gated_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(rtcclk_gated_clockVar.getStatus());
         break;
      default:
         sim_sopt1_osc32kselVar.setValue(3);
      case 3: // LPO 1 kHz
         system_erclk32k_clockVar.setValue(system_low_power_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(system_low_power_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(system_low_power_clockVar.getStatus());
         break;
      }

      // RTC Clock out pin select 
      //============================
      BooleanVariable sim_sopt2_rtcclkoutselVar = safeGetBooleanVariable("sim_sopt2_rtcclkoutsel");

      if (sim_sopt2_rtcclkoutselVar != null) {
         switch ((int)sim_sopt2_rtcclkoutselVar.getValueAsLong()) {
         default:
            sim_sopt2_rtcclkoutselVar.setValue(0);
         case 0: // RTC seconds clock = 1Hz
            rtc_clkoutVar.setValue(rtc_1hz_clockVar.getValueAsLong());
            rtc_clkoutVar.setStatus(rtc_1hz_clockVar.getStatus());
            rtc_clkoutVar.setOrigin(rtc_1hz_clockVar.getOrigin());
            break;
         case 1: // RTC 32.768kHz oscillator
            rtc_clkoutVar.setValue(rtcclk_gated_clockVar.getValueAsLong());
            rtc_clkoutVar.setStatus(rtcclk_gated_clockVar.getStatus());
            rtc_clkoutVar.setOrigin(rtcclk_gated_clockVar.getOrigin());
            break;
         }
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
      // USB FS Clock source select 
      //============================
      ChoiceVariable sim_sopt2_usbsrcVar = safeGetChoiceVariable("sim_sopt2_usbsrc");
      if (sim_sopt2_usbsrcVar != null) {
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
               long clock = peripheralClockVar.getValueAsLong();
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
                  long temp = sim_clkdiv2_usbVar.getValueAsLong();
                  sim_clkdiv2_usbVar.setSubstitutionValue(usbCalcValue);
                  if (sim_clkdiv2_usbVar.getValueAsLong() != temp) {
                     // Trigger update on change
                     sim_clkdiv2_usbVar.notifyListeners();
                  }
                  sim_clkdiv2_usbVar.setOrigin("Automatically calculated from input clock");
                  sim_clkdiv2_usbVar.setLocked(true);
               }
               else {
                  sim_clkdiv2_usbVar.setOrigin("Manually selected");
                  sim_clkdiv2_usbVar.setLocked(false);
               }
            }
         }
         LongVariable system_usbfs_clockVar = getLongVariable("system_usbfs_clock");
         if (sim_sopt2_usbsrcVar.getValueAsLong() == 0) {
            // Using USB_CLKIN
            system_usbfs_clockVar.setValue(system_usb_clkin_clockVar.getValueAsLong());
            system_usbfs_clockVar.setStatus(system_usb_clkin_clockVar.getStatus());
            system_usbfs_clockVar.setOrigin(system_usb_clkin_clockVar.getOrigin());
         }
         else {
            // Using internal clock
            if (sim_clkdiv2_usbVar != null) {
               // Peripheral Clock / CLKDIV2
               int  usbValue = Long.decode(sim_clkdiv2_usbVar.getSubstitutionValue()).intValue();
               int  usbfrac  = usbValue&0x1;
               int  usbdiv   = (usbValue>>1)&0x7;
               long usbPostDiv2 = peripheralClockVar.getValueAsLong()*(usbfrac+1)/(usbdiv+1);

               system_usbfs_clockVar.setValue(usbPostDiv2);
               system_usbfs_clockVar.setStatus(peripheralClockVar.getStatus());
               system_usbfs_clockVar.setOrigin(peripheralClockVar.getOrigin()+" after /CLKDIV2");
            }
            else {
               // Directly using peripheral clock
               system_usbfs_clockVar.setValue(peripheralClockVar.getValueAsLong());
               system_usbfs_clockVar.setStatus(peripheralClockVar.getStatus());
               system_usbfs_clockVar.setOrigin(peripheralClockVar.getOrigin());
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

      final ChoiceVariable sim_sopt2_pllfllselVar       = safeGetChoiceVariable("sim_sopt2_pllfllsel");

      final LongVariable   system_mcgfllclk_clockVar    = safeGetLongVariable("/MCG/system_mcgfllclk_clock");
      final LongVariable   system_mcgpllclk_clockVar    = safeGetLongVariable("/MCG/system_mcgpllclk_clock");
      final LongVariable   usb1pfdclk_ClockVar          = safeGetLongVariable("/MCG/usb1pfdclk_Clock");
      final LongVariable   system_irc48m_clockVar       = safeGetLongVariable("/MCG/system_irc48m_clock");

      // Determine PLLFLLCLOCK
      //=====================================

      final LongVariable   peripheralClockVar   = getLongVariable("system_peripheral_clock");

      switch ((int)sim_sopt2_pllfllselVar.getValueAsLong()) {
      default:
         sim_sopt2_pllfllselVar.setValue(0);
      case 0:
         if (system_mcgfllclk_clockVar != null) {
            peripheralClockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
            peripheralClockVar.setStatus(system_mcgfllclk_clockVar.getFilteredStatus());
            peripheralClockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         }
         else {
            peripheralClockVar.setValue(0);
            peripheralClockVar.setStatus(new Status("FLL not present", Severity.ERROR));
            peripheralClockVar.setOrigin(null);
         }
         break;
      case 1:
         if (system_mcgpllclk_clockVar != null) {
            peripheralClockVar.setValue(system_mcgpllclk_clockVar.getValueAsLong());
            peripheralClockVar.setStatus(system_mcgpllclk_clockVar.getFilteredStatus());
            peripheralClockVar.setOrigin(system_mcgpllclk_clockVar.getOrigin());
         }
         else {
            sim_sopt2_pllfllselVar.setValue(0);
         }
         break;
      case 2:
         if (usb1pfdclk_ClockVar != null) {
            peripheralClockVar.setValue(usb1pfdclk_ClockVar.getValueAsLong());
            peripheralClockVar.setStatus(usb1pfdclk_ClockVar.getStatus());
            peripheralClockVar.setOrigin(usb1pfdclk_ClockVar.getOrigin());
         }
         else {
            sim_sopt2_pllfllselVar.setValue(0);
         }
         break;
      case 3:
         if (system_irc48m_clockVar != null) {
            peripheralClockVar.setValue(system_irc48m_clockVar.getValueAsLong());
            peripheralClockVar.setStatus(system_irc48m_clockVar.getStatus());
            peripheralClockVar.setOrigin(system_irc48m_clockVar.getOrigin());
         }
         else {
            sim_sopt2_pllfllselVar.setValue(0);
         }
         break;
      }

      // Check if CLKDIV3 Present
      //=====================================
      final Long   pllPostDiv3Value;
      final String pllPostDiv3Origin;

      final Variable sim_clkdiv3_pllfllVar                  = safeGetVariable("sim_clkdiv3_pllfll");
      final Variable system_peripheral_postdivider_clockVar = safeGetVariable("system_peripheral_postdivider_clock");
      if (sim_clkdiv3_pllfllVar != null) {
         int  pllValue     = Long.decode(sim_clkdiv3_pllfllVar.getSubstitutionValue()).intValue();
         int  pllfllfrac   = pllValue&0x1;
         int  pllflldiv    = (pllValue>>1)&0x7;
         pllPostDiv3Value  = (peripheralClockVar.getValueAsLong()*(pllfllfrac+1))/(pllflldiv+1);
         pllPostDiv3Origin = peripheralClockVar.getOrigin() + " after /CLKDIV3";

         system_peripheral_postdivider_clockVar.setValue(pllPostDiv3Value);
         system_peripheral_postdivider_clockVar.setOrigin(pllPostDiv3Origin);
      }
      else {
         pllPostDiv3Value  = peripheralClockVar.getValueAsLong();
         pllPostDiv3Origin = peripheralClockVar.getOrigin();
      }

      //======================================
      final LongVariable   system_core_clockVar         = getLongVariable("system_core_clock");
      final LongVariable   system_bus_clockVar          = getLongVariable("system_bus_clock");
      final LongVariable   system_flexbus_clockVar      = safeGetLongVariable("system_flexbus_clock");
      final LongVariable   system_flash_clockVar        = getLongVariable("system_flash_clock");

      final LongVariable   sim_clkdiv1_outdiv1Var       = getLongVariable("sim_clkdiv1_outdiv1");
      final LongVariable   sim_clkdiv1_outdiv2Var       = getLongVariable("sim_clkdiv1_outdiv2");
      final LongVariable   sim_clkdiv1_outdiv3Var       = safeGetLongVariable("sim_clkdiv1_outdiv3");
      final LongVariable   sim_clkdiv1_outdiv4Var       = getLongVariable("sim_clkdiv1_outdiv4");

      // Core Clock
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

      // Bus Clock
      //===========================================
      // Attempt to find acceptable divisor
      final FindDivisor busDivisor = new FindDivisor(inputFrequency, system_bus_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=MAX_BUS_CLOCK_FREQ) &&
                  ((divisor % coreDivisor.divisor) == 0) &&  // Even multiple
                  ((divisor/coreDivisor.divisor)<=8);        // Differ from core < 8
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
         sim_clkdiv1_outdiv2Var.setValue(busDivisor.divisor);
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
         sim_clkdiv1_outdiv2Var.setValue(busDivisor.divisor);
      }

      // Flexbus Clock
      //===========================================
      if (sim_clkdiv1_outdiv3Var != null) {
         // Attempt to find acceptable divisor
         final FindDivisor flexDivisor = new FindDivisor(inputFrequency, system_flexbus_clockVar.getValueAsLong()) {
            @Override
            boolean okValue(int divisor, double frequency) {
               return (frequency<=MAX_FLEXBUS_CLOCK_FREQ) &&
                     (frequency<=busDivisor.nearestTargetFrequency) &&
                     ((divisor % coreDivisor.divisor) == 0) && // Even multiple
                     ((divisor/coreDivisor.divisor)<=8);       // Differ from core < 8
            }
         };
         severity = Severity.OK;
         sb = new StringBuilder();
         if (variable == system_flexbus_clockVar) {
            // Clock variable changed - replace with nearest value if found
            if (flexDivisor.divisor == 0) {
               severity = Severity.ERROR;
               sb.append("Illegal Frequency\n");
            }
            sb.append(flexDivisor.divisors);
            system_flexbus_clockVar.setValue(flexDivisor.nearestTargetFrequency);
            system_flexbus_clockVar.setStatus(new Status(sb.toString(), severity));
            sim_clkdiv1_outdiv3Var.setValue(flexDivisor.divisor);
         }
         else {
            // Clock variable not changed - just validate
            if ((flexDivisor.divisor == 0) || 
                  (system_flexbus_clockVar.getValueAsLong() != (flexDivisor.nearestTargetFrequency))) {
               severity = Severity.ERROR;
               sb.append("Illegal Frequency\n");
            }
            sb.append(flexDivisor.divisors);
            system_flexbus_clockVar.setStatus(new Status(sb.toString(), severity));
            sim_clkdiv1_outdiv3Var.setValue(flexDivisor.divisor);
         }
      }
      else if (system_flexbus_clockVar != null) {
         system_flexbus_clockVar.enable(false);
         system_flexbus_clockVar.setStatus(new Status("Function not available on this device", Severity.OK));
      }

      //      if (system_mcgoutclk_clockVar.getValueAsLong() == 12000000) {
      //         System.err.println("system_mcgoutclk_clockVar[" +fIndex+"] = " + system_mcgoutclk_clockVar);
      //      }
      // Flash Clock
      //===========================================
      final FindDivisor flashDivisor = new FindDivisor(inputFrequency, system_flash_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=MAX_FLASH_CLOCK_FREQ) &&
                  (frequency<=busDivisor.nearestTargetFrequency) &&
                  ((divisor % coreDivisor.divisor) == 0) &&    // Even multiple
                  ((divisor/coreDivisor.divisor)<=8);   // Differ from core < 8

         }
      };
      severity = Severity.OK;
      sb = new StringBuilder();
      if (variable == system_flash_clockVar) {
         if (flashDivisor.divisor == 0) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(flashDivisor.divisors);
         system_flash_clockVar.setValue(flashDivisor.nearestTargetFrequency);
         system_flash_clockVar.setStatus(new Status(sb.toString(), severity));
         sim_clkdiv1_outdiv4Var.setValue(flashDivisor.divisor);
      }
      else {
         // Clock variable not changed - just validate
         if ((flashDivisor.divisor == 0) || 
               (system_flash_clockVar.getValueAsLong() != (flashDivisor.nearestTargetFrequency))) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(flashDivisor.divisors);
         system_flash_clockVar.setStatus(new Status(sb.toString(), severity));
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

   @Override
   protected void createDependencies() throws Exception {
      
      StringVariable   osc0_peripheral_var    = safeGetStringVariable("osc0_peripheral");
      if (osc0_peripheral_var != null) {
         String   osc0_peripheral    = getStringVariable("osc0_peripheral").getValueAsString();
         final String[] externalVariables = {
               osc0_peripheral+"/oscer_clock",
               osc0_peripheral+"/osc32k_clock",
         };
         addToWatchedVariables(externalVariables);
      }
      
      StringVariable   osc32k_peripheral_var  = safeGetStringVariable("osc32k_peripheral");
      if (osc32k_peripheral_var != null) {
         String   osc32k_peripheral  = getStringVariable("osc32k_peripheral").getValueAsString();
         final String[] externalVariables = {
               osc32k_peripheral+"/rtcclk_gated_clock",
               osc32k_peripheral+"/rtc_1hz_clock",
         };
         addToWatchedVariables(externalVariables);
      }
      final String[] externalVariables = {
            "/PMC/system_low_power_clock",
            "/MCG/system_mcgfllclk_clock",
            "/MCG/system_mcgpllclk_clock",
            "/MCG/system_mcgoutclk_clock",
            "/MCG/system_mcgirclk_clock",
            "/MCG/system_irc48m_clock",
            "/MCG/system_usb_clkin_clock",
            "/MCG/usb1pfdclk_Clock",
      };
      addToWatchedVariables(externalVariables);
   }
}