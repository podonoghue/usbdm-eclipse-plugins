import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class representing information about a type of pin
 */
class PinTemplateInformation {

   static private ArrayList<PinTemplateInformation> list = new ArrayList<PinTemplateInformation>();
//   static private HashMap<String, PinTemplateInformation> map = new HashMap<String, PinTemplateInformation>();

   static void reset() {
//      map = new HashMap<String, PinTemplateInformation>();
      list = new ArrayList<PinTemplateInformation>();
   }

   static PinTemplateInformation getTemplate(String baseName) {
      for (PinTemplateInformation item:list) {
         if (item.baseName == baseName) {
            return item;
         }
      }
      return null;
   }

   static ArrayList<PinTemplateInformation> getList() {
//      ArrayList<String> list = new ArrayList<String>(map.keySet());
//      Collections.sort(list);
      return list;
   }

   static abstract interface IInstanceWriter {
      public void writeInstance(MappingInfo mappedPeripheral, int instanceCount, BufferedWriter cppFile) throws IOException;
   };

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
         String pinName          = mappingInfo.pin.getName();
         String instanceName     = "digitalIO_"+pinName;                                    // digitalIO_PTA0
//         String pcrInstance      = "&PCR("+pinName+"_GPIO_NAME,"+pinName+"_GPIO_BIT),";   // &PCR(PTA0_GPIO_NAME,PTA0_GPIO_BIT)
//         String gpioInstance     = "GPIO("+pinName+"_GPIO_NAME),";                        // GPIO(PTA0_GPIO_NAME),
//         String gpioInstanceMKE  = "(volatile GPIO_Type*)GPIO("+pinName+"_GPIO_NAME),";   // (volatile GPIO_Type*)GPIO(PTA0_GPIO_NAME),
//         String gpioClockMask    =  "PORT_CLOCK_MASK("+pinName+"_GPIO_NAME),";            // PORT_CLOCK_MASK(PTA0_GPIO_NAME),
//         String gpioBitMask      = "(1UL<<"+pinName+"_GPIO_BIT)";                         // (1UL<<PTA0_GPIO_BIT)

         String instance = mappingInfo.function.fInstance;
         String signal   = mappingInfo.function.fSignal;
         String pcrInstance      = String.format("&PORT%s->PCR[%s],", instance, signal);     // &PORTx->PCR[n]
         String gpioInstance     = String.format("GPIO%s,", instance);                       // GPIOx,
         String gpioInstanceMKE  = String.format("(volatile GPIO_Type*)GPIO%s,", instance);  // (volatile GPIO_Type*)GPIOx,
         String gpioClockMask    = String.format("PORT%s_CLOCK_MASK,", instance);            // PORTx_CLOCK_MASK,
         String gpioBitMask      = String.format("(1UL<<%s)", signal);                       // (1UL<<n)
         
