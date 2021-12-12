package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class to initialise multiple PCRs efficiently
 */
public class PcrInitialiser {
   
   /** Set of Port clock masks for used ports */
   private TreeSet<String> portClockMasks = new TreeSet<String>();
   
   /** HashMap of Ports to PCR values and pins used */
   private TreeMap<String, TreeMap<String, Long>> portToPcrValuesMap  = new TreeMap<String, TreeMap<String, Long>>();

   /**
    * Constructor
    */
   public PcrInitialiser() {
   }
   
   /**
    * Adds information required to set up the PCR associated with the given signal<br>
    * Note that only the lower 16-bits of the PCR value are used.
    * 
    * @param signal     Signal to process
    */
   public void addSignal(Signal signal) {

      MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
      if (mappingInfo == MappingInfo.UNASSIGNED_MAPPING) {
         return;
      }
      if (!mappingInfo.getMux().isMappedValue()) {
         return;
      }
      Pin pin = mappingInfo.getPin();
      if (pin.getPort() == null) {
         // Fixed port mapping
         return;
      }
      String pcrValue = longTo4Hex(mappingInfo.getProperties());
      portClockMasks.add(pin.getPort());
      String bitNums = pin.getGpioBitNum();
      if (bitNums != null) {
         long bitNum = Long.parseLong(bitNums);
         TreeMap<String, Long> pcrValueToBitsMap = portToPcrValuesMap.get(pin.getPort());
         if (pcrValueToBitsMap == null) {
            pcrValueToBitsMap = new TreeMap<String, Long>();
            portToPcrValuesMap.put(pin.getPort(), pcrValueToBitsMap);
         }
         Long bitMask = pcrValueToBitsMap.get(pcrValue);
         if (bitMask == null) {
            bitMask = (long) 0;
         }
         bitMask |= 1<<bitNum;
         pcrValueToBitsMap.put(pcrValue, bitMask);
      }
   }
   
   public static String longTo4Hex(long value) {
      return String.format("0x%04XUL", 0xFFFFL&value);
   }
   
   /**
    * Adds information required to set up the PCR associated with the given pin mapping.<br>
    * Note that only the lower 16-bits of the PCR value are used.
    * 
    * @param pin  Pin mapping to examine
    */
   public void addPinMapping(MappingInfo mappingInfo) {
      if (mappingInfo == MappingInfo.UNASSIGNED_MAPPING) {
         return;
      }
      if (!mappingInfo.getMux().isMappedValue()) {
         // No PCR for this mapping
         return;
      }
      String pcrValue = longTo4Hex(mappingInfo.getProperties());
      Pin pin = mappingInfo.getPin();
      
      portClockMasks.add(pin.getPort());
      String bitNumString = pin.getGpioBitNum();
      if (bitNumString != null) {
         long bitNum = Long.parseLong(bitNumString);
         TreeMap<String, Long> pcrValueToBitsMap = portToPcrValuesMap.get(pin.getPort());
         if (pcrValueToBitsMap == null) {
            pcrValueToBitsMap = new TreeMap<String, Long>();
            portToPcrValuesMap.put(pin.getPort(), pcrValueToBitsMap);
         }
         Long bitMask = pcrValueToBitsMap.get(pcrValue);
         if (bitMask == null) {
            bitMask = (long) 0;
         }
         bitMask |= 1<<bitNum;
         pcrValueToBitsMap.put(pcrValue, bitMask);
      }
   }
   
   /**
    * Adds information required to set up the PCR associated with the given pin.<br>
    * Note that only the lower 16-bits of the PCR value are used.<br>
    * It obtains information from the associated MappingInfos and hence signals.<br>
    * If more than one signal has been mapped to the pin then no setting is created for that pin.
    *  
    * @param pin  Pin to examine
    * 
    * @return Error description if the pin was not added due to multiple definition
    */
   public String addPin(Pin pin) {

      if (!pin.isAvailableInPackage()) {
         // Discard unmapped signal on this package 
         return null;
      }
      ArrayList<MappingInfo> mappedSignals = pin.getActiveMappings();
      if (mappedSignals.size()==0) {
         // No signals mapped to pin
         return null;
      }
      if (mappedSignals.size()>1) {
         // Multiply signals on mapped to this pin
         return "Multiple signals mapped to pin - " + pin.getMappedSignalNames();
      }
      MappingInfo mappingInfo = mappedSignals.get(0);
      if (!mappingInfo.getMux().isMappedValue()) {
         // No PCR for this mapping
         return null;
      }
      String pcrValue = longTo4Hex(mappingInfo.getProperties());

      portClockMasks.add(pin.getPort());
      String bitNums = pin.getGpioBitNum();
      if (bitNums != null) {
         long bitNum = Long.parseLong(bitNums);
         TreeMap<String, Long> pcrValueToBitsMap = portToPcrValuesMap.get(pin.getPort());
         if (pcrValueToBitsMap == null) {
            pcrValueToBitsMap = new TreeMap<String, Long>();
            portToPcrValuesMap.put(pin.getPort(), pcrValueToBitsMap);
         }
         Long bitMask = pcrValueToBitsMap.get(pcrValue);
         if (bitMask == null) {
            bitMask = (long) 0;
         }
         bitMask |= 1<<bitNum;
         pcrValueToBitsMap.put(pcrValue, bitMask);
      }
      return null;
   }
   
