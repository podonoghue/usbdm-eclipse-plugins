import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance
 */
abstract class InstanceWriter {
   
   /** Indicates the device is MKE family */
   protected final boolean fDeviceIsMKE;
   protected PeripheralTemplateInformation fOwner;
   
   /**
    * Get name of documentation group e.g. "DigitalIO_Group"
    * 
    * @return name
    */
   abstract String getGroupName();
   /**
    * Get documentation group title e.g. "Digital Input/Output"
    * 
    * @return name
    */
   abstract String getGroupTitle();
   /**
    * Get Documentation group brief description <br>e.g. "Allows use of port pins as simple digital inputs or outputs"
    * 
    * @return name
    */
   abstract String getGroupBriefDescription();

   /**
    * Create InstanceWriter
    * 
    * @param deviceIsMKE   Indicates the device is MKE family
    * @param useGuard      Indicates that <b><i>#if</b></i> ... <b><i>#endif</b></i> guards should be written
    */
   InstanceWriter(boolean deviceIsMKE) {
      this.fDeviceIsMKE = deviceIsMKE;
   }
   
   /** 
    * Indicates the device is in the MKE family 
    */
   protected boolean deviceIsMKE() {
      return fDeviceIsMKE;
   }

   /** 
    * Indicates that <b><i>#if</b></i> ... <b><i>#endif</b></i> guards should be written 
    */
   public boolean useGuard() {
      return false;
   }

   /**
    * Get instance name e.g. <b><i>gpioA_0</b></i>
    * 
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * 
    * @return  String 
    */
   public abstract String getInstanceName(MappingInfo mappingInfo, int fnIndex);
   
