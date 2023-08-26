package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class to initialise multiple PCRs efficiently
 */
public class PcrInitialiser {
   
   /** Set of ports needed for PCR access */
   private TreeSet<String> portsNeeded = new TreeSet<String>();
   
   /** HashMap of Ports to PCR values and pins to initialise */
   private TreeMap<String, TreeMap<String, Long>> portToPcrValuesMap  = new TreeMap<String, TreeMap<String, Long>>();

   /** HashMap of Ports to pins that are not in use for locking out */
   private TreeMap<String, Long> portToUnusedPinsMap  = new TreeMap<String, Long>();

   /** Accumulates error messages as pins are added */
   private StringBuilder fErrorMessages = new StringBuilder();
   
   /**
    * Constructor
    */
   public PcrInitialiser() {
   }
   
   /**
    * Format value as a 4 hex digit C literal unsigned long e.g. <b>0x00F4UL</b>
    * 
    * @param value   Value to convert
    * 
    * @return String representing a C unsigned long literal
    */
   public static String longTo4Hex(long value) {
      return String.format("0x%04XUL", 0xFFFFL&value);
   }
   
   /**
    * Format value as a 5 hex digit C literal unsigned long e.g. <b>0x00CD5UL</b>
    * 
    * @param value   Value to convert
    * 
    * @return String representing a C unsigned long literal
    */
   public static String longTo5Hex(long value) {
      return String.format("0x%05XUL", 0xFFFFFFL&value);
   }
   
   /**
    * Adds information required to set up the PCR associated with the given signal
    * 
    * @param signal     Signal to process
    * 
    * @return Error description if the signal was not added due to multiple mappings
    * 
    * @note Only the lower 16-bits of the PCR value are used.
    */
   public String addSignal(Signal signal) {

      MappingInfo mappingInfo = signal.getFirstMappedPinInformation();
      if (mappingInfo == MappingInfo.UNASSIGNED_MAPPING) {
         // No mapped to pin
         return null;
      }
      if (!mappingInfo.getMux().isMappedValue()) {
         // No PCR as fixed mapping
         return null;
      }
      
      Pin pin = mappingInfo.getPin();
      if (pin.getPort() == null) {
         // No associated port so no PCR
         return null;
      }
      
      // Associated port
      String port = pin.getPort();

      String bitNumStr = pin.getGpioBitNum();
      if (bitNumStr == null) {
         // No associated port for this pin
         return null;
      }
      long bitNum = Long.parseLong(bitNumStr);

      portsNeeded.add(port);

      String pcrValue = longTo4Hex(mappingInfo.getPcr());
      TreeMap<String, Long> pcrValueToBitsMap = portToPcrValuesMap.get(port);
      if (pcrValueToBitsMap == null) {
         // Create new entry for port
         pcrValueToBitsMap = new TreeMap<String, Long>();
         portToPcrValuesMap.put(port, pcrValueToBitsMap);
      }
      
      // Get shared entry for shared pcrValue being applied to this port
      Long bitMask = pcrValueToBitsMap.get(pcrValue);
      if (bitMask == null) {
         // 1st use of this PCR value on this port - create new entry
         bitMask = 0L;
      }
      
      // Add this bit for initialisation
      bitMask |= 1<<bitNum;
      
      // Update/add entry
      pcrValueToBitsMap.put(pcrValue, bitMask);
      
      return null;
   }
   
