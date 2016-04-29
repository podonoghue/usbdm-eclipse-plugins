package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.io.IOException;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Signal;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BinaryVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.CategoryModel;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
public class WriterForOsc32 extends PeripheralWithState {

   static final String ALIAS_BASE_NAME       = "osc_";
   static final String CLASS_BASE_NAME       = "Osc";
   static final String INSTANCE_BASE_NAME    = "osc";

   /* Keys for OSC */
   private static final String CR_OSCE_KEY             = "CR_OSCE";
   private static final String CR_CLKO_KEY             = "CR_CLKO";
   private static final String CR_UM_KEY               = "RTC_CR_UM";
   private static final String CR_SUP_KEY              = "RTC_CR_SUP";
   private static final String CR_WPE_KEY              = "RTC_CR_WPE";
   private static final String CR_SCP_KEY              = "CR_SCP";

   public WriterForOsc32(String basename, String instance, DeviceInfo deviceInfo) {
      super(basename, instance, deviceInfo);
      createValue(CR_OSCE_KEY,      "0",        "RTC Oscillator Enable",        0, 1);
      createValue(CR_CLKO_KEY,      "0",        "RTC 32kHz Clock Output",       0, 1);
      createValue(CR_UM_KEY,        "0",        "Update Mode",                  0, 1);
      createValue(CR_SUP_KEY,       "0",        "Supervisor Access",            0, 1);
      createValue(CR_WPE_KEY,       "0",        "Wakeup Pin Enable",            0, 1);
      createValue(CR_SCP_KEY,       "0",        "Oscillator load capacitance",  0, 30);
   }

   @Override
   public String getTitle() {
      return "32kHz Crystal Oscillator";
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
         "   //! RTC Oscillator Enable\n"+
         "   static constexpr uint32_t RTC_CR_OSCE_M  = (${"+CR_OSCE_KEY+"}<<RTC_CR_OSCE_SHIFT);\n\n"+
         "   //!  RTC 32kHz Clock Output\n"+
         "   static constexpr uint32_t RTC_CR_CLKO_M  = (${"+CR_CLKO_KEY+"}<<RTC_CR_CLKO_SHIFT);\n\n"+
         "   //! Update Mode\n"+
         "   static constexpr uint32_t RTC_CR_UM_M    = (${"+CR_UM_KEY+"}<<RTC_CR_UM_SHIFT);\n\n"+
         "   //! Supervisor Access\n"+
         "   static constexpr uint32_t RTC_CR_SUP_M   = (${"+CR_SUP_KEY+"}<<RTC_CR_SUP_SHIFT);\n\n"+
         "   //! Wakeup Pin Enable\n"+
         "   static constexpr uint32_t RTC_CR_WPE_M   = (${"+CR_WPE_KEY+"}<<RTC_CR_WPE_SHIFT);\n\n"+
         "   //! Oscillator load capacitance\n"+
         "   static constexpr uint32_t RCT_CR_SCP_M   = (${"+CR_SCP_KEY+"}<<RTC_CR_SC16P_SHIFT);\n\n"+
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
      
      model = new BinaryVariableModel(models[0], this, CR_OSCE_KEY, "[RTC_CR_OSCE]");
      model.setName("RTC Oscillator Enable");
      model.setToolTip("Enables 32.768 kHz RTC oscillator");
      model.setValue0("Disabled", "0");
      model.setValue1("Enabled", "1");

      model = new BinaryVariableModel(models[0], this, CR_CLKO_KEY, "[RTC_CR_CLKO]");
      model.setName("RTC 32kHz Clock Output");
      model.setToolTip("Determines if RTC 32kHz Clock is available to peripherals");
      model.setValue0("The 32kHz clock is output to other peripherals", "0");
      model.setValue1("The 32kHz clock is not output to other peripherals", "1");

      model = new BinaryVariableModel(models[0], this, CR_UM_KEY, "[RTC_CR_UM]");
      model.setName("Update Mode");
      model.setToolTip("Allows the SR[TCE] to be written even when the Status Register is locked.\n"+
                       "When set, the SR[TCE] can always be written if the SR[TIF] or SR[TOF] are set or if the SR[TCE] is clear");
      model.setValue0("Registers cannot be written when locked", "0");
      model.setValue1("Registers can be written when locked under limited conditions", "1");

      model = new BinaryVariableModel(models[0], this, CR_SUP_KEY, "[RTC_CR_SUP]");
      model.setName("");
      model.setToolTip("Determines if the RTC register access is available in non-supervisor mode\n"+
                       "Non supported write accesses generate a bus error");
      model.setValue0("Non-supervisor mode write accesses are not supported", "0");
      model.setValue1("Non-supervisor mode write accesses are supported", "1");

      model = new BinaryVariableModel(models[0], this, CR_WPE_KEY, "[RTC_CR_WPE]");
      model.setName("Wakeup Pin Enable");
      model.setToolTip("Determines if the wakeup pin is asserted on RTC interrupt when powered down\n"+
                       "The wakeup pin is optional and not available on all devices.");
      model.setValue0("Wakeup pin is disabled", "0");
      model.setValue1("Wakeup pin is enabled", "1");

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
   
      return models;
   }

}