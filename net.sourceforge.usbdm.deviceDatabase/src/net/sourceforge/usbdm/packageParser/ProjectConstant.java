package net.sourceforge.usbdm.packageParser;


public class ProjectConstant extends ProjectAction {
   protected       String  fValue;
   protected final boolean fDoReplace;
   protected final boolean fIsWeak;
   
   
   public ProjectConstant(String id, String value, boolean doReplace, boolean isWeak) {
      super(id);
      fValue     = value;
      fDoReplace = doReplace;
      fIsWeak    = isWeak;
   }
   public String getValue() {
      return fValue;
   }
   @Override
   public String toString() {
      return String.format("ProjectConstant[id=%s, value=%s, doReplace=%s]", getId(), fValue, Boolean.toString(fDoReplace));
   }
   /**
    * @return true if this constant is to replace earlier definitions
    */
   public boolean doReplace() {
      return fDoReplace;
   }
   /**
    * @return true if this constant is a weak value
    */
   public boolean isWeak() {
      return fIsWeak;
   }
}