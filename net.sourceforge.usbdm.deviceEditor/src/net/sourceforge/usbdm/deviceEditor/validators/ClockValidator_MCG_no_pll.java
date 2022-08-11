package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.Variable.ChoiceData;
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

   Variable       osc0_osc_cr_erclkenVar    = null;
   Variable       osc0_oscillatorRangeVar   = null;

   Variable       usb1pfdclk_ClockVar       = null;

   public ClockValidator_MCG_no_pll(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      DRST_DRS_MAX     = (Long)it.next();
   }

   /**
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
//      System.err.println(getSimpleClassName()+" "+variable +", Index ="+index);

      super.validate(variable);

      // Check configuration name is valid C identifier
      StringVariable clockConfig = getStringVariable("ClockConfig");
      clockConfig.setStatus(isValidCIdentifier(clockConfig.getValueAsString())?(String)null:"Illegal C enum value");

      // Enable whole category from clock enable variable
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration");
      clockConfig.enable(enableClockConfigurationVar.getValueAsBoolean());
      
      // OSC0 LOC Clock monitor (if present)
      //=================================
      Variable     mcg_c6_cme0Var      =  getVariable("mcg_c6_cme0");
      Variable     mcg_c2_locre0Var    =  getVariable("mcg_c2_locre0");

      if ((mcg_c6_cme0Var != null) && (mcg_c2_locre0Var != null)) {
         mcg_c2_locre0Var.enable(mcg_c6_cme0Var.getValueAsBoolean());
      }

      // OSC1 LOC (RTC) Clock monitor (if present)
      //=================================
      Variable     mcg_c8_cme1Var      =  safeGetVariable("mcg_c8_cme1");
      Variable     mcg_c8_locre1Var    =  safeGetVariable("mcg_c8_locre1");
      if ((mcg_c8_cme1Var != null) && (mcg_c8_locre1Var != null)) {
         mcg_c8_locre1Var.enable(mcg_c8_cme1Var.getValueAsBoolean());
      }

      // Determine mcg_c1_irefsten is available
      //========================================
      Variable mcg_c1_irclkenVar  = getVariable("mcg_c1_irclken");
      Variable mcg_c1_irefstenVar = getVariable("mcg_c1_irefsten");
      if (mcg_c1_irclkenVar.getValueAsBoolean()) {
         // Enabled
         mcg_c1_irefstenVar.enable(true);
      }
      else {
         // Disabled
         mcg_c1_irefstenVar.enable(false);
      }

      //=================================

      Variable fllOutputFrequencyVar            = getVariable("fllOutputFrequency");
      Variable system_mcgfllclk_clockVar        = getVariable("system_mcgfllclk_clock");
      Variable mcg_c4_dmx32Var                  = getVariable("mcg_c4_dmx32");
      Variable mcg_c1_frdivVar                  = getVariable("mcg_c1_frdiv");
      Variable mcg_c4_drst_drsVar               = getVariable("mcg_c4_drst_drs");

      Variable system_mcgoutclk_clock_sourceVar = getVariable("system_mcgoutclk_clock_source");
      Variable system_mcgoutclk_clockVar        = getVariable("system_mcgoutclk_clock");

      Variable mcg_c1_irefsVar                  = getVariable("mcg_c1_irefs");
      Variable mcg_c1_clksVar                   = getVariable("mcg_c1_clks");
      Variable mcg_c2_lpVar                     = getVariable("mcg_c2_lp");
      

      // Main clock mode
      //===============================
      int     mcg_c1_clks;
      int     mcg_c2_lp;
      boolean mcg_c1_irefs;
      
      // Main clock mode
      //====================
      ChoiceVariable mcgClockModeVar   = getChoiceVariable("mcgClockMode");
      McgClockMode   clock_mode        = McgClockMode.valueOf(mcgClockModeVar.getEnumValue());
      
      Variable   fll_enabledVar        = getVariable("fll_enabled");
      Variable   fllInputFrequencyVar  = getVariable("fllInputFrequency");

      boolean mcg_c2_ircsVar_StatusWarning = false;
      Variable mcg_c2_ircsVar              = getVariable("mcg_c2_ircs");

      switch (clock_mode) {
      default:
      case McgClockMode_FEI:
         mcg_c1_clks  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_FEE:
         mcg_c1_clks  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_FBI:
         mcg_c1_clks  = 1;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("MCGIRCLK");
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_FBE:
         mcg_c1_clks  = 2;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_BLPI:
         mcg_c1_clks  = 1;
         mcg_c2_lp    = 1;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("MCGIRCLK");
         fll_enabledVar.setValue(false);
         // Add BLPE/BLPI warning
         mcg_c2_ircsVar_StatusWarning = !mcg_c2_ircsVar.getValueAsBoolean();
         break;
      case McgClockMode_BLPE:
         mcg_c1_clks  = 2;
         mcg_c2_lp    = 1;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         fll_enabledVar.setValue(false);
         // Add BLPE/BLPI warning
         mcg_c2_ircsVar_StatusWarning = !mcg_c2_ircsVar.getValueAsBoolean();
         break;
      }     
      mcg_c1_clksVar.setValue(mcg_c1_clks);
      mcg_c2_lpVar.setValue(mcg_c2_lp);
      mcg_c1_irefsVar.setValue(mcg_c1_irefs);
      if (mcg_c2_ircsVar_StatusWarning) {
         mcg_c2_ircsVar.setStatus(new Status("Fast IRC clock should be selected if entering VLPR mode", Severity.WARNING));
      }
      else {
         mcg_c2_ircsVar.clearStatus();
      }

      // Determine MCG external reference clock [mcg_erc_clock]
      //========================================================
      ChoiceVariable mcg_c7_oscselVar = safeGetChoiceVariable("mcg_c7_oscsel");
      Variable       mcg_erc_clockVar = getVariable("mcg_erc_clock");

      Variable system_slow_irc_clockVar   = getVariable("system_slow_irc_clock");

      //=======================================
      // Find FLL dividers
      FllConfigure fllCheck = new FllConfigure(
            osc0_osc_cr_erclkenVar,
            osc0_oscillatorRangeVar,
            getVariable("mcg_c2_range0"),
            mcg_c1_irefs,
            mcg_erc_clockVar,
            system_slow_irc_clockVar.getValueAsLong(),
            (mcg_c7_oscselVar == null)?0:mcg_c7_oscselVar.getValueAsLong(), 
            mcg_c4_dmx32Var.getValueAsBoolean(),
            fll_enabledVar,
            fllInputFrequencyVar,
            fllOutputFrequencyVar,
            getVariable("system_mcgffclk_clock"),
            DRST_DRS_MAX
            );

      mcg_c1_frdivVar.setValue(fllCheck.mcg_c1_frdiv);
      mcg_c4_drst_drsVar.setValue(fllCheck.mcg_c4_drst_drs);

      //======================================
      // system_mcgfllclk_clock update
      //
      boolean fllEnabled = fll_enabledVar.getValueAsBoolean();
      String fllToolTip;
      if (fllEnabled) {
         fllToolTip = "Output of FLL. Available as MCGFLLCLK and used for MCGOUTCLK in FEI or FEE clock modes";
         system_mcgfllclk_clockVar.setStatus(fllOutputFrequencyVar.getFilteredStatus());
      }
      else {
         fllToolTip = "Unavailable in this clock mode";
         system_mcgfllclk_clockVar.setStatus(new Status("FLL clock unavailable in this clock mode", Severity.WARNING));
      }
      system_mcgfllclk_clockVar.enable(fllEnabled);
      system_mcgfllclk_clockVar.setOrigin(fllOutputFrequencyVar.getOrigin());
      system_mcgfllclk_clockVar.setValue(fllOutputFrequencyVar.getValueAsLong());
      system_mcgfllclk_clockVar.setToolTip(fllToolTip);
      
      // Main clock mode
      //===============================

      Status clock_mode_Status = null;
      Variable system_mcgirclk_ungated_clock = getVariable("system_mcgirclk_ungated");

      switch (clock_mode) {
      default:
      case McgClockMode_FEI:
         system_mcgoutclk_clockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgfllclk_clockVar.getFilteredStatus());
         break;
      case McgClockMode_FEE:
         system_mcgoutclk_clockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgfllclk_clockVar.getFilteredStatus());
         break;
      case McgClockMode_FBI:
         system_mcgoutclk_clockVar.setValue(system_mcgirclk_ungated_clock.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgirclk_ungated_clock.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgirclk_ungated_clock.getFilteredStatus());
         break;
      case McgClockMode_FBE:
         system_mcgoutclk_clockVar.setValue(mcg_erc_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(mcg_erc_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(mcg_erc_clockVar.getFilteredStatus());
         break;
      case McgClockMode_BLPI:
         system_mcgoutclk_clockVar.setValue(system_mcgirclk_ungated_clock.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgirclk_ungated_clock.getOrigin());
         system_mcgoutclk_clockVar.setStatus(system_mcgirclk_ungated_clock.getFilteredStatus());
         break;
      case McgClockMode_BLPE:
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
      super.createDependencies();
      
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      // mcg_erc_clock is the main clock input to MCG (maintained by SIM)
      variablesToWatch.add("/SIM/mcg_erc_clock");

      // mcg_erc Selection
      ChoiceVariable mcg_c7_oscselVar = safeGetChoiceVariable("mcg_c7_oscsel");
      if (mcg_c7_oscselVar == null) {
         mcg_c7_oscselVar = safeGetChoiceVariable("mcg_c7_oscsel_fixed");
      }
      ChoiceData[] choiceData = mcg_c7_oscselVar.getData();
      
      //  mcg_erc[0] = OSC0, input must always exists
      int index = choiceData[0].getReference().lastIndexOf("/");
      String osc0Name = choiceData[0].getReference().substring(0, index);
      osc0_osc_cr_erclkenVar     = safeGetBooleanVariable(osc0Name+"/osc_cr_erclken");
      osc0_oscillatorRangeVar    = safeGetVariable(osc0Name+"/oscillatorRange");

      usb1pfdclk_ClockVar = safeGetVariable("usb1pfdclk_Clock");
      if (usb1pfdclk_ClockVar != null) {
         variablesToWatch.add("usb1pfdclk_Clock");
      }
      
      addToWatchedVariables(variablesToWatch);

      // enableClockConfiguration[0] is always true to enable 1st clock configuration
      // Hide from user
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration[0]");
      enableClockConfigurationVar.setHidden(true);
   }
}
