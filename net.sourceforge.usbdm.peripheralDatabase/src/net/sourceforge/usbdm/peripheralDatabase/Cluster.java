package net.sourceforge.usbdm.peripheralDatabase;
import java.io.IOException;
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

import net.sourceforge.usbdm.packageParser.ReplacementParser;
import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

public class Cluster extends ModeControl implements Cloneable {
   /** Name of cluster */
   private  String               fName;
   
   /** Offset of cluster from start of peripheral */
   private  long                 fAddressOffset;
   
   /** Registers in this cluster */
   private  ArrayList<Cluster>   fRegisters;
   
   /** Address increment of iterated registers */
   private  int                  fDimensionIncrement;
   
   
   private String                fDim = null;

   /** Modifier for iterated register names (%s) */
   private  ArrayList<String>    fDimensionIndexes;
   
   private   AccessType          fAccessType;
   private   long                fResetValue;
   private   long                fResetMask;
   private   Cluster             fRerivedFrom       = null;
   private   String              fBaseName          = "";
   private   String              fNameMacroformat   = "";
   protected Peripheral          fOwner;
   private   boolean             fHidden;
   
   /** Indicates Field Macros should be written even for derived registers */
   private boolean               fDoDerivedMacros = false;

   /** Used to isolate the memory block associated with this register */
   private boolean               fIsolated;

   /** Keep as array in SVD viewer */
   private boolean fKeepAsArray;

   /** Set to provide debug messages */
   private boolean fDebugThis;
   
   Cluster(Peripheral owner) {
      this.fOwner            = owner;
      fName                  = "";
      fAddressOffset         = 0;
      fRegisters             = new ArrayList<Cluster>();
      fDimensionIncrement    = 0;
      fDimensionIndexes      = null;
      fHidden                = false;
      if (owner != null) {
         fAccessType     = owner.getAccessType();
         fResetValue     = owner.getResetValue();
         fResetMask      = owner.getResetMask();
      }
      else {
         fAccessType     =  AccessType.ReadWrite;
         fResetValue     =  0L;
         fResetMask      =  0xFFFFFFFFL;
      }
   }

