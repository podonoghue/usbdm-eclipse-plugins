package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.ConstantModel;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML;
import net.sourceforge.usbdm.deviceEditor.xmlParser.ParseMenuXML.Data;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of I2C
 */
public class WriterForI2c extends PeripheralWithState {
   private static final String CLASS_BASE_NAME       = "I2c";
   private static final String INSTANCE_BASE_NAME    = "i2c";

   /** Data about model loaded from file */
   protected Data fData = null;

   public WriterForI2c(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      loadModels();
   }

   @Override
   public String getTitle() {
      return "Inter-Integrated-Circuit Interface";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return INSTANCE_BASE_NAME+instance+"_"+signal;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * @throws Exception 
    */
   protected String getPcrDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      String instance  = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getSignalIndex(mappingInfo.getSignals().get(fnIndex)));
      return "const " + DeviceInfo.NAME_SPACE + "::PcrTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * @throws Exception 
    */
   protected String getGpioDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      String instance  = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal    = Integer.toString(getSignalIndex(mappingInfo.getSignals().get(fnIndex)));
      return "const " + DeviceInfo.NAME_SPACE + "::GpioTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }

   @Override
   public int getSignalIndex(Signal signal) {
      String signalNames[] = {"SCL", "SDA", "4WSCLOUT", "4WSDAOUT"};
      for (int signalName=0; signalName<signalNames.length; signalName++) {
         if (signal.getSignalName().matches(signalNames[signalName])) {
            return signalName;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + signal.getSignalName());
   }
   
   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return String.format("using %-20s = %s\n", alias, getDeclaration(mappingInfo, fnIndex)+";");
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return null;
   }

   @Override
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " + getDefinition(mappingInfo, fnIndex);
   }

   @Override
   public String getPcrDefinition() {
      return String.format(
            "   //! Base value for PCR (excluding MUX value)\n"+
            "   static constexpr uint32_t pcrValue  = I2C_DEFAULT_PCR;\n\n"
            );
   }

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      StringBuffer sb = new StringBuffer();
      sb.append(substitute(fData.fTemplate));
      pinMappingHeaderFile.write(sb.toString());
   }

   public void loadModels() {

      Path path = Paths.get("hardware/i2c.xml");
      try {
         fData = ParseMenuXML.parseFile(path, null, this);
      } catch (Exception e) {
         e.printStackTrace();
         BaseModel models[] = {
               new CategoryModel(null, getName(), getDescription()),
            };
         fData = new Data(models, "");
         new ConstantModel(models[0], "Error", "Failed to parse "+path, "");
      }
   }
   
   @Override
   public BaseModel[] getModels(BaseModel parent) {
      fData.fModels[0].setParent(parent);
      return fData.fModels;
   }

   @Override
   public void getVariables(Map<String, String> variableMap, VectorTable vectorTable) {
      super.getVariables(variableMap, vectorTable);
      final String headerFileName = getBaseName().toLowerCase()+".h";
      boolean handlerSet = false;
      for (InterruptEntry entry:vectorTable.getEntries()) {
         if ((entry != null) && entry.getName().startsWith(fName)) {
            if (getVariableValue("IRQ_HANDLER").equals("1")) {
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