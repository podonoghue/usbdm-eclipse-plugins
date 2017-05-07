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

   /**
    * Create a EraseMethod
    * 
    * @param mask Mask for erase method
    * @param name Name of erase method
    */
   EraseMethod(int mask, String name) {
      this.mask = mask;
      this.name = name;
   }
   
   /**
    * Get mask for erase method
    * 
    * @return
    */
   public int getMask() {
      return mask;
   }
   
   /**
    * Get EraseMethod for mask
    * 
    * @param mask
    * 
    * @return
    */
   public static EraseMethod valueOf(int mask) {
      for (EraseMethod type:values()) {
         if (type.mask == mask) {
            return type;
         }
      }
      return ERASE_MASS;
   }
   
   /**
    * Get EraseMethod from friendly name
    * 
    * @param name
    * @return
    * @throws Exception 
    */
   static EraseMethod getEraseMethod(String name) throws Exception {
      for (EraseMethod em:EraseMethod.values()) {
         if (em.name.equalsIgnoreCase(name)) {
            return em;
         }
      }
      throw new Exception("Invalid Enumeration name " + name);
   }
   
   /**
    * Get friendly name
    * 
    * @return
    */
   public String getName() {
      return name;
   }
}