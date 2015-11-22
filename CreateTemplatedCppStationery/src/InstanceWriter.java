import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance
 */
abstract class InstanceWriter {
   
   /** Indicates the device is MKE family */
   private final boolean fDeviceIsMKE;
   
   /** Indicates that <b><i>#if</b></i> ... <b><i>#endif</b></i> guards should be written */
   private final boolean fUseGuard;
   
   /**
    * Create InstanceWriter
    * 
    * @param deviceIsMKE   Indicates the device is MKE family
    * @param useGuard      Indicates that <b><i>#if</b></i> ... <b><i>#endif</b></i> guards should be written
    */
   InstanceWriter(boolean deviceIsMKE, boolean useGuard) {
      this.fDeviceIsMKE = deviceIsMKE;
      this.fUseGuard    = useGuard;
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
      return fUseGuard;
   }

   /**
    * Get instance name e.g. digitalIO_<b><i>PTA0</b></i>
    * 
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * 
    * @return  String e.g. digitalIO_<b><i>PTA0</b></i>
    */
   public abstract String getInstanceName(MappingInfo mappingInfo, int fnIndex);
   
   /** 
    * Write component definition e.g. 
    * <pre>
    * const DigitalIO digitalIO_<b><i>PTA0</b></i>  = {{&PORT<b><i>A</b></i>->PCR[<b><i>0</b></i>],   PORT<b><i>A</b></i>_CLOCK_MASK}, GPIO<b><i>A</b></i>,  (1UL<<<b><i>0</b></i>)};
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param fnIndex        Index into list of functions mapped to pin
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   public abstract void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException;
   
   /** 
    * Write component declaration e.g. 
    * <pre>
    * extern const DigitalIO digitalIO_<b><i>PTA0</b></i>;
    * </pre>
    * @param mappingInfo   Mapping information (pin and peripheral function)
    * @param fnIndex       Index into list of functions mapped to pin
    * @param cppFile       Where to write
    * 
    * @throws IOException
    */
   public abstract void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException;

   /**
    * Get alias name based on the given alias
    * 
    * @param alias   Alias name e.g. <b><i>A5</b></i>
    * 
    * @return Alias name e.g. digitalIO_<b><i>A5</b></i>
    */
   public abstract String getAliasName(String alias);
}