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
package org.eclipse.jdt.internal.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

public class EnableDisableBreakpointRulerAction extends AbstractBreakpointRulerAction {

    /**
     * Creates the action to enable/disable breakpoints
     */
    public EnableDisableBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo info) {
        setInfo(info);
        setTextEditor(editor);
        setText(ActionMessages.getString("EnableDisableBreakpointRulerAction.&Enable_Breakpoint_1")); //$NON-NLS-1$
    }

    /**
     * @see Action#run()
     */
    public void run() {
        if (getBreakpoint() != null) {
            new Job(ActionMessages.getString("EnableDisableBreakpointRulerAction.0")) { //$NON-NLS-1$
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        getBreakpoint().setEnabled(!getBreakpoint().isEnabled());
                        return Status.OK_STATUS;
                    } catch (final CoreException e) {
                        Display.getDefault().asyncExec(new Runnable(){
                            public void run() {
                                ErrorDialog.openError(getTextEditor().getEditorSite().getShell(), ActionMessages.getString("EnableDisableBreakpointRulerAction.Enabling/disabling_breakpoints_2"), ActionMessages.getString("EnableDisableBreakpointRulerAction.Exceptions_occurred_enabling_disabling_the_breakpoint_3"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$        
                            }
                        });                        
                    }
                    return Status.CANCEL_STATUS;
                }
            }.schedule();
        }
    }

    /**
     * @see IUpdate#update()
     */
    public void update() {
        setBreakpoint(determineBreakpoint());
        if (getBreakpoint() == null) {
            setEnabled(false);
            return;
        }
        setEnabled(true);
        try {
            boolean enabled = getBreakpoint().isEnabled();
            setText(enabled ? ActionMessages.getString("EnableDisableBreakpointRulerAction.&Disable_Breakpoint_4") : ActionMessages.getString("EnableDisableBreakpointRulerAction.&Enable_Breakpoint_5")); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (CoreException ce) {
            JDIDebugUIPlugin.log(ce);
        }
    }
}
