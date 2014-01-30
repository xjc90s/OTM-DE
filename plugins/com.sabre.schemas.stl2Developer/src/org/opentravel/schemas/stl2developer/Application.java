/*
 * Copyright (c) 2011, Sabre Inc.
 */
package org.opentravel.schemas.stl2developer;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.opentravel.schemas.preferences.CompilerPreferences;

import org.opentravel.schemacompiler.ioc.CompilerExtensionRegistry;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app. IApplicationContext)
     */
    @Override
    public Object start(final IApplicationContext context) {
        final Display display = PlatformUI.createDisplay();
        try {

            // Assign the initially-selected compiler extension from the workbench preferences
            final CompilerPreferences compilerPreferences = new CompilerPreferences(
                    CompilerPreferences.loadPreferenceStore());
            final String preferredExtension = compilerPreferences.getCompilerExtensionId();

            if (CompilerExtensionRegistry.getAvailableExtensionIds().contains(preferredExtension)) {
                CompilerExtensionRegistry.setActiveExtension(preferredExtension);
            }

            // Launch the application and wait for the return code
            final int returnCode = PlatformUI.createAndRunWorkbench(display,
                    new ApplicationWorkbenchAdvisor());
            if (returnCode == PlatformUI.RETURN_RESTART) {
                return IApplication.EXIT_RESTART;
            }
            return IApplication.EXIT_OK;
        } finally {
            display.dispose();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.equinox.app.IApplication#stop()
     */
    @Override
    public void stop() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final Display display = workbench.getDisplay();
        display.syncExec(new Runnable() {
            @Override
            public void run() {
                if (!display.isDisposed()) {
                    workbench.close();
                }
            }
        });
    }
}
