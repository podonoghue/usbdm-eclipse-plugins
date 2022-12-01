import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class TreeEditorButton {
  public static void main(String[] args) {
    Display display = new Display();
    final Shell shell = new Shell(display);
    shell.setText("Text Tree Editor");
    shell.setLayout(new FillLayout());

    final Tree tree = new Tree(shell, SWT.SINGLE);
    for (int i = 0; i < 3; i++) {
      TreeItem iItem = new TreeItem(tree, SWT.NONE);
      iItem.setText("Item " + (i + 1));
      for (int j = 0; j < 3; j++) {
        TreeItem jItem = new TreeItem(iItem, SWT.NONE);
        jItem.setText("Sub Item " + (j + 1));
        for (int k = 0; k < 3; k++) {
          new TreeItem(jItem, SWT.NONE).setText("Sub Sub Item " + (k + 1));
        }
        jItem.setExpanded(true);
      }
      iItem.setExpanded(true);
    }

    final TreeEditor editor = new TreeEditor(tree);
    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;

    tree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent event) {
        final TreeItem item = tree.getSelection()[0];

        final Button bn = new Button(tree, SWT.NONE);
        bn.setText("click");
        bn.setFocus();

        bn.addSelectionListener(new SelectionListener() {

          @Override
         public void widgetSelected(SelectionEvent arg0) {
            int style = SWT.ICON_QUESTION | SWT.YES | SWT.NO;

            MessageBox messageBox = new MessageBox(shell, style);
            messageBox.setMessage("Message");
            int rc = messageBox.open();

            item.setText(rc + "");
            bn.dispose();
          }

          @Override
         public void widgetDefaultSelected(SelectionEvent arg0) {
          }
        });

        editor.setEditor(bn, item);
      }
    });

    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
    display.dispose();
  }
}