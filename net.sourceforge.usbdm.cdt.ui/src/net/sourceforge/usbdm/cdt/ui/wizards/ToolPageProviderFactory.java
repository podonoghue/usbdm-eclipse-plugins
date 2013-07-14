package net.sourceforge.usbdm.cdt.ui.wizards;
import net.sourceforge.usbdm.constants.UsbdmSharedConstants.InterfaceType;

import org.eclipse.cdt.ui.templateengine.IPagesAfterTemplateSelectionProvider;
import org.eclipse.cdt.ui.templateengine.IWizardDataPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

public class ToolPageProviderFactory {

   static public class PageProvider implements IPagesAfterTemplateSelectionProvider {

      IWizardDataPage[] additionalWizardPages = null;
      InterfaceType interfaceType;
      
      PageProvider(InterfaceType interfaceType) {
         super();
         this.interfaceType = interfaceType;
      }
      
      @Override
      public IWizardDataPage[] createAdditionalPages(IWorkbenchWizard wizard,
            IWorkbench workbench, IStructuredSelection selection) {

         try {
            UsbdmProjectPage usbdmWizardPage = new UsbdmProjectPage(interfaceType);
            additionalWizardPages = new IWizardDataPage[] { 
                  usbdmWizardPage,
                  new UsbdmConfigurationPage(usbdmWizardPage) 
            };
         } catch (Exception e) {
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
   
   static public class Cfv1PageProvider extends PageProvider {
      public Cfv1PageProvider() {
         super(InterfaceType.T_CFV1);
      }      
   }
   
   static public class CfvxPageProvider extends PageProvider {
      public CfvxPageProvider() {
         super(InterfaceType.T_CFVX);
      }      
   }
   
   static public class ArmPageProvider extends PageProvider {
      public ArmPageProvider() {
         super(InterfaceType.T_ARM);
      }      
   }
   
}

