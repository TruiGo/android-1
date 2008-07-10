/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

/**
 * Reperesents the Jabber protocol icon. Implements the <tt>ProtocolIcon</tt>
 * interface in order to provide a jabber icon image in two different sizes.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ProtocolIconJabberImpl
    implements ProtocolIcon
{    
    private static Logger logger = Logger.getLogger(ProtocolIconJabberImpl.class); 

    /**
     * The path where all protocol icons are placed.
     */
    private final String iconPath;
    
    /**
     * A hash table containing the protocol icon in different sizes.
     */
    private final Hashtable iconsTable = new Hashtable();

    /**
     * Creates an instance of this class by passing to it the path, where all
     * protocol icons are placed.
     * 
     * @param iconPath the protocol icon path
     */
    public ProtocolIconJabberImpl(String iconPath)
    {
        this.iconPath = iconPath;

        iconsTable.put(ProtocolIcon.ICON_SIZE_16x16, loadIcon(iconPath
            + "/status16x16-online.png"));

        iconsTable.put(ProtocolIcon.ICON_SIZE_48x48, loadIcon(iconPath
            + "/logo48x48.png"));
    }
 
    /**
     * Implements the <tt>ProtocolIcon.getSupportedSizes()</tt> method. Returns
     * an iterator to a set containing the supported icon sizes.
     * @return an iterator to a set containing the supported icon sizes
     */
    public Iterator getSupportedSizes()
    {
        return iconsTable.keySet().iterator();
    }

    /**
     * Returne TRUE if a icon with the given size is supported, FALSE-otherwise.
     */
    public boolean isSizeSupported(String iconSize)
    {
        return iconsTable.containsKey(iconSize);
    }
    
    /**
     * Returns the icon image in the given size.
     * @param iconSize the icon size; one of ICON_SIZE_XXX constants
     */
    public byte[] getIcon(String iconSize)
    {
        return (byte[])iconsTable.get(iconSize);
    }
    
    /**
     * Returns the icon image used to represent the protocol connecting state.
     * @return the icon image used to represent the protocol connecting state
     */
    public byte[] getConnectingIcon()
    {
        return loadIcon(iconPath + "/status16x16-connecting.gif");
    }
    
    /**
     * Loads an image from a given image path.
     * @param imagePath The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] loadIcon(String imagePath)
    {
        return JabberStatusEnum.loadIcon(imagePath,
            ProtocolIconJabberImpl.class);
    }    
}
