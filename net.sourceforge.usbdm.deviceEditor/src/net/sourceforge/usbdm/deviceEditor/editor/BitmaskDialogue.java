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
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.PinListExpansion;

public class BitmaskDialogue extends Dialog {
   // Associated variable
   private final BitmaskVariable fVariable;
   
   // Mask indicating valid bits (if non-zero)
   private final long      fBitmask;
   
   // Maps button to bit number
   private final Integer[] fBitMapping;
   
   // List of names for the bits
   private final String[] fBitNames;
   
   // List of names for the bits
   private final String[] fDescriptions;
   
   // Buttons within dialogue
   private final Button    fButtons[];

   // Dialogue title
   private String fTitle = "Select pins";
   
   // Current value
   private long fValue;
   
   /**
    * Create dialogue displaying a set of check boxes
    * 
    * @param bitmaskVariable  Variable representing bit mask
    * @param parentShell      Shell<br>
    * 
    * Other information is obtained from the bitmaskVariable e.g.<br>
    * <li> Names for bits e.g. "B0,B2,*,,,B6", "Pin#%i" or null for defaults. '*' or ' ' = unused bit
    * <li> Bit-mask for valid bit selections.  May be zero if bitNames provided.
    * <li> Initial value for mask
    * 
    * <b>Examples:</b>
    * <pre>
    *          bitmask   bitList       Bit usage               BitMapping        Buttons
    *  case 1: 0x27      Pin%i      => Pin0,Pin1,Pin2,,,Pin5   0,1,2,5           Pin0,Pin1,Pin2,Pin5 (generated as needed)
    *  case 2: 0x27      A,B,C,D    => A,B,C,,,D               0,1,2,5           A,B,C,D
    *  case 3: 0x00      A,,B,,,C   => A,,B,,,C                0,3,5             A,B,C
    *  case 4: 0x27      null or '' => B0,B1,B2,,,B5           0,1,2,-1,-1,5     B0,B1,B2,,,B5 (generated as needed, some disabled)
    * </pre>
    * @return
    * 
    * @throws Exception
    */
   public BitmaskDialogue(BitmaskVariable bitmaskVariable, Shell parentShell) throws Exception {
      super(parentShell);
      fVariable = bitmaskVariable;
      
     String tBitList      = bitmaskVariable.getBitList();
     long   tBitmask      = bitmaskVariable.getPermittedBits();
     String tDescriptions = bitmaskVariable.getBitDescriptions();
     
     if  ((tBitmask !=0) && (tBitList != null) && !tBitList.isBlank()) {
        // Both supplied - cases 1,2
        
        // Create bit mapping
        ArrayList<Integer> bitMappingList = new ArrayList<Integer>();
        for (int index=0; index<32; index++) {
           long mask = (1L<<index);
           if (mask>tBitmask) {
              break;
           }
           if ((tBitmask&mask) != 0) {
              bitMappingList.add(index);
           }
        }

        // Create bit names
        String[] tBitNamesArray = tBitList.split(",", -1);
        if (tBitNamesArray.length == 1) {
           // Case 1 - template Pin%i + bitmask
           // Create names as needed
           
           String[] tDescriptionsArray = null;
           if ((tDescriptions != null) && !tDescriptions.isBlank()) {
              tDescriptionsArray = tDescriptions.split(",", -1);
              if (tDescriptionsArray.length != tBitNamesArray.length) {
                 throw new Exception("# of bit names does not match # of descriptions");
              }
           }
           ArrayList<String>  bitNameList        = new ArrayList<String>();
           ArrayList<String>  bitDescriptionList = new ArrayList<String>();
           for (int index=0; index<32; index++) {
              long mask = (1L<<index);
              if (mask>tBitmask) {
                 break;
              }
              if ((tBitmask&mask) != 0) {
                 bitNameList.add(tBitNamesArray[0].replaceAll("%i", Integer.toString(index)));
                 if (tDescriptionsArray != null) {
                    bitDescriptionList.add(tDescriptionsArray[0].replaceAll("%i", Integer.toString(index)));
                 }
              }
           }
           fBitNames = bitNameList.toArray(new String[bitNameList.size()]);
           if (tDescriptionsArray != null) {
              fDescriptions = bitDescriptionList.toArray(new String[bitNameList.size()]);
           }
           else {
              fDescriptions = null;
           }
//           System.err.println("Case 1: " + Long.toBinaryString(tBitmask) + " | " +  bitMappingList.toString() +" | " + Arrays.toString(fBitNames) + " | " + Arrays.toString(fDescriptions));
        }
        else {
           // Case 1 - Names list Pin1,Pin2,,Pin3 + bitmask
           fBitNames     = tBitNamesArray;
           if (tDescriptions != null) {
              fDescriptions = tDescriptions.split(",", -1);
           }
           else {
              fDescriptions = null;
           }
//           System.err.println("Case 2: " + Long.toBinaryString(tBitmask) + " | " +  bitMappingList.toString() +" | " + Arrays.toString(fBitNames) + " | " + Arrays.toString(fDescriptions));
        }
        fBitMapping = bitMappingList.toArray(new Integer[bitMappingList.size()]);
        fBitmask    = tBitmask;
     }
     else if  ((tBitmask == 0) && (tBitList != null) && !tBitList.isBlank()) {
        // Case 3 - Bitmask=0, Names provided
        // Determine bitmask and indices from names
        String[] bitNamesArray     = PinListExpansion.expandPinList(tBitList, ",");
        String[] descriptionsArray = PinListExpansion.expandPinList(tDescriptions, ",");
        if ((descriptionsArray != null) && (descriptionsArray.length != bitNamesArray.length)) {
           throw new Exception("# of expanded bit names does not match # of expanded descriptions");
        }
        ArrayList<Integer> bitMappingList     = new ArrayList<Integer>();
        ArrayList<String>  bitNamesList       = new ArrayList<String>();
        ArrayList<String>  bitDescriptionList = new ArrayList<String>();
        for (int index=0; index<bitNamesArray.length; index++) {
           long mask = (1L<<index);
           if (!bitNamesArray[index].isBlank()) {
              tBitmask |= mask;
              bitMappingList.add(index);
              bitNamesList.add(bitNamesArray[index]);
              if (descriptionsArray != null) {
                 bitDescriptionList.add(descriptionsArray[index]);
              }
           }
        }
        fBitNames     = bitNamesList.toArray(new String[bitNamesList.size()]);
        if (tDescriptions != null) {
           fDescriptions = bitDescriptionList.toArray(new String[bitDescriptionList.size()]);
        }
        else {
           fDescriptions = null;
        }
        fBitMapping   = bitMappingList.toArray(new Integer[bitMappingList.size()]);
        fBitmask      = tBitmask;
//        System.err.println("Case 3: " + Long.toBinaryString(tBitmask) + " | " +  bitMappingList.toString() + " | " + Arrays.toString(fBitNames) + " | " + Arrays.toString(fDescriptions));
     }
     else if ((tBitmask != 0) && ((tBitList == null)||tBitList.isBlank())) {
        // Case 4 - bitmask only
        // Create default bit names based on bitmask
        ArrayList<Integer> bitMappingList = new ArrayList<Integer>();
        ArrayList<String>  bitnames       = new ArrayList<String>();

        for (int index=0; index<32; index++) {
           long mask = (1L<<index);
           if (mask>tBitmask) {
              break;
           }
           if ((tBitmask&mask) != 0) {
              bitMappingList.add(index);
           }
           else {
              bitMappingList.add(-1);
           }
           bitnames.add("B%i".replaceAll("%i", Integer.toString(index)));
        }
        fBitNames     = bitnames.toArray(new String[bitnames.size()]);
        fDescriptions = null;
        fBitMapping   = bitMappingList.toArray(new Integer[bitMappingList.size()]);
        fBitmask      = tBitmask;
//        System.err.println("Case 4: " + Long.toBinaryString(tBitmask) + " | " +  bitMappingList.toString() + " | " + Arrays.toString(fBitNames) + " | " + Arrays.toString(fDescriptions));
     }
     else {
        throw new Exception("Either bitmask or bit names must be provided");
     }
     for (int index=0; index<fBitNames.length; index++) {
        fBitNames[index] = fBitNames[index].trim();
     }
     if (fDescriptions != null) {
        for (int index=0; index<fDescriptions.length; index++) {
           fDescriptions[index] = fDescriptions[index].trim();
        }
     }
     // Set value to permitted range
     fValue = bitmaskVariable.getValueAsLong() & fBitmask;
     
     // Create buttons array
     fButtons = new Button[fBitNames.length];
   }

