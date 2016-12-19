package net.sourceforge.usbdm.peripheralDatabase;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripheralDatabase.Field.Pair;

public class ModeControl {

   private static String reasonForDifference   = null;
   
   /**
    * Controls some standard differences between ARM header files and Freescale typical
    * 
    *                             Freescale         ARM
    * * Field bit masks use        _MASK            _Msk
    * * Field bit offsets use      _SHIFT           _Pos
    * 
    */
   private static boolean freescaleModeFieldNames = false;

   /**
    * Whether to generate Freescale style MACROs to access fields in registers
    */
   private static boolean generateFreescaleRegisterMacros = false;
   
   /**
    * Whether to map peripheral names to common Freescale names 
    */
   private static boolean mapFreescaleCommonNames = false;
   
   /**
    * Whether to try to combine peripheral registers into simple arrays where possible 
    */
   private static boolean extractSimpleRegisterArrays = false;
   
   /**
    * Whether to try to combine peripheral registers into complex arrays where possible 
    * This is only applicable to Freescale style as requires some hints
    */
   private static boolean extractComplexStructures = false;
   
   /**
    * Whether to try to extract peripherals that are derived from other peripherals 
    */
   private static boolean extractDerivedPeripherals = false;
   
   /**
    * Whether to regenerate the address blocks from the register information 
    */
   private static boolean regenerateAddressBlocks = false;
   
   /**
    * Whether to expand derivedFrom register in the SVD output
    */
   private static boolean expandDerivedRegisters = false;
   
   /**
    * Whether to expand derivedFrom peripherals in the SVD output
    */
   private static boolean expandDerivedPeripherals = false;
   
   /**
    * Whether to extract common prefixes from register names
    */
   private static boolean extractCommonPrefix = false;
   
   /**
    * Whether to extract common prefixes from register names
    */
   private static boolean foldRegisters = false;

   /**
    * Whether to generate bit fields a simple numbers or used (1<<N) shifts
    */
   private static boolean useShiftsInFieldMacros = true;

   /**
    * Whether to do some intelligent hacks on the files produced.  This
    * is used to clean up some initial incomplete files.
    * 
    * E.g. if the name starts with MKL then set cpu.name="CM3", cpu.nvicPrioBits=2, cpu.fpuPresent=false etc
    */
   private static boolean hackKnownValues = false;
   
   /**
    * Whether to strip spaces from descriptions when writing SVD files
    */
   private static boolean stripWhiteSpace = false;
   
   /**
    * Whether to flatten arrays to simple registers
    */
   private static boolean flattenArrays;

   /**
    * Whether to rename SIM peripherals source file  based on the device name
    */
   private static boolean renameSimSource = false;

   // Bit dodgy - assumes you only write the Macros once!
   private static HashMap<String, Integer> fieldMacros = null;

   /**
    * Whether to similar extract fields that can be expressed as <derivedFrom> 
    */
   private static boolean extractSimilarFields = false;

   /**
    * Whether to always pad structures with bytes rather than larger elements.
    */
   private static boolean useBytePadding = false;

   /**
    * Whether to map register names when loading XML e.g.
    * <pre>CAU_LDR_CASR => LDR_CASR</pre>
    * 
    * @return True is names are to be mapped.
    */
   private static boolean fMapRegisterNames = false;

   /**
    * Control if reset values of a register is considered when checking for equivalence
    */
   private static boolean fIgnoreResetValuesInEquivalence = false;

   /**
    * Control if Access Type of a register is considered when checking for equivalence
    */
   private static boolean fIgnoreAccessTypeInEquivalence = false;

   public ModeControl() {
   }

   /**
    * Indicates if Access Type of a register is considered when checking for equivalence
    * 
    * @return True indicates ignoring Access Types
    */
   public static boolean isIgnoreAccessTypeInEquivalence() {
      return fIgnoreAccessTypeInEquivalence;
   }

