package net.sourceforge.usbdm.deviceDatabase;


public class ProjectAction {
   private final String id;
   private Condition condition;
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
      this.id        = id;
      this.condition = null;
   }
   
   public String getId() {
      return id;
   }
   
   public Condition getCondition() {
      return condition;
   }
   
   public void setCondition(Condition condition) {
      this.condition = condition;
   }
   @Override
   public String toString() {
      return String.format("ProjectAction[ID=%s,COND=%s]", getId(), getCondition());
   }
}