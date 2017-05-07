package net.sourceforge.usbdm.cdt.ui.handlers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.model.IBinary;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import net.sourceforge.usbdm.cdt.tools.UsbdmConstants;
import net.sourceforge.usbdm.cdt.ui.newProjectWizard.LaunchParameterUtilities;
import net.sourceforge.usbdm.cdt.utilties.MacroSubstitute;
import net.sourceforge.usbdm.deviceDatabase.Device;
import net.sourceforge.usbdm.deviceDatabase.ui.DeviceSelector;
import net.sourceforge.usbdm.jni.Usbdm.TargetType;

public class CreateLaunchConfigurationHandler implements IHandler {

   private static String readFile(InputStream inputStream) throws IOException {
      StringBuffer buffer = new StringBuffer();
      BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
         buffer.append(inputLine);
         buffer.append('\n');
      }
      in.close();
      return buffer.toString();
   }

   private static String readFile(String filename) throws IOException {
      URL url = new URL("platform:/plugin/net.sourceforge.usbdm.cdt.ui/files/" + filename);
      InputStream inputStream = url.openConnection().getInputStream();
      return readFile(inputStream);
   }

   /**
    * Searches an inputStream line-by-line looking for a pattern
    * 
    * @param inputStream      Input stream to search
    * @param patternString    Pattern to look for
    * 
    * @return  Matcher holding line matched
    */
   public static Matcher scrapeFile(InputStream inputStream, String patternString) {
      BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
      Pattern pattern   = Pattern.compile(patternString);
      try {
         String inputLine;
         while ((inputLine = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(inputLine);
            if (matcher.matches()) {
               return matcher;
            }
         }
      } catch (IOException e) {
      }
      return null;
   }

   /**
    * Tries to determine the target device name by inspecting the existing project files
    * 
    * @param project   Project to search
    * @param file      File path within project
    * @param tuple     Search information
    * 
    * @return  Name found or null if unsuccessful
    */
   private static String scrapeFile(IProject project, IFile file, Tuple tuple) {
      String deviceName = null;
      try {
         InputStream in = file.getContents();
         Matcher m = scrapeFile(in, tuple.match);
         in.close();
         if (m != null) {
            deviceName = m.replaceAll(tuple.result);
            System.err.println(String.format("%-70s",tuple)+" => Found '"+deviceName+"' within '"+m.group()+"'");
         }
      } catch (Exception e) {
      }
      return deviceName;
   }

   /**
    * Tuple to represent a pattern to look for
    */
   static class Tuple {
      String match;
      String result;

      Tuple(String match, String result) {
         this.match  = match;
         this.result = result;
      }
      public String toString() {
         return "["+match+","+result+"]";
      }
   };

   /** Pattern to search files with */
   final static Tuple tuples[] = {
         new Tuple(".*DEVICE_NAME.*\"(?:Freescale|NXP)_.*_(.+)\".*",                   "$1"),    // PE Launch
         new Tuple(".*gdbServerDeviceName.*\"\\s*value\\s*=\\s*\"M?(.+?)x+(.*?)\".*",  "$1M$2"), // Segger launch
         new Tuple(".*&lt;name&gt;(LPC.*)/(.*)&lt;/name&gt;&#13;.*",                   "$1_$2"), // mcuExpress .cproject
         new Tuple(".*&lt;name&gt;(.*)&lt;/name&gt;&#13;.*",                           "$1"),    // mcuExpress .cproject
   };

   /**
    * Tries to determine the target device name by inspecting the existing launch configurations
    * 
    * @param project Project to search
    * 
    * @return  Target device name or null if none found
    */
   private static String scrapeDeviceName(IProject project) {
      
      // Which files to scrape
      final String probeFiles[] = {
            "Project_Settings/Debugger/"+project.getName()+"_Debug_PNE.launch",
            project.getName()+"_Debug_PNE.launch",
            "Project_Settings/Debugger/"+project.getName()+"_Debug.launch",
            project.getName()+"_Debug.launch",
            ".cproject",
      };

      String deviceName = null;
      
      for (String filePath:probeFiles) {
         System.err.println("============\nScraping "+filePath);
         IFile file = project.getFile(filePath);
         if (!file.exists()) {
            System.err.println("File \'" + file + "\' doesn't exist");
            continue;
         }
         for (Tuple tuple:tuples) {
            deviceName = scrapeFile(project, file, tuple);
            if (deviceName != null) {
               break;
            }
         }
         if (deviceName != null) {
            break;
         }
      }
      
      return deviceName;
   }

   @Override
   public void addHandlerListener(IHandlerListener handlerListener) {
   }

   @Override
   public void dispose() {
   }

   private static InputStream getLaunchFile(Map<String, String> variableMap) throws Exception {
      String launchFilePath = "Debug.launch";

      String fileContents;
      fileContents = readFile(launchFilePath);
      fileContents = MacroSubstitute.substitute(fileContents, variableMap);
      ByteArrayInputStream contents = new ByteArrayInputStream(fileContents.getBytes());

      return contents;
   }

   private static void displayError(Shell shell, String msg) {
      MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR|SWT.OK);
      mbox.setMessage(msg);
      mbox.setText("USBDM - Can't create launch configuration");
      mbox.open();
   }

   /**
    * Creates a launch configuration as a physical file
    * 
    * @param shell   Shell for dialogues
    * @param project Project to create launch configuration within
    * @param binPath 
    * @return 
    */
   public static ILaunchConfiguration createLaunchConfig(final Shell shell, final IProject project, IPath binPath) {

      String[] folders = {"Project_Settings/Debugger", "Project_Settings"};
      
      IContainer  folder = null;
      for (String trialFolder:folders) {
         IFolder t  = project.getFolder(trialFolder);
         if (t.exists()) {
            folder = t;
            break;
         }
      }
      if (folder == null) {
         System.err.println("Project_Settings folder doesn't exist, creating launch config in root directory");
         folder = project;
      }
      IFile launchFile = folder.getFile(new Path(project.getName()+"_Debug_USBDM.launch"));

      if (launchFile.exists()) {
         System.err.println("File " + launchFile + " already exist");
         displayError(shell, "Launch configuration \n\"" + launchFile.getName() + "\"\nalready exists");
         return null;
      }
      String deviceName = scrapeDeviceName(project);
      DeviceSelector deviceSelector = new DeviceSelector(shell, TargetType.T_ARM, deviceName);

      int rc = deviceSelector.open();
      if (rc != Window.OK) {
         return null;
      }
      
      Device device = deviceSelector.getDevice();
      
      Map<String, String> variableMap = new HashMap<String, String>();
      variableMap.put(UsbdmConstants.PROJECT_NAME_KEY, project.getName());

      // Add launch parameters from device information
      LaunchParameterUtilities.addLaunchParameters(variableMap, device, binPath);

      try {
         InputStream contents = getLaunchFile(variableMap);
         launchFile.create(contents, true, null);
         launchFile.refreshLocal(IResource.DEPTH_ONE, null);
         project.refreshLocal(IResource.DEPTH_INFINITE, null);
      } catch (CoreException e) {
         displayError(shell, e.getMessage());
         e.printStackTrace();
      } catch (Exception e) {
         displayError(shell, e.getMessage());
         e.printStackTrace();
      }
      return DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(launchFile);
   }
   
   @Override
   public Object execute(ExecutionEvent event) throws ExecutionException {

      final Shell shell = HandlerUtil.getActiveWorkbenchWindow(event).getShell();
      final Object source = HandlerUtil.getCurrentSelection(event);
      //      System.err.println("Event source = "+source.toString()+"\n class = "+source.getClass().toString());
      if (!(source instanceof TreeSelection)) {
         System.err.println("Source is not an instance of TreeSelection");
         return null;
      }
      TreeSelection selection = (TreeSelection) source;
      if (!(selection.getFirstElement() instanceof IProject)) {
         System.err.println("Selection.getFirstElement() is not an instance of org.eclipse.cdt.core.model.IProject");
         return null;
      }  
      IBinary[] binaries = null;
      try {
         binaries = LaunchParameterUtilities.searchForExecutable(new Object[]{selection}, "debug");
      } catch (Exception e) {
         System.err.println(e.getMessage());
         return null;
      }
      IBinary bin = null;
      if (binaries.length == 0) {
         bin = binaries[0];
      }
      createLaunchConfig(shell, (IProject) selection.getFirstElement(), bin.getPath());

      return  null;
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