   /**
    * Controls whether Access Type of a register is considered when checking for equivalence
    * 
    * @param value True to ignore Access Type
    */
   public static void setIgnoreAccessTypeInEquivalence(boolean value) {
      ModeControl.fIgnoreAccessTypeInEquivalence = value;
   }

   /**
    * Indicates if reset values of a register is considered when checking for equivalence
    * 
    * @return True indicates ignoring reset values
    */
   public static boolean isIgnoreResetValuesInEquivalence() {
      return fIgnoreResetValuesInEquivalence;
   }

   /**
    * Controls whether reset values of a register is considered when checking for equivalence
    * 
    * @param value True to ignore reset values
    */
   public static void setIgnoreResetValuesInEquivalence(boolean value) {
      ModeControl.fIgnoreResetValuesInEquivalence = value;
   }

   /**
    * Whether to map register names when loading XML e.g.
    * <pre>CAU_LDR_CASR => LDR_CASR</pre>
    * 
    * return True if ignoring reset values
    */
   public static boolean isMapRegisterNames() {
      return fMapRegisterNames;
   }

   /**
    * Sets whether to map register names when loading XML
    * 
    * @param mapRegisterNames True to map names
    */
   public static void setMapRegisterNames(boolean mapRegisterNames) {
      fMapRegisterNames = mapRegisterNames;
   }

   /**
    * Whether to always pad structures with bytes rather than larger elements.
    */
   public static boolean isUseBytePadding() {
      return useBytePadding;
   }

   /**
    * Whether to always pad structures with bytes rather than larger elements.
    */
   public static void setUseBytePadding(boolean useBytePadding) {
      ModeControl.useBytePadding = useBytePadding;
   }

   /**
    * @return Whether to rename SIM peripherals source file  based on the device name
    */
   public static boolean isRenameSimSource() {
      return renameSimSource;
   }

   /**
    * Cause SIM peripherals source file to be renamed based on the device name
    * 
    * @param foldRegisters the foldRegisters to set
    */
   public static void setRenameSimSources(boolean renameSimSource) {
      ModeControl.renameSimSource = renameSimSource;
   }

   /**
    * @return the foldRegisters
    */
   public static boolean isFoldRegisters() {
      return foldRegisters;
   }

   /**
    * Cause some register names that appears twice to be folded (i.e. 2nd occurrence deleted)
    * 
    * @param extractSimilarFields True to enable operation
    */
   public static void setExtractSimilarFields(boolean extractSimilarFields) {
      ModeControl.extractSimilarFields = extractSimilarFields;
   }

   /**
    * @return the foldRegisters
    */
   public static boolean isExtractSimilarFields() {
      return extractSimilarFields;
   }

   /**
    * Cause some register names that appears twice to be folded (i.e. 2nd occurrence deleted)
    * 
    * @param foldRegisters the foldRegisters to set
    */
   public static void setFoldRegisters(boolean foldRegisters) {
      ModeControl.foldRegisters = foldRegisters;
   }

   /**
    * Gets option controlling some standard differences between ARM and Freescale header files<br>
    * Only affects header file generation
    * <pre>
    *                            Freescale         ARM
    * Field bit masks use        _MASK            _Msk
    * Field bit offsets use      _SHIFT           _Pos
    * </pre>
    */
   public static boolean useFreescaleFieldNames() {
      return freescaleModeFieldNames;
   }

   /**
    * Gets option controlling some standard differences between ARM and Freescale header files<br>
    * Only affects header file generation
    * <pre>
    *                             Freescale         ARM
    * Field bit masks use        _MASK            _Msk
    * Field bit offsets use      _SHIFT           _Pos
    * </pre>
    * 
    * @param freescaleMode True to enable operation
    */
   public static void setFreescaleFieldNames(boolean freescaleMode) {
      ModeControl.freescaleModeFieldNames = freescaleMode;
   }

   /**
    *  Returns the suffix to use for field offset macros (_SHIFT or _Pos)
    *  
    * @return the fieldOffsetSuffixName
    */
   public static String getFieldOffsetSuffixName() {
      return useFreescaleFieldNames()?"_SHIFT":"_Pos";
   }

