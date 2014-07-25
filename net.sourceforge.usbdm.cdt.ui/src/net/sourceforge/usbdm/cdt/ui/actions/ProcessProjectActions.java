package net.sourceforge.usbdm.cdt.ui.actions;

import java.util.Map;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.cdt.ui.newProjectWizard.ProjectUtilities;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.Condition;
import net.sourceforge.usbdm.deviceDatabase.Device.CreateFolderAction;
import net.sourceforge.usbdm.deviceDatabase.Device.ExcludeAction;
import net.sourceforge.usbdm.deviceDatabase.Device.FileAction;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectAction;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectActionList;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectCustomAction;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectOption;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectVariable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public class ProcessProjectActions {

   public static void process(IProject projectHandle, Device device, Map<String,String> variableMap, IProgressMonitor monitor) throws Exception {

//      System.err.println("ProcessProjectActions.process("+device.getName()+") =============================================");
      ProjectActionList actionList = device.getProjectActionList();
      if (actionList == null) {
         return;
      }
      ApplyOptions applyOptions = null;
//      System.err.println("ProcessProjectActions.process() - has map");
      for (ProjectAction action : actionList) {
         Condition condition = action.getCondition();
         if (condition != null) {
            ProjectVariable variable = condition.getVariable();
            boolean conditionValue = false;
            String conditionString = variableMap.get(UsbdmConstants.CONDITION_PREFIX_KEY+"."+variable.getId());
            conditionValue = Boolean.valueOf(conditionString);
            if (condition.isNegated()) {
               conditionValue = !conditionValue;
            }
            if (!conditionValue) {
//               System.err.println("ProcessProjectActions.process() - not doing action based on: " + variable.getName());
               // Skip action
               continue;
            }
//            System.err.println("ProcessProjectActions.process() - Doing action based on: " + variable.getName());
         }
//         System.err.println("ProcessProjectActions.process() - Doing action: " + action.toString());
         try {
            if (action instanceof FileAction) {
               new AddTargetFiles().process(projectHandle, device, variableMap, (FileAction)action, monitor);
            }
            else if (action instanceof CreateFolderAction) {
               ProjectUtilities.createFolder(projectHandle, device, variableMap, (CreateFolderAction)action, monitor);
            }
            else if (action instanceof ProjectOption) {
               if (applyOptions == null) {
                  applyOptions = new ApplyOptions();
               }
               applyOptions.process(projectHandle, device, variableMap, (ProjectOption)action, monitor);
            }
            else if (action instanceof ExcludeAction) {
//               System.err.println("ProjectCustomAction: "+action.toString());
               ProjectUtilities.excludeItem(projectHandle, (ExcludeAction)action, monitor);
            }
            else if (action instanceof ProjectCustomAction) {
//               System.err.println("ProjectCustomAction: "+action.toString());
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
            System.err.println("Unable to process Action "+action.toString());
            e.printStackTrace();
//            new Exception("Unable to process Action"+action.toString(), e).printStackTrace();
         }
      }     
      if (applyOptions != null) {
         applyOptions.updateConfigurations(projectHandle);
      }
   }
}
