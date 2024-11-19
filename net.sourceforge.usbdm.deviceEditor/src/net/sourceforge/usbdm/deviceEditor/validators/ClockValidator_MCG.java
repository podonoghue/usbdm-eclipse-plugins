package net.sourceforge.usbdm.deviceEditor.validators;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine main MCG settings

 * Used for:
 *     mcg_mk_ics48mml
 *     mcg_mk
 */
public class ClockValidator_MCG extends IndexedValidator {

   private BooleanVariable allowUserClockTrimVar     = null;

   public ClockValidator_MCG(PeripheralWithState peripheral, Integer dimension) {
      super(peripheral, dimension);
   }

   /**
    * 
    * @throws Exception
    */
   @Override
   protected void validate(Variable variable, int properties, int index) throws Exception {
      //      System.err.println(getSimpleClassName()+" "+variable +", Index ="+index);

      if ((allowUserClockTrimVar != null)&&(variable == allowUserClockTrimVar)) {
         Boolean allowUserClockTrim = allowUserClockTrimVar.getValueAsBoolean();

         LongVariable system_slow_irc_clockVar = getLongVariable("system_slow_irc_clock[]");
         LongVariable system_fast_irc_clockVar = getLongVariable("system_fast_irc_clock[]");
         
         system_slow_irc_clockVar.setLocked(!allowUserClockTrim);
         system_fast_irc_clockVar.setLocked(!allowUserClockTrim);
         if (!allowUserClockTrim) {
            system_slow_irc_clockVar.setValue(system_slow_irc_clockVar.getDefault());
            system_fast_irc_clockVar.setValue(system_fast_irc_clockVar.getDefault());
         }
         return;
      }
      
      // Check configuration name is valid C identifier
      StringVariable clockConfig = getStringVariable("ClockConfig[]");
      clockConfig.setStatus(isValidCIdentifier(clockConfig.getValueAsString())?(String)null:"Illegal C enum value");

      // Enable whole category from clock enable variable
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration[]");
      clockConfig.enable(enableClockConfigurationVar.getValueAsBoolean());
      
      //=================================

      Variable pll0EnabledVar                   = getVariable("pll0Enabled[]");
      Variable pll0InputFrequencyVar            = getVariable("pll0InputFrequency[]");
      Variable pll0OutputFrequencyVar           = getVariable("pll0OutputFrequency[]");
      Variable mcg_c5_pllclken0Var              = getVariable("mcg_c5_pllclken0[]");

      Variable mcg_c6_pllsVar                   = getVariable("mcg_c6_plls[]");

      Variable mcg_c1_irefsVar                  = getVariable("mcg_c1_irefs[]");
      Variable mcg_c1_clksVar                   = getVariable("mcg_c1_clks[]");
      Variable mcg_c2_lpVar                     = getVariable("mcg_c2_lp[]");

      // Main clock mode
      //===============================
      int     mcg_c1_clks;
      int     mcg_c6_plls;
      int     mcg_c2_lp;
      boolean mcg_c1_irefs = false;

      // Main clock mode
      //====================
      ChoiceVariable mcgClockModeVar   = getChoiceVariable("mcgClockMode[]");
      McgClockMode   clock_mode        = McgClockMode.valueOf(mcgClockModeVar.getEnumValue());

      Variable   fll_enabledVar        = getVariable("fll_enabled[]");
      Variable   fllInputFrequencyVar  = getVariable("fllInputFrequency[]");

      Variable mcg_c11_pllcsVar = safeGetVariable("mcg_c11_pllcs[]");
      boolean pllIsInternal = (mcg_c11_pllcsVar == null) || !mcg_c11_pllcsVar.getValueAsBoolean();

      boolean pllEnabled = mcg_c5_pllclken0Var.getValueAsBoolean();
      boolean fllEnable  = false;

      switch (clock_mode) {
      default:
      case McgClockMode_FEI:
         mcg_c1_irefs = true; //* Required
         mcg_c6_plls  = 0;    //* Required
         mcg_c1_clks  = 0;    //* Required
         mcg_c2_lp    = 0;
         mcg_c5_pllclken0Var.enable(true);
         fllEnable    = true;
         break;
      case McgClockMode_FEE:
         mcg_c1_irefs = false; //* Required
         mcg_c6_plls  = 0;     //* Required
         mcg_c1_clks  = 0;     //* Required
         mcg_c2_lp    = 0;
         mcg_c5_pllclken0Var.enable(true);
         fllEnable    = true;
         break;
      case McgClockMode_FBI:
         mcg_c1_irefs = true; //* Required
         mcg_c6_plls  = 0;    //* Required
         mcg_c1_clks  = 1;    //* Required
         mcg_c2_lp    = 0;    //* Required
         mcg_c5_pllclken0Var.enable(true);
         fllEnable    = true;
         break;
      case McgClockMode_FBE:
         mcg_c1_irefs = false; //* Required
         mcg_c6_plls  = 0;     //* Required
         mcg_c1_clks  = 2;     //* Required
         mcg_c2_lp    = 0;     //* Required
         mcg_c5_pllclken0Var.enable(true);
         fllEnable    = true;
         break;
      case McgClockMode_BLPI:
         mcg_c1_irefs = true;  //* Required
         mcg_c6_plls  = 0;     //* Required
         mcg_c1_clks  = 1;     //* Required
         mcg_c2_lp    = 1;     //* Required
         pllEnabled   = false;
         mcg_c5_pllclken0Var.enable(false);
         break;
      case McgClockMode_BLPE:
         mcg_c1_irefs = false; //* Required
         mcg_c6_plls  = 0;
         mcg_c1_clks  = 2;     //* Required
         mcg_c2_lp    = 1;     //* Required
         pllEnabled   = false;
         mcg_c5_pllclken0Var.enable(false);
         break;
      case McgClockMode_PBE:
         mcg_c1_irefs = false; //* Required
         mcg_c6_plls  = 1;     //* Required
         mcg_c1_clks  = 2;     //* Required
         mcg_c2_lp    = 0;     //* Required
         mcg_c5_pllclken0Var.enable(false);
         pllEnabled = pllEnabled || pllIsInternal;
         break;
      case McgClockMode_PEE:
         mcg_c1_irefs = false; //* Required
         mcg_c6_plls  = 1;     //* Required
         mcg_c1_clks  = 0;     //* Required
         mcg_c2_lp    = 0;
         mcg_c5_pllclken0Var.enable(false);
         pllEnabled = pllEnabled || pllIsInternal;
         break;
      }
      pll0EnabledVar.setValue(pllEnabled);
      fll_enabledVar.setValue(fllEnable);

      mcg_c1_clksVar.setValue(mcg_c1_clks);
      mcg_c6_pllsVar.setValue(mcg_c6_plls);
      mcg_c2_lpVar.setValue(mcg_c2_lp);
      mcg_c1_irefsVar.setValue(mcg_c1_irefs);
      
      // Determine MCG external reference clock [mcg_erc_clock]
      //========================================================
      fllInputFrequencyVar.enable(fllEnable);

      pll0InputFrequencyVar.enable(pllEnabled);
      pll0OutputFrequencyVar.enable(pllEnabled);
   }

   @Override
   protected boolean createDependencies() throws Exception {
 
      final String watchedVariables[] = {
            "allowUserClockTrim",
            "system_slow_irc_clock[]",
            "system_fast_irc_clock[]",
            "ClockConfig[]",
            "mcgClockMode[]",
            "mcg_erc_clock[]",
            "enableClockConfiguration[]",
            "mcg_c6_plls[]",
            "mcg_c5_pllclken0[]",
            "mcg_c11_pllcs[]",
      };
      addSpecificWatchedVariables(watchedVariables);
      
      // Hide from user
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration[0]");
      enableClockConfigurationVar.setHidden(true);
      
      allowUserClockTrimVar = safeGetBooleanVariable("allowUserClockTrim");

      // Don't add default dependencies
      return false;
   }
}
