package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.jni.UsbdmException;
import net.sourceforge.usbdm.peripheralDatabase.Field.AccessType;

public class Peripheral extends ModeControl implements Cloneable {

   private String                    fName                   = "";
   private String                    fDescription            = "";
   private long                      fBaseAddress            = 0;
   private String                    fGroupName              = "";
   private String                    fSourceFilename         = null;
   private String                    fPrependToName          = "";
   private String                    fAppendToName           = "";
   private String                    fHeaderStructName       = "";
   private ArrayList<InterruptEntry> fInterrupts             = null;
   private ArrayList<String>         fUsedBy                 = new ArrayList<String>();
   private boolean                   fSorted                 = false;
   private Peripheral                fDerivedFrom            = null;

   // The following cannot be changed if derived
   private long                      fWidth;
   private AccessType                fAccessType;
   private long                      fResetValue;
   private long                      fResetMask;
   private int                       fPreferredAccessWidth   = 0;
   private int                       fForcedBlockMutiple     = 0;
   private ArrayList<AddressBlock>   fAddressBlocks          = new ArrayList<AddressBlock>();
   private ArrayList<Cluster>        fRegisters              = new ArrayList<Cluster>();
   private boolean                   fRefreshAll;
   private String                    fFilename               = null;
   private DevicePeripherals         fOwner                  = null;
   
   private TreeMap<String, Parameter> fParameters          = new TreeMap<String, Parameter>();
   private Map<String, String>        fSimpleParameterMap  = null;

   /** Arbitrary text to add to peripheral C declaration */
   private String                    fTemplate               = null;  
   
   private static HashSet<String>    fConflictedNames        = new HashSet<String>();
   private static HashSet<String>    fTypedefsTable          = new HashSet<String>();
   
   static class RegisterPair {
      public Register cReg;
      public Register sReg;
      
      public RegisterPair(Cluster c, Cluster s) {
         cReg = (Register)c;
         sReg = (Register)s;
      }
   }
   
//   /**
//    * A bit of a hack to fold together registers than wrongly appear twice<br>
//    * Uses the name only to decide
//    */
//   public void foldRegisters() {
//      ArrayList<RegisterPair> deletedRegisters = new ArrayList<RegisterPair>();
//      for (Cluster c1:fRegisters) {
//         for (Cluster c2:fRegisters) {
//            if ((c1 != c2) && 
//                (c1 instanceof Register) && (c2 instanceof Register) &&
//                c1.getName().matches("^"+Pattern.quote(c2.getName())+"%s$")) {
////               System.err.println("foldRegisters() - " + c1.getName() + ", " + c2.getName() );
//               // Assume actually the same register
//               deletedRegisters.add(new RegisterPair(c1, c2));
//            }
//         }
//      }
//      for(RegisterPair rp:deletedRegisters) {
//         System.err.println("foldRegisters() - " + rp.cReg.getName() + ", " + rp.sReg.getName() );
//         fRegisters.remove(rp.sReg);
//         for (Field f:rp.sReg.getFields()) {
//            rp.cReg.addField(f);
//         }
//      }
//   }
   
   /**
    * Create derivedFrom registers
    * 
    * @throws CloneNotSupportedException 
    */
   public void foldRegisters() throws CloneNotSupportedException {
      boolean oldIgnoreResetType  = isIgnoreResetValuesInEquivalence();
      setIgnoreResetValuesInEquivalence(true);
//      if (getName().startsWith("LLWU")) {
//         System.err.println("foldRegisters() - " + getName());
//      }
      for(int index1=0; index1<fRegisters.size(); index1++) {
         Cluster reference = fRegisters.get(index1);
         if (reference.getDerivedFrom() != null) {
            // Already mapped
            continue;
         }
         for(int index2=index1+1; index2<fRegisters.size(); index2++) {
            Cluster candidate = fRegisters.get(index2);
            if (candidate.getDerivedFrom() != null) {
               // Already mapped
               continue;
            }
//            System.err.println("foldRegisters() - " + c2.getName() + " to " + c1.getName());
//            if (c2.getName().startsWith("CMD1") && c1.getName().startsWith("CMD2")) {
//               System.err.println("foldRegisters() - " + c2.getName() + " to " + c1.getName());
//            }
            // Check of equivalent ignoring array info
            if (reference.equivalentStructure(candidate, MatchOptions.MATCH_NAMES)) {
               // Add derivation
               candidate.setDerivedFrom(reference);
//               derivedRegisters.put(c2, c1);
//               System.err.println("foldRegisters() - folding " + c2.getName() + " to " + c1.getName());
            }
         }
      }
      setIgnoreResetValuesInEquivalence(oldIgnoreResetType);
   }
   
   public Peripheral(DevicePeripherals owner) {
      if (owner != null) {
         fWidth          = owner.getWidth();
         fAccessType     = owner.getAccessType();
         fResetValue     = owner.getResetValue();
         fResetMask      = owner.getResetMask();
      }
      else {
         fWidth          =  32;
         fAccessType     =  AccessType.ReadWrite;
         fResetValue     =  0L;
         fResetMask      =  0xFFFFFFFFL;
      }
   }
   
   /**
    * Returns a relatively shallow copy of the peripheral.<br>
    * The following are copied but may be later changed:<br>
    *    <li>description
    *    <li>baseAddress
    *    <li>groupName
    *    <li>sourceFilename
    *    <li>prependToName
    *    <li>appendToName
    *    <li>usedBy
    *    <li>interrupts
    */
   @Override
   protected Object clone() throws CloneNotSupportedException {

      // Make shallow copy
      Peripheral clone = (Peripheral) super.clone();
      
      // Clones should be renamed
      clone.setName(getName()+"peripheral_clone");
      
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
      return fAppendToName;
   }

   /**
    * @param appendToName the appendToName to set
    */
   public void setAppendToName(String appendToName) {
      this.fAppendToName = appendToName;
   }

   /**
    * Get list of registers - The registers are sorted if necessary
    * 
    * @return
    */
   public ArrayList<Cluster> getSortedRegisters() {
      sortRegisters();
      return fRegisters;
   }

   /**
    * Get list of registers
    * 
    * @return
    */
   public ArrayList<Cluster> getRegisters() {
      return fRegisters;
   }

   /**
    * Get name of peripheral
    * 
    * @return
    */
   public String getName() {
      if ((fOwner != null) && (fOwner.isUseHeaderDefinitionsPrefix())) {
         return fOwner.getHeaderDefinitionsPrefix()+fName;
      }
      return fName;
   }

   /**
    * Set name of peripheral
    * 
    * @param name
    */
   public void setName(String name) {
      this.fName = getMappedPeripheralName(name);
   }

   /**
    * Set the name to use for the header file STRUCT for this device
    * 
    * @param name
    */
   public void setHeaderStructName(String name) {
      fHeaderStructName = name;
   }

   /**
    * Get the name to use for the header file struct for this device
    * 
    * @return name
    */
   public String getHeaderStructName() {
      String prefix = "";
      if ((fOwner != null) && (fOwner.isUseHeaderDefinitionsPrefix())) {
         prefix = fOwner.getHeaderDefinitionsPrefix();
      }

      if (fDerivedFrom != null) {
         return prefix+fDerivedFrom.getHeaderStructName();
      }
      if ((fHeaderStructName != null) && (!fHeaderStructName.isEmpty())) {
         return prefix+fHeaderStructName;
      }
      fHeaderStructName = getStructNamefromName(fName);
      return prefix+fHeaderStructName;
   }
   
   /**
    * Get the name of the file containing this description
    * 
    * @return
    */
   public String getSourceFilename() {
      return fSourceFilename;
   }

   /**
    * Set the name of the file containing this description
    * 
    */
   public void setSourceFilename(String sourceFilename) {
      this.fSourceFilename = sourceFilename;
   }

