package tests.internal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
import net.sourceforge.usbdm.peripheralDatabase.InterruptEntry;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral;
import net.sourceforge.usbdm.peripheralDatabase.Register;

public class CreateDeviceSkeletonFromSVD {

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

   private StringBuilder resultSb;
   
   // Enum for irq if multiple
   private StringBuilder irqEnum;
   
   private final int MAX_FIELD_WIDTH_FOR_AUTO_ENUM = 3;

   CreateDeviceSkeletonFromSVD(String fileSuffix, DevicePeripherals peripherals, Peripheral peripheral) throws Exception {
      this.fileSuffix     = "-"+fileSuffix;
      this.peripheralName = peripheral.getName();
      this.peripheral     = peripheral;
      irqsUsed = peripheral.getInterruptEntries() != null;
      peripheralBasename  = peripheralName;
      if (peripheralBasename.matches(".*[0-9]")) {
         peripheralBasename = peripheralBasename.substring(0, peripheralBasename.length()-1);
      }
      if (peripheralBasename.matches("GPIO[A-Z]")) {
         peripheralBasename = "GPIO";
      }
      resultSb          = new StringBuilder();
   }
   
   void savePeripheralFiles(boolean isFragment) throws UsbdmException, IOException {
      String filename = peripheral.getSourceFilename();
      if (filename == null) {
         filename = peripheral.getName();
      }
      filename = filename.toLowerCase();
      // USBDM installation path
      Path hardwarePath = Paths.get(Usbdm.getUsbdmResourcePath()).resolve(DeviceInfo.USBDM_ARM_PERIPHERALS_LOCATION).resolve(filename+"_new"+".xml");
      
      Charset charset = Charset.forName("US-ASCII");
      BufferedWriter writer = Files.newBufferedWriter(hardwarePath, charset);
      
      if (isFragment) {
         String stubTemplate = ""
               + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
               + "<!DOCTYPE fragment SYSTEM \"_menu.dtd\" >\n"
               + "<!-- %s -->\n"
               + "\n"
               + "<fragment xmlns:xi=\"http://www.w3.org/2001/XInclude\">\n"
               + "<xi:include href=\"gpio_def.xml\"/>\n"
               + "</fragment>\n";
         resultSb.append(String.format(stubTemplate, filename));
      }
      else {
         final String preamble =""
               + "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
               + "<!DOCTYPE peripheralPage SYSTEM \"_menu.dtd\" >\n"
               + "<!-- %s.xml -->\n"
               + "\n"
               + "<peripheralPage xmlns:xi=\"http://www.w3.org/2001/XInclude\" name=\"_instance\" description=\"%s\" >\n";

         String fileDescription=peripheral.getDescription();

         writer.write(String.format(preamble,
               filename,
               fileDescription
               ));

         if (irqEnum != null) {
            writer.write(irqEnum.toString());
         }
      }
      writer.write(resultSb.toString());
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

   /**
    * Class to visit each register in peripheral
    */
   static abstract class VisitRegisters {

      static String stripRegisterName(String name) {
         name = name.replaceAll("^([^,]*).*", "$1");
         name = name.replace("%s", "");
         return name;
      }
      
      final Peripheral fPeripheral;

      VisitRegisters(Peripheral peripheral) {
         fPeripheral = peripheral;
      }

      /**
       * Visitor for each register
       * 
       * @param reg Register being visited
       */
      abstract void visitor(Register reg, String context);

      /**
       * Get result from visitation
       */
      abstract Object getResult();
      
      /**
       * Get result from visitation as String
       */
      String getResultAsString() {
         return (String)getResult();
      }
      
      void getRegisterNames_Cluster(Cluster cluster, String context) {

         if (cluster instanceof Register) {
            Register reg = (Register)cluster;
            visitor(reg, context);
//            visitor(reg, context+stripRegisterName(cluster.getName()));
         }
         else {
            if (cluster.getDimension()>0) {
               context = context+stripRegisterName(cluster.getName())+"[%%paramName0].";
            }
            else {
               context = context+stripRegisterName(cluster.getName())+".";
            }
            for (Cluster cl:cluster.getRegisters()) {
               getRegisterNames_Cluster(cl, context);
            }
         }
      }

      /**
       * Visit each register
       * 
       * @param doer
       */
      void visit() {
         for (Cluster cluster:fPeripheral.getRegisters()) {
            getRegisterNames_Cluster(cluster, "");
         }
      }
   };
   
   //________________________________________________
   void writePreamble() {

      final String suppress = "\n"
         + "   <equation key=\"suppressInstance\"           value=\"false\"     />\n";
      final String pre = "\n"
         + "   <equation key=\"irq_parameters\"             value=\"\"          />\n"
         + "   <equation key=\"irq_dummy_parameters\"       value=\"\"          />\n"
         + "   <equation key=\"irq_call\"                   value=\"\"          />\n"
         + "   <equation key=\"generateDefault\"            value=\"false\"     />\n"
         + "   <equation key=\"configureInStartupDefault\"  value=\"false\"     />\n"
         + "   <xi:include href=\"enablePeripheral.xml\"    />\n"
         + "   <title />\n"
         + "";

      final String simExtra = "\n"
         + "   <xi:include href=\"_simCommon.xml\" />\n";
      
      String filename = peripheral.getSourceFilename();
      if (filename == null) {
         filename = peripheral.getName();
      }
      filename = filename.toLowerCase();
      isSim = filename.startsWith("sim");
      
      if (peripheral.getName().startsWith("GPIO")) {
         resultSb.append(suppress);
      }
      resultSb.append(String.format(pre, Boolean.toString(irqsUsed)));

//      resultSb.append(usefulInfo);
      
      if (isSim) {
         resultSb.append(simExtra);
      }
      String classDecl =
            "\n"+
            "   <!-- ____ Class Declaration ________ -->\n" +
            "   <constant key=\"_class_declaration\" type=\"String\" value='\"$(_Class)Info : public $(_Structname)BasicInfo\"' />\n";
      resultSb.append(classDecl);
   }

   HashSet<String> usedFieldNames = null;

   void processRegister(Register cluster) {
      String header = ""
            + "\n"
            + "   <!-- ____ %s ____ -->\n";
      String title = ""
            + "   <title description=\"%s\" />\n";
      Register reg = cluster;
      System.out.println("Processing " + reg.toString());
      boolean readOnlyRegister = (reg.getAccessType() == AccessType.ReadOnly);
      String regName = reg.getName().replace("%s", "");
      resultSb.append(String.format(header, regName));
      boolean titleDone = false;
      for (Field field:reg.getFields()) {
         
         String fieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
         
         if (usedFieldNames == null) {
            usedFieldNames = new HashSet<String>();
         }
         if (!usedFieldNames.add(fieldName)) {
            // Repeated filed name - delete
            resultSb.append("<!-- Repeated filed name '"+fieldName+"' -->\n");
            continue;
         }
         
         String hiddenAndDerived = "";
         boolean readOnlyField = readOnlyRegister || (field.getAccessType() == AccessType.ReadOnly);
         
         if (readOnlyField) {
            hiddenAndDerived = "\n      hidden=\"true\""+"\n      derived=\"true\"";
         }
         String condition = "condition=\""+fieldName+"_present\"";
         String enabledBy = "\n      enabledBy=\"enablePeripheralSupport\"";
         if (readOnlyField) {
            enabledBy = "";
         }
         String registerAttr="";
         if (regName.contains("_")) {
            registerAttr = "\n      register=\""+regName.toLowerCase()+"\"";
         }
         String enumName  = prettyName(fieldName);
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
         }
         fieldDescription = padNewline(fieldDescription, "                   ");
         if (toolTip == null) {
            toolTip = "";
         }
         int fieldWidth= (int) field.getBitwidth();
         if (!titleDone) {
            resultSb.append(String.format(title, reg.getDescription()));
            titleDone = true;
         }
         if (enumerations.isEmpty() && (fieldWidth>MAX_FIELD_WIDTH_FOR_AUTO_ENUM)) {
            // Assume intOption
            resultSb.append("\n   <intOption key=\"" + fieldName + "\" " + condition);
            resultSb.append(enabledBy);
            resultSb.append(hiddenAndDerived);
            resultSb.append(registerAttr);
            if (readOnlyField) {
               // Read-only so just return an integer
               resultSb.append("\n      typeName=\""+ getRegisterCType(fieldWidth) + "\"\n");
            }
            else {
               // Read/Write so create enum wrapper
               resultSb.append("\n      typeName=\""+ enumName + "\"\n");
               resultSb.append("      baseType=\""+ getRegisterCType((field.getBitwidth()+7)/8) + "\"\n");
            }
            resultSb.append("      toolTip=\"" + toolTip + "\"\n");
            resultSb.append("      description=\"" + fieldDescription + "\"\n");
            if (!readOnlyField) {
               resultSb.append("      value=\"0\"\n");
            }
            if (!readOnlyField) {
               resultSb.append(String.format("      min=\"%d\" max=\"%d\"\n", 0, (1<<fieldWidth)-1));
            }
            resultSb.append("   />\n");
            fieldDefaultList.add("0, // "+fieldDescription);
         }
         else {
            fieldNameList.add(regName.toLowerCase()+"_"+field.getName().toLowerCase());
            String typeName = "choiceOption";
            if (((enumerations != null) && (enumerations.size() == 2))||(fieldWidth==1)) {
               typeName = "binaryOption";
            }
            resultSb.append("\n   <"+typeName+" key=\"" + fieldName + "\" " + condition);
            resultSb.append(enabledBy);
            resultSb.append(hiddenAndDerived);
            resultSb.append(registerAttr);
            resultSb.append("\n      typeName=\"" +  enumName +"\"\n");
            resultSb.append("      baseType=\""+ getRegisterCType((reg.getWidth()+7)/8) + "\"\n");

            resultSb.append("      toolTip=\"" + toolTip + "\"\n");
            resultSb.append("      description=\"" + fieldDescription + "\" >\n");

            String defaultValue = null;
            if (enumerations.isEmpty()) {
               // Generate dummy choiceOption
               for (int enumValue = 0; enumValue<(1<<fieldWidth); enumValue++) {
                  String enumDescription = "Choice "+enumValue;
                  resultSb.append(String.format("      <choice %-10s %10s %s />\n",
                        "name=\"" + enumDescription + "\"",
                        "value=\"" + enumValue + "\"",
                        "enum=\""+makeEnumName(enumDescription)+"\""));
               }
            }
            else {
               int descriptionWidth = 10;
               for (Enumeration enumeration:enumerations) {
                  int width = truncateAtNewlineOrTab(enumeration.getDescription()).length();
                  if (width > descriptionWidth) {
                     descriptionWidth = width;
                  }
               }
               descriptionWidth += "name=\"\"".length();
               for (Enumeration enumeration:field.getEnumerations()) {
                  String enumDescription = XML_BaseParser.escapeString(truncateAtNewlineOrTab(enumeration.getDescription()));

                  resultSb.append(String.format("      <choice %-"+descriptionWidth+"s %10s %s />\n",
                        "name=\"" + enumDescription + "\"",
                        "value=\"" + enumeration.getValue() + "\"",
                        "enum=\""+makeEnumName(enumDescription)+"\""));
                  if (defaultValue == null) {
                     defaultValue = enumName+"_"+makeEnumName(enumDescription)+"  // "+fieldDescription;
                  }
               }
            }
            resultSb.append("   </"+typeName+" >\n");
            fieldDefaultList.add(defaultValue);
         }
      }
   }
   
