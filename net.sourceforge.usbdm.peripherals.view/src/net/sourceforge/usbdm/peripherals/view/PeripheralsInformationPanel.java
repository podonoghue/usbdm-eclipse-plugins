package net.sourceforge.usbdm.peripherals.view;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.peripheralDatabase.Enumeration;
import net.sourceforge.usbdm.peripherals.model.BaseModel;
import net.sourceforge.usbdm.peripherals.model.FieldModel;
import net.sourceforge.usbdm.peripherals.model.IModelChangeListener;
import net.sourceforge.usbdm.peripherals.model.MemoryException;
import net.sourceforge.usbdm.peripherals.model.ObservableModel;
import net.sourceforge.usbdm.peripherals.model.PeripheralModel;
import net.sourceforge.usbdm.peripherals.model.RegisterModel;

public class PeripheralsInformationPanel extends StyledText implements ISelectionChangedListener, IModelChangeListener {

   private CheckboxTreeViewer fPeripheralsTreeViewer;
   private BaseModel          fCurrentModel    = null; // Model to get value/description for
   private PeripheralModel    fPeripheralModel = null; // Model used to monitor changes
   
   public PeripheralsInformationPanel(Composite parent, int style, CheckboxTreeViewer peripheralsTreeViewer) {
      super(parent, style);
      fPeripheralsTreeViewer = peripheralsTreeViewer;
      
      // So information panel is updated when selection changes
      peripheralsTreeViewer.addSelectionChangedListener(this);
   }
   
   /**
    * Handles changes in selection of peripheral, register or field in tree view
    * 
    * Updates description in infoPanel
    * Attaches change listener to selected element
    * 
    * @param event
    */
   public void selectionChanged(SelectionChangedEvent event) {
      Object source = event.getSource();
//      System.err.println("PeripheralsInformationPanel.selectionChanged(), source = " + source.getClass());
      if (source == fPeripheralsTreeViewer) {
         ITreeSelection selection = (ITreeSelection) fPeripheralsTreeViewer.getSelection();
         Object uModel = selection.getFirstElement();
//         System.err.println("PeripheralsInformationPanel.selectionChanged(), uModel = " + uModel);
//         if (uModel != null) {
//            System.err.println("PeripheralsInformationPanel.selectionChanged(), uModel = " + uModel.getClass());
//         }
         // Detach from current peripheral
         if (fPeripheralModel != null) {
            fPeripheralModel.removeListener(this);
            fPeripheralModel = null;
         }
         if ((uModel == null) || !(uModel instanceof BaseModel)) {
            fCurrentModel = null;
            updateContent();
            return;
         }
         // Save model element to get values/description from
         fCurrentModel = (BaseModel) uModel;
         // Find peripheral that owns selected model
         BaseModel model = fCurrentModel;
         if (model instanceof FieldModel) {
//            System.err.println("PeripheralsInformationPanel.selectionChanged(), traversing field = " + model);
            model = model.getParent();
         }
         if (model instanceof RegisterModel) {
//            System.err.println("PeripheralsInformationPanel.selectionChanged(), traversing register = " + model);
            model = model.getParent();
         }
         if (model instanceof PeripheralModel) {
            // Attach listener to peripheral
            fPeripheralModel = (PeripheralModel)model;
            fPeripheralModel.addListener(this);
//            System.err.println("PeripheralsInformationPanel.selectionChanged(), listening to = " + model);
         }
//         else {
//            System.err.println("PeripheralsInformationPanel.selectionChanged(), ignoring = " + model);
//            System.err.println("PeripheralsInformationPanel.selectionChanged(), ignoring = " + model.getClass());
//         }
         updateContent();
      }
   }

   /**
    * Updates the peripheralsInformationPanel according to the current tree selection
    */
   public void updateContent() {

      //      ITreeSelection selection = (ITreeSelection) fPeripheralsTreeViewer.getSelection();
      //      Object uModel = selection.getFirstElement();
      //      if ((uModel == null) || !(uModel instanceof BaseModel)) {
      //         return;
      //      }
      //      fCurrentModel = (BaseModel) uModel;
//      System.err.println("PeripheralsInformationPanel.updateContent()");

      setText("");
      if (fCurrentModel == null) {
         return;
      }
      String basicDescription = fCurrentModel.getDescription();
      String valueString = "";
      if (fCurrentModel instanceof RegisterModel) {
         RegisterModel model = (RegisterModel)fCurrentModel;
         valueString = String.format(" (%s,%s,%s)", model.getValueAsDecimalString(), model.getValueAsHexString(), model.getValueAsBinaryString());
      }
      else if (fCurrentModel instanceof FieldModel) {
         FieldModel model = (FieldModel)fCurrentModel;
         valueString = String.format(" (%s,%s,%s)", model.getValueAsDecimalString(), model.getValueAsHexString(), model.getValueAsBinaryString());
      }
      StringBuffer description     = new StringBuffer();
      StyleRange   valueStyleRange = null;
      int          splitAt         = basicDescription.indexOf("\n");
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
      if (fCurrentModel instanceof FieldModel) {
         FieldModel uField = (FieldModel) fCurrentModel;
         int enumerationIndex = description.length(); // Start of enumeration
         // text
         int      enumerationlength  = 0;  // Length of enumeration text
         int      selectionIndex     = 0;  // Start of highlighted enumeration
         int      selectionLength    = 0;  // Length of highlighted enumeration
         long     enumerationValue   = 0;
         boolean  enumerationValid   = false;
         try {
            enumerationValue = uField.getValue();
            enumerationValid = true;
         } catch (MemoryException e) {
         }
         for (Enumeration enumeration : uField.getEnumeratedDescription()) {
            description.append("\n");
            String enumerationValueDescription = enumeration.getName() + ": " + enumeration.getCDescription();
            if ((selectionIndex==0) && (enumerationValid && enumeration.isSelected(enumerationValue))) {
               // Highlight first matching enumeration
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

   @Override
   public void modelElementChanged(ObservableModel observableModel) {
      // XXX Delete me
      //System.err.println("PeripheralsInformationPanel.modelElementChanged(" + observableModel.getClass() + ")");
      updateContent();
   }

   @Override
   public void modelStructureChanged(ObservableModel observableModel) {
   }

}
