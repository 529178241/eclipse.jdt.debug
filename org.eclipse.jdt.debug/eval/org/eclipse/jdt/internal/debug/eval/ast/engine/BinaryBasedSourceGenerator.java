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

 
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.internal.debug.core.model.JDIClassType;
import org.eclipse.jdt.internal.debug.core.model.JDIReferenceType;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.Type;

public class BinaryBasedSourceGenerator {
	
	private static final String RUN_METHOD_NAME= "___run"; //$NON-NLS-1$
	private static final String EVAL_METHOD_NAME= "___eval"; //$NON-NLS-1$
	private static final String ANONYMOUS_CLASS_NAME= "___EvalClass"; //$NON-NLS-1$
	
	
	private String[] fLocalVariableTypeNames;
	
	private String[] fLocalVariableNames;
	
	private boolean fIsInStaticMethod;
	
	private int fApiLevel;
	
	private StringBuffer fSource;
	
	private int fRunMethodStartOffset;
	private int fRunMethodLength;
	private int fCodeSnippetPosition;
	
	private String fCompilationUnitName;
	
	public BinaryBasedSourceGenerator(String[] localTypesNames, String[] localVariables, boolean isInStaticMethod, int apiLevel) {
		fLocalVariableTypeNames= localTypesNames;
		fLocalVariableNames= localVariables;
		fIsInStaticMethod= isInStaticMethod;
		fApiLevel= apiLevel;
	}
	
	/**
	 * Build source for an object value (instance context)
	 */
	public void buildSource(JDIReferenceType referenceType) {
		ReferenceType reference= (ReferenceType)referenceType.getUnderlyingType();
		fSource= buildTypeDeclaration(reference, buildRunMethod(reference), null);
	}
	