   void processCluster(Cluster unknown) {
      if (unknown instanceof Register) {
         processRegister((Register) unknown);
      }
      for (Cluster cluster:unknown.getRegisters()) {
         processCluster(cluster);
      }
   }
   
   void processRegisters() {
      
//      registerNameList = new ArrayList<>();
      fieldNameList    = new ArrayList<String>();
      fieldDefaultList = new ArrayList<>();
      
      for (Cluster cluster:peripheral.getRegisters()) {
         processCluster(cluster);
      }
   }
   
   void writeSettersAndGetters() {
      resultSb.append(
            "\n" +
            "   <!-- ____ Getters and Setters ____________ -->\n");
      
      if (fieldNameList.size() == 0) {
         // No fields!
         return;
      }
      final String simpleVariableTemplate =
          "      <variableTemplate variables=\"%(field)\" condition=\"%(set)\" codeGenCondition=\"%(genCode)\"\n" +
          "      ><![CDATA[\n" +
          "         \\t/**\n" +
          "         \\t * Set %description\n" +
          "         \\t * (%(field))\n" +
          "         \\t *\n" +
          "         %paramDescription\n" +
          "         \\t */\n" +
          "         \\tstatic void set%(name)(%params) {\n" +
          "         \\t   %fieldAssignment;\n" +
          "         \\t}\n" +
          "         \\t\\n\n" +
          "      ]]></variableTemplate>\n"+
          "      <variableTemplate variables=\"%(field)\" condition=\"%(get)\" codeGenCondition=\"%(genCode)\"\n" +
          "      ><![CDATA[\n" +
          "         \\t/**\n" +
          "         \\t * Get %description\n" +
          "         \\t * (%(field))\n" +
          "         \\t *\n" +
          "         \\t * @return %tooltip\n" +
          "         \\t */\n" +
          "         \\tstatic %returnType get%(name)() {\n" +
          "         \\t   return %fieldExtract;\n" +
          "         \\t}\n" +
          "         \\t\\n\n" +
          "      ]]></variableTemplate>\n"+
          "      <variableTemplate variables=\"%(field)\" condition=\"%(clear)\" codeGenCondition=\"%(genCode)\"\n" +
          "         tooltipPadding=\"x*x\"" +
          "      ><![CDATA[\n" +
          "         \\t/**\n" +
          "         \\t * Clear %description\n" +
          "         \\t * (%(field))\n" +
          "         \\t *\n" +
          "         \\t * %tooltip\n" +
          "         \\t */\n" +
          "         \\tstatic void clear%(name)() {\n" +
          "         \\t   %register = %register|%mask;\n" +
          "         \\t}\n" +
          "         \\t\\n\n" +
          "      ]]></variableTemplate>\n";
      
      final String indexedVariableTemplate =
            "      <variableTemplate variables=\"%(field)\" condition=\"%(set)\" codeGenCondition=\"%(genCode)\" context=\"%(context)\" nonDefaultParams=\"2\"\n" +
            "      ><![CDATA[\n" +
            "         \\t/**\n" +
            "         \\t * Set %description1\n" +
            "         \\t * (%(field))\n" +
            "         \\t *\n" +
            "         %paramDescription\n" +
            "         \\t */\n" +
            "         \\tstatic void set%(name)(%params) {\n" +
            "         \\t   %fieldAssignment1;\n" +
            "         \\t}\n" +
            "         \\t\\n\n" +
            "      ]]></variableTemplate>\n"+
            "      <variableTemplate variables=\"%(field)\" condition=\"%(get)\" codeGenCondition=\"%(genCode)\" context=\"%(context)\"\n" +
            "      ><![CDATA[\n" +
            "         \\t/**\n" +
            "         \\t * Get %description1\n" +
            "         \\t * (%(field))\n" +
            "         \\t *\n" +
            "         %paramDescription0\n" +
            "         \\t *\n" +
            "         \\t * @return %tooltip1\n" +
            "         \\t */\n" +
            "         \\tstatic %paramType1 get%(name)(%param0) {\n" +
            "         \\t   return %fieldExtract1;\n" +
            "         \\t}\n" +
            "         \\t\\n\n" +
            "      ]]></variableTemplate>\n"+
            "      <variableTemplate variables=\"%(field)\" condition=\"%(clear)\" codeGenCondition=\"%(genCode)\" context=\"%(context)\"\n" +
            "         tooltipPadding=\"x*x\"" +
            "      ><![CDATA[\n" +
            "         \\t/**\n" +
            "         \\t * Clear %description1\n" +
            "         \\t * (%(field))\n" +
            "         \\t *\n" +
            "         \\t * %tooltip\n" +
            "         \\t *\n" +
            "         %paramDescription0\n" +
            "         \\t */\n" +
            "         \\tstatic void clear%(name)(%param0) {\n" +
            "         \\t   %register1 = %register1|%mask1;\n" +
            "         \\t}\n" +
            "         \\t\\n\n" +
            "      ]]></variableTemplate>\n";
        
      VisitRegisters createSimpleFieldList = new VisitRegisters(peripheral) {

         final StringBuilder resultSb = new StringBuilder();
         Boolean firstField = true;

         @Override
         void visitor(Register register, String context) {
            
            if ((register.getDimension()>0)||(context.contains("[%%paramName0]"))) {
               return;
            }
            context = context+stripRegisterName(register.getName());
            boolean newRegister = true;
            System.err.println("Context = '"+context+"'");
            String regName = stripRegisterName(register.getName());
            for (Field field:register.getFields()) {
               if (!firstField) {
                  resultSb.append(";\n");
               }
               if (newRegister) {
                  resultSb.append("\n");
                  newRegister = false;
               }
               String fieldName  = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               String methodName = regName.toLowerCase()+"_"+field.getName().toLowerCase();
               String get   = "true";
               String set   = "true";
               String clear = "false";
               if (field.getAccessType() == AccessType.ReadOnly) {
                  set = "false";
               }
               firstField = false;
               resultSb.append(String.format("         %-30s : %-5s : %-5s : %-5s : enableGettersAndSetters : %s",
                     fieldName, set, get, clear, prettyName(methodName)));
            }
         }
         @Override
         Object getResult() {
            return resultSb.toString();
         }
      };

      VisitRegisters createIndexedFieldList = new VisitRegisters(peripheral) {

         final StringBuilder resultSb = new StringBuilder();
         Boolean firstField = true;
         
         @Override
         void visitor(Register register, String context) {

            if ((register.getDimension()==0)&&(!context.contains("[%%paramName0]"))) {
               return;
            }
            if (register.getDimension()>0) {
               // Assume this is the actual register
               context = context+"%s[%%paramName0]";
            }
            else {
               context = context+"%s";
            }
            String regName = stripRegisterName(register.getName());
            if (!firstField) {
               resultSb.append(";\n\n");
               firstField = true;
            }
            for (Field field:register.getFields()) {
               if (!firstField) {
                  resultSb.append(";\n");
               }
               String prefix = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_";
               String fieldName  = prefix+"index,"+prefix+field.getName().toLowerCase()+"[]";
               String methodName = regName.toLowerCase()+"_"+field.getName().toLowerCase();
               String get   = "true";
               String set   = "true";
               String clear = "false";
               if (field.getAccessType() == AccessType.ReadOnly) {
                  set = "false";
               }
               firstField = false;
               resultSb.append(String.format("         %-36s : %-5s : %-5s : %-5s : enableGettersAndSetters : %-25s : %s",
                     fieldName, set, get, clear, context, prettyName(methodName)));
            }
         }
         @Override
         Object getResult() {
            return resultSb.toString();
         }
      };

      createSimpleFieldList.visit();
      String simpleResult = createSimpleFieldList.getResultAsString();
      if (!simpleResult.isBlank()) {
         resultSb.append(
               "\n" +
               "   <for keys=\"field                     : set   : get   : clear : genCode                 : name\"\n" +
               "        values=\"\n" + simpleResult + "\" >\n");
         resultSb.append(simpleVariableTemplate);
         resultSb.append("   </for>\n");
      }
      createIndexedFieldList.visit();
      String indexedResult = createIndexedFieldList.getResultAsString();
      String preIndexedVariableTemplate = ""
            + "   <choiceOption key=\"xxx_yyy_num\"\n"
            + "      valueFormat=\"%s\"\n"
            + "      hidden=\"true\"\n"
            + "      derived=\"true\"\n"
            + "      typeName=\"XXXPortNum\"\n"
            + "      baseType=\"uint32_t\"\n"
            + "      toolTip=\"Selects a XXXX\"\n"
            + "      description=\"XXX Number\" >\n"
            + "      <choiceExpansion  keys=\"index\" dim=\"=ZZZ\"\n"
            + "         name=\"Slave Port %(index)\" value=\"%(index)\" enum=\"%(index)\" />\n"
            + "   </choiceOption>\n"
            + "\n"
            + "";
      if (!indexedResult.isBlank()) {
         resultSb.append(preIndexedVariableTemplate);
         resultSb.append(
               "\n" +
               "   <for keys=\"field                     : set   : get   : clear : genCode                 : context              : name\"\n" +
               "        values=\"\n" + indexedResult + "\" >\n");
         resultSb.append(indexedVariableTemplate);
         resultSb.append("   </for>\n");
      }
   }
   
