package Test;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;

public class ListDialogueExample {

   static class MyLabelProvider extends LabelProvider {

   };

   static class MyContentProvider implements ITreeContentProvider  {

      @Override
      public void dispose() {
      }

      @Override
      public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
      }

      @Override
      public Object[] getChildren(Object arg0) {
         return null;
      }

      @Override
      public Object[] getElements(Object arg0) {
         return null;
      }

      @Override
      public Object getParent(Object arg0) {
         return null;
      }

      @Override
      public boolean hasChildren(Object arg0) {
         return false;
      }                                           
   };

   public static void main(String[] args) {

      Display display = new Display();
      Shell shell = new Shell(display);

      ListSelectionDialog dialog =
            new ListSelectionDialog(
                  shell,
                  new String[]{"aa", "bbb"},
                  new MyContentProvider(),
                  new MyLabelProvider(),
                  "Select the resources to save:");
      dialog.setInitialSelections(new Object[]{"1"});
      dialog.setTitle("Save Resources");
      dialog.open();
   }

}
