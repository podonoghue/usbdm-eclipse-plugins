package net.sourceforge.usbdm.deviceEditor.popUpDialogue;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import net.sourceforge.usbdm.deviceEditor.information.Variable;
import net.sourceforge.usbdm.deviceEditor.information.VariableWithChoices;

public class PopupDialogue extends Dialog {

   private Shell fParentShell;
   private String fTitle;

   public PopupDialogue(Shell parent, String title) {
      super(parent);
      fParentShell = parent;
      fTitle  = title;
   }

   abstract class EditorInfo {
      final Variable var;
      
      public EditorInfo(Variable var) {
         this.var = var;
      }
      
      /**
       * Disposes SWT resources
       */
      abstract public void dispose();
   }
   
   class TextEditor extends EditorInfo {
      
      final Text text;
      
      public TextEditor(Canvas canvas, Variable var, Point p) {
         super(var);

         text = new Text(canvas, SWT.ON_TOP);
         text.setText(var.getValueAsString());
         text.setFocus();
         text.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
         text.setBounds(p.x, p.y, 100, 20);
         text.selectAll();
         text.setVisible(true);
         text.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent keyEvent) {
               if ((keyEvent.keyCode==SWT.CR) || (keyEvent.keyCode==SWT.KEYPAD_CR)) {
                  String value = text.getText();
                  if ((value != null) && !value.isBlank()) {
                     var.setValue(value.trim());
                  }
                  dispose();
               }
               else if (keyEvent.keyCode==SWT.ESC) {
                  dispose();
               }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
            }
         });
      }
      
      @Override
      public void dispose() {
         if (text != null) {
            text.setVisible(false);
            if (!text.isDisposed())
            text.dispose();
         }
      }
      
   }
   
   class ComboEditor extends EditorInfo {
      
      final Combo combo;
      
      public ComboEditor(Canvas canvas, VariableWithChoices var, Point p) {
         super(var);

         combo = new Combo(canvas, SWT.ON_TOP|SWT.READ_ONLY|SWT.DROP_DOWN);
         combo.setItems(var.getVisibleChoiceNames());
         combo.setText(var.getValueAsString());
         combo.setFocus();
         combo.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
         combo.setBounds(p.x,p.y,100,200);
         combo.pack();
         combo.setVisible(true);
         
//         combo.addListener (SWT.DefaultSelection, e -> System.out.println (e.widget + " - Default Selection"));
//         combo.addListener (SWT.Selection,        e -> System.out.println (e.widget + " - Changed"));
         
         combo.addSelectionListener(new SelectionAdapter() {
            
            @Override
            public void widgetSelected(SelectionEvent arg0) {
               String value = combo.getText();
//               System.err.println("Combo value = " + value);
               if ((value != null) && !value.isBlank()) {
                  VariableWithChoices cVar = var;
                  cVar.setValueByName(value.trim());
               }
               dispose();
            }
         });
         
      }
      
      @Override
      public void dispose() {
         if (combo != null) {
            combo.setVisible(false);
            if (!combo.isDisposed())
            combo.dispose();
         }
      }
      
   }
   
   @Override
   public int open() {

      Display display = fParentShell.getDisplay();

      Shell shell = new Shell(fParentShell, SWT.RESIZE|SWT.CLOSE|SWT.MAX);
      shell.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
      shell.setSize(1150,800);
      shell.setText(fTitle);
      shell.setLayout(new FillLayout());

      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      shell.dispose();
      
      return 0;
   }

}
