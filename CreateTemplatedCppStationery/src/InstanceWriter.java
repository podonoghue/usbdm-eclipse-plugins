import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance
 */
abstract class InstanceWriter {
   
   /** Indicates the device is MKE family */
   private final boolean fDeviceIsMKE;
   
   
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
    * @param alias          Name of alias
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * @throws Exception 
    */
   public String getAlias(String alias, MappingInfo mappingInfo, int fnIndex) throws Exception {
     return String.format("using %-20s = %s\n", alias, getDeclaration(mappingInfo, fnIndex)+";");
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
    * @throws Exception 
    */
   protected abstract String getDeclaration(MappingInfo mappingInfo, int fnIndex) throws IOException, Exception;

   /** 
    * Write component definition e.g. 
    * <pre>
    * using gpio<b><i>A</b></i>_<b><i>0</b></i>   = const USBDM::Gpio<b><i>A</b></i>&lt;<b><i>0</b></i>&gt;</b></i>;
    * using adc<b><i>0</i></b>_se<b><i>19</i></b> = const USBDM::Adc<b><i>0</i></b>&lt;<b><i>0</i></b>, <b><i>0</i></b>, <b><i>19</i></b>>;
    * using adc<b><i>1</i></b>_se<b><i>17</i></b> = const USBDM::Adc<b><i>1</i></b>&lt;PORT<b><i>E</i></b>_CLOCK_MASK, PORT<b><i>E</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>24</i></b>]), <b><i>17</i></b>> ;
    * using ftm<b><i>1</i></b>_ch<b><i>17</i></b> = const USBDM::Ftm<b><i>1</b></i>&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type,PCR[<b><i>0</i></b>]), <i><b>3</i></b>, <i><b>17</i></b>>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * @throws Exception 
    */
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws Exception {
      cppFile.write(getAlias(getInstanceName(mappingInfo, fnIndex), mappingInfo, fnIndex));
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
   public void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws Exception {
      cppFile.write("extern ");
      writeDefinition(mappingInfo, fnIndex, cppFile);
   }

   /**
    * Get alias name based on the given alias
    * 
    * @param alias   Base for alias name e.g. <b><i>A5</b></i>
    * 
    * @return Alias name e.g. gpio_<b><i>A5</b></i>
    */
   public abstract String getAliasName(String alias);
   
   /**
    * Indicates if a PCR table is required
    * 
    * @return
    */
   public abstract boolean needPcrTable();
   
   /**
    * Provides C template
    * 
    * @param pinTemplate   Pin template for information
    * @param instance      
    * 
    * @return Template
    */
   public String getTemplate(FunctionTemplateInformation pinTemplate) {
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
   public abstract int getFunctionIndex(PeripheralFunction function) throws Exception;

   /**
    * Indicates if pin aliases should be written
    * 
    * @return true => write aliases
    */
   public boolean useAliases() {
      return true;
   }

}