   /**
    * Gets C integer type appropriate for size e.g. uint8_t etc
    * 
    * @param size Size in bytes
    * 
    * @return C type as string
    */
   String getRegisterCType(long size) {
      if (size == 1) {
         return "uint8_t";
      }
      if (size == 2) {
         return "uint16_t";
      }
      return "uint32_t";
   }
   
   //________________________________________________________________________
   void getFieldNames_Register(List<String> list, Register register) {
      
      if (register.getAccessType() == AccessType.ReadOnly) {
         return;
      }
      String regName = register.getName().replace("%s", "");
      for (Field field:register.getFields()) {
         if (field.getAccessType() == AccessType.ReadOnly) {
            continue;
         }
         String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
         list.add(registerFieldName);
      }
   }

   void getFieldNames_Cluster(List<String> list, Cluster cluster) {

      if (cluster instanceof Register) {
         getFieldNames_Register(list, (Register) cluster);
      }
      else {
         for (Cluster cl:cluster.getRegisters()) {
            getFieldNames_Cluster(list, cl);
         }
      }
   }
   
   List<String> getAllFieldNames() {
      List<String> list = new ArrayList<String>();
      
      for (Cluster cluster:peripheral.getRegisters()) {
         getFieldNames_Cluster(list, cluster);
      }
      return list;
   }
   
