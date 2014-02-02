package net.sourceforge.usbdm.cdt.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class WorkbenchPropertyTopPage extends PropertyPage implements
      IWorkbenchPropertyPage {

   public WorkbenchPropertyTopPage() {
      // TODO Auto-generated constructor stub
   }

   @Override
   protected Control createContents(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      
      composite.setLayout(new GridLayout(1, false));
      
      Label label = new Label(composite, SWT.NONE);
      label.setText("Place Holder");

      return composite;
   }

   /**
    * @param args
    */
      // TODO Auto-generated method stub


}
