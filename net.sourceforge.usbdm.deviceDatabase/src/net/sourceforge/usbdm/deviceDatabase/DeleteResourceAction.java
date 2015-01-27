package net.sourceforge.usbdm.deviceDatabase;

public class DeleteResourceAction extends ProjectAction {
   private       String    root;
   private final String    target;

   public DeleteResourceAction(String target) throws Exception {
      super("---delete---");
      this.target  = target;
      this.root    = null;
   }
   
   public String getTarget() {
      return target;
   }
   
   @Override
   public String toString() {
      StringBuffer buffer = new StringBuffer(2000);
      buffer.append("DeleteResourceAction[\"");
      buffer.append(root);
      buffer.append(", ");
      buffer.append(target);
      buffer.append("]");
      return buffer.toString();
   }

   public void setRoot(String root) {
      this.root = root;
   }

   public String getRoot() {
      return root;
   }
   
}