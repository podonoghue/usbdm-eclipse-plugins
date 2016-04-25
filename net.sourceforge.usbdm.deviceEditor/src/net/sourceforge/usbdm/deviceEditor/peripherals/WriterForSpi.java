package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForSpi extends PeripheralWithState {

   static final String ALIAS_BASE_NAME       = "spi_";
   static final String CLASS_BASE_NAME       = "Spi";
   static final String INSTANCE_BASE_NAME    = "spi";

   /* Keys for SPI */
   private static final String SPI_MODE_KEY              = "MODE";
   private static final String SPI_LSBFE_KEY             = "LSBE";
   private static final String SPI_SPEED_KEY             = "SPEED";
   private static final String SPI_IRQ_HANDLER_KEY       = "SPI_IRQ_HANDLER";
   private static final String SPI_IRQ_LEVEL_KEY         = "IRQ_LEVEL";


   public WriterForSpi(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(SPI_MODE_KEY,        "0",        "Mode",                  0, 3);
      createValue(SPI_LSBFE_KEY,       "0",        "Transmission order",    0, 1);
      createValue(SPI_SPEED_KEY,       "10000000", "Speed",                 0, 10000000);
      createValue(SPI_IRQ_HANDLER_KEY, "0",        "Handler for IRQ", 0, 1);
      createValue(SPI_IRQ_LEVEL_KEY,   "0",        "IRQ Level in NVIC [0-15]", 0, 15);
   }

   @Override
   public String getTitle() {
      return "Serial Peripheral Interface";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return INSTANCE_BASE_NAME+instance+"_"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      String instance  = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getSignalIndex(mappingInfo.getSignals().get(fnIndex)));
      return "const " + DeviceInfo.NAME_SPACE + "::PcrTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }

   static final int PCS_START = 3;
   final String signalNames[] = {"SCK", "SIN|MISO", "SOUT|MOSI", "PCS0|PCS|SS", "PCS1", "PCS2", "PCS3", "PCS4", "PCS5"};

   @Override
   public int getSignalIndex(Signal function) {
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignalName().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + function.getSignalName());
   }
   
   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing an SPI pin\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " * using spi0_PCS0 = const USBDM::Spi0Pin<3>;\n"+
         " * @endcode\n"+
         " *\n"+
         " * @tparam spiPinNum    SPI pin number (index into SpiInfo[])\n"+
         " */\n";

   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      // No aliases
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
         "#ifdef SPI_CTAR_LSBFE_SHIFT\n"+
         "   //! Default communication mode: order, clock phase and clock polarity\n"+
         "   static constexpr uint32_t modeValue = (${"+SPI_LSBFE_KEY+"}<<SPI_CTAR_LSBFE_SHIFT)|(${"+SPI_MODE_KEY+"}<<SPI_CTAR_CPHA_SHIFT);\n\n" +
         "#endif\n"+
         "   //! Default speed (Hz)\n"+
         "   static constexpr uint32_t speed = ${"+SPI_SPEED_KEY+"};\n\n"+
         "   //! Default IRQ level\n"+
         "   static constexpr uint32_t irqLevel =  ${"+SPI_IRQ_LEVEL_KEY+"};\n\n";
   
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

      new SimpleSelectionModel(models[0], this, SPI_MODE_KEY, "[CTAR_CPOL,CTAR_CPHA]") {
         {
            setName(fVariableMap.get(SPI_MODE_KEY).name);
            setToolTip("Communication modes");
         }
         @Override
         protected String[] getChoicesArray() {
            String SELECTION_NAMES[] = {
                  "Mode 0: CPOL=0, CPHA=0",
                  "Mode 1: CPOL=0, CPHA=1",
                  "Mode 2: CPOL=1, CPHA=0",
                  "Mode 3: CPOL=1, CPHA=1",
                  "Default"
            };
            return SELECTION_NAMES;
         }

         @Override
         protected String[] getValuesArray() {
            final String VALUES[] = {
                  "0", "1", "2", "3",
                  "0", // Default
            };
            return VALUES;
         }
      };

      new VariableModel(models[0], this, SPI_SPEED_KEY, "").setName(fVariableMap.get(SPI_SPEED_KEY).name);

      BinaryModel model;
      
      model = new BinaryModel(models[0], this, SPI_LSBFE_KEY, "[CTAR_LSBFE]");
      model.setName(fVariableMap.get(SPI_LSBFE_KEY).name);
      model.setToolTip("Transmission order");
      model.setValue0("MSB first", "0");
      model.setValue1("LSB first", "1");
      
      model = new BinaryModel(models[0], this, SPI_IRQ_HANDLER_KEY, "");
      model.setName(fVariableMap.get(SPI_IRQ_HANDLER_KEY).name);
      model.setToolTip("Polling or interrupts may be used to update the SPI state machine");
      model.setValue0("Use polling",    "0");
      model.setValue1("Use interrupts", "1");
      
      VariableModel vModel = new VariableModel(models[0], this, SPI_IRQ_LEVEL_KEY, "");
      vModel.setName(fVariableMap.get(SPI_IRQ_LEVEL_KEY).name);
      vModel.setToolTip("Sets the priority level used to configure the NVIC");

      return models;
   }

   @Override
   public VariableInfo getVariableInfo(String key) {
      return fVariableMap.get(key);
   }

   @Override
   public void getVariables(Map<String, String> variableMap, VectorTable vectorTable) {
      final String headerFileName = getBaseName().toLowerCase()+".h";
      super.getVariables(variableMap, vectorTable);
      boolean handlerSet = false;
      for (InterruptEntry entry:vectorTable.getEntries()) {
         if ((entry != null) && entry.getName().startsWith(fName)) {
            if (getVariableInfo(SPI_IRQ_HANDLER_KEY).value.equals("1")) {
               entry.setHandlerName(DeviceInfo.NAME_SPACE+"::"+getClassName()+"::irqHandler");
               entry.setClassMemberUsedAsHandler(true);
               handlerSet = true;
            }
         }
      }
      if (handlerSet) {
         String headers = variableMap.get("VectorsIncludeFiles");
         if (!headers.contains(headerFileName)) {
            variableMap.put("VectorsIncludeFiles", headers + "#include \""+headerFileName+"\"\n");
         }
      }
   }

}