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
package org.eclipse.jdt.internal.debug.ui.monitors;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.views.AbstractDebugEventHandlerView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.internal.debug.core.model.JDIDebugElement;
import org.eclipse.jdt.internal.debug.ui.IJavaDebugHelpContextIds;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.Page;

/**
 * Handles the different viewers: Thread, Monitor and Deadlock
 */
public class MonitorsView extends AbstractDebugEventHandlerView implements ISelectionListener {

	public static final int VIEW_ID_THREAD = 1;
	public static final int VIEW_ID_MONITOR = 2;
	public static final int VIEW_ID_DEADLOCK = 3;
	
	private int fViewId = VIEW_ID_MONITOR;
	
	private Viewer fDeadLocksViewer;
	private Viewer fMonitorsViewer;
	
	private boolean fMonitorInformationAvailable= true;
	
	private boolean fValidSelection= false;
	
	private IJavaDebugTarget fLastSelectedTarget;
	
	/**
	 * A page in this view's page book that contains this
	 * view's viewer.
	 */
	class MonitorsViewerPage extends Page {
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			Viewer viewer = createMonitorsViewer(parent);
			setMonitorsViewer(viewer);			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.IPage#getControl()
		 */
		public Control getControl() {
			return getMonitorsViewer().getControl();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.IPage#setFocus()
		 */
		public void setFocus() {
			Viewer viewer= getMonitorsViewer();
			if (viewer != null) {
				Control c = viewer.getControl();
				if (!c.isFocusControl()) {
					c.setFocus();
				}
			}
		}
	}
	
