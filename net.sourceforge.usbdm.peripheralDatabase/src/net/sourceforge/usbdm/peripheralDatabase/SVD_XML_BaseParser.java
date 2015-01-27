package net.sourceforge.usbdm.peripheralDatabase;

import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SVD_XML_BaseParser {

   static final Pattern whiteSpacePattern = Pattern.compile("\\s+");
   static final String USBDM_SVD_DEFAULT_PATH = "/DeviceData/Device.SVD/Internal";
   
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
         if (ModeControl.isStripWhiteSpace()) {
            str = whiteSpacePattern.matcher(str).replaceAll(" ");
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
            case '×':
               sb.append("x");
               break;
            case 'µ':
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

   static Field.AccessType getAccessElement(Element element) throws Exception {
      String accessName = element.getTextContent();
      if (accessName.equals("read-only")) {
         return Field.AccessType.ReadOnly;
      }
      else if (accessName.equals("write-only")) {
         return Field.AccessType.WriteOnly;
      }
      else if (accessName.equals("read-write")) {
         return Field.AccessType.ReadWrite;
      }
      else if (accessName.equals("writeOnce")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("read-writeOnce")) {
         return Field.AccessType.ReadWriteOnce;
      }
      else {
         throw new Exception("Failed to parse ACCESS', value = \'"+accessName+"\'");
      }
   }

   static Field.AccessType getModifiedWriteValuesType(Element element) throws Exception {
      String accessName = element.getTextContent();
      if (accessName.equals("oneToClear")) {
         return Field.AccessType.ReadOnly;
      }
      else if (accessName.equals("oneToSet")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("oneToToggle")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("zeroToClear")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("zeroToSet")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("zeroToToggle")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("clear")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("set")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("modify")) {
         return Field.AccessType.WriteOnce;
      }
      else {
         throw new Exception("Failed to parse ACCESS', value = \'"+accessName+"\'");
      }
   }
   
   static Field.AccessType getReadActionType(Element element) throws Exception {
      String accessName = element.getTextContent();
      if (accessName.equals("clear")) {
         return Field.AccessType.ReadOnly;
      }
      else if (accessName.equals("set")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("modify")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("modifyExternal")) {
         return Field.AccessType.WriteOnce;
      }
      else {
         throw new Exception("Failed to parse ACCESS', value = \'"+accessName+"\'");
      }
   }
   
   static Field.AccessType getEnumUsageType(Element element) throws Exception {
      String accessName = element.getTextContent();
      if (accessName.equals("read")) {
         return Field.AccessType.ReadOnly;
      }
      else if (accessName.equals("write")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("read-write")) {
         return Field.AccessType.WriteOnce;
      }
      else {
         throw new Exception("Failed to parse ACCESS', value = \'"+accessName+"\'");
      }
   }
   
   static Field.AccessType getWriteConstraintType(Element element) throws Exception {
      String accessName = element.getTextContent();
      if (accessName.equals("writeAsRead")) {
         return Field.AccessType.ReadOnly;
      }
      else if (accessName.equals("useEnumeratedValues")) {
         return Field.AccessType.WriteOnce;
      }
      else if (accessName.equals("range")) {
         return Field.AccessType.WriteOnce;
      }
      else {
         throw new Exception("Failed to parse ACCESS', value = \'"+accessName+"\'");
      }
   }
   
   static IPath xmlRootPath   = Path.EMPTY;
   static String xmlExtension = "xml";
   
   /**
    * @param fileName - Name of the file to get path to
    * 
    * @return Path to the XML file describing the device peripherals
    * 
    * @throws Exception
    */
   public static IPath getXmlFilepath(String fileName) {
      IPath path = xmlRootPath.append(fileName).addFileExtension(xmlExtension);
//      System.out.println("getXmlFilepath() => \"" + path.toOSString() + "\"");
      return path;
   }

   /**
    * Sets where to look for XML files
    * 
    * @param xmlRootPath the xmlRootPath to set
    */
   public static void setXmlRootPath(IPath xmlRootPath) {
      SVD_XML_BaseParser.xmlRootPath = xmlRootPath;
   }

   /**
    * Sets where to look for XML files
    * 
    * @param xmlRootPath the xmlRootPath to set
    */
   public static IPath getXmlRootPath() {
      return xmlRootPath;
   }

   /**
    * Sets default extension added to XML files
    * 
    * @param xmlExtension the xmlExtension to set
    */
   public static void setXmlExtension(String xmlExtension) {
      SVD_XML_BaseParser.xmlExtension = xmlExtension;
   }
   
   public SVD_XML_BaseParser() {
      IPath usbdmResourcePath = Usbdm.getResourcePath();
      if (usbdmResourcePath == null) {
         xmlRootPath = Path.EMPTY;
      }
      else {
         xmlRootPath = usbdmResourcePath.append(USBDM_SVD_DEFAULT_PATH);
      }
//      System.out.println("SVD_XML_BaseParser() xmlRootPath = \"" + xmlRootPath.toOSString() + "\"");
   }
   
   /**
    * Parse the XML file into the XML internal DOM representation
    * 
    * @param devicenameOrFilename Either a full path to device SVD file or device name (default location & default extension will be added)
    * 
    * @return DOM Document representation (or null if locating file fails)
    * 
    * @throws Exception on XML parsing error or similar unexpected event
    */
   protected static Document parseXmlFile(String devicenameOrFilename) throws Exception {
      
      // Try deviceName as full path
      IPath databasePath = new Path(devicenameOrFilename);
//      System.err.println("SVD_XML_BaseParser.parseXmlFile()" + devicenameOrFilename);
      if (!databasePath.toFile().exists()) {
         // Retry using deviceName as simply name
         databasePath = getXmlFilepath(devicenameOrFilename);
      }
      if (!databasePath.toFile().exists()) {
         // Retry after stripping speed grade e.g. MK20DX128M5 => MK20DX128
         devicenameOrFilename = devicenameOrFilename.replaceAll("^(.*)M\\d$", "$1");
         databasePath = getXmlFilepath(devicenameOrFilename);
      }
      if (!databasePath.toFile().exists()) {
         // Retry after stripping speed grade e.g. MK20DX128M5Z => MK20DX128Z
         devicenameOrFilename = devicenameOrFilename.replaceAll("^(.*)M\\dZ$", "$1Z");
         databasePath = getXmlFilepath(devicenameOrFilename);
      }
      if (!databasePath.toFile().exists()) {
         return null;
      }
      // Get the factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();

      //  Parse using builder to get DOM representation of the XML file
      return db.parse(databasePath.toOSString());
   }
   
   /**
    * Parse the XML file into the XML internal DOM representation
    * 
    * @param databasePath Path to XML file to process
    * 
    * @return DOM Document representation
    * 
    * @throws Exception 
    */
   protected static Document parseXmlFile(IPath databasePath) throws Exception {
      
      if (!databasePath.toFile().exists()) {
         throw new Exception("Device file not found : \'"+databasePath.toOSString()+"\'");
      }

      // Get the factory
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();

      //  Parse using builder to get DOM representation of the XML file
//      System.out.println("parseXmlFile()"+databasePath.toOSString());
      return db.parse(databasePath.toOSString());
   }
   
}
