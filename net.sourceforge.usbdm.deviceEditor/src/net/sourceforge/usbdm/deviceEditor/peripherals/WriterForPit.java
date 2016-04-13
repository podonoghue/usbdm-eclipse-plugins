package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

/**
 * Class encapsulating the code for writing an instance of PIT
 */
public class WriterForPit extends PeripheralWithState {

   static final String ALIAS_PREFIX       = "pit_";

   /* Keys for PIT */
   private static final String PIT_LDVAL_KEY              = "LDVAL";
   private static final String PIT_MCR_FRZ_KEY            = "MCR_FRZ";
   private static final String PIT_IRQ_LEVEL_KEY          = "IRQ_LEVEL";
   private static final String PIT_USES_NAKED_HANDLER_KEY = "USES_NAKED_HANDLER";
   
   
   public WriterForPit(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(PIT_LDVAL_KEY,                "2000",  "Reload value [0-65535]",        0, 65535);
      createValue(PIT_USES_NAKED_HANDLER_KEY,   "0",     "Use naked handler",             0, 1);
      createValue(PIT_MCR_FRZ_KEY,              "0",     "PIT Freeze in debug mode",      0, 1);
      createValue(PIT_IRQ_LEVEL_KEY,            "0",     "PIT IRQ Level in NVIC [0-15]",  0, 15);
   }

   @Override
   public String getTitle() {
      return "Programmable Interrupt Timer";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return getClassName()+instance+"_"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal       = getSignalIndex(mappingInfo.getSignals().get(fnIndex));
      StringBuffer sb = new StringBuffer();
      sb.append(String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal));
      return sb.toString();
   }

   @Override
   public int getSignalIndex(Signal function) {
      final String signalNames[] = {"OUT"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignalName().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal "+function.getSignalName()+" does not match expected pattern ");
   }
   
//   static final String TEMPLATE_DOCUMENTATION = 
//         "/**\n"+
//         " * Convenience class representing a PIT\n"+
//         " *\n"+
//         " * Example\n"+
//         " * @code\n"+
//         " * using Pit = const USBDM::Pit<PitInfo>;\n"+
//         " * @endcode\n"+
//         " *\n"+
//         " */\n";
   
   @Override
   public String getCTemplate() {
      return null;
//      return TEMPLATE_DOCUMENTATION + String.format(
//            "template<uint8_t channel> using %s = %s<%sInfo>;\n\n",
//            getClassName(), getClassName(), getClassName());
   }

   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return super.getAliasDeclaration(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }

   @Override
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " + getDefinition(mappingInfo, fnIndex);
   }

   static final String TEMPLATE = 
         "   //! Default value for PIT->SC register\n"+
         "   static constexpr uint32_t loadValue  = ${"+PIT_LDVAL_KEY+"};\n\n"+
         "   //! PIT operation in debug mode\n"+
         "   static constexpr uint32_t mcrValue = (${"+PIT_MCR_FRZ_KEY+"}<<PIT_MCR_FRZ_SHIFT);\n\n" +
         "   //! PIT IRQ Level in NVIC\n"+
         "   static constexpr uint32_t irqLevel = ${"+PIT_IRQ_LEVEL_KEY+"};\n\n"+
         "   //! Indicates that naked interrupt handlers are used rather that software table\n"+
         "   #define PIT_USES_NAKED_HANDLER ${"+PIT_USES_NAKED_HANDLER_KEY+"}\n\n";
   
   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      pinMappingHeaderFile.write(substitute(TEMPLATE, fVariableMap));
   }

   @Override
   public BaseModel[] getModels(BaseModel parent) {
      BaseModel models[] = {
            new CategoryModel(parent, getName(), getDescription()),
         };

      new VariableModel(models[0], this, PIT_LDVAL_KEY).setName(fVariableMap.get(PIT_LDVAL_KEY).name);

      new SimpleSelectionModel(models[0], this, PIT_USES_NAKED_HANDLER_KEY, "") {
         {
            setName(fVariableMap.get(PIT_USES_NAKED_HANDLER_KEY).name);
            setToolTip("The interrupt handler may use an external functions named PITx_IRQHandler() or\n"+
                       "may be set by use of the setCallback() function");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0: Interrupt handlers are programmatically set",
                  "1: External functions PITx_IRQHandler() are used",
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

      new SimpleSelectionModel(models[0], this, PIT_MCR_FRZ_KEY, "") {
         {
            setName(fVariableMap.get(PIT_MCR_FRZ_KEY).name);
            setToolTip(" When FRZ is set, the PIT will pause when in debug mode");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "0: Timers continue to run in debug mode",
                  "1: Timers are stopped in Debug mode",
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

      new VariableModel(models[0], this, PIT_IRQ_LEVEL_KEY).setName(fVariableMap.get(PIT_IRQ_LEVEL_KEY).name);

      return models;
   }

   @Override
   public VariableInfo getVariableInfo(String key) {
      return fVariableMap.get(key);
   }

}