	/**
	 * A page in this view's page book that contains this
	 * view's viewer.
	 */
	class DeadLocksViewerPage extends Page {
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		public void createControl(Composite parent) {
			Viewer viewer = createDeadLocksViewer(parent);
			setDeadLocksViewer(viewer);			
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.IPage#getControl()
		 */
		public Control getControl() {
			return getDeadLocksViewer().getControl();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.part.IPage#setFocus()
		 */
		public void setFocus() {
			Viewer viewer= getDeadLocksViewer();
			if (viewer != null) {
				Control c = viewer.getControl();
				if (!c.isFocusControl()) {
					c.setFocus();
				}
			}
		}
	}
	
	public MonitorsView(){		
		setEventHandler(new MonitorsDebugEventHandler(this));
	}

	/**
	 * Sets the current view.
	 * Must be called after creation of the viewpart.
	 */	
	public void setViewId(int viewerIndex) {
		fViewId = viewerIndex;
		refreshCurrentViewer(fMonitorInformationAvailable, true);
	}
	
	/**
	 * Returns the current view id.
	 */	
	public int getViewId() {
		return fViewId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractDebugView#createViewer(org.eclipse.swt.widgets.Composite)
	 */
	protected Viewer createViewer(Composite parent) {
		StructuredViewer threadViewer = new TreeViewer(parent, SWT.MULTI);
		threadViewer.setContentProvider(new ThreadsViewContentProvider());
		threadViewer.setLabelProvider(new MonitorModelPresentation());
		threadViewer.setInput(MonitorManager.getDefault());	
		return threadViewer;
	}

	protected Viewer createMonitorsViewer(Composite parent) {
		StructuredViewer monitorsViewer = new TreeViewer(parent, SWT.MULTI);
		monitorsViewer.setContentProvider(new MonitorsViewContentProvider());
		monitorsViewer.setLabelProvider(new MonitorModelPresentation());
		monitorsViewer.setInput(MonitorManager.getDefault());	
		return monitorsViewer;
	}

	protected Viewer createDeadLocksViewer(Composite parent) {
		StructuredViewer deadLocksViewer = new TreeViewer(parent, SWT.MULTI) {
			//when refreshing, sets the color of the threads caught in a deadlock to red
			public void refresh() {
				getControl().setRedraw(false);
				super.refresh();
				
				Item[] children = getChildren(getControl());
				if (children != null) {
					//to be changed
					Color c= DebugUIPlugin.getPreferenceColor(IDebugPreferenceConstants.CHANGED_VARIABLE_COLOR);
					for (int i = 0; i < children.length; i++) {
						updateColor((TreeItem)children[i], c, 0);
					}
				}
				getControl().setRedraw(true);
			}
			
			//goes down the tree, but only changes the color of the items caught in a deadlock
			public void updateColor(TreeItem item, Color c, int count) {
				Object data= item.getData();
				if (data instanceof DeadLocksViewContentProvider.ContentThreadWrapper) {
					if(((DeadLocksViewContentProvider.ContentThreadWrapper)data).caughtInADeadLock) {
						item.setForeground(c);
					}
				}
				
				TreeItem[] children = item.getItems();
				for (int i = 0; i < children.length; i++) {
					updateColor(children[i], c, (count+1));
				}
			}
		};
		deadLocksViewer.setContentProvider(new DeadLocksViewContentProvider());
		deadLocksViewer.setLabelProvider(new MonitorModelPresentation());
		deadLocksViewer.setInput(MonitorManager.getDefault());	
		return deadLocksViewer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		// create the message page
		
		DeadLocksViewerPage deadLocksPage = new DeadLocksViewerPage();
		deadLocksPage.createControl(getPageBook());
		initPage(deadLocksPage);
		
		MonitorsViewerPage monitorsViewerPage = new MonitorsViewerPage();
		monitorsViewerPage.createControl(getPageBook());
		initPage(monitorsViewerPage);

		createContextMenu(getDeadLocksViewer().getControl());
		createContextMenu(getMonitorsViewer().getControl());
		
		setViewId(VIEW_ID_MONITOR);

		// listen to selection in debug view
		getSite().getPage().addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractDebugView#createActions()
	 */
	protected void createActions() {
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractDebugView#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return IJavaDebugHelpContextIds.MONITORS_VIEW;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractDebugView#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager menu) {
		menu.add(new Separator("vmGroup")); //$NON-NLS-1$
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractDebugView#configureToolBar(org.eclipse.jface.action.IToolBarManager)
	 */
	protected void configureToolBar(IToolBarManager tbm) {
		tbm.add(new Separator("vmGroup")); //$NON-NLS-1$
		updateObjects();
	}

	/**
	 * Returns the deadLocksViewer.
	 * @return DeadLocksViewer
	 */
	public Viewer getDeadLocksViewer() {
		return fDeadLocksViewer;
	}

	/**
	 * Returns the monitorsViewer.
	 * @return MonitorsViewer
	 */
	public Viewer getMonitorsViewer() {
		return fMonitorsViewer;
	}

	/**
	 * Sets the deadLocksViewer.
	 * @param deadLocksViewer The deadLocksViewer to set
	 */
	public void setDeadLocksViewer(Viewer deadLocksViewer) {
		fDeadLocksViewer = deadLocksViewer;
	}

	/**
	 * Sets the monitorsViewer.
	 * @param monitorsViewer The monitorsViewer to set
	 */
	public void setMonitorsViewer(Viewer monitorsViewer) {
		fMonitorsViewer = monitorsViewer;
	}

	protected void refreshCurrentViewer(boolean monitorInformationAvailable, boolean showPage) {
		if (getPageBook().isDisposed()) {
			return;
		}
		if (!fValidSelection) {
			showMessage(MonitorMessages.getString("MonitorsView.2")); //$NON-NLS-1$
			updateObjects();
			return;
		}
		boolean changeFromShowMessagePage= monitorInformationAvailable && !fMonitorInformationAvailable;
		fMonitorInformationAvailable= monitorInformationAvailable;
		if (!monitorInformationAvailable) {
			showMessage(MonitorMessages.getString("MonitorsView.1")); //$NON-NLS-1$
			updateObjects();
			return;
		}
		Control page= null;
		switch (fViewId) {
			case VIEW_ID_THREAD:
				page= getViewer().getControl();
				page.setRedraw(false);
				getViewer().refresh();
				((TreeViewer)getViewer()).expandAll();
				page.setRedraw(true);
				break;
			case VIEW_ID_DEADLOCK:
				if(MonitorManager.getDefault().getNumberOfDeadlocks() == 0 && MonitorManager.getDefault().getThreads().length > 0) {
					showMessage(MonitorMessages.getString("MonitorsView.3")); //$NON-NLS-1$
					showPage= false;
					break;
				} 
				changeFromShowMessagePage= true;
				page= getDeadLocksViewer().getControl();
				page.setRedraw(false);
				getDeadLocksViewer().refresh();
				((TreeViewer)getDeadLocksViewer()).expandAll();
				page.setRedraw(true);
				break;
			case VIEW_ID_MONITOR:
				page= getMonitorsViewer().getControl();
				page.setRedraw(false);
				getMonitorsViewer().refresh();
				((TreeViewer)getMonitorsViewer()).expandAll();
				page.setRedraw(true);				
				break;
		}
		if ((showPage | changeFromShowMessagePage) && page != null) {
			getPageBook().showPage(page);
		}
		updateObjects();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection)selection;
			Set targets= new HashSet();
			for (Iterator iter= structuredSelection.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof JDIDebugElement) {
					targets.add(((JDIDebugElement)element).getDebugTarget());
				}
			}
			if (targets.size() == 1) {
				final IJavaDebugTarget debugTarget= (IJavaDebugTarget)targets.toArray()[0];
				if (debugTarget != fLastSelectedTarget || !fValidSelection) {
					Job job = new Job(MonitorMessages.getString("MonitorsView.4")) { //$NON-NLS-1$
						protected IStatus run(IProgressMonitor monitor) {
							final boolean monitorInformationAvailable= debugTarget.supportsMonitorInformation();
							if (monitorInformationAvailable) {
								MonitorManager.getDefault().updatePartial(debugTarget);
							}
							fValidSelection= true;	
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									refreshCurrentViewer(monitorInformationAvailable, true);
									fLastSelectedTarget= debugTarget;						
								}
							});
							
							return Status.OK_STATUS;
						}
					};
					
					job.schedule();
				}
				return;
			}
		}
		if (fValidSelection) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					fValidSelection= false;
					refreshCurrentViewer(false, true);
				}
			});
		}
	}
}
