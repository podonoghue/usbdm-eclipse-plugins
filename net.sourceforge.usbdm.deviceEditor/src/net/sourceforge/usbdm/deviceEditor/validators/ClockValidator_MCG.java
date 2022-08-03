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

//   String         osc0_peripheralName       = null;
//   String         osc0_description          = null;
//   LongVariable   osc0_osc_clockVar         = null;
   Variable       osc0_osc_cr_erclkenVar    = null;
   Variable       osc0_oscillatorRangeVar   = null;

//   LongVariable   osc1_osc_clockVar         = null;
//   String         osc1_description          = null;
//
//   LongVariable   osc2_osc_clockVar         = null;
//   String         osc2_description          = null;

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

      Variable pll0EnabledVar                   = getVariable("pll0Enabled");
      Variable pll0InputFrequencyVar            = getVariable("pll0InputFrequency");
      Variable pll0OutputFrequencyVar           = getVariable("pll0OutputFrequency");
      Variable mcg_c5_pllclkenVar               = getVariable("mcg_c5_pllclken");
      Variable mcg_c5_prdiv0Var                 = getVariable("mcg_c5_prdiv0");
      Variable mcg_c6_vdiv0Var                  = getVariable("mcg_c6_vdiv0");

      Variable system_mcgpllclk_clockVar        = getVariable("system_mcgpllclk_clock");
      Variable mcg_c6_pllsVar                   = getVariable("mcg_c6_plls");

      Variable system_mcgoutclk_clock_sourceVar = getVariable("system_mcgoutclk_clock_source");
      Variable system_mcgoutclk_clockVar        = getVariable("system_mcgoutclk_clock");

      Variable mcg_c1_irefsVar                  = getVariable("mcg_c1_irefs");
      Variable mcg_c1_clksVar                   = getVariable("mcg_c1_clks");
      Variable mcg_c2_lpVar                     = getVariable("mcg_c2_lp");
      
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
      ChoiceVariable mcgClockModeVar   = getChoiceVariable("mcgClockMode");
      McgClockMode   clock_mode        = McgClockMode.valueOf(mcgClockModeVar.getEnumValue());
      
      Variable   fll_enabledVar        = getVariable("fll_enabled");
      Variable   fllInputFrequencyVar  = getVariable("fllInputFrequency");

      boolean mcg_c2_ircsVar_StatusWarning = false;
      Variable mcg_c2_ircsVar             = getVariable("mcg_c2_ircs");

      switch (clock_mode) {
      default:
      case McgClockMode_None:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_FEI:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_FEE:
         mcg_c1_clks  = 0;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("FLL output");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_FBI:
         mcg_c1_clks  = 1;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = true;
         system_mcgoutclk_clock_sourceVar.setValue("MCGIRCLK");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_FBE:
         mcg_c1_clks  = 2;
         mcg_c6_plls  = 0;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         pll0EnabledVar.setValue(mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(true);
         break;
      case McgClockMode_BLPI:
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
      case McgClockMode_BLPE:
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
      case McgClockMode_PBE:
         mcg_c1_clks  = 2;
         mcg_c6_plls  = 1;
         mcg_c2_lp    = 0;
         mcg_c1_irefs = false;
         system_mcgoutclk_clock_sourceVar.setValue("MCGERCLK");
         pll0EnabledVar.setValue(pllIsInternal||mcg_c5_pllclkenVar.getValueAsBoolean());
         fll_enabledVar.setValue(false);
         break;
      case McgClockMode_PEE:
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

      // Determine MCG external reference clock [mcg_erc_clock]
      //========================================================
      ChoiceVariable mcg_c7_oscselVar = safeGetChoiceVariable("/SIM/mcg_c7_oscsel");
      Variable       mcg_erc_clockVar = getVariable("/SIM/mcg_erc_clock");

      Variable system_slow_irc_clockVar   = getVariable("system_slow_irc_clock");

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
//      if (usb1pfdclk_ClockVar != null) {
//         // Check USB HS PLL
//         long irClockFreq = osc0_osc_clockVar.getValueAsLong();
//         String failedPreCondition = null;
//         if (!osc0_osc_cr_erclkenVar.getValueAsBoolean()) {
//            failedPreCondition = "Disabled: to use PFDCLK, OSCCLK clock must be enabled by osc_cr_erclkenVar";
//         }
//         else if (!mcg_c1_irclkenVar.getValueAsBoolean()) {
//            failedPreCondition = "Disabled: to use PFDCLK, IRC clock must be enabled by mcg_c1_irclken";
//         }
//         else if ((irClockFreq!=12000000)&&(irClockFreq!=16000000)&&(irClockFreq!=24000000)) {
//            failedPreCondition = "Disabled: to use PFDCLK, OSCCLK must be in [12Mhz, 16MHz, 24MHz]";
//         }
//         if (failedPreCondition==null) {
//            usb1pfdclk_ClockVar.enable(true);
//            usb1pfdclk_ClockVar.setOrigin("Clock from USB HS PLL"); 
//            usb1pfdclk_ClockVar.setStatus((Status)null);
//         }
//         else {
//            usb1pfdclk_ClockVar.enable(false);
//            usb1pfdclk_ClockVar.setOrigin("Clock from USB HS PLL (disabled)"); 
//            usb1pfdclk_ClockVar.setStatus(new Status(failedPreCondition, Severity.WARNING));
//         }
//      }
      
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
      Variable system_mcgirclk_ungated_clock = getVariable("system_mcgirclk_ungated");

      switch (clock_mode) {
      default:
      case McgClockMode_None:
         system_mcgoutclk_clockVar.setValue(system_mcgfllclk_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_mcgfllclk_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus((Status)null);
         clock_mode_Status = new Status("No clock settings are applied", Severity.WARNING);
         break;
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
      case McgClockMode_PBE:
         system_mcgoutclk_clockVar.setValue(mcg_erc_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(mcg_erc_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus(mcg_erc_clockVar.getFilteredStatus());
         break;
      case McgClockMode_PEE:
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
      super.createDependencies();
      
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      // mcg_erc_clock is the main clock input to MCG (maintained by SIM)
      variablesToWatch.add("/SIM/mcg_erc_clock");

      // mcg_erc Selection
      ChoiceVariable mcg_c7_oscselVar = safeGetChoiceVariable("/SIM/mcg_c7_oscsel");
      ChoiceData[] choiceData = mcg_c7_oscselVar.getData();
      
      //  mcg_erc[0] = OSC0, input must always exists
      int index = choiceData[0].getReference().lastIndexOf("/");
      String osc0Name = choiceData[0].getReference().substring(0, index);
      osc0_osc_cr_erclkenVar     = getBooleanVariable(osc0Name+"/osc_cr_erclken");
      osc0_oscillatorRangeVar    = getVariable(osc0Name+"/oscillatorRange");

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