   /** 
    * Get description of peripheral
    * 
    * @return
    */
   public String getDescription() {
      return fDescription;
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
      Matcher m = p.matcher(fDescription);
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
      Pattern p = Pattern.compile("^.*title=(LPC\\d+x+)?(.*)Modification.*$", Pattern.DOTALL);
      Matcher m = p.matcher(description);
      if (m.matches()) {
         description = m.group(2).replaceAll("\n", " ");
      }
      this.fDescription = getSanitizedDescription(description.trim());
      this.fDescription = simplifyDescription(this.fDescription);
   }

   /**
    * Get prefix for register names
    * 
    * @param fName
    */
   public String getPrependToName() {
      return fPrependToName;
   }

   /**
    * Set prefix for register names
    * 
    */
   public void setPrependToName(String prependToName) {
      prependToName = prependToName.trim();
      if (prependToName.equals((getName()+"_"))) {
         // Discard
         prependToName = "";
      }
      if (prependToName.equals(getName())) {
         // Discard
         prependToName = "";
      }
      fPrependToName = prependToName;
   }

   /**
    * Get base address of peripheral
    * All registers are relative to this address
    * 
    * @return
    */
   public long getBaseAddress() {
      return fBaseAddress;
   }

   /**
    * Get base address of peripheral
    * All registers are relative to this address
    * 
    * @return
    */
   public void setBaseAddress(long baseAddress) {
      this.fBaseAddress = baseAddress;
   }

   /**
    * Get list of address blocks that describe the memory occupied by the peripheral<br>
    * If derived may return the entries from the derived from peripheral
    * 
    * @return
    */
   public ArrayList<AddressBlock> getAddressBlocks() {
      ArrayList<AddressBlock> addressBlocks = fAddressBlocks;
      if ((addressBlocks == null) || addressBlocks.isEmpty()) {
         Peripheral derivedFrom = getDerivedFrom();
         if (derivedFrom != null) {
            addressBlocks = derivedFrom.getAddressBlocks();
         }
      }
      return addressBlocks;
   }

   /** 
    * Clears (removes) the current address blocks
    * 
    * @throws Exception 
    */
   public void clearAddressBlocks() {
      fAddressBlocks = new ArrayList<AddressBlock>();
   }
   
   /**
    * Adds an address block to the peripheral
    * 
    * @param addressBlock
    * 
    * @throws Exception
    */
   public void addAddressBlock(AddressBlock addressBlock) throws Exception {
      if (ModeControl.isRegenerateAddressBlocks()) {
         // Discard address blocks when regenerating them
         return;
      }
      fAddressBlocks.add(addressBlock);
   }

   /** Add register to peripheral
    * 
    * @param cluster
    * 
    * @throws Exception
    */
   public void addRegister(Cluster cluster) throws Exception {
      if (fDerivedFrom != null) {
         throw new Exception("Cannot addd register to derived peripheral");
      }
      fRegisters.add(cluster);
      fSorted = false;
   }

