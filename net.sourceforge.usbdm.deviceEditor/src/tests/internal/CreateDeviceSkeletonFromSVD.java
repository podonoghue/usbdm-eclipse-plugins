package tests.internal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;
import net.sourceforge.usbdm.deviceEditor.parsers.XML_BaseParser;
import net.sourceforge.usbdm.jni.Usbdm;
import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Cluster;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripherals;
import net.sourceforge.usbdm.peripheralDatabase.DevicePeripheralsFactory;
import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripheralDatabase.Field;
import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;

public class CreateDeviceSkeletonFromSVD {

   StringBuilder resultSb;
   
   /// e.g. "ACMP0"
   private String peripheralName;
   /// e.g. "ACMP"
   private String peripheralBasename;
   /// e.g. "-mke"
   private String fileSuffix;

   
   boolean isSim = false;

   private boolean irqsUsed = false;
   
   private Peripheral peripheral = null;
   
   private ArrayList<String> fieldNameList;
   private ArrayList<String> fieldDefaultList;

   CreateDeviceSkeletonFromSVD(String fileSuffix, DevicePeripherals peripherals, Peripheral peripheral) throws Exception {
      this.fileSuffix     = "-"+fileSuffix;
      this.peripheralName = peripheral.getName();
      this.peripheral     = peripheral;
      irqsUsed = peripheral.getInterruptEntries() != null;
      peripheralBasename  = peripheralName;
      if (peripheralBasename.matches(".*[0-9]")) {
         peripheralBasename = peripheralBasename.substring(0, peripheralBasename.length()-1);
      }
      resultSb = new StringBuilder();
   }
   
   void savePeripheralFiles() throws UsbdmException, IOException {
      // USBDM installation path
      Path hardwarePath = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(DeviceInfo.USBDM_ARM_PERIPHERALS_LOCATION).resolve(peripheral.getSourceFilename().toLowerCase()+"_new"+".xml");
      
      Charset charset = Charset.forName("US-ASCII");
      BufferedWriter writer = Files.newBufferedWriter(hardwarePath, charset);
      writer.write(resultSb.toString(), 0, resultSb.length());
      writer.close();
   }
   
   static String padNewline(String s, String pad) {
      return s.replace("\\n\n", "\\n\n"+pad);
   }

   /**
    * Converts a pin name to more pretty form e.g. PTC6 => Ptc6, VREF_OUT => VrefOut
    * 
    * @param name Original name
    * 
    * @return Converted name
    */
   static public String prettyName(String name) {
      char[] p = name.toLowerCase().toCharArray();
      StringBuffer sb = new StringBuffer();

      // Upper-case 1st character
      boolean convertFlag = true;
      for (int index=0; index<p.length; index++) {
         if (p[index]=='_') {
            // Discard and upper-case next character
            convertFlag = true;
         }
         else if (convertFlag) {
            sb.append(Character.toUpperCase(p[index]));
            convertFlag = false;
         }
         else {
            sb.append(p[index]);
         }
      }
      return sb.toString();
   }

   static String truncateAtNewlineOrTab(String s) {
      int index = s.indexOf("\n");
      if (index >0) {
         s = s.substring(0, index);
      }
      index = s.indexOf("\\n");
      if (index >0) {
         s = s.substring(0, index);
      }
      index = s.indexOf("\t");
      if (index >0) {
         s = s.substring(0, index);
      }
      index = s.indexOf("\\t");
      if (index >0) {
         s = s.substring(0, index);
      }
      return s;
   }
   
   /**
    * Converts a selection option description to a enum name e.g. "Clock enabled" => ClockEnable
    * 
    * @param description Description to base name on
    * 
    * @return Created name
    */
   static public String makeEnumName(String description) {
      char[] p = description.toLowerCase().toCharArray();
      StringBuffer sb = new StringBuffer();
      
      // Upper-case 1st character
      boolean convertFlag = true;
      for (int index=0; index<p.length; index++) {
         if (p[index] == ',') {
            // Change this char to '_'
            sb.append('_');
         }
         else if (!Character.isJavaIdentifierPart(p[index])) {
            // Discard this char and upper-case next character
            convertFlag = true;
         }
         else if (convertFlag) {
            sb.append(Character.toUpperCase(p[index]));
            convertFlag = false;
         }
         else {
            sb.append(p[index]);
         }
      }
      return sb.toString();
   }

