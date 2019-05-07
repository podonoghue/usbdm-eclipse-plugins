package net.sourceforge.usbdm.annotationEditor.validators;

import org.eclipse.jface.viewers.TreeViewer;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.Severity;
import net.sourceforge.usbdm.annotationEditor.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.Message;
import net.sourceforge.usbdm.annotationEditor.MyValidator;
import net.sourceforge.usbdm.annotationEditor.NumericOptionModelNode;

public class ClockValidate_KLxx_LP extends MyValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long MAX_FLASH_BUS_CLOCK_FREQ;


   private static final long FLL_CLOCK_RANGE1_MIN = 32000L;
   private static final long FLL_CLOCK_RANGE1_MAX = 40000L;
   
   private static final long FLL_CLOCK_RANGE2_MIN = 3000000L;
   private static final long FLL_CLOCK_RANGE2_MAX = 8000000L;
   
   private static final long FLL_CLOCK_RANGE3_MIN = 8000000L;
   private static final long FLL_CLOCK_RANGE3_MAX = 32000000L;
   
   public enum ClockModes {NONEClock, LIRC8MHzClock, LIRC2MHzClock, HIRCClock, EXTClock};
   
   public ClockValidate_KLxx_LP(long maxCoreClockFreq, long maxBusClockFreq) {
      MAX_CORE_CLOCK_FREQ      = maxCoreClockFreq;
      MAX_FLASH_BUS_CLOCK_FREQ = maxBusClockFreq;
   }

   public ClockValidate_KLxx_LP() {
      this(48000000L, 24000000L);
   }
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);

      // Inputs
      NumericOptionModelNode primaryClockModeNode       =  getNumericModelNode("clock_mode");               // Basic clock mode
      
      NumericOptionModelNode oscclk_clockNode           =  getNumericModelNode("oscclk_clock");             // External clock or crystal
      NumericOptionModelNode irc48mClockNode            =  getNumericModelNode("irc48m_clock");             // Internal 48MHz oscillator
      NumericOptionModelNode irc2mClockNode             =  getNumericModelNode("irc2m_clock");              // Internal 2MHz oscillator
      NumericOptionModelNode irc8mClockNode             =  getNumericModelNode("irc8m_clock");              // Internal 8MHz oscillator

      NumericOptionModelNode mcg_sc_fcrdivNode          =  getNumericModelNode("mcg_sc_fcrdiv");            // LIRC clock divider
      NumericOptionModelNode mcg_mc_lirc_div2Node       =  getNumericModelNode("mcg_mc_lirc_div2");         // LIRC clock divider

      // Controlled outputs
      NumericOptionModelNode mcg_c1_clksNode            =  getNumericModelNode("mcg_c1_clks");
      NumericOptionModelNode mcg_c2_ircsNode            =  getNumericModelNode("mcg_c2_ircs");
      BinaryOptionModelNode  mcg_c2_erefsNode           =  getBinaryModelNode("mcg_c2_erefs0");
      NumericOptionModelNode mcg_c2_rangeNode = null;
      try {
         mcg_c2_rangeNode = getNumericModelNode("mcg_c2_range0");
      } catch (Exception e) {
         // Ignore as may not be present
      }

      BinaryOptionModelNode  rtc_cr_osceNode            =  getBinaryModelNode("rtc_cr_osce");

      NumericOptionModelNode system_mcgir_clockNode     =  getNumericModelNode("system_mcgir_clock");       // MGCIRCLK output
      NumericOptionModelNode system_core_clockNode      =  getNumericModelNode("system_core_clock");        // Core clock
      NumericOptionModelNode system_bus_clockNode       =  getNumericModelNode("system_bus_clock");         // Bus clock

      NumericOptionModelNode system_mcgout_clockNode    =  getNumericModelNode("system_mcgout_clock");

      NumericOptionModelNode sim_clkdiv1_outdiv1Node    =  getNumericModelNode("sim_clkdiv1_outdiv1");
      NumericOptionModelNode sim_clkdiv1_outdiv4Node    =  getNumericModelNode("sim_clkdiv1_outdiv4");
      
      long irc48mClock    = irc48mClockNode.getValueAsLong();
      long irc2mClock     = irc2mClockNode.getValueAsLong();
      long irc8mClock     = irc8mClockNode.getValueAsLong();
      long oscclkClock    = oscclk_clockNode.getValueAsLong();
      long mcg_sc_fcrdiv  = mcg_sc_fcrdivNode.getValueAsLong();
      
      long system_erc_clock = oscclkClock;
      
      boolean mcg_c2_erefs = mcg_c2_erefsNode.safeGetValue();
      boolean rtc_cr_osce  = rtc_cr_osceNode.safeGetValue();
      
      String externalOscillatorMessage       = null;
      String externalOscillatorRangeMessage  = "";
      long   mcg_c2_range                    = -1;

      if ((mcg_c2_rangeNode == null) || rtc_cr_osce) { 
         // No range option - must uses low-range crystal
         externalOscillatorRangeMessage = String.format("Permitted ranges [%d-%d]", FLL_CLOCK_RANGE1_MIN, FLL_CLOCK_RANGE1_MAX);
         if ((system_erc_clock >= FLL_CLOCK_RANGE1_MIN) && (system_erc_clock <= FLL_CLOCK_RANGE1_MAX)) {
            mcg_c2_range = 0;
         }
      }
      else {
         // Determine mcg_c2_range - Clock range of oscillator
         externalOscillatorRangeMessage = String.format("Permitted ranges [%d-%d], [%d-%d] or [%d-%d]", 
               FLL_CLOCK_RANGE1_MIN, FLL_CLOCK_RANGE1_MAX, FLL_CLOCK_RANGE2_MIN, FLL_CLOCK_RANGE2_MAX, FLL_CLOCK_RANGE3_MIN, FLL_CLOCK_RANGE3_MAX);
         if ((system_erc_clock >= FLL_CLOCK_RANGE1_MIN) && (system_erc_clock <= FLL_CLOCK_RANGE1_MAX)) {
            mcg_c2_range = 0;
         }
         else if ((system_erc_clock >= FLL_CLOCK_RANGE2_MIN) && (system_erc_clock <= FLL_CLOCK_RANGE2_MAX)) {
            mcg_c2_range = 1;
         }
         else if ((system_erc_clock >= FLL_CLOCK_RANGE3_MIN) && (system_erc_clock <= FLL_CLOCK_RANGE3_MAX)) {
            mcg_c2_range = 2;
         }
      }
      if (mcg_c2_range < 0) {
         if (mcg_c2_erefs || rtc_cr_osce) {
            // External crystal selected but not suitable frequency
            externalOscillatorMessage = "Frequency of the External Crystal is not suitable for use with the Oscillator\n";
            externalOscillatorMessage += externalOscillatorRangeMessage;
         }
         // Set compromise value
         if (system_erc_clock <= FLL_CLOCK_RANGE2_MIN) {
            mcg_c2_range = 0;
         }
         else if (system_erc_clock <= FLL_CLOCK_RANGE2_MAX) {
            mcg_c2_range = 1;
         }
         else {
            mcg_c2_range = 2;
         }
      }
      long clk = primaryClockModeNode.getValueAsLong();
      if (clk > ClockModes.values().length) {
         throw new Exception("Illegal Clock mode");
      }
      ClockModes primaryClockMode = ClockModes.values()[(int)clk];
      
      // Values to modify
      int mcg_c1_clks   = 1;
      int mcg_c2_ircs   = (int)mcg_c2_ircsNode.getValueAsLong();
      
      long ircClock = 0;
      
      long system_mcgout_clock = irc48mClock;
      String primaryClockModeMessage = null;
      
      switch (primaryClockMode) {
      case NONEClock:
         primaryClockModeMessage = "No clock settings are applied - reset default assumed";
         mcg_c1_clks     = 1;
         mcg_c2_ircs     = 1;
         break;
      case LIRC2MHzClock:
         mcg_c1_clks     = 1;
         mcg_c2_ircs     = 0;
         ircClock = irc2mClock/(1<<mcg_sc_fcrdiv);
         system_mcgout_clock = ircClock;
         break;
      case LIRC8MHzClock:
         mcg_c1_clks     = 1;
         mcg_c2_ircs     = 1;
         ircClock = irc8mClock/(1<<mcg_sc_fcrdiv);
         system_mcgout_clock = ircClock;
         break;
      case HIRCClock:
         mcg_c1_clks     = 0;
         system_mcgout_clock = irc48mClock;
         break;
      case EXTClock:
         mcg_c1_clks     = 2;
         system_mcgout_clock = oscclkClock;
         break;
      }
      switch (mcg_c2_ircs) {
      case 0:
         ircClock = irc2mClock/(1<<mcg_sc_fcrdiv);
         break;
      case 1:
         ircClock = irc8mClock/(1<<mcg_sc_fcrdiv);
         break;
      default:
         break;
      }
      long mcg_mc_lirc_div2   = mcg_mc_lirc_div2Node.getValueAsLong();
      long system_mcgir_clock = ircClock/(1<<mcg_mc_lirc_div2);
      
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

      update(viewer, mcg_c1_clksNode, mcg_c1_clks);
      update(viewer, mcg_c2_ircsNode, mcg_c2_ircs);

      update(viewer, system_core_clockNode, system_core_clock);
      update(viewer, system_bus_clockNode, system_bus_clock);
      update(viewer, system_mcgir_clockNode, system_mcgir_clock);
      update(viewer, system_mcgout_clockNode, system_mcgout_clock);

      setValid(viewer, oscclk_clockNode, externalOscillatorMessage);
      setValid(viewer, rtc_cr_osceNode,  rtc_cr_osce?externalOscillatorMessage:null);
      setValid(viewer, mcg_c2_erefsNode, mcg_c2_erefs?externalOscillatorMessage:null);
      if (mcg_c2_rangeNode != null) {
         update(viewer, mcg_c2_rangeNode, mcg_c2_range);
      }
   }

}