   /**
    * Adds information required to set up the PCR associated with the given pin.<br>
    * It obtains information from the associated MappingInfos and hence signals.<br>
    * If more than one signal has been mapped to the pin then no setting is created for that pin.
    * 
    * @param pin  Pin to examine
    * 
    * @return Error description if the pin was not added due to multiple mappings
    * 
    * @note Only the lower 16-bits of the PCR value are used.
    */
   public String addPin(Pin pin) {

      String bitNumStr = pin.getGpioBitNum();
      if (bitNumStr == null) {
         // No associated port for this pin
         return null;
      }
      
      // Bit number in associated port
      long bitNum = Long.parseLong(bitNumStr);
      
      // Associated port
      String port = pin.getPort();
      
      // Record port as accessed (for clock enable)
      portsNeeded.add(port);

      // Unused pins in associated port
      Long unusedPinsInPort = portToUnusedPinsMap.get(port);
      if (unusedPinsInPort == null) {
         // All unused at start
         unusedPinsInPort = 0xFFFFFFFFL;
         portToUnusedPinsMap.put(pin.getPort(), unusedPinsInPort);
      }
      
      if (!pin.isAvailableInPackage()) {
         // Unmapped signal on this package - leave as unused pin
         portToUnusedPinsMap.put(pin.getPort(), unusedPinsInPort);
         return null;
      }
      
      // Remove as unused pin
      unusedPinsInPort &= ~(1<<bitNum);
      portToUnusedPinsMap.put(pin.getPort(), unusedPinsInPort);
      
      ArrayList<MappingInfo> mappedSignals = pin.getActiveMappings();
      if (mappedSignals.size()==0) {
         // No signals mapped to pin
         return null;
      }
      if (mappedSignals.size()>1) {
         // Multiply signals on mapped to this pin
         String errorMessage = "Multiple signals mapped to pin - " + pin.getMappedSignalNames();
         fErrorMessages.append(String.format("#warning \"PCR Not initialised for %-10s : %s\"\n", pin.getName(), errorMessage));
         return errorMessage;
      }
      MappingInfo mappingInfo = mappedSignals.get(0);
      if (!mappingInfo.getMux().isMappedValue()) {
         // No PCR for this mapping
         // Can't happen as already check for associated port/PCR
         return null;
      }
      // Get Information for associated port
      TreeMap<String, Long> pcrValueToBitsMap = portToPcrValuesMap.get(port);
      if (pcrValueToBitsMap == null) {
         // Create new entry for port
         pcrValueToBitsMap = new TreeMap<String, Long>();
         portToPcrValuesMap.put(port, pcrValueToBitsMap);
      }
      String pcrValue = longTo4Hex(mappingInfo.getPcr());

      // Get shared entry for shared pcrValue being applied to this port
      Long bitMask = pcrValueToBitsMap.get(pcrValue);
      if (bitMask == null) {
         // 1st use of this PCR value on this port - create new entry
         bitMask = (long) 0;
      }
      // Add this bit for initialisation
      bitMask |= 1<<bitNum;
      
      // Update/add entry
      pcrValueToBitsMap.put(pcrValue, bitMask);
      
      return null;
   }
   
