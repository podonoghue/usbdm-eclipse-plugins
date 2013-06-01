package net.sourceforge.usbdm.connections.usbdm;
import java.util.HashMap;
import java.util.Map;

public class JTAGInterfaceData {

   static String connectionSpeeds[] = new String[] {
      ClockSpeed.F_250KHZ.name,
      ClockSpeed.F_500KHZ.name,
      ClockSpeed.F_750KHZ.name,
      ClockSpeed.F_1MHZ.name,
      ClockSpeed.F_1_5MHZ.name,
      ClockSpeed.F_2MHZ.name,
      ClockSpeed.F_3MHZ.name,
      ClockSpeed.F_4MHZ.name,
      ClockSpeed.F_6MHZ.name,
      ClockSpeed.F_12MHZ.name 
      };

   static public String[] getConnectionSpeeds() {
      return connectionSpeeds;
   }

   //! RS08/HCS08/CFV1 clock types
   //!
   static enum ClockSpeed {
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
      ;
      static final ClockSpeed defFrequency = F_1MHZ;
      final String name;
      final int    frequency;

      // Used for reverse lookup of frequency (Hz)
      private static final Map<Integer,ClockSpeed> lookupMap 
         = new HashMap<Integer,ClockSpeed>();

      static {
         for(ClockSpeed cs : ClockSpeed.values())
            lookupMap.put(cs.frequency, cs);
      }

      private ClockSpeed(String name, int frequency) {
         this.name         = name;
         this.frequency    = frequency;
      }
      public String toString() {
         return name;
      }
      public int toFrequency() {
         return frequency;
      }
      static public ClockSpeed lookup(int frequency) {
         return lookupMap.get(new Integer(frequency));
      }
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

   static JTAGInterfaceData.ClockSpeed getClockSpeed(String speed) {
      for (ClockSpeed s : ClockSpeed.values()) {
         if (speed.equalsIgnoreCase(s.name)) {
            return s;
         }
      }
      return ClockSpeed.F_1MHZ;
   }
   
}
