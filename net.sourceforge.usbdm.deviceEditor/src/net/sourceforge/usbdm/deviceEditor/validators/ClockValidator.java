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
      system_core_clockNode.setMin(0);
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
            sb.append(" "+EngineeringNotation.convert(frequency, 3)+"Hz");
            if ((Math.abs(frequency-targetFrequency))<(Math.abs(nearestValue-targetFrequency))) {
               nearestValue = frequency;
               nearestDivisor = divisor;
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
   protected void validate() {

      // Clock monitors
      //=================================
      Variable     mcg_c6_cme0Node                 =  getVariable("mcg_c6_cme0");
      Variable     mcg_c2_locre0Node               =  getVariable("mcg_c2_locre0");
      
      Variable     mcg_c8_cme1Node                 =  getVariable("mcg_c8_cme1");
      Variable     mcg_c8_locre1Node               =  getVariable("mcg_c8_locre1");

      Variable     mcg_c9_pll_cmeNode              =  getVariable("mcg_c9_pll_cme");
      Variable     mcg_c9_pll_locreNode            =  getVariable("mcg_c9_pll_locre");
      
//      Variable     mcg_c6_lolie0Node               =  getVariable("mcg_c6_lolie0");
//      
//      Variable     mcg_c8_lolreNode                =  getVariable("mcg_c8_lolre");
//      
//      Variable     mcg_c11_pllcsNode               =  getVariable("mcg_c11_pllcs");
      
      mcg_c2_locre0Node.enable(mcg_c6_cme0Node.getValueAsBoolean());
      mcg_c8_locre1Node.enable(mcg_c8_cme1Node.getValueAsBoolean());
      mcg_c9_pll_locreNode.enable(mcg_c9_pll_cmeNode.getValueAsBoolean());
      
      // MCGIR
      //=================================
      Variable     slow_irc_clockNode               =  getVariable("system_slow_irc_clock");
      Variable     fast_irc_clockNode               =  getVariable("system_fast_irc_clock");
      Variable     mcg_sc_fcrdivNode                =  safeGetVariable("mcg_sc_fcrdiv");
      Variable     mcg_c2_ircsNode                  =  getVariable("mcg_c2_ircs");
      Variable     mcg_c1_irclkenNode               =  getVariable("mcg_c1_irclken");
      Variable     mcg_c1_irefstenNode              =  getVariable("mcg_c1_irefsten");
      Variable     system_mcgir_clockNode           =  getVariable("system_mcgir_clock");

      // Internal
      //=================================
      Variable     system_irc48m_clockNode          =  getVariable("system_irc48m_clock");
//    Variable     usb_clkin_clockNode              =  getLongVariable("system_usb_clkin_clock");
      Variable     system_low_power_clockNode       =  getVariable("system_low_power_clock");

      // OSC
      //=================================
      Variable     osc_cr_erclkenNode               =  getVariable("osc_cr_erclken");
      Variable     oscclk_clockNode                 =  getVariable("oscclk_clock");
      Variable     mcg_c2_erefs0Node                =  getVariable("mcg_c2_erefs0");
//    Variable mcg_c2_hgo0Node                      =  getLongVariable("mcg_c2_hgo0");
//    Variable osc_cr_scpNode                       =  getVariable("osc_cr_scp");
//    Variable osc_cr_erefstenNode                  =  getVariable("osc_cr_erefsten");
      Variable     mcg_c2_range0Node                =  getVariable("mcg_c2_range0");
      Variable     system_oscerclk_undiv_clockNode  =  safeGetVariable("system_oscerclk_undiv_clock");
      if (system_oscerclk_undiv_clockNode == null) {
         system_oscerclk_undiv_clockNode = new LongVariable("system_oscerclk_undiv_clock");
      }
      Variable     osc_div_erpsNode                 =  getVariable("osc_div_erps");
      Variable     system_oscerclk_clockNode        =  getVariable("system_oscerclk_clock");

      // RTC
      //=================================
      Variable     rtc_cr_osceNode                  =  getBooleanVariable("rtc_cr_osce");
      Variable     rtcclk_clockNode                 =  getVariable("rtcclk_clock");
      Variable     rtc_cr_scpNode                   =  getVariable("rtc_cr_scp");
      Variable     rtc_cr_clkoNode                  =  getVariable("rtc_cr_clko");
      Variable     sim_sopt1_osc32kselNode          =  getVariable("sim_sopt1_osc32ksel");
      Variable     system_erclk32k_clockNode        =  getVariable("system_erclk32k_clock");
      Variable     sim_sopt2_rtcclkoutselNode       =  getVariable("sim_sopt2_rtcclkoutsel");
      Variable     system_rtc_clkoutNode            =  getVariable("system_rtc_clkout");

      //=================================
      Variable     clock_modeNode                   =  getVariable("clock_mode");
      Variable     mcg_c7_oscselNode                =  safeGetVariable("mcg_c7_oscsel");
      Variable     mcg_erc_clockNode                =  getVariable("mcg_erc_clock");

      // FLL
      //=================================
      Variable     fll_enabledNode                  =  getVariable("fll_enabled");
      Variable     fllInputFrequencyNode            =  getVariable("fllInputFrequency");
      Variable     fllTargetFrequencyNode           =  getVariable("fllTargetFrequency");
      Variable     mcg_c4_dmx32Node                 =  getVariable("mcg_c4_dmx32");
      Variable     mcg_c1_frdivNode                 =  getVariable("mcg_c1_frdiv");
      Variable     mcg_c4_drst_drsNode              =  getVariable("mcg_c4_drst_drs");
      Variable     system_mcgffclk_clockNode        =  getVariable("system_mcgffclk_clock");

      // PLL
      //=================================
      Variable     pll_enabledNode                  =  getVariable("pll_enabled");
      Variable     mcg_c5_pllclken_Node             =  getVariable("mcg_c5_pllclken");
      Variable     pllInputFrequencyNode            =  getVariable("pllInputFrequency");
      Variable     pllTargetFrequencyNode           =  getVariable("pllTargetFrequency");
      Variable     mcg_c5_prdiv0Node                =  getVariable("mcg_c5_prdiv0");
      Variable     mcg_c5_pllstenNode               =  getVariable("mcg_c5_pllsten");
      Variable     mcg_c6_vdiv0Node                 =  getVariable("mcg_c6_vdiv0");

      //=================================
      Variable     system_mcgout_clock_sourceNode         =  getVariable("system_mcgout_clock_source");
      Variable     system_mcgout_clockNode          =  getVariable("system_mcgout_clock");
      Variable     system_core_clockNode            =  getVariable("system_core_clock");
      Variable     system_bus_clockNode             =  getVariable("system_bus_clock");
      Variable     system_flexbus_clockNode         =  safeGetVariable("system_flexbus_clock");
      Variable     system_flash_clockNode           =  getVariable("system_flash_clock");

      // Hidden
      //=====================
      Variable     mcg_c1_irefsNode                 =  getVariable("mcg_c1_irefs");
      Variable     mcg_c1_clksNode                  =  getVariable("mcg_c1_clks");
      Variable     mcg_c2_lpNode                    =  getVariable("mcg_c2_lp");
      Variable     mcg_c6_pllsNode                  =  getVariable("mcg_c6_plls");
                                                        
      Variable     sim_clkdiv1_outdiv1Node          =  getVariable("sim_clkdiv1_outdiv1");
      Variable     sim_clkdiv1_outdiv2Node          =  getVariable("sim_clkdiv1_outdiv2");
      Variable     sim_clkdiv1_outdiv3Node          =  safeGetVariable("sim_clkdiv1_outdiv3");
      Variable     sim_clkdiv1_outdiv4Node          =  getVariable("sim_clkdiv1_outdiv4");
      
      // Main clock mode
      //====================
      ClockMode clock_mode = ClockMode.valueOf(clock_modeNode.getSubstitutionValue());
      
      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //
      boolean  mcg_c2_erefs0 = mcg_c2_erefs0Node.getValueAsBoolean();
      long     oscclk_clock  = oscclk_clockNode.getValueAsLong();
      
      OscCheck oscCheck = new OscCheck(mcg_c2_erefs0, oscclk_clock);
      oscclk_clockNode.setOrigin(oscCheck.oscclk_clockOrigin);
      oscclk_clockNode.setStatus(oscCheck.oscclk_clockStatus);
      mcg_c2_erefs0Node.setStatus(oscCheck.oscclk_clockStatus);
      
      // Determine OSCERCLK, OSCERCLK_UNDIV 
      //==================================
      if (osc_cr_erclkenNode.getValueAsBoolean()) {
         // Oscillator/clock enabled
         system_oscerclk_undiv_clockNode.setValue(oscCheck.oscclk_clock);
         system_oscerclk_undiv_clockNode.setStatus(oscCheck.oscclk_clockStatus);
         system_oscerclk_undiv_clockNode.setOrigin(oscCheck.oscclk_clockOrigin);
         long system_oscerclk = oscCheck.oscclk_clock;
         String oscclk_clockOrg = oscCheck.oscclk_clockOrigin;
         if (osc_div_erpsNode != null) {
            // If divider exists
            system_oscerclk /= 1<<osc_div_erpsNode.getValueAsLong();
            oscclk_clockOrg += "/osc_div_erps";
         }
         system_oscerclk_clockNode.setValue(system_oscerclk);
         system_oscerclk_clockNode.setStatus(oscCheck.oscclk_clockStatus);
         system_oscerclk_clockNode.setOrigin(oscclk_clockOrg);
      }
      else {
         Message osc_crMessage = new Message("Disabled by osc_cr_erclken", Severity.OK);
         // Oscillator/clock disabled
         system_oscerclk_undiv_clockNode.setValue(0);
         system_oscerclk_undiv_clockNode.setStatus(osc_crMessage);
         system_oscerclk_undiv_clockNode.setOrigin(oscCheck.oscclk_clockOrigin);
         system_oscerclk_clockNode.setValue(0);
         system_oscerclk_clockNode.setStatus(osc_crMessage);
         system_oscerclk_clockNode.setOrigin(oscCheck.oscclk_clockOrigin);
      }
      
      // Determine MCGIRCLK (not gated/undivided and gated)
      //========================================
      Variable system_mcgir_ungated_clock = new LongVariable("system_mcgir_ungated");
      if (mcg_c2_ircsNode.getValueAsBoolean()) {
         // Fast IRC selected
         if (mcg_sc_fcrdivNode != null) {
            // Variable divisor
            long mcg_sc_fcrdiv = mcg_sc_fcrdivNode.getValueAsLong();
            system_mcgir_ungated_clock.setOrigin("(Fast IRC)/FCRDIV");
            system_mcgir_ungated_clock.setValue(fast_irc_clockNode.getValueAsLong() / (1<<mcg_sc_fcrdiv));
         }
         else {
            // Fixed divisor of 2
            system_mcgir_ungated_clock.setOrigin("(Fast IRC)/2");
            system_mcgir_ungated_clock.setValue(fast_irc_clockNode.getValueAsLong() / 2);
         }
      }
      else {
         // Slow IRC selected
         system_mcgir_ungated_clock.setOrigin("Slow IRC");
         system_mcgir_ungated_clock.setValue(slow_irc_clockNode.getValueAsLong());
      }
      system_mcgir_clockNode.setOrigin(system_mcgir_ungated_clock.getOrigin());
      if (mcg_c1_irclkenNode.getValueAsBoolean()) {
         // Enabled
         system_mcgir_clockNode.setValue(system_mcgir_ungated_clock.getValueAsLong());
         system_mcgir_clockNode.setStatus((Message)null);
         system_mcgir_clockNode.enable(true);
         mcg_c1_irefstenNode.enable(true);
         }
      else {
         // Disabled
         system_mcgir_clockNode.setValue(0);
         system_mcgir_clockNode.setStatus(new Message("Disabled by mcg_c1_irclken", Severity.OK));
         system_mcgir_clockNode.enable(false);
         mcg_c1_irefstenNode.enable(false);
      }
      
      // Determine RTC 32K CLK
      //==================================
      boolean rtc_cr_osce = rtc_cr_osceNode.getValueAsBoolean();
      
      rtcclk_clockNode.enable(rtc_cr_osce);
      rtc_cr_scpNode.enable(rtc_cr_osce);
      rtc_cr_clkoNode.enable(rtc_cr_osce);
      
      // 32K clock from RTC
      long    rtcclk_clock;
      Message rtcclk_clock_Message = null;
      if (rtc_cr_osce) {
         rtcclk_clock = rtcclk_clockNode.getValueAsLong();
      }
      else {
         rtcclk_clock = 0;
         rtcclk_clock_Message = new Message("Disabled by rtc_cr_osce", Severity.WARNING);
      }
      
      // 32K clock from RTC gated by rtc_cr_clko
      long rtcclk_clock_gated           = rtcclk_clock;
      Message rtcclk_clock_gatedMessage = rtcclk_clock_Message;
      if (!rtc_cr_clkoNode.getValueAsBoolean()) { // Active low!
         // RTCCLK not enabled - illegal as clock choice
         rtcclk_clock_gated = 0;
         rtcclk_clock_gatedMessage = new Message("Origin: rtcclk_clock, disabled by rtc_cr_clko", Severity.WARNING);
      }
      
      // Determine ERCLK32K
      //==================================
      int  sim_sopt1_osc32ksel = (int)sim_sopt1_osc32kselNode.getValueAsLong();
      long system_erclk32k;
      
      Message system_erclk32k_Message = null;
      String  system_erclk32k_Origin;   
      switch (sim_sopt1_osc32ksel) {
      case 0: // System oscillator (OSC32KCLK)
         system_erclk32k         = oscCheck.osc32kclk_clock;
         system_erclk32k_Message = oscCheck.osc32kclk_clockStatus;
         system_erclk32k_Origin  = oscCheck.osc32kclk_clockOrigin;
         break;
      default:
         sim_sopt1_osc32ksel = 2;
         sim_sopt1_osc32kselNode.setValue(sim_sopt1_osc32ksel);
      case 2: // RTC 32.768kHz oscillator
         system_erclk32k         = rtcclk_clock_gated;
         system_erclk32k_Message = rtcclk_clock_gatedMessage;
         system_erclk32k_Origin  = "RTCCLK";
         break;
      case 3: // LPO 1 kHz
         system_erclk32k         = system_low_power_clockNode.getValueAsLong();
         system_erclk32k_Origin  = "Low Power Oscillator";
         break;
      }
      system_erclk32k_clockNode.setValue(system_erclk32k);
      system_erclk32k_clockNode.setStatus(system_erclk32k_Message);
      system_erclk32k_clockNode.setOrigin(system_erclk32k_Origin);

      // RTC Clock out pin select 
      //============================
      long sim_sopt2_rtcclkoutsel = sim_sopt2_rtcclkoutselNode.getValueAsLong();

      long rtcclkout_clock= 0;
      Message rtcclkoutMessage;
      String  rtcclkoutOrigin;
      switch ((int)sim_sopt2_rtcclkoutsel) {
      default:
         sim_sopt2_rtcclkoutsel = 0;
         sim_sopt2_rtcclkoutselNode.setValue(sim_sopt2_rtcclkoutsel);
      case 0: // RTC seconds clock = 1Hz
         rtcclkout_clock = (rtcclk_clock_Message==null)?1:0;
         rtcclkoutMessage = rtcclk_clock_Message;
         rtcclkoutOrigin  = "RTC 1Hz output";
         break;
      case 1: // RTC 32.768kHz oscillator
         rtcclkout_clock  = rtcclk_clock_gated;
         rtcclkoutMessage = rtcclk_clock_gatedMessage;
         rtcclkoutOrigin  = "RTC 32kHz output";
         break;
      }
      system_rtc_clkoutNode.setStatus(rtcclkoutMessage);
      system_rtc_clkoutNode.setValue(rtcclkout_clock);
      system_rtc_clkoutNode.setOrigin(rtcclkoutOrigin);
      
      // Determine MCG external reference clock [mcg_erc_clock]
      //========================================================

      int oscsel;
      if (mcg_c7_oscselNode == null) {
         // Default if no MCG_C7_OSCSEL register field
         oscsel = 0;
      }
      else {
         oscsel = (int)mcg_c7_oscselNode.getValueAsLong();
      }
      switch (oscsel) {
      default:
      case 0: // ERC = OSCCLK
         mcg_erc_clockNode.setValue(oscCheck.oscclk_clock);
         mcg_erc_clockNode.setStatus(oscCheck.oscclk_clockStatus);
         mcg_erc_clockNode.setOrigin(oscCheck.oscclk_clockOrigin);
         break;
      case 1: // ERC = OSC32KCLK
         mcg_erc_clockNode.setValue(rtcclk_clock);
         mcg_erc_clockNode.setStatus(rtcclk_clock_Message);
         mcg_erc_clockNode.setOrigin("RTCCLK");
         break;
      case 2: // ERC = IRC48MCLK
         mcg_erc_clockNode.setValue(system_irc48m_clockNode.getValueAsLong());
         mcg_erc_clockNode.setStatus((Message)null);
         mcg_erc_clockNode.setOrigin("IRC48MCLK");
         break;
      }
      
//      System.err.println("ClockValidate.validate() system_mcgir_clock = " + system_mcgir_clock+ ", mcg_erc_clock = " + mcg_erc_clock);

      // Determine MCG_C1_IREFS
      boolean mcg_c1_irefs;
      switch (clock_mode) {
      case ClockMode_None:
      case ClockMode_FEI:
      case ClockMode_FBI:
      case ClockMode_BLPI:
         mcg_c1_irefs = true;
         break;
      default:
      case ClockMode_FBE:
      case ClockMode_FEE:
      case ClockMode_BLPE:
      case ClockMode_PBE:
      case ClockMode_PEE:
         mcg_c1_irefs = false;
         break;
      }
      mcg_c1_irefsNode.setValue(mcg_c1_irefs);

      //=======================================
      // Find FLL dividers
      FllDivider fllCheck = new FllDivider(
            oscCheck.mcg_c2_range,
            mcg_c1_irefs,
            mcg_erc_clockNode,
            slow_irc_clockNode.getValueAsLong(),
            mcg_c7_oscselNode.getValueAsLong(), 
            mcg_c4_dmx32Node.getValueAsBoolean(),
            fllInputFrequencyNode,
            fllTargetFrequencyNode);

      mcg_c1_frdivNode.setValue(fllCheck.mcg_c1_frdiv);
      mcg_c2_range0Node.setValue(fllCheck.mcg_c2_range);
      mcg_c4_drst_drsNode.setValue(fllCheck.mcg_c4_drst_drs);
      
      // Main clock mode
      //===============================
      int     mcg_c1_clks                = 0;
      int     mcg_c6_plls                = 0;
      int     mcg_c2_lp                  = 0;
      
      Message clock_mode_Status = null;

      switch (clock_mode) {
      case ClockMode_None:
         mcg_c1_clks             = 0;
         mcg_c6_plls             = 0;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("FLL output");
         system_mcgout_clockNode.setValue(fllTargetFrequencyNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(fllTargetFrequencyNode.getOrigin());
         system_mcgout_clockNode.setStatus((Message)null);
         pll_enabledNode.setValue(mcg_c5_pllclken_Node.getValueAsBoolean());
         fll_enabledNode.setValue(true);
         clock_mode_Status = new Message("No clock settings are applied", Severity.WARNING);
         break;
      case ClockMode_FEI:
         mcg_c1_clks             = 0;
         mcg_c6_plls             = 0;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("FLL output");
         system_mcgout_clockNode.setValue(fllTargetFrequencyNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(fllTargetFrequencyNode.getOrigin());
         system_mcgout_clockNode.setStatus(fllTargetFrequencyNode.getStatus());
         pll_enabledNode.setValue(mcg_c5_pllclken_Node.getValueAsBoolean());
         fll_enabledNode.setValue(true);
         break;
      case ClockMode_FEE:
         mcg_c1_clks             = 0;
         mcg_c6_plls             = 0;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("FLL output");
         system_mcgout_clockNode.setValue(fllTargetFrequencyNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(fllTargetFrequencyNode.getOrigin());
         system_mcgout_clockNode.setStatus(fllTargetFrequencyNode.getStatus());
         pll_enabledNode.setValue(mcg_c5_pllclken_Node.getValueAsBoolean());
         fll_enabledNode.setValue(true);
         break;
      case ClockMode_FBI:
         mcg_c1_clks             = 1;
         mcg_c6_plls             = 0;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("MCGIRCLK");
         system_mcgout_clockNode.setValue(system_mcgir_ungated_clock.getValueAsLong());
         system_mcgout_clockNode.setOrigin(system_mcgir_ungated_clock.getOrigin());
         system_mcgout_clockNode.setStatus(system_mcgir_ungated_clock.getStatus());
         pll_enabledNode.setValue(mcg_c5_pllclken_Node.getValueAsBoolean());
         fll_enabledNode.setValue(true);
         break;
      case ClockMode_FBE:
         mcg_c1_clks             = 2;
         mcg_c6_plls             = 0;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("MCGERCLK");
         system_mcgout_clockNode.setValue(mcg_erc_clockNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(mcg_erc_clockNode.getOrigin());
         system_mcgout_clockNode.setStatus(mcg_erc_clockNode.getStatus());
         pll_enabledNode.setValue(mcg_c5_pllclken_Node.getValueAsBoolean());
         fll_enabledNode.setValue(true);
         break;
      case ClockMode_BLPI:
         mcg_c1_clks             = 1;
         mcg_c6_plls             = 0;
         mcg_c2_lp               = 1;
         system_mcgout_clock_sourceNode.setValue("MCGIRCLK");
         system_mcgout_clockNode.setValue(system_mcgir_ungated_clock.getValueAsLong());
         system_mcgout_clockNode.setOrigin(system_mcgir_ungated_clock.getOrigin());
         system_mcgout_clockNode.setStatus(system_mcgir_ungated_clock.getStatus());
         pll_enabledNode.setValue(mcg_c5_pllclken_Node.getValueAsBoolean());
         fll_enabledNode.setValue(false);
         break;
      case ClockMode_BLPE:
         mcg_c1_clks             = 2;
         mcg_c6_plls             = 0;
         mcg_c2_lp               = 1;
         system_mcgout_clock_sourceNode.setValue("MCGERCLK");
         system_mcgout_clockNode.setValue(mcg_erc_clockNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(mcg_erc_clockNode.getOrigin());
         system_mcgout_clockNode.setStatus(mcg_erc_clockNode.getStatus());
         pll_enabledNode.setValue(mcg_c5_pllclken_Node.getValueAsBoolean());
         fll_enabledNode.setValue(false);
         break;
      case ClockMode_PBE:
         mcg_c1_clks             = 2;
         mcg_c6_plls             = 1;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("MCGERCLK");
         system_mcgout_clockNode.setValue(mcg_erc_clockNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(mcg_erc_clockNode.getOrigin());
         system_mcgout_clockNode.setStatus(mcg_erc_clockNode.getStatus());
         pll_enabledNode.setValue(true);
         fll_enabledNode.setValue(false);
         break;
      case ClockMode_PEE:
         mcg_c1_clks             = 0;
         mcg_c6_plls             = 1;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("PLL output");
         system_mcgout_clockNode.setValue(pllTargetFrequencyNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(pllTargetFrequencyNode.getOrigin());
         system_mcgout_clockNode.setStatus(pllTargetFrequencyNode.getStatus());
         pll_enabledNode.setValue(true);
         fll_enabledNode.setValue(false);
         break;
      }     
      
      system_mcgout_clock_sourceNode.setStatus(clock_mode_Status);
      system_mcgout_clock_sourceNode.setOrigin(system_mcgout_clockNode.getOrigin());

      mcg_c1_clksNode.setValue(mcg_c1_clks);
      mcg_c6_pllsNode.setValue(mcg_c6_plls);
      mcg_c2_lpNode.setValue(mcg_c2_lp);

      boolean pll_enabled = pll_enabledNode.getValueAsBoolean();
      pllInputFrequencyNode.enable(pll_enabled);
      pllTargetFrequencyNode.enable(pll_enabled);
      mcg_c5_prdiv0Node.enable(pll_enabled);
      mcg_c5_pllstenNode.enable(pll_enabled);
      mcg_c6_vdiv0Node.enable(pll_enabled);
      
      boolean fll_enabled = fll_enabledNode.getValueAsBoolean();
      fllTargetFrequencyNode.enable(fll_enabled);
      fllInputFrequencyNode.enable(fll_enabled);
      mcg_c4_dmx32Node.enable(fll_enabled);
      mcg_c1_frdivNode.enable(fll_enabled);
      mcg_c4_drst_drsNode.enable(fll_enabled);
      
      // Find MCGFFCLK
      //=====================================
      Long fllInputFrequency = fllInputFrequencyNode.getValueAsLong();
      system_mcgffclk_clockNode.setOrigin(fllInputFrequencyNode.getOrigin());
      if (fllInputFrequency>(system_mcgout_clockNode.getValueAsLong()/8.0)) {
         // Too high a frequency - disabled
         system_mcgffclk_clockNode.setValue(0);
         system_mcgffclk_clockNode.setStatus(new Message("Disabled as freq>(MCGOUTCLK/8)", Severity.WARNING));
      }
      else {
         system_mcgffclk_clockNode.setValue(fllInputFrequency);
         system_mcgffclk_clockNode.setStatus((Message)null);
      }
      
      // Core Clock
      //===========================================
      final FindDivisor coreDivisor = new FindDivisor(system_mcgout_clockNode.getValueAsLong(), system_core_clockNode.getValueAsLong()) {
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
      system_core_clockNode.setStatus(new Message(sb.toString(), severity));
      sim_clkdiv1_outdiv1Node.setValue(coreDivisor.divisor);

      // Bus Clock
      //===========================================
      final FindDivisor busDivisor = new FindDivisor(system_mcgout_clockNode.getValueAsLong(), system_bus_clockNode.getValueAsLong()) {
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
      system_bus_clockNode.setStatus(new Message(sb.toString(), severity));
      sim_clkdiv1_outdiv2Node.setValue(busDivisor.divisor);
      
      // Flexbus Clock
      //===========================================
      if ((sim_clkdiv1_outdiv3Node != null) && system_flexbus_clockNode != null) {
         final FindDivisor flexDivisor = new FindDivisor(system_mcgout_clockNode.getValueAsLong(), system_flexbus_clockNode.getValueAsLong()) {
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
         system_flexbus_clockNode.setStatus(new Message(sb.toString(), severity));
         sim_clkdiv1_outdiv3Node.setValue(flexDivisor.divisor);
      }
      
      // Flash Clock
      //===========================================
      final FindDivisor flashDivisor = new FindDivisor(system_mcgout_clockNode.getValueAsLong(), system_flash_clockNode.getValueAsLong()) {
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
      system_flash_clockNode.setStatus(new Message(sb.toString(), severity));
      sim_clkdiv1_outdiv4Node.setValue(flashDivisor.divisor);
      
      
      
   }
}
