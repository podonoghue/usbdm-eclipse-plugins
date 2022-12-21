package net.sourceforge.usbdm.deviceEditor.validators;

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
public class ClockValidator_MCG_Lite extends IndexedValidator {

   Variable       osc0_oscillatorRangeVar   = null;

   public ClockValidator_MCG_Lite(PeripheralWithState peripheral, Integer dimension) {
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
      clockConfig.setStatus(isValidCIdentifier(clockConfig.getValueAsString())?(String)null:"Illegal C enum name");

      // Enable whole category from clock enable variable
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration");
      clockConfig.enable(enableClockConfigurationVar.getValueAsBoolean());

      // C1
      //=================================
      Variable mcg_c1_clksVar            = getVariable("mcg_c1_clks");
                                       
      // C2
      //=================================
      Variable mcg_c2_range0Var          = safeGetVariable("mcg_c2_range0");
      Variable mcg_c2_ircsVar            = getVariable("mcg_c2_ircs");
                                     
      // MC
      //=================================
      Variable mcg_mc_hircenVar          = safeGetVariable("mcg_mc_hircen");
      Variable mcg_mc_hirclpenVar        = safeGetVariable("mcg_mc_hirclpen");
                                       
      // Internal (HIRC)
      //=================================
      Variable system_irc48m_clockVar  = safeGetVariable("/SIM/system_irc48m_clock");
                                       
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
      ChoiceVariable mcgClockModeVar    = getChoiceVariable("mcgClockMode");
      McgClockMode   mcgClockMode       = McgClockMode.valueOf(mcgClockModeVar.getEnumValue());
      
      // Run mode
      ChoiceVariable smc_pmctrl_runmVar = getChoiceVariable("/SMC/smc_pmctrl_runm");
      SmcRunMode     smcRunMode         = SmcRunMode.valueOf(smc_pmctrl_runmVar.getEnumValue());

      switch (mcgClockMode) {
      default:
      case McgClockMode_HIRC_48MHz:
         mcg_c1_clksVar.setValue(0);
         mcg_c2_ircsVar.setLocked(false);
         break;

      case McgClockMode_LIRC_2MHz:
         mcg_c1_clksVar.setValue(1);
         mcg_c2_ircsVar.setValue(0);
         mcg_c2_ircsVar.setLocked(true);
         break;

      case McgClockMode_LIRC_8MHz:
         mcg_c1_clksVar.setValue(1);
         mcg_c2_ircsVar.setValue(1);
         mcg_c2_ircsVar.setLocked(true);
         break;

      case McgClockMode_EXT:
         mcg_c1_clksVar.setValue(2);
         mcg_c2_ircsVar.setLocked(false);
         break;
      }
      
      // HIRC related clocks
      //========================================================================
      /*
       * POWER  MODE   HIRCEN HIRCLPEN
       * !VLPR  HIRC      X      X      => Enabled
       * !VLPR   X        T      X      => Enabled
       *  VLPR   -        T      T      => Enabled
       */
      
      // HIRC is available in non-low-power modes when in HIRC clock mode or explicitly enabled by HIRCEN
      boolean mcgpclkIsAvailable =
            ((smcRunMode != SmcRunMode.SmcRunMode_VeryLowPower) &&
             ((mcgClockMode == McgClockMode.McgClockMode_HIRC_48MHz) || (mcg_mc_hircenVar.getValueAsBoolean())) );
      
      if (mcg_mc_hirclpenVar != null) {
         // Can also can be enable in low power mode if HIRCLPEN exists
         // Note can't be in HIRC mode so clock mode is not relevant
         mcgpclkIsAvailable = mcgpclkIsAvailable ||
               ((smcRunMode == SmcRunMode.SmcRunMode_VeryLowPower) &&
                (mcg_mc_hircenVar.getValueAsBoolean()) &&
                (mcg_mc_hirclpenVar.getValueAsBoolean()));
      }
      if (mcgpclkIsAvailable) {
         // HIRC Enabled
         system_irc48m_clockVar.enable(true);
         system_irc48m_clockVar.setStatus((Status)null);
      }
      else {
         // HIRC Disabled
         system_irc48m_clockVar.enable(false);
         system_irc48m_clockVar.setStatus(new Status("Disabled in this clock mode", Severity.WARNING));
      }
   }
    
   @Override
   protected boolean createDependencies() throws Exception {
      super.createDependencies();

      //  MCG OSC0 input always exists
      String osc0_peripheralName;

      osc0_peripheralName        = getStringVariable("/SIM/osc0_peripheral").getValueAsString();
      osc0_oscillatorRangeVar    = safeGetVariable(osc0_peripheralName+"/oscillatorRange");
      final String watchedVariables0[] = {
            "ClockConfig",
            "mcgClockMode",
            osc0_peripheralName+"/oscillatorRange",
            "mcg_mc_hircen",
            "mcg_mc_hirclpen",
            "/SMC/smc_pmctrl_runm",
      };
      addSpecificWatchedVariables(watchedVariables0);

      // Hide from user
      Variable enableClockConfigurationVar = getVariable("enableClockConfiguration[0]");
      enableClockConfigurationVar.setHidden(true);
      
      return false;
   }
}
