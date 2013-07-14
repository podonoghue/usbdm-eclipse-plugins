package net.sourceforge.usbdm.cdt.ui.wizards;

import net.sourceforge.usbdm.constants.UsbdmSharedConstants;

import org.eclipse.cdt.ui.templateengine.IPagesAfterTemplateSelectionProvider;
import org.eclipse.cdt.ui.templateengine.IWizardDataPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class UsbdmNewProjectWizard extends Wizard implements INewWizard {
   
   @Override
   public void init(IWorkbench workbench, IStructuredSelection selection) {
      setNeedsProgressMonitor(true);
      setDefaultPageImageDescriptor(UsbdmSharedConstants.getUsbdmIcon());
//      setDefaultPageImageDescriptor(ImageDescriptor.createFromFile(null, "icons/usbdm.png"));
   }

   @Override
   public boolean performFinish() {
      return false;
//      return page.saveSetting();
   }

   @Override
   public void addPages() {
      IPagesAfterTemplateSelectionProvider pageProvider = new ToolPageProviderFactory.ArmPageProvider();
      
      IWizardDataPage[] pages = pageProvider.createAdditionalPages(this, null, null);
      for (int index=1; index<pages.length; index++) {
         pages[index-1].setNextPage(pages[index]);
         pages[index].setPreviousPage(pages[index-1]);
      }
      for (IWizardDataPage page:pages) {
         addPage(page);
      }
   }

   /* (non-Javadoc)
    * @see org.eclipse.jface.wizard.Wizard#canFinish()
    */
   @Override
   public boolean canFinish() {
      // TODO Auto-generated method stub
      return super.canFinish();
   }

   /**
    * @param args
    */
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