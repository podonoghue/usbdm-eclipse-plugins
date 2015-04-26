/*******************************************************************************
 * Copyright (c) 2013 Peter O'Donoghue
 * 
 * Added variable expansion on Path
 * 
 * Based on work by
 * 
 * Copyright (c) 2009, 2012 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Doug Schaefer - initial API and implementation
 *******************************************************************************/
package net.sourceforge.usbdm.cdt.tools;

import java.io.File;

import net.sourceforge.usbdm.constants.ToolInformationData;
import net.sourceforge.usbdm.constants.UsbdmSharedSettings;
import net.sourceforge.usbdm.jni.Usbdm;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

/**
 * @author Peter
 *
 */
public class ConfigurationEnvironmentVariableSupplier implements
IConfigurationEnvironmentVariableSupplier {

   /**
    * The id of the build tool option that identifies the build tool e.g. Codesourcery etc
    */
   String buildToolOptionId = null;

   /**
    * @param buildToolOptionId - option id for build tool
    */
   protected ConfigurationEnvironmentVariableSupplier(String buildToolOptionId) {
      this.buildToolOptionId = buildToolOptionId;
   }

   /**
    * @param configuration       The configuration to look in for options
    * @return                    The path to the build tools bin directory or null if error
    */
   private String getToolPath(IConfiguration configuration) {
      if (configuration == null) {
         // No configuration - Use default settings
         System.err.println("ConfigEnvVarSupplier.getToolPath() configuration = null");
         String toolPath = "";
         String pathVariableId = null; 
         if (buildToolOptionId.equals(UsbdmConstants.COLDFIRE_BUILDTOOLS_OPTIONS)) {
            pathVariableId = "usbdm_codesourcery_coldfire_path";
         }
         else if (buildToolOptionId.equals(UsbdmConstants.ARM_BUILDTOOLS_OPTIONS)) {
            pathVariableId = "usbdm_armLtd_arm_path";
         }
         UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();
         if ((settings != null) && (pathVariableId != null)) {
            toolPath = settings.get(pathVariableId);
         }
//         System.err.println("ConfigEnvVarSupplier.getToolPath() Found tool path = " + toolPath);
         return toolPath;
      }
//      System.err.println("ConfigEnvVarSupplier.getToolPath() configuration = "+configuration.getId());
      IToolChain toolChain = configuration.getToolChain();
      if (toolChain == null) {
         System.err.println("ConfigEnvVarSupplier.getToolPath() toolChain = null");
         return null;
      }
//      System.err.println("ConfigEnvVarSupplier.getToolPath() Checking toolchain: " + toolChain.getId());

      // Find selected build tool
      IOption buildToolOption = toolChain.getOptionBySuperClassId(buildToolOptionId);
//      if (buildToolOption != null) {
//         System.err.println("ConfigEnvVarSupplier.getToolPath() Checking toolchain: Found name =  " + buildToolOption.getName());
//         System.err.println("ConfigEnvVarSupplier.getToolPath() Checking toolchain: Found value = " + buildToolOption.getValue().toString());
//      }
      if (buildToolOption == null) {
         System.err.println("ConfigEnvVarSupplier.getToolPath() buildToolOption = null");
         return null;
      }

      // Get build path variable
      ToolInformationData toolData = ToolInformationData.getToolInformationTable().get(buildToolOption.getValue().toString());
      if (toolData == null) {
         System.err.println("ConfigEnvVarSupplier.getToolPath() toolData = null");

         System.err.println("ConfigEnvVarSupplier.getToolPath() configuration              = " + configuration.toString());
         System.err.println("ConfigEnvVarSupplier.getToolPath() toolChain                  = " + toolChain.toString());
         System.err.println("ConfigEnvVarSupplier.getToolPath() buildToolOption            = " + buildToolOption.toString());
         System.err.println("ConfigEnvVarSupplier.getToolPath() buildToolOption.getValue() = " + buildToolOption.getValue().toString());
         return null;
      }
      String pathVariableId = toolData.getPathVariableName();
      if (pathVariableId == null) {
         System.err.println("ConfigEnvVarSupplier.getToolPath() pathVariableId = null");
         return null;
      }
//      System.err.println("ConfigEnvVarSupplier.getToolPath() pathVariableId = "+pathVariableId);

      UsbdmSharedSettings settings = UsbdmSharedSettings.getSharedSettings();
      String toolPath = "";
      if (settings != null) {
         toolPath = settings.get(pathVariableId);
      }
//      System.err.println("ConfigEnvVarSupplier.getToolPath() Found tool path = " + toolPath);

//      // Do variable substitution
//      IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
//
//      IValueVariable pathVariable = null;
//      if (manager != null) {
//         pathVariable = manager.getValueVariable(pathVariableId);
//      }
//      String toolPath = null;
//      if (pathVariable != null) {
//         toolPath = pathVariable.getValue();
////         System.err.println("ConfigEnvVarSupplier.getToolPath() Found tool path = " + toolPath);
//      }
      return toolPath;
   }

   public IBuildEnvironmentVariable getVariable(String                       variableName,
                                                IConfiguration               configuration, 
                                                IEnvironmentVariableProvider provider) {
      
      if (PathEnvironmentVariable.isVar(variableName))
         return PathEnvironmentVariable.create(configuration, getToolPath(configuration));
      else
         return null;
   }

   public IBuildEnvironmentVariable[] getVariables(IConfiguration               configuration, 
                                                   IEnvironmentVariableProvider provider) {

      IBuildEnvironmentVariable path = PathEnvironmentVariable.create(configuration, getToolPath(configuration));
      return path != null ? new IBuildEnvironmentVariable[] { path } : new IBuildEnvironmentVariable[0];
   }

   private static class PathEnvironmentVariable implements IBuildEnvironmentVariable {

      public static String name = "PATH"; //$NON-NLS-1$
      private static IPath usbdmApplicationPath = null;

      private File path;

      private PathEnvironmentVariable(File path) {
         this.path = path;
      }

      public IPath getUsbdmApplicationPath() {
         
         if (usbdmApplicationPath == null) {
            usbdmApplicationPath = Usbdm.getApplicationPath();
            if (usbdmApplicationPath == null) {
               usbdmApplicationPath = new Path("USBDM PATH NOT FOUND");
            }
         }
         return usbdmApplicationPath;
      }
      
      public static PathEnvironmentVariable create(IConfiguration configuration, String buildToolPath) {

         if (buildToolPath == null) {
            System.err.println("ConfigurationEnvironmentVariableSupplier.PathEnvironmentVariable.create() - buildToolPath = null");
            return new PathEnvironmentVariable(new File("BUILD TOOLS PATH NOT FOUND"));
         }
         // Initially assume path is to bin dir
         File binDirectory = new File(buildToolPath);
         File probeBinDirectory = new File(binDirectory, "bin"); //$NON-NLS-1$
         if (probeBinDirectory.isDirectory()) {
            // Move to bin directory
            binDirectory = probeBinDirectory;
         }
         return new PathEnvironmentVariable(binDirectory);
      }

      public static boolean isVar(String name) {
         // Windows has case insensitive env var names
         return Platform.getOS().equals(Platform.OS_WIN32)
               ? name.equalsIgnoreCase(PathEnvironmentVariable.name)
                     : name.equals(PathEnvironmentVariable.name);
      }

      public String getDelimiter() {
         return Platform.getOS().equals(Platform.OS_WIN32) ? ";" : ":"; //$NON-NLS-1$ //$NON-NLS-2$
      }

      public String getName() {
         return name;
      }

      public int getOperation() {
         return IBuildEnvironmentVariable.ENVVAR_PREPEND;
      }

      public String getValue() {
         return path.getAbsolutePath()+getDelimiter()+getUsbdmApplicationPath().toOSString();
      }
   }
}
