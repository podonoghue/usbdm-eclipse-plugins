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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

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
   static Matcher scrapeFile(InputStream inputStream, String patternString) {
      BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
      Pattern pattern = Pattern.compile(patternString);

      try {
         String inputLine;
         while ((inputLine = in.readLine()) != null) {
            Matcher matcher = pattern.matcher(inputLine);
            if (matcher.matches()) {
//               System.err.println("Matched line = " + inputLine);
               return matcher;
            }
         }
         in.close();
      } catch (IOException e) {
      }
      return null;
   }

   /**
    * Tries to determine the target device name by inspecting the existing launch configurations
    * 
    * @param project Project to search
    * 
    * @return  Target device name or null if none found
    */
   private String scrapeDeviceName(IProject project) {
      String  targetDeviceName = null;
      final String pattern     = ".*DEVICE_NAME.*\"(Freescale|NXP)_.*_(.*)\".*";

      String[] launchFilePaths = {
            "Project_Settings/Debugger/"+project.getName()+"_Debug_PNE.launch",
            project.getName()+"_Debug_PNE.launch",
            "Project_Settings/Debugger/"+project.getName()+"_Debug.launch",
            project.getName()+"_Debug.launch",
      };

      for (String launchFilePath : launchFilePaths) {
         IFile launchFile = project.getFile(launchFilePath);
         if (!launchFile.exists()) {
            System.err.println("Launch file \'" + launchFile + "\' doesn't exist");
            continue;
         }
         try {
            Matcher m = scrapeFile(launchFile.getContents(), pattern);
            if (m != null) {
               targetDeviceName = m.group(2);
               System.err.println("Matched "+m.group(1)+" target = " + targetDeviceName);
               break;
            }
         } catch (Exception e) {
         }
      }
      return targetDeviceName;
   }
   
   @Override
   public void addHandlerListener(IHandlerListener handlerListener) {
   }

   @Override
   public void dispose() {
   }

   private InputStream getLaunchFile(Map<String, String> variableMap) throws Exception {
      String launchFilePath = "Debug.launch";

      String fileContents;
      fileContents = readFile(launchFilePath);
      fileContents = MacroSubstitute.substitute(fileContents, variableMap);
      ByteArrayInputStream contents = new ByteArrayInputStream(fileContents.getBytes());

      return contents;
   }
   
   private void displayError(Shell shell, String msg) {
      MessageBox mbox = new MessageBox(shell, SWT.ICON_ERROR|SWT.OK);
      mbox.setMessage(msg);
      mbox.setText("USBDM - Can't create launch configuration");
      mbox.open();
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
      IProject project    = (IProject) selection.getFirstElement();
      IContainer  folder  = project.getFolder("Project_Settings/Debugger");
      if (!folder.exists()) {
         System.err.println("Folder " + folder + " doesn't exist, creating launch config in root directory");
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
      variableMap.put("projectName", project.getName());
      variableMap.put("targetDevice", device.getName());
      
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
      return null;
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