   void writePreamble() {

      final String preamble =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<!DOCTYPE peripheralPage SYSTEM \"_menu.dtd\" >\n" +
            "<!-- %s.xml -->\n" +
            "\n" +
            "<peripheralPage xmlns:xi=\"http://www.w3.org/2001/XInclude\" name=\"_instance\" description=\"%s\" >\n";

      final String pre =
         "\n"                                                                                    +
         "   <constant key=\"suppressInstance\"         type=\"Boolean\" value=\"false\"         />\n" +
         "   <constant key=\"peripheralUsesInterrupts\" type=\"Boolean\" value=\"%s\"          />\n" +
         "   <constant key=\"irq_parameters\"           type=\"String\"  value=\"&quot;&quot;\"  />\n" +
         "   <constant key=\"irq_dummy_parameters\"     type=\"String\"  value=\"&quot;&quot;\"  />\n" +
         "   <constant key=\"irq_call\"                 type=\"String\"  value=\"&quot;&quot;\"  />\n" +
         "   <constant key=\"isGeneratedByDefault\"     type=\"Boolean\" value=\"false\"         />\n"   +
         "   <constant key=\"isSupportedinStartup\"     type=\"Boolean\" value=\"true\"          />\n"    +
         "   <xi:include href=\"_enablePeripheral.xml\"  />\n"                                    +
         "   <title description=\"------------------------------------------\" />\n"             +
         "";

      final String usefulInfo =
            "\n"
            + "<!--\n"
            + "    * General substitutions\n"
            + "    *  $(_NAME)         => e.g FTM2 => FTM2\n"
            + "    *  $(_name)         => e.g FTM2 => ftm2\n"
            + "    *  $(_BASENAME)     => e.g FTM0 => FTM, PTA => PT\n"
            + "    *  $(_basename)     => e.g FTM0 => ftm, PTA => pt\n"
            + "    *  $(_Class)        => e.g FTM2 => Ftm2\n"
            + "    *  $(_Baseclass)    => e.g FTM0 => Ftm\n"
            + "    *  $(_instance)     => e.g FTM0 => 0, PTA => A\n"
            + "-->\n"
            + "\n"
            + "<!--\n"
            + "    * Template substitutions\n"
            + "    *\n"
            + "    * %valueExpression[index]         Formatted value as numeric e.g. 0x12\n"
            + "    * %symbolicValueExpression[index] Symbolic formatted value e.g. AdcCompare_Disabled\n"
            + "    * %variable[index]                Variable name /ADC0/adc_sc2_acfe\n"
            + "    * %macro[index](value)            C register macro e.g. ADC_SC2_ACFGT(value)\n"
            + "    * %description[index]             Description from controlVar e.g. Compare Function Enable\n"
            + "    * %shortDescription[index]        Short description from controlVar e.g. Compare Function Enable\n"
            + "    * %tooltip[index]                 Tool-tip from controlVar e.g. Each bit disables the GPIO function\n"
            + "    * %paramDescription[index]        Tool-tip from controlVar formatted as param description @param ...\n"
            + "    * %params                         Formatted parameter list for function\n"
            + "    * %paramType[index]               Based on enumStem or typename e.g. AdcCompare (or uint32_t)\n"
            + "    * %paramName[index]               Based on enumStem with lower-case first letter adcCompare\n"
            + "    * %enumClass[index]               As for %paramType\n"
            + "    * %enumParam[index]               As for %paramName\n"
            + "    * %valueExpression                Numeric variable value e.g. 0x3\n"
            + "    * %symbolicValueExpression        Symbolic variable value e.g. AdcCompare_Disabled\n"
            + "    * %defaultClockExpression         Based on variable etc. Similar to %register = (%register&~%mask) | %paramName;\n"
            + "    * %defaultMaskingExpression       Based on variable etc. Similar to (%register&%mask)\n"
            + "    * %variable[index]                Variable name from condition\n"
            + "    * %mask[index]                    From &lt;mask&gt; or deduced from &lt;controlVarName&gt; e.g. \"SIM_SOPT_REG_MASK\" (_MASK is added)\n"
            + "    * %register[index]                Register associated with variable e.g. adc->APCTL1\n"
            + "    * %registerName[index]            Name of corresponding register (lowercase for Init()) e.g. apctl1 <br><br>\n"
            + "-->\n";
      final String simExtra =
            "\n" +
            "   <xi:include href=\"_simCommon.xml\" />\n";
      
      String filename = peripheral.getSourceFilename().toLowerCase();
      String fileDescription=peripheral.getDescription();
      isSim = filename.startsWith("sim");
      
      resultSb.append(String.format(preamble,
            filename,
            fileDescription
      ));

      resultSb.append(String.format(pre, Boolean.toString(irqsUsed)));

      resultSb.append(usefulInfo);
      
      if (isSim) {
         resultSb.append(simExtra);
      }
      String classDecl =
            "\n"+
            "   <!-- ************* Class Declaration ****************** -->\n" +
            "   <constant key=\"_class_declaration\" type=\"String\" \n" +
            "      value=\"&quot;$(_Class)Info : public $(_Baseclass)BasicInfo&quot;\" />\n";
      resultSb.append(classDecl);
   }
   
