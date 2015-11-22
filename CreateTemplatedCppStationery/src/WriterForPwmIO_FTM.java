import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Class encapsulating the code for writing an instance of PwmIO (FTM)
 */
class WriterForPwmIO_FTM extends WriterForDigitalIO {

   static final String CLASS_NAME = "pwmIO_";
   
   public WriterForPwmIO_FTM(boolean deviceIsMKE) {
      super(deviceIsMKE, true);
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
//      return CLASS_NAME+mappingInfo.pin.getName(); // e.g. pwmIO_PTA0;
      return CLASS_NAME+mappingInfo.functions.get(fnIndex).getName(); // e.g. pwmIO_FTM0_CH1
   }

   @Override
   public String getAliasName(String alias) {
      return CLASS_NAME+alias;
   }

   @Override
   public void writeDeclaration(MappingInfo mappingInfo, int fnIndex, BufferedWriter cppFile) throws IOException {
      String instance         = mappingInfo.functions.get(fnIndex).fPeripheral.fInstance;
      String signal           = mappingInfo.functions.get(fnIndex).fSignal;
      String instanceName     = getInstanceName(mappingInfo, fnIndex);                    // e.g. analogueIO_PTE1
      String pcrInit          = FunctionTemplateInformation.getPCRInitString(mappingInfo.pin);
      
      cppFile.write(String.format("const PwmIOT<"));
      cppFile.write(String.format("%-44s ", pcrInit));
      cppFile.write(String.format("FTM%s_BasePtr, ", instance));
      cppFile.write(String.format("SIM_BasePtr+offsetof(SIM_Type, FTM%s_CLOCK_REG), ", instance));
      cppFile.write(String.format("FTM%s_CLOCK_MASK, ", instance));
      cppFile.write(String.format("%-3s", signal+","));
      cppFile.write(String.format("%3s", mappingInfo.mux.value+">"));
      cppFile.write(String.format(" %s;\n", instanceName));
   }
}
