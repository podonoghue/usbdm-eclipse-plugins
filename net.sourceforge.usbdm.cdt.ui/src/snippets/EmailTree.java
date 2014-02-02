package snippets;
/*
Java Swing, 2nd Edition
By Marc Loy, Robert Eckstein, Dave Wood, James Elliott, Brian Cole
ISBN: 0-596-00408-7
Publisher: O'Reilly 
*/
// EmailTree.java
//A Simple test to see how we can build a tree and populate it. This
//application also uses custom renderers and editors.
//

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.CellEditor;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellEditor;

public class EmailTree extends JFrame {

  /**
    * 
    */
   private static final long serialVersionUID = -2581200064466675066L;

JTree tree;

  String[][] addresses = {
      { "paul@work.domain", "ptw@work.domain", "other@volunteer.domain" },
      { "paul@home.domain" },
      { "damian@work.domain", "damian@bigisp.domain" },
      { "paged@pager.domain" },
      { "damian@home.domain", "mosh@home.domain" }, { "angela@home.com" } };

  public EmailTree() {
    super("Hashtable Test");
    setSize(400, 300);
    setDefaultCloseOperation(EXIT_ON_CLOSE); // 1.3 & higher
    // addWindowListener(new BasicWindowMonitor()); // 1.1 & 1.2
  }

  public void init() {
    Hashtable<String, Hashtable<String, String[]>> h = new Hashtable<String, Hashtable<String, String[]>>();
    Hashtable<String, String[]> paul = new Hashtable<String, String[]>();
    paul.put("Work", addresses[0]);
    paul.put("Home", addresses[1]);
    Hashtable<String, String[]> damian = new Hashtable<String, String[]>();
    damian.put("Work", addresses[2]);
    damian.put("Pager", addresses[3]);
    damian.put("Home", addresses[4]);
    Hashtable<String, String[]> angela = new Hashtable<String,String[]>();
    angela.put("Home", addresses[5]);
    h.put("Paul", paul);
    h.put("Damian", damian);
    h.put("Angela", angela);
    tree = new JTree(h);

    DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) tree.getCellRenderer();
    renderer.setOpenIcon(new ImageIcon("mailboxdown.gif"));
    renderer.setClosedIcon(new ImageIcon("mailboxup.gif"));
    renderer.setLeafIcon(new ImageIcon("letter.gif"));
    EmailTreeCellEditor emailEditor = new EmailTreeCellEditor();
    DefaultTreeCellEditor editor = new DefaultTreeCellEditor(tree, renderer, emailEditor);
    tree.setCellEditor(editor);
    tree.setEditable(true);

    getContentPane().add(tree, BorderLayout.CENTER);
  }

  public static void main(String args[]) {
    EmailTree tt = new EmailTree();
    tt.init();
    tt.setVisible(true);
  }
}

//EmailTreeCellEditor.java
//An editor that actually manages two separate editors: one for folders
//(nodes) that uses a combobox; and one for files (leaves) that uses a
//textfield.
//

class EmailTreeCellEditor implements TreeCellEditor {

  EditorComboBox nodeEditor;

  EmailEditor leafEditor;

  CellEditor currentEditor;

  static String[] emailTypes = { "Home", "Work", "Pager", "Spam" };

  public EmailTreeCellEditor() {

    EmailEditor    tf = new EmailEditor();
    EditorComboBox cb = new EditorComboBox(emailTypes);

    nodeEditor = cb;
    leafEditor = tf;
  }

  public Component getTreeCellEditorComponent(JTree tree, Object value,
      boolean isSelected, boolean expanded, boolean leaf, int row) {
    if (leaf) {
      currentEditor = leafEditor;
      leafEditor.setText(value.toString());
    } else {
      currentEditor = nodeEditor;
      nodeEditor.setSelectedItem(((DefaultMutableTreeNode) value)
          .getUserObject());
    }
    return (Component) currentEditor;
  }

  public Object getCellEditorValue() {
    return currentEditor.getCellEditorValue();
  }

  // All cells are editable in this example...
  public boolean isCellEditable(EventObject event) {
    return true;
  }

  public boolean shouldSelectCell(EventObject event) {
    return currentEditor.shouldSelectCell(event);
  }

  public boolean stopCellEditing() {
    return currentEditor.stopCellEditing();
  }

  public void cancelCellEditing() {
    currentEditor.cancelCellEditing();
  }

  public void addCellEditorListener(CellEditorListener l) {
    nodeEditor.addCellEditorListener(l);
    leafEditor.addCellEditorListener(l);
  }

