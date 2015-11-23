import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of DigitalIO
 */
class WriterForDigitalIO extends InstanceWriter {

   static final String CLASS_NAME = "digitalIO_";
   
   public WriterForDigitalIO(boolean deviceIsMKE) {
      super(deviceIsMKE, false);
   }

   public WriterForDigitalIO(boolean deviceIsMKE, boolean useGuard) {
      super(deviceIsMKE, useGuard);
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      return CLASS_NAME+mappingInfo.pin.getName(); // e.g. digitalIO_PTA0;
   }

   @Override
   public String getAliasName(String alias) {
      return CLASS_NAME+alias;
   }

   /** 
    * Write DigitalIO template instantiation e.g. 
    * <pre>
    * extern const Port<b><i>A</b></i>_T&lt;<b><i>0</b></i>&gt;  digitalIO_PT<b><i>A0</b></i>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   @Override
   public void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {
      cppFile.write("extern ");
      writeDefinition(mappingInfo, fnIndex, cppFile);
   }

   /** 
    * Write DigitalIO template instantiation e.g. 
    * <pre>
    * const Port<b><i>A</b></i>_T&lt;<b><i>0</b></i>&gt;  digitalIO_PT<b><i>A0</b></i>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   @Override
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {

      String instance         = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal           = mappingInfo.functions.get(fnIndex).fSignal;
      String classRef         = CreatePinDescription.NAME_SPACE + "::" + "Port" + instance +"_T<"+signal+">";
      String classDef         = "digitalIO_PT" + instance + signal + ";";
      String comment          = "//!< See @ref DigitalIO";
      cppFile.write(String.format("const %-19s %-20s %s\n", classRef, classDef, comment));
      
   }
}