	/**
	 * Build source for a class type (static context)
	 */
	public void buildSourceStatic(JDIClassType type) {
		Type underlyingType= type.getUnderlyingType();
		if (!(underlyingType instanceof ReferenceType)) {
			return;
		}
		ReferenceType refType= (ReferenceType)underlyingType;
		fSource= buildTypeDeclaration(refType, buildRunMethod(refType), null, false);
		String packageName = getPackageName(refType.name());
		if (packageName != null) {
			fSource.insert(0, "package " + packageName + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
			fCodeSnippetPosition += 10 + packageName.length();
		}
		fCompilationUnitName= getSimpleName(refType.name());
	}
	
	protected String getUniqueMethodName(String methodName, ReferenceType type) {
		List methods= type.methodsByName(methodName);
		while (!methods.isEmpty()) {
			methodName += '_';
			methods= type.methodsByName(methodName);
		}
		return methodName;
	}
	
	private StringBuffer buildRunMethod(ReferenceType type) {
		StringBuffer source = new StringBuffer();
		
		if (isInStaticMethod()) {
			source.append("static "); //$NON-NLS-1$
		}

		source.append("void "); //$NON-NLS-1$
		source.append(getUniqueMethodName(RUN_METHOD_NAME, type));
		source.append('(');
		for(int i= 0, length= fLocalVariableNames.length; i < length; i++) {
			source.append(getDotName(fLocalVariableTypeNames[i]));
			source.append(' ');
			source.append(fLocalVariableNames[i]);
			if (i + 1 < length)
				source.append(", "); //$NON-NLS-1$
		}
		source.append(") throws Throwable {"); //$NON-NLS-1$
		source.append('\n');
		fCodeSnippetPosition= source.length();
		fRunMethodStartOffset= fCodeSnippetPosition;

		source.append('\n');
		source.append('}').append('\n');
		fRunMethodLength= source.length();
		return source;
	}
	
	private StringBuffer buildTypeDeclaration(ReferenceType referenceType, StringBuffer buffer, String nestedTypeName) {
		
		Field thisField= null;
		
		List fields= referenceType.visibleFields();
		for (Iterator iterator= fields.iterator(); iterator.hasNext();) {
			Field field= (Field) iterator.next();
			if (field.name().startsWith("this$")) { //$NON-NLS-1$
				thisField = field;
				break;
			}
		}
		
		StringBuffer source = buildTypeDeclaration(referenceType, buffer, nestedTypeName, thisField != null);
		
		if (thisField == null) {
			String packageName = getPackageName(referenceType.name());
			if (packageName != null) {
				source.insert(0, "package " + packageName + ";\n"); //$NON-NLS-1$ //$NON-NLS-2$
				fCodeSnippetPosition += 10 + packageName.length();
			}
			if (isAnonymousTypeName(referenceType.name())) {
				fCompilationUnitName= ANONYMOUS_CLASS_NAME;
			} else {
				fCompilationUnitName= getSimpleName(referenceType.name());
			}
		} else {
			try {
				return buildTypeDeclaration((ReferenceType) thisField.type(), source, referenceType.name());
			} catch (ClassNotLoadedException e) {
			}
		}
		
		return source;
	}

	private StringBuffer buildTypeDeclaration(ReferenceType referenceType, StringBuffer buffer, String nestedTypeName, boolean hasEnclosingInstance) {
		StringBuffer source= new StringBuffer();
		
		String typeName= referenceType.name();
		
		boolean isAnonymousType= isAnonymousTypeName(typeName);
		
		if (isAnonymousType) {
			ClassType classType= (ClassType) referenceType;
			
			List interfaceList= classType.interfaces();
			String superClassName= classType.superclass().name();
			if (hasEnclosingInstance) {
				source.append("void "); //$NON-NLS-1$
				source.append(getUniqueMethodName(EVAL_METHOD_NAME, referenceType));
				source.append("() {\nnew "); //$NON-NLS-1$
				if (interfaceList.size() != 0) {
					source.append(getDotName(((InterfaceType)interfaceList.get(0)).name()));
				} else {
					source.append(getDotName(superClassName));
				}
				source.append("()"); //$NON-NLS-1$
			} else {
				source.append("public class ").append(ANONYMOUS_CLASS_NAME).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
				if (interfaceList.size() != 0) {
					source.append(" implements ").append(getDotName(((InterfaceType)interfaceList.get(0)).name())); //$NON-NLS-1$
				} else {
					source.append(" extends ").append(getDotName(superClassName)); //$NON-NLS-1$
				}
			}
			
		} else {
			if (referenceType.isFinal()) {
				source.append("final "); //$NON-NLS-1$
			}
			
			if (referenceType.isStatic()) {
				source.append("static "); //$NON-NLS-1$
			}
			
			if (referenceType instanceof ClassType) {
				ClassType classType= (ClassType) referenceType;
				
				if (classType.isAbstract()) {
					source.append("abstract "); //$NON-NLS-1$
				}
			
				source.append("class "); //$NON-NLS-1$

				source.append(getSimpleName(typeName)).append(' ');

				String genericSignature= referenceType.genericSignature();
				if (fApiLevel == AST.JLS3 && genericSignature != null) {
					String[] typeParameters= Signature.getTypeParameters(genericSignature);
					if (typeParameters.length > 0) {
						source.append('<');
						source.append(Signature.getTypeVariable(typeParameters[0]));
						String[] typeParameterBounds= Signature.getTypeParameterBounds(typeParameters[0]);
						source.append(" extends ").append(Signature.toString(typeParameterBounds[0]).replace('/', '.')); //$NON-NLS-1$
						for (int i= 1; i < typeParameterBounds.length; i++) {
							source.append(" & ").append(Signature.toString(typeParameterBounds[i]).replace('/', '.')); //$NON-NLS-1$
						}
						for (int j= 1; j < typeParameters.length; j++) {
							source.append(',').append(Signature.getTypeVariable(typeParameters[j]));
							typeParameterBounds= Signature.getTypeParameterBounds(typeParameters[j]);
							source.append(" extends ").append(Signature.toString(typeParameterBounds[0]).replace('/', '.')); //$NON-NLS-1$
							for (int i= 1; i < typeParameterBounds.length; i++) {
								source.append(" & ").append(Signature.toString(typeParameterBounds[i]).replace('/', '.')); //$NON-NLS-1$
							}
						}
						source.append("> "); //$NON-NLS-1$
					}
					String[] superClassInterfaces= SignatureExt.getTypeSuperClassInterfaces(genericSignature);
					int length= superClassInterfaces.length;
					if (length > 0) {
						source.append("extends ").append(Signature.toString(superClassInterfaces[0]).replace('/', '.')); //$NON-NLS-1$
						if (length > 1) {
							source.append(" implements ").append(Signature.toString(superClassInterfaces[1]).replace('/', '.')); //$NON-NLS-1$
							for (int i = 2; i < length; i++) {
								source.append(',').append(Signature.toString(superClassInterfaces[1]));
							}
						}
					}
				} else {
				
					ClassType superClass= classType.superclass();
					if (superClass != null) {
						source.append("extends ").append(getDotName(superClass.name())).append(' '); //$NON-NLS-1$
					}
	
					List interfaces;
					try {
						interfaces= classType.interfaces();
					} catch (ClassNotPreparedException e) {
						return new StringBuffer();
					}
					if (interfaces.size() != 0) {
						source.append("implements "); //$NON-NLS-1$
						Iterator iterator= interfaces.iterator();
						InterfaceType interface_= (InterfaceType)iterator.next();
						source.append(getDotName(interface_.name()));
						while (iterator.hasNext()) {
							source.append(',').append(getDotName(((InterfaceType)iterator.next()).name()));
						}
					}
				}
			} else if (referenceType instanceof InterfaceType) {
				if (buffer != null) {
					source.append("abstract class "); //$NON-NLS-1$
					source.append(getSimpleName(typeName)).append("___ implements "); //$NON-NLS-1$
					source.append(typeName.replace('$', '.')).append(" {\n"); //$NON-NLS-1$
					fCodeSnippetPosition += source.length();
					source.append(buffer).append("}\n"); //$NON-NLS-1$
				}
				
				return source;
			}
		}
		
		source.append(" {\n"); //$NON-NLS-1$
		
		if (buffer != null && !(referenceType instanceof InterfaceType)) {
			fCodeSnippetPosition += source.length();
			source.append(buffer);
		}
		
		List fields= referenceType.fields();
		for (Iterator iterator= fields.iterator(); iterator.hasNext();) {
			Field field= (Field) iterator.next();
			if (!field.name().startsWith("this$")) { //$NON-NLS-1$
				source.append(buildFieldDeclaration(field));
			}
		}
		
		List methods= referenceType.methods();
		for (Iterator iterator = methods.iterator(); iterator.hasNext();) {
			Method method= (Method) iterator.next();
			if (!method.isConstructor() && !method.isStaticInitializer() && !method.isBridge()) {
				source.append(buildMethodDeclaration(method));
			}
		}
		
		List nestedTypes= referenceType.nestedTypes();
		if (nestedTypeName == null) {
			for (Iterator iterator = nestedTypes.iterator(); iterator.hasNext();) {
				ReferenceType nestedType= (ReferenceType) iterator.next();
				if (isADirectInnerType(typeName, nestedType.name())) {
					source.append(buildTypeDeclaration(nestedType, null, null, true));
				}
			}
		} else {
			for (Iterator iterator = nestedTypes.iterator(); iterator.hasNext();) {
				ReferenceType nestedType= (ReferenceType) iterator.next();
				if (!nestedTypeName.equals(nestedType.name()) && isADirectInnerType(typeName, nestedType.name())) {
					source.append(buildTypeDeclaration(nestedType, null, null, true));
				}
			}
		}
		
		if (isAnonymousType & hasEnclosingInstance) {
			source.append("};\n"); //$NON-NLS-1$
		}
		
		source.append("}\n"); //$NON-NLS-1$
		
		return source;
	}
	
	private StringBuffer buildFieldDeclaration(Field field) {
		StringBuffer source = new StringBuffer();
		
		if (field.isFinal()) {
			source.append("final "); //$NON-NLS-1$
		}
		
		if (field.isStatic()) {
			source.append("static "); //$NON-NLS-1$
		}
		
		if (field.isPublic()) {
			source.append("public "); //$NON-NLS-1$
		} else if (field.isPrivate()) {
			source.append("private "); //$NON-NLS-1$
		} else if (field.isProtected()) {
			source.append("protected "); //$NON-NLS-1$
		}
		
		source.append(getDotName(field.typeName())).append(' ').append(field.name()).append(';').append('\n');
		
		return source;
	}
	
	private StringBuffer buildMethodDeclaration(Method method) {
		StringBuffer source= new StringBuffer();
		
		if (method.isFinal()) {
			source.append("final "); //$NON-NLS-1$
		}
		
		if (method.isStatic()) {
			source.append("static "); //$NON-NLS-1$
		}
		
		if (method.isNative()) {
			source.append("native "); //$NON-NLS-1$
		} else if (method.isAbstract()) {
			source.append("abstract "); //$NON-NLS-1$
		}
		
		if (method.isPublic()) {
			source.append("public "); //$NON-NLS-1$
		} else if (method.isPrivate()) {
			source.append("private "); //$NON-NLS-1$
		} else if (method.isProtected()) {
			source.append("protected "); //$NON-NLS-1$
		}
		
		String genericSignature= method.genericSignature();
		if (fApiLevel == AST.JLS3 && genericSignature != null) {
			String[] typeParameters= Signature.getTypeParameters(genericSignature);
			if (typeParameters.length > 0) {
				source.append('<');
				source.append(Signature.getTypeVariable(typeParameters[0]));
				String[] typeParameterBounds= Signature.getTypeParameterBounds(typeParameters[0]);
				source.append(" extends ").append(Signature.toString(typeParameterBounds[0]).replace('/', '.')); //$NON-NLS-1$
				for (int i= 1; i < typeParameterBounds.length; i++) {
					source.append(" & ").append(Signature.toString(typeParameterBounds[i]).replace('/', '.')); //$NON-NLS-1$
				}
				for (int j= 1; j < typeParameters.length; j++) {
					source.append(',').append(Signature.getTypeVariable(typeParameters[j]));
					typeParameterBounds= Signature.getTypeParameterBounds(typeParameters[j]);
					source.append(" extends ").append(Signature.toString(typeParameterBounds[0]).replace('/', '.')); //$NON-NLS-1$
					for (int i= 1; i < typeParameterBounds.length; i++) {
						source.append(" & ").append(Signature.toString(typeParameterBounds[i]).replace('/', '.')); //$NON-NLS-1$
					}
				}
				source.append("> "); //$NON-NLS-1$
			}
			
			source.append(Signature.toString(Signature.getReturnType(genericSignature)).replace('/', '.')).append(' ').append(method.name()).append('(');
			
			String[] parameterTypes= Signature.getParameterTypes(genericSignature);
			int i= 0;
			if (parameterTypes.length != 0) {
				source.append(Signature.toString(parameterTypes[0]).replace('/', '.')).append(" arg").append(i++); //$NON-NLS-1$
				if (method.isVarargs()) {
					for (int j= 1; j < parameterTypes.length - 1; j++) {
						source.append(',').append(Signature.toString(parameterTypes[j]).replace('/', '.')).append(" arg").append(i++); //$NON-NLS-1$
					}
					String typeName= Signature.toString(parameterTypes[parameterTypes.length - 1]).replace('/', '.');
					source.append(',').append(typeName.substring(0,typeName.length() - 2)).append("...").append(" arg").append(i++); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					for (int j= 1; j < parameterTypes.length; j++) {
						source.append(',').append(Signature.toString(parameterTypes[j]).replace('/', '.')).append(" arg").append(i++); //$NON-NLS-1$
					}
				}
			}
			source.append(')');
		} else {
			source.append(getDotName(method.returnTypeName())).append(' ').append(method.name()).append('(');
			
			List arguments= method.argumentTypeNames();
			int i= 0;
			if (arguments.size() != 0) {
				Iterator iterator= arguments.iterator();
				source.append(getDotName((String) iterator.next())).append(" arg").append(i++); //$NON-NLS-1$
				if (method.isVarargs()) {
					while (iterator.hasNext()) {
						source.append(',');
						String argName = getDotName((String) iterator.next());
						if (!iterator.hasNext()) {
							source.append(argName.substring(0,argName.length() - 2)).append("..."); //$NON-NLS-1$
						} else {
							source.append(argName);
						}
						source.append(" arg").append(i++); //$NON-NLS-1$
					}
				} else {
					while (iterator.hasNext()) {
						source.append(',').append(getDotName((String) iterator.next())).append(" arg").append(i++); //$NON-NLS-1$
					}
				}
			}
			source.append(')');
		}
		
		if (method.isAbstract() || method.isNative()) {
			// No body for abstract and native methods
			source.append(";\n"); //$NON-NLS-1$
		} else {
			source.append('{').append('\n');
			source.append(getReturnStatement(method.returnTypeName()));
			source.append('}').append('\n');
		}

		return source;
	}
	
	private String getReturnStatement(String returnTypeName) {
		String typeName= getSimpleName(returnTypeName);
		if (typeName.charAt(typeName.length() - 1) == ']') {
			return "return null;\n"; //$NON-NLS-1$
		}
		switch (typeName.charAt(0)) {
			case 'v':
				return ""; //$NON-NLS-1$
			case 'b':
				if (typeName.charAt(1) == 'o') {
					return "return false;\n"; //$NON-NLS-1$
				}
			case 's':
			case 'c':
			case 'i':
			case 'l':
			case 'd':
			case 'f':
				return "return 0;\n"; //$NON-NLS-1$
			default:
				return "return null;\n"; //$NON-NLS-1$
		}
	}
	
	private String getDotName(String typeName) {
		return typeName.replace('$', '.');
	}
	
	private boolean isAnonymousTypeName(String typeName) {
		char char0 = getSimpleName(typeName).charAt(0);
		return '0' <= char0 && char0 <= '9';
	}

	private String getSimpleName(String qualifiedName) {
		int pos = qualifiedName.lastIndexOf('$');
		if (pos == -1) {
			pos = qualifiedName.lastIndexOf('.');
		}
		return ((pos == -1)? qualifiedName : qualifiedName.substring(pos + 1));
	}
	
	private String getPackageName(String qualifiedName) {
		int pos = qualifiedName.lastIndexOf('.');
		return ((pos == -1)? null : qualifiedName.substring(0, pos));
	}
	
	private boolean isADirectInnerType(String typeName, String nestedTypeName) {
		String end= nestedTypeName.substring(typeName.length() + 1);
		return end.indexOf('$') == -1;
	}
	
	private boolean isInStaticMethod() {
		return fIsInStaticMethod;
	}
	
	public StringBuffer getSource() {
		return fSource;
	}
	
	public int getCodeSnippetPosition() {
		return fCodeSnippetPosition;
	}
	
	public String getCompilationUnitName() {
		return fCompilationUnitName;
	}
	
	public int getSnippetStart() {
		return fCodeSnippetPosition - 2;
	}
	
	public int getRunMethodStart() {
		return fCodeSnippetPosition - fRunMethodStartOffset;
	}
	
	public int getRunMethodLength() {
		return fRunMethodLength;
	}

}
