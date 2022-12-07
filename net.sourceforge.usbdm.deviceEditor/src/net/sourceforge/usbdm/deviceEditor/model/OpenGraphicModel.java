package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;
import net.sourceforge.usbdm.deviceEditor.graphicModel.ClockSelectionFigure;
import net.sourceforge.usbdm.deviceEditor.graphicModel.GraphicsDialogue;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class OpenGraphicModel extends EditableModel implements CellEditorProvider {

   private final ClockSelectionFigure fFigure;
   private final Variable fClockConfigVar;

   class GraphicEditor extends DialogCellEditor {
      final ClockSelectionFigure fFigure;
      final Variable fVar;
      
      public GraphicEditor(Tree tree, ClockSelectionFigure figure, Variable var) {
         super(tree, SWT.NONE);
         fFigure = figure;
         fVar = var;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         StringBuilder description = new StringBuilder();
         description.append(getDescription());
         if (fVar != null) {
            description.append(" : ");
            description.append(fVar.getValueAsString());
         }
         GraphicsDialogue dialog = new GraphicsDialogue(paramControl.getShell(), description.toString(), fFigure);
         dialog.open();
         deactivate();
         return "";
      }

//      @Override
//      protected Button createButton(Composite parent) {
//         Button button = new Button(parent, SWT.NONE);
//         button.setText("Click Me!");
//         Rectangle r = parent.getBounds();
//         button.setBounds(r);
////         button.pack();
//         return button;
////         return super.createButton(parent);
//      }
//
//      @Override
//      protected Control createContents(Composite cell) {
//         Button button = new Button(cell, SWT.NONE);
//         button.setText("");
//         button.pack();
//         return button;
////         return super.createContents(cell);
//      }

      @Override
      protected Control createControl(Composite parent) {
         Button button = new Button(parent, SWT.NONE);
         button.setText("Click Me!");
         button.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(MouseEvent e) {
               openDialogBox(button);
            }

         });
         return button;
//         return super.createControl(parent);
      }

      @Override
      protected void doSetFocus() {
      }
      
      
   }

   /**
    * 
    * @param parent           Owning mode
    * @param key              Name to display
    * @param clockConfigVar   Associated variable
    * @param figure           Figure to display when clicked.
    */
   public OpenGraphicModel(BaseModel parent, String key, Variable clockConfigVar, ClockSelectionFigure figure) {
      super(parent, Variable.getBaseNameFromKey(key));
      fClockConfigVar    = clockConfigVar;
      fFigure = figure;
   }

   @Override
   public String getToolTip() {
      return super.getToolTip();
   }

   @Override
   public void setValueAsString(String value) {
   }

   @Override
   public String getValueAsString() {
      return "Click to open dialogue";
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new GraphicEditor(tree, fFigure, fClockConfigVar);
   }

   public ClockSelectionFigure getFigure() {
      return fFigure;
   }

}
