package net.sourceforge.usbdm.deviceDatabase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class ProjectActionList extends ArrayList<ProjectAction> {

   private static final long serialVersionUID = -1493390145777975399L;
   private String id = null;
   ApplyWhenCondition applyWhenCondition = null;
   
   /**
    * @return the condition
    */
   public ApplyWhenCondition getApplyWhenCondition() {
      return applyWhenCondition;
   }

   /**
    * @param condition the condition to set
    */
   public void setApplyWhenCondition(ApplyWhenCondition condition) {
      this.applyWhenCondition = condition;
   }

   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @param id the id to set
    */
   public void setId(String id) {
      this.id = id;
   }

   public String toString(String root) {
      StringBuffer buffer = new StringBuffer();
      Iterator<ProjectAction> it = this.iterator();
      while(it.hasNext()) {
         ProjectAction action = it.next();
         buffer.append(action.toString());
      }
      return buffer.toString();     
   }
   
   public void add(ProjectActionList actionList) {
      for(ProjectAction i : actionList) {
         this.add(i);
      }
   }
   public boolean appliesTo(Device device, Map<String, String> variableMap) throws Exception {
      if (applyWhenCondition == null) {
         return true;
      }
      return applyWhenCondition.appliesTo(device, variableMap);
   }
   @Override
   public void add(int arg0, ProjectAction projectAction) {
      projectAction.setOwner(this);
      super.add(arg0, projectAction);
   }
   @Override
   public boolean add(ProjectAction projectAction) {
      projectAction.setOwner(this);
      return super.add(projectAction);
   }
}