   /**
    *  Returns the suffix to use for field mask macros (_MASK or _Msk)
    *  
    * @return the fieldMaskSuffixName
    */
   public static String getFieldMaskSuffixName() {
      return useFreescaleFieldNames()?"_MASK":"_Msk";
   }

   /**
    * Indicates whether to generate Freescale style MACROs to access registers e.g.<br>
    * 
    * <pre>#define CMP1_CR0    (CMP1->CR0)</pre>
    * 
    * Only affects header file generation
    */
   public static boolean isGenerateFreescaleRegisterMacros() {
      return generateFreescaleRegisterMacros;
   }

   /**
    * Set whether to generate Freescale style MACROs to access registers e.g.<br>
    * 
    * <pre>#define CMP1_CR0    (CMP1->CR0)</pre>
    * 
    * Only affects header file generation
    */
   public static void setGenerateFreescaleRegisterMacros(
         boolean generateFreescaleRegisterMacros) {
      ModeControl.generateFreescaleRegisterMacros = generateFreescaleRegisterMacros;
   }

   /**
    * Indicates whether to map peripheral names to common Freescale names e.g.<br>
    * 
    * <pre>"PTA" => "GPIOA"</pre>
    */
   public static boolean isMapFreescaleCommonNames() {
      return mapFreescaleCommonNames;
   }

   /**
    * Sets whether to map peripheral names to common Freescale names e.g.<br>
    * 
    * <pre>"PTA" => "GPIOA"</pre>
    */
   public static void setMapFreescalePeriperalCommonNames(boolean mapFreescaleCommonNames) {
      ModeControl.mapFreescaleCommonNames = mapFreescaleCommonNames;
   }

   /**
    * Indicates whether to try to combine peripheral registers into simple arrays where possible 
    */
   public static boolean isExtractSimpleRegisterArrays() {
      return extractSimpleRegisterArrays;
   }

   /**
    * Sets whether to try to combine peripheral registers into simple arrays where possible 
    */
   public static void setExtractSimpleRegisterArrays(boolean extractRegisterArrays) {
      ModeControl.extractSimpleRegisterArrays = extractRegisterArrays;
   }

   /**
    * Indicates whether to try to combine peripheral registers into complex arrays where possible <br>
    * This is only applicable to Freescale style as requires some hints
    */
   public static boolean isExtractComplexStructures() {
      return extractComplexStructures;
   }

   /**
    * Sets whether to try to combine peripheral registers into complex arrays where possible <br>
    * This is only applicable to Freescale style as requires some hints
    */
   public static void setExtractComplexStructures(boolean extractComplexArrays) {
      ModeControl.extractComplexStructures = extractComplexArrays;
   }

   /**
    * Indicates whether to try to extract peripherals that are derived from other peripherals 
    */
   public static boolean isExtractDerivedPeripherals() {
      return extractDerivedPeripherals;
   }

   /**
    * Indicates whether to recalculate the peripheral address blocks from registers
    * 
    * @return value
    */
   public static boolean isRegenerateAddressBlocks() {
      return regenerateAddressBlocks;
   }

   /**
    * Sets whether to recalculate the peripheral address blocks from registers
    * 
    * @param regenerateAddressBlocks True to enable operation
    */
   public static void setRegenerateAddressBlocks(boolean regenerateAddressBlocks) {
      ModeControl.regenerateAddressBlocks = regenerateAddressBlocks;
   }

   /**
    * Indicates whether to whether to flatten arrays to simple registers
    * 
    * @return value
    */
   public static boolean isFlattenArrays() {
      return flattenArrays;
   }

   /**
    * Sets whether to flatten arrays to simple registers
    * 
    * @param regenerateAddressBlocks
    */
   public static void setFlattenArrays(boolean flattenArrays) {
      ModeControl.flattenArrays = flattenArrays;
   }

