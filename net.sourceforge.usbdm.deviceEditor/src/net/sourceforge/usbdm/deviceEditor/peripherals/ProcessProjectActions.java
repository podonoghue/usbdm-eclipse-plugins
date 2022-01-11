package net.sourceforge.usbdm.deviceEditor.peripherals;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import net.sourceforge.usbdm.cdt.utilties.AddTargetFiles;
import net.sourceforge.usbdm.cdt.utilties.ApplyOptions;
import net.sourceforge.usbdm.cdt.utilties.DeleteResource;
import net.sourceforge.usbdm.cdt.utilties.ProjectUtilities;
import net.sourceforge.usbdm.packageParser.CreateFolderAction;
import net.sourceforge.usbdm.packageParser.CustomAction;
import net.sourceforge.usbdm.packageParser.DeleteResourceAction;
import net.sourceforge.usbdm.packageParser.ExcludeAction;
import net.sourceforge.usbdm.packageParser.FileAction;
import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
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
 
   final HashSet<String> previousActions = new HashSet<String>();
   
   public ProcessProjectActions() {
   }
   
   /**
    * Process a project action list
    * 
    * @param projectHandle Project being manipulated
    * @param actionList    Actions to do
    * @param symbolMap   Variables that may be needed
    * @param monitor       Progress monitor
    * 
    * @throws Exception
    */
   public void process(
         final StringBuilder         actionRecord,
         final IProject              projectHandle, 
         final ProjectActionList     actionList,
         final ISubstitutionMap      symbolMap, 
         final IProgressMonitor      monitor) throws Exception {

      if (actionList == null) {
         return;
      }
      actionRecord.append("ProcessProjectActions.process " + actionList.getId() + "\n");
      final ApplyOptions applyOptions = new ApplyOptions(projectHandle);

      class MyVisitor implements ProjectActionList.Visitor {
         
         @Override
         public Result applyTo(ProjectAction action, Value result, IProgressMonitor monitor) {
            SubMonitor subMonitor = SubMonitor.convert(monitor);
            subMonitor.subTask(action.toString());
//            System.err.println("ProjectCustomAction: "+action.toString());
            try {
               if (action instanceof FileAction) {
                  new AddTargetFiles().process(projectHandle, symbolMap, (FileAction)action, subMonitor);
               }
               else if (action instanceof DeleteResourceAction) {
                  new DeleteResource().process(projectHandle, symbolMap, (DeleteResourceAction)action, subMonitor);
               }
               else if (action instanceof CreateFolderAction) {
//                  ProjectUtilities.createFolder(projectHandle, variableMap, (CreateFolderAction)action, subMonitor);
               }
               else if (action instanceof ProjectOption) {
                  applyOptions.process(symbolMap, (ProjectOption)action, subMonitor);
               }
               else if (action instanceof ExcludeAction) {
                  ProjectUtilities.excludeItem(projectHandle, (ExcludeAction)action, subMonitor);
               }
               else if (action instanceof ProjectActionList) {
                  ProjectActionList projectActionList = (ProjectActionList) action;
                  if (projectActionList.isDoOnceOnly()) {
                     if (previousActions.contains(projectActionList.getId())) {
                        // Don't repeat action
                        actionRecord.append("ProcessProjectActions.process - not repeating action " + projectActionList.getId() + "\n");
                        return new Result(Status.PRUNE);
                     }
                     previousActions.add(projectActionList.getId());
                  }
                  return new Result(projectActionList.applies(symbolMap)?Status.CONTINUE:Status.PRUNE);
               }
               else if (action instanceof ProjectCustomAction) {
                  ProjectCustomAction customAction = (ProjectCustomAction) action;
                  Class<?> actionClass = Class.forName(customAction.getclassName());
                  Object clsInstance = actionClass.getConstructor().newInstance();
                  if (!(clsInstance instanceof CustomAction)) {
                     throw new Exception("Custom action does not implement required interface");
                  }
                  ((CustomAction)clsInstance).action(projectHandle, symbolMap, subMonitor, customAction.getValue());
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
//               e.printStackTrace();
               StringBuffer sb = new StringBuffer();
               sb.append("Unable to process Action "+action.toString() + "\n");
               sb.append("Action id = " + action.getId() + "\n");
               sb.append("Action owned by = " + action.getOwnerId() + "\n");
               sb.append(e.getMessage());
               return new Result(new Exception(sb.toString(), e));
            }
            return new Result(Status.CONTINUE);
         }
      };

      MyVisitor visitor = new MyVisitor();
      Result res = actionList.visit(visitor, null, monitor);
      if (res.getStatus() == Status.EXCEPTION) {
         throw res.getException();
      }
   }
}
