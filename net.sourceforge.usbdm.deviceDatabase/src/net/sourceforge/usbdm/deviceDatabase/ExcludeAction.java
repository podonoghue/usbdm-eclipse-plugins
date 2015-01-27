package net.sourceforge.usbdm.deviceDatabase;


public class ExcludeAction extends ProjectAction {
   private final String    target;
   private final boolean   excluded;
   private final boolean   folder;
   
   public ExcludeAction(String target, boolean isExcluded, boolean isFolder) throws Exception {
      super("---exclude---");
      this.target      = target;
      this.excluded  = isExcluded;
      this.folder    = isFolder;
   }
   public String getTarget() {
      return target;
   }
   public boolean isExcluded() {
      return excluded;
   }
   public boolean isFolder() {
      return folder;
   }
   @Override
   public String toString() {
      return "ExcludeAction[\""+target+"\"]";
   }
}