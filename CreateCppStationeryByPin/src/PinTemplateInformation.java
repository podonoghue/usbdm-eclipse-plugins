import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class representing information about a type of pin
 */
class PinTemplateInformation {

   /**
    * List of templates
    */
   static private ArrayList<PinTemplateInformation> list = new ArrayList<PinTemplateInformation>();

   static void reset() {
      list = new ArrayList<PinTemplateInformation>();
   }

   /**
    * Get list of templates for given basename
    * 
    * @return list
    */
   static PinTemplateInformation getTemplate(String baseName) {
      for (PinTemplateInformation item:list) {
         if (item.baseName == baseName) {
            return item;
         }
      }
      return null;
   }

   /**
    * Get list of all templates
    * 
    * @return
    */
   static ArrayList<PinTemplateInformation> getList() {
      return list;
   }

   /**
    * Class encapsulating the code for writing an instance
    */
   static abstract interface IInstanceWriter {
      public void writeInstance(MappingInfo mappingInfo, int instanceCount, BufferedWriter cppFile) throws IOException;
   };

   /**
    * Get PCR initialisation string for given pin
    * 
    * @param pin The pin being configured
    * 
    * @return
    */
   private static String getPCRInitString(PinInformation pin) throws IOException {
      String pcrInstance      = pin.getPCR()+",";
      String portClockMask    = pin.getClockMask();

      boolean noDigitalIO = pin.getMappingList("GPIO").size() == 0;
      if (noDigitalIO) {
         // No PCR register - Only analogue function on pin
         return "{0,0}";
      }
      return String.format("{%-18s%-10s}", pcrInstance, portClockMask);
   }

   /**
    * Class encapsulating the code for writing an instance of DigitalIO
    */
   static class digitalIO_Writer implements IInstanceWriter {
      private final boolean deviceIsMKE;

      public digitalIO_Writer(boolean deviceIsMKE) {
         this.deviceIsMKE = deviceIsMKE;
      }
      /** 
       * Write DigitalIO instance e.g. 
       * <pre>
       * const DigitalIO digitalIO_<b><i>PTA17</i></b> = {&PCR(<b><i>PTA17</i></b>_GPIO_NAME,<b><i>PTA17</i></b>_GPIO_BIT),GPIO(<b><i>PTA17</i></b>_GPIO_NAME),PORT_CLOCK_MASK(<b><i>PTA17</i></b>_GPIO_NAME),(1UL<<<b><i>PTA17</i></b>_GPIO_BIT)};
       * </pre>
       * or for MKE devices
       * <pre>
       * const DigitalIO digitalIO_<b><i>PTA17</i></b> = {(volatile GPIO_Type*)GPIO(<b><i>PTA17</i></b>_GPIO_NAME),(1UL<<<b><i>PTA17</i></b>_GPIO_BIT)};
       * </pre>
       * @param mappingInfo    Mapping information (pin and peripheral function)
       * @param cppFile        Where to write
       * 
       * @throws IOException
       */
      @Override
      public void writeInstance(MappingInfo mappingInfo, int instanceCount, BufferedWriter cppFile) throws IOException {
         String pinName          = mappingInfo.pin.getName(); // e.g. PTA0
         String instanceName     = "digitalIO_"+pinName;      // e.g. digitalIO_PTA0
         
         String instance         = mappingInfo.function.fPeripheral.fInstance;
         String signal           = mappingInfo.function.fSignal;
         String gpioInstance     = String.format("GPIO%s,", instance);                       // GPIOx,
         String gpioInstanceMKE  = String.format("(volatile GPIO_Type*)GPIO%s,", instance);  // (volatile GPIO_Type*)GPIOx,
         String gpioBitMask      = String.format("(1UL<<%s)", signal);                       // (1UL<<n)
         String pcrInit          = getPCRInitString(mappingInfo.pin);
               
         cppFile.write(String.format("const DigitalIO %-18s = ", instanceName));
         boolean noDigitalIO = mappingInfo.pin.getMappingList("GPIO").size() == 0;
         if (deviceIsMKE) {
            if (noDigitalIO) {
               // No PCR register - Only analogue function on pin
               cppFile.write("{0,0}");
            }
            else {
               cppFile.write(String.format("{%-18s%s};\n", gpioInstanceMKE, gpioBitMask));
            }
         }
         else {
            if (noDigitalIO) {
               // No PCR register - Only analogue function on pin
               cppFile.write("{0,0,0,0}");
            }
            else {
               cppFile.write(String.format("{%s, %-8s%s}", pcrInit, gpioInstance, gpioBitMask));
            }
         }
         cppFile.write(String.format(";\n"));
      }
   };

   /**
    * Class encapsulating the code for writing an instance of PwmIO (FTM)
    */
   static class pwmIO_FTM_Writer extends digitalIO_Writer {

