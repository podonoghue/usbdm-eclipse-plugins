package net.sourceforge.usbdm.cdt.ui.actions;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

import net.sourceforge.usbdm.cdt.ui.Activator;
import net.sourceforge.usbdm.cdt.utilties.AddTargetFiles;
import net.sourceforge.usbdm.cdt.utilties.ApplyOptions;
import net.sourceforge.usbdm.cdt.utilties.DeleteResource;
import net.sourceforge.usbdm.cdt.utilties.ProjectUtilities;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.packageParser.CreateFolderAction;
import net.sourceforge.usbdm.packageParser.DeleteResourceAction;
import net.sourceforge.usbdm.packageParser.ExcludeAction;
import net.sourceforge.usbdm.packageParser.FileAction;
import net.sourceforge.usbdm.packageParser.ProjectAction;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Value;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result;
import net.sourceforge.usbdm.packageParser.ProjectActionList.Visitor.Result.Status;
import net.sourceforge.usbdm.packageParser.ProjectConstant;
import net.sourceforge.usbdm.packageParser.ProjectCustomAction;
import net.sourceforge.usbdm.packageParser.ProjectOption;
import net.sourceforge.usbdm.packageParser.ProjectVariable;
import net.sourceforge.usbdm.packageParser.WizardGroup;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

public class ProcessProjectActions {
 
   public static void process(
         final IProject              projectHandle, 
         final Device                device, 
         final ProjectActionList     actionList,
         final Map<String,String>    variableMap, 
         final IProgressMonitor      monitor) throws Exception {

//      System.out.println("ProcessProjectActions.process("+device.getName()+") =============================================");
      if (actionList == null) {
         return;
      }

      final ApplyOptions applyOptions = new ApplyOptions(projectHandle);

      class MyVisitor implements ProjectActionList.Visitor {
         
         @Override
         public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
//            System.err.println("ProjectCustomAction: "+action.toString());
            try {
               if (action instanceof FileAction) {
                  new AddTargetFiles().process(projectHandle, variableMap, (FileAction)action, monitor);
               }
               else if (action instanceof DeleteResourceAction) {
                  new DeleteResource().process(projectHandle, variableMap, (DeleteResourceAction)action, monitor);
               }
               else if (action instanceof CreateFolderAction) {
                  ProjectUtilities.createFolder(projectHandle, variableMap, (CreateFolderAction)action, monitor);
               }
               else if (action instanceof ProjectOption) {
                  applyOptions.process(variableMap, (ProjectOption)action, monitor);
               }
               else if (action instanceof ExcludeAction) {
                  ProjectUtilities.excludeItem(projectHandle, (ExcludeAction)action, monitor);
               }
               else if (action instanceof ProjectActionList) {
                  ProjectActionList projectActionList = (ProjectActionList) action;
                  return new Result(projectActionList.appliesTo(device, variableMap)?Status.CONTINUE:Status.PRUNE);
               }
               else if (action instanceof ProjectCustomAction) {
                  
                  ProjectCustomAction customAction = (ProjectCustomAction) action;
                  Class<?> actionClass = Class.forName(customAction.getclassName());
                  Object clsInstance = actionClass.getConstructor().newInstance();
                  if (!(clsInstance instanceof CustomAction)) {
                     throw new Exception("Custom action does not implement required interface");
                  }
                  ((CustomAction)clsInstance).action(projectHandle, variableMap, monitor, customAction.getValue());
               }
               else if (action instanceof ProjectConstant) {
                  // Ignore as already added to paramMap
               }
               else if (action instanceof WizardGroup) {
                  // Ignore as already added to paramMap
               }
               else if (action instanceof ProjectVariable) {
                  // Ignore as already added to paramMap
               }
               else if (action instanceof WizardPageInformation) {
                  // Ignore as only applicable to wizard dialogues
               }
               else {
                  throw new Exception("Unexpected action class: " + action.getClass());
               }
            } catch (Exception e) {
               StringBuffer sb = new StringBuffer();
               sb.append("Unable to process Action "+action.toString() + "\n");
               sb.append("Action id = " + action.getId() + "\n");
               sb.append("Action owned by = " + action.getOwnerId() + "\n");
               return new Result(new Exception(sb.toString(), e));
            }
            return new Result(Status.CONTINUE);
         }
      };

      MyVisitor visitor = new MyVisitor();
      Result res = actionList.visit(visitor, null, monitor);
      if (res.getStatus() == Status.EXCEPTION) {
         Activator.log(res.getMessage(), res.getException());
      }
   }
}
