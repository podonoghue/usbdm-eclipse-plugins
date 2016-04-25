package net.sourceforge.usbdm.deviceEditor.peripherals;

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
/**
 * @author podonoghue
 *
 */
public class WriterForI2c extends PeripheralWithState {
   private static final String CLASS_BASE_NAME       = "I2c";
   private static final String INSTANCE_BASE_NAME    = "i2c";

   private static final String I2C_IRQ_HANDLER_KEY = "I2C_IRQ_HANDLER";
   private static final String I2C_IRQ_LEVEL_KEY   = "I2C_IRQ_LEVEL";

   public WriterForI2c(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(I2C_IRQ_HANDLER_KEY, "0", "Handler for IRQ", 0, 1);
      createValue(I2C_IRQ_LEVEL_KEY,   "0", "IRQ Level in NVIC [0-15]", 0, 15);
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
//      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"Pcr<"+signal+">";
      return "const " + DeviceInfo.NAME_SPACE + "::PcrTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
//      return "const " + CreatePinDescription.NAME_SPACE + "::PcrTable_T<" + signal + ", " + CLASS_BASE_NAME + instance + "Info>";
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
//      return "const " + CreatePinDescription.NAME_SPACE + "::" + CLASS_BASE_NAME + instance +"Gpio<"+signal+">";
//      return "const " + CreatePinDescription.NAME_SPACE + "::GpioTable_T<" + signal + ", " + CLASS_BASE_NAME + instance + "Info>";
      return "const " + DeviceInfo.NAME_SPACE + "::GpioTable_T<" + CLASS_BASE_NAME + instance + "Info, " + signal + ">" ;
   }

   @Override
   public int getSignalIndex(Signal function) {
      String signalNames[] = {"SCL", "SDA", "4WSCLOUT", "4WSDAOUT"};
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignalName().matches(signalNames[signal])) {
            return signal;
         }
      }
      throw new RuntimeException("Signal does not match expected pattern " + function.getSignalName());
   }
   
   @Override
   public String getAliasDeclaration(String alias, MappingInfo mappingInfo, int fnIndex) {
      return String.format("using %-20s = %s\n", alias, getDeclaration(mappingInfo, fnIndex)+";");
   }

   @Override
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) {
      return null;
//      return String.format("using %-14s = %s\n", getInstanceName(mappingInfo, fnIndex)+"Pcr",  getPcrDeclaration(mappingInfo, fnIndex)+";") +
//             String.format("using %-14s = %s\n", getInstanceName(mappingInfo, fnIndex)+"Gpio", getGpioDeclaration(mappingInfo, fnIndex)+";");
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
   public BaseModel[] getModels(BaseModel parent) {
      BaseModel models[] = {
            new CategoryModel(parent, getName(), getDescription()),
      };
      BinaryModel model;
      
      model = new BinaryModel(models[0], this, I2C_IRQ_HANDLER_KEY, "");
      model.setName(fVariableMap.get(I2C_IRQ_HANDLER_KEY).name);
      model.setToolTip("Polling or interrupts may be used to update the I2C state machine");
      model.setValue0("Use polling",    "0");
      model.setValue1("Use interrupts", "1");
      
      VariableModel vModel = new VariableModel(models[0], this, I2C_IRQ_LEVEL_KEY, "");
      vModel.setName(fVariableMap.get(I2C_IRQ_LEVEL_KEY).name);
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
            if (getVariableInfo(I2C_IRQ_HANDLER_KEY).value.equals("1")) {
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