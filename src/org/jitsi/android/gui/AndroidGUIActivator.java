/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.content.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;
import org.jitsi.android.*;
import org.jitsi.android.gui.account.*;
import org.jitsi.android.gui.util.*;
import org.osgi.framework.*;

/**
 * Creates <tt>LoginManager</tt> and registers <tt>AlertUIService</tt>.
 * It's moved here from launcher <tt>Activity</tt> because it could be created
 * multiple times and result in multiple objects/registrations for those
 * services. It also guarantees that they wil be registered each time OSGI
 * service starts.
 *
 * @author Pawel Domas
 */
public class AndroidGUIActivator
        implements BundleActivator
{

    /**
     * The {@link LoginManager}
     */
    private static LoginManager loginManager;

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
        Context androidContext = JitsiApplication.getGlobalContext();

        AndroidLoginRenderer loginRenderer
                = new AndroidLoginRenderer(androidContext);

        loginManager = new LoginManager(loginRenderer);

        // Register the alert service android implementation.
        AlertUIService alertServiceImpl = new AlertUIServiceImpl(
                androidContext);

        bundleContext.registerService(
                AlertUIService.class.getName(),
                alertServiceImpl,
                null);

        AccountManager accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);

        if(accountManager.getStoredAccounts().size() > 0)
        {
            new Thread(new Runnable()
            {
                public void run()
                {
                    loginManager.runLogin();
                }
            }).start();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {

    }

    /**
     * Returns the <tt>LoginManager</tt> for Android application.
     * @return the <tt>LoginManager</tt> for Android application.
     */
    public static LoginManager getLoginManager()
    {
        return loginManager;
    }
}
