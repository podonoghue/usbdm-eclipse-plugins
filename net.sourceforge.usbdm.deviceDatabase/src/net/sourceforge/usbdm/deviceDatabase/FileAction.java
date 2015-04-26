package net.sourceforge.usbdm.deviceDatabase;

public class FileAction extends ProjectAction {
   private final String    source;
   private final String    target;
   private final FileType  fileType;
   private       String    root;
   private       boolean   doMacroReplacement;
   private       boolean   doReplace;
   private       PathType  sourcePathType;

   public enum FileType {
      NORMAL,
      LINK,
   }

   public enum PathType {
      UNKNOWN,
      RELATIVE,
      ABSOLUTE
   }
   public FileAction(String source, String target, FileType fileType) throws Exception {
      super("---file---");
      this.source             = source;
      this.target             = target;
      this.fileType           = fileType;
      this.doMacroReplacement = false;
      this.root               = null;
      this.doReplace          = false;
      this.sourcePathType     = PathType.UNKNOWN;
   }
   public String getSource() {
      return source;
   }
   public String getTarget() {
      return target;
   }
   /**
    * @param doMacroReplacement the doMacroReplacement to set
    */
   public void setDoMacroReplacement(boolean doMacroReplacement) {
      this.doMacroReplacement = doMacroReplacement;
   }
   public boolean isDoMacroReplacement() {
      return doMacroReplacement;
   }
   /**
    * @param doReplace the doReplace to set
    */
   public void setDoReplace(boolean doReplace) {
      this.doReplace = doReplace;
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
   /**
    * @return the sourcePathType
    */
   public PathType getSourcePathType() {
      return sourcePathType;
   }
   /**
    * @param sourcePathType the sourcePathType to set
    */
   public void setSourcePathType(PathType sourcePathType) {
      this.sourcePathType = sourcePathType;
   }
}