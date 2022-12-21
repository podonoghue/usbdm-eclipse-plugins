package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.StringVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine main MCG settings

 * Used for:
 *     mcg_mk_ics48mml
 *     mcg_mk
 */
public class ClockValidator_MCG_no_pll extends IndexedValidator {

   public ClockValidator_MCG_no_pll(PeripheralWithState peripheral,  Integer dimension) {
      super(peripheral, dimension);
   }

   /**
    * 
    * @throws Exception
    */
   @Override
   protected void validate(Variable variable, int index) throws Exception {
      //      System.err.println(getSimpleClassName()+" "+variable +", Index ="+index);

      // Check configuration name is valid C identifier
      StringVariable clockConfig = getStringVariable("ClockConfig");
      clockConfig.setStatus(isValidCIdentifier(clockConfig.getValueAsString())?(String)null:"Illegal C enum value");

      // Enable whole category from clock enable variable
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration");
      clockConfig.enable(enableClockConfigurationVar.getValueAsBoolean());

      //=================================

      Variable mcg_c1_irefsVar                  = getVariable("mcg_c1_irefs");
      Variable mcg_c1_clksVar                   = getVariable("mcg_c1_clks");
      Variable mcg_c2_lpVar                     = getVariable("mcg_c2_lp");

      // Main clock mode
      //===============================
      int     mcg_c1_clks;
      int     mcg_c2_lp;
      boolean mcg_c1_irefs = false;

      // Main clock mode
      //====================
      ChoiceVariable mcgClockModeVar   = getChoiceVariable("mcgClockMode");
      McgClockMode   clock_mode        = McgClockMode.valueOf(mcgClockModeVar.getEnumValue());

      Variable   fll_enabledVar        = getVariable("fll_enabled");
      Variable   fllInputFrequencyVar  = getVariable("fllInputFrequency");

      boolean mcg_c2_ircsVar_StatusWarning = false;
      Variable mcg_c2_ircsVar              = getVariable("mcg_c2_ircs");

      boolean fllEnable  = false;

      switch (clock_mode) {
      default:
      case McgClockMode_FEI:
         mcg_c1_irefs = true; //* Required
         mcg_c1_clks  = 0;    //* Required
         mcg_c2_lp    = 0;
         fllEnable    = true;
         break;
      case McgClockMode_FEE:
         mcg_c1_irefs = false; //* Required
         mcg_c1_clks  = 0;     //* Required
         mcg_c2_lp    = 0;
         fllEnable    = true;
         break;
      case McgClockMode_FBI:
         mcg_c1_irefs = true; //* Required
         mcg_c1_clks  = 1;    //* Required
         mcg_c2_lp    = 0;    //* Required
         fllEnable    = true;
         break;
      case McgClockMode_FBE:
         mcg_c1_irefs = false; //* Required
         mcg_c1_clks  = 2;     //* Required
         mcg_c2_lp    = 0;     //* Required
         fllEnable    = true;
         break;
      case McgClockMode_BLPI:
         mcg_c1_irefs = true;  //* Required
         mcg_c1_clks  = 1;     //* Required
         mcg_c2_lp    = 1;     //* Required
         // Add BLPE/BLPI warning
         mcg_c2_ircsVar_StatusWarning = mcg_c2_ircsVar.getValueAsLong() == 0;
         break;
      case McgClockMode_BLPE:
         mcg_c1_irefs = false; //* Required
         mcg_c1_clks  = 2;     //* Required
         mcg_c2_lp    = 1;     //* Required
         // Add BLPE/BLPI warning
         mcg_c2_ircsVar_StatusWarning = mcg_c2_ircsVar.getValueAsLong() == 0;
         break;
      }
      fll_enabledVar.setValue(fllEnable);

      mcg_c1_clksVar.setValue(mcg_c1_clks);
      mcg_c2_lpVar.setValue(mcg_c2_lp);
      mcg_c1_irefsVar.setValue(mcg_c1_irefs);
      
      if (mcg_c2_ircsVar_StatusWarning) {
         mcg_c2_ircsVar.setStatus(new Status("Fast IRC clock should be selected if entering VLPR mode", Severity.WARNING));
      }
      else {
         mcg_c2_ircsVar.clearStatus();
      }

      fllInputFrequencyVar.enable(fllEnable);

   }

   @Override
   protected boolean createDependencies() throws Exception {
      super.createDependencies();
 
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      variablesToWatch.add("ClockConfig");
      variablesToWatch.add("enableClockConfiguration");
      variablesToWatch.add("mcgClockMode");
      variablesToWatch.add("mcg_c6_plls");
      variablesToWatch.add("mcg_c5_pllclken0");
      variablesToWatch.add("mcg_c11_pllcs");
      
      // mcg_erc_clock is the main clock input to MCG
      variablesToWatch.add("mcg_erc_clock");

      addSpecificWatchedVariables(variablesToWatch);

      // Hide from user
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration[0]");
      enableClockConfigurationVar.setHidden(true);
      
      return false;
   }
}
