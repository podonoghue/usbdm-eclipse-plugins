package net.sourceforge.usbdm.packageParser;


public class FileAction extends ProjectAction {
   private final String    source;
   private final String    target;
   private final FileType  fileType;
   private       String    root              = null;
   private       boolean   fDoMacroReplace   = false;
   private       boolean   fDoFileOverwrite  = false;
   private       PathType  sourcePathType    = PathType.UNKNOWN;
   private       boolean   fDerived          = false;

   public enum FileType {
      NORMAL,
      LINK,
   }

   public enum PathType {
      UNKNOWN,
      RELATIVE,
      ABSOLUTE
   }
   public FileAction(String source, String target, FileType fileType) {
      super("---file---");
      this.source             = source;
      this.target             = target;
      this.fileType           = fileType;
   }
   public String getSource() {
      return source;
   }
   public String getTarget() {
      return target;
   }
   /**
    * Indicates whether macro replacement should occur when file is written
    * 
    * @param doMacroReplace value to set
    */
   public void setMacroReplace(boolean doMacroReplace) {
      fDoMacroReplace = doMacroReplace;
   }
   /**
    * Indicates macro replacement should occur when file is written
    * 
    * @param True if macro replacement should occur
    */
   public boolean doMacroReplace() {
      return fDoMacroReplace;
   }
   /**
    * Indicates whether file overwrite should occur when copied
    * 
    * @param doOverwrite True to indicate that files should be quietly overwritten
    */
   public void setFileOverwrite(boolean doOverwrite) {
      fDoFileOverwrite = doOverwrite;
   }
   /**
    * Indicates files should be overwritten when copied
    * 
    * @return True if files should be overwritten when copied
    */
   public boolean doFileOverwrite() {
      return fDoFileOverwrite;
   }
   @Override
   public String toString() {
      StringBuffer buffer = new StringBuffer(2000);
      buffer.append("FileAction[\"");
      buffer.append(root+"/");
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
   /**
    * Used to indicate that the file should be marked as derived in eclipse
    * 
    * @param derived
    */
   public void setDerived(boolean derived) {
      this.fDerived = derived;
   }
   /**
    * Indicate that the file should be marked as derived in eclipse
    * 
    * @return True if derived
    */
   public boolean isDerived() {
      return fDerived;
   }
   
}