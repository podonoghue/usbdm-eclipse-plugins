package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Class encapsulating the code for writing an instance of PIT
 */
public class WriterForPit extends PeripheralWithState {

   static final String ALIAS_PREFIX       = "pit_";

   /* Keys for PIT */
   private static final String PIT_LDVAL_KEY              = "LDVAL";
   private static final String PIT_MCR_FRZ_KEY            = "MCR_FRZ";
   private static final String PIT_IRQ_LEVEL_KEY          = "IRQ_LEVEL";
   private static final String PIT_IRQ_HANDLER_KEY        = "PIT_IRQ_HANDLER";
   private static final String PIT_IRQ_HANDLER0_KEY       = "PIT_IRQ_HANDLER0";
   private static final String PIT_IRQ_HANDLER1_KEY       = "PIT_IRQ_HANDLER1";
   private static final String PIT_IRQ_HANDLER2_KEY       = "PIT_IRQ_HANDLER2";
   private static final String PIT_IRQ_HANDLER3_KEY       = "PIT_IRQ_HANDLER3";
   
   
   public WriterForPit(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      switch (fDeviceInfo.getDeviceFamily()) {
      case mk:
         createValue(PIT_IRQ_HANDLER0_KEY,         "0",     "Handler for IRQ # 0",  0, 1);
         createValue(PIT_IRQ_HANDLER1_KEY,         "0",     "Handler for IRQ # 1",  0, 1);
         createValue(PIT_IRQ_HANDLER2_KEY,         "0",     "Handler for IRQ # 2",  0, 1);
         createValue(PIT_IRQ_HANDLER3_KEY,         "0",     "Handler for IRQ # 3",  0, 1);
         break;
      case mke:
         break;
      case mkl:
         createValue(PIT_IRQ_HANDLER_KEY,          "0",     "Handler for IRQ #",    0, 1);
         break;
      case mkm:
         break;
      default:
         break;
      
      }
      createValue(PIT_LDVAL_KEY,                "2000",  "Reload value [0-65535]",        0, 65535);
      createValue(PIT_MCR_FRZ_KEY,              "0",     "Freeze in debug mode",      0, 1);
      createValue(PIT_IRQ_LEVEL_KEY,            "0",     "IRQ Level in NVIC [0-15]",  0, 15);
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
         "   static constexpr uint32_t irqLevel = ${"+PIT_IRQ_LEVEL_KEY+"};\n\n";
   
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

      new VariableModel(models[0], this, PIT_LDVAL_KEY, "[LDVAL]").setName(fVariableMap.get(PIT_LDVAL_KEY).name);

      BinaryModel model;

      model = new BinaryModel(models[0], this, PIT_MCR_FRZ_KEY, "[MCR_FRZ]");
      model.setName(fVariableMap.get(PIT_MCR_FRZ_KEY).name);
      model.setToolTip("When FRZ is set, the PIT will pause when in debug mode");
      model.setValue0("Timers continue to run in debug mode",   "0");
      model.setValue1("Timers are stopped in Debug mode", "1");
      
      String tip = "The interrupt handler may be a static member function or\n"+
            "may be set by use of the setCallback() method";
      switch (fDeviceInfo.getDeviceFamily()) {
      case mk:
         model = new BinaryModel(models[0], this, PIT_IRQ_HANDLER0_KEY, "Pit_T<...>::irqHandler0()");
         model.setName(fVariableMap.get(PIT_IRQ_HANDLER0_KEY).name);
         model.setToolTip(tip);
         model.setValue0("No handler installed", "0");
         model.setValue1("Handler installed",    "1");
         model = new BinaryModel(models[0], this, PIT_IRQ_HANDLER1_KEY, "Pit_T<...>::irqHandler1()");
         model.setName(fVariableMap.get(PIT_IRQ_HANDLER1_KEY).name);
         model.setToolTip(tip);
         model.setValue0("No handler installed", "0");
         model.setValue1("Handler installed",    "1");
         model = new BinaryModel(models[0], this, PIT_IRQ_HANDLER2_KEY, "Pit_T<...>::irqHandler2()");
         model.setName(fVariableMap.get(PIT_IRQ_HANDLER2_KEY).name);
         model.setToolTip(tip);
         model.setValue0("No handler installed", "0");
         model.setValue1("Handler installed",    "1");
         model = new BinaryModel(models[0], this, PIT_IRQ_HANDLER3_KEY, "Pit_T<...>::irqHandler3()");
         model.setName(fVariableMap.get(PIT_IRQ_HANDLER3_KEY).name);
         model.setToolTip(tip);
         model.setValue0("No handler installed", "0");
         model.setValue1("Handler installed",    "1");
         break;
      case mke:
         break;
      case mkl:
         model = new BinaryModel(models[0], this, PIT_IRQ_HANDLER_KEY, "Pit_T<...>::irqHandler()");
         model.setName(fVariableMap.get(PIT_IRQ_HANDLER_KEY).name);
         model.setToolTip(tip);
         model.setValue0("No handler installed", "0");
         model.setValue1("Handler installed",    "1");
         break;
      case mkm:
         break;
      default:
         break;
      
      }
      VariableModel vModel = new VariableModel(models[0], this, PIT_IRQ_LEVEL_KEY, "");
      vModel.setName(fVariableMap.get(PIT_IRQ_LEVEL_KEY).name);
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
      Pattern p = Pattern.compile(fName+"((\\d+)?)");
      for (InterruptEntry entry:vectorTable.getEntries()) {
         if (entry != null) {
            Matcher m = p.matcher(entry.getName());
            if (m.matches()) {
               if (getVariableInfo("PIT_IRQ_HANDLER"+m.group(1)).value.equals("1")) {
                  entry.setHandlerName(DeviceInfo.NAME_SPACE+"::"+getClassName()+"::irqHandler"+m.group(1));
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

}