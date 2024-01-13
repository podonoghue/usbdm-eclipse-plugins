package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.usbdm.deviceEditor.information.PinListExpansion.PinMap;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;
import net.sourceforge.usbdm.deviceEditor.model.BitmaskVariableModel;
import net.sourceforge.usbdm.deviceEditor.model.ObservableModelInterface;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.Expression.VariableUpdateInfo;

public class BitmaskVariable extends LongVariable {

   // Pin mapping information
   private String fPinMap;
   
   // Permitted bits
   private long fPermittedBits = 0xFFFFFFFF;
   
   // Expanded list of bit names
   private String[] fBitNames;

   // Expanded descriptions of bits
   private String[] fDescriptions;

   // This maps the index in array of bit names & descriptions to a physical bit number
   // Index may be -ve to indicate no mapping (used in case 3 for dummy bits)
   private Integer[] fBitMapping;

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
   public BitmaskVariable(String name, String key) throws Exception {
      super(name, key);
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
         if (info.doFullUpdate || info.properties.contains(ObservableModelInterface.PROP_VALUE[0])) {
            updatePinMap(getValueAsLong());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
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
   public void init(long permittedBits, String bitNames, String bitDescriptions) throws Exception {

      fDescriptions = null;

      String[] tBitnames     = PinListExpansion.expandPinList(bitNames, ",");
      String[] tDescriptions = PinListExpansion.expandPinList(bitDescriptions, ",");
      
      if (permittedBits == 0) {
         // Case 3 - Bitmask=0, Names must be provided with skipped bits
         // Determine bitmask and indices from bit-names
         if (tBitnames == null) {
            throw new Exception("Either a bitList or bitNames must be provides");
         }

         ArrayList<Integer> bitMappingList     = new ArrayList<Integer>();
         ArrayList<String>  bitNamesList       = new ArrayList<String>();

         for (int index=0; index<tBitnames.length; index++) {
            long mask = (1L<<index);
            if (!tBitnames[index].isBlank()) {
               permittedBits |= mask;
               bitMappingList.add(index);
               bitNamesList.add(tBitnames[index]);
            }
         }
         fBitNames   = bitNamesList.toArray(new String[bitNamesList.size()]);
         fBitMapping = bitMappingList.toArray(new Integer[bitMappingList.size()]);
         
         if (tDescriptions != null) {
            // Cases 3b & 3c
            if (tDescriptions.length>1) {
               // Case 3b
               fDescriptions = new String[fBitNames.length];
               for (int index=0; index<fBitNames.length; index++) {
                  if (fBitMapping[index] < 0) {
                     fDescriptions[index] = "";
                  }
                  else {
                     fDescriptions[index] =
                           tDescriptions[fBitMapping[index]].replace("%i", fBitMapping[index].toString()).replace("%n", fBitNames[index]);
                  }
               }
            }
//            else {
//               // Case 3c
//               // Need to expand description pattern to match actual bits
//               fDescriptions = new String[fBitNames.length];
//               for (int index=0; index<fBitNames.length; index++) {
//                  if (fBitMapping[index] < 0) {
//                     fDescriptions[index] = "";
//                  }
//                  else {
//                     fDescriptions[index] =
//                           tDescriptions[0].replace("%i", fBitMapping[index].toString()).replace("%n", fBitNames[index]);
//                  }
//               }
//            }
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
            if (mask>permittedBits) {
               break;
            }
            if ((permittedBits&mask) != 0) {
               numberOfBitInUse++;
               bitMappingList.add(index);
            }
         }
         fBitMapping = bitMappingList.toArray(new Integer[bitMappingList.size()]);

         String format = null;
         if (tBitnames == null) {
            // Case 4 use default pin format string
            format = "B%i";
         }
         else if (tBitnames.length == 1) {
            // Cases 1b, 2c - format provided
            format = tBitnames[0];
         }
         if (format != null) {
            // Cases 1b, 2c or 4
            // Expand names

            ArrayList<String> bitNamesList = new ArrayList<String>();
            for (int index=0; index<fBitMapping.length; index++) {
               if (fBitMapping[index] >= 0) {
                  bitNamesList.add(format.replace("%i", fBitMapping[index].toString()));
               }
            }
            fBitNames = bitNamesList.toArray(new String[bitNamesList.size()]);
         }
         else {
            // Cases 2b
            // Should be a name for each bit
            if (tBitnames.length != fBitMapping.length) {
               throw new Exception("# of bits in use doesn't match # of names provided:\n"
                     + "PermittedBits = " + permittedBits +"\n"
                     + "Number        = " + numberOfBitInUse +"\n"
                     + "Names         = " + Arrays.toString(tBitnames));
            }
            fBitNames = tBitnames;
         }
      }
      if ((tDescriptions != null) && (fDescriptions == null)) {

         // Cases 1b,2b,2c & 3c

         if (tDescriptions.length>1) {
            // Cases 2b, 3b - explicit list
            // bitName and bitDescriptions must match in format
            if ((tDescriptions.length != tBitnames.length)) {
               throw new Exception("# of bit desciptions does not match # of names:\n"
                     + "Desc   = " + Arrays.toString(tDescriptions) +"\n"
                     + "Names  = " + Arrays.toString(fBitNames));
            }
            fDescriptions = tDescriptions;
         }
         else {
            // Cases 1b,2c,3c - pattern
            // Need to expand description pattern to match actual bits
            fDescriptions = new String[fBitNames.length];
            for (int index=0; index<fBitNames.length; index++) {
               if (fBitMapping[index] < 0) {
                  fDescriptions[index] = "";
               }
               else {
                  fDescriptions[index] =
                        tDescriptions[0].replace("%i", fBitMapping[index].toString()).replace("%n", fBitNames[index]);
               }
            }
         }
      }
      for (int index=0; index<fBitNames.length; index++) {
         fBitNames[index] = fBitNames[index].trim();
      }
      // Have fBitNames & fBitMapping

      if (fDescriptions != null) {
         for (int index=0; index<fDescriptions.length; index++) {
            fDescriptions[index] = fDescriptions[index].trim();
         }
      }
   }
   
   /**
    * Gets calculated bit-mask of permitted bit values
    * 
    * @return
    */
   public long getPermittedBits() {
      return fPermittedBits;
   }

   /**
    * Get expanded list of bit names
    * 
    * @return
    */
   public String[] getBitNames() {
      return fBitNames;
   }

   /**
    * Get expanded descriptions of bits
    * 
    * @return
    */
   public String[] getBitDescriptions() {
      return fDescriptions;
   }
     
   /**
    * Get bit mapping
    * 
    * @return
    */
   public Integer[] getBitMapping() {
      return fBitMapping;
   }

   @Override
   public String formatValueForRegister(String paramName) {
      
      if (getTypeName() != null) {
         // Pretend it's an enum
         return paramName;
      }
      return super.formatValueForRegister(paramName);
   }

   @Override
   public String getUsageValue() {

      
      String value = getSubstitutionValue();
      
      String format = getValueFormat();
      if (format != null) {
         value = String.format(format, value);
      }
      String typeName = getTypeName();
      if (typeName != null) {
         // Don't provides cast to (signed) int
         Pattern pSpecial = Pattern.compile("(signed(\\sint)?)|(int)");
         Matcher mSpecial = pSpecial.matcher(typeName);
         if (!mSpecial.matches()) {
            return typeName+"("+value+")";
         }
      }
      return value;
   }

}
