package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceEditor.information.IrqVariable;

public class IrqDialogue extends TitleAreaDialog {

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
     setTitle("Select interrupt handler");
     fOriginalValue = value;
     fValue         = value;
   }

   void validate() {
      String msg = null;
      if (fUserMethodButton.getSelection()) {
         fValue = fText.getText();
         msg = IrqVariable.isValidCIdentifier(fValue);
      }
      Button okButton =  getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(msg == null);
      }
      if (msg != null) {
         setMessage(msg, IMessageProvider.ERROR);
      }
      else {
         setMessage(null);
      }
   }
   
   @Override
   public void create() {
      super.create();
      setTitle("Interrupt handler setup");
      validate();
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      Composite area = (Composite) super.createDialogArea(parent);
      Composite container = new Composite(area, SWT.NONE);
      container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      
      GridLayout layout = new GridLayout(2, false);
      container.setLayout(layout);
      
      Group radioGroup = new Group(container, SWT.NONE);
      radioGroup.setLayout(new RowLayout(SWT.VERTICAL));
      radioGroup.setText("Interrupt handler installation");
      radioGroup.setToolTipText("This selection allow the interrupt handler for this peripheral to be installed using several different methods");
      GridData gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      gridData.horizontalSpan = 2;
      radioGroup.setLayoutData(gridData);
      
      fDisabledButton     = new Button(radioGroup, SWT.RADIO);
      fDisabledButton.setText("No handler installed");
      fDisabledButton.setToolTipText("No handler is installed");
      fDisabledButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            fValue = IrqVariable.NOT_INSTALLED_VALUE;
            fText.setEnabled(false);
            validate();
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
            validate();
         }
      });
      fUserMethodButton   = new Button(radioGroup, SWT.RADIO);
      fUserMethodButton.setText("User Method");
      fUserMethodButton.setToolTipText("User function (set name below)");
      fUserMethodButton.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            fText.setEnabled(true);
            validate();
         }
      });

      Label label = new Label(container, SWT.NONE);
      label.setText("C Function ");
      
      fText = new Text(container, SWT.BORDER);
      fText.setToolTipText("Name of C function to execute on interrupt");
      gridData = new GridData();
      gridData.horizontalAlignment = GridData.FILL;
      gridData.grabExcessHorizontalSpace = true;
      fText.setLayoutData(gridData);
      fText.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent arg0) {
            validate();
         }
      });
      
      switch(IrqVariable.getMode(fOriginalValue)) {
      default:
      case NotInstalled:
         fDisabledButton.setSelection(true);
         fText.setEnabled(false);
         break;
      case ClassMethod:
         fClassMethodButton.setSelection(true);
         fText.setEnabled(false);
         break;
      case UserMethod:
         fUserMethodButton.setSelection(true);
         fText.setEnabled(true);
         break;
      }

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
 } 