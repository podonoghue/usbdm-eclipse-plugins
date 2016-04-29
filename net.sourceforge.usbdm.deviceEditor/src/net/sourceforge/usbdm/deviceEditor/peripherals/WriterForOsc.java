package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;
import net.sourceforge.usbdm.deviceEditor.model.SelectionModel;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForOsc extends PeripheralWithState {

   static final String ALIAS_BASE_NAME       = "osc_";
   static final String CLASS_BASE_NAME       = "Osc";
   static final String INSTANCE_BASE_NAME    = "osc";

   /* Keys for OSC */
   private static final String CR_ERCLKEN_KEY           = "CR_ERCLKEN";
   private static final String CR_EREFSTEN_KEY          = "CR_EREFSTEN";
   private static final String CR_SCP_KEY               = "CR_SCP";


   public WriterForOsc(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(CR_ERCLKEN_KEY,    "0",        "External Reference Enable",            0, 1);
      createValue(CR_EREFSTEN_KEY,   "0",        "External Reference Stop Enable",       0, 1);
      createValue(CR_SCP_KEY,        "0",        "Oscillator load capacitance",          0, 32);
   }

   @Override
   public String getTitle() {
      return "Crystal Oscillator";
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

   final String signalNames[] = {"^XTAL(32)?$", "^EXTAL(32)?$", };

   @Override
   public int getSignalIndex(Signal function) {
      for (int signal=0; signal<signalNames.length; signal++) {
         if (function.getSignalName().matches(signalNames[signal])) {
            return signal;
         }
      }
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
         "   //! External Reference Enable\n"+
         "   static constexpr uint32_t OSC_CR_ERCLKEN_M  = (${"+CR_ERCLKEN_KEY+"}<<OSC_CR_ERCLKEN_SHIFT);\n\n"+
         "   //! External Reference Stop Enable\n"+
         "   static constexpr uint32_t OSC_CR_EREFSTEN_M = (${"+CR_EREFSTEN_KEY+"}<<OSC_CR_EREFSTEN_SHIFT);\n\n"+
         "   //! Oscillator load capacitance\n"+
         "   static constexpr uint32_t OSC_CR_SCP_M      = (${"+CR_SCP_KEY+"}<<OSC_CR_SC16P_SHIFT);\n\n"+
         "";
   @Override
   public void writeInfoConstants(DocumentUtilities pinMappingHeaderFile) throws IOException {
      super.writeInfoConstants(pinMappingHeaderFile);
      pinMappingHeaderFile.write(substitute(TEMPLATE));
   }

   @Override
   public BaseModel[] getModels(BaseModel parent) {
      BaseModel models[] = {
            new CategoryModel(parent, getName(), getDescription()),
         };

      BinaryVariableModel model;
      
      model = new BinaryVariableModel(models[0], this, CR_ERCLKEN_KEY, "[OSC_CR_ERCLKEN]");
      model.setName("External Reference Enable");
      model.setToolTip("Enables external reference clock");
      model.setValue0("Disabled", "0");
      model.setValue1("Enabled", "1");

      model = new BinaryVariableModel(models[0], this, CR_EREFSTEN_KEY, "[OSC_CR_EREFSTEN]");
      model.setName("External Reference Stop Enable");
      model.setToolTip("Determines if external reference clock is enabled in Stop mode");
      model.setValue0("Disabled in Stop mode", "0");
      model.setValue1("Enabled in Stop mode", "1");

      {
         SelectionVariableModel sModel = new SelectionVariableModel(models[0], this, CR_SCP_KEY, "[OSC_CR_SCxP]");
         sModel.setName("Oscillator load capacitance");
         sModel.setToolTip("Configures the oscillator load capacitance");
         String SELECTION_NAMES[] = {
               " 0 pF",
               " 2 pF",
               " 4 pF",
               " 6 pF",
               " 8 pF",
               "10 pF",
               "12 pF",
               "14 pF",
               "16 pF",
               "18 pF",
               "20 pF",
               "22 pF",
               "24 pF",
               "26 pF",
               "28 pF",
               "30 pF",

               "Default"
         };
         final String VALUES[] = {
               "0","8","4","12","2","10","6","14","1","9","5","13","3","11","7","15",
               "0", // Default
         };
         sModel.setChoices(SELECTION_NAMES);
         sModel.setValues(VALUES);
      };
      return models;
   }

}