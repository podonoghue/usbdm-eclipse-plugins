package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.ListIterator;

import net.sourceforge.usbdm.deviceEditor.information.BooleanVariable;
import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.model.Status;
import net.sourceforge.usbdm.deviceEditor.model.Status.Severity;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to determine FLL divider settings
 */
public class FllValidator extends IndexedValidator {

   // Range for FLL input - Wide
   private static final long FLL_CLOCK_WIDE_MIN   = 31250L;
   private static final long FLL_CLOCK_WIDE_MAX   = 39063L;
   
   // Range for FLL input - Narrow
   private static final long FLL_CLOCK_NARROW_MIN = 32768L-100;
   private static final long FLL_CLOCK_NARROW_MAX = 32768L+100;

   // FLL multiplication factor for narrow range
   private static final long FLL_NARROW_FACTOR = 732;

   // FLL multiplication factor for wide range
   private static final long FLL_WIDE_FACTOR   = 640;

   private final long  DRST_DRS_MAX;

   public FllValidator(PeripheralWithState peripheral, Integer dimension, ArrayList<Object> values) {
      super(peripheral, dimension);

      ListIterator<Object> it = values.listIterator();
      DRST_DRS_MAX = (Long)it.next();
   }

   @Override
   protected void validate(Variable variable, int properties, int index) throws Exception {

      boolean fll_enabled  = getBooleanVariable("fll_enabled[]").getValueAsBoolean();
      
      LongVariable irefs_clockVar  = getLongVariable("irefs_clock[]");

      LongVariable fllInputFrequencyVar      = getLongVariable("fllInputFrequency[]");
      LongVariable system_mcgfllclk_clockVar = getLongVariable("system_mcgfllclk_clock[]");
      
      String fllInputFrequencyOrigin = irefs_clockVar.getOrigin();
      long   fllInputFrequency       = irefs_clockVar.getValueAsLong();
      
      fllInputFrequencyVar.setOrigin(fllInputFrequencyOrigin);
      fllInputFrequencyVar.setValue(fllInputFrequency);
      
      fll_enabled = fll_enabled && (fllInputFrequency>0);
      fllInputFrequencyVar.enable(fll_enabled);
      system_mcgfllclk_clockVar.enable(fll_enabled);
      
      if (!fll_enabled) {
         fllInputFrequencyVar.clearStatus();
         system_mcgfllclk_clockVar.clearStatus();
         return;
      }
      String fllOrigin = fllInputFrequencyOrigin+"\n via FLL";

      // Initially assume PLL_INPUT status same as MCGFFCLK
      Status mcgffclkStatus = irefs_clockVar.getStatus();
      
      if ((mcgffclkStatus != null) && (mcgffclkStatus.getSeverity().greaterThan(Severity.INFO))) {
         // Invalid FLL input
         Status fllInputStatus = new Status(mcgffclkStatus.getSimpleText()+": Invalid FLL input", Severity.ERROR);
         fllInputFrequencyVar.setStatus(fllInputStatus);
         return;
      }

      BooleanVariable mcg_c4_dmx32Var = getBooleanVariable("mcg_c4_dmx32[]");
      boolean mcg_c4_dmx32 = mcg_c4_dmx32Var.getValueAsBoolean();
      
      fllInputFrequencyVar.setValue(fllInputFrequency);
      
      long fllInMin = mcg_c4_dmx32?FLL_CLOCK_NARROW_MIN:FLL_CLOCK_WIDE_MIN;
      long fllInMax = mcg_c4_dmx32?FLL_CLOCK_NARROW_MAX:FLL_CLOCK_WIDE_MAX;
      
      // Range check
      if ((fllInputFrequency<fllInMin)||(fllInputFrequency>fllInMax)) {
         Status fllInputStatus = new Status(String.format(
               "Range error, permitted range [%sHz,%sHz]",
                     EngineeringNotation.convert(fllInMin, 3),
                     EngineeringNotation.convert(fllInMax, 3)),
               Severity.ERROR);
         fllInputFrequencyVar.setStatus(fllInputStatus);
         system_mcgfllclk_clockVar.setStatus(fllInputStatus);
         return;
      }
      // OK - input is acceptable
      fllInputFrequencyVar.clearStatus();
      
      // Determine possible output frequencies & check against desired value
      //=======================================================================
      int mcg_c4_drst_drs = -1;

      long fllOutFrequency = fllInputFrequency * (mcg_c4_dmx32?FLL_NARROW_FACTOR:FLL_WIDE_FACTOR);

      Long fllTargetFrequency = system_mcgfllclk_clockVar.getRawValueAsLong();

      ArrayList<Long> fllFrequencies = new ArrayList<Long>();
      for (int probe=0; probe<=DRST_DRS_MAX; probe++) {
         fllFrequencies.add(fllOutFrequency*(probe+1));
         // Accept value within ~10% of desired
         if (Math.abs((fllOutFrequency*(probe+1)) - fllTargetFrequency) < (fllTargetFrequency/50)) {
            mcg_c4_drst_drs = probe;
         }
      }
      StringBuilder sb       = new StringBuilder();
      Severity      severity = Severity.OK;
      if (mcg_c4_drst_drs >= 0) {
         // Adjust rounded value
         fllTargetFrequency = fllOutFrequency*(mcg_c4_drst_drs+1);
      }
      else {
         mcg_c4_drst_drs = 0;
         sb.append("Not possible to generate desired FLL frequency from input clock\n");
         fllOrigin = fllOrigin+"(invalid output frequency)";
         severity = Severity.WARNING;
      }
      boolean needComma = false;
      for (Long freq : fllFrequencies) {
         if (needComma) {
            sb.append(", ");
         }
         else {
            sb.append("Possible values = ");
         }
         needComma = true;
         sb.append(EngineeringNotation.convert(freq, 5)+"Hz");
      }
      system_mcgfllclk_clockVar.setValue(fllTargetFrequency);
      system_mcgfllclk_clockVar.setStatus(new Status (sb.toString(), severity));
      system_mcgfllclk_clockVar.setOrigin(fllOrigin);
      
      ChoiceVariable mcg_c4_drst_drsVar = getChoiceVariable("mcg_c4_drst_drs[]");
      mcg_c4_drst_drsVar.setValue(mcg_c4_drst_drs);
   }

   @Override
   protected boolean createDependencies() throws Exception {
      
      // Variables to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      variablesToWatch.add("/SMC/smc_pmctrl_runm[]");
      variablesToWatch.add("fll_enabled[]");
      variablesToWatch.add("irefs_clock[]");
      variablesToWatch.add("system_mcgfllclk_clock[]");
      variablesToWatch.add("mcg_c4_dmx32[]");
      variablesToWatch.add("mcgClockMode[]");

      addSpecificWatchedVariables(variablesToWatch);
      
      // Don't add default dependencies
      return false;
   }
}