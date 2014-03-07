package net.sourceforge.usbdm.annotationEditor.validators;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class ClockValidate_MKxxM12 extends MyValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_BUS_CLOCK_FREQ;
   private final long MAX_FLEXBUS_CLOCK_FREQ;
   private final long MAX_FLASH_CLOCK_FREQ;
   
   public enum ClockModes {NONEClock, FEIClock, FEEClock, FBIClock, BLPIClock, FBEClock, BLPEClock, PBEClock,  PEEClock};
   
   public ClockValidate_MKxxM12() {
      MAX_CORE_CLOCK_FREQ    = 150000000;
      MAX_BUS_CLOCK_FREQ     =  75000000;
      MAX_FLEXBUS_CLOCK_FREQ =  50000000;
      MAX_FLASH_CLOCK_FREQ   =  25000000;
   }
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode       =  getNumericModelNode("clock_mode");
      NumericOptionModelNode oscclk0_clockNode          =  getNumericModelNode("oscclk0_clock");
      NumericOptionModelNode rtcclk_clockNode           =  getNumericModelNode("rtcclk_clock");
      
      NumericOptionModelNode system_erc_clockNode       =  getNumericModelNode("system_erc_clock");
      NumericOptionModelNode slowIRCNode                =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode fastIRCNode                =  getNumericModelNode("system_fast_irc_clock");
      NumericOptionModelNode mcg_c1_clksNode            =  getNumericModelNode("mcg_c1_clks");
      NumericOptionModelNode mcg_c1_irefsNode           =  getNumericModelNode("mcg_c1_irefs");
      BinaryOptionModelNode  mcg_c2_ircsNode            =  getBinaryModelNode("mcg_c2_ircs");
      NumericOptionModelNode mcg_c2_lpNode              =  getNumericModelNode("mcg_c2_lp");
      NumericOptionModelNode mcg_c6_pllsNode            =  getNumericModelNode("mcg_c6_plls");
      NumericOptionModelNode mcg_sc_fcrdivNode          =  safeGetNumericModelNode("mcg_sc_fcrdiv");
      BinaryOptionModelNode  mcg_c7_oscselNode          =  safeGetBinaryModelNode("mcg_c7_oscsel");
      BinaryOptionModelNode  mcg_c11_pllcsNode          =  safeGetBinaryModelNode("mcg_c11_pllcs");
      NumericOptionModelNode system_mcgout_clockNode    =  getNumericModelNode("system_mcgout_clock");
      NumericOptionModelNode system_mcgir_clockNode     =  getNumericModelNode("system_mcgir_clock");
      NumericOptionModelNode fllTargetFrequencyNode     =  getNumericModelNode("fllTargetFrequency");
      NumericOptionModelNode pll0TargetFrequencyNode    =  getNumericModelNode("pll0TargetFrequency");
      NumericOptionModelNode pll1TargetFrequencyNode    =  getNumericModelNode("pll1TargetFrequency");
                                                        
      NumericOptionModelNode sim_clkdiv1_outdiv1Node    =  getNumericModelNode("sim_clkdiv1_outdiv1");
      NumericOptionModelNode sim_clkdiv1_outdiv2Node    =  getNumericModelNode("sim_clkdiv1_outdiv2");
      NumericOptionModelNode sim_clkdiv1_outdiv3Node    =  safeGetNumericModelNode("sim_clkdiv1_outdiv3");
      NumericOptionModelNode sim_clkdiv1_outdiv4Node    =  getNumericModelNode("sim_clkdiv1_outdiv4");
      NumericOptionModelNode system_core_clockNode      =  getNumericModelNode("system_core_clock");
      NumericOptionModelNode system_bus_clockNode       =  getNumericModelNode("system_bus_clock");
      NumericOptionModelNode system_flexbus_clockNode   =  safeGetNumericModelNode("system_flexbus_clock");
      NumericOptionModelNode system_flash_clockNode     =  getNumericModelNode("system_flash_clock");

      
      long system_mcgir_clock;
      if (mcg_c2_ircsNode.safeGetValue()) {
         if ( mcg_sc_fcrdivNode != null) {
            // Variable divisor
            long mcg_sc_fcrdiv = mcg_sc_fcrdivNode.getValueAsLong();
            system_mcgir_clock = fastIRCNode.getValueAsLong() / (1<<mcg_sc_fcrdiv);
         }
         else {
            // Fixed divisor of 2
            system_mcgir_clock = fastIRCNode.getValueAsLong() / 2;
         }
      }
      else {
         system_mcgir_clock = slowIRCNode.getValueAsLong();
      }
      System.err.println("ClockValidate.validate() system_mcgir_clock = " + system_mcgir_clock);

      long system_erc_clock;
      if ((mcg_c7_oscselNode != null) && mcg_c7_oscselNode.safeGetValue()) {
         // ERC = OSC32KCLK
         system_erc_clock = rtcclk_clockNode.getValueAsLong();
      }
      else {
         // ERC = OSCCLK
         system_erc_clock = oscclk0_clockNode.getValueAsLong();
      }
      System.err.println("ClockValidate.validate() system_erc_clock = " + system_erc_clock);

      long clk = primaryClockModeNode.getValueAsLong();
      if (clk > ClockModes.values().length) {
         throw new Exception("Illegal Clock mode");
      }
      ClockModes primaryClockMode = ClockModes.values()[(int)primaryClockModeNode.getValueAsLong()];
      
      int mcg_c1_clks     = 0;
      int mcg_c1_irefs    = 1;
      int mcg_c6_plls     = 0;
      int mcg_c2_lp       = 0;
      
      long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();
      long pllTargetFrequency;
      if (mcg_c11_pllcsNode.safeGetValue()) {
         pllTargetFrequency = pll1TargetFrequencyNode.getValueAsLong();
      }
      else {
         pllTargetFrequency = pll0TargetFrequencyNode.getValueAsLong();
      }
      long system_mcgout_clock = 0;
      String primaryClockModeMessage = null;
      
      switch (primaryClockMode) {
      case NONEClock:
         mcg_c1_clks          = 0;
         mcg_c1_irefs         = 1;
         mcg_c6_plls          = 0;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = fllTargetFrequency;
         primaryClockModeMessage = "No clock settings are applied";
         break;
      case FEIClock:
         mcg_c1_clks     = 0;
         mcg_c1_irefs    = 1;
         mcg_c6_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = fllTargetFrequency;
         break;
      case FEEClock:
         mcg_c1_clks     = 0;
         mcg_c1_irefs    = 0;
         mcg_c6_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = fllTargetFrequency;
         break;
      case FBIClock:
         mcg_c1_clks     = 1;
         mcg_c1_irefs    = 1;
         mcg_c6_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = system_mcgir_clock;
         break;
      case BLPIClock:
         mcg_c1_clks     = 1;
         mcg_c1_irefs    = 1;
         mcg_c6_plls     = 0;
         mcg_c2_lp       = 1;
         system_mcgout_clock = system_mcgir_clock;
         break;
      case FBEClock:
         mcg_c1_clks     = 2;
         mcg_c1_irefs    = 0;
         mcg_c6_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = system_erc_clock;
         break;
      case BLPEClock:
         mcg_c1_clks     = 2;
         mcg_c1_irefs    = 0;
         mcg_c6_plls     = 0;
         mcg_c2_lp       = 1;
         system_mcgout_clock = system_erc_clock;
         break;
      case PBEClock:
         mcg_c1_clks     = 2;
         mcg_c1_irefs    = 0;
         mcg_c6_plls     = 1;
         mcg_c2_lp       = 0;
         system_mcgout_clock = system_erc_clock;
         break;
      case PEEClock:
         mcg_c1_clks     = 0;
         mcg_c1_irefs    = 0;
         mcg_c6_plls     = 1;
         mcg_c2_lp       = 0;
         system_mcgout_clock = pllTargetFrequency;
         break;
      }
      // Core Clock
      //===========================================
      long sim_clkdiv1_outdiv1 = sim_clkdiv1_outdiv1Node.getValueAsLong();
      long system_core_clock   = system_mcgout_clock / sim_clkdiv1_outdiv1;
      String system_core_clockMessage = null;
      if (system_core_clock > MAX_CORE_CLOCK_FREQ) {
         system_core_clockMessage = String.format("Clock frequency is too high. (Req. clock <= %d MHz)", MAX_CORE_CLOCK_FREQ/1000000);
      }
      setValid(viewer, system_core_clockNode, system_core_clockMessage);

      // Bus Clock
      //===========================================
      long sim_clkdiv1_outdiv2 = sim_clkdiv1_outdiv2Node.getValueAsLong();
      long system_bus_clock = system_mcgout_clock / sim_clkdiv1_outdiv2;
      String system_bus_clockMessage = null;
      if (system_bus_clock > MAX_BUS_CLOCK_FREQ) {
         system_bus_clockMessage = String.format("Clock frequency is too high. (Req. clock <= %d MHz)", MAX_BUS_CLOCK_FREQ/1000000);
      }
      else if (system_bus_clock>system_core_clock) {
         system_bus_clockMessage = "Clock is too high. (Req. clock <= Core clock)";
      }
      else if ((system_core_clock/system_bus_clock)*system_bus_clock != system_core_clock) {
         system_bus_clockMessage = "Clock frequency must be an integer divisor of Core clock frequency";
      }
      setValid(viewer, system_bus_clockNode, system_bus_clockMessage);
      
      long system_flexbus_clock = 0;
      if (sim_clkdiv1_outdiv3Node != null) {
         // Flexbus Clock
         //===========================================
         long sim_clkdiv1_outdiv3 = sim_clkdiv1_outdiv3Node.getValueAsLong();
         system_flexbus_clock = system_mcgout_clock / sim_clkdiv1_outdiv3;
         String system_flexbus_clockMessage = null;
         if (system_flexbus_clock > MAX_FLEXBUS_CLOCK_FREQ) {
            system_flexbus_clockMessage = String.format("Clock frequency is too high. (Req. clock <= %d MHz)", MAX_FLEXBUS_CLOCK_FREQ/1000000);
         }
         else if (system_flexbus_clock>system_bus_clock) {
            system_flexbus_clockMessage = "Clock is too high. (Req. clock <= Bus clock)";
         }
         setValid(viewer, system_flexbus_clockNode, system_flexbus_clockMessage);
      }
      
      // Flash Clock
      //===========================================
      long sim_clkdiv1_outdiv4 = sim_clkdiv1_outdiv4Node.getValueAsLong();
      long system_flash_clock = system_mcgout_clock / sim_clkdiv1_outdiv4;
      String system_flash_clockMessage = null;
      if (system_flash_clock > MAX_FLASH_CLOCK_FREQ) {
         system_flash_clockMessage = String.format("Clock frequency is too high. (Req. clock <= %d MHz)", MAX_FLASH_CLOCK_FREQ/1000000);
      }
      else if (system_flash_clock>system_bus_clock) {
         system_flash_clockMessage = "Clock is too high. (Req. clock <= Bus clock)";
      }
      else if ((system_core_clock/system_flash_clock)*system_flash_clock != system_core_clock) {
         system_flash_clockMessage = "Clock frequency must be an integer divisor of Core clock frequency";
      }
      setValid(viewer, system_flash_clockNode, system_flash_clockMessage);
      
      setValid(viewer, primaryClockModeNode, primaryClockModeMessage);

      update(viewer, system_core_clockNode, system_core_clock);
      update(viewer, system_bus_clockNode, system_bus_clock);
      if (system_flexbus_clockNode != null) {
         update(viewer, system_flexbus_clockNode, system_flexbus_clock);
      }
      update(viewer, system_flash_clockNode, system_flash_clock);
      update(viewer, system_erc_clockNode, system_erc_clock);
      update(viewer, system_mcgir_clockNode, system_mcgir_clock);
      update(viewer, system_mcgout_clockNode, system_mcgout_clock);
      update(viewer, mcg_c1_clksNode, mcg_c1_clks);
      update(viewer, mcg_c1_irefsNode, mcg_c1_irefs);
      update(viewer, mcg_c6_pllsNode, mcg_c6_plls);
      update(viewer, mcg_c2_lpNode, mcg_c2_lp);
   }

}
