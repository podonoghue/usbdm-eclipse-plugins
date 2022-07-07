package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.CategoryVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
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
public class ClockValidator_SCG extends BaseClockValidator {

   private boolean initialisationDone = false;
   
   long pll_mult_min;
   long pll_mult_max;
   long pll_prediv_min;
   long pll_prediv_max;
   long pll_post_divider;
   long pll_in_min;
   long pll_in_max;
   long pll_after_prediv_min;
   long pll_after_prediv_max;
   long pll_output_min;
   long pll_output_max;
   
   long osc_Low_min_frequency;    
   long osc_medium_min_frequency; 
   long osc_high_min_frequency;   
   long osc_high_max_frequency;   
   
   long vlpr_mode_max_core_frequency;   
   long vlpr_mode_max_bus_frequency;    
   long vlpr_mode_max_flash_frequency;   
   long run_mode_max_core_frequency;    
   long run_mode_max_bus_frequency;     
   long run_mode_max_flash_frequency;    
   long hsrun_mode_max_core_frequency;  
   long hsrun_mode_max_bus_frequency;   
   long hsrun_mode_max_flash_frequency; 

   public ClockValidator_SCG(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);
   }

   /**
    * Calculate divided peripheral clock from source clock frequency and divider value.
    *  
    * @param spll_clock   Source clock frequency
    * @param divider          Divider value
    * @return
    */
   double calculatePeripheralClock(double spll_clock, long divider) {
      if (divider <= 0) {
         return 0;
      }
      return spll_clock/(1<<(divider-1));
   }
   
   /**
    *    
    * @throws Exception 
    */
   @Override
   protected void validate(Variable variable) throws Exception {
      for (int index=0; index<fDimension; index++) {
         fIndex = index;
         if (!initialisationDone) {
            // Add SCG parameters
            pll_mult_min                   = getLongVariable("pll_mult_min").getValueAsLong();
            pll_mult_max                   = getLongVariable("pll_mult_max").getValueAsLong();
            pll_prediv_min                 = getLongVariable("pll_prediv_min").getValueAsLong();
            pll_prediv_max                 = getLongVariable("pll_prediv_max").getValueAsLong();
            pll_post_divider               = getLongVariable("pll_post_divider").getValueAsLong();
            pll_in_min                     = getLongVariable("pll_in_min").getValueAsLong();
            pll_in_max                     = getLongVariable("pll_in_max").getValueAsLong();
            pll_after_prediv_min           = getLongVariable("pll_after_prediv_min").getValueAsLong();
            pll_after_prediv_max           = getLongVariable("pll_after_prediv_max").getValueAsLong();
            pll_output_min                 = getLongVariable("pll_output_min").getValueAsLong();
            pll_output_max                 = getLongVariable("pll_output_max").getValueAsLong();
            vlpr_mode_max_core_frequency   = getDoubleVariable("/SIM/vlpr_mode_max_core_frequency").getValueAsLong(); 
            vlpr_mode_max_bus_frequency    = getDoubleVariable("/SIM/vlpr_mode_max_bus_frequency").getValueAsLong();
            vlpr_mode_max_flash_frequency  = getDoubleVariable("/SIM/vlpr_mode_max_flash_frequency").getValueAsLong();
            run_mode_max_core_frequency    = getDoubleVariable("/SIM/run_mode_max_core_frequency").getValueAsLong();
            run_mode_max_bus_frequency     = getDoubleVariable("/SIM/run_mode_max_bus_frequency").getValueAsLong();
            run_mode_max_flash_frequency   = getDoubleVariable("/SIM/run_mode_max_flash_frequency").getValueAsLong();
            hsrun_mode_max_core_frequency  = getDoubleVariable("/SIM/hsrun_mode_max_core_frequency").getValueAsLong();
            hsrun_mode_max_bus_frequency   = getDoubleVariable("/SIM/hsrun_mode_max_bus_frequency").getValueAsLong();
            hsrun_mode_max_flash_frequency = getDoubleVariable("/SIM/hsrun_mode_max_flash_frequency").getValueAsLong();

            osc_Low_min_frequency          = getLongVariable("osc_Low_min_frequency").getValueAsLong();               
            osc_medium_min_frequency       = getLongVariable("osc_medium_min_frequency").getValueAsLong();            
            osc_high_min_frequency         = getLongVariable("osc_high_min_frequency").getValueAsLong();              
            osc_high_max_frequency         = getLongVariable("osc_high_max_frequency").getValueAsLong();              
            
            LongVariable scg_spllcfg_multVar  = getLongVariable("scg_spllcfg_mult");
            scg_spllcfg_multVar.setOffset(-pll_mult_min);
            scg_spllcfg_multVar.setMin(pll_mult_min);
            scg_spllcfg_multVar.setMax(pll_mult_max);
            if (scg_spllcfg_multVar.isValid() != null) {
               scg_spllcfg_multVar.reset();
            }
            LongVariable scg_spllcfg_predivVar  = getLongVariable("scg_spllcfg_prediv");
            scg_spllcfg_predivVar.setOffset(-pll_prediv_min);
            scg_spllcfg_predivVar.setMin(pll_prediv_min);
            scg_spllcfg_predivVar.setMax(pll_prediv_max);
            if (scg_spllcfg_predivVar.isValid() != null) {
               scg_spllcfg_predivVar.reset();
            }
            LongVariable system_spll_clockVar  = getLongVariable("system_spll_clock");
            system_spll_clockVar.setMin(pll_output_min);
            system_spll_clockVar.setMax(pll_output_max);
            if (system_spll_clockVar.isValid() != null) {
               system_spll_clockVar.reset();
            }
         }
      }
      for (int fIndex=0; fIndex<fDimension; fIndex++) {
         validateClocks(variable);
      }
      initialisationDone = true;
      fIndex = 0;
   }

   /**
    * @param variable
    * @param index 
    * @throws Exception
    */
   protected void validateClocks(Variable variable) throws Exception {
//      System.err.println(getSimpleClassName()+" "+variable +", Index ="+index);

      super.validate(variable);

      StringVariable clockConfig = safeGetStringVariable("ClockConfig");
      clockConfig.setStatus(isValidCIdentifier(clockConfig.getValueAsString())?(String)null:"Illegal C enum value");

      Variable     enableClockConfigurationVar      =  getVariable("enableClockConfiguration");
      if (fIndex == 0) {
         // Clock configuration 0 is always enables
         if (enableClockConfigurationVar.isEnabled()) {
            enableClockConfigurationVar.enable(false);
            enableClockConfigurationVar.setToolTip("Clock configuration 0 must always be enabled");
         }
      }
      else {
         if (enableClockConfigurationVar.getValueAsBoolean()) {
            clockConfig.enable(true);
         }
         else {
            clockConfig.enable(false);
         }
      }
      
      CategoryVariable  runModeSystemClocksVar         = (CategoryVariable) getVariable("runModeSystemClocks");
      CategoryVariable  alternativeModeSystemClocksVar = (CategoryVariable) getVariable("alternativeModeSystemClocks");
      
      ChoiceVariable    clock_transition_modeVar   = getChoiceVariable("clock_transition_mode");

      ChoiceVariable    scg_runccr_scsVar          = getChoiceVariable("scg_runccr_scs");
      LongVariable      scg_runccr_divcoreVar      = getLongVariable("scg_runccr_divcore");
      LongVariable      scg_runccr_divbusVar       = getLongVariable("scg_runccr_divbus");
      LongVariable      scg_runccr_divslowVar      = getLongVariable("scg_runccr_divslow");
      DoubleVariable    run_mode_core_clockVar     = getDoubleVariable("run_mode_core_clock");
      DoubleVariable    run_mode_bus_clockVar      = getDoubleVariable("run_mode_bus_clock");
      DoubleVariable    run_mode_flash_clockVar    = getDoubleVariable("run_mode_flash_clock");
                                                   
      ChoiceVariable    scg_altccr_scsVar          = getChoiceVariable("scg_altccr_scs");
      LongVariable      scg_altccr_divcoreVar      = getLongVariable("scg_altccr_divcore");
      LongVariable      scg_altccr_divbusVar       = getLongVariable("scg_altccr_divbus");
      LongVariable      scg_altccr_divslowVar      = getLongVariable("scg_altccr_divslow");
      DoubleVariable    alt_mode_core_clockVar     = getDoubleVariable("alt_mode_core_clock");
      DoubleVariable    alt_mode_bus_clockVar      = getDoubleVariable("alt_mode_bus_clock");
      DoubleVariable    alt_mode_flash_clockVar    = getDoubleVariable("alt_mode_flash_clock");
                                                   
      LongVariable      system_firc_frequencyVar   = getLongVariable("system_firc_frequency");
      BooleanVariable   scg_firccsr_fircenVar      = getBooleanVariable("scg_firccsr_fircen");
//      ChoiceVariable    scg_firccfg_rangeVar       = getChoiceVariable("scg_firccfg_range");
      ChoiceVariable    scg_fircdiv_fircdiv1Var    = getChoiceVariable("scg_fircdiv_fircdiv1");
      ChoiceVariable    scg_fircdiv_fircdiv2Var    = getChoiceVariable("scg_fircdiv_fircdiv2");
      LongVariable      firc_frequencyVar          = getLongVariable("firc_frequency");
      DoubleVariable    firc_div1_frequencyVar     = getDoubleVariable("firc_div1_frequency");
      DoubleVariable    firc_div2_frequencyVar     = getDoubleVariable("firc_div2_frequency");
                                                   
      LongVariable      system_sirc_frequencyVar   = getLongVariable("system_sirc_frequency");
      BooleanVariable   scg_sirccsr_sircenVar      = getBooleanVariable("scg_sirccsr_sircen");
//      ChoiceVariable    scg_sirccfg_rangeVar       = getChoiceVariable("scg_sirccfg_range");
      ChoiceVariable    scg_sircdiv_sircdiv1Var    = getChoiceVariable("scg_sircdiv_sircdiv1");
      ChoiceVariable    scg_sircdiv_sircdiv2Var    = getChoiceVariable("scg_sircdiv_sircdiv2");
      LongVariable      sirc_frequencyVar          = getLongVariable("sirc_frequency");
      DoubleVariable    sirc_div1_frequencyVar     = getDoubleVariable("sirc_div1_frequency");
      DoubleVariable    sirc_div2_frequencyVar     = getDoubleVariable("sirc_div2_frequency");
                                                   
      LongVariable      system_sosc_frequencyVar   = getLongVariable("system_sosc_frequency");
      BooleanVariable   scg_sosccsr_soscenVar      = getBooleanVariable("scg_sosccsr_soscen");
      ChoiceVariable    scg_sosccfg_rangeVar       = getChoiceVariable("scg_sosccfg_range");
      LongVariable      sosc_frequencyVar          = getLongVariable("sosc_frequency");
      ChoiceVariable    scg_soscdiv_soscdiv1Var    = getChoiceVariable("scg_soscdiv_soscdiv1");
      ChoiceVariable    scg_soscdiv_soscdiv2Var    = getChoiceVariable("scg_soscdiv_soscdiv2");
                                                   
      BooleanVariable   scg_spllcsr_spllenVar      = getBooleanVariable("scg_spllcsr_spllen");
      LongVariable      scg_spllcfg_multVar        = getLongVariable("scg_spllcfg_mult");
      LongVariable      scg_spllcfg_predivVar      = getLongVariable("scg_spllcfg_prediv");
      ChoiceVariable    scg_splldiv_splldiv1Var    = getChoiceVariable("scg_splldiv_splldiv1");
      ChoiceVariable    scg_splldiv_splldiv2Var    = getChoiceVariable("scg_splldiv_splldiv2");
                                                   
      DoubleVariable    sosc_div1_frequencyVar     = getDoubleVariable("sosc_div1_frequency");
      DoubleVariable    sosc_div2_frequencyVar     = getDoubleVariable("sosc_div2_frequency");
                                                   
      LongVariable      system_spll_clockVar       = getLongVariable("system_spll_clock");
      DoubleVariable    spll_div1_frequencyVar     = getDoubleVariable("spll_div1_frequency");
      DoubleVariable    spll_div2_frequencyVar     = getDoubleVariable("spll_div2_frequency");

      if (!initialisationDone) {
         system_sosc_frequencyVar.setMin(osc_Low_min_frequency);
         system_sosc_frequencyVar.setMax(osc_high_max_frequency);
         run_mode_bus_clockVar.setMax(run_mode_max_bus_frequency);
         run_mode_core_clockVar.setMax(run_mode_max_core_frequency);
         run_mode_flash_clockVar.setMax(run_mode_max_flash_frequency);

//         scg_altccr_divslowVar.setDebug(true);
//         alt_mode_flash_clockVar.setDebug(true);
      }
      /** Do FIRC derived clocks */
      boolean  scg_firccsr_fircen      = scg_firccsr_fircenVar.getValueAsBoolean();
      Status   fircStatus              = null;
      long     system_firc_frequency   = system_firc_frequencyVar.getValueAsLong();
      double   firc_frequency          = 0;
      if (scg_firccsr_fircen) {
         firc_frequency = system_firc_frequency;
      }
      else {
         fircStatus = new Status("Disabled by firccsr.fircen", Severity.INFO);
      }
      firc_frequencyVar.setValue(firc_frequency);
      firc_div1_frequencyVar.setValue(calculatePeripheralClock(firc_frequency, scg_fircdiv_fircdiv1Var.getValueAsLong()));
      firc_div2_frequencyVar.setValue(calculatePeripheralClock(firc_frequency, scg_fircdiv_fircdiv2Var.getValueAsLong()));
      firc_frequencyVar.enable(scg_firccsr_fircen);
      firc_div1_frequencyVar.enable(scg_firccsr_fircen);
      firc_div2_frequencyVar.enable(scg_firccsr_fircen);
      firc_frequencyVar.setStatus(fircStatus);
      firc_div1_frequencyVar.setStatus(fircStatus);
      firc_div2_frequencyVar.setStatus(fircStatus);

      /** Do SIRC derived clocks */
      Status   sircStatus              = null;
      boolean  scg_sirccsr_sircen      = scg_sirccsr_sircenVar.getValueAsBoolean();
      long     system_sirc_frequency   = system_sirc_frequencyVar.getValueAsLong();
      double   sirc_frequency          = 0;
      if (scg_sirccsr_sircen) {
         sirc_frequency = system_sirc_frequency;
      }
      else {
         sircStatus = new Status("Disabled by sirccsr.sircen", Severity.INFO);
      }
      sirc_frequencyVar.setValue(sirc_frequency);
      sirc_div1_frequencyVar.setValue(calculatePeripheralClock(sirc_frequency, scg_sircdiv_sircdiv1Var.getValueAsLong()));
      sirc_div2_frequencyVar.setValue(calculatePeripheralClock(sirc_frequency, scg_sircdiv_sircdiv2Var.getValueAsLong()));
      sirc_frequencyVar.enable(scg_sirccsr_sircen);
      sirc_div1_frequencyVar.enable(scg_sirccsr_sircen);
      sirc_div2_frequencyVar.enable(scg_sirccsr_sircen);
      sirc_frequencyVar.setStatus(sircStatus);
      sirc_div1_frequencyVar.setStatus(sircStatus);
      sirc_div2_frequencyVar.setStatus(sircStatus);
      
      /** Do SOSC derived clocks */
      Status soscStatus = null;
      long system_sosc_frequency = system_sosc_frequencyVar.getValueAsLong();
      
      if (system_sosc_frequency>osc_high_max_frequency) {
         scg_sosccfg_rangeVar.setValue(2);
         scg_sosccfg_rangeVar.setStatus("Frequency not suitable for oscillator");
      }
      else {
         scg_sosccfg_rangeVar.clearStatus();
         if (system_sosc_frequency>=osc_high_min_frequency) {
            scg_sosccfg_rangeVar.setValue(3);
         }
         else if (system_sosc_frequency>=osc_medium_min_frequency) {
            scg_sosccfg_rangeVar.setValue(2);
         }
         else if (system_sosc_frequency>=osc_Low_min_frequency) {
            scg_sosccfg_rangeVar.setValue(1);
         }
      }
      boolean scg_sosccsr_soscen = scg_sosccsr_soscenVar.getValueAsBoolean();
      double  sosc_frequency     = 0;
      if (scg_sosccsr_soscen) {
         sosc_frequency = system_sosc_frequency;
      }
      else {
         soscStatus = new Status("Disabled by sosccsr.soscen", Severity.INFO);
      }
      sosc_frequencyVar.setValue(sosc_frequency);
      sosc_div1_frequencyVar.setValue(calculatePeripheralClock(sosc_frequency, scg_soscdiv_soscdiv1Var.getValueAsLong()));
      sosc_div2_frequencyVar.setValue(calculatePeripheralClock(sosc_frequency, scg_soscdiv_soscdiv2Var.getValueAsLong()));
      sosc_frequencyVar.enable(scg_sosccsr_soscen);
      sosc_div1_frequencyVar.enable(scg_sosccsr_soscen);
      sosc_div2_frequencyVar.enable(scg_sosccsr_soscen);
      sosc_frequencyVar.setStatus(soscStatus);
      sosc_div1_frequencyVar.setStatus(soscStatus);
      sosc_div2_frequencyVar.setStatus(soscStatus);

      /** Do SPLL derived clocks */
      boolean  scg_spllcsr_spllen   = scg_spllcsr_spllenVar.getValueAsBoolean();
      long     scg_spllcfg_mult     = scg_spllcfg_multVar.getValueAsLong();
      long     scg_spllcfg_prediv   = scg_spllcfg_predivVar.getValueAsLong();
      
      boolean pllInputClockValid    = scg_sosccsr_soscen && (system_sosc_frequency>=pll_in_min) && (system_sosc_frequency<=pll_in_max);
      double  spllDividedClock      = system_sosc_frequency / scg_spllcfg_prediv;
      boolean pllDividedClockValid  = pllInputClockValid && (spllDividedClock>=pll_after_prediv_min) && (spllDividedClock<=pll_after_prediv_max);

      Status spllInputStatus  = null;
      Status spllOutputStatus = null;

      if (!scg_spllcsr_spllen) {
         spllOutputStatus = new Status("Disabled by spllen", Severity.INFO);
      }
      else if (!scg_sosccsr_soscen) {
         spllInputStatus  = new Status("Unavailable because SOSC disabled (sosccsr.soscen)", Severity.INFO);
         spllOutputStatus = spllInputStatus;
      }
      else if (!pllInputClockValid) {
         spllInputStatus  = new Status("System Oscillator frequency not suitable for PLL", Severity.INFO);
         spllOutputStatus = spllInputStatus;
      }
      else if (!pllDividedClockValid) {
         spllOutputStatus = new Status("System Oscillator prescaled frequency not suitable for PLL", Severity.INFO);
      }
      double spll_clock = 0;
      if (spllOutputStatus==null) {
         spll_clock = (spllDividedClock * scg_spllcfg_mult) / pll_post_divider;
      }
      system_spll_clockVar.setValue(spll_clock);
      spll_div1_frequencyVar.setValue(calculatePeripheralClock(spll_clock, scg_splldiv_splldiv1Var.getValueAsLong()));
      spll_div2_frequencyVar.setValue(calculatePeripheralClock(spll_clock, scg_splldiv_splldiv2Var.getValueAsLong()));
      scg_spllcsr_spllenVar.setStatus(spllInputStatus);
      system_spll_clockVar.setStatus(spllOutputStatus);
      spll_div1_frequencyVar.setStatus(spllOutputStatus);
      spll_div2_frequencyVar.setStatus(spllOutputStatus);

      scg_spllcsr_spllenVar.enable(spllInputStatus == null);
      system_spll_clockVar.enable(spllOutputStatus==null);
      spll_div1_frequencyVar.enable(spllOutputStatus==null);
      spll_div2_frequencyVar.enable(spllOutputStatus==null);
      
      /**  **/
      
      // Default clock mode 
      //=============================
      ClockMode clock_transition_mode = ClockMode.valueOf(clock_transition_modeVar.getSubstitutionValue());
//      if (index == 0) {
//         System.err.println("clock_transition_mode = " + clock_transition_mode);
//      }

      double systemClockFrequency = 0;
      Status systemClockStatus    = null;
      String systemClockOrigin    = "Disabled";
      String  runModeValue   = "Unavailable";
      boolean runModeEnable  = true;
      String  altModeValue   = "Unavailable";
      boolean altModeEnable  = false;

      switch (clock_transition_mode) {
      default:
      case ClockMode_None:
         runModeEnable  = false;
         break;
      case ClockMode_SOSC:
         systemClockFrequency = sosc_frequency;
         systemClockStatus    = soscStatus;
         systemClockOrigin    = "SOSC";
         scg_runccr_scsVar.setSubstitutionValue("0001");
         scg_altccr_scsVar.setSubstitutionValue("0001");
         runModeValue  = "RUN mode - SOSC";
         break;
      case ClockMode_SIRC:
         systemClockFrequency = sirc_frequency;
         systemClockStatus    = sircStatus;
         systemClockOrigin    = "SIRC";
         scg_runccr_scsVar.setSubstitutionValue("0010");
         scg_altccr_scsVar.setSubstitutionValue("0010");
         runModeValue  = "RUN mode - SIRC";
         altModeValue  = "VLPR mode - SIRC";
         altModeEnable = true;
         alt_mode_bus_clockVar.setMax(vlpr_mode_max_bus_frequency);
         alt_mode_core_clockVar.setMax(vlpr_mode_max_core_frequency);
         alt_mode_flash_clockVar.setMax(vlpr_mode_max_flash_frequency);
         break;
      case ClockMode_FIRC:
         systemClockFrequency = system_firc_frequency;
         systemClockStatus    = fircStatus;
         systemClockOrigin    = "FIRC";
//         rccrValue       = 3;
         scg_runccr_scsVar.setSubstitutionValue("0011");
         scg_altccr_scsVar.setSubstitutionValue("0011");
         runModeValue  = "RUN mode - FIRC";
         altModeValue  = "HSRUN mode - FIRC";
         altModeEnable = true;
         alt_mode_bus_clockVar.setMax(hsrun_mode_max_bus_frequency);
         alt_mode_core_clockVar.setMax(hsrun_mode_max_core_frequency);
         alt_mode_flash_clockVar.setMax(hsrun_mode_max_flash_frequency);
         break;
      case ClockMode_SPLL:
         systemClockFrequency = spll_clock;
         systemClockStatus    = spllOutputStatus;
         systemClockOrigin    = "SPLL";
         scg_runccr_scsVar.setValue(3);
         scg_altccr_scsVar.setValue(3);
         scg_runccr_scsVar.setSubstitutionValue("0110");
         scg_altccr_scsVar.setSubstitutionValue("0110");
         runModeValue  = "RUN mode - SPLL";
         altModeValue  = "HSRUN mode - SPLL";
         altModeEnable = true;
         alt_mode_bus_clockVar.setMax(hsrun_mode_max_bus_frequency);
         alt_mode_core_clockVar.setMax(hsrun_mode_max_core_frequency);
         alt_mode_flash_clockVar.setMax(hsrun_mode_max_flash_frequency);
         break;
      }     
      runModeSystemClocksVar.setValue(runModeValue);
      runModeSystemClocksVar.enable(runModeEnable);
      alternativeModeSystemClocksVar.setValue(altModeValue);
      alternativeModeSystemClocksVar.enable(altModeEnable);

      if (systemClockStatus != null) {
         systemClockStatus = new Status("Clock unavailable: "+systemClockStatus.getSimpleText(), Severity.ERROR);
      }
      runModeSystemClocksVar.setStatus(systemClockStatus);
      alternativeModeSystemClocksVar.setStatus(systemClockStatus);
      
      /** RUN mode - RCCR derived clocks */
      double systemCoreFrequency = systemClockFrequency/scg_runccr_divcoreVar.getValueAsLong();
      run_mode_core_clockVar.setValue(systemCoreFrequency);
      run_mode_core_clockVar.setStatus(systemClockStatus);
      run_mode_core_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVCORE");
      
      run_mode_bus_clockVar.setValue(systemCoreFrequency/scg_runccr_divbusVar.getValueAsLong());     
      run_mode_bus_clockVar.setStatus(systemClockStatus);
      run_mode_bus_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVBUS");
      
      run_mode_flash_clockVar.setValue(systemCoreFrequency/scg_runccr_divslowVar.getValueAsLong());   
      run_mode_flash_clockVar.setStatus(systemClockStatus);
      run_mode_flash_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVSLOW");

      /** VLPR/HSRUN CCR derived clocks */
      systemCoreFrequency = systemClockFrequency/scg_altccr_divcoreVar.getValueAsLong();
      alt_mode_core_clockVar.setValue(systemCoreFrequency);
      alt_mode_core_clockVar.setStatus(systemClockStatus);
      alt_mode_core_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVCORE");

      alt_mode_bus_clockVar.setValue(systemCoreFrequency/scg_altccr_divbusVar.getValueAsLong());     
      alt_mode_bus_clockVar.setStatus(systemClockStatus);
      alt_mode_bus_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVBUS");

      alt_mode_flash_clockVar.setValue(systemCoreFrequency/scg_altccr_divslowVar.getValueAsLong());   
      alt_mode_flash_clockVar.setStatus(systemClockStatus);
      alt_mode_flash_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVSLOW");
   }
   
   @Override
   public void createDependencies() throws Exception {
   }
}
