package net.sourceforge.usbdm.annotationEditor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

public class MultiPageAnnotationEditor extends MultiPageEditorPart implements IResourceChangeListener {

	private BasicCEditor     editor;
	private AnnotationEditor annotationEditor;
	
	/**
	 * Creates a multi-page annotation editor.
	 * 
	 * Pages:
	 *   Text editor
	 *   Tree-based editor
	 */
	public MultiPageAnnotationEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}
	
	/**
	 * Creates the text editor page.
	 */
	void createTextEditorPage() {
		try {
			editor = new BasicCEditor();
			int index = addPage(editor, getEditorInput());
			setPageText(index, "Source");
//	      IDocumentProvider documentProvider = editor.getDocumentProvider();
//	      System.err.println("documentProvider = "+documentProvider+", " + ((documentProvider != null)?documentProvider.getClass():"null"));
		} catch (PartInitException e) {
			ErrorDialog.openError( getSite().getShell(), "Error creating text editor", null, e.getStatus());
		}
	}
	
	/**
	 * Creates the Tree editor page.
	 */
	void createTreeEditorPage() {
      try {
         annotationEditor = new AnnotationEditor(editor.getDocumentProvider());
         int index = addPage(annotationEditor, getEditorInput());
         setPageText(index, "Configuration");
      } catch (PartInitException e) {
         ErrorDialog.openError( getSite().getShell(), "Error creating annotation editor", null, e.getStatus());
      }
	}
	
	/**
	 * Creates the pages of the editor.
	 */
	protected void createPages() {
		createTextEditorPage();
      createTreeEditorPage();
      setActiveEditor(annotationEditor);
	}
	
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}
	
	/**
	 * Saves the editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
	   editor.doSave(monitor);
	}
	
	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
	public void doSaveAs() {
	   editor.doSaveAs();
		setInput(editor.getEditorInput());
	}
	
	/* (non-Javadoc)
	 * Method declared on IEditorPart 
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(1), marker);
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		super.init(site, editorInput);
      setPartName(editorInput.getName());
      setContentDescription("A configuration editor");
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	/**
	 * Updates the Annotation editor page when activated.
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		IEditorPart editorPart = getEditor(newPageIndex);
		if (editorPart instanceof AnnotationEditor) {
		   ((AnnotationEditor)editorPart).refresh();
		}
	}
	
	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event){
		if(event.getType() == IResourceChangeEvent.PRE_CLOSE){
			Display.getDefault().asyncExec(new Runnable(){
				public void run(){
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i<pages.length; i++){
						if(((FileEditorInput)editor.getEditorInput()).getFile().getProject().equals(event.getResource())){
							IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart,true);
						}
					}
				}            
			});
		}
	}
}
