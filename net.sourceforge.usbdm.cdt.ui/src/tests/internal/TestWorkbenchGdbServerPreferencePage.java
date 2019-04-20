package tests.internal;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.cdt.ui.WorkbenchGdbServerPreferencePage;
import net.sourceforge.usbdm.cdt.ui.WorkbenchGdbServerPreferencePage.WorkbenchPreferenceCfv1Page;

public class TestWorkbenchGdbServerPreferencePage {

   /**
    * @param args
    */
   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);
//      final WorkbenchGdbServerPreferencePage topPage = new WorkbenchPreferenceArmPage();
      final WorkbenchGdbServerPreferencePage topPage = new WorkbenchPreferenceCfv1Page();
      shell.setLayout(new FillLayout());
      topPage.init(null);
      topPage.createContents(shell);
      Button btn = new Button(shell, SWT.NONE);
      btn.setText("Save");
      btn.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            topPage.saveSettings();
            ArrayList<String> commandList = topPage.getServerCommandLine();
            String commandArray[] = new String[commandList.size()];
            commandArray = commandList.toArray(commandArray);
            for (String s : commandArray) { 
               System.err.print(s + " ");
            }
            System.err.print("\n");
         }
      });
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

}
