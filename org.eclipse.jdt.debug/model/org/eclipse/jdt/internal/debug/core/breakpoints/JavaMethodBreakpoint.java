package org.eclipse.jdt.internal.debug.core.breakpoints;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;

import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;

public class JavaMethodBreakpoint extends JavaLineBreakpoint implements IJavaMethodBreakpoint {
	
	private static final String JAVA_METHOD_BREAKPOINT = "org.eclipse.jdt.debug.javaMethodBreakpointMarker"; //$NON-NLS-1$
	
	/**
	 * Breakpoint attribute storing the name of the method
	 * in which a breakpoint is contained.
	 * (value <code>"org.eclipse.jdt.debug.core.methodName"</code>). This attribute is a <code>String</code>.
	 */
	private static final String METHOD_NAME = "org.eclipse.jdt.debug.core.methodName"; //$NON-NLS-1$	
	
	/**
	 * Breakpoint attribute storing the signature of the method
	 * in which a breakpoint is contained.
	 * (value <code>"org.eclipse.jdt.debug.core.methodSignature"</code>). This attribute is a <code>String</code>.
	 */
	private static final String METHOD_SIGNATURE = "org.eclipse.jdt.debug.core.methodSignature"; //$NON-NLS-1$	
	
	/**
	 * Breakpoint attribute storing whether this breakpoint
	 * is an entry breakpoint.
	 * (value <code>"org.eclipse.jdt.debug.core.entry"</code>).
	 * This attribute is a <code>boolean</code>.
	 */
	private static final String ENTRY = "org.eclipse.jdt.debug.core.entry"; //$NON-NLS-1$	

	/**
	 * Breakpoint attribute storing whether this breakpoint
	 * is an exit breakpoint.
	 * (value <code>"org.eclipse.jdt.debug.core.exit"</code>).
	 * This attribute is a <code>boolean</code>.
	 */
	private static final String EXIT = "org.eclipse.jdt.debug.core.exit"; //$NON-NLS-1$	
		
	/**
	 * Breakpoint attribute storing whether this breakpoint
	 * only applies to native methods.
	 * (value <code>"org.eclipse.jdt.debug.core.native"</code>).
	 * This attribute is a <code>boolean</code>.
	 */
	private static final String NATIVE = "org.eclipse.jdt.debug.core.native"; //$NON-NLS-1$
	
	/**
	 * Method request property storing the type in which the
	 * request is installed
	 */
	private static final String TYPE = "org.eclipse.jdt.debug.core.type"; //$NON-NLS-1$
			
	/**
	 * Cache of method name attribute
	 */
	private String fMethodName = null;
	
	/**
	 * Cache of method signature attribute
	 */
	private String fMethodSignature = null;
	
	/**
	 * Flag indicating that this breakpoint last suspended execution
	 * due to a method entry
	 */
	protected static final Integer ENTRY_EVENT= new Integer(0);	
	
	/**
	 * Flag indicating that this breakpoint last suspended execution
	 * due to a method exit
	 */
	protected static final Integer EXIT_EVENT= new Integer(1);		
	
	/**
	 * Maps each debug target that is suspended for this breakpiont to reason that 
	 * this breakpoint suspended it. Reasons include:
	 * <ol>
	 * <li>Method entry (value <code>ENTRY_EVENT</code>)</li>
	 * <li>Methdo exit (value <code>EXIT_EVENT</code>)</li>
	 * </ol>
	 */
	private HashMap fLastEventTypes= new HashMap(10); // $NON-NLS-1$
	
	/**
	 * Constructs a new unconfigured method breakpoint
	 */
	public JavaMethodBreakpoint() {
	}
	
