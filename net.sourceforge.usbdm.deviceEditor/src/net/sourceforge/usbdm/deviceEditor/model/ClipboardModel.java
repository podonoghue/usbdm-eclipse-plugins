package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.information.ClipboardVariable;
import net.sourceforge.usbdm.deviceEditor.information.Variable;

public class ClipboardModel extends VariableModel implements DoubleClickEventHandler {

   public ClipboardModel(BaseModel parent, Variable variable) {
      super(parent, variable);
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return null;
   }

   @Override
   public ClipboardVariable getVariable() {
      return (ClipboardVariable) super.getVariable();
   }
   @Override
   
   public void handleEvent(Object element) {
      Display display = Display.getCurrent();
      Clipboard cb = new Clipboard(display);
      String textData = getVariable().getValueAsString();
      TextTransfer textTransfer = TextTransfer.getInstance();
      cb.setContents(new Object[] { textData },
          new Transfer[] { textTransfer });
       }

   @Override
   public String getValueAsString() {
      return "Double-click to transfer to clip-board";
   }

   @Override
   public String getToolTip() {
      StringBuilder sb = new StringBuilder();
      sb.append(super.getToolTip());
      sb.append("\n------------------------\n");
      sb.append(getVariable().getValueAsString());
      sb.append("\n------------------------\n");
      return sb.toString();
   }

}
