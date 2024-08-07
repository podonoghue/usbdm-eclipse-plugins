package net.sourceforge.usbdm.configEditor.parser;
import java.io.BufferedWriter;
import java.io.IOException;

import net.sourceforge.usbdm.configEditor.information.PeripheralTemplateInformation;

public class DocumentUtilities {
   
   /**
    * Write open group comment
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <i><b>groupName groupTitle</i></b>                   
    *  * @brief       <i><b>groupBrief</i></b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param writer     Where to write 
    * @param template   Template used to describe the group
    * 
    * @throws IOException
    */
   public static void writeStartGroup(BufferedWriter writer, PeripheralTemplateInformation template) throws IOException {
      writeStartGroup(writer, template.getGroupName(), template.getGroupTitle(), template.getGroupBriefDescription());
   }

   /**
    * Conditionally write open group comment
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <i><b>groupName groupTitle</i></b>                   
    *  * @brief       <i><b>groupBrief</i></b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param writer     Where to write 
    * @param template   Template used to describe the group
    * @param groupDone  Indicates if group has already been done
    * 
    * @throws IOException
    */
   void conditionallyWriteGroup(BufferedWriter gpioHeaderFile, PeripheralTemplateInformation pinTemplate, boolean groupDone) throws IOException {
      if (groupDone) {
         return;
      }
      writeStartGroup(gpioHeaderFile, pinTemplate);
   }
   /**
    * Write open group comment
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <i><b>groupName groupTitle</i></b>                   
    *  * @brief       <i><b>groupBrief</i></b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param writer        Where to write 
    * @param groupName     Name of group
    * @param groupTitle    Title of group (may be null)
    * @param groupBrief    Brief description of group (may be null)
    * 
    * @throws IOException
    */
   public static void writeStartGroup(BufferedWriter writer, String groupName, String groupTitle, String groupBrief) throws IOException {
      final String startGroup1 = 
            "/**\n"+
            " * @addtogroup %s %s\n";
      final String startGroup2 = 
            " * @brief %s\n";
      final String startGroup3 = 
            " * @{\n"+
            " */\n";
      writer.write(String.format(startGroup1, groupName, (groupTitle==null)?"":groupTitle));
      if (groupBrief != null) {
         writer.write(String.format(startGroup2, groupBrief));
      }
      writer.write(String.format(startGroup3));
   }   
   /**
    * Write close group comment 
    * <pre><code>
    * /**                                                                    
    *  * @}                                                                   
    *  ** <i><b>groupName</i></b> **&#47;</code></pre>
    * 
    * @param writer        Where to write 
    * @param groupName     Name of group
    * 
    * @throws IOException
    */
   public static void writeCloseGroup(BufferedWriter writer, String groupName) throws IOException {
      final String endGroup = 
            "/**\n"+
            " * @}\n"+
            " ** %s\n */\n";
      writer.write(String.format(endGroup, groupName));
   }
   /**
    * Write close group comment 
    * <pre><code>
    * /**                                                                    
    *  * @}                                                                   
    *  *&#47;</code></pre>
    * 
    * @param writer        Where to write 
    * 
    * @throws IOException
    */
   public static void writeCloseGroup(BufferedWriter writer) throws IOException {
      final String endGroup = 
            "/**\n"+
            " * @}\n"+
            " */\n";
      writer.write(endGroup);
   }

   static final String HEADER_FILE_PREFIX = "PROJECT_HEADERS_";
   /**
    * Write header file preamble e.g.
    * <pre><code>
    *  /**
    *   * @file    <i><b>fileName</i></b> (derived from <i><b>trueFileName</i></b>)
    *   * @version <i><b>version</i></b>
    *   * @brief   <i><b>description</i></b> 
    *   *&#47;
    * #ifndef <i><b>FILENAME</i></b>
    * #define <i><b>FILENAME</i></b>
    * </code></pre>
    *
    * @param writer        Where to write
    * @param fileName      Filename to use in header block
    * @param trueFileName  Filename of actual file being written
    * @param version       Version to use in header block
    * @param description   Description to use in header block
    * 
    * @throws IOException
    */
   public static void writeHeaderFilePreamble(BufferedWriter writer, String fileName, String trueFileName, String version, String description) throws IOException {
      final String headerfilePreambleTemplate = 
            "/**\n"+
            " * @file      %s %s\n"+
            " * @version   %s\n"+
            " * @brief     %s\n"+ 
            " */\n"+
            "\n"+
            "#ifndef %s\n"+
            "#define %s\n"+
            "\n";
      String macroName = HEADER_FILE_PREFIX+fileName.toUpperCase().replaceAll("(\\.|-)", "_");
      if ((trueFileName == null) || trueFileName.isEmpty()) {
         trueFileName = "";
      }
      else {
         trueFileName = "(derived from "+trueFileName+")";
      }
      description = description.replaceAll("\n", "\n *            ");
      writer.write( String.format(headerfilePreambleTemplate, 
            fileName, trueFileName, version, description, macroName, macroName ));
   }

