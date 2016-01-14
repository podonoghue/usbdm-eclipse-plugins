package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

public class Peripheral extends ModeControl implements Cloneable {

   private String                    name              = "";
   private String                    description       = "";
   private long                      baseAddress       = 0;
   private String                    groupName         = "";
   private String                    sourceFilename    = null;
   private String                    prependToName     = "";
   private String                    appendToName      = "";
   private String                    headerStructName  = "";
   private ArrayList<InterruptEntry> interrupts        = new ArrayList<InterruptEntry>();
   private ArrayList<String>         usedBy            = new ArrayList<String>();
   private boolean                   sorted            = false;
   private Peripheral                derivedFrom       = null;

   // The following cannot be changed if derived
   private long                      width;
   private AccessType                accessType;
   private long                      resetValue;
   private long                      resetMask;
   private int                       preferredAccessWidth   = 0;
   private int                       forcedBlockMutiple     = 0;
   private ArrayList<AddressBlock>   addressBlocks          = new ArrayList<AddressBlock>();
   private ArrayList<Cluster>        registers              = new ArrayList<Cluster>();
   private boolean                   refreshAll;  
   private static HashSet<String>    conflictedNames        = new HashSet<String>();
   private static HashSet<String>    typedefsTable          = new HashSet<String>();
   

   static class RegisterPair {
      public Register cReg;
      public Register sReg;
      
      public RegisterPair(Cluster c, Cluster s) {
         cReg = (Register)c;
         sReg = (Register)s;
      }
   }
   
   /**
    * A bit of a hack to fold together registers than wrongly appear twice<br>
    * Uses the name only to decide
    */
   public void foldRegisters() {
      ArrayList<RegisterPair> deletedRegisters = new ArrayList<RegisterPair>();
      for (Cluster c1:registers) {
         for (Cluster c2:registers) {
            if ((c1 != c2) && 
                (c1 instanceof Register) && (c2 instanceof Register) &&
                c1.getName().matches("^"+Pattern.quote(c2.getName())+"%s$")) {
//               System.err.println("foldRegisters() - " + c1.getName() + ", " + c2.getName() );
               // Assume actually the same register
               deletedRegisters.add(new RegisterPair(c1, c2));
            }
         }
      }
      for(RegisterPair rp:deletedRegisters) {
         System.err.println("foldRegisters() - " + rp.cReg.getName() + ", " + rp.sReg.getName() );
         registers.remove(rp.sReg);
         for (Field f:rp.sReg.getFields()) {
            rp.cReg.addField(f);
         }
      }
   }
   
   public Peripheral(DevicePeripherals owner) {
      if (owner != null) {
         width          = owner.getWidth();
         accessType     = owner.getAccessType();
         resetValue     = owner.getResetValue();
         resetMask      = owner.getResetMask();
      }
      else {
         width          =  32;
         accessType     =  AccessType.ReadWrite;
         resetValue     =  0L;
         resetMask      =  0xFFFFFFFFL;
      }
   }
   
   /**
    * Returns a relatively shallow copy of the peripheral
    * The following may be changed:
    *    - name
    *    - description
    *    - baseAddress
    *    - groupName
    *    - sourceFilename
    *    - prependToName
    *    - appendToName
    *    - usedBy
    *    - interrupts
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {

      // Make shallow copy
      Peripheral clone = (Peripheral) super.clone();
      
      clone.setDerivedFrom(this);
      clone.setSourceFilename(null);
      return clone;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return String.format("Peripheral{%s}",  getName());
   }

   /**
    * @return the appendToName
    */
   public String getAppendToName() {
      return appendToName;
   }

   /**
    * @param appendToName the appendToName to set
    */
   public void setAppendToName(String appendToName) {
      this.appendToName = appendToName;
   }

   /**
    * Get list of registers - The registers are sorted if necessary
    * 
    * @return
    */
   public ArrayList<Cluster> getSortedRegisters() {
      sortRegisters();
      return registers;
   }

   /**
    * Get list of registers
    * 
    * @return
    */
   public ArrayList<Cluster> getRegisters() {
      return registers;
   }

   /**
    * Get name of peripheral
    * 
    * @return
    */
   public String getName() {
      return name;
   }

   /**
    * Set name of peripheral
    * 
    * @param name
    */
   public void setName(String name) {
      name = getMappedPeripheralName(name);
      this.name = name;
   }

   /**
    * Set the name to use for the header file struct for this device
    * 
    * @param name
    */
   public void setHeaderStructName(String name) {
      headerStructName = name;
   }

   /**
    * Set the name to use for the header file struct for this device
    * 
    * @return name
    */
   public String getHeaderStructName() {
      if (derivedFrom != null) {
         return derivedFrom.getHeaderStructName();
      }
      if ((headerStructName != null) && (!headerStructName.isEmpty())) {
         return headerStructName;
      }
      headerStructName = getStructNamefromName(name);
      return headerStructName;
   }
   
   /**
    * Get the name of the file containing this description
    * 
    * @return
    */
   public String getSourceFilename() {
      return sourceFilename;
   }

   /**
    * Set the name of the file containing this description
    * 
    */
   public void setSourceFilename(String sourceFilename) {
      this.sourceFilename = sourceFilename;
   }

   /** 
    * Get description of peripheral
    * 
    * @return
    */
   public String getDescription() {
      return description;
   }

