package net.sourceforge.usbdm.peripheralDatabase;

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

   /** Description of this register */
   private String             fDescription;
   
   /** Supported but not used */
   private String             fAlternateGroup;
   
   /** Fields for this register */
   private ArrayList<Field>   fFields;
   
   /** Flag used to indicate register has been deleted during optimisation */
   private boolean            fDeleted;
   
   /** Width of elements */
   private long               fWidth;
   
   /** Optional cluster that encloses this register */
   private Cluster            fCluster;
   
   /**
    * 
    * @param owner      Owning peripheral for default values
    * @param cluster    Optional cluster that encloses this register
    */
   public Register(Peripheral owner, Cluster cluster) {
      super(owner);
      this.fCluster       = cluster;
      fDescription        = "";
      fAlternateGroup     = "";
      fFields             = new ArrayList<Field>();
      fDeleted            = false;
      if (owner != null) {
         fWidth          = owner.getWidth();
      }
      else {
         fWidth          =  32;
      }
   }
   
   /**
    * Returns a relatively shallow copy of the register
    * 
    * The following should usually be changed:
    * <ul>
    *    <li>name
    *    <li>addressOffset
    *    <li>cluster
    * </ul>
    * Shares (don't modify)
    * <ul>
    *    <li>registers
    *    <li>addressBlock
    *    <li>fields
    * </ul>
    * Copied from this
    * <ul>
    *    <li>derivedFrom == this
    *    <li>baseName
    *    <li>description
    *    <li>width
    *    <li>nameMacroformat
    *    <li>dimensionIncrement
    *    <li>dimensionIndexes (shared reference)
    *    <li>accessType
    *    <li>resetValue
    *    <li>resetMask
    *    <li>owner
    *    <li>hidden = false;
    *    <li>prependToName
    *    <li>appendToName
    * </ul>
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {

      // Make shallow copy
      Register clone = (Register) super.clone();

      // Clones should be given a new name
      clone.setName(getName() + "_register_clone");

      // Clear deleted
      clone.setDeleted(false);
      return clone;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      if (getDimension()>0) {
         return String.format("Register[%s[%d] o:0x%X, w:%d, s:%d]", getName(), getDimension(), getAddressOffset(), getWidth(), getTotalSizeInBytes());
      }
      return String.format("Register[%s o:0x%X, w:%d, s:%d]", getName(), getAddressOffset(), getWidth(), getTotalSizeInBytes());
   }

   public void sortFields() {
      Collections.sort(fFields, new Comparator<Field>() {
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
            return field1.getName().compareTo(field2.getName());
         }
      });
   }

   /**
    * Checks if this register description agrees with another
    * 
    * @param other               Register to check against
    * @param ignoreRegisterName  Used to compare register structure ignoring name and description
    * 
    * @return true if equivalent
    */
   @Override
   public boolean equivalent(Object _other, int matchOptions) {
      if (!(_other instanceof Register)) {
         return false;
      }
      Register other = (Register)_other;
      if ((getName() == null) || (other.getName() == null)) {
         System.err.println("Check register without names");
      }
      if (  (this.getAddressOffset() != other.getAddressOffset()) ||
            (!this.getName().equals(other.getName()))) {
         return false;
      }
      return equivalentStructure(_other, matchOptions);
   }
   /**
    * Checks if this register structure agrees with another
    * 
    * @param other               Register to check against
    * @param ignoreRegisterName  Used to compare register structure ignoring name and description
    * 
    * @return true if equivalent
    */
   @Override
   public boolean equivalentStructure(Object _other, int matchOptions) {
      if (!(_other instanceof Register)) {
         return false;
      }
      Register other = (Register)_other;
      boolean verbose = false;
//      verbose = getName().equalsIgnoreCase("PCC_FTFC") && other.getName().equalsIgnoreCase("PCC_FlexCAN0");
      if (verbose) {
         System.err.println("Comparing base \""+getName()+"\", \""+other.getName());
      }
      if ((matchOptions&MatchOptions.MATCH_SUBS) != 0) {
         if (  (this.getDimension()          != other.getDimension()) ||
               (this.getDimensionIncrement() != other.getDimensionIncrement())) {
            return false;
         }
      }
      if (!equivalent(other, "(.+)()(.*)", "(.+)()(.*)", matchOptions)) {
         if (verbose) {
            System.err.println("Comparing structure \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      // Check if equivalent fields
      sortFields();
      other.sortFields();
      for (int index=0; index<fFields.size(); index++) {
         if (!fFields.get(index).equivalent(other.fFields.get(index))) {
            return false;
         }
      }
      return true;
   }
   
   /** Determines if two registers are equivalent
    * 
    * @param other               Other enumeration to check
    * @param pattern1            Pattern to apply to name & description of self  "(prefix)(index)(suffix)"
    * @param pattern2            Pattern to apply to name & description of other "(prefix)(index)(suffix)"
    * @param matchOptions  Used to compare register structure ignoring name and description
    * 
    * @note Patterns are applied recursively to enumerations etc.
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Register other, String pattern1, String pattern2, int matchOptions) {
      boolean verbose = false;
//      verbose = getName().equalsIgnoreCase("PCC_FTFC") && other.getName().equalsIgnoreCase("PCC_FlexCAN0");
      if (verbose) {
         System.err.println("Comparing base \""+getName()+"\", \""+other.getName());
      }
      boolean rv =
            (this.getWidth()       == other.getWidth()) &&
            (this.fFields.size()    == other.fFields.size());
      
      if ((matchOptions&MatchOptions.MATCH_SUBS) != 0) {
          rv = rv && (this.getDimension() == other.getDimension()) &&
               (this.getDimensionIncrement() == other.getDimensionIncrement());
      }
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
         System.err.println("Comparing simple structure \""+getName()+"\", \""+other.getName()+"\"=> "+rv);
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
      if ((matchOptions&MatchOptions.MATCH_NAMES) != 0) {
         String d1 = getDescription();
         String d2 = other.getDescription();
         rv = d1.equalsIgnoreCase(d2);
         if (!rv) {
            d1 = d1.replaceFirst(pattern1, "$1%s$3");
            d2 = d2.replaceFirst(pattern2, "$1%s$3");
            rv =  d1.equalsIgnoreCase(d2);
         }
      }
      if (!rv) {
         return false;
      }
      sortFields();
      other.sortFields();
      for (int index=0; index<fFields.size(); index++) {
         if (!fFields.get(index).equivalent(other.fFields.get(index), pattern1, pattern2)) {
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
      for (Field field : fFields) {
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
   
   void reportFields() {
      for (Field field:fFields) {
         System.err.println(field);
      }
   }
   /**
    *  Checks the fields don't overlap or exceed register dimensions
    * 
    * @throws Exception
    */
   public void checkFieldDimensions() throws Exception {
      long bitsUsed = 0L;
      for (Field field : fFields) {
         if (field.getAccessType() == null) {
            field.setAccessType(getAccessType());
         }
         // Check for field exceeds register
         if ((field.getBitOffset()+field.getBitwidth()) > getWidth()) {
            reportFields();
            throw new Exception(String.format("Bit field \'%s\' outside register in \'%s\'", field.getName(), getName()));
         }
         if (!field.isIgnoreOverlap()) {
            // Check for field overlap
            long bitsUsedThisField = 0;
            for (long i=field.getBitOffset(); i<(field.getBitOffset()+field.getBitwidth()); i++) {
               bitsUsedThisField |= 1L<<i;
            }
            if ((bitsUsed&bitsUsedThisField) != 0) {
               reportFields();
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
      format = fCluster.format(format, clusterIndex);
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
    * Returns the description of the register <br>
    * Sanitised for use in C code <br>
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
    * Returns the description of the register <br>
    * Sanitised for use in C code <br>
    * Description if truncated at first newline if present.
    * 
    * @param index   Used for %s index
    * 
    * @return        String description
    * @throws Exception
    */
   public String getBriefCDescription() {
      String s = SVD_XML_BaseParser.unEscapeString(getDescription());
      int truncateAt = s.indexOf('\n');
      if (truncateAt>0) {
         s = s.substring(0, truncateAt);
      }
      return s;
   }

   /**
    * Set register description
    * 
    * @param description
    */
   public void setDescription(String description) {
      this.fDescription = getSanitizedDescription(description.trim());
   }

   /**
    * Returns the description of the register
    * 
    * @return String description
    */
   public String getDescription() {
      return fDescription;
   }

   /**
    * 
    * @return
    */
   public String getAlternateGroup() {
      return fAlternateGroup;
   }

   /**
    * 
    * @param alternateGroup
    */
   public void setAlternateGroup(String alternateGroup) {
      this.fAlternateGroup = alternateGroup;
   }

   @Override
   public long getTotalSizeInBytes() {
      if (getDimension() == 0) {
         return getElementSizeInBytes();
      }
      return getDimensionIncrement() * getDimension();
   }

   public void addField(Field field) {
      fFields.add(0,field);
   }
   
   public ArrayList<Field> getFields() {
      return fFields;
   }

   public ArrayList<Field> getSortedFields() {
      sortFields();
      return fFields;
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.Cluster#addAddressBlocks(net.sourceforge.usbdm.peripheralDatabase.Peripheral.AddressBlocksMerger)
    */
   @Override
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger, int isolatedIndex, long addressOffset) throws Exception {
//    // XXX Delete OK
//      System.err.println(String.format("Register.addAddressBlocks(%s) addressOffset = 0x%04X, offset = 0x%04X", getName(), addressOffset, addressOffset+getAddressOffset()));
//      if (isIsolated()) {
//         System.err.println("addAddressBlocks(Isolated register " +getName() + ", #" + isolatedIndex + ")");
//      }
      addressOffset += getAddressOffset();
      try {
         if (getDimension() == 0) {
            // Simple array
            addressBlocksMerger.addBlock(addressOffset, isolatedIndex, this);
            return;
         }
         // Do each dimension of array
         for (int index=0; index<getDimension(); index++) {
            if (isIsolated()) {
               // Isolate each array element
               isolatedIndex = addressBlocksMerger.createNewIsolation();
            }
            addressBlocksMerger.addBlock(addressOffset, isolatedIndex, this);
            addressOffset += getDimensionIncrement();
         }
      } catch (Exception e) {
         System.err.println("Failing Register = " + this.toString());
         throw e;
      }
   }

   /* (non-Javadoc)
    * @see net.sourceforge.usbdm.peripheralDatabase.Cluster#addAddressBlocks(net.sourceforge.usbdm.peripheralDatabase.Peripheral.AddressBlocksMerger)
    */
   @Override
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger, int isolatedIndex) throws Exception {
      addAddressBlocks(addressBlocksMerger, isolatedIndex, 0L);
   }

   /**
    * Returns width of the register
    * 
    * @return width in bits
    */
   @Override
   public long getWidth() {
      return fWidth;
   }

   /**
    * Sets width of register
    * 
    * @param width in bits
    */
   public void setWidth(long width) {
      this.fWidth = width;
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
      return fDeleted;
   }

   /**
    * @param deleted the deleted to set
    */
   @Override
   public void setDeleted(boolean deleted) {
      this.fDeleted = deleted;
   }

   @Override
   public void report() throws Exception {
         if (getDimension() > 0) {
            for (int dim =0; dim < getDimension(); dim++) {
               System.out.println(String.format("       Register \"%s\" [@0x%08X, W=%d, RV=0x%08X, RM=0x%08X], Description = \"%s\"",
                     getName(dim), getAddressOffset(dim), getWidth(), getResetValue(), getResetMask(), getDescription()));
               for (Field field : fFields) {
                  field.report();
               }
            }
         }
         else {
            System.out.println(String.format("       Register \"%s\" [@0x%08X, W=%d, RV=0x%08X, RM=0x%08X], Description = \"%s\"",
                  getName(), getAddressOffset(), getWidth(), getResetValue(), getResetMask(), getDescription()));
            for (Field field : fFields) {
               field.report();
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
   public void writederivedfromSVD(Writer writer, boolean standardFormat, Peripheral owner, int level) throws Exception {
      final String fill = "                                                     ";
      String indent = fill.substring(0, level);
      writer.write(String.format(   indent+"<register derivedFrom=\"%s\">",          getDerivedFrom().getName()));

      Cluster derivedCluster = getDerivedFrom();
      if (!(derivedCluster instanceof Register)) {
         throw new Exception("Only support derived registers");
      }
      Register derived = (Register) derivedCluster;

      if (getDimensionIndexes() != derived.getDimensionIndexes()) {
         writeSvdDimensionList(writer, "", derived);
         writer.flush();
      }
      if (!getName().equals(derived.getName())) {
         writer.write(String.format(" <name>%s</name>",                     SVD_XML_BaseParser.escapeString(getName())));
      }
      if (isHidden() && (isHidden() != derived.isHidden())) {
         writer.write("<?"+SVD_XML_Parser.HIDE_PROCESSING+"?>");
      }
      if (isDoDerivedMacros() && (isDoDerivedMacros() != derived.isDoDerivedMacros())) {
         writer.write(String.format(" <?doDerivedMacros?>"));
      }
      if (!getDescription().equals(derived.getDescription())) {
         writer.write(String.format(" <description>%s</description>",       SVD_XML_BaseParser.escapeString(getDescription())));
      }
      writer.write(String.format(" <addressOffset>0x%X</addressOffset>", getAddressOffset()));
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
         writeBaseRegisterSVD(writer, standardFormat, owner, index, indent);
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
   public void writeSvd(Writer writer, boolean standardFormat, Peripheral owner, int indent) throws Exception {
      
      if (isFlattenArrays() && (getDimension()>0)) {
         writeFlattenedSVD(writer, standardFormat, owner, indent);
         return;
      }
      if ((getDerivedFrom() != null) && !isExpandDerivedRegisters()) {
         writederivedfromSVD(writer, standardFormat, owner, indent);
         return;
      }
      final String indenter = RegisterUnion.getIndent(indent);
      writer.write(                 indenter+"<register>\n");
      writeSvdDimensionList(writer, indenter+"   ", null);
      writeBaseRegisterSVD(writer, standardFormat, owner, -1, indent);
      writer.write(                 indenter+"</register>\n");
   }

   /**
    *   Writes the Register description to file in a SVF format
    * 
    *  @param writer          The destination for the XML
    *  @param standardFormat  Suppresses some non-standard size optimisations
    *  @param owner           The owner - This is used to reduce the size by inheriting default values
    *  @param indent          Level of indenting
    *  @param index           Index to use for name/description expansion (<0 to suppress)
    * 
    *  @throws Exception
    */
   public void writeBaseRegisterSVD(Writer writer, boolean standardFormat, Peripheral owner, int index, int indent) throws Exception {
      long offset;
      String name;
      if (index>=0) {
         offset = getAddressOffset(index);
         name   = getCName(index);
      }
      else {
         offset = getAddressOffset();
         name   = getName();
      }
      final String indenter = RegisterUnion.getIndent(indent);
      writer.write(String.format(   indenter+"   <name>%s</name>\n",                     SVD_XML_BaseParser.escapeString(name)));
      if (isKeepAsArray()) {
         writer.write(              indenter+"   <?"+SVD_XML_Parser.KEEPASARRAY_PROCESSING+"?>\n");
      }
      if (isHidden()) {
         writer.write(              indenter+"   <?"+SVD_XML_Parser.HIDE_PROCESSING+"?>\n");
      }
      if (isDoDerivedMacros()) {
         writer.write(              indenter+"   <?"+SVD_XML_Parser.DODERIVEDMACROS_PROCESSING+"?>\n");
      }
      if (isIsolated()) {
         writer.write(              indenter+"   <?"+SVD_XML_Parser.ISOLATE_PROCESSING+"?>\n");
      }
      if ((getDescription() != null) && (getDescription().length() > 0)) {
         String description;
         if ((getDimension()>0) && (index>=0)) {
            description = getCDescription(index);
         }
         else {
            description = getDescription();
         }
         writer.write(String.format(indenter+"   <description>%s</description>\n",       SVD_XML_BaseParser.escapeString(description)));
      }
      writer.write(String.format(   indenter+"   <addressOffset>0x%X</addressOffset>\n", offset));
      if ((owner == null) || (owner.getWidth() != getWidth()) || (fFields.size() == 0)) {
         writer.write(String.format(indenter+"   <size>%d</size>\n",                     getWidth()));
      }
      if ((owner == null) || (owner.getAccessType() != getAccessType()) || (fFields.size() == 0)) {
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

   public final static String LINE_FORMAT                  = "%-47s /**< %04X: %-60s */\n";
   public final static String LINE_FORMAT_NO_DOCUMENTATION = "%-47s\n";

   /**
    *    Writes C code for Register as part of a STRUCT element e.g.<br>
    *    <pre><b>__I  uint8_t  registerName;</pre></b>
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
         writer.write(String.format(LINE_FORMAT, line.toString(), offset, truncateAtNewlineOrTab(getCDescription(-1))));
         return;
      }
      if (isSimpleArray()) {
         line.append(String.format("%-4s %-9s %s;",
               accessPrefix,
               getCSizeName(getWidth()),
               getArrayDeclaration()));
         writer.write(String.format(LINE_FORMAT, line.toString(), offset, truncateAtNewlineOrTab(getCDescription(-1))));
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
            writer.write(String.format(LINE_FORMAT, String.format(baseLine,sIndex)+";",
                  offset, truncateAtNewlineOrTab(format(getCDescription(index), index))));
            writer.flush();
            if (padding>0) {
               registerUnion.fill(offset, padding);
            }
            offset += getDimensionIncrement();
         }
//         line = new StringBuilder(120);
//         line.append(indenter);
//         line.append(String.format("%-4s %-9s %s;",
//               accessPrefix,
//               getCSizeName(getWidth()),
//               getArrayDeclaration()));
//         writer.write(String.format(lineFormat, line.toString(), offset, truncateAtNewlineOrTab(getCDescription(-1))));
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
            String name      = peripheral.getName()+"_"+getArrayName(index);
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

   static final String REGISTER_MACRO_PREFIX =
         "\n/** @name %s - %s */ /** @{ */\n";

   static final String REGISTER_MACRO_SUFFIX =
         "/** @} */\n";

//   static final String REGISTER_MACRO_PREFIX =
//         "/* ------- %-40s ------ */\n";
//
   private void writeFieldMacro(StringBuilder writer, String prefix) throws Exception {
      for (Field field : fFields) {
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
   public void writeHeaderFileFieldMacros(StringBuilder writer, Peripheral peripheral) throws Exception {
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
   public void writeHeaderFileFieldMacros(StringBuilder writer, Peripheral peripheral, String registerPrefix) throws Exception {
      sortFields();
      String formattedName = getFormattedName(-1, peripheral.getName(), this, registerPrefix);
      formattedName = String.format(formattedName, "");
      formattedName.replaceAll("\\[\\]", "");

      StringBuilder sb = new StringBuilder();
      if (getDimension() == 0) {
         // Simple register
         String name = getMappedRegisterMacroName(peripheral.getName())+"_"+formattedName;
         if (name.length() != 0) {
            writeFieldMacro(sb, name);
         }
      }
      else if (isSimpleArray()) {
         // Array
         String name = peripheral.getName()+"_"+formattedName;
         writeFieldMacro(sb, name);
      }
      else {
         String name = getMappedRegisterMacroName(peripheral.getName())+"_"+formattedName;
         // Quietly delete empty names
         if (name.length() != 0) {
            writeFieldMacro(sb, name);
         }
      }
      if (sb.length()>0) {
         writer.append(String.format(REGISTER_MACRO_PREFIX, getSimplifedName(), getBriefCDescription()));
         writer.append(sb.toString());
         writer.append(String.format(REGISTER_MACRO_SUFFIX));
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
      for (Field f:fFields) {
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
         Field oField = fFields.get(outer);
         if (oField.getDerivedFrom() != null) {
            continue;
         }
         for (int inner = outer+1; inner<getFields().size(); inner++) {
            Field iField = fFields.get(inner);
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
   @Override
   public void optimise() {
      if (isExtractSimilarFields()) {
         extractSimilarFields();
      }
      
   }
}
