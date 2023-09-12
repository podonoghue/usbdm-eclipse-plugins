package net.sourceforge.usbdm.deviceEditor.editor;

import java.util.ArrayList;

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
   // Buttons within dialogue
   private       Button    fButtons[];

   // Maps button to bit number
   private       Integer[] fBitMapping;
   
   // List of names for the bits
   private String[] fBitNames = null;
   
   // Current value
   private       long      fValue;
   
   // Mask indicating valid bits (if non-zero)
   private final long      fBitmask;
   
   // Associated variable
   private final BitmaskVariable fVariable;
   
   // Name pattern for buttons e.g. pin%d (if not individually specified)
   private String   fBitNameTemplate = "B%i";
   
   // Dialogue title
   private String   fTitle = "Select pins";
   
   /**
    * Create dialogue displaying a set of check boxes
    * 
    * @param bitmaskVariable  Variable representing bit mask
    * @param parentShell      Shell
    * @param bitNames         Names for bits e.g. "B0,B2,*,,,B6", "Pin#%i" or null for defaults. '*' or ' ' = unused bit
    * @param bitmask          Bit-mask for valid bit selections.  May be zero if bitNames provided.
    * @param initialValue     Initial value for mask<br>
    * 
    * <b>Examples:</b>
    * <pre>
    *          bitmask   bitNames      Bit usage               BitMapping        Buttons
    *  case 1: 0x27      Pin%i      => Pin0,Pin1,Pin2,,,Pin5   0,1,2,5           Pin0,Pin1,Pin2,Pin5 (generated as needed)
    *  case 2: 0x27      A,B,C,D    => A,B,C,,,D               0,1,2,5           A,B,C,D
    *  case 3: 0x00      A,,B,,,C   => A,,B,,,C                0,3,5             A,B,C
    *  case 4: 0x27      null or '' => B0,B1,B2,,,B5           -1,1,2,3,-1,-1,5  B0,B1,B2,B3,B4,B5 (generated as needed, some disabled)
    * </pre>
    * 
    * @throws Exception
    */
   public BitmaskDialogue(BitmaskVariable bitmaskVariable, Shell parentShell, String bitNames, long bitmask, long initialValue) throws Exception {
     super(parentShell);
     fVariable = bitmaskVariable;
     if  ((bitmask !=0) && (bitNames != null) && !bitNames.isBlank()) {
        // Both supplied - cases 1,2
        String[] t = bitNames.split(",", -1);
        if (t.length == 1) {
           // Case 1
           // Name is a template e.g. Pin%i
           fBitNameTemplate = bitNames;
           fBitNames = null;
        }
        else {
           // Case 2
           fBitNames = t;
        }
        ArrayList<Integer> indices = new ArrayList<Integer>();
        for (int index=0; index<32; index++) {
           long mask = (1L<<index);
           if (mask>bitmask) {
              break;
           }
           if ((bitmask&mask) != 0) {
              indices.add(index);
           }
        }
        fBitMapping = indices.toArray(new Integer[indices.size()]);
        fBitmask    = bitmask;
     }
     else if  ((bitmask == 0) && (bitNames != null)) {
        // Case 3
        // Determine bitmask and indices from names
        String[] tBitNames = PinListExpansion.expandPinList(bitNames, ",");
        long tBitmask = 0;
        ArrayList<Integer> indices = new ArrayList<Integer>();
        ArrayList<String>  names   = new ArrayList<String>();
        for (int index=0; index<tBitNames.length; index++) {
           long mask = (1L<<index);
           if (!tBitNames[index].isBlank()) {
              tBitmask |= mask;
              indices.add(index);
              names.add(tBitNames[index]);
           }
        }
        fBitNames   = names.toArray(new String[names.size()]);
        fBitMapping = indices.toArray(new Integer[indices.size()]);
        fBitmask    = tBitmask;
     }
     else if ((bitmask != 0) && (bitNames == null)) {
        // Case 4
        // Create default bit names based on bitmask
        ArrayList<Integer> indices = new ArrayList<Integer>();
        for (int index=0; index<32; index++) {
           long mask = (1L<<index);
           if ((bitmask&mask) != 0) {
              indices.add(index);
           }
           else {
              indices.add(-1);
           }
        }
        fBitMapping = indices.toArray(new Integer[indices.size()]);
        fBitmask    = bitmask;
     }
     else {
        throw new Exception("Either bitmask or bit names must be provided");
     }
     // Limit value to permitted range
     fValue = initialValue & fBitmask;
     
     // Create button array
//     if ((fBitNames != null) && (numButtons != fBitNames.length)) {
//        throw new Exception("Number of names provided ("+fBitNames.length+") from '"+String.join(",", fBitNames)+"' doesn't match mask 0x"+Long.toBinaryString(fBitmask));
//     }
//     if (highestOne != 0) {
//        fButtons  = new Button[numButtons];
//     }
//     else {
//        fButtons = null;
//     }
     fButtons = new Button[fBitMapping.length];
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
      fBitNameTemplate = name;
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
      GridLayout gl  = new GridLayout(width, true);
      gl.marginLeft  = 10;
      gl.marginRight = 10;
      gl.marginTop   = 10;
      container.setLayout(gl);
      container.layout();

      if (fBitmask == 0) {
         Label lbl = new Label(container, SWT.CHECK);
         lbl.setText("No bits selectable");
      }
      else {
         int nameIndex = 0;
         for (int i=0; i<32; i++) {
            long mask = (1L<<i);
            if (mask > fBitmask) {
               break;
            }
            fButtons[nameIndex] = null;
            if (fBitNames != null) {
               // If names are given only create required buttons
               if ((fBitmask & mask) != 0) {
                  Button btn = new Button(container, SWT.CHECK);
                  String text = fBitNames[nameIndex];
                  if ((text == null) || text.isBlank()) {
                     text = fBitNameTemplate.replaceAll("%i", Integer.toString(i));
                  }
                  btn.setText(text);
                  btn.setSelection((fValue & mask) != 0);
                  btn.setData(i);
                  fButtons[nameIndex] = btn;
                  btn.setToolTipText(fVariable.getToolTip());
                  nameIndex++;
               }
            }
            else {
               // Create all buttons but selectively enabled
               Button btn = new Button(container, SWT.CHECK);
               btn.setText(fBitNameTemplate.replaceAll("%i", Integer.toString(i)));
               btn.setSelection((fValue & mask) != 0);
               btn.setEnabled((fBitmask & mask) != 0);
//               Color c = Display.getCurrent().getSystemColor(changedFromDefault?SWT.COLOR_BLACK:SWT.COLOR_GRAY);
//               btn.setForeground(c);
               btn.setData(i);
               fButtons[i] = btn;
            }
//            if (fButtons[i] != null) {
//               fButtons[i].setToolTipText(fVariable.getToolTip());
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
//            }
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
      for (int buttonIndex=0; buttonIndex<fButtons.length; buttonIndex++) {
         if ((fValue & (1<<fBitMapping[buttonIndex]))!=0) {
            if (!firstElement) {
               sb.append(",");
            }
            sb.append(buttonIndex);
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
         for (int buttonIndex=0; buttonIndex<fButtons.length; buttonIndex++) {
            Control b = fButtons[buttonIndex];
            Button btn = (Button)b;
            if (btn != null) {
               fValue |= btn.getSelection()?(1L<<fBitMapping[buttonIndex]):0;
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