package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable    ;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

public class ClockValidator extends BaseClockValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_BUS_CLOCK_FREQ;
   private final long MAX_FLASH_CLOCK_FREQ;
   private final long MAX_FLEXBUS_CLOCK_FREQ;
   
   public ClockValidator(PeripheralWithState peripheral, long maxCoreClockfrequency, long maxBusClockFrequency, long maxFlashClockFrequency, long maxFlexbusFrequency) {
      super(peripheral);
      
      MAX_CORE_CLOCK_FREQ     = maxCoreClockfrequency;
      MAX_BUS_CLOCK_FREQ      = maxBusClockFrequency;
      MAX_FLASH_CLOCK_FREQ    = maxFlashClockFrequency;
      MAX_FLEXBUS_CLOCK_FREQ  = maxFlexbusFrequency;
      
      LongVariable system_core_clockNode = getLongVariable("system_core_clock");
      system_core_clockNode.setMax(MAX_CORE_CLOCK_FREQ);

      LongVariable system_bus_clockNode = getLongVariable("system_bus_clock");
      system_bus_clockNode.setMin(0);
      system_bus_clockNode.setMax(MAX_BUS_CLOCK_FREQ);

      LongVariable system_flash_clockNode = getLongVariable("system_flash_clock");
      system_flash_clockNode.setMin(0);
      system_flash_clockNode.setMax(MAX_FLASH_CLOCK_FREQ);

      LongVariable system_flexbus_clockNode = getLongVariable("system_flexbus_clock");
      system_flexbus_clockNode.setMin(0);
      system_flexbus_clockNode.setMax(MAX_FLEXBUS_CLOCK_FREQ);
   }
   
   public ClockValidator(PeripheralWithState peripheral, long maxCoreClockfrequency, long maxBusClockFrequency, long maxFlashClockFrequency) {
      this(peripheral, maxCoreClockfrequency, maxBusClockFrequency, maxFlashClockFrequency, 50000000);
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
         double nearestValue = Double.MAX_VALUE;
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
            sb.append(" "+EngineeringNotation.convert(frequency, 3)+"Hz");
            if ((Math.abs(frequency-targetFrequency))<(Math.abs(nearestValue-targetFrequency))) {
               nearestValue = frequency;
               nearestDivisor = divisor;
            }
         }
         nearestTargetFrequency = Math.round(nearestValue);
         divisor = nearestDivisor;
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
   protected void validate() {
      
      // Internal
      //=================================
      Variable     system_irc48m_clockNode    =  getVariable("system_irc48m_clock");
//    Variable usb_clkin_clockNode            =  getLongVariable("system_usb_clkin_clock");
      Variable     system_low_power_clockNode =  getVariable("system_low_power_clock");
      Variable     slow_irc_clockNode         =  getVariable("system_slow_irc_clock");
      Variable     fast_irc_clockNode         =  getVariable("system_fast_irc_clock");
      Variable     mcg_sc_fcrdivNode          =  safeGetVariable("mcg_sc_fcrdiv");
      Variable     mcg_c1_irclkenNode         =  getVariable("mcg_c1_irclken");
      Variable     mcg_c2_ircsNode            =  getVariable("mcg_c2_ircs");
      Variable     system_mcgir_clockNode     =  getVariable("system_mcgir_clock");

      // OSC
      //=================================
      Variable     osc_cr_erclkenNode         =  getVariable("osc_cr_erclken");
      Variable     oscclk_clockNode           =  getVariable("oscclk_clock");
      Variable     mcg_c2_erefs0Node          =  getVariable("mcg_c2_erefs0");
//      Variable mcg_c2_hgo0Node            =  getLongVariable("mcg_c2_hgo0");
      Variable     mcg_c2_rangeNode           =  getVariable("mcg_c2_range0");
//      Variable osc_cr_scpNode             =  getVariable("osc_cr_scp");
//      Variable osc_cr_erefstenNode        =  getVariable("osc_cr_erefsten");
      Variable osc_div_erpsNode               =  getVariable("osc_div_erps");

      // ERCLK32K
      //=================================
      Variable     rtc_cr_osceNode            =  getBooleanVariable("rtc_cr_osce");
      Variable     rtcclk_clockNode           =  getVariable("rtcclk_clock");
      Variable     rtc_cr_scpNode             =  getVariable("rtc_cr_scp");
      Variable     rtc_cr_clkoNode            =  getVariable("rtc_cr_clko");
      Variable     sim_sopt1_osc32kselNode    =  getVariable("sim_sopt1_osc32ksel");
      Variable     sim_sopt2_rtcclkoutselNode =  getVariable("sim_sopt2_rtcclkoutsel");
      Variable     system_erclk32k_clockNode  =  getVariable("system_erclk32k_clock");
      Variable     system_rtc_clkoutNode        =  getVariable("system_rtc_clkout");
      
      //=================================
      Variable     clock_modeNode             =  getVariable("clock_mode");
      Variable     mcgOutputClockSourceNode   =  getVariable("mcgOutputClockSource");
      Variable     mcg_c7_oscselNode          =  safeGetVariable("mcg_c7_oscsel");
      Variable     system_erc_clockNode       =  getVariable("mcg_erc_clock");

      // FLL
      //=================================
      Variable     fll_enabled_node           =  getVariable("fll_enabled");

      // PLL
      //=================================
      Variable     pll_enabled_node           =  getVariable("pll_enabled");
      
      Variable     pllTargetFrequencyNode     =  getVariable("pllTargetFrequency");
      Variable     fllTargetFrequencyNode     =  getVariable("fllTargetFrequency");
      Variable     system_core_clockNode      =  getVariable("system_core_clock");
      Variable     system_bus_clockNode       =  getVariable("system_bus_clock");
      Variable     system_flexbus_clockNode   =  safeGetVariable("system_flexbus_clock");
      Variable     system_flash_clockNode     =  getVariable("system_flash_clock");
      
      Variable     mcg_c1_clksNode            =  getVariable("mcg_c1_clks");
      
      Variable     mcg_c2_lpNode              =  getVariable("mcg_c2_lp");
      Variable     mcg_c6_pllsNode            =  getVariable("mcg_c6_plls");
      Variable     system_mcgout_clockNode    =  getVariable("system_mcgout_clock");
                                                        
      Variable     sim_clkdiv1_outdiv1Node    =  getVariable("sim_clkdiv1_outdiv1");
      Variable     sim_clkdiv1_outdiv2Node    =  getVariable("sim_clkdiv1_outdiv2");
      Variable     sim_clkdiv1_outdiv3Node    =  safeGetVariable("sim_clkdiv1_outdiv3");
      Variable     sim_clkdiv1_outdiv4Node    =  getVariable("sim_clkdiv1_outdiv4");

      // System clocks
      //==================================
      Variable     system_oscerclk_clockNode  =  getVariable("system_oscerclk_clock");
      
      // Determine OSCERCLK
      //==================================
      if (osc_cr_erclkenNode.getValueAsBoolean()) {
         Long oscclk = oscclk_clockNode.getValueAsLong();
         if (osc_div_erpsNode != null) {
            // If divider exists
            oscclk /= 1<<osc_div_erpsNode.getValueAsLong();
         }
         system_oscerclk_clockNode.setValue(oscclk);
         system_oscerclk_clockNode.setMessage((Message)null);
      }
      else {
         system_oscerclk_clockNode.setValue(0);
         system_oscerclk_clockNode.setMessage(new Message("Disabled by osc_cr_erclken", Severity.OK));
      }
      // Determine MCGIRCLK (ungated and gated)
      //========================================
      long system_mcgir_ungated_clock;
      if (mcg_c2_ircsNode.getValueAsBoolean()) {
         // Fast IRC selected
         if (mcg_sc_fcrdivNode != null) {
            // Variable divisor
            long mcg_sc_fcrdiv = mcg_sc_fcrdivNode.getValueAsLong();
            system_mcgir_ungated_clock = fast_irc_clockNode.getValueAsLong() / (1<<mcg_sc_fcrdiv);
         }
         else {
            // Fixed divisor of 2
            system_mcgir_ungated_clock = fast_irc_clockNode.getValueAsLong() / 2;
         }
      }
      else {
         // Slow IRC selected
         system_mcgir_ungated_clock = slow_irc_clockNode.getValueAsLong();
      }
      if (mcg_c1_irclkenNode.getValueAsBoolean()) {
         // Enabled
         system_mcgir_clockNode.setValue(system_mcgir_ungated_clock);
         system_mcgir_clockNode.setMessage((Message)null);
         }
      else {
         // Disabled
         system_mcgir_clockNode.setValue(0);
         system_mcgir_clockNode.setMessage(new Message("Disabled by mcg_c1_irclken", Severity.OK));
      }
      
      // Determine MCG FLL external reference clock
      //==================================

      // Default if no MCG_C7_OSCSEL register field
      long mcg_erc_clock = oscclk_clockNode.getValueAsLong();
      
      if (mcg_c7_oscselNode != null) {
         switch ((int)mcg_c7_oscselNode.getValueAsLong()) {
         default:
         case 0: // ERC = OSCCLK
            mcg_erc_clock = oscclk_clockNode.getValueAsLong();
            break;
         case 1: // ERC = OSC32KCLK
            mcg_erc_clock = rtcclk_clockNode.getValueAsLong();
            break;
         case 2: // ERC = IRC48M
            mcg_erc_clock = system_irc48m_clockNode.getValueAsLong();
            break;
         }
      }

//      System.err.println("ClockValidate.validate() system_mcgir_clock = " + system_mcgir_clock+ ", mcg_erc_clock = " + mcg_erc_clock);

      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //   - Affects FLL prescale
      //
      FllDivider fllCheck = new FllDivider(
            mcg_erc_clock, 
            mcg_c2_erefs0Node.getValueAsBoolean(), 
            mcg_c7_oscselNode.getValueAsLong(),
            oscclk_clockNode.getValueAsLong());
      
      oscclk_clockNode.setMessage(fllCheck.oscclk_clockMessage);
      mcg_c2_rangeNode.setValue(fllCheck.mcg_c2_range);

      ClockMode clock_mode;
      try {
         clock_mode = ClockMode.valueOf(clock_modeNode.getSubstitutionValue());
      } catch (Exception e) {
         System.err.println(e.getMessage());
         clock_mode = ClockMode.ClockMode_None;
         clock_modeNode.setValue(0);
      }

      // Determine RTC 32K CLK
      //==================================
      boolean rtc_cr_osce = rtc_cr_osceNode.getValueAsBoolean();
      
      rtcclk_clockNode.enable(rtc_cr_osce);
      rtc_cr_scpNode.enable(rtc_cr_osce);
      rtc_cr_clkoNode.enable(rtc_cr_osce);
      
      // 32K clock from RTC
      long rtcclk_clock;
      Message rtcclk_clock_Message = new Message("Origin rtcclk_clock", Severity.OK);;
      if (rtc_cr_osce) {
         rtcclk_clock = rtcclk_clockNode.getValueAsLong();
      }
      else {
         rtcclk_clock = 0;
         rtcclk_clock_Message = new Message("Disabled as RTC OSC disabled by rtc_cr_osce", Severity.WARNING);
      }
      
      // 32K clock from RTC gated by 
      long rtcclk_clock_gated           = rtcclk_clock;
      Message rtcclk_clock_gatedMessage = rtcclk_clock_Message;
      if (rtc_cr_clkoNode.getValueAsBoolean()) {
         // RTCCLK not enabled - illegal as clock choice
         rtcclk_clock_gated = 0;
         rtcclk_clock_gatedMessage = new Message("Disabled as RTC OSC gated off by rtc_cr_clko", Severity.WARNING);
      }
      
      // Determine ERCLK32K
      //==================================
      int  sim_sopt1_osc32ksel = (int)sim_sopt1_osc32kselNode.getValueAsLong();
      long system_erclk32k     = 0;
      
      Message erclk32kMessage = null;
      switch (sim_sopt1_osc32ksel) {
      case 0: // System oscillator (OSC32KCLK)
         if (fllCheck.mcg_c2_range == 0) {
            // OSC in low range
            system_erclk32k = oscclk_clockNode.getValueAsLong();
            erclk32kMessage = new Message("Origin: System oscillator (OSC32KCLK)", Severity.OK);
         }
         else {
            // OSC not in low range - illegal as 32K clock
            system_erclk32k = 0;
            erclk32kMessage = new Message("Disabled as OSC32KCLK unavailable\n(OSC not in low frequency range)", Severity.WARNING);
         }
         break;
      default:
         sim_sopt1_osc32ksel = 2;
         sim_sopt1_osc32kselNode.setValue(sim_sopt1_osc32ksel);
      case 2: // RTC 32.768kHz oscillator
         system_erclk32k = rtcclk_clock_gated;
         erclk32kMessage = rtcclk_clock_gatedMessage;
         break;
      case 3: // LPO 1 kHz
         system_erclk32k = system_low_power_clockNode.getValueAsLong();
         erclk32kMessage = new Message("Origin: LPO", Severity.OK);
         break;
      }
      system_erclk32k_clockNode.setValue(system_erclk32k);
      system_erclk32k_clockNode.setMessage(erclk32kMessage);
      sim_sopt1_osc32kselNode.setMessage(erclk32kMessage);

      // RTC Clock out pin select 
      //============================
      long sim_sopt2_rtcclkoutsel = sim_sopt2_rtcclkoutselNode.getValueAsLong();

      long rtcclkout_clock= 0;
      
      switch ((int)sim_sopt2_rtcclkoutsel) {
      default:
         sim_sopt2_rtcclkoutsel = 0;
         sim_sopt2_rtcclkoutselNode.setValue(sim_sopt2_rtcclkoutsel);
      case 0: // // RTC seconds clock = 1Hz
         rtcclkout_clock = (rtcclk_clock_gatedMessage.getSeverity()==Severity.OK)?1:0;
         break;
      case 1: // RTC 32.768kHz oscillator
         rtcclkout_clock        = rtcclk_clock_gated;
         break;
      }
      sim_sopt2_rtcclkoutselNode.setMessage(rtcclk_clock_gatedMessage);
      system_rtc_clkoutNode.setMessage(rtcclk_clock_gatedMessage);
      system_rtc_clkoutNode.setValue(rtcclkout_clock);
      
      // Main clock mode
      //===============================
      boolean pll_enabled = false;
      boolean fll_enabled = false;

      int     mcg_c1_clks                = 0;
      int     mcg_c6_plls                = 0;
      int     mcg_c2_lp                  = 0;
      
      long    fllTargetFrequency         = fllTargetFrequencyNode.getValueAsLong();
      long    pllTargetFrequency         = pllTargetFrequencyNode.getValueAsLong();
      long    system_mcgout_clock        = 0;
      Message primaryClockModeMessage    = null;

      String mcgOutputClockSource = "Unknown";
      switch (clock_mode) {
      case ClockMode_None:
         mcg_c1_clks          = 0;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = fllTargetFrequency;
         mcgOutputClockSource = "FLL output";
         primaryClockModeMessage = new Message("No clock settings are applied", Severity.WARNING);
         break;
      case ClockMode_FEI:
         mcg_c1_clks          = 0;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = fllTargetFrequency;
         mcgOutputClockSource = "FLL output";
         fll_enabled          = true;
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_FEE:
         mcg_c1_clks          = 0;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = fllTargetFrequency;
         mcgOutputClockSource = "FLL output";
         fll_enabled          = true;
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_FBI:
         mcg_c1_clks          = 1;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = system_mcgir_ungated_clock;
         mcgOutputClockSource = "MCGIRCLK";
         fll_enabled          = true;
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_FBE:
         mcg_c1_clks          = 2;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = mcg_erc_clock;
         mcgOutputClockSource = "MCGERCLK";
         fll_enabled          = true;
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_BLPI:
         mcg_c1_clks          = 1;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 1;
         system_mcgout_clock  = system_mcgir_ungated_clock;
         mcgOutputClockSource = "MCGIRCLK";
         break;
      case ClockMode_BLPE:
         mcg_c1_clks          = 2;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 1;
         system_mcgout_clock  = mcg_erc_clock;
         mcgOutputClockSource = "MCGERCLK";
         break;
      case ClockMode_PBE:
         mcg_c1_clks          = 2;
         mcg_c6_plls          = 1;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = mcg_erc_clock;
         mcgOutputClockSource = "MCGERCLK";
         pll_enabled          = true;
         break;
      case ClockMode_PEE:
         mcg_c1_clks          = 0;
         mcg_c6_plls          = 1;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = pllTargetFrequency;
         mcgOutputClockSource = "PLL output";
         pll_enabled          = true;
         break;
      }
      fll_enabled_node.setValue(fll_enabled);
      fllTargetFrequencyNode.enable(fll_enabled);

      pll_enabled_node.setValue(pll_enabled);
      pllTargetFrequencyNode.enable(pll_enabled);
      
      mcgOutputClockSourceNode.setValue(mcgOutputClockSource);
      
      if (system_erc_clockNode == null) {
         // Default to oscillator clock
         system_erc_clockNode =  getVariable("oscclk_clock");
      }

      // Core Clock
      //===========================================
      final FindDivisor coreDivisor = new FindDivisor(system_mcgout_clock, system_core_clockNode.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return frequency<=MAX_CORE_CLOCK_FREQ;
         }
      };
      Severity severity = Severity.OK;
      StringBuilder sb = new StringBuilder();
      if (coreDivisor.divisor == 0) {
         severity = Severity.ERROR;
         sb.append("Illegal Frequency\n");
      }
      sb.append(coreDivisor.divisors);
      system_core_clockNode.setValue(coreDivisor.nearestTargetFrequency);
      system_core_clockNode.setMessage(new Message(sb.toString(), severity));
      sim_clkdiv1_outdiv1Node.setValue(coreDivisor.divisor);

      // Bus Clock
      //===========================================
      final FindDivisor busDivisor = new FindDivisor(system_mcgout_clock, system_bus_clockNode.getValueAsLong()) {
         @Override
         boolean okValue(int divisor, double frequency) {
            return (frequency<=MAX_BUS_CLOCK_FREQ) &&
                   ((divisor % coreDivisor.divisor) == 0) &&    // Even multiple
                   (Math.abs(coreDivisor.divisor-divisor)<8);   //  Differ from core < 8

         }
      };
      severity = Severity.OK;
      sb = new StringBuilder();
      if (busDivisor.divisor == 0) {
         severity = Severity.ERROR;
         sb.append("Illegal Frequency\n");
      }
      sb.append(busDivisor.divisors);
      system_bus_clockNode.setValue(busDivisor.nearestTargetFrequency);
      system_bus_clockNode.setMessage(new Message(sb.toString(), severity));
      sim_clkdiv1_outdiv2Node.setValue(busDivisor.divisor);
      
      // Flexbus Clock
      //===========================================
      if ((sim_clkdiv1_outdiv3Node != null) && system_flexbus_clockNode != null) {
         final FindDivisor flexDivisor = new FindDivisor(system_mcgout_clock, system_flexbus_clockNode.getValueAsLong()) {
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
         if (flexDivisor.divisor == 0) {
            severity = Severity.ERROR;
            sb.append("Illegal Frequency\n");
         }
         sb.append(flexDivisor.divisors);
         system_flexbus_clockNode.setValue(flexDivisor.nearestTargetFrequency);
         system_flexbus_clockNode.setMessage(new Message(sb.toString(), severity));
         sim_clkdiv1_outdiv3Node.setValue(flexDivisor.divisor);
      }
      
      // Flash Clock
      //===========================================
      final FindDivisor flashDivisor = new FindDivisor(system_mcgout_clock, system_flash_clockNode.getValueAsLong()) {
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
      if (flashDivisor.divisor == 0) {
         severity = Severity.ERROR;
         sb.append("Illegal Frequency\n");
      }
      sb.append(flashDivisor.divisors);
      system_flash_clockNode.setValue(flashDivisor.nearestTargetFrequency);
      system_flash_clockNode.setMessage(new Message(sb.toString(), severity));
      sim_clkdiv1_outdiv4Node.setValue(flashDivisor.divisor);

      Message message = fllTargetFrequencyNode.getMessage();
      if (message != null) {
         clock_modeNode.setMessage(primaryClockModeMessage);
      }

      system_erc_clockNode.setValue(mcg_erc_clock);
      system_mcgout_clockNode.setValue(system_mcgout_clock);
      mcg_c1_clksNode.setValue(mcg_c1_clks);
      mcg_c6_pllsNode.setValue(mcg_c6_plls);
      mcg_c2_lpNode.setValue(mcg_c2_lp);
   }
}
