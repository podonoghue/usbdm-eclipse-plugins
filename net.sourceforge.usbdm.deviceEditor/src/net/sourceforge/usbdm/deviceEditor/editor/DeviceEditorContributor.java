package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.EditorActionBarContributor;

import net.sourceforge.usbdm.deviceEditor.Activator;

/**
 * Manages the installation/deinstallation of global actions
 */
public class DeviceEditorContributor extends EditorActionBarContributor {
   private DeviceEditor fDeviceEditor;

   @Override
   public void setActiveEditor(IEditorPart targetEditor) {
      if (fDeviceEditor == targetEditor) {
         return;
      }
      if (!(targetEditor instanceof DeviceEditor)) {
         fDeviceEditor = null;
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
   }

   class GenerateCodeAction extends MyAction {
      GenerateCodeAction() {
         super("Regenerate Files", IAction.AS_PUSH_BUTTON, Activator.ID_GEN_FILES_IMAGE);
      }

      @Override
      public void run() {
         if (fDeviceEditor != null) {
            fDeviceEditor.generateCode();
         }
      }
   }

   private GenerateCodeAction generateCodeAction = new GenerateCodeAction();

   @Override
   public void contributeToMenu(IMenuManager manager) {
      //    IMenuManager menu = new MenuManager("Editor &Menu");
      //    manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
      //      manager.add(menu);
      IMenuManager menu = manager.findMenuUsingPath("USBDM");
      if (menu != null) {
         menu.add(generateCodeAction);
      }
   }

   @Override
   public void contributeToToolBar(IToolBarManager manager) {
      manager.add(new Separator());
      manager.add(generateCodeAction);
   }
}
