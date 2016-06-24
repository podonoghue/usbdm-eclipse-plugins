package net.sourceforge.usbdm.deviceEditor.information;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Class to initialise multiple PCRs efficiently
 */
public class PcrInitialiser {
   
   /** Set of Port clock masks for used ports */
   private HashSet<String> portClockMasks = new HashSet<String>();
   
   /** HashMap of Ports to PCR values and pins used */
   private HashMap<String, ArrayList<HashMap<String, Long>>> portMap  = new HashMap<String, ArrayList<HashMap<String, Long>>>();

   /**
    * Constructor
    * 
    * @param deviceInfo Where to get information about the device
    */
   public PcrInitialiser(DeviceInfo deviceInfo) {
   }
   
   /**
    * 
    * @param signal
    */
   public void addSignal(Signal signal, String pcrValue) {

      MappingInfo mappingInfo = signal.getMappedPin();

      Pin pin = mappingInfo.getPin();
      if (!pin.isAvailableInPackage()) {
         // Discard unmapped signals on this package 
         return;
      }
      MuxSelection mux = pin.getMuxValue();
      if (!mux.isMappedValue()) {
         // Skip unmapped pin
         return;
      }
      if (mappingInfo.isSelected()) {
         portClockMasks.add(pin.getClockMask());

         String bitNums = pin.getGpioBitNum();
         if (bitNums != null) {
            long bitNum = Long.parseLong(bitNums);
            ArrayList<HashMap<String, Long>> muxValues = portMap.get(pin.getPORTBasePtr());
            if (muxValues == null) {
               muxValues = new ArrayList<HashMap<String, Long>>(Collections.nCopies(8, (HashMap<String, Long>)null));
               portMap.put(pin.getPORTBasePtr(), muxValues);
            }
            HashMap<String, Long> pcrMap = muxValues.get(mux.value);
            if (pcrMap == null) {
               pcrMap = new HashMap<String, Long>();
               muxValues.set(mux.value, pcrMap);
            }
            Long bitMask = pcrMap.get(pcrValue);
            if (bitMask == null) {
               bitMask = new Long(0);
            }
            bitMask |= 1<<bitNum;
            pcrMap.put(pcrValue, bitMask);
         }
      }
   }
   
   /**
    * 
    * @param pin
    * @throws Exception 
    */
   public void addPin(Pin pin, String pcrValue) throws Exception {

      if (!pin.isAvailableInPackage()) {
         // Discard unmapped signals on this package 
         return;
      }
      MuxSelection mux = pin.getMuxValue();
      if (!mux.isMappedValue()) {
         // Skip unmapped pin
         return;
      }
      
      MappingInfo mapping = pin.getMappedSignal();
      
      if (pcrValue == null) {
         for (Signal sig:mapping.getSignals()) {
            Peripheral peripheral = sig.getPeripheral();
            if (peripheral == null) {
               continue;
            }
            String pinPcrValue = peripheral.getPcrValue(sig);
            if (pinPcrValue != null) {
               if (pcrValue == null) {
                  pcrValue = pinPcrValue;
               }
               else if (!pinPcrValue.equals(pcrValue)) {
                  throw new Exception("Conflicting PCR values, 1st = "+pcrValue+", 2nd="+pinPcrValue);
               }
            }
         }
         if (pcrValue ==null) {
            pcrValue = "USBDM::DEFAULT_PCR";
         }
      }
      portClockMasks.add(pin.getClockMask());
      String bitNums = pin.getGpioBitNum();
      if (bitNums != null) {
         long bitNum = Long.parseLong(bitNums);
         ArrayList<HashMap<String, Long>> muxValues = portMap.get(pin.getPORTBasePtr());
         if (muxValues == null) {
            muxValues = new ArrayList<HashMap<String, Long>>(Collections.nCopies(8, (HashMap<String, Long>)null));
            portMap.put(pin.getPORTBasePtr(), muxValues);
         }
         HashMap<String, Long> pcrMap = muxValues.get(mux.value);
         if (pcrMap == null) {
            pcrMap = new HashMap<String, Long>();
            muxValues.set(mux.value, pcrMap);
         }
         Long bitMask = pcrMap.get(pcrValue);
         if (bitMask == null) {
            bitMask = new Long(0);
         }
         bitMask |= 1<<bitNum;
         pcrMap.put(pcrValue, bitMask);
      }
   }
   
