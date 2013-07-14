package net.sourceforge.usbdm.cdt.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConfigureHandler extends AbstractHandler {
	/**
	 *  Display the USBDM Configuration dialogue
	 */
   
   static final String prefPages[] = {
      "net.sourceforge.usbdm.cdt.ui.preferencePages.topPage",
      "net.sourceforge.usbdm.cdt.ui.preferencePages.ArmPage",
      "net.sourceforge.usbdm.cdt.ui.preferencePages.Cfv1Page",
      "net.sourceforge.usbdm.cdt.ui.preferencePages.CfvxPage",
      };
	public Object execute(ExecutionEvent event) throws ExecutionException {
	   
      IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

      PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn (
               window.getShell(),
               prefPages[0], 
               prefPages, 
               null);
      if (pref != null) {
         pref.open();
      }
      return null;
	}
}
