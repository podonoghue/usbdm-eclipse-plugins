package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;

public class Enumeration extends ModeControl {

   private String name;
   private String description;
   private long   value;
   private long   mask;
   private String sValue;

   public Enumeration() {
      name        = "";
      description = "";
      value       = 0;
      mask        = 0;
   }

   /** Determines if two enumerations are equivalent
    * 
    * @param other      Other enumeration to check
    * @param pattern1   Pattern to apply to description of self
    * @param pattern2   Pattern to apply to description of other
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Enumeration other, String pattern1, String pattern2) {
      boolean rv = (this.mask == other.mask) &&
                   (this.value == other.value);
      if (!rv) {
         return rv;
      }
      
      if (getDescription().equalsIgnoreCase(other.getDescription())) {
         return true;
      }
      String n1;
      String n2;
      n1 = getDescription().replaceFirst(pattern1, "$1%s$3");
      n2 = other.getDescription().replaceFirst(pattern2, "$1%s$3");
      return n1.equalsIgnoreCase(n2);
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return String.format("Enumeration[%s]", getName());
   }

   /** Determines if two enumerations are equivalent
    * 
    * @param other      Other enumeration to check
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Enumeration other) {
      return this.name.equals(other.name) &&
             this.description.equals(other.description) &&
             (this.mask == other.mask) &&
             (this.value == other.value);
   }
   
   public String getName() {
      if (name != null) {
         return name;
      }
      else if (sValue == null) {
         return "default";
      }
      return sValue;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }
   
   public void setDescription(String description) {
      this.description = getSanitizedDescription(description.trim());
   }

   public long getValue() {
      return value;
   }

   public long getMask() {
      return mask;
   }

   public void setValue(String string) throws Exception {
      sValue = string;
      mask   = 0;
      value  = 0;
      if (string.startsWith("0b")) {
         string = string.substring(2);
         for (int index = 0; index <string.length(); index++) {
            int bitIndex = string.length()-index-1;
            switch (string.charAt(index)) {
            case '0': value |= (0<<bitIndex); mask |= (1<<bitIndex); break; 
            case '1': value |= (1<<bitIndex); mask |= (1<<bitIndex); break; 
            case 'x': break; 
            case 'X': break; 
            default: throw new Exception("Unexpected enumeration value \""+string.charAt(index)+"\"");
            }
         }
      }
      else if (string.startsWith("#")) {
         string = string.substring(1);
         for (int index = 0; index <string.length(); index++) {
            int bitIndex = string.length()-index-1;
            switch (string.charAt(index)) {
            case '0': value |= (0<<bitIndex); mask |= (1<<bitIndex); break; 
            case '1': value |= (1<<bitIndex); mask |= (1<<bitIndex); break; 
            case 'x': break; 
            case 'X': break; 
            default: throw new Exception("Unexpected enumeration value \""+string.charAt(index)+"\"");
            }
         }
      }
      else if (string.startsWith("0x")||string.startsWith("0X")) {
         string = string.substring(2);
         for (int index = 0; index <string.length(); index++) {
            int hexIndex = 4*(string.length()-index-1);
            char ch = string.charAt(index);
            switch (ch) {
            case '0': 
            case '1': 
            case '2': 
            case '3': 
            case '4': 
            case '5': 
            case '6': 
            case '7': 
            case '8': 
            case '9': 
               value |= ((ch-'0')<<(4*hexIndex));
               mask  |= (0xF<<(4*hexIndex)); 
               break; 
            case 'a': 
            case 'b': 
            case 'c': 
            case 'd': 
            case 'e': 
            case 'f': 
               value |= ((ch-'a'+0xA)<<(4*hexIndex));
               mask  |= (0xF<<(4*hexIndex)); 
               break; 
            case 'A': 
            case 'B': 
            case 'C': 
            case 'D': 
            case 'E': 
            case 'F': 
               value |= ((ch-'A'+0xA)<<(4*hexIndex));
               mask  |= (0xF<<(4*hexIndex)); 
               break; 
            case 'x': break; 
            case 'X': break; 
            default: throw new Exception("Unexpected enumeration value \""+string.charAt(index)+"\"");
            }
         }
      }
      else {
         value = Long.decode(string);
         mask  = 0xFFFFFFFF;
      }
      // Sanity check results
      assert (mask != 0)             : String.format("Mask calculation failed, input = \'%s\' => V=0x%08X, M=0x%08X", string, value, mask);
      assert ((value&mask) == value) : String.format("Mask calculation failed, input = \'%s\' => V=0x%08X, M=0x%08X", string, value, mask);
   }

   public void setAsDefault() throws Exception {
      // This causes this field to match any number
      // It is assumed that it is check LAST!
      if (mask != 0) {
         throw new Exception("Enumeration already has value when set as default");
      }
      this.mask  = 0;
      this.value = 0;
   }
   
   public boolean isSelected(long value) {
      return (this.value == (value&this.mask));
   }
   
   public void report() {
      System.out.println(String.format("             Enumeration \"%s\", V=0x%08X, M=0x%08X, description = \"%s\"",
            getName(), getValue(), getMask(), getDescription())); 
   }

   /**
    * 
    *  @param writer          The destination for the XML
    *  @param standardFormat Suppresses some non-standard size optimizations 
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat, int indent) {
      final String indenter = RegisterUnion.getIndent(indent);

      writer.println(                 indenter+"<enumeratedValue>");
      writer.println(String.format(   indenter+"   <name>%s</name>",               SVD_XML_BaseParser.escapeString(getName())));
      if ((getDescription() != null) && (getDescription().length() > 0)) {
         writer.println(String.format(indenter+"   <description>%s</description>", SVD_XML_BaseParser.escapeString(getDescription())));
      }
      if (mask == 0) {
         writer.println(              indenter+"   <default>true</default>" );
      }
      else {
         writer.println(String.format(indenter+"   <value>%s</value>",             SVD_XML_BaseParser.escapeString(sValue)));
      }
      writer.println(                 indenter+"</enumeratedValue>");
   }
}
