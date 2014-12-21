package net.sourceforge.usbdm.peripherals.view;

import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripherals.model.BaseModel;
import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.MemoryException;
import net.sourceforge.usbdm.peripherals.model.RegisterModel;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class PeripheralsInformationPanel extends StyledText implements ISelectionChangedListener{

   private CheckboxTreeViewer peripheralsTreeViewer;

   public PeripheralsInformationPanel(Composite parent, int style, CheckboxTreeViewer peripheralsTreeViewer) {
      super(parent, style);
      this.peripheralsTreeViewer = peripheralsTreeViewer;
   }

   /**
    * Handles changes in selection of peripheral or register in tree view
    * 
    * Updates description in infoPanel
    */
   public void selectionChanged(SelectionChangedEvent event) {
      Object source = event.getSource();
      if (source == peripheralsTreeViewer) {
         updateContent();
      }
   }

   /**
    * Updates the peripheralsInformationPanel according to the current tree selection
    */
   public void updateContent() {

      setText("");

      ITreeSelection selection = (ITreeSelection) peripheralsTreeViewer.getSelection();
      Object uModel = selection.getFirstElement();
      if ((uModel == null) || !(uModel instanceof BaseModel)) {
         return;
      }
      String basicDescription = ((BaseModel) uModel).getDescription();
      String valueString = "";
      try {
         if (uModel instanceof RegisterModel) {
            RegisterModel model = (RegisterModel)uModel;
            valueString = String.format(" (%d,%s,%s)", model.getValue(), model.getValueAsHexString(), model.getValueAsBinaryString());
         }
         else if (uModel instanceof FieldModel) {
            FieldModel model = (FieldModel)uModel;
            valueString = String.format(" (%d,%s,%s)", model.getValue(), model.getValueAsHexString(), model.getValueAsBinaryString());
         }
      } catch (MemoryException e) {
         valueString = "--invalid--";
      }
      StringBuffer description     = new StringBuffer();
      StyleRange   valueStyleRange = null;
      int splitAt = basicDescription.indexOf("\n");
      if (!valueString.isEmpty()) {
         if (splitAt != -1) {
            description.append(basicDescription.substring(0, splitAt));
            valueStyleRange = new StyleRange(description.length(), valueString.length(), Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.NORMAL);
            description.append(valueString);
            description.append(basicDescription.substring(splitAt)); 
         } else {
            description.append(basicDescription);
            valueStyleRange = new StyleRange(description.length(), valueString.length(), Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.NORMAL);
            description.append(valueString);
         }
      }
      else {
         description.append(basicDescription);
      }
      StyleRange styleRange = new StyleRange(0, description.length(), null, null, SWT.BOLD);
      if (uModel instanceof FieldModel) {
         FieldModel uField = (FieldModel) uModel;
         int enumerationIndex = description.length(); // Start of enumeration
         // text
         int enumerationlength = 0; // Length of enumeration text
         int selectionIndex = 0;    // Start of highlighted enumeration
         int selectionLength = 0;   // Length of highlighted enumeration
         long enumerationValue = 0;
         boolean enumerationValid = false;
         try {
            enumerationValue = uField.getValue();
            enumerationValid = true;
         } catch (MemoryException e) {
         }
         for (Enumeration enumeration : uField.getEnumeratedDescription()) {
            description.append("\n");
            String enumerationValueDescription = enumeration.getName() + ": " + enumeration.getCDescription();
            if (enumerationValid && enumeration.isSelected(enumerationValue)) {
               selectionIndex  = description.length();
               selectionLength = enumerationValueDescription.length();
            }
            enumerationlength += enumerationValueDescription.length();
            description.append(enumerationValueDescription);
         }
         setText(description.toString());
         setStyleRange(styleRange);
         if (valueStyleRange != null) {
            setStyleRange(valueStyleRange);
         }
         styleRange = new StyleRange(enumerationIndex, enumerationlength, null, null, SWT.NORMAL);
         setStyleRange(styleRange);
         styleRange = new StyleRange(selectionIndex, selectionLength, Display.getCurrent().getSystemColor(SWT.COLOR_RED), null, SWT.NORMAL);
         setStyleRange(styleRange);

      } else {
         setText(description.toString());
         setStyleRange(styleRange);
         if (valueStyleRange != null) {
            setStyleRange(valueStyleRange);
         }
      }
   }


}
