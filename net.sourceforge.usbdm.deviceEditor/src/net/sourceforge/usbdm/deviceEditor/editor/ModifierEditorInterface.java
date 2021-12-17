package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;

import net.sourceforge.usbdm.deviceEditor.model.SignalModel;

/**
 * Provides display and editing methods for Modifier cell in Peripheral View
 */
public interface ModifierEditorInterface {

   /**
    * Indicates the cell can be edited
    * 
    * @param model   Model associated with cell
    * 
    * @return True indicates can be edited
    */
   public boolean canEdit(SignalModel model);

   /**
    * Get cell editor
    * 
    * @param viewer  Parent composite 
    * 
    * @return Cell editor e.g. CheckboxCellEditor or ChoiceCellEditor
    */
   public CellEditor getCellEditor(TreeViewer viewer);

   /**
    * Gets cell value for editing
    * 
    * @param model   Model associated with cell
    * 
    * @return Cell value for editor use 
    *         (e.g. boolean for CheckboxCellEditor or index for ChoiceCellEditor use)
    */
   public Object getValue(SignalModel model);

   /**
    * Set cell value
    * 
    * @param model   Model associated with cell
    * @param value   Value to set
    *                (e.g. boolean for CheckboxCellEditor or index for ChoiceCellEditor use)
    * @return
    */
   public boolean setValue(SignalModel model, Object value);

   /**
    * Gets cell value for display as text
    * 
    * @param model   Model associated with cell
    * 
    * @return Cell value as text e.g. True/False/ActiveHigh/ActiveLow/RisingEdge
    */
   public String getText(SignalModel model);

   /**
    * Gets Image for use within cell
    * 
    * @param model   Model associated with cell
    * 
    * @return Small image
    */
   public Image getImage(SignalModel model);

   /**
    * Tool-tip to display on sell hover
    * 
    * @param model   Model associated with cell
    * 
    * @return Text to display as tool-tip
    */
   public String getModifierHint(SignalModel model);

}