  public void removeCellEditorListener(CellEditorListener l) {
    nodeEditor.removeCellEditorListener(l);
    leafEditor.removeCellEditorListener(l);
  }
}

//EditorComboBox.java
//A CellEditor JComboBox subclass for use with Trees (and possibly tables).
//This version will work with any list of values passed as an Object[].
//

class EditorComboBox extends JComboBox<String> implements CellEditor {

  /**
    * 
    */
   private static final long serialVersionUID = 4083436939026553675L;

String value;

  Vector<CellEditorListener> listeners = new Vector<CellEditorListener>();

  // Mimic all the constructors people expect with ComboBoxes.
  public EditorComboBox(String[] list) {
    super(list);
    setEditable(false);
    value = list[0].toString();

    // Listen to our own action events so that we know when to stop editing.
    addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if (stopCellEditing()) {
          fireEditingStopped();
        }
      }
    });
  }

  // Implement the CellEditor methods.
  public void cancelCellEditing() {
  }

  // Stop editing only if the user entered a valid value.
  public boolean stopCellEditing() {
    try {
      value = (String) getSelectedItem();
      if (value == null) {
        value = (String) getItemAt(0);
      }
      return true;
    } catch (Exception e) {
      // Something went wrong.
      return false;
    }
  }

  public Object getCellEditorValue() {
    return value;
  }

  // Start editing when the right mouse button is clicked.
  public boolean isCellEditable(EventObject eo) {
    if ((eo == null)
        || ((eo instanceof MouseEvent) && (((MouseEvent) eo)
            .isMetaDown()))) {
      return true;
    }
    return false;
  }

  public boolean shouldSelectCell(EventObject eo) {
    return true;
  }

  // Add support for listeners.
  public void addCellEditorListener(CellEditorListener cel) {
    listeners.addElement(cel);
  }

  public void removeCellEditorListener(CellEditorListener cel) {
    listeners.removeElement(cel);
  }

  protected void fireEditingStopped() {
    if (listeners.size() > 0) {
      ChangeEvent ce = new ChangeEvent(this);
      for (int i = listeners.size() - 1; i >= 0; i--) {
        ((CellEditorListener) listeners.elementAt(i))
            .editingStopped(ce);
      }
    }
  }
}

//EmailEditor.java
//An extension of JTextField that requires an "@" somewhere in the field.
//Meant to be used as a cell editor within a JTable or JTree.
//

class EmailEditor extends JTextField implements CellEditor {
  /**
    * 
    */
   private static final long serialVersionUID = -7473467961644171439L;

String value = "";

  Vector<CellEditorListener> listeners = new Vector<CellEditorListener>();

  // Mimic all the constructors people expect with text fields.
  public EmailEditor() {
    this("", 5);
  }

  public EmailEditor(String s) {
    this(s, 5);
  }

  public EmailEditor(int w) {
    this("", w);
  }

  public EmailEditor(String s, int w) {
    super(s, w);
    // Listen to our own action events so that we know when to stop editing.
    addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        if (stopCellEditing()) {
          fireEditingStopped();
        }
      }
    });
  }

  // Implement the CellEditor methods.
  public void cancelCellEditing() {
    setText("");
  }

  // Stop editing only if the user entered a valid value.
  public boolean stopCellEditing() {
    try {
      String tmp = getText();
      int at = tmp.indexOf("@");
      if (at != -1) {
        value = tmp;
        return true;
      }
      return false;
    } catch (Exception e) {
      // Something went wrong (most likely we don't have a valid integer).
      return false;
    }
  }

  public Object getCellEditorValue() {
    return value;
  }

  // Start editing when the right mouse button is clicked.
  public boolean isCellEditable(EventObject eo) {
    if ((eo == null) || ((eo instanceof MouseEvent) && (((MouseEvent) eo).isMetaDown()))) {
      return true;
    }
    return false;
  }

  public boolean shouldSelectCell(EventObject eo) {
    return true;
  }

  // Add support for listeners.
  public void addCellEditorListener(CellEditorListener cel) {
    listeners.addElement(cel);
  }

  public void removeCellEditorListener(CellEditorListener cel) {
    listeners.removeElement(cel);
  }

  protected void fireEditingStopped() {
    if (listeners.size() > 0) {
      ChangeEvent ce = new ChangeEvent(this);
      for (int i = listeners.size() - 1; i >= 0; i--) {
        ((CellEditorListener) listeners.elementAt(i)).editingStopped(ce);
      }
    }
  }
}

           
         
    