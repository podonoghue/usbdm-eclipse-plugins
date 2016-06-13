package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class CheckBoxListDialogue extends Dialog {
   private final int    fNumBoxes;
   private final Button fButtons[];
   private String fResult = null;
   private final String fInitialSelections;
   
   public CheckBoxListDialogue(Shell parentShell, int numBoxes, String selections) {
     super(parentShell);
     fNumBoxes = numBoxes;
     fButtons = new Button[numBoxes];
     fInitialSelections = selections;
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      
      Composite container = (Composite) super.createDialogArea(parent);
      GridLayout gl = new GridLayout(fNumBoxes+1, true);
      gl.marginLeft = 10;
      gl.marginRight = 10;
      gl.marginTop = 10;
      container.setLayout(gl);
      container.layout();

      String[] x = fInitialSelections.split(", ");

      new Label(container, SWT.NONE);
      for (int i=0; i<fNumBoxes; i++) {
         Label label = new Label(container, SWT.NONE);
         label.setText(Integer.toString(i));
      }
      for (int r=0; r<61; r++) {
         Label label = new Label(container, SWT.NONE);
         label.setText(Integer.toString(r));
         for (int c=0; c<fNumBoxes; c++) {
            fButtons[c] = new Button(container, SWT.CHECK);
         }
      }
      parent.layout(true);
      return container;
   }

   String getResult() {
      return fResult;
   }
   
   @Override
   protected void okPressed() {
      StringBuilder sb = new StringBuilder(5*fNumBoxes);
      for (int index=0; index<fNumBoxes; index++) {
         Button b = fButtons[index];
         if ((b != null) && b.getSelection()) {
            sb.append(Integer.toString(index));
            sb.append(", ");
         }
      }
      fResult = sb.toString();
      super.okPressed();
   };
   
   @Override
   protected void configureShell(Shell newShell) {
     super.configureShell(newShell);
     newShell.setText("Select pins");
   }

   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display, SWT.DIALOG_TRIM|SWT.CENTER);
      shell.setText("Device Editor");
      shell.setLayout(new FillLayout());
      shell.setSize(600, 200);
      
      CheckBoxListDialogue editor = new CheckBoxListDialogue(shell, 61, "");
      if (editor.open() == OK) {
         System.err.println("res = " + editor.getResult());
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