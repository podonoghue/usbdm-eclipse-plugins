package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.ListIterator;

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
public class SimValidate extends PeripheralValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_BUS_CLOCK_FREQ;
   private final long MAX_FLASH_CLOCK_FREQ;
   private final long MAX_FLEXBUS_CLOCK_FREQ;

   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/OSC0/system_oscerclk_clock",
         "/OSC0/osc32kclk_clock",
         "/MCG/system_low_power_clock",
         "/MCG/system_mcgfllclk_clock",
         "/MCG/system_mcgpllclk_clock",
         "/MCG/system_mcgoutclk_clock",
         "/MCG/system_mcgirclk_clock",
         "/MCG/usb1pfdclk_Clock",
         "/MCG/system_irc48m_clock",
         "/MCG/system_usb_clkin_clock",
         "/RTC/rtc_clkout",
         "/RTC/rtcclk_clock",
         "/RTC/rtcclk_gated_clock",
   };

   public SimValidate(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);

      ListIterator<Object> it = values.listIterator();
      MAX_CORE_CLOCK_FREQ     = (Long)it.next();
      MAX_BUS_CLOCK_FREQ      = (Long)it.next();
      MAX_FLASH_CLOCK_FREQ    = (Long)it.next();
      MAX_FLEXBUS_CLOCK_FREQ  = (Long)it.next();

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
      } catch (Exception e) {
         e.printStackTrace();
      }

      LongVariable system_flexbus_clockVar = safeGetLongVariable("system_flexbus_clock");
      if (system_flexbus_clockVar != null) {
         system_flexbus_clockVar.setMin(0);
         system_flexbus_clockVar.setMax(MAX_FLEXBUS_CLOCK_FREQ);
      }
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
      
      super.validate(variable);

      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }
      //OSC
      //=================
      final Variable     system_oscerclk_clockVar        =  getVariable("/OSC0/system_oscerclk_clock");
      final Variable     osc32kclk_clockVar              =  getVariable("/OSC0/osc32kclk_clock");

      // MCG
      //=================
      final Variable     system_low_power_clockVar       =  getVariable("/MCG/system_low_power_clock");
      final Variable     system_mcgfllclk_clockVar       =  getVariable("/MCG/system_mcgfllclk_clock");
      final Variable     system_mcgpllclk_clockVar       =  getVariable("/MCG/system_mcgpllclk_clock");
      final Variable     system_mcgoutclk_clockVar       =  getVariable("/MCG/system_mcgoutclk_clock");
      final Variable     system_mcgirclk_clockVar        =  getVariable("/MCG/system_mcgirclk_clock");
      final Variable     usb1pfdclk_ClockVar             =  safeGetVariable("/MCG/usb1pfdclk_Clock");
      final Variable     system_irc48m_clockVar          =  safeGetVariable("/MCG/system_irc48m_clock");
      final Variable     system_usb_clkin_clockVar       =  safeGetVariable("/MCG/system_usb_clkin_clock");

      // RTC
      //=================
      final Variable     rtcclk_clockVar                 =  safeGetVariable("/RTC/rtcclk_clock");
      final Variable     rtcclk_gated_clockVar           =  safeGetVariable("/RTC/rtcclk_gated_clock");
      final Variable     rtc_clkoutVar                   =  safeGetVariable("/RTC/rtc_clkout");

      //
      //=====================
      final Variable system_peripheral_clockVar = getVariable("system_peripheral_clock");
      final Variable sim_sopt2_pllfllselVar     = getVariable("sim_sopt2_pllfllsel");

      final Long   pllPostDiv3Value;
      final String pllPostDiv3Origin;

      // Check if CLKDIV3 Present
      //=====================================
      Variable sim_clkdiv3_pllfllVar = safeGetVariable("sim_clkdiv3_pllfll");
      if (sim_clkdiv3_pllfllVar != null) {
         int  pllValue     = Long.decode(sim_clkdiv3_pllfllVar.getSubstitutionValue()).intValue();
         int  pllfllfrac   = pllValue&0x1;
         int  pllflldiv    = (pllValue>>1)&0x7;
         pllPostDiv3Value  = (system_peripheral_clockVar.getValueAsLong()*(pllfllfrac+1))/(pllflldiv+1);
         pllPostDiv3Origin = system_peripheral_clockVar.getOrigin() + " after /CLKDIV3";
      }
      else {
         pllPostDiv3Value  = system_peripheral_clockVar.getValueAsLong();
         pllPostDiv3Origin = system_peripheral_clockVar.getOrigin();
      }

      /**
       * Clock selector used for LPUARTs and TPMs
       */
      LpClockSelector clockSelector = new LpClockSelector() {
         @Override
         public void lpClockSelect(String sourceVar, String clockVarId) throws Exception {

            // Clock source select (if present)
            //===================================
            Variable srcVar = safeGetVariable(sourceVar);
            if (srcVar != null) {
               Variable clockVar   = getVariable(clockVarId);
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
                  clockVar.setStatus(system_peripheral_clockVar.getStatus());
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
      Variable system_erclk32k_clockVar = getVariable("system_erclk32k_clock");
      Variable sim_sopt1_osc32kselVar   = getVariable("sim_sopt1_osc32ksel");
      switch ((int)sim_sopt1_osc32kselVar.getValueAsLong()) {
      case 0: // System oscillator (OSC32KCLK)
         system_erclk32k_clockVar.setValue(osc32kclk_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(osc32kclk_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(osc32kclk_clockVar.getStatus());
         break;
      default:
         sim_sopt1_osc32kselVar.setValue(2);
      case 2: // RTC 32.768kHz oscillator
         system_erclk32k_clockVar.setValue(rtcclk_gated_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(rtcclk_gated_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(rtcclk_gated_clockVar.getStatus());
         break;
      case 3: // LPO 1 kHz
         system_erclk32k_clockVar.setValue(system_low_power_clockVar.getValueAsLong());
         system_erclk32k_clockVar.setOrigin(system_low_power_clockVar.getOrigin());
         system_erclk32k_clockVar.setStatus(system_low_power_clockVar.getStatus());
         break;
      }

      if (rtcclk_clockVar != null) {
         // RTC Clock out pin select 
         //============================
         Variable sim_sopt2_rtcclkoutselVar = getVariable("sim_sopt2_rtcclkoutsel");
         switch ((int)sim_sopt2_rtcclkoutselVar.getValueAsLong()) {
         default:
            sim_sopt2_rtcclkoutselVar.setValue(0);
         case 0: // RTC seconds clock = 1Hz
            rtc_clkoutVar.setValue((rtcclk_clockVar.getValueAsLong()!=0)?1:0);
            rtc_clkoutVar.setStatus(rtcclk_clockVar.getStatus());
            rtc_clkoutVar.setOrigin(rtcclk_clockVar+" (1Hz output)");
            break;
         case 1: // RTC 32.768kHz oscillator
            rtc_clkoutVar.setValue(rtcclk_gated_clockVar.getValueAsLong());
            rtc_clkoutVar.setStatus(rtcclk_gated_clockVar.getStatus());
            rtc_clkoutVar.setOrigin(rtcclk_gated_clockVar.getOrigin());
            break;
         }
      }
      // Find PLLFLLCLOCK
      //=====================================
      switch ((int)sim_sopt2_pllfllselVar.getValueAsLong()) {
      default:
         sim_sopt2_pllfllselVar.setValue(0);
      case 0:
         if (system_mcgfllclk_clockVar != null) {
            system_peripheral_clockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
            system_peripheral_clockVar.setStatus(system_mcgfllclk_clockVar.getFilteredStatus());
            system_peripheral_clockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         }
         break;
      case 1:
         if (system_mcgpllclk_clockVar != null) {
            system_peripheral_clockVar.setValue(system_mcgpllclk_clockVar.getValueAsLong());
            system_peripheral_clockVar.setStatus(system_mcgpllclk_clockVar.getFilteredStatus());
            system_peripheral_clockVar.setOrigin(system_mcgpllclk_clockVar.getOrigin());
         }
         else {
            sim_sopt2_pllfllselVar.setValue(0);
         }
         break;
      case 2:
         if (usb1pfdclk_ClockVar != null) {
            system_peripheral_clockVar.setValue(usb1pfdclk_ClockVar.getValueAsLong());
            system_peripheral_clockVar.setStatus(usb1pfdclk_ClockVar.getStatus());
            system_peripheral_clockVar.setOrigin(usb1pfdclk_ClockVar.getOrigin());
         }
         else {
            sim_sopt2_pllfllselVar.setValue(0);
         }
         break;
      case 3:
         if (system_irc48m_clockVar != null) {
            system_peripheral_clockVar.setValue(system_irc48m_clockVar.getValueAsLong());
            system_peripheral_clockVar.setStatus(system_irc48m_clockVar.getStatus());
            system_peripheral_clockVar.setOrigin(system_irc48m_clockVar.getOrigin());
         }
         else {
            sim_sopt2_pllfllselVar.setValue(0);
         }
         break;
      }

      // UART0 Clock source select (if present)
      //============================
      clockSelector.lpClockSelect("sim_sopt2_uart0src", "system_uart0_clock");

      // LPUART Clock source select (if present)
      //========================================
      clockSelector.lpClockSelect("sim_sopt2_lpuartsrc", "system_lpuart_clock");

      // TPM Clock source select 
      //============================
      clockSelector.lpClockSelect("sim_sopt2_tpmsrc", "system_tpm_clock");

      // USB FS Clock source select 
      //============================
      Variable sim_sopt2_usbsrcVar = safeGetVariable("sim_sopt2_usbsrc");
      if (sim_sopt2_usbsrcVar != null) {
         Variable sim_clkdiv2_usbVar    = safeGetVariable("sim_clkdiv2_usb");

         if (sim_clkdiv2_usbVar != null) {
            // USB divider CLKDIV2 exists

            int pllCalcValue = -1;
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
               for (int  pllflldiv=0; pllflldiv<=7; pllflldiv++) {
                  for (int  pllfllfrac=0; pllfllfrac<=1; pllfllfrac++) {
                     long testValue = Math.round(clock*(pllfllfrac+1.0)/(pllflldiv+1.0));
                     if (testValue == 48000000) {
                        pllCalcValue = (pllflldiv<<1) + pllfllfrac;
                        break;
                     }
                  }
                  if (pllCalcValue>=0) {
                     break;
                  }
               }
               sim_clkdiv2_usbVar.enable(true);
               if (pllCalcValue>=0) {
                  ((ChoiceVariable)sim_clkdiv2_usbVar).setRawValue(pllCalcValue);
                  sim_clkdiv2_usbVar.setOrigin("Automatically calculated from input clock");
                  sim_clkdiv2_usbVar.setLocked(true);
               }
               else {
                  sim_clkdiv2_usbVar.setOrigin("Manually selected");
                  sim_clkdiv2_usbVar.setLocked(false);
               }
            }
         }
         Variable system_usbfs_clockVar = getVariable("system_usbfs_clock");
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
               int  pllValue = Long.decode(sim_clkdiv2_usbVar.getSubstitutionValue()).intValue();
               int  pllfllfrac  = pllValue&0x1;
               int  pllflldiv   = (pllValue>>1)&0x7;
               long pllPostDiv2 = system_peripheral_clockVar.getValueAsLong()*(pllfllfrac+1)/(pllflldiv+1);

               system_usbfs_clockVar.setValue(pllPostDiv2);
               system_usbfs_clockVar.setStatus(system_peripheral_clockVar.getStatus());
               system_usbfs_clockVar.setOrigin(system_peripheral_clockVar.getOrigin()+" after /CLKDIV2");
            }
            else {
               // Directly using peripheral clock
               system_usbfs_clockVar.setValue(system_peripheral_clockVar.getValueAsLong());
               system_usbfs_clockVar.setStatus(system_peripheral_clockVar.getStatus());
               system_usbfs_clockVar.setOrigin(system_peripheral_clockVar.getOrigin());
            }
         }
      }

      Variable     system_core_clockVar           =  getVariable("system_core_clock");
      Variable     system_bus_clockVar            =  getVariable("system_bus_clock");
      Variable     system_flexbus_clockVar        =  safeGetVariable("system_flexbus_clock");
      Variable     system_flash_clockVar          =  getVariable("system_flash_clock");

      Variable     sim_clkdiv1_outdiv1Var         =  getVariable("sim_clkdiv1_outdiv1");
      Variable     sim_clkdiv1_outdiv2Var         =  getVariable("sim_clkdiv1_outdiv2");
      Variable     sim_clkdiv1_outdiv3Var         =  safeGetVariable("sim_clkdiv1_outdiv3");
      Variable     sim_clkdiv1_outdiv4Var         =  getVariable("sim_clkdiv1_outdiv4");


      // Core Clock
      //===========================================
      // Attempt to find acceptable divisor
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
                  ((divisor % coreDivisor.divisor) == 0) &&    // Even multiple
                  (Math.abs(coreDivisor.divisor-divisor)<8);   //  Differ from core < 8
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
                     ((divisor % coreDivisor.divisor) == 0) &&    // Even multiple
                     (Math.abs(coreDivisor.divisor-divisor)<8);   // Differ from core < 8
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

      // Flash Clock
      //===========================================
      final FindDivisor flashDivisor = new FindDivisor(inputFrequency, system_flash_clockVar.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=MAX_FLASH_CLOCK_FREQ) &&
                  (frequency<=busDivisor.nearestTargetFrequency) &&
                  ((divisor % coreDivisor.divisor) == 0) &&    // Even multiple
                  (Math.abs(coreDivisor.divisor-divisor)<8);   // Differ from core < 8

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
//               System.err.println("Rejected f= " + frequency);
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