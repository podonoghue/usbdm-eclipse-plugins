/*
 * NOT USED
 */


package net.sourceforge.usbdm.cdt.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.wizards.IWizardDescriptor;

public class StartNewProjectWizard extends AbstractHandler {

   public  void openWizard(Shell shell, String id) {
      // First see if this is a "new wizard".
      IWizardDescriptor descriptor = PlatformUI.getWorkbench().getNewWizardRegistry().findWizard(id);
      try  {
        // Then if we have a wizard, open it.
        if  (descriptor != null) {
          IWizard wizard = descriptor.createWizard();
          WizardDialog wd = new  WizardDialog(shell, wizard);
          wd.setTitle(wizard.getWindowTitle());
          wd.open();
        }
      } catch  (CoreException e) {
        e.printStackTrace();
      }
     }

   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {
      IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
      openWizard(window.getShell(), "");
      return null;
   }

}
