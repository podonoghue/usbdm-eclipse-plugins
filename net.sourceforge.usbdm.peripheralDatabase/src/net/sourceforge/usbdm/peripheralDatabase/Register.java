package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

public class Register extends Cluster implements Cloneable {

   private String             description;
   private String             alternateGroup;
   private ArrayList<Field>   fields;
   private boolean            deleted;
   private boolean            sorted;
   private long               width;
   private boolean            hidden;
   private Cluster            cluster;
   
   public Register(Peripheral owner, Cluster cluster) {
      super(owner);
      this.cluster       = cluster;
      description        = "";
      alternateGroup     = "";
      fields             = new ArrayList<Field>();
      deleted            = false;
      hidden             = false;
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
    *  
    * @throws Exception 
    */
   public void checkFieldAccess() throws Exception {
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
   
   /**
    *  Checks the fields don't overlap or exceed register dimensions
    *  
    * @throws Exception 
    */
   public void checkFieldDimensions() throws Exception {
      long bitsUsed = 0L;
      for (Field field : fields) {
         if (field.getAccessType() == null) {
            field.setAccessType(getAccessType());
         }
         // Check for field exceeds register
         if ((field.getBitOffset()+field.getBitwidth()) > getWidth()) {
            throw new Exception(String.format("Bit field \'%s\' outside register in \'%s\'", field.getName(), getName()));
         }
         if (!field.isIgnoreOverlap()) {
            // Check for field overlap
            long bitsUsedThisField = 0;
            for (long i=field.getBitOffset(); i<(field.getBitOffset()+field.getBitwidth()); i++) {
               bitsUsedThisField |= 1L<<i;
            }
            if ((bitsUsed&bitsUsedThisField) != 0) {
               throw new Exception(String.format("Bit field \'%s\' overlaps in register \'%s\'", field.getName(), getName()));
            }
            bitsUsed |= bitsUsedThisField;
         }
      }
   }
   
   @Override
   public String format(String format, int index) {
      return super.format(format, index);
   }

   /**
    * Formats a string using register number and modifier substitution<br>
    * The format may contain:
    * <ul>
    * <li>%c = cluster index as a number
    * <li>%s = register index as a number
    * <li>%m = dimensionIndex modifier (a text string)
    * </ul>
    * @param format Format string to use
    * @param index  Index used to select dimensionIndex & modifier
    * 
    * @return
    * @throws Exception
    */
   public String format(String format, int clusterIndex, int registerIndex) {
      final Pattern pattern = Pattern.compile("(^.*):(.*$)");
      String sRegisterIndex   = "";
      if (registerIndex>=0) {
         ArrayList<String> dimensionIndexes = getDimensionIndexes();
         if (dimensionIndexes != null) {
            String dimensionIndex = dimensionIndexes.get(registerIndex);
            Matcher matcher       = pattern.matcher(dimensionIndex);
            if (matcher.matches()) {
               sRegisterIndex   = matcher.replaceAll("$1");
            }
            else {
               sRegisterIndex = dimensionIndex;
            }
         }
         else {
            sRegisterIndex = Integer.toString(registerIndex);
         }
      }
      format = format.replaceAll("%s", sRegisterIndex);
      format = format.replaceAll("%c", "%s");
      format = cluster.format(format, clusterIndex);
      return SVD_XML_BaseParser.unEscapeString(format);
   }

   /**
    * Returns the description of the register with %c and %s substitution
    * 
    * @param clusterIndex    Used for %c index
    * @param registerIndex   Used for %s index
    * 
    * @return        String description
    * 
    * @throws Exception
    */
   public String getCDescription(int clusterIndex, int registerIndex) {
      return SVD_XML_BaseParser.unEscapeString(format(getDescription(), clusterIndex, registerIndex));
   }

   /**
    * Returns the description of the register with %s substitution
    * 
    * @param index   Used for %s index
    * 
    * @return        String description
    * 
    * @throws Exception
    */
   public String getCDescription(int registerIndex) {
      return SVD_XML_BaseParser.unEscapeString(format(getDescription(), registerIndex));
   }

   /**
    * Returns the description of the register with %s substitution
    * Sanitised for use in C code
    * 
    * @param index   Used for %s index
    * 
    * @return        String description
    * @throws Exception
    */
   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }

   /**
    * Set register description
    * 
    * @param description
    */
   public void setDescription(String description) {
      this.description = getSanitizedDescription(description.trim());
   }

   /**
    * Returns the description of the register
    * 
    * @return String description
    */
   public String getDescription() {
      return description;
   }

   /**
    * 
    * @return
    */
   public String getAlternateGroup() {
      return alternateGroup;
   }

   /**
    * 
    * @param alternateGroup
    */
   public void setAlternateGroup(String alternateGroup) {
      this.alternateGroup = alternateGroup;
   }

   @Override
   public long getTotalSizeInBytes() {
      if (getDimension() == 0) {
         return getElementSizeInBytes();
      }
      return getDimensionIncrement() * getDimension();
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

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.Cluster#addAddressBlocks(net.sourceforge.usbdm.peripheralDatabase.Peripheral.AddressBlocksMerger)
    */
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger, long addressOffset) throws Exception {
//      System.err.println(String.format("Register.addAddressBlocks(%s) addressOffset = 0x%04X, offset = 0x%04X", getName(), addressOffset, addressOffset+getAddressOffset()));
      addressOffset += getAddressOffset();
      if (getDimension()== 0) {
         // Simple array
         addressBlocksMerger.addBlock(addressOffset, getWidth(), getAccessType());
         return;
      }
      for (int index=0; index<getDimension(); index++) {
         addressBlocksMerger.addBlock(addressOffset, getWidth(), getAccessType());
         addressOffset += getDimensionIncrement();
      }
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.Cluster#addAddressBlocks(net.sourceforge.usbdm.peripheralDatabase.Peripheral.AddressBlocksMerger)
    */
   @Override
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger) throws Exception {
      addAddressBlocks(addressBlocksMerger, 0);
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
   
   /**
    *   Writes the Register description to file in a SVF format
    *   
    *  @param writer          The destination for the XML
    *  @param standardFormat  Suppresses some non-standard size optimisations 
    *  @param level           Level of indenting
    *  @param owner           The owner - This is used to reduce the size by inheriting default values
    *  
    *  @throws Exception 
    */
   public void writederivedfromSVD(PrintWriter writer, boolean standardFormat, Peripheral owner, int level) throws Exception {
      String indent = fill.substring(0, level);
      writer.print(String.format(   indent+"<register derivedFrom=\"%s\">",          getDerivedFrom().getName()));

      Cluster derivedCluster = getDerivedFrom();
      if (!(derivedCluster instanceof Register)) {
         throw new Exception("Only support derived registers");
      }
      Register derived = (Register) derivedCluster;

      if (getDimensionIndexes() != derived.getDimensionIndexes()) {
         writeDimensionList(writer, indent);
      }
      writer.print(String.format(" <name>%s</name>",                     SVD_XML_BaseParser.escapeString(getName())));
      
      if (!getDescription().equals(derived.getDescription())) {
         writer.print(String.format(" <description>%s</description>",       SVD_XML_BaseParser.escapeString(getDescription())));
      }
      writer.print(String.format(" <addressOffset>0x%X</addressOffset>", getAddressOffset()));
      if (!getAccessType().equals(derived.getAccessType())) {
         writer.print(String.format(" <access>%s</access>",                 getAccessType().getPrettyName()));
      }
      if (getResetValue() != derived.getResetValue()) {
         writer.print(String.format(" <resetValue>0x%X</resetValue>",       getResetValue()));
      }
      if (getResetMask() != derived.getResetMask()) {
         writer.print(String.format(" <resetMask>0x%X</resetMask>",         getResetMask()));
      }
      writer.println(" </register>");
   }
   
   /**
    * Write dimension list to SVD file
    * 
    *  @param writer          The destination for the XML
    *  @param level           Level of indenting
    */
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
         writer.println(String.format("</dimIndex>"));
      }
   }
   
