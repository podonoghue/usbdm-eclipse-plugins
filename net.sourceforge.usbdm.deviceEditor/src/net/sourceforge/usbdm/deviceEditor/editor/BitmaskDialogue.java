package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class BitmaskDialogue extends Dialog {
   private final Button  fButtons[];
   private       long    fValue;
   private       long    fBitmask;
   
   // Set name pattern for elements e.g. pin%d
   private String   fElementName = "%d";
   
   // List of names for the bits
   private String[] fBitNames = null;
   private String   fTitle = "Select pins";
   
   /**
    * Create dialogue displaying a set of check boxes
    * 
    * @param parentShell
    * @param bitmask        Bit mask for available bits
    * @param initialValue   Initial bit mask
    */
   public BitmaskDialogue(Shell parentShell, long bitmask, long initialValue) {
     super(parentShell);
     fBitmask  = bitmask;
     long highestOne = Long.highestOneBit(bitmask);
     if (highestOne != 0) {
        fButtons  = new Button[Long.numberOfTrailingZeros(highestOne)+1];
     }
     else {
        fButtons = null;
     }
     fValue    = initialValue & fBitmask;
   }

   /**
    * Set name pattern for elements e.g. pin%d
    * 
    * @param name
    */
   void setElementName(String name) {
      fElementName = name;
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);
      int width = 8;
      if (fBitNames != null) {
         width = Integer.min(width, fBitNames.length);            
      }
      else {
         width = Integer.min(width, fButtons.length);            
      }
      GridLayout gl = new GridLayout(width, true);
      gl.marginLeft = 10;
      gl.marginRight = 10;
      gl.marginTop = 10;
      container.setLayout(gl);
      container.layout();

      if (fBitmask == 0) {
         Label lbl = new Label(container, SWT.CHECK);
         lbl.setText("No bits selectable");
      }
      else {
         int bitNameIndex = 0;
         for (int i=0; i<32; i++) {
            long mask = (1<<i);
            if (mask > fBitmask) {
               break;
            }
            fButtons[i] = null;
            if (fBitNames != null) {
               // If names are given only create required buttons
               if ((fBitmask & mask) != 0) {
                  Button btn = new Button(container, SWT.CHECK);
                  btn.setText(fBitNames[bitNameIndex++]);
                  btn.setSelection((fValue & mask) != 0);
                  fButtons[i] = btn;
               }
            }
            else {
               // Create all buttons but selectively enabled
               Button btn = new Button(container, SWT.CHECK);
               btn.setText(fElementName.replaceAll("%d", Integer.toString(i)));
               btn.setSelection((fValue & mask) != 0);
               btn.setEnabled((fBitmask & mask) != 0);
               fButtons[i] = btn;
            }
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
   String getResultAsList() {
      StringBuilder sb = new StringBuilder();
      boolean firstElement = true;
      if (fButtons == null) {
         return "";
      }
      for (int index=0; index<fButtons.length; index++) {
         if (((fBitmask & (1<<index))!=0) && ((fValue & (1<<index))!=0)) {
            if (!firstElement) {
               sb.append(",");
            }
            sb.append(index);
            firstElement = false;
         }
      }
      return sb.toString();
   }
   
   /**
    * Get dialogue result
    * 
    * @return Comma separated list of check boxes selected e.g. 1,8,45
    */
   long getResult() {
      return fValue;
   }
   
   @Override
   protected void okPressed() {
      fValue = 0;
      if (fButtons != null) {
         for (int index=0; index<fButtons.length; index++) {
            Control b = fButtons[index];
            Button btn = (Button)b;
            if (btn != null) {
               fValue |= btn.getSelection()?(1<<index):0;
            }
         }
      }
      super.okPressed();
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
      
      long selection = 0xC2;
      while(true) {
         BitmaskDialogue editor = new BitmaskDialogue(shell, 0xC2, selection);
//         editor.setElementName("pin%d");
         editor.setBitNameList("This is #1,This is #6,This is #7");
         if  (editor.open() != OK) {
            break;
         }
         selection = editor.getResult();
         System.err.println("res = " + editor.getResultAsList() + " (0x" + Long.toHexString(editor.getResult())+")");
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

   public void setBitNameList(String bitList) {
      if ((bitList != null) && !bitList.isEmpty()) {
         fBitNames = bitList.split(",");
      }
   }

   public String getTitle() {
      return fTitle;
   }

   public void setTitle(String fTitle) {
      this.fTitle = fTitle;
   }
 } 