   void processRegisters() {
      
      String header =
            "\n" +
            "   <!-- ************* %s ****************** -->\n" +
            "";
      
//      registerNameList = new ArrayList<>();
      fieldNameList    = new ArrayList<String>();
      fieldDefaultList = new ArrayList<>();
      
      for (Cluster cluster:peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            Register reg = (Register) cluster;
            if (reg.getAccessType() == AccessType.ReadOnly) {
               continue;
            }
            String regName = reg.getName().replace("%s", "");
            resultSb.append(String.format(header, regName));
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               String periphName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               String condition = "condition=\""+periphName+"_present\"";
               String enabledBy = "enabledBy=\"enablePeripheralSupport\"";
               String enumName  = prettyName(periphName);
               ArrayList<Enumeration> enumerations = field.getEnumerations();

               String fieldDescription = XML_BaseParser.escapeString(field.getDescription());
               String toolTip = null;

               int termIndex = fieldDescription.indexOf("\n");
               if (termIndex >= 0) {
                  toolTip = fieldDescription.substring(termIndex+1).trim();
                  fieldDescription = fieldDescription.substring(0, termIndex).trim();
                  if (fieldDescription.endsWith("\n")) {
                     fieldDescription = fieldDescription.substring(0, fieldDescription.length()-1).trim();
                  }
                  if (fieldDescription.endsWith("\\n")) {
                     fieldDescription = fieldDescription.substring(0, fieldDescription.length()-2).trim();
                  }
                  if (fieldDescription.endsWith(":")) {
                     fieldDescription = fieldDescription.substring(0, fieldDescription.length()-1).trim();
                  }
                  toolTip = padNewline(toolTip, "               ");
                  if (toolTip.isBlank()) {
                     toolTip = null;
                  }
               }
               fieldDescription = padNewline(fieldDescription, "                   ");
               
               if (enumerations.isEmpty()) {
                  resultSb.append("\n   <intOption key=\"" + periphName + "\" " + condition);
                  resultSb.append("\n      "+ enabledBy);
                  resultSb.append("\n      typeName=\""+ getRegisterCType(reg.getElementSizeInBytes()) + "\"\n");
                  resultSb.append("      description=\"" + fieldDescription + "\"\n");
                  if (toolTip != null) {
                     resultSb.append("      toolTip=\"" + toolTip + "\"\n");
                  }
                  resultSb.append("   />\n");
                  fieldDefaultList.add("0, // "+fieldDescription);
               }
               else {
                  fieldNameList.add(regName.toLowerCase()+"_"+field.getName().toLowerCase());
                  String typeName = "choiceOption";
                  if (enumerations.size() == 2) {
                     typeName = "binaryOption";
                  }
                  resultSb.append("\n   <"+typeName+" key=\"" + periphName + "\" " + condition);
                  resultSb.append("\n      "+ enabledBy);
                  resultSb.append("\n      enumStem=\"" +  enumName +"\"\n");

                  if (toolTip != null) {
                     resultSb.append("      toolTip=\"" + toolTip + "\"\n");
                  }
                  resultSb.append("      description=\"" + fieldDescription + "\" >\n");

                  int descriptionWidth = 10;
                  for (Enumeration enumeration:enumerations) {
                     int width = truncateAtNewlineOrTab(enumeration.getDescription()).length();
                     if (width > descriptionWidth) {
                        descriptionWidth = width;
                     }
                  }
                  descriptionWidth += "name=\"\"".length();
                  String defaultValue = null;
                  for (Enumeration enumeration:field.getEnumerations()) {
                     String enumDescription = XML_BaseParser.escapeString(truncateAtNewlineOrTab(enumeration.getDescription()));
                     
                     resultSb.append(String.format("      <choice %-"+descriptionWidth+"s %10s %s/>\n",
                           "name=\"" + enumDescription + "\"",
                           "value=\"" + enumeration.getValue() + "\"",
                           "enum=\""+makeEnumName(enumDescription)+"\""));
                     if (defaultValue == null) {
                        defaultValue = enumName+"_"+makeEnumName(enumDescription)+"  // "+fieldDescription;
                     }
                  }
                  resultSb.append("   </"+typeName+" >\n");
                  
                  fieldDefaultList.add(defaultValue);
               }
            }
         }
      }

   }
   
   void writeSetters() {
      resultSb.append(
            "\n" +
            "<!-- Graphic here -->\n");
      
      final String setTemplate =
          "      <setTemplate variables=\"%(field)\" codeGenCondition=\"enableGettersAndSetters\"\n" +
          "      ><![CDATA[\n" +
          "         \\t/**\n" +
          "         \\t * Set %description\n" +
          "         \\t *\n" +
          "         %paramDescription\n" +
          "         \\t */\n" +
          "         \\tstatic void set%(name)(%params) {\n" +
          "         \\t   %defaultFieldExpression\n" +
          "         \\t}\n" +
          "         \\t\n" +
          "         \\t/**\n" +
          "         \\t * Get %description\n" +
          "         \\t *\n" +
          "         \\t * @return %tooltip\n" +
          "         \\t */\n" +
          "         \\tstatic %paramType get%(name)() {\n" +
          "         \\t   return %paramType(%register&%mask);\n" +
          "         \\t}\n" +
          "         \\t\\n\n" +
          "      ]]></setTemplate>\n";
    
      StringBuilder fieldListsb = new StringBuilder();
      boolean firstField = true;
      for (String field:fieldNameList) {
         if (!firstField) {
            fieldListsb.append(";\n");
         }
         String fieldName = peripheralBasename.toLowerCase()+"_"+field;
         firstField = false;
         fieldListsb.append("      "+fieldName);
         fieldListsb.append(":"+prettyName(field));
      }
      resultSb.append(
            "\n" +
            "   <for keys=\"field:name\"\n" +
            "        values=\"\n" + fieldListsb.toString() + "\" >\n");
      resultSb.append(setTemplate);
      resultSb.append("   </for>\n");
   }
   
   String getRegisterCType(long size) {
      if (size == 1) {
         return "uint8_t";
      }
      if (size == 2) {
         return "uint16_t";
      }
      return "uint32_t";
   }
   
   void writeInitClass() {

      final String init_description_template =
         "\n" +
         "<!--   ========== %s Init class =============================== -->\n" +
         "\n" +
         "   <template key=\"init_description\" namespace=\"all\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\" >\n" +
         "   <![CDATA[\n" +
         "      \\t/**\n" +
         "      \\t * Class used to do initialisation of the $(_Class)\n" +
         "      \\t *\n" +
         "      \\t * This class has a templated constructor that accepts various values.\n" +
         "      \\t *\n" +
         "      \\t * @note This constructor may be used to create a const instance in Flash\n" +
         "      \\t *\n" +
         "      \\t * Example:\n" +
         "      \\t * ///\n" +
         "      \\t * /// $(_NAME) call-back\n" +
         "      \\t * ///\n" +
         "      \\t * /// @param status  Status reflecting active inputs\n" +
         "      \\t * ///\n" +
         "      \\t * void $(_name)Callback(uint32_t status) {\n" +
         "      \\t *    (void) status;\n" +
         "      \\t * }\n" +
         "      \\t *\n" +
         "      \\t * @code\n" +
         "      \\t * static const $(_Class)::Init $(_name)Init {\n" +
         "      \\t *\n" +
         "      \\t *   // Setup values\n" +
         "%s" +
         "      \\t *\n" +
         "      \\t *    $(_name)Callback,                // Call-back to execute on event - call-back function name\n" +
         "      \\t *    NvicPriority_Low,                // Priority for interrupt - Low\n" +
         "      \\t * };\n" +
         "      \\t *\n" +
         "      \\t * // Initialise $(_Class) from values specified above\n" +
         "      \\t * $(_Class)::configure($(_name)Init)\n" +
         "      \\t * @endcode\n" +
         "      \\t */\n" +
         "   ]]>\n" +
         "   </template>\n";
      
      StringBuilder sb = new StringBuilder();
      for (String defaultValue:fieldDefaultList) {
         int index = defaultValue.indexOf("/");
         String first = defaultValue.substring(0, index);
         String last  = defaultValue.substring(index);
         sb.append("      \\t *   "+String.format("%-40s  %s", first, last)+",\n");
      }
      resultSb.append(String.format(init_description_template, peripheralBasename, sb.toString()));
      
      String openBasicInfoClass =
            "\n" +
            "   <template namespace=\"usbdm\" >\n" +
            "   <![CDATA[\n" +
            "      class $(_Class)BasicInfo {\n" +
            "      \\t\n" +
            "      public:\\n\n" +
            "   ]]>\n" +
            "   </template>\n";

      String irqDeclaration =
            "\n" +
            "   <template namespace=\"usbdm\" codeGenCondition=\"irqHandlingMethod\" >\n" +
            "   <![CDATA[\n" +
            "      \\t/**\n" +
            "      \\t * Type definition for $(_Class) interrupt call back.\n" +
            "      \\t */\n" +
            "      \\ttypedef void (*CallbackFunction)($(irq_parameters));\\n\n" +
            "   ]]>\n" +
            "   </template>\n";

      String irqUnhandledMethod =
            "\n" +
            "   <template codeGenCondition=\"irqHandlingMethod\" >\n" +
            "   <![CDATA[\n" +
            "      \\t/** Callback function for ISR */\n"                                   +
            "      \\tstatic CallbackFunction sCallback;\n"                                 +
            "      \\t\n"                                                                   +
            "      \\t/**\n"                                                                +
            "      \\t * Callback to catch unhandled interrupt\n"                           +
            "      \\t */\n"                                                                +
            "      \\tstatic void unhandledCallback($(irq_dummy_parameters)) {\n"                 +
            "      \\t   setAndCheckErrorCode(E_NO_HANDLER);\n"                             +
            "      \\t}\n"                                                                  +
            "      \\t\\n\n"                                                                                      +
            "   ]]>\n" +
            "   </template>\n";

      String irqHandler =
            "\n" +
           "   <template codeGenCondition=\"irqHandlingMethod\" >\n"  +
           "   <![CDATA[\n"                                                                                   +
           "      \\t/**\n"                                                                                   +
           "      \\t * Set interrupt callback function.\n"                                                   +
           "      \\t *\n"                                                                                    +
           "      \\t * @param[in]  %cb Callback function to execute on interrupt\n"        +
           "      \\t */\n"                                                                                   +
           "      \\tstatic void setCallback(CallbackFunction %cb) {\n"                               +
           "      \\t   if (%cb == nullptr) {\n"                                                      +
           "      \\t      %cb = unhandledCallback;\n"                                                +
           "      \\t   }\n"                                                                                  +
           "      \\t   usbdm_assert(\n" +
           "      \\t         (sCallback == unhandledCallback) || (sCallback == %cb),\n" +
           "      \\t         \"Handler already set\");\n" +
           "      \\t   sCallback = %cb;\n"                                                           +
           "      \\t}\n"                                                                                     +
           "      \\t\n"                                                                                      +
           "      \\t/**\n"                                                                                   +
           "      \\t * $(_BASENAME) interrupt handler -  Calls $(_BASENAME) callback\n"                      +
           "      \\t */\n"                                                                                   +
           "      \\tstatic void irqHandler() {\n"                                                            +
           "      \\t\n"                                                                                      +
           "      \\t   //.....IRQ handler code here..........\n"                                +
           "      \\t\n"                                                                                      +
           "      \\t   // Execute call-back\n"                                                                                      +
           "      \\t   sCallback($(irq_call));\n"                                   +
           "      \\t   return;\n"                                                                            +
           "      \\t}\n"                                                                                  +
           "      \\t\\n\n" +
           "   ]]>\n"                                                                                         +
           "   </template>\n"                                                                                 +
           "";
      
      String irqStaticDefinition =
            "\n" +
            "   <template namespace=\"all\" key=\"/HARDWARE/StaticObjects\" codeGenCondition=\"irqHandlingMethod\" >\n"                       +
            "   <![CDATA[\n"                                                                                                            +
            "      \\t\n"                                                                                                               +
            "      \\t/** Callback for programmatically set handler */\n"                                                               +
            "      \\t$(_Class)Info::CallbackFunction $(_Class)Info::sCallback = $(_Class)Info::unhandledCallback;\n"                                                                       +
            "      \\t\\n\n"                                                                                                            +
            "   ]]>\n"                                                                                                                  +
            "   </template>\n"                                                                                                          +
            "";
      
      resultSb.append (String.format(openBasicInfoClass, ""));
      
      if (irqsUsed) {
         resultSb.append (String.format(irqDeclaration));
         resultSb.append (String.format(irqUnhandledMethod));
         resultSb.append (String.format(irqHandler.replace("%cb", peripheralBasename.toLowerCase()+"Callback")));
         resultSb.append (String.format(irqStaticDefinition.replace("%cb", peripheralBasename.toLowerCase()+"Callback")));
      }
      
      String openInitClass =
            "\n" +
            "   <template namespace=\"usbdm\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\" >\n" +
            "   <![CDATA[\n" +
            "      $(init_description)\n" +
            "      \\tclass Init {\n" +
            "      \\t\n" +
            "      \\tpublic:\n" +
            "      \\t   /**\n" +
            "      \\t    * Copy Constructor\n" +
            "      \\t    */\n" +
            "      \\t   constexpr Init(const Init &other) = default;\n" +
            "      \\t\n" +
            "      \\t   /**\n" +
            "      \\t    * Default Constructor\n" +
            "      \\t    */\n" +
            "      \\t   constexpr Init() = default;\n" +
            "      \\t\\n\n" +
            "   ]]>\n" +
            "   </template>\n" +
            "\n" +
            "<!--   Member variables -->\n";

      resultSb.append(openInitClass);

      String irqEntryTemplate =
            "\n" +
            "   <initialValueTemplate namespace=\"usbdm\" variables=\"irqHandlingMethod\" codeGenCondition=\"irqHandlingMethod\"\n" +
            "    ><![CDATA[\n" +
            "      \\t   /// %%description\n" +
            "      \\t   %%params = nullptr;\\n\\n\n" +
            "   ]]></initialValueTemplate>\n";
      
      if (irqsUsed) {
         resultSb.append(String.format(irqEntryTemplate));
      }

      String memberDeclaration =
            "\n" +
            "   <initialValueTemplate namespace=\"usbdm\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\"\n" +
            "      variables=\"%s\"\n" +
            "   ><![CDATA[\n" +
            "      \\t   /// %s\n" +
            "      \\t   %s %%registerName = 0;\\n\\n\n" +
            "   ]]></initialValueTemplate>\n";
      
      for (Cluster cluster:peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            Register reg = (Register) cluster;
            if (reg.getAccessType() == AccessType.ReadOnly) {
               continue;
            }
            String regName = reg.getName().replace("%s", "");
            sb = new StringBuilder();
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               if (!sb.isEmpty()) {
                  sb.append(",");
               }
               sb.append(registerFieldName);
            }
            if (!sb.isEmpty()) {
               resultSb.append(String.format(
                     memberDeclaration, sb.toString(), reg.getBriefCDescription(),
                     getRegisterCType(reg.getElementSizeInBytes())));
            }
         }
      }

      String irqLevelTemplate =
            "\n" +
            "   <initialValueTemplate namespace=\"usbdm\" codeGenCondition=\"irqHandlingMethod\"\n" +
            "      variables=\"/PCR/nvic_irqLevel,irqLevel\"\n" +
            "   ><![CDATA[\n" +
            "      \\t   /// %%description\n" +
            "      \\t   %%enumClass %%registerName0 = %%symbolicValueExpression1;\n" +
            "      \\t\\n\n" +
            "   ]]></initialValueTemplate>\n";
            
      if (irqsUsed) {
         resultSb.append(String.format(irqLevelTemplate));
      }
      
      String configureMethodDefaultConfig =
            "\n" +
            "<!--   Configure methods -->\n" +
            "\n" +
            "   <template key=\"/$(_BASENAME)/InitMethod\" namespace=\"all\" discardRepeats=\"true\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\" ><![CDATA[\n" +
            "      \\t/**\n" +
            "      \\t * Configure with default settings.\n" +
            "      \\t * Configuration determined from Configure.usbdmProject\n" +
            "      \\t */\n" +
            "      \\tstatic inline void defaultConfigure() {\n" +
            "      \\t\n" +
            "      \\t   // Update settings\n" +
            "      \\t   configure(Info::DefaultInitValue);\n" +
            "      \\t}\n" +
            "      \\t\\n\n" +
            "   ]]></template>\n";
      
      String configureMethodConfigureOpen =
            "\n" +
            "   <template key=\"/$(_BASENAME)/InitMethod\" namespace=\"all\" discardRepeats=\"true\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\" ><![CDATA[\n" +
            "      \\t/**\n" +
            "      \\t * Configure $(_BASENAME) from values specified in init\n" +
            "      \\t *\n" +
            "      \\t * @param init Class containing initialisation values\n" +
            "      \\t */\n" +
            "      \\tstatic void configure(const typename Info::Init &init) {\n" +
            "      \\t   // ..........  Configure ...........\n" +
            "      \\t\n" +
            "      \\t   // Enable peripheral clock\n" +
            "      \\t   Info::enableClock();\n" +
            "      \\t\\n\n" +
            "   ]]></template>\n";

      String configureMethodIrq =
            "   <template key=\"/$(_BASENAME)/InitMethod\" namespace=\"all\" discardRepeats=\"true\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\"\n" +
            "             condition=\"irqHandlingMethod\" ><![CDATA[\n" +
            "      \\t   if constexpr (Info::irqHandlerInstalled) {\n" +
            "      \\t      Info::setCallback(init.callbackFunction);\n" +
            "      \\t      enableNvicInterrupts(init.irqlevel);\n" +
            "      \\t   }\n" +
            "      \\t\\n\n" +
            "   ]]></template>\n" +
            "\n";
            
      String configureMethodRegs =
            "   <template key=\"/$(_BASENAME)/InitMethod\" namespace=\"all\" discardRepeats=\"true\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\" ><![CDATA[\n" +
            "      \\t   // ..........  Regs to init .......... ;\n"                               +
            "%s" +
            "      \\t}\n"                                                                            +
            "      \\t\\n\n"                                                                          +
            "   ]]></template>\n"                                                                     +
            "";
      
      resultSb.append(String.format(configureMethodDefaultConfig));
      resultSb.append(String.format(configureMethodConfigureOpen));
      resultSb.append(String.format(configureMethodIrq));
      
      sb = new StringBuilder();
      for (Cluster cluster:peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            Register reg = (Register) cluster;
            if (reg.getAccessType() == AccessType.ReadOnly) {
               continue;
            }
            String regName = reg.getName().replace("%s", "");
            String line = String.format(
                  "      \\t   %s->%s    = init.%s;\n",
                  peripheralBasename.toLowerCase(), regName.toUpperCase(),regName.toLowerCase());
            sb.append(line);
         }
      }
      
      resultSb.append(String.format(configureMethodRegs, sb.toString()));
      
      if (irqsUsed) {
      }
      else {
      }
      String constructorTitle =
          "\n" +
          "<!--   Constructors -->\n";
      
      String irqHandlerConstructorTemplate =
            "\n" +
            "   <setTemplate namespace=\"usbdm\" codeGenCondition=\"irqHandlingMethod\"\n" +
            "      variables=\"irqHandlingMethod\"\n" +
            "      linePadding=\"xxx\"\n" +
            "    ><![CDATA[\n" +
            "      \\t   /**\n" +
            "      \\t    * Constructor for %description\n" +
            "      \\t    *\n" +
            "      \\t    * @tparam   Types\n" +
            "      \\t    * @param    rest\n" +
            "      %paramDescription\n" +
            "      \\t    */\n" +
            "      \\t   template <typename... Types>\n" +
            "      \\t   constexpr Init(%params, Types... rest) : Init(rest...) {\n" +
            "      \\t\n" +
            "      \\t      this->%paramName0 = %paramExpression;\n" +
            "      \\t   }\\n\\n\n" +
            "   ]]></setTemplate>\n" +
            "\n" +
            "   <setTemplate namespace=\"usbdm\" codeGenCondition=\"irqHandlingMethod\"\n" +
            "      variables=\"/PCR/nvic_irqLevel\"\n" +
            "      linePadding=\"xxx\"\n" +
            "    ><![CDATA[\n" +
            "      \\t   /**\n" +
            "      \\t    * Constructor for %description\n" +
            "      \\t    *\n" +
            "      \\t    * @tparam   Types\n" +
            "      \\t    * @param    rest\n" +
            "      %paramDescription\n" +
            "      \\t    */\n" +
            "      \\t   template <typename... Types>\n" +
            "      \\t   constexpr Init(%params, Types... rest) : Init(rest...) {\n" +
            "      \\t\n" +
            "      \\t      %registerName = %paramExpression;\n" +
            "      \\t   }\\n\\n\n" +
            "   ]]></setTemplate>\n";
      
      boolean constructorTitleDone = false;

      if (irqsUsed) {
         resultSb.append(constructorTitle);
         constructorTitleDone = true;
         resultSb.append(irqHandlerConstructorTemplate);
      }
      
      String enumConstructorListTemplate =
          "\n" +
          "   <for keys=\"r\"\n" +
          "      values=\"\n" +
          "%s\n" +
          "            \" >\n" +
          "      <setTemplate namespace=\"usbdm\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\"\n" +
          "         variables=\"%%(r)\"\n" +
          "         linePadding=\"xxx\" >\n" +
          "      <![CDATA[\n" +
          "         \\t   /**\n" +
          "         \\t    * Constructor for %%description\n" +
          "         \\t    *\n" +
          "         \\t    * @tparam   Types\n" +
          "         \\t    * @param    rest\n" +
          "         \\t    *\n" +
          "         %%paramDescription\n" +
          "         \\t    */\n" +
          "         \\t   template <typename... Types>\n" +
          "         \\t   constexpr Init(%%params, Types... rest) : Init(rest...) {\n" +
          "         \\t\n" +
          "         \\t      %%registerName = (%%registerName&~%%mask) | %%paramExpression;\n" +
          "         \\t   }\n" +
          "         \\t\\n\n" +
          "      ]]>\n" +
          "      </setTemplate>\n" +
          "   </for>\n";
    
      String integerConstructorListTemplate =
          "\n" +
          "   <for keys=\"r\"\n" +
          "      values=\"\n" +
          "%s\n" +
          "            \" >\n" +
          "      <setTemplate namespace=\"usbdm\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\"\n" +
          "         variables=\"%%(r)\"\n" +
          "         linePadding=\"xxx\" >\n" +
          "      <![CDATA[\n" +
          "         \\t   /**\n" +
          "         \\t    * Constructor for %%description\n" +
          "         \\t    *\n" +
          "         \\t    * @tparam   Types\n" +
          "         \\t    * @param    rest\n" +
          "         \\t    *\n" +
          "         \\t    * @param value %%description\n" +
          "         \\t    */\n" +
          "         \\t   template <typename... Types>\n" +
          "         \\t   constexpr Init(%%paramType0 value, Types... rest) : Init(rest...) {\n" +
          "         \\t\n" +
          "         \\t      %%registerName = (%%registerName&~%%mask0) | %%macro0(value);\n" +
          "         \\t   }\n" +
          "         \\t\\n\n" +
          "      ]]>\n" +
          "      </setTemplate>\n" +
          "   </for>\n" +
          "\n";
    
      sb = new StringBuilder();
      for (Cluster cluster:peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            Register reg = (Register) cluster;
            if (reg.getAccessType() == AccessType.ReadOnly) {
               continue;
            }
            String regName = reg.getName().replace("%s", "");
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               ArrayList<Enumeration> enumerations = field.getEnumerations();
               if (enumerations.isEmpty()) {
                  continue;
               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               if (!sb.isEmpty()) {
                  sb.append(";\n");
               }
               sb.append("            "+registerFieldName);
            }
         }
      }
      
      if (!sb.isEmpty()) {
         if (!constructorTitleDone) {
            resultSb.append(constructorTitle);
         }
         resultSb.append(String.format(enumConstructorListTemplate, sb.toString()));
      }

      sb = new StringBuilder();
      for (Cluster cluster:peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            Register reg = (Register) cluster;
            if (reg.getAccessType() == AccessType.ReadOnly) {
               continue;
            }
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               ArrayList<Enumeration> enumerations = field.getEnumerations();
               if (!enumerations.isEmpty()) {
                  continue;
               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+reg.getName().toLowerCase()+"_"+field.getName().toLowerCase();
               if (!sb.isEmpty()) {
                  sb.append(";\n");
               }
               sb.append("            "+registerFieldName);
            }
         }
      }
      if (!sb.isEmpty()) {
         if (!constructorTitleDone) {
            resultSb.append(constructorTitle);
         }
         resultSb.append(String.format(integerConstructorListTemplate, sb.toString()));
      }

      String initValueTemplate =
            "\n" +
            "<!--   Default Initialisation value -->\n" +
            "\n" +
            "   <initialValueTemplate codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\"\n" +
            "      separator=\",\"\n" +
            "      terminator=\",\"\n" +
            "      variables=\"\n" +
            "%s\n" +
            "         \"\n" +
            "   ><![CDATA[\n" +
            "      \\t/**\n" +
            "      \\t * Default initialisation value for $(_Class)\n" +
            "      \\t * This value is created from Configure.usbdmProject settings\n" +
            "      \\t */\n" +
            "      \\tstatic constexpr Init DefaultInitValue = {%%initExpression\n" +
            "      \\t};\\n\\n\n" +
            "   ]]></initialValueTemplate>\n";
      
