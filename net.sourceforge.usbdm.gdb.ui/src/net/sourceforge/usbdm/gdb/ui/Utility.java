package net.sourceforge.usbdm.gdb.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class Utility {
   /**
    * Browse for external binary file
    * 
    * @param title         Title for dialogue
    * @param textControl   Text control to be updated
    * @return null - cancelled or failed, else path accepted
    */
   public static String browseButtonSelected(Shell shell, String title, Text textControl) {
      final String filters[] = {"*.afx;*.elf;*.bin", "*.*"};
      final String filterNames[] = {"binary files (*.elf;*.afx;*.bin)", "all files (*.*)"};
      FileDialog fd = new FileDialog(shell, SWT.OPEN);
      fd.setText(title);
      fd.setFilterExtensions(filters);
      fd.setFilterNames(filterNames);
      fd.setFilterPath(textControl.getText());
      String filePath = fd.open();
      if (filePath != null) {
         textControl.setText(filePath);
         textControl.setToolTipText(filePath);
      }
      return filePath;
   }

   /**
    * Browse workspace for binary file
    * 
    * @param title         Title for dialogue
    * @param textControl   Text control to be updated
    * @return null - cancelled or failed, else path accepted
    */
   public static String browseWorkspaceButtonSelected(Shell shell, String title, Text textControl) {
      ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(), new org.eclipse.ui.model.BaseWorkbenchContentProvider());
      dialog.setTitle(title); 
      dialog.setMessage("File brows"); 
      dialog.setInput(ResourcesPlugin.getWorkspace().getRoot()); 
      dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
      String filePath = null;
      if (dialog.open() == IDialogConstants.OK_ID) {
         IResource resource = (IResource) dialog.getFirstResult();
         String arg = resource.getFullPath().toOSString();
         filePath = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression("workspace_loc", arg); //$NON-NLS-1$
         textControl.setText(filePath);
         textControl.setToolTipText(filePath);
      }
      return filePath;
   }


}
