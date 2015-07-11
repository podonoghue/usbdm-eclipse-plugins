package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Map;

import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.packageParser.ProjectActionList;
import net.sourceforge.usbdm.packageParser.WizardPageInformation;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Composite;

public class UsbdmNewProjectOptionsPanel extends UsbdmOptionsPanel {

   /**
    * 
    * @param parent           Parent composite to contain the panel
    * @param style            Standard SWT style
    * @param device           Device - used to determine panel contents
    * @param dialogSettings   Unused
    * @param optionMap        Option map used to determine panel contents
    * @param wizardPageInfo 
    * 
    * @throws Exception 
    */
   public UsbdmNewProjectOptionsPanel(
         Composite             parent, 
         int                   style, 
         IDialogSettings       dialogSettings, 
         Device                device, 
         ProjectActionList     projectActionList,
         Map<String,String>    optionMap,
         WizardPageInformation wizardPageInfo) {
      
      super(parent, style, dialogSettings, device, projectActionList, optionMap, wizardPageInfo);
      createControl();
   }
}
