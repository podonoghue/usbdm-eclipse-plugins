package net.sourceforge.usbdm.peripheralDatabase;
/*
 Change History
+===================================================================================
| Revision History
+===================================================================================
| 19 Jan 15 | Some name changes to avoid MACRO clashes                    4.10.6.250
+===================================================================================
 */

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* 
 * How to do a hidden derived field
 * <field derivedFrom="VLLSM" > <name>LLSM</name> <?ignoreOverlap?> <?hide?> </field>
 */
public class Field extends ModeControl implements Cloneable {
   static final int READ_MASK  = (1<<1); 
   static final int WRITE_MASK = (1<<2); 
   static final int ONCE_MASK  = (1<<3); 

   public enum AccessType {
      //                                       multiple write   read
      ReadOnly       ("read-only",        "RO",                         READ_MASK),
      WriteOnly      ("write-only",       "WO",             WRITE_MASK           ),
      ReadWrite      ("read-write",       "RW",             WRITE_MASK| READ_MASK),
      WriteOnce      ("write-once",       "W1",  ONCE_MASK| WRITE_MASK           ),
      ReadWriteOnce  ("read-write-once",  "RW1", ONCE_MASK| WRITE_MASK| READ_MASK),
      ;
      final String fPrettyName;
      final String fAbbreviatedName;
      final int    fMask;

      // Used for reverse lookup of AccessType from prettyName
      private static final Map<String,AccessType> lookupPrettyName
         = new HashMap<String,AccessType>();

      // Used for reverse lookup of AccessType from prettyName
      private static final Map<Integer,AccessType> lookupMask
         = new HashMap<Integer,AccessType>();

      static {
         for(AccessType accessType : AccessType.values()) {
            lookupPrettyName.put(accessType.fPrettyName, accessType);
         }
         for(AccessType accessType : AccessType.values()) {
            lookupMask.put(accessType.fMask, accessType);
         }
      }
      
      AccessType(String prettyName, String abbreviatedName, int mask) {
         fPrettyName      = prettyName;
         fAbbreviatedName = abbreviatedName;
         fMask            = mask;
      }
      
      public String getPrettyName() {
         return fPrettyName;
      }
      
      public String getAbbreviatedName() {
         return fAbbreviatedName;
      }
      
      public static AccessType lookup(String prettyName) {
         return lookupPrettyName.get(prettyName);
      }
      
      public AccessType or(AccessType other) {
         return lookupMask.get(fMask | other.fMask);
      }

      public AccessType and(AccessType other) {
         return lookupMask.get(fMask & other.fMask);
      }
      
      public boolean isWriteable() {
         return (fMask & WRITE_MASK) != 0;
      }
      public boolean isReadable() {
         return (fMask & READ_MASK) != 0;
      }
   };

   /*
    * ====================================================================
    */
   private AccessType                   fAccessType;
   private String                       fBitOffsetText = null;
   private long                         fBitOffset;
   private String                       fBitwidthText = null;
   private long                         fBitwidth;
   private Field                        fDerivedFrom;
   private String                       fDescription;
   private ArrayList<Enumeration>       fEnumerations;
   private TreeMap<String, Enumeration> fSortedEnumerations;
   private String                       fName;
   private boolean                      fIgnoreOverlap;
   private boolean                      fHidden;
   private final Register               fOwner;

   /*
    * Constructor
    */
   public Field(Register owner) {
      fAccessType          = null;
      fBitOffset           = 0;
      fBitwidth            = 0;
      fDescription         = "";
      fDerivedFrom         = null;
      fEnumerations        = new ArrayList<Enumeration>();
      fSortedEnumerations  = new TreeMap<String, Enumeration>();
      fName                = "";
      fIgnoreOverlap       = false;
      fOwner               = owner;
      if (owner != null) {
         fBitwidth       = owner.getWidth();
         fAccessType     = owner.getAccessType();
      }
      else {
         fBitwidth       =  32;
         fAccessType     =  AccessType.ReadWrite;
      }
   }