   /**
    * Indicates whether to expand derivedFrom registers in the SVD output
    * 
    * @return value
    */
   public static boolean isExpandDerivedRegisters() {
      return expandDerivedRegisters;
   }

   /**
    * Sets whether to expand derivedFrom registers in the SVD output
    * 
    * @param expandDerivedRegisters
    */
   public static void setExpandDerivedRegisters(boolean expandDerivedRegisters) {
      ModeControl.expandDerivedRegisters = expandDerivedRegisters;
   }

   /**
    * Indicates whether to expand derivedFrom registers in the SVD output
    * 
    * @return value
    */
   public static boolean isExpandDerivedPeripherals() {
      return expandDerivedPeripherals;
   }
   
   /**
    * Sets whether to expand derivedFrom registers in the SVD output
    * 
    * @param expandDerivedRegisters
    */
   public static void setExpandDerivedPeripherals(boolean expandDerivedPeripherals) {
      ModeControl.expandDerivedPeripherals = expandDerivedPeripherals;
   }

   /**
    * Indicates whether to extract common prefixes from register names
    */
   public static boolean isExtractCommonPrefix() {
      return extractCommonPrefix;
   }

   /**
    * Indicates whether to do some intelligent hacks on the files produced.  <br>
    * This is used to clean up some initial incomplete files.<br>
    * 
    * E.g. if the name starts with MKL then set cpu.name="CM3", cpu.nvicPrioBits=2, cpu.fpuPresent=false etc
    */
   public static boolean isHackKnownValues() {
      return hackKnownValues;
   }

   /**
    * Sets whether to do some intelligent hacks on the files produced. <br>
    * This is used to clean up some initial incomplete files.<br>
    * 
    * E.g. if the name starts with MKL then set cpu.name="CM3", cpu.nvicPrioBits=2, cpu.fpuPresent=false etc
    */
   public static void setHackKnownValues(boolean hackKnownValues) {
      ModeControl.hackKnownValues = hackKnownValues;
   }

   /**
    * Indicates whether to strip spaces from descriptions when writing SVD files
    */
   public static boolean isStripWhiteSpace() {
      return stripWhiteSpace;
   }

   /**
    * Set whether to strip spaces from descriptions when writing SVD files
    */
   public static void setStripWhiteSpace(boolean stripWhiteSpace) {
      ModeControl.stripWhiteSpace = stripWhiteSpace;
   }

   /**
    * Sets whether to extract common prefixes from register names
    */
   public static void setExtractCommonPrefix(boolean extractCommonPrefix) {
      ModeControl.extractCommonPrefix = extractCommonPrefix;
   }

   /**
    * Sets whether to try to extract peripherals that are derived from other peripherals 
    */
   public static void setExtractDerivedPeripherals(boolean extractDerivedPeripherals) {
      ModeControl.extractDerivedPeripherals = extractDerivedPeripherals;
   }

   /**
    * Returns Reason while the last peripheral comparison failed.
    * 
    * @return reason (or null if none)
    */
   public static String getReasonForDifference() {
      return reasonForDifference;
   }

   /**
    * Set reason while the last peripheral comparison failed.
    * 
    * @param reasonForDifference 
    */
   public static void setReasonForDifference(String reasonForDifference) {
      ModeControl.reasonForDifference = reasonForDifference;
   }

   /**
    * Does some simple modifications to descriptions to increase the likelihood of folding.
    * @param description String to operate on
    * 
    * @return     Modified string
    */
   public static String getSanitizedDescription(String description) {
      final Pattern p = Pattern.compile("((.*)(\\.$))|((.*)( [R|r]egisters)( )*$)");

      Matcher m = p.matcher(description);
      return m.replaceAll("$2$5");
   }