//      String irqReferenceDeclaration =
//            "      \\t/**\n" +
//            "      \\t * $(_Class) interrupt call back\n" +
//            "      \\t */\n" +
//            "      \\ttypedef $(_Class)BasicInfo::CallbackFunction CallbackFunction;\n" +
//            "      \\t\n";
      
      sb = new StringBuilder();
      for (Cluster cluster:peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            Register reg = (Register) cluster;
            if (reg.getAccessType() == AccessType.ReadOnly) {
               continue;
            }
            String regName = reg.getName().replace("%s", "");
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
//               ArrayList<Enumeration> enumerations = field.getEnumerations();
//               if (enumerations.isEmpty()) {
//                  continue;
//               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               if (!sb.isEmpty()) {
                  sb.append(",\n");
               }
               sb.append("            "+registerFieldName);
            }
         }
      }
      resultSb.append(String.format(initValueTemplate, sb.toString(), ""));
      
//      String configureMethodsOpen =
//            "\n" +
//            "   <template>\n" +
//            "      <![CDATA[\n" +
//            "      \\t/**\n" +
//            "      \\t * Configure $(_BASENAME)\n" +
//            "      \\t */\n" +
//            "      \\tstatic void configure(const Init &configValue) {\n";
//
//      String configureMethodsClose =
//            "      \\t}\n" +
//            "      \\t\n" +
//            "      \\t/**\n" +
//            "      \\t * Default initialisation for $(_BASENAME)\n" +
//            "      \\t */\n" +
//            "      \\tstatic void defaultConfigure() {\n" +
//            "      \\t   configure(DefaultInitValue);\n" +
//            "      \\t}\n" +
//            "      \\t\\n\n" +
//            "   ]]>\n" +
//            "   </template>\n";
//
//      resultSb.append(configureMethodsOpen);
//
//      for (Cluster cluster:peripheral.getRegisters()) {
//         if (cluster instanceof Register) {
//            Register reg = (Register) cluster;
//            if (reg.getAccessType() == AccessType.ReadOnly) {
//               continue;
//            }
//            Boolean foundValidField = false;
//            String regName = reg.getName().replace("%s", "");
//            for (Field field:reg.getFields()) {
//               if (field.getAccessType() == AccessType.ReadOnly) {
//                  continue;
//               }
//               foundValidField = true;
//            }
//            if (foundValidField) {
//               resultSb.append(String.format("      \\t   %s->%-15s = configValue.%s;\n", peripheralBasename.toLowerCase(), regName, regName.toLowerCase()));
//            }
//         }
//      }
//      resultSb.append(configureMethodsClose);

      String closeInitClass =
            "\n" +
            "   <template namespace=\"usbdm\" codeGenCondition=\"/$(_BASENAME)/enablePeripheralSupport\" >\n" +
            "      <![CDATA[\n" +
            "      \\t}; // $(_Class)BasicInfo::Init\\n\\n\n" +
            "   ]]>\n" +
            "   </template>\n";
      
      String closeBasicInfoClass =
            "\n" +
            "   <template namespace=\"usbdm\" >\n" +
            "      <![CDATA[\n" +
            "      }; // $(_Class)BasicInfo\\n\\n\n" +
            "   ]]>\n" +
            "   </template>\n";

      resultSb.append(closeInitClass);
      resultSb.append(closeBasicInfoClass);
   }
   
   void writeCommon() {
      String common =
            "\n" +
            "   <!-- ************* Common ****************** -->\n" +
            "\n" +
            "   <template key=\"/$(_BASENAME)/declarations\" namespace=\"all\" codeGenCondition=\"enablePeripheralSupport\" ><![CDATA[\n" +
            "   \\t/**\n" +
            "   \\t * Class representing $(_NAME)\n" +
            "   \\t */\n" +
            "   \\tclass $(_Class) : public $(_Baseclass)Base_T<$(_Class)Info> {};\\n\n" +
            "   ]]></template>\n" +
            "\n" +
            "   <validate\n" +
            "      class=\"net.sourceforge.usbdm.deviceEditor.validators.PeripheralValidator\" >\n" +
            "   </validate>\n";
      resultSb.append(String.format(common));
      
      String fileInclude =
            "\n" +
                  "   <projectActionList id = \"%s_files\" >\n" +
                  "      <copy source=\"Project_Headers/%s.h\"  target=\"Project_Headers/%s.h\"  overwrite=\"true\"  derived=\"true\" />\n" +
                  "   </projectActionList>\n";
      String filename = peripheralBasename.toLowerCase();
      resultSb.append(String.format(fileInclude, filename, filename+fileSuffix.toUpperCase(), filename));

      resultSb.append(
            "\n" +
            "   <!-- ************* Startup ****************** -->\n" +
            "\n" +
            "   <template key=\"/SYSTEM/Includes\" namespace=\"all\" condition=\"@isSupportedinStartup\" codeGenCondition=\"configurePeripheralInStartUp\" >\n" +
            "      <![CDATA[#include \"$(_basename).h\"\\n\n" +
            "   ]]></template>\n" +
            "\n" +
            "   <template key=\"/SYSTEM/Startup\" namespace=\"all\" condition=\"@isSupportedinStartup\" codeGenCondition=\"configurePeripheralInStartUp\" >\n" +
            "   <![CDATA[\n" +
            "      \\t/*  Initialise $(_Class) */\n" +
            "      \\tUSBDM::$(_Class)::defaultConfigure();\\n\n" +
            "   ]]></template>\n");
      
      if (isSim) {
         resultSb.append("\n   <xi:include href=\"_simFiles-MKE.xml\" />");
         //         resultSb.append("\n   <xi:include href=\"_sim_commonTemplates.xml\" />\n");
      }
      
      resultSb.append(
            "\n" +
            "   <!-- ************* Signal mapping ****************** -->\n" +
            "   <category name=\"SIM\" description=\"Pin mapping and clock enables\">\n" +
            "      <aliasOption key=\"/SIM/sim_scgc_$(_name)\"      constant=\"false\" optional=\"true\" />\n" +
            "      <aliasOption key=\"/SIM/sim_scgc_$(_basename)\"  constant=\"false\" optional=\"true\" />\n" +
            "      <aliasOption key=\"/SIM/sim_pinsel_$(_name)ps\"  constant=\"false\" optional=\"true\" />\n" +
            "      <aliasOption key=\"/SIM/sim_pinsel0_$(_name)ps\" constant=\"false\" optional=\"true\" />\n" +
            "      <aliasOption key=\"/SIM/sim_pinsel1_$(_name)ps\" constant=\"false\" optional=\"true\" />\n" +
            "   </category>\n" +
            "\n" +
            "   <signals enabledBy=\"/$(_BASENAME)/enablePeripheralSupport\" />\n");
      
      resultSb.append("\n</peripheralPage>\n");
   }
   
   static String peripheralsToDo[] = {
//         "ACMP",
//         "ADC",
//         "CAN"
//         "IRQ",
//         "I2C",
//         "KBI",
//         "PMC",
//         "PWT",
//         "RTC",
//         "RCM",
//         "SMC",
//         "SPI",
//            "UART",
//         "LPTMR",
//       "OSC",
//       "PIT",

         "FTM",
//         "FGPIO",
//         "PORT",
//         "SIM",
//         "WDOG",
//         "UART",
//         "ICS",
   };
   
   static boolean doThisPeripheral(String name) {
      for (String tname:peripheralsToDo) {
         if (name.startsWith(tname)) {
            return true;
         }
      }
      return false;
   }
   
   static void doAllPeripherals(String deviceName) throws Exception {
      DevicePeripheralsFactory factory = new DevicePeripheralsFactory();
      DevicePeripherals peripherals = factory.getDevicePeripherals(deviceName);
      if (peripherals == null) {
         throw new Exception("Unable to get peripheral data for '"+deviceName+"'\n");
      }
      for (Peripheral peripheral:peripherals.getPeripherals()) {
         if (peripheral.getSourceFilename() == null) {
            continue;
         }
         if (!doThisPeripheral(peripheral.getName())) {
            System.out.println("Skipping " + peripheral.getSourceFilename());
            continue;
         }
         System.out.println("Processing " + peripheral.getSourceFilename());
         CreateDeviceSkeletonFromSVD instance = new CreateDeviceSkeletonFromSVD("mke", peripherals, peripheral);
         instance.writePreamble();
         instance.processRegisters();
         instance.writeSetters();
         instance.writeInitClass();
         instance.writeCommon();
         instance.savePeripheralFiles();
      }
   }

   public static void main(String[] args) throws Exception {
//      doAllPeripherals("FRDM_KE04Z");
      doAllPeripherals("FRDM_KE06Z");
//      doAllPeripherals("FRDM_KL02Z");
   }

}
