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
package org.eclipse.jdt.internal.debug.eval.ast.engine;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.AndAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.AndOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ArrayAllocation;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ArrayInitializerInstruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.AssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Cast;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.CompoundInstruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ConditionalJump;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Constructor;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.DivideAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.DivideOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.EqualEqualOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.GreaterEqualOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.GreaterOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.InstanceOfOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Instruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.InstructionSequence;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Jump;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LeftShiftAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LeftShiftOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LessEqualOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LessOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LocalVariableCreation;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MinusAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MultiplyAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MultiplyOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.NoOp;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.NotOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.OrAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.OrOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PlusAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Pop;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PostfixMinusMinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PostfixPlusPlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PrefixMinusMinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PrefixPlusPlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushArrayLength;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushArrayType;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushBoolean;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushChar;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushClassLiteralValue;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushDouble;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushFieldVariable;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushFloat;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushInt;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushLocalVariable;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushLong;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushNull;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushStaticFieldVariable;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushString;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushThis;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushType;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RemainderAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RemainderOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ReturnInstruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RightShiftAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RightShiftOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.SendMessage;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.SendStaticMessage;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ThrowInstruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.TwiddleOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnaryMinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnaryPlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnsignedRightShiftAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnsignedRightShiftOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.XorAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.XorOperator;

/**
 * The AST instruction compiler generates a sequence
 * of instructions (InstructionSequence) from a
 * DOM AST.
 */
public class ASTInstructionCompiler extends ASTVisitor {

	/**
	 * Represent a break or a continue instruction.
	 * These instructions needs are stored and managed later by their
	 * related statement.
	 */
	class CompleteInstruction {
		Jump fInstruction;
		String fLabel;
		boolean fIsBreak;

		public CompleteInstruction(Jump instruction, String label, boolean isBreak) {
			fInstruction= instruction;
			fLabel= label;
			fIsBreak= isBreak;
		}
	}

	/**
	 * Whether to print debug messages to the console
	 */
	private static boolean VERBOSE = false;

	private InstructionSequence fInstructions;

	/**
	 * The list of pending break and continue instruction.
	 */
	private List fCompleteInstructions;

	private int fStartPosition;

	private boolean fActive;

	private boolean fHasErrors;

	private Stack fStack;

	private int fCounter;


	/**
	 * Create a new AST instruction compiler
	 */
	public ASTInstructionCompiler(int startPosition, String snippet) {
		fStartPosition = startPosition;
		fInstructions = new InstructionSequence(snippet);
		fStack = new Stack();
		fCompleteInstructions= new ArrayList();
	}

	/**
	 * Returns the instruction sequence generated
	 * by this AST instruction compiler
	 */
	public InstructionSequence getInstructions() {
		return fInstructions;
	}

	/**
	 * Returns whether the generated instruction sequence
	 * has errors.
	 * Errors include:
	 * <ol>
	 * <li>AST contains unimplemented operations (features which will be supported,
	 *  but aren't yet)</li>
	 * <li>AST contains unsupported operations (features which are not yet implemented
	 *  and are likely NOT to be implemented)</li>
	 * </ol>
	 */
	public boolean hasErrors() {
		return fHasErrors;
	}

	private void setHasError(boolean value) {
		fHasErrors= value;
	}

	private void addErrorMessage(String message) {
		fInstructions.addError(message);
	}

	private boolean isActive() {
		return fActive;
	}

	private void setActive(boolean active) {
		fActive = active;
	}


	private void push(Instruction i) {
		fStack.push(i);
	}

	private Instruction pop() {
		return (Instruction)fStack.pop();
	}

	private void storeInstruction() {
		Instruction instruction= pop();
		fCounter++;
		if (instruction instanceof CompoundInstruction) {
			((CompoundInstruction)instruction).setEnd(fCounter);
		}
		fInstructions.add(instruction);
		verbose("Add " + instruction.toString()); //$NON-NLS-1$
	}


	/**
	 * Prints the given message to the console if verbose
	 * mode is on.
	 *
	 * @param message the message to display
	 */
	private void verbose(String message) {
		if (VERBOSE) {
			System.out.println(message);
		}
	}

	private String getTypeName(ITypeBinding typeBinding) {
		StringBuffer name;
		if (typeBinding.isArray()) {
			name= new StringBuffer(getTypeName(typeBinding.getElementType()));
			int dimensions= typeBinding.getDimensions();
			for (int i= 0; i < dimensions; i++) {
				name.append("[]"); //$NON-NLS-1$
			}
			return name.toString();
		}
		String typeName= typeBinding.getName();
		int parameters= typeName.indexOf('<');
		if (parameters >= 0) {
			typeName= typeName.substring(0, parameters);
		}
		name= new StringBuffer(typeName);
		IPackageBinding packageBinding= typeBinding.getPackage();
		typeBinding= typeBinding.getDeclaringClass();
		while(typeBinding != null) {
			name.insert(0, '$').insert(0, typeBinding.getName());
			typeBinding= typeBinding.getDeclaringClass();
		}
		if (packageBinding != null && !packageBinding.isUnnamed()) {
			name.insert(0, '.').insert(0, packageBinding.getName());
		}
		return name.toString();
	}

	private String getTypeSignature(ITypeBinding typeBinding) {
		return Signature.createTypeSignature(getTypeName(typeBinding), true).replace('.', '/');
	}

	private boolean isALocalType(ITypeBinding typeBinding) {
		while(typeBinding != null) {
			if (typeBinding.isLocal()) {
				return true;
			}
			typeBinding= typeBinding.getDeclaringClass();
		}
		return false;
	}

	private boolean containsALocalType(IMethodBinding methodBinding) {
		ITypeBinding[] typeBindings= methodBinding.getParameterTypes();
		for (int i= 0, length= typeBindings.length; i < length; i++) {
			if (isALocalType(typeBindings[i])) {
				return true;
			}
		}
		return false;
	}

	private int getEnclosingLevel(ASTNode node, ITypeBinding referenceTypeBinding) {
		ASTNode parent= node;
		do {
			parent= parent.getParent();
		} while (!(parent instanceof TypeDeclaration || parent instanceof AnonymousClassDeclaration));
		ITypeBinding parentBinding;
		if (parent instanceof TypeDeclaration) {
			parentBinding= ((TypeDeclaration)parent).resolveBinding();
		} else {
			parentBinding= ((AnonymousClassDeclaration)parent).resolveBinding();
		}
		if (isInstanceOf(parentBinding, referenceTypeBinding)) {
			return 0;
		}
		return getEnclosingLevel(parent, referenceTypeBinding) + 1;
	}

	private int getSuperLevel(ITypeBinding current, ITypeBinding reference) {
		if (current.equals(reference)) {
			return 0;
		}
		return getSuperLevel(current.getSuperclass(), reference);
	}

	private boolean isInstanceOf(ITypeBinding current, ITypeBinding reference) {
		if (current.equals(reference)) {
			return true;
		}
		
		ITypeBinding[] interfaces= current.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			if (isInstanceOf(interfaces[i], reference)) {
				return true;
			}
		}
		
