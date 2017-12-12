package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
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

   private final long  DRST_DRS_MAX;

   public ClockValidator_MCG_no_pll(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      DRST_DRS_MAX     = (Long)it.next();
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

      // OSC0 Clock monitor
      //=================================
      Variable     mcg_c6_cme0Var      =  getVariable("mcg_c6_cme0");
      Variable     mcg_c2_locre0Var    =  getVariable("mcg_c2_locre0");

      mcg_c2_locre0Var.enable(mcg_c6_cme0Var.getValueAsBoolean());

      //=================================

      Variable system_mcgfllclk_clockVar        = getVariable("system_mcgfllclk_clock");
      Variable mcg_c4_dmx32Var                  = getVariable("mcg_c4_dmx32");
      Variable mcg_c1_frdivVar                  = getVariable("mcg_c1_frdiv");
      Variable mcg_c4_drst_drsVar               = getVariable("mcg_c4_drst_drs");

      Variable system_mcgoutclk_clock_sourceVar = getVariable("system_mcgoutclk_clock_source");
      Variable system_mcgoutclk_clockVar        = getVariable("system_mcgoutclk_clock");

      Variable mcg_c1_irefsVar                  = getVariable("mcg_c1_irefs");
      Variable mcg_c1_clksVar                   = getVariable("mcg_c1_clks");
      Variable mcg_c2_lpVar                     = getVariable("mcg_c2_lp");
      
      // Determine MCGIRCLK (not gated/undivided and gated)
      //========================================
      Variable mcg_sc_fcrdivVar           = safeGetVariable("mcg_sc_fcrdiv");
      Variable system_fast_irc_clockVar   = getVariable("system_fast_irc_clock");
      Variable system_slow_irc_clockVar   = getVariable("system_slow_irc_clock");
      Variable mcg_c2_ircsVar             = getVariable("mcg_c2_ircs");
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
      
      Variable system_mcgirclk_clockVar = getVariable("system_mcgirclk_clock");
      system_mcgirclk_clockVar.setOrigin(system_mcgir_ungated_clock.getOrigin());

      Variable mcg_c1_irclkenVar  = getVariable("mcg_c1_irclken");
      Variable mcg_c1_irefstenVar = getVariable("mcg_c1_irefsten");
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

      // Clock Mapping OSC0 always exists
      //====================================
      String         osc0_peripheral      = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      LongVariable   osc0_osc_clockVar    = getLongVariable(osc0_peripheral+"/osc_clock");

      // Determine MCG external reference clock [mcg_erc_clock]
      //========================================================
      ChoiceVariable mcg_c7_oscselVar = safeGetChoiceVariable("mcg_c7_oscsel");
      Variable mcg_erc_clockVar       = getVariable("mcg_erc_clock");
      
      if (mcg_c7_oscselVar == null) {
         // ERC = fixed OSCCLK (OSC0 main oscillator)
         mcg_erc_clockVar.setValue(osc0_osc_clockVar.getValueAsLong());
         mcg_erc_clockVar.setStatus(osc0_osc_clockVar.getFilteredStatus());
         mcg_erc_clockVar.setOrigin(osc0_osc_clockVar.getOrigin());
      }
      else {
         // Get alternative oscillator Must exist if mcg_c7_oscsel exists
         String         osc32k_peripheral   = getStringVariable("/SIM/osc32k_peripheral").getValueAsString();
         LongVariable   osc32k_osc_clockVar = getLongVariable(osc32k_peripheral+"/osc_clock");
         
         // Get alternative oscillator #2 may not exist
         Variable system_irc48m_clockVar           = safeGetVariable("system_irc48m_clock");

         // Determine ERC
         switch ((int)mcg_c7_oscselVar.getValueAsLong()) {
         case 2: // ERC = IRC48MCLK (OSC2) if it exists 
            if (system_irc48m_clockVar != null) {
               mcg_erc_clockVar.setValue(system_irc48m_clockVar.getValueAsLong());
               mcg_erc_clockVar.setStatus((Status)null);
               mcg_erc_clockVar.setOrigin("IRC48MCLK");
            break;
            }
            // Force legal selection
            mcg_c7_oscselVar.setValue(0);
            // no break
         default:
         case 0: // ERC = OSCCLK (OSC0 main oscillator)
            mcg_erc_clockVar.setValue(osc0_osc_clockVar.getValueAsLong());
            mcg_erc_clockVar.setStatus(osc0_osc_clockVar.getFilteredStatus());
            mcg_erc_clockVar.setOrigin(osc0_osc_clockVar.getOrigin());
            break;
         case 1: // ERC = RTCCLK (OSC1 oscillator)
            mcg_erc_clockVar.setValue(osc32k_osc_clockVar.getValueAsLong());
            mcg_erc_clockVar.setStatus(osc32k_osc_clockVar.getFilteredStatus());
            mcg_erc_clockVar.setOrigin(osc32k_osc_clockVar.getOrigin()+"[RTCCLK]");
            break;
         }
      }
      
      // Main clock mode
      //===============================
      int     mcg_c1_clks;
      int     mcg_c2_lp;
      boolean mcg_c1_irefs;
      
      // Main clock mode
      //====================
      ClockMode clock_mode = ClockMode.valueOf(getVariable("clock_mode").getSubstitutionValue());
      Variable fll_enabledVar                   = getVariable("fll_enabled");
      Variable fllInputFrequencyVar             = getVariable("fllInputFrequency");

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

      Variable osc0_osc_cr_erclkenVar  = safeGetBooleanVariable(osc0_peripheral+"/osc_cr_erclken");

      //=======================================
      // Find FLL dividers
      FllConfigure fllCheck = new FllConfigure(
            osc0_osc_cr_erclkenVar,
            safeGetVariable(osc0_peripheral+"/oscillatorRange"),
            getVariable("mcg_c2_range"),
            mcg_c1_irefs,
            mcg_erc_clockVar,
            system_slow_irc_clockVar.getValueAsLong(),
            (mcg_c7_oscselVar == null)?0:mcg_c7_oscselVar.getValueAsLong(), 
            mcg_c4_dmx32Var.getValueAsBoolean(),
            fllInputFrequencyVar,
            system_mcgfllclk_clockVar,
            getVariable("system_mcgffclk_clock"),
            DRST_DRS_MAX
            );

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
   
   @Override
   protected void createDependencies() throws Exception {
      // Clock Mapping
      //=================
      final String         osc0_peripheral       = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      final StringVariable osc32k_peripheralVar  = safeGetStringVariable("/SIM/osc32k_peripheral");

      if (osc32k_peripheralVar != null) {
         addToWatchedVariables(osc32k_peripheralVar.getValueAsString()+"/osc_clock"); // RTC
      }
      final String externalVariables[] = {
            osc0_peripheral+"/osc_clock",
            osc0_peripheral+"/osc_cr_erclken",
            osc0_peripheral+"/oscillatorRange",
      };
      addToWatchedVariables(externalVariables);
   }
}
