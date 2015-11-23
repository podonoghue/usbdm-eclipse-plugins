import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
class WriterForAnalogueIO extends WriterForDigitalIO {      

   static final String CLASS_NAME        = "analogueIO_";

   public WriterForAnalogueIO(boolean deviceIsMKE) {
      super(deviceIsMKE, true);
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      //         return CLASS_NAME+mappingInfo.pin.getName(); // e.g. analogueIO_PTA0;
      return CLASS_NAME+mappingInfo.functions.get(fnIndex).getName(); // e.g. analogueIO_PTA0
   }

   @Override
   public String getAliasName(String alias) {
      return CLASS_NAME+alias;
   }
   /** 
    * Write AnalogueIO template instantiation e.g. 
    * <pre>
    * extern const AnalogueIOT&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type, PCR[<b><i>0</i></b>]), ADC<b><i>1</i></b>_BasePtr, SIM_BasePtr+offsetof(SIM_Type, ADC<b><i>1</i></b>_CLOCK_REG), ADC<b><i>1</i></b>_CLOCK_MASK, <i><b>17</i></b>> analogueIO_ADC<b><i>1</i></b>_SE<b><i>17</i></b>;
    * </pre>
    * or, if no PCR
    * <pre>
    * extern const AnalogueIOT&lt;0, 0, ADC<b><i>1</i></b>_BasePtr, SIM_BasePtr+offsetof(SIM_Type, ADC<b><i>1</i></b>_CLOCK_REG), ADC<b><i>1</i></b>_CLOCK_MASK, <i><b>17</i></b>> analogueIO_ADC<b><i>1</i></b>_SE<b><i>17</i></b>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param suffix         Used to create a unique name when multiple ADC are mappable to the same pin
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
    * Write AnalogueIO template instantiation e.g. 
    * <pre>
    * const AnalogueIOT&lt;PORT<b><i>A</i></b>_CLOCK_MASK, PORT<b><i>A</i></b>_BasePtr+offsetof(PORT_Type, PCR[<b><i>0</i></b>]), ADC<b><i>1</i></b>_BasePtr, SIM_BasePtr+offsetof(SIM_Type, ADC<b><i>1</i></b>_CLOCK_REG), ADC<b><i>1</i></b>_CLOCK_MASK, <i><b>17</i></b>> analogueIO_ADC<b><i>1</i></b>_SE<b><i>17</i></b>;
    * </pre>
    * or, if no PCR
    * <pre>
    * const AnalogueIOT&lt;0, 0, ADC<b><i>1</i></b>_BasePtr, SIM_BasePtr+offsetof(SIM_Type, ADC<b><i>1</i></b>_CLOCK_REG), ADC<b><i>1</i></b>_CLOCK_MASK, <i><b>17</i></b>> analogueIO_ADC<b><i>1</i></b>_SE<b><i>17</i></b>;
    * </pre>
    * @param mappingInfo    Mapping information (pin and peripheral function)
    * @param suffix         Used to create a unique name when multiple ADC are mappable to the same pin
    * @param cppFile        Where to write
    * 
    * @throws IOException
    */
   @Override
   public void writeDefinition(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {

      String instance         = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal           = mappingInfo.functions.get(fnIndex).fSignal;
      String instanceName     = getInstanceName(mappingInfo, fnIndex);                    // e.g. analogueIO_PTE1
      String pcrInit          = FunctionTemplateInformation.getPCRInitString(mappingInfo.pin);
      
      cppFile.write(String.format("const %s::AnalogueIOT<", CreatePinDescription.NAME_SPACE));
      cppFile.write(String.format("%-44s ", pcrInit));
      cppFile.write(String.format("ADC%s_BasePtr, ", instance));
      cppFile.write(String.format("SIM_BasePtr+offsetof(SIM_Type, ADC%s_CLOCK_REG),", instance));
      cppFile.write(String.format("ADC%s_CLOCK_MASK,", instance));
      cppFile.write(String.format("%3s", signal+">"));
      cppFile.write(String.format(" %s;\n", instanceName));
   }
}