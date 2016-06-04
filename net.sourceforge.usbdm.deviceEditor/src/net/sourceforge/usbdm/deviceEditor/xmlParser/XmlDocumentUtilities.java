package net.sourceforge.usbdm.deviceEditor.xmlParser;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

/**
 * Utility routines to aid writing XML
 */
public class XmlDocumentUtilities {
   
   static private int fAttrWidth = 20;
   
   final BufferedWriter  fWriter;
   final Stack<XmlEntry> fXmlStack;
   int                   fLevel;
   int                   padding;

   /**
    * Constructor
    * 
    * @param writer Where to write the XML
    */
   public XmlDocumentUtilities(BufferedWriter writer) {
      fWriter   = writer;
      fXmlStack = new Stack<XmlEntry>();
      fLevel    = 0;
      padding   = 0;
   }

   Deque<Integer> fWidthStack = new ArrayDeque<Integer>();
   
   public void setAttrWidth(int width) {
      fWidthStack.push(fAttrWidth);
      fAttrWidth = width;
   }
   
   public void popAttrWidth() {
      if (!fWidthStack.isEmpty()) {
         fAttrWidth = fWidthStack.pop();
      }
   }
   
   /**
    * Used to keep track of tags and indenting
    */
   static class XmlEntry {
      /** Tag name */
      String      fTag;
      
      /** Indicates if opening tag is still open */
      boolean     open;
      
      /**
       * Create Tag
       * 
       * @param tag Tag name
       */
      XmlEntry(String tag) {
         this.fTag = tag;
         this.open = true;
      }

      /**
       * Get tag name
       * 
       * @return
       */
      public String getTag() {
         return fTag;
      }
      
      /**
       * Indicates if opening tag is still open
       * 
       * @return
       */
      boolean isOpen() {
         return open;
      }
      
      /** 
       * Close opening tag 
       */
      void close() {
         open = false;
      }
   }
   
   /**
    * Return a string of spaces of given size for indenting
    * 
    * @param length
    * 
    * @return
    */
   private static String getpadding(int length) {
      final String indentString = 
            "                                                                       " +
            "                                                                       ";
      if (length>indentString.length()) {
         throw new StringIndexOutOfBoundsException("");
      }
      return indentString.substring(0, length);
   }

   /**
    * Return a string of spaces for the current indent
    * 
    * @return
    */
   private String getIndent() {
      return getpadding(3*fLevel);
   }

   /**
    * Write an opening tag e.g. <b>&lt;tag</b>
    * 
    * @param tag Name of tag
    * 
    * @throws IOException
    */
   public void openTag(String tag) throws IOException {
      closeTagIfOpen();
      fXmlStack.push(new XmlEntry(tag));
      fWriter.write(getIndent() + "<" + tag);
      fLevel++;
      padding = 1;
   }
   /**
    * Writes close of tag if still open <b>/&gt\n</b>
    * 
    * @throws IOException
    */
   private void closeTagIfOpen() throws IOException {
      if (!fXmlStack.isEmpty()) {
         XmlEntry current = fXmlStack.peek();
         if ((current != null) && current.isOpen()) {
            current.close();
            fWriter.write(">\n");
         }
      }
   }
   
   /**
    * Write an attribute within a tag e.g. <b>attribute</b>=<b>value</b><br>
    * Checks that the tag is still open
    * 
    * @param   attribute Attribute name
    * @param   value     Attribute value
    * 
    * @throws  IOException
    */
   public void writeAttribute(String attribute, String value) throws IOException {
      XmlEntry current = fXmlStack.peek();
      if (!current.isOpen()) {
         throw new RuntimeException("Attempt to add attribute to closed tag");
      }
      String attr = attribute+"=\""+value+"\"";
      fWriter.write(getpadding(padding)+attr);
      padding = max(1,fAttrWidth-attr.length());
   }

   /**
    * get larger of two values
    * 
    * @param i
    * @param j
    * 
    * @return
    */
   private int max(int i, int j) {
      return (i>j)?i:j;
   }

   /**
    * Write an attribute within a tag e.g. <b>attribute</b>=<b>value</b><br>
    * Checks that the tag is still open
    * 
    * @param   attribute Attribute name
    * @param   value     Attribute value
    * 
    * @throws  IOException
    */
   public void writeAttribute(String attribute, long value) throws IOException {
      writeAttribute(attribute, Long.toString(value));
   }

   /**
    * Write an attribute within a tag e.g. <b>attribute</b>=<b>value</b><br>
    * Checks that the tag is still open
    * 
    * @param   attribute Attribute name
    * @param   value     Attribute value
    * 
    * @throws  IOException
    */
   public void writeAttribute(String attribute, boolean value) throws IOException {
      writeAttribute(attribute, Boolean.toString(value));
   }

   /**
    * Writes text within a tag
    * 
    * @param text Text to write
    * 
    * @throws IOException
    */
   public void writeText(String text) throws IOException {
      closeTagIfOpen();
      fWriter.write(getIndent() + "   " + text + "\n");
   }

   /**
    * Closes a tag either with <b>/&gt\n</b> or <b>&lttag /&gt\n</b> as necessary
    * 
    * @throws IOException
    */
   public void closeTag() throws IOException {
      if (fLevel==0) {
         throw new RuntimeException("XML Stack underflow");
      }
      fLevel--;
      XmlEntry entry = fXmlStack.pop();
      if (entry.isOpen()) {
         fWriter.write(" />\n");
      }
      else {
         fWriter.write(getIndent() + "</" + entry.getTag() + ">\n");
      }
   }

   /**
    * Write XML file preamble e.g.
    * 
    * <pre><code>
    *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
    *  &lt;!DOCTYPE root SYSTEM "<b><i>dtdFilename</i></b>"&gt;
    *  &lt;!-- <b><i>filename</b></i> --&gt;
    *  &lt;!-- 
    *     <b><i>description</b></i>
    *  --&gt; 
    * </code></pre>
    *
    * @param filename      Filename of actual file being written
    * @param dtdFilename   DTD file to reference (may be null)
    * @param description   Description to use in header block
    * 
    * @throws IOException
    */
   public void writeXmlFilePreamble(String filename, String dtdFilename, String description) throws IOException {
      final String headerfilePreambleTemplate = 
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
         "%s" +
         "<!-- %s -->\n" +
         "<!-- \n" +
         "   %s\n" +
         "-->\n" +
         "\n";
      String dtdLine = "";
      if (dtdFilename != null) {
         dtdLine = String.format("<!DOCTYPE root SYSTEM \"%s\">\n", dtdFilename);
      }
      description = description.replaceAll("\n", "\n   ");
      fWriter.write(String.format(headerfilePreambleTemplate, dtdLine, filename, description));
   }

   /**
    * Sanitise string for use as C identifier
    * 
    * @param s String
    * 
    * @return Sanitised string
    */
   public static String sanitise(String s) {
      return s.replaceAll("/", "_");
   }
}
   