   //________________________________________________________________________
   void getRegisterNames_Cluster(List<String> list, Cluster cluster) {

      if (cluster instanceof Register) {
         Register reg = (Register)cluster;
         list.add(reg.getName());
      }
      else {
         for (Cluster cl:cluster.getRegisters()) {
            getRegisterNames_Cluster(list, cl);
         }
      }
   }
   
   List<String> getAllRegisterNames() {
      List<String> list = new ArrayList<String>();
      
      for (Cluster cluster:peripheral.getRegisters()) {
         getRegisterNames_Cluster(list, cluster);
      }
      return list;
   }
   
   void writeInitClass() {
      
      writeHandlers();
      
      final String open_init_class ="\n"+
         "   <!-- ________ %s Init class ____________________________ -->\n" +
         "\n" +
         "   <template where=\"basicInfo\" codeGenCondition=\"/$(_STRUCTNAME)/generateSharedInfo\" >\n" +
         "   <![CDATA[\n" +
         "      \\t/**\n" +
         "      \\t * Class used to do initialisation of the $(_Baseclass)\n" +
         "      \\t *\n" +
         "      \\t * This class has a templated constructor that accepts various values.\n" +
         "      \\t * Parameters available may vary with device - see $(_Class)::DefaultInitValue for relevant example.\n" +
         "      \\t * Omitted parameters default to zero (disabled) or unchanged if initialiser is provided as last parameter.\n" +
         "      \\t *\n" +
         "      \\t * @note This constructor may be used to create a const instance in Flash\n" +
         "      \\t *\n" +
         "      \\t * Example:\n" +
         "      \\t * @code\n" +
         "      \\t * ///\n" +
         "      \\t * /// $(_NAME) call-back\n" +
         "      \\t * ///\n" +
         "      \\t * /// @param status  Status reflecting active inputs\n" +
         "      \\t * ///\n" +
         "      \\t * void $(_name)Callback(ErrorCode ec) {\n" +
         "      \\t *    ....\n" +
         "      \\t * }\n" +
         "      \\t *\n" +
         "      \\t * static const $(_Class)::Init $(_name)Init {\n" +
         "      \\t *\n" +
         "      \\t *   // Setup values\n" +
         "XXXXXXXXXXXXXXXXXXXXXX\n" +
         "      \\t *\n" +
         "      \\t *   $(_name)Callback,                 // Call-back to execute on event - call-back function name\n" +
         "      \\t *   NvicPriority_Low,                 // Priority for interrupt - Low\n" +
         "      \\t *\n" +
         "      \\t *   // Optional base value to start with (must be last parameter)\n" +
         "      \\t *   $(_Class)::DefaultInitValue   // Used as base value modified by above\n" +
         "      \\t * };\n" +
         "      \\t *\n" +
         "      \\t * // Initialise $(_Class) from values specified above\n" +
         "      \\t * $(_Class)::configure($(_name)Init)\n" +
         "      \\t * @endcode\n" +
         "      \\t */\n" +
         "      \\tclass Init {\n" +
         "      \\t\n" +
         "      \\tprivate:\n" +
         "      \\t   /**\n" +
         "      \\t    * Prevent implicit parameter conversions\n" +
         "      \\t    */\n" +
         "      \\t   template <typename... Types>\n" +
         "      \\t   constexpr Init(Types...) = delete;\n" +
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
         "   ]]>\n"+
         "   </template>\n";
      resultSb.append(String.format(open_init_class, peripheralBasename));
      
      resultSb.append("\n   <!-- ____ Init class Member variables ________ -->\n");
      
      final String initIrqMemberTemplate = "\n"
            + "   <variableTemplate where=\"basicInfo\" codeGenCondition=\"/$(_STRUCTNAME)/generateSharedIrqInfo\"\n"
            + "      variables=\"irqHandlingMethod\"\n"
            + "      linePadding=\"xxx\"\n"
            + "   ><![CDATA[\n"
            + "      %multilineDescription\n"
            + "      \\t   %params = nullptr;\\n\\n\n"
            + "   ]]></variableTemplate>\n";
      
      resultSb.append(initIrqMemberTemplate);
      
      // Create Init registers
      VisitRegisters createInitRegistersList = new VisitRegisters(peripheral) {
         
         final ArrayList<String> result = new ArrayList<String>();
         HashSet<String> usedFieldNames = new HashSet<String>();
         final int MAX_LIST_LENGTH = 40;
         final String memberDeclaration =
               "%%baseType   : %s";
         final String padding=
               "\n                             ";
         
         @Override
         void visitor(Register register, String context) {

            if (register.getAccessType() == AccessType.ReadOnly) {
               return;
            }
            context = context+stripRegisterName(register.getName());
            String regName = register.getName().replace("%s", "");
            StringBuilder sb = new StringBuilder();
            int lineLength=0;
            for (Field field:register.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               if (!usedFieldNames.add(registerFieldName)) {
                  continue;
               }
               if (!sb.isEmpty()) {
                  sb.append(",");
                  lineLength++;
               }
               if (lineLength>MAX_LIST_LENGTH) {
                  lineLength = 0;
                  sb.append(padding);
               }
               sb.append(registerFieldName);
               lineLength += registerFieldName.length();
            }
            if (!sb.isEmpty()) {
               while(lineLength<(MAX_LIST_LENGTH+12)) {
                  sb.append(" ");
                  lineLength++;
               }
               sb.append(" :   0");
               result.add(String.format(memberDeclaration, sb.toString()));
            }
         }

         @Override
         String[] getResult() {
            return result.toArray(new String[result.size()]);
         }
         
      };
      
      final String memberPreamble = "\n"
            + "   <for keys=\" type        : variables                                            : init    \"\n"
            + "       values=\"";

      final String memberDeclaration = " >\n"
            + "      <variableTemplate where=\"basicInfo\" codeGenCondition=\"/$(_STRUCTNAME)/generateSharedInfo\"\n"
            + "         variables=\"%(variables)\"\n"
            + "         linePadding=\"xxx\"\n"
            + "      ><![CDATA[\n"
            + "         %multilineDescription\n"
            + "         \\t   %(type) %registerName = %(init);\\n\\n\n"
            + "      ]]></variableTemplate>\n"
            + "   </for>\n";
      
      createInitRegistersList.visit();
      boolean firstRegister = true;
      for (String res:(String[])(createInitRegistersList.getResult())) {
         if (firstRegister) {
            resultSb.append(memberPreamble);
            firstRegister = false;
         }
         else {
            resultSb.append("      ;\n               ");
         }
         resultSb.append(res);
      }
      resultSb.append("      ;\n               %paramType  : /PCR/nvic_irqLevel                                   :   %defaultValue   \"");
      resultSb.append(memberDeclaration);

      /*
       *   Create Irq Constructors
       */
      String constructorTitle =""
          + "\n"
          + "   <!-- ____ Init class Constructors ____________ -->\n";
      
      resultSb.append(constructorTitle);

      //________________________________________________________________________________________________
      
      final String initIrqConstructor = "\n"
            + "   <variableTemplate where=\"basicInfo\" codeGenCondition=\"/$(_STRUCTNAME)/generateSharedIrqInfo\"\n"
            + "      variables=\"irqHandlingMethod\"\n"
            + "      linePadding=\"xxx\"\n"
            + "   ><![CDATA[\n"
            + "      \\t   /**\n"
            + "      \\t    * Constructor for %description\n"
            + "      \\t    * (%variables)\n"
            + "      \\t    *\n"
            + "      \\t    * @tparam   Types\n"
            + "      \\t    * @param    rest\n"
            + "      \\t    *\n"
            + "      %paramDescription\n"
            + "      \\t    */\n"
            + "      \\t   template <typename... Types>\n"
            + "      \\t   constexpr Init(%params, Types... rest) : Init(rest...) {\n"
            + "      \\t\n"
            + "      \\t      this->%paramName = %paramExpression;\n"
            + "      \\t   }\\n\\n\n"
            + "   ]]></variableTemplate>\n";
      
      resultSb.append(initIrqConstructor);
      
////      String irqHandlerConstructorTemplate =
////            "\n" +
////            "   <variableTemplate where=\"basicInfo\" codeGenCondition=\"/$(_STRUCTNAME)/generateSharedIrqInfo\"\n" +
////            "      variables=\"/PCR/nvic_irqLevel\"\n" +
////            "      linePadding=\"xxx\"\n" +
////            "   ><![CDATA[\n" +
////            "      \\t   /**\n" +
////            "      \\t    * Constructor for %description\n" +
////            "      \\t    * (%variables)\n" +
////            "      \\t    *\n" +
////            "      \\t    * @tparam   Types\n" +
////            "      \\t    * @param    rest\n" +
////            "      \\t    *\n" +
////            "      %paramDescription\n" +
////            "      \\t    */\n" +
////            "      \\t   template <typename... Types>\n" +
////            "      \\t   constexpr Init(%params, Types... rest) : Init(rest...) {\n" +
////            "      \\t\n" +
////            "      \\t      %registerName = %paramExpression;\n" +
////            "      \\t   }\\n\\n\n" +
////            "   ]]></variableTemplate>\n";
////
//      if (irqsUsed) {
//         resultSb.append(irqHandlerConstructorTemplate);
//      }
//
      /*
       *   Create Constructors
       */
      String constructorListTemplateForEnumeratedFields = ""
          + "\n"
          + "   <for keys=\"r\"\n"
          + "      values=\"/PCR/nvic_irqLevel;"
          + "%s\n"
          + "            \" >\n"
          + "      <variableTemplate where=\"basicInfo\" codeGenCondition=\"/$(_STRUCTNAME)/generateSharedInfo\"\n"
          + "         variables=\"%%(r)\"\n"
          + "         linePadding=\"xxx\" >\n"
          + "      <![CDATA[\n"
          + "         \\t   /**\n"
          + "         \\t    * Constructor for %%description\n"
          + "         \\t    * (%%variables)\n"
          + "         \\t    *\n"
          + "         \\t    * @tparam   Types\n"
          + "         \\t    * @param    rest\n"
          + "         \\t    *\n"
          + "         %%paramDescription\n"
          + "         \\t    */\n"
          + "         \\t   template <typename... Types>\n"
          + "         \\t   constexpr Init(%%params, Types... rest) : Init(rest...) {\n"
          + "         \\t\n"
          + "         \\t      %%constructorFieldAssignment;\n"
          + "         \\t   }\n"
          + "         \\t\\n\n"
          + "      ]]>\n"
          + "      </variableTemplate>\n"
          + "   </for>\n"
          + "\n";
    
      VisitRegisters createConstructorsForEnumeratedFields = new VisitRegisters(peripheral) {
         
         final StringBuilder resultSb = new StringBuilder();
         HashSet<String> usedFieldNames = new HashSet<String>();
         final int MAX_LINE_LENGTH = 40;
         final String PADDING = "\n            ";
         
         @Override
         void visitor(Register reg, String context) {
            if (reg.getAccessType() == AccessType.ReadOnly) {
               return;
            }
            context = context+stripRegisterName(reg.getName());
            boolean firstInRegister = true;
            int lineLength=0;
            String regName = reg.getName().replace("%s", "");
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               if (!usedFieldNames.add(registerFieldName)) {
                  continue;
               }
               if (!resultSb.isEmpty()) {
                  resultSb.append(";");
                  lineLength++;
                  if (lineLength>MAX_LINE_LENGTH) {
                     resultSb.append(PADDING);
                     lineLength = 0;
                  }
               }
               if (firstInRegister) {
                  resultSb.append(PADDING);
                  firstInRegister = false;
               }
               resultSb.append(registerFieldName);
               lineLength += registerFieldName.length();
            }
         }

         @Override
         Object getResult() {
            return resultSb.toString();
         }
         
      };
      
