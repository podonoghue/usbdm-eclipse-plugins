package net.sourceforge.usbdm.deviceEditor.editor;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

import net.sourceforge.usbdm.deviceEditor.model.BaseModel;

abstract class BaseLabelProvider extends LabelProvider implements IStyledLabelProvider, IToolTipProvider{

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
      if (baseModel.isError()) {
         return new StyledString(text, ERROR_STYLER);
      }
      if (baseModel.isWarning()) {
         return new StyledString(text, WARNING_STYLER);
      }
      if (baseModel.isReset()) {
         return new StyledString(text, DISABLED_STYLER);
      }
      if (!baseModel.isEnabled()) {
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

   public abstract String getText(BaseModel model);
   public abstract Image getImage(BaseModel model);
}