   /**
    * Get description of peripheral
    * 
    * @return
    */
   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }
   
   /*
    * Strips a trailing acronym in brackets from the description e.g. (SPI0)
    */
   private String simplifyDescription(String s) {
      final Pattern p = Pattern.compile("(^.*?)\\s*\\([A-Z0-9\\s]*\\)\\s*$");
      Matcher m = p.matcher(description);
      String ss = m.replaceAll("$1");
//      if (ss != s) {
//         System.err.println("simplifyDescription() \""+s+"\"\n"
//                          + "                   => \""+ss+"\"");
//      }
      return ss;
   }
   
   /** 
    * Set description of peripheral
    * 
    */
   public void setDescription(String description) {
      this.description = getSanitizedDescription(description.trim());
      this.description = simplifyDescription(this.description);
   }

   /**
    * Get prefix for register names
    * 
    * @param name
    */
   public String getPrependToName() {
      return prependToName;
   }

   /**
    * Set prefix for register names
    * 
    */
   public void setPrependToName(String prependToName) {
      this.prependToName = prependToName;
   }

   /**
    * Get base address of peripheral
    * All registers are relative to this address
    * 
    * @return
    */
   public long getBaseAddress() {
      return baseAddress;
   }

   /**
    * Get base address of peripheral
    * All registers are relative to this address
    * 
    * @return
    */
   public void setBaseAddress(long baseAddress) {
      this.baseAddress = baseAddress;
   }

   /**
    * Get list of address blocks that describe the mmeory occupied by the peripheral
    * 
    * @return
    */
   public ArrayList<AddressBlock> getAddressBlocks() {
      return addressBlocks;
   }

   /** 
    * Clears (removes) the current address blocks
    * 
    * @throws Exception 
    */
   public void clearAddressBlocks() throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot remove address blocks from derived peripheral");
      }
      this.addressBlocks = new ArrayList<AddressBlock>();
   }
   
   /**
    * Adds an address block to the peripheral
    * 
    * @param addressBlock
    * 
    * @throws Exception
    */
   public void addAddressBlock(AddressBlock addressBlock) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot add address blocks to derived peripheral");
      }
      this.addressBlocks.add(addressBlock);
   }

   /** Add register to peripheral
    * 
    * @param cluster
    * 
    * @throws Exception
    */
   public void addRegister(Cluster cluster) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot addd register to derived peripheral");
      }
      registers.add(cluster);
      sorted = false;
   }

   /**
    * Creates a set of address blocks to cover the current register set
    * 
    * @throws Exception 
    */
   public void createAddressBlocks() throws Exception {
      if (derivedFrom != null) {
         // Address blocks are determined from registers which are determined by the peripheral
         // derived from so they should always agree.
         return;
      }
      //      System.err.println("Creating address blocks for ============= " + getName());
      sortRegisters();
      try {
         clearAddressBlocks();
         AddressBlocksMerger addressBlocksMerger = new AddressBlocksMerger(this);
         for (Cluster cluster : registers) {
//            System.err.println("Peripheral = " + cluster.getName());
            cluster.addAddressBlocks(addressBlocksMerger);
         }
         addressBlocksMerger.finalise();
      } catch (Exception e) {
         System.err.println("Peripheral = " + getName());
         e.printStackTrace();
         throw e;
      }
   }
   
   /**
    * Get group name associated with this peripheral
    * 
    * @return
    */
   static final Pattern groupNamePattern = Pattern.compile("(^.*?)[0-9]*$");

   public String getGroupName() {
      if ((groupName != null) && (!groupName.isEmpty())) {
         return groupName;
      }
      if (derivedFrom != null) {
         return derivedFrom.getGroupName();
      }
      Matcher matcher = groupNamePattern.matcher(name);
      if (matcher.matches()) {
         groupName = matcher.replaceAll("$1");
//         System.err.println(String.format("'%s' => '%s'",  name, groupName));
         return groupName;
      }
      return name;
   }

   /**
    * Set group name associated with this peripheral
    * 
    * @return
    */
   public void setGroupName(String groupName) {
      this.groupName = groupName;
   }

   /**
    * Get (default) register width associated with this peripheral
    * 
    * @return
    */
   public long getWidth() {
      return width;
   }

   /**
    * Get (default) register width associated with this peripheral
    * 
    * @return
    * @throws Exception 
    */
   public void setWidth(long size) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot set default width for derived peripheral");
      }
      this.width = size;
   }   

   /**
    * Get (default) register access type for this peripheral
    * 
    * @return the accessType
    */
   public AccessType getAccessType() {
      return accessType;
   }

   /**
    * Set (default) register access type for this peripheral
    * 
    * @return the accessType
    * @throws Exception 
    */
   public void setAccessType(AccessType accessType) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot set accesstype for derived peripheral");
      }
      this.accessType = accessType;
   }

   /**
    * Get (default) register reset value for this peripheral
    * 
    * @return the resetValue
    */
   public long getResetValue() {
      return resetValue;
   }

   /**
    * Set (default) register reset value for this peripheral
    * 
    * @param resetValue
    * @throws Exception 
    */
   public void setResetValue(long resetValue) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot set default reset value for derived peripheral");
      }
      this.resetValue = resetValue;
   }

   /**
    * Get (default) register reset value for this peripheral
    * 
    * @return the resetMask
    */
   public long getResetMask() {
      return resetMask;
   }

   /**
    * Set (default) register reset mask for this peripheral
    * 
    * @param resetMask the resetMask to set
    * @throws Exception 
    */
   public void setResetMask(long resetMask) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot set default reset mask for derived peripheral");
      }
      this.resetMask = resetMask;
   }

   /**
    * Indicates the preferred access width for address blocks<br>
    * Address blocks will be merged ignoring individual widths
    * 
    * @return Preferred access width for address blocks
    */
   public int getPreferredAccessWidth() {
      return preferredAccessWidth;
   }

   /**
    * Sets the preferred access width for address blocks<br>
    * Address blocks will be merged ignoring individual widths (if non-zero)
    * 
    * @param Preferred access width for address blocks
    * @throws Exception 
    */
   public void setBlockAccessWidth(int preferredAccessWidth) throws Exception {
      this.preferredAccessWidth = preferredAccessWidth;
      if (derivedFrom != null) {
         throw new Exception("Cannot set default access width for derived peripheral");
      }
   }

   /**
    * Indicates the forced access width for address blocks. 
    * Address blocks will be rounded up to this size and merged ignoring individual widths
    * 
    * @return Forced access width for address blocks
    */
   public int getForcedBlockMultiple() {
      return forcedBlockMutiple;
   }

   /**
    * Sets the forced access width for address blocks. 
    * Address blocks will be rounded up to this size and merged ignoring individual widths
    * 
    * @param Forced access width for address blocks
    * @throws Exception 
    */
   public void setForcedBlockMultiple(int forcedBlockMutiple) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot set forced access wdth for derived peripheral");
      }
      this.forcedBlockMutiple = forcedBlockMutiple;
   }

   public Peripheral getDerivedFrom() {
      return derivedFrom;
   }

   /**
    * Sets this device a being derived from another peripheral.
    * 
    * @param derivedFrom The device derived from
    */
   public void setDerivedFrom(Peripheral derivedFrom) {
      this.derivedFrom = derivedFrom;
   }

   /**
    * Sorts usedBy list and removed duplicates
    */
   public void sortUsedBy() {
      sortUsedBy(usedBy);
   }

   /**
    * Sorts usedBy list and removed duplicates
    */
   public static void sortUsedBy(ArrayList<String> usedBy) {
      Collections.sort(usedBy, new Comparator<String>() {
         @Override
         public int compare(String s1, String s2) {
            Pattern p1 = Pattern.compile("^(.*[(DX)|(DN|(FX)|(FN)|Z])(\\d+)(.*)$");
            Pattern p2 = Pattern.compile("^(.*[DX)|(DN|(FX)|(FN)|Z])(\\d+)(.*)$");
            Matcher m1 = p1.matcher(s1);
            Matcher m2 = p2.matcher(s2);
            String p1a = m1.replaceAll("$1$3");
            String p1b = m1.replaceAll("$2");
            String p2a = m2.replaceAll("$1$3");
            String p2b = m2.replaceAll("$2");
            int compare = (p1a.compareTo(p2a));
            if (compare != 0) {
               return compare;
            }
            try {
               return Integer.parseInt(p1b) - Integer.parseInt(p2b);
            } catch (NumberFormatException e) {
               System.err.println("sortUsedBy() s1 = "+s1+", s2 = "+s2);
               System.err.println("sortUsedBy() P1 = "+p1a+":"+p1b+", p2 = "+p2a+":"+p2b);
               e.printStackTrace();
            }
            return compare;
         }
      });
      int insertionIndex = 0;
      int size = usedBy.size();
      for (int index = 0; index<size; index++) {
         if (!usedBy.get(index).equals(usedBy.get(insertionIndex))) {
            usedBy.set(++insertionIndex, usedBy.get(index));
         }
      }
      while (--size>insertionIndex) {
         usedBy.remove(size);
      }
   }
   
   /**
    * Returns the list of devices using this peripheral
    * 
    * @return
    */
   public ArrayList<String> getUsedBy() {
      sortUsedBy();
      return usedBy;
   }

   /**
    * Add record that this peripheral is used by a particular device
    * 
    * @param usedBy
    */
   public void addUsedBy(String usedBy) {
      
      this.usedBy.add(usedBy);
   }

   /**
    * Add record that this peripheral is used by a particular device
    * 
    * @param usedBy
    */
   public void clearUsedBy() {
      
      this.usedBy = new ArrayList<String>();
   }

   /**
    * Add interrupt entry for this peripheral 
    * 
    * @param entry
    */
   void addInterruptEntry(InterruptEntry entry) {
      interrupts.add(entry);
   }

   /**
    * Clear interrupt entries for this peripheral 
    */
   void clearInterruptEntries() {
      interrupts = new ArrayList<InterruptEntry>();
   }

   /** 
    * Get list of interrupt entries for this peripheral
    * 
    * @return
    */
   ArrayList<InterruptEntry> getInterruptEntries() {
      return interrupts;
   }
   
   /**
    * Check if this peripheral is equivalent to another
    * 
    * @param other Other peripheral to compare against.
    * @return outcome of comparison
    */
   public boolean equivalent(Peripheral other) {
      boolean rv = 
            getName().equals(other.getName()) &&
            getDescription().equals(other.getDescription()) &&
            this.prependToName.equals(other.prependToName) &&
            (this.baseAddress == other.baseAddress) &&
            this.appendToName.equals(other.appendToName) &&
            this.getGroupName().equals(other.getGroupName());
      if (!rv) {
         return false;
      }
      if ((this.derivedFrom == null) && (other.derivedFrom == null)) {
         // Both not derived - compare structure
         rv = equivalentStructure(other);
      }
      else if ((this.derivedFrom != null) && (other.derivedFrom != null)) {
         // Both derived - check if derived from same peripheral
         rv = this.derivedFrom.getName().equals(other.derivedFrom.getName());
      }
      else {
         rv = false;
      }
      if (!rv) {
         return false;
      }
      if (getInterruptEntries() == other.getInterruptEntries()) {
         return true;
      }
      if ((getInterruptEntries() == null) || (other.getInterruptEntries() == null)) {
         return false;
      }
      if (getInterruptEntries().size() != other.getInterruptEntries().size()) {
         return false;
      }
      sortInterrupts();
      for (int index=0; index<interrupts.size(); index++) {
         InterruptEntry int1 = interrupts.get(index);
         InterruptEntry int2 = other.interrupts.get(index);
            if (!int1.equals(int2)) {
               return false;
            }
      }
      return rv;
   }
   
   /**
    * Check is this peripheral is equivalent in structure to another.
    * This basically means they may differ in:
    * name, groupName, prependToName, appendToName, description, base address and interrupt information but
    * are otherwise equivalent.
    * 
    * @param other Other peripheral to compare against.
    * @return outcome of comparison
    */
   public boolean equivalentStructure(Peripheral other) {
      boolean rv = 
            (this.width       == other.width) &&
            (this.resetValue  == other.resetValue) &&
            (this.resetMask   == other.resetMask) &&
            (this.accessType  == other.accessType) &&
            (registers.size() == other.registers.size());

      if (!rv) {
         setReasonForDifference("Basic peripheral description different");
         return false;
      }
      sortRegisters();
      other.sortRegisters();
      for (int index=0; index<registers.size(); index++) {
         Cluster reg1 = registers.get(index);
         Cluster reg2 = other.registers.get(index);
         if ((reg1 instanceof Register) && reg2 instanceof Register) {
            if (!((Register)reg1).equivalent((Register)(reg2))) {
               return false;
            }
         }
         else {
            if (!reg1.equivalent(reg2)) {
               return false;
            }
         }
      }
      
      return true;
   }
   
   /**
    * Sort peripheral registers by address offset
    * 
    */
   private void sortInterrupts() {
      Collections.sort(interrupts, new Comparator<InterruptEntry>() {
         @Override
         public int compare(InterruptEntry interruptEntry1, InterruptEntry interruptEntry2) {
            return interruptEntry1.getIndexNumber() - interruptEntry2.getIndexNumber();
         }
      });
      sorted = true;
   }
   
   /**
    * Sort peripheral registers by address offset
    * 
    */
   private void sortRegisters() {
      if (sorted) {
         return;
      }
      sortRegisters(registers);
      sorted = true;
   }
   
   /**
    * @throws Exception 
    * 
    */
   public void report() throws Exception {
      System.out.println("    Peripheral \"" + getName() + "\" = ");
      System.out.println("       description   = " + getDescription());
      System.out.println("       prependToName = " + getPrependToName());
      System.out.println("       appendToName  = " + getAppendToName());
      System.out.println("       baseAddress   = " + String.format("0x%08X", getBaseAddress()));
      if (groupName != null) {
         System.out.println("       groupName = " + getGroupName());
      }
      if (addressBlocks != null) {
         for (AddressBlock addressBlock : addressBlocks) {
            addressBlock.report();
         }
      }
      for (Cluster register : registers) {
         register.report();
      }
   }

   static boolean isNewConflict(String name) {
      if (conflictedNames.contains(name)) {
         return false;
      }
      conflictedNames.add(name);
      return true;
   }

   /**
    * Determines the longest prefix that can be extracted from the register names.
    * The registers names are then reduced to the suffix required.
    */
   public void extractNamePrefix() {
      if ((getRegisters() == null) || (getRegisters().size() == 0)) {
         return;
      }
      if ((getPrependToName() != null) && (getPrependToName().length() > 0)) {
         return;
      }
//      System.err.println("extractNamePrefix() - processing: "+getName());
      String namePrefix = getRegisters().get(0).getName();
      int commonLength = namePrefix.length();

      for (Cluster register : getRegisters()) {
         String registerName = register.getName();
         if (commonLength>registerName.length()) {
            commonLength = registerName.length();
         }
         for (int index = 0; index < commonLength; index++) {
            if (namePrefix.charAt(index) != registerName.charAt(index)) {
               commonLength = index;
               break; 
            }
         }
         if (commonLength == 0) {
            return;
         }
      }
      // Now cut off any characters after an underscore if there is one.
      int underscoreLocation = namePrefix.indexOf('_');
      if ((underscoreLocation>0) && ((underscoreLocation+1)<commonLength)) {
         commonLength = underscoreLocation+1;
      }
      if (commonLength <= 1) {
         // No common root
         return;
      }
      namePrefix = namePrefix.substring(0, commonLength);
      Matcher matcher = Pattern.compile("^(.*)_$").matcher(namePrefix);
      if (matcher.matches()) {
         namePrefix = matcher.replaceAll("$1");
      }
      setPrependToName(namePrefix);
      // Remove the prefix from the actual register names
      for (Cluster register : getRegisters()) {
         register.setName(register.getName().substring(commonLength));
      }
      if (!(getName().equals(namePrefix))) {
         if (getDescription().length() == 0) {
            setDescription(getName());
         }
         if (isNewConflict(getName())) {
            System.out.println("\nWARNING: Peripheral.extractNamePrefix(), conflict: peripheral: "
                  + "\""+getName()+"\" <==> prefix: \""+namePrefix+"\"");
         }
      }
   }
   
   static HashMap<String, String> freescalePeripheralNameMap = null;
   
   /*
    * Maps peripheral names in the data to preferred Freescale names
    */
   public static String getMappedPeripheralName(String name) {
      if (isMapFreescaleCommonNames()) {
         return name;
      }
      if (freescalePeripheralNameMap == null) {
         freescalePeripheralNameMap = new HashMap<String, String>();
         freescalePeripheralNameMap.put("PTA", "GPIOA");
         freescalePeripheralNameMap.put("PTB", "GPIOB");
         freescalePeripheralNameMap.put("PTC", "GPIOC");
         freescalePeripheralNameMap.put("PTD", "GPIOD");
         freescalePeripheralNameMap.put("PTE", "GPIOE");
         freescalePeripheralNameMap.put("PTF", "GPIOF");
         freescalePeripheralNameMap.put("FPTA", "FGPIOA");
         freescalePeripheralNameMap.put("FPTB", "FGPIOB");
         freescalePeripheralNameMap.put("FPTC", "FGPIOC");
         freescalePeripheralNameMap.put("FPTD", "FGPIOD");
         freescalePeripheralNameMap.put("FPTE", "FGPIOE");
         freescalePeripheralNameMap.put("FPTF", "FGPIOF");
         freescalePeripheralNameMap.put("FTFA_FlashConfig", "NV");
         freescalePeripheralNameMap.put("FTFL_FlashConfig", "NV");
         freescalePeripheralNameMap.put("FTFE_FlashConfig", "NV");
         freescalePeripheralNameMap.put("SystemControl", "SCB");
         freescalePeripheralNameMap.put("SysTick", "SYST");
      }
      String mappedName = freescalePeripheralNameMap.get(name);
      if (mappedName != null) {
         return mappedName;
      }
      return name;
   }
   
   static class ComplexStructuresInformation {
      public final Pattern pattern;
      public final String  arrayName;
      public final String  nameIndex;
      public final String  fieldName;
      public final String  nameFormat;
      
      ComplexStructuresInformation(String pattern, String arrayName, String nameIndex, String fieldName, String nameFormat) {
         this.pattern    = Pattern.compile(pattern);    // Pattern used to break register name up
         this.arrayName  = arrayName;                   // Portion to use for matching start of name
         this.nameIndex  = nameIndex;                   // Portion to use for index in STRUCT and macro name, excluded from matching
         this.fieldName  = fieldName;                   // Portion to use as field name in STRUCT & matching
      
         // This is used as the name of the cluster in the SVD file.
         // Up to the 1st comma is the name of the Cluster proper which is used as the array/struct name in the C STRUCT
         // Following portion is a format string to construct the name of the register
         //  and bit mask MACROs???
         // Format: @p=peripheralName, @a=arrayName, @i=nameIndex, @f=fieldName
         this.nameFormat = nameFormat; 
      }
   }
   
   static HashMap<String, ArrayList<ComplexStructuresInformation>> freescaleComplexStructures = null;
   
   public static ArrayList<ComplexStructuresInformation> getFreescaleFreescaleComplexStructures(String name) {
      //TODO - Where complex structures are defined
      if (freescaleComplexStructures == null) {
         freescaleComplexStructures = new HashMap<String,  ArrayList<ComplexStructuresInformation>>(20);
         ArrayList<ComplexStructuresInformation> entry = null;

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(E.R)(\\d+)$",                 "$1","$2","$1", "SP,@p@f@i"));
         entry.add(new ComplexStructuresInformation("^(RGD)(\\d+)_(WORD.*)$",        "$1","$2","$3", "RGD,@p@a@i_@f"));
         freescaleComplexStructures.put("MPU",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(DATAW)(\\d+)(S.*)$",          "$1","$2","$3", "DATAW,@pDATAW@i@f"));
         entry.add(new ComplexStructuresInformation("^(TAGVDW)(\\d+)(S.*)$",         "$1","$2","$3", "TAGVDW,@pTAGVDW@i@f"));
         freescaleComplexStructures.put("FMC",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CS|ID|WORD0|WORD1)(\\d+)$",    "$1","$2","$1", "MB,@p@f@i"));
         freescaleComplexStructures.put("CAN0",  entry);
         freescaleComplexStructures.put("CAN1",  entry);
         freescaleComplexStructures.put("CAN2",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CS[^\\d]+)(\\d+)$",           "$1","$2","$1", "CS,@p@f@i"));
         freescaleComplexStructures.put("FB",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CH)(\\d+)(C1|S|DLY0|DLY1)$",  "$1","$2","$3", "CH,@pCH@i@f"));
         entry.add(new ComplexStructuresInformation("^(DAC)(INTC|INT)(\\d+)$",       "$1","$3","$2", "DAC,@pDAC@f@i"));
         entry.add(new ComplexStructuresInformation("^(PO)(\\d+)(DLY)$",             "$1","$2","$3", "PO,@pPO@i@f"));
         freescaleComplexStructures.put("PDB0",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^([^\\d]+)(\\d+)$",             "$1","$2","$1", "CHANNEL,@p@f@i"));
         freescaleComplexStructures.put("PIT",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(TCD)(\\d+)_(.*)$",            "$1","$2","$3", "TCD,@p@a@i_@f"));
         freescaleComplexStructures.put("DMA",   entry);
         freescaleComplexStructures.put("DMA0",  entry);
         freescaleComplexStructures.put("DMA1",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(SAR|DAR|DSR.*|DCR)(\\d+)$",  "$1","$2","$1", "DMA,@p@f@i"));
         freescaleComplexStructures.put("DMA",   entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(C)(\\d+)(.*)$",               "$1","$2","Cn$3", "CONTROLS,@pC@i@f"));
         freescaleComplexStructures.put("FTM0",  entry);
         freescaleComplexStructures.put("FTM1",  entry);
         freescaleComplexStructures.put("FTM2",  entry);
         freescaleComplexStructures.put("FTM3",  entry);
         freescaleComplexStructures.put("TPM0",  entry);
         freescaleComplexStructures.put("TPM1",  entry);
         freescaleComplexStructures.put("TPM2",  entry);
         freescaleComplexStructures.put("TPM3",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(DAT)(\\d+)(.*)$",             "$1","$2","$1$3", "DAT,@p@f@i"));
         freescaleComplexStructures.put("DAC0",  entry);
         freescaleComplexStructures.put("DAC1",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(.RS)(\\d+)$",                 "$1","$2","$1",   "SLAVE,@p@f@i"));
         entry.add(new ComplexStructuresInformation("^(MGPCR)(\\d+)$",               "$1","$2","$1",   "MASTER,@p@f@i"));
         freescaleComplexStructures.put("AXBS",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(ENDPT)(\\d+)$",               "$1","$2","$1",   "ENDPOINT,@p@f@i"));
         freescaleComplexStructures.put("USB0",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(COMP|MASK|FUNCTION)(\\d+)$",  "$1","$2","$1",   "COMPARATOR,@p@f@i"));
         freescaleComplexStructures.put("DWT",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(COMP|MASK|FCT)(\\d+)$",       "$1","$2","$1",   "COMPARATOR,@p@f@i"));
         freescaleComplexStructures.put("MTBDWT",  entry);
         
         // ==== Coldfire V2 ====
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CODE|CTRL|TIME|ID|DATA_WORD_1|DATA_WORD_2|DATA_WORD_3|DATA_WORD_4)_?(\\d+)$",
               "$1","$2","$1", "MB,@p@f@i"));
         freescaleComplexStructures.put("CANMB",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(SAR|DAR|DSR|DCR|BCR)_?(\\d+)$",
               "$1","$2","$1", "CH,@p@f@i"));
         freescaleComplexStructures.put("DMA",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(DTMR|DTXMR|DTER|DTRR|DTCR|DTCN)_?(\\d+)$",
               "$1","$2","$1", "CH,@p@f@i"));
         freescaleComplexStructures.put("DTIM",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CSAR|CSMR|CSCR)_?(\\d+)$",
               "$1","$2","$1", "CH,@p@f@i"));
         freescaleComplexStructures.put("FBCS",  entry);

//         entry = new ArrayList<ComplexStructuresInformation>();
//         entry.add(new ComplexStructuresInformation("^(PORTT(?:E|F|G|H|I|J))(\\d+)$",
//               "$1","$2","$1", "CH,@p@f@i"));
//         freescaleComplexStructures.put("GPIO",  entry);

      }
      return freescaleComplexStructures.get(name);
   }
   
   /**
    * Reformats the data to take advantage of complex array structures
    * @throws Exception 
    */
   private void extractComplexStructures() throws Exception {
      ArrayList<ComplexStructuresInformation> information = getFreescaleFreescaleComplexStructures(this.getName());
      if (information == null) {
         return;
      }
      for (ComplexStructuresInformation info : information) {
         extractComplexStructures(info);
      }
      sortRegisters();
   }
   
   /**
    * Reformats the data to take advantage of complex array structures
    * 
    * @param information The information necessary to restructure the data
    * @throws Exception 
    */
   private void extractComplexStructures(ComplexStructuresInformation information) throws Exception {
//      System.err.println("\nextractComplexStructures() Processing "+getName());
//      System.err.println(String.format("extractComplexStructures()"));

      sortRegisters();
      
      for(Cluster reg : registers) {
         reg.setDeleted(false);
      }
      ArrayList<Register> removedRegisters = new ArrayList<Register>();
      Cluster cluster = null;

      for (int reg1 = 0; reg1<getRegisters().size(); reg1++) {
         // Only match simple registers
         if (!(registers.get(reg1) instanceof Register)) {
            continue;
         }
         Register mergeReg   = (Register)registers.get(reg1);
         String   mergeName  = mergeReg.getName();
         // Already removed?
         if (mergeReg.isDeleted()) {
//            System.err.println(String.format("Skipping %s as already deleted", mergeName));
            continue;
         }
         boolean debug = false; //mergeName.matches("TCD0.*");
         if (debug ) {
            System.err.println(String.format("\n    extractComplexStructures(), mergeName=\"%s\"", mergeName));
         }
         // Check if matches pattern
         Matcher mergeMatcher = information.pattern.matcher(mergeName);
         if (!mergeMatcher.matches()) {
            continue;
         }
         String arrayName   = mergeMatcher.replaceFirst(information.arrayName);
         String nameIndex   = mergeMatcher.replaceFirst(information.nameIndex);
         String fieldName   = mergeMatcher.replaceFirst(information.fieldName);
         long stride = 0;
         
         // Starting new set of matches
         ArrayList<Register> candidateRegisters = new ArrayList<Register>();
         candidateRegisters.add(mergeReg);
         // Reset indexes
         ArrayList<String>   dimensionIndexes = new ArrayList<String>();
         dimensionIndexes.add(nameIndex);

//         System.err.println(String.format("Complex Pattern: %-15s => %s[%s].%s ", mergeName, arrayName, nameIndex, fieldName));

         int index = 1;
         for (int reg2 = reg1+1; reg2<getRegisters().size(); reg2++) {
            // Only match simple registers 
            if (!(registers.get(reg2) instanceof Register)) {
               continue;
            }
            // Get candidate to match
            Register victimReg   = (Register)registers.get(reg2);
            if (victimReg.isDeleted()) {
               continue;
            }
            // Check against pattern
            String  victimName    = victimReg.getName();
            Matcher victimMatcher = information.pattern.matcher(victimName);
            if (!victimMatcher.matches()) {
               continue;
            }
            String vArrayName   = victimMatcher.replaceFirst(information.arrayName);
            String vNameIndex   = victimMatcher.replaceFirst(information.nameIndex);
            String vFieldName   = victimMatcher.replaceFirst(information.fieldName);
            if (!vArrayName.equals(arrayName) || !vFieldName.equals(fieldName)) {
               continue;
            }
//            System.err.println(String.format("   Found %s ", victimName));
            if (stride == 0) {
               // Get stride from 1st matching register
               stride = victimReg.getAddressOffset()-mergeReg.getAddressOffset();
//               System.err.println(String.format("   Stride = %d", stride)); 
            }
            else {
               // Check stride matches
               if ((victimReg.getAddressOffset()-mergeReg.getAddressOffset()) != (index*stride)) {
                  System.err.println(
                        String.format("   %s <=> %s, Expected offset %d, found %s ",
                              victimReg.getName(),
                              mergeReg.getName(),
                              (index*stride), 
                              (victimReg.getAddressOffset()-mergeReg.getAddressOffset())));
                  continue;
               }
            }
            index++;
            candidateRegisters.add(victimReg);
            dimensionIndexes.add(vNameIndex);
         }
         
         // No register matched
         if (candidateRegisters.size() <= 1) {
            continue;
         }
         // Need to start a new cluster or can this group be added to existing cluster?
         // This assumes we find registers belonging to a cluster consecutively
         if ((cluster == null) ||
             (cluster.getDimension() != dimensionIndexes.size()) &&
             (cluster.getDimensionIncrement() != stride) &&
             (cluster.getDimensionIndexesAsString().equalsIgnoreCase(Cluster.appendStrings(dimensionIndexes)) 
                   && (!cluster.getName().equals(arrayName)))) {
//            System.err.println("Allocating new Cluster: "+arrayName);
            cluster = new Cluster(this);
            cluster.setName(information.nameFormat);
            cluster.setDimensionIncrement((int)stride);
            cluster.setDimensionIndexes(dimensionIndexes);
            cluster.setAddressOffset(mergeReg.getAddressOffset());
            this.addRegister(cluster);
         }
         cluster.addRegister(mergeReg);
         mergeReg.setName(fieldName);
         mergeReg.setDescription(mergeReg.getDescription());
         // Record removed registers
         for (Register victimReg : candidateRegisters) {
            victimReg.setDeleted(true);
            removedRegisters.add(victimReg);
         }
      }
      // Remove replaced registers
      for (Register victimReg : removedRegisters) {
         registers.remove(victimReg);
      }
      if (cluster != null) {
         for (Register clusterReg : cluster.getRegisters()) {
            clusterReg.setAddressOffset(clusterReg.getAddressOffset()-cluster.getAddressOffset());
         }
      }
      sortRegisters();
   }
   
   // This is a list of peripherals and particular registers not to turn into arrays
   static HashMap<String, String> freescaleExcludedCommonRegisterPeripherals = null;
   public static String getFreescaleExcludedCommonRegisterPeripherals(String name) {
      if (freescaleExcludedCommonRegisterPeripherals == null) {
         freescaleExcludedCommonRegisterPeripherals = new HashMap<String,  String>(200);
         freescaleExcludedCommonRegisterPeripherals.put("ITM",   "PID.*"); // PIDs are in a strange order
         freescaleExcludedCommonRegisterPeripherals.put("DWT",   "PID.*");
         freescaleExcludedCommonRegisterPeripherals.put("FPB",   "PID.*");
         freescaleExcludedCommonRegisterPeripherals.put("TPIU",  "PID.*");
         freescaleExcludedCommonRegisterPeripherals.put("ETM",   "PID.*");
         freescaleExcludedCommonRegisterPeripherals.put("ETB",   "PID.*");
         freescaleExcludedCommonRegisterPeripherals.put("ETF",   "PID.*");
         freescaleExcludedCommonRegisterPeripherals.put("FMC",   "PID.*");
         freescaleExcludedCommonRegisterPeripherals.put("MCG",   ".*");    // Some odd reg. pairs are better separate
      }
      return freescaleExcludedCommonRegisterPeripherals.get(name);
   }
   
   // This is a list of peripherals with special matching patterns for register combining
   static HashMap<String, String> freescalePeripheralRegisterArrayPatterns = null;
   public static String getFreescalePeripheralRegisterArrayPatterns(String name) {
      if (freescalePeripheralRegisterArrayPatterns == null) {
         freescalePeripheralRegisterArrayPatterns = new HashMap<String,  String>(200);
         freescalePeripheralRegisterArrayPatterns.put("ITM", "(.+)([0-9]+)(.*)$");      // Special pattern for ITM
         freescalePeripheralRegisterArrayPatterns.put("PDB", "(.+)([0-9]+)(.*)$");      // Special pattern for PDB
      }
      String pattern = freescalePeripheralRegisterArrayPatterns.get(name);
      if (pattern == null) {
         pattern = "(.+)([0-9|A-F|a-f]+)()$";
      }
      return pattern;
   }
   
   /**
    * Reformats the data to take advantage of simple array structures
    */
   private void extractSimpleRegisterArrays() {
      
      // Check if peripheral is excluded
      String excludedRegisterName = getFreescaleExcludedCommonRegisterPeripherals(this.getName());
      Pattern excludedRegisterPattern = null;
      if (excludedRegisterName != null) {
         if (excludedRegisterName.isEmpty()) {
            // Exclude entire peripheral
            return;
         }  
         // Pattern to exclude register from collection
         excludedRegisterPattern = Pattern.compile(excludedRegisterName);
      }
      
      // Pattern used to find candidate registers
      Pattern namePattern = Pattern.compile(getFreescalePeripheralRegisterArrayPatterns(this.getName()));
      ArrayList<Register>  removedRegisters = new ArrayList<Register>();
      
      sortRegisters();
      
      for (int reg1 = 0; reg1<getRegisters().size(); reg1++) {
         if (!(registers.get(reg1) instanceof Register)) {
            continue;
         }
         Register mergeReg   = (Register)registers.get(reg1);
         if (mergeReg.getDerivedFrom() != null) {
            // Don't merge derived registers
            continue;
         }
         String   mergeName  = mergeReg.getName();
         boolean  debug      = false; // mergeName.matches("FCCOB.*");
         
         if (debug) {
            System.err.println(String.format("\n    extractSimpleRegisterArrays(), mergeName=\"%s\"", mergeName));
         }
         if ((excludedRegisterPattern!= null) && excludedRegisterPattern.matcher(mergeName).matches()) {
//            System.err.println("Excluding "+getName()+"."+mergeName);
            continue;
         }
         Matcher nameMatcher = namePattern.matcher(mergeName);
         if (!nameMatcher.matches()) {
            continue;
         }
         long                 dimensionIncrement      = (mergeReg.getWidth()+7)/8;
         long                 rootOffset              = mergeReg.getAddressOffset();
         ArrayList<String>    dimensionIndexes        = new ArrayList<String>();
         String               mergeNamePrefix         = nameMatcher.replaceFirst("$1");
         String               mergeNameMiddle         = nameMatcher.replaceFirst("$2");
         String               mergeNameSuffix         = nameMatcher.replaceFirst("$3");
         String               mergeNameIndexPattern   = "(.+)("+mergeNameMiddle+")(.*)";
         Pattern              mergePatternName        = Pattern.compile("("+mergeNamePrefix+")(.+)("+mergeNameSuffix+")");
         String               mergeSubstituteName     = mergeNamePrefix+"%s"+mergeNameSuffix;
         String               mergeDescription        = mergeReg.getDescription();
         String               descriptionPattern      = "([a-z|A-Z|\\s]+)("+mergeNameMiddle+")([a-z|A-Z|\\s]*)";
         String               mergePatternDescription = mergeDescription.replaceFirst(descriptionPattern, "$1%s$3");
         int                  dimension               = 1;
         dimensionIndexes.add(mergeNameMiddle);
         
         // Go through other register looking for candidates that match the current register
         // When found add to deleted list.
         for (int reg2 = reg1+1; reg2<getRegisters().size(); reg2++) {
            
            // Only process registers (not clusters)
            if (!(registers.get(reg2) instanceof Register)) {
               continue;
            }
            Register victimReg  = (Register)registers.get(reg2);
            if (victimReg.getDerivedFrom() != null) {
               // Don't merge derived registers
               continue;
            }
            String   victimName = victimReg.getName();

            boolean debug2 = debug && victimName.matches("FCCOB.*");
            if (debug2) {
               System.err.println(
                     String.format("    extractSimpleRegisterArrays(), comparing %-20s ?= %-20s",
                           "\""+mergeName+"\"",
                           "\""+victimName+"\""));
            }
            
            // Check if candidate matches the same pattern as original register i.e.
            // Both register name should be the same after applying the regular expression
            Matcher victimNameMatcher = mergePatternName.matcher(victimName);
            String s1 = victimNameMatcher.replaceFirst("$1%s$3");
            if (!s1.equalsIgnoreCase(mergeSubstituteName)) {
//               if (debug2) {
//                  System.err.println(
//                        String.format("    extractSimpleRegisterArrays(), rejecting %-20s != %-20s",
//                              "\""+mergeName+"\"",
//                              "\""+victimName+"\""));
//               }
               continue;
            }
            // Extract the variant part of the candidate name
            String victimNameMiddle  = victimNameMatcher.replaceFirst("$2");
            
            // Create a pattern to apply to other parts of the register e.g. description to
            // improve the matching success
            String victimNameIndexPattern  = "(.+)("+victimNameMiddle+")(.*)";
//            if (!mergeSubstituteName.equals(victimPatternName)) {
//               if (debug) {
//               System.err.println(
//                     String.format("    extractSimpleRegisterArrays(), rejected %-20s != %-20s",
//                        "\""+mergeSubstituteName+"\"",
//                        "\""+victimPatternName+"\""));
//               }
//               continue;
//            }
            
            // Check that the register indexing matches the expected sequence
            if ((rootOffset+(dimension*dimensionIncrement)) != victimReg.getAddressOffset()) {
//               System.err.println(
//                     String.format("    extractSimpleRegisterArrays(), failed combining %-20s & %-20s, expected offset 0x%X != 0x%X",
//                        "\""+mergeReg.getName()+"\"",
//                        "\""+victimReg.getName()+"\"",
//                        (rootOffset+(dimension*dimensionIncrement)),
//                        victimReg.getAddressOffset()
//                        ));
               continue;
            }
            // Check if registers are equivalent (apart from indexed stuff)
            if (!mergeReg.equivalent(victimReg, mergeNameIndexPattern, victimNameIndexPattern)) {
               continue;
            }
            // Update dimension for index checking
            dimension++;
            
            // Add candidate to removed registers
            removedRegisters.add(victimReg);
            
            // Add extracted index to dimension indexes for substitution when expanding the register
            dimensionIndexes.add(victimNameMiddle);
//            System.err.println(
//                  String.format("    extractSimpleRegisterArrays(), combining %-20s & %-20s as (%s + %s)",
//                     "\""+mergeReg.getName()+"\"",
//                     "\""+victimReg.getName()+"\"",
//                     "\""+mergePatternName+"\"",
//                     "\""+victimSuffix+"\""
//                     ));
         }
         if (dimensionIndexes.size() > 1) {
            // Remove replaced registers
            for (Register victimReg : removedRegisters) {
               registers.remove(victimReg);
            }
            mergeReg.setName(mergeSubstituteName);
            mergeReg.setDescription(mergePatternDescription);
            mergeReg.setDimensionIncrement((int)dimensionIncrement);
            mergeReg.setDimensionIndexes(dimensionIndexes);
         }
      }
   }
   
   public void optimise() throws Exception {
      sortRegisters(registers);

      // TODO - Modify optimisations done here
      if (isExtractComplexStructures()) {
         extractComplexStructures();
      }
      if (isExtractSimpleRegisterArrays()) {
         extractSimpleRegisterArrays();
      }
      if (isRegenerateAddressBlocks()) {
         createAddressBlocks();
      }
      if (isExtractCommonPrefix()) {
         extractNamePrefix();
      }
      if (isFoldRegisters()) {
         foldRegisters();
      }
   }

   private static final String deviceListPreamble = 
           "<!--\n"
         + "Devices using this peripheral: \n";
   
   private static final String deviceListPostamble = 
           "-->\n";
   
   /**
    *    Writes the Peripheral description to file in a SVF format
    *   
    *    @param writer         The destination for the XML
    *    @param standardFormat Suppresses some non-standard size optimisations 
    *  
    *    @param owner   The owner - This is used to reduce the size by inheriting default values
    *  
    *    @throws Exception 
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat, DevicePeripherals owner) throws Exception {
      final int indent = 6;
      final String indenter = RegisterUnion.getIndent(indent);
      sortRegisters();
      
      if (getUsedBy().size()>0) {
         writer.print(deviceListPreamble);
         for (String deviceName : getUsedBy()) {
            writer.println(String.format( indenter+"%s", deviceName));
         }
         writer.print(deviceListPostamble);
      }

      if ((derivedFrom != null) && !ModeControl.isExpandDerivedPeripherals()){
         writeDerivedFromSVD(writer, indent);
         return;
      }

      writer.print(                       indenter+"<peripheral");
      writer.println(">");

      if (getSourceFilename() != null) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.SOURCEFILE_ATTRIB+" \"%s\" ?>", SVD_XML_BaseParser.escapeString(getSourceFilename())));
      }
      if (getPreferredAccessWidth() != 0) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.PREFERREDACCESSWIDTH_ATTRIB+" \"%d\" ?>", getPreferredAccessWidth()));
      }
      if (getForcedBlockMultiple() != 0) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.FORCED_BLOCK_WIDTH+" \"%d\" ?>", getForcedBlockMultiple()));
      }
      if (isRefreshAll()) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.REFRESH_WHOLE_PERIPHERAL_ATTRIB+"?>"));
      }
      String name = getName();
      writer.println(String.format(       indenter+"   <name>%s</name>",                   SVD_XML_BaseParser.escapeString(name)));
      writer.println(String.format(       indenter+"   <description>%s</description>",     SVD_XML_BaseParser.escapeString(getDescription())));
      if (getGroupName().length()>0) {
         writer.println(String.format(    indenter+"   <groupName>%s</groupName>",         SVD_XML_BaseParser.escapeString(getGroupName())));
      }
      if (getPrependToName().length()>0) {
         writer.println(String.format(    indenter+"   <prependToName>%s</prependToName>", SVD_XML_BaseParser.escapeString(getPrependToName())));
      }
      if (getAppendToName().length()>0) {
         writer.println(String.format(    indenter+"   <appendToName>%s</appendToName>",   SVD_XML_BaseParser.escapeString(getAppendToName())));
      }
      if (getHeaderStructName().length()>0) {
         writer.println(String.format(    indenter+"   <%s>%s</%s>", SVD_XML_Parser.HEADERSTRUCTNAME_TAG, SVD_XML_BaseParser.escapeString(getHeaderStructName()), SVD_XML_Parser.HEADERSTRUCTNAME_TAG));
      }
      writer.println(String.format(       indenter+"   <baseAddress>0x%08X</baseAddress>", getBaseAddress()));
      if ((owner == null) || (owner.getWidth() != getWidth())) {
         writer.println(String.format(    indenter+"   <size>%d</size>",                   getWidth()));
      }
      if ((owner == null) || (owner.getAccessType() != getAccessType())) {
         writer.println(String.format(    indenter+"   <access>%s</access>",               getAccessType().getPrettyName()));
      }
      if ((owner == null) || (owner.getResetValue() != getResetValue())) {
         writer.println(String.format(    indenter+"   <resetValue>0x%X</resetValue>",     getResetValue()));
      }
      if ((owner == null) || (owner.getResetMask() != getResetMask())) {
         writer.println(String.format(    indenter+"   <resetMask>0x%X</resetMask>",       getResetMask()));
      }
      if (getInterruptEntries() != null) {
         for (InterruptEntry interrupt : getInterruptEntries()) {
            interrupt.writeSVD(writer, indent+3);
         }
      }
      if (getAddressBlocks() != null) {
         for (AddressBlock addressBlock : getAddressBlocks()) {
            addressBlock.writeSVD(writer, standardFormat);
         }
      }
      writer.println(                     indenter+"   <registers>");
      for (Cluster clusterOrRegister : getRegisters()) {
         clusterOrRegister.writeSVD(writer, standardFormat, this, indent+6);
      }
      writer.println(                     indenter+"   </registers>");
      writer.println(                     indenter+"</peripheral>");
      writer.flush();
   }

   /**
    *    Writes the Peripheral description to file in a SVF format
    *   
    *    @param writer         The destination for the XML
    *    @param standardFormat Suppresses some non-standard size optimisations 
    *  
    *    @param owner   The owner - This is used to reduce the size by inheriting default values
    */
   private void writeDerivedFromSVD(PrintWriter writer, int indent) {
      Peripheral derived = getDerivedFrom();

      final String indenter = "";//RegisterUnion.getIndent(indent);
      
      writer.print(String.format(   indenter+"<%s %s=\"%s\">", SVD_XML_Parser.PERIPHERAL_TAG, SVD_XML_Parser.DERIVEDFROM_ATTRIB, SVD_XML_BaseParser.escapeString(derived.getName())));
      
      writer.print(String.format("<%s>%s</%s>", SVD_XML_Parser.NAME_TAG,SVD_XML_BaseParser.escapeString(getName()), SVD_XML_Parser.NAME_TAG));
      if (!getDescription() .equals(getDescription())) {
         writer.print(String.format(indenter+"<%s>%s</%s>", SVD_XML_Parser.DESCRIPTION_TAG,  SVD_XML_BaseParser.escapeString(getDescription()), SVD_XML_Parser.DESCRIPTION_TAG));
      }
      if (!getGroupName().equals(derived.getGroupName())) {
         //XXXX Check this
         System.err.println(String.format("writeDerivedFromSVD() d=%s, i=%s", derived.getGroupName(), getGroupName()));
         writer.print(String.format(indenter+"<%s>%s</%s>", SVD_XML_Parser.GROUPNAME_TAG,  SVD_XML_BaseParser.escapeString(getGroupName()), SVD_XML_Parser.GROUPNAME_TAG));
      }
      if (!getPrependToName().equals(derived.getPrependToName())) {
         writer.print(String.format(indenter+"<%s>%s</%s>", SVD_XML_Parser.PREPENDTONAME_TAG,SVD_XML_BaseParser.escapeString(getPrependToName()), SVD_XML_Parser.PREPENDTONAME_TAG));
      }
      if (!getAppendToName().equals(derived.getAppendToName())) {
         writer.print(String.format(indenter+"<%s>%s</%s>", SVD_XML_Parser.APPENDTONAME_TAG,SVD_XML_BaseParser.escapeString(getAppendToName()), SVD_XML_Parser.APPENDTONAME_TAG));
      }
      if (!getHeaderStructName().equals(derived.getHeaderStructName())) {
         writer.print(String.format(indenter+"<%s>%s</%s>", SVD_XML_Parser.HEADERSTRUCTNAME_TAG, SVD_XML_BaseParser.escapeString(getHeaderStructName()), SVD_XML_Parser.HEADERSTRUCTNAME_TAG));
      }
      writer.print(String.format("<%s>0x%08X</%s>", SVD_XML_Parser.BASEADDRESS_TAG, getBaseAddress(), SVD_XML_Parser.BASEADDRESS_TAG));
      if (getAccessType() != derived.getAccessType()) {
         writer.print(String.format(indenter+"<%s>%s</%s>", SVD_XML_Parser.ACCESS_TAG,SVD_XML_BaseParser.escapeString(getAccessType().getPrettyName()), SVD_XML_Parser.ACCESS_TAG));
      }
      if (getInterruptEntries() != derived.getInterruptEntries()) {
         for (InterruptEntry interrupt : getInterruptEntries()) {
            interrupt.writeSVD(writer, indent+3);
         }
      }
      writer.println(String.format("</%s>", SVD_XML_Parser.PERIPHERAL_TAG));
   }

   static final String DEVICE_HEADER_FILE_STRUCT_PREAMBLE =   
       "\n"
      +"/* ================================================================================ */\n"
      +"/* ================           %-30s       ================ */\n"
      +"/* ================================================================================ */\n"
      +"\n"
      +"/**\n"
      +" * @brief %s\n"
      +" */\n"
      ;

   static final String DEVICE_OPEN_STRUCT  = "typedef struct {                                /*       %-60s */\n";
   static final String DEVICE_CLOSE_STRUCT = "} %s;\n\n";

   public String getSafeHeaderStructName() {
      return getHeaderStructName()+"_Type";
   }
   
   public static void clearTypedefsTable() {
      typedefsTable = new HashSet<String>();
   }
   
   public void addTypedefsTable(String name) throws Exception {
      if (typedefsTable.contains(name)) {
         throw new Exception("Peripheral Typedef clash - " + this.getName() + ", " + name);
      }
      typedefsTable.add(name);
   }

   /**
    *    Writes C code for Peripheral declaration e.g. a typedef for a STRUCT representing all the peripheral registers<br>
    *    e.g. <pre><b>typedef struct {...} peripheralName_Type;</b></pre>
    * 
    *    @param writer
    *    @param devicePeripherals
    */
   public void writeHeaderFileStruct(PrintWriter writer, int indent, Peripheral peripheral) throws Exception {

      final String indenter = RegisterUnion.getIndent(indent);
      
      sortRegisters();
      
      RegisterUnion unionRegisters = new RegisterUnion(writer, indent+3, peripheral, 0L);
      
      writer.print(indenter+String.format(DEVICE_OPEN_STRUCT, getName()+" Structure"));
      
      for(Cluster cluster : registers) {
         unionRegisters.add(cluster);
      }
      // Flush current union if exists
      unionRegisters.writeHeaderFileUnion();
      
      addTypedefsTable(getSafeHeaderStructName());
      
      writer.print(indenter+String.format(DEVICE_CLOSE_STRUCT, getSafeHeaderStructName()));
   }
      
//   static final String DEVICE_SIMPLE_STRUCT    = "typedef %s %s;  /*!< %-60s*/\n\n";
   
   /**
    *    Writes C code for Peripheral declaration e.g. a typedef for a STRUCT representing all the peripheral registers<br>
    *    e.g. <pre><b>typedef struct {...} peripheralName_Type;</b></pre>
    * 
    *    @param writer
    *    @param devicePeripherals
    */
   public void writeHeaderFileTypedef(PrintWriter writer, DevicePeripherals devicePeripherals) throws Exception {
      final String structGroupSuffix  = "structs";
      final int indent = 0;
      String uniqueId;
      if (getDerivedFrom() != null) {
         uniqueId = " (derived from " + derivedFrom.getName() + ")";
      }
      else {
         uniqueId = (getSourceFilename()==null)?"":" (file:"+getSourceFilename()+")";
      }
      writer.print(String.format(DEVICE_HEADER_FILE_STRUCT_PREAMBLE, getName()+uniqueId, getCDescription()));

      if (getDerivedFrom() != null) {
//         writer.print(String.format(DEVICE_SIMPLE_STRUCT, derivedFrom.getSafeHeaderStructName(), getSafeHeaderStructName(), getName()+" Structure"));
         return;
      }
      writeGroupPreamble(writer, getGroupName()+"_"+structGroupSuffix, getGroupName()+" struct", "Struct for "+getGroupName());
      writeHeaderFileStruct(writer, indent, this);
      writeGroupPostamble(writer, getGroupName()+"_"+structGroupSuffix);
   }

   static final String DEVICE_HEADER_FILE_REGISTER_MACRO_PREAMBLE =   
         "\n"
        +"/* -------------------------------------------------------------------------------- */\n"
        +"/* -----------     %-50s   ----------- */\n"
        +"/* -------------------------------------------------------------------------------- */\n"
        +"\n"
        ;

   /**
    *    Writes a set of macros to allow 'Freescale' style access to the registers of the peripheral<br>
    *    e.g. <pre><b>#define I2S0_CR3 (I2S0->CR[3])</b></pre>
    * 
    *    @param  writer
    *    @param  devicePeripherals
    *    @throws Exception
    */
   public void writeHeaderFileRegisterMacro(PrintWriter writer) throws Exception {
      writer.print(String.format(DEVICE_HEADER_FILE_REGISTER_MACRO_PREAMBLE, "\'"+getName()+"\' Register Access macros"));
      for (Cluster cluster : getRegisters()) {
         cluster.writeHeaderFileRegisterMacro(writer, this);
      }
   }
   
   static final String DEVICE_HEADER_FILE_MACRO_PREAMBLE =   
         "\n"
        +"/* -------------------------------------------------------------------------------- */\n"
        +"/* -----------     %-50s   ----------- */\n"
        +"/* -------------------------------------------------------------------------------- */\n"
        +"\n"
        ;

   /**
    *    Writes a set of MACROs to allow convenient operations on the fields of the registers of this peripheral<br>
    *    e.g. <pre><b>#define PERIPHERAL_FIELD(x)  (((x)&lt;&lt;FIELD_OFFSET)&FIELD_MASK)</pre></b>
    * 
    *    @param  writer
    *    @param  devicePeripherals
    *    @throws Exception
    */
   public void writeHeaderFileFieldMacros(Writer writer, DevicePeripherals devicePeripherals) throws Exception {
      final String macroGroupSuffix  = "Register_Masks";
      if (derivedFrom != null) {
         // Derived peripherals re-uses existing MACROs
         return;
      }
      writer.write(String.format(DEVICE_HEADER_FILE_MACRO_PREAMBLE, "\'"+getName()+"\' Position & Mask macros"));
      sortRegisters();
      writeGroupPreamble(writer, getGroupName()+"_"+macroGroupSuffix, getGroupName()+" Register Masks", "Register Masks for "+getGroupName());
      for (Cluster cluster : getRegisters()) {
         if (cluster instanceof Register) {
            ((Register)cluster).writeHeaderFileFieldMacros(writer, this);
         }
         else {
            cluster.writeHeaderFileFieldMacros(writer, this);
         }
      }
      writeGroupPostamble(writer, getGroupName()+"_"+macroGroupSuffix);
   }

   /**
    *    Find register within this peripheral
    * 
    *    @param name
    *    @return
    */
   public Cluster findRegister(String name) {
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
    * Determines a new base address for the peripheral by examining all the (absolute) register addresses
    * 
    * @throws Exception 
    */
   public void rebase() throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot rebase derived peripheral");
      }
      if (registers.size() == 0) {
         return;
      }
      long baseAddress = registers.get(0).getAddressOffset();
      for (Cluster reg : registers) {
         long address = reg.getAddressOffset();
         if (address < baseAddress) {
            baseAddress = address;
         }
      }
      for (Cluster reg : registers) {
         reg.setAddressOffset(reg.getAddressOffset()-baseAddress);
      }
      this.setBaseAddress(baseAddress);
   }

   public void setRefreshAll(boolean value) {
      refreshAll = value;
   }

   public boolean isRefreshAll() {
      return refreshAll;
   }
}
