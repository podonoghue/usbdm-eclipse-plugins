package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Map;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.Device.Condition;
import net.sourceforge.usbdm.deviceDatabase.Device.CreateFolderAction;
import net.sourceforge.usbdm.deviceDatabase.Device.FileInfo;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectAction;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectActionList;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectOption;
import net.sourceforge.usbdm.deviceDatabase.Device.ProjectVariable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

public class ProcessProjectActions {

   public void process(IProject projectHandle, Device device, Map<String,String> variableMap, IProgressMonitor monitor) throws Exception {

      System.err.println("ProcessProjectActions.process("+device.getName()+")");
      ProjectActionList actionList = device.getProjectActionList();
      if (actionList == null) {
         return;
      }
      ApplyOptions applyOptions = null;
      System.err.println("ProcessProjectActions.process() - has map");
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
               System.err.println("ProcessProjectActions.process() - not doing action based on: " + variable.getName());
               continue;
            }
         }
         if (action instanceof FileInfo) {
            new AddTargetFiles().process(projectHandle, device, variableMap, (FileInfo)action, monitor);
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
      }     
      if (applyOptions != null) {
         applyOptions.updateConfigurations(projectHandle);
      }
   }
}
