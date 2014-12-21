package net.sourceforge.usbdm.deviceDatabase;

public enum MemoryType {
   MemInvalid  ("MemInvalid", "",        false, false ), 
   MemRAM      ("MemRAM",     "ram",     false, true  ), 
   MemEEPROM   ("MemEEPROM",  "eeprom",  true,  false ), 
   MemFLASH    ("MemFLASH",   "flash",   true,  false ), 
   MemFlexNVM  ("MemFlexNVM", "flexNVM", true,  false ), 
   MemFlexRAM  ("MemFlexRAM", "flexRAM", false, true  ), 
   MemROM      ("MemROM",     "rom",     false, false ), 
   MemIO       ("MemIO",      "io",      false, false ), 
   MemPFlash   ("MemPFlash",  "pFlash",  true,  false ), 
   MemDFlash   ("MemDFlash",  "dFlash",  true,  false ), 
   MemXRAM     ("MemXRAM",    "xRAM",    false, true  ),    // DSC
   MemPRAM     ("MemPRAM",    "pRAM",    false, true  ),    // DSC
   MemXROM     ("MemXROM",    "xROM",    true,  false ),    // DSC
   MemPROM     ("MemPROM",    "pROM",    true,  false ),    // DSC
   ;
   public final String  name;
   public final String  xmlName;
   public final boolean isProgrammable;
   public final boolean isRam;

   MemoryType(String name, String xmlName, boolean isProgrammable, boolean isRam) {
      this.name = name;
      this.xmlName        = xmlName;
      this.isProgrammable = isProgrammable;
      this.isRam          = isRam;
   }

   static public MemoryType getTypeFromXML(String memoryType) {
      //         System.err.println("getTypeFromXML(" + memoryType + ")");
      MemoryType[] memoryTypes = MemoryType.values();
      for (int index=0; index<memoryTypes.length; index++) {
         //            System.err.println("getTypeFromXML() checking \"" + memoryTypes[index].xmlName + "\"");
         if (memoryType.equals(memoryTypes[index].xmlName)) {
            //               System.err.println("getTypeFromXML() found it \"" + memoryTypes[index].xmlName + "\"");
            return memoryTypes[index];
         }
      }
      return null;
   }

   @Override
   public String toString() {
      return name;
   }
}