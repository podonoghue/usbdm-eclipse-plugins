package net.sourceforge.usbdm.annotationEditor.validators;

import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;
import net.sourceforge.usbdm.annotationEditor.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.Message;
import net.sourceforge.usbdm.annotationEditor.MyValidator;
import net.sourceforge.usbdm.annotationEditor.NumericOptionModelNode;

public class ClockValidate_KLxx extends MyValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_FLASH_BUS_CLOCK_FREQ;
   
   public enum ClockModes {NONEClock, FEIClock, FEEClock, FBIClock, BLPIClock, FBEClock, BLPEClock, PBEClock,  PEEClock};
   
   public ClockValidate_KLxx(long maxCoreClockFreq, long maxBusClockFreq) {
      MAX_CORE_CLOCK_FREQ      = maxCoreClockFreq;
      MAX_FLASH_BUS_CLOCK_FREQ = maxBusClockFreq;
   }

   public ClockValidate_KLxx() {
      this(48000000L, 24000000L);
   }
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode       =  getNumericModelNode("clock_mode");
      NumericOptionModelNode oscclk_clockNode           =  getNumericModelNode("oscclk_clock");
      NumericOptionModelNode system_erc_clockNode       =  getNumericModelNode("system_erc_clock");
      NumericOptionModelNode slowIRCNode                =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode fastIRCNode                =  getNumericModelNode("system_fast_irc_clock");
      NumericOptionModelNode mcg_c1_clksNode            =  getNumericModelNode("mcg_c1_clks");
      NumericOptionModelNode mcg_c1_irefsNode           =  getNumericModelNode("mcg_c1_irefs");
      BinaryOptionModelNode  mcg_c2_ircsNode            =  getBinaryModelNode("mcg_c2_ircs");
      NumericOptionModelNode mcg_c2_lpNode              =  getNumericModelNode("mcg_c2_lp");
      NumericOptionModelNode mcg_c6_pllsNode            =  safeGetNumericModelNode("mcg_c6_plls");
      NumericOptionModelNode mcg_sc_fcrdivNode          =  getNumericModelNode("mcg_sc_fcrdiv");
      NumericOptionModelNode system_mcgout_clockNode    =  getNumericModelNode("system_mcgout_clock");
      NumericOptionModelNode system_mcgir_clockNode     =  getNumericModelNode("system_mcgir_clock");
      NumericOptionModelNode fllTargetFrequencyNode     =  getNumericModelNode("fllTargetFrequency");
      NumericOptionModelNode pllTargetFrequencyNode     =  safeGetNumericModelNode("pllTargetFrequency");

      NumericOptionModelNode sim_clkdiv1_outdiv1Node    =  getNumericModelNode("sim_clkdiv1_outdiv1");
      NumericOptionModelNode sim_clkdiv1_outdiv4Node    =  getNumericModelNode("sim_clkdiv1_outdiv4");
      NumericOptionModelNode system_core_clockNode      =  getNumericModelNode("system_core_clock");
      NumericOptionModelNode system_bus_clockNode       =  getNumericModelNode("system_bus_clock");

      long system_mcgir_clock;
      if (mcg_c2_ircsNode.safeGetValue()) {
         long mcg_sc_fcrdiv = mcg_sc_fcrdivNode.getValueAsLong();
         system_mcgir_clock = fastIRCNode.getValueAsLong() / (1<<mcg_sc_fcrdiv);
      }
      else {
         system_mcgir_clock = slowIRCNode.getValueAsLong();
      }
      
      // ERC = OSCCLK
      long system_erc_clock = oscclk_clockNode.getValueAsLong();
      
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
      long pllTargetFrequency = 0;
      if (pllTargetFrequencyNode != null) {
         pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();
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
      Message system_core_clockMessage = 
            new Message(String.format("Must be <= %2.1f MHz.", MAX_CORE_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
      if (system_core_clock > MAX_CORE_CLOCK_FREQ) {
         system_core_clockMessage = 
               new Message(String.format("Clock frequency is too high. (Req. clock <= %2.2f MHz)", MAX_CORE_CLOCK_FREQ/1000000.0));
      }
      setValid(viewer, system_core_clockNode, system_core_clockMessage);

      // Bus Clock
      //===========================================
      long sim_clkdiv1_outdiv4 = sim_clkdiv1_outdiv4Node.getValueAsLong();
      long system_bus_clock = system_core_clock / sim_clkdiv1_outdiv4;
      Message system_bus_clockMessage = 
            new Message(String.format("Must be <= %2.1f MHz.", MAX_FLASH_BUS_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
      if (system_bus_clock > MAX_FLASH_BUS_CLOCK_FREQ) {
         system_bus_clockMessage = 
               new Message(String.format("Clock frequency is too high. (Req. clock <= %2.2f MHz)", MAX_FLASH_BUS_CLOCK_FREQ/1000000.0));
      }
      else if (system_bus_clock>system_core_clock) {
         system_bus_clockMessage = new Message("Clock is too high. (Req. clock <= Core clock)");
      }
      setValid(viewer, system_bus_clockNode, system_bus_clockMessage);
      
      setValid(viewer, primaryClockModeNode, primaryClockModeMessage);

      update(viewer, system_core_clockNode, system_core_clock);
      update(viewer, system_bus_clockNode, system_bus_clock);
      update(viewer, system_erc_clockNode, system_erc_clock);
      update(viewer, system_mcgir_clockNode, system_mcgir_clock);
      update(viewer, system_mcgout_clockNode, system_mcgout_clock);
      update(viewer, mcg_c1_clksNode, mcg_c1_clks);
      update(viewer, mcg_c1_irefsNode, mcg_c1_irefs);
      if (mcg_c6_pllsNode != null) {
         update(viewer, mcg_c6_pllsNode, mcg_c6_plls);
      }
      update(viewer, mcg_c2_lpNode, mcg_c2_lp);
   }

}