   /**
    * Get string to clear the referenced PCRs
    * 
    * @return
    */
   public String getPcrClearStatements(String indent) {
      
      StringBuffer sb = new StringBuffer();

      // For each port
      for (String port:portToPcrValuesMap.keySet()) {
         Long bits = 0L;
         
         // For each PCR value
         TreeMap<String, Long> pcrToBitsMap = portToPcrValuesMap.get(port);
         for (String pcrValue:pcrToBitsMap.keySet()) {
            // Merge all bits on this port
            bits |= pcrToBitsMap.get(pcrValue);
         }
         if ((bits&0xFFFF) != 0) {
            sb.append(String.format(indent+"      %s = PORT_PCR_MUX(%d)|PORT_GPCLR_GPWE(0x%sU);\n", port+"->GPCLR", 0, Long.toHexString(bits&0xFFFF).toUpperCase()));
         }
         bits >>= 16;
         if ((bits&0xFFFF) != 0) {
            sb.append(String.format(indent+"      %s = PORT_PCR_MUX(%d)|PORT_GPCHR_GPWE(0x%sU);\n", port+"->GPCHR", 0, Long.toHexString(bits&0xFFFF).toUpperCase()));
         }
      }
      return sb.toString();
   }

   /**
    * Get string to clear the referenced PCRs
    * 
    * @return
    */
   public String getPcrInitStatements(String indent) {
      
      StringBuffer sb = new StringBuffer();

      // For each port
      for (String port:portToPcrValuesMap.keySet()) {
         // For each PCR value
         TreeMap<String, Long> pcrToBitsMap = portToPcrValuesMap.get(port);
         for (String pcrValue:pcrToBitsMap.keySet()) {
//          String pcrValue = "0x"+Long.toHexString(pin.getPcrValue());

            // Bits that share this PCR value on this port
            Long bits = pcrToBitsMap.get(pcrValue);
            
            if ((bits&0xFFFF) != 0) {
               sb.append(String.format(indent+"      %s = %s|PORT_GPCLR_GPWE(%s);\n", port+"->GPCLR", pcrValue, longTo4Hex(bits&0xFFFF)));
            }
            bits >>= 16;
            if ((bits&0xFFFF) != 0) {
               sb.append(String.format(indent+"      %s = %s|PORT_GPCHR_GPWE(%s);\n", port+"->GPCHR", pcrValue, longTo4Hex(bits&0xFFFF)));
            }
         }
      }
      return sb.toString();
   }
   
   /**
    * Get string to initialise port clocks e.g.<br><br>
    * 
    * <code>
    *    <b>enablePortClocks(PORTA_CLOCK_MASK|PORTE_CLOCK_MASK);</b>
    * </code>
    * 
    * @param indent 
    * @return 
    */
   public String getInitPortClocksStatement(String indent) {
      if (portClockMasks.isEmpty()) {
         return "";
      }
      StringBuffer sb = new StringBuffer();
      sb.append("#ifdef PCC_PCCn_CGC_MASK\n");
      for (String p:portClockMasks) {
         if (p == null) {
            continue;
         }
         sb.append(indent+String.format("      PCC->PCC_%s = PCC_PCCn_CGC_MASK;\n", p));
      }
      sb.append("#else\n");
      boolean isFirst = true;
      for (String p:portClockMasks) {
         if (p == null) {
            continue;
         }
         if (isFirst) {
            sb.append(indent+"      enablePortClocks(");
            isFirst = false;
         }
         else {
            sb.append("|");
         }
         sb.append(String.format("%s_CLOCK_MASK", p));
      }
      if (!isFirst) {
         sb.append(");\n");
      }
      sb.append("#endif\n");
      
      return sb.toString();
   }
}