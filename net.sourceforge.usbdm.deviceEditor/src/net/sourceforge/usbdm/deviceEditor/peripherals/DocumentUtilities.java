package net.sourceforge.usbdm.deviceEditor.peripherals;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

public class DocumentUtilities {

   /** This is written at the top of each generated file */
   static final String WARNING_BANNER = 
         " * *****************************\n"+
         " * *** DO NOT EDIT THIS FILE ***\n"+
         " * *****************************\n"+
         " *\n"+
         " * This file is generated automatically.\n"+
         " * Any manual changes will be lost.\n";

   /** Writer to use */
   private final BufferedWriter fWriter;

   /** StringBuilder to use */
   private final StringBuilder  fStringBuidler;

   /** Stack of documentation group names */
   private Stack<String> fGroupStack = new Stack<String>();

   /** Name of current group */
   private String fCurrentGroupName = null;

   /**
    * Write text to StringBuilder or BufferedWriter as appropriate
    *
    * @param text          Text to write
    * 
    * @throws IOException
    */
   private void _write(String text) throws IOException {
      if (fWriter != null) {
         fWriter.write(text);
      }
      else {
         fStringBuidler.append(text);
      }
   }
   
   /**
    * Return contents as String
    * 
    * @return Contents
    */
   @Override
   public String toString() {
      return fStringBuidler.toString();
      
   }
   public DocumentUtilities(StringBuilder sb) {
      fWriter = null;
      fStringBuidler = sb;
   }
   
   public DocumentUtilities(BufferedWriter writer) {
      fWriter      = writer;
      fStringBuidler = null;
   }
   
   public void close() throws IOException {
      if (fWriter != null) {
         fWriter.close();
      }
   }

   void pushGroup(String groupName) {
      fGroupStack.push(groupName);
      fCurrentGroupName = fGroupStack.peek();
   }

   void popGroup() {
      if (fGroupStack.isEmpty()) {
         throw new RuntimeException("Trying to close non-existent Documentation Group");
      }
      fGroupStack.pop();
      fCurrentGroupName = null;
      if (!fGroupStack.isEmpty()) {
         fCurrentGroupName = fGroupStack.peek();
      }
   }
   
   /**
    * Opens a nested documentation group
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <i><b>groupName groupTitle</i></b>                   
    *  * @brief       <i><b>groupBrief</i></b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param groupName     Name of group
    * @param groupTitle    Title of group (may be null)
    * @param groupBrief    Brief description of group (may be null)
    * 
    * @throws IOException
    */
   void openDocumentationGroup(String groupName, String groupTitle, String groupBrief) throws IOException {
      if (groupName == null) {
         groupName = "";
      }
      pushGroup(groupName);

      final String startGroup1 = 
            "/**\n"+
            " * @addtogroup %s %s\n";
      final String startGroup2 = 
            " * @brief %s\n";
      final String startGroup3 = 
            " * @{\n"+
            " */\n";
      _write(String.format(startGroup1, groupName, (groupTitle==null)?"":groupTitle));
      if (groupBrief != null) {
         _write(String.format(startGroup2, groupBrief));
      }
      _write(String.format(startGroup3));
   }   
   
   /**
    * Opens a nested documentation group for a peripheral
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <i><b>peripheral.groupName peripheral.groupTitle</i></b>                   
    *  * @brief       <i><b>peripheral.groupBrief</i></b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param peripheral Peripheral used to obtain description etc.
    * 
    * @throws IOException
    */
   void openDocumentationGroup(Peripheral peripheral) throws IOException {
      openDocumentationGroup(peripheral.getGroupName(), peripheral.getGroupTitle(), peripheral.getGroupBriefDescription());
   }

   /**
    * Conditionally opens a nested documentation group for a peripheral
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <i><b>peripheral.groupName peripheral.groupTitle</i></b>                   
    *  * @brief       <i><b>peripheral.groupBrief</i></b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param peripheral Peripheral used to obtain description etc.
    * @param groupDone  Indicates if group has already been done
    * 
    * @throws IOException
    */
   void conditionallyOpenDocumentationGroup(Peripheral peripheral, boolean groupDone) throws IOException {
      if (groupDone) {
         return;
      }
      openDocumentationGroup(peripheral);
   }
   
