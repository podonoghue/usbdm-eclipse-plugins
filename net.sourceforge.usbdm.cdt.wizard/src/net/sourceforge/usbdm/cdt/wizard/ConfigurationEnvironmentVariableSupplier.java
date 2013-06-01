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
package net.sourceforge.usbdm.cdt.wizard;

import java.io.File;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;

public class ConfigurationEnvironmentVariableSupplier implements
		IConfigurationEnvironmentVariableSupplier {

   String codesourceryPathVariableName = null;
   
   /**
    * @param codesourceryPathVariableName - environment variable name containing path e.g. codesourcery_arm_path
    */
   public ConfigurationEnvironmentVariableSupplier(String codesourceryPathVariableName) {
//      System.err.println("UsbdmConfigurationEnvironmentVariableSupplier() - codesourceryPathVariableName = "+codesourceryPathVariableName);
      this.codesourceryPathVariableName =  codesourceryPathVariableName;
   }
	public IBuildEnvironmentVariable getVariable(String                       variableName,
                                       			IConfiguration               configuration, 
                                       			IEnvironmentVariableProvider provider) {
	   
		if (PathEnvironmentVariable.isVar(variableName))
			return PathEnvironmentVariable.create(configuration, codesourceryPathVariableName);
		else
			return null;
	}

	public IBuildEnvironmentVariable[] getVariables(IConfiguration               configuration, 
	                                                IEnvironmentVariableProvider provider) {
	   
		IBuildEnvironmentVariable path = PathEnvironmentVariable.create(configuration, codesourceryPathVariableName);
		return path != null ? new IBuildEnvironmentVariable[] { path } : new IBuildEnvironmentVariable[0];
	}

	private static class PathEnvironmentVariable implements IBuildEnvironmentVariable {

		public static String name = "PATH"; //$NON-NLS-1$
		
		private File path;
		
		private PathEnvironmentVariable(File path) {
			this.path = path;
		}
		
		public static PathEnvironmentVariable create(IConfiguration configuration, String codesourceryPathVariableName) {

		   // Do variable substitution
		   IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();

		   IValueVariable codesourceryPathVariable = null;
		   if (manager != null) {
            codesourceryPathVariable = manager.getValueVariable(codesourceryPathVariableName);
		   }
		   String codesourceryPath = null;
		   if (codesourceryPathVariable != null) {
		      codesourceryPath = codesourceryPathVariable.getValue();
		   }
		   if (codesourceryPath == null) {
            System.err.println("C.PathEnvironmentVariable.create() - codesourceryPath = null");
		      return new PathEnvironmentVariable(new File("PATH NOT FOUND"));
		   }
		   // Initially assume path is to codesourcery bin dir
		   File codesourceryBinDirectory = new File(codesourceryPath);
		   File probeBinDirectory = new File(codesourceryBinDirectory, "bin"); //$NON-NLS-1$
		   if (probeBinDirectory.isDirectory()) {
		      // Move to bin directory
		      codesourceryBinDirectory = probeBinDirectory;
		   }
//	      System.err.println("C.PathEnvironmentVariable.create() - sysroot = "+codesourceryBinDirectory.toString());
		   return new PathEnvironmentVariable(codesourceryBinDirectory);
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
			return path.getAbsolutePath();
		}
	}
}
