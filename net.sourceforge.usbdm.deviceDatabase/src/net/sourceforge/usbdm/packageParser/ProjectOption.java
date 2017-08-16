package net.sourceforge.usbdm.packageParser;


public class ProjectOption extends ProjectAction {
      private final String   fPath;
      private final String[] fValue;
      private final boolean  fOverwrite;
      private final String   fConfig;

      public ProjectOption(String id, String path,  String[] value, String config, boolean overwrite) {
         super(id);
         fPath      = path;
         fValue     = value;
         fOverwrite = overwrite;
         fConfig    = config;
//         System.err.println("ProjectOption() value = "+value[0]);
      }
      public String[]  getValue() {
         return fValue;
      }
      public String getPath() {
         return fPath;
      }
      public boolean doOverwrite() {
         return fOverwrite;
      }
      @Override
      public String toString() {
         return String.format("ProjectOption[id=%s, path=%s, value=%s, config=%s", 
                     getId(), fPath, (fValue.length==0)?"<empty>":fValue[0], (fConfig==null)?"<empty>":fConfig);
      }
      /**
       * @return the configuration name to modify or null for all
       */
      public String getConfig() {
         return fConfig;
      }
   }