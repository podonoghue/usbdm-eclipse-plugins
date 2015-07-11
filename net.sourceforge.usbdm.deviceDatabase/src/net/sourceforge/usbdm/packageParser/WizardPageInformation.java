package net.sourceforge.usbdm.packageParser;

import java.util.ArrayList;

public class WizardPageInformation extends ProjectAction {
   private String fName;
   private String fDescription;

   ArrayList<WizardGroup> fGroups = new ArrayList<WizardGroup>();

   public void addGroup(WizardGroup group) {
      fGroups.add(group);
   }
   
   public ArrayList<WizardGroup> getGroups() {
      return fGroups;
   }

   public WizardPageInformation(String id, String name, String description) {
      super(id);
      fName        = name;
      fDescription = description;
   }

   /**
    * @return the Name
    */
   public String getName() {
      return fName;
   }

   /**
    * @return the Description
    */
   public String getDescription() {
      return fDescription;
   }
   
   public String toString() {
      return String.format("[WizardPage %s:%s:%s]", getId(), fName, fDescription);
   }
}
