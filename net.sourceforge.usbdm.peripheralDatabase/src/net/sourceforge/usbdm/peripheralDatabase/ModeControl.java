package net.sourceforge.usbdm.peripheralDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.peripheralDatabase.Field.Pair;

public class ModeControl implements Cloneable {

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
      ModeControl clone = (ModeControl) super.clone();
      return clone;
   }

   private static String reasonForDifference   = null;
   
   /**
    * Controls some standard differences between ARM header files and Freescale typical
    * 
    *                             Freescale         ARM
    * * Field bit masks use        _MASK            _Msk
    * * Field bit offsets use      _SHIFT           _Pos
    * 
    */
   private static boolean freescaleMode = false;

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
    * Whether to do some intelligent hacks on the files produced.  This
    * is used to clean up some initial incomplete files.
    * 
    * E.g. if the name starts with MKL then set cpu.name="CM3", cpu.nvicPrioBits=2, cpu.fpuPresent=false etc
    */
   private static boolean hackKnownValues = false;
   
   /**
    * Whether to strip spaces from descriptions when writing SVD files
    * 
    */
   private static boolean stripWhiteSpace = false;
   
   /**
    * Sets some standard differences between ARM header files and Freescale typical
    * 
    *                             Freescale         ARM
    * * Field bit masks use        _MASK            _Msk
    * * Field bit offsets use      _SHIFT           _Pos
    * 
    */
   public static boolean isFreescaleMode() {
      return freescaleMode;
   }

   /**
    * Report is using some standard differences between ARM header files and Freescale typical
    * 
    *                             Freescale         ARM
    * * Field bit masks use        _MASK            _Msk
    * * Field bit offsets use      _SHIFT           _Pos
    * 
    */
   public static void setFreescaleMode(boolean freescaleMode) {
      ModeControl.freescaleMode = freescaleMode;
   }

   /**
    * @return the fieldOffsetSuffixName
    */
   public static String getFieldOffsetSuffixName() {
      return freescaleMode?"_SHIFT":"_Pos";
   }

   /**
    * @return the fieldMaskSuffixName
    */
   public static String getFieldMaskSuffixName() {
      return freescaleMode?"_MASK":"_Msk";
   }

   /**
    * Indicates whether to generate Freescale style MACROs to access registers
    */
   public static boolean isGenerateFreescaleRegisterMacros() {
      return generateFreescaleRegisterMacros;
   }

   /**
    * Sets whether to generate Freescale style MACROs to access registers
    */
   public static void setGenerateFreescaleRegisterMacros(
         boolean generateFreescaleRegisterMacros) {
      ModeControl.generateFreescaleRegisterMacros = generateFreescaleRegisterMacros;
   }

   /**
    * Indicates whether to map peripheral names to common Freescale names 
    */
   public static boolean isMapFreescaleCommonNames() {
      return mapFreescaleCommonNames;
   }

   /**
    * Sets whether to map peripheral names to common Freescale names 
    */
   public static void setMapFreescaleCommonNames(boolean mapFreescaleCommonNames) {
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
    * Indicates whether to try to combine peripheral registers into complex arrays where possible 
    * This is only applicable to Freescale style as requires some hints
    */
   public static boolean isExtractComplexStructures() {
      return extractComplexStructures;
   }

   /**
    * Sets whether to try to combine peripheral registers into complex arrays where possible 
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
    * @param regenerateAddressBlocks
    */
   public static void setRegenerateAddressBlocks(boolean regenerateAddressBlocks) {
      ModeControl.regenerateAddressBlocks = regenerateAddressBlocks;
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
    * Indicates whether to do some intelligent hacks on the files produced.  This
    * is used to clean up some initial incomplete files.
    * 
    * E.g. if the name starts with MKL then set cpu.name="CM3", cpu.nvicPrioBits=2, cpu.fpuPresent=false etc
    */
   public static boolean isHackKnownValues() {
      return hackKnownValues;
   }

   /**
    * Sets whether to do some intelligent hacks on the files produced.  This
    * is used to clean up some initial incomplete files.
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
    * Sorts a list of registers/clusters by address and then name
    * @param <E>
    * 
    * @param registers List of registers to sort
    */
   protected static <E> void sortRegisters(ArrayList<E> registers) {
      Collections.sort(registers, new Comparator<E>() {
         @Override
         public int compare(E reg1, E reg2) {
             if (((Cluster)reg1).getAddressOffset() > ((Cluster)reg2).getAddressOffset()) {
               return 1;
            }
            else if (((Cluster)reg1).getAddressOffset() < ((Cluster)reg2).getAddressOffset()) {
               return -1;
            }
            if ((reg1 instanceof Register) && (reg2 instanceof Register)) {
               if (((Register)reg1).getWidth() < ((Register)reg2).getWidth()) {
                  return 1;
               }
               else if (((Register)reg1).getWidth() > ((Register)reg2).getWidth()) {
                  return -1;
               }
               return (((Register)reg1).getName().compareTo(((Register)reg2).getName()));
            }
            return 0;
         }
      });
   }

   /**
    * Maps a Register macro name to a Freescale style name
    * e.g. DAC0_DATL0 -> DAC0_DAT0L
    * 
    * @param   Name to map
    * @return  Mapped name (unchanged if not mapped, null if already mapped)
    */
   static String getMappedRegisterMacroName(String name) {
      final ArrayList<Pair> mappedMacros = new ArrayList<Pair>();
      if (mappedMacros.size() == 0) {
         mappedMacros.add(new Pair(Pattern.compile("^(FTM\\d_C\\d)Cn(.*)$"),      "$1$2"));   // e.g. FTM0_C1CnV  -> FTM0_C1V
         mappedMacros.add(new Pair(Pattern.compile("^(TPM\\d_C\\d)Cn(.*)$"),      "$1$2"));   // e.g. TPM0_C0CnSC -> TPM0_C0SC
         mappedMacros.add(new Pair(Pattern.compile("^(DAC\\d_DAT)([L|H])(\\d)$"), "$1$3$2")); // e.g. DAC0_DATL0 -> DAC0_DAT0L
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
    * Returns a string truncated at the first newline character
    * 
    * @param line
    * @return
    */
   String truncateAtNewline(String line) {
      
      int newlineIndex = line.indexOf('\n');
      if (newlineIndex > 0) {
         line = line.substring(0,  newlineIndex);
      }
      return line;
   }
   
   // Bit dodgy - assumes you only write the Macros once!
   protected static HashMap<String, Integer> fieldMacros = new HashMap<String,Integer>();

   static void resetMacroCache() {
      fieldMacros = new HashMap<String, Integer>();
   }
   
}