      createConstructorsForEnumeratedFields.visit();
      if (!createConstructorsForEnumeratedFields.getResultAsString().isBlank()) {
         resultSb.append(
               String.format(constructorListTemplateForEnumeratedFields,
               createConstructorsForEnumeratedFields.getResultAsString()));
      }
      
      /*
       * Create configure methods
       */
      
      String configureMethod = "\n"
            + "   <!-- ____ Init class Configure method ____ -->\n"
            + "\n"
            + "   <template codeGenCondition=\"enablePeripheralSupport\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Configure with default settings.\n"
            + "      \\t * Configuration determined from Configure.usbdmProject\n"
            + "      \\t */\n"
            + "      \\tstatic inline void defaultConfigure() {\n"
            + "      \\t\n"
            + "      \\t   // Update settings\n"
            + "      \\t   configure(DefaultInitValue);\n"
            + "      \\t}\n"
            + "      \\t\n"
            + "      \\t/**\n"
            + "      \\t * Configure $(_BASENAME) from values specified in init\n"
            + "      \\t *\n"
            + "      \\t * @param init Class containing initialisation values\n"
            + "      \\t */\n"
            + "      \\tstatic void configure(const Init &init) {\n"
            + "      \\t\n"
            + "      \\t   // Enable peripheral\n"
            + "      \\t   enable();\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n";