   /**
    * Get string to initialise PORT clocks
    * 
    * @return
    */
   public String getPcrClearString(String indent) {
      
      StringBuffer initPcrbuffer = new StringBuffer();

      for (String key:portMap.keySet()) {
         // Each PORT
         ArrayList<HashMap<String, Long>> port = portMap.get(key);
         long collectedMask = 0;
         // Each pin (ignoring pcrValue)
         for (HashMap<String, Long> pcrValuesForMux:port) {
            if (pcrValuesForMux != null) {
               for (String key2:pcrValuesForMux.keySet()) {
                  collectedMask |= pcrValuesForMux.get(key2);
               }
            }
         }
         if ((collectedMask&0xFFFF) != 0) {
            initPcrbuffer.append(String.format(indent+"      %s = PORT_PCR_MUX(%d)|PORT_GPCLR_GPWE(0x%sU);\n", "((PORT_Type *)"+key+")->GPCLR", 0, Long.toHexString(collectedMask&0xFFFF).toUpperCase()));
         }
         collectedMask >>= 16;
         if ((collectedMask&0xFFFF) != 0) {
            initPcrbuffer.append(String.format(indent+"      %s = PORT_PCR_MUX(%d)|PORT_GPCHR_GPWE(0x%sU);\n", "((PORT_Type *)"+key+")->GPCHR", 0, Long.toHexString(collectedMask&0xFFFF).toUpperCase()));
         }
      }
      return initPcrbuffer.toString();
   }

   public String getPcrInitString(String indent) {
      StringBuffer initPcrbuffer = new StringBuffer();

      for (String key:portMap.keySet()) {
         // Each PORT
         ArrayList<HashMap<String, Long>> port = portMap.get(key);
         // Each mux value on port
         int muxValue = 0;
         for (HashMap<String, Long> pcrValuesForMux:port) {
            if (pcrValuesForMux != null) {
               // Each PCR value for mux
               for (String pcrValue:pcrValuesForMux.keySet()) {
                  Long mask = pcrValuesForMux.get(pcrValue);
                  if ((mask&0xFFFF) != 0) {
                     initPcrbuffer.append(String.format(indent+"      %s = %s|PORT_PCR_MUX(%d)|PORT_GPCLR_GPWE(0x%sU);\n", "((PORT_Type *)"+key+")->GPCLR", pcrValue, muxValue, Long.toHexString(mask&0xFFFF).toUpperCase()));
                  }
                  mask >>= 16;
                  if ((mask&0xFFFF) != 0) {
                     initPcrbuffer.append(String.format(indent+"      %s = %s|PORT_PCR_MUX(%d)|PORT_GPCHR_GPWE(0x%sU);\n", "((PORT_Type *)"+key+")->GPCHR", pcrValue, muxValue, Long.toHexString(mask&0xFFFF).toUpperCase()));
                  }
               }
            }
            muxValue++;
         }
      }
      return initPcrbuffer.toString();
   }
   
   /**
    * Get string to initialise port clocks e.g.<br><br>
    * 
    * <code>
    *    <b>enablePortClocks(PORTA_CLOCK_MASK|PORTE_CLOCK_MASK);</b>
    * </code>
    * @param indent 
    * @return 
    */
   public String getInitPortClocks(String indent) {
      StringBuffer sb = new StringBuffer();
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
         sb.append(p);
      }
      if (!isFirst) {
         sb.append(");\n\n");
      }
      return sb.toString();
   }
}