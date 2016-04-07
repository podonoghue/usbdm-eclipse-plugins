package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForSpi extends Peripheral {

   static final String ALIAS_BASE_NAME       = "spi_";
   static final String CLASS_BASE_NAME       = "Spi";
   static final String INSTANCE_BASE_NAME    = "spi";

   public WriterForSpi(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Serial Peripheral Interface";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignal();
      return INSTANCE_BASE_NAME+instance+"_"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      String instance  = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getFunctionIndex(mappingInfo.getSignals().get(fnIndex)));
      return "const " + DeviceInfo.NAME_SPACE + "::PcrTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }

   static final int PCS_START = 3;
   final String signalNames[] = {"SCK", "SIN|MISO", "SOUT|MOSI", "PCS0|PCS|SS", "PCS1", "PCS2", "PCS3", "PCS4", "PCS5"};

   @Override
   public int getFunctionIndex(Signal function) {
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignal().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + function.getSignal());
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

   @Override
   public void writeExtraDefinitions(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.fPeripheralFunctions.table.size();
      String name = getClassName();
      StringBuffer buff = new StringBuffer();
      for (int index=PCS_START; index<super.fPeripheralFunctions.table.size(); index++) {
         buff.append(String.format("using %s_PCS%s = USBDM::PcrTable_T<USBDM::%sInfo, %s>;\n", name, index-PCS_START, name, index));
      }
      pinMappingHeaderFile.write(buff.toString());
   }

}