      public pwmIO_FTM_Writer(boolean deviceIsMKE) {
         super(deviceIsMKE);
      }

      /** 
       * Write PwmIO instance for a FTM e.g. 
       * <pre>
       * const PwmIO  pwmIO_<b><i>PTA17</i></b> = {&digitalIO_<b><i>PTA17</i></b>, (volatile FTM_Type*)FTM(<b><i>PTA17</i></b>_FTM_NUM), <b><i>PTA17</i></b>_FTM_CH, PORT_PCR_MUX(<b><i>PTA17</i></b>_FTM_FN), &FTM_CLOCK_REG(<b><i>PTA17</i></b>_FTM_NUM), FTM_CLOCK_MASK(<b><i>PTA17</i></b>_FTM_NUM), <b><i>FTM0</b></i>_SC};
       * </pre>
       * @param mappingInfo    Mapping information (pin and peripheral function)
       * @param cppFile        Where to write
       * 
       * @note It is not allowable to simultaneously map multiple FTMs to the same pin so no suffix is used.
       * 
       * @throws IOException
       */
      @Override
      public void writeInstance(MappingInfo mappingInfo, int instanceCount, BufferedWriter cppFile) throws IOException {
         String pinName      = mappingInfo.pin.getName();

         String instance = mappingInfo.function.fPeripheral.fInstance;
         String signal   = mappingInfo.function.fSignal;
         String muxValue = Integer.toString(mappingInfo.mux);
         
         String instanceName = "pwmIO_"+pinName;                                       // pwmIO_PTA0
         String ftmInstance  = String.format("(volatile FTM_Type*)FTM%s,", instance);  // (volatile FTM_Type*)FTMx,
         String ftmChannel   = String.format("%s,", signal);                           // n
         String ftmMuxValue  = String.format("%s,", muxValue);                         // m;
         String ftmClockReg  = String.format("&FTM%s_CLOCK_REG,", instance);           // &FTMx_CLOCK_REG,
         String ftmClockMask = String.format("FTM%s_CLOCK_MASK,", instance);           // FTMx_CLOCK_MASK,
         String ftmSCValue   = String.format("FTM%s_SC", instance);                    // FTMx_SC
         String pcrInit      = getPCRInitString(mappingInfo.pin);

//         cppFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappingInfo.mux)));
         cppFile.write(String.format("const PwmIO  %-15s = {", instanceName));
         cppFile.write(String.format("%s,%-28s%-6s%-6s%-20s%s %s};\n", 
               pcrInit, ftmInstance, ftmChannel, ftmMuxValue, ftmClockReg, ftmClockMask,ftmSCValue) );
//         cppFile.write(String.format("#endif\n"));
      }
   };

   /**
    * Class encapsulating the code for writing an instance of PwmIO (TPM)
    */
   static class pwmIO_TPM_Writer extends digitalIO_Writer {     
      
      public pwmIO_TPM_Writer(boolean deviceIsMKE) {
         super(deviceIsMKE);
      }

      /** 
       * Write PwmIO instance for a TPM e.g. 
       * <pre>
       * const PwmIO  pwmIO_<b><i>PTA17</i></b> = {&digitalIO_<b><i>PTA17</i></b>, (volatile TPM_Type*)TPM(<b><i>PTA17</i></b>_TPM_NUM), <b><i>PTA17</i></b>_TPM_CH, PORT_PCR_MUX(<b><i>PTA17</i></b>_TPM_FN), &TPM_CLOCK_REG(<b><i>PTA17</i></b>_TPM_NUM), TPM_CLOCK_MASK(<b><i>PTA17</i></b>_TPM_NUM), <b><i>TPM0</b></i>_SC};
       * </pre>
       * @param mappingInfo    Mapping information (pin and peripheral function)
       * @param cppFile        Where to write
       * 
       * @note It is not allowable to simultaneously map multiple TPMs to the same pin so no suffix is used.
       * 
       * @throws IOException
       */
      @Override
      public void writeInstance(MappingInfo mappingInfo, int instanceCount, BufferedWriter cppFile) throws IOException {
         String pinName      = mappingInfo.pin.getName();

         String instance = mappingInfo.function.fPeripheral.fInstance;
         String signal   = mappingInfo.function.fSignal;
         String muxValue = Integer.toString(mappingInfo.mux);
         
         String instanceName = "pwmIO_"+pinName;                                       // pwmIO_PTA0
         String ftmInstance  = String.format("(volatile TPM_Type*)TPM%s,", instance);  // (volatile TPM_Type*)TPMx,
         String ftmChannel   = String.format("%s,", signal);                           // n
         String ftmMuxValue  = String.format("%s,", muxValue);                         // m;
         String ftmClockReg  = String.format("&TPM%s_CLOCK_REG,", instance);           // &FTMx_CLOCK_REG,
         String ftmClockMask = String.format("TPM%s_CLOCK_MASK,", instance);           // FTMx_CLOCK_MASK,
         String ftmSCValue   = String.format("TPM%s_SC", instance);                    // FTMx_SC
         String pcrInit      = getPCRInitString(mappingInfo.pin);

//         cppFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappingInfo.mux)));
         cppFile.write(String.format("const PwmIO  %-15s = {", instanceName));
         cppFile.write(String.format("%s, %-28s%-6s%-6s%-20s%s %s};\n", 
               pcrInit, ftmInstance, ftmChannel, ftmMuxValue, ftmClockReg, ftmClockMask,ftmSCValue) );
//         cppFile.write(String.format("#endif\n"));
      }
   };