      String configureMethodIrq = ""
            + "   <template codeGenCondition=\"irqHandlingMethod\" condition=\"irqHandlingMethod\" >\n"
            + "   <![CDATA[\n"
            + "      \\t   // Configure call-backs\n"
            + "      \\t   setCallback(init.callbackFunction);\\n\n"
            + "   ]]>\n"
            + "   </template>\n";
      
      resultSb.append(String.format(configureMethod));
      resultSb.append(String.format(configureMethodIrq));
            
      VisitRegisters createFieldList = new VisitRegisters(peripheral) {
         
         final ArrayList<String> result = new ArrayList<String>();
         HashSet<String> usedFieldNames = new HashSet<String>();
         final int MAX_LIST_LENGTH = 40;
         final String memberDeclaration =
               " %s ";
         final String padding=
               "\n               ";
         
         @Override
         void visitor(Register register, String context) {

            if (register.getAccessType() == AccessType.ReadOnly) {
               return;
            }
            context = context+stripRegisterName(register.getName());
            String regName = register.getName().replace("%s", "");
            StringBuilder sb = new StringBuilder();
            int lineLength=0;
            for (Field field:register.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               if (!usedFieldNames.add(registerFieldName)) {
                  continue;
               }
               if (!sb.isEmpty()) {
                  sb.append(",");
                  lineLength++;
               }
               if (lineLength>MAX_LIST_LENGTH) {
                  lineLength = 0;
                  sb.append(padding);
               }
               sb.append(registerFieldName);
               lineLength += registerFieldName.length();
            }
            if (!sb.isEmpty()) {
               while(lineLength<(MAX_LIST_LENGTH+15)) {
                  sb.append(" ");
                  lineLength++;
               }
               sb.append(": %configRegAssignment");
               result.add(String.format(memberDeclaration, sb.toString()));
            }
         }

         @Override
         String[] getResult() {
            return result.toArray(new String[result.size()]);
         }
      };
      String configureMethodPreamble = ""
            + "   <for keys=\n"
            + "             \" var                                                    : statement            \"\n";
      
      String configureMethodPostamble = ""
            + "\" >\n"
            + "      <variableTemplate codeGenCondition=\"enablePeripheralSupport\"\n"
            + "      variables=\"%(var)\"\n"
            + "      linePadding=\"xxx\"\n"
            + "      ><![CDATA[\n"
            + "         \\t\n"
            + "         %multilineDescription\n"
            + "         \\t   %(statement);\\n\n"
            + "      ]]></variableTemplate>\n"
            + "   </for>\n"
            + "   <template codeGenCondition=\"/$(_STRUCTNAME)/generateSharedInfo\" >\n"
            + "   <![CDATA[\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n"
            + "";
      
      createFieldList.visit();
      resultSb.append(configureMethodPreamble);
      resultSb.append("      values=\" irqLevel                                               : enableNvicInterrupts(init.irqlevel)");
      for (String registerFieldList : (String[]) createFieldList.getResult()) {
         resultSb.append(" ;\n              ");
         resultSb.append(registerFieldList);
      }
      resultSb.append(configureMethodPostamble);
      
      /*
       * Create DefaultInitValue
       */
      String initValueTemplate = ""
            + "\n"
            + "   <!-- ____  Default Initialisation value ____ -->\n"
            + "\n"
            + "   <variableTemplate codeGenCondition=\"enablePeripheralSupport\"\n"
            + "      separator=\",\"\n"
            + "      terminator=\",\"\n"
            + "      variables=\"%s\n"
            + "            irqLevel\" >\n "
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Default initialisation value for $(_Class)\n"
            + "      \\t * This value is created from Configure.usbdmProject settings\n"
            + "      \\t */\n"
            + "      \\tstatic constexpr Init DefaultInitValue = {%%initExpression\n"
            + "      \\t};\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </variableTemplate>\n";
      
