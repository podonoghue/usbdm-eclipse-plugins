package net.sourceforge.usbdm.deviceDatabase;


public class ProjectCustomAction extends ProjectAction {
      private final String   className;
      private final String[] value;

      public ProjectCustomAction(String className, String[] value) throws Exception {
         super("---custom---");
         this.className     = className;
         this.value    = value;
//         System.err.println("ProjectCustomAction() value = "+value[0]);
      }
      
      public String[]  getValue() {
         return value;
      }
      public String getclassName() {
         return className;
      }
      
      @Override
      public String toString() {
         return String.format("ProjectCustomAction[class=%s, value=%s]", className, (value.length==0)?"<empty>":value[0]);
      }
   }