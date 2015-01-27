package net.sourceforge.usbdm.annotationEditor.validators;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.NumericOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.MyValidator;

import org.eclipse.jface.viewers.TreeViewer;

public class ClockValidate_CFV1 extends MyValidator {

   private final long MAX_CORE_CLOCK_FREQ;
   private final long USB_CLOCK_FREQ      = 48000000L;
   
   public enum ClockModes      {NONEClock, FEIClock, FEEClock, FBIClock, BLPIClock, FBEClock, BLPEClock, PBEClock,  PEEClock};
   public enum UsbClockSources {DividedPLL, DividedFLL, External};
   
   @Deprecated
   public ClockValidate_CFV1() {
      this(50000000);
   }
   
   public ClockValidate_CFV1(long maxCoreClockfrequency) {
      MAX_CORE_CLOCK_FREQ  = maxCoreClockfrequency;
   }
   
   @Override
   public void validate(TreeViewer viewer) throws Exception {
      super.validate(viewer);
      
      NumericOptionModelNode primaryClockModeNode       =  getNumericModelNode("clock_mode");
      NumericOptionModelNode oscclk_clockNode           =  getNumericModelNode("oscclk_clock");
      NumericOptionModelNode usbclkin_clockNode         =  getNumericModelNode("usbclkin_clock");
      
      NumericOptionModelNode system_erc_clockNode       =  getNumericModelNode("system_erc_clock");
      NumericOptionModelNode slowIRCNode                =  getNumericModelNode("system_slow_irc_clock");
      NumericOptionModelNode mcg_c1_clksNode            =  getNumericModelNode("mcg_c1_clks");
      NumericOptionModelNode mcg_c1_irefsNode           =  getNumericModelNode("mcg_c1_irefs");
      NumericOptionModelNode mcg_c2_bdivNode            =  getNumericModelNode("mcg_c2_bdiv");
      NumericOptionModelNode mcg_c2_lpNode              =  getNumericModelNode("mcg_c2_lp");
      NumericOptionModelNode mcg_c3_pllsNode            =  getNumericModelNode("mcg_c3_plls");
      NumericOptionModelNode system_mcgout_clockNode    =  getNumericModelNode("system_mcgout_clock");
      NumericOptionModelNode system_mcgir_clockNode     =  getNumericModelNode("system_mcgir_clock");
      NumericOptionModelNode fllTargetFrequencyNode     =  getNumericModelNode("fllTargetFrequency");
      NumericOptionModelNode pllTargetFrequencyNode     =  getNumericModelNode("pllTargetFrequency");
                                                        
      NumericOptionModelNode system_core_clockNode      =  getNumericModelNode("system_core_clock");
      NumericOptionModelNode system_bus_clockNode       =  getNumericModelNode("system_bus_clock");

      // USB
      NumericOptionModelNode system_usb_clockNode       =  getNumericModelNode("system_usb_clock");
      NumericOptionModelNode sim_clkdiv1_usbsrcNode     =  getNumericModelNode("sim_clkdiv1_usbsrc");
      NumericOptionModelNode sim_clkdiv1_usbdivNode     =  getNumericModelNode("sim_clkdiv1_usbdiv");
      NumericOptionModelNode sim_clkdiv1_usbfracNode    =  getNumericModelNode("sim_clkdiv1_usbfrac");

      long system_mcgir_clock = slowIRCNode.getValueAsLong();
//      System.err.println("ClockValidate.validate() system_mcgir_clock = " + system_mcgir_clock);

      long system_erc_clock;
      // ERC = OSCCLK
      system_erc_clock = oscclk_clockNode.getValueAsLong();
//      System.err.println("ClockValidate.validate() system_erc_clock = " + system_erc_clock);

      long clk = primaryClockModeNode.getValueAsLong();
      if (clk > ClockModes.values().length) {
         throw new Exception("Illegal Clock mode");
      }
      ClockModes primaryClockMode = ClockModes.values()[(int)primaryClockModeNode.getValueAsLong()];
      
      int mcg_c1_clks     = 0;
      int mcg_c1_irefs    = 1;
      int mcg_c3_plls     = 0;
      int mcg_c2_lp       = 0;
      
      long fllTargetFrequency = fllTargetFrequencyNode.getValueAsLong();
      long pllTargetFrequency = pllTargetFrequencyNode.getValueAsLong();
      long system_mcgout_clock = 0;
      String primaryClockModeMessage = null;
      
      switch (primaryClockMode) {
      case NONEClock:
         mcg_c1_clks          = 0;
         mcg_c1_irefs         = 1;
         mcg_c3_plls          = 0;
         mcg_c2_lp            = 0;
         system_mcgout_clock  = fllTargetFrequency;
         primaryClockModeMessage = "No clock settings are applied";
         break;
      case FEIClock:
         mcg_c1_clks     = 0;
         mcg_c1_irefs    = 1;
         mcg_c3_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = fllTargetFrequency;
         break;
      case FEEClock:
         mcg_c1_clks     = 0;
         mcg_c1_irefs    = 0;
         mcg_c3_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = fllTargetFrequency;
         break;
      case FBIClock:
         mcg_c1_clks     = 1;
         mcg_c1_irefs    = 1;
         mcg_c3_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = system_mcgir_clock;
         break;
      case BLPIClock:
         mcg_c1_clks     = 1;
         mcg_c1_irefs    = 1;
         mcg_c3_plls     = 0;
         mcg_c2_lp       = 1;
         system_mcgout_clock = system_mcgir_clock;
         break;
      case FBEClock:
         mcg_c1_clks     = 2;
         mcg_c1_irefs    = 0;
         mcg_c3_plls     = 0;
         mcg_c2_lp       = 0;
         system_mcgout_clock = system_erc_clock;
         break;
      case BLPEClock:
         mcg_c1_clks     = 2;
         mcg_c1_irefs    = 0;
         mcg_c3_plls     = 0;
         mcg_c2_lp       = 1;
         system_mcgout_clock = system_erc_clock;
         break;
      case PBEClock:
         mcg_c1_clks     = 2;
         mcg_c1_irefs    = 0;
         mcg_c3_plls     = 1;
         mcg_c2_lp       = 0;
         system_mcgout_clock = system_erc_clock;
         break;
      case PEEClock:
         mcg_c1_clks     = 0;
         mcg_c1_irefs    = 0;
         mcg_c3_plls     = 1;
         mcg_c2_lp       = 0;
         system_mcgout_clock = pllTargetFrequency;
         break;
      }
      // Core Clock
      //===========================================
      long mcg_c2_bdiv = mcg_c2_bdivNode.getValueAsLong();
      long system_core_clock   = system_mcgout_clock / (1<<mcg_c2_bdiv);
      String system_core_clockMessage = null;
      if (system_core_clock > MAX_CORE_CLOCK_FREQ) {
         system_core_clockMessage = String.format("Clock frequency is too high. (Req. clock <= %2.2f MHz)", MAX_CORE_CLOCK_FREQ/1000000.0);
      }
      setValid(viewer, system_core_clockNode, system_core_clockMessage);

      // Bus Clock
      //===========================================
      long system_bus_clock = system_core_clock / 2;
      
      setValid(viewer, system_bus_clockNode, null);
      setValid(viewer, primaryClockModeNode, primaryClockModeMessage);

      // USB
      //=========================================================
      UsbClockSources usbsrc = UsbClockSources.values()[(int)sim_clkdiv1_usbsrcNode.getValueAsLong()];
      long sim_clkdiv1_usbfrac = sim_clkdiv1_usbfracNode.getValueAsLong();
      long sim_clkdiv1_usbdiv = sim_clkdiv1_usbdivNode.getValueAsLong();
      
      long system_usb_clock = 0;
      if (usbsrc == UsbClockSources.External) {
         system_usb_clock = usbclkin_clockNode.getValueAsLong();
      } 
      else {
         boolean exact = false;
         double clockIn = 0;
         if (usbsrc == UsbClockSources.DividedFLL) {
            clockIn = fllTargetFrequency;
         } 
         else {
            clockIn = pllTargetFrequency;
         }
         // Try each possible DIV & FRAC
         for (sim_clkdiv1_usbdiv=1; sim_clkdiv1_usbdiv<=8; sim_clkdiv1_usbdiv++) {
            for (sim_clkdiv1_usbfrac = 1; sim_clkdiv1_usbfrac<=2; sim_clkdiv1_usbfrac++) {
               system_usb_clock = Math.round((clockIn*sim_clkdiv1_usbfrac)/sim_clkdiv1_usbdiv);
               exact = system_usb_clock == USB_CLOCK_FREQ;
               if (exact) {
                  break;
               }
            }
            if (exact) {
               break;
            }
         }
         if (!exact) {
            sim_clkdiv1_usbdiv  = 1;
            sim_clkdiv1_usbfrac = 1;
            system_usb_clock = Math.round((clockIn*sim_clkdiv1_usbfrac)/sim_clkdiv1_usbdiv);
         }
      }
      switch(usbsrc) {
      case DividedFLL:
         system_usb_clock = Math.round((fllTargetFrequency*sim_clkdiv1_usbfrac)/sim_clkdiv1_usbdiv);
         break;
      case DividedPLL:
         system_usb_clock = Math.round((pllTargetFrequency*sim_clkdiv1_usbfrac)/sim_clkdiv1_usbdiv);
         break;
      case External:
         system_usb_clock = usbclkin_clockNode.getValueAsLong();
         break;
      }
      String system_usb_clock_clockMessage = null;
      String sim_clkdiv1_usbsrcMessage     = null;
      if (system_usb_clock != USB_CLOCK_FREQ) {
         system_usb_clock_clockMessage = String.format("USB Clock frequency must be %d MHz", USB_CLOCK_FREQ/1000000);
         sim_clkdiv1_usbsrcMessage     = String.format("It is not possible to generate a valid USB clock from this source");
      }
      setValid(viewer, system_usb_clockNode,   system_usb_clock_clockMessage);
      setValid(viewer, sim_clkdiv1_usbsrcNode, sim_clkdiv1_usbsrcMessage);

      update(viewer, system_usb_clockNode,    system_usb_clock);
      update(viewer, sim_clkdiv1_usbfracNode, sim_clkdiv1_usbfrac);
      update(viewer, sim_clkdiv1_usbdivNode,  sim_clkdiv1_usbdiv);
      
      update(viewer, system_core_clockNode,   system_core_clock);
      update(viewer, system_bus_clockNode,    system_bus_clock);
      update(viewer, system_erc_clockNode,    system_erc_clock);
      update(viewer, system_mcgir_clockNode,  system_mcgir_clock);
      update(viewer, system_mcgout_clockNode, system_mcgout_clock);
      update(viewer, mcg_c1_clksNode,         mcg_c1_clks);
      update(viewer, mcg_c1_irefsNode,        mcg_c1_irefs);
      update(viewer, mcg_c2_lpNode,           mcg_c2_lp);
      update(viewer, mcg_c3_pllsNode,         mcg_c3_plls);
   }

}