      VisitRegisters createInitValueFieldList = new VisitRegisters(peripheral) {
         
         final StringBuilder resultSb = new StringBuilder();
         HashSet<String> usedFieldNames = new HashSet<String>();
         final int MAX_LINE_LENGTH = 40;
         final String PADDING = "\n            ";
         
         @Override
         void visitor(Register reg, String context) {
            if (reg.getAccessType() == AccessType.ReadOnly) {
               return;
            }
            context = context+stripRegisterName(reg.getName());
            boolean firstInRegister = true;
            int lineLength=0;
            String regName = reg.getName().replace("%s", "");
            for (Field field:reg.getFields()) {
               if (field.getAccessType() == AccessType.ReadOnly) {
                  continue;
               }
               String registerFieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               if (!usedFieldNames.add(registerFieldName)) {
                  continue;
               }
               if (!resultSb.isEmpty()) {
                  resultSb.append(",");
                  lineLength++;
                  if (lineLength>MAX_LINE_LENGTH) {
                     resultSb.append(PADDING);
                     lineLength = 0;
                  }
               }
               if (firstInRegister) {
                  resultSb.append(PADDING);
                  firstInRegister = false;
               }
               resultSb.append(registerFieldName);
               lineLength += registerFieldName.length();
            }
         }

         @Override
         Object getResult() {
            return resultSb.toString();
         }
         
      };
      
      createInitValueFieldList.visit();
      
      resultSb.append(String.format(initValueTemplate, createInitValueFieldList.getResultAsString()+",", ""));
   
      String closeInitClass =
            "\n" +
            "   <template where=\"basicInfo\" codeGenCondition=\"/$(_STRUCTNAME)/generateSharedInfo\" >\n" +
            "   <![CDATA[\n" +
            "      \\t}; // class $(_Structname)BasicInfo::Init\n" +
            "      \\t\\n\n" +
            "   ]]>\n" +
            "   </template>\n";

