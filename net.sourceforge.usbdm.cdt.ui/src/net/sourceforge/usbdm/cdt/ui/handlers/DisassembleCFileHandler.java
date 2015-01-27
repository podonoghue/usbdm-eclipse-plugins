package net.sourceforge.usbdm.cdt.ui.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;

public class DisassembleCFileHandler implements IHandler {

   @Override
   public void addHandlerListener(IHandlerListener handlerListener) {
   }

   @Override
   public void dispose() {
   }

//   public static void pipeStream(InputStream input, OutputStream output)
//         throws IOException {
//      
//      byte buffer[] = new byte[1024];
//      int numRead = 0;
//
//      while (input.available() > 0) {
//         numRead = input.read(buffer);
//         output.write(buffer, 0, numRead);
//      }
//      output.flush();
//   }

   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {
      String os    = System.getProperty("os.name");            
      boolean isLinux = (os != null) && os.toUpperCase().contains("LINUX");
      
      Object source = HandlerUtil.getCurrentSelection(event);
//      System.err.println("Event source = "+source.toString()+"\n class = "+source.getClass().toString());
      if (!(source instanceof TreeSelection)) {
//         System.err.println("Source is not an instance of TreeSelection");
         return null;
      }
      TreeSelection selection = (TreeSelection) source;
      if (!(selection.getFirstElement() instanceof IBinary)) {
//         System.err.println("Selection.getFirstElement() is not an instance of org.eclipse.cdt.core.model.IBinary");
         return null;
      }         
      IBinary   binary            = (IBinary) selection.getFirstElement();
      IResource resource          = binary.getUnderlyingResource();
      IPath     resourcePath      = resource.getLocation();
      IPath     dissassemblyPath  = resourcePath.removeFileExtension().addFileExtension("lis");

      // Get Environment (path etc) 
      CoreModel cdtCoreModel = org.eclipse.cdt.core.model.CoreModel.getDefault();
      ICProjectDescription cProjectDescription = cdtCoreModel.getProjectDescription(resource.getProject());
      ICConfigurationDescription cConfigurationDescription = cProjectDescription.getActiveConfiguration();
      IContributedEnvironment contributedEnvironment = CCorePlugin.getDefault().getBuildEnvironmentManager().getContributedEnvironment();
      IEnvironmentVariable[] environmentVariablesArray = contributedEnvironment.getVariables(cConfigurationDescription);
      HashMap<String,String> environmentMap = new HashMap<String, String>();
      for (IEnvironmentVariable ev : environmentVariablesArray) {
         String name = ev.getName();
         if ((!isLinux) && name.equals("PATH")) {
            name = "Path";
         }
         System.err.println("Adding Environment variable: "+name+" => "+ev.getValue());
         environmentMap.put(name, ev.getValue());
      }

//      String resourceName = resource.getName();
//      System.err.println("resourceName = "+resourceName);

      IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(resource);
      IConfiguration configuration = buildInfo.getDefaultConfiguration();
//      System.err.println("configuration = "+configuration);
      
      // Find lister tool in configuration
      ITool[] toolArray = configuration.getTools();
      ITool tool = null;
      for (ITool t : toolArray) {
         Pattern p = Pattern.compile("net\\.sourceforge\\.usbdm\\.cdt\\..*\\.toolchain\\.lister\\..*");
         if (p.matcher(t.getId()).matches()) {
            tool = configuration.getTool(t.getId());
            break;
         }
      }
      if (tool == null) {
         return null;
      }
//      System.err.println("tool.getName = "+tool.getName());
//      System.err.println("tool.getToolCommand = "+tool.getToolCommand());
      
      // Get command line generator
      IManagedCommandLineGenerator commandLineGenerator = tool.getCommandLineGenerator();

      // Get command line
      try {
         String[] inputs = {resourcePath.lastSegment()};
         IManagedCommandLineInfo commandLineInfo = commandLineGenerator.generateCommandLineInfo(
               tool, 
               tool.getToolCommand(), 
               tool.getToolCommandFlags(resourcePath, dissassemblyPath),
               "", 
               ">", 
               dissassemblyPath.lastSegment(), 
               inputs, 
               "${COMMAND} ${INPUTS} ${FLAGS} ${OUTPUT_FLAG}${OUTPUT_PREFIX}${OUTPUT}");
//         System.err.println("cl.toString() = "+commandLineInfo.toString());
//         System.err.println("cl.getCommandLine() = "+commandLineInfo.getCommandLine());

         String[] commandArray = null;
         if (isLinux) {
            // Construct command (Use cmd for PATH changes!)
            ArrayList<String> command = new ArrayList<String>(20);
            command.add("/bin/sh");
            command.add("-c");
            command.add(commandLineInfo.getCommandLine());
            commandArray = (String[])command.toArray(new String[command.size()]);
         }
         else {
            // Construct command (Use cmd for PATH changes!)
            ArrayList<String> command = new ArrayList<String>(20);
            command.add("cmd.exe");
            command.add("/C");
            command.add(commandLineInfo.getCommandLine());
            commandArray = (String[])command.toArray(new String[command.size()]);
         }
         // Run command
         System.err.println("Running...");
         ProcessBuilder pb = new ProcessBuilder(commandArray);
         pb.environment().putAll(environmentMap);

         File commandFile = findExecutableOnPath(pb.environment().get("PATH"), commandLineInfo.getCommandName());
         if (commandFile != null) {
            System.err.println("commandFile.toPath() = "+ commandFile.toPath());
            System.err.println("pwd = "+ resourcePath.removeLastSegments(1).toFile());
         }
//            System.err.println("=================== Environment variables ===================");
//            for (String envName : pb.environment().keySet()) {
//               System.err.println("Environment variable: "+envName+" => "+pb.environment().get(envName));
//            }
//            System.err.println("=================== End of Environment variables ===================");
         pb.redirectInput(Redirect.INHERIT);
         pb.redirectError(Redirect.INHERIT);
         pb.redirectOutput(Redirect.INHERIT);
         pb.directory(resourcePath.removeLastSegments(1).toFile());
//         System.err.println("WD =" + pb.directory());
         Process p = pb.start();
//         System.err.println("Waiting...");
         p.waitFor();
//         System.err.println("Exit value = " + p.waitFor());
      } catch (IOException e) {
         e.printStackTrace();
      } catch (InterruptedException e) {
         e.printStackTrace();
      } catch (BuildException e) {
         e.printStackTrace();
      }
      try {
         // Refresh parent directory so file is visible
         resource.getParent().refreshLocal(1, null);
      } catch (CoreException e) {
         e.printStackTrace();
      }
      try {
         // Open disassembled file in editor
         IPath asmPath = resource.getFullPath().removeFileExtension().addFileExtension("lis");
         IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
         IDE.openEditor(page, ResourcesPlugin.getWorkspace().getRoot().getFile(asmPath));
      } catch (PartInitException e) {
         e.printStackTrace();
      }
//      System.err.println("Completed");
      return null;
   }

   private static File findExecutableOnPath(String systemPath, String executableName) {
//    System.err.println("findExecutableOnPath systemPath = " + systemPath);

      if (systemPath == null) {
         return null;
      }
      String[] pathDirs = systemPath.split(File.pathSeparator);

      File fullyQualifiedExecutable = null;
      for (String pathDir : pathDirs) {
//         System.err.println("findExecutableOnPath pathDir = " + pathDir);
         File[] listOfFiles = new File(pathDir).listFiles();
         if (listOfFiles == null) {
            continue;
         }
         for (File f : listOfFiles) {
//            System.err.println("findExecutableOnPath checking = " + f.getName());
            if (f.getName().startsWith(executableName)) {
               fullyQualifiedExecutable = f;
               break;
            }
         }
         if (fullyQualifiedExecutable != null) {
            break;
         }
      }
      return fullyQualifiedExecutable;
   }

   @Override
   public boolean isEnabled() {
      return true;
   }

   @Override
   public boolean isHandled() {
      return true;
   }

   @Override
   public void removeHandlerListener(IHandlerListener handlerListener) {
   }
}
