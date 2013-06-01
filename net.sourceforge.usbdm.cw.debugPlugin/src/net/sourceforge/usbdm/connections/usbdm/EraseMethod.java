package net.sourceforge.usbdm.connections.usbdm;

// ! Erase Methods
// !
public enum EraseMethod {
   E_NONE("None"), 
   E_MASS("Mass"), 
   E_ALL("All"), 
   E_SELECTIVE("Selective"), ;
   static final EraseMethod defMethod = E_MASS;
   final String name;

   private EraseMethod(String name) {
      this.name = name;
   }

   public String toString() {
      return name;
   }
};
