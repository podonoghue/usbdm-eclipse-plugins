package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable    ;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Message;
import net.sourceforge.usbdm.deviceEditor.model.Message.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine MCG settings
 
 * Used for:
 *     mcg_mk_ics48mml
 *     mcg_mk
 */
public class ClockValidator_MK_ICS48M extends BaseClockValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_BUS_CLOCK_FREQ;
   private final long MAX_FLASH_CLOCK_FREQ;
   private final long MAX_FLEXBUS_CLOCK_FREQ;
   
   private final long PLL_IN_MIN;
   private final long PLL_IN_MAX;

   private final long PLL_OUT_MIN;
   private final long PLL_OUT_MAX;

   private final long  PRDIV_MIN;
   private final long  PRDIV_MAX;

   private final long  VDIV_MIN;
   private final long  VDIV_MAX;

   private final long  PLL_POST_DIV;

   private final static String[] names = {
         "/RTC/rtc_cr_osce",
         "/RTC/rtcclk_clock",
         "/RTC/rtc_cr_scp",
         "/RTC/rtc_cr_clko",
         "/OSC0/oscclk_clock",
         "/OSC0/osc32kclk_clock",
         OscValidate.OSC_RANGE_KEY,
         "/OSC0/range",
   };

   public ClockValidator_MK_ICS48M(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);

      ListIterator<Object> it = values.listIterator();
      MAX_CORE_CLOCK_FREQ     = (Long)it.next();
      MAX_BUS_CLOCK_FREQ      = (Long)it.next();
      MAX_FLASH_CLOCK_FREQ    = (Long)it.next();
      MAX_FLEXBUS_CLOCK_FREQ  = (Long)it.next();

      PLL_IN_MIN   = (Long)it.next();
      PLL_IN_MAX   = (Long)it.next();  
      PLL_OUT_MIN  = (Long)it.next();   
      PLL_OUT_MAX  = (Long)it.next();   
      PRDIV_MIN    = (Long)it.next();   
      PRDIV_MAX    = (Long)it.next();   
      VDIV_MIN     = (Long)it.next();    
      VDIV_MAX     = (Long)it.next();    
      PLL_POST_DIV = (Long)it.next();  

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

      LongVariable pllInputFrequencyNode = getLongVariable("pllInputFrequency");
      pllInputFrequencyNode.setMin(PLL_IN_MIN);
      pllInputFrequencyNode.setMax(PLL_IN_MAX);
      
      LongVariable pllTargetFrequencyNode = getLongVariable("pllTargetFrequency");
      pllTargetFrequencyNode.setMin(PLL_OUT_MIN);
      pllTargetFrequencyNode.setMax(PLL_OUT_MAX);

      LongVariable mcg_c5_prdiv0Node = getLongVariable("mcg_c5_prdiv0");
      mcg_c5_prdiv0Node.setOffset(-PRDIV_MIN);
      mcg_c5_prdiv0Node.setMin(PRDIV_MIN);
      mcg_c5_prdiv0Node.setMax(PRDIV_MAX);

      LongVariable mcg_c6_vdiv0Node = getLongVariable("mcg_c6_vdiv0");
      mcg_c6_vdiv0Node.setOffset(-VDIV_MIN);
      mcg_c6_vdiv0Node.setMin(VDIV_MIN);
      mcg_c6_vdiv0Node.setMax(VDIV_MAX);
      
      for (String name:names) {
         Variable var = safeGetVariable(name);
         if (var != null) {
            var.addListener(fPeripheral);
         }
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

      Variable     mcg_c9_pll_cmeNode              =  safeGetVariable("mcg_c9_pll_cme");
      if (mcg_c9_pll_cmeNode != null) {
         getVariable("mcg_c9_pll_locre").enable(mcg_c9_pll_cmeNode.getValueAsBoolean());
      }
//      Variable     mcg_c6_lolie0Node               =  getVariable("mcg_c6_lolie0");
//      Variable     mcg_c8_lolreNode                =  getVariable("mcg_c8_lolre");
      Variable     mcg_c11_pllcsNode                =  safeGetVariable("mcg_c11_pllcs");
      Variable     usb1pfdclk_ClockNode             =  safeGetVariable("usb1pfdclk_Clock");
      
      mcg_c2_locre0Node.enable(mcg_c6_cme0Node.getValueAsBoolean());
      mcg_c8_locre1Node.enable(mcg_c8_cme1Node.getValueAsBoolean());
   
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
      Variable     system_irc48m_clockNode          =  safeGetVariable("system_irc48m_clock");
//    Variable     usb_clkin_clockNode              =  getLongVariable("system_usb_clkin_clock");
      Variable     system_low_power_clockNode       =  getVariable("system_low_power_clock");

      // RTC
      //=================================
      Variable     rtcclk_clockNode                 =  getVariable("/RTC/rtcclk_clock");
      Variable     rtc_cr_clkoNode                  =  getVariable("/RTC/rtc_cr_clko");
      Variable     sim_sopt1_osc32kselNode          =  getVariable("sim_sopt1_osc32ksel");
      Variable     system_erclk32k_clockNode        =  getVariable("system_erclk32k_clock");
      Variable     sim_sopt2_rtcclkoutselNode       =  getVariable("sim_sopt2_rtcclkoutsel");
      Variable     system_rtc_clkoutNode            =  getVariable("system_rtc_clkout");

      // Clocks and information from main oscillator
      //=================================
      Variable     oscclk_clockNode                 =  getVariable("/OSC0/oscclk_clock");
      Variable     osc32kclk_clockNode              =  getVariable("/OSC0/osc32kclk_clock");
      Variable     osc_cr_erclkenNode               =  getVariable("/OSC0/osc_cr_erclken");
      Variable     osc0_rangeNode                   =  getVariable(OscValidate.OSC_RANGE_KEY);

      //===================
      Variable     mcg_c2_range0Node                =  getVariable("/OSC0/range");

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
      Variable     system_mcgout_clock_sourceNode   =  getVariable("system_mcgout_clock_source");
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
      
      // Determine MCGIRCLK (not gated/undivided and gated)
      //========================================
      Variable system_mcgir_ungated_clock = new LongVariable("system_mcgir_ungated", null);
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
      
      // RTC Clocks
      //==============================
      Long    rtcclk_clockValue  = rtcclk_clockNode.getValueAsLong();
      Message rtcclk_clockStatus = rtcclk_clockNode.getStatus();
      String  rtcclk_clockOrigin = rtcclk_clockNode.getOrigin();

      Long    rtcclk_gated_clockValue;
      Message rtcclk_gated_clockStatus;
      String  rtcclk_gated_clockOrigin;
      if (rtc_cr_clkoNode.getValueAsBoolean()) {
         rtcclk_gated_clockValue  = rtcclk_clockValue;
         rtcclk_gated_clockStatus = rtcclk_clockStatus;
         rtcclk_gated_clockOrigin = rtcclk_clockOrigin;
      }
      else {
         rtcclk_gated_clockValue  = 0L;
         rtcclk_gated_clockStatus = new Message("Disabled by rtc_cr_clko", Severity.WARNING);
         rtcclk_gated_clockOrigin = rtcclk_clockOrigin;
      }
      
      // Determine ERCLK32K
      //==================================
      switch ((int)sim_sopt1_osc32kselNode.getValueAsLong()) {
      case 0: // System oscillator (OSC32KCLK)
         system_erclk32k_clockNode.setValue(osc32kclk_clockNode.getValueAsLong());
         system_erclk32k_clockNode.setStatus(osc32kclk_clockNode.getStatus());
         system_erclk32k_clockNode.setOrigin(osc32kclk_clockNode.getOrigin());
         break;
      default:
         sim_sopt1_osc32kselNode.setValue(2);
      case 2: // RTC 32.768kHz oscillator
         system_erclk32k_clockNode.setValue(rtcclk_gated_clockValue);
         system_erclk32k_clockNode.setStatus(rtcclk_gated_clockStatus);
         system_erclk32k_clockNode.setOrigin(rtcclk_gated_clockOrigin);
         break;
      case 3: // LPO 1 kHz
         system_erclk32k_clockNode.setValue(system_low_power_clockNode.getValueAsLong());
         system_erclk32k_clockNode.setStatus((Message)null);
         system_erclk32k_clockNode.setOrigin("Low Power Oscillator");
         break;
      }

      // RTC Clock out pin select 
      //============================
      switch ((int)sim_sopt2_rtcclkoutselNode.getValueAsLong()) {
      default:
         sim_sopt2_rtcclkoutselNode.setValue(0);
      case 0: // RTC seconds clock = 1Hz
         system_rtc_clkoutNode.setValue(Math.round(rtcclk_clockValue/32768.0));
         system_rtc_clkoutNode.setStatus(rtcclk_clockStatus);
         system_rtc_clkoutNode.setOrigin("RTC 1Hz output");
         break;
      case 1: // RTC 32.768kHz oscillator
         system_rtc_clkoutNode.setValue(rtcclk_gated_clockValue);
         system_rtc_clkoutNode.setStatus(rtcclk_gated_clockStatus);
         system_rtc_clkoutNode.setOrigin("RTC 32kHz output");
         break;
      }
      
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
         mcg_erc_clockNode.setValue(oscclk_clockNode.getValueAsLong());
         mcg_erc_clockNode.setStatus(oscclk_clockNode.getStatus());
         mcg_erc_clockNode.setOrigin(oscclk_clockNode.getOrigin());
         break;
      case 1: // ERC = OSC32KCLK
         mcg_erc_clockNode.setValue(rtcclk_clockValue);
         mcg_erc_clockNode.setStatus(rtcclk_clockStatus);
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
      FllConfigure fllCheck = new FllConfigure(
            osc_cr_erclkenNode,
            osc0_rangeNode,
            mcg_c2_range0Node,
            mcg_c1_irefs,
            mcg_erc_clockNode,
            slow_irc_clockNode.getValueAsLong(),
            mcg_c7_oscselNode.getValueAsLong(), 
            mcg_c4_dmx32Node.getValueAsBoolean(),
            fllInputFrequencyNode,
            fllTargetFrequencyNode);

      mcg_c1_frdivNode.setValue(fllCheck.mcg_c1_frdiv);
      mcg_c4_drst_drsNode.setValue(fllCheck.mcg_c4_drst_drs);
      
      //========================================
      // Find PLL divider
      PllConfigure pllConfigure = new PllConfigure(
            PLL_OUT_MIN, 
            PLL_OUT_MAX, 
            PLL_IN_MIN, 
            PLL_IN_MAX, 
            PRDIV_MIN, 
            PRDIV_MAX, 
            VDIV_MIN, 
            VDIV_MAX, 
            PLL_POST_DIV);
      
      pllConfigure.validate(mcg_erc_clockNode, pllInputFrequencyNode, pllTargetFrequencyNode, mcg_c5_prdiv0Node, mcg_c6_vdiv0Node);
      
      Variable pllcsNode = pllTargetFrequencyNode;
      boolean pllIsInternal = true;
      if (mcg_c11_pllcsNode!= null) {
         long irClockFreq = oscclk_clockNode.getValueAsLong();
         String failedPreCondition = null;
         if (!osc_cr_erclkenNode.getValueAsBoolean()) {
            failedPreCondition = "Disabled: to use PFDCLK, OSCCLK clock must be enabled by osc_cr_erclkenNode";
         }
         else if (!mcg_c1_irclkenNode.getValueAsBoolean()) {
            failedPreCondition = "Disabled: to use PFDCLK, IRC clock must be enabled by mcg_c1_irclken";
         }
         else if ((irClockFreq!=12000000)&&(irClockFreq!=16000000)&&(irClockFreq!=24000000)) {
            failedPreCondition = "Disabled: to use PFDCLK, OSCCLK must be in [12Mhz, 16MHz, 24MHz]";
         }
         if (failedPreCondition==null) {
            usb1pfdclk_ClockNode.enable(true);
            usb1pfdclk_ClockNode.setOrigin("USB1 PFDCLK"); 
            usb1pfdclk_ClockNode.setStatus((Message)null);
         }
         else {
            usb1pfdclk_ClockNode.enable(false);
            usb1pfdclk_ClockNode.setOrigin("USB1 PFDCLK (disabled)"); 
            usb1pfdclk_ClockNode.setStatus(new Message(failedPreCondition, Severity.WARNING));
         }
         if (mcg_c11_pllcsNode.getValueAsBoolean()) {
            pllcsNode = usb1pfdclk_ClockNode;
            pllIsInternal = false;
         }
         else {
            pllcsNode = pllTargetFrequencyNode;
         }
      }
      
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
         pll_enabledNode.setValue(pllIsInternal);
         fll_enabledNode.setValue(false);
         break;
      case ClockMode_PEE:
         mcg_c1_clks             = 0;
         mcg_c6_plls             = 1;
         mcg_c2_lp               = 0;
         system_mcgout_clock_sourceNode.setValue("PLL output");
         system_mcgout_clockNode.setValue(pllcsNode.getValueAsLong());
         system_mcgout_clockNode.setOrigin(pllcsNode.getOrigin());
         system_mcgout_clockNode.setStatus(pllcsNode.getStatus());
         pll_enabledNode.setValue(pllIsInternal);
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
