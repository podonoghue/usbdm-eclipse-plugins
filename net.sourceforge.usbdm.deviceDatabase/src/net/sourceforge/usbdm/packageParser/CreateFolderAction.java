package net.sourceforge.usbdm.packageParser;



public class CreateFolderAction extends ProjectAction {
   
   private String    target;
   private String    type;
   private String    root;

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
}