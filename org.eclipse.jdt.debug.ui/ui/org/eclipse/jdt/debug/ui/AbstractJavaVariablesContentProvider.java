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
package org.eclipse.jdt.debug.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.debug.ui.IRootVariablesContentProvider;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.debug.core.IJavaVariable;

/**
 * Base class for java variables content providers.  This class provices several useful classes
 * that reduce the work of creating java variables content ptoviders.
 */
public abstract class AbstractJavaVariablesContentProvider implements IJavaVariablesContentProvider {

	/**
	 * Return an instanceof of <code>IJavaVariable</code> that is named <b>name</b> and is a child
	 * of <b>parent</b>.
	 * 
	 * @param parent
	 * @param name
	 * @return
	 * @throws DebugException
	 */
	protected IJavaVariable getChildNamed(IJavaVariable parent, String name) throws DebugException {
		IJavaValue javaValue = (IJavaValue) parent.getValue();
		IVariable[] children = javaValue.getVariables();
		for (int i = 0; i < children.length; i++) {
			IJavaVariable var = (IJavaVariable) children[i];
			String varName = var.getName();
			if (varName.equals(name)) {
				if (isNullValued(var)) {
					return null;
				}
				return var;
			}
		}	
		return null;	
	}
	
	/**
	 * Return a <code>java.util.List</code> containing all instances of <code>IJavaVariable</code>
	 * that are not <code>null</code> valued.
	 * 
	 * @param parent
	 * @return
	 * @throws DebugException
	 */
	protected List getNonNullChildren(IJavaVariable parent) throws DebugException {
		IJavaValue elementValue = (IJavaValue)parent.getValue();
		IVariable[] elementChildren = elementValue.getVariables();
		ArrayList selectedChildren = new ArrayList(elementChildren.length);
		for (int i = 0; i < elementChildren.length; i++) {
			IJavaVariable var = (IJavaVariable) elementChildren[i];
			if (isNullValued(var)) {
				continue;
			}
			selectedChildren.add(var);
		}	
		return selectedChildren;	
	}
	
	/**
	 * Return <code>true</code> if the specified variable is <code>null</code> valued.
	 * 
	 * @param var
	 * @return
	 * @throws DebugException
	 */
	protected boolean isNullValued(IJavaVariable var) throws DebugException {
		return ((IJavaValue)var.getValue()).getJavaType() == null;
	}

	protected IJavaObject getObjectValue(IJavaVariable variable) throws DebugException {
		IJavaValue parentValue = (IJavaValue)variable.getValue();
		if (!(parentValue instanceof IJavaObject)) {
			return null;
		}
		return (IJavaObject) parentValue;		
	}
	
	protected IJavaThread getJavaThreadFor(IDebugView view) {
		IRootVariablesContentProvider rootCP = (IRootVariablesContentProvider) view.getAdapter(IRootVariablesContentProvider.class);
		if (rootCP == null) {
			return null;
		}
		return (IJavaThread) rootCP.getThread();		
	}
}
