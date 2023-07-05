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
   
   private String peripheralName;     // e.g. "ACMP0"
   private String peripheralBasename; // e.g. "ACMP"
   private String fileSuffix;         // e.g. "_mke"

   
   boolean isSim = false;

   private boolean irqsUsed = false;
   
   private Peripheral peripheral = null;
   
   private ArrayList<String> fieldNameList;
   private ArrayList<String> fieldDefaultList;

   CreateDeviceSkeletonFromSVD(String fileSuffix, DevicePeripherals peripherals, Peripheral peripheral) throws Exception {
      this.fileSuffix     = "_"+fileSuffix;
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
      Path hardwarePath = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(DeviceInfo.USBDM_ARM_PERIPHERALS_LOCATION).resolve(peripheral.getSourceFilename().toLowerCase()+".xml");
      
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

      final String simExtra =
            "\n" +
            "   <xi:include href=\"_simCommon.xml\"/>\n";
      
      final String irqExtra =
            "\n" +
            "   <xi:include href=\"_irqOption.xml\"/>\n";
      
      final String preamble =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
            "<!DOCTYPE peripheralPage SYSTEM \"_menu.dtd\" >\n" +
            "<!-- %s.xml -->\n" +
            "\n" +
            "<peripheralPage xmlns:xi=\"http://www.w3.org/2001/XInclude\" name=\"_instance\" description=\"%s\">\n" +
            "\n" +
            "   <xi:include href=\"_default_instance.xml\"/>\n" +
            "%s" +
            "\n" +
            "   <xi:include href=\"_mapPinsOption.xml\"/>\n" +
            "%s";

      String filename = peripheral.getSourceFilename().toLowerCase();
      String fileDescription=peripheral.getDescription();
      isSim = filename.startsWith("sim");
      
      resultSb.append(String.format(preamble,
            filename,
            fileDescription,
            isSim?simExtra:"",
            irqsUsed?irqExtra:""
            ));

   }
   
   void processRegisters() {
      
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
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               String periphName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               String condition = "condition=\""+periphName+"_present\"";
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
                  resultSb.append("\n   <intOption key=\"" + periphName + "\" " + condition+"\n");

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
                  resultSb.append("\n   <"+typeName+" key=\"" + periphName + "\" " + condition+"\n");
                  resultSb.append("      enumStem=\"" +  enumName +"\"\n");

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
                  resultSb.append("   </"+typeName+">\n");
                  
                  fieldDefaultList.add(defaultValue);
               }
            }
         }
      }

   }
   
   void writeSetters() {
      resultSb.append(
            "\n" +
            "<!-- Grahic here -->  \n");
      
      final String setTemplate =
          "      <setTemplate variables=\"%(field)\"\n" +
          "      ><![CDATA[\n" +
          "         \\t/**\n" +
          "         \\t * Set %description\n" +
          "         \\t *\n" +
          "         %paramDescription\n" +
          "         \\t */\n" +
          "         \\tstatic void configure%(name)(%params) {\n" +
          "         \\t   %defaultFieldExpression\n" +
          "         \\t}\\n\\n\n" +
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
            "        values=\"\n" + fieldListsb.toString() + "\">\n");
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
         "   <template key=\"init_description\" namespace=\"all\">\n" +
         "   <![CDATA[\n" +
         "      \\t/**\n" +
         "      \\t * Class used to do initialisation of the $(_class)\n" +
         "      \\t *\n" +
         "      \\t * This class has a templated constructor that accepts various values.\n" +
         "      \\t *\n" +
         "      \\t * @note This constructor may be used to create a const instance in Flash\n" +
         "      \\t *\n" +
         "      \\t * Example:\n" +
         "      \\t * @code\n" +
         "      \\t * static const $(_class)::Init init {\n" +
         "      \\t *\n" +
         "      \\t *   // Setup values\n" +
         "%s" +
         "      \\t * };\n" +
         "      \\t *\n" +
         "      \\t * // Initialise $(_class) from values specified above\n" +
         "      \\t * $(_class)::configure(init)\n" +
         "      \\t * @endcode\n" +
         "      \\t */\n" +
         "   ]]>\n" +
         "   </template>\n" +
         "\n";
      
      StringBuilder sb = new StringBuilder();
      for (String defaultValue:fieldDefaultList) {
         int index = defaultValue.indexOf("/");
         String first = defaultValue.substring(0, index);
         String last  = defaultValue.substring(index);
         sb.append("      \\t *   "+String.format("%-40s  %s", first, last)+",\n");
      }
      resultSb.append(String.format(init_description_template, peripheralBasename, sb.toString()));
      
      String openBasicInfoClass =
            "   <template namespace=\"usbdm\">\n" +
            "   <![CDATA[\n" +
            "      class $(_class)BasicInfo {\n" +
            "      \\t\n" +
            "      public:\n" +
            "%s" +
            "      \\t\\n\n" +
            "   ]]>\n" +
            "   </template>\n";

      String irqDeclaration =
            "      \\t\n" +
            "      \\t/**\n" +
            "      \\t * Type definition for $(_class) interrupt call back.\n" +
            "      \\t */\n" +
            "      \\ttypedef void (*CallbackFunction)(const uint32_t &);\n";
      
      if (irqsUsed) {
         resultSb.append (String.format(openBasicInfoClass, irqDeclaration));
      }
      else {
         resultSb.append (String.format(openBasicInfoClass, ""));
      }
      
      String openInitClass =
            "\n" +
            "   <template namespace=\"usbdm\">\n" +
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
            "   <setTemplate namespace=\"usbdm\" variables=\"irqHandlingMethod\" codeGenCondition=\"irqHandlingMethod\"\n" +
            "    ><![CDATA[\n" +
            "      \\t   /// %%description\n" +
            "      \\t   %%params = nullptr;\\n\\n\n" +
            "   ]]></setTemplate>\n";
      
      if (irqsUsed) {
         resultSb.append(String.format(irqEntryTemplate));
      }
      
      String memberDeclaration =
            "\n" +
            "   <initialValueTemplate namespace=\"usbdm\"\n" +
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
            "      variables=\"/PCR/nvic_irqLevel\"\n" +
            "   ><![CDATA[\n" +
            "      \\t   /// %%description\n" +
            "      \\t   %%enumClass %%registerName = %%enumClass_Normal;\\n\\n\n" +
            "   ]]></initialValueTemplate>\n";
            
      if (irqsUsed) {
         resultSb.append(String.format(irqLevelTemplate));
      }
      
      String configureMethodIrq =
            "      \\t\n" +
            "      \\t   if constexpr (Info::irqHandlerInstalled) {\n" +
            "      \\t      // Only set call-back if feature enabled\n" +
            "      \\t      setCallback(init.callbackFunction);\n" +
            "      \\t      enableNvicInterrupts(init.irqlevel);\n" +
            "      \\t   }\n";
            
      String configureMethodTemplate =
            "\n" +
            "<!--   Configure methods -->\n" +
            "\n" +
            "   <template discardRepeats=\"true\" key=\"/%s/InitMethod\" namespace=\"all\" ><![CDATA[\n" +
            "      \\t/**\n" +
            "      \\t * Configure %s from values specified in init\n" +
            "      \\t *\n" +
            "      \\t * @param init Class containing initialisation values\n" +
            "      \\t */\n" +
            "      \\tstatic void configure(const typename Info::Init &init) {\n" +
            "%s" +
            "      \\t   enable();\n" +
            "      \\t\n" +
            "%s" +
            "      \\t\n" +
            "      \\t   calibrate();\n" +
            "      \\t}\n" +
            "      \\t\n" +
            "      \\t/**\n" +
            "      \\t * Configure with default settings.\n" +
            "      \\t * Configuration determined from Configure.usbdmProject\n" +
            "      \\t */\n" +
            "      \\tstatic inline void defaultConfigure() {\n" +
            "      \\t\n" +
            "      \\t   // Update settings\n" +
            "      \\t   configure(Info::DefaultInitValue);\n" +
            "      \\t}\\n\n" +
            "   ]]></template>\n";

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
      
      if (irqsUsed) {
         resultSb.append(
               String.format(configureMethodTemplate,
                     peripheralBasename.toUpperCase(), peripheralBasename.toUpperCase(),
                     configureMethodIrq, sb.toString()));
      }
      else {
         resultSb.append(
               String.format(configureMethodTemplate,
                     peripheralBasename.toUpperCase(), peripheralBasename.toUpperCase(),
                     "", sb.toString()));
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
            "   <setTemplate namespace=\"usbdm\"\n" +
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
            "      #if $(irqHandlingMethod)\n" +
            "      \\t      %registerName = %paramExpression;\n" +
            "      #else\n" +
            "      \\t      (void)%paramExpression;\n" +
            "      #endif\n" +
            "      \\t   }\\n\\n\n" +
            "   ]]></setTemplate>\n";
      
      boolean constructorTitleDone = false;

      if (irqsUsed) {
         resultSb.append(constructorTitle);
         constructorTitleDone = true;
         resultSb.append(irqHandlerConstructorTemplate);
      }
      
      String enumConstructorTemplate =
          "\n" +
          "   <for keys=\"r\"\n" +
          "      values=\"\n" +
          "%s\n" +
          "            \">\n" +
          "      <setTemplate namespace=\"usbdm\"\n" +
          "         variables=\"%%(r)\"\n" +
          "         linePadding=\"xxx\">\n" +
          "      <![CDATA[\n" +
          "         \\t   /**\n" +
          "         \\t    * Constructor for %%description\n" +
          "         \\t    *\n" +
          "         \\t    * @tparam   Types\n" +
          "         \\t    * @param    rest\n" +
          "         \\t    *\n" +
          "         %%comments\n" +
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
    
      String integerConstructorTemplate =
          "\n" +
          "   <for keys=\"r\"\n" +
          "      values=\"\n" +
          "%s\n" +
          "            \">\n" +
          "      <setTemplate namespace=\"usbdm\"\n" +
          "         variables=\"%%(r)\"\n" +
          "         linePadding=\"xxx\">\n" +
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
          "         \\t   constexpr Init(unsigned value, Types... rest) : Init(rest...) {\n" +
          "         \\t\n" +
          "         \\t      %%registerName = (%%registerName&~%%mask) | value;\n" +
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
         resultSb.append(String.format(enumConstructorTemplate, sb.toString()));
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
         resultSb.append(String.format(integerConstructorTemplate, sb.toString()));
      }

      String initValueTemplate =
            "<!--   Default Initialisation value -->\n" +
            "\n" +
            "   <initialValueTemplate\n" +
            "      separator=\",\"\n" +
            "      terminator=\",\"\n" +
            "      variables=\"\n" +
            "%s\n" +
            "         \"\n" +
            "   ><![CDATA[\n" +
            "      $(init_description)\n" +
            "      \\ttypedef $(_class)BasicInfo::Init Init;\n" +
            "      \\t\n" +
            "      \\t/**\n" +
            "      \\t * Default initialisation value for $(_class)\n" +
            "      \\t * This value is created from Configure.usbdmProject settings\n" +
            "      \\t */\n" +
            "      \\tstatic constexpr Init DefaultInitValue = {%%initExpression\n" +
            "      \\t};\\n\\n\n" +
            "   ]]></initialValueTemplate>\n";
      
      String irqReferenceDeclaration =
            "      \\t/**\n" +
            "      \\t * $(_class) interrupt call back\n" +
            "      \\t */\n" +
            "      \\ttypedef $(_class)BasicInfo::CallbackFunction CallbackFunction;\n" +
            "      \\t\n";
      
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
                  sb.append(",\n");
               }
               sb.append("            "+registerFieldName);
            }
         }
      }
      if (irqsUsed) {
         if (!sb.isEmpty()) {
            sb.append(",\n");
         }
         sb.append("            irqLevel");
         resultSb.append(String.format(initValueTemplate, sb.toString(), irqReferenceDeclaration));
      }
      else {
         resultSb.append(String.format(initValueTemplate, sb.toString(), ""));
      }
      
      String configureMethodsOpen =
            "\n" +
            "   <template>\n" +
            "      <![CDATA[\n" +
            "      \\t/**\n" +
            "      \\t * Configure $(_basename)\n" +
            "      \\t */\n" +
            "      \\tstatic void configure(const Init &configValue) {\n";
      
      String configureMethodsClose =
            "      \\t}\n" +
            "      \\t\n" +
            "      \\t/**\n" +
            "      \\t * Default initialisation for $(_basename)\n" +
            "      \\t */\n" +
            "      \\tstatic void defaultConfigure() {\n" +
            "      \\t   configure(DefaultInitValue);\n" +
            "      \\t}\n" +
            "      \\t\\n\n" +
            "   ]]>\n" +
            "   </template>\n";
      
      resultSb.append(configureMethodsOpen);
      
      for (Cluster cluster:peripheral.getRegisters()) {
         if (cluster instanceof Register) {
            Register reg = (Register) cluster;
            if (reg.getAccessType() == AccessType.ReadOnly) {
               continue;
            }
            Boolean foundValidField = false;
            String regName = reg.getName().replace("%s", "");
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               foundValidField = true;
            }
            if (foundValidField) {
               resultSb.append(String.format("      \\t   %s->%-15s = configValue.%s;\n", peripheralBasename.toLowerCase(), regName, regName.toLowerCase()));
            }
         }
      }
      resultSb.append(configureMethodsClose);

      String closeInitClass =
            "\n" +
            "   <template namespace=\"usbdm\">\n" +
            "      <![CDATA[\n" +
            "      \\t}; // $(_class)::BasicInfo::Init\\n\\n\n" +
            "   ]]>\n" +
            "   </template>\n";
      
      String closeBasicInfoClass =
            "\n" +
            "   <template namespace=\"usbdm\">\n" +
            "      <![CDATA[\n" +
            "      }; // $(_class)::BasicInfo\\n\\n\n" +
            "   ]]>\n" +
            "   </template>\n" +
            "\n";

      resultSb.append(closeInitClass);
      resultSb.append(closeBasicInfoClass);
   }
   
   static String peripheralsHeaderFilesToDo[] = {
         "OSC",
         "PMC",
         "ICS",
   };
   
   static boolean doThisPeripheralHeaderFile(String name) {
      for (String tname:peripheralsHeaderFilesToDo) {
         if (name.startsWith(tname)) {
            return true;
         }
      }
      return false;
   }
   

   void writePostamble() {
      String common =
            "   <!-- ************* Common ****************** -->\n" +
            "\n" +
            "   <template key=\"/%s/declarations\" namespace=\"all\"  ><![CDATA[\n" +
            "   \\t/**\n" +
            "   \\t * Class representing $(_name)\n" +
            "   \\t */\n" +
            "   \\tclass $(_class) : public $(_base_class)Base_T<$(_class)Info> {};\\n\n" +
            "   ]]></template>\n" +
            "\n" +
            "   <validate\n" +
            "      class=\"net.sourceforge.usbdm.deviceEditor.validators.PeripheralValidator\">\n" +
            "   </validate>\n" +
            "\n";
      resultSb.append(String.format(common,peripheralBasename));
      
      if (doThisPeripheralHeaderFile(peripheralName)) {
         String fileInclude =
            "   <projectActionList id = \"%s_files\" >\n" +
            "      <copy source=\"Project_Headers/%s.h\"  target=\"Project_Headers/%s.h\"  overwrite=\"true\"  derived=\"true\" />\n" +
            "   </projectActionList>\n" +
            "\n";
         String filename = peripheralBasename.toLowerCase();
         resultSb.append(String.format(fileInclude, peripheralName.toLowerCase(), filename+fileSuffix, filename));
      }
      if (isSim) {
         //         resultSb.append("\n   <xi:include href=\"_sim_commonTemplates.xml\" />\n");
      }
      resultSb.append("\n<signals />\n");

      resultSb.append("\n</peripheralPage>\n");
   }
   
   static String peripheralsToDo[] = {
         "SIM",
//         "OSC",
//         "PMC",
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
         instance.writePostamble();
         instance.savePeripheralFiles();
      }
   }

   public static void main(String[] args) throws Exception {
      doAllPeripherals("FRDM_KE04Z");
//      doAllPeripherals("FRDM_KE06Z");
   }

}
