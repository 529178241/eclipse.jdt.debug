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
package org.eclipse.jdi.internal.connect;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdi.internal.VirtualMachineManagerImpl;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.ListeningConnector;


public class SocketListeningConnectorImpl extends ConnectorImpl implements ListeningConnector {
	/** Port to which is attached. */
	private int fPort;
	/** Timeout before accept returns. */
	private int fTimeout;
	
	
	/**
	 * Creates new SocketAttachingConnectorImpl.
	 */
	public SocketListeningConnectorImpl(VirtualMachineManagerImpl virtualMachineManager) {
		super(virtualMachineManager);
		
		// Create communication protocol specific transport.
		SocketTransportImpl transport = new SocketTransportImpl();
		setTransport(transport);
	}
	
	/**
	 * @return Returns the default arguments.
	 */	
	public Map defaultArguments() {
		HashMap arguments = new HashMap(1);
		
		// Port
		IntegerArgumentImpl intArg = new IntegerArgumentImpl("port", ConnectMessages.getString("SocketListeningConnectorImpl.Port_number_at_which_to_listen_for_VM_connections_1"), ConnectMessages.getString("SocketListeningConnectorImpl.Port_2"), true, SocketTransportImpl.MIN_PORTNR, SocketTransportImpl.MAX_PORTNR); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		arguments.put(intArg.name(), intArg);
		
		// Timeout
		intArg = new IntegerArgumentImpl("timeout", ConnectMessages.getString("SocketListeningConnectorImpl.Timeout_before_accept_returns_3"), ConnectMessages.getString("SocketListeningConnectorImpl.Timeout_4"), false, 0, Integer.MAX_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		arguments.put(intArg.name(), intArg);
		
		return arguments;
	}
	
	/**
	 * @return Returns a short identifier for the connector.
	 */	
	public String name() {
		return "com.sun.jdi.SocketListen"; //$NON-NLS-1$
	}
	
	/**
	 * @return Returns a human-readable description of this connector and its purpose.
	 */	
	public String description() {
		return ConnectMessages.getString("SocketListeningConnectorImpl.Accepts_socket_connections_initiated_by_other_VMs_5"); //$NON-NLS-1$
	}
	
 	/**
 	 * Retrieves connection arguments.
 	 */
	private void getConnectionArguments(Map connectionArgs) throws IllegalConnectorArgumentsException {
		String attribute = "port"; //$NON-NLS-1$
		try {
		 	fPort = ((Connector.IntegerArgument)connectionArgs.get(attribute)).intValue();
		 	// Note that timeout is not used in SUN's ListeningConnector, but is used by our
		 	// LaunchingConnector.
		 	attribute = "timeout"; //$NON-NLS-1$
		 	fTimeout = ((Connector.IntegerArgument)connectionArgs.get(attribute)).intValue();
		} catch (ClassCastException e) {
			throw new IllegalConnectorArgumentsException(ConnectMessages.getString("SocketListeningConnectorImpl.Connection_argument_is_not_of_the_right_type_6"), attribute); //$NON-NLS-1$
		} catch (NullPointerException e) {
			throw new IllegalConnectorArgumentsException(ConnectMessages.getString("SocketListeningConnectorImpl.Necessary_connection_argument_is_null_7"), attribute); //$NON-NLS-1$
		} catch (NumberFormatException e) {
			throw new IllegalConnectorArgumentsException(ConnectMessages.getString("SocketListeningConnectorImpl.Connection_argument_is_not_a_number_8"), attribute); //$NON-NLS-1$
		}
	}
	
	/**
	 * Listens for one or more connections initiated by target VMs. 
	 * @return Returns the address at which the connector is listening for a connection.
	 */
	public String startListening(Map connectionArgs) throws IOException, IllegalConnectorArgumentsException {
		getConnectionArguments(connectionArgs);
		String result = ConnectMessages.getString("SocketListeningConnectorImpl.ListeningConnector_Socket_Port") + fPort; //$NON-NLS-1$
		try {
			((SocketTransportImpl)fTransport).listen(fPort);
		} catch (IllegalArgumentException e) {
			throw new IllegalConnectorArgumentsException(e.getMessage(), "port"); //$NON-NLS-1$
		}
		return result;
	}
	
	/**
	 * Cancels listening for connections. 
	 */
	public void stopListening(Map connectionArgs) throws IOException {
		((SocketTransportImpl)fTransport).closeListen();
	}
		
	/**
	 * Waits for a target VM to attach to this connector.
	 * @return Returns a connected Virtual Machine.
	 */
	public VirtualMachine accept(Map connectionArgs) throws IOException {
		// TODO TransportTimeoutException ?
		((SocketTransportImpl)fTransport).setAcceptTimeout(fTimeout);
		((SocketTransportImpl)fTransport).accept();
		if (fTransport.isOpen()) {
			return establishedConnection();
		} 
		return null;
	}
	
	/**
	 * @return Returns whether this listening connector supports multiple connections for a single argument map. 
	 */
	public boolean supportsMultipleConnections() {
		return true;
	}
	
	/**
	 * @return Returns port number that is listened to. 
	 */
	public int listeningPort() {
		return ((SocketTransportImpl)fTransport).listeningPort();
	}
}