   /**
    * Class encapsulating the code for writing an instance of AnalogueIO
    */
   static class analogueIO_Writer extends digitalIO_Writer {      
      
      public analogueIO_Writer(boolean deviceIsMKE) {
         super(deviceIsMKE);
      }

      /** 
       * Write AnalogueIO instance e.g. 
       * <pre>
       * const AnalogueIO analogueIO_<b><i>PTA17</i></b> = {&digitalIO_<b><i>PTA17</i></b>, ADC(<b><i>PTA17</i></b>_ADC_NUM), &ADC_CLOCK_REG(<b><i>PTA17</i></b>_ADC_NUM), ADC_CLOCK_MASK(<b><i>PTA17</i></b>_ADC_NUM), <b><i>PTA17</i></b>_ADC_CH};
       * </pre>
       * or, if no PCR
       * <pre>
       * const AnalogueIO analogueIO_<b><i>PTA17</i></b> = {0, ADC(<b><i>PTA17</i></b>_ADC_NUM), &ADC_CLOCK_REG(<b><i>PTA17</i></b>_ADC_NUM), ADC_CLOCK_MASK(<b><i>PTA17</i></b>_ADC_NUM), <b><i>PTA17</i></b>_ADC_CH};
       * </pre>
       * @param mappingInfo    Mapping information (pin and peripheral function)
       * @param suffix         Used to create a unique name when multiple ADC are mappable to the same pin
       * @param cppFile        Where to write
       * 
       * @throws IOException
       */
      @Override
      public void writeInstance(MappingInfo mappingInfo, int instanceCount, BufferedWriter cppFile) throws IOException {

         String instance = mappingInfo.function.fPeripheral.fInstance;
         String signal   = mappingInfo.function.fSignal;
         
         String pinName      = mappingInfo.pin.getName();
         String instanceName = "analogueIO_"+pinName;                        // analogueIO_PTE1
         if (instanceCount>0) {
            instanceName += "_"+Integer.toString(instanceCount);
         }
         String adcInstance  = String.format("ADC%s,", instance);            // ADC(PTE1_ADC_NUM),
         String adcClockReg  = String.format("&ADC%s_CLOCK_REG,", instance); // &ADCx_CLOCK_REG,
         String adcClockMask = String.format("ADC%s_CLOCK_MASK,", instance); // ADC_CLOCK_MASK(PTE1_ADC_NUM),
         String adcChannel   = signal;                                       // N
         String pcrInit      = getPCRInitString(mappingInfo.pin);

//         cppFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappingInfo.mux)));
         cppFile.write(String.format("const AnalogueIO %-25s = {", instanceName));
         cppFile.write(String.format("%s, %-10s%-20s%-20s%s};\n", pcrInit, adcInstance, adcClockReg, adcClockMask, adcChannel));
//         cppFile.write(String.format("#endif\n"));
      }
   };

   String baseName;
   String groupName;
   String groupTitle;
   String groupBriefDescription;
   String externTemplate;
   String className;
   IInstanceWriter instanceWriter;

   /**
    * 
    * @param baseName               e.g. GPIO
    * @param groupName              e.g. "DigitalIO_Group"
    * @param groupTitle             e.g. "Digital Input/Output"
    * @param groupBriefDescription  e.g. "Allows use of port pins as simple digital inputs or outputs"
    * @param className              e.g. "digitalIO_"
    * @param externTemplate         e.g. "extern const DigitalIO %-24s //!< DigitalIO on %s\n"
    */
   public PinTemplateInformation(
         String baseName, String groupName, String groupTitle, 
         String groupBriefDescription, String className, String externTemplate, IInstanceWriter instanceWriter) {
      this.baseName              = baseName;
      this.groupName             = groupName;
      this.groupTitle            = groupTitle;
      this.groupBriefDescription = groupBriefDescription;
      this.externTemplate        = externTemplate;
      this.className             = className;
      this.instanceWriter        = instanceWriter;
      list.add(this);
   }
  
}