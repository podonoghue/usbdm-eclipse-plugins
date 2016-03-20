package net.sourceforge.usbdm.annotationEditor.validators;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;
import net.sourceforge.usbdm.annotationEditor.Message;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class ClockValidate_MKMxx extends MyValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_BUS_CLOCK_FREQ;
   
   public enum ClockModes {NONEClock, FEIClock, FEEClock, FBIClock, BLPIClock, FBEClock, BLPEClock, PBEClock,  PEEClock, PEIClock, PBIClock};
   
   public ClockValidate_MKMxx(long maxCoreClockfrequency, long maxBusClockFrequency) {
      MAX_CORE_CLOCK_FREQ     = maxCoreClockfrequency;
      MAX_BUS_CLOCK_FREQ      = maxBusClockFrequency;
   }
   
   public ClockValidate_MKMxx() {
      this(50000000L, 25000000L);
   }
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode       =  getNumericModelNode("clock_mode");
      NumericOptionModelNode oscclk_clockNode           =  getNumericModelNode("oscclk_clock");
      NumericOptionModelNode osc32kclk_clockNode        =  safeGetNumericModelNode("osc32kclk_clock");
      
      NumericOptionModelNode system_erc_clockNode       =  getNumericModelNode("system_erc_clock");
      NumericOptionModelNode slowIRCNode                =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode fastIRCNode                =  getNumericModelNode("system_fast_irc_clock");
      NumericOptionModelNode mcg_c1_clksNode            =  getNumericModelNode("mcg_c1_clks");
      NumericOptionModelNode mcg_c1_irefsNode           =  getNumericModelNode("mcg_c1_irefs");
      BinaryOptionModelNode  mcg_c2_ircsNode            =  getBinaryModelNode("mcg_c2_ircs");
      NumericOptionModelNode mcg_c2_lpNode              =  getNumericModelNode("mcg_c2_lp");
      NumericOptionModelNode mcg_c6_pllsNode            =  getNumericModelNode("mcg_c6_plls");
      NumericOptionModelNode mcg_c7_pll32krefselNode    =  getNumericModelNode("mcg_c7_pll32krefsel");
      NumericOptionModelNode mcg_sc_fcrdivNode          =  safeGetNumericModelNode("mcg_sc_fcrdiv");
      NumericOptionModelNode mcg_c7_oscselNode          =  safeGetNumericModelNode("mcg_c7_oscsel");
      NumericOptionModelNode system_mcgout_clockNode    =  getNumericModelNode("system_mcgout_clock");
      NumericOptionModelNode system_mcgir_clockNode     =  getNumericModelNode("system_mcgir_clock");
      NumericOptionModelNode fllTargetFrequencyNode     =  getNumericModelNode("fllTargetFrequency");
      NumericOptionModelNode pllTargetFrequencyNode     =  getNumericModelNode("pllTargetFrequency");
                                                        
      NumericOptionModelNode sim_clkdiv1_sysdivNode     =  getNumericModelNode("sim_clkdiv1_sysdiv");
      NumericOptionModelNode sim_clkdiv1_sysclkmodeNode =  getNumericModelNode("sim_clkdiv1_sysclkmode");
      NumericOptionModelNode system_core_clockNode      =  getNumericModelNode("system_core_clock");
      NumericOptionModelNode system_bus_clockNode       =  getNumericModelNode("system_bus_clock");

      long system_mcgir_clock;
      if (mcg_c2_ircsNode.safeGetValue()) {
         // Variable divisor
         long mcg_sc_fcrdiv = mcg_sc_fcrdivNode.getValueAsLong();
         system_mcgir_clock = fastIRCNode.getValueAsLong() / (1<<mcg_sc_fcrdiv);
      }
      else {
         system_mcgir_clock = slowIRCNode.getValueAsLong();
      }
//      System.err.println("ClockValidate_MKMxx.validate() system_mcgir_clock = " + system_mcgir_clock);

      // Default if no MCG_C7_OSCSEL register field
      long system_erc_clock = oscclk_clockNode.getValueAsLong();
      if (mcg_c7_oscselNode != null) {
         switch ((int)mcg_c7_oscselNode.getValueAsLong()) {
         case 0: // ERC = OSCCLK
            system_erc_clock = oscclk_clockNode.getValueAsLong();
            break;
         case 1: // ERC = OSC32KCLK
            system_erc_clock = osc32kclk_clockNode.getValueAsLong();
            break;
         default:
            throw new Exception("Illegal Clock source (mcg_c7_oscsel)");
         }
      }