	/**
	 * @see JDIDebugModel#createMethodBreakpoint(IResource, String, String, String, boolean, boolean, boolean, int, boolean, Map)
	 */
	public JavaMethodBreakpoint(final IResource resource, final String typeName, final String methodName, final String methodSignature, final boolean entry, final boolean exit, final boolean nativeOnly, final int lineNumber, final int charStart, final int charEnd, final int hitCount, final boolean register, final Map attributes) throws CoreException {
		IWorkspaceRunnable wr= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				// create the marker
				setMarker(resource.createMarker(JAVA_METHOD_BREAKPOINT));
				
				// add attributes
				addLineBreakpointAttributes(attributes, getModelIdentifier(), true, lineNumber, charStart, charEnd);
				addMethodNameAndSignature(attributes, methodName, methodSignature);
				addTypeNameAndHitCount(attributes, typeName, hitCount);
				attributes.put(ENTRY, new Boolean(entry));
				attributes.put(EXIT, new Boolean(exit));
				attributes.put(NATIVE, new Boolean(nativeOnly));
				
				//set attributes
				ensureMarker().setAttributes(attributes);
				
				register(register);
			}

		};
		run(wr);
	}
	
	/**
	 * @see JavaBreakpoint#createRequest(JDIDebugTarget, ReferenceType)
	 * 
	 * Creates and installs an entry and exit watchpoint request
	 * in the given reference type, configuring the requests as appropriate
	 * for this breakpoint. The requests are then enabled based on whether
	 * this breakpoint is an entry breakpoint, exit breakpoint, or
	 * both. Finally, the requests are registered with the given target.
	 */	
	protected boolean createRequest(JDIDebugTarget target, ReferenceType type) throws CoreException {
		MethodEntryRequest entryRequest= createMethodEntryRequest(target, type);
		MethodExitRequest exitRequest= createMethodExitRequest(target, type);
		
		registerRequest(entryRequest, target);
		registerRequest(exitRequest, target);
		return true;
	}	
	
	/**
	 * @see JavaBreakpoint#recreateRequest(EventRequest, JDIDebugTarget)
	 */
	protected EventRequest recreateRequest(EventRequest request, JDIDebugTarget target)	throws CoreException {
		ReferenceType type= (ReferenceType) request.getProperty(TYPE);
		EventRequest newRequest= null;
		try {
			if (request instanceof MethodEntryRequest) {
				newRequest= createMethodEntryRequest(target, type);
			} else {
				newRequest= createMethodExitRequest(target, type);
			}
		} catch (VMDisconnectedException e) {
			if (!target.isAvailable()) {
				return request;
			}
			JDIDebugPlugin.logError(e);
		} catch (RuntimeException e) {
			JDIDebugPlugin.logError(e);
		}
		return newRequest;
	}
	
	
	/**
	 * Returns a new method entry request for this breakpoint's
	 * criteria
	 * 
	 * @param the target in which to create the request
	 * @param type the type on which to create the request
	 * @return method entry request
	 * @exception CoreException if an exception occurs accessing
	 *  this breakpoint's underlying marker
	 */
	protected MethodEntryRequest createMethodEntryRequest(JDIDebugTarget target, ReferenceType type) throws CoreException {	
		return (MethodEntryRequest)createMethodRequest(target, type, true);
	}
	
	/**
	 * Returns a new method exit request for this breakpoint's
	 * criteria
	 * 
	 * @param target the target in which to create the request
	 * @param type the type on which to create the request
	 * @return method exit request
	 * @exception CoreException if an exception occurs accessing
	 *  this breakpoint's underlying marker
	 */
	protected MethodExitRequest createMethodExitRequest(JDIDebugTarget target, ReferenceType type) throws CoreException {	
		return (MethodExitRequest)createMethodRequest(target, type, false);
	}
	
	/**
	 * @see JavaMethodBreakpoint#createMethodEntryRequest(JDIDebugTarget, ReferenceType)
	 *  or JavaMethodBreakpoint#createMethodExitRequest(JDIDebugTarget, ReferenceType)
	 *
	 * Returns a MethodEntryRequest if entry is <code>true</code>,
	 *  a MethodExitRequest if entry is <code>false</code>
	 */
	private EventRequest createMethodRequest(JDIDebugTarget target, ReferenceType type, boolean entry) throws CoreException {
		EventRequest request = null;
		try {
			if (entry) {
				request= target.getEventRequestManager().createMethodEntryRequest();
				((MethodEntryRequest)request).addClassFilter(type);
			} else {
				request= target.getEventRequestManager().createMethodExitRequest();
				((MethodExitRequest)request).addClassFilter(type);
			}
			configureRequest(request, target, type);
		} catch (VMDisconnectedException e) {
			if (!target.isAvailable()) {
				return null;
			}
			JDIDebugPlugin.logError(e);
		} catch (RuntimeException e) {
			JDIDebugPlugin.logError(e);
		}			
		return request;
	}
	
	/**
	 * @see JavaBreakpoint#setRequestThreadFilter(EventRequest)
	 */
	protected void setRequestThreadFilter(EventRequest request, ThreadReference thread) {
		if (request instanceof MethodEntryRequest) {
			((MethodEntryRequest)request).addThreadFilter(thread);
		} else {
			((MethodExitRequest)request).addThreadFilter(thread);
		}
	}

	/**
	 * Adds the type in which this method is installed to the request as a property.
	 * This type corresponds to the class filter that is placed on the request.
	 */
	protected void configureRequest(EventRequest request, JDIDebugTarget target, ReferenceType type) throws CoreException {
		request.putProperty(TYPE, type);
		configureRequest(request, target);
	}
	
	/**
	 * Configure the given request's hit count. Since method
	 * entry/exit requests do not support hit counts, we simulate
	 * a hit count by manually updating a counter stored on the
	 * request.
	 */
	protected void configureRequestHitCount(EventRequest request) throws CoreException {
		int hitCount = getHitCount();
		if (hitCount > 0) {
			request.putProperty(HIT_COUNT, new Integer(hitCount));
		}	
	}

	/**
	 * Update the hit count associated with this method entry breakpoint
	 * in the given request
	 */	
	protected EventRequest updateHitCount(EventRequest request, JDIDebugTarget target) throws CoreException {
		if (hasHitCountChanged(request) || (isExpired(request) && isEnabled())) {
			try {
				int hitCount = getHitCount();
				Integer hc = null;
				if (hitCount > 0) {
					hc = new Integer(hitCount);
				}
				request.putProperty(HIT_COUNT, hc);
			} catch (VMDisconnectedException e) {
				if (!target.isAvailable()) {
					return request;
				}
				JDIDebugPlugin.logError(e);
				return request;
			} catch (RuntimeException e) {
				JDIDebugPlugin.logError(e);
			}
		}
		return request;
	}
	
	/**
	 * @see JavaBreakpoint#updateEnabledState(EventRequest)
	 */
	protected void updateEnabledState(EventRequest request) throws CoreException  {
		boolean enabled = isEnabled();
		if (request instanceof MethodEntryRequest) {
			if (isEntry()) {
				if (enabled != request.isEnabled()) {
					internalUpdateEnabeldState(request, enabled);
				}
			} else {
				if (request.isEnabled()) {
					internalUpdateEnabeldState(request, false);
				}
			}
		}
		if (request instanceof MethodExitRequest) {
			if (isExit()) {
				if (enabled != request.isEnabled()) {
					internalUpdateEnabeldState(request, enabled);
				}
			} else {
				if (request.isEnabled()) {
					internalUpdateEnabeldState(request, false);
				}
			}
		}
	}	
	
	/**
	 * Set the enabled state of the given request to the given
	 * value
	 */
	protected void internalUpdateEnabeldState(EventRequest request, boolean enabled) {
		// change the enabled state
		try {
			// if the request has expired, do not enable/disable.
			// Requests that have expired cannot be deleted.
			if (!isExpired(request)) {
				request.setEnabled(enabled);
			}
		} catch (VMDisconnectedException e) {
		} catch (RuntimeException e) {
			JDIDebugPlugin.logError(e);
		}
	}
		
	/**
	 * Adds the method name and signature attributes to the
	 * given attribute map, and intializes the local cache
	 * of method name and signature.
	 */
	private void addMethodNameAndSignature(Map attributes, String methodName, String methodSignature) {
		if (methodName != null) {		
			attributes.put(METHOD_NAME, methodName);
		}
		if (methodSignature != null) {
			attributes.put(METHOD_SIGNATURE, methodSignature);
		}
		fMethodName= methodName;
		fMethodSignature= methodSignature;
	}

	/**
	 * @see IJavaMethodBreakpoint#isEntrySuspend(IDebugTarget)
	 */
	public boolean isEntrySuspend(IDebugTarget target) {
		Integer lastEventType= (Integer) fLastEventTypes.get(target);
		if (lastEventType == null) {
			return false;
		}
		return lastEventType.equals(ENTRY_EVENT);
	}	
	
	/**
	 * @see JavaBreakpoint#handleBreakpointEvent(Event, JDIDebugTarget)
	 */
	public boolean handleBreakpointEvent(Event event, JDIDebugTarget target) {
		if (event instanceof MethodEntryEvent) {
			MethodEntryEvent entryEvent= (MethodEntryEvent) event;
			fLastEventTypes.put(target, ENTRY_EVENT);
			return handleMethodEvent(entryEvent, entryEvent.method(), target);
		}
		if (event instanceof MethodExitEvent) {
			MethodExitEvent exitEvent= (MethodExitEvent) event;
			fLastEventTypes.put(target, EXIT_EVENT);
			return handleMethodEvent(exitEvent, exitEvent.method(), target);
		}
		return true;
	}
	
	/**
	 * Method entry/exit events are fired each time any method is invoked in a class
	 * in which a method entry/exit breakpoint has been installed.
	 * When a method entry/exit event is received by this breakpoint, ensure that
	 * the event has been fired by a method invocation that this breakpoint
	 * is interested in. If it is not, do nothing.
	 */
	protected boolean handleMethodEvent(LocatableEvent event, Method method, JDIDebugTarget target) {
		try {
			if (getMethodName() != null) {
				if (!method.name().equals(getMethodName())) {
					return true;
				}
			}
			
			if (getMethodSignature() != null) {
				if (!method.signature().equals(getMethodSignature())) {
					return true;
				}
			}
			
			if (isNativeOnly()) {
				if (!method.isNative()) {
					return true;
				}
			}
			
			// simulate hit count
			Integer count = (Integer)event.request().getProperty(HIT_COUNT);
			if (count != null) {
				return handleHitCount(event, count, target);
			} else {
				// no hit count - suspend
				return doSuspend(event.thread(), target);
			}
			
		} catch (CoreException e) {
			JDIDebugPlugin.logError(e);
		}
		return true;
	}
	
	/**
	 * Suspend the given thread in the given target, and returns
	 * whether the thread should be suspended.
	 */
	private boolean doSuspend(ThreadReference threadRef, JDIDebugTarget target) {
		JDIThread thread= target.findThread(threadRef);	
		if (thread == null) {
			return true;
		} else {
			return suspend(thread);				
		}		
	}
	
	/**
	 * Method breakpoints simulate hit count.
	 * When a method event is received, decrement the hit count
	 * property on the request and suspend if the hit count reaches 0.
	 */
	private boolean handleHitCount(LocatableEvent event, Integer count, JDIDebugTarget target) {	
	// decrement count and suspend if 0
		int hitCount = count.intValue();
		if (hitCount > 0) {
			hitCount--;
			count = new Integer(hitCount);
			event.request().putProperty(HIT_COUNT, count);
			if (hitCount == 0) {
				// the count has reached 0, breakpoint hit
				boolean resume = doSuspend(event.thread(), target);
				try {
					// make a note that we auto-disabled the breakpoint
					// order is important here...see methodEntryChanged
					setExpired(true);
					setEnabled(false);
				} catch (CoreException e) {
					JDIDebugPlugin.logError(e);
				}
				return resume;
			}  else {
				// count still > 0, keep running
				return true;		
			}
		} else {
			// hit count expired, keep running
			return true;
		}
	}
	
	/**
	 * @see IJavaMethodEntryBreakpoint#getMethodName()		
	 */
	public String getMethodName() {
		return fMethodName;
	}
	
	/**
	 * @see IJavaMethodEntryBreakpoint#getMethodSignature()		
	 */
	public String getMethodSignature() {
		return fMethodSignature;
	}		
		
	/**
	 * @see IJavaMethodBreakpoint#isEntry()
	 */
	public boolean isEntry() throws CoreException {
		return ensureMarker().getAttribute(ENTRY, false);
	}

	/**
	 * @see IJavaMethodBreakpoint#isExit()
	 */
	public boolean isExit() throws CoreException {
		return ensureMarker().getAttribute(EXIT, false);
	}

	/**
	 * @see IJavaMethodBreakpoint#isNative()
	 */
	public boolean isNativeOnly() throws CoreException {
		return ensureMarker().getAttribute(NATIVE, false);
	}

	/**
	 * @see IJavaMethodBreakpoint#setEntry(boolean)
	 */
	public void setEntry(boolean entry) throws CoreException {
		if (isEntry() != entry) {
			ensureMarker().setAttribute(ENTRY, entry);
			if (entry && !isEnabled()) {
				setEnabled(true);
			} else if (!(entry || isExit())) {
				setEnabled(false);
			}			
		}
	}

	/**
	 * @see IJavaMethodBreakpoint#setExit(boolean)
	 */
	public void setExit(boolean exit) throws CoreException {
		if (isExit() != exit) {
			ensureMarker().setAttribute(EXIT, exit);
			if (exit && !isEnabled()) {
				setEnabled(true);
			} else if (!(exit || isEntry())) {
				setEnabled(false);
			}			
		}		
	}

	/**
	 * @see IJavaMethodBreakpoint#setNativeOnly(boolean)
	 */
	public void setNativeOnly(boolean nativeOnly) throws CoreException {
		if (isNativeOnly() != nativeOnly) {
			ensureMarker().setAttribute(NATIVE, nativeOnly);
		}
	}
		
	/**
	 * Initialize cache of attributes
	 * 
	 * @see IBreakpoint#setMarker(IMarker)
	 */
	public void setMarker(IMarker marker) throws CoreException {
		super.setMarker(marker);
		fMethodName = marker.getAttribute(METHOD_NAME, null);
		fMethodSignature = marker.getAttribute(METHOD_SIGNATURE, null);
	}	
	
	/**
	 * @see IBreakpoint#setEnabled(boolean)
	 * 
	 * If this breakpoing is not entry or exit enabled,
	 * set the default (entry)
	 */
	public void setEnabled(boolean enabled) throws CoreException {
		super.setEnabled(enabled);
		if (isEnabled()) {
			if (!(isEntry() || isExit())) {
				setEntry(true);
			}
		}
	}
}