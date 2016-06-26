package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface IEditor {

   public Control createControl(Composite parent);

   public Control getControl();
   
   public void setModel(BaseModel peripheralConfigurationModel);
   
   public void refresh();
}
