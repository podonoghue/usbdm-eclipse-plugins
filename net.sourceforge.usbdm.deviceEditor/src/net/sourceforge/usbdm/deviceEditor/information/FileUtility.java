package net.sourceforge.usbdm.deviceEditor.information;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

public class FileUtility {

   public interface IKeyMaker {
      /**
       * Generate variable key from name
       * 
       * @param  name Name used to create key
       * @return Key generated from name
       */
      public String makeKey(String name);
   }

   public static final class PublicKeyMaker implements IKeyMaker {
      /**
       * Generate variable key from name
       * 
       * @param  name Name used to create key
       * @return Key generated from name
       */
      @Override
      public String makeKey(String name) {
         return name;
      }
   }

   /**
    * Key mapper for public symbols
    */
   public static final PublicKeyMaker publicKeyMaker = new PublicKeyMaker();
   
   /**
    * Finds all $(..) patterns in string
    * 
    * @param input
    * @return array of names within the $(...)
    */
   private static ArrayList<String> findAllPatterns(String input) {
      ArrayList<String> patterns = new ArrayList<String>();

      Pattern pattern = Pattern.compile("\\$\\(([^\\)]+)\\)");
      Matcher matcher = pattern.matcher(input);
      while (matcher.find()) {
         patterns.add(matcher.group(1));
         //         System.err.println("p = \'"+matcher.group(1)+"\'");
      }
      return patterns;
   }

   /**
    * Replaces macros e.g. $(name:defaultValue) with values from a map or default if not found
    * 
    * @param input        String to replace macros in
    * @param map          Map of key->value pairs for substitution
    * @param keyMaker     Interface providing a method to create a key from a variable name
    * 
    * @return      String with substitutions (or original if none)
    */
   public static String substitute(String input, Map<String, String> map, IKeyMaker keyMaker) {

      if (input == null) {
         return null;
      }
      if (map == null) {
         return input;
      }
      ArrayList<String> patterns = findAllPatterns(input);
      Pattern variablePattern = Pattern.compile("([^:]+):(.*)");
      for (String pattern : patterns) {
         // p is the middle part of the pattern 
         // e.g. $(pattern) => pattern, $(pattern:default) => pattern:default
         Matcher matcher = variablePattern.matcher(pattern);
         String key = pattern;
         String defaultValue = null;
         if (matcher.find()) {
            key          = matcher.group(1);
            defaultValue = matcher.group(2);
            //          System.out.println(String.format("p=\'%s\', d=\'%s\'", pattern, defaultValue));
         }
         String replaceWith = map.get(keyMaker.makeKey(key));
         if (replaceWith == null) {
            //           System.out.println("Using default \'" + defaultValue + "\'");
            replaceWith = defaultValue;
         }
         if (replaceWith == null) {
            System.err.println("---Symbol not found for substitution \'$("+pattern+")\'");
            replaceWith = "---Symbol not found for substitution \'$("+pattern+")\'";
         }
         input = input.replaceAll("\\$\\("+pattern+"\\)", Matcher.quoteReplacement(replaceWith));
      }
      return input;
   }

   /**
    * Replaces macros e.g. $(key:defaultValue) with values from a map or default if not found
    * 
    * @param input        String to replace macros in
    * @param variableMap  Map of key->value pairs for substitution
    * 
    * @return      String with substitutions (or original if none)
    */
   public static String substitute(String input, Map<String,String> variableMap) {
      return substitute(input, variableMap, publicKeyMaker);
   }

