package net.sourceforge.usbdm.dialogues;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XML_BaseParser {

   static final Pattern whiteSpacePattern = Pattern.compile("\\s+");

   /**
    * Escapes a string for writing to an XML file
    * 
    * @param str
    * @return
    */
   public static String escapeString(String str) {
      if (str == null || str.length() == 0) {
         return "";
      }
      StringBuffer sb = new StringBuffer();
      int len = str.length();
      for (int i = 0; i < len; i++) {
         char ch = str.charAt(i);
         switch (ch) {
         case '<':
            sb.append("&lt;");
            break;
         case '>':
            sb.append("&gt;");
            break;
         case '&':
            sb.append("&amp;");
            break;
         case '"':
            sb.append("&quot;");
            break;
         case '\'':
            sb.append("&apos;");
            break;
         case '\327':
            sb.append("x");
            break;
         case '\265':
            sb.append("u");
            break;
            // Used once-off to change description formats
            //            case '\n':
            //               sb.append("\\");
            //               sb.append("n");
            //               sb.append("\n");
            //               break;
         default:
            // This is really crude!
            if (Character.isLetterOrDigit(ch) || Character.isWhitespace(ch)) {
               sb.append(ch);
            }
            else if ((ch >= 0x20) && (ch <= 0x7F)) {
               sb.append(ch);
            }
            else {
               sb.append('?');
            }
            break;
         }
      }
      return sb.toString();
   }

   /**
    * Converts a string (usually a description) into plain text for use
    * 
    * @param str
    * @return
    */
   public static String unEscapeString(String str) {
      if (str == null || str.length() == 0) {
         return "";
      }
      StringBuffer sb = new StringBuffer();
      int len = str.length();
      boolean escapeMode             = false; // Escapes next character
      boolean leadingWhitespaceMode  = false; // Used to swallow consecutive whitespace
      for (int i = 0; i < len; i++) {
         char ch = str.charAt(i);
         if (escapeMode) {
            escapeMode = false;
            switch (ch) {
            case 'n':
               sb.append('\n');
               leadingWhitespaceMode = true;
               break;
            case 't':
               sb.append('\t');
               break;
            case '\\':
               sb.append('\\');
               break;
            default:
               // just treat as regular characters
               sb.append('\\');
               sb.append(ch);
               break;
            }
            continue;
         }
         if (Character.isWhitespace(ch) && leadingWhitespaceMode) {
            // Swallow runs of white space
            continue;
         }
         leadingWhitespaceMode = false;
         switch (ch) {
         case '\n':
            sb.append('\n');
            leadingWhitespaceMode = true;
            break;
         case '\\':
            escapeMode = true;
            break;
         default:
            // This is really crude!
            if (Character.isLetterOrDigit(ch) || Character.isWhitespace(ch)) {
               sb.append(ch);
            }
            else if ((ch >= 0x20) && (ch <= 0x7F)) {
               sb.append(ch);
            }
            else {
               sb.append('?');
            }
            break;
         }
      }
      return sb.toString();
   }

   /**
    * Parses number element of form 0bBBBB or 0xXXXX
    * 
    * @param element Element to parse
    * 
    * @return numeric value
    * 
    * @throws Exception
    */
   protected static long getNumberElement(Element element) throws Exception {

      String s = element.getTextContent();
      if ((s == null) || (s.length()==0)) {
         throw new Exception("Text not found");
      }
      if (s.startsWith("0b") || s.startsWith("0B")) {
         s = s.substring(2);
         return Long.parseLong(s, 2);
      }
      if (s.startsWith("0x") || s.startsWith("0X")) {
         s = s.substring(2);
         return Long.parseLong(s, 16);
      }
      return Long.parseLong(s, 10);
   }

   /**
    * @param element - XML element to parse
    * 
    * @return integer
    * 
    * @throws Exception
    */
   protected static String stripQuotes(String s) {
      final Pattern x = Pattern.compile("^(\\s)*\\\"(.*)\\\"(\\s)*$");

      if (s != null) {
         // Strip enclosing quotes and white space
         s = x.matcher(s).replaceAll("$2");
      }
      return s;
   }

   /**
    * @param element - XML element to parse
    * 
    * @return integer
    * 
    * @throws Exception
    */
   protected static long getIntElement(Node element) throws Exception {

      String s = stripQuotes(element.getTextContent());

      if ((s == null) || (s.length()==0)) {
         throw new Exception("Text not found");
      }
      long value      = 0;
      long multiplier = 1;
      int kIndex = s.lastIndexOf('K');
      int mIndex = s.lastIndexOf('M');
      if (kIndex>0) {
         //         System.out.println("getIntAttribute("+s+"), K found");
         s = s.substring(0, kIndex);
         multiplier = 1024;
         //         System.out.println("getIntAttribute("+s+"), K found");
      }
      if (mIndex>0) {
         //         System.out.println("getIntAttribute("+s+"), M found");
         s = s.substring(0, mIndex);
         multiplier = 1024*1024;
         //         System.out.println("getIntAttribute("+s+"), M found");
      }
      try {
         value = multiplier*Long.decode(s);
      } catch (NumberFormatException e) {
         //         System.out.println("getIntAttribute("+s+"), failed");
         e.printStackTrace();
         throw new Exception("Failed to parse Int text', value = \'"+element.getTextContent()+"\'");
         //         throw e;
      }
      return value;
   }

   /**
    * @param element
    * @param name
    * @return
    * @throws Exception
    */
   protected static long getIntAttribute(Element element, String name) throws Exception {
      String s = element.getAttribute(name);
      if ((s == null) || (s.length()==0)) {
         throw new Exception("Attribute \'"+name+"\'not found");
      }
      long value      = 0;
      long multiplier = 1;
      int kIndex = s.lastIndexOf('K');
      int mIndex = s.lastIndexOf('M');
      if (kIndex>0) {
         //         System.out.println("getIntAttribute("+s+"), K found");
         s = s.substring(0, kIndex);
         multiplier = 1024;
         //         System.out.println("getIntAttribute("+s+"), K found");
      }
      if (mIndex>0) {
         //         System.out.println("getIntAttribute("+s+"), M found");
         s = s.substring(0, mIndex);
         multiplier = 1024*1024;
         //         System.out.println("getIntAttribute("+s+"), M found");
      }
      try {
         value = multiplier*Long.decode(s);
      } catch (NumberFormatException e) {
         //         System.out.println("getIntAttribute("+s+"), failed");
         e.printStackTrace();
         throw new Exception("Failed to parse Int Attribute \'"+name+"\', value = \'"+element.getAttribute(name)+"\'");
         //         throw e;
      }
      return value;
   }

   /**
    * Parse the XML file into the XML internal DOM representation
    * 
    * @param path path to device SVD file
    * 
    * @return DOM Document representation (or null if locating file fails)
    * 
    * @throws Exception on XML parsing error or similar unexpected event
    */
   protected static Document parseXmlFile(Path path) throws Exception {
      // Get the factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      
      dbf.setXIncludeAware(true);
      dbf.setNamespaceAware(true);

      DocumentBuilder db = dbf.newDocumentBuilder();

      InputStream is  = null;
      Document    doc = null;

      try {
         is = Files.newInputStream(path);
         //  Parse using builder to get DOM representation of the XML file
         doc = db.parse(is, path.toString());
      }
      finally {
         if (is != null) {
            is.close();
         }
      }
      return doc;
   }
}
