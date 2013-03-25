/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui.account;

import android.app.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.*;
import org.jitsi.android.gui.util.*;
import org.jitsi.android.gui.util.event.EventListener;
import org.osgi.framework.*;

import java.util.*;

/**
 * This is a convenience class which implements an {@link Adapter} interface
 * to put the list of {@link Account}s into Android widgets.
 *
 * The {@link View}s for each row are created from the layout resource id
 * given in constructor. This view must contain:
 * <br/>
 *  - <tt>R.id.accountName</tt> for the account name text ({@link TextView})
 * <br/>
 *  - <tt>R.id.accountProtoIcon</tt> for the protocol icon of type
 *      ({@link ImageView})
 * <br/>
 *  - <tt>R.id.accountStatusIcon</tt> for the presence status icon
 *      ({@link ImageView})
 * <br/>
 *  - <tt>R.id.accountStatus</tt> for the presence status name
 *      ({@link TextView})
 * <br/>
 * It implements {@link EventListener} to refresh the list on any
 * changes to the {@link Account}.
 * 
 * @author Pawel Domas
 */
public class AccountsListAdapter
    extends CollectionAdapter<Account>
    implements EventListener<AccountEvent>,
        ServiceListener
{
    /**
     * The logger
     */
    private static final Logger logger =
            Logger.getLogger(AccountsListAdapter.class);

    /**
     * The {@link View} resources ID describing list's row
     */
    private final int listRowResourceID;
    /**
     * The {@link BundleContext} of parent
     * {@link org.jitsi.service.osgi.OSGiActivity}
     */
    private final BundleContext bundleContext;
    /**
     * The flag indicates whether disabled accounts should be filtered
     * out from the list
     */
    private final boolean filterDisabledAccounts;

    /**
     * Creates new instance of {@link AccountsListAdapter}
     *
     * @param context the {@link BundleContext} of parent
     *  {@link org.jitsi.service.osgi.OSGiActivity}
     * @param parent the {@link Activity} running this adapter
     * @param accounts collection of accounts that will be displayed
     * @param inflater the {@link LayoutInflater} which
     *  will be used to create new {@link View}s
     * @param listRowResourceID the layout resource ID see
     *  {@link AccountsListAdapter} for detailed description
     * @param filterDisabledAccounts flag indicates if disabled accounts
     *  should be filtered out from the list
     */
    public AccountsListAdapter(
            BundleContext context,
            Activity parent,
            LayoutInflater inflater,
            int listRowResourceID,
            Collection<AccountID> accounts,
            boolean filterDisabledAccounts)
    {
        super(parent, inflater);

        this.filterDisabledAccounts = filterDisabledAccounts;

        this.listRowResourceID = listRowResourceID;

        this.bundleContext = context;
        context.addServiceListener(this);

        initAccounts(accounts);
    }

    /**
     * Initialize the list and filters out disabled accounts if necessary.
     *
     * @param collection set of {@link AccountID} that will be displayed
     */
    private void initAccounts(Collection<AccountID> collection)
    {
        ArrayList<Account> accounts = new ArrayList<Account>();

        for(AccountID acc : collection)
        {
            Account account = new Account( acc,
                                           bundleContext,
                                           getParentActivity());
            if( filterDisabledAccounts
                && !account.isEnabled() )
                continue;

            account.addAccountEventListener(this);
            accounts.add(account);
        }

        setList(accounts);
    }

    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (event.getServiceReference().getBundle().getState()
                == Bundle.STOPPING)
        {
            return;
        }
        Object sourceService =
                bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a protocol provider
        if (!(sourceService instanceof ProtocolProviderService))
        {
            return;
        }

        ProtocolProviderService protocolProvider
                = (ProtocolProviderService) sourceService;

        // Add or remove the protocol provider from our accounts list.
        if (event.getType() == ServiceEvent.REGISTERED)
        {
            AccountID accountID = protocolProvider.getAccountID();
            if(findAccountID(accountID) == null)
            {
                addAccount(
                        new Account( accountID,
                                     bundleContext,
                                     getParentActivity().getBaseContext()));
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            removeAccount(protocolProvider.getAccountID());
        }
    }

    /**
     * Unregisters status update listeners for accounts
     */
    void deinitStatusListeners()
    {
        for(int accIdx=0; accIdx < getCount(); accIdx++)
        {
            Account account = getObject(accIdx);

            account.destroy();
        }
    }

    /**
     * Convenience method for creating new {@link View}s for each
     * adapter's object
     *
     * @param account the account for which a new View shall be created
     * @param parent {@link ViewGroup} parent View
     * @param inflater the {@link LayoutInflater} for creating new Views
     *
     * @return a {@link View} for given <tt>item</tt>
     */
    @Override
    protected View getView( Account account,
                            ViewGroup parent,
                            LayoutInflater inflater )
    {

        View statusItem = inflater.inflate(
                listRowResourceID, parent, false);

        TextView accountName =
                (TextView) statusItem.findViewById(R.id.accountName);
        ImageView accountProtocol =
                (ImageView) statusItem.findViewById(R.id.accountProtoIcon);
        ImageView statusIconView =
                (ImageView) statusItem.findViewById(R.id.accountStatusIcon);
        TextView accountStatus =
                (TextView) statusItem.findViewById(R.id.accountStatus);

        // Sets account's properties
        accountName.setText(account.getAccountName());

        Drawable protoIcon = account.getProtocolIcon();
        if(protoIcon != null)
        {
            accountProtocol.setImageDrawable(protoIcon);
        }

        accountStatus.setText(account.getStatusName());

        Drawable statusIcon = account.getStatusIcon();
        if(statusIcon != null)
        {
            statusIconView.setImageDrawable(statusIcon);
        }

        return statusItem;
    }

    /**
     * Check if given <tt>account</tt> exists on the list
     *
     * @param account {@link AccountID} that has to be found on the list
     *
     * @return <tt>true</tt> if account is on the list
     */
    private Account findAccountID(AccountID account)
    {
        for(int i=0; i<getCount(); i++)
        {
            Account acc = getObject(i);
            if(acc.getAccountID().equals(account))
                return acc;
        }
        return null;
    }

    /**
     * Adds new account to the list
     *
     * @param account {@link Account} that will be added to the list
     */
    public void addAccount(Account account)
    {
        if(filterDisabledAccounts &&
           !account.isEnabled())
            return;

        logger.debug("Account added: " + account.getAccountName());
        add(account);
        account.addAccountEventListener(this);
    }

    /**
     * Removes the account from the list
     * @param account the {@link AccountID} that will be removed from the list
     */
    public void removeAccount(AccountID account)
    {
        Account acc = findAccountID(account);
        if(acc != null)
        {
            acc.removeAccountEventListener(this);
            logger.debug("Account removed: " + account.getDisplayName());
        }
    }

    /**
     * Does refresh the list
     *
     * @param accountEvent the {@link AccountEvent} that caused the change event
     */
    public void onChangeEvent(AccountEvent accountEvent)
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("Received accountEvent update "
                    + accountEvent.getSource().getAccountName(),
                    new Throwable());
        }
        doRefreshList();
    }
}
