package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
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
      
      Variable system_core_clockNode      =  fPeripheral.getVariable("system_core_clock");
      Variable system_bus_clockNode       =  fPeripheral.getVariable("system_bus_clock");
      Variable system_flash_clockNode     =  fPeripheral.getVariable("system_flash_clock");
      Variable system_flexbus_clockNode   =  fPeripheral.getVariable("system_flexbus_clock");
      
      system_core_clockNode.setMin(0);
      system_core_clockNode.setMax(MAX_CORE_CLOCK_FREQ);
      system_bus_clockNode.setMin(0);
      system_bus_clockNode.setMax(MAX_BUS_CLOCK_FREQ);
      system_flash_clockNode.setMin(0);
      system_flash_clockNode.setMax(MAX_FLASH_CLOCK_FREQ);
      system_flexbus_clockNode.setMin(0);
      system_flexbus_clockNode.setMax(MAX_FLEXBUS_CLOCK_FREQ);
   }
   
   public ClockValidator(PeripheralWithState peripheral, long maxCoreClockfrequency, long maxBusClockFrequency, long maxFlashClockFrequency) {
      this(peripheral, maxCoreClockfrequency, maxBusClockFrequency, maxFlashClockFrequency, 50000000);
   }
   
   @Override
   protected void validate() {
      
      // Internal
      //=================================
      Variable irc48m_clockNode           =  fPeripheral.getVariable("irc48m_clock");
//    Variable usb_clkin_clockNode            =  fPeripheral.getVariable("system_usb_clkin_clock");
//      Variable low_power_clockNode          =  fPeripheral.getVariable("system_low_power_clock");
      Variable slow_irc_clockNode         =  fPeripheral.getVariable("system_slow_irc_clock");
      Variable fast_irc_clockNode         =  fPeripheral.getVariable("system_fast_irc_clock");
      Variable mcg_sc_fcrdivNode          =  fPeripheral.safeGetVariable("mcg_sc_fcrdiv");
//      Variable mcg_c1_irclkenNode         =  fPeripheral.getVariable("mcg_c1_irclken");
//      Variable mcg_c1_irefstenNode        =  fPeripheral.getVariable("mcg_c1_irefsten");
      Variable mcg_c2_ircsNode            =  fPeripheral.getVariable("mcg_c2_ircs");
      Variable system_mcgir_clockNode     =  fPeripheral.getVariable("system_mcgir_clock");

      // OSC
      //=================================
//      Variable osc_cr_erclkenNode         =  fPeripheral.getVariable("osc_cr_erclken");
      Variable oscclk_clockNode           =  fPeripheral.getVariable("oscclk_clock");
      Variable mcg_c2_erefs0Node          =  fPeripheral.getVariable("mcg_c2_erefs0");
//      Variable mcg_c2_hgo0Node            =  fPeripheral.getVariable("mcg_c2_hgo0");
      Variable mcg_c2_rangeNode               =  fPeripheral.getVariable("mcg_c2_range0");
//      Variable osc_cr_scpNode             =  fPeripheral.getVariable("osc_cr_scp");
//      Variable osc_cr_erefstenNode        =  fPeripheral.getVariable("osc_cr_erefsten");
//      Variable osc_div_erpsNode           =  fPeripheral.getVariable("osc_div_erps");

      // ERCLK32K
      //=================================
//      Variable rtc_cr_osceNode            =  fPeripheral.getVariable("rtc_cr_osce");
      Variable rtcclk_clockNode           =  fPeripheral.getVariable("rtcclk_clock");
//      Variable rtc_cr_scpNode             =  fPeripheral.getVariable("rtc_cr_scp");
//      Variable rtc_cr_clkoNode            =  fPeripheral.getVariable("rtc_cr_clko");
//      Variable sim_sopt1_osc32kselNode    =  fPeripheral.getVariable("sim_sopt1_osc32ksel");
      
      //=================================
      Variable clock_modeNode             =  fPeripheral.getVariable("clock_mode");
      Variable mcgOutputClockNode         =  fPeripheral.getVariable("mcgOutputClock");
      Variable mcg_c7_oscselNode          =  fPeripheral.safeGetVariable("mcg_c7_oscsel");
      Variable system_erc_clockNode       =  fPeripheral.getVariable("system_erc_clock");

      // FLL
      //=================================
      Variable fll_enabled_node     =  fPeripheral.getVariable("fll_enabled");

      // PLL
      //=================================
      Variable pll_enabled_node     =  fPeripheral.getVariable("pll_enabled");

      
      
      Variable pllTargetFrequencyNode     =  fPeripheral.getVariable("pllTargetFrequency");
      Variable fllTargetFrequencyNode     =  fPeripheral.getVariable("fllTargetFrequency");
      Variable system_core_clockNode      =  fPeripheral.getVariable("system_core_clock");
      Variable system_bus_clockNode       =  fPeripheral.getVariable("system_bus_clock");
      Variable system_flexbus_clockNode   =  fPeripheral.safeGetVariable("system_flexbus_clock");
      Variable system_flash_clockNode     =  fPeripheral.getVariable("system_flash_clock");
      
      
      
      
      Variable mcg_c1_clksNode            =  fPeripheral.getVariable("mcg_c1_clks");
      
      Variable mcg_c1_irefsNode           =  fPeripheral.getVariable("mcg_c1_irefs");
      
      Variable mcg_c2_lpNode              =  fPeripheral.getVariable("mcg_c2_lp");
      Variable mcg_c6_pllsNode            =  fPeripheral.getVariable("mcg_c6_plls");
      Variable system_mcgout_clockNode    =  fPeripheral.getVariable("system_mcgout_clock");
                                                        
      Variable sim_clkdiv1_outdiv1Node    =  fPeripheral.getVariable("sim_clkdiv1_outdiv1");
      Variable sim_clkdiv1_outdiv2Node    =  fPeripheral.getVariable("sim_clkdiv1_outdiv2");
      Variable sim_clkdiv1_outdiv3Node    =  fPeripheral.safeGetVariable("sim_clkdiv1_outdiv3");
      Variable sim_clkdiv1_outdiv4Node    =  fPeripheral.getVariable("sim_clkdiv1_outdiv4");

      // Determine MCGIRCLK
      //==================================
      long system_mcgir_clock;
      if (mcg_c2_ircsNode.getValueAsBoolean()) {
         if ( mcg_sc_fcrdivNode != null) {
            // Variable divisor
            long mcg_sc_fcrdiv = mcg_sc_fcrdivNode.getValueAsLong();
            system_mcgir_clock = fast_irc_clockNode.getValueAsLong() / (1<<mcg_sc_fcrdiv);
         }
         else {
            // Fixed divisor of 2
            system_mcgir_clock = fast_irc_clockNode.getValueAsLong() / 2;
         }
      }
      else {
         system_mcgir_clock = slow_irc_clockNode.getValueAsLong();
      }
      system_mcgir_clockNode.setValue(system_mcgir_clock);
      
      // Determine MCG FLL external reference clock
      //==================================

      // Default if no MCG_C7_OSCSEL register field
      long system_erc_clock = oscclk_clockNode.getValueAsLong();
      if (mcg_c7_oscselNode != null) {
         switch ((int)mcg_c7_oscselNode.getValueAsLong()) {
         default:
         case 0: // ERC = OSCCLK
            system_erc_clock = oscclk_clockNode.getValueAsLong();
            break;
         case 1: // ERC = OSC32KCLK
            system_erc_clock = rtcclk_clockNode.getValueAsLong();
            break;
         case 2: // ERC = IRC48M
            system_erc_clock = irc48m_clockNode.getValueAsLong();
            break;
         }
      }

      System.err.println("ClockValidate.validate() system_mcgir_clock = " + system_mcgir_clock+ ", system_erc_clock = " + system_erc_clock);

      //=========================================
      // Check input clock/oscillator ranges
      //   - Determine mcg_c2_range
      //   - Affects FLL prescale
      //
      FllDivider check = new FllDivider(system_erc_clock, mcg_c2_erefs0Node.getValueAsLong(), oscclk_clockNode.getValueAsLong());
      oscclk_clockNode.setMessage(check.oscclk_clockMessage);
      mcg_c2_rangeNode.setValue(check.mcg_c2_range);

      ClockMode primaryClockMode = ClockMode.valueOf(clock_modeNode.getValue());

      int     mcg_c1_clks                = 0;
      int     mcg_c1_irefs               = 1;
      int     mcg_c6_plls                = 0;
      int     mcg_c2_lp                  = 0;
      
      long    fllTargetFrequency         = fllTargetFrequencyNode.getValueAsLong();
      long    pllTargetFrequency         = pllTargetFrequencyNode.getValueAsLong();
      long    system_mcgout_clock        = 0;
      Message primaryClockModeMessage    = null;


      fll_enabled_node.setLocked(true);
      pll_enabled_node.setLocked(true);

      String mcgOutputClock = "Unknown";
      switch (primaryClockMode) {
      case ClockMode_None:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = fllTargetFrequency;
         mcgOutputClock = "FLL";
         fll_enabled_node.setValue("0");
         pll_enabled_node.setValue("0");
         primaryClockModeMessage = new Message("No clock settings are applied", Severity.WARNING);
         break;
      case ClockMode_FEI:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = fllTargetFrequency;
         mcgOutputClock = "FLL";
         fll_enabled_node.setValue("1");
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_FEE:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = fllTargetFrequency;
         mcgOutputClock = "FLL";
         fll_enabled_node.setValue("1");
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_FBI:
         mcg_c1_clks         = 1;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = system_mcgir_clock;
         mcgOutputClock = "MCGIRCLK";
         fll_enabled_node.setValue("1");
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_FBE:
         mcg_c1_clks         = 2;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = system_erc_clock;
         mcgOutputClock = "MCGERCLK";
         fll_enabled_node.setValue("1");
         pll_enabled_node.setLocked(false); // optional PLL
         break;
      case ClockMode_BLPI:
         mcg_c1_clks         = 1;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 1;
         system_mcgout_clock = system_mcgir_clock;
         mcgOutputClock = "MCGIRCLK";
         fll_enabled_node.setValue("0");
         pll_enabled_node.setValue("0");
         break;
      case ClockMode_BLPE:
         mcg_c1_clks         = 2;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 1;
         system_mcgout_clock = system_erc_clock;
         mcgOutputClock = "MCGERCLK";
         fll_enabled_node.setValue("0");
         pll_enabled_node.setValue("0");
         break;
      case ClockMode_PBE:
         mcg_c1_clks         = 2;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 1;
         mcg_c2_lp           = 0;
         system_mcgout_clock = system_erc_clock;
         mcgOutputClock = "MCGERCLK";
         fll_enabled_node.setValue("0");
         pll_enabled_node.setValue("1");
         break;
      case ClockMode_PEE:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 1;
         mcg_c2_lp           = 0;
         system_mcgout_clock = pllTargetFrequency;
         mcgOutputClock = "PLL";
         fll_enabled_node.setValue("0");
         pll_enabled_node.setValue("1");
         break;
      }
      mcgOutputClockNode.setValue(mcgOutputClock);
      
      // Core Clock
      //===========================================
      long sim_clkdiv1_outdiv1 = sim_clkdiv1_outdiv1Node.getValueAsLong()+1;
      long system_core_clock   = system_mcgout_clock / sim_clkdiv1_outdiv1;
      Message system_core_clockMessage = 
         new Message(String.format("Must be <= %2.1f MHz.", MAX_CORE_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
      if (system_core_clock > MAX_CORE_CLOCK_FREQ) {
         system_core_clockMessage = new Message(String.format("Frequency is too high. (Req. <= %2.2f MHz)", MAX_CORE_CLOCK_FREQ/1000000.0));
      }
      system_core_clockNode.setValue(system_core_clock);
      system_core_clockNode.setMessage(system_core_clockMessage);

      // Bus Clock
      //===========================================
      long sim_clkdiv1_outdiv2 = sim_clkdiv1_outdiv2Node.getValueAsLong()+1;
      long system_bus_clock = system_mcgout_clock / sim_clkdiv1_outdiv2;
      Message system_bus_clockMessage = 
            new Message(String.format("Must be <= %2.1f MHz.", MAX_BUS_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
      if (system_bus_clock > MAX_BUS_CLOCK_FREQ) {
         system_bus_clockMessage = 
               new Message(String.format("Frequency is too high. (Req. <= %2.2f MHz)", MAX_BUS_CLOCK_FREQ/1000000.0));
      }
      else if (system_bus_clock>system_core_clock) {
         system_bus_clockMessage = new Message("Clock is too high. (Req. <= Core clock)");
      }
      else if ((sim_clkdiv1_outdiv2 % sim_clkdiv1_outdiv1) != 0) {
         system_bus_clockMessage = new Message("Frequency must be an integer divisor of Core clock frequency");
      }
      system_bus_clockNode.setValue(system_bus_clock);
      system_bus_clockNode.setMessage(system_bus_clockMessage);

      long system_flexbus_clock = 0;
      if ((sim_clkdiv1_outdiv3Node != null) && system_flexbus_clockNode != null) {
         // Flexbus Clock
         //===========================================
         long sim_clkdiv1_outdiv3 = sim_clkdiv1_outdiv3Node.getValueAsLong()+1;
         system_flexbus_clock = system_mcgout_clock / sim_clkdiv1_outdiv3;
         Message system_flexbus_clockMessage = 
               new Message(String.format("Must <= %2.1f MHz.", MAX_FLEXBUS_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
         if (system_flexbus_clock > MAX_FLEXBUS_CLOCK_FREQ) {
            system_flexbus_clockMessage = 
                  new Message(String.format("Frequency is too high. (Req. <= %2.2f MHz)", MAX_FLEXBUS_CLOCK_FREQ/1000000.0));
         }
         else if ((sim_clkdiv1_outdiv3 % sim_clkdiv1_outdiv1) != 0) {
            system_flexbus_clockMessage = new Message("Frequency must be an integer divisor of Core clock frequency");
         }
         else if (system_flexbus_clock>system_bus_clock) {
            system_flexbus_clockMessage = new Message("Clock is too high. (Req. <= Bus clock)");
         }
         system_flexbus_clockNode.setValue(system_flexbus_clock);
         system_flexbus_clockNode.setMessage(system_flexbus_clockMessage);
      }
      
      // Flash Clock
      //===========================================
      long sim_clkdiv1_outdiv4 = sim_clkdiv1_outdiv4Node.getValueAsLong()+1;
      long system_flash_clock = system_mcgout_clock / sim_clkdiv1_outdiv4;
      Message system_flash_clockMessage = 
            new Message(String.format("Must be <= %2.1f MHz.", MAX_FLASH_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
      if (system_flash_clock > MAX_FLASH_CLOCK_FREQ) {
         system_flash_clockMessage = 
               new Message(String.format("Clock frequency is too high. (Req. clock <= %2.2f MHz)", MAX_FLASH_CLOCK_FREQ/1000000.0));
      }
      else if (system_flash_clock>system_bus_clock) {
         system_flash_clockMessage = new Message("Clock is too high. (Req. <= Bus clock)");
      }
      else if ((sim_clkdiv1_outdiv4 % sim_clkdiv1_outdiv1) != 0) {
         system_flash_clockMessage = new Message("Frequency must be an integer divisor of Core clock frequency");
      }
      system_flash_clockNode.setValue(system_flash_clock);
      system_flash_clockNode.setMessage(system_flash_clockMessage);
      
      clock_modeNode.setMessage(primaryClockModeMessage);

      system_erc_clockNode.setValue(system_erc_clock);
      system_mcgir_clockNode.setValue(system_mcgir_clock);
      system_mcgout_clockNode.setValue(system_mcgout_clock);
      mcg_c1_clksNode.setValue(mcg_c1_clks);
      mcg_c1_irefsNode.setValue(mcg_c1_irefs);
      mcg_c6_pllsNode.setValue(mcg_c6_plls);
      mcg_c2_lpNode.setValue(mcg_c2_lp);
   }
}
