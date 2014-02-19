package net.sourceforge.usbdm.annotationEditor.validators;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class ClockValidate_KExx extends MyValidator {

   private final long MAX_CORE_CLOCK_FREQ      = 20000000L;
   private final long MAX_FLASH_BUS_CLOCK_FREQ = 20000000L;
   
   public enum ClockModes {NONEClock, FEIClock, FEEClock, FBIClock, BLPIClock, FBEClock, BLPEClock, PBEClock,  PEEClock};
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode       =  getNumericModelNode("clock_mode");
      NumericOptionModelNode oscclk_clockNode           =  getNumericModelNode("oscclk_clock");
      NumericOptionModelNode slowIRCNode                =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode ics_c1_clksNode            =  getNumericModelNode("ics_c1_clks");
      NumericOptionModelNode ics_c1_irefsNode           =  getNumericModelNode("ics_c1_irefs");
      NumericOptionModelNode ics_c2_lpNode              =  getNumericModelNode("ics_c2_lp");
      NumericOptionModelNode system_icsout_clockNode    =  getNumericModelNode("system_icsout_clock");
      NumericOptionModelNode system_icsir_clockNode     =  getNumericModelNode("system_icsir_clock");
      NumericOptionModelNode fllTargetFrequencyNode     =  getNumericModelNode("fllTargetFrequency");

      NumericOptionModelNode sim_busdiv_busdivNode      =  getNumericModelNode("sim_busdiv_busdiv");
      NumericOptionModelNode system_core_clockNode      =  getNumericModelNode("system_core_clock");
      NumericOptionModelNode system_bus_clockNode       =  getNumericModelNode("system_bus_clock");

      long system_icsir_clock = slowIRCNode.getValueAsLong();
      
      // ERC = OSCCLK
      long system_erc_clock = oscclk_clockNode.getValueAsLong();
      
      long clk = primaryClockModeNode.getValueAsLong();
      if (clk > ClockModes.values().length) {
         throw new Exception("Illegal Clock mode");
      }
      ClockModes primaryClockMode = ClockModes.values()[(int)primaryClockModeNode.getValueAsLong()];
      
      int ics_c1_clks     = 0;
      int ics_c1_irefs    = 1;
      int ics_c2_lp       = 0;
      
      long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();
      long system_icsout_clock = 0;
      String primaryClockModeMessage = null;
      
      NumericOptionModelNode ics_c2_bdivNode  =  getNumericModelNode("ics_c2_bdiv");
      long icsoutclk = fllTargetFrequency / (1<<ics_c2_bdivNode.getValueAsLong());

      switch (primaryClockMode) {
      case NONEClock:
         ics_c1_clks          = 0;
         ics_c1_irefs         = 1;
         ics_c2_lp            = 0;
         system_icsout_clock  = icsoutclk;
         primaryClockModeMessage = "No clock settings are applied";
         break;
      case FEIClock:
         ics_c1_clks     = 0;
         ics_c1_irefs    = 1;
         ics_c2_lp       = 0;
         system_icsout_clock = icsoutclk;
         break;
      case FEEClock:
         ics_c1_clks     = 0;
         ics_c1_irefs    = 0;
         ics_c2_lp       = 0;
         system_icsout_clock = icsoutclk;
         break;
      case FBIClock:
         ics_c1_clks     = 1;
         ics_c1_irefs    = 1;
         ics_c2_lp       = 0;
         system_icsout_clock = system_icsir_clock;
         break;
      case BLPIClock:
         ics_c1_clks     = 1;
         ics_c1_irefs    = 1;
         ics_c2_lp       = 1;
         system_icsout_clock = system_icsir_clock;
         break;
      case FBEClock:
         ics_c1_clks     = 2;
         ics_c1_irefs    = 0;
         ics_c2_lp       = 0;
         system_icsout_clock = system_erc_clock;
         break;
      case BLPEClock:
         ics_c1_clks     = 2;
         ics_c1_irefs    = 0;
         ics_c2_lp       = 1;
         system_icsout_clock = system_erc_clock;
         break;
      default:
         throw new Exception("Illegal Clock mode");
      }
      // Core Clock
      //===========================================
      long system_core_clock = system_icsout_clock;
      String system_core_clockMessage = null;
      if (system_core_clock > MAX_CORE_CLOCK_FREQ) {
         system_core_clockMessage = String.format("Clock frequency is too high. (Req. clock <= %d MHz)", MAX_CORE_CLOCK_FREQ/1000000);
      }
      setValid(viewer, system_core_clockNode, system_core_clockMessage);


      // Bus Clock
      //===========================================
      long sim_busdiv_busdiv = sim_busdiv_busdivNode.getValueAsLong();
      long system_bus_clock = system_core_clock / sim_busdiv_busdiv;
      String system_bus_clockMessage = null;
      if (system_bus_clock > MAX_FLASH_BUS_CLOCK_FREQ) {
         system_bus_clockMessage = String.format("Clock frequency is too high. (Req. clock <= %d MHz)", MAX_FLASH_BUS_CLOCK_FREQ/1000000);
      }
      setValid(viewer, system_bus_clockNode, system_bus_clockMessage);
      
      setValid(viewer, primaryClockModeNode, primaryClockModeMessage);

      update(viewer, system_core_clockNode, system_core_clock);
      update(viewer, system_bus_clockNode, system_bus_clock);
      update(viewer, system_icsir_clockNode, system_icsir_clock);
      update(viewer, system_icsout_clockNode, system_icsout_clock);
      update(viewer, ics_c1_clksNode, ics_c1_clks);
      update(viewer, ics_c1_irefsNode, ics_c1_irefs);
      update(viewer, ics_c2_lpNode, ics_c2_lp);
   }

}