   /**
    *   Writes the Register description to file in a SVF format
    *   
    *  @param writer          The destination for the XML
    *  @param standardFormat  Suppresses some non-standard size optimisations 
    *  @param level           Level of indenting
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
      
      writeDimensionList(writer, indenter+"   ");

      writer.println(String.format(   indenter+"   <name>%s</name>",                     SVD_XML_BaseParser.escapeString(getName())));
      if (isHidden()) {
         writer.println(              indenter+"   <?"+SVD_XML_Parser.HIDE_ATTRIB+"?>");
      }
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

   /**
    * Gets the C typedef name for the given size e.g. 'uint8_t'
    * 
    * @param size
    * 
    * @return
    * @throws Exception
    */
   private String getCSizeName(long size) throws Exception {
      switch ((((int)size)+7)/8) {
      case 1 : return "uint8_t";
      case 2 : return "uint16_t";
      case 4 : return "uint32_t";
      case 8 : return "uint64_t";
      }
      throw new Exception("Unknown size in register : "+size);
   }
   
   /**
    * Gets the C name for the appropriate access e.g. "__IO"
    * 
    * @param accessType
    * @return
    */
   private String getCAccessName(AccessType accessType) {
      String accessPrefix = "__IO";
      switch (accessType) {
      case ReadOnly      : accessPrefix = "__I";  break;
      case ReadWrite     : accessPrefix = "__IO"; break;
      case ReadWriteOnce : accessPrefix = "__IO"; break;
      case WriteOnce     : accessPrefix = "__IO"; break;
      case WriteOnly     : accessPrefix = "__O";  break;
      default            : accessPrefix = "__IO"; break;
      }
      return accessPrefix;
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.Cluster#getElementSizeInBytes()
    */
   @Override
   public long getElementSizeInBytes() {
      if ((getDimensionIndexes() != null)) {
         // Array - use stride as element size
         return getDimensionIncrement();
      }
      else {
         return (getWidth()+7)/8;
      }
   }

   public final static String lineFormat = "%-47s /*!< %04X: %-60s */\n";

   /**
    *    Writes C code for Register as part of a STRUCT element e.g.<br>
    *    <pre><b>__I  uint8_t  registerName;"</pre></b>
    * 
    *    @param writer
    *    @param devicePeripherals
    */
   @Override
   public void writeHeaderFileDeclaration(PrintWriter writer, int indent, RegisterUnion registerUnion, Peripheral peripheral, long offset) throws Exception {

      String accessPrefix = getCAccessName(getAccessType());
      final String indenter = RegisterUnion.getIndent(indent);

      StringBuffer line = new StringBuffer(120);
      line.append(indenter);
      
      if (getDimension()==0) {
         // Simple register
         line.append(String.format("%-4s %-9s %s;", 
               accessPrefix,
               getCSizeName(getWidth()), 
               getBaseName()));
         writer.print(String.format(lineFormat, line.toString(), offset, truncateAtNewlineOrTab(getCDescription(-1))));
         return;
      }
      if (isSimpleArray()) {
         line.append(String.format("%-4s %-9s %s;", 
               accessPrefix,
               getCSizeName(getWidth()), 
               getSimpleArraySubscriptedName(getDimension())));
         writer.print(String.format(lineFormat, line.toString(), offset, truncateAtNewlineOrTab(getCDescription(-1))));
         return;
      }
      if (getName().matches("^.+%s.*$")) {
         // Register name contains placement e.g. ADCREG%sOUT
         line.append(String.format("%-4s %-9s %s", 
               accessPrefix,
               getCSizeName(getWidth()), 
               getName()));
         String baseLine = line.toString();
         long padding = getDimensionIncrement()-getWidth()/8;
         for (int index=0; index< getDimension(); index++) {
            String sIndex = getDimensionIndexes().get(index);
            int delimeter = sIndex.indexOf(':');
            if (delimeter > 0) {
               sIndex = sIndex.substring(0, delimeter);
            }
            writer.print(String.format(lineFormat, String.format(baseLine,sIndex)+";", 
                  offset, truncateAtNewlineOrTab(format(getCDescription(index), index))));
            if (padding>0) {
               registerUnion.fill(offset, padding);
            }
            offset += getDimensionIncrement();
         }
         return;
      }
      throw new Exception(String.format("Register Too complex to handle\'%s\'", getName()));
   }

   /**
    *    Writes a macro to allow 'Freescale' style access to the registers of the peripheral<br>
    *    e.g. <pre><b>#define I2S0_CR3 (I2S0->CR[3])</b></pre>
    * 
    * @param  writer
    * @param  devicePeripherals
    * @throws Exception
    */
   @Override
   public void writeHeaderFileRegisterMacro(PrintWriter writer, Peripheral peripheral) throws Exception {
      if (getDimension() == 0) {
         // Simple register
         String name = peripheral.getName()+"_"+getName();
         name = getMappedRegisterMacroName(name);
         if (name.length() == 0) {
            return;
         }
         writer.print(String.format("#define %-30s (%s->%s)\n", name, peripheral.getName(), getName()));
         return;
      }
      // Array
      for(int index=0; index<getDimension(); index++) {
         if (isSimpleArray()) {
            String name      = peripheral.getName()+"_"+getSimpleArrayName(index);
            String arrayName = peripheral.getName()+"->"+getSimpleArraySubscriptedName(index);
            writer.print(String.format("#define %-30s (%s)\n", name, arrayName));
            continue;
         }
         String name = peripheral.getName()+"_"+getName(index);
         name = getMappedRegisterMacroName(name);
         if (name.length() == 0) {
            // Quietly delete name
            return;
         }
         writer.print(String.format("#define %-30s (%s->%s)\n", name, peripheral.getName(), getName(index)));
      }
   }

   static final String registerMacroPrefix = 
         "/* ------- %-40s ------ */\n";

   private void writeFieldMacro(PrintWriter writer, String prefix) throws Exception {
      for (Field field : fields) {
         field.writeHeaderFileFieldMacros(writer, prefix);
      }
   }

   /**
    *    Writes a set of MACROs to allow convenient operations on the fields of the registers of this register e.g.
    *    <pre><b>#define PERIPHERAL_FIELD(x)    (((x)&lt;&lt;FIELD_OFFSET)&FIELD_MASK)</b></pre>
    *    <pre><b>#define FP_CTRL_NUM_LIT_MASK   (0x0FUL << FP_CTRL_NUM_LIT_SHIFT)     </b></pre>
    * 
    * @param  writer
    * @param  devicePeripherals
    * @throws Exception
    */
   @Override
   public void writeHeaderFileFieldMacros(PrintWriter writer, Peripheral peripheral) throws Exception {
      writeHeaderFileFieldMacros(writer, peripheral, "");
   }

   /**
    *    Writes a set of MACROs to allow convenient operations on the fields of the registers of this register e.g.
    *    <pre><b>#define PERIPHERAL_FIELD(x)    (((x)&lt;&lt;FIELD_OFFSET)&FIELD_MASK)</b></pre>
    *    <pre><b>#define FP_CTRL_NUM_LIT_MASK   (0x0FUL << FP_CTRL_NUM_LIT_SHIFT)     </b></pre>
    * 
    * @param  writer
    * @param  devicePeripherals
    * @throws Exception
    */
   public void writeHeaderFileFieldMacros(PrintWriter writer, Peripheral peripheral, String registerPrefix) throws Exception {
      if (getDerivedFrom() != null) {
         // Don't do macros for derived registers
         return;
      }
      //      writer.print(String.format(registerMacroPrefix, getFormattedName(-1, peripheral, this, registerPrefix)));
      writer.print(String.format(registerMacroPrefix, getSimplifedName() + " Bit Fields"));
      //      String registerPrefix = (isMapFreescaleCommonNames())?peripheral.getPrependToName()+"_":peripheral.getName()+"_";
      //      sortFields();
      //      for (Field field : fields) {
      //         field.writeHeaderFileFieldMacros(writer, registerPrefix+getBaseName());
      //      }
      //      return;
      sortFields();

      String formattedName = getFormattedName(-1, peripheral.getName(), this, registerPrefix);
      formattedName = String.format(formattedName, "");
      formattedName.replaceAll("\\[\\]", "");

      if (getDimension() == 0) {
         // Simple register
         String name = getMappedRegisterMacroName(peripheral.getName())+"_"+formattedName;
         if (name.length() == 0) {
            return;
         }
         writeFieldMacro(writer, name);
         return;
      }
      // Array
      if (isSimpleArray()) {
         String name      = peripheral.getName()+"_"+formattedName;
         writeFieldMacro(writer, name);
      }
      else {
         String name = getMappedRegisterMacroName(peripheral.getName())+"_"+formattedName;
         if (name.length() == 0) {
            // Quietly delete name
            return;
         }
         writeFieldMacro(writer, name);
      }
   }

   private String getSimplifedName() {
      return getBaseName().replaceAll("\\[?%s\\]?", "");
   }

   /**
    * Locates the register field with given name
    * 
    * @param name Name of field to search for
    * 
    * @return Field found or null if not present
    */
   public Field findField(String name) {
      for (Field f:fields) {
         if (f.getName().equals(name)) {
            return f;
         }
      }
      return null;
   }

   public boolean isHidden() {
      return hidden;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }


}
