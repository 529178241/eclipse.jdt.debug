/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.debug.testplugin.JavaProjectHelper;
import org.eclipse.jdt.debug.testplugin.JavaTestPlugin;
import org.eclipse.jdt.internal.debug.ui.IJDIPreferencesConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Test to close the workbench, since debug tests do not run in the UI
 * thread.
 */
public class ProjectCreationDecorator extends AbstractDebugTest {
	
	public ProjectCreationDecorator(String name) {
		super(name);
	}
	
	public void testProjectCreation() throws Exception {
		// delete any pre-existing project
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject("DebugTests");
		if (pro.exists()) {
			pro.delete(true, true, null);
		}
		// create project and import source
		fJavaProject = JavaProjectHelper.createJavaProject("DebugTests", "bin");
		IPackageFragmentRoot src = JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		File root = JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.TEST_SRC_DIR);
		JavaProjectHelper.importFilesFromDirectory(root, src.getPath(), null);
		
		// add rt.jar
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		assertNotNull("No default JRE", vm);
		JavaProjectHelper.addVariableEntry(fJavaProject, new Path(JavaRuntime.JRELIB_VARIABLE), new Path(JavaRuntime.JRESRC_VARIABLE), new Path(JavaRuntime.JRESRCROOT_VARIABLE));
		
		pro = fJavaProject.getProject();
		
		//add A.jar
		root = JavaTestPlugin.getDefault().getFileInPlugin(new Path("testjars"));
		JavaProjectHelper.importFilesFromDirectory(root, src.getPath(), null);
		IPath path = src.getPath().append("A.jar");
		JavaProjectHelper.addLibrary(fJavaProject, path);
		
		// create launch configuration folder
		
		IFolder folder = pro.getFolder("launchConfigurations");
		if (folder.exists()) {
			folder.delete(true,null);
		}
		folder.create(true, true, null);
		
		// delete any existing launch configs
		ILaunchConfiguration[] configs = getLaunchManager().getLaunchConfigurations();
		for (int i = 0; i < configs.length; i++) {
			configs[i].delete();
		}
		
		// create launch configurations
		createLaunchConfiguration("Breakpoints");
		createLaunchConfiguration("InstanceVariablesTests");
		createLaunchConfiguration("LocalVariablesTests");
		createLaunchConfiguration("StaticVariablesTests");
		createLaunchConfiguration("DropTests");
		createLaunchConfiguration("ThrowsNPE");
		createLaunchConfiguration("org.eclipse.debug.tests.targets.Watchpoint");
		createLaunchConfiguration("A");
		createLaunchConfiguration("HitCountLooper");
		createLaunchConfiguration("CompileError");
		createLaunchConfiguration("MultiThreadedLoop");
		createLaunchConfiguration("HitCountException");
		createLaunchConfiguration("MultiThreadedException");
		createLaunchConfiguration("MultiThreadedList");
		createLaunchConfiguration("MethodLoop");
		createLaunchConfiguration("StepFilterOne");
				
