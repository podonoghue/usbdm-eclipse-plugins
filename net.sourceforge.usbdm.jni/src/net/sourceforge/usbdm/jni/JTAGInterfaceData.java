package net.sourceforge.usbdm.jni;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.12
 */
public class JTAGInterfaceData {

   public static final String connectionSpeeds[] = new String[] {
      ClockSpeed.F_250KHZ.name,
      ClockSpeed.F_500KHZ.name,
      ClockSpeed.F_750KHZ.name,
      ClockSpeed.F_1MHZ.name,
      ClockSpeed.F_1_5MHZ.name,
      ClockSpeed.F_2MHZ.name,
      ClockSpeed.F_3MHZ.name,
      ClockSpeed.F_4MHZ.name,
      ClockSpeed.F_6MHZ.name,
      ClockSpeed.F_12MHZ.name, 
      ClockSpeed.F_24MHZ.name 
      };

   static public String[] getConnectionSpeeds() {
      return connectionSpeeds;
   }

   //! ARM/CFVx clock speeds
   //!
   public static enum ClockSpeed {
      F_250KHZ      ("250kHz",       250000),
      F_500KHZ      ("500kHz",       500000),
      F_750KHZ      ("750kHz",       750000),
      F_1MHZ        ("1MHz",        1000000),
      F_1_5MHZ      ("1.5MHz",      1500000),
      F_2MHZ        ("2MHz",        2000000),
      F_3MHZ        ("3MHz",        3000000),
      F_4MHZ        ("4MHz",        4000000),
      F_6MHZ        ("6MHz",        6000000),
      F_12MHZ       ("12MHz",      12000000),
      F_24MHZ       ("24MHz",      24000000),
      ;
      static final ClockSpeed defFrequency = F_4MHZ;
      final String name;
      final int    frequency;

      // Used for reverse lookup of frequency (Hz)
      private static final Map<Integer,ClockSpeed> lookupFrequency 
         = new HashMap<Integer,ClockSpeed>();

      // Used for reverse lookup of frequency (Hz)
      private static final Map<String,ClockSpeed> lookupString 
         = new HashMap<String,ClockSpeed>();

      static {
         for(ClockSpeed cs : ClockSpeed.values()) {
            lookupFrequency.put(cs.frequency, cs);
            lookupString.put(cs.name, cs);
         }
      }

      private ClockSpeed(String name, int frequency) {
         this.name         = name;
         this.frequency    = frequency;
      }
      /* (non-Javadoc)
       * @see java.lang.Enum#toString()
       */
      public String toString() {
         return name;
      }
      /**
       * @return
       */
      public int getFrequency() {
         return frequency;
      }
      /**
       *   Get matching ClockSpeed
       *   
       *   @param frequency
       * 
       *   @return ClockSpeed matching (exactly) the frequency given or the default value if not found.
       */
      static public ClockSpeed lookup(int frequency) {
         ClockSpeed rv = lookupFrequency.get(new Integer(frequency));
         if (rv == null) {
            rv = defFrequency;
         }
         return  rv;
      }
      /**
       *   Get matching ClockSpeed
       *   
       *   @param pretty name (e.g. 1.5MHz)
       * 
       *   @return ClockSpeed matching (exactly) the string given or the default value if not found.
       */
      static public ClockSpeed parse(String name) {
         ClockSpeed rv = lookupString.get(name);
         if (rv == null) {
            rv = defFrequency;
         }
         return  rv;
      }
      /**
       *   Get best matching ClockSpeed
       *   
       *   @param  frequency to try to match
       * 
       *   @return ClockSpeed for most suitable for value given
       */
      static public ClockSpeed findSuitable(int frequency) {
         ClockSpeed last = ClockSpeed.values()[0];
         for(ClockSpeed cs : ClockSpeed.values()) {
            if (cs.frequency > frequency)
               break;
            last = cs;
         }
         return last;
      }
   };

   static JTAGInterfaceData defaultDevice = new JTAGInterfaceData();
   
   @Override
   public String toString() {
      return "CFVx Device Data";
   }

   public static JTAGInterfaceData.ClockSpeed getClockSpeed(String speed) {
      for (ClockSpeed s : ClockSpeed.values()) {
         if (speed.equalsIgnoreCase(s.name)) {
            return s;
         }
      }
      return ClockSpeed.F_1MHZ;
   }
   
}
