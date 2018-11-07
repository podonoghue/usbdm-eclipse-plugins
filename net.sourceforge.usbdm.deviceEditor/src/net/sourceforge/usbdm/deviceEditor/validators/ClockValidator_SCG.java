package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DoubleVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
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
            pll_mult_min         = getLongVariable("pll_mult_min").getValueAsLong();
            pll_mult_max         = getLongVariable("pll_mult_max").getValueAsLong();
            pll_prediv_min       = getLongVariable("pll_prediv_min").getValueAsLong();
            pll_prediv_max       = getLongVariable("pll_prediv_max").getValueAsLong();
            pll_post_divider     = getLongVariable("pll_post_divider").getValueAsLong();
            pll_in_min           = getLongVariable("pll_in_min").getValueAsLong();
            pll_in_max           = getLongVariable("pll_in_max").getValueAsLong();
            pll_after_prediv_min = getLongVariable("pll_after_prediv_min").getValueAsLong();
            pll_after_prediv_max = getLongVariable("pll_after_prediv_max").getValueAsLong();
            pll_output_min       = getLongVariable("pll_output_min").getValueAsLong();
            pll_output_max       = getLongVariable("pll_output_max").getValueAsLong();
            
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
      for (int index=0; index<fDimension; index++) {
         fIndex = index;
         validateClocks(variable, index);
      }
      initialisationDone = true;
      fIndex = 0;
   }

   /**
    * @param variable
    * @param index 
    * @throws Exception
    */
   protected void validateClocks(Variable variable, int index) throws Exception {
//      System.err.println(getSimpleClassName()+" "+variable +", Index ="+index);

      super.validate(variable);

      ChoiceVariable    clock_modeVar              = getChoiceVariable("clock_mode");

      ChoiceVariable    scg_rccr_scsVar            = getChoiceVariable("scg_rccr_scs");
      LongVariable      scg_rccr_divcoreVar        = getLongVariable("scg_rccr_divcore");
      LongVariable      scg_rccr_divbusVar         = getLongVariable("scg_rccr_divbus");
      LongVariable      scg_rccr_divslowVar        = getLongVariable("scg_rccr_divslow");
      DoubleVariable    run_mode_core_clockVar     = getDoubleVariable("run_mode_core_clock");
      DoubleVariable    run_mode_bus_clockVar      = getDoubleVariable("run_mode_bus_clock");
      DoubleVariable    run_mode_flash_clockVar    = getDoubleVariable("run_mode_flash_clock");
                                                   
      ChoiceVariable    scg_vccr_scsVar            = getChoiceVariable("scg_vccr_scs");
      LongVariable      scg_vccr_divcoreVar        = getLongVariable("scg_vccr_divcore");
      LongVariable      scg_vccr_divbusVar         = getLongVariable("scg_vccr_divbus");
      LongVariable      scg_vccr_divslowVar        = getLongVariable("scg_vccr_divslow");
      DoubleVariable    vlpr_mode_core_clockVar    = getDoubleVariable("vlpr_mode_core_clock");
      DoubleVariable    vlpr_mode_bus_clockVar     = getDoubleVariable("vlpr_mode_bus_clock");
      DoubleVariable    vlpr_mode_flash_clockVar   = getDoubleVariable("vlpr_mode_flash_clock");
                                                   
      ChoiceVariable    scg_hccr_scsVar            = getChoiceVariable("scg_hccr_scs");
      LongVariable      scg_hccr_divcoreVar        = getLongVariable("scg_hccr_divcore");
      LongVariable      scg_hccr_divbusVar         = getLongVariable("scg_hccr_divbus");
      LongVariable      scg_hccr_divslowVar        = getLongVariable("scg_hccr_divslow");
      DoubleVariable    hsrun_mode_core_clockVar   = getDoubleVariable("hsrun_mode_core_clock");
      DoubleVariable    hsrun_mode_bus_clockVar    = getDoubleVariable("hsrun_mode_bus_clock");
      DoubleVariable    hsrun_mode_flash_clockVar  = getDoubleVariable("hsrun_mode_flash_clock");

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
      if ((system_sosc_frequency>=4000000) && (system_sosc_frequency<8000000)) {
         scg_sosccfg_rangeVar.setValue(2);
      }
      else if ((system_sosc_frequency>=8000000) && (system_sosc_frequency<=40000000)) {
         scg_sosccfg_rangeVar.setValue(3);
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
      ClockMode clock_mode = ClockMode.valueOf(clock_modeVar.getSubstitutionValue());

//      System.err.println("Mode = " + clock_mode);

      Status clockModeStatus = null;
      switch (clock_mode) {
      default:
      case ClockMode_None:
         break;
      case ClockMode_SOSC:
         clockModeStatus = soscStatus;
         break;
      case ClockMode_SIRC:
         clockModeStatus = sircStatus;
         break;
      case ClockMode_FIRC:
         clockModeStatus = fircStatus;
         break;
      case ClockMode_SPLL:
         clockModeStatus = spllOutputStatus;
         break;
      }     
      if (clockModeStatus != null) {
         clockModeStatus = new Status("Clock unavailable: "+clockModeStatus.getSimpleText(), Severity.ERROR);
      }
      clock_modeVar.setStatus(clockModeStatus);

      /** RCCR derived clocks */
      int scg_rccr_scs = Integer.parseInt(scg_rccr_scsVar.getSubstitutionValue(), 2);

      double systemClockFrequency;
      Status systemClockStatus;
      String systemClockOrigin;
      
      switch (scg_rccr_scs) {
      default:
      case 1:
         systemClockFrequency = sosc_frequency;
         systemClockStatus    = soscStatus;
         systemClockOrigin    = "SOSC";
         break;
      case 2:
         systemClockFrequency = sirc_frequency;
         systemClockStatus    = sircStatus;
         systemClockOrigin    = "SIRC";
         break;
      case 3:
         systemClockFrequency = system_firc_frequency;
         systemClockStatus    = fircStatus;
         systemClockOrigin    = "FIRC";
         break;
      case 6:
         systemClockFrequency = spll_clock;
         systemClockStatus    = spllOutputStatus;
         systemClockOrigin    = "SPLL";
         break;
      }
      double systemCoreFrequency = systemClockFrequency/scg_rccr_divcoreVar.getValueAsLong();
      run_mode_core_clockVar.setValue(systemCoreFrequency);
      run_mode_core_clockVar.setStatus(systemClockStatus);
      run_mode_core_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVCORE");
      
      run_mode_bus_clockVar.setValue(systemCoreFrequency/scg_rccr_divbusVar.getValueAsLong());     
      run_mode_bus_clockVar.setStatus(systemClockStatus);
      run_mode_bus_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVBUS");
      
      run_mode_flash_clockVar.setValue(systemCoreFrequency/scg_rccr_divslowVar.getValueAsLong());   
      run_mode_flash_clockVar.setStatus(systemClockStatus);
      run_mode_flash_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVSLOW");

      /** VCCR derived clocks */
      int scg_vccr_scs = Integer.parseInt(scg_vccr_scsVar.getSubstitutionValue(), 2);

      switch (scg_vccr_scs) {
      default:
      case 1:
         systemClockFrequency = sosc_frequency;
         systemClockStatus    = soscStatus;
         systemClockOrigin    = "SOSC";
         break;
      case 2:
         systemClockFrequency = sirc_frequency;
         systemClockStatus    = sircStatus;
         systemClockOrigin    = "SIRC";
         break;
      case 3:
         systemClockFrequency = system_firc_frequency;
         systemClockStatus    = fircStatus;
         systemClockOrigin    = "FIRC";
         break;
      case 6:
         systemClockFrequency = spll_clock;
         systemClockStatus    = spllOutputStatus;
         systemClockOrigin    = "SPLL";
         break;
      }     
      systemCoreFrequency = systemClockFrequency/scg_vccr_divcoreVar.getValueAsLong();
      vlpr_mode_core_clockVar.setValue(systemCoreFrequency);
      vlpr_mode_core_clockVar.setStatus(systemClockStatus);
      vlpr_mode_core_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVCORE");
      
      vlpr_mode_bus_clockVar.setValue(systemCoreFrequency/scg_vccr_divbusVar.getValueAsLong());     
      vlpr_mode_bus_clockVar.setStatus(systemClockStatus);
      vlpr_mode_bus_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVBUS");
      
      vlpr_mode_flash_clockVar.setValue(systemCoreFrequency/scg_vccr_divslowVar.getValueAsLong());   
      vlpr_mode_flash_clockVar.setStatus(systemClockStatus);
      vlpr_mode_flash_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVSLOW");

      /** HSRUN derived clocks */
      int scg_hccr_scs = Integer.parseInt(scg_hccr_scsVar.getSubstitutionValue(), 2);

      switch (scg_hccr_scs) {
      default:
      case 1:
         systemClockFrequency = sosc_frequency;
         systemClockStatus    = soscStatus;
         systemClockOrigin    = "SOSC";
         break;
      case 2:
         systemClockFrequency = sirc_frequency;
         systemClockStatus    = sircStatus;
         systemClockOrigin    = "SIRC";
         break;
      case 3:
         systemClockFrequency = system_firc_frequency;
         systemClockStatus    = fircStatus;
         systemClockOrigin    = "FIRC";
         break;
      case 6:
         systemClockFrequency = spll_clock;
         systemClockStatus    = spllOutputStatus;
         systemClockOrigin    = "SPLL";
         break;
      }     
      systemCoreFrequency = systemClockFrequency/scg_hccr_divcoreVar.getValueAsLong();
      hsrun_mode_core_clockVar.setValue(systemCoreFrequency);
      hsrun_mode_core_clockVar.setStatus(systemClockStatus);
      hsrun_mode_core_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVCORE");
      
      hsrun_mode_bus_clockVar.setValue(systemCoreFrequency/scg_hccr_divbusVar.getValueAsLong());     
      hsrun_mode_bus_clockVar.setStatus(systemClockStatus);
      hsrun_mode_bus_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVBUS");
      
      hsrun_mode_flash_clockVar.setValue(systemCoreFrequency/scg_hccr_divslowVar.getValueAsLong());   
      hsrun_mode_flash_clockVar.setStatus(systemClockStatus);
      hsrun_mode_flash_clockVar.setOrigin(systemClockOrigin + " after division by SSCG_xCCR.DIVSLOW");

   }
   
   @Override
   public void createDependencies() throws Exception {
   }
}
