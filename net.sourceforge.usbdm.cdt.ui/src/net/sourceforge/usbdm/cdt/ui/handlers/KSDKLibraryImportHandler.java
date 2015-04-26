package net.sourceforge.usbdm.cdt.ui.handlers;

import net.sourceforge.usbdm.cdt.ui.ksdk.KSDKLibraryImportWizard;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class KSDKLibraryImportHandler extends AbstractHandler {
	/**
	 *  Display the USBDM Configuration dialogue
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
	   
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		KSDKLibraryImportWizard wizard = new KSDKLibraryImportWizard();
      wizard.init(window.getWorkbench(), null );
      
      // Instantiates the wizard container with the wizard and opens it
      WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
      dialog.create();
		return dialog.open();
	}
}
