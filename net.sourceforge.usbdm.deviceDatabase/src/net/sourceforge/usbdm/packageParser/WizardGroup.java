package net.sourceforge.usbdm.packageParser;


public class WizardGroup extends ProjectAction {
   private String fName;
   private int    fRow;
   private int    fCol;
   private int    fWidth;
   private int    fSpan;
   
   public WizardGroup(String id, String name) {
      super(id);
      fName    = name;
      fRow     = 0;
      fCol     = 0;
      fWidth   = 1;
   }

   /**
    * @return the Name
    */
   public String getName() {
      return fName;
   }

   /**
    * @param row the row to set
    */
   public void setRow(int row) {
      fRow = row;
   }

   /**
    * @return the Row
    */
   public int getRow() {
      return fRow;
   }

   /**
    * @param col the col to set
    */
   public void setCol(int col) {
      fCol = col;
   }

   /**
    * @return the Col
    */
   public int getCol() {
      return fCol;
   }

   /**
    * @param width the width to set
    */
   public void setWidth(int width) {
      fWidth = width;
   }

   /**
    * @return the Width
    */
   public int getWidth() {
      return fWidth;
   }

   /**
    * @param span the width to set
    */
   public void setSpan(int span) {
      fSpan = span;
   }

   /**
    * @return the Width
    */
   public int getSpan() {
      return fSpan;
   }

   public String toString() {
      return String.format("[WizardGroup %s:%s]", getId(), fName);
   }
}
