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
   
   // Generate constant to place information in header file rather than pin_mapping.h
   private static boolean placeInHeaderFile  = true;
   
   // Use static methods in Info rather than class methods in BasicInfo
   private static boolean useStaticMethods   = true;
   
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
    * @param text Text to process
    * 
    * @return Converted name
    */
   static public String makeNameFromText(String text) {
      text = text.substring(0, Math.min(40,text.length()));
      char[] p = text.toLowerCase().toCharArray();
      StringBuffer sb = new StringBuffer();

      // Delete spaces and Upper-case 1st character of each word
      boolean convertFlag = true;
      for (int index=0; index<p.length; index++) {
         char ch = p[index];
         if ((ch == '\n')||(ch == '.')||(ch == ';')||(ch == '\\')) {
            break;
         }
         if ((ch=='_')||(ch=='-')||(ch==' ')||(ch=='(')) {
            // Discard and upper-case next character
            convertFlag = true;
            continue;
         }
         if ((ch=='/')) {
            // Use '_' and upper-case next character
            sb.append("_");
            convertFlag = true;
            continue;
         }
         if (!(((ch>='0')&&(ch<='9'))||((ch>='a')&&(ch<='z'))||((ch>='A')&&(ch<='Z')))) {
            // Discard
            continue;
         }
         if (convertFlag) {
            sb.append(Character.toUpperCase(ch));
            convertFlag = false;
         }
         else {
            sb.append(ch);
         }
      }
      return sb.toString();
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
         char ch = p[index];
         if (ch=='_') {
            // Discard and upper-case next character
            convertFlag = true;
            continue;
         }
         if (!(((ch>='0')&&(ch<='9'))||((ch>='a')&&(ch<='z'))||((ch>='A')&&(ch<='Z')))) {
            // Discard
            continue;
         }
         if (p[index]=='_') {
            // Discard and upper-case next character
            convertFlag = true;
            continue;
         }
         if (convertFlag) {
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
   
   //____________________
   void writePreamble() {

      String classDecl1 =
            "\n"+
            "   <!-- ____ Class Declarations ________ -->\n" +
            "   <constant key=\"_basicInfo_declaration\" type=\"String\" value='\"$(_BasicInfo)\"' />\n";
      resultSb.append(classDecl1);
      String classDecl2 =
            "   <constant key=\"_class_declaration\" type=\"String\" value='\"$(_Info) : public $(_BasicInfo)\"' />\n";
      resultSb.append(classDecl2);
      
      if (placeInHeaderFile) {
         String defInHeader = "\n"
               + "   <constant key=\"definitionsInHeader\" value=\"true\" type=\"Boolean\" />\n";
         resultSb.append(defInHeader);
      }

      final String suppress = "\n"
         + "   <equation key=\"suppressInstance\"           value=\"false\"     />\n";
      final String pre = "\n"
         + "   <equation key=\"irq_parameters\"             value=\"\"          />\n"
         + "   <equation key=\"irq_dummy_parameters\"       value=\"\"          />\n"
         + "   <equation key=\"irq_call\"                   value=\"\"          />\n"
         + "   <equation key=\"generateDefault\"            value=\"false\"     />\n"
         + "   <equation key=\"configureInStartupDefault\"  value=\"false\"     />\n"
         + "   <xi:include href=\"enablePeripheral.xml\"    />\n"
         + "   <title />\n";

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
      int count = 0;
      for (Field field:reg.getFields()) {
         
         String fieldName = peripheralBasename.toLowerCase()+"_"+regName.toLowerCase()+"_"+field.getName().toLowerCase();
         String name = prettyName(peripheralBasename)+makeNameFromText(field.getDescription());
         
         if (usedFieldNames == null) {
            usedFieldNames = new HashSet<String>();
         }
         if (!usedFieldNames.add(name)) {
            // Repeated filed name - delete
            resultSb.append("<!-- Repeated field name '"+name+"' -->\n");
            name = name + count++;
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
         String enumName  = name;
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
   
   void writeGettersAndSetters() {
      resultSb.append(
            "\n   <!-- ____ Getters and Setters ____________ -->\n");
      
      if (fieldNameList.size() == 0) {
         // No fields!
         return;
      }
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
//               String methodName = regName.toLowerCase()+"_"+field.getName().toLowerCase();
               String methodName = makeNameFromText(field.getDescription());
               String get   = "true";
               String set   = "true";
               String clear = "false";
               if (field.getAccessType() == AccessType.ReadOnly) {
                  set = "false";
               }
               firstField = false;
               resultSb.append(String.format("         %-30s : %-5s : %-5s : %-5s : enableGettersAndSetters : %s",
                     fieldName, set, get, clear, methodName));
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
               resultSb.append(String.format("         %-30s : %-5s : %-5s : %-5s : enableGettersAndSetters : %-25s : %s",
                     fieldName, set, get, clear, context, prettyName(methodName)));
            }
         }
         @Override
         Object getResult() {
            return resultSb.toString();
         }
      };

      final String simpleVariableTemplate = ""
          + "      <variableTemplate %where condition=\"%(set)\" codeGenCondition=\"%(genCode)\"\n"
          + "         variables=\"%(field)\" >\n"
          + "      <![CDATA[\n"
          + "         \\t/**\n"
          + "         \\t * Set %description\n"
          + "         \\t * (%variables)\n"
          + "         \\t *\n"
          + "         %paramDescription\n"
          + "         \\t */\n"
          + "         \\t%static void set%(name)(%params) %const {\n"
          + "         \\t   %fieldAssignment;\n"
          + "         \\t}\n"
          + "         \\t\\n\n"
          + "      ]]>\n"
          + "      </variableTemplate>\n"
          + "      <variableTemplate %where condition=\"%(get)\" codeGenCondition=\"%(genCode)\"\n"
          + "         variables=\"%(field)\" >\n"
          + "      <![CDATA[\n"
          + "         \\t/**\n"
          + "         \\t * Get %description\n"
          + "         \\t * (%variables)\n"
          + "         \\t *\n"
          + "         \\t * @return %tooltip\n"
          + "         \\t */\n"
          + "         \\t%static %returnType get%(name)() %const {\n"
          + "         \\t   return %fieldExtract;\n"
          + "         \\t}\n"
          + "         \\t\\n\n"
          + "      ]]>\n"
          + "      </variableTemplate>\n"
          + "      <variableTemplate %where condition='=\"(%variables)\"==\"%(clear)\"' codeGenCondition=\"%(genCode)\"\n"
          + "         variables=\"%(field)\"\n"
          + "         tooltipPadding=\"x*x\" >\n"
          + "      <![CDATA[\n"
          + "         \\t/**\n"
          + "         \\t * Clear %description\n"
          + "         \\t * (%variables)\n"
          + "         \\t *\n"
          + "         %paramDescription\n"
          + "         \\t */\n"
          + "         \\t%static void clear%(name)(%params) %const {\n"
          + "         \\t   // w1c and mixed register\n"
          + "         \\t   %register = %register|%paramExpression;\n"
          + "         \\t}\n"
          + "         \\t\\n\n"
          + "      ]]>\n"
          + "      </variableTemplate>\n"
          + "      <variableTemplate %where condition='=\"w1cIm\"==\"%(clear)\"' codeGenCondition=\"%(genCode)\"\n"
          + "         variables=\"%(field)\"\n"
          + "         tooltipPadding=\"x*x\" >\n"
          + "      <![CDATA[\n"
          + "         \\t/**\n"
          + "         \\t * Clear %description\n"
          + "         \\t * (%variables)\n"
          + "         \\t *\n"
          + "         \\t * %tooltip\n"
          + "         \\t */\n"
          + "         \\t%static void clear%(name)() %const {\n"
          + "         \\t   // w1c and mixed register\n"
          + "         \\t   %register = %register|%mask;\n"
          + "         \\t}\n"
          + "         \\t\\n\n"
          + "      ]]>\n"
          + "      </variableTemplate>\n";
      
      createSimpleFieldList.visit();
      String simpleResult = createSimpleFieldList.getResultAsString();
      if (!simpleResult.isBlank()) {
         resultSb.append(
               "\n" +
               "   <for keys=\"field                     : set   : get   : clear : genCode                 : name\"\n" +
               "        values=\"\n" + simpleResult + "\" >\n");
         String svt = patchInfoTemplate(simpleVariableTemplate);
         resultSb.append(svt);
         resultSb.append("   </for>\n");
      }
      
      final String indexedVariableTemplate =
            "      <variableTemplate %where condition=\"%(set)\" codeGenCondition=\"%(genCode)\" context=\"%(context)\"\n" +
            "         variables=\"%(field)\"\n" +
            "         nonDefaultParams=\"2\" >\n" +
            "      <![CDATA[\n" +
            "         \\t/**\n" +
            "         \\t * Set %description1\n" +
            "         \\t * (%variables)\n" +
            "         \\t *\n" +
            "         %paramDescription\n" +
            "         \\t */\n" +
            "         \\t%static void set%(name)(%params) %const {\n" +
            "         \\t   %fieldAssignment1;\n" +
            "         \\t}\n" +
            "         \\t\\n\n" +
            "      ]]>\n" +
            "      </variableTemplate>\n"+
            "      <variableTemplate %where condition=\"%(get)\" codeGenCondition=\"%(genCode)\" context=\"%(context)\"\n" +
            "         variables=\"%(field)\"\n" +
            "         nonDefaultParams=\"2\" >\n" +
            "      <![CDATA[\n" +
            "         \\t/**\n" +
            "         \\t * Get %description1\n" +
            "         \\t * (%variables)\n" +
            "         \\t *\n" +
            "         %paramDescription0\n" +
            "         \\t *\n" +
            "         \\t * @return %tooltip1\n" +
            "         \\t */\n" +
            "         \\t%static %paramType1 get%(name)(%param0) %const {\n" +
            "         \\t   return %fieldExtract1;\n" +
            "         \\t}\n" +
            "         \\t\\n\n" +
            "      ]]>\n" +
            "      </variableTemplate>\n"+
            "      <variableTemplate %where condition=\"%(clear)\" codeGenCondition=\"%(genCode)\" context=\"%(context)\"\n" +
            "         variables=\"%(field)\"\n" +
            "         tooltipPadding=\"x*x\" >\n" +
            "      <![CDATA[\n" +
            "         \\t/**\n" +
            "         \\t * Clear %description1\n" +
            "         \\t * (%variables)\n" +
            "         \\t *\n" +
            "         \\t * %tooltip\n" +
            "         \\t *\n" +
            "         %paramDescription0\n" +
            "         \\t */\n" +
            "         \\t%static void clear%(name)(%param0) %const {\n" +
            "         \\t   %register1 = %register1|%mask1;\n" +
            "         \\t}\n" +
            "         \\t\\n\n" +
            "      ]]>\n" +
            "      </variableTemplate>\n";
        
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
         String ivt = null;
         ivt = patchInfoTemplate(indexedVariableTemplate);
         resultSb.append(ivt);
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
   
   //________________
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
   
   String patchInfoTemplate(String template) {
      
      // For code placed in Info i.e. static functions
      String where        = "where=\"info\" ";
      String condition    = "codeGenCondition=\"$(_InfoGuard)\" ";
      String irqCondition = "codeGenCondition=\"$(_InfoIrqGuard)\" ";
      String Const        = "";
      String Static       = "static ";

      if (!useStaticMethods) {
         // For code in BasicInfo i.e. const functions
         where        = "where=\"basicInfo\" ";
         condition    = "codeGenCondition=\"$(_BasicInfoGuard)\" ";
         irqCondition = "codeGenCondition=\"$(_BasicInfoIrqGuard)\" ";
         Const        = "const ";
         Static       = "";
      }
      String t = template.replace("%where ", where);
      t = t.replace("%condition ",     condition);
      t = t.replace("%irqCondition ",  irqCondition);
      t = t.replace("%const ",         Const);
      t = t.replace("%static ",        Static);
      return t;
   }
   
   void writeBanner(StringBuilder sb, String name) {
      String banner = ""
            + "<!-- ====================================================================================================== -->\n"
            + "<!--     %name      -->\n"
            + "<!-- ====================================================================================================== -->\n";
      String text = banner.replace("%name", name);
      sb.append(text);
   }
   
   enum Where {info, InfoIrq, basicInfo, basicInfoIrq, commonInfo};
   
   String makeTemplateBody(int padding, String body) {
      String paddingText = String.format("%"+padding+"s", " ")+"\\t";
      body = paddingText+body.replace("\n", "\n"+paddingText);
      return body;
   }
   
   void writeTemplate(StringBuilder sb, Where where, String body) {
      String location="";
      String guard = "";
      
      switch(where) {
      case commonInfo:
         location = "commonInfo";
         guard    = "$(_CommonInfoGuard)";
         break;
      case basicInfo:
         location = "basicInfo";
         guard    = "$(_BasicInfoGuard)";
         break;
      case basicInfoIrq:
         location = "basicInfo";
         guard    = "$(_BasicInfoIrqGuard)";
         break;
      case info:
         location = "info";
         guard    = "$(_InfoGuard)";
         break;
      case InfoIrq:
         location = "info";
         guard    = "$(_InfoIrqGuard)";
         break;
      }
      
      String template = "\n"
            + "   <template where=\"%location\" codeGenCondition=\"%guard\" >\n"
            + "   <![CDATA[\n"
            + "%body\n"
            + "   ]]>\n"
            + "   </template>\n"
            + "\n";
      String text = template;
      text = text.replace("%location", location);
      text = text.replace("%guard",    guard);
      text = text.replace("%body",     body);
      sb.append(text);

   }
   
   void writeConstructor() {
      
      writeBanner(resultSb, "START BasicInfo Class");
      
      String input_clock = "\n"
            + "   <!-- BasicInfo Clock methods  -->\n"
            + "   <!-- Individual clock selection -->\n"
            + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" condition=\"=individual_clock_source\">\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Get input clock frequency\n"
            + "      \\t * (Individual to each peripheral)\n"
            + "      \\t *\n"
            + "      \\t * @return Input clock frequency as a uint32_t in Hz\n"
            + "      \\t */\n"
            + "      \\tuint32_t (*getInputClockFrequency)();\n"
            + "      \\t\\n\n"
            + "   ]]></template>\n"
            + "\n"
            + "   <!-- Shared clock selection -->\n"
            + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" condition=\"=shared_clock_source\">\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Get input clock frequency\n"
            + "      \\t * (Shared by all peripherals)\n"
            + "      \\t *\n"
            + "      \\t * @return Input clock frequency as a uint32_t in Hz\n"
            + "      \\t */\n"
            + "      \\tstatic inline uint32_t getInputClockFrequency() {\n"
            + "      \\t   return SimInfo::get$(_Baseclass)Clock();\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]></template>\n"
            + "";
      
      String constructors = "\n"
            + "   <!-- BasicInfo Instance pointer and Constructors  -->\n"
            + "\n"
            + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" >\n"
            + "   <![CDATA[\n"
            + "      \\t// Pointer to $(_BASENAME) hardware instance\n"
            + "      \\tvolatile $(_Type) * const $(_basename);\n"
            + "      \\t\\n\n"
            + "   ]]></template>\n"
            + "\n"
            + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" condition=\"=individual_clock_source\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Constructor\n"
            + "      \\t *\n"
            + "      \\t * @param $(_basename) $(_BASENAME) hardware instance\n"
            + "      \\t */\n"
            + "      \\t$(_BasicInfo)(volatile $(_Type) * $(_basename), uint32_t (*getInputClockFrequency)()) :\n"
            + "      \\t               getInputClockFrequency(getInputClockFrequency), $(_basename)($(_basename)) {\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]></template>\n"
            + "\n"
            + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" condition=\"=shared_clock_source\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Constructor\n"
            + "      \\t *\n"
            + "      \\t * @param $(_basename) $(_BASENAME) hardware instance\n"
            + "      \\t */\n"
            + "      \\tconstexpr $(_BasicInfo)(volatile $(_Type) * $(_basename)) : $(_basename)($(_basename)) {\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]></template>\n";
      if (!useStaticMethods) {
         resultSb.append(constructors);
         resultSb.append(input_clock);
      }
   }
   
   void writeInitClass() {
      
      writeHandlers();
      
      writeBanner(resultSb, "START BasicInfo::Init class");
      
      final String open_init_class =""
         + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" >\n"
         + "   <![CDATA[\n"
         + "      \\t/**\n"
         + "      \\t * Class used to do initialisation of the $(_Baseclass)\n"
         + "      \\t *\n"
         + "      \\t * This class has a templated constructor that accepts various values.\n"
         + "      \\t * Parameters available may vary with device - see $(_Class)::DefaultInitValue for relevant example.\n"
         + "      \\t * Omitted parameters default to zero (disabled) or unchanged if initialiser is provided as last parameter.\n"
         + "      \\t *\n"
         + "      \\t * @note This constructor may be used to create a const instance in Flash\n"
         + "      \\t *\n"
         + "      \\t * Example:\n"
         + "      \\t * @code\n"
         + "      \\t * static const $(_Class)::Init $(_name)Init {\n"
         + "      \\t *\n"
         + "      \\t *   // Setup values\n"
         + "XXXXXXXXXXXXXXXXXXXXXX\n"
         + "      \\t *\n"
         + "      \\t *   // Optional base value to start with (must be last parameter)\n"
         + "      \\t *   $(_Class)::DefaultInitValue   // Used as base value modified by above\n"
         + "      \\t * };\n"
         + "      \\t *\n"
         + "      \\t * // Initialise $(_Class) from values specified above\n"
         + "      \\t * $(_Class)::configure($(_name)Init)\n"
         + "      \\t * @endcode\n"
         + "      \\t */\n"
         + "      \\tclass Init {\n"
         + "      \\t\n"
         + "      \\tprivate:\n"
         + "      \\t   /**\n"
         + "      \\t    * Prevent implicit parameter conversions\n"
         + "      \\t    */\n"
         + "      \\t   template <typename... Types>\n"
         + "      \\t   constexpr Init(Types...) = delete;\n"
         + "      \\t\n"
         + "      \\tpublic:\n"
         + "      \\t   /**\n"
         + "      \\t    * Copy Constructor\n"
         + "      \\t    */\n"
         + "      \\t   constexpr Init(const Init &other) = default;\n"
         + "      \\t\n"
         + "      \\t   /**\n"
         + "      \\t    * Default Constructor\n"
         + "      \\t    */\n"
         + "      \\t   constexpr Init() = default;\n"
         + "      \\t\\n\n"
         + "   ]]>\n"
         + "   </template>\n";
      resultSb.append(String.format(open_init_class, peripheralBasename));
      
      resultSb.append("\n   <!-- ____ BasicInfo::Init class Member variables ________ -->\n");
      
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
            + "      <variableTemplate where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\"\n"
            + "         variables=\"%(variables)\"\n"
            + "         linePadding=\"xxx\" >\n"
            + "      <![CDATA[\n"
            + "         %multilineDescription\n"
            + "         \\t   %(type) %registerName = %(init);\\n\\n\n"
            + "      ]]>\n"
            + "      </variableTemplate>\n"
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
      resultSb.append("      \"");
//    resultSb.append("      ;\n               %paramType  : /PCR/nvic_irqLevel                                   :   %defaultValue   \"");
      resultSb.append(memberDeclaration);

      /*
       *   Create Irq Constructors
       */
      String constructorTitle =""
          + "\n"
          + "   <!-- ____ BasicInfo::Init class Constructors ____________ -->\n";
      
      resultSb.append(constructorTitle);

       /*
       *   Create Constructors
       */
      String constructorListTemplateForEnumeratedFields = ""
          + "\n"
          + "   <for keys=\"r\"\n"
          + "      values=\"/PCR/nvic_irqLevel;"
          + "%s\n"
          + "            \" >\n"
          + "      <variableTemplate where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\"\n"
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
      
      String closeInitClass =
            "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" >\n" +
            "   <![CDATA[\n" +
            "      \\t}; // class $(_BasicInfo)::Init\n" +
            "      \\t\\n\n" +
            "   ]]>\n" +
            "   </template>\n\n";

      resultSb.append(closeInitClass);
     
      writeBanner(resultSb, "END BasicInfo::Init class");
      
      writeBanner(resultSb, "START BasicInfo::InitVectors class");
      
//      final String initVectors_class =""
//            + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoIrqGuard)\" >\n"
//            + "   <![CDATA[\n"
//            + "      \\t/**\n"
//            + "      \\t * Class used to do initialisation of the $(_Baseclass)\n"
//            + "      \\t *\n"
//            + "      \\t * This class has a templated constructor that accepts various values.\n"
//            + "      \\t * Parameters available may vary with device - see $(_Class)::DefaultInitValue for relevant example.\n"
//            + "      \\t * Omitted parameters default to zero (disabled) or unchanged if initialiser is provided as last parameter.\n"
//            + "      \\t *\n"
//            + "      \\t * @note This constructor may be used to create a const instance in Flash\n"
//            + "      \\t *\n"
//            + "      \\t */\n"
//            + "      \\ttemplate <typename EnumType, size_t NumVectors>\n"
//            + "      \\tclass InitVectors : public Init {\n"
//            + "      \\t\n"
//            + "      \\tpublic:\n"
//            + "      \\t   /**\n"
//            + "      \\t    * Copy Constructor\n"
//            + "      \\t    */\n"
//            + "      \\t   constexpr InitVectors(const InitVectors &other) = default;\n"
//            + "      \\t\n"
//            + "      \\t   /**\n"
//            + "      \\t    * Other constructors are inherited\n"
//            + "      \\t    *\n"
//            + "      \\t    * @tparam Types\n"
//            + "      \\t    * @param rest\n"
//            + "      \\t    */\n"
//            + "      \\t   template <typename... Types>\n"
//            + "      \\t   constexpr InitVectors(Types... rest) : Init(rest...) {\n"
//            + "      \\t   }\n"
//            + "      \\t\n"
//            + "      \\t   /**\n"
//            + "      \\t    * Initialise Interrupt callbacks\n"
//            + "      \\t    *\n"
//            + "      \\t    * @tparam Types\n"
//            + "      \\t    * @param rest         Remaining parameters\n"
//            + "      \\t\n"
//            + "      \\t    * @param irqNum       Interrupt number\n"
//            + "      \\t    * @param nvicPriority Priority for the handler\n"
//            + "      \\t    * @param callback     Callback function to use\n"
//            + "      \\t    */\n"
//            + "      \\t   template <typename... Types>\n"
//            + "      \\t   constexpr InitVectors(EnumType irqNum, NvicPriority nvicPriority, CallbackFunction callback, Types... rest) : InitVectors(rest...) {\n"
//            + "      \\t      priorities[irqNum]  = nvicPriority;\n"
//            + "      \\t      callbacks[irqNum]   = callback;\n"
//            + "      \\t   }\n"
//            + "      \\t\n"
//            + "      \\t   /**\n"
//            + "      \\t    * Information describing the priority and callback function for each interrupt\n"
//            + "      \\t    */\n"
//            + "      \\t    CallbackFunction callbacks[NumVectors]  = {};\n"
//            + "      \\t    NvicPriority     priorities[NumVectors] = {};\n"
//            + "      \\t\n"
//            + "      \\t}; // class $(_BasicInfo)::InitVectors\n"
//            + "      \\t\\n\n"
//            + "   ]]>\n"
//            + "   </template>\n"
//            + "\n";
      
//      resultSb.append(String.format(initVectors_class, peripheralBasename));

      writeBanner(resultSb, "END BasicInfo::InitVectors class");
      
      /*
       * Create configure methods
       */
      
      String clockConfigAndReopen = ""
            + "\n"
            + "   <!-- ____ BasicInfo class Configure methods ____ -->\n"
            + "\n"
            + "   <variableTemplate where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\"\n"
            + "      variables=\"uart_baudrate\"\n"
            + "      linePadding=\"xxx\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Configure $(_BASENAME) from values specified in init\n"
            + "      \\t * This routine does not configure pins or interrupt handlers\n"
            + "      \\t *\n"
            + "      \\t * @param $(_basename)           Hardware instance pointer\n"
            + "      \\t * @param clockFrequency Clock frequency\n"
            + "      \\t * @param init Class containing initialisation values\n"
            + "      \\t */\n"
            + "      \\tstatic void configure(\n"
            + "      \\t               volatile $(_Type) *$(_basename),\n"
            + "      \\t               uint32_t         clockFrequency,\n"
            + "      \\t               const Init    &init) {\n"
            + "      \\t\n"
            + "      \\t   setBaudRate($(_basename), clockFrequency, init.%registerName0);\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </variableTemplate>\n"
            + "";
           
      if (!useStaticMethods) {
         resultSb.append(clockConfigAndReopen);
      }
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
            + "             \" var                                                    : statement            \"\n"
            + "      values=\"";
      
      String configureMethodPostamble = ""
            + "\" >\n"
            + "      <variableTemplate %where %condition \n"
            + "         variables=\"%(var)\"\n"
            + "         linePadding=\"xxx\" >\n"
            + "      <![CDATA[\n"
            + "         \\t\n"
            + "         %multilineDescription\n"
            + "         \\t   %(statement);\\n\n"
            + "      ]]>\n"
            + "      </variableTemplate>\n"
            + "   </for>\n"
            + "   <template %where %condition >\n"
            + "   <![CDATA[\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n"
            + "";
      
      createFieldList.visit();
      resultSb.append(configureMethodPreamble);
//      resultSb.append("      values=\" irqLevel                                               : enableNvicInterrupts(init.irqlevel)");
      boolean isFirst = true;
      for (String registerFieldList : (String[]) createFieldList.getResult()) {
         if (!isFirst) {
            resultSb.append(" ;\n              ");
         }
         isFirst = false;
         resultSb.append(registerFieldList);
      }
      resultSb.append(patchInfoTemplate(configureMethodPostamble));
      
      String instance_configue = "\n"
            + "   <template where=\"basicInfo\" codeGenCondition=\"$(_BasicInfoGuard)\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Configure $(_BASENAME) from values specified in init\n"
            + "      \\t * This routine does not configure pins or interrupt handlers\n"
            + "      \\t *\n"
            + "      \\t * @param init Class containing initialisation values\n"
            + "      \\t */\n"
            + "      \\tvoid configure(const Init &init) const {\n"
            + "      \\t\n"
            + "      \\t   configure($(_basename), getInputClockFrequency(), init);\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n"
            + "";
      resultSb.append(instance_configue);
      
      writeBanner(resultSb, "END BasicInfo class");
      
      writeBanner(resultSb, "START Info class");
      
      if (!useStaticMethods) {
         String infoClassConstructor = ""
               + "   <!-- ____ Info constructors ________ -->\n"
               + "\n"
               + "   <template where=\"info\" codeGenCondition=\"$(_InfoGuard)\" >\n"
               + "   <![CDATA[\n"
               + "      \\t/*\n"
               + "      \\t *   Default Constructor\n"
               + "      \\t */\n"
               + "      \\t$(_Info)() : $(_BasicInfo)($(_basename), SimInfo::get$(_Class)Clock) {\n"
               + "      \\t   defaultConfigure();\n"
               + "      \\t}\n"
               + "      \\t\n"
               + "      \\t/*\n"
               + "      \\t *   Constructor\n"
               + "      \\t */\n"
               + "      \\t$(_Info)(const Init &init) : $(_BasicInfo)($(_basename), SimInfo::get$(_Class)Clock) {\n"
               + "      \\t   configure(init);\n"
               + "      \\t}\n"
               + "      \\t\\n\n"
               + "   ]]>\n"
               + "   </template>\n";

         resultSb.append(infoClassConstructor.replace("%Info", prettyName(peripheral.getName())+"Info"));
      }
      
      /*
       * Init configure methods
       */
      resultSb.append("\n   <!-- Init configure methods -->\n");
      
      String init_configure_template = "   \n"
            + "   <template where=\"info\" codeGenCondition=\"$(_InfoGuard)\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Configure with default settings.\n"
            + "      \\t * Configuration determined from Configure.usbdmProject\n"
            + "      \\t */\n"
            + "      \\tstatic void defaultConfigure() {\n"
            + "      \\t\n"
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
            + "   </template>\n"
            + "   <template where=\"info\" codeGenCondition=\"$(_InfoIrqGuard)\" condition=\"=(_hardwareIrqCount==1)\" >\n"
            + "   <![CDATA[\n"
            + "      \\t   // Configure call-back\n"
            + "      \\t   if (init.callbacks[0] != nullptr) {\n"
            + "      \\t      setCallback(init.callbacks[0]);\n"
            + "      \\t      enableNvicInterrupts(init.priorities[0]);\n"
            + "      \\t   }\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n"
            + "   <variableTemplate where=\"info\" codeGenCondition=\"$(_InfoIrqGuard)\" condition=\"=(_hardwareIrqCount>1)\"\n"
            + "      variables=\"irq_enum\"\n"
            + "      immediateVariables=\"_hardwareIrqCount\">\n"
            + "   <![CDATA[\n"
            + "      \\t   for(int index=0; index<$(_hardwareIrqCount); index++) {\n"
            + "      \\t      if (init.callbacks[index] != nullptr) {\n"
            + "      \\t         // Configure call-back\n"
            + "      \\t         setCallback(%returnType0(index), init.callbacks[index]);\n"
            + "      \\t         enableNvicInterrupts(%returnType0(index), init.priorities[index]);\n"
            + "      \\t      }\n"
            + "      \\t   }\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </variableTemplate>\n"
            + "   <template where=\"info\" codeGenCondition=\"$(_InfoGuard)\" condition=\"=individual_clock_source\" >\n"
            + "   <![CDATA[\n"
            + "      \\t   $(_BasicInfo)::configure($(_basename), SimInfo::get$(_Class)Clock(), init);\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n"
            + "   <template where=\"info\" codeGenCondition=\"$(_InfoGuard)\" condition=\"=shared_clock_source\" >\n"
            + "   <![CDATA[\n"
            + "      \\t   $(_BasicInfo)::configure($(_basename), getInputClockFrequency(), init);\n"
            + "      \\t}\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n";
      
      resultSb.append(init_configure_template);
      
      /*
       * Create DefaultInitValue
       */
      String initValueTemplate1 = "\n"
            + "   <!-- ____  Default Initialisation value ____ -->\n"
            + "\n"
            + "   <template where=\"info\" codeGenCondition=\"$(_InfoGuard)\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Default initialisation value for $(_Class)\n"
            + "      \\t * This value is created from Configure.usbdmProject settings\n"
            + "      \\t */\n"
            + "      \\tstatic constexpr $(_BasicInfo)::Init DefaultInitValue = {\\n\n"
            + "   ]]>\n"
            + "   <!-- Interrupt information -->\n"
            + "   </template>\n"
            + "<!--\n"
            + "   <for keys=\"irqName\" values=\"=_hardwareIrqNums\">\n"
            + "      <equation key=\"irqEnum\" value='=ReplaceAll(\"%(irqName)\",\"^(.*?)_(.*)_IRQn$\",\"IrqNum_$2\")' />\n"
            + "      <equation key=\"var\"     value='=ReplaceAll(\"%(irqName)\",\"^(.*?)_(.*)_IRQn$\",\"irqLevel_$2\")' />\n"
            + "      <print text=\"(irqName) = %(irqName)\"/>\n"
            + "      <printVar key=\"irqEnum\" />\n"
            + "      <printVar key=\"var\" />\n"
            + "      <variableTemplate where=\"info\" codeGenCondition='$(_InfoIrqGuard)&amp;&amp;!IsZero(@var)'\n"
            + "         variables='=var'\n"
            + "         separator=\",\"\n"
            + "         terminator=\",\"\n"
            + "         padToComments=\"40\"\n"
            + "         immediateVariables=\"irqEnum\" >\n"
            + "      <![CDATA[\n"
            + "         \\t   $(irqEnum), %initExpression,\n"
            + "         \\t   unhandledCallback,\\n\n"
            + "      ]]>\n"
            + "      </variableTemplate>\n"
            + "   </for>\n"
            + "-->\n";
      resultSb.append(initValueTemplate1);

      /*
       * Field values for init
       */
      String initValueTemplate = ""
            + "   <variableTemplate where=\"info\" codeGenCondition=\"$(_InfoGuard)\"\n"
            + "      separator=\",\"\n"
            + "      terminator=\",\"\n"
            + "      padToComments=\"40\"\n"
            + "      variables=\"%s\n"
            + "            \" >\n"
            + "   <![CDATA[\n"
            + "      \\t%%initNonZeroValues\n"
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
      
      writeBanner(resultSb, "END Info class");
   }
   
   void writeCommon() {
      writeBanner(resultSb, "Common");
      
      String declaration ="\n"
            + "   <template key=\"/$(_BASENAME)/declarations\" codeGenCondition=\"$(_InfoGuard)\" >\n"
            + "   <![CDATA[\n"
            + "      \\t/**\n"
            + "      \\t * Class representing $(_NAME)\n"
            + "      \\t */\n"
            + "      \\t//using $(_Class) = $(_Class)Info;\n"
            + "      \\t//class $(_Class) : public $(_Baseclass)Base_T<$(_Info)> {};\n"
            + "      \\t//typedef $(_Baseclass)Base_T<$(_Info)> $(_Class);\n"
            + "      \\t\\n\n"
            + "   ]]>\n"
            + "   </template>\n";
      
      resultSb.append(String.format(declaration));
      
      String validator ="\n"
            + "   <validate\n"
            + "      class=\"net.sourceforge.usbdm.deviceEditor.validators.PeripheralValidator\" >\n"
            + "   </validate>\n";
      
      resultSb.append(String.format(validator));
      
      String fileInclude =
            "\n" +
                  "   <projectActionList id=\"%s_files\" >\n" +
                  "      <copy source=\"Project_Headers/%s.h\"  target=\"Project_Headers/%s.h\"  overwrite=\"true\"  derived=\"true\" />\n" +
                  "   </projectActionList>\n";
      String filename = peripheralBasename.toLowerCase();
      resultSb.append(String.format(fileInclude, filename, filename+fileSuffix.toUpperCase(), filename));

      resultSb.append(
            "\n" +
            "   <!-- ____ Startup ____ -->\n" +
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
      }
      
      resultSb.append(""
            + "\n"
            + "   <!-- ____ SIM configuration ____ -->\n"
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
      
      resultSb.append("\n   <!--  ____ Signal mapping ____ -->\n"
            + "   <signals enabledBy=\"enablePeripheralSupport\" locked=\"!/PCR/_present\" />\n");
      resultSb.append("\n</peripheralPage>\n");
   }

   /******************************************************
    * Create IRQ related entries
    ******************************************************/
   private void writeHandlers() {
      
      final String irqHandlingOpeningText = "\n"
            + "   <!-- ____ Interrupt handling (only needed when not done in enablePeripheral.xml) _____________ -->\n"
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
      
      //__________________________
      
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
         "ADC",
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
//         "MCM",
//         "MPU",
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
            instance.writeConstructor();
            instance.writeGettersAndSetters();
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
      useStaticMethods=false;
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
