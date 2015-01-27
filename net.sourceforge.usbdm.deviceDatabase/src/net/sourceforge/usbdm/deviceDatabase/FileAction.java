package net.sourceforge.usbdm.deviceDatabase;

public class FileAction extends ProjectAction {
   private final String    source;
   private final String    target;
   private       String    root;
   private final FileType  fileType;
   private final boolean   doMacroReplacement;
   private final boolean   doReplace;

   public enum FileType {
      NORMAL,
      LINK,
   }

   public FileAction(String source, String target, FileType fileType, boolean doMacroReplacement, boolean doReplace) throws Exception {
      super("---file---");
      this.source             = source;
      this.target             = target;
      this.fileType           = fileType;
      this.doMacroReplacement = doMacroReplacement;
      this.root               = null;
      this.doReplace          = doReplace;
   }
   public String getSource() {
      return source;
   }
   public String getTarget() {
      return target;
   }
   public boolean isDoMacroReplacement() {
      return doMacroReplacement;
   }
   public boolean isDoReplace() {
      return doReplace;
   }
   @Override
   public String toString() {
      StringBuffer buffer = new StringBuffer(2000);
      buffer.append("FileAction[\"");
      buffer.append(root);
      buffer.append(source);
      buffer.append("\" =>            \"");
      buffer.append(target);
      buffer.append("\"]");
      return buffer.toString();
   }

   public void setRoot(String root) {
      this.root = root;
   }

   public String getRoot() {
      return root;
   }
   
   public FileType getFileType() {
      return fileType;
   }
}