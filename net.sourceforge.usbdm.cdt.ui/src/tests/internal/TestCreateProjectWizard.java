package tests.internal;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.cdt.ui.newProjectWizard.UsbdmNewProjectWizard;

public class TestCreateProjectWizard {

   public static void main(String[] args) {
      Display display = new Display();
      Shell shell = new Shell(display);

      // Instantiates and initialises the wizard
      UsbdmNewProjectWizard wizard = new UsbdmNewProjectWizard();
      wizard.init(null,null);

      // Instantiates the wizard container with the wizard and opens it
      WizardDialog dialog = new WizardDialog(shell, wizard);
      dialog.create();
      dialog.open();
   }

}