   /**
    * Write source file preamble e.g.
    * <pre><code>
    *  /**
    *   * @file    <i><b>fileName</i></b> (derived from <i><b>trueFileName</i></b>)
    *   * @brief   <i><b>description</i></b> 
    *   *&#47;
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param fileName      Filename to use in header block
    * @param trueFileName  Filename of actual file being written
    * @param description   Description to use in header block
    * 
    * @throws IOException
    */
   public static void writeCppFilePreamble(BufferedWriter writer, String fileName, String trueFileName, String version, String description) throws IOException {
      final String headerfilePreambleTemplate = 
            "/**\n"+
            " * @file      %s %s\n"+
            " * @version   %s\n"+
            " * @brief     %s\n"+ 
            " */\n"+
            "\n";
      if ((trueFileName == null) || trueFileName.isEmpty()) {
         trueFileName = "";
      }
      else {
         trueFileName = "(derived from "+trueFileName+")";
      }
      description = description.replaceAll("\n", "\n *            ");
      writer.write( String.format(headerfilePreambleTemplate, 
            fileName, trueFileName, version, description));
   }
   
   /**
    * Write last lines of CPP file<br>
    * EMPTY
    */
   public static void writeCppFilePostAmble() {
      
   }
   
   /**
    * Write header file postamble e.g.
    * <pre><code>
    *  #endif /* <i><b>fileName</i></b> *&#47;
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param fileName   Filename to use in closing block
    * 
    * @throws IOException
    */
   public static void writeHeaderFilePostamble(BufferedWriter writer, String fileName) throws IOException {
      final String headerfilePostambleTemplate = 
            "\n"+
            "#endif /* %s */\n";
      String macroName = HEADER_FILE_PREFIX+fileName.toUpperCase().replaceAll("(\\.|-)", "_");
      writer.write( String.format(headerfilePostambleTemplate, macroName));
   }
   
   /**
    * Write header file include e.g.
    * <pre><code>
    *  #include &lt;<i><b>fileName</i></b>&gt;
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public static void writeSystemHeaderFileInclude(BufferedWriter writer, String fileName) throws IOException {
      writer.write(String.format("#include <%s>\n", fileName));
   }

   /**
    * Write header file include e.g.
    * <pre><code>
    *  #include "<i><b>fileName</i></b>"
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public static void writeHeaderFileInclude(BufferedWriter writer, String fileName) throws IOException {
      writer.write(String.format("#include \"%s\"\n", fileName));
   }

   /**
    * Write namespace open
    * <pre><code>
    *  namespace "<i><b>USBDM</i></b>" {
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param namespace  Namespace to use
    * 
    * @throws IOException
    */
   public static void writeOpenNamespace(BufferedWriter writer, String namespace) throws IOException {
//      writeConditionalStart(writer, CreatePinDescription.NAMESPACES_GUARD_STRING);
      writer.write(String.format("namespace %s {\n\n", namespace));
//      writeConditionalEnd(writer);
   }

   /**
    * Write namespace close
    * <pre><code>
    *  } // End "<i><b>namespace</i></b>"
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param namespace  Namespace to use
    * 
    * @throws IOException
    */
   public static void writeCloseNamespace(BufferedWriter writer, String namespace) throws IOException {
      writer.write(String.format("\n} // End namespace %s\n", namespace));
   }

   /**
    * Write namespace close
    * <pre><code>
    *  }  // Close namespace
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param namespace  Namespace to use
    * 
    * @throws IOException
    */
   public static void writeCloseNamespace(BufferedWriter writer) throws IOException {
      writer.write(String.format("\n} // End namespace\n"));
   }