//      System.err.println("ClockValidate_MKMxx.validate() system_erc_clock = " + system_erc_clock);

      long clk = primaryClockModeNode.getValueAsLong();
      if (clk > ClockModes.values().length) {
         throw new Exception("Illegal Clock mode");
      }
      ClockModes primaryClockMode = ClockModes.values()[(int)primaryClockModeNode.getValueAsLong()];
      
      int     mcg_c1_clks                = 0;
      int     mcg_c1_irefs               = 1;
      int     mcg_c6_plls                = 0;
      int     mcg_c2_lp                  = 0;
      int     mcg_c7_pll32krefsel        = (int) mcg_c7_pll32krefselNode.getValueAsLong();
      boolean mcg_c7_pll32krefsel_lock   = false;
      String  mcg_c7_pll32krefselMessage = null;
      long    fllTargetFrequency         = fllTargetFrequencyNode.getValueAsLong();
      long    pllTargetFrequency         = pllTargetFrequencyNode.getValueAsLong();
      long    system_mcgout_clock        = 0;
      String  primaryClockModeMessage    = null;
      
      switch (primaryClockMode) {
      case NONEClock:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = fllTargetFrequency;
         primaryClockModeMessage = "No clock settings are applied";
         break;
      case FEIClock:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = fllTargetFrequency;
         break;
      case FEEClock:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = fllTargetFrequency;
         break;
      case FBIClock:
         mcg_c1_clks         = 1;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = system_mcgir_clock;
         break;
      case BLPIClock:
         mcg_c1_clks         = 1;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 1;
         system_mcgout_clock = system_mcgir_clock;
         break;
      case FBEClock:
         mcg_c1_clks         = 2;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 0;
         system_mcgout_clock = system_erc_clock;
         break;
      case BLPEClock:
         mcg_c1_clks         = 2;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 0;
         mcg_c2_lp           = 1;
         system_mcgout_clock = system_erc_clock;
         break;
      case PBEClock:
         mcg_c1_clks         = 2;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 1;
         mcg_c7_pll32krefsel = 2;
         mcg_c7_pll32krefsel_lock = true;
         mcg_c2_lp           = 0;
         system_mcgout_clock = system_erc_clock;
         break;
      case PEEClock:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 0;
         mcg_c6_plls         = 1;
         if ((mcg_c7_pll32krefsel&1) != 0) {
            mcg_c7_pll32krefselMessage = "Setting must be 0 or 2 for PEE mode";
         }
         mcg_c2_lp           = 0;
         system_mcgout_clock = pllTargetFrequency;
         break;
      case PBIClock:
         mcg_c1_clks         = 1;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 1;
         mcg_c7_pll32krefsel = 1;
         mcg_c7_pll32krefsel_lock = true;
         mcg_c2_lp           = 0;
         system_mcgout_clock = pllTargetFrequency;
         break;
      case PEIClock:
         mcg_c1_clks         = 0;
         mcg_c1_irefs        = 1;
         mcg_c6_plls         = 1;
         mcg_c7_pll32krefsel = 1;
         mcg_c7_pll32krefsel_lock = true;
         mcg_c2_lp           = 0;
         system_mcgout_clock = pllTargetFrequency;
         break;
      }
      // Core Clock
      //===========================================
      long sim_clkdiv1_sysdiv = sim_clkdiv1_sysdivNode.getValueAsLong();
      long system_core_clock   = system_mcgout_clock / sim_clkdiv1_sysdiv;
      Message system_core_clockMessage = 
            new Message(String.format("Must be <= %2.1f MHz.", MAX_CORE_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
      if (system_core_clock > MAX_CORE_CLOCK_FREQ) {
         system_core_clockMessage = 
               new Message(String.format("Frequency is too high. (Req. <= %2.2f MHz)", MAX_CORE_CLOCK_FREQ/1000000.0));
//         System.err.println("ClockValidate_MKMxx.validate() Core clock frequency is too high = " + system_core_clock);
      }
//      System.err.println("ClockValidate_MKMxx.validate() Core clock frequency = " + system_core_clock + 
//            ", MAX_BUS_CLOCK_FREQ = " + MAX_CORE_CLOCK_FREQ );
      setValid(viewer, system_core_clockNode, system_core_clockMessage);

      // Bus Clock
      //===========================================
      long sim_clkdiv1_sysclkmode = sim_clkdiv1_sysclkmodeNode.getValueAsLong();
      long system_bus_clock = system_mcgout_clock;
      switch ((int)sim_clkdiv1_sysclkmode) {
      case 0 :
         system_bus_clock = system_core_clock;
         break;
      case 1 :
         system_bus_clock = system_core_clock/2;
         break;
      default:
         break;
      }
      Message system_bus_clockMessage = 
            new Message(String.format("Must be <= %2.1f MHz.", MAX_BUS_CLOCK_FREQ/1000000.0), Severity.INFORMATION);
      if (system_bus_clock > MAX_BUS_CLOCK_FREQ) {
         system_bus_clockMessage = 
               new Message(String.format("Frequency is too high. (Req. <= %2.2f MHz)", MAX_BUS_CLOCK_FREQ/1000000.0));
//         System.err.println("ClockValidate_MKMxx.validate() Bus clock frequency is too high = " + system_bus_clock);
      }
//      System.err.println("ClockValidate_MKMxx.validate() Bus clock = " + system_bus_clock + 
//            ", MAX_BUS_CLOCK_FREQ = " + MAX_BUS_CLOCK_FREQ );
      setValid(viewer, system_bus_clockNode, system_bus_clockMessage);
      setValid(viewer, primaryClockModeNode, primaryClockModeMessage);

      update(viewer, system_core_clockNode, system_core_clock);
      update(viewer, system_bus_clockNode, system_bus_clock);
      update(viewer, system_erc_clockNode, system_erc_clock);
      update(viewer, system_mcgir_clockNode, system_mcgir_clock);
      update(viewer, system_mcgout_clockNode, system_mcgout_clock);
      update(viewer, mcg_c1_clksNode, mcg_c1_clks);
      update(viewer, mcg_c1_irefsNode, mcg_c1_irefs);
      update(viewer, mcg_c6_pllsNode, mcg_c6_plls);
      setValid(viewer, mcg_c7_pll32krefselNode, mcg_c7_pll32krefselMessage);
      mcg_c7_pll32krefselNode.setModifiable(!mcg_c7_pll32krefsel_lock);
      update(viewer, mcg_c7_pll32krefselNode, mcg_c7_pll32krefsel);
      update(viewer, mcg_c2_lpNode, mcg_c2_lp);
   }
}
