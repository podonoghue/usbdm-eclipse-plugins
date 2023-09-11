package net.sourceforge.usbdm.deviceEditor.model;

public class TitleModel extends BaseModel {

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

}