		ITypeBinding superClass= current.getSuperclass();
		if (superClass != null) {
			return isInstanceOf(current.getSuperclass(), reference);
		}
		return false;
	}

	/**
	 * Return the label associated with the given statement.
	 *
	 * @param statement the statement.
	 * @return the associated label, or <code>null</code> if there is none.
	 */
	private String getLabel(Statement statement) {
		ASTNode parent= statement.getParent();
		if (parent instanceof LabeledStatement) {
			return ((LabeledStatement)parent).getLabel().getIdentifier();
		}
		return null;
	}

	/**
	 * Append a pop instruction in the instruction list if needed.
	 * A pop instruction is added when the expression has a return value,
	 * i.e. all expressions expect method invocation expressions which
	 * have <code>void</code> as return type and variable declaration expression.
	 *
	 * @param expression the expressien to test.
	 */
	private void addPopInstructionIfNeeded(Expression expression) {
		boolean pop= true;

		if (expression instanceof MethodInvocation) {
			IMethodBinding methodBinding= (IMethodBinding)((MethodInvocation)expression).getName().resolveBinding();
			if ("void".equals(methodBinding.getReturnType().getName())) { //$NON-NLS-1$
				pop= false;
			}
		} else if (expression instanceof SuperMethodInvocation) {
			IMethodBinding methodBinding= (IMethodBinding)((SuperMethodInvocation)expression).getName().resolveBinding();
			if ("void".equals(methodBinding.getReturnType().getName())) { //$NON-NLS-1$
				pop= false;
			}
		} else if (expression instanceof VariableDeclarationExpression) {
			pop= false;
		}

		if (pop) {
			push(new Pop());
			storeInstruction();
		}
	}

	/**
	 * End visit methods
	 *
	 * There are two paths to ending a visit to a node:
	 * <ol>
	 * <li>For control statements, the necessary control
	 *  instructions (jump, conditional jump) are inserted
	 *  into the instruction sequence</li>
	 * <li>For other cases, we simply remove the node's
	 *  instruction from the stack and add it to the
	 *  instruction sequence.</li>
	 * </ol>
	 */

	/**
	 * @see ASTVisitor#endVisit(AnonymousClassDeclaration)
	 */
	public void endVisit(AnonymousClassDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ArrayAccess)
	 */
	public void endVisit(ArrayAccess node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ArrayCreation)
	 */
	public void endVisit(ArrayCreation node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ArrayInitializer)
	 */
	public void endVisit(ArrayInitializer node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ArrayType)
	 */
	public void endVisit(ArrayType node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(AssertStatement)
	 */
	public void endVisit(AssertStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(Assignment)
	 */
	public void endVisit(Assignment node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(Block)
	 */
	public void endVisit(Block node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(BooleanLiteral)
	 */
	public void endVisit(BooleanLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(BreakStatement)
	 */
	public void endVisit(BreakStatement node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(CastExpression)
	 */
	public void endVisit(CastExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(CatchClause)
	 */
	public void endVisit(CatchClause node) {

	}

	/**
	 * @see ASTVisitor#endVisit(CharacterLiteral)
	 */
	public void endVisit(CharacterLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ClassInstanceCreation)
	 */
	public void endVisit(ClassInstanceCreation node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(CompilationUnit)
	 */
	public void endVisit(CompilationUnit node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ConditionalExpression)
	 */
	public void endVisit(ConditionalExpression node) {
		if (!isActive() || hasErrors())
			return;

		// Get the instructions
		int ifFalseAddress= fInstructions.getEnd();
		Instruction ifFalse= fInstructions.get(ifFalseAddress);
		int ifTrueAddress= ifFalseAddress - ifFalse.getSize();
		Instruction ifTrue= fInstructions.get(ifTrueAddress);
		int conditionalAddress= ifTrueAddress - ifTrue.getSize();

		// Insert the conditional jump
		ConditionalJump conditionalJump= new ConditionalJump(false);
		fInstructions.insert(conditionalJump, conditionalAddress + 1);

		// Insert the jump
		int jumpAddress= ifTrueAddress + 2;
		Jump jump= new Jump();
		fInstructions.insert(jump, jumpAddress);

		// Set the jump offsets
		conditionalJump.setOffset(ifTrue.getSize() + 1);
		jump.setOffset(ifFalse.getSize() + 1);

		fCounter += 2;
		storeInstruction();

	}

	/**
	 * @see ASTVisitor#endVisit(ConstructorInvocation)
	 */
	public void endVisit(ConstructorInvocation node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ContinueStatement)
	 */
	public void endVisit(ContinueStatement node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(DoStatement)
	 */
	public void endVisit(DoStatement node) {
		if (!isActive() || hasErrors())
			return;

		/* The structure of generated instructions is :
		 *
		 * --
		 * | body
		 * --
		 * --
		 * |condition
		 * --
		 * - jump to the first instruction of the body if the condition is true.
		 *
		 */

		String label= getLabel(node);

		// get adress of each part
		int conditionAddress= fInstructions.getEnd();
		Instruction condition= fInstructions.getInstruction(conditionAddress);
		int bodyAddress= conditionAddress - condition.getSize();
		Instruction body= fInstructions.getInstruction(bodyAddress);

		// add the conditionnalJump
		ConditionalJump conditionalJump= new ConditionalJump(true);
		fInstructions.add(conditionalJump);
		fCounter++;

		// set jump offsets
		conditionalJump.setOffset(-(condition.getSize() + body.getSize() + 1));

		// for each pending break or continue instruction which are related to
		// this loop, set the offset of the corresponding jump.
		for (Iterator iter= fCompleteInstructions.iterator(); iter.hasNext();) {
			CompleteInstruction instruction= (CompleteInstruction) iter.next();
			if (instruction.fLabel == null || instruction.fLabel.equals(label)) {
				iter.remove();
				Jump jumpInstruction= instruction.fInstruction;
				int instructionAddress= fInstructions.indexOf(jumpInstruction);
				if (instruction.fIsBreak) {
					// jump to the instruction after the last jump
					jumpInstruction.setOffset((conditionAddress - instructionAddress) + 1);
				} else {
					// jump to the first instruction of the condition
					jumpInstruction.setOffset(bodyAddress - instructionAddress);
				}
			}
		}

		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(EmptyStatement)
	 */
	public void endVisit(EmptyStatement node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ExpressionStatement)
	 */
	public void endVisit(ExpressionStatement node) {
		if (!isActive() || hasErrors())
			return;

		addPopInstructionIfNeeded(node.getExpression());
	}

	/**
	 * @see ASTVisitor#endVisit(FieldAccess)
	 */
	public void endVisit(FieldAccess node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(FieldDeclaration)
	 */
	public void endVisit(FieldDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ForStatement)
	 */
	public void endVisit(ForStatement node) {
		if (!isActive() || hasErrors())
			return;

		/* The structure of generated instructions is :
		 *
		 * --
		 * |initialization
		 * --
		 * --
		 * |condition
		 * --
		 * - jump to the instruction after the last jump if the condition is false.
		 * --
		 * | body
		 * --
		 * --
		 * | updaters
		 * --
		 * - jump to the first instruction of the condition.
		 *
		 */

		String label= getLabel(node);
		boolean hasCondition= node.getExpression() != null;

		// get adress of each part
		int updatersAddress= fInstructions.getEnd();
		Instruction updaters= fInstructions.getInstruction(updatersAddress);
		int bodyAddress= updatersAddress - updaters.getSize();
		Instruction body= fInstructions.getInstruction(bodyAddress);

		int conditionAddress;
		Instruction condition;

		if (hasCondition) {
			conditionAddress= bodyAddress - body.getSize();
			condition= fInstructions.getInstruction(conditionAddress);
		} else {
			conditionAddress= 0;
			condition= null;
		}

		// add jump
		Jump jump= new Jump();
		fInstructions.add(jump);
		fCounter++;

		if (hasCondition) {
			// add conditionnal jump
			ConditionalJump condJump= new ConditionalJump(false);
			fInstructions.insert(condJump, conditionAddress + 1);
			bodyAddress++;
			updatersAddress++;
			fCounter++;
			// conditionnal set jump offset
			condJump.setOffset(body.getSize() + updaters.getSize() + 1);
		}

		// set jump offset
		jump.setOffset(-((hasCondition ? condition.getSize() : 0) + body.getSize() + updaters.getSize() + 2));

		// for each pending break or continue instruction which are related to
		// this loop, set the offset of the corresponding jump.
		for (Iterator iter= fCompleteInstructions.iterator(); iter.hasNext();) {
			CompleteInstruction instruction= (CompleteInstruction) iter.next();
			if (instruction.fLabel == null || instruction.fLabel.equals(label)) {
				iter.remove();
				Jump jumpInstruction= instruction.fInstruction;
				int instructionAddress= fInstructions.indexOf(jumpInstruction);
				if (instruction.fIsBreak) {
					// jump to the instruction after the last jump
					jumpInstruction.setOffset((updatersAddress - instructionAddress) + 1);
				} else {
					// jump to the first instruction of the condition
					jumpInstruction.setOffset(bodyAddress - instructionAddress);
				}
			}
		}

		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(IfStatement)
	 */
	public void endVisit(IfStatement node) {
		if (!isActive() || hasErrors())
			return;

		boolean hasElseStatement= node.getElseStatement() != null;

		// Get the instructions

		int ifFalseAddress= 0;
		Instruction ifFalse= null;
		int ifTrueAddress= 0;
		Instruction ifTrue= null;

		if (hasElseStatement) {
			ifFalseAddress= fInstructions.getEnd();
			ifFalse= fInstructions.get(ifFalseAddress);
			ifTrueAddress= ifFalseAddress - ifFalse.getSize();
			ifTrue= fInstructions.get(ifTrueAddress);
		} else {
			ifTrueAddress= fInstructions.getEnd();
			ifTrue= fInstructions.get(ifTrueAddress);
		}

		int conditionalAddress= ifTrueAddress - ifTrue.getSize();

		// Insert the conditional jump
		ConditionalJump conditionalJump= new ConditionalJump(false);
		fInstructions.insert(conditionalJump, conditionalAddress + 1);
		// Set the jump offset
		conditionalJump.setOffset(ifTrue.getSize() + ((hasElseStatement)? 1 : 0));
		fCounter++;

		if (hasElseStatement) {
			// Insert the jump
			int jumpAddress= ifTrueAddress + 2;
			Jump jump= new Jump();
			fInstructions.insert(jump, jumpAddress);
			// Set the jump offset
			jump.setOffset(ifFalse.getSize() + 1);
			fCounter++;
		}

		storeInstruction();

	}

	/**
	 * @see ASTVisitor#endVisit(ImportDeclaration)
	 */
	public void endVisit(ImportDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(InfixExpression)
	 */
	public void endVisit(InfixExpression node) {
	}

	/**
	 * @see ASTVisitor#endVisit(Initializer)
	 */
	public void endVisit(Initializer node) {

	}

	/**
	 * @see ASTVisitor#endVisit(InstanceofExpression)
	 */
	public void endVisit(InstanceofExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(Javadoc)
	 */
	public void endVisit(Javadoc node) {

	}

	/**
	 * @see ASTVisitor#endVisit(LabeledStatement)
	 */
	public void endVisit(LabeledStatement node) {
		if (!isActive() || hasErrors())
			return;

		String label= node.getLabel().getIdentifier();

		// for each pending continue instruction which are related to
		// this statement, set the offset of the corresponding jump.
		for (Iterator iter= fCompleteInstructions.iterator(); iter.hasNext();) {
			CompleteInstruction instruction= (CompleteInstruction) iter.next();
			if (instruction.fLabel != null && instruction.fLabel.equals(label)) {
				iter.remove();
				Jump jumpInstruction= instruction.fInstruction;
				int instructionAddress= fInstructions.indexOf(jumpInstruction);
				if (instruction.fIsBreak) {
					// jump to the instruction after the statement
					jumpInstruction.setOffset(fInstructions.getEnd() - instructionAddress);
				}
			}
		}
	}

	/**
	 * @see ASTVisitor#endVisit(MethodDeclaration)
	 */
	public void endVisit(MethodDeclaration node) {
		setActive(false);
	}

	/**
	 * @see ASTVisitor#endVisit(MethodInvocation)
	 */
	public void endVisit(MethodInvocation node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(NullLiteral)
	 */
	public void endVisit(NullLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(NumberLiteral)
	 */
	public void endVisit(NumberLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(PackageDeclaration)
	 */
	public void endVisit(PackageDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SimpleType)
	 */
	public void endVisit(ParameterizedType node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ParenthesizedExpression)
	 */
	public void endVisit(ParenthesizedExpression node) {

	}

	/**
	 * @see ASTVisitor#endVisit(PostfixExpression)
	 */
	public void endVisit(PostfixExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(PrefixExpression)
	 */
	public void endVisit(PrefixExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(PrimitiveType)
	 */
	public void endVisit(PrimitiveType node) {
	}

	/**
	 * @see ASTVisitor#endVisit(QualifiedName)
	 */
	public void endVisit(QualifiedName node) {
	}

	/**
	 * @see ASTVisitor#endVisit(SimpleType)
	 */
	public void endVisit(QualifiedType node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ReturnStatement)
	 */
	public void endVisit(ReturnStatement node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(SimpleName)
	 */
	public void endVisit(SimpleName node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(SimpleType)
	 */
	public void endVisit(SimpleType node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(SingleVariableDeclaration)
	 */
	public void endVisit(SingleVariableDeclaration node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(StringLiteral)
	 */
	public void endVisit(StringLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(SuperConstructorInvocation)
	 */
	public void endVisit(SuperConstructorInvocation node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SuperFieldAccess)
	 */
	public void endVisit(SuperFieldAccess node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(SuperMethodInvocation)
	 */
	public void endVisit(SuperMethodInvocation node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(SwitchCase)
	 */
	public void endVisit(SwitchCase node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SwitchStatement)
	 */
	public void endVisit(SwitchStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SynchronizedStatement)
	 */
	public void endVisit(SynchronizedStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ThisExpression)
	 */
	public void endVisit(ThisExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ThrowStatement)
	 */
	public void endVisit(ThrowStatement node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(TryStatement)
	 */
	public void endVisit(TryStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(TypeDeclaration)
	 */
	public void endVisit(TypeDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(TypeDeclarationStatement)
	 */
	public void endVisit(TypeDeclarationStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(TypeLiteral)
	 */
	public void endVisit(TypeLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(VariableDeclarationExpression)
	 */
	public void endVisit(VariableDeclarationExpression node) {

	}

	/**
	 * @see ASTVisitor#endVisit(VariableDeclarationFragment)
	 */
	public void endVisit(VariableDeclarationFragment node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(VariableDeclarationStatement)
	 */
	public void endVisit(VariableDeclarationStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(WhileStatement)
	 */
	public void endVisit(WhileStatement node) {
		if (!isActive() || hasErrors())
			return;

		/* The structure of generated instructions is :
		 *
		 * --
		 * |condition
		 * --
		 * - jump to the instruction after the last jump if the condition is false.
		 * --
		 * | body
		 * --
		 * - jump to the first instruction of the condition.
		 *
		 */

		String label= getLabel(node);

		// get adress of each part
		int bodyAddress= fInstructions.getEnd();
		Instruction body= fInstructions.getInstruction(bodyAddress);
		int conditionAddress= bodyAddress - body.getSize();
		Instruction condition= fInstructions.getInstruction(conditionAddress);

		// add the conditionnalJump
		ConditionalJump conditionalJump= new ConditionalJump(false);
		fInstructions.insert(conditionalJump, conditionAddress + 1);

		// add the jump
		Jump jump= new Jump();
		fInstructions.add(jump);

		// set jump offsets
		conditionalJump.setOffset(body.getSize() + 1);
		jump.setOffset(-(condition.getSize() + body.getSize() + 2));

		// for each pending break or continue instruction which are related to
		// this loop, set the offset of the corresponding jump.
		for (Iterator iter= fCompleteInstructions.iterator(); iter.hasNext();) {
			CompleteInstruction instruction= (CompleteInstruction) iter.next();
			if (instruction.fLabel == null || instruction.fLabel.equals(label)) {
				iter.remove();
				Jump jumpInstruction= instruction.fInstruction;
				int instructionAddress= fInstructions.indexOf(jumpInstruction);
				if (instruction.fIsBreak) {
					// jump to the instruction after the last jump
					jumpInstruction.setOffset((bodyAddress - instructionAddress) + 2);
				} else {
					// jump to the first instruction of the condition
					jumpInstruction.setOffset((conditionAddress - condition.getSize()) - instructionAddress);
				}
			}
		}

		fCounter+= 2;
		storeInstruction();
	}

	/*
	 * Visit methods
	 *
	 * There are two variations of node visiting:
	 * <ol>
	 * <li>Push the instruction corresponding to the node
	 *  onto the stack and return <code>true</code> to visit
	 *  the children of the node.</li>
	 * <li>Push the instruction corresponding to the node
	 *  onto the stack and visit the children of the node
	 *  manually (return <code>false</code> to avoid the
	 *  default child visiting implementation).</li>
	 * </ol>
	 */

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeDeclaration)
	 */
	public boolean visit(AnnotationTypeDeclaration node) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration)
	 */
	public boolean visit(AnnotationTypeMemberDeclaration node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		if (!isActive()) {
			return true;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Anonymous_type_declaration_cannot_be_used_in_an_evaluation_expression_2")); //$NON-NLS-1$
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		if (!isActive()) {
			return false;
		}

		push(new org.eclipse.jdt.internal.debug.eval.ast.instructions.ArrayAccess(fCounter));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		if (!isActive()) {
			return false;
		}

		ArrayType arrayType= node.getType();

		if (isALocalType(arrayType.resolveBinding().getElementType())) {
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Local_type_array_instance_creation_cannot_be_used_in_an_evaluation_expression_29")); //$NON-NLS-1$
			setHasError(true);
			return true;
		}

		push(new ArrayAllocation(arrayType.getDimensions(), node.dimensions().size(), node.getInitializer() != null, fCounter));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		if (!isActive()) {
			return false;
		}

		ITypeBinding typeBinding= node.resolveTypeBinding();
		int dimension= typeBinding.getDimensions();
		String signature= getTypeSignature(typeBinding.getElementType());

		push(new ArrayInitializerInstruction(signature, node.expressions().size(), dimension, fCounter));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		if (!isActive()) {
			return false;
		}
		ITypeBinding arrayTypeBinding= node.resolveBinding();
		int dimension= arrayTypeBinding.getDimensions();
		String signature= getTypeSignature(arrayTypeBinding.getElementType());

		push(new PushArrayType(signature, dimension, fCounter));

		return false;
	}

	/**
	 * @see ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Assert_statement_cannot_be_used_in_an_evaluation_expression_3")); //$NON-NLS-1$
		return true;
	}

	/**
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		if (!isActive()) {
			return false;
		}
		int variableTypeId = getTypeId(node.getLeftHandSide());
		int valueTypeId = getTypeId(node.getRightHandSide());

		String opToken = node.getOperator().toString();
		int opTokenLength = opToken.length();
		char char0 = opToken.charAt(0);
		char char2 = '\0';
		if (opTokenLength > 2) {
			char2 = opToken.charAt(2);
		}

		boolean unrecognized = false;

		switch (char0) {
			case '=': // equal
				push(new AssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '+': // plus equal
				push(new PlusAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '-': // minus equal
				push(new MinusAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '*': // multiply equal
				push(new MultiplyAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '/': // divide equal
				push(new DivideAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '%': // remainder equal
				push(new RemainderAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '^': // xor equal
				push(new XorAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '|': // or equal
				push(new OrAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '&': // and equal
				push(new AndAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '<': // left shift equal
				push(new LeftShiftAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '>': // right shift equal or unsigned right shift equal
				switch (char2) {
					case '=': // right shift equal
						push(new RightShiftAssignmentOperator(variableTypeId, valueTypeId, fCounter));
						break;
					case '>': // unsigned right shift equal
						push(new UnsignedRightShiftAssignmentOperator(variableTypeId, valueTypeId, fCounter));
						break;
					default:
						unrecognized = true;
						break;
				}
				break;
			default:
				unrecognized = true;
				break;
		}

		if (unrecognized) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Unrecognized_assignment_operator____4") + opToken); //$NON-NLS-1$
		}

		return true;
	}

	/**
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		int start= node.getStartPosition();
		if (start == fStartPosition || start == (fStartPosition + 1)) {
			setActive(true);
		}
		if (!isActive()) {
			return true;
		}

		push(new NoOp(fCounter));

		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.BlockComment)
	 */
	public boolean visit(BlockComment node) {
		return false;
	}
	
	/**
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		if (!isActive()) {
			return false;
		}

		push(new PushBoolean(node.booleanValue()));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		if (!isActive()) {
			return false;
		}
		// create the equivalent jump instruction in the instruction
		// and add an element in the list of pending break and continue
		// instructions
		Jump instruction= new Jump();
		SimpleName labelName= node.getLabel();
		String label= null;
		if (labelName != null) {
			label= labelName.getIdentifier();
		}
		push(instruction);
		fCompleteInstructions.add(new CompleteInstruction(instruction, label, true));

		return false;
	}

	/**
	 * @see ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		if (!isActive()) {
			return false;
		}

		Type type= node.getType();
		int typeId= getTypeId(type);
		ITypeBinding typeBinding= type.resolveBinding();

		String baseTypeSignature;
		int dimension= typeBinding.getDimensions();

		if (typeBinding.isArray()) {
			typeBinding= typeBinding.getElementType();
		}
		
		baseTypeSignature= getTypeName(typeBinding);

		push(new Cast(typeId, baseTypeSignature, dimension, fCounter));

		node.getExpression().accept(this);

		return false;
	}

	/**
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Catch_clause_cannot_be_used_in_an_evaluation_expression_6")); //$NON-NLS-1$
		return true;
	}

	/**
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		if (!isActive()) {
			return false;
		}

		push(new PushChar(node.charValue()));

		return true;
	}

	/**
	 * return false, visit expression, type name & arguments, don't visit body declaration
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (!isActive()) {
			return true;
		}

		if (node.getAnonymousClassDeclaration() != null) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Anonymous_type_declaration_cannot_be_used_in_an_evaluation_expression_7")); //$NON-NLS-1$
		}

		IMethodBinding methodBinding= node.resolveConstructorBinding();
		ITypeBinding typeBinding= methodBinding.getDeclaringClass();
		ITypeBinding enclosingTypeBinding= typeBinding.getDeclaringClass();

		boolean isInstanceMemberType= typeBinding.isMember() && ! Modifier.isStatic(typeBinding.getModifiers());

		if (isALocalType(typeBinding)) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Constructor_of_a_local_type_cannot_be_used_in_an_evaluation_expression_8")); //$NON-NLS-1$
		}

		if (containsALocalType(methodBinding)) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Constructor_which_contains_a_local_type_as_parameter_cannot_be_used_in_an_evaluation_expression_30")); //$NON-NLS-1$
		}


		if (hasErrors()) {
			return true;
		}

		int argCount= methodBinding.getParameterTypes().length;

		String enclosingTypeSignature= null;
		if (isInstanceMemberType) {
			enclosingTypeSignature= getTypeSignature(enclosingTypeBinding);
			argCount++;
		}

		String signature= getMethodSignature(methodBinding, enclosingTypeSignature).replace('.','/');

		push(new Constructor(signature, argCount, fCounter));

		push(new PushType(getTypeName(typeBinding)));
		storeInstruction();

		if (isInstanceMemberType) {
			Expression optionalExpression= node.getExpression();
			if (optionalExpression != null) {
				optionalExpression.accept(this);
			} else {
				// for a non-static inner class, check if we are not in a static context (method)
				ASTNode parent= node;
				do {
					parent= parent.getParent();
				} while (! (parent instanceof MethodDeclaration));
				if (Modifier.isStatic(((MethodDeclaration)parent).getModifiers())) {
					setHasError(true);
					addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Must_explicitly_qualify_the_allocation_with_an_instance_of_the_enclosing_type_33")); //$NON-NLS-1$
					return true;
				}

				push(new PushThis(getEnclosingLevel(node, enclosingTypeBinding)));
				storeInstruction();
			}
		}

		Iterator iterator= node.arguments().iterator();
		while (iterator.hasNext()) {
			((Expression) iterator.next()).accept(this);
		}

		return false;
	}

	/**
	 * @see ASTVisitor#visit(CompilationUnit)
	 */
	public boolean visit(CompilationUnit node) {
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		if (!isActive()) {
			return true;
		}

		push(new NoOp(fCounter));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.this_constructor_invocation_cannot_be_used_in_an_evaluation_expression_9")); //$NON-NLS-1$
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		if (!isActive()) {
			return false;
		}
		// create the equivalent jump instruction in the instruction
		// and add an element in the list of pending break and continue
		// instructions
		Jump instruction= new Jump();
		SimpleName labelName= node.getLabel();
		String label= null;
		if (labelName != null) {
			label= labelName.getIdentifier();
		}
		push(instruction);
		fCompleteInstructions.add(new CompleteInstruction(instruction, label, false));

		return false;
	}

	/**
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		if (!isActive()) {
			return false;
		}

		push(new NoOp(fCounter));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		if (!isActive()) {
			return false;
		}
		push(new NoOp(fCounter));
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.EnumConstantDeclaration)
	 */
	public boolean visit(EnumConstantDeclaration node) {
		if (!isActive()) {
			return true;
		}

		// nothing to do, we shouldn't hit this node
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.EnumDeclaration)
	 */
	public boolean visit(EnumDeclaration node) {
		if (!isActive()) {
			return true;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.0")); //$NON-NLS-1$
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		return true;
	}

	/**
	 * return false, visit expression, don't visit name
	 *
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		if (!isActive()) {
			return false;
		}

		SimpleName fieldName= node.getName();
		IVariableBinding fieldBinding= (IVariableBinding) fieldName.resolveBinding();
		ITypeBinding declaringTypeBinding= fieldBinding.getDeclaringClass();
		Expression expression = node.getExpression();
		String fieldId = fieldName.getIdentifier();

		if (Modifier.isStatic(fieldBinding.getModifiers())) {
			push(new PushStaticFieldVariable(fieldId, getTypeName(declaringTypeBinding), fCounter));
			expression.accept(this);
			push(new Pop());
			storeInstruction();
		} else {
			if (declaringTypeBinding == null) { // it is a field without declaring type => it is the special length array field
				push(new PushArrayLength(fCounter));
			} else {
				if (isALocalType(declaringTypeBinding)) {
					setHasError(true);
					addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Qualified_local_type_field_access_cannot_be_used_in_an_evaluation_expression_31")); //$NON-NLS-1$
					return false;
				}
				push(new PushFieldVariable(fieldId, getTypeSignature(declaringTypeBinding), fCounter));
			}
			expression.accept(this);
		}

		return false;
	}

	/**
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ForStatement)
	 * return <code>false</code>, don't use the standart accept order.
	 * order used for visite children :
	 * initializers, condition, body, updaters
	 */
	public boolean visit(ForStatement node) {
		if (!isActive()) {
			return false;
		}

		push(new NoOp(fCounter));

		push(new NoOp(fCounter));
		for (Iterator iter= node.initializers().iterator(); iter.hasNext();) {
			Expression expr= (Expression) iter.next();
			expr.accept(this);
			addPopInstructionIfNeeded(expr);
		}
		storeInstruction();

		Expression condition= node.getExpression();
		if (condition != null) {
			condition.accept(this);
		}

		node.getBody().accept(this);

		push(new NoOp(fCounter));
		for (Iterator iter= node.updaters().iterator(); iter.hasNext();) {
			Expression expr= (Expression) iter.next();
			expr.accept(this);
			addPopInstructionIfNeeded(expr);
		}
		storeInstruction();

		return false;
	}

	/**
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		if (!isActive()) {
			return false;
		}

		push(new NoOp(fCounter));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		return false;
	}

	/**
	 * return <code>false</code>, don't use the standart accept order.
	 *
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		if (!isActive()) {
			return false;
		}

		String opToken = node.getOperator().toString();
		int opTokenLength = opToken.length();
		char char0 = opToken.charAt(0);
		char char1 = '\0';
		char char2 = '\0';
		if (opTokenLength > 1) {
			char1 = opToken.charAt(1);
			if (opTokenLength > 2) {
				char2 = opToken.charAt(2);
			}
		}

		List extendedOperands = node.extendedOperands();

		int operatorNumber=extendedOperands.size() + 1;

		int[][] types = new int[operatorNumber][3];

		Iterator iterator = extendedOperands.iterator();

		int leftTypeId = getTypeId(node.getLeftOperand());
		int rightTypeId = getTypeId(node.getRightOperand());
		int resultTypeId = Instruction.getBinaryPromotionType(leftTypeId, rightTypeId);

		types[0][0] = resultTypeId;
		types[0][1] = leftTypeId;
		types[0][2] = rightTypeId;

		for (int i = 1; i < operatorNumber; i++) {
			Expression operand = (Expression) iterator.next();
			leftTypeId = resultTypeId;
			rightTypeId = getTypeId(operand);
			resultTypeId = Instruction.getBinaryPromotionType(leftTypeId, rightTypeId);
			types[i][0] = resultTypeId;
			types[i][1] = leftTypeId;
			types[i][2] = rightTypeId;
		}

		boolean unrecognized= false;

		switch (char0) {
			case '*': // multiply
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new MultiplyOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '/': // divide
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new DivideOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '%': // remainder
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new RemainderOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '+': // plus
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new PlusOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '-': // minus
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new MinusOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '<': // left shift or less or less equal
				switch (char1) {
					case '\0': // less
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new LessOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					case '<': // left shift
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new LeftShiftOperator(Instruction.getUnaryPromotionType(types[i][1]), types[i][1], types[i][2], fCounter));
						}
						break;
					case '=': // less equal
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new LessEqualOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '>': // right shift or unsigned right shift or greater or greater equal
				switch (char1) {
					case '\0': // greater
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new GreaterOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					case '>': // right shift or unsigned right shift
						switch (char2) {
							case '\0': // right shift
								for (int i = operatorNumber - 1; i >= 0; i--) {
									push(new RightShiftOperator(Instruction.getUnaryPromotionType(types[i][1]), types[i][1], types[i][2], fCounter));
								}
								break;
							case '>': // unsigned right shift
								for (int i = operatorNumber - 1; i >= 0; i--) {
									push(new UnsignedRightShiftOperator(Instruction.getUnaryPromotionType(types[i][1]), types[i][1], types[i][2], fCounter));
								}
								break;
						}
						break;
					case '=': // greater equal
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new GreaterEqualOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '=': // equal equal
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new EqualEqualOperator(types[i][1], types[i][2], true, fCounter));
				}
				break;
			case '!': // not equal
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new EqualEqualOperator(types[i][1], types[i][2], false, fCounter));
				}
				break;
			case '^': // xor
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new XorOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '|': // or or or or
				switch (char1) {
					case '\0': // or
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new OrOperator(types[i][0], types[i][1], types[i][2], fCounter));
						}
						break;
					case '|': // or or
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new NoOp(fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '&': // and or and and
				switch (char1) {
					case '\0': // and
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new AndOperator(types[i][0], types[i][1], types[i][2], fCounter));
						}
						break;
					case '&': // and and
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new NoOp(fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			default:
				unrecognized= true;
				break;
		}

		if (unrecognized) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Unrecognized_infix_operator____13") + opToken); //$NON-NLS-1$
		}

		if (hasErrors()) {
			return true;
		}

		iterator = extendedOperands.iterator();

		if ((char0 == '&' && char1 == '&') || (char0 == '|' && char1 == '|')) { // and and operator

			boolean isOrOr= char0 == '|';

			ConditionalJump[] conditionalJumps= new ConditionalJump[operatorNumber];
			int[] conditionalJumpAddresses = new int[operatorNumber];

			node.getLeftOperand().accept(this);

			ConditionalJump conditionalJump= new ConditionalJump(isOrOr);
			conditionalJumps[0]= conditionalJump;
			conditionalJumpAddresses[0] = fCounter;
			push(conditionalJump);
			storeInstruction();

			node.getRightOperand().accept(this);

			for (int i= 1; i < operatorNumber; i ++) {
				conditionalJump= new ConditionalJump(isOrOr);
				conditionalJumps[i]= conditionalJump;
				conditionalJumpAddresses[i] = fCounter;
				push(conditionalJump);
				storeInstruction();
				((Expression) iterator.next()).accept(this);
			}

			Jump jump = new Jump();
			jump.setOffset(1);
			push(jump);
			storeInstruction();

			for (int i= 0; i < operatorNumber; i ++) {
				conditionalJumps[i].setOffset(fCounter - conditionalJumpAddresses[i] - 1);
			}

			push(new PushBoolean(isOrOr));
			storeInstruction();

			// store the noop
			storeInstruction();

		} else { // other operatos

			node.getLeftOperand().accept(this);
			node.getRightOperand().accept(this);

			storeInstruction();
			for (int i= 1; i < operatorNumber; i ++) {
				((Expression) iterator.next()).accept(this);
				storeInstruction();
			}
		}



		return false;
	}

	/**
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		return true;
	}

	/**
	 * @see ASTVisitor#visit(InstanceofExpression)
	 */
	public boolean visit(InstanceofExpression node) {
		if (!isActive()) {
			return false;
		}
		push(new InstanceOfOperator(fCounter));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(Javadoc node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(LabeledStatement)
	 * return <code>false</code>, don't use the standart accept order.
	 */
	public boolean visit(LabeledStatement node) {
		node.getBody().accept(this);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.LineComment)
	 */
	public boolean visit(LineComment node) {
		return false;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MarkerAnnotation)
	 */
	public boolean visit(MarkerAnnotation node) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MemberRef)
	 */
	public boolean visit(MemberRef node) {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MemberValuePair)
	 */
	public boolean visit(MemberValuePair node) {
		return false;
	}
	
	/**
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		int start= node.getStartPosition();
		int end= start + node.getLength();
		if (start < fStartPosition && end > fStartPosition) {
			return true;
		}
		return false;
	}

	/**
	 * return false, don't visit name, visit expression & arguments
	 *
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		if (!isActive()) {
			return false;
		}

		IMethodBinding methodBinding= (IMethodBinding) node.getName().resolveBinding();

		if (containsALocalType(methodBinding)) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Method_which_contains_a_local_type_as_parameter_cannot_be_used_in_an_evaluation_expression_32")); //$NON-NLS-1$
		}

		if (hasErrors()) {
			return true;
		}

		int argCount= methodBinding.getParameterTypes().length;
		String selector= methodBinding.getName();

		String signature= getMethodSignature(methodBinding, null).replace('.','/');

		boolean isStatic= Flags.isStatic(methodBinding.getModifiers());
		Expression expression= node.getExpression();

		if (isStatic) {
			String typeName= getTypeName(methodBinding.getDeclaringClass());
			push(new SendStaticMessage(typeName, selector, signature, argCount, fCounter));
			if (expression != null) {
				node.getExpression().accept(this);
				push(new Pop());
				storeInstruction();
			}
		} else {
			push(new SendMessage(selector, signature, argCount, null, fCounter));
			if (expression == null) {
				push(new PushThis(getEnclosingLevel(node, methodBinding.getDeclaringClass())));
				storeInstruction();
			} else {
				node.getExpression().accept(this);
			}
		}

		Iterator iterator= node.arguments().iterator();
		while (iterator.hasNext()) {
			((Expression) iterator.next()).accept(this);
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodRef)
	 */
	public boolean visit(MethodRef node) {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodRefParameter)
	 */
	public boolean visit(MethodRefParameter node) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.Modifier)
	 */
	public boolean visit(Modifier node) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.NormalAnnotation)
	 */
	public boolean visit(NormalAnnotation node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		if (!isActive()) {
			return false;
		}

		push(new PushNull());

		return true;
	}

	/**
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		if (!isActive()) {
			return false;
		}

		int literalType= getTypeId(node);
		String token= node.getToken();
		int tokenLastCharOffset= token.length() - 1;
		char lastChar= token.charAt(tokenLastCharOffset);
		String subToken= token.substring(0, tokenLastCharOffset);


		switch (literalType) {
			case Instruction.T_int:
				push(new PushInt(parseIntValue(token)));
				break;
			case Instruction.T_long:
				push(new PushLong(parseLongValue(subToken)));
				break;
			case Instruction.T_float:
				push(new PushFloat(Float.parseFloat(subToken)));
				break;
			case Instruction.T_double:
				if (lastChar == 'D' || lastChar == 'd') {
					push(new PushDouble(Double.parseDouble(subToken)));
				} else {
					push(new PushDouble(Double.parseDouble(token)));
				}
				break;
		}

		return true;
	}

	/**
	 * Method parseIntValue.
	 * @param token
	 */
	private int parseIntValue(String token) {
		int tokenLength= token.length();
		if (tokenLength < 10) {
			// Integer.decode can handle tokens with less than 18 digits
			return Integer.decode(token).intValue();
		} 
		switch (getBase(token)) {
			case 8:
				return (Integer.decode(token.substring(0, tokenLength - 1)).intValue() << 3) | Integer.decode("0" + token.charAt(tokenLength - 1)).intValue(); //$NON-NLS-1$
			case 10:
				return Integer.decode(token).intValue();
			case 16:
				return (Integer.decode(token.substring(0, tokenLength - 1)).intValue() << 4) | Integer.decode("0x" + token.charAt(tokenLength - 1)).intValue(); //$NON-NLS-1$
			default:
				// getBase(String) only returns 8, 10, or 16. This code is unreachable
				return 0;
		}
	}


	/**
	 * Method parseLongValue.
	 * @param token
	 */
	private long parseLongValue(String token) {
		int tokenLength= token.length();
		if (tokenLength < 18) {
			// Long.decode can handle tokens with less than 10 digits
			return Long.decode(token).longValue();
		} 
		switch (getBase(token)) {
			case 8:
				return (Long.decode(token.substring(0, tokenLength - 1)).longValue() << 3) | Long.decode("0" + token.charAt(tokenLength - 1)).longValue(); //$NON-NLS-1$
			case 10:
				return Long.decode(token).longValue();
			case 16:
				return (Long.decode(token.substring(0, tokenLength - 1)).longValue() << 4) | Long.decode("0x" + token.charAt(tokenLength - 1)).longValue(); //$NON-NLS-1$
			default:
				// getBase(String) only returns 8, 10, or 16. This code is unreachable
				return 0;
		}
	}

	/**
	 * Returns the numeric base for the given token
	 * according to the Java specification. Returns
	 * 8, 10, or 16.
	 */
	private int getBase(String token) {
		if (token.charAt(0) == '0') {
			if (token.charAt(1) == 'x') {
				return 16; // "0x" prefix: Hexadecimal
			} 
			return 8; // "0" prefix: Octal
		} 
		return 10; // No prefix: Decimal
	}

	/**
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.ParameterizedType)
	 */
	public boolean visit(ParameterizedType node) {
		if (!isActive()) {
			return false;
		}
		ITypeBinding typeBinding  = node.resolveBinding();
		push(new PushType(getTypeName(typeBinding)));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		if (!isActive()) {
			return false;
		}
		return true;
	}

	/**
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		if (!isActive()) {
			return false;
		}

		int expressionTypeId = getTypeId(node.getOperand());

		String opToken = node.getOperator().toString();
		char char0 = opToken.charAt(0);

		switch (char0) {
			case '+': // plus plus or unary plus
				push(new PostfixPlusPlusOperator(expressionTypeId, fCounter));
				break;
			case '-': // minus minus or unary minus
				push(new PostfixMinusMinusOperator(expressionTypeId, fCounter));
				break;
			default:
				setHasError(true);
				addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.unrecognized_postfix_operator____15") + opToken); //$NON-NLS-1$
				break;
		}

		return true;
	}

	/**
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		if (!isActive()) {
			return false;
		}

		int expressionTypeId = getTypeId(node.getOperand());

		String opToken = node.getOperator().toString();
		int opTokenLength = opToken.length();
		char char0 = opToken.charAt(0);
		char char1 = '\0';
		if (opTokenLength > 1) {
			char1 = opToken.charAt(1);
		}

		boolean unrecognized = false;

		switch (char0) {
			case '+': // plus plus or unary plus
				switch (char1) {
					case '\0': // unary plus
						push(new UnaryPlusOperator(expressionTypeId, fCounter));
						break;
					case '+': // plus plus
						push(new PrefixPlusPlusOperator(expressionTypeId, fCounter));
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '-': // minus minus or unary minus
				switch (char1) {
					case '\0': // unary minus
						push(new UnaryMinusOperator(expressionTypeId, fCounter));
					break;
					case '-': // minus minus
						push(new PrefixMinusMinusOperator(expressionTypeId, fCounter));
					break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '~': // twiddle
				push(new TwiddleOperator(expressionTypeId, fCounter));
				break;
			case '!': // not
				push(new NotOperator(expressionTypeId, fCounter));
				break;
			default:
				unrecognized= true;
				break;
		}


		if (unrecognized) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.unrecognized_prefix_operator____16") + opToken); //$NON-NLS-1$
		}

		return true;
	}

	/**
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		if (!isActive()) {
			return false;
		}

		return true;
	}

	/**
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		if (!isActive()) {
			return false;
		}

		if (hasErrors()) {
			return true;
		}

		IBinding binding = node.resolveBinding();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				node.getName().accept(this);
				break;
			case IBinding.VARIABLE:
				SimpleName fieldName= node.getName();
				IVariableBinding fieldBinding= (IVariableBinding) fieldName.resolveBinding();
				ITypeBinding declaringTypeBinding= fieldBinding.getDeclaringClass();
				String fieldId = fieldName.getIdentifier();

				if (Modifier.isStatic(fieldBinding.getModifiers())) {
					push(new PushStaticFieldVariable(fieldId, getTypeName(declaringTypeBinding), fCounter));
				} else {
					if (declaringTypeBinding == null) {
						push(new PushArrayLength(fCounter));
					} else {
						push(new PushFieldVariable(fieldId, getTypeSignature(declaringTypeBinding), fCounter));
					}
					node.getQualifier().accept(this);
				}
				storeInstruction();
				break;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.QualifiedType)
	 */
	public boolean visit(QualifiedType node) {
		if (!isActive()) {
			return false;
		}
		ITypeBinding typeBinding  = node.resolveBinding();
		push(new PushType(getTypeName(typeBinding)));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		if (!isActive()) {
			return false;
		}
		push(new ReturnInstruction(fCounter));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		if (!isActive()) {
			return false;
		}

		if (hasErrors()) {
			return true;
		}

		IBinding binding = node.resolveBinding();

		String variableId = node.getIdentifier();
		if (binding == null) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.binding_null_for__17") + variableId); //$NON-NLS-1$
			return true;
		}

		switch (binding.getKind()) {
			case IBinding.TYPE:
				ITypeBinding typeBinding= (ITypeBinding) binding;
				push(new PushType(getTypeName(typeBinding)));
				break;
			case IBinding.VARIABLE:
				IVariableBinding variableBinding= (IVariableBinding) binding;
				ITypeBinding declaringTypeBinding= variableBinding.getDeclaringClass();
				if (variableBinding.isField()) {
					if (Modifier.isStatic(variableBinding.getModifiers())) {
						push(new PushStaticFieldVariable(variableId, getTypeName(declaringTypeBinding), fCounter));
					} else {
						if (isALocalType(declaringTypeBinding)) {
							setHasError(true);
							addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.36")); //$NON-NLS-1$
							return false;
						}
						push(new PushFieldVariable(variableId, getTypeSignature(declaringTypeBinding), fCounter));
						push(new PushThis(getEnclosingLevel(node, declaringTypeBinding)));
						storeInstruction();
					}
				} else {
					push(new PushLocalVariable(variableId));
				}
				break;
		}
		return true;
	}

	/**
	 * return false, don't visit child
	 *
	 * @see ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		if (!isActive()) {
			return false;
		}

		ITypeBinding typeBinding  = node.resolveBinding();
		push(new PushType(getTypeName(typeBinding)));

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SingleMemberAnnotation)
	 */
	public boolean visit(SingleMemberAnnotation node) {
		return false;
	}
	
	/**
	 * @see ASTVisitor#visit(SingleVariableDeclaration)
	 * return <code>false</code>, don't use the standart accept order.
	 */
	public boolean visit(SingleVariableDeclaration node) {
		if (!isActive()) {
			return false;
		}

		Expression initializer= node.getInitializer();
		boolean hasInitializer= initializer != null;

		ITypeBinding typeBinding= node.getType().resolveBinding();
		int typeDimension= typeBinding.getDimensions();
		if (typeDimension != 0) {
			typeBinding= typeBinding.getElementType();
		}

		push(new LocalVariableCreation(node.getName().getIdentifier(), getTypeSignature(typeBinding), typeDimension, typeBinding.isPrimitive(), hasInitializer, fCounter));
		if (hasInitializer) {
			initializer.accept(this);
		}

		return false;
	}

	/**
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		if (!isActive()) {
			return false;
		}

		push(new PushString(node.getLiteralValue()));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.super_constructor_invocation_cannot_be_used_in_an_evaluation_expression_19")); //$NON-NLS-1$
		return false;
	}

	/**
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		if (!isActive()) {
			return false;
		}

		SimpleName fieldName= node.getName();
		IVariableBinding fieldBinding= (IVariableBinding) fieldName.resolveBinding();
		ITypeBinding declaringTypeBinding= fieldBinding.getDeclaringClass();
		String fieldId = fieldName.getIdentifier();

		if (Modifier.isStatic(fieldBinding.getModifiers())) {
			push(new PushStaticFieldVariable(fieldId, getTypeName(declaringTypeBinding), fCounter));
		} else {
			Name qualifier = node.getQualifier();
			int superLevel= 1;
			int enclosingLevel= 0;
			if (qualifier != null) {
				superLevel= getSuperLevel(qualifier.resolveTypeBinding(), declaringTypeBinding);
				enclosingLevel= getEnclosingLevel(node, (ITypeBinding)qualifier.resolveBinding());
			}
			push(new PushFieldVariable(fieldId, superLevel, fCounter));
			push(new PushThis(enclosingLevel));
			storeInstruction();
		}

		return false;
	}

	/**
	 * return false, don't visit name, visit arguments
	 *
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (!isActive()) {
			return false;
		}

		IMethodBinding methodBinding = (IMethodBinding) node.getName().resolveBinding();

		if (containsALocalType(methodBinding)) {
			setHasError(true);
			addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Method_which_contains_a_local_type_as_parameter_cannot_be_used_in_an_evaluation_expression_32")); //$NON-NLS-1$
		}

		if (hasErrors()) {
			return true;
		}

		ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
		int argCount = parameterTypes.length;
		String selector = methodBinding.getName();
		String signature = getMethodSignature(methodBinding, null);

		Name qualifier= node.getQualifier();
		if (Modifier.isStatic(methodBinding.getModifiers())) {
			push(new SendStaticMessage(getTypeName(methodBinding.getDeclaringClass()), selector, signature, argCount, fCounter));
		} else {
			push(new SendMessage(selector, signature, argCount, getTypeSignature(methodBinding.getDeclaringClass()), fCounter));
			int enclosingLevel= 0;
			if (qualifier != null) {
				enclosingLevel= getEnclosingLevel(node, (ITypeBinding)qualifier.resolveBinding());
			}
			push(new PushThis(enclosingLevel));
			storeInstruction();
		}

		Iterator iterator = node.arguments().iterator();
		while (iterator.hasNext()) {
			((Expression) iterator.next()).accept(this);
		}

		return false;
	}

	/**
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Switch_case_cannot_be_used_in_an_evaluation_expression_20")); //$NON-NLS-1$
		return true;
	}

	/**
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Switch_statement_cannot_be_used_in_an_evaluation_expression_21")); //$NON-NLS-1$
		return true;
	}

	/**
	 * @see ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		if (!isActive()) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TagElement)
	 */
	public boolean visit(TagElement node) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TextElement)
	 */
	public boolean visit(TextElement node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		if (!isActive()) {
			return false;
		}

		Name qualifier= node.getQualifier();
		int enclosingLevel= 0;
		if (qualifier != null) {
			enclosingLevel= getEnclosingLevel(node, (ITypeBinding)qualifier.resolveBinding());
		}
		push(new PushThis(enclosingLevel));

		return false;
	}

	/**
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		if (!isActive()) {
			return false;
		}
		push(new ThrowInstruction(fCounter));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Try_statement_cannot_be_used_in_an_evaluation_expression_23")); //$NON-NLS-1$
		return true;
	}

	/**
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (!isActive()) {
			return true;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Type_declaration_cannot_be_used_in_an_evaluation_expression_24")); //$NON-NLS-1$
		return false;
	}

	/**
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		if (!isActive()) {
			return true;
		}
		setHasError(true);
		addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Type_declaration_statement_cannot_be_used_in_an_evaluation_expression_25")); //$NON-NLS-1$
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.TypeParameter)
	 */
	public boolean visit(TypeParameter node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		if (!isActive()) {
			return false;
		}

		push(new PushClassLiteralValue(fCounter));

		return true;
	}

	/**
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 * return <code>false</code>, don't use the standart accept order.
	 */
	public boolean visit(VariableDeclarationExpression node) {
		if (!isActive()) {
			return false;
		}
		for (Iterator iter= node.fragments().iterator(); iter.hasNext();) {
			((VariableDeclarationFragment) iter.next()).accept(this);
		}
		return false;
	}

	/**
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 * return <code>false</code>, don't use the standart accept order.
	 */
	public boolean visit(VariableDeclarationFragment node) {
		if (!isActive()) {
			return false;
		}
		// get the type of the variable
		ITypeBinding typeBinding;
		ASTNode parent= node.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.VARIABLE_DECLARATION_EXPRESSION:
				typeBinding= ((VariableDeclarationExpression)parent).getType().resolveBinding();
				break;
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				typeBinding= ((VariableDeclarationStatement)parent).getType().resolveBinding();
				break;
			default:
				setHasError(true);
				addErrorMessage(EvaluationEngineMessages.getString("ASTInstructionCompiler.Error_in_type_declaration_statement")); //$NON-NLS-1$
				return false;
		}
		int typeDimension= typeBinding.getDimensions();
		if (typeDimension != 0) {
			typeBinding= typeBinding.getElementType();
		}

		Expression initializer= node.getInitializer();
		boolean hasInitializer= initializer != null;

		push(new LocalVariableCreation(node.getName().getIdentifier(), getTypeSignature(typeBinding), typeDimension, typeBinding.isPrimitive(), hasInitializer, fCounter));

		if (hasInitializer) {
			initializer.accept(this);
		}

		return false;
	}

	/**
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 * return <code>false</code>, don't use the standart accept order.
	 */
	public boolean visit(VariableDeclarationStatement node) {
		if (!isActive()) {
			return false;
		}
		for (Iterator iter= node.fragments().iterator(); iter.hasNext();) {
			((VariableDeclarationFragment) iter.next()).accept(this);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.WildcardType)
	 */
	public boolean visit(WildcardType node) {
		// we shouldn't have to do anything
		return false;
	}

	/**
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		if (!isActive()) {
			return false;
		}

		push(new NoOp(fCounter));
		return true;
	}

	//--------------------------

	private int getTypeId(Expression expression) {
		ITypeBinding typeBinding = expression.resolveTypeBinding();
		String typeName = typeBinding.getName();
		if (typeBinding.isPrimitive()) {
			return getPrimitiveTypeId(typeName);
		} else if ("String".equals(typeName) && "java.lang".equals(typeBinding.getPackage().getName())){ //$NON-NLS-1$ //$NON-NLS-2$
			return Instruction.T_String;
		} else {
			return Instruction.T_Object;
		}
	}

	private int getTypeId(Type type) {
		if (type.isPrimitiveType()) {
			return getPrimitiveTypeId(((PrimitiveType)type).getPrimitiveTypeCode().toString());
		} else if (type.isSimpleType()) {
			SimpleType simpleType = (SimpleType) type;
			if ("java.lang.String".equals(simpleType.getName())){ //$NON-NLS-1$
				return Instruction.T_String;
			} 
			return Instruction.T_Object;
		} else if (type.isArrayType()) {
			return Instruction.T_Object;
		} else {
			return Instruction.T_undefined;
		}

	}

	private String getMethodSignature(IMethodBinding methodBinding, String enclosingTypeSignature) {
		ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
		int i;
		int argCount;
		String[] parameterSignatures;
		if (enclosingTypeSignature == null) {
			i= 0;
			argCount= parameterTypes.length;
			parameterSignatures= new String[argCount];
		} else {
			i= 1;
			argCount= parameterTypes.length + 1;
			parameterSignatures= new String[argCount];
			parameterSignatures[0]= enclosingTypeSignature;
		}
		for (; i < argCount; i++) {
			parameterSignatures[i]= getTypeSignature(parameterTypes[i]);
		}
		String signature= Signature.createMethodSignature(parameterSignatures, getTypeSignature(methodBinding.getReturnType()));
		return signature;
	}

	private int getPrimitiveTypeId(String typeName) {
		switch (typeName.charAt(0)) {
			case 'b': // byte or boolean
				switch (typeName.charAt(1)) {
					case 'o': // boolean;
						return Instruction.T_boolean;
					case 'y': // byte
						return Instruction.T_byte;
				}
				break;
			case 'c': // char
				return Instruction.T_char;
			case 'd': // double
				return Instruction.T_double;
			case 'f': // float
				return Instruction.T_float;
			case 'i': // int
				return Instruction.T_int;
			case 'l': // long
				return Instruction.T_long;
			case 'n':
				return Instruction.T_null;
			case 's': // short
				return Instruction.T_short;
			case 'v': // void
				return Instruction.T_void;
		}
		return Instruction.T_undefined;
	}
}
