import java.io.BufferedWriter;
import java.io.IOException;

public class DocumentUtilities {
   
   class PinFunctionDescription {
      String baseName;
      String outputTemplate;
      String className;
      
      public PinFunctionDescription(String baseName, String className, String outputTemplate) {
         this.baseName              = baseName;
         this.outputTemplate        = outputTemplate;
         this.className             = className;
      }
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
    * @param description   Description of group to use
    * 
    * @throws IOException
    */
   static void writeStartGroup(BufferedWriter writer, FunctionTemplateInformation description) throws IOException {
      writeStartGroup(writer, description.groupName, description.groupTitle, description.groupBriefDescription);
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
   static void writeStartGroup(BufferedWriter writer, String groupName, String groupTitle, String groupBrief) throws IOException {
      final String startGroup1 = 
            "/**\n"+
            "* @addtogroup %s %s\n";
      final String startGroup2 = 
            "* @brief %s\n";
      final String startGroup3 = 
            "* @{\n"+
            "*/\n";
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
   static void writeCloseGroup(BufferedWriter writer, String groupName) throws IOException {
      final String endGroup = 
            "/**\n"+
            " * @}\n"+
            " ** %s */\n";
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
   static void writeCloseGroup(BufferedWriter writer) throws IOException {
      final String endGroup = 
            "/**\n"+
            " * @}\n"+
            " */\n";
      writer.write(endGroup);
   }

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
   void writeHeaderFilePreamble(BufferedWriter writer, String fileName, String trueFileName, String version, String description) throws IOException {
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
      String macroName = fileName.toUpperCase().replaceAll("(\\.|-)", "_")+"_";
      if ((trueFileName == null) || trueFileName.isEmpty()) {
         trueFileName = "";
      }
      else {
         trueFileName = "(derived from "+trueFileName+")";
      }
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
   void writeCppFilePreable(BufferedWriter writer, String fileName, String trueFileName, String description) throws IOException {
      final String cppFilePreambleTemplate = 
            " /**\n"+
            "  * @file     %s %s\n"+
            "  *\n"+
            "  * @brief   %s\n"+ 
            "  */\n"+
            "\n";
      if ((trueFileName == null) || trueFileName.isEmpty()) {
         trueFileName = "";
      }
      else {
         trueFileName = "(from "+trueFileName+")";
      }
      writer.write( String.format(cppFilePreambleTemplate, fileName, trueFileName, description ));
   }
   
   void writeCppFilePostAmple() {
      
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
   void writeHeaderFilePostamble(BufferedWriter writer, String fileName) throws IOException {
      final String headerfilePostambleTemplate = 
            "\n"+
            "#endif /* %s */\n";
      String macroName = fileName.toUpperCase().replaceAll("\\.", "_")+"_";
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
   void writeSystemHeaderFileInclude(BufferedWriter writer, String fileName) throws IOException {
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
   void writeHeaderFileInclude(BufferedWriter writer, String fileName) throws IOException {
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
   void writeOpenNamespace(BufferedWriter writer, String namespace) throws IOException {
      writeConditionalStart(writer, CreatePinDescription.NAMESPACES_GUARD_STRING);
      writer.write(String.format("namespace %s {\n", namespace));
      writeConditionalEnd(writer);
   }

   /**
    * Write namespace close
    * <pre><code>
    *  } // End "<i><b>USBDM</i></b>"
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param namespace  Namespace to use
    * 
    * @throws IOException
    */
   void writeCloseNamespace(BufferedWriter writer, String namespace) throws IOException {
      writeConditionalStart(writer, CreatePinDescription.NAMESPACES_GUARD_STRING);
      writer.write(String.format("} // End namespace %s\n", namespace));
      writeConditionalEnd(writer);
   }

   /**
    * Write #if directive
    * 
    * <pre><code>
    *  #if <i><b>condition</i></b>
    * </code></pre>
    * 
    * @param writer
    * @param condition
    * @throws IOException
    */
   void writeConditionalStart(BufferedWriter writer, String condition) throws IOException {
      writer.write(String.format("#if %s\n", condition));
   }
   
   /**
    * Write #elif directive
    * 
    * <pre><code>
    *  #elif <i><b>condition</i></b>
    * </code></pre>
    * 
    * @param writer
    * @param condition
    * @throws IOException
    */
   void writeConditionalElse(BufferedWriter writer, String condition) throws IOException {
      writer.write(String.format("#elif %s\n", condition));
   }
   
   /**
    * Write #endif directive
    * 
    * <pre><code>
    *  #endif
    * </code></pre>
    * 
    * @param writer
    * @param condition
    * @throws IOException
    */
   void writeConditionalEnd(BufferedWriter writer) throws IOException {
      writer.write(String.format("#endif\n"));
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
   void writeWizardMarker(BufferedWriter writer) throws IOException {
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
   void writeEndWizardMarker(BufferedWriter writer) throws IOException {
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
   void writeWizardSectionOpen(BufferedWriter writer, String title) throws IOException {
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
   void writeWizardSectionClose(BufferedWriter writer) throws IOException {
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
   void writeWizardConditionalSectionOpen(BufferedWriter writer, String comment, int offset, WizardAttribute[] attributes, String title, String hint) 
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
   void writeWizardConditionalSectionClose(BufferedWriter writer) throws IOException {
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
    * //  &lt;i&gt;   <i><b>hint</i></b>
    * </code></pre>
    * 
    * @param writer     Where to write
    * @param comment    Comment written above (may be null)
    * @param offset     Offset to argument
    * @param attributes Attributes to apply e.g. <constant>
    * @param title      Title to use in selection
    * @param hint       Hint to use with title
    * 
    * @throws IOException
    */
   /**
    * 
    * @param writer
    * @param comment
    * @param offset
    * @param attributes
    * @param title
    * @param hint
    * @throws IOException
    */
   void writeWizardOptionSelectionPreamble(BufferedWriter writer, String comment, int offset, WizardAttribute[] attributes, String title, String hint) 
         throws IOException {
      if (comment != null) {
         writer.write(String.format("// %s\n", comment));
      }
      final String oTemplate = "//   <o%s> %s";
      writer.write(String.format(oTemplate, (offset==0)?"":Integer.toString(offset), title));
      if (attributes != null) {
         for (WizardAttribute attribute:attributes) {
            if (attribute != null) {
               writer.write(attribute.getAttributeString());
            }
         }
      }
      writer.write("\n");
      final String iTemplate = "//   <i> %s\n";
      hint = hint.replaceAll("\n", "\n//   <i> ");
      writer.write(String.format(iTemplate, hint));
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
   void writeWizardBinaryOptionSelectionPreamble(BufferedWriter writer, String comment, int offset, boolean isConstant, String title, String hint) 
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
   void writeWizardOptionSelectionEnty(BufferedWriter writer, String value, String description, WizardAttribute[] attributes) throws IOException {
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
   void writeWizardOptionSelectionEnty(BufferedWriter writer, String value, String description) throws IOException {
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
   void writeWizardDefaultSelectionEnty(BufferedWriter writer, String value) throws IOException {
      final String defaultTemplate = "//     <%s=> Default\n";
      writer.write(String.format(defaultTemplate, value));
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
   void writeMacroDefinition(BufferedWriter writer, String name, String value) throws IOException {
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
   void writeMacroDefinition(BufferedWriter writer, String name, String value, String comment) throws IOException {
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
   static void writeMacroDefinition(BufferedWriter writer, String briefDescription, String paramDescription, String name, String value) throws IOException {
      
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

}
   