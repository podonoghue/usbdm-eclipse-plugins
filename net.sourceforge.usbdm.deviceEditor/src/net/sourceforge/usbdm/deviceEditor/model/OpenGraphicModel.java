package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;
import net.sourceforge.usbdm.deviceEditor.graphicModel.ClockSelectionFigure;
import net.sourceforge.usbdm.deviceEditor.graphicModel.GraphicsDialogue;

public class OpenGraphicModel extends EditableModel implements CellEditorProvider {

   private ClockSelectionFigure fFigure;

   class GraphicEditor extends DialogCellEditor {
      final ClockSelectionFigure fFigure;

      public GraphicEditor(Tree tree, ClockSelectionFigure figure) {
         super(tree, SWT.NONE);
         fFigure = figure;
      }

      @Override
      protected Object openDialogBox(Control paramControl) {
         GraphicsDialogue dialog = new GraphicsDialogue(paramControl.getShell(), getDescription(), fFigure);
         dialog.open();
         return "";
      }
   }

   /**
    * 
    * @param parent        Parent model
    * @param name          Name to use
    * @param description   Description for model
    * @param figure
    * 
    * @note Added as child of parent if not null
    */
   public OpenGraphicModel(BaseModel parent, String name, String description, ClockSelectionFigure figure) {
      super(parent, name);
      setSimpleDescription(description);
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
   protected Object clone() throws CloneNotSupportedException {
      // TODO Auto-generated method stub
      return super.clone();
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new GraphicEditor(tree, fFigure);
   }

   public ClockSelectionFigure getFigure() {
      return fFigure;
   }

}