   /**
    * Write #if directive
    * 
    * <pre><code>
    *  #if (<i><b>condition</i></b>)
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param condition     Condition to use
    * 
    * @throws IOException
    */
   public static void writeConditionalStart(BufferedWriter writer, String condition) throws IOException {
      writer.write(String.format("#if (%s)\n", condition));
   }
   
   /**
    * Write #elif directive
    * 
    * <pre><code>
    *  #elif (<i><b>condition</i></b>)
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param condition     Condition to use
    * 
    * @throws IOException
    */
   public static void writeConditionalElif(BufferedWriter writer, String condition) throws IOException {
      writer.write(String.format("#elif (%s)\n", condition));
   }
   
   /**
    * Write #else directive
    * 
    * <pre><code>
    *  #else <i><b>condition</i></b>
    * </code></pre>
    * 
    * @param writer        Write to write to
    * 
    * @throws IOException
    */
   public static void writeConditionalElse(BufferedWriter writer) throws IOException {
      writer.write(String.format("#else\n"));
   }
   
   /**
    * Write #endif directive
    * 
    * <pre><code>
    *  #endif
    * </code></pre>
    * 
    * @param writer        Write to write to
    * 
    * @throws IOException
    */
   public static void writeConditionalEnd(BufferedWriter writer) throws IOException {
      writer.write(String.format("#endif\n"));
   }
   
   /**
    * Write #if directive or #elif
    * 
    * <pre><code>
    *  #if (<i><b>condition</i></b>)
    *  OR
    *  #elif (<i><b>condition</i></b>)
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param condition     Condition to use
    * @param guardWritten  Indicates a prior condition has been written so a <b>#elif</b> is used
    * 
    * @throws IOException
    */
   public static void writeConditional(BufferedWriter writer, String condition, Boolean guardWritten) throws IOException {
      if (guardWritten) {
         writeConditionalElif(writer, condition);
      }
      else {
         writeConditionalStart(writer, condition);
      }
   }
   
   /**
    * Conditionally write #else directive
    * 
    * <pre><code>
    *  #elif <i><b>condition</i></b>
    * </code></pre>
    * 
    * @param writer        Write to write to
    * @param guardWritten  If false nothing is written (implies a prior #if was written)
    * 
    * @throws IOException
    */
   public static void writeConditionalElse(BufferedWriter writer, boolean guardWritten) throws IOException {
      if (guardWritten) {
         writer.write(String.format("#else\n"));
      }
   }
   
   /**
    * Conditionally write #endif directive
    * 
    * <pre><code>
    *  #endif
    * </code></pre>
    * 
    * @param writer        Write to write to
    * @param guardWritten  If false nothing is written (implies a prior #if was written)
    * 
    * @throws IOException
    */
   public static void writeConditionalEnd(BufferedWriter writer, boolean guardWritten) throws IOException {
      if (guardWritten) {
         writeConditionalEnd(writer);
      }
   }
   
   /**
    * Write wizard marker e.g.
    * <pre><code>
    * //-------- <<< Use Configuration Wizard in Context Menu >>> -----------------
    * </code></pre>
    * 
    * @param writer Where to write
    * @throws IOException
    */
   public static void writeWizardMarker(BufferedWriter writer) throws IOException {
      final String wizardMarker =                                                            
            "//-------- <<< Use Configuration Wizard in Context Menu >>> -----------------  \n\n";
      writer.write(wizardMarker);
   }
   
   /**
    * Write end wizard marker e.g.
    * <pre><code>
    * //-------- <<< end of configuration section >>> -----------------
    * </code></pre>
    * 
    * @param writer Where to write
    * @throws IOException
    */
   public static void writeEndWizardMarker(BufferedWriter writer) throws IOException {
      final String wizardMarker =                                                            
            "//-------- <<< end of configuration section >>> -----------------  \n\n";
      writer.write(wizardMarker);
   }
   
