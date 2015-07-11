package net.sourceforge.usbdm.packageParser;

import org.eclipse.swt.SWT;

public class ProjectVariable extends ProjectAction {
   protected final String              fName;
   protected final String              fDescription;
   protected final String              fDefaultValue;
   protected       ApplyWhenCondition  fRequirement;
   protected       String              fButtonGroupId;
   protected       int                 fButtonStyle;
   
   public ProjectVariable(String id, String name, String description, String defaultValue) {
      super(id);
      this.fName           = name;
      this.fDescription    = description;
      this.fDefaultValue   = defaultValue;
      this.fRequirement    = ApplyWhenCondition.trueCondition;
      this.fButtonGroupId  = null;
      this.fButtonStyle    = SWT.CHECK;
   }
   public String getName() {
      return fName;
   }
   public String getDescription() {
      return fDescription;
   }
   public String getDefaultValue() {
      return fDefaultValue;
   }
   /**
    * @return the requirements
    */
   public ApplyWhenCondition getRequirement() {
      return fRequirement;
   }
   /**
    * @param requirement The name of requirement to add
    */
   public void setRequirement(ApplyWhenCondition requirement) {
      this.fRequirement = requirement;
   }
   /**
    * @return the group
    */
   public String getButtonGroupId() {
      return fButtonGroupId;
   }
   /**
    * @return the groupType
    */
   public int getButtonStyle() {
      return fButtonStyle;
   }
   /**
    * @param groupId     Name of group
    * @param buttonStyle Style of button (SWT.CHECK, SWT.RADIO)
    */
   public void setGroup(String groupId, int buttonStyle) {
      this.fButtonGroupId = groupId;
      this.fButtonStyle   = buttonStyle;
   }
   
   @Override
   public String toString() {
      return String.format("ProjectVariable[id=%s, group=%s, name=%s, description=%s]", getId(), fButtonGroupId, fName, fDescription);
   }
}