package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable;
import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable.BitInformation;
import net.sourceforge.usbdm.deviceEditor.information.BitmaskVariable.BitInformationEntry;

public class BitmaskDialogue extends Dialog {
   // Associated variable
   private final BitmaskVariable fVariable;
   
   // Information for each bit
   private final BitInformation fBitInformation;
   
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
    * @return
    * 
    * @throws Exception
    */
   public BitmaskDialogue(BitmaskVariable bitmaskVariable, Shell parentShell) throws Exception {
      super(parentShell);
      fVariable = bitmaskVariable;
      
      // Information describing each bit
      fBitInformation = bitmaskVariable.getFinalBitInformation();
      
      // Set value to permitted range
      fValue = bitmaskVariable.getValueAsLong() & fBitInformation.permittedBits;

      // Create buttons array
      fButtons = new Button[fBitInformation.bits.length];
   }

   @Override
   protected boolean isResizable() {
      return true;
  }

   @Override
   protected Control createDialogArea(Composite parent) {
      
      Composite container = (Composite) super.createDialogArea(parent);
      container.setToolTipText(fVariable.getToolTip());
      
      GridLayout gl  = (GridLayout) container.getLayout();
      if (fBitInformation.bits[0].description != null) {
         gl.numColumns  = 2;
      }
      else {
         int width = fBitInformation.bits.length;
         if (width>8) {
            width = 8;
         }
         gl.numColumns  = width;
      }

      if (fBitInformation.permittedBits == 0) {
         Label lbl = new Label(container, SWT.CHECK);
         lbl.setText("No bits selectable");
      }
      else {
         for (int nameIndex=0; nameIndex<fBitInformation.bits.length; nameIndex++) {
            boolean specialButton =
                  (fBitInformation.bits[nameIndex].bitNum == BitmaskVariable.BIT_INDEX_ALL) ||
                  (fBitInformation.bits[nameIndex].bitNum == BitmaskVariable.BIT_INDEX_NONE);

            Button btn = new Button(container, specialButton?SWT.PUSH:SWT.CHECK);
            BitInformationEntry bitInfo = fBitInformation.bits[nameIndex];
            btn.setText(bitInfo.bitName+(specialButton?"":" ("+bitInfo.bitNum+")"));
            btn.setData(bitInfo);
            btn.setToolTipText(fVariable.getToolTip());
            btn.setEnabled(true);
            fButtons[nameIndex] = btn;
            if (fBitInformation.bits[nameIndex].description != null) {
               Text text = new Text(container, SWT.LEFT);
               text.setText(fBitInformation.bits[nameIndex].description);
            }
            
            if ((fBitInformation.bits[nameIndex].bitNum == BitmaskVariable.BIT_INDEX_ALL) ||
                (fBitInformation.bits[nameIndex].bitNum == BitmaskVariable.BIT_INDEX_NONE)) {
               btn.addListener(SWT.Selection, new Listener() {
                  @Override
                  public void handleEvent(Event e) {
                     BitInformationEntry bitInfo = (BitInformationEntry) e.widget.getData();
                     boolean select = bitInfo.bitNum == BitmaskVariable.BIT_INDEX_ALL;
                     if (e.type == SWT.Selection) {
                        for (Button b:fButtons) {
                           b.setSelection(select);
                        }
                     }
                  }
               });
            }
            else if (fBitInformation.bits[nameIndex].bitNum>=0) {
               btn.setSelection((fValue & (1<<fBitInformation.bits[nameIndex].bitNum)) != 0);
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
         if (fBitInformation.bits[buttonIndex].bitNum>=0) {
            if ((fValue & (1<<fBitInformation.bits[buttonIndex].bitNum))!=0) {
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
         if (fBitInformation.bits[buttonIndex].bitNum>=0) {
            fValue |= btn.getSelection()?(1L<<fBitInformation.bits[buttonIndex].bitNum):0;
         }
      }
      fVariable.setValue(fValue);
      super.okPressed();
   }
   
   @Override
   protected void configureShell(Shell newShell) {
     super.configureShell(newShell);
     newShell.setText(fTitle);
   }

   static class TestData {
      public long     permittedBits;       // e.g. 0x23
      public String   bitList;             // Pin%i or A,B,C,D
      public String   bitDescriptions;     // null or Desc1, Desc2  or Desc%i
      public String   testName;
      
      public TestData(long permittedBits, String bitList, String bitDescriptions, String testName) {
         this.permittedBits   = permittedBits;
         this.bitList         = bitList;
         this.bitDescriptions = bitDescriptions;
         this.testName        = testName;
      }
   };
   
   public static void main(String[] args) throws Exception {
      Display display = new Display();

      Shell shell = new Shell(display, SWT.DIALOG_TRIM|SWT.CENTER);
      shell.setText("Device Editor");
      shell.setLayout(new FillLayout());
      shell.setSize(600, 600);
      
      long selection = 0xFF;
      
      int index=0;
      
      TestData[] testData = {
//                        permittedBits    bitNames                      bitDescriptions
            new TestData( 0x36L,         "Pin%i",                        null,                          "Case 1a     " ),
            new TestData( 0x36L,         "Pin%i",                        "Desc %n p=%i",                "Case 1b     " ),
            new TestData( 0x36L,         "a,b,c,d",                      null,                          "Case 2a     " ),
            new TestData( 0x36L,         "Pin a,Pin b,Pin c,Pin d",      "Desc w,Desc x,Desc y,Desc z", "Case 2b     " ),
            new TestData( 0x36L,         "Pin a,Pin b,Pin c,Pin d",      "Desc %i p=%n",                "Case 2c     " ),
            new TestData( 0L,            ",#1,,,,#5,#6,#7",              null,                          "Case 3a     " ),
            new TestData( 0L,            ",#1,,,,#5,#6,#7",              ",(1),,,,(5),(6),(7)",         "Case 3b     " ),
            new TestData( 0L,            ",#1,,,,#5,#6,#7",              "Desc %i p=%n",                "Case 3c     " ),
            new TestData( 0x36L,         null,                           null,                          "Case 4      " ),
            new TestData( 0xFFFFFFFFL,   "bit%i",                        null,                          "Full case 1a" ),
            new TestData( 0xFFFFFFFFL,   "Bit%i",                        "Desc %i p=%n",                "Full case 1a" ),
      };
      
      while(true) {

         BitmaskVariable var = new BitmaskVariable(null, "Name", "Key");
         var.init(
               testData[index].permittedBits,
               testData[index].bitList,
               testData[index].bitDescriptions);
         var.setValue(selection);
         
         var.printDebugInfo();
         
         BitmaskDialogue editor = new BitmaskDialogue(var, shell);
         editor.setTitle(testData[index].testName);
         if  (editor.open() != OK) {
            break;
         }
         selection = editor.getResult();
         System.err.println("Result = " + Long.toBinaryString(selection));
         index++;
         if (index>=testData.length) {
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