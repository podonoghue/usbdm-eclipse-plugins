package net.sourceforge.usbdm.deviceEditor.editor;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import net.sourceforge.usbdm.deviceEditor.information.LongVariable;
//import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class RtcTimeDialogue extends Dialog {
   private DateTime fCalenda;
   private DateTime fTime;
   private long fResult = 0;
   
   /**
    * Create dialogue displaying a set of pin selection spinners
    * 
    * @param parentShell
    * @param numValues          Number of spinners to display (starts at zero)
    */
   public RtcTimeDialogue(Shell parentShell, LongVariable variable) {
     super(parentShell);
   }
   
   @Override
   protected Control createDialogArea(Composite parent) {
      
      Composite container = (Composite) super.createDialogArea(parent);
      GridLayout gl = new GridLayout();
      gl.numColumns = 1;
      gl.marginLeft = 10;
      gl.marginRight = 10;
      gl.marginTop = 10;
      container.setLayout(gl);
      container.layout();

      fCalenda = new DateTime(parent, SWT.CALENDAR);
      fTime    = new DateTime(parent, SWT.TIME);
      
      parent.pack();
      
      parent.layout(true);
      return container;
   }

   /**
    * Get dialogue result
    * 
    * @return result
    */
   public long getResult() {

      return fResult;
   }
   
   /**
    * Get dialogue result
    * 
    * @return result
    */
   public String getResultAsString() {
      
      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
      return sdf.format(new Date(fResult));
   }
   
   @Override
   protected void okPressed() {

      Calendar cal = new GregorianCalendar();
      cal.set(
            fCalenda.getYear(),
            fCalenda.getMonth(),
            fCalenda.getDay(),
            fTime.getHours(),
            fTime.getMinutes(),
            fTime.getSeconds());
      
      fResult = cal.getTimeInMillis();
      
      super.okPressed();
   };
   
   @Override
   protected void configureShell(Shell newShell) {
     super.configureShell(newShell);
     newShell.setText("RTC Date & Time");
   }

   public static void main(String[] args) {
      Display display = new Display();

      Shell shell = new Shell(display);
      shell.setSize(800, 800);
      shell.setLayout(new RowLayout());

      LongVariable var = new LongVariable(null, null, "A variable", "/avar");
      while(true) {
         RtcTimeDialogue editor = new RtcTimeDialogue(shell, var);
         if  (editor.open() != OK) {
            break;
         }
         String selection = editor.getResultAsString();
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
