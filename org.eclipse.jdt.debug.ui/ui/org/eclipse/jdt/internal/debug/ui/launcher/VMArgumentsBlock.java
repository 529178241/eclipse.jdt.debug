/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.launcher;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.stringsubstitution.StringVariableSelectionDialog;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Editor for VM arguments of a Java launch configuration.
 */
public class VMArgumentsBlock extends JavaLaunchConfigurationTab {

	// VM arguments widgets
	protected Label fVMArgumentsLabel;
	protected Text fVMArgumentsText;
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();

		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);
		GridLayout topLayout = new GridLayout();
		topLayout.marginHeight = 0;
		topLayout.marginWidth = 0;
		comp.setLayout(topLayout);	
		GridData gd = new GridData(GridData.FILL_BOTH);
		comp.setLayoutData(gd);
				
		fVMArgumentsLabel = new Label(comp, SWT.NONE);
		fVMArgumentsLabel.setText(LauncherMessages.getString("JavaArgumentsTab.VM_ar&guments__6")); //$NON-NLS-1$
		fVMArgumentsLabel.setFont(font);
		
		fVMArgumentsText = new Text(comp, SWT.MULTI | SWT.WRAP| SWT.BORDER | SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 40;
		gd.widthHint = 100;
		fVMArgumentsText.setLayoutData(gd);
		fVMArgumentsText.setFont(font);
		fVMArgumentsText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent evt) {
				updateLaunchConfigurationDialog();
			}
		});	
				
		Button pgrmArgVariableButton = createPushButton(comp, LauncherMessages.getString("VMArgumentsBlock.4"), null); //$NON-NLS-1$
		pgrmArgVariableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		pgrmArgVariableButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
				dialog.open();
				String variable = dialog.getVariableExpression();
				if (variable != null) {
					fVMArgumentsText.append(variable);
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
			
		});
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String)null);		
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			fVMArgumentsText.setText(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "")); //$NON-NLS-1$
		} catch (CoreException e) {
			setErrorMessage(LauncherMessages.getString("JavaArgumentsTab.Exception_occurred_reading_configuration___15") + e.getStatus().getMessage()); //$NON-NLS-1$
			JDIDebugUIPlugin.log(e);			
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, getAttributeValueFrom(fVMArgumentsText));
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return LauncherMessages.getString("VMArgumentsBlock.VM_Arguments"); //$NON-NLS-1$
	}
	
	/**
	 * Retuns the string in the text widget, or <code>null</code> if empty.
	 * 
	 * @return text or <code>null</code>
	 */
	protected String getAttributeValueFrom(Text text) {
		String content = text.getText().trim();
		if (content.length() > 0) {
			return content;
		}
		return null;
	}	
	
	public void setEnabled(boolean enabled) {
		fVMArgumentsLabel.setEnabled(enabled);
		fVMArgumentsText.setEnabled(enabled);
	}
}