   /**
    * Opens default USBDM_group
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <b>USBDM_Group USBDM Peripheral Interface</b>                   
    *  * @brief       <b>Hardware Peripheral Interface and library</b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param peripheral Peripheral used to obtain description etc.
    * @param groupDone  Indicates if group has already been done
    * 
    * @throws IOException
    */
   void openUsbdmDocumentationGroup() throws IOException {
      openDocumentationGroup("USBDM_Group", "USBDM Peripheral Interface", "Hardware Peripheral Interface and library");
   }
   
   /**
    * Closes a nested documentation group 
    * <pre><code>
    * /**                                                                    
    *  * @}                                                                   
    *  ** <i><b>groupName</i></b> **&#47;</code></pre>
    * 
    * @throws IOException
    */
   void closeDocumentationGroup() throws IOException {
      final String endGroup = 
            "/** \n"+
            " * End group %s\n"+
            " * @}\n"+
            " */\n";
      _write(String.format(endGroup, fCurrentGroupName));
      popGroup();
   }

   final String HEADER_FILE_PREFIX = "PROJECT_HEADERS_";
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
    * @param fileName      Filename to use in header block
    * @param trueFileName  Filename of actual file being written
    * @param version       Version to use in header block
    * @param description   Description to use in header block
    * 
    * @throws IOException
    */
   public void writeHeaderFilePreamble(String fileName, String trueFileName, String version, String description) throws IOException {
      final String headerfilePreambleTemplate = 
            "/**\n"+
            " * @file      %s %s\n"+
            " * @version   %s\n"+
            " * @brief     %s\n"+ 
            " *\n"+
            WARNING_BANNER+
            " */\n"+
            "#ifndef %s\n"+
            "#define %s\n"+
            "\n";
      String macroName = HEADER_FILE_PREFIX+fileName.toUpperCase().replaceAll("(\\.|-)", "_");
      if ((trueFileName == null) || trueFileName.isEmpty()) {
         trueFileName = "";
      }
      else {
         trueFileName = "(generated from "+trueFileName+")";
      }
      description = description.replaceAll("\n", "\n *            ");
      _write( String.format(headerfilePreambleTemplate, 
            fileName, trueFileName, version, description, macroName, macroName ));
   }

   /**
    * Format source file preamble e.g.
    * <pre><code>
    *  /**
    *   * @file    <i><b>fileName</i></b> (derived from <i><b>trueFileName</i></b>)
    *   * @version <i><b>version</i></b>
    *   * @brief   <i><b>description</i></b> 
    *   *&#47;
    * </code></pre>
    * 
    * @param fStringBuidler            Where to write to
    * @param fileName      Filename to use in header block
    * @param trueFileName  Filename of actual file being written
    * @param description   Description to use in header block
    * @return 
    * 
    * @throws IOException
    */
   public void writeCppFilePreamble(String fileName, String trueFileName, String version, String description) throws IOException {
      final String headerfilePreambleTemplate = 
            "/**\n"+
            " * @file      %s %s\n"+
            " * @version   %s\n"+
            " * @brief     %s\n"+ 
            " *\n"+
            WARNING_BANNER+
            " */\n"+
            "\n";
      if ((trueFileName == null) || trueFileName.isEmpty()) {
         trueFileName = "";
      }
      else {
         trueFileName = "(generated from "+trueFileName+")";
      }
      description = description.replaceAll("\n", "\n *            ");
      _write(String.format(headerfilePreambleTemplate, fileName, trueFileName, version, description));

   }
   
   /**
    * Write last lines of CPP file<br>
    * EMPTY
    */
   public void writeCppFilePostAmble() {
      
   }
   
   /**
    * Write header file postamble e.g.
    * <pre><code>
    *  #endif /* <i><b>fileName</i></b> *&#47;
    * </code></pre>
    * 
    * @param fileName   Filename to use in closing block
    * 
    * @throws IOException
    */
   public void writeHeaderFilePostamble(String fileName) throws IOException {
      final String headerfilePostambleTemplate = 
            "\n"+
            "#endif /* %s */\n";
      String macroName = HEADER_FILE_PREFIX+fileName.toUpperCase().replaceAll("(\\.|-)", "_");
      _write( String.format(headerfilePostambleTemplate, macroName));
   }
   
