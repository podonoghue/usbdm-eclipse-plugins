package net.sourceforge.usbdm.deviceDatabase;


public class ProjectVariable extends ProjectAction {
   protected final String name;
   protected final String description;
   protected final String defaultValue;
   protected String value;
   
   public ProjectVariable(String id, String name, String description, String defaultValue) {
      super(id);
      this.name         = name;
      this.description  = description;
      this.defaultValue = defaultValue;
      this.value        = defaultValue;
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
   
   @Override
   public String toString() {
      return String.format("ProjectVariable[name=%s, description=%s, value=%s]", name, description, value);
   }
}