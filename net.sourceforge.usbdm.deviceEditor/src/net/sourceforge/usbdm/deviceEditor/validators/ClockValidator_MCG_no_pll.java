package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine MCG settings

 * Used for:
 *     mcg_mk_ics48mml
 *     mcg_mk
 */
public class ClockValidator_MCG_no_pll extends BaseClockValidator {

   private final static String[] externalVariables = {
         "/RTC/rtcclk_clock",
         "/OSC0/oscclk_clock",
         "/OSC0/osc_cr_erclken",
         "/OSC0/oscillatorRange",
   };

   public ClockValidator_MCG_no_pll(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);
   }

   /**
    * Inputs
    *    /RTC/rtcclk_clock
    *    /OSC0/oscclk_clock
    *    /OSC0/osc_cr_erclken
    *    /OSC0/oscillatorRange
    * 
    * Outputs (direct)
    * - system_mcgir_ungated_clock
    * - system_mcgirclk_clock
    * - mcg_erc_clock
    *
    * - fll_enabled
    * - fllInputFrequency
    * - system_mcgfllclk_clock
    * 
    * - system_mcgffclk_clock
    * 
    * - system_mcgoutclk_clock_source
    * - system_mcgoutclk_clock
    * 
    * - mcg_c1_clks
    * - mcg_c1_irefs
    * - mcg_c1_frdiv
    * - mcg_c2_lp
    * - mcg_c4_drst_drs
    * - mcg_c2_range
    *    
    * @throws Exception 
    */
   @Override
   protected void validate(Variable variable) throws Exception {
      addToWatchedVariables(externalVariables);

      for (int index=0; index<fDimension; index++) {
         fIndex = index;
         validateClocks(variable);
      }
      fIndex = 0;
   }

   protected void validateClocks(Variable variable) throws Exception {
      //      System.err.println(getSimpleClassName()+" Var = "+variable);

      super.validate(variable);

      StringVariable clockConfig = safeGetStringVariable("ClockConfig");
      clockConfig.setStatus(isValidCIdentifier(clockConfig.getValueAsString())?(String)null:"Illegal C enum value");

      // MCGIR
      //=================================
      Variable system_slow_irc_clockVar;
      Variable system_fast_irc_clockVar;
      Variable mcg_sc_fcrdivVar;
      Variable mcg_c2_ircsVar;
      Variable mcg_c1_irclkenVar;
      Variable mcg_c1_irefstenVar;
      Variable system_mcgirclk_clockVar;
      // Internal
      //=================================
      Variable system_irc48m_clockVar;
      // RTC
      //=================================
      Variable rtcclk_clockVar;
      // Clocks and information from main oscillator
      //=================================
      Variable osc_oscclk_clockVar;
      Variable osc_osc_cr_erclkenVar;
      Variable osc_oscillatorRangeVar;
      //===================
      Variable mcg_c2_rangeVar;
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
      //=================================
      Variable system_mcgoutclk_clock_sourceVar;
      Variable system_mcgoutclk_clockVar;
      // Hidden
      //=====================
      Variable mcg_c1_irefsVar;
      Variable mcg_c1_clksVar;
      Variable mcg_c2_lpVar;

      // Clock monitors
      //=================================
      Variable     mcg_c6_cme0Var      =  getVariable("mcg_c6_cme0");
      Variable     mcg_c2_locre0Var    =  getVariable("mcg_c2_locre0");

      Variable     mcg_c8_cme1Var      =  safeGetVariable("mcg_c8_cme1");
      Variable     mcg_c8_locre1Var    =  safeGetVariable("mcg_c8_locre1");

      Variable     mcg_c9_pll_cmeVar   =  safeGetVariable("mcg_c9_pll_cme");
      if (mcg_c9_pll_cmeVar != null) {
         getVariable("mcg_c9_pll_locre").enable(mcg_c9_pll_cmeVar.getValueAsBoolean());
      }

      mcg_c2_locre0Var.enable(mcg_c6_cme0Var.getValueAsBoolean());

      rtcclk_clockVar = safeGetVariable("/RTC/rtcclk_clock");
      if (rtcclk_clockVar != null) {
         mcg_c8_locre1Var.enable(mcg_c8_cme1Var.getValueAsBoolean());
         mcg_c8_cme1Var.enable(rtcclk_clockVar != null);
      }
      system_slow_irc_clockVar         = getVariable("system_slow_irc_clock");
      system_fast_irc_clockVar         = getVariable("system_fast_irc_clock");
      mcg_sc_fcrdivVar                 = safeGetVariable("mcg_sc_fcrdiv");
      mcg_c2_ircsVar                   = getVariable("mcg_c2_ircs");
      mcg_c1_irclkenVar                = getVariable("mcg_c1_irclken");
      mcg_c1_irefstenVar               = getVariable("mcg_c1_irefsten");
      system_mcgirclk_clockVar         = getVariable("system_mcgirclk_clock");

      system_irc48m_clockVar           = safeGetVariable("system_irc48m_clock");

      osc_oscclk_clockVar              = getVariable("/OSC0/oscclk_clock");
      osc_osc_cr_erclkenVar            = getVariable("/OSC0/osc_cr_erclken");
      osc_oscillatorRangeVar           = getVariable("/OSC0/oscillatorRange");

      mcg_c2_rangeVar                  = getVariable("mcg_c2_range");

      clock_modeVar                    = getVariable("clock_mode");
      mcg_c7_oscselVar                 = safeGetVariable("mcg_c7_oscsel");
      mcg_erc_clockVar                 = getVariable("mcg_erc_clock");

      fll_enabledVar                   = getVariable("fll_enabled");
      fllInputFrequencyVar             = getVariable("fllInputFrequency");
      system_mcgfllclk_clockVar        = getVariable("system_mcgfllclk_clock");
      mcg_c4_dmx32Var                  = getVariable("mcg_c4_dmx32");
      mcg_c1_frdivVar                  = getVariable("mcg_c1_frdiv");
      mcg_c4_drst_drsVar               = getVariable("mcg_c4_drst_drs");
      system_mcgffclk_clockVar         = getVariable("system_mcgffclk_clock");

      system_mcgoutclk_clock_sourceVar = getVariable("system_mcgoutclk_clock_source");
      system_mcgoutclk_clockVar        = getVariable("system_mcgoutclk_clock");

      mcg_c1_irefsVar                  = getVariable("mcg_c1_irefs");
      mcg_c1_clksVar                   = getVariable("mcg_c1_clks");
      mcg_c2_lpVar                     = getVariable("mcg_c2_lp");

      // Main clock mode
      //====================
      ClockMode clock_mode = ClockMode.valueOf(clock_modeVar.getSubstitutionValue());
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
            system_mcgir_ungated_clock.setValue(system_fast_irc_clockVar.getValueAsLong() / (1<<mcg_sc_fcrdiv));
         }
         else {
            // Fixed divisor of 2
            system_mcgir_ungated_clock.setOrigin("(Fast IRC)/2");
            system_mcgir_ungated_clock.setValue(system_fast_irc_clockVar.getValueAsLong() / 2);
         }
      }
      else {
         // Slow IRC selected
         system_mcgir_ungated_clock.setOrigin("Slow IRC");
         system_mcgir_ungated_clock.setValue(system_slow_irc_clockVar.getValueAsLong());
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
         mcg_erc_clockVar.setValue(osc_oscclk_clockVar.getValueAsLong());
         mcg_erc_clockVar.setStatus(osc_oscclk_clockVar.getFilteredStatus());
         mcg_erc_clockVar.setOrigin(osc_oscclk_clockVar.getOrigin());
         break;
      case 1: // ERC = OSC32KCLK
         if (rtcclk_clockVar != null) {
            mcg_erc_clockVar.setValue(rtcclk_clockVar.getValueAsLong());
            mcg_erc_clockVar.setStatus(rtcclk_clockVar.getFilteredStatus());
            mcg_erc_clockVar.setOrigin(rtcclk_clockVar.getOrigin()+"[RTCCLK]");
         }
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
      int     mcg_c2_lp;
      boolean mcg_c1_irefs;

      switch (clock_mode) {
      default:
      case ClockMode_None:
         mcg_c1_clks  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FEI:
         mcg_c1_clks  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FEE:
         mcg_c1_clks  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FBI:
         mcg_c1_clks  = 1;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("MCGIRCLK");
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_FBE:
         mcg_c1_clks  = 2;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         fll_enabledVar.setValue(true);
         break;
      case ClockMode_BLPI:
         mcg_c1_clks  = 1;
         mcg_c2_lp    = 1;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("MCGIRCLK");
         fll_enabledVar.setValue(false);
         break;
      case ClockMode_BLPE:
         mcg_c1_clks  = 2;
         mcg_c2_lp    = 1;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         fll_enabledVar.setValue(false);
         break;
      }     
      mcg_c1_clksVar.setValue(mcg_c1_clks);
      mcg_c2_lpVar.setValue(mcg_c2_lp);
      mcg_c1_irefsVar.setValue(mcg_c1_irefs);

      long mcg_c7_oscsel = 0;
      if (mcg_c7_oscselVar != null) {
         mcg_c7_oscsel = mcg_c7_oscselVar.getValueAsLong();
      }
      //=======================================
      // Find FLL dividers
      FllConfigure fllCheck = new FllConfigure(
            osc_osc_cr_erclkenVar.getValueAsBoolean(),
            osc_oscillatorRangeVar,
            mcg_c2_rangeVar,
            mcg_c1_irefs,
            mcg_erc_clockVar,
            system_slow_irc_clockVar.getValueAsLong(),
            mcg_c7_oscsel, 
            mcg_c4_dmx32Var.getValueAsBoolean(),
            fllInputFrequencyVar,
            system_mcgfllclk_clockVar,
            system_mcgffclk_clockVar);

      mcg_c1_frdivVar.setValue(fllCheck.mcg_c1_frdiv);
      mcg_c4_drst_drsVar.setValue(fllCheck.mcg_c4_drst_drs);

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
         system_mcgfllclk_clockVar.setStatus(new Status("FLL is disabled", Severity.WARNING));
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
      }     
      system_mcgoutclk_clock_sourceVar.setStatus(clock_mode_Status);
      system_mcgoutclk_clock_sourceVar.setOrigin(system_mcgoutclk_clockVar.getOrigin());

   }
}
