/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.sun.jdi;

import java.security.BasicPermission;

public class JDIPermission extends  BasicPermission {
	
	public JDIPermission(String arg1) {
		super(arg1);
	}

	public JDIPermission(String arg1, String arg2) {
		super(arg1, arg2);
	}

}
