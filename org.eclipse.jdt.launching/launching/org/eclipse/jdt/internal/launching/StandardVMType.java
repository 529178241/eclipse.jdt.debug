package org.eclipse.jdt.internal.launching;/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */import java.io.File;import org.eclipse.core.runtime.IPath;import org.eclipse.core.runtime.IStatus;import org.eclipse.core.runtime.Path;import org.eclipse.core.runtime.Status;import org.eclipse.jdt.launching.AbstractVMInstallType;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.LibraryLocation;/** * A VM install type for VMs the conform to the standard * JDK installion layout. */public class StandardVMType extends AbstractVMInstallType {		/**	 * The root path for the attached src	 */	private String fDefaultRootPath;		/**	 * @see IVMInstallTypeype#validateInstallLocation(File)	 */	public IStatus validateInstallLocation(File installLocation) {		File java= new File(installLocation, "bin"+File.separator+"java"); //$NON-NLS-2$ //$NON-NLS-1$		File javaExe= new File(installLocation, "bin"+File.separator+"java.exe"); //$NON-NLS-2$ //$NON-NLS-1$		if (!(java.isFile() || javaExe.isFile())) {			return new Status(IStatus.ERROR, LaunchingPlugin.PLUGIN_ID, 0, LaunchingMessages.getString("StandardVMType.Not_a_JDK_Root;_Java_executable_was_not_found_1"), null); //$NON-NLS-1$		}		return new Status(IStatus.OK, LaunchingPlugin.PLUGIN_ID, 0, LaunchingMessages.getString("StandardVMType.ok_2"), null); //$NON-NLS-1$	}	/**	 * @see IVMInstallType#getName()	 */	public String getName() {		return LaunchingMessages.getString("StandardVMType.Standard_VM_3"); //$NON-NLS-1$	}		protected IVMInstall doCreateVMInstall(String id) {		return new StandardVM(this, id);	}		protected boolean canDetectExecutable(File javaHome) {		File java= new File(javaHome, File.separator+"bin"+File.separator+"java"); //$NON-NLS-2$ //$NON-NLS-1$		File javaExe= new File(javaHome, File.separator+"bin"+File.separator+"java.exe"); //$NON-NLS-2$ //$NON-NLS-1$		if (!(java.isFile() || javaExe.isFile())) {			return false;		}		return true; 	}		/**	 * @see IVMInstallType#detectInstallLocation()	 */	public File detectInstallLocation() {		File javaHome= new File (System.getProperty("java.home")); //$NON-NLS-1$		File parent= new File(javaHome.getParent());		if (!canDetectExecutable(javaHome)) {			return null;		}					if (canDetectExecutable(parent)) {			javaHome= parent;		}					String vendor= System.getProperty("java.vendor"); //$NON-NLS-1$		if (!(vendor.startsWith("Sun") || vendor.startsWith("IBM"))) { //$NON-NLS-2$ //$NON-NLS-1$ 			return null;		}		if ("J9".equals(System.getProperty("java.vm.name"))) {//$NON-NLS-2$ //$NON-NLS-1$			return null;		}		return javaHome;	}	private IPath getDefaultSystemLibrary(File installLocation) {		IPath jreLibPath= new Path(installLocation.getPath()).append("lib").append("rt.jar"); //$NON-NLS-2$ //$NON-NLS-1$		if (jreLibPath.toFile().isFile()) {			return jreLibPath;		}		return new Path(installLocation.getPath()).append("jre").append("lib").append("rt.jar"); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$	}	private IPath getDefaultSystemLibrarySource(File installLocation) {		File parent= installLocation.getParentFile();		if (parent != null) {			File parentsrc= new File(parent, "src.jar"); //$NON-NLS-1$			if (parentsrc.isFile()) {				setDefaultRootPath("src");//$NON-NLS-1$				return new Path(parentsrc.getPath());			}			parentsrc= new File(parent, "src.zip"); //$NON-NLS-1$			if (parentsrc.isFile()) {				setDefaultRootPath(""); //$NON-NLS-1$				return new Path(parentsrc.getPath());			}			parentsrc= new File(installLocation, "src.jar"); //$NON-NLS-1$			if (parentsrc.isFile()) {				setDefaultRootPath("src"); //$NON-NLS-1$				return new Path(parentsrc.getPath());			}						parentsrc= new File(installLocation, "src.zip"); //$NON-NLS-1$			if (parentsrc.isFile()) {				setDefaultRootPath(""); //$NON-NLS-1$				return new Path(parentsrc.getPath());			}					}		setDefaultRootPath("src"); //$NON-NLS-1$		return new Path(installLocation.getPath()).append("src.jar"); //$NON-NLS-1$	}	protected IPath getDefaultPackageRootPath() {		return new Path(getDefaultRootPath());	}	/**	 * @see IVMInstallType#getDefaultSystemLibraryDescription(File)	 */	public LibraryLocation getDefaultLibraryLocation(File installLocation) {		return new LibraryLocation(getDefaultSystemLibrary(installLocation),						getDefaultSystemLibrarySource(installLocation), 						getDefaultPackageRootPath());	}	protected String getDefaultRootPath() {		return fDefaultRootPath;	}	protected void setDefaultRootPath(String defaultRootPath) {		fDefaultRootPath = defaultRootPath;	}}