   /**
    * Returns a relatively shallow copy of the cluster
    * 
    * The following should usually be changed:
    * <ul>
    *    <li>name
    *    <li>addressOffset
    * </ul>
    * Shares (don't modify)
    * <ul>
    *    <li>registers
    *    <li>addressBlock
    * </ul>
    * Copied from this
    * <ul>
    *    <li>derivedFrom == this
    *    <li>baseName
    *    <li>nameMacroformat
    *    <li>dimensionIncrement
    *    <li>dimensionIndexes (shared reference)
    *    <li>accessType
    *    <li>resetValue
    *    <li>resetMask
    *    <li>owner
    *    <li>hidden = false;
    * </ul>
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {

      // Make shallow copy
      Cluster clone = (Cluster) super.clone();

      // Clones should be given a new name
      clone.setName(getName() + "_cluster_clone");
      
      // Record original
      clone.setDerivedFrom(this);
      
      // Don't inherit hidden state
      clone.setHidden(false);
      
      return clone;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      if (getDimension()>0) {
         return String.format("Cluster[%s[%d] o:0x%X, w:%d, s:%d]", getName(), getDimension(), getAddressOffset(), getWidth(), getTotalSizeInBytes());
      }
      return String.format("Cluster[%s o:0x%X, w:%d, s:%d]", getName(), getAddressOffset(), getWidth(), getTotalSizeInBytes());
   }

   /**
    * Find register within cluster
    * 
    * @param name
    * @return
    */
   public Register findRegister(String name) {
      for (Cluster register : fRegisters) {
         if (register instanceof Register) {
            if (name.equalsIgnoreCase(register.getName())) {
               return (Register)register;
            }
         }
         else {
            Cluster cluster = register.findRegister(name);
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
      for (Cluster register : fRegisters) {
         if (register instanceof Register) {
            if (name.equalsIgnoreCase(register.getName())) {
               return register;
            }
         }
         else {
            Cluster cluster = register.findRegister(name);
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
         if (fDimensionIndexes != null) {
            if (index>=fDimensionIndexes.size()) {
               System.err.println("format()");
            }
            String dimensionIndex = fDimensionIndexes.get(index);
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
      
      this.fName         = name;
      Matcher matcher   = pattern.matcher(name);
      fBaseName          = matcher.replaceAll("$1");
      fNameMacroformat   = matcher.replaceAll("$2");
      if (fNameMacroformat.length() == 0) {
         fNameMacroformat = fBaseName;
      }
   }

   /**
    * Get cluster name
    * 
    * @return the name
    */
   public String getName() {
      return fName;
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
      return fBaseName;
   }
   
   /**
    * Gets Cluster macro format<br>
    * If set, this would be something like: <b>"@pTAGVDW@i@f"</b><br>
    */
   public String getNameMacroFormat() {
      return fNameMacroformat;
   }

   /**
    * Get address offset
    * 
    * @return Offset of cluster from start of peripheral
    */
   public long getAddressOffset() {
      return fAddressOffset;
   }

   /**
    * Get address offset for dimensioned cluster
    * 
    * @param index index
    * 
    * @return Offset of cluster from start of peripheral
    */
   public long getAddressOffset(int index) {
      return getAddressOffset()+index*getDimensionIncrement();
   }

   /**
    * Sets the offset of the cluster
    * 
    * @param addressOffset Offset of cluster from start of peripheral
    */
   public void setAddressOffset(long addressOffset) {
      this.fAddressOffset = addressOffset;
   }

   /**
    * Get registers contained in cluster
    * 
    * @return the registers
    */
   public ArrayList<Cluster> getRegisters() {
      return fRegisters;
   }

   /**
    * Get registers contained in cluster in sorted order<br>
    * Note - This sorts the registers!
    * 
    * @return the registers
    */
   public ArrayList<Cluster> getSortedRegisters() {
      sortRegisters();
      return fRegisters;
   }

   /**
    * Add register to cluster
    * 
    * @param fRegisters the registers to set
    */
   public void addRegister(Register register) {
      this.fRegisters.add(register);
   }

   /**
    * Set dimension indices
    * The dimensionIndexes will be split on commas
    * 
    * @param dimensionIndexes Array of indices or null to remove
    */
   public void setDimensionIndexes(ArrayList<String> dimensionIndexes) {
      this.fDimensionIndexes = dimensionIndexes;
      this.fDim = Integer.toString(dimensionIndexes.size());
   }

   /**
    * Set dimension indices
    * The dimensionIndexes will be split on commas
    * 
    * @param dimensionIndexes  String of indices or null to remove
    */
   public void setDimensionIndexes(String dimensionIndexes) {
      if ((dimensionIndexes == null)||(dimensionIndexes.isEmpty())) {
         this.fDimensionIndexes = null;
         return;
      }
      String[] x = dimensionIndexes.split(",", 0);
      this.fDimensionIndexes = new ArrayList<String>(x.length);
      for (int index = 0; index < x.length; index++) {
         String part = x[index];
         Pattern p = Pattern.compile("^\\s*\\[?\\s*(\\d+)\\s*\\-\\s*(\\d+)\\s*\\]?\\s*$");
         Matcher m = p.matcher(part);
         if (m.matches()) {
            int start = Integer.parseInt(m.group(1));
            int end   = Integer.parseInt(m.group(2));
            for (int sub=start; sub<=end; sub++) {
               this.fDimensionIndexes.add(String.valueOf(sub));
            }
         }
         else {
            this.fDimensionIndexes.add(part.trim());
         }
      }
   }

   /**
    * Get dimension indices
    * 
    * @return ArrayList of indices
    */
   public ArrayList<String> getDimensionIndexes() {
      return fDimensionIndexes;
   }

   /**
    * Get dimension indices
    * 
    * @return String representing the indices
    */
   public String getDimensionIndexesAsString() {
      return appendStrings(fDimensionIndexes);
   }
   
   /**
    * Get dimension as integer
    * 
    * @return dimension or zero if not an array
    */
   public int getDimension() {
      return (fDimensionIndexes != null)?fDimensionIndexes.size():0;
   }

   /**
    * Get dimension as string
    * 
    * @return dimension as string e.g. "3" or "$(FILTER_COUNT)*4" or null if not set (not an array)
    */
   public String getDim() {
      return fDim;
   }

   /**
    * Get dimension as string suitable for C expression
    * 
    * @return dimension as string e.g. "3" or "CAN_FILTER_COUNT*4" or null if not set (not an array)
    */
   public String getDimAsExpressionForC() {
      return ReplacementParser.addPrefixToKeys(fDim, fOwner.getHeaderStructName()+"_");
   }

   /**
    * Set dimension as string
    * 
    * @param dim Dimension as string e.g. "%VALUE"
    */
   public void setDim(String dim) {
      fDim = dim;
   }

   /**
    * Get dimension
    * 
    * @param dimension Dimension of array to set.
    * 
    * The array will have numeric indices from 0 to dimension-1
    */
   public void setAutoDimension(int dimension) {
      this.fDimensionIndexes = new ArrayList<String>();
      for (int dim=0; dim<dimension; dim++) {
         this.fDimensionIndexes.add(String.valueOf(dim));
      }
   }

   /**
    * Get dimension increment<br>
    * i.e. how much to increment on each index
    * 
    * @return
    */
   public int getDimensionIncrement() {
      return fDimensionIncrement;
   }

   /**
    * Set dimension increment<br>
    * i.e. how much to increment on each index
    */
   public void setDimensionIncrement(int dimensionIncrement) {
      this.fDimensionIncrement = dimensionIncrement;
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

   /**
    * Returns minimum width of the register/cluster elements
    * 
    * @return width in bits
    */
   public long getWidth() {
      long width = 32;
      for (Cluster reg : fRegisters) {
         if (reg.getWidth()<width) {
            width = reg.getWidth();
         }
      }
      return width;
   }

   /**
    * Get cluster/register from which this cluster/register was derived
    * 
    * @return cluster/register or null
    */
   public Cluster getDerivedFrom() {
      return fRerivedFrom;
   }

   /**
    * Set cluster/register from which this cluster/register was derived
    * 
    * @param derivedFrom cluster/register
    */
   public void setDerivedFrom(Cluster derivedFrom) {
      this.fRerivedFrom = derivedFrom;
   }

   /**
    * Indicates if this cluster/register was derived
    * 
    * @return
    */
   public boolean isDerived() {
      return fRerivedFrom != null;
   }
   

   /**
    * Sets keep as array in SVD viewer
    */
   public void setKeepAsArray(boolean keepAsArray) {
      fKeepAsArray = keepAsArray;
   }

   /**
    * Get keep as array in SVD viewer
    */
   public boolean isKeepAsArray() {
      return fKeepAsArray;
   }
   /**
    * @return the accessType
    */
   public AccessType getAccessType() {
      return fAccessType;
   }

   /**
    * @param accessType the accessType to set
    */
   public void setAccessType(AccessType accessType) {
      this.fAccessType = accessType;
   }

   /**
    * @return the resetValue
    */
   public long getResetValue() {
      return fResetValue;
   }

   /**
    * @param resetValue the resetValue to set
    */
   public void setResetValue(long resetValue) {
      this.fResetValue = resetValue;
   }

   /**
    * @return the resetMask
    */
   public long getResetMask() {
      return fResetMask;
   }

   /**
    * @param resetMask the resetMask to set
    */
   public void setResetMask(long resetMask) {
      this.fResetMask = resetMask;
   }

   public void sortRegisters() {
      sortRegisters(fRegisters);
   }

   /**
    * Checks if this register description agrees with another
    * 
    * @param other               Register to check against
    * @param matchOptions  Used to compare register structure ignoring name and description
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Object _other, int matchOptions) {
      if (!(_other instanceof Cluster)) {
         return false;
      }
      Cluster other = (Cluster)_other;
      if (  (this.getAddressOffset()      != other.getAddressOffset()) ||
            (!this.getName().equals(other.getName()))) {
         return false;
      }
      return equivalentStructure(_other, matchOptions);
   }
   /**
    * Checks if two Clusters are equivalent
    * 
    * @param other register to check against
    * @param ignoreRegisterName  Used to compare register structure ignoring name
    * 
    * @return true if equivalent
    */
   public boolean equivalentStructure(Object _other, int matchOptions) {
      if (!(_other instanceof Cluster)) {
         return false;
      }
      Cluster other = (Cluster) _other;
      if (fRegisters.size() != other.fRegisters.size()) {
         return false;
      }
      if (((fDimensionIndexes == null) && (other.fDimensionIndexes != null)) ||
          ((fDimensionIndexes != null) && (other.fDimensionIndexes == null)) ||
          ((fDimensionIndexes != null) && (other.fDimensionIndexes != null) && (fDimensionIndexes.size() != other.fDimensionIndexes.size()))) {
         return false;
      }
      sortRegisters();
      other.sortRegisters();
      for (int index=0; index<fRegisters.size(); index++) {
         Cluster reg1 = fRegisters.get(index);
         if (!reg1.equivalent(other.fRegisters.get(index), matchOptions)) {
            return false;
         }
      }
      return true;
   }

   public void report() throws Exception {
      for (Cluster register : fRegisters) {
         register.report();
      }
   }

   /**
    * Write dimension list to SVD file
    * 
    *  @param writer          The destination for the XML
    *  @param level           Level of indenting
    *  @param derivedCluster  Cluster derived from (may be null)
    * 
    *  @throws IOException
    */
   void writeSvdDimensionList(Writer writer, String indenter, Cluster derivedCluster) throws IOException {
      if (getDebugThis()) {
         System.err.println("Found it "+fOwner.getName()+":"+getName() );
      }
      
      if (derivedCluster != null) {
         if (getDim() != derivedCluster.getDim()) {
            writer.write(String.format("<dim>%s</dim>", (getDim()==null)?"":getDim()));
         }
         if (getDimensionIncrement() != derivedCluster.getDimensionIncrement()) {
            writer.write(String.format("<dimIncrement>%d</dimIncrement>", getDimensionIncrement()));
         }
         if ((getDim() != null) && !getDim().contains("$")) {
            if (getDimensionIndexes() != derivedCluster.getDimensionIndexes()) {
               writer.write(String.format("<dimIndex>"));
               if (getDimensionIndexes() != null) {
                  boolean doComma = false;
                  for (String s : getDimensionIndexes()) {
                     if (doComma) {
                        writer.write(",");
                     }
                     doComma = true;
                     writer.write(SVD_XML_BaseParser.escapeString(s));
                  }
               }
               writer.write(String.format("</dimIndex>"));
            }
         }
      }
      else if ((getDim() != null) && !getDim().isEmpty()) {
         writer.write(String.format(indenter+"<dim>%s</dim>\n",                       getDim()));
         writer.write(String.format(indenter+"<dimIncrement>%d</dimIncrement>\n",     getDimensionIncrement()));
         if (!getDim().contains("$")) {
            writer.write(String.format(  indenter+"<dimIndex>"));
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
    *    Writes Cluster to writer in SVD format SVD
    * 
    *    @param writer
    *    @param standardFormat
    *    @param owner
    *    @param level
    *    @throws Exception
    */
   public void writeSvd(Writer writer, boolean standardFormat, Peripheral owner, int indent) throws Exception {
      final String indenter = RegisterUnion.getIndent(indent);

      sortRegisters();

      writer.write(                 indenter+"<cluster>\n");
      
      writeSvdDimensionList(writer, indenter+"   ", getDerivedFrom());
      
      writer.write(String.format(   indenter+"   <name>%s</name>\n",                     SVD_XML_BaseParser.escapeString(getName())));
      if (isKeepAsArray()) {
         writer.write(              indenter+"   <?"+SVD_XML_Parser.KEEPASARRAY_PROCESSING+"?>\n");
      }
      if (isHidden()) {
         writer.write(            indenter+"   <?"+SVD_XML_Parser.HIDE_PROCESSING+"?>\n");
      }
      writer.write(String.format(   indenter+"   <addressOffset>0x%X</addressOffset>\n", getAddressOffset()));
      if (isIsolated()) {
         writer.write(            indenter+"   <?"+SVD_XML_Parser.ISOLATE_PROCESSING+"?>\n");
      }
      for (Cluster register : fRegisters) {
         register.writeSvd(writer, standardFormat, owner, indent+3);
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

   private final String CLUSTER_OPEN_STRUCT     = "struct {\n";
   private final String CLUSTER_CLOSE_STRUCT    = "} %s;";

   /**
    *    Writes C code for Cluster as a STRUCT element e.g.<br>
    *    <pre><b>
    *    struct {
    *      __I  uint8_t  registerName;
    *                    ...
    *    };</pre></b>
    * 
    * @param writer        Writer to use
    * @param indent        Current indent level
    * @param registerUnion Not used
    * @param peripheral    Peripheral owning registers
    * @param baseAddress   Base address of enclosing <b>struct</b> to write
    * 
    * @throws Exception
    */
   public void writeHeaderFileDeclaration(Writer writer, int indent, RegisterUnion registerUnion, Peripheral peripheral, long baseAddress) throws Exception {

      final String indenter = RegisterUnion.getIndent(indent);
      sortRegisters();
      RegisterUnion unionRegisters = new RegisterUnion(writer, indent+3, peripheral, baseAddress);
//      writer.write(String.format(Register.lineFormat, indenter+clusterOpenStruct, baseAddress, String.format("(size=0x%04X, %d)", getTotalSizeInBytes(), getTotalSizeInBytes())));
      writer.write(indenter+CLUSTER_OPEN_STRUCT);
      
      for(Cluster cluster : fRegisters) {
         unionRegisters.add(cluster);
      }
      // Flush current union if exists
      unionRegisters.writeHeaderFileUnion();
      
      if (getDimension()>0) {
         // Fill to array boundary
         unionRegisters.fillTo(baseAddress+getDimensionIncrement());
         // Finish off struct as array
//         writer.write(indenter+String.format(clusterCloseStruct, getBaseName()+String.format("[%d]",getDimension())));
         
         writer.write(String.format(Register.LINE_FORMAT, // s # s
               indenter+String.format(CLUSTER_CLOSE_STRUCT, getBaseName()+String.format("[%s]", getDimAsExpressionForC())),
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
         writer.write(String.format(Register.LINE_FORMAT, // s # s
               indenter+String.format(CLUSTER_CLOSE_STRUCT, getBaseName()),
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
            for (Cluster register : fRegisters) {
               if (register.getDimension()>0) {
                  for (int arIndex=0; arIndex<register.getDimension(); arIndex++) {
                     String name = peripheral.getName()+"_"+ getArrayName(index)+"_"+register.getArrayName(arIndex);
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
                  String name = peripheral.getName()+"_"+ getArrayName(index)+"_"+    register.getBaseName();
                  name = ModeControl.getMappedRegisterMacroName(name);
                  writer.write(String.format("#define %-30s (%s)\n",
                        name,
                        peripheral.getName()+"->"+getSimpleArraySubscriptedName(index)+"."+register.getBaseName()));
               }
            }
         }
      }
      else {
         for (Cluster register : fRegisters) {
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
   public void writeHeaderFileFieldMacros(StringBuilder writer, Peripheral peripheral) throws Exception {
      for (Cluster register : fRegisters) {
         Register reg = (Register) register;
         reg.writeHeaderFileFieldMacros(writer, peripheral, getFormattedName(-1, peripheral.getName(), this, ""));
      }
   }

   /**
    * Gets total size of cluster/register in bytes
    * 
    * @return
    */
   public long getTotalSizeInBytes() {
      if ((fDimensionIndexes != null)) {
         // Array - use stride as size
         return getDimensionIncrement() * fDimensionIndexes.size();
      }
      else {
         long size = 0;
         for (Cluster reg : fRegisters) {
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
      if ((fDimensionIndexes != null)) {
         // Array - use stride as element size
         return getDimensionIncrement();
      }
      else {
         long size = 0;
         for (Cluster reg : fRegisters) {
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
    * @param isolatedIndex
    * 
    * @throws Exception
    */
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger, int isolatedIndex, long addressOffset) throws Exception {
      sortRegisters();
//      System.err.println(String.format("Cluster.addAddressBlocks(%s) addressOffset = 0x%04X", getName(), addressOffset));
      addressOffset += getAddressOffset();
      
//      if (isIsolated()) {
//         System.err.println("addAddressBlocks(Isolated cluster " +getName() + ", #" + isolatedIndex + ")");
//      }
      if (getDimension()>0) {
         for (int dimension=0; dimension < getDimension(); dimension++) {
            // Do each dimension of array
            if (isIsolated()) {
               // Isolate each array element
               isolatedIndex = addressBlocksMerger.createNewIsolation();
            }
            for (Cluster cluster : fRegisters) {
               cluster.addAddressBlocks(addressBlocksMerger, isolatedIndex, addressOffset);
            }
            addressOffset += getDimensionIncrement();
         }
      }
      else {
         for (Cluster cluster : fRegisters) {
            cluster.addAddressBlocks(addressBlocksMerger, isolatedIndex, addressOffset);
         }
      }
   }

   /**
    * Adds the register's memory address range to the AddressBlockManager
    * 
    * @param addressBlocksMerger Manager to use
    * @param isolatedIndex
    * 
    * @throws Exception
    */
   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger, int isolatedIndex) throws Exception {
      addAddressBlocks(addressBlocksMerger, isolatedIndex, 0L);
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
   
   final Pattern ARRAY_SUBSCIPT_PATTERN  = Pattern.compile("^(.+)\\[%s\\]$");
   final Pattern SUBSTITUTE_PATTERN      = Pattern.compile("^(.+)%s(.*)$");

   /**
    * Gets the register at given subscript of a register array as a simple name e.g. ADC_RESULT10
    * 
    * @param sIndex Index of register
    * 
    * @return
    * 
    * @throws Exception
    */
   public String getArrayName(int index) {
      fName = getBaseName();
      // Remove existing subscript placeholder
      fName = fName.replaceAll("\\[%s\\]", "");
      if (fName.contains("%s")) {
         return format(fName, index);
      }
      else {
         return format(fName+"%s", index);
      }
   }
   
   /**
    * Gets register declaration e.g. ADC_RESULT[10] or ADC_RESULT[%ADC_SIZE]
    * 
    * @return
    * 
    * @throws Exception
    */
   public String getArrayDeclaration() throws Exception {
      fName = getBaseName();
      // Remove existing subscript placeholder
      fName = fName.replaceAll("\\[%s\\]", "");
      fName = fName.replaceAll("%s", "");
      return fName + "[" + getDimAsExpressionForC() + "]";
   }
   
   /**
    * Gets the register at given subscript of a simple register array as a subscripted name e.g. ADC_RESULT[10]
    * 
    * @param sIndex Index of register
    * 
    * @return
    * @throws RegisterException
    * 
    * @throws Exception if it is not possible to express as simple array using a subscript
    */
   public String getSimpleArraySubscriptedName(int index) throws RegisterException {
      if (!isSimpleArray()) {
         // Trying to treat as simple array!
         throw new RegisterException(String.format("Register is not simple array, name=\'%s\'", getName()));
      }
      fName = getBaseName();
      String sIndex = Integer.toString(index);
      Matcher m;
      m = ARRAY_SUBSCIPT_PATTERN.matcher(fName);
      if (m.matches()) {
         return String.format("%s[%s]", m.group(1), sIndex);
      }
      m = SUBSTITUTE_PATTERN.matcher(fName);
      if (m.matches()) {
         return String.format("%s%s[%s]", m.group(1), m.group(2), sIndex);
      }
      return fName+"["+sIndex+"]";
   }
   
   /**
    * Gets the register at given subscript of a register array as a<br>
    * subscripted name e.g. ADC_RESULT[10] if possible or as a simple name e.g. ADC_RESULT10
    * 
    * @param sIndex Index of register
    * 
    * @return
    * 
    * @throws Exception if it is not possible to express as simple array using a subscript
    */
   public String getArraySubscriptedName(int index) throws RegisterException {
      if (!isSimpleArray()) {
         return getArrayName(index);
      }
      return getSimpleArraySubscriptedName(index);
      }

   /**
    * Used to improve the appearance of a cluster if it only holds a single (visible) register
    * 
    * @return The single visible register found or null if none/too many found
    */
   public Cluster getSingleVisibleRegister() {
      Cluster visibleRegister = null;
      for (Cluster reg : fRegisters) {
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
      return fHidden;
   }

   public void setHidden(boolean hidden) {
      this.fHidden = hidden;
   }

   public void optimise() {
      for (Cluster r:getRegisters()) {
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

   /**
    * Used to isolate the memory block associated with this register
    */
   public void setIsolated() {
      fIsolated           = true;
   }

   /**
    * Indicates to isolate the memory block associated with this register
    * 
    * @param true if isolate
    */
   public boolean isIsolated() {
      return fIsolated;
   }

   public void setDebugThis(boolean b) {
      fDebugThis = b;
   }
   
   public boolean getDebugThis() {
      return fDebugThis;
   }

}