   /*
    * SHALLOW Copy constructor for never 'derivedFrom' elements
    */
   public Field(Field other) {
      fDerivedFrom        = other;
      fAccessType         = other.fAccessType;    
      fBitOffsetText      = other.fBitOffsetText;     
      fBitOffset          = other.fBitOffset;     
      fBitwidthText       = other.fBitwidthText;      
      fBitwidth           = other.fBitwidth;      
      fDescription        = other.fDescription;
      fSortedEnumerations = other.fSortedEnumerations;
      fEnumerations       = other.fEnumerations;  
      fName               = other.fName;          
      fOwner              = other.fOwner;
   }

   public Field getDerivedFrom() {
      return fDerivedFrom;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return String.format("Field[%-10s:o=%d:w=%d]", getName(), getBitOffset(), getBitwidth());
   }

   public String getName() {
      return fName;
   }

   public String getName(int index) {
      return fOwner.format(fName, index);
   }

   public void setName(String name) {
      fName = name;
   }

   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }
   
   public String getCDescription(int index) {
      return SVD_XML_BaseParser.unEscapeString(fOwner.format(fDescription, index));
   }
   
   public String getCDescription(int clusterIndex, int registerIndex) {
      return SVD_XML_BaseParser.unEscapeString(fOwner.format(fDescription, clusterIndex, registerIndex));
   }

   public String getDescription() {
      return fDescription;
   }

   public void setDescription(String description) {
      fDescription = getSanitizedDescription(description.trim());
   }

   public void setBitOffset(long bitOffset) {
      if (fBitOffsetText == null) {
         fBitOffsetText = Long.toString(bitOffset);
      }
      fBitOffset = bitOffset;
   }

   public void setBitOffsetText(String text) {
      fBitOffsetText = text;
   }

   public String getBitOffsetText() {
      return fBitOffsetText;
   }

   public long getBitOffset() {
      return fBitOffset;
   }

   public void setBitwidth(long bitwidth) throws Exception {
      if (bitwidth == 0) {
         throw new Exception("Illegal width");
      }
      if (fBitwidthText == null) {
         fBitwidthText = Long.toString(bitwidth);
      }
      fBitwidth = bitwidth;
   }

   public void setBitWidthText(String text) {
      fBitwidthText = text;
   }

   public String getBitwidthText() {
      return fBitwidthText;
   }

   public long getBitwidth() {
      return fBitwidth;
   }

   public AccessType getAccessType() {
      return fAccessType;
   }

   public void setAccessType(AccessType accessType) {
      fAccessType = accessType;
   }
   
   public boolean isIgnoreOverlap() {
      return fIgnoreOverlap;
   }

   public void setIgnoreOverlap(boolean ignoreOverlap) {
      fIgnoreOverlap = ignoreOverlap;
   }

   public boolean isHidden() {
      return fHidden;
   }

   public void setHidden(boolean hide) {
      fHidden = hide;
   }

   public ArrayList<Enumeration> getEnumerations() {
      return fEnumerations;
   }

   public void addEnumeration(Enumeration enumeration) throws Exception {
      if (fDerivedFrom != null) {
         throw new Exception("Cannot change enumerations of a derived Field");
      }
      fSortedEnumerations.put(enumeration.getName(), enumeration);
      fEnumerations.add(enumeration);
   }

   /** Determines if two fields are equivalent
    * 
    * @param other      Other enumeration to check
    * @param pattern1   Pattern to apply to name & description of self  "(prefix)(index)(suffix)"
    * @param pattern2   Pattern to apply to name & description of other "(prefix)(index)(suffix)"
    * 
    * @note Patterns are applied recursively to enumerations etc.
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Field other, String pattern1, String pattern2) {
      boolean verbose = false;
//      verbose = getName().equalsIgnoreCase("WUPE0") && other.getName().equalsIgnoreCase("WUPE0");
      boolean rv =  
            (fBitOffset == other.fBitOffset) &&
            (fBitwidth == other.fBitwidth);
      if (!isIgnoreAccessTypeInEquivalence()) {
         rv = rv &&
               (fAccessType == other.fAccessType);
      }
      if (!rv) {
         if (verbose) {
            System.err.println("Comparing simple field structure \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      if (!getName().equalsIgnoreCase(other.getName())) {
         if ((pattern1 == null) || (pattern2 == null)) {
            return false;
         }
         // Try after pattern
         String n1 = getName().replaceFirst(pattern1, "$1%s$3");
         String n2 = other.getName().replaceFirst(pattern2, "$1%s$3");
         if (!n1.equalsIgnoreCase(n2)) {
            return false;
         }
      }
      Iterator<Entry<String, Enumeration>> enumerationIt      = fSortedEnumerations.entrySet().iterator();
      Iterator<Entry<String, Enumeration>> otherEnumerationIt = other.fSortedEnumerations.entrySet().iterator();
      
      while (enumerationIt.hasNext() || otherEnumerationIt.hasNext()) {
         if (!enumerationIt.hasNext() || !otherEnumerationIt.hasNext()) {
            // Unbalanced
            return false;
         }
         Entry<String, Enumeration> enumeration      = enumerationIt.next();
         Entry<String, Enumeration> otherEnumeration = otherEnumerationIt.next();
         if (!enumeration.getValue().equivalent(otherEnumeration.getValue(), pattern1, pattern2)) {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Check if two fields have equivalent structure
    * 
    * @param other
    * @return
    */
   public boolean equivalent(Field other) {
      boolean verbose = false;
//      verbose = getName().equalsIgnoreCase("DLLSB") && other.getName().equalsIgnoreCase("DLLSB");
      boolean rv =  
            fName.equals(other.fName) && 
            fDescription.equalsIgnoreCase(other.fDescription) &&
            equivalent(other, null, null);
      if (!rv) {
         if (verbose) {
            System.err.println("Comparing simple field structure \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      Iterator<Entry<String, Enumeration>> enumerationIt      = fSortedEnumerations.entrySet().iterator();
      Iterator<Entry<String, Enumeration>> otherEnumerationIt = other.fSortedEnumerations.entrySet().iterator();
      
      while (enumerationIt.hasNext() || otherEnumerationIt.hasNext()) {
         if (!enumerationIt.hasNext() || !otherEnumerationIt.hasNext()) {
            // Unbalanced
            return false;
         }
         Entry<String, Enumeration> enumeration      = enumerationIt.next();
         Entry<String, Enumeration> otherEnumeration = otherEnumerationIt.next();
         if (!enumeration.getValue().equivalent(otherEnumeration.getValue())) {
            return false;
         }
      }
      return true;
   }
   
   public void report() {
      System.out.println(String.format("          Field \"%s\" [%d-%d], Description = \"%s\" : " + fAccessType.toString(), 
            getName(), getBitOffset(), getBitOffset()+getBitwidth()-1, getDescription()));
      
      for(Enumeration enumeration : fEnumerations) {
         enumeration.report();
      }
   }

   /**
    *   Writes the Register description to file in a SVF format
    *   
    *  @param writer          The destination for the XML
    *  @param standardFormat  Suppresses some non-standard size optimisations 
    *  @param owner           The owner - This is used to reduce the size by inheriting default values
    * @throws IOException 
    */
   public void writeSVD(Writer writer, boolean standardFormat, Register owner, int indent) throws IOException {
      final String indenter = RegisterUnion.getIndent(indent);
      if (fDerivedFrom != null) {
         writer.write(String.format(indenter+"<field derivedFrom=\"%s\" >", fDerivedFrom.getName()));
         if (!fDerivedFrom.getName().equals(getName()) && (getName().length()>0)) {
            writer.write(String.format(" <name>%s</name>",               SVD_XML_BaseParser.escapeString(getName())));
         }
         if (!fDerivedFrom.getDescription().equals(getDescription()) && (getDescription().length()>0)) {
            writer.write(String.format(" <description>%s</description>", SVD_XML_BaseParser.escapeString(getDescription())));
         }
         if (fDerivedFrom.getBitOffsetText() != getBitOffsetText()) {
            writer.write(String.format(" <bitOffset>%s</bitOffset>",     getBitOffsetText()));
         }
         if (fDerivedFrom.getBitwidthText() != getBitwidthText()) {
            writer.write(String.format(" <bitWidth>%s</bitWidth>",       getBitwidthText()));
         }
         if (fDerivedFrom.getAccessType() != getAccessType()) {
            writer.write(String.format(" <access>%s</access>",           getAccessType().getPrettyName()));
         }
         if (isIgnoreOverlap()) {
            writer.write(" <?"+SVD_XML_Parser.IGNOREOVERLAP_PROCESSING+"?>");
         }
         if (isHidden()) {
            writer.write(" <?"+SVD_XML_Parser.HIDE_PROCESSING+"?>");
         }
         writer.write(" </field>\n");
      }
      else {
         writer.write(                 indenter+"<field>\n");
         writer.write(String.format(   indenter+"   <name>%s</name>\n",               SVD_XML_BaseParser.escapeString(getName())));
         if (isIgnoreOverlap()) {
            writer.write(              indenter+"   <?"+SVD_XML_Parser.IGNOREOVERLAP_PROCESSING+"?>\n");
         }
         if (isHidden()) {
            writer.write(              indenter+"   <?"+SVD_XML_Parser.HIDE_PROCESSING+"?>\n");
         }
         if (getDescription().length() > 0) {
            writer.write(String.format(indenter+"   <description>%s</description>\n", SVD_XML_BaseParser.escapeString(getDescription())));
         }
         writer.write(String.format(   indenter+"   <bitOffset>%s</bitOffset>\n",     getBitOffsetText()));
         if (getBitwidthText() != null) {
            if ((owner == null) || standardFormat || (Long.toString(owner.getWidth()) != getBitwidthText())) {
               writer.write(String.format(indenter+"   <bitWidth>%s</bitWidth>\n",  getBitwidthText()));
            }
         }
         if ((owner == null) || standardFormat || (owner.getAccessType() != getAccessType())) {
            writer.write(String.format(indenter+"   <access>%s</access>\n",           getAccessType().getPrettyName()));
         }
         if ((getEnumerations() != null) && (!getEnumerations().isEmpty())) {
            writer.write(              indenter+"   <enumeratedValues>\n");
            for(Enumeration enumeration : fEnumerations) {
               enumeration.writeSVD(writer, standardFormat, indent+6);
            }
            writer.write(              indenter+"   </enumeratedValues>\n");
         }
         writer.write(                 indenter+"</field>\n");
      }
   }

   public static class Pair {
      public Pattern regex;
      public String  replacement;
      
      public Pair(Pattern regex, String replacement) {
         this.regex      = regex;
         this.replacement = replacement;
      }
   }
   
   /**
    * Maps a bit-field macro name for shared definitions (make generic)
    * e.g. PORTA_PCR_MUX() -> PORT_PCR_MUX()
    * 
    * @param   Name to map
    * @return  Mapped name (unchanged if not mapped, null if to be deleted)
    */
   static String getMappedBitfieldMacroName(String name) {
      // TODO Where common names in macros are converted
      final ArrayList<Pair> mappedMacros = new ArrayList<Pair>();

      if (mappedMacros.size() == 0) {
         // Prevent multiple definitions of bit fields that are common to multiple instances of a device e.g. ADC0, ADC1 etc
         // Fields are masked to a root name e.g. GPIOA_PDOR_PDO_MASK => GPIO_PDOR_PDO_MASK
         // Fields can also be deleted by mapping to null
         mappedMacros.add(new Pair(Pattern.compile("^(ADC)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(ACMP)[0-9](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(AIPS)[0-9](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(CAN)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(CMP)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(CRC)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DAC)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DMA)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DMAMUX)[0-9](_.*)$"),                   "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(GPIO_.*)(?:AN|AS|DD|GP|LD|NQ|QS|TA|TC|TD|TE|TF|TG|TH|TI|TJ|UA|UB|UC)(_.*)$"),  "$1$2")); // GPIO_PORTNQ_PORT_MASK => GPIO_PORT_PORT_MASK
         mappedMacros.add(new Pair(Pattern.compile("^F?(GPIO)[A-Z](_.*)$"),                    null));    // Delete useless pin macros 
         mappedMacros.add(new Pair(Pattern.compile("^(FTM)[0-9](_.*)$"),                      "$1$2"));
//         mappedMacros.add(new Pair(Pattern.compile("^(FMC)[0-9]?_S_(.*)$"),                   "$1_TAGVD_$2"));  // FMC_S_valid_SHIFT -> FMC_TAGVD_valid_SHIFT
//         mappedMacros.add(new Pair(Pattern.compile("^(FMC_S)[0-9]*(.*)$"),                    "$1$2"));  // Fold cache ways
         mappedMacros.add(new Pair(Pattern.compile("^((?:LP)?I2C)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^((?:LP)?I2S)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(OSC)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(LPTMR)[0-9](_.*)$"),                    "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(PDB)[A-Z](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(PDB)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^PORT[A-Z]_(ISFR|DFER)_[A-Z]*[0-9]*.$"),   null));
         mappedMacros.add(new Pair(Pattern.compile("^(PORT)[A-Z](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(PCTL)[A-Z](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(QSPI)\\d?(_BUF)\\d(CR_(MSTRID|ADATSZ).*)$"),  "$1$2$3"));   // QSPI_BUF0CR_ADATSZ => QSPI_BUFCR_ADATSZ
         mappedMacros.add(new Pair(Pattern.compile("^(QSPI)\\d?(_BUF)\\d(.*)\\d(.*)$"),      "$1$2$3$4"));    // QSPI_BUF0CR_ADATSZ => QSPI_BUFCR_ADATSZ
         mappedMacros.add(new Pair(Pattern.compile("^(QSPI)\\d?(_SF)(A1|A2|B1|B2)(.*)(A1|A2|B1|B2)(.*)$"), "$1$2$4$6"));    // QSPI_SFA1AD_TPADA1_MASK => QSPI_SFAD_TPAD_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(SPI)[0-9](_CTAR)[0-9](.*)$"),           "$1$2$3")); // e.g SPI0_CTAR0_SLAVE_FMSZ_MASK => SPI_CTAR_SLAVE_FMSZ_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(SDHC)[0-9](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(L?PIT)[0-9](_.*)$"),                    "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^((?:LP)?SPI)[0-9](_.*)$"),                "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(QSPI)[0-9](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(USB)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(USBHS)[0-9](_.*)$"),                    "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(USBDCD)[0-9](_.*)$"),                   "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(TPM)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(TSI)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(TRNG)[0-9](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^((?:LP)?UART)[0-9](_.*)$"),                   "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(INTC)[0-9](_.*)$"),                     "$1$2")); // INTC0_INTFRCH_FRCH51_MASK => INTC_INTFRCH_FRCH51_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(SIM_OSC1)_CNTRL(.*)$"),                 "$1$2")); 
         mappedMacros.add(new Pair(Pattern.compile("^(EMVSIM)\\d(.*)$"),                      "$1$2")); // EMVSIM0_VER_ID_VER_MASK => EMVSIM_VER_ID_VER_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(FLEXIO)\\d(.*)$"),                      "$1$2")); // FLEXIO0_VERID_FEATURE_MASK => FLEXIO_VERID_FEATURE_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(INTMUX)\\d(.*)$"),                      "$1$2")); // INTMUX0_CSR_RST_MASK => INTMUX_CSR_RST_MASK
         
         mappedMacros.add(new Pair(Pattern.compile("^(LTC)\\d(.*)$"),                         "$1$2")); // LTC0_MD_ENC_MASK => LTC_MD_ENC_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(CAU_.*?)\\d*$"),                        "$1")); // CAU_DIRECT_CAU_DIRECT0 => CAU_DIRECT_CAU_DIRECT
      }
      for (Pair p : mappedMacros) {
         Matcher matcher = p.regex.matcher(name);
         if (matcher.matches()) {
            if (p.replacement == null) {
               return null;
            }
            return matcher.replaceAll(p.replacement);
         }
      }
      return name;
   }

   static final String BITFIELD_MACRO_POS_FORMAT        = "#define %-40s (%dU)";
   static final String BITFIELD_MACRO_MSK_NUM_FORMAT    = "#define %-40s (0x%XU)";
   static final String BITFIELD_MACRO_MSK_FORMAT        = "#define %-40s (0x%02XUL << %s)";
   static final String BITFIELD_MACRO_FIELD_FORMAT      = "#define %-40s (((%s)(((%s)(x))<<%s))&%s)";
   static final String BITFIELD_MACRO_FIELD_NUM_FORMAT  = "#define %-40s (((%s)(((%s)(x))<<%dU))&0x%XUL)";
   static final String BITFIELD_FORMAT_COMMENT       = " /*!< %-40s*/\n";

   String getBaseName() {
      return getName().replaceAll("%s", "n");
   }
   
   private String getCWidth(long width) {
      if (width<=8) {
         return "uint8_t";
      }
      if (width<=16) {
         return "uint16_t";
      }
      return "uint32_t";
   }
   
   /**
    * Writes a set of macros to allow convenient access to the register field<br>
    * e.g. "#define PERIPHERAL_FIELD(x)  (((x)&lt;&lt;FIELD_OFFSET)&FIELD_MASK)"
    * 
    * @param  writer    Where to write 
    * @param  baseName  Basename of the peripheral
    * @throws Exception 
    */
   public void writeHeaderFileFieldMacros(Writer writer, String baseName) throws Exception {
      String fieldname = baseName+"_"+getBaseName();
      // Filter names
      fieldname = getMappedBitfieldMacroName(fieldname);
      if (fieldname == null) {
         return;
      }
      if (fieldMacroAlreadyDone(this, fieldname)) {
         return;
      }
      String posName   = fieldname+getFieldOffsetSuffixName();
      String mskName   = fieldname+getFieldMaskSuffixName();
      int    mask      = (int) (((1L<<getBitwidth())-1)<<getBitOffset());
      
      if (isUseShiftsInFieldMacros()) {
         // Write "#define XXX_YY_MASK (0xXXXXUL << XXX_YY_SHIFT)"
         writer.write(String.format("%-100s%s",
               String.format(BITFIELD_MACRO_MSK_FORMAT, mskName, ((1L<<getBitwidth())-1), posName), // Value 
               String.format(BITFIELD_FORMAT_COMMENT,  baseName+"."+getBaseName()+" Mask")));       // Comment
      }
      else {
         // Write "#define XXX_YY_MASK (0xXXXXU)"
         writer.write(String.format("%-100s%s",
               String.format(BITFIELD_MACRO_MSK_NUM_FORMAT, mskName, mask),  // Value 
               String.format(BITFIELD_FORMAT_COMMENT,  baseName+"."+getBaseName()+" Mask")));                   // Comment
      }
      // Write "#define XXX_YY_SHIFT (XXXXU)"
      writer.write(String.format("%-100s%s",
            String.format(BITFIELD_MACRO_POS_FORMAT, posName, getBitOffset()),                 // Value
            String.format(BITFIELD_FORMAT_COMMENT,  baseName+"."+getBaseName()+" Position"))); // Comment

      String width = getCWidth(fOwner.getWidth());
      if (isUseShiftsInFieldMacros()) {
         writer.write(String.format("%-100s%s",
               String.format(BITFIELD_MACRO_FIELD_FORMAT, fieldname+"(x)", width, width, posName, mskName), 
               String.format(BITFIELD_FORMAT_COMMENT,    baseName+"."+getBaseName()+" Field"))); 
      }
      else {
         writer.write(String.format("%-100s%s",
               String.format(BITFIELD_MACRO_FIELD_NUM_FORMAT, fieldname+"(x)", width, width, getBitOffset(), mask), 
               String.format(BITFIELD_FORMAT_COMMENT,    baseName+"."+getBaseName()+" Field"))); 
      }
//         writer.write(String.format("%-100s%s",
//            String.format(BITFIELD_MACRO_FIELD_FORMAT, fieldname+"(x)", posName, mskName), 
//            String.format(BITFIELD_FORMAT_COMMENT,    baseName, getBaseName()+" Field"))); 
//         String.format(BitfieldFormatComment,    baseName+": "+getBaseName()+" Field"))); 
   }

   public void setDerivedFrom(Field oField) {
      fDerivedFrom = oField;
      fEnumerations.clear();
   }

   public TreeMap<String, Enumeration> getSortedEnumerations() {
      return fSortedEnumerations;
   }

}
