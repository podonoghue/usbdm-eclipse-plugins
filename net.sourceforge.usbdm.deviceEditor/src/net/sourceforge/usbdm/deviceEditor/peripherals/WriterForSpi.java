package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.VariableModel;

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
   private static final String SPI_IRQ_LEVEL_KEY         = "IRQ_LEVEL";

   public WriterForSpi(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(SPI_MODE_KEY,                 "0",  "Mode",                  0, 3);
      createValue(SPI_LSBFE_KEY,                "0",  "Transmission order",    0, 1);
      createValue(SPI_SPEED_KEY,         "10000000",  "Speed",                 0, 10000000);
      createValue(SPI_IRQ_LEVEL_KEY,            "0",  "IRQ Level in NVIC [0-15]", 0, 15);
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
         "   //! Default communication mode: order, clock phase and clock polarity\n"+
         "   static constexpr uint32_t modeValue = (${"+SPI_LSBFE_KEY+"}<<SPI_CTAR_LSBFE_SHIFT)|(${"+SPI_MODE_KEY+"}<<SPI_CTAR_CPHA_SHIFT);\n\n" +
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

      new VariableModel(models[0], this, SPI_IRQ_LEVEL_KEY, "").setName(fVariableMap.get(SPI_IRQ_LEVEL_KEY).name);

      BinaryModel model = new BinaryModel(models[0], this, SPI_LSBFE_KEY, "[CTAR_LSBFE]");
      model.setName(fVariableMap.get(SPI_LSBFE_KEY).name);
      model.setToolTip("Transmission order");
      model.setValue0("MSB first", "0");
      model.setValue1("LSB first", "1");
      
      return models;
   }

   @Override
   public VariableInfo getVariableInfo(String key) {
      return fVariableMap.get(key);
   }

//   @Override
//   public void writeExtraDefinitions(DocumentUtilities pinMappingHeaderFile) throws IOException {
//      String name = getClassName();
//      StringBuffer buff = new StringBuffer();
//      for (int index=PCS_START; index<super.fInfoTable.table.size(); index++) {
//         buff.append(String.format("using %s_PCS%s = USBDM::PcrTable_T<USBDM::%sInfo, %s>;\n", name, index-PCS_START, name, index));
//      }
//      pinMappingHeaderFile.write(buff.toString());
//   }
}