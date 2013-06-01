package net.sourceforge.usbdm.cdt.wizard;
import net.sourceforge.usbdm.cdt.UsbdmCdtConstants.InterfaceType;
import net.sourceforge.usbdm.jni.UsbdmException;

import org.eclipse.cdt.ui.templateengine.IPagesAfterTemplateSelectionProvider;
import org.eclipse.cdt.ui.templateengine.IWizardDataPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;


public class ArmPageProvider implements IPagesAfterTemplateSelectionProvider {

   IWizardDataPage[] additionalWizardPages = null;
   
   @Override
   public IWizardDataPage[] createAdditionalPages(IWorkbenchWizard wizard,
         IWorkbench workbench, IStructuredSelection selection) {
      
//    System.err.println("ArmPageProvider!!");
      
      try {
         additionalWizardPages = new IWizardDataPage[] { new UsbdmWizardPage(InterfaceType.T_ARM) };
      } catch (UsbdmException e) {
         additionalWizardPages = new IWizardDataPage[0];
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      return additionalWizardPages;
   }

   @Override
   public IWizardDataPage[] getCreatedPages(IWorkbenchWizard wizard) {
      return additionalWizardPages;
   }
}
