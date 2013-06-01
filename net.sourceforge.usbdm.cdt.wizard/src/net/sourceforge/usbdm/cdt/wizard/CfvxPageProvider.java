package net.sourceforge.usbdm.cdt.wizard;
import net.sourceforge.usbdm.cdt.UsbdmCdtConstants.InterfaceType;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.cdt.ui.templateengine.IPagesAfterTemplateSelectionProvider;
import org.eclipse.cdt.ui.templateengine.IWizardDataPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;


public class CfvxPageProvider implements IPagesAfterTemplateSelectionProvider {

   IWizardDataPage[] additionalWizardPages = null;
   
   @Override
   public IWizardDataPage[] createAdditionalPages(IWorkbenchWizard wizard,
         IWorkbench workbench, IStructuredSelection selection) {

      additionalWizardPages    = new IWizardDataPage[1];
      try {
         additionalWizardPages[0] = new UsbdmWizardPage(InterfaceType.T_CFVX);
      } catch (UsbdmException e) {
         additionalWizardPages = new IWizardDataPage[0];
         e.printStackTrace();
      }
      
      return additionalWizardPages;
   }

   @Override
   public IWizardDataPage[] getCreatedPages(IWorkbenchWizard wizard) {
      return additionalWizardPages;
   }
}
