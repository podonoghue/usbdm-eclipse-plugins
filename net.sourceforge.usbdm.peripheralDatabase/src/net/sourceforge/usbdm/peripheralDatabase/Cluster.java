package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;
import net.sourceforge.usbdm.peripheralDatabase.Peripheral.AddressBlocksMerger;

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
   private  Cluster              derivedFrom = null;

   Cluster(Peripheral owner) {
      name                  = "";
      addressOffset         = 0;
      registers             = new ArrayList<Register>();
      dimensionIncrement    = 0;
      dimensionIndexes      = null;
      sorted                = false;
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
    * @return the name
    */
   public String getName() {
      return name;
   }

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
    * Simple name for cluster 
    */
   String baseName = "";
   /**
    * Format string used to construct name of MACRO for register
    */
   String nameMacroformat   = "";
   
   /**
    * @return the simple name without formatting
    */
   public String getBaseName() {
      return baseName;
   }

   public String getNameMacroFormat() {
      return nameMacroformat;
   }

   /**
    * Formats a string using register number and modifier substitution
    * 
    * @param format Format string to use (may contain %s=register index, %m=dimensionIndex modifier
    * @param index  Index used to select dimensionIndex & modifier
    * @return
    * @throws Exception
    */
   public String format(String format, int index) throws Exception {
      final Pattern pattern = Pattern.compile("(^.*):(.*$)");
      if ((name == null) || ((index>=0) && (dimensionIndexes == null))) {
         throw new Exception("name or dimensionIndexes is null");
      }
      String sIndex   = "";
      String modifier = null;
      if (index>=0) {
         String dimensionIndex = dimensionIndexes.get(index);
         Matcher matcher = pattern.matcher(dimensionIndex);
         if (matcher.matches()) {
            sIndex   = matcher.replaceAll("$1");
            modifier = matcher.replaceAll("$2");
         }
         else {
            sIndex = dimensionIndex;
         }
      }
      String rv =  format.replaceAll("%s", sIndex);
      if (modifier != null) {
         rv =  rv.replaceAll("%m", modifier);
      }
      return rv;
   }

   public String getName(int index) throws Exception {
      return format(getName(), index);
   }

   private static final Pattern pattern = Pattern.compile("^([^,]*),(.*)$");
   
   /**
    * @param name The name to set
    */
   public void setName(String name) {
      this.name = name;
      Matcher matcher = pattern.matcher(name);
      baseName = matcher.replaceAll("$1");
      nameMacroformat   = matcher.replaceAll("$2");
   }

   /**
    * @return the addressOffset
    */
   public long getAddressOffset() {
      return addressOffset;
   }

   /**
    * @return the addressOffset
    */
   public long getAddressOffset(int index) {
      return getAddressOffset()+index*getDimensionIncrement();
   }

   /**
    * @param addressOffset the addressOffset to set
    */
   public void setAddressOffset(long addressOffset) {
      this.addressOffset = addressOffset;
   }

   /**
    * @return the registers
    */
   public ArrayList<Register> getRegisters() {
      return registers;
   }

   /**
    * @return the registers
    */
   public ArrayList<Register> getSortedRegisters() {
      sortRegisters();
      return registers;
   }

   /**
    * @param registers the registers to set
    */
   public void addRegister(Register register) {
      this.registers.add(register);
      sorted = false;
   }

   public int getDimension() {
      return (dimensionIndexes != null)?dimensionIndexes.size():0;
   }

   public int getDimensionIncrement() {
      return dimensionIncrement;
   }

   public void setDimensionIncrement(int dimensionIncrement) {
      this.dimensionIncrement = dimensionIncrement;
   }

   /**
    * Indicates whether the dimensionIndexes are simple sequential numbers
    * 
    * @return
    */
   public boolean checkSequential() {
      boolean isSequentialIndexes = true;
      int currentIndex = 0;
      for (String dimIndex : getDimensionIndexes()) {
         int index;
         try {
            index = Integer.parseInt(dimIndex);
         } catch (NumberFormatException e) {
            isSequentialIndexes = false;
            break;
         }
         if (currentIndex++ != index) {
            isSequentialIndexes = false;
            break;
         }
      }
      return isSequentialIndexes;
   }
   
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
      for (String s: x) {
         s = s.trim();
      }
      this.dimensionIndexes = new ArrayList<String>(Arrays.asList(x));
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
    * Writes Cluster to writer in SVD format SVD
    * 
    * @param writer
    * @param standardFormat
    * @param owner
    * @param level
    * @throws Exception 
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat, Peripheral owner, int indent) throws Exception {
      final String indenter = RegisterUnion.getIndent(indent);

      sortRegisters();
      
      writer.print(                 indenter+"<cluster>\n");
      if ((dimensionIndexes!= null) && (dimensionIndexes.size()>0)) {
         writer.print(String.format(indenter+"   <dim>%d</dim>\n",                       dimensionIndexes.size()));
         writer.print(String.format(indenter+"   <dimIncrement>%d</dimIncrement>\n",     getDimensionIncrement()));
         writer.print(String.format(indenter+"   <dimIndex>%s</dimIndex>\n",             SVD_XML_BaseParser.escapeString(getDimensionIndexesAsString())));
      }
      writer.print(String.format(   indenter+"   <name>%s</name>\n",                     SVD_XML_BaseParser.escapeString(getName())));
      writer.print(String.format(   indenter+"   <addressOffset>0x%X</addressOffset>\n", getAddressOffset()));
      for (Register register : registers) {
         register.writeSVD(writer, standardFormat, owner, indent+3);
      }
      writer.print(                 indenter+"</cluster>\n");
   }


   public static class Pair {
      public Pattern regex;
      public String replacement;
      
      public Pair(Pattern regex, String replacement) {
         this.regex       = regex;
         this.replacement = replacement;
      }
   }
  
   /**
    * Writes a set of macros to allow 'Freescale' style access to registers in this cluster
    * e.g. "#define I2S0_CR3 (I2S0->CR[3])"
    * 
    * @param  writer
    * @param  devicePeripherals
    * @throws Exception
    */
   public void writeHeaderFileRegisterMacro(PrintWriter writer, Peripheral peripheral)  throws Exception{

      String nameFormat = getNameMacroFormat();
      if (getDimension()>0) {
         for(int index=0; index<getDimension(); index++) {
            for (Register register : registers) {
               String name;
               name = nameFormat.replaceAll("@f", register.getName());
               name = name.replaceAll("@i", String.format("%d", index));
               name = name.replaceAll("@a", getBaseName());
               name = name.replaceAll("@p", peripheral.getName()+"_");
               if (name.length() == 0) {
                  continue;
               }
               if (register.getDimension()>0) {
                  for (int arIndex=0; arIndex<register.getDimension(); arIndex++) {
                     String name2 = register.format(name, arIndex);
                     name2 = ModeControl.getMappedRegisterMacroName(name2);
                     writer.print(String.format("#define %-30s (%s->%s[%s].%s)\n", 
                           name2,
                           peripheral.getName(), getBaseName(), dimensionIndexes.get(index), register.getName(arIndex)));
                  }
               }
               else {
                  name = ModeControl.getMappedRegisterMacroName(name);
                  writer.print(String.format("#define %-30s (%s->%s[%s].%s)\n", 
                        name,
                        peripheral.getName(), getBaseName(), dimensionIndexes.get(index), register.getName()));
               }
            }
         }
      }
      else {
         writer.print(String.format("#define %-30s (%s->%s)\n", 
               peripheral.getName()+"_"+getName(), peripheral.getName(), getName()));
      }
   }
   /**
    * Writes a set of macros to allow convenient access to the fields of the registers of this Cluster
    * e.g. "#define PERIPHERAL_FIELD(x)  (((x)<<FIELD_OFFSET)&FIELD_MASK)"
    * 
    * @param  writer
    * @param  devicePeripherals
    * @throws Exception
    */
   public void writeHeaderFileFieldMacros(PrintWriter writer, Peripheral peripheral) throws Exception {
      for (Register register : registers) {
         register.writeHeaderFileFieldMacros(writer, peripheral);
      }
   }

   public boolean isDeleted() {
      return false;
   }

   public void setDeleted(boolean b) {
   }

   private final String clusterOpenStruct     = "struct { /* (cluster) */";
   private final String clusterCloseStruct    = "} %s;\n";

   /**
    * Writes C code for Peripheral declaration e.g. a typedef for a STRUCT representing all the peripheral registers
    * e.g. typedef struct {...} peripheralName_Type;
    * 
    * @param writer
    * @param devicePeripherals
    */
   public void writeHeaderFileDeclaration(PrintWriter writer, int indent, Peripheral peripheral, long baseAddress) throws Exception {

      final String indenter = RegisterUnion.getIndent(indent);
      
      sortRegisters();
      
      RegisterUnion unionRegisters = new RegisterUnion(writer, indent+3, peripheral, baseAddress);
      
      writer.print(String.format(Register.lineFormat, indenter+clusterOpenStruct, baseAddress, String.format("(size=0x%04X, %d)", getTotalSizeInBytes(), getTotalSizeInBytes())));
      
      for(Cluster cluster : registers) {
         unionRegisters.add(cluster);
      }
      // Flush current union if exists
      unionRegisters.writeHeaderFileUnion();
      
      if (getDimension()>0) {
         // Fill to array boundary
         unionRegisters.fillTo(getDimensionIncrement());
         // Finish off struct as array
         writer.print(indenter+String.format(clusterCloseStruct, getBaseName()+String.format("[%d]",getDimension())));
      }
      else {
         // Finish off struct
         writer.print(indenter+String.format(clusterCloseStruct, getBaseName()));
      }
   }

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

   public void addAddressBlocks(AddressBlocksMerger addressBlocksMerger) throws Exception {
      sortRegisters();

      // Offset of this cluster as may be an array
      long addressOffset = getAddressOffset();

      if (getDimension()>0) {
         // Do each dimension of array
         for (int dimension=0; dimension < getDimension(); dimension++) {
            for (Cluster cluster : registers) {
               addressBlocksMerger.addBlock(cluster.getAddressOffset()+addressOffset, cluster.getTotalSizeInBytes(), cluster.getWidth());
            }
            addressOffset += getDimensionIncrement();
         }
      }
      else {
         for (Cluster cluster : registers) {
            addressBlocksMerger.addBlock(cluster.getAddressOffset()+addressOffset, cluster.getTotalSizeInBytes(), cluster.getWidth());
         }
      }
   }

}
