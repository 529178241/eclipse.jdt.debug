package org.eclipse.jdt.internal.debug.ui.monitors;

import org.eclipse.debug.ui.viewers.AsynchronousTreeContentAdapter;
import org.eclipse.jdt.internal.debug.ui.IJDIPreferencesConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

public abstract class AsyncMonitorAdapter extends AsynchronousTreeContentAdapter implements IPropertyChangeListener {

	private boolean fDisplayMonitors;

	public AsyncMonitorAdapter() {
        IPreferenceStore preferenceStore = JDIDebugUIPlugin.getDefault().getPreferenceStore();
		preferenceStore.addPropertyChangeListener(this);
		fDisplayMonitors= preferenceStore.getBoolean(IJDIPreferencesConstants.PREF_SHOW_MONITOR_THREAD_INFO);
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(IJDIPreferencesConstants.PREF_SHOW_MONITOR_THREAD_INFO)) {
			fDisplayMonitors= ((Boolean)event.getNewValue()).booleanValue();
		}
	}

	protected boolean isDisplayMonitors() {
	    return fDisplayMonitors;
	}
}