   /**
    * Write start of wizard section marker e.g.
    * <pre><code>
    * //  &lth&gt; <i><b>title</i></b>
    * </code></pre>
    * 
    * @param writer Where to write
    * @param title  Title to write
    * 
    * @throws IOException
    */
   public static void writeWizardSectionOpen(BufferedWriter writer, String title) throws IOException {
      final String optionSectionOpenTemplate = 
            "// <h> %s\n"+
            "\n";
      writer.write(String.format(optionSectionOpenTemplate, title));
   }
   
   /**
    * Write end of wizard section marker e.g.
    * <pre><code>
    * //  &lt/h&gt;
    * </code></pre>
    * 
    * @param writer Where to write
    * 
    * @throws IOException
    */
   public static void writeWizardSectionClose(BufferedWriter writer) throws IOException {
      final String optionSectionClose = 
            "// </h>\n"+
            "\n";
      writer.write(optionSectionClose);
   }
   
   /**
    * Write start of wizard conditional section marker e.g.
    * <pre><code>
    * //  comment
    * //  &lt;e<i><b>offset</i></b>&gt; <i><b>title</i></b> <i><b>[&lt;attribute&gt;...]</i></b>
    * //  &lt;i&gt;   <i><b>hint</i></b>
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param comment    Comment written above (may be null)
    * @param offset     Offset to argument
    * @param attributes Attributes to apply e.g. &lt;constant>
    * @param title      Title to use in selection
    * @param hint       Hint to use with title
    * 
    * @throws IOException
    */
   public static void writeWizardConditionalSectionOpen(BufferedWriter writer, String comment, int offset, WizardAttribute[] attributes, String title, String hint) 
         throws IOException {
      if (comment != null) {
         writer.write(String.format("// %s\n", comment));
      }
      final String headerTemplate =                                                            
            "//   <e%s> %s %s\n"+
            "//   <i> %s\n";
      hint = hint.replaceAll("\n", "\n//   <i> ");

      StringBuffer sb = new StringBuffer();
      if (attributes != null) {
         for (WizardAttribute attribute:attributes) {
            if (attribute != null) {
               sb.append(attribute.getAttributeString());
            }
         }
      }
      writer.write(String.format(headerTemplate, (offset==0)?"":Integer.toString(offset), title, sb.toString(), hint));

   }

   /**
    * Write end of wizard section marker e.g.
    * <pre><code>
    * //  &lt/h&gt;
    * </code></pre>
    * 
    * @param writer Where to write
    * 
    * @throws IOException
    */
   public static void writeWizardConditionalSectionClose(BufferedWriter writer) throws IOException {
      final String optionSectionClose = 
            "// </e>\n"+
            "\n";
      writer.write(optionSectionClose);
   }
   
   /**
    * Write wizard selection preamble e.g.
    * <pre><code>
    * //  comment
    * //  &lt;o<i><b>offset</i></b>&gt; <i><b>title</i></b> <i><b>[&lt;constant&gt;]</i></b>
    * //  &lt;i&gt;       <i><b>hint</i></b>
    * //  &lt;info&gt;    <i><b>information</i></b>
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param comment       Comment written above (may be null)
    * @param offset        Offset to argument
    * @param attributes    Attributes to apply e.g. <constant>
    * @param title         Title to use in selection
    * @param hint          Hint to use with title
    * @param information   Additional information for option
    * 
    * @throws IOException
    */
   public static void writeWizardOptionSelectionPreamble(
         BufferedWriter writer, 
         String comment, 
         int offset, 
         WizardAttribute[] attributes, 
         String title, 
         String hint, 
         String information) throws IOException {
      if (comment != null) {
         writer.write(String.format("// %s\n", comment));
      }
      final String oTemplate = "//   <o%s>    %s";
      writer.write(String.format(oTemplate, (offset==0)?"":Integer.toString(offset), title));
      if (attributes != null) {
         for (WizardAttribute attribute:attributes) {
            if (attribute != null) {
               writer.write(attribute.getAttributeString());
            }
         }
      }
      writer.write("\n");
      final String iTemplate = "//   <i>    %s\n";
      hint = hint.replaceAll("\n", "\n//   <i>   ");
      writer.write(String.format(iTemplate, hint));

      if ((information != null) && (information.length()>0)) {
         final String infoTemplate = "//   <info> %s\n";
         information = information.replaceAll("\n", "\n//   <info> ");
         writer.write(String.format(infoTemplate, information));
      }
   }

