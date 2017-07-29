package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;

public class IrqDialogue extends Dialog {

   private String       fTitle = "Interrupt handler setup";

   private String       fValue;
   
   private Button       fDisabledButton;
   private Button       fClassMethodButton;
   private Button       fUserMethodButton;
   private Text         fText;
   
   private String       fOriginalValue;

   /**
    * Create dialogue displaying 
    * 
    * @param parentShell
    * @param bitmask        Bit mask for available bits
    * @param initialValue   Initial bit mask
    */
   public IrqDialogue(Shell parentShell, String value) {
     super(parentShell);
     fOriginalValue = value;
     fValue         = value;
   }

   @Override
   protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);

      GridLayout gl = new GridLayout(2, false);
      gl.marginLeft = 10;
      gl.marginRight = 10;
      gl.marginTop = 10;
      container.setLayout(gl);

      Group radioGroup = new Group(parent, SWT.NONE);
      radioGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
      radioGroup.setText("Mode");
      radioGroup.setToolTipText("This selection allow the interrupt handler for this peripheral to be installed using several different methods");
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.verticalSpan = 3;
      radioGroup.setLayoutData(gridData);
      
      fDisabledButton     = new Button(radioGroup, SWT.RADIO);
      fDisabledButton.setText("No handler installed");
      fDisabledButton.setToolTipText("No handler is installed");
      fDisabledButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            fValue = IrqVariable.NOT_INSTALLED_VALUE;
            fText.setEnabled(false);
         }
      });
      fClassMethodButton  = new Button(radioGroup, SWT.RADIO);
      fClassMethodButton.setText("Class Method");
      fClassMethodButton.setToolTipText("Software (Use setCallback() or override class method)");
      fClassMethodButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            fValue = IrqVariable.CLASS_VALUE;
            fText.setEnabled(false);
         }
      });
      fUserMethodButton   = new Button(radioGroup, SWT.RADIO);
      fUserMethodButton.setText("User Method");
      fUserMethodButton.setToolTipText("User function (set name below)");
      fUserMethodButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            fText.setEnabled(true);
         }
      });

      switch(IrqVariable.getMode(fOriginalValue)) {
      default:
      case NotInstalled:
         fDisabledButton.setSelection(true);
         break;
      case ClassMethod:
         fClassMethodButton.setSelection(true);
         break;
      case UserMethod:
         fUserMethodButton.setSelection(true);
         break;
      }

      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.verticalSpan = 3;
      fText = new Text(parent, SWT.BORDER);
      fText.setLayoutData(gridData);
      
      fText.setText(IrqVariable.getHandlerName(fOriginalValue));

      parent.layout(true);
      
      return container;
   }

   /**
    * Get dialogue result
    * 
    * @return Comma separated list of check boxes selected e.g. 1,8,45
    */
   public String getResult() {
      return fValue;
   }
   
   @Override
   protected void okPressed() {
      if (fUserMethodButton.getSelection()) {
         fValue = fText.getText();
      }
      super.okPressed();
   }
   
   @Override
   protected void cancelPressed() {
      super.cancelPressed();
      fValue = fOriginalValue;
   }

   @Override
   protected void configureShell(Shell newShell) {
     super.configureShell(newShell);
     newShell.setText(fTitle);
   }

   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display, SWT.DIALOG_TRIM|SWT.CENTER);
      shell.setText("Device Editor");
      shell.setLayout(new FillLayout());
      shell.setSize(600, 200);
      
      String selection = "";
      while(true) {
         IrqDialogue editor = new IrqDialogue(shell, "");
         if  (editor.open() != OK) {
            break;
         }
         selection = editor.getResult();
         System.err.println("res = " + selection);
      }
      
      while (!shell.isDisposed()) {
          if (!display.readAndDispatch()) display.sleep();
      }
      
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }

   public String getTitle() {
      return fTitle;
   }

   public void setTitle(String fTitle) {
      this.fTitle = fTitle;
   }
 } 