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
package org.eclipse.jdt.internal.debug.eval.ast.engine;

import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.model.JDIClassType;
import org.eclipse.jdt.internal.debug.core.model.JDIObjectValue;
import org.eclipse.jdt.internal.debug.core.model.JDIStackFrame;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;

/**
 * Creates the source code necessary to evaluate a code snippet.
 * The (simplified) structure of the source is as follows:
 * [package <package name>;]
 * [import <import name>;]*
 * public class <code snippet class name> extends <global variable class name> {
 *   public void run() {
 *     <code snippet>
 *   }
 * }
 */
public class EvaluationSourceGenerator {

	private String fCodeSnippet;
	
	private String[] fLocalVariableTypeNames;
	private String[] fLocalVariableNames;
		
	
	private String fSource;
	private String fCompilationUnitName;
	private int fSnippetStartPosition;
	private int fRunMethodStartPosition;
	private int fRunMethodLength;
	
	/**
	 * Rebuild source in presence of external local variables
	 */
	public EvaluationSourceGenerator(String[] localVariableTypesNames, String[] localVariableNames, String codeSnippet) {
		fLocalVariableTypeNames = localVariableTypesNames;
		fLocalVariableNames = localVariableNames;
	 	fCodeSnippet= getCompleteSnippet(codeSnippet);
	}
	
	public EvaluationSourceGenerator(String codeSnippet) {
		this(new String[0], new String[0], codeSnippet);
	}

	protected String getCompleteSnippet(String codeSnippet) {

		if (isExpression(codeSnippet)) {
			codeSnippet = "return " + codeSnippet + ';'; //$NON-NLS-1$
		}
		return codeSnippet;
	}
	
	/**
	 * Returns whether the given snippet represents an expression.
	 * This is determined by examining the snippet for non-quoted semicolons.
	 * 
	 * Returns <code>true</code> if the snippet is an expression, or
	 * <code>false</code> if the expresssion contains a statement.
	 */
	protected boolean isExpression(String codeSnippet) {
		boolean inString= false;
		byte[] chars= codeSnippet.getBytes();
		for (int i= 0, numChars= chars.length; i < numChars; i++) {
			switch (chars[i]) {
				case '\\':
					if (inString) { // skip the char after an escape char
						i++;
					}
					break;
				case '\"':
				case '\'':
					inString= !inString;
					break;
				case ';':
					if (!inString) {
						return false;
					}
					break;
			}
		}
		return true;
	}
	
	public String getCompilationUnitName() {
		return fCompilationUnitName;
	}
	
	public int getSnippetStart() {
		return fSnippetStartPosition;
	}
	public int getRunMethodStart() {
		return fRunMethodStartPosition;
	}
	public int getRunMethodLength() {
		return fRunMethodLength;
	}
	protected void setSnippetStart(int position) {
		fSnippetStartPosition= position;
	}
	protected void setRunMethodStart(int position) {
		fRunMethodStartPosition= position;
	}
	protected void setRunMethodLength(int length) {
		fRunMethodLength= length;
	}
	
	public String getSnippet() {
		return fCodeSnippet;
	}

	private void createEvaluationSourceFromSource(String source, int position, boolean isLineNumber, boolean createInAnInstanceMethod) throws DebugException {
		ASTParser parser = ASTParser.newParser(AST.JLS2);
		parser.setSource(source.toCharArray());
		CompilationUnit unit= (CompilationUnit)parser.createAST(null);
		SourceBasedSourceGenerator visitor= new SourceBasedSourceGenerator(unit, position, isLineNumber, createInAnInstanceMethod, fLocalVariableTypeNames, fLocalVariableNames, fCodeSnippet);
		unit.accept(visitor);
		
		if (visitor.hasError()) {
			throw new DebugException(new Status(IStatus.ERROR, JDIDebugPlugin.getUniqueIdentifier(), IStatus.OK, visitor.getError(), null));
		}
		
		String sourceRes= visitor.getSource();
		if (sourceRes == null) {
			return;
		}
		setSource(sourceRes);
		setCompilationUnitName(visitor.getCompilationUnitName());
		setSnippetStart(visitor.getSnippetStart());
		setRunMethodStart(visitor.getRunMethodStart());
		setRunMethodLength(visitor.getRunMethodLength());
	}
	
	private void createEvaluationSourceFromJDIObject(BinaryBasedSourceGenerator objectToEvaluationSourceMapper) {
		
		setCompilationUnitName(objectToEvaluationSourceMapper.getCompilationUnitName());
		setSnippetStart(objectToEvaluationSourceMapper.getSnippetStart());
		setRunMethodStart(objectToEvaluationSourceMapper.getRunMethodStart());
		setRunMethodLength(fCodeSnippet.length() + objectToEvaluationSourceMapper.getRunMethodLength());
		setSource(objectToEvaluationSourceMapper.getSource().insert(objectToEvaluationSourceMapper.getCodeSnippetPosition(), fCodeSnippet).toString());
	}
	
	private BinaryBasedSourceGenerator getInstanceSourceMapper(JDIObjectValue objectValue, boolean isInStaticMethod) {
		BinaryBasedSourceGenerator objectToEvaluationSourceMapper = new BinaryBasedSourceGenerator(fLocalVariableTypeNames, fLocalVariableNames, isInStaticMethod);
		objectToEvaluationSourceMapper.buildSource(objectValue);
		return objectToEvaluationSourceMapper;
	}
	
