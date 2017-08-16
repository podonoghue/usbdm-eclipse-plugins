package net.sourceforge.usbdm.packageParser;


public class ProjectConstant extends ProjectAction {
   protected       String  fValue;
   protected final boolean fDoOverwrite;
   protected final boolean fIsWeak;
   
   
   public ProjectConstant(String id, String value, boolean doOverwrite, boolean isWeak) {
      super(id);
      fValue         = value;
      fDoOverwrite   = doOverwrite;
      fIsWeak        = isWeak;
   }
   public String getValue() {
      return fValue;
   }
   @Override
   public String toString() {
      return String.format("ProjectConstant[id=%s, value=%s, doReplace=%s]", getId(), fValue, Boolean.toString(fDoOverwrite));
   }
   /**
    * @return true if this constant is to overwrite earlier definitions
    */
   public boolean doOverwrite() {
      return fDoOverwrite;
   }
   /**
    * @return true if this constant is a weak value
    */
   public boolean isWeak() {
      return fIsWeak;
   }
}