   /**
    * Sorts a list of registers/clusters by address and then size (largest size first)
    * @param <E>
    * 
    * @param registers List of registers to sort
    */
   protected static <E> void sortRegisters(ArrayList<E> registers) {
      Collections.sort(registers, new Comparator<E>() {
         @Override
         public int compare(E reg1, E reg2) {
            long num1 = ((Cluster)reg1).getAddressOffset();
            long num2 = ((Cluster)reg2).getAddressOffset();
            if (num1 == num2) {
               num2 = ((Cluster)reg1).getTotalSizeInBytes();
               num1 = ((Cluster)reg2).getTotalSizeInBytes();
            }
            if (num1<num2) {
               return -1;
            }
            if (num1>num2) {
               return 1;
            }
            return (((Cluster)reg1).getName().compareTo(((Cluster)reg2).getName()));
         }
      });
   }

   /**
    * Maps a Register macro name to a Freescale style name
    * e.g. DAC0_DATL0 => DAC0_DAT0L
    * 
    * @param   Name to map
    * @return  Mapped name (unchanged if not mapped, null if already mapped)
    */
   static String getMappedRegisterMacroName(String name) {
      final ArrayList<Pair> mappedMacros = new ArrayList<Pair>();
      if (mappedMacros.size() == 0) {
         //TODO - Where register name MACROs are mapped
         mappedMacros.add(new Pair(Pattern.compile("^(FTM[0-9]?)_CONTROLS([0-9]+)_Cn(.*)$"),       "$1_C$2$3"));  // e.g. FTM1_CONTROLS0_CnSC -> FTM1_C0SC
         mappedMacros.add(new Pair(Pattern.compile("^(TPM[0-9]?)_CONTROLS([0-9]+)_Cn(.*)$"),       "$1_C$2$3"));  // e.g. TPM1_CONTROLS0_CnSC -> TPM_C0SC
         mappedMacros.add(new Pair(Pattern.compile("^(FMC[0-9]?_.*)_(S[0-9]+.*)$"),                "$1$2"));      // e.g. FMC_TAGVDW0_S0 -> FMC_TAGVDW0S0
//         mappedMacros.add(new Pair(Pattern.compile("^(FMC[0-9]?_)S(_.*)$"),                        "$1TAGVD$2")); // e.g. FMC_S_valid_MASK -> FMC_TAGVD_valid_MASK
         mappedMacros.add(new Pair(Pattern.compile("^(PDB[0-9]?_.*[0-9]*)_(.*)$"),                 "$1$2"));      // e.g. PDB0_CH0_C1 -> PDB0_CH0C1
         mappedMacros.add(new Pair(Pattern.compile("^(PIT[0-9]?)_CHANNEL([0-9]+)(.*)$"),           "$1$3$2"));    // e.g. PIT_CHANNEL0_LDVAL -> PIT_LDVAL0
         mappedMacros.add(new Pair(Pattern.compile("^(DAC[0-9]?)_DAT([0-9]+)(.*)([HL])$"),         "$1$3$2$4"));  // e.g. DAC0_DAT0_DATH -> DAC0_DAT0H
         mappedMacros.add(new Pair(Pattern.compile("^(DAC[0-9]?)_DAT([0-9]+)(.*)$"),               "$1$3$2"));    // e.g. DAC0_DAT0_DATA -> DAC0_DATA0
         mappedMacros.add(new Pair(Pattern.compile("^(FB[0-9]?)_CS([0-9]+)(.*)$"),                 "$1$3$2"));    // e.g. FB_CS0_CSAR -> FB_CSAR0
         mappedMacros.add(new Pair(Pattern.compile("^(MPU[0-9]?)_SP([0-9]+)(.*)$"),                "$1$3$2"));    // e.g. MPU_SP0_EAR -> MPU_EAR0
         mappedMacros.add(new Pair(Pattern.compile("^(MTBDWT[0-9]?)_COMPARATOR([0-9]+)(.*)$"),     "$1$3$2"));    // e.g. MPU_SP0_EAR -> MPU_EAR0
         mappedMacros.add(new Pair(Pattern.compile("^(DMA[0-9]?)_DMA([0-9]+)(.*)$"),               "$1$3$2"));    // e.g. DMA_DMA0_SAR -> MPU_EAR0

         mappedMacros.add(new Pair(Pattern.compile("^(DMA)_CH([0-9]+)(_.*)$"),                     "$1$3$2"));    // e.g. DMA_CH0_SAR -> DMA_SAR0
         mappedMacros.add(new Pair(Pattern.compile("^(DTIM)_CH([0-9]+)(_.*)$"),                    "$1$3$2"));    // e.g. DTIM_CH0_DTMR -> DTIM_DTMR0
         mappedMacros.add(new Pair(Pattern.compile("^(FBCS)_CH([0-9]*)(_.*)$"),                    "$1$3$2"));    // e.g. FBCS_CH0_CSAR -> FBCS_CSAR0
         mappedMacros.add(new Pair(Pattern.compile("^(USB[0-9]?)_ENDP(?:OIN)T([0-9]*)(_.*)$"),     "$1$3$2"));    // e.g. FBCS_CH0_CSAR -> FBCS_CSAR0
      }
      for (Pair p : mappedMacros) {
         Matcher matcher = p.regex.matcher(name);
         if (matcher.matches()) {
//            String oldName = name;
            name = matcher.replaceAll(p.replacement);
//            System.err.println(String.format("getMappedRegisterMacroName() : %s -> %s", oldName, name));
            break;
         }
      }
      return name;
   }

