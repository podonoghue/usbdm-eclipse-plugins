package net.sourceforge.usbdm.peripheralDatabase;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Field extends ModeControl implements Cloneable {

   public enum AccessType {
      ReadOnly       ("read-only",        "RO",  (1<<4)       |(1<<1)),
      WriteOnly      ("write-only",       "WO",  (1<<4)|(1<<2)       ),
      ReadWrite      ("read-write",       "RW",  (1<<4)|(1<<2)|(1<<1)),
      WriteOnce      ("write-once",       "W1",         (1<<2)       ),
      ReadWriteOnce  ("readWrite-once",   "RW1",        (1<<2)|(1<<1)),
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
   };
   
   private String                 name;
   private String                 description;
   private long                   bitOffset;
   private long                   bitwidth;
   private AccessType             accessType;
   private ArrayList<Enumeration> enumerations;
   private final Register         owner;
   
   public Field(Register owner) {
      name         = "";
      description  = "";
      bitOffset    = 0;
      bitwidth     = 0;
      accessType   = null;
      enumerations = new ArrayList<Enumeration>();
      this.owner   = owner;
      if (owner != null) {
         bitwidth       = owner.getWidth();
         accessType     = owner.getAccessType();
      }
      else {
         bitwidth       =  32;
         accessType     =  AccessType.ReadWrite;
      }
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

   public String getName(int index) throws Exception {
      return owner.format(name, index);
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public String getDescription(int index) throws Exception {
      return owner.format(description, index);
   }

   public String getCDescription() {
      return SVD_XML_BaseParser.unEscapeString(getDescription());
   }
   
   public String getCDescription(int index) throws Exception {
      return SVD_XML_BaseParser.unEscapeString(getDescription(index));
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

   public void setBitWidth(long bitwidth) {
      this.bitwidth = bitwidth;
   }

   public AccessType getAccessType() {
      return accessType;
   }

   public void setAccessType(AccessType accessType) {
      this.accessType = accessType;
   }
   
   public ArrayList<Enumeration> getEnumerations() {
      return enumerations;
   }

   public void addEnumeration(Enumeration enumeration) {
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
    *  @param standardFormat  Suppresses some non-standard size optimizations 
    *  @param owner           The owner - This is used to reduce the size by inheriting default values
    */
   public void writeSVD(PrintWriter writer, boolean standardFormat, Register owner, int indent) {
      final String indenter = RegisterUnion.getIndent(indent);

      writer.println(                 indenter+"<field>");
      writer.println(String.format(   indenter+"   <name>%s</name>",               SVD_XML_BaseParser.escapeString(getName())));
      if ((getDescription() != null) && (getDescription().length() > 0)) {
         writer.println(String.format(indenter+"   <description>%s</description>", SVD_XML_BaseParser.escapeString(getDescription())));
      }
      writer.println(String.format(   indenter+"   <bitOffset>%d</bitOffset>",     getBitOffset()));
      if ((owner == null) || standardFormat || (owner.getWidth() != getBitwidth())) {
         writer.println(String.format(indenter+"   <bitWidth>%d</bitWidth>",       getBitwidth()));
      }
      if ((owner == null) || standardFormat || (owner.getAccessType() != getAccessType())) {
         writer.println(String.format(indenter+"   <access>%s</access>",           getAccessType().getPrettyName()));
      }
      if ((getEnumerations() != null) && (!getEnumerations().isEmpty())) {
         writer.println(              indenter+"   <enumeratedValues>");
         for (Enumeration enumeration : getEnumerations()) {
            enumeration.writeSVD(writer, standardFormat, indent+6);
         }
         writer.println(              indenter+"   </enumeratedValues>");
      }
      writer.println(                 indenter+"</field>");
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
    * Maps a bit-field macro name to a Freescale style new name
    * e.g. PORTA_PCR_MUX() -> PORT_PCR_MUX()
    * 
    * @param   Name to map
    * @return  Mapped name (unchanged if not mapped, null if to be deleted)
    * TODO Manual name optimisations
    */
   static String getMappedBitfieldMacroName(String name) {

      final ArrayList<Pair> mappedMacros = new ArrayList<Pair>();

      if (mappedMacros.size() == 0) {
         // Manually eliminate redundant definitions for peripherals which are a subset of earlier peripherals but not derived
         mappedMacros.add(new Pair(Pattern.compile("^(PORT)[^A](_.*)$"),      null));
         mappedMacros.add(new Pair(Pattern.compile("^F{01}(GPIO)[^A](_.*)$"), null));
         mappedMacros.add(new Pair(Pattern.compile("^(GPIO)[^A](_.*)$"),      null));
         mappedMacros.add(new Pair(Pattern.compile("^(UART)[^0](_.*)$"),      null));
         mappedMacros.add(new Pair(Pattern.compile("^(SPI)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(DMAMUX)[^0](_.*)$"),    null));
         mappedMacros.add(new Pair(Pattern.compile("^(SPI)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(ADC)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(I2C)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(AIPS)[^0](_.*)$"),      null));
         mappedMacros.add(new Pair(Pattern.compile("^(CAN)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(I2S)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(OSC)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(DAC)[^0](_.*)$"),       null));
         mappedMacros.add(new Pair(Pattern.compile("^(CMP)[^0](_.*)$"),       null));

         mappedMacros.add(new Pair(Pattern.compile("^(PORT)A(_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^F{01}(GPIO)[ABC](_P(([DSCT]O)|(D[ID])|(ID))R_PT.)$"),    "$1$2")); // MKL FGPIO.. => GPIO..
         mappedMacros.add(new Pair(Pattern.compile("^F{01}(GPIO)A(_.*)$"),                "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(UART)0(_.*)$"),                     "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DMAMUX)0(_.*)$"),                   "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(SPI)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(ADC)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(I2C)0(_.*)$"),                      "$1$2"));
//         mappedMacros.add(new Pair(Pattern.compile("^(AIPS)0(_PACR)A(.*)$"),            "$1$2$3")); // AIPS0_PACRA_TP7_SHIFT -> AIPS_PACR_TP7_SHIFT etc
//         mappedMacros.add(new Pair(Pattern.compile("^(AIPS)0(_PACR)[^A](.*)$"),         null));     // Remove as redundant
         mappedMacros.add(new Pair(Pattern.compile("^(AIPS)0(_.*)$"),                     "$1$2"));   // AIPS0_MPRA_MPL5_SHIFT -> AIPS_MPRA_MPL5_SHIFT etc
         mappedMacros.add(new Pair(Pattern.compile("^(CAN)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(I2S)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(FTM)[^0](_.*)$"),                   null));     // Remove as present when FTMs differ in # of channels
         mappedMacros.add(new Pair(Pattern.compile("^(FTM0_C\\d)Cn(.*)$"),                "$1$2"));   // Fix inconsistent name
         mappedMacros.add(new Pair(Pattern.compile("^(FTM)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(SPI)[^0](_.*)$"),                   null));     // Remove as present when SPIs differ in FIFO depth
         mappedMacros.add(new Pair(Pattern.compile("^(TPM)[^0](_.*)$"),                   null));     // Remove as present when TPMs differ in # of channels
         mappedMacros.add(new Pair(Pattern.compile("^(TPM0_C\\d)Cn(.*)$"),                "$1$2"));   // Fix inconsistent name
         mappedMacros.add(new Pair(Pattern.compile("^(TPM)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(OSC)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DAC)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(CMP)0(_.*)$"),                      "$1$2"));
         mappedMacros.add(new Pair(Pattern.compile("^(DAC\\d_DAT)([L|H])(\\d)$"),         "$1$3$2")); // Fix inconsistent name DAC0_DATL0 -> DAC0_DAT0L
         mappedMacros.add(new Pair(Pattern.compile("^(NV_BACKKEY)3(_KEY.*)$"),            "$1$2"));   // NV_BACKKEY3_KEY_SHIFT -> NV_BACKKEY_KEY_SHIFT etc
//         mappedMacros.add(new Pair(Pattern.compile("^(NV_BACKKEY\\d_KEY.*)$"),            null));     // Remove as redundant
         mappedMacros.add(new Pair(Pattern.compile("^(NV_FPROT)3(.*)$"),                  "$1$2"));   // NV_FPROT3_PROT_SHIFT -> NV_FPROT_PROT_SHIFT etc
//         mappedMacros.add(new Pair(Pattern.compile("^(NV_FPROT\\d.*)$"),                  null));     // Remove as redundant
         mappedMacros.add(new Pair(Pattern.compile("^(FTF._F.*)3(.*)$"),                  "$1$2"));   // FTFA_FCCOB3_CCOBn_SHIFT -> FTFA_FCCOB_CCOBn_SHIFT etc
         mappedMacros.add(new Pair(Pattern.compile("^(DMA_DCHPRI)3(.*)$"),                "$1$2"));   // DMA_DCHPRI3_CHPRI_SHIFT -> DMA_DCHPRI_CHPRI_SHIFT etc
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

   static final String BitfieldMacroPosFormat   = "#define %-40s %d";
   static final String BitfieldMacroMskFormat   = "#define %-40s (0x%02XUL << %s)";
   static final String BitfieldMacroFieldFormat = "#define %-40s (((x)<<%s)&%s)";
   static final String BitfieldFormatComment    =  " /*!< %-40s*/\n";

   String getBaseName() {
      return getName().replaceAll("%s", "n");
   }
   
   /**
    * Writes a set of macros to allow convenient access to the register field
    * e.g. "#define PERIPHERAL_FIELD(x)  (((x)<<FIELD_OFFSET)&FIELD_MASK)"
    * 
    * @param  writer    Where to write 
    * @param  baseName  Basename of the peripheral
    */
   public void writeHeaderFileFieldMacros(PrintWriter writer, String baseName) {
      String fieldname = baseName+"_"+getBaseName();
      if (isMapFreescaleCommonNames()) {
         fieldname = getMappedBitfieldMacroName(fieldname);
         if (fieldname == null) {
            return;
         }
      }
      String posName   = fieldname+getFieldOffsetSuffixName();
      String mskName   = fieldname+getFieldMaskSuffixName();
      
      writer.print(String.format("%-100s%s",
            String.format(BitfieldMacroMskFormat, mskName, ((1L<<getBitwidth())-1), posName), 
            String.format(BitfieldFormatComment,  baseName+": "+getBaseName()+" Mask")));
      writer.print(String.format("%-100s%s",
            String.format(BitfieldMacroPosFormat, posName, getBitOffset()),      
            String.format(BitfieldFormatComment,  baseName+": "+getBaseName()+" Position")));      
      if (getBitwidth()>1) {
         writer.print(String.format("%-100s%s",
            String.format(BitfieldMacroFieldFormat, fieldname+"(x)", posName, mskName), 
            String.format(BitfieldFormatComment,    baseName, getBaseName()+" Field"))); 
      }
   }

}
