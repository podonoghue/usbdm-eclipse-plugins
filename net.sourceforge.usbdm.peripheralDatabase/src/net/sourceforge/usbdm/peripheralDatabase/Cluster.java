package net.sourceforge.usbdm.peripheralDatabase;
/*
===============================================================================================================
| History                                                                                                      
---------------------------------------------------------------------------------------------------------------
| 19 Jan 2015 | Added support for more complex <dim>'s                                            | V4.10.6.250
===============================================================================================================
*/
import java.io.Writer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

public class Cluster extends ModeControl implements Cloneable {
   
   private  String               name;
   private  long                 addressOffset;
   private  ArrayList<Register>  registers;
   private  int                  dimensionIncrement;  // Address increment of iterated registers
   private  ArrayList<String>    dimensionIndexes;    // Modifier for iterated register names (%s)
   private  boolean              sorted;
   private  AccessType           accessType;
   private  long                 resetValue;
   private  long                 resetMask;
   private  Cluster              derivedFrom       = null;
   private  String               baseName          = "";
   private  String               nameMacroformat   = "";
   protected Peripheral          owner;
   private boolean               hidden;
   
   /** Indicates Field Macros should be written even for derived registers */
   private boolean               fDoDerivedMacros = false;

   Cluster(Peripheral owner) {
      this.owner            = owner;
      name                  = "";
      addressOffset         = 0;
      registers             = new ArrayList<Register>();
      dimensionIncrement    = 0;
      dimensionIndexes      = null;
      sorted                = false;
      hidden                = false;
      if (owner != null) {
         accessType     = owner.getAccessType();
         resetValue     = owner.getResetValue();
         resetMask      = owner.getResetMask();
      }
      else {
         accessType     =  AccessType.ReadWrite;
         resetValue     =  0L;
         resetMask      =  0xFFFFFFFFL;
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
      Cluster clone = (Cluster) super.clone();

      clone.setDerivedFrom(this);
      return clone;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return String.format("Cluster[%s]", getName());
   }

   /**
    * Find register within cluster
    * 
    * @param name
    * @return
    */
   public Register findRegister(String name) {
      for (Cluster register : registers) {
         if (register instanceof Register) {
            if (name.equalsIgnoreCase(register.getName())) {
               return (Register)register;
            }
         }
         else {
            Cluster cluster = ((Cluster)register).findRegister(name);
            if (cluster != null) {
               return (Register)cluster;
            }
         }
      }
      return null;
   }

   /**
    * Find cluster within register
    * 
    * @param name
    * @return
    */
   public Cluster findCluster(String name) {
      for (Cluster register : registers) {
         if (register instanceof Register) {
            if (name.equalsIgnoreCase(register.getName())) {
               return register;
            }
         }
         else {
            Cluster cluster = ((Cluster)register).findRegister(name);
            if (cluster != null) {
               return cluster;
            }
         }
      }
      return null;
   }

   /**
    * Formats a string using register number and modifier substitution<br>
    * The format may contain:
    * <ul>
    * <li>%s = cluster/register index as a number
    * <li>%m = dimensionIndex modifier (a text string)
    * </ul>
    * @param format Format string to use
    * @param index  Index used to select dimensionIndex & modifier
    * 
    * @return
    * @throws Exception
    */
   public String format(String format, int index) {
      final Pattern pattern = Pattern.compile("(^.*):(.*$)");
      String sIndex   = "";
      String modifier = "";
      if (index>=0) {
         if (dimensionIndexes != null) {
            if (index>=dimensionIndexes.size()) {
               System.err.println("format()");
            }
            String dimensionIndex = dimensionIndexes.get(index);
            Matcher matcher       = pattern.matcher(dimensionIndex);
            if (matcher.matches()) {
               sIndex   = matcher.replaceAll("$1");
               modifier = matcher.replaceAll("$2");
            }
            else {
               sIndex = dimensionIndex;
            }
         }
         else {
            sIndex = Integer.toString(index);
         }
      }
      return format.replaceAll("%s", sIndex).replaceAll("%m", modifier);
   }

   /**
    * Sets Cluster name<br>
    * The name may have two components separated by a comma e.g. <b>"TAGVDW,@pTAGVDW@i@f"</b><br>
    * 
    * The first part of the name (or only part) is returned by <b>getBaseName()</b><br>
    * The second part of the name is returned by <b>getNameMacroFormat()</b><br>
    * 
    * @param name The name to set
    */
   public void setName(String name) {
      final Pattern pattern = Pattern.compile("^([^,]*),(.*)$");
      
      this.name         = name;
      Matcher matcher   = pattern.matcher(name);
      baseName          = matcher.replaceAll("$1");
      nameMacroformat   = matcher.replaceAll("$2");
      if (nameMacroformat.length() == 0) {
         nameMacroformat = baseName;
      }
   }

   /**
    * Get cluster name
    * 
    * @return the name
    */
   public String getName() {
      return name;
   }

   /**
    * Returns formatted name i.e. substitutions for %s etc are done
    * 
    * @param index
    * @return
    * @throws Exception
    */
   public String getName(int index) throws Exception {
      return format(getName(), index);
   }

   /** Get register simple name without formatting
    * 
    * @return 
    * @throws Exception 
    */
   public String getBaseName() {
      return baseName;
   }
   
   /**
    * Gets Cluster macro format<br>
    * If set, this would be something like: <b>"@pTAGVDW@i@f"</b><br>
    */
   public String getNameMacroFormat() {
      return nameMacroformat;
   }

   /**
    * Get address offset
    * 
    * @return the addressOffset
    */
   public long getAddressOffset() {
      return addressOffset;
   }

   /**
    * Get address offset for dimensioned cluster
    * 
    * @param index index
    * @return the addressOffset
    */
   public long getAddressOffset(int index) {
      return getAddressOffset()+index*getDimensionIncrement();
   }

   /**
    * Sets the offset of the cluster
    * 
    * @param addressOffset the addressOffset to set
    */
   public void setAddressOffset(long addressOffset) {
      this.addressOffset = addressOffset;
   }

   /**
    * Get registers contained in cluster
    * 
    * @return the registers
    */
   public ArrayList<Register> getRegisters() {
      return registers;
   }

   /**
    * Get registers contained in cluster in sorted order<br>
    * Note - This sorts the registers!
    * 
    * @return the registers
    */
   public ArrayList<Register> getSortedRegisters() {
      sortRegisters();
      return registers;
   }

   /**
    * Add register to cluster
    * 
    * @param registers the registers to set
    */
   public void addRegister(Register register) {
      this.registers.add(register);
      sorted = false;
   }

   /**
    * Get dimension
    * 
    * @return dimension or zero if not an array
    */
   public int getDimension() {
      return (dimensionIndexes != null)?dimensionIndexes.size():0;
   }

   /**
    * Get dimension increment<br>
    * i.e. how much to increment on each index
    *  
    * @return
    */
   public int getDimensionIncrement() {
      return dimensionIncrement;
   }

   /**
    * Set dimension increment<br>
    * i.e. how much to increment on each index
    */
   public void setDimensionIncrement(int dimensionIncrement) {
      this.dimensionIncrement = dimensionIncrement;
   }

   /**
    * Checks whether the dimensionIndexes are simple sequential numbers
    * 
    * @return
    */
   public boolean checkIndexesSequentialFromZero() {
      
      int currentIndex = 0;
      for (String dimIndex : getDimensionIndexes()) {
         int index;
         try {
            index = Integer.parseInt(dimIndex);
         } catch (NumberFormatException e) {
            return false;
         }
         if (currentIndex++ != index) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Changes 
    * 
    * @param dimensionIndexes
    * @return
    */
   public static String appendStrings(ArrayList<String> dimensionIndexes) {
      if (dimensionIndexes == null) {
         return null;
      }
      StringBuffer b = null;
      for (String s : dimensionIndexes) {
         if (b == null) {
            b = new StringBuffer(s);
         }
         else {
            b.append(","+s);
         }
      }
      return b.toString();
   }

   public void setDimensionIndexes(ArrayList<String> dimensionIndexes) {
      this.dimensionIndexes = dimensionIndexes;
   }

   public void setDimensionIndexes(String dimensionIndexes) {
      String[] x = dimensionIndexes.split(",", 0);
      this.dimensionIndexes = new ArrayList<String>(x.length);
      for (int index = 0; index < x.length; index++) {
         String part = x[index];
         Pattern p = Pattern.compile("^\\s*\\[?\\s*(\\d+)\\s*\\-\\s*(\\d+)\\s*\\]?\\s*$");
         Matcher m = p.matcher(part);
         if (m.matches()) {
            int start = Integer.parseInt(m.group(1));
            int end   = Integer.parseInt(m.group(2));
            for (int sub=start; sub<=end; sub++) {
               this.dimensionIndexes.add(String.valueOf(sub));
            }
         }
         else {
            this.dimensionIndexes.add(part.trim());
         }
      }
   }

   public ArrayList<String> getDimensionIndexes() {
      return dimensionIndexes;
   }

   public String getDimensionIndexesAsString() {
      return appendStrings(dimensionIndexes);
   }
   
   /**
    * Returns minimum width of the register/cluster elements
    * 
    * @return width in bits
    */
   public long getWidth() {
      long width = 32;
      for (Register reg : registers) {
         if (reg.getWidth()<width) {
            width = reg.getWidth();
         }
      }
      return width;
   }

   public Cluster getDerivedFrom() {
      return derivedFrom;
   }
   
   public void setDerivedFrom(Cluster derivedFrom) {
      this.derivedFrom = derivedFrom;
   }

   /**
    * @return the sorted
    */
   public boolean isSorted() {
      return sorted;
   }

   /**
    * @param sorted the sorted to set
    */
   public void setSorted(boolean sorted) {
      this.sorted = sorted;
   }

   /**
    * @return the accessType
    */
   public AccessType getAccessType() {
      return accessType;
   }

   /**
    * @param accessType the accessType to set
    */
   public void setAccessType(AccessType accessType) {
      this.accessType = accessType;
   }

   /**
    * @return the resetValue
    */
   public long getResetValue() {
      return resetValue;
   }

   /**
    * @param resetValue the resetValue to set
    */
   public void setResetValue(long resetValue) {
      this.resetValue = resetValue;
   }

   /**
    * @return the resetMask
    */
   public long getResetMask() {
      return resetMask;
   }

   /**
    * @param resetMask the resetMask to set
    */
   public void setResetMask(long resetMask) {
      this.resetMask = resetMask;
   }

   public void sortRegisters() {
      if (sorted) {
         return;
      }
      sortRegisters(registers);
   }

   /**
    * Checks if two Clusters are equivalent
    * 
    * @param other
    * @return
    */
   public boolean equivalent(Cluster other) {
      if (other instanceof Register) {
         return false;
      }
      if (registers.size() != (((Cluster)other).registers.size())) {
         return false;
      }
      if (dimensionIndexes.size() != (((Cluster)other).dimensionIndexes.size())) {
         return false;
      }
      sortRegisters();
      other.sortRegisters();
      for (int index=0; index<registers.size(); index++) {
         Cluster reg1 = registers.get(index);
         Cluster reg2 = other.registers.get(index);
         if ((reg1 instanceof Register) && reg2 instanceof Register) {
            if (!((Register)reg1).equivalent((Register)(other.registers.get(index)))) {
               return false;
            }
         }
         else {
            if (!reg1.equivalent(other.registers.get(index))) {
               return false;
            }
         }
      }
      return true;
   }

   public void report() throws Exception {
      for (Register register : registers) {
         register.report();
      }
   }

   /**
    *    Writes Cluster to writer in SVD format SVD
    * 
    *    @param writer
    *    @param standardFormat
    *    @param owner
    *    @param level
    *    @throws Exception 
    */
   public void writeSVD(Writer writer, boolean standardFormat, Peripheral owner, int indent) throws Exception {
      final String indenter = RegisterUnion.getIndent(indent);

      sortRegisters();

      writer.write(                 indenter+"<cluster>\n");
      if ((dimensionIndexes != null) && (dimensionIndexes.size()>0)) {
         writer.write(String.format(indenter+"   <dim>%d</dim>\n",                       dimensionIndexes.size()));
         writer.write(String.format(indenter+"   <dimIncrement>%d</dimIncrement>\n",     getDimensionIncrement()));
         writer.write(String.format(indenter+"   <dimIndex>%s</dimIndex>\n",             SVD_XML_BaseParser.escapeString(getDimensionIndexesAsString())));
      }
      writer.write(String.format(   indenter+"   <name>%s</name>\n",                     SVD_XML_BaseParser.escapeString(getName())));
      if (isHidden()) {
         writer.write(            indenter+"   <?"+SVD_XML_Parser.HIDE_ATTRIB+"?>\n");
      }
      writer.write(String.format(   indenter+"   <addressOffset>0x%X</addressOffset>\n", getAddressOffset()));
      for (Register register : registers) {
         register.writeSVD(writer, standardFormat, owner, indent+3);
      }
      writer.write(                 indenter+"</cluster>\n");
   }


   public static class Pair {
      public Pattern regex;
      public String replacement;
      
      public Pair(Pattern regex, String replacement) {
         this.regex       = regex;
         this.replacement = replacement;
      }
   }
  
   public boolean isDeleted() {
      return false;
   }

   public void setDeleted(boolean b) {
   }

   private final String clusterOpenStruct     = "struct {\n";
   private final String clusterCloseStruct    = "} %s;";

   /**
    *    Writes C code for Cluster as a STRUCT element e.g.<br>
    *    <pre><b>__I  uint8_t  registerName;</pre></b>
    * 
    *    @param writer
    *    @param registerUnion 
    *    @param devicePeripherals
    */
   public void writeHeaderFileDeclaration(Writer writer, int indent, RegisterUnion registerUnion, Peripheral peripheral, long baseAddress) throws Exception {

      final String indenter = RegisterUnion.getIndent(indent);
      sortRegisters();
      RegisterUnion unionRegisters = new RegisterUnion(writer, indent+3, peripheral, baseAddress);
//      writer.write(String.format(Register.lineFormat, indenter+clusterOpenStruct, baseAddress, String.format("(size=0x%04X, %d)", getTotalSizeInBytes(), getTotalSizeInBytes())));
      writer.write(indenter+clusterOpenStruct);
      
      for(Cluster cluster : registers) {
         unionRegisters.add(cluster);
      }
      // Flush current union if exists
      unionRegisters.writeHeaderFileUnion();
      
      if (getDimension()>0) {
         // Fill to array boundary
         unionRegisters.fillTo(getDimensionIncrement());
         // Finish off struct as array
//         writer.write(indenter+String.format(clusterCloseStruct, getBaseName()+String.format("[%d]",getDimension())));
         
         writer.write(String.format(Register.lineFormat, // s # s
               indenter+String.format(clusterCloseStruct, getBaseName()+String.format("[%d]",getDimension())),
               baseAddress,
               String.format("(cluster: size=0x%04X, %d)", getTotalSizeInBytes(), getTotalSizeInBytes())));
         writer.flush();       
//                String.format("(size=0x%04X, %d)", getTotalSizeInBytes(), getTotalSizeInBytes())));
//         writer.write(indenter+String.format(clusterCloseStruct, getBaseName()+String.format("[%d]",getDimension())) + 
//               String.format(Register.lineFormat, baseAddress, String.format("(size=0x%04X, %d)", getTotalSizeInBytes(), getTotalSizeInBytes())));
      }
      else {
         // Finish off struct
//         writer.write(indenter+String.format(clusterCloseStruct, getBaseName()));
         writer.write(String.format(Register.lineFormat, // s # s
               indenter+String.format(clusterCloseStruct, getBaseName()),
               baseAddress,
               String.format("(cluster: size=0x%04X, %d)", getTotalSizeInBytes(), getTotalSizeInBytes())));
         writer.flush();       
      }
   }

   /**
    *    Writes a macro to allow 'Freescale' style access to the registers of the peripheral<br>
    *    e.g. <pre><b>#define I2S0_CR3 (I2S0->CR[3])</b></pre>
    * 
    *    @param  writer
    *    @param  devicePeripherals
    *    @throws Exception
    */
   public void writeHeaderFileRegisterMacro(Writer writer, Peripheral peripheral)  throws Exception{
      writeHeaderFileRegisterMacro(writer, peripheral, "");
   }
   
   /**
    * Does substitution on the register name <pre>
    * "@p" => Peripheral name + "_"
    * "@a" => Cluster base name (name without formatting)
    * "@f" => Register name
    * "@i" => index<br>
    * e.g. "@pTAGVDW@i@f" => "FMC_TAGVDW1S0"
    *</pre> 
    * @param index
    * @param peripheralName
    * @param register
    * @param registerPrefix
    * @return
    * @throws Exception
    */
   public String getFormattedName(int index, String peripheralName, Cluster register, String registerPrefix) throws Exception {
      String name = getNameMacroFormat();
      name = name.replaceAll("@f", registerPrefix+register.getBaseName());
      name = name.replaceAll("@a", getBaseName());
      name = name.replaceAll("@p", peripheralName+"_");
      if (name.contains("@i")) {
         String sIndex = "";
         if (index>=0) {
            sIndex = getDimensionIndexes().get(index);
         }
         name = name.replaceAll("@i", sIndex);
      }
      return name;
   }
   
   /**
    *    Writes a macro to allow 'Freescale' style access to the registers of the peripheral<br>
    *    e.g. <pre><b>#define I2S0_CR3 (I2S0->CR[3])</b></pre>
    * 
    *    @param  writer
    *    @param  devicePeripherals
    *    @param  registerPrefix             Prefix to add to peripheral name
    *    @throws Exception
    */
   public void writeHeaderFileRegisterMacro(Writer writer, Peripheral peripheral, String registerPrefix)  throws Exception{
      if (getDimension()>0) {
         for(int index=0; index<getDimension(); index++) {
            for (Register register : registers) {
               if (register.getDimension()>0) {
                  for (int arIndex=0; arIndex<register.getDimension(); arIndex++) {
                     String name = peripheral.getName()+"_"+ getSimpleArrayName(index)+"_"+register.getSimpleArrayName(arIndex);
                     name = ModeControl.getMappedRegisterMacroName(name);
//                     writer.write(String.format("#define %-30s (%s->%s[%s].%s)\n", 
//                           name2,
//                           peripheral.getName(), getBaseName(), dimensionIndexes.get(index), register.getSimpleArraySubscriptedName(arIndex)));
                     writer.write(String.format("#define %-30s (%s)\n", 
                           name,
                           peripheral.getName()+"->"+getSimpleArraySubscriptedName(index)+"."+register.getSimpleArraySubscriptedName(arIndex)));
                  }
               }
               else {
                  String name = peripheral.getName()+"_"+ getSimpleArrayName(index)+"_"+    register.getBaseName();
                  name = ModeControl.getMappedRegisterMacroName(name);
                  writer.write(String.format("#define %-30s (%s)\n", 
                        name,
                        peripheral.getName()+"->"+getSimpleArraySubscriptedName(index)+"."+register.getBaseName()));
               }
            }
         }
      }
      else {
         for (Register register : registers) {
            register.writeHeaderFileRegisterMacro(writer, peripheral, registerPrefix+getName());
         }
      }
   }

   /**
    *    Writes a set of MACROs to allow convenient operations on the fields of the registers of this cluster e.g.
    *    <pre><b>#define PERIPHERAL_FIELD(x)    (((x)&lt;&lt;FIELD_OFFSET)&FIELD_MASK)</b></pre>
    *    <pre><b>#define FP_CTRL_NUM_LIT_MASK   (0x0FUL << FP_CTRL_NUM_LIT_SHIFT)     </b></pre>
    * 
    *    @param  writer
    *    @param  devicePeripherals
    *    @throws Exception
    */
   public void writeHeaderFileFieldMacros(Writer writer, Peripheral peripheral) throws Exception {
      for (Register register : registers) {
         register.writeHeaderFileFieldMacros(writer, peripheral, getFormattedName(-1, peripheral.getName(), this, ""));
      }
   }

   /**
    * Gets total size of cluster/register in bytes
    * 
    * @return
    */
   public long getTotalSizeInBytes() {
      if ((dimensionIndexes != null)) {
         // Array - use stride as size
         return getDimensionIncrement() * dimensionIndexes.size();
      }
      else {
         long size = 0;
         for (Cluster reg : registers) {
            long regEnd = reg.getAddressOffset()+reg.getTotalSizeInBytes();
            if (regEnd > size) {
               size = regEnd;
            }
         }
         return size;
      }
   }

   /**
    * Gets size of cluster/register element in bytes
    * 
    * @return
    */
   public long getElementSizeInBytes() {
      if ((dimensionIndexes != null)) {
         // Array - use stride as element size
         return getDimensionIncrement();
      }
      else {
         long size = 0;
         for (Cluster reg : registers) {
            long regEnd = reg.getAddressOffset()+reg.getTotalSizeInBytes();
            if (regEnd > size) {
               size = regEnd;
            }
         }
         return size;
      }
   }

   /**
    * Adds the register's memory address range to the AddressBlockManager
    *     
    * @param addressBlocksMerger Manager to use
    * @param addressOffset       Offset for base of register (needed for arrays etc)
    * 
    * @throws Exception
    */
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger, long addressOffset) throws Exception {
      sortRegisters();
//      System.err.println(String.format("Cluster.addAddressBlocks(%s) addressOffset = 0x%04X", getName(), addressOffset));
      addressOffset += getAddressOffset();
      
      if (getDimension()>0) {
         // Do each dimension of array
         for (int dimension=0; dimension < getDimension(); dimension++) {
            for (Cluster cluster : registers) {
               cluster.addAddressBlocks(addressBlocksMerger, addressOffset);
            }
            addressOffset += getDimensionIncrement();
         }
      }
      else {
         for (Cluster cluster : registers) {
            cluster.addAddressBlocks(addressBlocksMerger, addressOffset);
         }
      }
   }

   /**
    * Adds the register's memory address range to the AddressBlockManager
    *     
    * @param addressBlocksMerger Manager to use
    * 
    * @throws Exception
    */
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger) throws Exception {
      addAddressBlocks(addressBlocksMerger, 0);
   }
 
   /**
    * Check if the cluster can be expressed as a simple array using subscripts starting at 0<p>
    * It checks the name, dimensionIndexes, dimIncrement and width<p>
    * 
    * Register should be able to be declared as e.g. uint8_t ADC_RESULT[10]; 
    */
   public boolean isSimpleArray() throws RegisterException {
      if (getDimension() == 0) {
         return false;
      }
//      if (getDimensionIncrement() != (getWidth()+7)/8) {
//         return false;
//      }
      boolean indexesSequentialFromZero = checkIndexesSequentialFromZero();
      if (getName().matches("^.+\\[%s\\]$")) {
         // MUST be a simple register array
         if (!indexesSequentialFromZero) {
            throw new RegisterException(String.format("Register name implies simple array but dimensions not consecutive, name=\'%s\', dimIndexes=\'%s\'", getName(), getDimensionIndexes().toString()));
         }
         return true;
      }
      return indexesSequentialFromZero;
   }
   
   final Pattern arraySubscriptPattern  = Pattern.compile("^(.+)\\[%s\\]$");
   final Pattern substitutePattern      = Pattern.compile("^(.+)%s(.*)$");
   
   /**
    * Gets the register at given subscript of a simple register array as a simple name e.g. ADC_RESULT10
    * 
    * @param name   Name to use as base name
    * @param sIndex Index of register
    * 
    * @return
    * @throws Exception
    */
   String getSimpleArrayName(String name, int index) {
      name = name.replaceAll("\\[%s\\]", "");
      if (name.contains("%s")) {
         return format(name, index);
      }
      else {
         return format(name+"%s", index);
      }
   }
   
   /**
    * Gets the register at given subscript of a simple register array as a simple name e.g. ADC_RESULT10
    * 
    * @param sIndex Index of register
    * 
    * @return
    * @throws Exception
    */
   public String getSimpleArrayName(int index) throws Exception {
      return getSimpleArrayName(getBaseName(), index);
   }
   
   /**
    * Gets the register at given subscript of a simple register array as a subscripted name e.g. ADC_RESULT[10]
    * 
    * @param name   Name to use as base name
    * @param sIndex Index of register
    * 
    * @return
    * @throws RegisterException 
    * 
    * @throws Exception if it is not possible to express as simple array using a subscript
    */
   public String getSimpleArraySubscriptedName(String name, int index) throws RegisterException {
      if (!isSimpleArray()) {
         // Trying to treat as simple array!
         throw new RegisterException(String.format("Register is not simple array, name=\'%s\'", getName()));
      }
      String sIndex = Integer.toString(index);
      Matcher m;
      m = arraySubscriptPattern.matcher(name);
      if (m.matches()) {
         return String.format("%s[%s]", m.group(1), sIndex);
      }
      m = substitutePattern.matcher(name);
      if (m.matches()) {
         return String.format("%s%s[%s]", m.group(1), m.group(2), sIndex);
      }
      return name+"["+sIndex+"]";
   }
   
   /**
    * Gets the register at given subscript of a simple register array as a subscripted name e.g. ADC_RESULT[10]
    * 
    * @param sIndex Index of register
    * 
    * @return
    * 
    * @throws Exception if it is not possible to express as simple array using a subscript
    */
   public String getSimpleArraySubscriptedName(int index) throws Exception {
      return getSimpleArraySubscriptedName(getBaseName(), index);
   }
   
   /**
    * Gets the register at given subscript of a simple register array as a<br>
    * subscripted name e.g. ADC_RESULT[10] if possible or as a simple name e.g. ADC_RESULT10
    * 
    * @param sIndex Index of register
    * 
    * @return
    * 
    * @throws Exception if it is not possible to express as simple array using a subscript
    */
   public String getArraySubscriptedName(String name, int index) throws RegisterException {
      if (!isSimpleArray()) {
         return getSimpleArrayName(name, index);
      }
      return getSimpleArraySubscriptedName(name, index);
   }

   /**
    * Gets the register at given subscript of a simple register array as a<br>
    * subscripted name e.g. ADC_RESULT[10] if possible or as a simple name e.g. ADC_RESULT10
    * 
    * @param sIndex Index of register
    * 
    * @return
    * 
    * @throws Exception if it is not possible to express as simple array using a subscript
    */
   public String getArraySubscriptedName(int index) throws RegisterException {
      return getArraySubscriptedName(getBaseName(), index);
   }

   /**
    * Used to improve the appearance of a cluster if it only holds a single (visible) register
    * 
    * @return The single visible register found or null if none/too many found
    */
   public Register getSingleVisibleRegister() {
      Register visibleRegister = null;
      for (Register reg : registers) {
         if (!reg.isHidden()) {
            if (visibleRegister != null) {
               // Too many
               return null;
            }
            visibleRegister = reg;
         }
      }
      // Return the one found
      return visibleRegister;
   }

   public boolean isHidden() {
      return hidden;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   public void optimise() {
      for (Register r:getRegisters()) {
         r.optimise();
      }
   }

   /**
    * Check if an array with non-consecutive indexes<br>
    * Register will be written as individual elements
    * 
    * @return  True if a complex array
    * 
    * @throws RegisterException
    */
   public boolean isComplexArray() throws RegisterException {
      return (getDimension()>0)&&!isSimpleArray();
   }

   /**
    * Controls whether field macros should be written even for derived registers 
    *
    * @param doDerivedMacros
    */
   public void setDoDerivedMacros(boolean doDerivedMacros) {
      fDoDerivedMacros = doDerivedMacros;
   }

   /**
    * Indicates if Field Macros should be written even for derived registers 

    * @return
    */
   public boolean isDoDerivedMacros() {
      return fDoDerivedMacros;
   }

}
