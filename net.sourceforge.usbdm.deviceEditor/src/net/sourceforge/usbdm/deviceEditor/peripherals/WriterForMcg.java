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
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForMcg extends PeripheralWithState {

   static final String ALIAS_BASE_NAME       = "mcg_";
   static final String CLASS_BASE_NAME       = "Mcg";
   static final String INSTANCE_BASE_NAME    = "mcg";

   /* Keys for MCG */
   public WriterForMcg(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Multipurpose Clock Generator";
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

   @Override
   public int getSignalIndex(Signal function) {
      throw new RuntimeException("Signal does not match expected pattern " + function.getSignalName());
   }
   
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
         "";
   
   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      pinMappingHeaderFile.write(substitute(TEMPLATE));
   }

   @Override
   public BaseModel[] getModels(BaseModel parent) {
      Path path = Paths.get("hardware/mcg.xml");
      try {
         Data data = ParseMenuXML.parseFile(path, parent, this);
         return data.fModels;
      } catch (Exception e) {
         e.printStackTrace();
      }
      BaseModel models[] = {
            new CategoryModel(parent, getName(), getDescription()),
         };
      new ConstantModel(models[0], "Error", "Failed to parse "+path, "");
      return models;
   }

   @Override
   public void getVariables(Map<String, String> variableMap, VectorTable vectorTable) {
      super.getVariables(variableMap, vectorTable);
   }

}