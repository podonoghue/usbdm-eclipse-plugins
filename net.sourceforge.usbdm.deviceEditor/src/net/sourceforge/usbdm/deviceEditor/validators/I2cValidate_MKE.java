package net.sourceforge.usbdm.deviceEditor.validators;

import java.util.ArrayList;
import java.util.Arrays;

import net.sourceforge.usbdm.deviceEditor.information.ChoiceVariable;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo.InitPhase;
import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.model.EngineeringNotation;
import net.sourceforge.usbdm.deviceEditor.peripherals.PeripheralWithState;

/**
 * Class to validate SPI settings
 */
public class I2cValidate_MKE extends PeripheralValidator {

   private final int multFactors[] = {1,2,4};

   // I2C baud rate divisor table
   private final int icrFactors[] = {
         20,   22,   24,   26,    28,   30,   34,   40,   28,   32,
         36,   40,   44,   48,    56,   68,   48,   56,   64,   72,
         80,   88,   104,  128,   80,   96,  112,  128,  144,  160,
         192,  240,  160,  192,  224,  256,  288,  320,  384,  480,
         320,  384,  448,  512,  576,  640,  768,  960,  640,  768,
         896, 1024, 1152, 1280, 1536, 1920, 1280, 1536, 1792, 2048,
         2304,2560, 3072, 3840,
   };

   public I2cValidate_MKE(PeripheralWithState peripheral) {
      super(peripheral);
   }

   String calculateAvailableFrequencies(int clockFrequency) {
      ArrayList<Integer> freqs = new ArrayList<Integer>();
      
      for (int mult = 0; mult <= multFactors.length-1; mult++) {
         for (int icr = 0; icr <= icrFactors.length-1; icr++) {
            int calculatedFrequency = clockFrequency/(multFactors[mult]*icrFactors[icr]);
            if (!freqs.contains(calculatedFrequency)) {
               freqs.add(calculatedFrequency);
            }
         }
      }
      
      Integer[] freqAr = freqs.toArray(new Integer[freqs.size()]);
      Arrays.sort(freqAr);
      
      StringBuilder sb = new StringBuilder();
      int lineLength = 0;
      for(Integer freq:freqAr) {
         lineLength++;
         if (!sb.isEmpty()) {
            sb.append(", ");
         }
         else {
            sb.append("Available Frequencies:\n");
         }
         if (lineLength>=60) {
            lineLength = 0;
            sb.append("\n");
         }
         String freqStr = EngineeringNotation.convert(freq, 3);
         lineLength += freqStr.length()+4;
         sb.append(freqStr);
         sb.append("Hz");
      }
      return sb.toString();
   }
   
   /**
    * Class to validate SPI settings
    * @throws Exception
    */
   @Override
   public void validate(Variable variable) throws Exception {
      
      LongVariable   i2cInputClockVar           = getLongVariable("i2cInputClock");
      LongVariable   speedVar                   = getLongVariable("i2c_speed");

      int clockFrequency = (int)i2cInputClockVar.getValueAsLong();
      int speed          = (int)speedVar.getValueAsLong();

      String freqs = calculateAvailableFrequencies(clockFrequency);
      speedVar.setToolTip(freqs);
      
      int bestMULT = multFactors.length-1;
      int bestICR  = (1<<7)-1;
      int bestDifference = 0x7FFFFFFF;
      int calculatedFrequency = 0;
      
      for (int mult = 0; mult <= multFactors.length-1; mult++) {
         for (int icr = 0; icr <= icrFactors.length-1; icr++) {
            calculatedFrequency = clockFrequency/(multFactors[mult]*icrFactors[icr]);
            int difference = Math.abs(speed-calculatedFrequency);
            if (difference < bestDifference) {
               // New "best value"
               bestDifference = difference;
               bestICR  = icr;
               bestMULT = mult;
            }
         }
      }
      calculatedFrequency = clockFrequency/(multFactors[bestMULT]*icrFactors[bestICR]);
      
      ChoiceVariable   i2c_f_multVar  = getChoiceVariable("i2c_f_mult");
      LongVariable     i2c_f_icrVar   = getLongVariable("i2c_f_icr");
      
      i2c_f_multVar.setValue(bestMULT);
      i2c_f_icrVar.setValue(bestICR);
      
      if (fDeviceInfo.getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
         speedVar.setValue(calculatedFrequency);
      }
   }

   @Override
   protected boolean createDependencies() throws Exception {
 
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      
      variablesToWatch.add("i2cInputClock");
      variablesToWatch.add("i2c_speed");

      addSpecificWatchedVariables(variablesToWatch);

      // Don't add default dependencies
      return false;
   }

}