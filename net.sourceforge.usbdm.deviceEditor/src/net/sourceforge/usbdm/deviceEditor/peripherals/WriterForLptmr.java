package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.MuxSelection;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForLptmr extends PeripheralWithState {

   public WriterForLptmr(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);

      createValue(LPTMR_PSR_PCS,             "1", "Clock Source");
      createValue(LPTMR_PSR_PBYP,            "1", "Prescaler Bypass");
      createValue(LPTMR_PSR_PRESCALE,        "1", "Prescaler Value");
      createValue(LPTMR_CSR_TMS,             "1", "Timer Mode Select");
      createValue(LPTMR_CSR_TFC,             "1", "Timer Free-Running Counter");
      createValue(LPTMR_CSR_TPP,             "1", "Timer Pin Polarity");
      createValue(LPTMR_CSR_TPS,             "1", "Timer Pin Select");
      createValue(LPTMR_IRQ_LEVEL,           "0", "IRQ Level in NVIC [0-15]", 0, 15);
      createValue(LPTMR_USE_NAKED_HANDLER,   "1", "Interrupt handler setup");
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return getClassName()+instance+"_"+signal;
   }

   @Override
   public String getTitle() {
      return "Low Power Timer";
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"ALT0", "ALT1", "ALT2", "ALT3"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignalName().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal "+function.getSignalName()+" does not match expected pattern ");
   }
   
   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   /** Key for PIT_LDVAL */
   private static final String LPTMR_PSR_PCS             = "PSR_PCS";
   private static final String LPTMR_PSR_PBYP            = "PSR_PBYP";
   private static final String LPTMR_PSR_PRESCALE        = "PSR_PRESCALE";
   private static final String LPTMR_CSR_TMS             = "CSR_TMS";
   private static final String LPTMR_CSR_TFC             = "CSR_TFC";
   private static final String LPTMR_CSR_TPP             = "CSR_TPP";
   private static final String LPTMR_CSR_TPS             = "CSR_TPS";
   private static final String LPTMR_IRQ_LEVEL           = "IRQ_LEVEL";
   private static final String LPTMR_USE_NAKED_HANDLER   = "USE_NAKED_HANDLER";
   
   @Override
   public BaseModel[] getModels(BaseModel parent) {
      BaseModel models[] = {
            new CategoryModel(parent, getName(), getDescription()),
      };

      new SimpleSelectionModel(models[0], this, LPTMR_PSR_PCS, "[LPTMR_PSR_PCS]") {
         {
            setName(fVariableMap.get(LPTMR_PSR_PCS).name);
            setToolTip("Low Power Timer clock source");
         }
         @Override
         protected String[] getChoicesArray() {
            final String SELECTION_NAMES[] = {
                  "0: MCG Internal Reference Clock (MCGIRCLK)",
                  "1: Low power oscillator (LPO - 1kHz)",
                  "2: 32kHz Clock Source (ERCLK32)",
                  "3: Oscillator External Reference Clock (OSCERCLK)",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "2",
                  "3",
                  "1", // Default
            };
            return VALUES;
         }
      };

      new SimpleSelectionModel(models[0], this, LPTMR_PSR_PBYP, "[LPTMR_PSR_PBYP]") {
         {
            setName(fVariableMap.get(LPTMR_PSR_PBYP).name);
            setToolTip(
                  "When PBYP is set:\n"+
                  "- The selected prescaler clock in Time Counter mode or\n"+
                  "  the selected input source in Pulse Counter mode, directly clocks the CNR.\n" +
                  "When PBYP is clear:\n"+
                  "- The CNR is clocked by the output of the prescaler/glitch filter");
         }
         @Override
         protected String[] getChoicesArray() {
            final String SELECTION_NAMES[] = {
                  "0: Prescaler/glitch filter is enabled",
                  "1: Prescaler/glitch filter is bypassed",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "1", // Default
            };
            return VALUES;
         }
      };

      new SimpleSelectionModel(models[0], this, LPTMR_PSR_PRESCALE, "[LPTMR_PSR_PRESCALE]") {
         {
            setName(fVariableMap.get(LPTMR_PSR_PRESCALE).name);
            setToolTip(" Configures the size of the Prescaler in Time Counter mode or\n"+
                       "width of the glitch filter in Pulse Counter mode");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0:  Prescaler = 2, no glitch filter",
                  "1:  Prescaler = 4, 2 clock glitch filter",
                  "2:  Prescaler = 8, 4 clock glitch filter",
                  "3:  Prescaler = 16, 8 clock glitch filter",
                  "4:  Prescaler = 32, 16 clock glitch filter",
                  "5:  Prescaler = 64, 32 clock glitch filter",
                  "6:  Prescaler = 128, 64 clock glitch filter",
                  "7:  Prescaler = 256, 128 clock glitch filter",
                  "8:  Prescaler = 512, 256 clock glitch filter",
                  "9:  Prescaler = 1024, 512 clock glitch filter",
                  "10: Prescaler = 2048, 1024 clock glitch filter",
                  "11: Prescaler = 4096, 2048 clock glitch filter",
                  "12: Prescaler = 8192, 4096 clock glitch filter",
                  "13: Prescaler = 16384, 8192 clock glitch filter",
                  "14: Prescaler = 32768, 16384 clock glitch filter",
                  "15: Prescaler = 65536, 32768 clock glitch filter",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "2",
                  "3",
                  "4",
                  "5",
                  "6",
                  "7",
                  "8",
                  "9",
                  "10",
                  "11",
                  "12",
                  "13",
                  "14",
                  "15",
                  "0", // Default
            };
            return VALUES;
         }
      };

      new SimpleSelectionModel(models[0], this, LPTMR_CSR_TMS, "[LPTMR_CSR_TMS]") {
         {
            setName(fVariableMap.get(LPTMR_CSR_TMS).name);
            setToolTip("Configures the mode of the LPTMR");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0: Time Counter mode",
                  "1: Pulse Counter mode",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "0", // Default
            };
            return VALUES;
         }
      };
      
      new SimpleSelectionModel(models[0], this, LPTMR_CSR_TFC, "[LPTMR_CSR_TFC]") {
         {
            setName(fVariableMap.get(LPTMR_CSR_TFC).name);
            setToolTip("When clear, TFC configures the CNR to reset whenever TCF is set.n"+
                       "When set, TFC configures the CNR to reset on overflow");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0: CNR is reset whenever TCF is set",
                  "1: CNR is reset on overflow",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "0", // Default
            };
            return VALUES;
         }
      };

      new SimpleSelectionModel(models[0], this, LPTMR_CSR_TPP, "[LPTMR_CSR_TPP]") {
         {
            setName(fVariableMap.get(LPTMR_CSR_TPP).name);
            setToolTip("Configures the polarity of the input source in Pulse Counter mode");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0: Active-high source, rising-edge increments CNR",
                  "1: Active-low source,  falling-edge increments CNR",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "0", // Default
            };
            return VALUES;
         }
      };

      new SimpleSelectionModel(models[0], this, LPTMR_CSR_TPS, "[LPTMR_CSR_TPS]") {
         {
            setName(fVariableMap.get(LPTMR_CSR_TPS).name);
            setToolTip("Configures the input source to be used in Pulse Counter mode.\n"+
                       "The input connections vary by device");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0: CMP0 output",
                  "1: LPTMR_ALT1 pin",
                  "1: LPTMR_ALT2 pin",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "2",
                  "0", // Default
            };
            return VALUES;
         }
      };

      VariableInfo variableInfo = fVariableMap.get(LPTMR_IRQ_LEVEL);
      new VariableModel(models[0], this, LPTMR_IRQ_LEVEL).setName(variableInfo.name);

      new SimpleSelectionModel(models[0], this, LPTMR_USE_NAKED_HANDLER, "") {
         {
            setName(fVariableMap.get(LPTMR_USE_NAKED_HANDLER).name);
            setToolTip("The interrupt handler may use an external function named LPTMR0_IRQHandler() or\n"+
                       "may be set by use of the setCallback() function");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0: Interrupt handler is programmatically set",
                  "1: LPTMR0_IRQHandler() is externally provided",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0",
                  "1",
                  "0", // Default
            };
            return VALUES;
         }
      };

      return models;
   }
   
   static final String TEMPLATE = 
       "   //! Default PSR value\n"+
       "   static constexpr uint32_t psrValue = LPTMR_PSR_PRESCALE(${PSR_PRESCALE})|LPTMR_PSR_PCS(${PSR_PCS})|(${PSR_PBYP}<<LPTMR_PSR_PBYP_SHIFT)|(${PSR_PCS}<<LPTMR_PSR_PCS_SHIFT);\n"+
       "\n"+
       "   //! Default CSR value\n"+
       "   static constexpr uint32_t csrValue =  (${CSR_TMS}<<LPTMR_CSR_TMS_SHIFT)|(${CSR_TFC}<<LPTMR_CSR_TFC_SHIFT)|(${CSR_TPP}<<LPTMR_CSR_TPP_SHIFT)|LPTMR_CSR_TPS(${CSR_TPS});\n"+
       "\n"+
       "   //! Default IRQ level\n"+
       "   static constexpr uint32_t irqLevel =  ${IRQ_LEVEL};\n"+
       "\n";
   
   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      pinMappingHeaderFile.write(substitute(TEMPLATE, fVariableMap));
   }

   @Override
   public void writeExtraInfo(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeExtraInfo(pinMappingHeaderFile);
      pinMappingHeaderFile.write(
            "   /**\n"+
            "    * Initialise pins used by peripheral\n"+
            "    */\n"+
            "   static void initPCRs() {\n"
            );
      for (int index=0; index<fInfoTable.table.size(); index++) {
         Signal signal = fInfoTable.table.get(index);
         if (signal == null) {
            continue;
         }
         ArrayList<MappingInfo> mappedPins = fDeviceInfo.getPins(signal);
         for (MappingInfo mappedPin:mappedPins) {
            if (!mappedPin.getPin().isAvailableInPackage()) {
               // Discard unmapped signals on this package 
               continue;
            }
            if (mappedPin.getMux() == MuxSelection.disabled) {
               // Disabled selection - ignore
               continue;
            }
            if (mappedPin.getMux() == MuxSelection.reset) {
               // Reset selection - ignore
               continue;
            }
            if (mappedPin.getMux() == MuxSelection.fixed) {
               // No PCR
               break;
            }
            if (mappedPin.getPin().getMuxValue() == mappedPin.getMux()) {
               pinMappingHeaderFile.write(String.format("      PcrTable_T<Lptmr0Info, %d, pcrValue>::setPCR();\n", index));
            }
         }
      }
      pinMappingHeaderFile.write(
            "   }\n"+
                  "\n"
            );

   }

   @Override
   public VariableInfo getVariableInfo(String key) {
      return fVariableMap.get(key);
      }

}