      resultSb.append(closeInitClass);
     
   }
   
   void writeCommon() {
      String common =
            "\n" +
            "   <!-- ____ Common __________________ -->\n" +
            "\n" +
            "   <template key=\"/$(_BASENAME)/declarations\" codeGenCondition=\"enablePeripheralSupport\" >\n" +
            "   <![CDATA[\n" +
            "      \\t/**\n" +
            "      \\t * Class representing $(_NAME)\n" +
            "      \\t */\n" +
            "      \\tclass $(_Class) : public $(_Baseclass)Base_T<$(_Class)Info> {};\n" +
            "      \\t//typedef $(_Baseclass)Base_T<$(_Class)Info> $(_Class);\n" +
            "      \\t\\n\n" +
            "   ]]>\n" +
            "   </template>\n" +
            "\n" +
            "   <validate\n" +
            "      class=\"net.sourceforge.usbdm.deviceEditor.validators.PeripheralValidator\" >\n" +
            "   </validate>\n";
      resultSb.append(String.format(common));
      
      String fileInclude =
            "\n" +
                  "   <projectActionList id=\"%s_files\" >\n" +
                  "      <copy source=\"Project_Headers/%s.h\"  target=\"Project_Headers/%s.h\"  overwrite=\"true\"  derived=\"true\" />\n" +
                  "   </projectActionList>\n";
      String filename = peripheralBasename.toLowerCase();
      resultSb.append(String.format(fileInclude, filename, filename+fileSuffix.toUpperCase(), filename));

      resultSb.append(
            "\n" +
            "   <!-- ____ Startup __________________ -->\n" +
            "\n" +
            "   <template key=\"/SYSTEM/Includes\" condition=\"configurePeripheralInStartUp\" codeGenCondition=\"configurePeripheralInStartUp\" >\n" +
            "      <![CDATA[#include \"$(_basename).h\"\\n\n" +
            "   ]]></template>\n" +
            "\n" +
            "   <template key=\"/SYSTEM/Startup\" condition=\"configurePeripheralInStartUp\" codeGenCondition=\"configurePeripheralInStartUp\" >\n" +
            "   <![CDATA[\n" +
            "      \\t/*  Initialise $(_Class) */\n" +
            "      \\tUSBDM::$(_Class)::defaultConfigure();\\n\n" +
            "   ]]></template>\n");
      
      if (isSim) {
         resultSb.append("\n   <xi:include href=\"_simFiles-MKE.xml\" />");
         //         resultSb.append("\n   <xi:include href=\"_sim_commonTemplates.xml\" />\n");
      }
      
      resultSb.append(""
            + "\n"
            + "   <!-- ____ SIM configuration __________________ -->\n"
            + "\n"
            + "   <category name=\"Advanced\" description=\"SIM configuration\"\n"
            + "      toolTip=\"These settings only have effect if the SIM configuration is enabled\" >\n"
            + "      <title description=\"$(_BASENAME) Shared\" />\n"
            + "      <for keys=\"v\" values=\"=/SIM/$(_Baseclass)ExternalItems\" condition=\"/SIM/$(_Baseclass)ExternalItems\" >\n"
            + "         <aliasOption key=\"/SIM/%(v)\"           optional=\"true\" locked=\"false\" />\n"
            + "      </for>\n"
            + "      <title description=\"$(_NAME) Specific\" />\n"
            + "      <aliasOption key=\"=_scgc_clock\" locked=\"false\" condition=\"_scgc_clock\" />\n"
            + "      <for keys=\"v\" values=\"=/SIM/$(_Class)ExternalItems\" condition=\"/SIM/$(_Class)ExternalItems\" >\n"
            + "         <aliasOption key=\"/SIM/%(v)\"           optional=\"true\" locked=\"false\" />\n"
            + "      </for>\n"
            + "   </category>\n"
            + "");
      
      resultSb.append("\n   <!--  ____ Signal mapping __________________ -->\n"
            + "   <signals enabledBy=\"enablePeripheralSupport\" locked=\"!/PCR/_present\" />\n");
      resultSb.append("\n</peripheralPage>\n");
   }

   /******************************************************
    * Create IRQ related entries
    ******************************************************/
   private void writeHandlers() {
      
      final String irqHandlingOpeningText = "\n"
            + "   <!-- ____ Interrupt handling _____________ -->\n"
            + "\n"
            + "   <template codeGenCondition=\"irqHandlingMethod\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * $(_Class) interrupt handler\n"
            + "      \\t * Passes control to call-back function\n"
            + "      \\t */\n"
            + "      \\tstatic void irqHandler() {\n"
            + "      \\t\n"
            + "      \\t   // Execute call-back\n"
            + "      \\t   sCallback($(irq_call));\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n";
      
      //________________________________________________________________________________________________
      
      ArrayList<InterruptEntry> entries = peripheral.getInterruptEntries();
      String pName = peripheral.getName();
      if (entries == null) {
         return;
      }
      
      StringBuilder typeDefSb          = new StringBuilder();
      StringBuilder irqHandlerSb       = new StringBuilder();
      StringBuilder staticSb           = new StringBuilder();
//      StringBuilder irqEnumSb          = null;
//
//      if (entries.size()>1) {
//         irqEnumSb = new StringBuilder();
//      }
      
      irqHandlerSb.append(irqHandlingOpeningText);
      for (InterruptEntry entry:entries) {
//         String description      = entry.getCDescription();
         String irqVectorName    = entry.getName();
         
         if (irqVectorName.startsWith(pName+"_")) {
            irqVectorName = irqVectorName.substring(pName.length()+1);
         }
         else if (irqVectorName.startsWith(pName)) {
            irqVectorName = irqVectorName.substring(pName.length());
         }
         else if (irqVectorName.startsWith("DMA_")) {
            irqVectorName = irqVectorName.substring("DMA_".length());
         }
         else if (irqVectorName.startsWith("DMA")) {
            irqVectorName = irqVectorName.substring("DMA".length());
         }
         String irqHandlerName   = irqVectorName+"Handler";
         irqHandlerName = "irq"+irqHandlerName;
         String callbackType     = entry.getName()+"_CallbackFunction";
         if (callbackType.startsWith(pName+"_")) {
            callbackType = callbackType.substring(pName.length()+1);
         }
         callbackType = Character.toUpperCase(callbackType.charAt(0))+callbackType.substring(1);
//         String callbackName = Character.toLowerCase(callbackType.charAt(0))+callbackType.substring(1);
//         String callbackName = "sCallback";
         
      }
      resultSb.append(typeDefSb.toString());
      resultSb.append(irqHandlerSb.toString());
      resultSb.append(staticSb.toString());
      
//      irqEnum = irqEnumSb;
   }
   
   static String peripheralsToDo[] = {
//         "ACMP",
//         "ADC",
//         "CAN",
//         "CMP",
//         "CMT",
//         "CRC",
//         "DAC",
//         "DMA",
//         "DMAMUX",
//         "ENET",
//         "EWM",
//         "FTF",
//         "FTM",
//         "FMC",
//         "GPIO",
//         "FGPIO",
//         "ICS",
//         "IRQ",
//         "I2C",
//         "I2S",
//         "KBI",
//         "LPTMR",
//         "LPUART",
//         "LLWU",
//       "MCM",
       "MPU",
//         "OSC",
//         "PDB",
//         "PIT",
//         "PMC",
//         "PORT",
//         "PWT",
//         "QSPI"
//         "RCM",
//         "RNGA",
//         "RTC",
//         "SDHC",
//         "SDRAMC",
//         "SIM"
//         "SMC",
//         "SPI",
//         "SIM",
//         "TSI",
//         "TRNG",
//         "UART",
//         "VREF",
//         "USB",
//         "USBDCD",
//         "WDOG",
   };
   
   static boolean doThisPeripheral(String name) {
      for (String tname:peripheralsToDo) {
         if (name.startsWith(tname)) {
            return true;
         }
      }
      return false;
   }
   
   static void doAllPeripherals(String deviceName, String suffix) throws Exception {
      DevicePeripheralsFactory factory = new DevicePeripheralsFactory();
      DevicePeripherals peripherals = factory.getDevicePeripherals(deviceName);
      if (peripherals == null) {
         throw new Exception("Unable to get peripheral data for '"+deviceName+"'\n");
      }
      for (Peripheral peripheral:peripherals.getPeripherals()) {
         if (!doThisPeripheral(peripheral.getName())) {
            System.out.println("Skipping " + peripheral.getName());
            continue;
         }
         if (peripheral.getDerivedFrom() != null) {
            continue;
         }
         String name = peripheral.getSourceFilename();
         if (name == null) {
            name = peripheral.getName()+suffix;
         }
         System.err.println("Processing " + name);
         name = name+suffix;
         CreateDeviceSkeletonFromSVD instance = new CreateDeviceSkeletonFromSVD(suffix, peripherals, peripheral);
         String filename = peripheral.getSourceFilename();
         if (filename == null) {
            // Just create a stub
            instance.savePeripheralFiles(true);
         }
         else {
            //         instance.listFields();
            instance.writePreamble();
            instance.processRegisters();
            instance.writeSettersAndGetters();
            instance.writeInitClass();
            instance.writeCommon();
            instance.savePeripheralFiles(false);
         }
      }
   }
   
   public void listFields() {

      VisitRegisters createSimpleFieldList = new VisitRegisters(peripheral) {

         final StringBuilder resultSb = new StringBuilder();
         Boolean firstField = true;

         @Override
         void visitor(Register register, String context) {

//            if ((register.getDimension()>0)||(context.contains("[index]"))) {
//               return;
//            }
            context = context+stripRegisterName(register.getName());
            System.err.println("Context = '"+context+"'");
            String regName = stripRegisterName(register.getName());
            for (Field field:register.getFields()) {
               if (!firstField) {
                  resultSb.append("\n");
               }
               String fieldName  = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
               firstField = false;
               resultSb.append(String.format("%s", fieldName));
            }
         }
         @Override
         Object getResult() {
            return resultSb.toString()+"\n";
         }
      };

      createSimpleFieldList.visit();
      
      System.err.print(createSimpleFieldList.getResult());
   }
   
   public static void main(String[] args) throws Exception {
//      doAllPeripherals("STM32F030", "mke");
//      doAllPeripherals("FRDM_KE04Z", "mke");
//      doAllPeripherals("FRDM_KE06Z", "mke");
//    doAllPeripherals("FRDM_KL02Z");
//      doAllPeripherals("FRDM_KL03Z");
//    doAllPeripherals("FRDM_KL05Z");
//    doAllPeripherals("FRDM_KL25Z", "mkl");
//    doAllPeripherals("FRDM_KL27Z", "mkl");
//    doAllPeripherals("FRDM_K20D50M", "mk");
//    doAllPeripherals("FRDM_K22F", "mk");
    doAllPeripherals("FRDM_K28F", "mk");
//      doAllPeripherals("FRDM_K66F", "mk");
//      doAllPeripherals("FRDM_K64F", "mk");
//      doAllPeripherals("FRDM_K82F", "mk");
//    doAllPeripherals("FRDM_KW41Z", "mkw");
   }

}
