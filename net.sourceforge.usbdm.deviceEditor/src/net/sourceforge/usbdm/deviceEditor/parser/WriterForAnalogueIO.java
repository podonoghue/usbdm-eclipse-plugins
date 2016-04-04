package net.sourceforge.usbdm.deviceEditor.parser;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.information.MappingInfo;
import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralFunction;
import net.sourceforge.usbdm.deviceEditor.information.PeripheralTemplateInformation;

/**
 * Class encapsulating the code for writing an instance of AnalogueIO
 */
/**
 * @author podonoghue
 *
 */
public class WriterForAnalogueIO extends Peripheral {      

   static private final String ALIAS_PREFIX = "adc_";
   
   /** Functions that use this writer */
   protected InfoTable fSeFunctions = new InfoTable("infoSE");
         
   /** Functions that use this writer */
   protected InfoTable fDmFunctions = new InfoTable("infoDM");
         
   /** Functions that use this writer */
   protected InfoTable fDpFunctions = new InfoTable("infoDP");
         
   public WriterForAnalogueIO(String basename, String instance, PeripheralTemplateInformation template, DeviceInfo deviceInfo) {
      super(basename, instance, template, deviceInfo);
   }

   @Override
   public String getTitle() {
      return "Analogue Input";
   }

   @Override
   public String getAliasName(String signalName, String alias) {
      Pattern p = Pattern.compile(".*(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(signalName);
      if (!m.matches()) {
         throw new RuntimeException("Function " + signalName +" does not match expected pattern");
      }
      String signalType = m.group(1);
      if (signalType.equalsIgnoreCase("SE")) {
         return ALIAS_PREFIX+alias;
      }
      else if (signalType.equalsIgnoreCase("DM")) {
         return null;
      }
      else if (signalType.equalsIgnoreCase("DP")) {
         return null;
      }
      return null;
   }

   @Override
   public String getInstanceName(MappingInfo mappingInfo, int fnIndex) {
      String instance = mappingInfo.getFunctions().get(fnIndex).getPeripheral().getInstance();
      String signal   = mappingInfo.getFunctions().get(fnIndex).getSignal();
      return getClassName()+instance+"_se"+signal;
   }

   @Override
   protected String getDeclaration(MappingInfo mappingInfo, int fnIndex) {
      int signal = getFunctionIndex(mappingInfo.getFunctions().get(fnIndex));
      return String.format("const %s::%s<%d>", DeviceInfo.NAME_SPACE, getClassName(), signal);
   }
   
   @Override
   public int getFunctionIndex(PeripheralFunction function) {
      Pattern p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(function.getSignal());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignal() + " does not match expected pattern");
      }
      int index = Integer.parseInt(m.group(2));
      if ((m.group(3) != null) && m.group(3).equalsIgnoreCase("a")) {
         index += 32;
      }
      return index;
   }

   @Override
   public boolean needPCRTable() {
      boolean required = 
            (fPeripheralFunctions.table.size() +
                  fSeFunctions.table.size() + 
                  fDpFunctions.table.size() + 
                  fDmFunctions.table.size()) > 0;
      return required;
   }

   static final String TEMPLATE_DOCUMENTATION = 
         "/**\n"+
         " * Convenience templated class representing an ADC\n"+
         " *\n"+
         " * Example\n"+
         " * @code\n"+
         " *  // Instantiate ADC0 single-ended channel #8\n"+
         " *  const adc0<8> adc0_se8;\n"+
         " *\n"+
         " *  // Initialise ADC\n"+
         " *  adc0_se8.initialiseADC(USBDM::resolution_12bit_se);\n"+
         " *\n"+
         " *  // Set as analogue input\n"+
         " *  adc0_se8.setAnalogueInput();\n"+
         " *\n"+
         " *  // Read input\n"+
         " *  uint16_t value = adc0_se8.readAnalogue();\n"+
         " *  @endcode\n"+
         " *\n"+
         " * @tparam adcChannel    ADC channel\n"+
         " */\n";

   @Override
   public String getCTemplate() {   
      return TEMPLATE_DOCUMENTATION+String.format(
            "template<uint8_t channel> using %s = Adc_T<%sInfo, channel>;\n\n",
            getClassName(), getClassName());
   }

   @Override
   protected void addFunctionToTable(PeripheralFunction function) {
      InfoTable fFunctions = super.fPeripheralFunctions;
      
      Pattern p = Pattern.compile("(SE|DM|DP)(\\d+)(a|b)?");
      Matcher m = p.matcher(function.getSignal());
      if (!m.matches()) {
         throw new RuntimeException("Function "+function+", Signal " + function.getSignal() + " does not match expected pattern");
      }
      int signalIndex = getFunctionIndex(function);
      String signalType = m.group(1);
      if (signalType.equalsIgnoreCase("SE")) {
         fFunctions = fSeFunctions;
      }
      else if (signalType.equalsIgnoreCase("DM")) {
         fFunctions = fDmFunctions;
      }
      else if (signalType.equalsIgnoreCase("DP")) {
         fFunctions = fDpFunctions;
      }
      if (signalIndex>=fFunctions.table.size()) {
         fFunctions.table.setSize(signalIndex+1);
      }
      if ((fFunctions.table.get(signalIndex) != null) && 
            (fFunctions.table.get(signalIndex) != function)) {
         throw new RuntimeException("Multiple functions mapped to index new = " + function + ", old = " + fFunctions.table.get(signalIndex));
      }
      fFunctions.table.setElementAt(function, signalIndex);
   }

   @Override
   public ArrayList<InfoTable> getFunctionTables() {
      ArrayList<InfoTable> rv = new ArrayList<InfoTable>();
      rv.add(fPeripheralFunctions);
      rv.add(fSeFunctions);
      rv.add(fDpFunctions);
      rv.add(fDmFunctions);
      return rv;
   }

}