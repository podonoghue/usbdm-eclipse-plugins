package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

public class Register extends Cluster implements Cloneable {

   private String             description;
   private String             alternateGroup;
   private ArrayList<Field>   fields;
   private boolean            deleted;
   private boolean            sorted;
   private long               width;
   
   public Register(Peripheral owner) {
      super(owner);
      description        = "";
      alternateGroup     = "";
      fields             = new ArrayList<Field>();
      deleted            = false;
      if (owner != null) {
         width          = owner.getWidth();
      }
      else {
         width          =  32;
      }
   }
   
   /**
    * Returns a relatively shallow copy of the peripheral
    * Only the following should be changed:
    *    - name
    *    - baseAddress
    *    - addressBlock
    *    - prependToName
    *    - appendToName
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {

      // Make shallow copy
      Register clone = (Register) super.clone();

      return clone;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return String.format("Register[%s]", getName());
   }

   public void sortFields() {
      if (sorted) {
         return;
      }
      Collections.sort(fields, new Comparator<Field>() {
         @Override
         public int compare(Field field1, Field field2) {
            if (field2.getBitOffset() < field1.getBitOffset()) {
               return 1;
            }
            else if (field2.getBitOffset() > field1.getBitOffset()) {
               return -1;
            }
            return (int)(field2.getBitwidth() - field1.getBitwidth());
         }
      });
      sorted = true;
   }

   /**
    * Checks if this register description agrees with the another
    * 
    * @param other register to check against
    * @return
    */
   public boolean equivalent(Register other) {
      boolean verbose = false;//name.equalsIgnoreCase("FCSR") && other.getName().equalsIgnoreCase("FCSR");
      if (!equivalent(other, "(.+)()(.*)", "(.+)()(.*)")) {
         if (verbose) {
            System.err.println("Comparing structure \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      if (  (this.getAddressOffset()      != other.getAddressOffset()) ||
            (this.getDimension()          != other.getDimension()) ||
            (this.getDimensionIncrement() != other.getDimensionIncrement()) ||
            !this.getName().equals(other.getName())) {
         if (verbose) {
            System.err.println("Comparing base \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      return true;
   }
   
   /** Determines if two registers are equivalent
    * 
    * @param other      Other enumeration to check
    * @param pattern1   Pattern to apply to name & description of self
    * @param pattern2   Pattern to apply to name & description of other
    * 
    * @note Patterns are applied recursively to enumerations etc.
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Register other, String pattern1, String pattern2) {
      boolean verbose = false; //name.equalsIgnoreCase("FCSR") && other.getName().equalsIgnoreCase("FCSR");
      boolean rv = 
            (this.getWidth()        == other.getWidth()) &&
            (this.getAccessType()  == other.getAccessType()) &&
            (this.getResetValue()  == other.getResetValue()) &&
            (this.getResetMask()   == other.getResetMask()) &&
            (this.fields.size()    == other.fields.size()) &&
            (this.getDimension()   == other.getDimension()) ; 
      if (!rv) {
         if (verbose) {
            System.err.println("Comparing simple structure \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      // Assume name matched already
//      String d1 = getName().replaceFirst(pattern1, "$1%s$3");
//      String d2 = other.getName().replaceFirst(pattern2, "$1%s$3");
//      if (!d1.equalsIgnoreCase(d2)) {
//         return false;
//      }
      String d1 = getDescription().replaceFirst(pattern1, "$1%s$3");
      String d2 = other.getDescription().replaceFirst(pattern2, "$1%s$3");
      if (!d1.equalsIgnoreCase(d2)) {
         return false;
      }
      sortFields();
      other.sortFields();
      for (int index=0; index<fields.size(); index++) {
            if (!fields.get(index).equivalent(other.fields.get(index), pattern1, pattern2)) {
               return false;
            }
      }
      return true;
   }
   
   /**
    *  Checks the access type of all fields and propagates to register if identical
    *  and  more restrictive
    * @throws Exception 
    */
   public void checkAccess() throws Exception {
      AccessType accessType = null;
      for (Field field : fields) {
         AccessType regAccess = field.getAccessType();
         if (accessType == null) {
            accessType = regAccess;
            continue;
         }
         if (accessType != regAccess) {
            // Differing access - quit
            return;
         }
      }
      if (accessType != null) {
         if (this.getAccessType() == null) {
            this.setAccessType(accessType);
         }
         else {
            this.setAccessType(accessType.and(this.getAccessType()));
            if (this.getAccessType() == null) {
               throw new Exception("Impossible combination of access types");
            }
         }
      }
   }
   
   @Override
   public String getBaseName() {
      return String.format(getName(), "");
   }

   public String getDescription() {
      return description;
   }

   public String getDescription(int index) throws Exception {
      return this.format(getDescription(), index);
   }

   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }

   public String getCDescription(int index) throws Exception {
      return SVD_XML_BaseParser.unEscapeString(getDescription(index));
   }
   
   public void setDescription(String description) {
      this.description = getSanitizedDescription(description.trim());
   }

   public String getAlternateGroup() {
      return alternateGroup;
   }

   public void setAlternateGroup(String alternateGroup) {
      this.alternateGroup = alternateGroup;
   }

   @Override
   public long getTotalSizeInBytes() {
      return ((getWidth()+7)/8) * ((getDimension()>0)?getDimension():1);
   }

   public void addField(Field field) {
      fields.add(0,field);
   }
   
   public ArrayList<Field> getFields() {
      return fields;
   }

   public ArrayList<Field> getSortedFields() {
      sortFields();
      return fields;
   }

   /**
    * Returns width of the register
    * 
    * @return width in bits
    */
   @Override
   public long getWidth() {
      return width;
   }

   /**
    * Sets width of register
    * 
    * @param width in bits
    */
   public void setWidth(long width) {
      this.width = width;
      if (width>0) {
         // Trim reset mask to size of element
         setResetMask(getResetMask() & ((1L<<width)-1));
      }
   }

   /**
    * @return the deleted
    */
   @Override
   public boolean isDeleted() {
      return deleted;
   }

   /**
    * @param deleted the deleted to set
    */
   @Override
   public void setDeleted(boolean deleted) {
      this.deleted = deleted;
   }

   @Override
   public void report() throws Exception {
         if (getDimension() > 0) {
            for (int dim =0; dim < getDimension(); dim++) {
               System.out.println(String.format("       Register \"%s\" [@0x%08X, W=%d, RV=0x%08X, RM=0x%08X], Description = \"%s\"", 
                     getName(dim), getAddressOffset(dim), getWidth(), getResetValue(), getResetMask(), getDescription()));
               for (Field field : fields) {
                  field.report();
               }
            }
         }
         else {
            System.out.println(String.format("       Register \"%s\" [@0x%08X, W=%d, RV=0x%08X, RM=0x%08X], Description = \"%s\"", 
                  getName(), getAddressOffset(), getWidth(), getResetValue(), getResetMask(), getDescription()));
            for (Field field : fields) {
               field.report();
            }
         }
   }

   static final String fill = "                                                     ";
   
   public void writederivedfromSVD(PrintWriter writer, boolean standardFormat, Peripheral owner, int level) throws Exception {
      String indent = fill.substring(0, level);
      writer.println(String.format(   indent+"            <register derivedFrom=\"%s\">",          getDerivedFrom().getName()));

      Cluster derivedCluster = getDerivedFrom();
      if (!(derivedCluster instanceof Register)) {
         throw new Exception("Only support derived registers");
      }
      Register derived = (Register) derivedCluster;

      writer.println(String.format(   indent+"               <name>%s</name>",                     SVD_XML_BaseParser.escapeString(getName())));
      
      if ((getDescription() != null) && !getDescription().equals(derived.getDescription())) {
         writer.println(String.format(indent+"               <description>%s</description>",       SVD_XML_BaseParser.escapeString(getDescription())));
      }
      writer.println(String.format(   indent+"               <addressOffset>0x%X</addressOffset>", getAddressOffset()));
      if (!getAccessType().equals(derived.getAccessType())) {
         writer.println(String.format(indent+"               <access>%s</access>",                 getAccessType().getPrettyName()));
      }
      if (getResetValue() != derived.getResetValue()) {
         writer.println(String.format(indent+"               <resetValue>0x%X</resetValue>",       getResetValue()));
      }
      if (getResetMask() != derived.getResetMask()) {
         writer.println(String.format(indent+"               <resetMask>0x%X</resetMask>",         getResetMask()));
      }
      if (getDimensionIndexes() != derived.getDimensionIndexes()) {
         writeDimensionList(writer, indent);
      }
      writer.println(                 indent+"            </register>");
   }
   
   void writeDimensionList(PrintWriter writer, String indent) {
      if (getDimension()>0) {
         writer.println(String.format(indent+"<dim>%d</dim>",                       getDimension()));
         writer.println(String.format(indent+"<dimIncrement>%d</dimIncrement>",     getDimensionIncrement()));
         writer.print(String.format(  indent+"<dimIndex>"));
         boolean doComma = false;
         for (String s : getDimensionIndexes()) {
            if (doComma) {
               writer.print(",");
            }
            doComma = true;
            writer.print(SVD_XML_BaseParser.escapeString(s));
         }
         writer.println(String.format(indent+"</dimIndex>"));
      }
   }
   
   /**
    *   Writes the Register description to file in a SVF format
    *   
    *  @param writer          The destination for the XML
    *  @param standardFormat  Suppresses some non-standard size optimisations 
    *  @param owner           The owner - This is used to reduce the size by inheriting default values
    *  
    *  @throws Exception 
    */
   @Override
   public void writeSVD(PrintWriter writer, boolean standardFormat, Peripheral owner, int indent) throws Exception {
      
      if ((getDerivedFrom() != null) && !isExpandDerivedRegisters()) {
         writederivedfromSVD(writer, standardFormat, owner, indent);
         return;
      }
//      final String indenter = RegisterUnion.getIndent(indent);
      final String indenter = RegisterUnion.getIndent(indent);
      writer.println(                 indenter+"<register>");
      
      writer.println(String.format(   indenter+"   <name>%s</name>",                     SVD_XML_BaseParser.escapeString(getName())));

      if ((getDescription() != null) && (getDescription().length() > 0)) {
         writer.println(String.format(indenter+"   <description>%s</description>",       SVD_XML_BaseParser.escapeString(getDescription())));
      }
      writer.println(String.format(   indenter+"   <addressOffset>0x%X</addressOffset>", getAddressOffset()));
      if ((owner == null) || (owner.getWidth() != getWidth()) || (fields.size() == 0)) {
         writer.println(String.format(indenter+"   <size>%d</size>",                     getWidth()));
      }
      if ((owner == null) || (owner.getAccessType() != getAccessType()) || (fields.size() == 0)) {
         writer.println(String.format(indenter+"   <access>%s</access>",                 getAccessType().getPrettyName()));
      }
      if ((owner == null) || (owner.getResetValue() != getResetValue())) {
         writer.println(String.format(indenter+"   <resetValue>0x%X</resetValue>",       getResetValue()));
      }
      if ((owner == null) || (owner.getResetMask() != getResetMask())) {
         writer.println(String.format(indenter+"   <resetMask>0x%X</resetMask>",         getResetMask()));
      }

      writeDimensionList(writer, indenter);

      if ((getFields() != null) && (getFields().size() > 0)) {
         writer.println(              indenter+"   <fields>");
         sortFields();
         for (Field field : getFields()) {
            field.writeSVD(writer, standardFormat, this, indent+6);
         }
         writer.println(              indenter+"   </fields>");
      }
      writer.println(                 indenter+"</register>");
   }

//   static final String RegisterStructFormat = "%-4s %-9s %-30s";

   private String getCSizeName(long size) throws Exception {
      switch ((((int)size)+7)/8) {
      case 1 : return "uint8_t";
      case 2 : return "uint16_t";
      case 4 : return "uint32_t";
      case 8 : return "uint64_t";
      }
      throw new Exception("Unknown size in register : "+size);
   }
   
   public final static String lineFormat = "%-47s /*!< %04X: %-60s */\n";

   /**
    * Writes C code for Register as STRUCT element e.g. "uint32_t registerName;"
    * 
    * @param writer
    * @param devicePeripherals
    */
   @Override
   public void writeHeaderFileDeclaration(PrintWriter writer, int indent, Peripheral peripheral, long offset) throws Exception {

      String accessPrefix = "__IO";
      switch (getAccessType()) {
         case ReadOnly      : accessPrefix = "__I";  break;
         case ReadWrite     : accessPrefix = "__IO"; break;
         case ReadWriteOnce : accessPrefix = "__IO"; break;
         case WriteOnce     : accessPrefix = "__IO"; break;
         case WriteOnly     : accessPrefix = "__O";  break;
         default            : accessPrefix = "__IO"; break;
      }
      final String indenter = RegisterUnion.getIndent(indent);
      StringBuffer line = new StringBuffer(120);
      line.append(indenter+String.format("%-4s %-9s %s", 
                                accessPrefix,
                                getCSizeName(getWidth()), 
                                getBaseName()));
      if (getDimension()>0) {
         if (checkSequential()) {
            line.append(String.format("[%d];", getDimension()));
            writer.print(String.format(lineFormat, line.toString(), offset, truncateAtNewline(format(getCDescription(),-1))));
         }
         else {
            String baseLine = line.toString();
            for (int index=0; index< getDimension(); index++) {
               String sIndex = getDimensionIndexes().get(index);
               int delimeter = sIndex.indexOf(':');
               if (delimeter > 0) {
                  sIndex = sIndex.substring(0, delimeter);
               }
               writer.print(String.format(lineFormat, baseLine+sIndex+";", 
                     offset+(index*getDimensionIncrement()), truncateAtNewline(format(getCDescription(index), index))));
            }
//            for (String index : getDimensionIndexes()) {
//               int delimiter = index.indexOf(':');
//               if (delimiter > 0) {
//                  index = index.substring(0, delimiter);
//               }
//               writer.print(String.format("%-47s /*!< %-60s*/\n", baseLine+index+";", truncateAtNewline(getDescription(3))));
//            }
         }
      }
      else {
         line.append(';');
         writer.print(String.format(lineFormat, line.toString(), offset, truncateAtNewline(getCDescription())));
      }
   }

   /**
    * Writes a macro to allow 'Freescale' style access to this register
    * e.g. "#define I2S0_CR3 (I2S0->CR[3])"
    * 
    * @param  writer
    * @param  devicePeripherals
    * @throws Exception
    */
   @Override
   public void writeHeaderFileRegisterMacro(PrintWriter writer, Peripheral peripheral) throws Exception {
      if (getDimension()>0) {
         for(int index=0; index<getDimension(); index++) {
            String name = peripheral.getName()+"_"+getName(index);
            name = getMappedRegisterMacroName(name);
            if (name.length() == 0) {
               return;
            }
            if (checkSequential()) {
               writer.print(String.format("#define %-30s (%s->%s[%d])\n", name, peripheral.getName(), getBaseName(), index));
            }
            else {
               writer.print(String.format("#define %-30s (%s->%s)\n", name, peripheral.getName(), getName(index)));
            }
         }
      }
      else {
         String name = peripheral.getName()+"_"+getName();
         name = getMappedRegisterMacroName(name);
         if (name.length() == 0) {
            return;
         }
         writer.print(String.format("#define %-30s (%s->%s)\n", name, peripheral.getName(), getName()));
      }
   }

   static final String registerMacroPrefix = 
         "\n/* ------- %-40s ------ */\n";

   /**
    * Writes a set of macros to allow convenient access to the register fields
    * e.g. "#define PERIPHERAL_FIELD(x)  (((x)<<FIELD_OFFSET)&FIELD_MASK)"
    * 
    * @param  writer
    * @param  devicePeripherals
    * @throws Exception
    */
   @Override
   public void writeHeaderFileFieldMacros(PrintWriter writer, Peripheral peripheral) throws Exception {
      if (getDerivedFrom() != null) {
         return;
      }
      writer.print(String.format(registerMacroPrefix, peripheral.getName()+"_"+getBaseName()));
      String registerPrefix = (isMapFreescaleCommonNames())?peripheral.getPrependToName()+"_":peripheral.getName()+"_";
      sortFields();
      for (Field field : fields) {
         field.writeHeaderFileFieldMacros(writer, registerPrefix+getBaseName());
      }
   }

}
