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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
      ReadWriteOnce  ("readWrite-once",   "RW1", ONCE_MASK| WRITE_MASK| READ_MASK),
      ;
      final String prettyName;
      final String abbreviatedName;
      final int    mask;

      // Used for reverse lookup of AccessType from prettyName
      private static final Map<String,AccessType> lookupPrettyName
         = new HashMap<String,AccessType>();

      // Used for reverse lookup of AccessType from prettyName
      private static final Map<Integer,AccessType> lookupMask
         = new HashMap<Integer,AccessType>();

      static {
         for(AccessType accessType : AccessType.values()) {
            lookupPrettyName.put(accessType.prettyName, accessType);
         }
         for(AccessType accessType : AccessType.values()) {
            lookupMask.put(accessType.mask, accessType);
         }
      }
      
      AccessType(String prettyName, String abbreviatedName, int mask) {
         this.prettyName      = prettyName;
         this.abbreviatedName = abbreviatedName;
         this.mask            = mask;
      }
      
      public String getPrettyName() {
         return prettyName;
      }
      
      public String getAbbreviatedName() {
         return abbreviatedName;
      }
      
      public static AccessType lookup(String prettyName) {
         return lookupPrettyName.get(prettyName);
      }
      
      public AccessType or(AccessType other) {
         return lookupMask.get(this.mask | other.mask);
      }

      public AccessType and(AccessType other) {
         return lookupMask.get(this.mask & other.mask);
      }
      
      public boolean isWriteable() {
         return (this.mask & WRITE_MASK) != 0;
      }
      public boolean isReadable() {
         return (this.mask & READ_MASK) != 0;
      }
   };

   /*
    * ====================================================================
    */
   private AccessType             accessType;
   private long                   bitOffset;
   private long                   bitwidth;
   private Field                  derivedFrom;
   private String                 description;
   private ArrayList<Enumeration> enumerations;
   private String                 name;
   private boolean                ignoreOverlap;
   private boolean                hidden;
   private final Register         owner;

   /*
    * Constructor
    */
   public Field(Register owner) {
      accessType    = null;
      bitOffset     = 0;
      bitwidth      = 0;
      description   = "";
      derivedFrom   = null;
      enumerations  = new ArrayList<Enumeration>();
      name          = "";
      ignoreOverlap = false;
      this.owner    = owner;
      if (owner != null) {
         bitwidth       = owner.getWidth();
         accessType     = owner.getAccessType();
      }
      else {
         bitwidth       =  32;
         accessType     =  AccessType.ReadWrite;
      }
   }

   /*
    * SHALLOW Copy constructor
    */
   public Field(Field other) {
      accessType   = other.accessType;    
      bitOffset    = other.bitOffset;     
      bitwidth     = other.bitwidth;      
      description  = other.description;
      derivedFrom  = other;
      enumerations = other.enumerations;  
      name         = other.name;          
      owner        = other.owner;
   }

   public Field getDerivedFrom() {
      return derivedFrom;
   }

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return String.format("Field[%s]", getName());
   }

   public String getName() {
      return name;
   }

   public String getName(int index) {
      return owner.format(name, index);
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }
   
   public String getCDescription(int index) {
      return SVD_XML_BaseParser.unEscapeString(owner.format(description, index));
   }
   
   public String getCDescription(int clusterIndex, int registerIndex) {
      return SVD_XML_BaseParser.unEscapeString(owner.format(description, clusterIndex, registerIndex));
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = getSanitizedDescription(description.trim());
   }

   public long getBitOffset() {
      return bitOffset;
   }

   public void setBitOffset(long bitOffset) {
      this.bitOffset = bitOffset;
   }

   public long getBitwidth() {
      return bitwidth;
   }

   public void setBitwidth(long bitwidth) throws Exception {
      if (bitwidth == 0) {
         throw new Exception("Illegal width");
      }
      this.bitwidth = bitwidth;
   }

   public AccessType getAccessType() {
      return accessType;
   }

   public void setAccessType(AccessType accessType) {
      this.accessType = accessType;
   }
   
   public boolean isIgnoreOverlap() {
      return ignoreOverlap;
   }

   public void setIgnoreOverlap(boolean ignoreOverlap) {
      this.ignoreOverlap = ignoreOverlap;
   }

   public boolean isHidden() {
      return hidden;
   }

   public void setHidden(boolean hide) {
      this.hidden = hide;
   }

   public ArrayList<Enumeration> getEnumerations() {
      return enumerations;
   }

   public void addEnumeration(Enumeration enumeration) throws Exception {
      if (derivedFrom != null) {
         throw new Exception("Cannot change enumerations of a derived Field");
      }
      this.enumerations.add(enumeration);
   }

   /** Determines if two fields are equivalent
    * 
    * @param other      Other enumeration to check
    * @param pattern1   Pattern to apply to name & description of self
    * @param pattern2   Pattern to apply to name & description of other
    * 
    * @note Patterns are applied recursively to enumerations etc.
    * 
    * @return true if equivalent
    */
   public boolean equivalent(Field other, String pattern1, String pattern2) {
      boolean verbose = false; //name.equalsIgnoreCase("TFWM1") && other.getName().equalsIgnoreCase("TFWM1");
      boolean rv =  
            (this.accessType == other.accessType) &&
            (this.bitOffset == other.bitOffset) &&
            (this.bitwidth == other.bitwidth);
      if (!rv) {
         if (verbose) {
            System.err.println("Comparing simple field structure \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      if (!getName().equalsIgnoreCase(other.getName())) {
         // Try after pattern
         String n1 = getName().replaceFirst(pattern1, "$1%s$3");
         String n2 = other.getName().replaceFirst(pattern2, "$1%s$3");
         if (!n1.equalsIgnoreCase(n2)) {
            return false;
         }
      }
      for(Enumeration enumeration : enumerations) {
         boolean foundEquivalent = false;
         for(Enumeration otherEnumeration : other.enumerations) {
            if (enumeration.equivalent(otherEnumeration, pattern1, pattern2)) {
               foundEquivalent = true;
               break;
            }
         }
         if (!foundEquivalent) {
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
      boolean verbose = false; //name.equalsIgnoreCase("TFWM1") && other.getName().equalsIgnoreCase("TFWM1");
      boolean rv =  this.name.equals(other.name) && equivalent(other, null, null);
      if (!rv) {
         if (verbose) {
            System.err.println("Comparing simple field structure \""+getName()+"\", \""+other.getName()+"\"=> false");
         }
         return false;
      }
      for(Enumeration enumeration : enumerations) {
         boolean foundEquivalent = false;
         for(Enumeration otherEnumeration : other.enumerations) {
            if (enumeration.getName().equals(otherEnumeration.getName())) {
               foundEquivalent = enumeration.equivalent(otherEnumeration);
               if (foundEquivalent) {
                  break;
               }
            }
         }
         if (!foundEquivalent) {
            return false;
         }
      }
      return true;
   }
   
   public void report() {
      System.out.println(String.format("          Field \"%s\" [%d-%d], Description = \"%s\" : " + accessType.toString(), 
            getName(), getBitOffset(), getBitOffset()+getBitwidth()-1, getDescription()));
      
      for(Enumeration enumeration : enumerations) {
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
      if (derivedFrom!=null) {
         writer.write(String.format(indenter+"<field derivedFrom=\"%s\" >", derivedFrom.getName()));
         if (!derivedFrom.getName().equals(getName()) && (getName().length()>0)) {
            writer.write(String.format(" <name>%s</name>",               SVD_XML_BaseParser.escapeString(getName())));
         }
         if (!derivedFrom.getDescription().equals(getDescription()) && (getDescription().length()>0)) {
            writer.write(String.format(" <description>%s</description>", SVD_XML_BaseParser.escapeString(getDescription())));
         }
         if (derivedFrom.getBitOffset() != getBitOffset()) {
            writer.write(String.format(" <bitOffset>%d</bitOffset>",     getBitOffset()));
         }
         if (derivedFrom.getBitwidth() != getBitwidth()) {
            writer.write(String.format(" <bitWidth>%d</bitWidth>",       getBitwidth()));
         }
         if (derivedFrom.getAccessType() != getAccessType()) {
            writer.write(String.format(" <access>%s</access>",           getAccessType().getPrettyName()));
         }
         writer.write(" </field>\n");
      }
      else {
         writer.write(                 indenter+"<field>\n");
         writer.write(String.format(   indenter+"   <name>%s</name>\n",               SVD_XML_BaseParser.escapeString(getName())));
         if (isIgnoreOverlap()) {
            writer.write(              indenter+"   <?"+SVD_XML_Parser.IGNOREOVERLAP_ATTRIB+"?>\n");
         }
         if (isHidden()) {
            writer.write(              indenter+"   <?"+SVD_XML_Parser.HIDE_ATTRIB+"?>\n");
         }
         if (getDescription().length() > 0) {
            writer.write(String.format(indenter+"   <description>%s</description>\n", SVD_XML_BaseParser.escapeString(getDescription())));
         }
         writer.write(String.format(   indenter+"   <bitOffset>%d</bitOffset>\n",     getBitOffset()));
         if ((owner == null) || standardFormat || (owner.getWidth() != getBitwidth())) {
            writer.write(String.format(indenter+"   <bitWidth>%d</bitWidth>\n",       getBitwidth()));
         }
         if ((owner == null) || standardFormat || (owner.getAccessType() != getAccessType())) {
            writer.write(String.format(indenter+"   <access>%s</access>\n",           getAccessType().getPrettyName()));
         }
         if ((getEnumerations() != null) && (!getEnumerations().isEmpty())) {
            writer.write(              indenter+"   <enumeratedValues>\n");
            for (Enumeration enumeration : getEnumerations()) {
               enumeration.writeSVD(writer, standardFormat, indent+6);
            }
            writer.write(              indenter+"   </enumeratedValues>\n");
         }
         writer.write(                 indenter+"</field>\n");
      }
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
    * Maps a bit-field macro name for shared definitions (made generic)
    * e.g. PORTA_PCR_MUX() -> PORT_PCR_MUX()
    * 
    * @param   Name to map
    * @return  Mapped name (unchanged if not mapped, null if to be deleted)
    */
   static String getMappedBitfieldMacroName(String name) {
      // TODO - Common names in macros are done here
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
         mappedMacros.add(new Pair(Pattern.compile("^(DAC)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DMA)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DMAMUX)[0-9](_.*)$"),                   "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(GPIO_.*)(?:AN|AS|DD|GP|LD|NQ|QS|TA|TC|TD|TE|TF|TG|TH|TI|TJ|UA|UB|UC)(_.*)$"),  "$1$2")); // GPIO_PORTNQ_PORT_MASK => GPIO_PORT_PORT_MASK
         mappedMacros.add(new Pair(Pattern.compile("^F?(GPIO)[A-Z](_.*)$"),                    null));    // Delete useless pin macros 
         mappedMacros.add(new Pair(Pattern.compile("^(FTM)[0-9](_.*)$"),                      "$1$2"));
//         mappedMacros.add(new Pair(Pattern.compile("^(FMC)[0-9]?_S_(.*)$"),                   "$1_TAGVD_$2"));  // FMC_S_valid_SHIFT -> FMC_TAGVD_valid_SHIFT
//         mappedMacros.add(new Pair(Pattern.compile("^(FMC_S)[0-9]*(.*)$"),                    "$1$2"));  // Fold cache ways
         mappedMacros.add(new Pair(Pattern.compile("^(I2C)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(I2S)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(OSC)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(LPTMR)[0-9](_.*)$"),                    "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(PDB)[A-Z](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(PDB)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^PORT[A-Z]_(ISFR|DFER)_[A-Z]*[0-9]*.$"),   null));
         mappedMacros.add(new Pair(Pattern.compile("^(PORT)[A-Z](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(PCTL)[A-Z](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(SPI)[0-9](_CTAR)[0-9](.*)$"),           "$1$2$3")); // e.g SPI0_CTAR0_SLAVE_FMSZ_MASK => SPI_CTAR_SLAVE_FMSZ_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(SPI)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(USB)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(TPM)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(TSI)[0-9](_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(UART)[0-9](_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(LPUART)[0-9](_.*)$"),                   "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(INTC)[0-9](_.*)$"),                     "$1$2")); // INTC0_INTFRCH_FRCH51_MASK => INTC_INTFRCH_FRCH51_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(SIM_OSC1)_CNTRL(.*)$"),                 "$1$2")); // INTC0_INTFRCH_FRCH51_MASK => INTC_INTFRCH_FRCH51_MASK
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

   static final String BITFIELD_MACRO_POS_FORMAT     = "#define %-40s %d";
   static final String BITFIELD_MACRO_MSK_NUM_FORMAT = "#define %-40s 0x%Xu";
   static final String BITFIELD_MACRO_MSK_FORMAT     = "#define %-40s (0x%02XUL << %s)";
//   static final String BITFIELD_MACRO_MSK_FORMAT   = "#define %-40s 0x%Xu";
//   static final String BITFIELD_MACRO_FIELD_FORMAT   = "#define %-40s (((uint32_t)(((uint32_t)(x))<<%s))&%s)";
   static final String BITFIELD_MACRO_FIELD_FORMAT   = "#define %-40s (((%s)(((%s)(x))<<%s))&%s)";
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
    * Writes a set of macros to allow convenient access to the register field
    * e.g. "#define PERIPHERAL_FIELD(x)  (((x)<<FIELD_OFFSET)&FIELD_MASK)"
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
      
      if (isUseShiftsInFieldMacros()) {
         writer.write(String.format("%-100s%s",
               String.format(BITFIELD_MACRO_MSK_FORMAT, mskName, ((1L<<getBitwidth())-1), posName), 
//               String.format(BitfieldMacroMskFormat, mskName, ((1L<<getBitwidth())-1)<<getBitOffset()), 
               String.format(BITFIELD_FORMAT_COMMENT,  baseName+": "+getBaseName()+" Mask")));
      }
      else {
         writer.write(String.format("%-100s%s",
               String.format(BITFIELD_MACRO_MSK_NUM_FORMAT, mskName, ((1L<<getBitwidth())-1)<<getBitOffset()), 
//               String.format(BitfieldMacroMskFormat, mskName, ((1L<<getBitwidth())-1)<<getBitOffset()), 
               String.format(BITFIELD_FORMAT_COMMENT,  baseName+": "+getBaseName()+" Mask")));
      }
      writer.write(String.format("%-100s%s",
            String.format(BITFIELD_MACRO_POS_FORMAT, posName, getBitOffset()),      
            String.format(BITFIELD_FORMAT_COMMENT,  baseName+": "+getBaseName()+" Position")));      

      if (getBitwidth()>1) {
         String width = getCWidth(this.owner.getWidth());
         writer.write(String.format("%-100s%s",
               String.format(BITFIELD_MACRO_FIELD_FORMAT, fieldname+"(x)", width, width, posName, mskName), 
               String.format(BITFIELD_FORMAT_COMMENT,    baseName, getBaseName()+" Field"))); 
//         writer.write(String.format("%-100s%s",
//            String.format(BITFIELD_MACRO_FIELD_FORMAT, fieldname+"(x)", posName, mskName), 
//            String.format(BITFIELD_FORMAT_COMMENT,    baseName, getBaseName()+" Field"))); 
//         String.format(BitfieldFormatComment,    baseName+": "+getBaseName()+" Field"))); 
      }
   }

}