   /**
    * Write header file include e.g.
    * <pre><code>
    *  #include &lt;<i><b>fileName</i></b>&gt;
    * </code></pre>
    * 
    * @param fWriter     Where to write
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public void writeSystemHeaderFileInclude(String fileName) throws IOException {
      _write(String.format("#include <%s>\n", fileName));
   }

   /**
    * Format header file include e.g.
    * <pre><code>
    *  #include "<i><b>fileName</i></b>"
    * </code></pre>
    * 
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public static String createHeaderFileInclude(String fileName) {
      return String.format("#include \"%s\"\n", fileName);
   }

   /**
    * Write header file include e.g.
    * <pre><code>
    *  #include "<i><b>fileName</i></b>"
    * </code></pre>
    * 
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public void writeHeaderFileInclude(String fileName) throws IOException {
      _write(createHeaderFileInclude(fileName));
   }

   private Deque<String> nameSpaceStack = new ArrayDeque<String>();

   /**
    * Write namespace open
    * <pre><code>
    *  /* 
    *   * <b><i>description</i></b>
    *   *&#47;
    *  namespace "<i><b><b><i>namespace</i></b></i></b>" {
    * </code></pre>
    * 
    * @param namespace   Namespace to use
    * @param description Description to use
    * 
    * @throws IOException
    */
   public void writeOpenNamespace(String namespace, String description) throws IOException {
      if (description != null) {
         _write(String.format("/**\n * %s\n */\n", description));
      }
      _write(String.format("namespace %s {\n\n", namespace));
      nameSpaceStack.push(namespace);
      flush();
   }

   /**
    * Write namespace open
    * <pre><code>
    *  namespace "<i><b><b><i>namespace</i></b></i></b>" {
    * </code></pre>
    * 
    * @param namespace  Namespace to use
    * 
    * @throws IOException
    */
   public void writeOpenNamespace(String namespace) throws IOException {
      writeOpenNamespace(namespace, null);
   }

   /**
    * Write USBDM namespace open
    * <pre><code>
    *  /* 
    *   * <b><i>Namespace enclosing USBDM classes</i></b>
    *   *&#47;
    *  namespace "<i><b><b><i>USBDM</i></b></i></b>" {
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeOpenUsbdmNamespace() throws IOException {
      writeOpenNamespace(DeviceInfo.NAME_SPACE_USBDM_LIBRARY, "Namespace enclosing USBDM classes");
   }
  
   /**
    * Write USBDM namespace open
    * <pre><code>
    *  /* 
    *   * <b><i>Namespace enclosing USBDM variables representing peripheral signals mapped to pins</i></b>
    *   *&#47;
    *  namespace "<i><b><b><i>SIGNALS</i></b></i></b>" {
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeOpenSignalsNamespace() throws IOException {
      writeOpenNamespace(DeviceInfo.NAME_SPACE_SIGNALS, "Namespace enclosing USBDM variables representing peripheral signals mapped to pins");
   }

   /**
    * Write namespace close
    * <pre><code>
    *  } // End "<i><b>namespace</i></b>"
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeCloseNamespace() throws IOException {
      if (nameSpaceStack.isEmpty()) {
         throw new RuntimeException("Closing non-open namespace");
      }
      String currentNamespace = nameSpaceStack.pop();
      _write(String.format("\n} // End namespace %s\n\n", currentNamespace));
      flush();
   }

   /**
    * Write #if directive
    * 
    * <pre><code>
    *  #if (<i><b>condition</i></b>)
    * </code></pre>
    * 
    * @param condition     Condition to use
    * 
    * @throws IOException
    */
   public void writeConditionalStart(String condition) throws IOException {
      _write(String.format("#if (%s)\n", condition));
   }
   
   /**
    * Write #elif directive
    * 
    * <pre><code>
    *  #elif (<i><b>condition</i></b>)
    * </code></pre>
    * 
    * @param condition     Condition to use
    * 
    * @throws IOException
    */
   public void writeConditionalElif(String condition) throws IOException {
      _write(String.format("#elif (%s)\n", condition));
   }
   
   /**
    * Write #else directive
    * 
    * <pre><code>
    *  #else <i><b>condition</i></b>
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeConditionalElse() throws IOException {
      _write(String.format("#else\n"));
   }
   
   /**
    * Write #endif directive
    * 
    * <pre><code>
    *  #endif
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeConditionalEnd() throws IOException {
      _write(String.format("#endif\n"));
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
    * @param condition     Condition to use
    * @param guardWritten  Indicates a prior condition has been written so a <b>#elif</b> is used
    * 
    * @throws IOException
    */
   public void writeConditional(String condition, Boolean guardWritten) throws IOException {
      if (guardWritten) {
         writeConditionalElif(condition);
      }
      else {
         writeConditionalStart(condition);
      }
   }
   
