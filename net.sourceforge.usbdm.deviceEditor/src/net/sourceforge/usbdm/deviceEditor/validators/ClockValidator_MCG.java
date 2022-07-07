package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
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
public class ClockValidator_MCG extends BaseClockValidator {

   private final long  PLL_IN_MIN;
   private final long  PLL_IN_MAX;

   private final long  PLL_OUT_MIN;
   private final long  PLL_OUT_MAX;

   private final long  PRDIV_MIN;
   private final long  PRDIV_MAX;

   private final long  VDIV_MIN;
   private final long  VDIV_MAX;

   private final long  PLL_POST_DIV;
   
   private final long  DRST_DRS_MAX;

   String         osc0_peripheralName       = null;
   String         osc0_description          = null;
   LongVariable   osc0_osc_clockVar         = null;
   Variable       osc0_osc_cr_erclkenVar    = null;
   Variable       osc0_oscillatorRangeVar   = null;

   LongVariable   osc1_osc_clockVar         = null;
   String         osc1_description          = null;

   LongVariable   osc2_osc_clockVar         = null;
   String         osc2_description          = null;

   Variable       usb1pfdclk_ClockVar       = null;

   public ClockValidator_MCG(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      DRST_DRS_MAX     = (Long)it.next();
      PLL_IN_MIN   = (Long)it.next();
      PLL_IN_MAX   = (Long)it.next();  
      PLL_OUT_MIN  = (Long)it.next();   
      PLL_OUT_MAX  = (Long)it.next();   
      PRDIV_MIN    = (Long)it.next();   
      PRDIV_MAX    = (Long)it.next();   
      VDIV_MIN     = (Long)it.next();    
      VDIV_MAX     = (Long)it.next();    
      PLL_POST_DIV = (Long)it.next();  

      addVariable(new LongVariable(null, "/MCG/pll_vdiv_min",     Long.toString(VDIV_MIN)));
      addVariable(new LongVariable(null, "/MCG/pll_post_divider", Long.toString(PLL_POST_DIV)));
      
      try {
         for (fIndex=0; fIndex<dimension; fIndex++) {
            LongVariable mcg_c5_prdiv0Var = getLongVariable("mcg_c5_prdiv0");
            mcg_c5_prdiv0Var.setOffset(-PRDIV_MIN);
            mcg_c5_prdiv0Var.setMin(PRDIV_MIN);
            mcg_c5_prdiv0Var.setMax(PRDIV_MAX);

            LongVariable mcg_c6_vdiv0Var = getLongVariable("mcg_c6_vdiv0");
            mcg_c6_vdiv0Var.setOffset(-VDIV_MIN);
            mcg_c6_vdiv0Var.setMin(VDIV_MIN);
            mcg_c6_vdiv0Var.setMax(VDIV_MAX);
         }
         fIndex = 0;
      } catch (Exception e) {
         e.printStackTrace();
      }
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

      // Fix enabling of clock configurations
      StringVariable clockConfig = getStringVariable("ClockConfig");
      clockConfig.setStatus(isValidCIdentifier(clockConfig.getValueAsString())?(String)null:"Illegal C enum value");

      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration");
      if (fIndex == 0) {
         // Clock configuration 0 is always true to enable 1st clock configuration
         enableClockConfigurationVar.setDefault(true);
      }
      clockConfig.enable(enableClockConfigurationVar.getValueAsBoolean());
      
      // OSC0 LOC Clock monitor (if present)
      //=================================
      Variable     mcg_c6_cme0Var      =  getVariable("mcg_c6_cme0");
      Variable     mcg_c2_locre0Var    =  getVariable("mcg_c2_locre0");

      if ((mcg_c6_cme0Var != null) && (mcg_c2_locre0Var != null)) {
         mcg_c2_locre0Var.enable(mcg_c6_cme0Var.getValueAsBoolean());
      }

      // PLL LOL monitor (if present)
      //=================================
      Variable     mcg_c6_lolie0Var    =  safeGetVariable("mcg_c6_lolie0");
      Variable     mcg_c8_lolreVar     =  safeGetVariable("mcg_c8_lolre");

      if ((mcg_c8_lolreVar != null) && (mcg_c6_lolie0Var != null)) {
         mcg_c8_lolreVar.enable(mcg_c6_lolie0Var.getValueAsBoolean());
      }

      // OSC1 LOC (RTC) Clock monitor (if present)
      //=================================
      Variable     mcg_c8_cme1Var      =  safeGetVariable("mcg_c8_cme1");
      Variable     mcg_c8_locre1Var    =  safeGetVariable("mcg_c8_locre1");
      if ((mcg_c8_cme1Var != null) && (mcg_c8_locre1Var != null)) {
         mcg_c8_locre1Var.enable(mcg_c8_cme1Var.getValueAsBoolean());
      }
      
      // External PLL LOC monitor (if present)
      //=================================
      Variable     mcg_c9_pll_cmeVar   =  safeGetVariable("mcg_c9_pll_cme");
      if (mcg_c9_pll_cmeVar != null) {
         getVariable("mcg_c9_pll_locre").enable(mcg_c9_pll_cmeVar.getValueAsBoolean());
      }

      //=================================

      Variable fllOutputFrequencyVar            = getVariable("fllOutputFrequency");
      Variable system_mcgfllclk_clockVar        = getVariable("system_mcgfllclk_clock");
      Variable mcg_c4_dmx32Var                  = getVariable("mcg_c4_dmx32");
      Variable mcg_c1_frdivVar                  = getVariable("mcg_c1_frdiv");
      Variable mcg_c4_drst_drsVar               = getVariable("mcg_c4_drst_drs");

      Variable pll0EnabledVar                   = getVariable("pll0Enabled");
      Variable pll0InputFrequencyVar            = getVariable("pll0InputFrequency");
      Variable pll0OutputFrequencyVar           = getVariable("pll0OutputFrequency");
      Variable mcg_c5_pllclkenVar               = getVariable("mcg_c5_pllclken");
//      Variable mcg_c5_pllstenVar                = getVariable("mcg_c5_pllsten");
      Variable mcg_c5_prdiv0Var                 = getVariable("mcg_c5_prdiv0");
      Variable mcg_c6_vdiv0Var                  = getVariable("mcg_c6_vdiv0");

      Variable system_mcgpllclk_clockVar        = getVariable("system_mcgpllclk_clock");
      Variable mcg_c6_pllsVar                   = getVariable("mcg_c6_plls");

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

      // Determine MCG external reference clock [mcg_erc_clock]
      //========================================================
      ChoiceVariable mcg_c7_oscselVar = safeGetChoiceVariable("mcg_c7_oscsel");
      Variable       mcg_erc_clockVar = getVariable("mcg_erc_clock");

      Variable ercOrigin;
      String    ercPathDescription = "";
      if (mcg_c7_oscselVar == null) {
         // Fixed OSCCLK (OSC0 main oscillator)
         ercOrigin = osc0_osc_clockVar;
      }
      else {
         int mcg_c7_oscsel = (int)mcg_c7_oscselVar.getValueAsLong();
         
         // Find erc 
         switch (mcg_c7_oscsel) {
         default:
         case 0: // ERC = OSCCLK (OSC0 main oscillator)
            ercOrigin = osc0_osc_clockVar;
            break;
         case 1: // ERC = RTCCLK (OSC1 oscillator)
            ercOrigin = osc1_osc_clockVar;
            break;
         case 2: // ERC = IRC48MCLK (OSC2)
            ercOrigin = osc2_osc_clockVar;
            break;
         }
         ercPathDescription = " selected by mcg.c7.oscsel";
      }
      mcg_erc_clockVar.setValue(ercOrigin.getValueAsLong());
      mcg_erc_clockVar.setStatus(ercOrigin.getStatus());
      mcg_erc_clockVar.setOrigin(ercOrigin.getOrigin() + ercPathDescription);

      Variable mcg_c11_pllcsVar = safeGetVariable("mcg_c11_pllcs");
      boolean pllIsInternal = (mcg_c11_pllcsVar == null) || !mcg_c11_pllcsVar.getValueAsBoolean();

      // Main clock mode
      //===============================
      int     mcg_c1_clks;
      int     mcg_c6_plls;
      int     mcg_c2_lp;
      boolean mcg_c1_irefs;
      
      // Main clock mode
      //====================
      Variable clock_modeVar = getVariable("clock_mode");
      ClockMode clock_mode = ClockMode.valueOf(clock_modeVar.getSubstitutionValue());
      Variable fll_enabledVar                   = getVariable("fll_enabled");
      Variable fllInputFrequencyVar             = getVariable("fllInputFrequency");

      boolean mcg_c2_ircsVar_StatusWarning = false;
      
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
         // Add BLPE/BLPI warning
         mcg_c2_ircsVar_StatusWarning = !mcg_c2_ircsVar.getValueAsBoolean();
         break;
      case ClockMode_BLPE:
         mcg_c1_clks  = 2;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 1;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(false);
         // Add BLPE/BLPI warning
         mcg_c2_ircsVar_StatusWarning = !mcg_c2_ircsVar.getValueAsBoolean();
         break;
      case ClockMode_PBE:
         mcg_c1_clks  = 2;
         mcg_c6_plls  = 1;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         pll0EnabledVar.setValue(pllIsInternal||mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(false);
         break;
      case ClockMode_PEE:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 1;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("PLL output");
         pll0EnabledVar.setValue(pllIsInternal||mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(false);
         break;
      }     
      mcg_c1_clksVar.setValue(mcg_c1_clks);
      mcg_c6_pllsVar.setValue(mcg_c6_plls);
      mcg_c2_lpVar.setValue(mcg_c2_lp);
      mcg_c1_irefsVar.setValue(mcg_c1_irefs);
      if (mcg_c2_ircsVar_StatusWarning) {
         mcg_c2_ircsVar.setStatus(new Status("Fast IRC clock should be selected if entering VLPR mode", Severity.WARNING));
      }
      else {
         mcg_c2_ircsVar.clearStatus();
      }

      //=======================================
      // Find FLL dividers
      FllConfigure fllCheck = new FllConfigure(
            osc0_osc_cr_erclkenVar,
            osc0_oscillatorRangeVar,
            getVariable("mcg_c2_range"),
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

      // External PLLs
      //=================================================
      if (usb1pfdclk_ClockVar != null) {
         // Check USB HS PLL
         long irClockFreq = osc0_osc_clockVar.getValueAsLong();
         String failedPreCondition = null;
         if (!osc0_osc_cr_erclkenVar.getValueAsBoolean()) {
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

      pllConfigure.validate(mcg_erc_clockVar, pll0InputFrequencyVar, pll0OutputFrequencyVar, mcg_c5_prdiv0Var, mcg_c6_vdiv0Var);

      pll0OutputFrequencyVar.enable(pll0InputFrequencyVar.getFilteredStatus() == null);
      String pllToolTip = "Output of PLL. Available as MCGPLLCLK and used for MGCOUTCLK in PEE clock mode";
      if (pllIsInternal) {
         // Internal PLL
         system_mcgpllclk_clockVar.setOrigin(pll0OutputFrequencyVar.getOrigin());
         if (pll0EnabledVar.getValueAsBoolean()) {
            system_mcgpllclk_clockVar.enable(true);
            system_mcgpllclk_clockVar.setStatus(pll0OutputFrequencyVar.getFilteredStatus());
         }
         else {
            pllToolTip = "Unavailable in this clock mode unless mcg_c5_pllclken is selected";
            system_mcgpllclk_clockVar.enable(false);
            system_mcgpllclk_clockVar.setStatus(new Status("PLL clock unavailable", Severity.WARNING));
         }
         system_mcgpllclk_clockVar.setValue(pll0OutputFrequencyVar.getValueAsLong());
      }
      else {
         // External PLL (USB1 (HS) PHY)
         system_mcgpllclk_clockVar.enable(true);
         system_mcgpllclk_clockVar.setValue(usb1pfdclk_ClockVar.getValueAsLong());
         system_mcgpllclk_clockVar.setOrigin(usb1pfdclk_ClockVar.getOrigin());
         system_mcgpllclk_clockVar.setStatus(usb1pfdclk_ClockVar.getFilteredStatus());
      }
      system_mcgpllclk_clockVar.setToolTip(pllToolTip);

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
   
   @Override
   protected void createDependencies() throws Exception {

      // OSC Selection
      ArrayList<ChoiceData> mcg_c7_oscsel_entries = new ArrayList<ChoiceData>();
      
      //  MCG OSC0 input always exists
      osc0_peripheralName        = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      osc0_description           = getStringVariable("/SIM/osc0_description").getValueAsString();
      osc0_osc_clockVar          = getLongVariable(osc0_peripheralName+"/osc_clock");
      osc0_osc_cr_erclkenVar     = safeGetBooleanVariable(osc0_peripheralName+"/osc_cr_erclken");
      osc0_oscillatorRangeVar    = safeGetVariable(osc0_peripheralName+"/oscillatorRange");
      mcg_c7_oscsel_entries.add(new ChoiceData(osc0_description, "0"));
      String externalVariables0[] = {
            osc0_peripheralName+"/osc_clock",
            osc0_peripheralName+"/osc_cr_erclken",
            osc0_peripheralName+"/oscillatorRange",
      };
      addToWatchedVariables(externalVariables0);

      // MCG OSC1 input may exist
      StringVariable osc1_osc_clockNameVar = safeGetStringVariable("/SIM/osc1_clock");
      if (osc1_osc_clockNameVar != null) {
         String osc1_variableName = osc1_osc_clockNameVar.getValueAsString();
         osc1_osc_clockVar  = getLongVariable(osc1_variableName);
         osc1_description   = getStringVariable("/SIM/osc1_description").getValueAsString();
         mcg_c7_oscsel_entries.add(new ChoiceData(osc1_description, "1"));
         String externalVariables[] = {
               osc1_variableName, // OSC1 ~ RTC
         };
         addToWatchedVariables(externalVariables);
      }

      // MCG OSC2 input may exist
      StringVariable osc2_osc_clockNameVar  = safeGetStringVariable("/SIM/osc2_clock");
      if (osc2_osc_clockNameVar != null) {
         String osc2_variableName = osc2_osc_clockNameVar.getValueAsString();
         osc2_osc_clockVar  = getLongVariable(osc2_variableName);
         osc2_description   = getStringVariable("/SIM/osc2_description").getValueAsString();
         mcg_c7_oscsel_entries.add(new ChoiceData(osc2_description, "2"));
         String externalVariables[] = {
               osc2_variableName,
         };
         addToWatchedVariables(externalVariables);
      }

      if (osc2_osc_clockVar == null) {
         // MCG OSC2 input may exist as IRC48M
         osc2_osc_clockVar = safeGetLongVariable("system_irc48m_clock");
         if (osc2_osc_clockVar != null) {
            mcg_c7_oscsel_entries.add(new ChoiceData("IRC48M - fix-me", "2"));
            String externalVariables[] = {
                  "system_irc48m_clock",
            };
            addToWatchedVariables(externalVariables);
         }
      }

      usb1pfdclk_ClockVar = safeGetVariable("usb1pfdclk_Clock");
      if (usb1pfdclk_ClockVar != null) {
         String externalVariables[] = {
               "usb1pfdclk_Clock",
         };
         addToWatchedVariables(externalVariables);
      }

      for (fIndex=0; fIndex<fDimension; fIndex++) {
         if (fIndex == 0) {
            Variable enableClockConfigurationVar = getVariable("enableClockConfiguration");
            // Clock configuration 0 is always true to enable 1st clock configuration
            // Disable variable so user can't change it
            enableClockConfigurationVar.setValue(true);
            enableClockConfigurationVar.setDisabledValue(true);
            enableClockConfigurationVar.enable(false);
            enableClockConfigurationVar.setToolTip("Clock configuration 0 must always be enabled");
            enableClockConfigurationVar.setDerived(true);
         }
         ChoiceVariable mcg_c7_oscselVar = safeGetChoiceVariable("mcg_c7_oscsel");
         if (mcg_c7_oscselVar != null) {
            mcg_c7_oscselVar.setData(mcg_c7_oscsel_entries);
            mcg_c7_oscselVar.setValue(mcg_c7_oscsel_entries.get(0).name);
         }
      }
      fIndex = 0;
   }
}