         if (deviceIsMKE) {
            cppFile.write(String.format("const DigitalIO %-18s = {%-18s%s};\n", 
                  instanceName, gpioInstanceMKE, gpioBitMask));
         }
         else {
            cppFile.write(String.format("const DigitalIO %-18s = {%-18s%-10s%-20s%s};\n", 
                  instanceName, pcrInstance, gpioInstance, gpioClockMask, gpioBitMask));
         }
      }
   };

   static class pwmIO_FTM_Writer implements IInstanceWriter {
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
//         String ftmNum       = mappingInfo.function.fInstance;
         String pinName      = mappingInfo.pin.getName();

//         String instanceName = "pwmIO_"+pinName;                                 // pwmIO_PTA0
//         String gpioName     = "&digitalIO_"+pinName+",";                        // &digitalIO_PTA0,
//         String ftmInstance  = "(volatile FTM_Type*)FTM("+pinName+"_FTM_NUM),";  // (volatile FTM_Type*)FTM(PTA0_FTM_NUM),
//         String ftmChannel   = pinName+"_FTM_CH,";                               // PTA0_FTM_CH
//         String ftmMuxValue  = "PORT_PCR_MUX("+pinName+"_FTM_FN),";              // PORT_PCR_MUX(PTA0_FTM_FN);
//         String ftmClockReg  = "&FTM_CLOCK_REG("+pinName+"_FTM_NUM),";           // &FTM_CLOCK_REG(PTA0_FTM_NUM),
//         String ftmClockMask = "FTM_CLOCK_MASK("+pinName+"_FTM_NUM),";           // FTM_CLOCK_MASK(PTA0_FTM_NUM),
//         String ftmSCValue   = "FTM"+ftmNum+"_SC";                               // FTM0_SC

         String instance = mappingInfo.function.fInstance;
         String signal   = mappingInfo.function.fSignal;
         String muxValue = Integer.toString(mappingInfo.mux);
         
         String instanceName = "pwmIO_"+pinName;                                       // pwmIO_PTA0
         String gpioName     = String.format("&digitalIO_%s,", pinName);               // &digitalIO_PTA0,
         String ftmInstance  = String.format("(volatile FTM_Type*)FTM%s,", instance);  // (volatile FTM_Type*)FTMx,
         String ftmChannel   = String.format("%s,", signal);                           // n
         String ftmMuxValue  = String.format("%s,", muxValue);                         // m;
         String ftmClockReg  = String.format("&FTM%s_CLOCK_REG,", instance);           // &FTMx_CLOCK_REG,
         String ftmClockMask = String.format("FTM%s_CLOCK_MASK,", instance);           // FTMx_CLOCK_MASK,
         String ftmSCValue   = String.format("FTM%s_SC", instance);                    // FTMx_SC

         cppFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappingInfo.mux)));
         cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-28s%-6s%-6s%-20s%s %s};\n", 
               instanceName, gpioName, ftmInstance, ftmChannel, ftmMuxValue, ftmClockReg, ftmClockMask,ftmSCValue) );
         cppFile.write(String.format("#endif\n"));
      }
   };

   static class pwmIO_TPM_Writer implements IInstanceWriter {      
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
//         String ftmNum       = mappingInfo.function.fInstance;
         String pinName      = mappingInfo.pin.getName();

//         String instanceName = "pwmIO_"+pinName;                                 // pwmIO_PTA0
//         String gpioName     = "&digitalIO_"+pinName+",";                        // &digitalIO_PTA0,
//         String ftmInstance  = "(volatile TPM_Type*)TPM("+pinName+"_TPM_NUM),";  // (volatile TPM_Type*)TPM(PTA0_TPM_NUM),
//         String ftmChannel   = pinName+"_TPM_CH,";                               // PTA0_TPM_CH
//         String ftmMuxValue  = "PORT_PCR_MUX("+pinName+"_TPM_FN),";              // PORT_PCR_MUX(PTA0_TPM_FN);
//         String ftmClockReg  = "&TPM_CLOCK_REG("+pinName+"_TPM_NUM),";           // &TPM_CLOCK_REG(PTA0_TPM_NUM),
//         String ftmClockMask = "TPM_CLOCK_MASK("+pinName+"_TPM_NUM),";           // TPM_CLOCK_MASK(PTA0_TPM_NUM),
//         String ftmSCValue   = "TPM"+ftmNum+"_SC";                               // TPM0_SC

         String instance = mappingInfo.function.fInstance;
         String signal   = mappingInfo.function.fSignal;
         String muxValue = Integer.toString(mappingInfo.mux);
         
         String instanceName = "pwmIO_"+pinName;                                       // pwmIO_PTA0
         String gpioName     = String.format("&digitalIO_%s,", pinName);               // &digitalIO_PTA0,
         String ftmInstance  = String.format("(volatile TPM_Type*)TPM%s,", instance);  // (volatile TPM_Type*)TPMx,
         String ftmChannel   = String.format("%s,", signal);                           // n
         String ftmMuxValue  = String.format("%s,", muxValue);                         // m;
         String ftmClockReg  = String.format("&TPM%s_CLOCK_REG,", instance);           // &FTMx_CLOCK_REG,
         String ftmClockMask = String.format("TPM%s_CLOCK_MASK,", instance);           // FTMx_CLOCK_MASK,
         String ftmSCValue   = String.format("TPM%s_SC", instance);                    // FTMx_SC
         cppFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappingInfo.mux)));
         cppFile.write(String.format("const PwmIO  %-15s = {%-19s%-28s%-6s%-6s%-20s%s %s};\n", 
               instanceName, gpioName, ftmInstance, ftmChannel, ftmMuxValue, ftmClockReg, ftmClockMask,ftmSCValue) );
         cppFile.write(String.format("#endif\n"));
      }
   };

   static class analogueIO_Writer implements IInstanceWriter {      
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
//         String modifier = "";
//         String pinName      = mappingInfo.pin.getName();
//         String instanceName = "analogueIO_"+pinName+modifier;                      // analogueIO_PTE1
//         String gpioName     = "&digitalIO_"+pinName+",";                           // &digitalIO_PTE1,
//         String adcInstance  = "ADC("+pinName+"_ADC_NUM"+modifier+"),";             // ADC(PTE1_ADC_NUM),
//         String adcClockReg  = "&ADC_CLOCK_REG("+pinName+"_ADC_NUM"+modifier+"),";  // &ADC_CLOCK_REG(PTE1_ADC_NUM),
//         String adcClockMask = "ADC_CLOCK_MASK("+pinName+"_ADC_NUM"+modifier+"),";  // ADC_CLOCK_MASK(PTE1_ADC_NUM),
//         String adcChannel   = pinName+"_ADC_CH"+modifier+"";                       // PTE1_ADC_CH

         String instance = mappingInfo.function.fInstance;
         String signal   = mappingInfo.function.fSignal;
//         String muxValue = Integer.toString(mappingInfo.mux);
         
         String pinName      = mappingInfo.pin.getName();
         String instanceName = "analogueIO_"+pinName;                        // analogueIO_PTE1
         if (instanceCount>0) {
            instanceName += "_"+Integer.toString(instanceCount);
         }
         String gpioName     = "&digitalIO_"+pinName+",";                    // &digitalIO_PTE1,
         String adcInstance  = String.format("ADC%s,", instance);            // ADC(PTE1_ADC_NUM),
         String adcClockReg  = String.format("&ADC%s_CLOCK_REG,", instance); // &ADCx_CLOCK_REG,
         String adcClockMask = String.format("ADC%s_CLOCK_MASK,", instance); // ADC_CLOCK_MASK(PTE1_ADC_NUM),
         String adcChannel   = signal;                                       // nn

         if (mappingInfo.pin.getMappingList("GPIO").size() == 0) {
            // No PCR register - Only analogue function on pin
            gpioName = "0,"; // NULL indicates no PCR
         }
         cppFile.write(String.format("#if %s == %s\n", pinName+"_SEL", Integer.toString(mappingInfo.mux)));
         cppFile.write(String.format("const AnalogueIO %-25s = {%-18s%-10s%-20s%-20s%s};\n", 
               instanceName, gpioName, adcInstance, adcClockReg, adcClockMask, adcChannel));
         cppFile.write(String.format("#endif\n"));
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