package net.sourceforge.usbdm.annotationEditor;

import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
//import org.eclipse.swt.widgets.Event;
//import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;

import net.sourceforge.usbdm.annotationEditor.AnnotationModel.BitField;

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

   class MyValueColumnLabelProvider extends ColumnLabelProvider {
      Image fUncheckedImage = null;
      Image fCheckedImage   = null;
      Image fLockedImage    = null;
      final Color disabledColour = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);

      @Override
      public Image getImage(Object element) {
         if ((fUncheckedImage == null) && (Activator.getDefault() != null)) {
            fUncheckedImage = Activator.getImageDescriptor(Activator.ID_CHECKBOX_UNCHECKED_IMAGE).createImage();
         }
         if ((fCheckedImage == null) && (Activator.getDefault() != null)) {
            fCheckedImage = Activator.getImageDescriptor(Activator.ID_CHECKBOX_CHECKED_IMAGE).createImage();
         }
         if ((fLockedImage == null) && (Activator.getDefault() != null)) {
            fLockedImage = Activator.getImageDescriptor(Activator.ID_LOCKED_NODE_IMAGE).createImage();
         }
         if (element instanceof AnnotationModelNode) {
            if (!(element instanceof HeadingModelNode) && !((AnnotationModelNode)element).canModify()) {
               return fLockedImage;
            }
            if (element instanceof BinaryOptionModelNode) {
               return ((Boolean)((BinaryOptionModelNode)element).safeGetActiveValue())?fCheckedImage:fUncheckedImage;
            }
            if (element instanceof NumericOptionModelNode) {
               BitField field = ((NumericOptionModelNode)element).getBitField();
               if ((field != null) && (field.getStart() == field.getEnd())) {
                  long value = ((NumericOptionModelNode)element).getValueAsLong();
                  return (value!=0)?fCheckedImage:fUncheckedImage;
               }
            }
         }
         return null;
      }
      
      @Override
      public void dispose(ColumnViewer viewer, ViewerColumn column) {
         if (fUncheckedImage != null) {
            fUncheckedImage.dispose();
         }
         if (fCheckedImage != null) {
            fCheckedImage.dispose();
         }
         if (fLockedImage != null) {
            fLockedImage.dispose();
         }

         super.dispose(viewer, column);
      }

      @Override
      public String getText(Object element) {
         try {
            return ((AnnotationModelNode) element).getDialogueValueAsString();
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
         if (fUncheckedImage != null) {
            fUncheckedImage.dispose();
         }
         if (fCheckedImage != null) {
            fCheckedImage.dispose();
         }
         if (fLockedImage != null) {
            fLockedImage.dispose();
         }
      }
   }
   
   class MyNameColumnLabelProvider extends ColumnLabelProvider {
      private final  Color fErrorColour       = Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW);
      private        Image fErrorImage        = null;
      private        Image fWarningImage      = null;

      @Override
      public Image getImage(Object element) {
         if ((fErrorImage == null) && (Activator.getDefault() != null)) {
            fErrorImage   = Activator.getImageDescriptor(Activator.ID_INVALID_NODE_IMAGE).createImage();
            fWarningImage = Activator.getImageDescriptor(Activator.ID_WARNING_NODE_IMAGE).createImage();
         }
         if (element instanceof AnnotationModelNode) {
            switch (((AnnotationModelNode)element).getErrorState()) {
            case OK:
            case INFORMATION:  return null;
            case WARNING:      return fWarningImage;
            case ERROR:        return fErrorImage;
            }
         }
         return null;
      }
      
      @Override
      public void dispose(ColumnViewer viewer, ViewerColumn column) {
         if (fErrorImage != null) {
            fErrorImage.dispose();
         }
         if (fWarningImage != null) {
            fWarningImage.dispose();
         }
         super.dispose(viewer, column);
      }

      @Override
      public String getText(Object element) {
         return ((AnnotationModelNode) element).getDescription();
      }
      
      @Override
      public Color getBackground(Object element) {
         if (element instanceof ErrorNode) {
            return fErrorColour;
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
         if (fErrorImage != null) {
            fErrorImage.dispose();
         }
      }
   }
   
   class MyInformationColumnLabelProvider extends ColumnLabelProvider {

      @Override
      public String getText(Object element) {
         return ((AnnotationModelNode) element).getChoiceTip();
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
   }
   
   public void createControls(Composite parent) {
      viewer = new TreeViewer(parent, SWT.BORDER|SWT.FULL_SELECTION);
      
      Tree tree = viewer.getTree();
      tree.setLinesVisible(true);
      tree.setHeaderVisible(true);
      ColumnViewerToolTipSupport.enableFor(viewer);
      
//      // Suppress tree expansion on double-click
//      // see http://www.eclipse.org/forums/index.php/t/257325/
//      viewer.getControl().addListener(SWT.MeasureItem, new Listener(){
//         @Override
//         public void handleEvent(Event event) {
//         }});
      
      viewer.setContentProvider(new ViewContentProvider());

//      FocusCellOwnerDrawHighlighter highlighter = new FocusCellOwnerDrawHighlighter(viewer) {
//         protected Color getSelectedCellBackgroundColorNoFocus(ViewerCell cell) {
//            return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
//         }
//         protected Color getSelectedCellForegroundColorNoFocus(ViewerCell cell) {
//            return Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
//         }
//      };

//      TreeViewerFocusCellManager focusCellManager     = new TreeViewerFocusCellManager(viewer, highlighter);
      
//      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
//         @Override
//         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
//            return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
//                  || event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
//                  || (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
//                  || event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
//         }
//      };  
      ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(viewer) {
         @Override
         protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
            return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION;
         }
      };

      TreeViewerEditor.create(viewer, actSupport, ColumnViewerEditor.DEFAULT);

      /*
       * Do columns
       */
      TreeViewerColumn column;
      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(300);
      column.getColumn().setText("Option Name");
      column.getColumn().setResizable(true);
      column.setLabelProvider(new MyNameColumnLabelProvider());

      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(450);
      column.getColumn().setText("Option Value");
      column.getColumn().setResizable(true);
      column.setEditingSupport(new AnnotationEditingSupport(viewer));
      column.setLabelProvider(new MyValueColumnLabelProvider());

      column = new TreeViewerColumn(viewer, SWT.NONE);
      column.getColumn().setWidth(450);
      column.getColumn().setText("Information");
      column.getColumn().setResizable(true);
      column.setLabelProvider(new MyInformationColumnLabelProvider());
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
      if (viewer.getControl().isDisposed()) {
         return;
      }
      try {
         if (annotationParser.getModelRoot() == null) {
            // No existing model
            AnnotationModelNode newModel = annotationParser.parse();
            IResource resource = (IResource) getEditorInput().getAdapter(IResource.class);
            if (resource != null) {
               annotationParser.attachErrorMarkers(resource);
//               System.err.println("Attaching error markers");
            }
            annotationParser.setModelRoot(newModel);
            viewer.setInput(newModel);
            viewer.refresh();
         }
         else {
            reconcile();
         }
      } catch (Exception e) {
         e.printStackTrace();
         annotationParser.setModelRoot(null);
         viewer.setInput(annotationParser.constructErrorNode("Error while parsing file: ", e));
         return;
      }
      try {
         annotationParser.collectScannedInformation();
         annotationParser.validate(viewer);
      } catch (Exception e) {
         e.printStackTrace();
         annotationParser.getModelRoot().addChild(annotationParser.constructErrorNode("Error while creating functions: ", e));
//         viewer.setInput(annotationParser.constructErrorNode("Error while parsing file: ", e));
      }
      viewer.refresh();
   }

   public void reconcile() throws Exception {
      AnnotationModelNode newModel     = annotationParser.parse();
      AnnotationModelNode currentModel = annotationParser.getModelRoot();
      
      if (!reconcile(currentModel, newModel)) {
         annotationParser.setModelRoot(newModel);
         viewer.setInput(newModel);
      }
      IResource resource = (IResource) getEditorInput().getAdapter(IResource.class);
      if (resource != null) {
         annotationParser.attachErrorMarkers(resource);
//         System.err.println("Attaching error markers");
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
      return false;
   }

   @Override
   public boolean isSaveAsAllowed() {
      return false;
   }

   @Override
   public void setFocus() {
   }

   @Override
   public void documentAboutToBeChanged(DocumentEvent event) {
   }

   @Override
   public void documentChanged(DocumentEvent event) {
//      System.err.println("AnnotationEditor.documentChanged()");
      refresh();
   }
}
