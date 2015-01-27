package net.sourceforge.usbdm.cdt.ui.actions;

import java.util.ArrayList;
import java.util.Map;

import net.sourceforge.usbdm.cdt.ui.newProjectWizard.ProjectUtilities;
import net.sourceforge.usbdm.deviceDatabase.ApplyWhenCondition;
import net.sourceforge.usbdm.deviceDatabase.Block;
import net.sourceforge.usbdm.deviceDatabase.CreateFolderAction;
import net.sourceforge.usbdm.deviceDatabase.DeleteResourceAction;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.ExcludeAction;
import net.sourceforge.usbdm.deviceDatabase.FileAction;
import net.sourceforge.usbdm.deviceDatabase.ProjectAction;
import net.sourceforge.usbdm.deviceDatabase.ProjectActionList;
import net.sourceforge.usbdm.deviceDatabase.ProjectCustomAction;
import net.sourceforge.usbdm.deviceDatabase.ProjectOption;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public class ProcessProjectActions {
 
   public static void process(IProject projectHandle, Device device, Map<String,String> variableMap, IProgressMonitor monitor) throws Exception {
      
//      System.err.println("ProcessProjectActions.process("+device.getName()+") =============================================");
      ArrayList<ProjectActionList> actionLists = device.getProjectActionLists(variableMap);
      if (actionLists == null) {
         return;
      }
//      System.err.println("ProcessProjectActions.process("+device.getName()+") =============================================");
      for (ProjectActionList actionList:actionLists) {
         ApplyOptions applyOptions = null;
//         System.err.println("ProcessProjectActions.process() - actionList.ID = " + actionList.getId() + " ========================");
         for (ProjectAction action : actionList) {
            Block condition = action.getCondition();
            if (condition != null) {
               ApplyWhenCondition applyWhenCondition = condition.getApplyWhen();
               if (applyWhenCondition == null) {
//                  System.err.println("ProcessProjectActions.process() - not doing action based on missing applyWhenCondition");
                  // Skip action
                  continue;
               }
               boolean conditionValue = applyWhenCondition.appliesTo(device, variableMap);
               if (!conditionValue) {
//                  System.err.println("ProcessProjectActions.process() - not doing action based on: " + condition.toString());
                  // Skip action
                  continue;
               }
//             System.err.println("ProcessProjectActions.process() - Doing action based on: " + condition.toString());
            }
//            System.err.println("ProcessProjectActions.process() - Doing action: " + action.toString());
            try {
               if (action instanceof FileAction) {
                  new AddTargetFiles().process(projectHandle, device, variableMap, (FileAction)action, monitor);
               }
               else if (action instanceof DeleteResourceAction) {
                  new DeleteResource().process(projectHandle, device, variableMap, (DeleteResourceAction)action, monitor);
               }
               else if (action instanceof CreateFolderAction) {
                  ProjectUtilities.createFolder(projectHandle, device, variableMap, (CreateFolderAction)action, monitor);
               }
               else if (action instanceof ProjectOption) {
                  if (applyOptions == null) {
                     applyOptions = new ApplyOptions(projectHandle);
                  }
                  applyOptions.process(device, variableMap, (ProjectOption)action, monitor);
               }
               else if (action instanceof ExcludeAction) {
//                System.err.println("ProjectCustomAction: "+action.toString());
                  ProjectUtilities.excludeItem(projectHandle, (ExcludeAction)action, monitor);
               }
               else if (action instanceof ProjectCustomAction) {
                  //              System.err.println("ProjectCustomAction: "+action.toString());
                  ProjectCustomAction customAction = (ProjectCustomAction) action;
                  Class<?> actionClass = Class.forName(customAction.getclassName());
                  Object clsInstance = actionClass.newInstance();
                  if (!(clsInstance instanceof CustomAction)) {
                     throw new Exception("Custom action does not implement required interface");
                  }
                  ((CustomAction)clsInstance).action(projectHandle, device, variableMap, monitor, customAction.getValue());
               }
               else {
                  throw new Exception("Unexpected action class: " + action.getClass());
               }
            } catch (Exception e) {
               StringBuffer sb = new StringBuffer();
               sb.append("Unable to process Action "+action.toString() + "\n");
               sb.append("Action id = " + action.getId() + "\n");
               sb.append("Action owned by = " + action.getOwnerId() + "\n");
               throw new Exception(sb.append(e.getMessage()).toString());
            }
         }
         if (applyOptions != null) {
            applyOptions.updateConfigurations();
         }
      }
      ProjectOptionSettings projectOptionSettings = new ProjectOptionSettings(projectHandle, variableMap);
      projectOptionSettings.saveSetting();
//      ProjectOptionSettings projectOptionSettings2 = new ProjectOptionSettings(projectHandle);
//      Map<String, String> v = projectOptionSettings2.getVariableMap();
//      Iterator<Entry<String, String>> it = v.entrySet().iterator(); 
//      System.err.println("ProcessProjectActions()");
//      System.err.println("=====================================================================");
//      while (it.hasNext()) {
//         Entry<String, String> pairs = it.next();
//         System.err.println(pairs.getKey() + " = " + pairs.getValue());
//      }
//      System.err.println("=====================================================================");
   }
}