   /**
    * Copy a file with substitutions
    * 
    * @param inFilePath
    * @param outFilePath
    * @param variableMap
    * @throws Exception 
    */
   private static void copy(Path inFilePath, Path outFilePath, Map<String, String> variableMap) throws Exception {
      Path backupFilePath = null;
      String filename = inFilePath.getFileName().toString();
      boolean areSameFile = Files.isSameFile(inFilePath, outFilePath);
      if (Files.exists(outFilePath)) {
         // Rename original output file as backup
         backupFilePath = outFilePath.getParent().resolve(outFilePath.getFileName()+"_bak");
         Files.move(outFilePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
         if (areSameFile) {
            // Use backup as source
            inFilePath = backupFilePath;
         }
      }
      BufferedReader reader = null;
      BufferedWriter writer = null;
      final Pattern startPattern = Pattern.compile("/\\*.*\\$start\\((.*)\\).*\\*/");

      try {
         reader = Files.newBufferedReader(inFilePath, StandardCharsets.UTF_8);
         writer = Files.newBufferedWriter(outFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
         boolean includeLine = true;
         String variable = null;
         Pattern endPattern   = null;
         do {
            String line = reader.readLine();
            if (line == null) {
               break;
            }
            if (endPattern != null) {
               Matcher matcher = endPattern.matcher(line);
               if (matcher.matches()) {
                  includeLine = true;
               }
            }
            if (includeLine) {
               line = substitute(line, variableMap);
               writer.write(line);
               writer.newLine();
            }
            Matcher matcher = startPattern.matcher(line);
            if (matcher.matches()) {
               variable = matcher.group(1);
               endPattern   = Pattern.compile("/\\*.*\\$end\\("+Pattern.quote(variable)+"\\).*\\*/");
               includeLine = false;
            }
            if (variable != null) {
               String value = variableMap.get(variable);
               if (value == null) {
                  throw new Exception(
                        "Variable not defined, \""+variable+"\"\n"+
                              "within file \""+filename+"\""
                        );
               }
               writer.write(variableMap.get(variable));
               variable = null;
            }
         } while (true);
         reader.close();
         writer.close();
         if ((backupFilePath != null) && Files.exists(backupFilePath)) {
            Files.delete(backupFilePath);
         }
      } catch (Exception e) {
         //         e.printStackTrace();
         System.err.println("Failed to process " + inFilePath.toString());
         if (writer != null) {
            writer.close();
         }
         if (reader != null) {
            reader.close();
         }
         Files.delete(outFilePath);
         if (backupFilePath != null) {
            Files.move(backupFilePath, outFilePath, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("Restoring backup " + backupFilePath.toString());
         }
         throw e;
      }
   }

   /**
    * Copy files within a project<br>
    * Project changes are refreshed
    * 
    * @param project       Project being modified (if any)
    * @param source        Source file path, absolute or relative to project
    * @param target        Target file path, relative to project
    * @param variableMap   Map of substitutions to apply when copying
    * @param monitor       Progress monitor
    * 
    * @throws Exception
    */
   public static void copyFile(IProject project, String source, String target, Map<String, String> variableMap, IProgressMonitor monitor) throws Exception {
      Path projectDirectory = Paths.get(project.getLocation().toPortableString());
      Path sourcePath = projectDirectory.resolve(source);
      Path targetPath = projectDirectory.resolve(target);
      //      System.err.println(String.format("FileUtility.copyFile()\n\'%s\' \n\t=> \'%s\'", sourcePath.toAbsolutePath().toString(), targetPath.toAbsolutePath().toString()));
      try {
         monitor.beginTask("Copy File", 100);
         IFile iFile = project.getFile(targetPath.toString());
         if (!iFile.getParent().exists()) {
            createFolder(project, iFile.getParent().getProjectRelativePath().toString(), new SubProgressMonitor(monitor, 50));
         }
         copy(sourcePath, targetPath, variableMap);
         iFile.refreshLocal(IResource.DEPTH_ONE, null);
         iFile.setDerived(true, monitor);
         //         project.refreshLocal(IResource.DEPTH_INFINITE, null);
      } catch (CoreException e) {
         throw new Exception("Failed" + e.getMessage(), e); //$NON-NLS-1$
      } finally {
         monitor.done();
      }
   }

   /**
    * Copy files<br>
    * For testing
    * 
    * @param project       Project being modified (if any)
    * @param source        Source file path
    * @param target        Target file path
    * @param variableMap   Map of substitutions to apply when copying
    * @param monitor       Progress monitor
    * 
    * @throws Exception
    */
   private static void copyFile(IProject project, Path sourcePath, Path targetPath, Map<String, String> variableMap, IProgressMonitor monitor) throws Exception {
      //      System.err.println(String.format("FileUtility.copyFile() \'%s\' \n\t=> \'%s\'", sourcePath, targetPath));
      if (!targetPath.getParent().toFile().exists()) {
         //         System.err.println(String.format("FileUtility.copyFile() Creating folder \'%s\'", targetPath.getParent().toString()));
         targetPath.getParent().toFile().mkdirs();
      }
      copy(sourcePath, targetPath, variableMap);
      if (project != null) {
         Path projectPath = Paths.get(project.getLocation().toPortableString()).toAbsolutePath();
         if (targetPath.startsWith(projectPath)) {
            try {
               IFile iFile = project.getFile(projectPath.relativize(targetPath).toString());
               iFile.refreshLocal(IResource.DEPTH_ONE, monitor);
               iFile.setDerived(true, monitor);
            } catch (Exception e) {
               // Report and ignore
               e.printStackTrace();
            }
         }
      }
   }

   /*
    * ===================================================================================================================
    */
   /**
    * Copy files<br>
    * For testing
    * 
    * @param project       Project being modified
    * @param sourcePath    Source file path
    * @param targetPath    Target file path
    * @param variableMap   Map of substitutions to apply when copying
    * @param monitor       Progress monitor
    * 
    * @throws Exception
    */
   public static void copyDirectory(IProject project, Path sourcePath, Path targetPath, Map<String, String> variableMap, IProgressMonitor monitor) throws Exception {
      sourcePath = sourcePath.toAbsolutePath();
      targetPath = targetPath.toAbsolutePath();
      if (Files.isDirectory(sourcePath)) {
         DirectoryStream<Path> folderStream = Files.newDirectoryStream(sourcePath);
         for (Path filePath : folderStream) {
            Path relativePath = sourcePath.relativize(filePath);
            copyDirectory(project, filePath, targetPath.resolve(relativePath), variableMap, monitor);
         }
      }
      else if (Files.isRegularFile(sourcePath)) {
         copyFile(project, sourcePath, targetPath, variableMap, monitor);
      }
   }

   /**
    * Copy directory
    * 
    * @param source        Source file path, absolute or relative to project
    * @param target        Target file path, relative to project
    * @param variableMap   Map of substitutions to apply when copying
    * 
    * @throws Exception
    */
   public static void copyDirectory(Path sourcePath, Path targetPath, Map<String, String> variableMap) throws Exception {
      copyDirectory(null, sourcePath, targetPath, variableMap, null);
   }

   /**
    * Copy directory within a project<br>
    * Project changes are refreshed
    * 
    * @param project       Project being modified
    * @param source        Source file path, absolute or relative to project
    * @param target        Target file path, relative to project
    * @param variableMap   Map of substitutions to apply when copying
    * @param monitor       Progress monitor
    * 
    * @throws Exception
    */
   public static void copyDirectory(IProject project, String source, String target, Map<String, String> variableMap, IProgressMonitor monitor) throws Exception {
      Path projectDirectory = Paths.get(project.getLocation().toPortableString());
      Path sourcePath = projectDirectory.resolve(source).toAbsolutePath();
      Path targetPath = projectDirectory.resolve(target).toAbsolutePath();
      //      System.err.println(String.format("FileUtility.copyDirectory()\n\'%s\' \n\t=> \'%s\'", sourcePath.toAbsolutePath().toString(), targetPath.toAbsolutePath().toString()));
      try {
         monitor.beginTask("Copy Files", 100);
         copyDirectory(project, sourcePath, targetPath, variableMap, monitor);
      } finally {
         monitor.done();
      }
   }

   /**
    * Creates specified folder in the project recursively.
    *
    * @param project            - project.
    * @param targetPath         - project relative path to the new folder.
    * @param progressMonitor    - progress monitor.
    * 
    * @throws CoreException 
    */
   public static void createFolder(IProject project, String targetPath, IProgressMonitor progressMonitor) throws CoreException {
      //If the targetPath is an empty string, there will be no folder to create.
      // Also this is not an error. So just return gracefully.
      if (targetPath == null || targetPath.length()==0) {
         return;
      }
      IPath path = new org.eclipse.core.runtime.Path(targetPath);
      for (int i=1; i<=path.segmentCount(); i++) {
         IFolder subfolder = project.getFolder(path.uptoSegment(i));
         if (!subfolder.exists()) {
            subfolder.create(true, true, progressMonitor);
            subfolder.refreshLocal(IResource.DEPTH_ONE, null);
         }
      }
   }

   public static void refreshFile(Path path, Map<String, String> variableMap) throws Exception {
      copyFile(null, path, path, variableMap, null);
   }

   public static void refreshFile(IProject project, String path, Map<String, String> variableMap, IProgressMonitor progressMonitor) throws Exception {
      Path projectDirectory = Paths.get(project.getLocation().toPortableString());
      Path sourcePath = projectDirectory.resolve(path);
      copyFile(project, sourcePath, sourcePath, variableMap, progressMonitor);
   }
}