   /**
    * Conditionally write #else directive
    * 
    * <pre><code>
    *  #elif <i><b>condition</i></b>
    * </code></pre>
    * 
    * @param guardWritten  If false nothing is written (implies a prior #if was written)
    * 
    * @throws IOException
    */
   public void writeConditionalElse(boolean guardWritten) throws IOException {
      if (guardWritten) {
         _write(String.format("#else\n"));
      }
   }
   
   /**
    * Conditionally write #endif directive
    * 
    * <pre><code>
    *  #endif
    * </code></pre>
    * 
    * @param guardWritten  If false nothing is written (implies a prior #if was written)
    * 
    * @throws IOException
    */
   public void writeConditionalEnd(boolean guardWritten) throws IOException {
      if (guardWritten) {
         writeConditionalEnd();
      }
   }
   
   /**
    * Write wizard marker e.g.
    * <pre><code>
    * //-------- <<< Use Configuration Wizard in Context Menu >>> -----------------
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeWizardMarker() throws IOException {
      final String wizardMarker =                                                            
            "//-------- <<< Use Configuration Wizard in Context Menu >>> -----------------  \n\n";
      _write(wizardMarker);
   }
   
   /**
    * Write end wizard marker e.g.
    * <pre><code>
    * //-------- <<< end of configuration section >>> -----------------
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeEndWizardMarker() throws IOException {
      final String wizardMarker =                                                            
            "//-------- <<< end of configuration section >>> -----------------  \n\n";
      _write(wizardMarker);
   }
   
   /**
    * Write start of wizard section marker e.g.
    * <pre><code>
    * //  &lth&gt; <i><b>title</i></b>
    * </code></pre>
    * 
    * @param title  Title to write
    * 
    * @throws IOException
    */
   public void writeWizardSectionOpen(String title) throws IOException {
      final String optionSectionOpenTemplate = 
            "// <h> %s\n"+
            "\n";
      _write(String.format(optionSectionOpenTemplate, title));
   }
   
