/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.actions;


import java.util.Iterator;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.internal.debug.ui.ExceptionHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

public class DropToFrameAction extends ObjectActionDelegate {

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IStructuredSelection selection= getCurrentSelection();
		if (selection == null) {
			return;
		}
		Iterator itr= selection.iterator();
		
		while (itr.hasNext()) {
			IStackFrame frame= (IStackFrame)itr.next();
			IJavaStackFrame stackFrame = (IJavaStackFrame)frame.getAdapter(IJavaStackFrame.class);
			if (stackFrame != null) {
				try {
					stackFrame.dropToFrame();
				} catch (DebugException de) {
					String title= ActionMessages.getString("DropToFrameAction.Drop_to_Frame_1"); //$NON-NLS-1$
					String message= ActionMessages.getString("DropToFrameAction.Exceptions_occurred_attempting_to_drop_to_frame._2"); //$NON-NLS-1$
					ExceptionHandler.handle(de, title, message);
				}
			}
		}
	}
}
