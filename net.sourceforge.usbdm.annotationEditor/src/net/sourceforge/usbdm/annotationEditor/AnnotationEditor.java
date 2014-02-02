package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.AnnotationModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BinaryOptionModelNode;
import net.sourceforge.usbdm.annotationEditor.AnnotationModel.ErrorNode;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 *  USBDM Annotation Editor
 *
 *  Used to edit configuration information contained in a (C) file controlled
 *  by mark-up in the comments. 
 */
public class AnnotationEditor extends EditorPart implements IDocumentListener {
   public static final String ID = "net.sourceforge.usbdm.cdt.ui.annotationEditor";

   private TreeViewer viewer;
   private IDocumentProvider documentProvider = null;
   
   public AnnotationEditor(IDocumentProvider documentProvider) {
      this.documentProvider = documentProvider;
   }

   class MyColumnLabelProvider extends ColumnLabelProvider {
      Image uncheckedImage = null;
      Image checkedImage   = null;
      final Color disabledColour = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

      @Override
      public Image getImage(Object element) {
         if ((Activator.getDefault() != null) && (uncheckedImage == null)) {
            uncheckedImage = Activator.getDefault().getImageDescriptor(Activator.ID_CHECKBOX_UNCHECKED_IMAGE).createImage();
         }
         if ((Activator.getDefault() != null) && (checkedImage == null)) {
            checkedImage = Activator.getDefault().getImageDescriptor(Activator.ID_CHECKBOX_CHECKED_IMAGE).createImage();
         }
         if (element instanceof BinaryOptionModelNode) {
            return ((Boolean)((BinaryOptionModelNode)element).safeGetValue())?checkedImage:uncheckedImage;
         }
         return null;
      }
      
      @Override
      public String getText(Object element) {
         try {
            return ((AnnotationModelNode) element).getValueAsString();
         } catch (Exception e) {
//            e.printStackTrace();
            return e.getMessage();
         }
      }
      
      @Override
      public Color getBackground(Object element) {
         if (!((AnnotationModelNode) element).isEnabled()) {
            return disabledColour;
         }
         return super.getBackground(element);
      }

      @Override
      public Color getForeground(Object element) {
         if (!((AnnotationModelNode) element).isEnabled()) {
            return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
         }
         return super.getForeground(element);
      }

      @Override
      public String getToolTipText(Object element) {
         return ((AnnotationModelNode) element).getToolTip();
      }
      
      @Override
      public Font getFont(Object element) {
         return super.getFont(element);
      }

      @Override
      public void dispose() {
         super.dispose();
         if (uncheckedImage != null) {
            uncheckedImage.dispose();
         }
         if (checkedImage != null) {
            checkedImage.dispose();
         }
      }
   }
   
   public void createControls(Composite parent) {
      viewer = new TreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION);
      viewer.getTree().setLinesVisible(true);
      viewer.getTree().setHeaderVisible(true);
      ColumnViewerToolTipSupport.enableFor(viewer);

      viewer.setContentProvider(new ViewContentProvider());

      FocusCellOwnerDrawHighlighter highlighter = new FocusCellOwnerDrawHighlighter(viewer) {
         protected Color getSelectedCellBackgroundColorNoFocus(ViewerCell cell) {
            return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
         }
         protected Color getSelectedCellForegroundColorNoFocus(ViewerCell cell) {
            return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
         }
      };