   /**
    * Creates a set of address blocks to cover the current register set
    * 
    * @throws Exception 
    */
   public void createAddressBlocks() throws Exception {
      if (fDerivedFrom != null) {
         // Address blocks are determined from registers which are determined by the peripheral
         // derived from so they should always agree.
         return;
      }
//      // XXX Delete OK
//      System.err.println("Creating address blocks for ============= " + getName());
      sortRegisters();
      try {
         int isolatedIndex = 0;
         clearAddressBlocks();
         AddressBlocksMerger addressBlocksMerger = new AddressBlocksMerger(this);
         for (Cluster cluster : fRegisters) {
//            System.err.println("Peripheral = " + cluster.getName());
            if (cluster.isIsolated()) {
               isolatedIndex = addressBlocksMerger.createNewIsolation();
            }
            cluster.addAddressBlocks(addressBlocksMerger, isolatedIndex);
         }
         addressBlocksMerger.generate();
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
      if ((fGroupName != null) && (!fGroupName.isEmpty())) {
         return fGroupName;
      }
      if (fDerivedFrom != null) {
         return fDerivedFrom.getGroupName();
      }
      Matcher matcher = groupNamePattern.matcher(fName);
      if (matcher.matches()) {
         fGroupName = matcher.replaceAll("$1");
//         System.err.println(String.format("'%s' => '%s'",  name, groupName));
         return fGroupName;
      }
      return fName;
   }

   /**
    * Set group name associated with this peripheral
    * 
    * @return
    */
   public void setGroupName(String groupName) {
      this.fGroupName = groupName;
   }

   /**
    * Get (default) register width associated with this peripheral
    * 
    * @return
    */
   public long getWidth() {
      return fWidth;
   }

   /**
    * Get (default) register width associated with this peripheral
    * 
    * @return
    * @throws Exception 
    */
   public void setWidth(long size) throws Exception {
      if (fDerivedFrom != null) {
         throw new Exception("Cannot set default width for derived peripheral");
      }
      this.fWidth = size;
   }   

   /**
    * Get (default) register access type for this peripheral
    * 
    * @return the accessType
    */
   public AccessType getAccessType() {
      if (fDerivedFrom != null) {
         return fDerivedFrom.getAccessType();
      }
      return fAccessType;
   }

   /**
    * Set (default) register access type for this peripheral
    * 
    * @return the accessType
    * @throws Exception 
    */
   public void setAccessType(AccessType accessType) throws Exception {
      if (fDerivedFrom != null) {
         throw new Exception("Cannot set accesstype for derived peripheral");
      }
      this.fAccessType = accessType;
   }

   /**
    * Get (default) register reset value for this peripheral
    * 
    * @return the resetValue
    */
   public long getResetValue() {
      return fResetValue;
   }

   /**
    * Set (default) register reset value for this peripheral
    * 
    * @param resetValue
    * @throws Exception 
    */
   public void setResetValue(long resetValue) throws Exception {
      if (fDerivedFrom != null) {
         throw new Exception("Cannot set default reset value for derived peripheral");
      }
      this.fResetValue = resetValue;
   }

   /**
    * Get (default) register reset value for this peripheral
    * 
    * @return the resetMask
    */
   public long getResetMask() {
      return fResetMask;
   }

   /**
    * Set (default) register reset mask for this peripheral
    * 
    * @param resetMask the resetMask to set
    * @throws Exception 
    */
   public void setResetMask(long resetMask) throws Exception {
      if (fDerivedFrom != null) {
         throw new Exception("Cannot set default reset mask for derived peripheral");
      }
      this.fResetMask = resetMask;
   }

   /**
    * Indicates the preferred access width for address blocks<br>
    * Address blocks will be merged ignoring individual widths
    * 
    * @return Preferred access width for address blocks
    */
   public int getPreferredAccessWidth() {
      return fPreferredAccessWidth;
   }

   /**
    * Sets the preferred access width for address blocks<br>
    * Address blocks will be merged ignoring individual widths (if non-zero)
    * 
    * @param Preferred access width for address blocks
    * @throws Exception 
    */
   public void setBlockAccessWidth(int preferredAccessWidth) throws Exception {
      this.fPreferredAccessWidth = preferredAccessWidth;
      if (fDerivedFrom != null) {
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
      return fForcedBlockMutiple;
   }

   /**
    * Sets the forced access width for address blocks. 
    * Address blocks will be rounded up to this size and merged ignoring individual widths
    * 
    * @param Forced access width for address blocks
    * @throws Exception 
    */
   public void setForcedBlockMultiple(int forcedBlockMutiple) throws Exception {
      if (fDerivedFrom != null) {
         throw new Exception("Cannot set forced access wdth for derived peripheral");
      }
      this.fForcedBlockMutiple = forcedBlockMutiple;
   }

   /**
    * Gets peripheral this peripheral is derived from
    * 
    * @return Derived from peripheral or null if none 
    */
   public Peripheral getDerivedFrom() {
      return fDerivedFrom;
   }

   /**
    * Follows chain of derived from peripherals to base peripheral
    * 
    * @return Base peripheral
    */
   public Peripheral getBasePeripheral() {
      Peripheral peripheral = this;
      while (peripheral.getDerivedFrom() != null) {
         peripheral = peripheral.getDerivedFrom();
      }
      return peripheral;
   }

   /**
    * Sets this device a being derived from another peripheral.
    * 
    * @param derivedFrom The device derived from
    */
   public void setDerivedFrom(Peripheral derivedFrom) {
      clearAddressBlocks();
      this.fDerivedFrom = derivedFrom;
   }

   /**
    * Sorts usedBy list and removed duplicates
    */
   public void sortUsedBy() {
      sortUsedBy(fUsedBy);
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
      return fUsedBy;
   }

   /**
    * Add record that this peripheral is used by a particular device
    * 
    * @param usedBy
    */
   public void addUsedBy(String usedBy) {
      this.fUsedBy.add(usedBy);
   }

   /**
    * Add record that this peripheral is used by a particular device
    * 
    * @param fUsedBy
    */
   public void clearUsedBy() {
      
      this.fUsedBy = new ArrayList<String>();
   }

   /**
    * Add interrupt entry for this peripheral 
    * 
    * @param entry
    */
   void addInterruptEntry(InterruptEntry entry) {
      if (fInterrupts  == null) {
         fInterrupts = new ArrayList<InterruptEntry>();
      }
      fInterrupts.add(entry);
   }

   /**
    * Clear interrupt entries for this peripheral 
    */
   void clearInterruptEntries() {
      fInterrupts = null;
   }

   /** 
    * Get list of interrupt entries for this peripheral.
    * 
    * @return
    */
   public ArrayList<InterruptEntry> getInterruptEntries() {
      return fInterrupts;
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
            this.fPrependToName.equals(other.fPrependToName) &&
            (this.fBaseAddress == other.fBaseAddress) &&
            this.fAppendToName.equals(other.fAppendToName) &&
            this.getGroupName().equals(other.getGroupName());
      if (!rv) {
         return false;
      }
      if ((this.fDerivedFrom == null) && (other.fDerivedFrom == null)) {
         // Both not derived - compare structure
         rv = equivalentStructure(other);
      }
      else if ((this.fDerivedFrom != null) && (other.fDerivedFrom != null)) {
         // Both derived - check if derived from same peripheral
         rv = this.fDerivedFrom.getName().equals(other.fDerivedFrom.getName());
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
      // These are a problem since the interrupt entries may change while being constructed
//      if ((getInterruptEntries() == null) || (other.getInterruptEntries() == null)) {
//         return false;
//      }
//      if (getInterruptEntries().size() != other.getInterruptEntries().size()) {
//         return false;
//      }
//      sortInterrupts();
//      for (int index=0; index<fInterrupts.size(); index++) {
//         InterruptEntry int1 = fInterrupts.get(index);
//         InterruptEntry int2 = other.fInterrupts.get(index);
//            if (!int1.equals(int2)) {
//               return false;
//            }
//      }
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
            (this.fWidth       == other.fWidth) &&
            (this.fResetValue  == other.fResetValue) &&
            (this.fResetMask   == other.fResetMask) &&
            (this.fAccessType  == other.fAccessType) &&
            (fRegisters.size() == other.fRegisters.size());

      if (!rv) {
         setReasonForDifference("Basic peripheral description different");
         return false;
      }
      sortRegisters();
      other.sortRegisters();
      for (int index=0; index<fRegisters.size(); index++) {
         Cluster reg1 = fRegisters.get(index);
         Cluster reg2 = other.fRegisters.get(index);
         if (!reg1.equivalent(reg2, MatchOptions.MATCH_SUBS|MatchOptions.MATCH_NAMES)) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Sort peripheral registers by address offset
    * 
    */
   @SuppressWarnings("unused")
   private void sortInterrupts() {
      Collections.sort(fInterrupts, new Comparator<InterruptEntry>() {
         @Override
         public int compare(InterruptEntry interruptEntry1, InterruptEntry interruptEntry2) {
            return interruptEntry1.getIndexNumber() - interruptEntry2.getIndexNumber();
         }
      });
      fSorted = true;
   }
   
   /**
    * Sort peripheral registers by address offset
    * 
    */
   private void sortRegisters() {
      if (fSorted) {
         return;
      }
      sortRegisters(fRegisters);
      fSorted = true;
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
      if (fGroupName != null) {
         System.out.println("       groupName = " + getGroupName());
      }
      if (fAddressBlocks != null) {
         for (AddressBlock addressBlock : fAddressBlocks) {
            addressBlock.report();
         }
      }
      for (Cluster register : fRegisters) {
         register.report();
      }
   }

   static boolean isNewConflict(String name) {
      if (fConflictedNames.contains(name)) {
         return false;
      }
      fConflictedNames.add(name);
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
   
   /**
    * Maps peripheral names in the data to preferred Freescale names
    */
   public static String getMappedPeripheralName(String name) {
      if (!isMapFreescaleCommonNames()) {
         return name;
      }
      // TODO Where Peripheral names are mapped
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
   
   public static ArrayList<ComplexStructuresInformation> getComplexStructures(String name) {
      final HashMap<String, ArrayList<ComplexStructuresInformation>> complexStructures = new HashMap<String,  ArrayList<ComplexStructuresInformation>>(20);
      if (complexStructures.isEmpty()) {
         ArrayList<ComplexStructuresInformation> entry = null;

         //TODO Where complex structures are defined
         /**
          * Substitution applied to the register name macros <pre>
          * "@p" => Peripheral name + "_"
          * "@a" => Cluster base name (name without formatting)
          * "@f" => Register name
          * "@i" => index<br>
          * e.g. "@pTAGVDW@i@f" => "FMC_TAGVDW1S0"
          */
         //                                         pattern                    arrayName nameIndex fieldName nameFormat
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(E.R)(\\d+)$",                 "$1","$2","$1", "SP,@p@f@i"));
         entry.add(new ComplexStructuresInformation("^(RGD)(\\d+)_(WORD.*)$",        "$1","$2","$3", "RGD,@p@a@i_@f"));
         complexStructures.put("MPU",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(DATAW)(\\d+)(S.*)$",          "$1","$2","$3", "DATAW,@pDATAW@i@f"));
         entry.add(new ComplexStructuresInformation("^(TAGVDW)(\\d+)(S.*)$",         "$1","$2","$3", "TAGVDW,@pTAGVDW@i@f"));
         complexStructures.put("FMC",  entry);
         
         //                                         pattern                    arrayName nameIndex fieldName nameFormat
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(TVAL|CVAL|TCTRL)(\\d+)$", "$1","$2","$1",   "TMR,@p@f@i"));
         complexStructures.put("LPIT0",  entry);
         complexStructures.put("LPIT1",  entry);
         
         //                                         pattern                    arrayName nameIndex fieldName nameFormat
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(EICHD)(\\d+)_(WORD0|WORD1)$", "$1","$2","$3",   "EICHDn,@p@f@i"));
         complexStructures.put("EIM",  entry);
         
         //                                         pattern                    arrayName nameIndex fieldName nameFormat
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(EAR)(\\d+)$", "$1","$2","$1",   "EARn,@p@f@i"));
         complexStructures.put("ERM",  entry);
         
         //                                         pattern                    arrayName nameIndex fieldName nameFormat
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(_EmbeddedRAM)(\\d+)(LL|LU|HL|HU)$", "$1","$2","DATA_8$3",   "RAMn,@p@f@i"));
         complexStructures.put("CSE_PRAM",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CS|ID|WORD0|WORD1)(\\d+)$",    "$1","$2","$1", "MB,@p@f@i"));
         entry.add(new ComplexStructuresInformation("^(WMB)(\\d+)_(CS|ID|D03|D47)$",   "$3","$2","$1n_$3", "WMB,@p@f@i"));
         complexStructures.put("CAN0",  entry);
         complexStructures.put("CAN1",  entry);
         complexStructures.put("CAN2",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CS[^\\d]+)(\\d+)$",           "$1","$2","$1", "CS,@p@f@i"));
         complexStructures.put("FB",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CH)(\\d+)(C1|S|DLY.*)$",      "$1","$2","$3", "CH,@pCH@i@f"));
         entry.add(new ComplexStructuresInformation("^(DAC)(INTC|INT)(\\d+)$",       "$1","$3","$2", "DAC,@pDAC@f@i"));
         complexStructures.put("PDB",  entry);
         complexStructures.put("PDB0",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         //                                         pattern                    arrayName nameIndex fieldName nameFormat
         entry.add(new ComplexStructuresInformation("^CTX(\\d+)_(KEY|CTR|RGD)(.*?)(\\d)?$",      "CTX","$1","$2$4", "CTX,@p@a@i_@f"));
         complexStructures.put("OTFAD",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^([^\\d]+)(\\d+)$",             "$1","$2","$1", "CHANNEL,@p@f@i"));
         complexStructures.put("PIT",  entry);
         complexStructures.put("PIT0",  entry);
         
         //                                                pattern                             arrayName nameIndex fieldName nameFormat
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CH)(\\d+)_(CSR|VEC|IER_31_0|IPR_31_0)$", "$1","$2","$3", "CHANNEL,@p@f@i"));
         complexStructures.put("INTMUX",   entry);
         complexStructures.put("INTMUX0",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(?:DMA_)?(TCD)(\\d+)_(.*)$",     "$1","$2","$3", "TCD,@p@a@i_@f"));
         entry.add(new ComplexStructuresInformation("^(SAR|DAR|DSR.*|DCR)(\\d+)$",     "$1","$2","$1", "DMA,@p@f@i"));
         entry.add(new ComplexStructuresInformation("^(SAR|DAR|DSR|DCR|BCR)_?(\\d+)$", "$1","$2","$1", "CH,@p@f@i"));
         complexStructures.put("DMA",   entry);
         complexStructures.put("DMA0",  entry);
         complexStructures.put("DMA1",  entry);
         
//         entry = new ArrayList<ComplexStructuresInformation>();
//         entry.add(new ComplexStructuresInformation("^(SAR|DAR|DSR.*|DCR)(\\d+)$",  "$1","$2","$1", "DMA,@p@f@i"));
//         complexStructures.put("DMA",   entry);
         
//         entry = new ArrayList<ComplexStructuresInformation>();
//         entry.add(new ComplexStructuresInformation("^(SAR|DAR|DSR|DCR|BCR)_?(\\d+)$", "$1","$2","$1", "CH,@p@f@i"));
//         complexStructures.put("DMA",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(C)(\\d+)(SC|V)$",               "$1","$2","Cn$3", "CONTROLS,@pC@i@f"));
//         entry.add(new ComplexStructuresInformation("^(C)(\\d+)(V_MIRROR)$",           "$1","$2","Cn$3", "MIRROR,@pC@i@f"));
         complexStructures.put("FTM0",  entry);
         complexStructures.put("FTM1",  entry);
         complexStructures.put("FTM2",  entry);
         complexStructures.put("FTM3",  entry);
         complexStructures.put("TPM0",  entry);
         complexStructures.put("TPM1",  entry);
         complexStructures.put("TPM2",  entry);
         complexStructures.put("TPM3",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(DAT)(\\d+)(.*)$",             "$1","$2","$1$3", "DAT,@p@f@i"));
         complexStructures.put("DAC0",  entry);
         complexStructures.put("DAC1",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(.RS)(\\d+)$",                 "$1","$2","$1",   "SLAVE,@p@f@i"));
//         entry.add(new ComplexStructuresInformation("^(MGPCR)(\\d+)$",               "$1","$2","$1",   "MASTER,@p@f@i"));
         complexStructures.put("AXBS",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(ENDPT)(\\d+)$",               "$1","$2","$1",   "ENDPOINT,@p@f@i"));
         complexStructures.put("USB0",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(COMP|MASK|FUNCTION)(\\d+)$",  "$1","$2","$1",   "COMPARATOR,@p@f@i"));
         complexStructures.put("DWT",  entry);
         
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(COMP|MASK|FCT)(\\d+)$",       "$1","$2","$1",   "COMPARATOR,@p@f@i"));
         complexStructures.put("MTBDWT",  entry);
         
         // ==== Coldfire V2 ====
         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CODE|CTRL|TIME|ID|DATA_WORD_1|DATA_WORD_2|DATA_WORD_3|DATA_WORD_4)_?(\\d+)$",
               "$1","$2","$1", "MB,@p@f@i"));
         complexStructures.put("CANMB",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(DTMR|DTXMR|DTER|DTRR|DTCR|DTCN)_?(\\d+)$",
               "$1","$2","$1", "CH,@p@f@i"));
         complexStructures.put("DTIM",  entry);

         entry = new ArrayList<ComplexStructuresInformation>();
         entry.add(new ComplexStructuresInformation("^(CSAR|CSMR|CSCR)_?(\\d+)$",
               "$1","$2","$1", "CH,@p@f@i"));
         complexStructures.put("FBCS",  entry);

//         entry = new ArrayList<ComplexStructuresInformation>();
//         entry.add(new ComplexStructuresInformation("^(PORTT(?:E|F|G|H|I|J))(\\d+)$",
//               "$1","$2","$1", "CH,@p@f@i"));
//         complexStructures.put("GPIO",  entry);

      }
      return complexStructures.get(name);
   }
   
   /**
    * Reformats the data to take advantage of complex array structures
    * @throws Exception 
    */
   private void extractComplexStructures() throws Exception {
      ArrayList<ComplexStructuresInformation> information = getComplexStructures(this.getName());
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
      
      for(Cluster reg : fRegisters) {
         reg.setDeleted(false);
      }
      ArrayList<Register> removedRegisters = new ArrayList<Register>();
      Cluster cluster = null;

      for (int reg1 = 0; reg1<getRegisters().size(); reg1++) {
         // Only match simple registers
         if (!(fRegisters.get(reg1) instanceof Register)) {
            continue;
         }
         Register mergeReg   = (Register)fRegisters.get(reg1);
         String   mergeName  = mergeReg.getName();
         // Already removed?
         if (mergeReg.isDeleted()) {
//            System.err.println(String.format("Skipping %s as already deleted", mergeName));
            continue;
         }
         boolean debug = false;//mergeName.matches("CH0DLY.*");
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
            if (!(fRegisters.get(reg2) instanceof Register)) {
               continue;
            }
            // Get candidate to match
            Register victimReg   = (Register)fRegisters.get(reg2);
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
                        String.format("extractComplexStructures():   %s <=> %s, Expected offset(stride) %d, found %s ",
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
         fRegisters.remove(victimReg);
      }
      if (cluster != null) {
         for (Cluster clusterReg : cluster.getRegisters()) {
            clusterReg.setAddressOffset(clusterReg.getAddressOffset()-cluster.getAddressOffset());
         }
      }
      sortRegisters();
   }
   
   // This is a list of peripherals and particular registers not to turn into arrays
   public static String getExcludedSimpleRegisterArrayPeripherals(String name) {
      final HashMap<String, String> excludedCommonRegisterPeripherals = new HashMap<String,  String>(200);
      if (excludedCommonRegisterPeripherals.isEmpty()) {
         // TODO Where to select peripherals and particular registers not to turn into arrays
         excludedCommonRegisterPeripherals.put("ITM",   "PID.*"); // PIDs are in a strange order
         excludedCommonRegisterPeripherals.put("DWT",   "PID.*");
         excludedCommonRegisterPeripherals.put("FPB",   "PID.*");
         excludedCommonRegisterPeripherals.put("TPIU",  "PID.*");
         excludedCommonRegisterPeripherals.put("ETM",   "PID.*");
         excludedCommonRegisterPeripherals.put("ETB",   "PID.*");
         excludedCommonRegisterPeripherals.put("ETF",   "PID.*");
         excludedCommonRegisterPeripherals.put("FMC",   "PID.*");
         excludedCommonRegisterPeripherals.put("ADC",   "CLP.*");    // Leave as individual registers
         excludedCommonRegisterPeripherals.put("ADC0",  "CLP.*");    // Leave as individual registers
         excludedCommonRegisterPeripherals.put("ADC1",  "CLP.*");    // Leave as individual registers
         excludedCommonRegisterPeripherals.put("PCC",   "PCC_.*");   // Each peripheral has own register
         excludedCommonRegisterPeripherals.put("TRGMUX",".*_.*");    // Registers are odd
         excludedCommonRegisterPeripherals.put("TRGMUX0",".*_.*");   // Registers are odd
         excludedCommonRegisterPeripherals.put("TRGMUX1",".*_.*");   // Registers are odd
         excludedCommonRegisterPeripherals.put("MCG",   ".*");       // Some odd reg. pairs are better separate
      }
      return excludedCommonRegisterPeripherals.get(name);
   }
   
   static class PatternTuple {
      final Pattern pattern;      // Pattern to match
      final String  indexPattern; // String to produce result
      final String  namePattern;  // String to produce result
      
      public PatternTuple(String pattern, String namePattern, String indexPattern) {
         this.pattern    = Pattern.compile(pattern);
         this.namePattern = namePattern;
         this.indexPattern = indexPattern;
      }
      String getBaseRegisterName(String regName) {
         Matcher m = pattern.matcher(regName);
         return m.replaceAll(namePattern);
      }
      String getIndex(String regName) {
         Matcher m = pattern.matcher(regName);
         return m.replaceAll(indexPattern);
      }
   }
   
   public static PatternTuple getRegisterArrayPatterns(String name) {
      // This is a list of peripherals with special matching patterns for register combining
      final HashMap<String, PatternTuple> freescalePeripheralRegisterArrayPatterns = new HashMap<String, PatternTuple>();
      if (freescalePeripheralRegisterArrayPatterns.isEmpty()) {
         freescalePeripheralRegisterArrayPatterns.put("ADC0", new PatternTuple("(SC1|R|.+)([0-9]+)(.*)$", "$1%s$3",  "$2")); // Special pattern for ADC
         freescalePeripheralRegisterArrayPatterns.put("ADC1", new PatternTuple("(SC1|R|.+)([0-9]+)(.*)$", "$1%s$3",  "$2")); // Special pattern for ADC
         freescalePeripheralRegisterArrayPatterns.put("ITM",  new PatternTuple("(.+)([0-9]+)(.*)$",       "$1%s$3",  "$2")); // Special pattern for ITM
         freescalePeripheralRegisterArrayPatterns.put("ITM0", new PatternTuple("(.+)([0-9]+)(.*)$",       "$1%s$3",  "$2")); // Special pattern for ITM
         freescalePeripheralRegisterArrayPatterns.put("PDB",  new PatternTuple("(.+)([0-9]+)(.*)$",       "$1%s$3",  "$2")); // Special pattern for PDB
         freescalePeripheralRegisterArrayPatterns.put("PDB0", new PatternTuple("(.+)([0-9]+)(.*)$",       "$1%s$3",  "$2")); // Special pattern for PDB
         freescalePeripheralRegisterArrayPatterns.put("LTC",  new PatternTuple("(.+)_([0-9]+)$",          "$1%s",    "$2")); // Special pattern for LTC
         freescalePeripheralRegisterArrayPatterns.put("LTC0", new PatternTuple("(.+)_([0-9]+)$",          "$1%s",    "$2")); // Special pattern for LTC
      }
      PatternTuple pair = freescalePeripheralRegisterArrayPatterns.get(name);
      if (pair == null) {
         //           prefix index suffix
         // pair = new PatternTuple("(.+?)([0-9|A-F|a-f]+)$",  "$1%s", "$2");
         pair = new PatternTuple("(.+?)(\\d+|[A-F|a-f])$",  "$1%s", "$2");
      }
      return pair;
   }
   
   /**
    * Reformats the data to take advantage of simple array structures
    * 
    * @param registerList              List of register from Peripheral or Cluster
    * @param excludedRegisterPattern   Pattern to exclude register to process
    * @param matchInformation          Pattern to match registers
    * @return 
    * 
    * @throws Exception
    */
   void extractSimpleRegisterArrays(
         ArrayList<Cluster> registerList, 
         Pattern excludedRegisterPattern, 
         PatternTuple matchInformation) throws Exception {
      
      sortRegisters(registerList);

      boolean  debugThisPeripheral = false; //getName().equals("ADC0"); 
      if (debugThisPeripheral) {
         System.err.println(String.format("    extractSimpleRegisterArrays(%s):1", getName()));
      }
      for (int reg1 = 0; reg1<registerList.size(); reg1++) {
         Cluster mergeCluster = registerList.get(reg1);
         if (!(mergeCluster instanceof Register)) {
            extractSimpleRegisterArrays(mergeCluster.getRegisters(), excludedRegisterPattern, matchInformation);
            continue;
         }
         Register mergeReg = (Register)mergeCluster;
         if (mergeReg.getDerivedFrom() != null) {
            // Don't merge derived registers
            continue;
         }
         if (mergeReg.getDimension()>=1) {
            // Don't merge register arrays
            continue;
         }
         String   mergeName = mergeReg.getName();
         
         boolean  debugThisRegister = debugThisPeripheral && mergeName.equals("CV1"); 
         if (debugThisRegister) {
            System.err.println(String.format("        extractSimpleRegisterArrays():2, reg=\"%s\"", getName()+":"+mergeName));
         }
         if (mergeName.contains("%s")) {
            throw new Exception("Merging register which is already merged " + getName()+"."+mergeReg.getName());
         }
         if ((excludedRegisterPattern != null) && excludedRegisterPattern.matcher(mergeName).matches()) {
            continue;
         }
         /** List of indexes for the elements - extracted from element names */
         ArrayList<String>    dimensionIndexes        = new ArrayList<String>();

         /** Increment in bytes to next element - based on element width */
         long                 dimensionIncrement      = (mergeReg.getWidth()+7)/8;
         
         /** Offset of start of array element being constructed */
         long                 rootOffset              = mergeReg.getAddressOffset();

         /** Index extracted from merge register */
         String               index                   = matchInformation.getIndex(mergeName);
         /* Create modified description */
         String               descriptionPattern      = "([a-z|A-Z|\\s]+)"+index+"([a-z|A-Z|\\s]*)";
         String               mergePatternDescription = mergeReg.getDescription().replaceFirst(descriptionPattern, "$1%s$2");
         
         int                  dimension               = 1;
         dimensionIndexes.add(index);
         /** Register name with %s substitution for index */
         String               modifiedRegisterName            = matchInformation.getBaseRegisterName(mergeName);
         

         // Go through other register looking for candidates that match the current register
         // When found add to removedRegisters list.
         ArrayList<Register> removedRegisters = new ArrayList<Register>();
         for (int reg2 = reg1+1; reg2<registerList.size(); reg2++) {
            
            // Only process registers (not clusters)
            if (!(registerList.get(reg2) instanceof Register)) {
               continue;
            }
            Register victimReg  = (Register)registerList.get(reg2);
            if (victimReg.getDerivedFrom() != null) {
               // Don't merge derived registers
               continue;
            }
            String   victimName = victimReg.getName();

            boolean  debugThisMatch = debugThisRegister && victimName.equals("CV2"); 
            if (debugThisMatch) {
               System.err.println(
                     String.format("            extractSimpleRegisterArrays(), comparing %-20s ?= %-20s",
                           "\""+mergeName+"\"",
                           "\""+victimName+"\""));
            }
            
            // Check if candidate matches the same pattern as original register i.e.
            // Both register name should be the same after applying the regular expression
            
            /** Name of register based on victim register */
            String victimModifiedRegisterName = matchInformation.getBaseRegisterName(victimName);

            if (!victimModifiedRegisterName.equals(modifiedRegisterName)) {
//               System.err.println(
//                     String.format("    extractSimpleRegisterArrays(), rejecting %-20s != %-20s",
//                           "\""+mergeName+"\"",
//                           "\""+victimName+"\""));
               continue;
            }
            // Extract the variant part of the candidate name
            String victimIndex  = matchInformation.getIndex(victimName);
            
            // Check that the register indexing matches the expected sequence
            if ((rootOffset+(dimension*dimensionIncrement)) != victimReg.getAddressOffset()) {
//               System.err.println(
//                     String.format("    extractSimpleRegisterArrays(), failed combining %-20s & %-20s, expected offset 0x%X != 0x%X",
//                           "\""+mergeReg.getName()+"\"",
//                           "\""+victimReg.getName()+"\"",
//                           (rootOffset+(dimension*dimensionIncrement)),
//                           victimReg.getAddressOffset()
//                           ));
               continue;
            }
            // Check if registers are equivalent (apart from indexed stuff)
            if (!mergeReg.equivalent(victimReg, "(.+?)("+index+")(.*)", "(.+?)("+victimIndex+")(.*)", MatchOptions.MATCH_SUBS|MatchOptions.MATCH_NAMES)) {
               continue;
            }
            // Update dimension for index checking
            dimension++;
            
            // Add candidate to removed registers
            removedRegisters.add(victimReg);
            
            // Add extracted index to dimension indexes for substitution when expanding the register
            dimensionIndexes.add(victimIndex);
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
               registerList.remove(victimReg);
            }
            mergeReg.setName(modifiedRegisterName);
            mergeReg.setDescription(mergePatternDescription);
            mergeReg.setDimensionIncrement((int)dimensionIncrement);
            mergeReg.setDimensionIndexes(dimensionIndexes);
//            mergeReg.setResetMask(0);
//            mergeReg.setResetValue(0);
         }
      }
      sortRegisters(registerList);
   }
   
   /**
    * Reformats the data to take advantage of simple array structures
    * 
    * @throws Exception 
    */
   private void extractSimpleRegisterArrays() throws Exception {
      
      if (getDerivedFrom() != null) {
         // Don't process derived peripherals
         return;
      }
      // Check if peripheral is excluded
      String excludedRegisterName = getExcludedSimpleRegisterArrayPeripherals(getName());
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
      PatternTuple matchInformation = getRegisterArrayPatterns(getName());
      
      ArrayList<Cluster> registerList = getRegisters();
      
      extractSimpleRegisterArrays(registerList, excludedRegisterPattern,  matchInformation);
   }
   
   public void optimise() throws Exception {
      sortRegisters(fRegisters);

      if (isExtractSimpleRegisterArrays()) {
         extractSimpleRegisterArrays();
      }
      if (isExtractComplexStructures()) {
         extractComplexStructures();
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
      for (Cluster r:getRegisters()) {
         r.optimise();
      }
   }

   private static final String DEVICE_LIST_PREAMBLE = 
           "<!--\n"
         + "Devices using this peripheral: \n";
   
   private static final String DEVICE_LIST_POSTAMBLE = 
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
      writeSVD(writer, standardFormat, owner, indent);
   }
   
   /**
    *    Writes the Peripheral description to file in a SVF format
    *   
    *    @param writer         The destination for the XML
    *    @param standardFormat Suppresses some non-standard size optimisations 
    *    @param owner          The owner - This is used to reduce the size by inheriting default values. May be null.
    *    @param indent         The starting indent for the XML
    *  
    *    @throws Exception 
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat, DevicePeripherals owner, int indent) throws Exception {
      final String indenter = RegisterUnion.getIndent(indent);
      sortRegisters();
      
      if (getUsedBy().size()>0) {
         writer.print(DEVICE_LIST_PREAMBLE);
         for (String deviceName : getUsedBy()) {
            writer.println(String.format( indenter+"%s", deviceName));
         }
         writer.print(DEVICE_LIST_POSTAMBLE);
      }

      if ((fDerivedFrom != null) && !ModeControl.isExpandDerivedPeripherals()){
         writeDerivedFromSVD(writer, standardFormat, indent);
         return;
      }

      writer.print(                       indenter+"<peripheral");
      writer.println(">");

      if (getSourceFilename() != null) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.SOURCEFILE_PROCESSING+" \"%s\" ?>", SVD_XML_BaseParser.escapeString(getSourceFilename())));
      }
      if (getPreferredAccessWidth() != 0) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.PREFERREDACCESSWIDTH_PROCESSING+" \"%d\" ?>", getPreferredAccessWidth()));
      }
      if (getForcedBlockMultiple() != 0) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.FORCED_BLOCK_PROCESSING+" \"%d\" ?>", getForcedBlockMultiple()));
      }
      if (isRefreshAll()) {
         writer.println(String.format(       indenter+"   <?"+SVD_XML_Parser.REFRESH_WHOLE_PERIPHERAL_PROCESSING+"?>"));
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
      writeParameters(writer, indent);
      if (!isCollectVectors() && (getInterruptEntries() != null)) {
         for (InterruptEntry interrupt : getInterruptEntries()) {
            interrupt.writeSVD(writer, indent+3);
         }
      }
      if (getAddressBlocks() != null) {
         for (AddressBlock addressBlock : getAddressBlocks()) {
            addressBlock.writeSVD(writer, standardFormat);
         }
      }
      if (getTemplate() != null) {
         writer.print(indenter+"   <template>");
         writer.print(getTemplate());
         writer.println("</template>");
      }
      writer.println(                     indenter+"   <registers>");
//      for (Cluster clusterOrRegister : getRegisters()) {
//         if (clusterOrRegister.isHidden()) {
//            clusterOrRegister.writeSVD(writer, standardFormat, this, indent+6);
//            writer.flush();
//         }
//      }
      for (Cluster clusterOrRegister : getRegisters()) {
//         if (!clusterOrRegister.isHidden()) {
            clusterOrRegister.writeSvd(writer, standardFormat, this, indent+6);
//         }
      }
      writer.println(                     indenter+"   </registers>");
      writer.println(                     indenter+"</peripheral>");
      writer.flush();
   }

   private void writeParameters(PrintWriter writer, int indent) {
      if ((fParameters == null) || fParameters.isEmpty()) {
         return;
      }
      
      final String indenter = RegisterUnion.getIndent(indent+3);
      writer.println(indenter+"<parameters>");
      
      for (Entry<String, Parameter> e:fParameters.entrySet()) {
         Parameter p = e.getValue();
         writer.print(indenter+"   <parameter>");
         writer.print(String.format(" <value>%s</value>", p.getValue()));
         writer.print(String.format(" <name>%s</name>", p.getName()));
         writer.print(String.format(" <description>%s</description>", p.getDescription()));
         writer.println("</parameter>");
      }
      writer.println(indenter+"</parameters>");
   }

   /**
    *    Writes the Peripheral description to file in a SVF format
    *   
    *    @param writer         The destination for the XML
    *    @param standardFormat Suppresses some non-standard size optimisations 
    *  
    *    @param fOwner   The owner - This is used to reduce the size by inheriting default values
    */
   private void writeDerivedFromSVD(PrintWriter writer, boolean standardFormat, int indent) {
      Peripheral derived = getDerivedFrom();

      String indenter = RegisterUnion.getIndent(indent);
      
      writer.print(String.format(   indenter+"<%s %s=\"%s\">", SVD_XML_Parser.PERIPHERAL_TAG, SVD_XML_Parser.DERIVEDFROM_ATTRIB, SVD_XML_BaseParser.escapeString(derived.getName())));
      
      writer.print(String.format("<%s>%s</%s>", SVD_XML_Parser.NAME_TAG,SVD_XML_BaseParser.escapeString(getName()), SVD_XML_Parser.NAME_TAG));
      if (!getDescription() .equals(getDescription())) {
         writer.print(String.format(indenter+"<%s>%s</%s>", SVD_XML_Parser.DESCRIPTION_TAG,  SVD_XML_BaseParser.escapeString(getDescription()), SVD_XML_Parser.DESCRIPTION_TAG));
      }
      if (!getGroupName().equals(derived.getGroupName())) {
         //XXXX Check this
//         System.err.println(String.format("writeDerivedFromSVD() d=%s, i=%s", derived.getGroupName(), getGroupName()));
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
      boolean doneNewline = false;
      String template = getTemplate(); 
      if (template != null) {
         boolean hasNewLine = template.contains("\n");
         if (hasNewLine) {
            writer.print("\n"+indenter+"   ");
            doneNewline = true;
         }
         writer.print("<template>");
         writer.print(template);
         writer.print("</template>");
         if (hasNewLine) {
            writer.println();
         }
      }
      if (!isCollectVectors() && (getInterruptEntries() != derived.getInterruptEntries())) {
         writer.print('\n');
         doneNewline = true;
         if (getInterruptEntries() == null) {
            System.err.println("Opps - expected vectors in derived peripheral");
         }
         else
         for (InterruptEntry interrupt : getInterruptEntries()) {
            writer.write(RegisterUnion.getIndent(indent+3));
            interrupt.writeSVD(writer, -1);
         }
      }
      if (getAddressBlocks() != derived.getAddressBlocks()) {
         writer.print('\n');
         doneNewline = true;
         for (AddressBlock addressBlock : getAddressBlocks()) {
            addressBlock.writeSVD(writer, standardFormat);
         }
      }
      if (!doneNewline) {
         indenter = "";
      }
      writer.println(String.format(indenter+"</%s>", SVD_XML_Parser.PERIPHERAL_TAG));
   }

   /**
    * Write interrupt #define
    * 
    * @param writer
    */
   void writeHeaderFileInterruptList(PrintWriter writer)  {
      if ((getInterruptEntries() != null) && (getInterruptEntries().size()>0)) {
         writer.print("#define "+getName()+"_IRQS { ");
         for (InterruptEntry entry:getInterruptEntries()) {
            writer.print(entry.getName()+"_IRQn, ");
         }
         writer.println(" }\n");
      }
   }

   private static final String DMA_ENUM_OPENING =
         "\n"+
         "/**\n"+ 
         " * DMA multiplexor slot (source) numbers\n"+
         " */\n"+
         "typedef enum DmaSlot {\n";

   private static final String DMA_FORMAT = "   %-35s = %8s, //!<  %s\n";

   private static final String DMA_ENUM_CLOSING = "} DmaSlot;\n\n";

   /**
    * Create array of DMAMUX slot descrptions
    * 
    * @return array of names or null if not a DMAMUX
    * 
    * @throws UsbdmException if illegal name found
    */
   public String[] getDmaMuxInputs() throws UsbdmException {
      final Pattern peripheralPattern = Pattern.compile("DMAMUX(\\d+)");
      final Matcher peripheralMatcher = peripheralPattern.matcher(getName());
      if (!peripheralMatcher.matches()) {
         return null;
      }
      ArrayList<String> entries = null;
      for (Cluster cluster:getRegisters()) {
         if (cluster instanceof Register) {
            Register register = (Register)cluster;
            final Pattern registerPattern = Pattern.compile("CHCFG_?(LOW|HIGH)?.*");
            final Matcher registerMatcher = registerPattern.matcher(register.getName());
            for (Field field:register.getFields()) {
               if (registerMatcher.matches()) {
                  String modifier    = ""; 
                  if (registerMatcher.group(1) != null) {
                     if (registerMatcher.group(1).equals("LOW")) {
                        modifier    = " (Low channels 0-15)";
                     }
                     else if (registerMatcher.group(1).equals("HIGH")) {
                        modifier    = " (High channels 16-31)";
                     }
                  }
                  if (field.getName().equals("SOURCE")) {
                     for (Enumeration e:field.getEnumerations()) {
                        String identifier = e.getCDescription()+modifier;
                        if (entries == null) {
                           entries = new ArrayList<String>();
                        }
                        entries.add(identifier);
                     }
                  }
               }
            }
         }
      }
      if (entries == null) {
         return null;
      }
      return entries.toArray(new String[entries.size()]);
   }
   
   /**
    * Write closing enum if required.
    * 
    * @param sb
    * @throws UsbdmException 
    */
   static void completeHeaderFileDmaInformation(StringBuilder sb) throws UsbdmException  {
      if (sb.length() > 0) {
         sb.append(String.format(DMA_ENUM_CLOSING));
      }
   }
   /**
    * Write header file miscellaneous information
    * 
    * @param writer
    * @throws UsbdmException 
    */
   void accumulateHeaderFileDmaInformation(StringBuilder sb) throws UsbdmException  {
      // Only applies to DMAMUX devices
      final Pattern peripheralPattern = Pattern.compile("DMAMUX(\\d+)");
      final Matcher peripheralMatcher = peripheralPattern.matcher(getName());
      if (!peripheralMatcher.matches()) {
         return;
      }
      boolean doneBraces = sb.length() > 0;
//      System.err.println("writeHeaderFileDmaInformation() - creating enum for "+ this);
      HashSet<String> usedEnums = null;
      String instance = peripheralMatcher.group(1);
      String enumBasename = "Dma"+instance+"Slot";
      for (Cluster cluster:getRegisters()) {
         if (cluster instanceof Register) {
            Register register = (Register)cluster;
            final Pattern registerPattern = Pattern.compile("CHCFG_?(LOW|HIGH)?.*");
            final Matcher registerMatcher = registerPattern.matcher(register.getName());
            for (Field field:register.getFields()) {
               if (registerMatcher.matches()) {
//                  System.err.println("writeHeaderFileDmaInformation() - matched "+register);
                  String description = ""; 
                  String modifier    = ""; 
                  String offset      = "";
                  if (registerMatcher.group(1) != null) {
                     if (registerMatcher.group(1).equals("LOW")) {
                        description = " - low channels 0-15";
                        modifier    = "Low";
                     }
                     else if (registerMatcher.group(1).equals("HIGH")) {
                        description = " - high channels 16-31";
                        modifier    = "High";
                        offset      = "0x800|";
                     }
                  }
                  if (field.getName().equals("SOURCE")) {
                     for (Enumeration e:field.getEnumerations()) {
                        if (e.getName().startsWith("Reserved")) {
                           continue;
                        }
                        String identifier = enumBasename+modifier+"_"+e.getName();
                        if (!Utiltity.isCIdentifier(identifier)) {
                           throw new UsbdmException("Invalid name for C identifier in DMAMUX names \'"+identifier+"\'");
                        }
                        if (!doneBraces) {
                           sb.append(String.format(DMA_ENUM_OPENING, description));
                           doneBraces = true;
                        }
                        if (usedEnums == null) {
                           usedEnums = new HashSet<String>();
                        }
                        if (usedEnums.contains(identifier)) {
                           continue;
//                           throw new UsbdmException("Repeated enum value for DMA slot" + identifier);
                        }
                        usedEnums.add(identifier);
                        sb.append(String.format(DMA_FORMAT, identifier, offset+e.getValue(), e.getDescription()+description));
                     }
                  }
               }
            }
         }
      }
   }
   
   /**
    * Write header file miscellaneous information
    * 
    * @param writer
    * @throws UsbdmException 
    */
   void writeHeaderFileTemplates(PrintWriter writer) throws UsbdmException  {
      String template = getExtraDeclarations();
      if (template != null) {
         writer.print(template);
      }
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

   static final String DEVICE_OPEN_STRUCT  = "typedef struct %s {\n";
   static final String DEVICE_CLOSE_STRUCT = "} %s;\n\n";

   /**
    * Get struct name e.g. "ADC0_Type"
    * 
    * @return
    */
   public String getSafeHeaderStructName() {
      return getHeaderStructName()+"_Type";
   }
   
   /**
    * Get struct name prefix e.g. "ADC0_"
    * 
    * @return
    */
   public String getHeaderStructNamePrefix() {
      return getHeaderStructName()+"_";
   }
   
   public static void clearTypedefsTable() {
      fTypedefsTable = new HashSet<String>();
   }
   
   public void addTypedefsTable(String name) throws Exception {
      if (fTypedefsTable.contains(name)) {
         throw new Exception("Peripheral Typedef clash - " + this.getName() + ", " + name);
      }
      fTypedefsTable.add(name);
   }

   /**
    *    Writes C code for Peripheral declaration e.g. a typedef for a STRUCT representing all the peripheral registers<br>
    *    e.g. <pre>
    *    <b>typedef struct peripheralName_Type {
    *    ...
    *    } peripheralName_Type;</b>
    *    </pre>
    * 
    *    @param writer
    *    @param devicePeripherals
    */
   public void writeHeaderFileStruct(PrintWriter writer, int indent) throws Exception {

      final String indenter = RegisterUnion.getIndent(indent);
      
      sortRegisters();
      
      RegisterUnion.clearSuffix();
      
      RegisterUnion unionRegisters = new RegisterUnion(writer, indent+3, this, 0L);
      
      writer.print(indenter+String.format(DEVICE_OPEN_STRUCT, getSafeHeaderStructName() ));

      for(Cluster cluster : fRegisters) {
//         if (cluster.getName().startsWith("FILT")) {
//            System.err.print("Writing union - " + cluster + "\n");
//         }
         unionRegisters.add(cluster);
      }
      unionRegisters.toString();
      // Flush current union if exists
      unionRegisters.writeHeaderFileUnion();
      
      addTypedefsTable(getSafeHeaderStructName());
      
      writer.print(indenter+String.format(DEVICE_CLOSE_STRUCT, getSafeHeaderStructName()));
   }

   static final String PARAMETER_TEMPLATE = "#define %-20s %s";
   
   /**
    * Write header file parameters for a peripheral
    * 
    * @param writer
    */
   public void writeHeaderParameters(PrintWriter writer) {
      if ((fParameters == null) || fParameters.isEmpty()) {
         return;
      }
      
      for (Entry<String, Parameter> e:fParameters.entrySet()) {
         Parameter p = e.getValue();
         writer.write(String.format("%-40s/**< %-50s */\n",
               String.format(PARAMETER_TEMPLATE, getHeaderStructName()+"_"+p.getName(), p.getValue()), // #define x y
               p.getDescription()));       // /**< Comment **/
      }
   }
   
   /**
    *    Writes C code for Peripheral declaration e.g. a typedef for a STRUCT representing all the peripheral registers<br>
    *    e.g. <pre><b>typedef struct {...} peripheralName_Type;</b></pre>
    * 
    *    @param writer
    */
   public void writeHeaderFileTypedef(PrintWriter writer) throws Exception {
      final String structGroupSuffix  = "structs";
      final int indent = 0;
      String uniqueId;
      if (getDerivedFrom() != null) {
         uniqueId = " (derived from " + fDerivedFrom.getName() + ")";
      }
      else {
         uniqueId = (getSourceFilename()==null)?"":" (file:"+getSourceFilename()+")";
      }
      writer.print(String.format(DEVICE_HEADER_FILE_STRUCT_PREAMBLE, getName()+uniqueId, getCDescription()));

      if (getDerivedFrom() != null) {
//         writer.print(String.format(DEVICE_SIMPLE_STRUCT, derivedFrom.getSafeHeaderStructName(), getSafeHeaderStructName(), getName()+" Structure"));
         return;
      }
      writeHeaderParameters(writer);
      writeGroupPreamble(writer, getGroupName()+"_"+structGroupSuffix, getGroupName()+" struct", "Struct for "+getGroupName());
      writeHeaderFileStruct(writer, indent);
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
    *    @throws Exception
    */
   public void writeHeaderFileFieldMacros(Writer writer) throws Exception {
      final String macroGroupSuffix  = "Register_Masks";
      if (fDerivedFrom != null) {
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
      for (Cluster register : fRegisters) {
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
      if (fDerivedFrom != null) {
         throw new Exception("Cannot rebase derived peripheral");
      }
      if (fRegisters.size() == 0) {
         return;
      }
      long baseAddress = fRegisters.get(0).getAddressOffset();
      for (Cluster reg : fRegisters) {
         long address = reg.getAddressOffset();
         if (address < baseAddress) {
            baseAddress = address;
         }
      }
      for (Cluster reg : fRegisters) {
         reg.setAddressOffset(reg.getAddressOffset()-baseAddress);
      }
      this.setBaseAddress(baseAddress);
   }

   public void setRefreshAll(boolean value) {
      fRefreshAll = value;
   }

   public boolean isRefreshAll() {
      return fRefreshAll;
   }

   /**
    * Set location of SVD file describing this peripheral
    * 
    * @param string
    * @throws Exception 
    */
   public void setFilename(String string) throws Exception {
      if (getDerivedFrom() != null) {
         throw new Exception("Attempt to set file path for derived peripheral");
      }
      fFilename = string;
   }
   
   /**
    * Get location of SVD file describing this peripheral
    * 
    * @return Path to SVD file
    */
   String getFilename() {
     Peripheral derivedFrom = getDerivedFrom();
      if (derivedFrom != null) {
         return derivedFrom.getFilename();
      }
      return fFilename;
   }

   /**
    * Get relative peripheral path
    * 
    * @param peripheral
    * @return
    * @throws Exception
    */
   String getRelativePath() throws Exception {
      if (getFilename() == null) {
         System.err.println("Filename = null for " + getName());
         System.err.println("Derived  = " + getDerivedFrom());
      }
      return PeripheralDatabaseMerger.PERIPHERAL_FOLDER+"/"+getFilename()+PeripheralDatabaseMerger.XML_EXTENSION;
   }

   /**
    * Set current owner of peripheral<br>
    * Used during header file writing
    * 
    * @param device
    */
   public void setOwner(DevicePeripherals device) {
      fOwner = device;
   }

   /**
    * Sets arbitrary text to add to peripheral C declarations
    *
    * @param text
    */
   public void addTemplate(String text) {
      fTemplate = text;
   }

   /**
    * Gets C template
    * 
    * @return Unprocessed template text to add to C header file
    */
   public String getTemplate() {
      return fTemplate;
   }

   /**
    * Gets extra text to add to peripheral C declarations<br>
    * This text is processed.
    * 
    * @return Text to add to C header file
    */
   public String getExtraDeclarations() {
      if (fTemplate == null) {
         return null;
      }
      String text = fTemplate.replaceAll("%n", getName());
      text = text.replaceAll("\n\\s*", "\n");
      text = text.replaceAll("\\\\t", "   ");
      
      return text;
   }

   /**
    * Add parameter to peripheral
    * 
    * @param parameter
    */
   public void addParameter(Parameter parameter) {
      fParameters.put(parameter.getName(), parameter);
      fSimpleParameterMap = null;
   }

   /**
    * Get parameter from peripheral
    * 
    * @param name Name of parameter to retrieve
    * 
    * @return Parameter or null if not found
    */
   public Parameter getParameter(String name) {
      return fParameters.get(name);
   }
   
   /**
    * Get parameter from peripheral
    * 
    * @param name Name of parameter to retrieve
    * 
    * @return Value of parameter or exception if not found
    */
   public String getParameterValue(String name) {
      return fParameters.get(name).getValue();
   }
   
   /**
    * Get peripheral parameters as a simple map
    * 
    * @return Map of key => values
    */
   Map<String, String> getSimpleParameterMap() {
      if (fSimpleParameterMap == null) {
         fSimpleParameterMap = new HashMap<String, String>();
         for (Entry<String, Parameter> entry:fParameters.entrySet()) {
            fSimpleParameterMap.put(entry.getKey(), entry.getValue().getValue());
         }
      }
      return fSimpleParameterMap;
   }
}