   /**
    * Write wizard selection preamble e.g.
    * <pre><code>
    * //  comment
    * //  &lt;o<i><b>offset</i></b>&gt; <i><b>title</i></b> <i><b>[&lt;constant&gt;]</i></b>
    * //  &lt;i&gt;       <i><b>hint</i></b>
    * //  &lt;info&gt;    <i><b>information</i></b>
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param comment       Comment written above (may be null)
    * @param offset        Offset to argument
    * @param attributes    Attributes to apply e.g. <constant>
    * @param title         Title to use in selection
    * @param hint          Hint to use with title
    * @param information   Additional information for option
    * 
    * @throws IOException
    */
   public static void writeWizardOptionSelectionPreamble(
         BufferedWriter writer, 
         String comment, 
         int offset, 
         WizardAttribute[] attributes, 
         String title, 
         String hint) throws IOException {
      writeWizardOptionSelectionPreamble(writer, comment, offset, attributes, title, hint, null);
   }
   /**
    * Write wizard selection preamble e.g.
    * <pre><code>
    * //  comment
    * //  &lt;o<i><b>offset</i></b>&gt; <i><b>title</i></b> <i><b>[&lt;constant&gt;]</i></b>
    * //  &lt;i&gt;   <i><b>hint</i></b>
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param comment    Comment written above (may be null)
    * @param offset     Offset to argument
    * @param isConstant Indicates the entry should be marked &lt;constant&gt;
    * @param title      Title to use in selection
    * @param hint       Hint to use with title
    * 
    * @throws IOException
    */
   public static void writeWizardBinaryOptionSelectionPreamble(BufferedWriter writer, String comment, int offset, boolean isConstant, String title, String hint) 
         throws IOException {
      if (comment != null) {
         writer.write(String.format("// %s\n", comment));
      }
      final String headerTemplate =                                                            
            "//   <q%s> %s %s\n"+
            "//   <i> %s\n";
      hint = hint.replaceAll("\n", "\n//   <i> ");
      writer.write(String.format(headerTemplate, (offset==0)?"":Integer.toString(offset), title, isConstant?"<constant>":"", hint));
   }

   /**
    * Write wizard selection entry e.g.
    * <pre><code>
    * //  &lt;<i><b>value</i></b>=&gt; <i><b>description</i></b> <i><b>&lt;attribute ...&gt; ...</i></b>
    * </code></pre>
    * 
    * @param writer       Where to write
    * @param value        Value to use in selection
    * @param description  Description to use in selection
    * @param attributes   Attributes to add to the options
    * 
    * @throws IOException
    */
   public static void writeWizardOptionSelectionEnty(BufferedWriter writer, String value, String description, WizardAttribute[] attributes) throws IOException {
      final String entryTemplate = "//     <%s=> %s";
      writer.write(String.format(entryTemplate, value, description));
      if (attributes != null) {
         for (WizardAttribute attribute:attributes) {
            if (attribute != null) {
               writer.write(attribute.getAttributeString());
            }
         }
      }
      writer.write("\n");
   }

   /**
    * Write wizard selection entry e.g.
    * <pre><code>
    * //  &lt;<i><b>value</i></b>=&gt; <i><b>description</i></b>
    * </code></pre>
    * 
    * @param writer       Where to write
    * @param value        Value to use in selection
    * @param description  Description to use in selection
    * 
    * @throws IOException
    */
   public static void writeWizardOptionSelectionEnty(BufferedWriter writer, String value, String description) throws IOException {
      writeWizardOptionSelectionEnty(writer, value, description, null);
   }

   /**
    * Write wizard selection entry e.g.
    * <pre><code>
    * //  &lt;<i><b>value</i></b>=&gt; Default
    * </code></pre>
    * 
    * @param writer       Where to write
    * @param value        Value to use in selection
    * 
    * @throws IOException
    */
   public static void writeWizardDefaultSelectionEnty(BufferedWriter writer, String value) throws IOException {
      final String defaultTemplate = "//     <%s=> Default\n";
      writer.write(String.format(defaultTemplate, value));
   }
   
