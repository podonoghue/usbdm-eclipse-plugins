package net.sourceforge.usbdm.packageParser;


public class ProjectConstant extends ProjectAction {
   protected       String  fValue;
   protected final boolean fDoReplace;
   
   public ProjectConstant(String id, String value, boolean doReplace) throws Exception {
      super(id);
      fValue     = value;
      fDoReplace = doReplace;
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
}