		createLaunchConfiguration("EvalArrayTests");
		createLaunchConfiguration("EvalSimpleTests");
		createLaunchConfiguration("EvalTypeTests");
		createLaunchConfiguration("EvalNestedTypeTests");
		createLaunchConfiguration("EvalTypeHierarchyTests");
		createLaunchConfiguration("WorkingDirectoryTest");
		createLaunchConfiguration("OneToTen");
		createLaunchConfiguration("OneToTenPrint");
		createLaunchConfiguration("FloodConsole");
		createLaunchConfiguration("ConditionalStepReturn");	
		createLaunchConfiguration("VariableChanges");		
		createLaunchConfiguration("DefPkgReturnType");
		createLaunchConfiguration("InstanceFilterObject");
		createLaunchConfiguration("org.eclipse.debug.tests.targets.CallStack");
		createLaunchConfiguration("org.eclipse.debug.tests.targets.HcrClass");
		createLaunchConfiguration("org.eclipse.debug.tests.targets.StepIntoSelectionClass");
		createLaunchConfiguration("WatchItemTests");
		createLaunchConfiguration("ArrayTests");
		createLaunchConfiguration("PerfLoop");
		createLaunchConfiguration("StackTraces");
		createLaunchConfiguration("ConsoleInput");
		createLaunchConfiguration("PrintConcatenation");
	}
	
	/**
	 * Creates a shared launch configuration for the type with the given
	 * name.
	 */
	protected void createLaunchConfiguration(String mainTypeName) throws Exception {
		ILaunchConfigurationType type = getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		ILaunchConfigurationWorkingCopy config = type.newInstance(getJavaProject().getProject().getFolder("launchConfigurations"), mainTypeName);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainTypeName);
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, getJavaProject().getElementName());
		// use 'java' instead of 'javaw' to launch tests (javaw is problematic on JDK1.4.2)
		Map map = new HashMap(1);
		map.put(IJavaLaunchConfigurationConstants.ATTR_JAVA_COMMAND, "java");
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE_SPECIFIC_ATTRS_MAP, map);
		config.doSave();
	}
	
	/**
	 * Set up preferences that need to be changed for the tests
	 */
	public void testSetPreferences() {
		// Turn of suspend on  uncaught exceptions
		setSuspendOnUncaughtExceptionsPreference(false);
		IPreferenceStore debugUIPreferences = DebugUIPlugin.getDefault().getPreferenceStore();
		// Don't prompt for perspective switching
		debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND, MessageDialogWithToggle.ALWAYS);
		debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE, MessageDialogWithToggle.ALWAYS);
		debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_RELAUNCH_IN_DEBUG_MODE, MessageDialogWithToggle.NEVER);
		debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_WAIT_FOR_BUILD, MessageDialogWithToggle.ALWAYS);
		debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, MessageDialogWithToggle.ALWAYS);
		
		String property = System.getProperty("debug.workbenchActivation");
		if (property != null && property.equals("off")) {
		    debugUIPreferences.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, false);
		    debugUIPreferences.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, false);
		    debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW, false);
		    debugUIPreferences.setValue(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH, false);
		}
		
		IPreferenceStore jdiUIPreferences = JDIDebugUIPlugin.getDefault().getPreferenceStore();
		// Don't warn about HCR failures
		jdiUIPreferences.setValue(IJDIPreferencesConstants.PREF_ALERT_HCR_FAILED, false);
		jdiUIPreferences.setValue(IJDIPreferencesConstants.PREF_ALERT_HCR_NOT_SUPPORTED, false);
		jdiUIPreferences.setValue(IJDIPreferencesConstants.PREF_ALERT_OBSOLETE_METHODS, false);
		// Set the timeout preference to a high value, to avoid timeouts while testing
		JDIDebugModel.getPreferences().setDefault(JDIDebugModel.PREF_REQUEST_TIMEOUT, 10000);
		// turn off monitor information
		jdiUIPreferences.setValue(IJDIPreferencesConstants.PREF_SHOW_MONITOR_THREAD_INFO, false);
	}
	
	/**
	 * Create a project with non-default, mulitple output locations.
	 * 
	 * @throws Exception
	 */
	public void testMultipleOutputProjectCreation() throws Exception {
		// delete any pre-existing project
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject("MultiOutput");
		if (pro.exists()) {
			pro.delete(true, true, null);
		}
		// create project with two src folders and output locations
		IJavaProject project = JavaProjectHelper.createJavaProject("MultiOutput");
		JavaProjectHelper.addSourceContainer(project, "src1", "bin1");
		JavaProjectHelper.addSourceContainer(project, "src2", "bin2");
		
		// add rt.jar
		IVMInstall vm = JavaRuntime.getDefaultVMInstall();
		assertNotNull("No default JRE", vm);
		JavaProjectHelper.addVariableEntry(project, new Path(JavaRuntime.JRELIB_VARIABLE), new Path(JavaRuntime.JRESRC_VARIABLE), new Path(JavaRuntime.JRESRCROOT_VARIABLE));
				
	}
}
