package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.ui.CElementLabelProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.TwoPaneElementSelector;

public class UsbdmLaunchShortcut implements ILaunchShortcut {

   /**
    * Locates a launchable entity in the given active editor, and launches an application in the specified mode. 
    * This launch configuration shortcut is responsible for progress reporting as well as error handling, in 
    * the event that a launchable entity cannot be found, or launching fails.
    * 
    * @param editor - the active editor in the workbench
    * @param mode - one of the launch modes defined by the launch manager
    */
   @Override
   public void launch(IEditorPart editor, String mode) {
      searchAndLaunch(new Object[] { editor.getEditorInput() }, mode);
   }

   /**
    * Locates a launchable entity in the given selection and launches an application in the specified mode. 
    * This launch configuration shortcut is responsible for progress reporting as well as error handling, 
    * in the event that a launchable entity cannot be found, or launching fails.
    * 
    * @param  selection - workbench selection
    * @param  mode - one of the launch modes defined by the launch manager
    */
   @Override
   public void launch(ISelection selection, String mode) {
      if (selection instanceof IStructuredSelection) {
         searchAndLaunch(((IStructuredSelection) selection).toArray(), mode);
      }
   }

   /**
    * Locates or creates a launchable for the given binary and then launches it
    * 
    * @param bin  Binary to create launchable for
    * @param mode One of the launch modes defined by the launch manager
    */
   public void launch(IBinary bin, String mode) {
      try {
         DebugUITools.launch(findLaunchConfiguration(bin), mode);
      } catch (Exception e) {
         handleFail(getActiveWorkbenchShell(), e.getMessage());
      }
   }

   /**
    * Method getLaunchConfigType.
    * 
    * @return ILaunchConfigurationType
    */
   protected ILaunchConfigurationType getLaunchConfigType() {
      return getLaunchManager().getLaunchConfigurationType("net.sourceforge.usbdm.gdb.launchConfigurationType");
   }

   /**
    * Search and launch binary.
    * 
    * @param elements  Binaries to search.
    * @param mode      Launch mode.
    */
   private void searchAndLaunch(final Object[] elements, String mode) {
      
      IBinary[] results = LaunchParameterUtilities.searchForExecutable(elements);
      
      if (results.length == 0) {
         handleFail(getActiveWorkbenchShell(), "Binary not found\nPlease build one target first");
         return;
      }
      IBinary bin = LaunchParameterUtilities.chooseBinary(getActiveWorkbenchShell(), results);
      // May be cancelled so quietly check for null
      if (bin != null) {
         launch(bin, mode);
      }
   }

   protected void handleFail(Shell shell, String message) {
      MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR|SWT.OK);
      mbox.setMessage(message);
      mbox.setText("USBDM - Launching failed");
      mbox.open();
   }

   /**
    * Locate a configuration to launch the given binary.<br>
    * If one cannot be found, create one.
    * 
    * @param bin   The binary to launched
    *
    * @return A re-usable config or <code>null</code> if none.
    * @throws Exception 
    */
   protected ILaunchConfiguration findLaunchConfiguration(IBinary bin) throws Exception {
      
      ILaunchConfigurationType configType = getLaunchConfigType();
      List<ILaunchConfiguration> candidateConfigs = Collections.emptyList();
      IPath binPath = bin.getResource().getProjectRelativePath();
      try {
         ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
         candidateConfigs = new ArrayList<ILaunchConfiguration>(configs.length);
         for (ILaunchConfiguration config : configs) {
            IPath programPath = CDebugUtils.getProgramPath(config);
            String projectName = CDebugUtils.getProjectName(config);
            if (programPath != null && programPath.equals(binPath)) {
               if ((projectName != null) && projectName.equals(bin.getCProject().getProject().getName())) {
                  candidateConfigs.add(config);
               }
            }
         }
      } catch (CoreException e) {
         System.err.println(e);
      }

      IProject project  = bin.getCProject().getProject();

      // If there are no existing configurations associated with the IBinary,
      // create one. If there is exactly one configuration associated with the
      // IBinary, return it. Otherwise, if there is more than one
      // configuration associated with the IBinary, prompt the user to choose
      // one.
      ILaunchConfiguration configuration = null;
      int candidateCount = candidateConfigs.size();
      if (candidateCount < 1) {
         // Create default launch
         configuration = LaunchParameterUtilities.createLaunchConfig(getActiveWorkbenchShell(), project, bin);
      } else if (candidateCount == 1) {
         configuration = candidateConfigs.get(0);
      } else {
         // Prompt the user to choose a configuration. A null result means
         // the user
         // cancelled the dialog, in which case this method returns null,
         // since canceling the dialog should also cancel launching
         // anything.
         configuration = chooseConfiguration(candidateConfigs);
      }
      return configuration;
   }

   protected ILaunchManager getLaunchManager() {
      return DebugPlugin.getDefault().getLaunchManager();
   }

   /**
    * Show a selection dialog that allows the user to choose one of the
    * specified launch configurations.
    * 
    * @param configList  The list of launch configurations to choose from.
    * 
    * @return The chosen config, or <b><code>null</code></b> if the user cancelled the dialog.
    */
   protected ILaunchConfiguration chooseConfiguration(List<ILaunchConfiguration> configList) {
      
      IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
      ElementListSelectionDialog dialog = new ElementListSelectionDialog(getActiveWorkbenchShell(), labelProvider);
      dialog.setElements(configList.toArray());
      dialog.setTitle("Launch Configuration Selection");
      dialog.setMessage("Choose a launch configuration");
      dialog.setMultipleSelection(false);
      int result = dialog.open();
      labelProvider.dispose();
      if (result == IStatus.OK) {
         return (ILaunchConfiguration) dialog.getFirstResult();
      }
      return null;
   }

   protected Shell getActiveWorkbenchShell() {
      IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (window != null) {
         return window.getShell();
      }
      IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
      if (windows.length > 0) {
         return windows[0].getShell();
      }
      return null;
   }

}