   /** 
    * Write alias definition e.g. 
    * <pre>
    * using <b><i>alias</b></i> = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * </pre>
    * @param alias          Name of alias e.g. ftm_D8
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * @throws IOException 
    * 
    * @throws Exception 
    */
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) {
     String declaration = getDeclaration(mappingInfo, fnIndex);
     if (declaration == null) {
        return null;
     }
     return String.format("using %-20s = %s\n", alias, declaration+";");
   }

   /** 
    * Get declaration as string e.g. 
    * <pre>
    * const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i> 
    * const USBDM::Adc<b><i>0</i></b>&lt;<b><i>0</i></b>, <b><i>0</i></b>, <b><i>19</i></b>>
    * const USBDM::Adc<b><i>1</i></b>&lt;PORT<b><i>E</i></b>_CLOCK_MASK, PORT<b><i>E</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>24</i></b>]), <b><i>17</i></b>>
    * const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * @throws IOException 
    */
   protected abstract String getDeclaration(MappingInfo mappingInfo, int fnIndex);

   /** 
    * Get a definition for a simple single-pin device 
    * <pre>
    * using gpio<b><i>A</b></i>_<b><i>0</b></i>   = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * using adc<b><i>0</i></b>_se<b><i>19</i></b> = const USBDM::Adc<b><i>0</i></b>&lt;<b><i>0</i></b>, <b><i>0</i></b>, <b><i>19</i></b>>;
    * using adc<b><i>1</i></b>_se<b><i>17</i></b> = const USBDM::Adc<b><i>1</i></b>&lt;PORT<b><i>E</i></b>_CLOCK_MASK, PORT<b><i>E</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>24</i></b>]), <b><i>17</i></b>> ;
    * using ftm<b><i>1</i></b>_ch<b><i>17</i></b> = const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * @throws IOException 
    */
   public String getDefinition(MappingInfo mappingInfo, int fnIndex) throws IOException {
      return getAlias(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex);
   }
   
   /** 
    * Write component declaration e.g. 
    * <pre>
    * extern const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i> gpio<b><i>A</b></i>_<b><i>0</b></i>;
    * extern const USBDM::Adc<b><i>0</i></b>&lt;<b><i>19</i></b>&gt adc<b><i>A</b></i>_ch<b><i>0</b></i>;
    * extern const USBDM::Ftm<b><i>1</b></i>&lt;<i><b>17</i></b>> ftm<b><i>1</i></b>_ch<b><i>17</i></b>;
    * </pre>
    * @param mappingInfo   Mapping information (pin and peripheral function)
    * @param fnIndex       Index into list of functions mapped to pin
    * @param cppFile       Where to write
    * @throws Exception 
    */
   public String getExternDeclaration(MappingInfo mappingInfo, int fnIndex) throws Exception {
      return "extern " +  getDefinition(mappingInfo, fnIndex);
   }

   /**
    * Get alias name based on the given alias
    * @param signalName 
    * 
    * @param signalName   Function being mapped to alias e.g. 
    * @param alias        Base for alias name e.g. <b><i>A5</b></i>
    * 
    * @return Alias name e.g. gpio_<b><i>A5</b></i>
    */
   public abstract String getAliasName(String signalName, String alias);
   
   /**
    * Indicates if a Peripheral Information class is required<br>
    * The default implementation does some sanity checks and returns true if functions are present 
    *
    * @return
    * @throws Exception 
    */
   public boolean needPeripheralInformationClass() {
      // Assume required if functions are present
      boolean required = fOwner.getFunctions().size() > 0;
      if (!required) {
         // Shouldn't have clock information for non-existent peripheral 
         if ((fOwner.getClockReg() != null) || (fOwner.getClockMask() != null)) {
            throw new RuntimeException("Unexpected clock information for non-present peripheral " + fOwner.fPeripheralName);
         }
      }
      return required;
   }
   
   /**
    * Provides C template
    * 
    * @return Template
    */
   public String getTemplate() {
      return null;
   }
   
   /**
    * Gets the numeric index of the function for use in PCR tables\n
    * e.g. FTM3_Ch2 => 2 etc.
    * 
    * @param function   Function to look up
    * @return  Index, -1 is returned if template doesn't match
    * 
    * @throws Exception If template matches peripheral but unexpected function 
    */
   public abstract int getFunctionIndex(PeripheralFunction function);

   /**
    * Indicates if pin aliases should be written
    * @param pinInfo 
    * 
    * @return true => write aliases
    */
   public boolean useAliases(PinInformation pinInfo) {
      return true;
   }

   /**
    * Returns the PCR constant to use with pins from this peripheral
    * e.g. <b>DEFAULT_PCR</b>
    * 
    * @return
    */
   public String getPcrValue() {
      return String.format(
            "   //! Base value for PCR (excluding MUX value)\n"+
            "   static constexpr uint32_t pcrValue  = DEFAULT_PCR;\n\n"
            );
      }

   /**
    * Returns a string containing definitions to be included in the information class describing the peripheral
    * 
    * <pre>
    * //! Clock mask for peripheral
    * static constexpr uint32_t clockMask = ADC1_CLOCK_MASK;
    * 
    * //! Address of clock register for peripheral
    * static constexpr uint32_t clockReg  = SIM_BasePtr+offsetof(SIM_Type,ADC1_CLOCK_REG);
    * </pre>
    * 
    * @return Definitions string
    */
   public String getInfoConstants() {
      StringBuffer buff = new StringBuffer();
      
      // Base address
      buff.append(String.format(
            "   //! Hardware base pointer\n"+
            "   static constexpr uint32_t basePtr   = %s\n\n",
            fOwner.fPeripheralName+"_BasePtr;"
            ));

      buff.append(getPcrValue());
      
      if (fOwner.getClockMask() != null) {
         buff.append(String.format(
               "   //! Clock mask for peripheral\n"+
               "   static constexpr uint32_t clockMask = %s;\n\n",
               fOwner.getClockMask()));
      }
      if (fOwner.getClockReg() != null) {
         buff.append(String.format(
               "   //! Address of clock register for peripheral\n"+
               "   static constexpr uint32_t clockReg  = %s;\n\n",
               "SIM_BasePtr+offsetof(SIM_Type,"+fOwner.getClockReg()+")"));
      }
      buff.append(String.format(
            "   //! Number of IRQs for hardware\n"+
            "   static constexpr uint32_t irqCount  = %s;\n\n",
            fOwner.getIrqCount()));
      if (fOwner.getIrqNumsAsInitialiser() != null) {
         buff.append(String.format(
               "   //! IRQ numbers for hardware\n"+
               "   static constexpr IRQn_Type irqNums[]  = {%s};\n\n",
               fOwner.getIrqNumsAsInitialiser()));
      }
      return buff.toString();
   }

   public void setOwner(PeripheralTemplateInformation owner) {
      this.fOwner = owner;
   }

   public String getExtraDefinitions() {
      return "";
   }
}