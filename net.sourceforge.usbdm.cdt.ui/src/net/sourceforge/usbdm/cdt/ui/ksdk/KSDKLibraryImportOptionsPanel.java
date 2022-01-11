package net.sourceforge.usbdm.cdt.ui.ksdk;

import org.eclipse.swt.widgets.Composite;

import net.sourceforge.usbdm.cdt.ui.newProjectWizard.UsbdmOptionsPanel;
import net.sourceforge.usbdm.packageParser.PackageParser;
import net.sourceforge.usbdm.packageParser.SubstitutionMap;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

public class KSDKLibraryImportOptionsPanel extends UsbdmOptionsPanel {

   /**
    * 
    * @param parent           Parent composite to contain the panel
    * @param style            Standard SWT style
    * @throws Exception 
    */
   public KSDKLibraryImportOptionsPanel(Composite parent, int style) throws Exception {
      super(parent, 
            style, 
            null, 
            null, 
            PackageParser.getKDSPackageList(new SubstitutionMap()), 
            new SubstitutionMap(),
            new WizardPageInformation("usbdm-kds-creation-page", "Create KDS Library Project", "Select options for creation of library project"));
      createControl();
   }

}
