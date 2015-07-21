package net.sourceforge.usbdm.cdt.ui.ksdk;

import java.util.HashMap;

import net.sourceforge.usbdm.cdt.ui.newProjectWizard.UsbdmOptionsPanel;
import net.sourceforge.usbdm.packageParser.PackageParser;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

import org.eclipse.swt.widgets.Composite;

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
            PackageParser.getKDSPackageList(new HashMap<String, String>()), 
            new HashMap<String, String>(),
            new WizardPageInformation("usbdm-kds-creation-page", "Create KDS Library Project", "Select options for creation of library project"));
      createControl();
   }

}
