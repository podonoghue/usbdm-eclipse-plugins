package net.sourceforge.usbdm.deviceEditor.model;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Tree;

import net.sourceforge.usbdm.deviceEditor.editor.CellEditorProvider;
import net.sourceforge.usbdm.deviceEditor.information.DeviceInfo;

public class FilePathModel extends EditableModel  implements CellEditorProvider{

   static class HardwareCellEditor extends DialogCellEditor {

      String currentPath = null;
      
      HardwareCellEditor(Composite parent) {
         super(parent, SWT.NONE);
      }
      
      @Override
      protected void doSetValue(Object value) {
         currentPath = (String) value;
         super.doSetValue(value);
      }

      @Override
      protected Object openDialogBox(Control paramControl) { 
         FileDialog dialog = new FileDialog(paramControl.getShell(), SWT.OPEN);
         dialog.setFilterExtensions(new String [] {"*"+DeviceInfo.HARDWARE_FILE_EXTENSION});
         Path path = Paths.get(currentPath).toAbsolutePath();
         dialog.setFilterPath(path.getParent().toString());
         dialog.setFileName(path.getFileName().toString());
         return dialog.open();
      }
   }

   /**  Model factory associated with this model */
   ModelFactory fFactory;
   
   /**
    * Constructor
    * 
    * @param parent        Parent model
    * @param modelFactory  Model factory associated with this model
    */
   public FilePathModel(BaseModel parent, ModelFactory modelFactory) {
      super(parent, "Hardware");
      
      fFactory = modelFactory;
   }

   @Override
   public String getSimpleDescription() {
      return "Path to hardware description";
   }
   
   @Override
   public void setValueAsString(String value) {
      fFactory.setHardwareFile(value);
   }
   
   @Override
   public String getValueAsString() {
      return fFactory.getDeviceInfo().getSourceFilename();
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public CellEditor createCellEditor(Tree tree) {
      return new HardwareCellEditor(tree);
   }

}
