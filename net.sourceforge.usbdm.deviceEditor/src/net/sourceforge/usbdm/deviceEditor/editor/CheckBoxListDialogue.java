package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.HashSet;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class CheckBoxListDialogue extends Dialog {
   private final int    fNumBoxes;
   private final Button fButtons[];
   private final String fInitialSelections;
   private       String fResult = null;
   
   /**
    * Create dialogue displaying a set of check boxes
    * 
    * @param parentShell
    * @param numBoxes            Number of maximum check box to display (starts at zero)
    * @param initialSelections   List of initially checked boxes e.g. 1,6,9,24
    */
   public CheckBoxListDialogue(Shell parentShell, int numBoxes, String initialSelections) {
     super(parentShell);
     fNumBoxes = numBoxes;
     fButtons = new Button[numBoxes];
     fInitialSelections = initialSelections;
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      
      Composite container = (Composite) super.createDialogArea(parent);
      GridLayout gl = new GridLayout(10, true);
      gl.marginLeft = 10;
      gl.marginRight = 10;
      gl.marginTop = 10;
      container.setLayout(gl);
      container.layout();

      HashSet<Integer> selections = new HashSet<Integer>();
      
      String[] initialSelections = fInitialSelections.split("[, ]+");
      for (String s:initialSelections) {
         if (s.isEmpty()) {
            continue;
         }
         selections.add(Integer.parseInt(s));
      }
      for (int i=0; i<fNumBoxes; i++) {
         fButtons[i] = new Button(container, SWT.CHECK);
         fButtons[i].setText(Integer.toString(i));
         if (selections.contains(i)) {
            fButtons[i].setSelection(true);
         }
      }
      parent.layout(true);
      return container;
   }

   /**
    * Get dialogue result
    * 
    * @return Comma separated list of check boxes selected e.g. 1,8,45
    */
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
            sb.append(",");
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
      
      String selection = "1  , 2,3    ,4, 29";
      while(true) {
         CheckBoxListDialogue editor = new CheckBoxListDialogue(shell, 61, selection);
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