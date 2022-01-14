package net.sourceforge.usbdm.packageParser;



public class CreateFolderAction extends ProjectAction {
   
   private String    target;
   private String    type;
   private String    root;
   private boolean fDerived;

   public CreateFolderAction(String target, String type) {
      super("---folder---");
      this.target = target;
      this.type   = type;
   }
   
   public String getTarget() {
      return target;
   }
   
   public String getType() {
      return type;
   }

   public void setRoot(String root) {
      this.root = root;
   }

   public String getRoot() {
      return root;
   }
   
   @Override
   public String toString() {
      return String.format("CreateFolderAction[root=%s, target=%s, type=%s]", root, target, type);
   }
   /**
    * Used to indicate that the folder should be marked as derived in eclipse
    * 
    * @param derived
    */
   public void setDerived(boolean derived) {
      this.fDerived = derived;
   }
   /**
    * Indicate that the folder should be marked as derived in eclipse
    * 
    * @return True if derived
    */
   public boolean isDerived() {
      return fDerived;
   }
}