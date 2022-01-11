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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import net.sourceforge.usbdm.packageParser.ISubstitutionMap;
import net.sourceforge.usbdm.packageParser.ReplacementParser;

public class FileUtility {

   /**
    * Copy a file with substitutions
    * 
    * @param inFilePath
    * @param outFilePath
    * @param variableMap
    * @throws Exception 
    */
   private static void copy(Path inFilePath, Path outFilePath, ISubstitutionMap variableMap) throws Exception {
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
               line = variableMap.substitute(line);
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
               String value = variableMap.getSubstitutionValue(variable);
               if (value == null) {
                  throw new Exception(
                        "Variable not defined, \""+variable+"\"\n"+
                              "within file \""+filename+"\""
                        );
               }
               writer.write(variableMap.getSubstitutionValue(variable));
               variable = null;
            }
         } while (true);
         reader.close();
         writer.close();
         if ((backupFilePath != null) && Files.exists(backupFilePath)) {
            Files.delete(backupFilePath);
         }
      } catch (Exception e) {
         if (reader != null) {
            reader.close();
         }
         if (writer != null) {
            writer.close();
         }
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
   public static void copyFile(IProject project, String source, String target, ISubstitutionMap variableMap, IProgressMonitor progressMonitor) throws Exception {
      SubMonitor monitor = SubMonitor.convert(progressMonitor, 100);
      
      Path projectDirectory = Paths.get(project.getLocation().toPortableString());
      Path sourcePath = projectDirectory.resolve(source);
      Path targetPath = projectDirectory.resolve(target);
//      System.err.println(String.format("FileUtility.copyFile()\n\'%s\' \n\t=> \'%s\'", sourcePath.toAbsolutePath().toString(), targetPath.toAbsolutePath().toString()));
      try {
         monitor.beginTask("Copy File", 100);
         IFile iFile = project.getFile(targetPath.toString());
         if (!iFile.getParent().exists()) {
            createFolder(project, iFile.getParent().getProjectRelativePath().toString(), monitor.newChild(100));
         }
         copy(sourcePath, targetPath, variableMap);
         iFile.refreshLocal(IResource.DEPTH_ONE, null);
         iFile.setDerived(true, monitor);
      } catch (CoreException e) {
         throw new Exception("Failed to copy file "+source+" to "+target , e);
      } finally {
         monitor.done();
      }
   }

   /**
    * Copy file<br>
    * 
    * @param project       Project being modified (if any)
    * @param source        Source file path
    * @param target        Target file path
    * @param variableMap   Map of substitutions to apply when copying
    * @param monitor       Progress monitor
    * 
    * @throws Exception
    */
   private static void copyFile(IProject project, Path sourcePath, Path targetPath, ISubstitutionMap variableMap, IProgressMonitor monitor) throws Exception {
      
      try {
         SubMonitor progress = SubMonitor.convert(monitor, 100); 
         //      System.err.println(String.format("FileUtility.copyFile() \'%s\' \n\t=> \'%s\'", sourcePath, targetPath));
         if (!targetPath.getParent().toFile().exists()) {
            //         System.err.println(String.format("FileUtility.copyFile() Creating folder \'%s\'", targetPath.getParent().toString()));
            targetPath.getParent().toFile().mkdirs();
         }
         copy(sourcePath, targetPath, variableMap);
         if (project != null) {
            Path projectPath = Paths.get(project.getLocation().toPortableString()).toAbsolutePath();
            if (targetPath.startsWith(projectPath)) {
               IFile iFile = project.getFile(projectPath.relativize(targetPath).toString());
               iFile.refreshLocal(IResource.DEPTH_ZERO, progress.newChild(50));
               iFile.setDerived(true, progress.newChild(50));
            }
         }
      } catch (Exception e) {
         throw new Exception("Failed to copy file "+sourcePath+" to "+targetPath, e);
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
   public static void copyDirectory(IProject project, Path sourcePath, Path targetPath, ISubstitutionMap variableMap, IProgressMonitor monitor) throws Exception {
      try {
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
      } catch (Exception e) {
         throw new Exception("Failed to copy directory "+sourcePath+" to "+targetPath, e);
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
   public static void copyDirectory(Path sourcePath, Path targetPath, ISubstitutionMap variableMap) throws Exception {
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
   public static void copyDirectory(IProject project, String source, String target, ISubstitutionMap variableMap, IProgressMonitor monitor) throws Exception {
      SubMonitor subMonitor = SubMonitor.convert(monitor,100);
      Path projectDirectory = Paths.get(project.getLocation().toPortableString());
      Path sourcePath = projectDirectory.resolve(source).toAbsolutePath();
      Path targetPath = projectDirectory.resolve(target).toAbsolutePath();
      //      System.err.println(String.format("FileUtility.copyDirectory()\n\'%s\' \n\t=> \'%s\'", sourcePath.toAbsolutePath().toString(), targetPath.toAbsolutePath().toString()));
      try {
         monitor.beginTask("Copy Files", 100);
         copyDirectory(project, sourcePath, targetPath, variableMap, subMonitor.newChild(100));
      } finally {
         monitor.done();
      }
   }

   /**
    * Creates specified folder in the project recursively.
    *
    * @param project      - project.
    * @param targetPath   - project relative path to the new folder.
    * @param monitor      - progress monitor.
    * @throws Exception 
    */
   public static void createFolder(IProject project, String targetPath, IProgressMonitor monitor) throws Exception {
      SubMonitor subMonitor = SubMonitor.convert(monitor,100);
      //If the targetPath is an empty string, there will be no folder to create.
      // Also this is not an error. So just return gracefully.
      if (targetPath == null || targetPath.length()==0) {
         return;
      }
      IPath path = new org.eclipse.core.runtime.Path(targetPath);
      try {
         for (int i=1; i<=path.segmentCount(); i++) {
            IFolder subfolder = project.getFolder(path.uptoSegment(i));
            if (!subfolder.exists()) {
               subfolder.create(true, true, subMonitor.newChild(10));
               subfolder.refreshLocal(IResource.DEPTH_ONE, null);
            }
         }
      } catch (Exception e) {
         throw new Exception("Failed to create folder "+targetPath, e);
      }
   }

   public static void refreshFile(Path path, ISubstitutionMap variableMap) throws Exception {
      try {
         copyFile(null, path, path, variableMap, null);
      } catch (Exception e) {
         throw new Exception("Failed to refresh file "+path, e);
      }
   }

   /**
    * Refresh the file by copying with substitutions
    * 
    * @param project       Project being modified (if any)
    * @param path          File path
    * @param target        Target file path
    * @param variableMap   Map of substitutions to apply when copying
    * @param monitor       Progress monitor
    * 
    * @throws Exception
    */
   public static void refreshFile(IProject project, String path, ISubstitutionMap variableMap, IProgressMonitor monitor) throws Exception {
      SubMonitor subMonitor = SubMonitor.convert(monitor);
      subMonitor.subTask("Refreshing");
      try {
         Path projectDirectory = Paths.get(project.getLocation().toPortableString());
         Path sourcePath = projectDirectory.resolve(path);
         copyFile(project, sourcePath, sourcePath, variableMap, monitor);
      } catch (Exception e) {
         throw new Exception("Failed to refresh file "+path+" in project "+project.getName(), e);
      }
   }
}
