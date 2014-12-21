package net.sourceforge.usbdm.deviceDatabase;

/**
 * Selects erase method
 */
public enum EraseMethod {
   ERASE_NONE        (0x00, "None"),       //!< No erase is done
   ERASE_MASS        (0x01, "Mass"),       //!< A Mass erase operation is done
   ERASE_ALL         (0x02, "All"),        //!< All Flash is erased (by Flash Block)
   ERASE_SELECTIVE   (0x03, "Selective"),  //!< A selective erase (by sector) is done
   ;
   private final int    mask;
   private final String name;
   EraseMethod(int mask, String name) {
      this.mask = mask;
      this.name = name;
   }
   public int getMask() {
      return mask;
   }
   public static EraseMethod valueOf(int mask) {
      for (EraseMethod type:values()) {
         if (type.mask == mask) {
            return type;
         }
      }
      return ERASE_MASS;
   }
   public String getName() {
      return name;
   }
}