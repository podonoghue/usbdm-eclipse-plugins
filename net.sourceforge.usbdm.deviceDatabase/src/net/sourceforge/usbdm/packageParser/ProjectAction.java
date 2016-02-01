package net.sourceforge.usbdm.packageParser;

public class ProjectAction {
   protected final String fId;

   private ProjectActionList owner = null;
   
   /**
    * @return the owner
    */
   public ProjectActionList getOwner() {
      return owner;
   }
   /**
    * @return the owner
    */
   public String getOwnerId() {
      if (owner == null) {
         return "No Owner";
      }
      return owner.getId();
   }
   /**
    * @param owner the owner to set
    */
   public void setOwner(ProjectActionList owner) {
      this.owner = owner;
   }

   public ProjectAction(String id) {
      this.fId = id;
      if ((id == null) || (id.length() == 0)) {
         throw new RuntimeException ("Project action must have id");
      }
   }
   
   public String getId() {
      return fId;
   }
   
   @Override
   public String toString() {
      return String.format("ProjectAction[ID=%s]", getId());
   }

}