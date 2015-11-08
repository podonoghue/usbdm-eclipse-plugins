package net.sourceforge.usbdm.cdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import net.sourceforge.usbdm.deviceDatabase.Device;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.eclipse.ui.wizards.IWizardRegistry;

public class AddProcessorExpert implements CustomAction {
   @Override
   public boolean action(
         final Wizard                 wizard, 
         final IProject               projectHandle, 
         final Device                 device, 
         final Map<String, String>    variableMap, 
         final IProgressMonitor       progressMonitor, 
         final String[]               parameters) {
      
      PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
         public void run() {
            executePeWizard(wizard.getShell(), projectHandle);
        }
     });
      return false;
   }

   /**
    *  Display the Processor Expert Import dialogue
    *  Uses reflection
    * 
    * @param shell
    * @param projectHandle
    * @return
    */
   public int executeExperimentalPeWizard(Shell shell, IProject projectHandle) {
      
      tryIt();
      
      IConfigurationElement[] config = null;

      IExtension[] extensions = Platform.getExtensionRegistry()
            .getExtensionPoint("com.processorexpert.core.ide.newprojectwizard", "newProjectWizard")
            .getExtensions();
      if (extensions.length == 0) {
         System.err.println("extensions.length == 0");
      }
      for (IExtension extension : extensions) {
         System.err.println(String.format("extension EP.ID = %s, UID = %s, SID = %s",
               extension.getExtensionPointUniqueIdentifier(), 
               extension.getUniqueIdentifier(),
               extension.getSimpleIdentifier()));
         config = extension.getConfigurationElements();
         for (IConfigurationElement configurationElement : config) {
            System.err.println(String.format("configurationElement name = %s, value = %s",
                  configurationElement.getName(), 
                  configurationElement.getValue()));
         }
      }
      System.err.println("=========================================");
      config = extensions[0].getConfigurationElements();
      for (IConfigurationElement configurationElement : config) {
         System.err.println(String.format("configurationElement name = %s, value = %s, id = %s ",
               configurationElement.getName(), 
               configurationElement.getValue(),
               configurationElement.getAttribute("id")));
      }
      System.err.println("=========================================");

      IWizard wizard = findWizard("com.processorexpert.ui.kds.newprojectwizard.wizard1");
      if (wizard == null) {
         System.err.println("No Wizard found");
         return Window.CANCEL;
      }
      
//      IExtensionRegistry registry = RegistryFactory.createRegistry(null, null, null);
//      Bundle bundle = Platform.getBundle(Activator.getId());
//      URL fileURL = bundle.getEntry("files/wizard.xml");
//      IContributor contributor = org.eclipse.core.runtime.ContributorFactorySimple.createContributor(this);
//      try {
//         InputStream is = fileURL.openConnection().getInputStream();
//         if (!registry.addContribution(is, contributor , false, "usbdmWizard", null, null )) {
//            System.err.println("registry.addContribution() failed");
//         }
//      } catch (IOException e1) {
//         e1.printStackTrace();
//      }
//      
//      config = Platform.getExtensionRegistry().getConfigurationElementsFor("net.sourceforge.usbdm.cdt.ui.wizard1");
//      System.err.println("config = " + ((config.length==0)?"<empty>":config[0].toString()));
//      if ((config == null) || (config.length == 0)) {
//         config = registry.getConfigurationElementsFor("com.processorexpert.core.ide.newprojectwizard.newProjectWizard", "vvv");
//         System.err.println("config = " + ((config.length==0)?"<empty>":config[0].toString()));
//      }
//      if ((config == null) || (config.length == 0)) {
//         config = registry.getConfigurationElementsFor("vvv");
//         System.err.println("config = " + ((config.length==0)?"<empty>":config[0].toString()));
//      }
//      if ((config == null) || (config.length == 0)) {
//         config = registry.getConfigurationElementsFor("net.sourceforge.usbdm.cdt.ui.actions.AddProcessorExpert.vvv");
//         System.err.println("config = " + ((config.length==0)?"<empty>":config[0].toString()));
//      }
//      if ((config == null) || (config.length == 0)) {
//         config = registry.getConfigurationElementsFor("com.processorexpert.core.ide.newprojectwizard.newProjectWizard");
//         System.err.println("config = " + ((config.length==0)?"<empty>":config[0].toString()));
//      }
////      config[0].getAttribute("id");
//      extensions = registry.getExtensions(contributor);
//      if (extensions.length == 0) {
//         System.err.println("extensions.length == 0");
//      }
//      for (IExtension extension : extensions) {
//         System.err.println(String.format("extension EP.ID = %s, UID = %s, SID = %s",
//               extension.getExtensionPointUniqueIdentifier(), 
//               extension.getUniqueIdentifier(),
//               extension.getSimpleIdentifier()));
//         config = extension.getConfigurationElements();
//         for (IConfigurationElement configurationElement : config) {
//            System.err.println(String.format("configurationElement name = %s, value = %s",
//                  configurationElement.getName(), 
//                  configurationElement.getValue()));
//         }
//      }
//      String[] nameSpaces  = registry.getNamespaces();
//      if (nameSpaces.length == 0) {
//         System.err.println("nameSpaces.length == 0");
//      }
//      for (String nameSpace : nameSpaces) {
//         System.err.println("nameSpace = " + nameSpace);
//      }
      if ((config == null) || (config.length == 0)) {
         return Window.CANCEL;
      }
      System.err.println("Found Wizard " + wizard.getClass().toString());
      Class<?> peClass = wizard.getClass();
      if (projectHandle != null) {
         // Set project handle so as to avoid initial dialogue pages
         try {
            System.err.println("Calling methods");
            Method method;
            method = peClass.getMethod("setProject", new Class[]{org.eclipse.core.resources.IProject.class});
            method.invoke(wizard, projectHandle);
            
            method = peClass.getMethod("setInitializationData", new Class[]{IConfigurationElement.class, String.class, Object.class});
            method.invoke(wizard, config[0], "", "");
            
            method = peClass.getMethod("setDeviceId", new Class[]{String.class});
            method.invoke(wizard, "MK64FN1M0xxx12_4SDK");
            
            method = peClass.getMethod("setProjectName", new Class[]{String.class});
            method.invoke(wizard, "A_Project");
            
            method = peClass.getMethod("setWizardInfo", new Class[]{String.class});
            method.invoke(wizard, "com.processorexpert.ui.kds.newprojectwizard.wizard1");
            
            
         } catch (NoSuchMethodException e) {
            System.err.println("setInitialProject() failed - NoSuchMethodException");
         } catch (SecurityException e) {
            System.err.println("setInitialProject() failed - SecurityException");
         } catch (IllegalAccessException e) {
            System.err.println("setInitialProject() failed - IllegalAccessException");
         } catch (IllegalArgumentException e) {
            System.err.println("setInitialProject() failed - IllegalArgumentException");
         } catch (InvocationTargetException e) {
         }
      }
      WizardDialog wizardDialog = new WizardDialog(shell, wizard);
      wizardDialog.setTitle(wizard.getWindowTitle());
      return wizardDialog.open();
   }
   
   void tryIt() {

      try {
         System.err.println("tryIt() ==========================================================");
         final String className = "com.processorexpert.core.ide.newprojectwizard.NewProjectWizardPlugin";
         Class<?> theClass = Class.forName(className);
         System.err.println("tryIt() - Obtained class " + className);
         Object clsInstance = theClass.newInstance();
         System.err.println("tryIt() - Instantiated class");

         System.err.println("Calling method getCachedWizards()");
         Method method;
         method = theClass.getMethod("getCachedWizards", new Class[]{});
         Object[] res1 = (Object[])method.invoke(clsInstance);
         System.err.println("tryIt() - res1.length = " + res1.length);
         
         for (Object obj : res1) {
            theClass = obj.getClass();
            method = theClass.getMethod("getWizardID", new Class[]{});
            String res2 = (String)method.invoke(obj);
            System.err.println("tryIt() - res2 = " + res2);
         }
         System.err.println("tryIt() ==========================================================");
         
      } catch (NoSuchMethodException e) {
         System.err.println("setInitialProject() failed - NoSuchMethodException");
      } catch (SecurityException e) {
         System.err.println("setInitialProject() failed - SecurityException");
      } catch (IllegalAccessException e) {
         System.err.println("setInitialProject() failed - IllegalAccessException");
      } catch (IllegalArgumentException e) {
         System.err.println("setInitialProject() failed - IllegalArgumentException");
      } catch (InvocationTargetException e) {
         System.err.println("setInitialProject() failed - InvocationTargetException");
      } catch (ClassNotFoundException e) {
         System.err.println("setInitialProject() failed - ClassNotFoundException");
      } catch (InstantiationException e) {
         System.err.println("setInitialProject() failed - InstantiationException");
      }
   }
   
   /**
    *  Display the Processor Expert Import dialogue
    *  Uses reflection
    * 
    * @param shell
    * @param projectHandle
    * @return
    * @throws ExecutionException
    */
   public int executePeWizard(Shell shell, IProject projectHandle) {

      IWizard wizard = findWizard("com.processorexpert.ui.pewizard.newprjwizard");
      if (wizard == null) {
         System.err.println("No Wizard found");
         return Window.CANCEL;
      }
//      System.err.println("Found Wizard " + wizard.getClass().toString());

      Class<?> peClass = wizard.getClass();
      if (projectHandle != null) {
         // Set project handle so as to avoid initial dialogue pages
         try {
            Method method = peClass.getMethod("setInitialProject", new Class[]{org.eclipse.core.resources.IProject.class});
            method.invoke(wizard, projectHandle);
         } catch (NoSuchMethodException e) {
            System.err.println("setInitialProject() failed - NoSuchMethodException");
         } catch (SecurityException e) {
            System.err.println("setInitialProject() failed - SecurityException");
         } catch (IllegalAccessException e) {
            System.err.println("setInitialProject() failed - IllegalAccessException");
         } catch (IllegalArgumentException e) {
            System.err.println("setInitialProject() failed - IllegalArgumentException");
         } catch (InvocationTargetException e) {
         }
      }
      WizardDialog wizardDialog = new WizardDialog(shell, wizard);
      wizardDialog.setTitle(wizard.getWindowTitle());
      return wizardDialog.open();
   }
   
   /**
    * Locate a wizard from its ID
    * 
    * @param id ID to search for
    * 
    * @return Wizard found or null
    */
   private IWizard findWizard(String id) {
      IWizardRegistry wizardRegistry[] = new IWizardRegistry[] {
            PlatformUI.getWorkbench().getNewWizardRegistry(),
            PlatformUI.getWorkbench().getImportWizardRegistry(),
            PlatformUI.getWorkbench().getExportWizardRegistry()};
      
      for (IWizardRegistry wizardReg : wizardRegistry) {
         IWizardDescriptor wizardDescriptor = wizardReg.findWizard(id);
         if (wizardDescriptor != null) {
            try {
               return wizardDescriptor.createWizard();
            } catch (CoreException e) {
               e.printStackTrace();
               return null;
            }
         }
      }
      return null;
   }

}
