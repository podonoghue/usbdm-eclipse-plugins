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

import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.PinListExpansion;

public class BitmaskDialogue extends Dialog {
   private final Button    fButtons[];
   private       long      fValue;
   private final long      fBitmask;
   private final BitmaskVariable fVariable;
   
   // Set name pattern for elements e.g. pin%d
   private String   fElementName = "%i";
   
   // List of names for the bits
   private String[] fBitNames = null;
   private String   fTitle = "Select pins";
   
   /**
    * Create dialogue displaying a set of check boxes
    * 
    * @param bitmaskVariable  Variable representing bit mask
    * @param parentShell      Shell
    * @param bitNames         Names for bits e.g. "B0,B2,,,,B6", "Pin#%i" or null for defaults
    * @param bitmask          Bit-mask for valid bit selections
    * @param initialValue     Initial value for mask
    * 
    * @throws Exception
    */
   public BitmaskDialogue(BitmaskVariable bitmaskVariable, Shell parentShell, String bitNames, long bitmask, long initialValue) throws Exception {
     super(parentShell);
     fVariable = bitmaskVariable;
     
     if  ((bitmask !=0) && (bitNames != null)) {
        // Both supplied
        String[] t = bitNames.split(",", -1);
        if (t.length == 1) {
           if (!bitNames.isBlank()) {
              // Name is a template e.g. Bit%i
              fElementName = bitNames;
           }
           fBitNames = null;
        }
        else {
           fBitNames = t;
        }
        fBitmask  = bitmask;
     }
     else if  ((bitmask == 0) && (bitNames != null)) {
        // Determine bitmask from names
        fBitNames = PinListExpansion.expandPinList(bitNames, ",");
        long tBitmask = 0;
        for (int index=0; index<fBitNames.length; index++) {
           long mask = (1L<<index);
           if (!fBitNames[index].isBlank()) {
              tBitmask |= mask;
           }
        }
        fBitmask = tBitmask;
     }
     else if ((bitmask != 0) && (bitNames == null)) {
        // Create default bit names based on bitmask
        fBitmask  = bitmask;
     }
     else {
        throw new Exception("Either bitmask or bit names must be provided");
     }
     // Limit value to permitted range
     fValue = initialValue & fBitmask;
     
     // Create button array
     long highestOne = Long.highestOneBit(fBitmask);
     int numButtons = Long.numberOfTrailingZeros(highestOne)+1;
     if ((fBitNames != null) && (numButtons != fBitNames.length)) {
        throw new Exception("Number of names provided ("+fBitNames.length+") from '"+String.join(",", fBitNames)+"' doesn't match mask 0x"+Long.toBinaryString(fBitmask));
     }
     if (highestOne != 0) {
        fButtons  = new Button[numButtons];
     }
     else {
        fButtons = null;
     }
     if (fBitNames != null) {
        for (int index=0; index<fBitNames.length; index++) {
           fBitNames[index] = fBitNames[index].trim();
        }
     }
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
      container.setToolTipText(fVariable.getToolTip());
      int width = 8;
      if (fBitNames != null) {
         if (fBitNames.length<width) {
            width = fBitNames.length;
         }
      }
      else {
         if (fButtons.length<width) {
            width = fButtons.length;
         }
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
         for (int i=0; i<32; i++) {
            long mask = (1L<<i);
            if (mask > fBitmask) {
               break;
            }
            fButtons[i] = null;
            if (fBitNames != null) {
               // If names are given only create required buttons
               if ((fBitmask & mask) != 0) {
                  Button btn = new Button(container, SWT.CHECK);
                  String text = fBitNames[i];
                  if ((text == null) || text.isBlank()) {
                     text = fElementName.replaceAll("%i", Integer.toString(i));
                  }
                  btn.setText(text);
                  btn.setSelection((fValue & mask) != 0);
                  btn.setData(i);
                  fButtons[i] = btn;
               }
            }
            else {
               // Create all buttons but selectively enabled
               Button btn = new Button(container, SWT.CHECK);
               btn.setText(fElementName.replaceAll("%i", Integer.toString(i)));
               btn.setSelection((fValue & mask) != 0);
               btn.setEnabled((fBitmask & mask) != 0);
//               Color c = Display.getCurrent().getSystemColor(changedFromDefault?SWT.COLOR_BLACK:SWT.COLOR_GRAY);
//               btn.setForeground(c);
               btn.setData(i);
               fButtons[i] = btn;
            }
            if (fButtons[i] != null) {
               fButtons[i].setToolTipText(fVariable.getToolTip());
//               fButtons[i].addSelectionListener(new SelectionListener() {
//                  @Override
//                  public void widgetSelected(SelectionEvent e) {
//                     Button b = (Button) e.widget;
//                     long mask  = 1<<((int) b.getData());
//                     long value = (b.getSelection()?mask:0);
//                     boolean changedFromDefault = (((value^fDefaultValue) & mask) != 0);
//                     Color c = Display.getCurrent().getSystemColor(changedFromDefault?SWT.COLOR_BLACK:SWT.COLOR_TITLE_INACTIVE_BACKGROUND);
//                     b.setForeground(c);
//                  }
//
//                  @Override
//                  public void widgetDefaultSelected(SelectionEvent e) {
//                  }
//               });
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
   public long getResult() {
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
               fValue |= btn.getSelection()?(1L<<index):0;
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

   public static void main(String[] args) throws Exception {
      Display display = new Display();

      Shell shell = new Shell(display, SWT.DIALOG_TRIM|SWT.CENTER);
      shell.setText("Device Editor");
      shell.setLayout(new FillLayout());
      shell.setSize(600, 600);
      
      long selection = 0xFF;
      BitmaskVariable var = new BitmaskVariable("Name", "Key");
      int index=0;
      String names[]    = {",This is #1,,,,This is #5,,", "Num%i", "",   null, ",#1,,,,#5,#6,#7"};
      long   bitmasks[] = {0xA2,                           0xA2,   0xA2, 0xA2, 0};
      while(true) {
         
         // BitmaskVariable bitmaskVariable, Shell parentShell, String bitNames, long bitmask, long initialValue, long defaultValue
         BitmaskDialogue editor = new BitmaskDialogue(var, shell, names[index], bitmasks[index], selection);
         if  (editor.open() != OK) {
            break;
         }
         selection = editor.getResult();
         System.err.println("Result = " + Long.toBinaryString(selection));
         index++;
         if (index>=names.length) {
            index = 0;
         }
//         System.err.println("res = " + editor.getResultAsList() + " (0x" + Long.toHexString(editor.getResult())+")");
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
         fBitNames = PinListExpansion.expandPinList(bitList,",");
      }
   }

   public String getTitle() {
      return fTitle;
   }

   public void setTitle(String fTitle) {
      this.fTitle = fTitle;
   }
 }