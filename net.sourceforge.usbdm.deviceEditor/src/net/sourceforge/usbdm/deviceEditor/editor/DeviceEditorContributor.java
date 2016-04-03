package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditor;

import net.sourceforge.usbdm.deviceEditor.Activator;

/**
 * Manages the installation/deinstallation of global actions
 */
public class DeviceEditorContributor extends EditorActionBarContributor {
	private DeviceEditor fDeviceEditor;
	private Action       fGenerateCodeAction;
	
   @Override
   public void setActiveEditor(IEditorPart targetEditor) {
      if (fDeviceEditor == targetEditor)
         return;

      if (!(targetEditor instanceof DeviceEditor)) {
         return;
      }

      fDeviceEditor = (DeviceEditor)targetEditor;

//    IActionBars actionBars = getActionBars();
      
//    if (actionBars != null) {
//       actionBars.setGlobalActionHandler(
//          ActionFactory.DELETE.getId(),
//          getAction(fDeviceEditor, ITextEditorActionConstants.DELETE));
//       actionBars.updateActionBars();
//    }
   }

   /**
	 * Creates a multi-page contributor.
	 */
	public DeviceEditorContributor() {
		super();
		createActions();
	}
	
	/**
	 * Returns the action registered with the given text editor.
	 * @return IAction or null if editor is null.
	 */
	protected IAction getAction(ITextEditor editor, String actionID) {
		return (editor == null ? null : editor.getAction(actionID));
	}
	
	private void createActions() {
		fGenerateCodeAction = new Action() {
			public void run() {
			   if (fDeviceEditor != null) {
			      fDeviceEditor.generateCode();
			   }
			}
		};
		fGenerateCodeAction.setText("Regenerate Code");
		fGenerateCodeAction.setToolTipText("Regenerate Code Files");
		fGenerateCodeAction.setImageDescriptor(Activator.getDefault().getImageDescriptor(Activator.ID_COG_IMAGE));
	}
	
	public void contributeToMenu(IMenuManager manager) {
		IMenuManager menu = new MenuManager("Editor &Menu");
		manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
		menu.add(fGenerateCodeAction);
	}
	
	public void contributeToToolBar(IToolBarManager manager) {
		manager.add(new Separator());
		manager.add(fGenerateCodeAction);
	}
}
