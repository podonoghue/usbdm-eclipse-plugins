package net.sourceforge.usbdm.deviceDatabase;

import java.util.HashMap;
import java.util.Map;

//! RS08/HCS08/CFV1 clock types
//!
public enum ClockTypes {
   INVALID        (-1, "Invalid"      ),
   EXTERNAL        (0, "External"     ),
   S08ICGV1        (1, "S08ICGV1"     ),
   S08ICGV2        (2, "S08ICGV2"     ),
   S08ICGV3        (3, "S08ICGV3"     ),
   S08ICGV4        (4, "S08ICGV4"     ),
   S08ICSV1        (5, "S08ICSV1"     ),
   S08ICSV2        (6, "S08ICSV2"     ),
   S08ICSV2x512    (7, "S08ICSV2x512" ),
   S08ICSV3        (8, "S08ICSV3"     ),
   S08ICSV4        (9, "S08ICSV4"     ),
   RS08ICSOSCV1   (10, "RS08ICSOSCV1" ),
   RS08ICSV1      (11, "RS08ICSV1"    ),
   S08MCGV1       (12, "S08MCGV1"     ),
   S08MCGV2       (13, "S08MCGV2"     ),
   S08MCGV3       (14, "S08MCGV3"     ),
   ;

   private final int    mask;
   private final String name;

   // Used for reverse lookup of frequency (Hz)
   private static final Map<String,ClockTypes> lookupString 
      = new HashMap<String, ClockTypes>();

   static {
      for(ClockTypes ct : ClockTypes.values()) {
         lookupString.put(ct.name, ct);
      }
   }

   ClockTypes(int mask, String name) {
      this.mask = mask;
      this.name = name;
   }
   public int getMask() {
      return mask;
   }
   public String getName() {
      return name;
   }
   /**
    *   Get matching ClockType
    *   
    *   @param name Readable name of ClockType
    * 
    *   @return ClockSpeed matching (exactly) the frequency given or the default value if not found.
    */
   public static ClockTypes parse(String name) {
      ClockTypes rv = lookupString.get(name);
      if (rv == null) {
         rv = INVALID;
      }
      return  rv;
   }   
}