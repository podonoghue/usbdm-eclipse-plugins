import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.cdt.examplewizard.UsbdmSelectProjectWizardPage;


public class TestExampleProjectWizard  extends ApplicationWindow {

   /**
    * Create the application window.
    */
   public TestExampleProjectWizard() {
      super(null);
      addToolBar(SWT.FLAT | SWT.WRAP);
      addMenuBar();
      addStatusLine();
   }

   /**
    * Create contents of the application window.
    * @param parent
    */
  
   protected Control createContents(Composite parent) {
      parent.setSize(500, 600);
      Control container;
      UsbdmSelectProjectWizardPage wizardPage = new UsbdmSelectProjectWizardPage();
      wizardPage.createControl(parent);
      container = wizardPage.getControl();
      return container;
   }

   /**
    * @param args
    */
   public static void main(String[] args) {
      try {
         TestExampleProjectWizard window = new TestExampleProjectWizard();
         window.setBlockOnOpen(true);
         window.open();
         Display.getCurrent().dispose();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