   /**
    * Write end of wizard section marker e.g.
    * <pre><code>
    * //  &lt/h&gt;
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeWizardSectionClose() throws IOException {
      final String optionSectionClose = 
            "// </h>\n"+
            "\n";
      _write(optionSectionClose);
   }
   
   /**
    * Write start of wizard conditional section marker e.g.
    * <pre><code>
    * //  comment
    * //  &lt;e<i><b>offset</i></b>&gt; <i><b>title</i></b> <i><b>[&lt;attribute&gt;...]</i></b>
    * //  &lt;i&gt;   <i><b>hint</i></b>
    * </code></pre>
    * 
    * @param comment    Comment written above (may be null)
    * @param offset     Offset to argument
    * @param attributes Attributes to apply e.g. &lt;constant>
    * @param title      Title to use in selection
    * @param hint       Hint to use with title
    * 
    * @throws IOException
    */
   public void writeWizardConditionalSectionOpen(String comment, int offset, WizardAttribute[] attributes, String title, String hint) 
         throws IOException {
      if (comment != null) {
         _write(String.format("// %s\n", comment));
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
      _write(String.format(headerTemplate, (offset==0)?"":Integer.toString(offset), title, sb.toString(), hint));

   }

   /**
    * Write end of wizard section marker e.g.
    * <pre><code>
    * //  &lt/h&gt;
    * </code></pre>
    * 
    * @throws IOException
    */
   public void writeWizardConditionalSectionClose() throws IOException {
      final String optionSectionClose = 
            "// </e>\n"+
            "\n";
      _write(optionSectionClose);
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
    * @param comment       Comment written above (may be null)
    * @param offset        Offset to argument
    * @param attributes    Attributes to apply e.g. <constant>
    * @param title         Title to use in selection
    * @param hint          Hint to use with title
    * @param information   Additional information for option
    * 
    * @throws IOException
    */
   public void writeWizardOptionSelectionPreamble(
         
         String comment, 
         int offset, 
         WizardAttribute[] attributes, 
         String title, 
         String hint, 
         String information) throws IOException {
      if (comment != null) {
         _write(String.format("// %s\n", comment));
      }
      final String oTemplate = "//   <o%s>    %s";
      _write(String.format(oTemplate, (offset==0)?"":Integer.toString(offset), title));
      if (attributes != null) {
         for (WizardAttribute attribute:attributes) {
            if (attribute != null) {
               _write(attribute.getAttributeString());
            }
         }
      }
      _write("\n");
      final String iTemplate = "//   <i>    %s\n";
      hint = hint.replaceAll("\n", "\n//   <i>   ");
      _write(String.format(iTemplate, hint));

      if ((information != null) && (information.length()>0)) {
         final String infoTemplate = "//   <info> %s\n";
         information = information.replaceAll("\n", "\n//   <info> ");
         _write(String.format(infoTemplate, information));
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
    * @param comment       Comment written above (may be null)
    * @param offset        Offset to argument
    * @param attributes    Attributes to apply e.g. <constant>
    * @param title         Title to use in selection
    * @param hint          Hint to use with title
    * @param information   Additional information for option
    * 
    * @throws IOException
    */
   public void writeWizardOptionSelectionPreamble(
         String comment, 
         int offset, 
         WizardAttribute[] attributes, 
         String title, 
         String hint) throws IOException {
      writeWizardOptionSelectionPreamble(comment, offset, attributes, title, hint, null);
   }
   
   /**
    * Write wizard selection preamble e.g.
    * <pre><code>
    * //  comment
    * //  &lt;o<i><b>offset</i></b>&gt; <i><b>title</i></b> <i><b>[&lt;constant&gt;]</i></b>
    * //  &lt;i&gt;   <i><b>hint</i></b>
    * </code></pre>
    * 
    * @param comment    Comment written above (may be null)
    * @param offset     Offset to argument
    * @param isConstant Indicates the entry should be marked &lt;constant&gt;
    * @param title      Title to use in selection
    * @param hint       Hint to use with title
    * 
    * @throws IOException
    */
   public void writeWizardBinaryOptionSelectionPreamble(String comment, int offset, boolean isConstant, String title, String hint) 
         throws IOException {
      if (comment != null) {
         _write(String.format("// %s\n", comment));
      }
      final String headerTemplate =                                                            
            "//   <q%s> %s %s\n"+
            "//   <i> %s\n";
      hint = hint.replaceAll("\n", "\n//   <i> ");
      _write(String.format(headerTemplate, (offset==0)?"":Integer.toString(offset), title, isConstant?"<constant>":"", hint));
   }

   /**
    * Write wizard selection entry e.g.
    * <pre><code>
    * //  &lt;<i><b>value</i></b>=&gt; <i><b>description</i></b> <i><b>&lt;attribute ...&gt; ...</i></b>
    * </code></pre>
    * 
    * @param value        Value to use in selection
    * @param description  Description to use in selection
    * @param attributes   Attributes to add to the options
    * 
    * @throws IOException
    */
   public void writeWizardOptionSelectionEnty(String value, String description, WizardAttribute[] attributes) throws IOException {
      final String entryTemplate = "//     <%s=> %s";
      _write(String.format(entryTemplate, value, description));
      if (attributes != null) {
         for (WizardAttribute attribute:attributes) {
            if (attribute != null) {
               _write(attribute.getAttributeString());
            }
         }
      }
      _write("\n");
   }

   /**
    * Write wizard selection entry e.g.
    * <pre><code>
    * //  &lt;<i><b>value</i></b>=&gt; <i><b>description</i></b>
    * </code></pre>
    * 
    * @param value        Value to use in selection
    * @param description  Description to use in selection
    * 
    * @throws IOException
    */
   public void writeWizardOptionSelectionEnty(String value, String description) throws IOException {
      writeWizardOptionSelectionEnty(value, description, null);
   }

   /**
    * Write wizard selection entry e.g.
    * <pre><code>
    * //  &lt;<i><b>value</i></b>=&gt; Default
    * </code></pre>
    * 
    * @param value        Value to use in selection
    * 
    * @throws IOException
    */
   public void writeWizardDefaultSelectionEnty(String value) throws IOException {
      final String defaultTemplate = "//     <%s=> Default\n";
      _write(String.format(defaultTemplate, value));
   }
   
   /**
    * Write simple macro definition e.g.
    * <pre><code>
    * constexpr uint32_t <i><b>name value</i></b>
    * </code></pre>
    * 
    * @param size          Constant size (8, 16 or 32 for uintXX, -8, -16,-32 for intXX )
    * @param name          Constant name
    * @param value         Constant value
    * 
    * @throws IOException
    */
   public void writeConstexpr(int size, String name, String value) throws IOException {
      String type;
      switch(size) {
      case 8:   type = "uint8_t";    break;
      case 16:  type = "uint16_t";   break;
      case 32:  type = "uint32_t";   break;
      case -8:  type = "int8_t";     break;
      case -16: type = "int16_t";    break;
      case -32: type = "int32_t";    break;
      default: type = null;          break;
      }
      final String defineTemplate = "constexpr %8s %-20s = %s;\n";
      _write(String.format(defineTemplate, type, name, value));
   }

   /**
    * Write simple macro definition e.g.
    * <pre><code>
    * #define <i><b>name</i></b>
    * </code></pre>
    * 
    * @param name          Macro name
    * 
    * @throws IOException
    */
   public void writeMacroDefinition(String name) throws IOException {
      final String defineTemplate = "#define %-20s\n";
      _write(String.format(defineTemplate, name));
   }


   /**
    * Write simple macro definition e.g.
    * <pre><code>
    * #define <i><b>name value</i></b>
    * </code></pre>
    * 
    * @param name          Macro name
    * @param value         Macro value
    * 
    * @throws IOException
    */
   public void writeMacroDefinition(String name, String value) throws IOException {
      final String defineTemplate = "#define %-20s %s\n";
      _write(String.format(defineTemplate, name, value));
   }
   /**
    * Write simple macro definition with in-line comment e.g.
    * <pre><code>
    * #define <i><b>name value</i></b> // <i><b>comment</i></b>
    * </code></pre>
    * 
    * @param name          Macro name
    * @param value         Macro value
    * @param comment       Comment
    * 
    * @throws IOException
    */
   public void writeMacroDefinition(String name, String value, String comment) throws IOException {
      final String defineTemplate = "#define %-24s %-20s //%s\n";
      _write(String.format(defineTemplate, name, value, comment));
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
    * @param briefDescription    Brief description
    * @param paramDescription    Parameter with description e.g. <i>value Value to square</i>
    * @param name                Macro name with parameter e.g. <i>sqr(value)</i>
    * @param value               Macro value with substitutions e.g. <i>((value)&(value))</i>
    * 
    * @throws IOException
    */
   public void writeMacroDefinition(String briefDescription, String paramDescription, String name, String value) throws IOException {
      
      _write( String.format(
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
    * @param name          Macro name
    * @param comment       Comment
    * @throws IOException 
    */
   public void writeMacroUnDefinition(String name, String comment) throws IOException {
      final String defineTemplate = "#undef %-24s //%s\n";
      _write(String.format(defineTemplate, name, comment));
   }
   /**
    * Write Undef for macro
    * <pre><code>
    * #undef <i><b>name</i></b>
    * </code></pre>
    * 
    * @param name          Macro name
    * @throws IOException 
    */
   public void writeMacroUnDefinition(String name) throws IOException {
      final String defineTemplate = "#undef %-24s\n";
      _write(String.format(defineTemplate, name, ""));
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
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public void writeBanner(String banner) throws IOException {
      banner = "/*\n * " + banner.replaceAll("\n", "\n * ") + "\n */\n";
      _write(banner);
   }

   /**
    * Writes am indented simple banner
    * <pre><code>
    * .../*
    * ... * <b><i>banner...</b></i>
    * ... * <b><i>banner...</b></i>
    * ... *&#47;
    * </code></pre>
    * 
    * @param fileName   Filename to use in #include directive
    * 
    * @throws IOException
    */
   public void writeBanner(String indent, String banner) throws IOException {
      banner = indent+"/*\n"+indent+" * " + banner.replaceAll("\n", "\n"+indent+" * ") + "\n"+indent+" */\n";
      _write(banner);
   }

   /**
    * Writes a simple banner.<br>
    * Any new-lines in the banner will be used to break the banner into multiple formatted lines.
    * <pre><code>
    * /**
    *  * <b><i>banner...</b></i>
    *  * <b><i>banner...</b></i>
    *  *&#47;
    * </code></pre>
    * 
    * @param banner   String to be printed as banner 
    * 
    * @throws IOException
    */
   public void writeDocBanner(String banner) throws IOException {
      banner = "/**\n * " + banner.replaceAll("\n", "\n * ") + "\n */\n";
      _write(banner);
   }

   /**
    * Writes a string
    * 
    * @param string   String to write
    * 
    * @throws IOException
    */
   public void write(String string) throws IOException {
      _write(string);
   }

   public void flush() throws IOException {
      if (fWriter != null) {
         fWriter.flush();
      }
   }

}
   