	private BinaryBasedSourceGenerator getStaticSourceMapper(JDIClassType classType, boolean isInStaticMethod) {
		BinaryBasedSourceGenerator objectToEvaluationSourceMapper = new BinaryBasedSourceGenerator(fLocalVariableTypeNames, fLocalVariableNames, isInStaticMethod);
		objectToEvaluationSourceMapper.buildSource(classType);
		return objectToEvaluationSourceMapper;
	}
			
	public String getSource(IJavaStackFrame frame) throws DebugException {
		if (fSource == null) {
			try {
				String baseSource= getSourceFromFrame(frame);
				int lineNumber= frame.getLineNumber();
				if (baseSource != null && lineNumber != -1) {
					createEvaluationSourceFromSource(baseSource,  frame.getLineNumber(), true, false);
				} 
				if (fSource == null) {
					JDIObjectValue object= (JDIObjectValue)frame.getThis();
					BinaryBasedSourceGenerator mapper;
					if (object != null) {
						// Class instance context
						mapper= getInstanceSourceMapper(object, ((JDIStackFrame)frame).getUnderlyingMethod().isStatic());
					} else {
						// Static context
						mapper= getStaticSourceMapper((JDIClassType)frame.getDeclaringType(), ((JDIStackFrame)frame).getUnderlyingMethod().isStatic());
					}
					createEvaluationSourceFromJDIObject(mapper);
				}
			} catch (JavaModelException e) {
				throw new DebugException(e.getStatus());
			}
		}
		return fSource;
	}
	
	public String getSource(IJavaObject thisObject, IJavaProject javaProject) throws DebugException  {
		if (fSource == null) {
				String baseSource= getTypeSourceFromProject(thisObject.getJavaType().getName(), javaProject);
				int lineNumber= getLineNumber((JDIObjectValue) thisObject);
				if (baseSource != null && lineNumber != -1) {
					createEvaluationSourceFromSource(baseSource, lineNumber, true, true);
				}
				if (fSource == null) {
					BinaryBasedSourceGenerator mapper= getInstanceSourceMapper((JDIObjectValue) thisObject, false);
					createEvaluationSourceFromJDIObject(mapper);
				}
		}
		return fSource;
	}

	private int getLineNumber(JDIObjectValue objectValue) {
		ReferenceType referenceType= objectValue.getUnderlyingObject().referenceType();
		String referenceTypeName= referenceType.name();
		Location location;
		Hashtable lineNumbers= new Hashtable();
		try {
			for (Iterator iterator = referenceType.allLineLocations().iterator(); iterator.hasNext();) {
				lineNumbers.put(new Integer(((Location)iterator.next()).lineNumber()), this);
			}
			for (Iterator iterator = referenceType.allLineLocations().iterator(); iterator.hasNext();) {
				location= (Location)iterator.next();
				if (!location.declaringType().name().equals(referenceTypeName)) {
					lineNumbers.remove(new Integer(((Location)iterator.next()).lineNumber()));
				}
			}
			if (lineNumbers.size() > 0) {
				return ((Integer)lineNumbers.keys().nextElement()).intValue();
			}
			return -1;
		} catch(AbsentInformationException e) {
			return -1;
		}
	}

	protected String getSourceFromFrame(IJavaStackFrame frame) throws JavaModelException {
		ILaunch launch= frame.getLaunch();
		if (launch == null) {
			return null;
		}
		ISourceLocator locator= launch.getSourceLocator();
		if (locator == null) {
			return null;
		}
		Object sourceElement= locator.getSourceElement(frame);
		if (sourceElement == null) {
			return null;
		}
		if (!(sourceElement instanceof IJavaElement) && sourceElement instanceof IAdaptable) {
			sourceElement = ((IAdaptable)sourceElement).getAdapter(IJavaElement.class);
		}
		if (sourceElement instanceof IType) {
			return ((IType)sourceElement).getCompilationUnit().getSource();
		}
		if (sourceElement instanceof ICompilationUnit) {
			return ((ICompilationUnit)sourceElement).getSource();
		}
		if (sourceElement instanceof IClassFile) {
			return ((IClassFile)sourceElement).getSource();
		}
		return null;
	}
	
	protected void setCompilationUnitName(String name) {
		fCompilationUnitName= name;
	}
	
	protected void setSource(String source) {
		fSource= source;
	}

	private String getTypeSourceFromProject(String typeName, IJavaProject javaProject) throws DebugException {
		String path = typeName;
		int pos = path.indexOf('$');
		if (pos != -1) {
			path= path.substring(0, pos);
		}
		pos++;
		path = path.replace('.', IPath.SEPARATOR);
		path+= ".java";			 //$NON-NLS-1$
		IPath sourcePath =  new Path(path);
		
		String source= null;
		try {
			IJavaElement result = javaProject.findElement(sourcePath);
			if (result != null) {
				if (result instanceof IClassFile) {
					source = ((IClassFile)result).getSource();
				} else if (result instanceof ICompilationUnit) {
					source = ((ICompilationUnit)result).getSource();
				}
			}
		} catch (JavaModelException e) {
			throw new DebugException(e.getStatus());
		}
		
		return source;	
	}

}
