package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.Map;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.VectorTable;

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
      loadModels();
   }

   @Override
   public String getTitle() {
      return "Low Power Timer";
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getSignals().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getSignals().get(fnIndex).getSignalName();
      return getClassName()+instance+"_"+signal;
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

   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      StringBuffer sb = new StringBuffer();
      sb.append(substitute(fData.fTemplate));
      pinMappingHeaderFile.write(sb.toString());
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
            if (isCTrueValue("IRQ_HANDLER")) {
               entry.setHandlerName(DeviceInfo.NAME_SPACE+"::"+getClassName()+"::irqHandler");
               entry.setClassMemberUsedAsHandler(true);
               handlerSet = true;
            }
         }
      }
      if (handlerSet) {
         String headers = variableMap.get("VectorsIncludeFiles");
         if (!headers.contains(headerFileName)) {
            // Add include file
            variableMap.put("VectorsIncludeFiles", headers + "#include \""+headerFileName+"\"\n");
         }
      }
   }

}