   /**
    * Generates a structure name from a peripheral name
    * e.g. DAC0 -> DAC, GPIOA->GPIO
    * There are used for the structure names e.g. GPIOA => GPIO_Type
    * 
    * @param   Name to map
    * @return  Mapped name (unchanged if not mapped)
    */
   static String getStructNamefromName(String name) {
      final ArrayList<Pair> mappedNames = new ArrayList<Pair>();
      if (mappedNames.size() == 0) {
         //TODO - Where structure names are mapped
         mappedNames.add(new Pair(Pattern.compile("(^FTM[0-9]*$)"),   "$1"));  // e.g. FTM0 etc. unchanged
         mappedNames.add(new Pair(Pattern.compile("(^TPM[0-9]*$)"),   "$1"));  // e.g. TPM0 etc. unchanged
         mappedNames.add(new Pair(Pattern.compile("(^EPORT[0-9]*$)"), "$1"));  // e.g. EPORT0 etc. unchanged
         mappedNames.add(new Pair(Pattern.compile("(^GPIO)[A-F]*$"),  "$1"));  // e.g. GPIOA -> GPIO
         mappedNames.add(new Pair(Pattern.compile("(^FGPIO)[A-F]*$"), "$1"));  // e.g. FGPIOA -> FGPIO
         mappedNames.add(new Pair(Pattern.compile("(^PT)[A-F]*$"),    "$1"));  // e.g. PTA -> PA
         mappedNames.add(new Pair(Pattern.compile("(^LTC)[0-9]*$"),   "$1"));  // e.g. LTC0 -> LTC
//       mappedNames.add(new Pair(Pattern.compile("(^.*?)[0-9]*$"),   "$1"));  // e.g. DMA1 -> DMA
      }
      for (Pair p : mappedNames) {
         Matcher matcher = p.regex.matcher(name);
         if (matcher.matches()) {
//            String oldName = name;
            name = matcher.replaceAll(p.replacement);
//            System.err.println(String.format("getMappedRegisterMacroName() : %s -> %s", oldName, name));
            break;
         }
      }
      return name;
   }

   /**
    * Returns a string truncated at the first newline or tab character
    * 
    * @param line
    * @return
    */
   static public String truncateAtNewlineOrTab(String line) {
      
      int newlineIndex = line.indexOf('\n');
      if (newlineIndex > 0) {
         line = line.substring(0,  newlineIndex);
      }
      int tabIndex = line.indexOf('\t');
      if (tabIndex > 0) {
         line = line.substring(0,  tabIndex);
      }
      return line;
   }
   
   /**
    * Reset the cache used to detect macro collisions.
    */
   static protected void clearMacroCache() {
      fieldMacros = null;
   }

