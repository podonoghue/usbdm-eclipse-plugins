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
public class SpiValidate extends PeripheralValidator {

   private final int spprFactors[] = {1,2,3,4,5,6,7,8};
   private final int sprFactors[]  = {2,4,8,16,32,64,128,256,512};

   public SpiValidate(PeripheralWithState peripheral) {
      super(peripheral);
   }

   String calculateAvailableFrequencies(int clockFrequency) {
      ArrayList<Integer> freqs = new ArrayList<Integer>();
      
      for (int sppr = spprFactors.length-1; sppr >= 0; sppr--) {
         for (int spr = sprFactors.length-1; spr >= 0; spr--) {
            int calculatedFrequency = clockFrequency/(spprFactors[sppr]*sprFactors[spr]);
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
      
      LongVariable   spiInputClockVar           = getLongVariable("spiInputClock");
      LongVariable   speedVar                   = getLongVariable("spi_speed");

      int clockFrequency = (int)spiInputClockVar.getValueAsLong();
      int speed          = (int)speedVar.getValueAsLong();

      String freqs = calculateAvailableFrequencies(clockFrequency);
      speedVar.setToolTip(freqs);
      
      int bestSPPR = spprFactors.length-1;
      int bestSPR  = sprFactors.length-1;
      int bestDifference = 0x7FFFFFFF;
      int calculatedFrequency = 0;
      for (int sppr = spprFactors.length-1; sppr >= 0; sppr--) {
         for (int spr = sprFactors.length-1; spr >= 0; spr--) {
            calculatedFrequency = clockFrequency/(spprFactors[sppr]*sprFactors[spr]);
            int difference = speed-calculatedFrequency;
            if (difference < 0) {
               // Too high stop looking here
               break;
            }
            if (difference < bestDifference) {
               // New "best value"
               bestDifference = difference;
               bestSPR  = spr;
               bestSPPR = sppr;
            }
         }
      }
      calculatedFrequency = clockFrequency/(spprFactors[bestSPPR]*sprFactors[bestSPR]);
      
      ChoiceVariable   spi_br_spprVar  = getChoiceVariable("spi_br_sppr");
      ChoiceVariable   spi_br_sprVar   = getChoiceVariable("spi_br_spr");
      
      spi_br_spprVar.setValue(bestSPPR);
      spi_br_sprVar.setValue(bestSPR);
      
      if (fDeviceInfo.getInitialisationPhase() == InitPhase.VariableAndGuiPropagationAllowed) {
         speedVar.setValue(calculatedFrequency);
      }
   }

   @Override
   protected boolean createDependencies() throws Exception {
 
      // Variable to watch
      ArrayList<String> variablesToWatch = new ArrayList<String>();

      
      variablesToWatch.add("spiInputClock");
      variablesToWatch.add("spi_speed");

      addSpecificWatchedVariables(variablesToWatch);

      // Don't add default dependencies
      return false;
   }

}