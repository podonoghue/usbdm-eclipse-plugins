package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.PinListExpansion.PinMap;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BitmaskVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.IModelChangeListener;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;
import net.sourceforge.usbdm.deviceEditor.parsers.ParseMenuXML;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class BitmaskVariable extends LongVariable {

   public static final int BIT_INDEX_ALL  = -1;
   public static final int BIT_INDEX_NONE = -2;
   
   public static class BitInformation {
      // Details of each bit
      public BitInformationEntry[] bits          = null;
      // Permitted bit in register
      public long                  permittedBits = 0;
      // Index of entry in bits that describes 'all' bits selected if exists
      public int                   allIndex  = -1;
      // Index of entry in bits that describes 'no' bits selected if exists
      public int                   noneIndex = -1;
      
      private void updateSpecialIndices() {
         allIndex  = -1;
         noneIndex = -1;
         for (int index=0; index<bits.length; index++) {
            BitInformationEntry bit = bits[index];
            if (bit.bitNum!=null) {
               if (bit.bitNum == BIT_INDEX_ALL) {
                  allIndex = index;
               }
               if (bit.bitNum == BIT_INDEX_NONE) {
                  noneIndex = index;
               }
            }
         }
      }
      
      private long calculateBitMask() {
         long bitmask = 0;
         for (BitInformationEntry bit:bits) {
            if ((bit.bitNum!=null)&&(bit.bitNum>=0)) {
               bitmask |= (1L<<bit.bitNum);
            }
         }
         return bitmask;
      }

      
      public BitInformation() {
      }
      
      /**
       * Constructor<br>
       * Member permittedBits is calculated from the bits provided.
       * This assumes that bitNum field has been set for each.<br>
       * 
       * @param bits          Information about each bit
       * @param permittedBits Bit-mask indicating permitted bits
       * @throws Exception
       */
      public BitInformation(BitInformationEntry[] bits) {
         this.bits = bits;
         this.permittedBits = calculateBitMask();
         updateSpecialIndices();
      }
      
      /**
       * Constructor<br>
       * If permittedBits is zero or null then it is calculated from the bits provided.
       * This assumes that bitNum field has been set for each.<br>
       * If permittedBits is non-zero then it is used to calculate the bitNum for each bit.
       * This assumes the bits are in the order provided.
       * 
       * @param bits          Information about each bit
       * @param permittedBits Bit-mask indicating permitted bits
       * @throws Exception
       */
      public BitInformation(BitInformationEntry[] bits, Long permittedBits) throws Exception {
         this.bits = bits;
         if ((permittedBits == null)||(permittedBits == 0L)) {
            this.permittedBits = calculateBitMask();
         }
         else {
            this.permittedBits = permittedBits;
            calculateBitPositions();
         }
         updateSpecialIndices();
      }

      /**
       * Allocates or checks the bit positions against the permittedBits member
       * 
       * @throws Exception
       */
      private void calculateBitPositions() throws Exception {
         
         // Sort by bit so we can check pre-allocated bits conveniently
         BitInformationEntry[] sortedBits = Arrays.copyOf(bits, bits.length);
         
         Arrays.sort(sortedBits,new Comparator<BitInformationEntry>() {
            @Override
            public int compare(BitInformationEntry left, BitInformationEntry right) {
               if ((left.bitNum == null)||(right.bitNum == null)) {
                  return 0;
               }
               return left.bitNum - right.bitNum;
            }
         });
         
         // Used as progressive check on unallocated bits
         long unallocatedBits = permittedBits;
         
         // Index of bit to place
         int bitIndex   = 0;
         
         // Bit number in mask
         int bitNum     = 0;
         
         // Should either have allocated all or checked all - mixture not allowed
         boolean haveAllocatedBits = false;
         boolean haveCheckedBits   = false;
         
         while(bitNum<32) {
            if (bitIndex>=sortedBits.length) {
               // Finished
               break;
            }
            if ((sortedBits[bitIndex].bitNum != null)&&(sortedBits[bitIndex].bitNum<0)) {
               // Ignore special bits
               bitIndex++;
               continue;
            }
            // Have field to allocate to bit
            long bitmask = 1L<<bitNum;
            if (bitmask>unallocatedBits) {
               // Ran out of bit positions before bits to place
               throw new Exception("Mis-match between bit mask provided and number of bits defined");
            }
            if ((bitmask & unallocatedBits) == 0) {
               // Bit location not available - Keep looking
               bitNum++;
               continue;
            }
            // Have possible bit position
            if (sortedBits[bitIndex].bitNum != null) {
               // Check pre-allocation
               if (sortedBits[bitIndex].bitNum != bitNum) {
                  throw new Exception("Mis-match between bit position calculated ("+bitNum+") and"
                        + " provided in field ("+sortedBits[bitIndex++].bitNum+")");
               }
               haveCheckedBits = true;
            }
            else {
               // Allocate position
               sortedBits[bitIndex].bitNum = bitNum;
               haveAllocatedBits = true;
            }
            unallocatedBits &= ~bitmask;
            bitIndex++;
            bitNum++;
         }
         // Check for unallocated bits
         if (unallocatedBits != 0) {
            throw new Exception("Mis-match between bit mask provided and bits used by fields");
         }
         if (haveAllocatedBits && haveCheckedBits) {
            throw new Exception("Mixture of allocated and unallocated bits found");
         }
      }
   }
   
   public static class BitInformationEntry {
      
      // Bit name - used for enum etc
      public String  bitName = null;
      // Descriptions of bit - used for GUI/commenting
      public String  description = null;
      // This maps the index used for bit information array to a physical bit number
      // Index may be -ve to indicate no mapping (used in case 3 for dummy bits)
      public Integer bitNum = -1;
      // C-code mask value - used in enum generation if provided
      public String mask = null;
      
      public BitInformationEntry(String bitName, String description, Integer bitnum, String mask) {
         this.bitName      = bitName;
         this.description  = description;
         this.bitNum       = bitnum;
         this.mask         = mask;
//         System.err.println(String.format("BitInformationEntry: %-2d, %-30s %-35s %-20s",
//               bitnum, (mask==null)?"-":"("+mask+")", bitName, description));
      }

      public BitInformationEntry() {
      }

      /**
       * Initialise bitInformation from this
       * 
       * @param bitInformation
       */
      public void initialiseFrom(BitInformationEntry bi) {
         bitName     = bi.bitName;
         description = bi.description;
         bitNum      = bi.bitNum;
         mask        = bi.mask;
      }
      
   };
   
   // Information describing each bit
   private BitInformation fBitInformation = null;
   
   // Pin mapping information
   private String fPinMap;
   
   private long   fRawPermittedBits;
   private String fRawBitNames;
   private String fRawBitDescriptions;
   
   /**
    * @param name
    * @param key
    * 
    * @param permittedBits Bit-mask of permitted bit values
    * 
    * @param bitNames e.g. "bit0,bit1"
    * List of bit names (lsb-msb).  The list is expanded before use:<br>
    *    <li>A1-3;BB5-7    => A1,A2,A3,BB5,BB6,BB7 and then split on commas
    *    <li>PT(A-D)(0-7); => PTA0,PTA1,PTA2...PTD5,PTD6,PTD7<br>
    * After expansion %i is replace with the index of the name in the list<br>
    * 
    * @param fBitDescriptions Descriptions for bits
    * 
    * @throws Exception
    */
   public BitmaskVariable(VariableProvider provider, String name, String key) throws Exception {
      super(provider, name, key);
   }

   @Override
   protected BitmaskVariableModel privateCreateModel(BaseModel parent) {
      return new BitmaskVariableModel(parent, this);
   }

   @Override
   public String formatValueAsString(long value) {
      return "0x"+Long.toHexString(value);
   }

   @Override
   public String getSubstitutionValue() {
      return getValueAsString();
   }

   /**
    * Set pin map<br>
    * This indicates the Signal to Pin mapping for each bit in the bitmask value
    * 
    * @param pinMap
    */
   public void setPinMap(String pinMap) {
      fPinMap = pinMap;
   }

   /**
    * Update pin mapping based on the bitmap provided
    * 
    * @param bitmap Bitmap where '1' => map pin to signal, '0' => unmap the pin
    * 
    * @throws Exception
    */
   private void updatePinMap(long bitmap) throws Exception {
      String disablePinMap = getDisabledPinMap();
      if (!isEnabled() && (disablePinMap != null)) {
         // Disabled and disabled map provided
         setActivePinMappings(getDisabledPinMap());
      }
      else {
         // Use map associated with choice (even if variable disabled)
         if (fPinMap != null) {
            PinMap[] pinMaps = PinListExpansion.expandNameList(fPinMap);

            for (int index=0; index<pinMaps.length; index++) {
               PinMap pinMapEntry = pinMaps[index];
               if (pinMapEntry.pin.isBlank()) {
                  continue;
               }
               try {
                  if ((bitmap & (1L<<index)) != 0) {
                     // Map these
                     setActivePinMapping(pinMapEntry.signal, pinMapEntry.pin);
                  }
                  else {
                     // Unmap these
                     setActivePinMapping(pinMapEntry.signal, null);
                  }
               } catch (Exception e) {
                  System.err.println("Signal mapping change failed for " + pinMapEntry + ", reason=" + e.getMessage());
               }
            }
         }
      }
   }
   
   @Override
   public boolean enable(boolean enabled) {
      boolean rv = super.enable(enabled);
      if (rv) {
         try {
            updatePinMap(getValueAsLong());
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return rv;
   }
   
   @Override
   public boolean setValueQuietly(Long value) {
      boolean rv = super.setValueQuietly(value);
      if (rv) {
         try {
            updatePinMap(value);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return rv;
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      return super.clone();
   }

   @Override
   public void update(VariableUpdateInfo info, Expression expression) {
      
      super.update(info, expression);
      
      try {
         if (info.doFullUpdate || ((info.properties&IModelChangeListener.PROPERTY_VALUE)!=0)) {
            updatePinMap(getValueAsLong());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   /**
    * Initialise bit information from another instance of BitmaskVariable
    * 
    * @param other   Variable to initialise from
    */
   public void init(BitmaskVariable other) {
      this.fBitInformation = other.getBitInformation();
   }

   /**
    * Initialise bit information
    * 
    * @param bitInformation Description of bits
    */
   public void init(BitInformation bitInformation) {
      fBitInformation = bitInformation;
   }
   
   /**
    * Initialise bit information
    * 
    * @param permittedBits    Bit mask of permitted bits
    * @param bitNames         Names of bits
    * @param bitDescriptions  Description of bits
    */
   public void init(long permittedBits, String bitNames, String bitDescriptions) {
      fRawPermittedBits    = permittedBits;
      fRawBitNames         = bitNames;
      fRawBitDescriptions  = bitDescriptions;
   }

   /**
    * Expands lists using substitutions<br>
    * 
    * @param permittedBits Bit-mask of permitted bit values
    * 
    * @param bitNames e.g. "bit0,bit1" or "Bit%i"
    * 
    * @param fBitDescriptions Descriptions for bits
    * 
    * <b>Examples:</b>
    * <pre>
    * permittedBits bitList    bitDescription Bit usage               BitMapping           Buttons                            Description
    *                                                                    (from permittedBits
    *                                                                    or bitList)
    * 1a: 0x36       Pin%i      null        => Pin0,Pin1,Pin2,,,Pin5   0,1,2,5              Pin0,Pin1,Pin2,Pin5 (generated)    -
    * 1b: 0x36       Pin%i      Load %i     => Pin0,Pin1,Pin2,,,Pin5   0,1,2,5              Pin0,Pin1,Pin2,Pin5 (generated)    Load 0, Load 1 ...
    * 2a: 0x36       A,B,C,D                => A,B,C,,,D               0,1,2,5              A,B,C,D
    * 2b: 0x36       A,B,C,D    W,X,Y,Z     => A,B,C,,,D               0,1,2,5              A,B,C,D                            W,X,Y,Z
    * 3a: 0x00       A,,B,,,C               => A,,B,,,C                0,3,5                A,B,C
    * 3b: 0x00       A,,B,,,C   W,,X,,,Y    => A,,B,,,C                0,3,5                A,B,C                              W,X,Y
    * 4:  0x36       null or ''             => B0,B1,B2,,,B5           0,1,2,-1,-1,5        B0,B1,B2,,,B5 (generated as needed, some disabled)
    * </pre>
    * @return
    * 
    * @throws Exception
    */
   public void calculateValues() throws Exception {

      fBitInformation = new BitInformation();
      
      String[] tBitnames;
      if ((fRawBitNames != null)&&(fRawBitNames.startsWith("@"))) {
         tBitnames     = PinListExpansion.expandPinList(Expression.getValueAsString(fRawBitNames.substring(1), getProvider()), ",");
      }
      else {
         tBitnames     = PinListExpansion.expandPinList(fRawBitNames, ",");
      }
      String[] tDescriptions;
      if ((fRawBitDescriptions != null)&&(fRawBitDescriptions.startsWith("@"))) {
//         tDescriptions     = PinListExpansion.expandPinList(Expression.getValueAsString(fRawBitDescriptions.substring(1), getProvider()), ",");
         tDescriptions     = new String[1];
         tDescriptions[0]  = fRawBitDescriptions;
      }
      else {
         tDescriptions     = PinListExpansion.expandPinList(fRawBitDescriptions, ",");
      }
      
      if (fRawPermittedBits == 0) {
         // Case 3 - Bitmask=0, Names must be provided with skipped bits
         // Determine bitmask and indices from bit-names
         if (tBitnames == null) {
            throw new Exception("BitmaskVariable: Either a bitList or bitNames must be provided");
         }

         ArrayList<Integer> bitMappingList     = new ArrayList<Integer>();
         ArrayList<String>  bitNamesList       = new ArrayList<String>();

         for (int index=0; index<tBitnames.length; index++) {
            long mask = (1L<<index);
            if (!tBitnames[index].isBlank()) {
               fRawPermittedBits |= mask;
               bitMappingList.add(index);
               bitNamesList.add(tBitnames[index]);
            }
         }
         fBitInformation.bits = new BitInformationEntry[bitNamesList.size()];
         for (int index=0; index<fBitInformation.bits.length; index++) {
            fBitInformation.bits[index]          = new BitInformationEntry();
            fBitInformation.bits[index].bitNum   = bitMappingList.get(index);
            fBitInformation.bits[index].bitName  = bitNamesList.get(index);
         }
         
         if (tDescriptions != null) {
            // Cases 3b
            if (tDescriptions.length>1) {
               // Case 3b
               for (int index=0; index<fBitInformation.bits.length; index++) {
                  if ((fBitInformation.bits[index].bitNum != null)||(fBitInformation.bits[index].bitNum < 0)) {
                     fBitInformation.bits[index].description = "";
                  }
                  else {
                     fBitInformation.bits[index].description =
                           tDescriptions[fBitInformation.bits[index].bitNum.intValue()].
                           replace("%i", fBitInformation.bits[index].bitNum.toString()).replace("%n", fBitInformation.bits[index].bitName);
                  }
               }
            }
         }
      }
      else {
         // Cases 1,2 & 4
         // Permitted bits are supplied => directly create bit mapping

         // Number of bit present in permittedBits
         int numberOfBitInUse = 0;

         // Create bit mapping from bit list
         // This maps the list of bit names & descriptions to a physical bit number
         ArrayList<Integer> bitMappingList = new ArrayList<Integer>();
         for (int index=0; index<32; index++) {
            long mask = (1L<<index);
            if (mask>fRawPermittedBits) {
               break;
            }
            if ((fRawPermittedBits&mask) != 0) {
               numberOfBitInUse++;
               bitMappingList.add(index);
            }
         }
         fBitInformation.bits = new BitInformationEntry[bitMappingList.size()];
         for (int index=0; index<fBitInformation.bits.length; index++) {
            fBitInformation.bits[index] = new BitInformationEntry();
            fBitInformation.bits[index].bitNum = bitMappingList.get(index);
         }

         String format = null;
         if (tBitnames == null) {
            // Case 4 use default pin format string
            format = "B%i";
         }
         else if (tBitnames.length == 1) {
            // Cases 1a, 1b - format provided
            format = tBitnames[0];
         }
         if (format != null) {
            // Cases 1a, 1b or 4
            // Expand names
            for (int index=0; index<fBitInformation.bits.length; index++) {
               if ((fBitInformation.bits[index].bitNum != null)&&(fBitInformation.bits[index].bitNum >= 0)) {
                  fBitInformation.bits[index].bitName = format.replace("%i", fBitInformation.bits[index].bitNum.toString());
               }
            }
         }
         else {
            // Cases 2a or 2b
            // Check there is a name for each bit
            if (tBitnames.length != fBitInformation.bits.length) {
               throw new Exception("# of bits in use doesn't match # of names provided:\n"
                     + "PermittedBits = " + fRawPermittedBits +"\n"
                     + "Number        = " + numberOfBitInUse +"\n"
                     + "Names         = " + Arrays.toString(tBitnames));
            }
            for (int index=0; index<fBitInformation.bits.length; index++) {
               fBitInformation.bits[index].bitName = tBitnames[index];
            }
         }
      }
      if ((tDescriptions != null) && (fBitInformation.bits[0].description == null)) {

         // Cases 1b,2b,2c & 3c

         if (tDescriptions.length>1) {
            // Cases 2b, 3b - explicit list
            // bitName and bitDescriptions must match in format
            if ((tDescriptions.length < fBitInformation.bits.length)) {
               throw new Exception("# of  bit descriptions does not match # of names:\n"
                     + "Desc   = " + Arrays.toString(tDescriptions) +"\n"
                     /*+ "Names  = " + Arrays.toString(fBitNames)*/);
            }
            for (int index=0; index<fBitInformation.bits.length; index++) {
               fBitInformation.bits[index].description = tDescriptions[index];
            }
         }
         else {
            // Cases 1b,2c,3c - pattern
            // Need to expand description pattern to match actual bits
            for (int index=0; index<fBitInformation.bits.length; index++) {
               if ((fBitInformation.bits[index].bitNum != null)&&(fBitInformation.bits[index].bitNum < 0)) {
                  fBitInformation.bits[index].description = "";
               }
               else {
                  fBitInformation.bits[index].description =
                        tDescriptions[0].replace("%i",
                              fBitInformation.bits[index].bitNum.toString()).replace("%n",fBitInformation.bits[index].bitName);
               }
            }
         }
      }
      // Have fBitNames & fBitMapping
      for (int index=0; index<fBitInformation.bits.length; index++) {
         if (fBitInformation.bits[index].bitName != null) {
            fBitInformation.bits[index].bitName     = fBitInformation.bits[index].bitName.trim();
         }
         if (fBitInformation.bits[index].description != null) {
            fBitInformation.bits[index].description = fBitInformation.bits[index].description.trim();
         }
      }
      
      if (isLogging()) {
         printDebugInfo();
         System.err.println("fPinMap = "+fPinMap);
      }
   }
   
   /**
    * Get information about bits with any expressions evaluated
    * 
    * @return Array describing each bit
    * @throws Exception
    */
   public BitInformation getFinalBitInformation() throws Exception {
      if (fBitInformation == null) {
         try {
            calculateValues();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      BitInformationEntry[] finalBitInfo = new BitInformationEntry[fBitInformation.bits.length];
      for (int index=0; index<fBitInformation.bits.length; index++) {
         finalBitInfo[index] = new BitInformationEntry();
         finalBitInfo[index].initialiseFrom(fBitInformation.bits[index]);
         if ((finalBitInfo[index].description!= null) &&
             (finalBitInfo[index].description.startsWith("@"))) {
            try {
               finalBitInfo[index].description =
                     Expression.getValueAsString(finalBitInfo[index].description.substring(1), getProvider());
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
         if (finalBitInfo[index].bitName.startsWith("@")) {
            try {
               finalBitInfo[index].bitName =
                     Expression.getValueAsString(finalBitInfo[index].bitName.substring(1), getProvider());
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
      return new BitInformation(finalBitInfo, fBitInformation.permittedBits);
   }
   
   /**
    * Get information about bits
    * 
    * @return Array describing each bit
    * @throws Exception
    */
   public BitInformation getBitInformation() {
      if (fBitInformation == null) {
         try {
            calculateValues();
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return fBitInformation;
   }
   
   @Override
   public String formatValueForRegister(String paramName) {
      
      String value;
      if (getTypeName() != null) {
         // Pretend it's an enum
         value = paramName;
      }
      else {
         value = super.formatValueForRegister(paramName);
      }
      if (useEnumClass()) {
         value = "uint32_t("+value+")";
      }
      return value;
   }

   @Override
   public String formatUsageValue(String value) {

      // Try to convert to long
      Long valueAsLong = null;
      try {
         valueAsLong = Long.parseLong(value);
      } catch (NumberFormatException e) {
      }
      BitInformationEntry[] bitInformation = getBitInformation().bits;
      if ((bitInformation != null)&&(valueAsLong != null)) {
         StringBuilder sb = new StringBuilder();
         int  bitsSetCount = 0;
         for (int index=0; index<bitInformation.length; index++) {
            if ((bitInformation[index].bitNum == null)||(bitInformation[index].bitNum < 0)) {
               // Not allocated
               continue;
            }
            if ((valueAsLong & (1L<<bitInformation[index].bitNum))==0) {
               // Special value
               continue;
            }
            if (!sb.isEmpty()) {
               sb.append("|\n\\t ");
            }
            if (useEnumClass()) {
               sb.append(getTypeName());
               sb.append("::");
            }
            sb.append(bitInformation[index].bitName);
            bitsSetCount++;
         }
         String result = sb.toString();
         if (bitsSetCount == 0) {
            result = getTypeName()+"(0)";
         }
         if (bitsSetCount>1) {
            result = "("+result+")";
         }
         return result;
      }

      String result = value;

      String format = getValueFormat();
      if (format != null) {
         result = String.format(format, result);
      }
      String typeName = getTypeName();
      if (typeName != null) {
         // Don't provides cast to (signed) int
         Pattern pSpecial = Pattern.compile("(signed(\\sint)?)|(int)");
         Matcher mSpecial = pSpecial.matcher(typeName);
         if (!mSpecial.matches()) {
            return typeName+"("+result+")";
         }
      }
      return result;
   }
   
   public void printDebugInfo() {
      BitInformationEntry[] bitInformation = getBitInformation().bits;
      for (int index=0; index<bitInformation.length; index++) {
         System.err.println(String.format("%2d: %-2d %-35s %-20s",
               index, bitInformation[index].bitNum, bitInformation[index].bitName,bitInformation[index].description));
      }
   }
   
   /**
    * Get definition value for selection choice
    * 
    * @param value The entry to use for formatted value
    * 
    * @return Formatted string suitable for enum definition e.g. <b>MPU_CESR_SPERR(1U<<3)</b>
    * 
    * @throws Exception
    */
   public String getDefinitionValue(BitInformationEntry bitInformationEntry) throws Exception {
      
      Integer bitNum = bitInformationEntry.bitNum;
      String hexValue;
      if (bitNum == null) {
         hexValue="--Not allocated--";
      }
      else if (bitNum == BIT_INDEX_ALL) {
         hexValue = "0x"+Long.toHexString(fBitInformation.permittedBits)+"U";
      }
      else if (bitNum == BIT_INDEX_NONE) {
         hexValue = "0x0U";
      }
      else {
         if ((bitInformationEntry.mask != null)&&(!bitInformationEntry.mask.isBlank())) {
            // Use supplied C mask
            hexValue = bitInformationEntry.mask;
         }
         else {
            // Create mask
            hexValue = "1U<<"+bitInformationEntry.bitNum;
         }
      }
      String format = getValueFormat();
      if ((format != null)&&(!format.isBlank())) {
         hexValue = String.format(format, hexValue);
      }
      if (isGenerateAsConstants()) {
         hexValue = getBaseType() + "(" + hexValue + ")";
      }
      return hexValue;
   }

   /**
    * Get definition value for selection choice
    * 
    * @param value The value to format
    * 
    * @return Formatted string suitable for enum definition etc.
    */
   public String getDefinitionValue(String value) {
      
      if (fLogging) {
         System.err.println("Logging");
      }
      String[] valueFormats = getValueFormat().split(",");
      String[] vals         = value.split(",");
      if (valueFormats.length != vals.length) {
         return ("valueFormat '"+getValueFormat()+"' does not match value '"+value+"'" );
      }
      StringBuilder sb = new StringBuilder();
      for(int valIndex=0; valIndex<valueFormats.length; valIndex++) {
         if (valIndex>0) {
            sb.append('|');
         }
         sb.append(String.format(valueFormats[valIndex], vals[valIndex]));
      }
      return sb.toString();
   }

   @Override
   public String getDefinitionValue() {
      return getDefinitionValue(getSubstitutionValue());
   }
   
   @Override
   public String getUsageValue() {

      BitInformation bitInformation = getBitInformation();
      if (bitInformation != null) {
         StringBuilder sb = new StringBuilder();
         long value  = getValueAsLong();
         int  countOfBitsSet = 0;
         for (int index=0; index<bitInformation.bits.length; index++) {
            if ((value & (1L<<bitInformation.bits[index].bitNum))!=0) {
               if (!sb.isEmpty()) {
                     sb.append("|\n\\t ");
               }
               sb.append(makeEnumWithPrefix(bitInformation.bits[index].bitName));
               countOfBitsSet++;
            }
         }
         String result = sb.toString();
         
         if (countOfBitsSet == 0) {
            // Create dummy value
            result = getTypeName()+"(0)";
         }
         else if ((countOfBitsSet > 1)&& (!getGenerateOperators())) {
            result = getTypeName()+"("+result+")";
         }
         return result;
      }
      

      String result = getSubstitutionValue();

      String format = getValueFormat();
      if (format != null) {
         result = String.format(format, result);
      }
      String typeName = getTypeName();
      if (typeName != null) {
         // Don't provides cast to (signed) int
         Pattern pSpecial = Pattern.compile("(signed(\\sint)?)|(int)");
         Matcher mSpecial = pSpecial.matcher(typeName);
         if (!mSpecial.matches()) {
            return typeName+"("+result+")";
         }
      }
      return result;
   }

   @Override
   public String fieldExtractFromRegister(String registerValue) {
    String valueFormat = getValueFormat();
    String returnType  = getReturnType();
    boolean hasOuterBrackets = false;
    if ((valueFormat != null) && !valueFormat.matches("^\\(?%s\\)?$")) {
       String parts[] = valueFormat.split(",");
       StringBuilder sb = new StringBuilder();
       boolean needsBrackets = false;
       for(String format:parts) {
          Pattern p = Pattern.compile("^([a-zA-Z0-9_]*)\\(?\\%s\\)?");
          Matcher m = p.matcher(format);
          if (!m.matches()) {
             return "Illegal use of formatParam - unexpected pattern '"+valueFormat+"'";
          }
          String macro = m.group(1);
          boolean isNumeric = Character.isDigit(macro.charAt(0));
          if (!sb.isEmpty()) {
             sb.append("|");
             needsBrackets = true;
          }
          sb.append(String.format("((%s&%s%s))",registerValue, macro, isNumeric?"":"_MASK"));
       }
       registerValue = sb.toString();
       if (needsBrackets) {
          registerValue = "("+registerValue+")";
       }
       hasOuterBrackets = true;
    }
    if (returnType != null) {
       if (!hasOuterBrackets) {
          registerValue = "("+registerValue+")";
       }
       registerValue = String.format("%s%s", returnType, registerValue);
    }
    return registerValue;
 }

   /**
    * Get the bitmask associated with this bitField
    * @return
    */
   public long getBitmask() {
      return fRawPermittedBits;
   }
   
   /**
    * Convert an enum value into a complete enum for code use
    * 
    * @param enumValue
    * 
    * @return Converted value e.g. Disable => LowPower_Disabled
    */
   public String makeEnumWithPrefix(String enumValue) {
      enumValue = ParseMenuXML.makeSafeIdentifierName(enumValue);
      String prefix =  getEnumPrefix();
      if (useEnumClass()) {
         prefix = prefix + "::";
      }
      else {
         prefix = prefix + "_";
      }
      if ((prefix == null) || (enumValue == null)) {
         return null;
      }
      return prefix+enumValue;
   }

   
}