   /**
    * Checks for collisions on field macro names
    * 
    * @param field
    * @param fieldname
    * 
    * @return  true - generate macro, false discard macro
    * 
    * @throws Exception if name used previously with different offset/width
    */
   protected static boolean fieldMacroAlreadyDone(Field field, String fieldname) throws Exception {
      if (fieldMacros == null) {
         fieldMacros = new HashMap<String, Integer>();
      }
      int newHashcode = (int) (field.getBitwidth() + (field.getBitOffset()<<8));
      Integer hashCode = fieldMacros.get(fieldname);
      if (hashCode != null) {
         // Check if re-definition is the same
         if (hashCode != newHashcode) {
            throw new Exception("Redefined MACRO has different hashcode \""+fieldname+"\"");
         }
         // Don't regenerate the MACRO (duplication due to name folding)
         return true;
      }
      // Add to table
      fieldMacros.put(fieldname, newHashcode);
      // Indicate new MACRO
      return false;
   }
   
   /**
    * Indicates whether to use shifts in filed macros
    *  
    * @return the useShiftsInFiledMacros
    */
   public static boolean isUseShiftsInFieldMacros() {
      return useShiftsInFieldMacros;
   }

   /**
    * Sets whether to use shifts in field macros
    *  
    * @param useShiftsInFieldMacros the useShiftsInFiledMacros to set
    */
   public static void setUseShiftsInFieldMacros(boolean useShiftsInFieldMacros) {
      ModeControl.useShiftsInFieldMacros = useShiftsInFieldMacros;
   }

   static final String groupPostamble =  
    "/**\n"+
    " * @}\n"+
    " */ /* end of group %s */\n\n";

   /**
    * Write open group comment
    * <pre><code>
    * /**                                                                    
    *  * @addtogroup  <i><b>groupName</i></b>_GROUP <i><b>groupTitle</i></b>                   
    *  * @brief       <i><b>groupBrief</i></b>  
    *  * @{                                                                   
    *  *&#47;</code></pre>
    * 
    * @param writer        Where to write 
    * @param groupName     Name of group
    * @param groupTitle    Title of group (may be null)
    * @param groupBrief    Brief description of group (may be null)
    * 
    * @throws IOException
    */
   static void writeGroupPreamble(Writer writer, String groupName, String groupTitle, String groupBrief) throws IOException {
      final String startGroup1 = 
            "/**\n"+
            "* @addtogroup %s %s\n";
      final String startGroup2 = 
            "* @brief %s\n";
      final String startGroup3 = 
            "* @{\n"+
            "*/\n";
      writer.write(String.format(startGroup1, groupName+"_GROUP", (groupTitle==null)?"":groupTitle));
      if (groupBrief != null) {
         writer.write(String.format(startGroup2, groupBrief));
      }
      writer.write(String.format(startGroup3));
   }   
   /**
    * Write close group comment 
    * <pre><code>
    * /**                                                                    
    *  * End Group <i><b>groupName</i></b>_GROUP
    *  * @}                                                                   
    *  **&#47;</code></pre>
    * 
    * @param writer        Where to write 
    * @param groupName     Name of group
    * 
    * @throws IOException
    */
   static void writeGroupPostamble(Writer writer, String groupName) throws IOException {
      final String endGroup = 
            "/**\n"+
            " * @} */ /* End group %s \n"+
            " */\n";
      writer.write(String.format(endGroup, groupName+"_GROUP"));
   }
   /**
    * Write close group comment 
    * <pre><code>
    * /**
    *  * End Group                                                                    
    *  * @}                                                                   
    *  *&#47;</code></pre>
    * 
    * @param writer        Where to write 
    * 
    * @throws IOException
    */
   static void writeGroupPostamble(Writer writer) throws IOException {
      final String endGroup = 
            "/**\n"+
            " * End Group\n"+
            " * @}\n"+
            "*/\n";
      writer.write(endGroup);
   }

}