package net.sourceforge.usbdm.deviceDatabase;


public class ProjectOption extends ProjectAction {
      private final String   path;
      private final String[] value;
      private final boolean  replace;

      public ProjectOption(String id, String path,  String[] value, boolean replace) throws Exception {
         super(id);
         this.path     = path;
         this.value    = value;
         this.replace  = replace;
//         System.err.println("ProjectOption() value = "+value[0]);
      }
      public String[]  getValue() {
         return value;
      }
      public String getPath() {
         return path;
      }
      public boolean isReplace() {
         return replace;
      }
      @Override
      public String toString() {
         return String.format("ProjectOption[id=%s, path=%s, value=%s ...]", getId(), path, (value.length==0)?"<empty>":value[0]);
      }
   }