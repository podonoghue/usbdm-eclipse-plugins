package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.Activator;
import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

abstract class BaseLabelProvider extends StyledCellLabelProvider  implements IStyledLabelProvider {

   protected static Image errorImage       = null;
   protected static Image warningImage     = null;
   protected static Image lockedImage      = null;
   protected static Image checkedImage     = null;
   protected static Image uncheckedImage   = null;
   protected static Image greycheckedImage = null;
   protected static Image disabledImage    = null;
   protected static Image emptyImage       = null;
   private   static int   referenceCount   = 0;
   
   BaseLabelProvider() {
      referenceCount++;
//      System.err.println("BaseLabelProvider(), ref = " + referenceCount);
      if ((errorImage == null) && (Activator.getDefault() != null)) {
         errorImage         = Activator.getImageDescriptor(Activator.ID_ERROR_NODE_IMAGE).createImage();
         warningImage       = Activator.getImageDescriptor(Activator.ID_WARNING_NODE_IMAGE).createImage();
         lockedImage        = Activator.getImageDescriptor(Activator.ID_LOCKED_NODE_IMAGE).createImage();
         checkedImage       = Activator.getImageDescriptor(Activator.ID_CHECKBOX_CHECKED_IMAGE).createImage();
         uncheckedImage     = Activator.getImageDescriptor(Activator.ID_CHECKBOX_UNCHECKED_IMAGE).createImage();
         greycheckedImage   = Activator.getImageDescriptor(Activator.ID_CHECKBOX_GREYED_IMAGE).createImage();
         disabledImage      = Activator.getImageDescriptor(Activator.ID_DISABLED_IMAGE).createImage();
         emptyImage         = Activator.getImageDescriptor(Activator.ID_EMPTY_IMAGE).createImage();
      }
   }

   protected static final Styler CATEGORY_STYLER  = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
         textStyle.font = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
      }
   };
   
   protected static final Styler ERROR_STYLER  = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
         textStyle.font = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
         textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_RED);
      }
   };
   
   protected static final Styler WARNING_STYLER  = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
         textStyle.font = JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
         textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
      }
   };
   
   protected static final Styler DISABLED_STYLER  = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
         textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
      }
   };

   protected static final Styler DEFAULT_STYLER  = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
         textStyle.foreground = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
      }
   };
   
   @Override
   public StyledString getStyledText(Object element) {
      if (!(element instanceof BaseModel)) {
         return new StyledString("");
      }
      BaseModel baseModel = (BaseModel) element;
      
      String text = getText(baseModel);
      if ((text == null)||(text.length() == 0)) {
         return new StyledString("");
      }
      if (!baseModel.isEnabled()) {
         return new StyledString(text, DISABLED_STYLER);
      }
      if (baseModel.isError()) {
         return new StyledString(text, ERROR_STYLER);
      }
      if (baseModel.isWarning()) {
         return new StyledString(text, WARNING_STYLER);
      }
      if (baseModel.isInactive()) {
         return new StyledString(text, DISABLED_STYLER);
      }
      if (baseModel.hasChildren()) {
         return new StyledString(text, CATEGORY_STYLER);
      }
      return new StyledString(text, DEFAULT_STYLER);
   }

   @Override
   public Image getImage(Object element) {
      if (element instanceof BaseModel) {
         return getImage((BaseModel) element);
      }
      return null;
   }

   @Override
   public String getToolTipText(Object element) {
      if (element instanceof BaseModel) {
         return ((BaseModel) element).getToolTip();
      }
      return null;
   }

   @Override
   public void dispose() {
      super.dispose();
//      System.err.println("BaseLabelProvider.dispose(), ref = " + referenceCount);
      if (--referenceCount>0) {
         return;
      }
//      System.err.println("BaseLabelProvider.dispose(), disposing");
      if (errorImage != null) {
         errorImage.dispose();
         errorImage = null;
      }
      if (warningImage != null) {
         warningImage.dispose();
         warningImage = null;
      }
      if (lockedImage != null) {
         lockedImage.dispose();
         lockedImage = null;
      }
      if (checkedImage != null) {
         checkedImage.dispose();
         checkedImage = null;
      }
      if (uncheckedImage != null) {
         uncheckedImage.dispose();
         uncheckedImage = null;
      }
      if (greycheckedImage != null) {
         greycheckedImage.dispose();
         greycheckedImage = null;
      }
      if (disabledImage != null) {
         disabledImage.dispose();
         disabledImage = null;
      }
      if (emptyImage != null) {
         emptyImage.dispose();
         emptyImage = null;
      }
   }

   public abstract String getText(BaseModel model);
   public abstract Image getImage(BaseModel model);
}
