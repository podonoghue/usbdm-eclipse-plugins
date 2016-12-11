package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class NumericListDialogue extends Dialog {
   /** Number of Spinners */
   private final int     fNumSpinners;
   /** The actual Spinners */
   private final Spinner fSpinners[];
   /** Initial list of values */
   private final String  fInitialSelections;
   /** Resulting list */
   private       String  fResult = null;
   /** Maximum value for spinner value */
   private       int  fMaxValue;
   
   /**
    * Create dialogue displaying a set of pin selection spinners
    * 
    * @param parentShell
    * @param numValues          Number of spinners to display (starts at zero)
    * @param initialSelections  List of initially spinner values e.g. 1,6,9,24
    */
   public NumericListDialogue(Shell parentShell, int numValues, int maxValue, String initialSelections) {
     super(parentShell);
     fNumSpinners       = numValues;
     fSpinners          = new Spinner[numValues];
     fInitialSelections = initialSelections;
     fMaxValue          = maxValue;
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

//      HashSet<Integer> selections = new HashSet<Integer>();
      
      String[] initialSelections = fInitialSelections.split("[, ]+");
      List<Integer> selections = new ArrayList<Integer>();
      for (String s:initialSelections) {
         if (s.isEmpty()) {
            continue;
         }
         selections.add(Integer.parseInt(s));
      }
      for (int i=0; i<fNumSpinners; i++) {
         fSpinners[i] = new Spinner(container, SWT.CHECK);
         fSpinners[i].setMinimum(0);
         fSpinners[i].setMaximum(fMaxValue);
         fSpinners[i].setIncrement(1);
         fSpinners[i].setSelection(0);
         if (i<selections.size()) {
            fSpinners[i].setSelection(selections.get(i));
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
   public String getResult() {
      return fResult;
   }
   
   @Override
   protected void okPressed() {
      StringBuilder sb = new StringBuilder(5*fNumSpinners);
      for (int index=0; index<fNumSpinners; index++) {
         Spinner b = fSpinners[index];
         if ((b != null) && (b.getSelection()>0)) {
            sb.append(Integer.toString(b.getSelection()));
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
         NumericListDialogue editor = new NumericListDialogue(shell, 8, 62, selection);
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