   /**
    * Get C code to clear the referenced PCRs<br><br>
    * 
    * Example:
    * <pre>
    *       PORTB->GPCLR = PORT_PCR_MUX(0)|PORT_GPCLR_GPWE(0x0FCFUL);
    *       ...
    *       PORTD->GPCLR = PORT_PCR_MUX(0)|PORT_GPCLR_GPWE(0x00BCUL);
    * </pre>
    * 
    * @param indent Indenting to use
    * 
    * @return Formatted string
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
            sb.append(String.format(indent+"%s = PinMux_Disabled|PORT_GPCLR_GPWE(%s);\n", port+"->GPCLR", longTo4Hex(bits&0xFFFF)));
         }
         bits = Long.rotateRight(bits, 16);
         if ((bits&0xFFFF) != 0) {
            sb.append(String.format(indent+"%s = PinMux_Disabled|PORT_GPCHR_GPWE(%s);\n", port+"->GPCHR", longTo4Hex(bits&0xFFFF)));
         }
      }
      return sb.toString();
   }

   /**
    * Get C code to initialise the referenced PCRs<br><br>
    * 
    * Example:
    * <pre>
    *       PORTB->GPCLR = <i><b>ForceLockedPins</i></b>|0x0500UL|PORT_GPCLR_GPWE(0x0FCFUL);
    *       ...
    *       PORTD->GPCLR = <i><b>ForceLockedPins</i></b>|0x0500UL|PORT_GPCLR_GPWE(0x00BCUL);
    * </pre>
    * 
    * @param indent     Indenting to use
    * @param global     Indicates generating global initialisation (includes <i><b>ForceLockedPins</i></b> prefix)
    * 
    * @return Formatted string
    */
   private String getPcrInitStatements(String indent, boolean global) {
      
      String prefix = "";
      if (global) {
         prefix = "ForceLockedPins|";
      }
      StringBuffer sb = new StringBuffer();

      // For each port
      for (String port:portToPcrValuesMap.keySet()) {

         // For each PCR value
         TreeMap<String, Long> pcrToBitsMap = portToPcrValuesMap.get(port);
         for (String pcrValue:pcrToBitsMap.keySet()) {
            //          String pcrValue = "0x"+Long.toHexString(pin.getPcrValue());

            // Bits that share this PCR value on this port
            Long bits   = pcrToBitsMap.get(pcrValue);
            if (bits == null) {
               continue;
            }
            if ((bits&0xFFFFL) != 0) {
               sb.append(String.format(indent+"%s = %s|PORT_GPCLR_GPWE(%s);\n", port+"->GPCLR", prefix+pcrValue, longTo4Hex(bits&0xFFFF)));
            }
            bits = Long.rotateRight(bits, 16);
            if ((bits&0xFFFFL) != 0) {
               sb.append(String.format(indent+"%s = %s|PORT_GPCHR_GPWE(%s);\n", port+"->GPCHR", prefix+pcrValue, longTo4Hex(bits&0xFFFF)));
            }
         }
      }
      return sb.toString();
   }
   
   /**
    * Get C code to initialise the referenced PCRs<br><br>
    * 
    * Example:
    * <pre>
    *       PORTB->GPCLR = 0x0500UL|PORT_GPCLR_GPWE(0x0FCFUL);
    *       ...
    *       PORTD->GPCHR = 0x0500UL|PORT_GPCHR_GPWE(0x00BCUL);
    * </pre>
    * 
    * @param indent Indenting to use
    * 
    * @return Formatted string
    */
   public String getPcrInitStatements(String indent) {
      return getPcrInitStatements(indent, false);
   }
      
   /**
    * Get C code to initialise the referenced PCRs<br><br>
    * 
    * Example:
    * <pre>
    *       PORTB->GPCLR = ForceLockedPins|0x0500UL|PORT_GPCLR_GPWE(0x0FCFUL);
    *       ...
    *       PORTD->GPCHR = ForceLockedPins|0x0500UL|PORT_GPCHR_GPWE(0x00BCUL);
    *       ...
    * </pre>
    * 
    * @param indent Indenting to use
    * 
    * @return Formatted string
    */
   public String getGlobalPcrInitStatements(String indent) {
      return getPcrInitStatements(indent, true);
   }
   
   /**
    * Get C code to lockout unused pins<br><br>
    * 
    * Example:
    * <pre>
    *       PORTB->GPCLR = PinLock_Locked |0x0000UL|PORT_GPCLR_GPWE(0xC0F0UL); // Lockout unavailable pins
    *       ...
    *       PORTE->GPCHR = PinLock_Locked |0x0000UL|PORT_GPCHR_GPWE(0xFF00UL); // Lockout unavailable pins
    * </pre>
    * 
    * @param indent Indenting to use
    * 
    * @return Formatted string
    */
   public String getGlobalPcrLockoutStatements(String indent) {

      StringBuffer sb = new StringBuffer();

      for (String port:portToUnusedPinsMap.keySet()) {

         // For each port
         Long unusedPinsInPort = portToUnusedPinsMap.get(port);
         if ((unusedPinsInPort&0xFFFFL) != 0) {
            sb.append(String.format(indent+"%s = %s|PORT_GPCLR_GPWE(%s); // Lockout unavailable pins\n",
                  port+"->GPCLR", "PinLock_Locked |0x0000UL", longTo4Hex(unusedPinsInPort&0xFFFF)));
         }
         unusedPinsInPort = Long.rotateRight(unusedPinsInPort, 16);
         if ((unusedPinsInPort&0xFFFFL) != 0) {
            sb.append(String.format(indent+"%s = %s|PORT_GPCHR_GPWE(%s); // Lockout unavailable pins\n",
                  port+"->GPCHR", "PinLock_Locked |0x0000UL", longTo4Hex(unusedPinsInPort&0xFFFF)));
         }
      }
      return sb.toString();
   }
   
