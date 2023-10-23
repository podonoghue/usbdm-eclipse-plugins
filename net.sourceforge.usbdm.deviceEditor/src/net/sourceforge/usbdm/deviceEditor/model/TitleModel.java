package net.sourceforge.usbdm.deviceEditor.model;

import org.eclipse.jface.viewers.StructuredViewer;

import net.sourceforge.usbdm.deviceEditor.parsers.Expression;
import net.sourceforge.usbdm.deviceEditor.parsers.IExpressionChangeListener;
import net.sourceforge.usbdm.deviceEditor.peripherals.VariableProvider;

public class TitleModel extends BaseModel implements IExpressionChangeListener {

   public static final int TITLE_WIDTH = 50;
   
   public TitleModel(BaseModel parent, String name) {
      super(parent, name);
   }

   @Override
   protected void removeMyListeners() {
   }

   @Override
   public void addChild(BaseModel model) {
      throw new RuntimeException();
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      throw new RuntimeException();
   }

   String makePadding(int pad, char padChar) {
      StringBuilder sb = new StringBuilder();
      for (int index=0; index<pad; index++) {
         sb.append(padChar);
      }
      return sb.toString();
   }
   @Override
   public String getValueAsString() {
      String des = getDescription();
      if ((des == null) || des.isBlank()) {
         return makePadding(TITLE_WIDTH, '-');
      }
      int pad = (int) ((TITLE_WIDTH-1.5*des.length()-2)/2);
      String padding = makePadding(pad, '-');
      return padding + " " + getDescription()+ " " + padding;
   }

   Expression hiddenByExpression = null;
   
   /**
    * Set equation for hiding this title
    * 
    * @param hiddenBy   Expression hiding this title
    * @param provider   Provider for variables used in expression
    * 
    * @throws Exception
    */
   public void setHiddenBy(String hiddenBy, VariableProvider provider) throws Exception {
      if (hiddenBy != null) {
         hiddenByExpression = new Expression(hiddenBy, provider);
         hiddenByExpression.addListener(this);
      }
   }

   @Override
   public void expressionChanged(Expression expression) {
      try {
         setHidden(hiddenByExpression.getValueAsBoolean());
         StructuredViewer v = getViewer();
         if (v != null) {
            v.refresh(getParent());
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
