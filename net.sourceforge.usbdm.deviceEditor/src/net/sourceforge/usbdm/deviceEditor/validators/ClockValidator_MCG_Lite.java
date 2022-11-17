package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

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
public class ClockValidator_MCG_Lite extends BaseClockValidator {

   String         osc0_peripheralName       = null;
   String         osc0_description          = null;
   LongVariable   osc0_osc_clockVar         = null;
   Variable       osc0_osc_cr_erclkenVar    = null;
   Variable       osc0_oscillatorRangeVar   = null;

   public ClockValidator_MCG_Lite(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);
   }

   /**
    * Inputs
    *    /OSC0/osc_clock
    *    /OSC0/oscillatorRange
    * 
    * Outputs
    *    system_slow_irc_clock
    *    system_fast_irc_clock
    *    system_mcgirclk_clock
    *    system_mcgoutclk_clock
    *    system_mcgpclk_clock
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

      // C1
      //=================================
      Variable mcg_c1_clksVar                   = getVariable("mcg_c1_clks");
      Variable mcg_c1_irclkenVar                = getVariable("mcg_c1_irclken");
      Variable mcg_c1_irefstenVar               = getVariable("mcg_c1_irefsten");
                                       
      // C2
      //=================================
      Variable mcg_c2_range0Var                 = safeGetVariable("mcg_c2_range0");
      Variable mcg_c2_ircsVar                   = getVariable("mcg_c2_ircs");
                                       
      // SC
      //=================================
      Variable mcg_sc_fcrdivVar        = safeGetVariable("mcg_sc_fcrdiv");
                                       
      // MC
      //=================================
      Variable mcg_mc_hircenVar        = safeGetVariable("mcg_mc_hircen");
      Variable mcg_mc_hirclpenVar      = safeGetVariable("mcg_mc_hirclpen");
      Variable mcg_mc_lirc_div2Var     = safeGetVariable("mcg_mc_lirc_div2");
                                       
      // LIRC
      //=================================
      Variable system_slow_irc_clockVar  = getVariable("system_slow_irc_clock");
      Variable system_fast_irc_clockVar  = getVariable("system_fast_irc_clock");
      Variable system_lirc_clockVar      = getVariable("system_lirc_clock");
      Variable system_lirc_div1_clockVar = getVariable("system_lirc_div1_clock");
      Variable system_mcgirclk_clockVar  = getVariable("system_mcgirclk_clock");
                                       
      // Internal (HIRC)
      //=================================
      Variable system_irc48m_clockVar  = safeGetVariable("system_irc48m_clock");
                                       
      //=================================
      Variable system_mcgoutclk_clock_sourceVar = getVariable("system_mcgoutclk_clock_source");
      Variable system_mcgoutclk_clockVar        = getVariable("system_mcgoutclk_clock");
      Variable system_mcgpclk_clockVar          = getVariable("system_mcgpclk_clock");
      
      if (mcg_c2_range0Var != null) {
         long rangeIn = osc0_oscillatorRangeVar.getValueAsLong();
         if (rangeIn != OscValidate.UNCONSTRAINED_RANGE) {
            mcg_c2_range0Var.enable(true);
            mcg_c2_range0Var.setValue(osc0_oscillatorRangeVar.getValueAsLong());
         }
         else {
            mcg_c2_range0Var.enable(false);
         }
      }
      
      // Main clock mode
      //====================
      ChoiceVariable mcgClockModeVar   = getChoiceVariable("mcgClockMode");
      McgClockMode   mcgClockMode        = McgClockMode.valueOf(mcgClockModeVar.getEnumValue());
      
      // Run mode
      ChoiceVariable smc_pmctrl_runmVar = getChoiceVariable("/SMC/smc_pmctrl_runm");
      SmcRunMode     smcRunMode         = SmcRunMode.valueOf(smc_pmctrl_runmVar.getEnumValue());

      switch (mcgClockMode) {
      default:
      case McgClockMode_HIRC_48MHz:
         mcg_c1_clksVar.setValue(0);
         mcg_c2_ircsVar.setLocked(false);

         system_mcgoutclk_clockVar.setValue(system_irc48m_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_irc48m_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus((Status)null);
         system_mcgoutclk_clock_sourceVar.setValue("HIRC 48M (IRCLK48MCLK)");
         break;

      case McgClockMode_LIRC_2MHz:
         mcg_c1_clksVar.setValue(1);
         mcg_c2_ircsVar.setValue(0);
         mcg_c2_ircsVar.setLocked(true);

         system_mcgoutclk_clockVar.setValue(system_lirc_div1_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_lirc_div1_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus((Status)null);
         system_mcgoutclk_clock_sourceVar.setValue("LIRC2");
         break;

      case McgClockMode_LIRC_8MHz:
         mcg_c1_clksVar.setValue(1);
         mcg_c2_ircsVar.setValue(1);
         mcg_c2_ircsVar.setLocked(true);

         system_mcgoutclk_clockVar.setValue(system_lirc_div1_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(system_lirc_div1_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus((Status)null);
         system_mcgoutclk_clock_sourceVar.setValue("LIRC8");
         break;

      case McgClockMode_EXT:
         mcg_c1_clksVar.setValue(2);
         mcg_c2_ircsVar.setLocked(false);

         system_mcgoutclk_clockVar.setValue(osc0_osc_clockVar.getValueAsLong());
         system_mcgoutclk_clockVar.setOrigin(osc0_osc_clockVar.getOrigin());
         system_mcgoutclk_clockVar.setStatus((Status)null);
         system_mcgoutclk_clock_sourceVar.setValue("External Clock (OSCCLK)");
         break;
      }
      system_mcgoutclk_clock_sourceVar.setStatus(system_mcgoutclk_clockVar.getStatus());
      
      // HIRC related clocks
      //========================================================================
      
      // HIRC is available is available in non-low-power modes when in HIRC clock mode or explicitly enabled by HIRCEN
      boolean mcgpclkIsAvailable =
            ((smcRunMode != SmcRunMode.SmcRunMode_VeryLowPower) &&
             (mcgClockMode == McgClockMode.McgClockMode_HIRC_48MHz) || (mcg_mc_hircenVar.getValueAsBoolean()) );
      
      if (mcg_mc_hirclpenVar != null) {
         // Can also can be enable in low power mode if HIRCLPEN exists
         mcgpclkIsAvailable = mcgpclkIsAvailable ||
               ((smcRunMode != SmcRunMode.SmcRunMode_VeryLowPower) &&
                (mcgClockMode == McgClockMode.McgClockMode_HIRC_48MHz) || (mcg_mc_hirclpenVar.getValueAsBoolean()) );
      }
      if (mcgpclkIsAvailable) {
         // HIRC Enabled
         system_mcgpclk_clockVar.setValue(system_irc48m_clockVar.getValueAsLong());
         system_mcgpclk_clockVar.enable(true);
         system_mcgpclk_clockVar.setStatus((Status)null);
      }
      else {
         // HIRC Disabled
         system_mcgpclk_clockVar.enable(false);
         system_mcgpclk_clockVar.setStatus(new Status("Disabled in this clock mode", Severity.WARNING));
      }

      // LIRC related clocks
      //========================================
      if (mcg_c1_irclkenVar.getValueAsBoolean() ||
            (mcgClockMode == McgClockMode.McgClockMode_LIRC_2MHz) || (mcgClockMode == McgClockMode.McgClockMode_LIRC_8MHz) ) {
         // LIRC Enabled
         mcg_c1_irefstenVar.enable(true);
         system_lirc_clockVar.enable(true);
         system_lirc_clockVar.setStatus((Status)null);
         if (mcg_c2_ircsVar.getValueAsBoolean()) {
            // Fast IRC selected
            system_lirc_clockVar.setValue(system_fast_irc_clockVar.getValueAsLong());
            system_lirc_clockVar.setOrigin(system_fast_irc_clockVar.getOrigin());
         }
         else {
            // Slow IRC selected
            system_lirc_clockVar.setValue(system_slow_irc_clockVar.getValueAsLong());
            system_lirc_clockVar.setOrigin(system_fast_irc_clockVar.getOrigin());
         }
         mcg_sc_fcrdivVar.enable(true);
         system_lirc_div1_clockVar.enable(true);
         mcg_mc_lirc_div2Var.enable(true);
         system_mcgirclk_clockVar.enable(true);
         system_mcgirclk_clockVar.setStatus((Status)null);
      }
      else {
         // LIRC Disabled
         mcg_c1_irefstenVar.enable(false);
         system_lirc_clockVar.enable(false);
         system_lirc_clockVar.setStatus(new Status("Disabled by mcg_c1_irclken", Severity.WARNING));
         mcg_sc_fcrdivVar.enable(false);
         system_lirc_div1_clockVar.enable(false);
         mcg_mc_lirc_div2Var.enable(false);
         system_mcgirclk_clockVar.enable(false);
         system_mcgirclk_clockVar.setStatus(new Status("Disabled by mcg_c1_irclken", Severity.WARNING));
      }

      long mcg_sc_fcrdiv = mcg_sc_fcrdivVar.getValueAsLong();
      system_lirc_div1_clockVar.setValue(system_lirc_clockVar.getValueAsLong()/(1<<mcg_sc_fcrdiv));
      system_lirc_div1_clockVar.setOrigin(system_lirc_clockVar.getOrigin()+"/LIRC_DIV1");

      long mcg_mc_lirc_div2 = mcg_mc_lirc_div2Var.getValueAsLong();
      system_mcgirclk_clockVar.setValue(system_lirc_div1_clockVar.getValueAsLong()/(1<<mcg_mc_lirc_div2));
      system_mcgirclk_clockVar.setOrigin(system_lirc_div1_clockVar.getOrigin()+"/LIRC_DIV2");
   }
   
   @Override
   protected void createDependencies() throws Exception {
      super.createDependencies();

      //  MCG OSC0 input always exists
      osc0_peripheralName        = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      osc0_description           = getStringVariable("/SIM/osc0_description").getValueAsString();
      osc0_osc_clockVar          = getLongVariable(osc0_peripheralName+"/osc_clock");
      osc0_osc_cr_erclkenVar     = safeGetBooleanVariable(osc0_peripheralName+"/osc_cr_erclken");
      osc0_oscillatorRangeVar    = safeGetVariable(osc0_peripheralName+"/oscillatorRange");
      String externalVariables0[] = {
            osc0_peripheralName+"/osc_clock",
            osc0_peripheralName+"/osc_cr_erclken",
            osc0_peripheralName+"/oscillatorRange",
      };
      addToWatchedVariables(externalVariables0);
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
      }
      fIndex = 0;
   }
}
