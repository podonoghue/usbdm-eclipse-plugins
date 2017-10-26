package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
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
   private Cluster            cluster;
   
   public Register(Peripheral owner, Cluster cluster) {
      super(owner);
      this.cluster       = cluster;
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
            if (field2.getBitwidth() > field1.getBitwidth()) {
               return 1;
            }
            else if (field2.getBitwidth() < field1.getBitwidth()) {
               return -1;
            }
            if (field2.getDerivedFrom() == field1) {
               return -1;  
            }
            if (field1.getDerivedFrom() == field2) {
               return 1;  
            }
            return 0;
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
      boolean verbose = false;
//      verbose = getName().equalsIgnoreCase("PE1") && other.getName().equalsIgnoreCase("PE1");
      if (verbose) {
         System.err.println("Comparing base \""+getName()+"\", \""+other.getName());
      }
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
         return false;
      }
      // Check if equivalent fields
      sortFields();
      other.sortFields();
      for (int index=0; index<fields.size(); index++) {
         if (!fields.get(index).equivalent(other.fields.get(index))) {
            return false;
         }
      }
      return true;
   }
   
   /** Determines if two registers are equivalent
    * 
    * @param other      Other enumeration to check
    * @param pattern1   Pattern to apply to name & description of self  "(prefix)(index)(suffix)"
    * @param pattern2   Pattern to apply to name & description of other "(prefix)(index)(suffix)"
    * 
    * @note Patterns are applied recursively to enumerations etc.
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Register other, String pattern1, String pattern2) {
      boolean verbose = false;
//    verbose = getName().equalsIgnoreCase("PE1") && other.getName().equalsIgnoreCase("PE1");
      if (verbose) {
         System.err.println("Comparing base \""+getName()+"\", \""+other.getName());
      }
      boolean rv = 
            (this.getWidth()       == other.getWidth()) &&
            (this.fields.size()    == other.fields.size()) &&
            (this.getDimension()   == other.getDimension()); 
      if (!isIgnoreAccessTypeInEquivalence()) {
         rv = rv &&
               (this.getAccessType() == other.getAccessType());
      }
      if (!isIgnoreResetValuesInEquivalence()) {
         rv = rv &&
               (this.getResetValue()  == other.getResetValue()) &&
               (this.getResetMask()   == other.getResetMask());
      }
      if (verbose) {
         System.err.println("Comparing simple structure \""+getName()+"\", \""+other.getName()+"\"=> false");
      }
      if (!rv) {
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
    * 
    * @param index   Used for %s index
    * 
    * @return        String description
    * 
    * @throws Exception
    */
   public String getCName(int registerIndex) {
      return SVD_XML_BaseParser.unEscapeString(format(getName(), registerIndex));
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
   public void writederivedfromSVD(Writer writer, boolean standardFormat, Peripheral owner, int level) throws Exception {
      String indent = fill.substring(0, level);
      writer.write(String.format(   indent+"<register derivedFrom=\"%s\">",          getDerivedFrom().getName()));

      Cluster derivedCluster = getDerivedFrom();
      if (!(derivedCluster instanceof Register)) {
         throw new Exception("Only support derived registers");
      }
      Register derived = (Register) derivedCluster;

      if (getDimensionIndexes() != derived.getDimensionIndexes()) {
         writeDimensionList(writer, "", derived);
      }
      if (!getName().equals(derived.getName())) {
         writer.write(String.format(" <name>%s</name>",                     SVD_XML_BaseParser.escapeString(getName())));
      }
      if (isHidden() && (isHidden() != derived.isHidden())) {
         writer.write("<?"+SVD_XML_Parser.HIDE_ATTRIB+"?>");
      }
      if (isDoDerivedMacros() && (isDoDerivedMacros() != derived.isDoDerivedMacros())) {
         writer.write(String.format(" <?doDerivedMacros?>"));
      }
      if (!getDescription().equals(derived.getDescription())) {
         writer.write(String.format(" <description>%s</description>",       SVD_XML_BaseParser.escapeString(getDescription())));
      }
      if (getAddressOffset() != derived.getAddressOffset()) {
         writer.write(String.format(" <addressOffset>0x%X</addressOffset>", getAddressOffset()));
      }
      if (!getAccessType().equals(derived.getAccessType())) {
         writer.write(String.format(" <access>%s</access>",                 getAccessType().getPrettyName()));
      }
      if (getResetValue() != derived.getResetValue()) {
         writer.write(String.format(" <resetValue>0x%X</resetValue>",       getResetValue()));
      }
      if (getResetMask() != derived.getResetMask()) {
         writer.write(String.format(" <resetMask>0x%X</resetMask>",         getResetMask()));
      }
      writer.write(" </register>\n");
   }
   
   /**
    * Write dimension list to SVD file
    * 
    *  @param writer          The destination for the XML
    *  @param level           Level of indenting
    *  @param derivedRegister Register derived from (may be null)
    *  
    *  @throws IOException 
    */
   void writeDimensionList(Writer writer, String indent, Register derivedRegister) throws IOException {
      if (getDimension()>0) {
         if (derivedRegister != null) {
            if (getDimension() != derivedRegister.getDimension()) {
               writer.write(String.format("<dim>%d</dim>", getDimension()));
            }
            if (getDimensionIncrement() != derivedRegister.getDimensionIncrement()) {
               writer.write(String.format("<dimIncrement>%d</dimIncrement>", getDimensionIncrement()));
            }
            if (!getDimensionIndexes().equals(derivedRegister.getDimensionIndexes())) {
               writer.write(String.format("<dimIndex>"));
               boolean doComma = false;
               for (String s : getDimensionIndexes()) {
                  if (doComma) {
                     writer.write(",");
                  }
                  doComma = true;
                  writer.write(SVD_XML_BaseParser.escapeString(s));
               }
               writer.write(String.format("</dimIndex>"));
            }
         }
         else {
            writer.write(String.format(indent+"<dim>%d</dim>\n",                       getDimension()));
            writer.write(String.format(indent+"<dimIncrement>%d</dimIncrement>\n",     getDimensionIncrement()));
            writer.write(String.format(  indent+"<dimIndex>"));
            boolean doComma = false;
            for (String s : getDimensionIndexes()) {
               if (doComma) {
                  writer.write(",");
               }
               doComma = true;
               writer.write(SVD_XML_BaseParser.escapeString(s));
            }
            writer.write(String.format("</dimIndex>\n"));
         }
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
   public void writeFlattenedSVD(Writer writer, boolean standardFormat, Peripheral owner, int indent) throws Exception {
      // TODO complete this
      for (int index=0; index<getDimension(); index++) {
         final String indenter = RegisterUnion.getIndent(indent);
         writer.write(                 indenter+"<register>\n");
         writeBaseRegisterSVD(writer, standardFormat, owner, indent, this.getCName(index), this.getAddressOffset(index));
         writer.write(                 indenter+"</register>\n");
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
   public void writeSVD(Writer writer, boolean standardFormat, Peripheral owner, int indent) throws Exception {
      
      if ((getDerivedFrom() != null) && !isExpandDerivedRegisters()) {
         writederivedfromSVD(writer, standardFormat, owner, indent);
         return;
      }
      if (isFlattenArrays() && (getDimension()>0)) {
         writeFlattenedSVD(writer, standardFormat, owner, indent);
      }
      else {
         final String indenter = RegisterUnion.getIndent(indent);
         writer.write(                 indenter+"<register>\n");
         writeDimensionList(writer, indenter+"   ", null);
         writeBaseRegisterSVD(writer, standardFormat, owner, indent, getName(), getAddressOffset());
         writer.write(                 indenter+"</register>\n");
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
   public void writeBaseRegisterSVD(Writer writer, boolean standardFormat, Peripheral owner, int indent, String name, long offset) throws Exception {
      
      final String indenter = RegisterUnion.getIndent(indent);
      writer.write(String.format(   indenter+"   <name>%s</name>\n",                     SVD_XML_BaseParser.escapeString(name)));
      if (isHidden()) {
         writer.write(              indenter+"   <?"+SVD_XML_Parser.HIDE_ATTRIB+"?>\n");
      }
      if (isDoDerivedMacros()) {
         writer.write(              indenter+"   <?"+SVD_XML_Parser.DODERIVEDMACROS_ATTRIB+"?>\n");
      }
      if ((getDescription() != null) && (getDescription().length() > 0)) {
         writer.write(String.format(indenter+"   <description>%s</description>\n",       SVD_XML_BaseParser.escapeString(getDescription())));
      }
      writer.write(String.format(   indenter+"   <addressOffset>0x%X</addressOffset>\n", offset));
      if ((owner == null) || (owner.getWidth() != getWidth()) || (fields.size() == 0)) {
         writer.write(String.format(indenter+"   <size>%d</size>\n",                     getWidth()));
      }
      if ((owner == null) || (owner.getAccessType() != getAccessType()) || (fields.size() == 0)) {
         writer.write(String.format(indenter+"   <access>%s</access>\n",                 getAccessType().getPrettyName()));
      }
      if ((owner == null) || (owner.getResetValue() != getResetValue())) {
         writer.write(String.format(indenter+"   <resetValue>0x%X</resetValue>\n",       getResetValue()));
      }
      if ((owner == null) || (owner.getResetMask() != getResetMask())) {
         writer.write(String.format(indenter+"   <resetMask>0x%X</resetMask>\n",         getResetMask()));
      }

      if ((getFields() != null) && (getFields().size() > 0)) {
         writer.write(              indenter+"   <fields>\n");
         sortFields();
         for (Field field : getFields()) {
            field.writeSVD(writer, standardFormat, this, indent+6);
         }
         writer.write(              indenter+"   </fields>\n");
      }
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

   public final static String lineFormat                = "%-47s /**< %04X: %-60s */\n";
   public final static String lineFormatNoDocumentation = "%-47s\n";

   /**
    *    Writes C code for Register as part of a STRUCT element e.g.<br>
    *    <pre><b>__I  uint8_t  registerName;"</pre></b>
    * 
    *    @param writer
    *    @param devicePeripherals
    */
   @Override
   public void writeHeaderFileDeclaration(Writer writer, int indent, RegisterUnion registerUnion, Peripheral peripheral, long offset) throws Exception {

      String accessPrefix = getCAccessName(getAccessType());
      final String indenter = RegisterUnion.getIndent(indent);

      StringBuilder line = new StringBuilder(120);
      line.append(indenter);
      
      if (getDimension()==0) {
         // Simple register
         line.append(String.format("%-4s %-9s %s;", 
               accessPrefix,
               getCSizeName(getWidth()), 
               getBaseName()));
         writer.write(String.format(lineFormat, line.toString(), offset, truncateAtNewlineOrTab(getCDescription(-1))));
         return;
      }
      if (isSimpleArray()) {
         line.append(String.format("%-4s %-9s %s;", 
               accessPrefix,
               getCSizeName(getWidth()), 
               getSimpleArraySubscriptedName(getDimension())));
         writer.write(String.format(lineFormat, line.toString(), offset, truncateAtNewlineOrTab(getCDescription(-1))));
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
            writer.write(String.format(lineFormat, String.format(baseLine,sIndex)+";", 
                  offset, truncateAtNewlineOrTab(format(getCDescription(index), index))));
            writer.flush();
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
   public void writeHeaderFileRegisterMacro(Writer writer, Peripheral peripheral) throws Exception {
      if (getDimension() == 0) {
         // Simple register
         String name = peripheral.getName()+"_"+getName();
         name = getMappedRegisterMacroName(name);
         if (name.length() == 0) {
            return;
         }
         writer.write(String.format("#define %-30s (%s->%s)\n", name, peripheral.getName(), getName()));
         return;
      }
      // Array
      for(int index=0; index<getDimension(); index++) {
         if (isSimpleArray()) {
            String name      = peripheral.getName()+"_"+getSimpleArrayName(index);
            String arrayName = peripheral.getName()+"->"+getSimpleArraySubscriptedName(index);
            writer.write(String.format("#define %-30s (%s)\n", name, arrayName));
            continue;
         }
         String name = peripheral.getName()+"_"+getName(index);
         name = getMappedRegisterMacroName(name);
         if (name.length() == 0) {
            // Quietly delete name
            return;
         }
         writer.write(String.format("#define %-30s (%s->%s)\n", name, peripheral.getName(), getName(index)));
      }
   }

   static final String registerMacroPrefix = 
         "/* ------- %-40s ------ */\n";

   private void writeFieldMacro(Writer writer, String prefix) throws Exception {
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
   public void writeHeaderFileFieldMacros(Writer writer, Peripheral peripheral) throws Exception {
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
   public void writeHeaderFileFieldMacros(Writer writer, Peripheral peripheral, String registerPrefix) throws Exception {
      if ((getDerivedFrom() != null) && !isDoDerivedMacros()) {
         // Don't do macros for derived registers
         return;
      }
      //      writer.write(String.format(registerMacroPrefix, getFormattedName(-1, peripheral, this, registerPrefix)));
      writer.write(String.format(registerMacroPrefix, getSimplifedName() + " Bit Fields"));
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
   
   /**
    * Checks if two strings are similar
    * 
    * @param p    Pattern used to compare strings
    * @param enumeration1   First string
    * @param enumeration2   Second string
    * 
    * @return true if the strings are similar
    */
   boolean similarTo(Pattern p, Enumeration enumeration1, Enumeration enumeration2) {
      if (!enumeration1.equivalent(enumeration2)) {
         return false;
      }
//      Matcher m1 = p.matcher(enumeration1.getDescription());
//      if (!m1.matches()) {
//         return false;
//      }
//      Matcher m2 = p.matcher(enumeration2.getDescription());
//      if (!m2.matches()) {
//         return false;
//      }
//      if (!m1.group(1).equalsIgnoreCase(m2.group(1))) {
//         return false;
//      }
//      if (!m1.group(3).equalsIgnoreCase(m2.group(3))) {
//         return false;
//      }
////      System.err.println(String.format("Found match %s.(%s:%s) ~= (%s:%s)", getName(), m1.group(1), m1.group(2), m2.group(1), m2.group(2)));
      return true;
   }
   
   /**
    * Checks if two fields are similar
    * 
    * @param p    Pattern used to compare strings
    * @param f1   First field
    * @param f2   Second field
    * 
    * @return true if the fields are similar
    */
   boolean similarTo(Pattern p, Field f1, Field f2) {
      if (f1.getEnumerations().size() != f2.getEnumerations().size()) {
         return false;
      }
      if (f1.getEnumerations().size() == 0) {
         return false;
      }
      if (f1.getBitwidth() != f2.getBitwidth()) {
         return false;
      }
      Iterator<Entry<String, Enumeration>> enumerationIt      = f1.getSortedEnumerations().entrySet().iterator();
      Iterator<Entry<String, Enumeration>> otherEnumerationIt = f2.getSortedEnumerations().entrySet().iterator();
      
      while (enumerationIt.hasNext() || otherEnumerationIt.hasNext()) {
         if (!enumerationIt.hasNext() || !otherEnumerationIt.hasNext()) {
            // Unbalanced
            return false;
         }
         Entry<String, Enumeration> enumeration      = enumerationIt.next();
         Entry<String, Enumeration> otherEnumeration = otherEnumerationIt.next();
         if (!similarTo(p, enumeration.getValue(), otherEnumeration.getValue())) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Examines the bit fields and attempts to extract similar fields that may
    * be described by a &lt;derivedFrom&gt; relation
    */
   void extractSimilarFields() {
      sortFields();
      Pattern pattern = Pattern.compile("^(.+)(\\d*)(.*)$");
      // Compare each field to every other field
      for (int outer = 0; outer<getFields().size()-1; outer++) {
         Field oField = fields.get(outer);
         if (oField.getDerivedFrom() != null) {
            continue;
         }
         for (int inner = outer+1; inner<getFields().size(); inner++) {
            Field iField = fields.get(inner);
            if (iField.getDerivedFrom() != null) {
               continue;
            }
            if (similarTo(pattern, oField, iField)) {
               // Make fields derived
//               System.err.println(String.format("Found match: %s (%s ~= %s)", getName(), oField.getName(), iField.getName()));
               iField.setDerivedFrom(oField);
            }
         }
      }
   }
   /**
    * Apply some optimisations to the bit fields
    * 
    */
   public void optimise() {
      if (isExtractSimilarFields()) {
         extractSimilarFields();
      }
      
   }
}