   /**
    * Get C code to enable port clocks<br><br>
    * 
    * Example:
    * <pre>
    * <b>#if</b> defined(PCC_PCCn_CGC_MASK)
    *       PCC->PCC_PORTB = PCC_PCCn_CGC_MASK;
    *       ...
    *       PCC->PCC_PORTD = PCC_PCCn_CGC_MASK;
    * <b>#else</b>
    *       enablePortClocks(PORTB_CLOCK_MASK|...|PORTD_CLOCK_MASK);
    * <b>#endif</b>
    * </pre>
    * 
    * @param indent Indenting to use
    * 
    * @return Formatted string
    */
   public String getEnablePortClocksStatement(String indent) {
      if (portsNeeded.isEmpty()) {
         return "";
      }
      StringBuffer sb = new StringBuffer();
      sb.append("\n#if defined(PCC_PCCn_CGC_MASK)\n");
      for (String port:portsNeeded) {
         if (port == null) {
            continue;
         }
         sb.append(indent+String.format("PCC->PCC_%s = PCC_PCCn_CGC_MASK;\n", port));
      }
      sb.append("#else\n");
      boolean isFirst = true;
      for (String port:portsNeeded) {
         if (port == null) {
            continue;
         }
         if (isFirst) {
            sb.append(indent+"enablePortClocks(");
            isFirst = false;
         }
         else {
            sb.append("|");
         }
         sb.append(String.format("USBDM::%s_CLOCK_MASK", port));
      }
      if (!isFirst) {
         sb.append(");\n");
      }
      sb.append("#endif\n\n");
      
      return sb.toString();
   }
   /**
    * Get C code to disable port clocks<br><br>
    * 
    * Example:
    * <pre>
    * <b>#if</b> defined(PCC_PCCn_CGC_MASK)
    *       PCC->PCC_PORTB = PCC_PCCn_CGC_MASK;
    *       ...
    *       PCC->PCC_PORTD = PCC_PCCn_CGC_MASK;
    * <b>#else</b>
    *       enablePortClocks(PORTB_CLOCK_MASK|...|PORTD_CLOCK_MASK);
    * <b>#endif</b>
    * </pre>
    * 
    * @param indent Indenting to use
    * 
    * @return Formatted string
    */
   public String getDisablePortClocksStatement(String indent) {
      if (portsNeeded.isEmpty()) {
         return "";
      }
      StringBuffer sb = new StringBuffer();
      sb.append("#if defined(PCC_PCCn_CGC_MASK)\n");
      for (String p:portsNeeded) {
         if (p == null) {
            continue;
         }
         sb.append(indent+String.format("      PCC->PCC_%s = PCC_PCCn_CGC_MASK;\n", p));
      }
      sb.append("#else\n");
      boolean isFirst = true;
      for (String port:portsNeeded) {
         if (port == null) {
            continue;
         }
         if (isFirst) {
            sb.append(indent+"      enablePortClocks(");
            isFirst = false;
         }
         else {
            sb.append("|");
         }
         sb.append(String.format("%s_CLOCK_MASK", port));
      }
      if (!isFirst) {
         sb.append(");\n");
      }
      sb.append("#endif\n");
      
      return sb.toString();
   }

   /**
    * Get error messages about pins or signals added
    * 
    * @return String suitable for inclusion in C code
    */
   public String getErrorMessages() {
      return fErrorMessages.toString();
   }

}