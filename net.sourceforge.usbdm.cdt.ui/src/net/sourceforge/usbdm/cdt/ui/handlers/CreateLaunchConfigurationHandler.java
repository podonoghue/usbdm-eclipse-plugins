package net.sourceforge.usbdm.cdt.ui.handlers;

import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import net.sourceforge.usbdm.cdt.tools.Activator;
import net.sourceforge.usbdm.cdt.ui.newProjectWizard.LaunchParameterUtilities;

public class CreateLaunchConfigurationHandler implements IHandler {
   @Override
   public void addHandlerListener(IHandlerListener handlerListener) {
   }

   @Override
   public void dispose() {
   }

   /**
    * Display basic error dialogue
    * 
    * @param shell   Shell for dialogue
    * @param msg     Message to display
    */
   private static void displayError(Shell shell, String msg) {
      MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR|SWT.OK);
      mbox.setMessage(msg);
      mbox.setText("USBDM - Can't create launch configuration");
      mbox.open();
   }

   /**
    * Handler for "Create USBDM Launch configuration"
    */
   @Override
   public Object execute(ExecutionEvent event) {

      final Shell shell   = HandlerUtil.getActiveWorkbenchWindow(event).getShell();
      final Object source = HandlerUtil.getCurrentSelection(event);

      //      System.err.println("Event source = "+source.toString()+"\n class = "+source.getClass().toString());
      if (!(source instanceof TreeSelection)) {// Ignore
//         System.err.println("Source is not an instance of TreeSelection");
         return null;
      }
      TreeSelection selection = (TreeSelection) source;
      if (!(selection.getFirstElement() instanceof IProject)) {
         // Must be project
//         System.err.println("Selection.getFirstElement() is not an instance of org.eclipse.cdt.core.model.IProject");
         return null;
      }  
      IBinary[] results = LaunchParameterUtilities.searchForExecutable(selection.toArray());
      if (results.length < 1) {
         displayError(shell, "Binary not found\nPlease build target first");
         return null;
      }
      IBinary bin = LaunchParameterUtilities.chooseBinary(shell, results);
      try {
         // May be cancelled so quietly check for null
         if (bin != null) {
            LaunchParameterUtilities.createLaunchConfig(shell, (IProject) selection.getFirstElement(), bin);
         }
      } catch (Exception e) {
         Activator.logError("Failed to launch program", e);
         displayError(shell, e.getMessage());
      }
      return  null;
   }

//   void delme(IProject project, IBinary bin) {
//      IPath path = bin.getPath();
//      String buildName = path.segment(path.segmentCount());
//      try {
//         IBuildConfiguration x = project.getActiveBuildConfig();
//      } catch (CoreException e1) {
//         e1.printStackTrace();
//      }
//      ILaunchConfiguration launchConfiguration = (ILaunchConfiguration)bin.getAdapter(ILaunchConfiguration.class);
//      try {
//         ILaunchConfigurationType x = launchConfiguration.getType();
//      } catch (CoreException e) {
//         e.printStackTrace();
//      }
//      try {
//         ICProject cProject = (ICProject) project.getAdapter(ICProject.class);
//         for (IBuildConfiguration config:project.getBuildConfigs()) {
//            System.err.println("IBuildConfiguration = "+config.getName());
//         }
//      } catch (CoreException e) {
//         e.printStackTrace();
//      }
//   }
   
   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public boolean isHandled() {
      return true;
   }

   @Override
   public void removeHandlerListener(IHandlerListener handlerListener) {
   }
}