      TreeViewerFocusCellManager focusCellManager     = new TreeViewerFocusCellManager(viewer, highlighter);
      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer);

      TreeViewerEditor.create(viewer, focusCellManager, actSupport, 
              ColumnViewerEditor.TABBING_HORIZONTAL
            | ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
            | ColumnViewerEditor.TABBING_VERTICAL 
            | ColumnViewerEditor.KEYBOARD_ACTIVATION);

      /*
       * Do columns
       */
      TreeViewerColumn column;
      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(400);
      column.getColumn().setText("Option Name");
      column.getColumn().setResizable(true);
      column.setLabelProvider(new ColumnLabelProvider() {
         final Color errorColour    = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
         public String getText(Object element) {
            return ((AnnotationModelNode) element).getDescription();
         }
         @Override
         public String getToolTipText(Object element) {
            return ((AnnotationModelNode) element).getToolTip();
         }
         @Override
         public Color getBackground(Object element) {
            if (element instanceof ErrorNode) {
               return errorColour;
            }
            return super.getBackground(element);
         }

      });

      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(150);
      column.getColumn().setText("Option Value");
      column.getColumn().setResizable(true);
      column.setEditingSupport(new AnnotationEditingSupport(viewer));
      column.setLabelProvider(new MyColumnLabelProvider());
   }

   class ViewContentProvider implements ITreeContentProvider {

//      private TreeViewer treeViewer = null;

      @Override
      public void dispose() {
      }

      @Override
      public Object[] getElements(Object inputElement) {
         return ((AnnotationModelNode) inputElement).getChildren().toArray();
      }

      @Override
      public Object[] getChildren(Object parentElement) {
         return getElements(parentElement);
      }

      @Override
      public Object getParent(Object element) {
         return ((AnnotationModelNode)element).getParent();
      }

      @Override
      public boolean hasChildren(Object element) {
         return ((AnnotationModelNode) element).getChildren().size() > 0;
      }

      public void inputChanged(Viewer v, Object oldInput, Object newInput) {
         // Save view
//         this.treeViewer = (TreeViewer) viewer;
         if (oldInput != null) {
            // Remove old input as listener
//            removeListenerFrom((BaseModel) oldInput);
         }
         if (newInput != null) {
            // Add new input as listener
//            addListenerTo((BaseModel) newInput);
         }
      }
   }
   
   AnnotationParser annotationParser = null;

   @Override
   public void createPartControl(Composite parent) {
      parent.setLayout(new FillLayout());
      createControls(parent);
      
      IDocument document = documentProvider.getDocument(getEditorInput());
      document.addDocumentListener(this);
      annotationParser = new AnnotationParser(document);
      refresh();
   }

   public void refresh() {
      try {
         if (annotationParser.getModelRoot() == null) {
            // No existing model
            AnnotationModelNode newModel = annotationParser.parse();
            IResource resource = (IResource) getEditorInput().getAdapter(IResource.class);
            if (resource != null) {
               annotationParser.attachErrorMarkers(resource);
               System.err.println("Attaching error markers");
            }
            annotationParser.setModelRoot(newModel);
            viewer.setInput(newModel);
            viewer.refresh();
            return;
         }
         reconcile();
      } catch (Exception e) {
         e.printStackTrace();
         annotationParser.setModelRoot(null);
         viewer.setInput(annotationParser.constructErrorNode("Error while parsing file: ", e));
      }
   }

   public void reconcile() throws Exception {
      AnnotationModelNode newModel     = annotationParser.parse();
      AnnotationModelNode currentModel = annotationParser.getModelRoot();
      
      if (!reconcile(currentModel, newModel)) {
         annotationParser.setModelRoot(newModel);
         viewer.setInput(newModel);
      }
      viewer.refresh();
      IResource resource = (IResource) getEditorInput().getAdapter(IResource.class);
      if (resource != null) {
         annotationParser.attachErrorMarkers(resource);
         System.err.println("Attaching error markers");
      }

   }
   
   boolean compatibleNodes(AnnotationModelNode n1, AnnotationModelNode n2) {
      if (n1.getClass() != n2.getClass()) {
         return false;
      }
      return true;
   }
   
   private boolean reconcile(AnnotationModelNode currentModelNode, AnnotationModelNode newModelNode) throws Exception {
//      System.err.println("===========================");
//      if (currentModelNode instanceof OptionModelNode) {
//         System.err.println(String.format("reconcile() - cur: %s [ref = %d]", currentModelNode.getDescription(), ((OptionModelNode)currentModelNode).getReferenceIndex()));
//      }
//      if (newModelNode instanceof OptionModelNode) {
//         System.err.println(String.format("reconcile() - new:%s [ref = %d]", newModelNode.getDescription(), ((OptionModelNode)newModelNode).getReferenceIndex()));
//      }
//      System.err.println(String.format("reconcile() - %s ?? %s", currentModelNode.getDescription(), newModelNode.getDescription()));
      currentModelNode.copyFrom(newModelNode);
      if ((currentModelNode.getChildren() == null) || (currentModelNode.getChildren().isEmpty())) {
         currentModelNode.setChildren(newModelNode.getChildren());
//         System.err.println("reconcile() - no children, just copy");
//         if (currentModelNode instanceof OptionModelNode) {
//            System.err.println(String.format("reconcile() - cur: %s [ref = %d]", currentModelNode.getDescription(), ((OptionModelNode)currentModelNode).getReferenceIndex()));
//         }
         return true;
      }
      
//      System.err.println("reconcile() - examining children");
      ArrayList<AnnotationModelNode> currentChildren = currentModelNode.getChildren();
      ArrayList<AnnotationModelNode> newChildren     = newModelNode.getChildren();
      currentModelNode.removeAllChildren();
      ArrayList<AnnotationModelNode> children     = new ArrayList<AnnotationModelNode>();
      
      while (currentChildren.size()>0) {
         AnnotationModelNode currentChild = currentChildren.get(0);
         for (int index=0; index<min(3,newChildren.size()); index++) {
            // Look ahead up to 3 nodes for compatible node
            if (compatibleNodes(currentChild, newChildren.get(index))) {
//               System.err.println(String.format("reconcile() - found compatible child [%d]%s == [%d]%s", 
//                     0, currentChild.getDescription(), index, newChildren.get(index).getDescription()));
               // found one - copy intervening nodes
               for (int index2=0; index2<index; index2++) {
//                  System.err.println(String.format("reconcile() - copying intervening child [%d]%s", 
//                        index2, newChildren.get(0).getDescription()));
                  children.add(newChildren.get(0));
                  newChildren.remove(0);
               }
               // merge found node with current node
               reconcile(currentChild, newChildren.get(0));
               children.add(currentChild);
               newChildren.remove(0);
               break;
            }
         }
//         System.err.println(String.format("reconcile() - removing current child [%d]%s", 0, currentChildren.get(0).getDescription()));
         currentChildren.remove(0);
      }
//      System.err.println(String.format("reconcile() - remaining newChildren = %s", newChildren.size()));
      for (AnnotationModelNode child : newChildren) {
         children.add(child);
      }
      currentModelNode.setChildren(children);

      return true;
   }
   
   private int min(int i, int j) {
      return (i<j)?i:j;
   }

   @Override
   public void doSave(IProgressMonitor monitor) {
   }

   @Override
   public void doSaveAs() {
   }

   @Override
   public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
      if (!(editorInput instanceof IFileEditorInput)) {
         throw new PartInitException("Invalid Input: Must be IFileEditorInput");
      }
      setSite(site);
      setInput(editorInput);
      setPartName(editorInput.getName());
      setContentDescription("A configuration editor");
   }

   @Override
   public boolean isDirty() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public boolean isSaveAsAllowed() {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public void setFocus() {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void documentAboutToBeChanged(DocumentEvent event) {
   }

   @Override
   public void documentChanged(DocumentEvent event) {
      System.err.println("AnnotationEditor.documentChanged()");
      refresh();
   }
}
