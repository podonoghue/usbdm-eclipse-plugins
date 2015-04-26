package net.sourceforge.usbdm.cdt.ui.newProjectWizard;

import java.util.Map;

import net.sourceforge.usbdm.deviceDatabase.Device;
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
    * 
    * @throws Exception 
    */
   public UsbdmNewProjectOptionsPanel(
         Composite          parent, 
         int                style, 
         Device             device, 
         IDialogSettings    dialogSettings, 
         Map<String,String> optionMap) throws Exception {
      
      super(parent, style, device, dialogSettings, optionMap);
      createControl();
   }

}
