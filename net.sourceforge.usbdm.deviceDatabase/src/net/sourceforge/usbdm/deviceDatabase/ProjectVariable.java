package net.sourceforge.usbdm.deviceDatabase;

import java.util.ArrayList;

public class ProjectVariable extends ProjectAction {
   protected final String name;
   protected final String description;
   protected final String defaultValue;
   protected       String value;
   protected       ArrayList<ProjectVariable> requirements;
   protected       ArrayList<ProjectVariable> preclusions;
   protected       String groupName;
   public    enum  GroupType {DEFAULT, RADIO_GROUP, CHECK_GROUP};
   protected       GroupType groupType;
   
   public ProjectVariable(String id, String name, String description, String defaultValue) throws Exception {
      super(id);
      this.name         = name;
      this.description  = description;
      this.defaultValue = defaultValue;
      this.value        = defaultValue;
      this.requirements = new ArrayList<ProjectVariable>();
      this.preclusions  = new ArrayList<ProjectVariable>();
      this.groupName        = null;
      this.groupType    = GroupType.DEFAULT;
   }
   public String getName() {
      return name;
   }
   public String getDescription() {
      return description;
   }
   public String getDefaultValue() {
      return defaultValue;
   }
   public String getValue() {
      return value;
   }
   public void setValue(String value) {
      this.value = value;
   }
   /**
    * @return the requirements
    */
   public ArrayList<ProjectVariable> getRequirements() {
      return requirements;
   }
   /**
    * @param requirement The requirement to add
    */
   public void addRequirement(ProjectVariable requirement) {
      this.requirements.add(requirement);
   }
   /**
    * @return the requirements
    */
   public ArrayList<ProjectVariable> getPreclusion() {
      return preclusions;
   }
   /**
    * @param preclusion The preclusion to add
    */
   public void addPreclusion(ProjectVariable preclusion) {
      this.preclusions.add(preclusion);
   }
   /**
    * @return the group
    */
   public String getGroupName() {
      return groupName;
   }
   /**
    * @return the groupType
    */
   public GroupType getGroupType() {
      return groupType;
   }
   /**
    * @param groupName Name of group
    * @param groupType Type of group
    */
   public void setGroup(String groupName, GroupType groupType) {
      this.groupName = groupName;
      this.groupType = groupType;
   }
   
   @Override
   public String toString() {
      return String.format("ProjectVariable[name=%s, description=%s, value=%s]", name, description, value);
   }
}