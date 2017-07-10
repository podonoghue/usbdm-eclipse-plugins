package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable    ;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine MCG settings

 * Used for:
 *     mcg_mk_ics48mml
 *     mcg_mk
 */
public class ClockValidator_MKL extends BaseClockValidator {

   private final long PLL_IN_MIN;
   private final long PLL_IN_MAX;

   private final long PLL_OUT_MIN;
   private final long PLL_OUT_MAX;

   private final long  PRDIV_MIN;
   private final long  PRDIV_MAX;

   private final long  VDIV_MIN;
   private final long  VDIV_MAX;

   private final long  PLL_POST_DIV;

   private boolean addedExternalVariables = false;
   private final static String[] externalVariables = {
         "/OSC0/oscclk_clock",
         "/OSC0/osc32kclk_clock",
         "/OSC0/"+OscValidate.OSC_RANGE_KEY,
         "/OSC0/range",
   };

   public ClockValidator_MKL(PeripheralWithState peripheral, ArrayList<Object> values) {
      super(peripheral);

      ListIterator<Object> it = values.listIterator();
      PLL_IN_MIN   = (Long)it.next();
      PLL_IN_MAX   = (Long)it.next();  
      PLL_OUT_MIN  = (Long)it.next();   
      PLL_OUT_MAX  = (Long)it.next();   
      PRDIV_MIN    = (Long)it.next();   
      PRDIV_MAX    = (Long)it.next();   
      VDIV_MIN     = (Long)it.next();    
      VDIV_MAX     = (Long)it.next();    
      PLL_POST_DIV = (Long)it.next();  

      try {
         LongVariable mcg_c5_prdiv0Var = getLongVariable("mcg_c5_prdiv0");
         mcg_c5_prdiv0Var.setOffset(-PRDIV_MIN);
         mcg_c5_prdiv0Var.setMin(PRDIV_MIN);
         mcg_c5_prdiv0Var.setMax(PRDIV_MAX);

         LongVariable mcg_c6_vdiv0Var = getLongVariable("mcg_c6_vdiv0");
         mcg_c6_vdiv0Var.setOffset(-VDIV_MIN);
         mcg_c6_vdiv0Var.setMin(VDIV_MIN);
         mcg_c6_vdiv0Var.setMax(VDIV_MAX);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Inputs
    *    /OSC0/OscValidate.OSC_RANGE_KEY
    *    /OSC0/oscclk_clock
    *    /OSC0/range
    * 
    * Outputs 
    *    system_slow_irc_clock
    *    system_fast_irc_clock
    *    system_mcgirclk_clock
    *    system_mcgffclk_clock
    *    system_mcgoutclk_clock
    *    system_mcgfllclk_clock
    *    system_mcgpllclk_clock
    * @throws Exception 
    */
   @Override
   protected void validate(Variable variable) throws Exception {
      //      System.err.println(getSimpleClassName()+" Var = "+variable);

      if (!addedExternalVariables) {
         addToWatchedVariables(externalVariables);
         addedExternalVariables = true;
      }

      // MCGIR
      //=================================
      Variable slow_irc_clockVar;
      Variable fast_irc_clockVar;
      Variable mcg_sc_fcrdivVar;
      Variable mcg_c2_ircsVar;
      Variable mcg_c1_irclkenVar;
      Variable mcg_c1_irefstenVar;
      Variable system_mcgirclk_clockVar;
      // Internal
      //=================================
      Variable system_irc48m_clockVar;
      //    Variable     usb_clkin_clockVar              =  getLongVariable("system_usb_clkin_clock");
      Variable usb1pfdclk_ClockVar;
      // Clocks and information from main oscillator
      //=================================
      Variable oscclk_clockVar;
      Variable osc32kclk_clockVar;
      Variable osc_cr_erclkenVar;
      Variable oscRangeInVar;
      //===================
      Variable mcg_c2_range0Var;
      //=================================
      Variable clock_modeVar;
      Variable mcg_c7_oscselVar;
      Variable mcg_erc_clockVar;
      // FLL
      //=================================
      Variable fll_enabledVar;
      Variable fllInputFrequencyVar;
      Variable system_mcgfllclk_clockVar;
      Variable mcg_c4_dmx32Var;
      Variable mcg_c1_frdivVar;
      Variable mcg_c4_drst_drsVar;
      Variable system_mcgffclk_clockVar;
      // PLL0
      //=================================
      Variable pll0EnabledVar;
      Variable pll0InputFrequencyVar;
      Variable pll0OutputFrequency;
      Variable mcg_c5_pllclkenVar;
      Variable mcg_c5_pllstenVar;
      Variable mcg_c5_prdiv0Var;
      Variable mcg_c6_vdiv0Var;
      // PLL
      Variable system_mcgpllclk_clockVar;
      //=================================
      Variable system_mcgoutclk_clock_sourceVar;
      Variable system_mcgoutclk_clockVar;
      // Hidden
      //=====================
      Variable mcg_c1_irefsVar;
      Variable mcg_c1_clksVar;
      Variable mcg_c2_lpVar;
      Variable mcg_c6_pllsVar;

      // Clock monitors
      //=================================
      Variable     mcg_c6_cme0Var                 =  getVariable("mcg_c6_cme0");
      Variable     mcg_c2_locre0Var               =  getVariable("mcg_c2_locre0");

      //      Variable     mcg_c6_lolie0Var               =  getVariable("mcg_c6_lolie0");

      mcg_c2_locre0Var.enable(mcg_c6_cme0Var.getValueAsBoolean());

      slow_irc_clockVar = getVariable("system_slow_irc_clock");
      fast_irc_clockVar = getVariable("system_fast_irc_clock");
      mcg_sc_fcrdivVar = safeGetVariable("mcg_sc_fcrdiv");
      mcg_c2_ircsVar = getVariable("mcg_c2_ircs");
      mcg_c1_irclkenVar = getVariable("mcg_c1_irclken");
      mcg_c1_irefstenVar = getVariable("mcg_c1_irefsten");
      system_mcgirclk_clockVar = getVariable("system_mcgirclk_clock");

      system_irc48m_clockVar = safeGetVariable("system_irc48m_clock");
      usb1pfdclk_ClockVar = safeGetVariable("/MCG/usb1pfdclk_Clock");

      oscclk_clockVar = getVariable("/OSC0/oscclk_clock");
      osc32kclk_clockVar = getVariable("/OSC0/osc32kclk_clock");
      osc_cr_erclkenVar = getVariable("/OSC0/osc_cr_erclken");
      oscRangeInVar = getVariable("/OSC0/"+OscValidate.OSC_RANGE_KEY);

      mcg_c2_range0Var = getVariable("/OSC0/range");

      clock_modeVar = getVariable("clock_mode");
      mcg_c7_oscselVar = safeGetVariable("mcg_c7_oscsel");
      mcg_erc_clockVar = getVariable("mcg_erc_clock");

      fll_enabledVar = getVariable("fll_enabled");
      fllInputFrequencyVar = getVariable("fllInputFrequency");
      system_mcgfllclk_clockVar = getVariable("system_mcgfllclk_clock");
      mcg_c4_dmx32Var = getVariable("mcg_c4_dmx32");
      mcg_c1_frdivVar = getVariable("mcg_c1_frdiv");
      mcg_c4_drst_drsVar = getVariable("mcg_c4_drst_drs");
      system_mcgffclk_clockVar = getVariable("system_mcgffclk_clock");

      pll0EnabledVar = getVariable("pll0Enabled");
      pll0InputFrequencyVar = getVariable("pll0InputFrequency");
      pll0OutputFrequency = getVariable("pll0OutputFrequency");
      mcg_c5_pllclkenVar = getVariable("mcg_c5_pllclken");
      mcg_c5_pllstenVar = getVariable("mcg_c5_pllsten");
      mcg_c5_prdiv0Var = getVariable("mcg_c5_prdiv0");
      mcg_c6_vdiv0Var = getVariable("mcg_c6_vdiv0");

      system_mcgpllclk_clockVar = getVariable("system_mcgpllclk_clock");

      system_mcgoutclk_clock_sourceVar = getVariable("system_mcgoutclk_clock_source");
      system_mcgoutclk_clockVar = getVariable("system_mcgoutclk_clock");

      mcg_c1_irefsVar = getVariable("mcg_c1_irefs");
      mcg_c1_clksVar = getVariable("mcg_c1_clks");
      mcg_c2_lpVar = getVariable("mcg_c2_lp");
      mcg_c6_pllsVar = getVariable("mcg_c6_plls");

      // Main clock mode
      //====================
      if (system_irc48m_clockVar != null) {
         system_irc48m_clockVar.setOrigin("48MHz clock from IRC48MCLK");
      }

      // Determine MCGIRCLK (not gated/undivided and gated)
      //========================================
      Variable system_mcgir_ungated_clock = new LongVariable("system_mcgir_ungated", null);
      if (mcg_c2_ircsVar.getValueAsBoolean()) {
         // Fast IRC selected
         if (mcg_sc_fcrdivVar != null) {
            // Variable divisor
            long mcg_sc_fcrdiv = mcg_sc_fcrdivVar.getValueAsLong();
            system_mcgir_ungated_clock.setOrigin("(Fast IRC)/FCRDIV");
            system_mcgir_ungated_clock.setValue(fast_irc_clockVar.getValueAsLong() / (1<<mcg_sc_fcrdiv));
         }
         else {
            // Fixed divisor of 2
            system_mcgir_ungated_clock.setOrigin("(Fast IRC)/2");
            system_mcgir_ungated_clock.setValue(fast_irc_clockVar.getValueAsLong() / 2);
         }
      }
      else {
         // Slow IRC selected
         system_mcgir_ungated_clock.setOrigin("Slow IRC");
         system_mcgir_ungated_clock.setValue(slow_irc_clockVar.getValueAsLong());
      }
      system_mcgirclk_clockVar.setOrigin(system_mcgir_ungated_clock.getOrigin());
      if (mcg_c1_irclkenVar.getValueAsBoolean()) {
         // Enabled
         system_mcgirclk_clockVar.setValue(system_mcgir_ungated_clock.getValueAsLong());
         system_mcgirclk_clockVar.setStatus((Status)null);
         system_mcgirclk_clockVar.enable(true);
         mcg_c1_irefstenVar.enable(true);
      }
      else {
         // Disabled
         system_mcgirclk_clockVar.setValue(0);
         system_mcgirclk_clockVar.setStatus(new Status("Disabled by mcg_c1_irclken", Severity.OK));
         system_mcgirclk_clockVar.enable(false);
         mcg_c1_irefstenVar.enable(false);
      }
      // Determine MCG external reference clock [mcg_erc_clock]
      //========================================================
      int oscsel;
      if (mcg_c7_oscselVar == null) {
         // Default if no MCG_C7_OSCSEL register field
         oscsel = 0;
      }
      else {
         oscsel = (int)mcg_c7_oscselVar.getValueAsLong();
      }
      switch (oscsel) {
      default:
      case 0: // ERC = OSCCLK
         mcg_erc_clockVar.setValue(oscclk_clockVar.getValueAsLong());
         mcg_erc_clockVar.setStatus(oscclk_clockVar.getFilteredStatus());
         mcg_erc_clockVar.setOrigin(oscclk_clockVar.getOrigin());
         break;
      case 1: // ERC = OSC32KCLK
         mcg_erc_clockVar.setValue(osc32kclk_clockVar.getValueAsLong());
         mcg_erc_clockVar.setStatus(osc32kclk_clockVar.getFilteredStatus());
         mcg_erc_clockVar.setOrigin(osc32kclk_clockVar.getOrigin()+"[RTCCLK]");
         break;
      case 2: // ERC = IRC48MCLK
         mcg_erc_clockVar.setValue(system_irc48m_clockVar.getValueAsLong());
         mcg_erc_clockVar.setStatus((Status)null);
         mcg_erc_clockVar.setOrigin("IRC48MCLK");
         break;
      }

      // Main clock mode
      //===============================
      int     mcg_c1_clks;
      int     mcg_c6_plls;
      int     mcg_c2_lp;
      boolean mcg_c1_irefs;

      ClockMode clock_mode = ClockMode.valueOf(clock_modeVar.getSubstitutionValue());
      switch (clock_mode) {
      default:
      case ClockMode_None:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FEI:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FEE:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FBI:
         mcg_c1_clks  = 1;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("MCGIRCLK");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FBE:
         mcg_c1_clks  = 2;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_BLPI:
         mcg_c1_clks  = 1;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 1;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("MCGIRCLK");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(false);
         break;
      case ClockMode_BLPE:
         mcg_c1_clks  = 2;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 1;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(false);
         break;
      case ClockMode_PBE:
         mcg_c1_clks  = 2;
         mcg_c6_plls  = 1;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         pll0EnabledVar.setValue(true);
         fll_enabledVar.setValue(false);
         break;
      case ClockMode_PEE:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 1;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("PLL output");
         pll0EnabledVar.setValue(true);
         fll_enabledVar.setValue(false);
         break;
      }     
      mcg_c1_clksVar.setValue(mcg_c1_clks);
      mcg_c6_pllsVar.setValue(mcg_c6_plls);
      mcg_c2_lpVar.setValue(mcg_c2_lp);
      mcg_c1_irefsVar.setValue(mcg_c1_irefs);

      long mcg_c7_oscsel = 0;
      if (mcg_c7_oscselVar != null) {
         mcg_c7_oscsel = mcg_c7_oscselVar.getValueAsLong();
      }
      //=======================================
      // Find FLL dividers
      FllConfigure fllCheck = new FllConfigure(
            osc_cr_erclkenVar,
            oscRangeInVar,
            mcg_c2_range0Var,
            mcg_c1_irefs,
            mcg_erc_clockVar,
            slow_irc_clockVar.getValueAsLong(),
            mcg_c7_oscsel, 
            mcg_c4_dmx32Var.getValueAsBoolean(),
            fllInputFrequencyVar,
            system_mcgfllclk_clockVar,
            system_mcgffclk_clockVar);

      mcg_c1_frdivVar.setValue(fllCheck.mcg_c1_frdiv);
      mcg_c4_drst_drsVar.setValue(fllCheck.mcg_c4_drst_drs);

      //=================================================
      // PLLs
      if (usb1pfdclk_ClockVar != null) {
         // Check USB HS PLL
         long irClockFreq = oscclk_clockVar.getValueAsLong();
         String failedPreCondition = null;
         if (!osc_cr_erclkenVar.getValueAsBoolean()) {
            failedPreCondition = "Disabled: to use PFDCLK, OSCCLK clock must be enabled by osc_cr_erclkenVar";
         }
         else if (!mcg_c1_irclkenVar.getValueAsBoolean()) {
            failedPreCondition = "Disabled: to use PFDCLK, IRC clock must be enabled by mcg_c1_irclken";
         }
         else if ((irClockFreq!=12000000)&&(irClockFreq!=16000000)&&(irClockFreq!=24000000)) {
            failedPreCondition = "Disabled: to use PFDCLK, OSCCLK must be in [12Mhz, 16MHz, 24MHz]";
         }
         if (failedPreCondition==null) {
            usb1pfdclk_ClockVar.enable(true);
            usb1pfdclk_ClockVar.setOrigin("Clock from USB HS PLL"); 
            usb1pfdclk_ClockVar.setStatus((Status)null);
         }
         else {
            usb1pfdclk_ClockVar.enable(false);
            usb1pfdclk_ClockVar.setOrigin("Clock from USB HS PLL (disabled)"); 
            usb1pfdclk_ClockVar.setStatus(new Status(failedPreCondition, Severity.WARNING));
         }
      }
      // Internal PLL
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

      pllConfigure.validate(mcg_erc_clockVar, pll0InputFrequencyVar, pll0OutputFrequency, mcg_c5_prdiv0Var, mcg_c6_vdiv0Var);

      boolean pll0Enabled = pll0EnabledVar.getValueAsBoolean();
      pll0InputFrequencyVar.enable(pll0Enabled);
      mcg_c5_prdiv0Var.enable(pll0Enabled);
      mcg_c6_vdiv0Var.enable(pll0Enabled);
      mcg_c5_pllstenVar.enable(pll0Enabled);
      if (pll0Enabled) {
         pll0OutputFrequency.enable(pll0InputFrequencyVar.getFilteredStatus() == null);
         pll0OutputFrequency.setStatus(pllConfigure.getPllStatus());
      }
      else {
         pll0OutputFrequency.enable(false);
         pll0OutputFrequency.setStatus(new Status("PLL is disabled", Severity.WARNING));
      }
      // Internal PLL
      system_mcgpllclk_clockVar.setValue(pll0OutputFrequency.getValueAsLong());
      system_mcgpllclk_clockVar.setOrigin(pll0OutputFrequency.getOrigin());
      system_mcgpllclk_clockVar.setStatus(pll0OutputFrequency.getFilteredStatus());

      //======================================
      // FLL status
      boolean fllEnabled = fll_enabledVar.getValueAsBoolean();
      fllInputFrequencyVar.enable(fllEnabled);
      if (fllEnabled) {
         boolean fllInputIsOK = (fllInputFrequencyVar.getStatus() == null) ||
               (fllCheck.getFllStatus().getSeverity().lessThan(Severity.WARNING));
         system_mcgfllclk_clockVar.enable(fllInputIsOK);
         system_mcgfllclk_clockVar.setStatus(fllCheck.getFllStatus());
      }
      else {
         system_mcgfllclk_clockVar.enable(false);
         system_mcgfllclk_clockVar.setStatus(new Status("Fll is disabled", Severity.WARNING));
      }
      mcg_c4_dmx32Var.enable(fllEnabled);
      mcg_c4_drst_drsVar.enable(fllEnabled);

      // Main clock mode
      //===============================

      Status clock_mode_Status = null;
      switch (clock_mode) {
      default:
      case ClockMode_None:
         system_mcgoutclk_clockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus((Status)null);
         clock_mode_Status = new Status("No clock settings are applied", Severity.WARNING);
         break;
      case ClockMode_FEI:
         system_mcgoutclk_clockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgfllclk_clockVar.getFilteredStatus());
         break;
      case ClockMode_FEE:
         system_mcgoutclk_clockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgfllclk_clockVar.getFilteredStatus());
         break;
      case ClockMode_FBI:
         system_mcgoutclk_clockVar.setValue(system_mcgir_ungated_clock.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgir_ungated_clock.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgir_ungated_clock.getFilteredStatus());
         break;
      case ClockMode_FBE:
         system_mcgoutclk_clockVar.setValue(mcg_erc_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(mcg_erc_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(mcg_erc_clockVar.getFilteredStatus());
         break;
      case ClockMode_BLPI:
         system_mcgoutclk_clockVar.setValue(system_mcgir_ungated_clock.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgir_ungated_clock.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgir_ungated_clock.getFilteredStatus());
         break;
      case ClockMode_BLPE:
         system_mcgoutclk_clockVar.setValue(mcg_erc_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(mcg_erc_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(mcg_erc_clockVar.getFilteredStatus());
         break;
      case ClockMode_PBE:
         system_mcgoutclk_clockVar.setValue(mcg_erc_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(mcg_erc_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(mcg_erc_clockVar.getFilteredStatus());
         break;
      case ClockMode_PEE:
         system_mcgoutclk_clockVar.setValue(system_mcgpllclk_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgpllclk_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgpllclk_clockVar.getFilteredStatus());
         break;
      }     
      system_mcgoutclk_clock_sourceVar.setStatus(clock_mode_Status);
      system_mcgoutclk_clock_sourceVar.setOrigin(system_mcgoutclk_clockVar.getOrigin());

   }
}
