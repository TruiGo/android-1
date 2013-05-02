/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jitsi.android.gui;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import org.jitsi.R;
import org.jitsi.android.gui.util.*;

import java.util.*;

/**
 * <tt>DialogActivity</tt> can be used to display alerts without having parent
 * <tt>Activity</tt> (from services). <br/>Simple alerts can be displayed using
 * static method <tt>showDialog(...)</tt>.<br/>
 * Optionally confirm button's text and the listener can be supplied. It allows
 * to react to users actions. For this purpose use method
 * <tt>showConfirmDialog(...)</tt>.<br/>
 * For more sophisticated use cases content fragment class with it's arguments
 * can be specified in method <tt>showCustomDialog()</tt>. When they're present
 * the alert message will be replaced by the {@link Fragment}'s <tt>View</tt>.
 *
 * @author Pawel Domas
 */
public class DialogActivity
    extends Activity
{

    /**
     * Dialog title extra.
     */
    public static final String EXTRA_TITLE="title";

    /**
     * Dialog message extra.
     */
    public static final String EXTRA_MESSAGE="message";

    /**
     * Optional confirm button label extra.
     */
    public static final String EXTRA_CONFRIM_TXT="confirm_txt";

    /**
     * Optional listener ID extra(can be supplied only using method static
     * <tt>showConfirmDialog</tt>.
     */
    public static final String EXTRA_LISTENER_ID="listener_id";

    /**
     * Optional content fragment's class name that will be used instead of text
     * message.
     */
    public static final String EXTRA_CONTENT_FRAGMENT="framgent_class";

    /**
     * Optional content fragment's argument <tt>Bundle</tt>.
     */
    public static final String EXTRA_CONTENT_ARGS = "fragment_args";

    /**
     * Static map holds listeners for currently displayed dialogs.
     */
    private static Map<Long, DialogListener> listenersMap
            = new HashMap<Long, DialogListener>();

    /**
     * The dialog listener.
     */
    private DialogListener listener;

    /**
     * Dialog listener's id used to identify listener in {@link #listenersMap}.
     */
    private long listenerID;

    /**
     * Flag remembers if the dialog was confirmed.
     */
    private boolean confirmed;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        setContentView(R.layout.alert_dialog);
        View content = findViewById(android.R.id.content);

        // Title
        setTitle(intent.getStringExtra(EXTRA_TITLE));

        // Message or custom content
        String contentFragment = intent.getStringExtra(EXTRA_CONTENT_FRAGMENT);
        if(contentFragment != null)
        {
            // Hide alert text
            ViewUtil.ensureVisible(content, R.id.alertText, false);

            // Display content fragment
            if(savedInstanceState == null)
            {
                try
                {
                    // Instantiate content fragment
                    Class contentClass = Class.forName(contentFragment);
                    Fragment fragment = (Fragment) contentClass.newInstance();

                    // Set fragment arguments
                    fragment.setArguments(
                            intent.getBundleExtra(EXTRA_CONTENT_ARGS));

                    // Insert the fragment
                    getFragmentManager()
                            .beginTransaction()
                            .replace(R.id.alertContent, fragment)
                            .commit();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        else
        {
            ViewUtil.setTextViewValue(
                    findViewById(android.R.id.content),
                    R.id.alertText,
                    intent.getStringExtra(EXTRA_MESSAGE));
        }

        // Confirm button text
        String confirmTxt = intent.getStringExtra(EXTRA_CONFRIM_TXT);
        if(confirmTxt != null)
        {
            ViewUtil.setTextViewValue(content, R.id.okButton, confirmTxt);
        }

        // Show cancel button if confirm label is not null
        ViewUtil.ensureVisible(content, R.id.cancelButton, confirmTxt != null);

        // Sets the listener
        this.listenerID = intent.getLongExtra(EXTRA_LISTENER_ID, -1);
        if(listenerID != -1)
        {
            this.listener = listenersMap.get(listenerID);
        }
    }

    /**
     * Fired when confirm button is clicked.
     * @param v the confirm button view.
     */
    public void onOkClicked(View v)
    {
        if(listener != null)
        {
            listener.onConfirmClicked(this);
        }
        confirmed = true;
        finish();
    }

    /**
     * Fired when cancel button is clicked.
     * @param v the cancel button view.
     */
    public void onCancelClicked(View v)
    {
        finish();
    }

    /**
     * Removes listener from the map.
     */
    @Override
    protected void onDestroy()
    {
        // Notify that dialog was cancelled if confirmed == false
        if(listener != null && !confirmed)
        {
            listener.onDialogCancelled(this);
        }

        // Removes the listener from map
        if(listenerID != -1)
        {
            listenersMap.remove(listenerID);
        }

        super.onDestroy();
    }

    /**
     * Show simple alert that will be disposed when user presses OK button.
     * @param ctx Android context.
     * @param title the dialog title that will be used.
     * @param message the dialog message that will be used.
     */
    public static void showDialog(Context ctx, String title, String message)
    {
        Intent alert = new Intent(ctx, DialogActivity.class);
        alert.putExtra(EXTRA_TITLE, title);
        alert.putExtra(EXTRA_MESSAGE, message);
        alert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(alert);
    }

    /**
     * Shows confirm dialog allowing to handle confirm action using supplied
     * <tt>listener</tt>.
     *
     * @param context Android context.
     * @param title dialog title that will be used
     * @param message dialog message that wil be used.
     * @param confirmTxt confirm button label.
     * @param listener the confirm action listener.
     */
    public static void showConfirmDialog(
            Context context,
            String title,
            String message,
            String confirmTxt,
            DialogListener listener )
    {
        Intent alert = new Intent(context, DialogActivity.class);

        if(listener != null)
        {
            long listenerID = System.currentTimeMillis();
            listenersMap.put(listenerID, listener);
            alert.putExtra(EXTRA_LISTENER_ID, listenerID);
        }

        alert.putExtra(EXTRA_TITLE, title);
        alert.putExtra(EXTRA_MESSAGE, message);
        alert.putExtra(EXTRA_CONFRIM_TXT, confirmTxt);

        alert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(alert);
    }

    /**
     * Show custom dialog. Alert text will be replaced by the {@link Fragment}
     * created from <tt>fragmentClass</tt> name. Optional
     * <tt>fragmentArguments</tt> <tt>Bundle</tt> will be supplied to created
     * instance.
     *
     * @param context Android context.
     * @param title the title that will be used.
     * @param fragmentClass <tt>Fragment</tt>'s class name that will be used
     *        instead of text message.
     * @param fragmentArguments optional <tt>Fragment</tt> arguments
     *        <tt>Bundle</tt>.
     * @param confirmTxt the confirm button's label.
     * @param listener listener that will be notified on user actions.
     */
    public static void showCustomDialog(
            Context context,
            String title,
            String fragmentClass,
            Bundle fragmentArguments,
            String confirmTxt,
            DialogListener listener )
    {
        Intent alert = new Intent(context, DialogActivity.class);

        if(listener != null)
        {
            long listenerID = System.currentTimeMillis();
            listenersMap.put(listenerID, listener);
            alert.putExtra(EXTRA_LISTENER_ID, listenerID);
        }

        alert.putExtra(EXTRA_TITLE, title);
        alert.putExtra(EXTRA_CONFRIM_TXT, confirmTxt);

        alert.putExtra(EXTRA_CONTENT_FRAGMENT, fragmentClass);
        alert.putExtra(EXTRA_CONTENT_ARGS, fragmentArguments);


        alert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(alert);
    }

    /**
     * The listener that will be notified when user clicks the confirm button
     * or dismisses the dialog.
     */
    public interface DialogListener
    {
        /**
         * Fired when user clicks the dialog's confirm button.
         * @param dialog source <tt>DialogActivity</tt>.
         */
        public void onConfirmClicked(DialogActivity dialog);

        /**
         * Fired when user dismisses the dialog.
         * @param dialog source <tt>DialogActivity</tt>
         */
        public void onDialogCancelled(DialogActivity dialog);
    }
}