   @Override
   protected Control createDialogArea(Composite parent) {
      
      Composite container = (Composite) super.createDialogArea(parent);
      container.setToolTipText(fVariable.getToolTip());
      
      GridLayout gl  = (GridLayout) container.getLayout();
      if (fDescriptions != null) {
         gl.numColumns  = 2;
      }
      else {
         int width = fBitNames.length;
         if (width>8) {
            width = 8;
         }
         gl.numColumns  = width;
      }

      if (fBitmask == 0) {
         Label lbl = new Label(container, SWT.CHECK);
         lbl.setText("No bits selectable");
      }
      else {
         for (int nameIndex=0; nameIndex<fBitNames.length; nameIndex++) {
            Button btn = new Button(container, SWT.CHECK);
            if (fBitMapping[nameIndex]>=0) {
               btn.setSelection((fValue & (1<<fBitMapping[nameIndex])) != 0);
            }
            else {
               btn.setEnabled(false);
            }
            btn.setText(fBitNames[nameIndex]);
            btn.setData(nameIndex);
            btn.setToolTipText(fVariable.getToolTip());
            fButtons[nameIndex] = btn;
            if (fDescriptions != null) {
               Text text = new Text(container, SWT.LEFT);
               text.setText(fDescriptions[nameIndex]);
            }
         }
      }
//      container.layout();
//      parent.layout(true);
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
         if (fBitMapping[buttonIndex]>=0) {
            if ((fValue & (1<<fBitMapping[buttonIndex]))!=0) {
               if (!firstElement) {
                  sb.append(",");
               }
               sb.append(buttonIndex);
               firstElement = false;
            }
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
      for (int buttonIndex=0; buttonIndex<fButtons.length; buttonIndex++) {
         Button btn = fButtons[buttonIndex];
         if (fBitMapping[buttonIndex]>=0) {
            fValue |= btn.getSelection()?(1L<<fBitMapping[buttonIndex]):0;
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
      
      int index=0;
      String descriptions[]   = {",Bit 1,,,,Bit 5,,",           "Bit%i", "",   null, ",(1),,,,(5),(6),(7)", null};
      String names[]          = {",This is #1,,,,This is #5,,", "Num%i", "",   null, ",#1,,,,#5,#6,#7",     "bit%i" };
      long   bitmasks[]       = {0x0L,                            0xA2L,   0xA3L, 0xA7L, 0L,                     0xFFFFFFFFL};
      while(true) {

         BitmaskVariable var = new BitmaskVariable("Name", "Key");
         var.setBitDescription(descriptions[index]);
         var.setPermittedBits(bitmasks[index]);
         var.setBitList(names[index]);
         var.setValue(selection);
//         var.setPinMap();
         
         BitmaskDialogue editor = new BitmaskDialogue(var, shell);
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

   public String getTitle() {
      return fTitle;
   }

   public void setTitle(String fTitle) {
      this.fTitle = fTitle;
   }
 }