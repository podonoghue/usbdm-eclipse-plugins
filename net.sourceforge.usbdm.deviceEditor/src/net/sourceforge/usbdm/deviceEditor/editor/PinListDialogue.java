package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.deviceEditor.information.Peripheral;
import net.sourceforge.usbdm.deviceEditor.information.Pin;
import net.sourceforge.usbdm.deviceEditor.information.PinListVariable;
import net.sourceforge.usbdm.deviceEditor.information.Signal;

public class PinListDialogue extends Dialog {
   /** The actual Spinners */
   private final Combo fCombos[];
   /** Initial list of values */
   private final String  fInitialSelections;
   /** Resulting list */
   private       String  fResult = null;
   /** */
   private final Peripheral fPeripheral;
   /** */
   private final PinListVariable fVariable;
   /** */
   private final String[]  fPinList;
   private final Integer[] fSignalIndexList;
   
   private static final String PIN_TEMPLATE = "%s => %s"; 
   /**
    * Create dialogue displaying a set of pin selection spinners
    * 
    * @param parentShell
    * @param numValues          Number of spinners to display (starts at zero)
    * @param initialSelections  List of initially spinner values e.g. 1,6,9,24
    */
   public PinListDialogue(Shell parentShell, PinListVariable variable, String initialSelections) {
     super(parentShell);
     fVariable   = variable;
     fPeripheral = fVariable.getPeripheral();
     /*
      * Create list of available pins and indexes
      */
     ArrayList<String> pins             = new ArrayList<String>();
     ArrayList<Integer> signalIndexList = new ArrayList<Integer>();
     pins.add("");
     signalIndexList.add(-1);
     
     Vector<Signal> table  = fPeripheral.getSignalTables().get(0).table;
     for (int pinIndex=0; pinIndex<table.size(); pinIndex++) {
        Signal entry = table.get(pinIndex);
        if ((entry == null) || (entry.getMappedPin().getPin() == Pin.UNASSIGNED_PIN)) {
           continue;
        }
        pins.add(String.format(PIN_TEMPLATE, entry.getName(), entry.getMappedPin().getPin().getName()));
        signalIndexList.add(pinIndex);
     }
     fPinList         = pins.toArray(new String[pins.size()]);
     fSignalIndexList = signalIndexList.toArray(new Integer[pins.size()]);
     
     fCombos = new Combo[variable.getMaxListSize()];
     fInitialSelections = initialSelections;
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      
      Composite container = (Composite) super.createDialogArea(parent);
      GridLayout gl = new GridLayout(5, true);
      gl.marginLeft = 10;
      gl.marginRight = 10;
      gl.marginTop = 10;
      container.setLayout(gl);
      container.layout();

      String[] initialSelections = fInitialSelections.split("[, ]+");
      Vector<Signal> pinTable = fPeripheral.getSignalTables().get(0).table;
      for (int i=0; i<fCombos.length; i++) {
         try {
            fCombos[i] = new Combo(container, SWT.CHECK);
            fCombos[i].setItems(fPinList);
            if (i<initialSelections.length) {
               int signalIndex = Integer.parseInt(initialSelections[i]);
               Signal entry = pinTable.elementAt(signalIndex);
               String item = String.format(PIN_TEMPLATE, entry.getName(), entry.getMappedPin().getPin().getName());
               int sel = fCombos[i].indexOf(item);
               if (sel > 0) {
                  fCombos[i].select(sel);
               }
               else {
                  fCombos[i].select(0);
               }
            }
         }
         catch (NumberFormatException e) {
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
      StringBuilder sb = new StringBuilder(5*fCombos.length);
      for (int index=0; index<fCombos.length; index++) {
         Combo b = fCombos[index];
         if ((b != null) && (b.getSelectionIndex()>=0) && !b.getText().isEmpty()) {
            Integer signalIndex = fSignalIndexList[b.getSelectionIndex()];
            sb.append(signalIndex.toString());
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

//   public static void main(String[] args) {
//      Display display = new Display();
//
//      Shell shell = new Shell(display, SWT.DIALOG_TRIM|SWT.CENTER);
//      shell.setText("Device Editor");
//      shell.setLayout(new FillLayout());
//      shell.setSize(600, 200);
//      
//      String selection = "1  , 2,3    ,4, 29";
//      while(true) {
//         PinListDialogue editor;// = new PinListDialogue(shell, 8, selection);
//         if  (editor.open() != OK) {
//            break;
//         }
//         selection = editor.getResult();
//         System.err.println("res = " + selection);
//      }
//      
//      while (!shell.isDisposed()) {
//          if (!display.readAndDispatch()) display.sleep();
//      }
//      
//      shell.open();
//      while (!shell.isDisposed()) {
//         if (!display.readAndDispatch())
//            display.sleep();
//      }
//      display.dispose();
//   }
 }