   /**
    * Write simple macro definition e.g.
    * <pre><code>
    * constexpr uint32_t <i><b>name value</i></b>
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param size          Constant size (8, 16 or 32 bits)
    * @param name          Constant name
    * @param value         Constant value
    * 
    * @throws IOException
    */
   public static void writeConstexpr(BufferedWriter writer, int size, String name, String value) throws IOException {
      String type;
      switch(size) {
      case 8:  type = "uint8_t";    break;
      case 16: type = "uint16_t";   break;
      case 32: type = "uint32_t";   break;
      default: type = null;         break;
      }
      final String defineTemplate = "constexpr %8s %-20s = %s;\n";
      writer.write(String.format(defineTemplate, type, name, value));
   }

   /**
    * Write simple macro definition e.g.
    * <pre><code>
    * #define <i><b>name value</i></b>
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param name          Macro name
    * @param value         Macro value
    * 
    * @throws IOException
    */
   public static void writeMacroDefinition(BufferedWriter writer, String name, String value) throws IOException {
      final String defineTemplate = "#define %-20s %s\n";
      writer.write(String.format(defineTemplate, name, value));
   }

   /**
    * Write simple macro definition with in-line comment e.g.
    * <pre><code>
    * #define <i><b>name value</i></b> // <i><b>comment</i></b>
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param name          Macro name
    * @param value         Macro value
    * @param comment       Comment
    * 
    * @throws IOException
    */
   public static void writeMacroDefinition(BufferedWriter writer, String name, String value, String comment) throws IOException {
      final String defineTemplate = "#define %-24s %-20s //%s\n";
      writer.write(String.format(defineTemplate, name, value, comment));
   }
   /**
    * Write parametised macro with comment block
    * 
    * <pre><code>
    * &#47;**
    *  *  &#64;brief <i>briefDescription</i>
    *  *
    *  *  &#64;param <i>paramDescription</i>
    *  *
    *  *  #define <i>name</i>    <i>value</i>
    *  *&#47;
    * </code></pre>
    * 
    * @param writer              Where to write
    * @param briefDescription    Brief description
    * @param paramDescription    Parameter with description e.g. <i>value Value to square</i>
    * @param name                Macro name with parameter e.g. <i>sqr(value)</i>
    * @param value               Macro value with substitutions e.g. <i>((value)&(value))</i>
    * 
    * @throws IOException
    */
   public static void writeMacroDefinition(BufferedWriter writer, String briefDescription, String paramDescription, String name, String value) throws IOException {
      
      writer.write( String.format(
      "/**\n"+
      " * @brief %s\n"+
      " *\n"+
      " * @param %s\n"+
      " */\n"+
      "#define %-20s  %s\n"+
      "\n",
      briefDescription, paramDescription, name, value
      ));
      
   }
   /**
    * Write Undef for macro
    * <pre><code>
    * #undef <i><b>name</i></b> // <i><b>comment</i></b>
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param name          Macro name
    * @param comment       Comment
    * @throws IOException 
    */
   public static void writeMacroUnDefinition(BufferedWriter writer, String name, String comment) throws IOException {
      final String defineTemplate = "#undef %-24s //%s\n";
      writer.write(String.format(defineTemplate, name, comment));
   }
   /**
    * Write Undef for macro
    * <pre><code>
    * #undef <i><b>name</i></b>
    * </code></pre>
    * 
    * @param writer        Where to write
    * @param name          Macro name
    * @throws IOException 
    */
   public static void writeMacroUnDefinition(BufferedWriter writer, String name) throws IOException {
      final String defineTemplate = "#undef %-24s\n";
      writer.write(String.format(defineTemplate, name, ""));
   }

   /**
    * Writes a simple banner
    * <pre><code>
    * /*
    *  * <b><i>banner...</b></i>
    *  * <b><i>banner...</b></i>
    *  *&#47;
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public static void writeBanner(BufferedWriter writer, String banner) throws IOException {
      banner = "/*\n * " + banner.replaceAll("\n", "\n * ") + "\n */\n";
      writer.write(banner);
   }

   /**
    * Writes a simple banner
    * <pre><code>
    * /**
    *  * <b><i>banner...</b></i>
    *  * <b><i>banner...</b></i>
    *  *&#47;
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public static void writeDocBanner(BufferedWriter writer, String banner) throws IOException {
      banner = "/**\n * " + banner.replaceAll("\n", "\n * ") + "\n */\n";
      writer.write(banner);
   }

}
   