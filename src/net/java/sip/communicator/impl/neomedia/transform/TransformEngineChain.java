/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform;

import java.util.*;

import net.java.sip.communicator.impl.neomedia.*;

/**
 * The engine chain allows using numerous <tt>TransformEngine</tt>s on a single
 * stream.
 *
 * @author Emil Ivov
 */
public class TransformEngineChain
    implements TransformEngine
{

    /**
     * The sequence of <tt>TransformEngine</tt>s whose
     * <tt>PacketTransformer</tt>s that this engine chain will be applying to
     * RTP and RTCP packets.
     */
    private final List<TransformEngine> engineChain;

    /**
     * The sequence of <tt>PacketTransformer</tt>s that this engine chain will
     * be applying to RTP packets.
     */
    private final PacketTransformerChain rtpTransformChain;

    /**
     * The sequence of <tt>PacketTransformer</tt>s that this engine chain will
     * be applying to RTCP packets.
     */
    private final PacketTransformerChain rtcpTransformChain;

    /**
     * Creates a new <tt>TransformEngineChain</tt> using the
     * <tt>engineChain</tt> array. Engines will be applied in the order
     * specified by the <tt>engineChain</tt> array for outgoing packets
     * and in the reverse order for incoming packets.
     *
     * @param engineChain an array containing <tt>TransformEngine</tt>s in the
     * order that they are to be applied on outgoing packets.
     */
    public TransformEngineChain(TransformEngine[] engineChain)
    {
        this.engineChain = Arrays.asList(engineChain);

        rtpTransformChain = new PacketTransformerChain(true);
        rtcpTransformChain = new PacketTransformerChain(false);
    }

    /**
     * Returns the meta <tt>PacketTransformer</tt> that will be applying
     * RTCP transformations from all engines registered in this
     * <tt>TransformEngineChain</tt>.
     *
     * @return a <tt>PacketTransformerChain</tt> over all RTP transformers in
     * this engine chain.
     */
    public PacketTransformerChain getRTPTransformer()
    {
        return rtpTransformChain;
    }

    /**
     * Returns the meta <tt>PacketTransformer</tt> that will be applying
     * RTCP transformations from all engines registered in this
     * <tt>TransformEngineChain</tt>.
     *
     * @return a <tt>PacketTransformerChain</tt> over all RTCP transformers in
     * this engine chain.
     */
    public PacketTransformer getRTCPTransformer()
    {
        return rtcpTransformChain;
    }

    /**
     * A <tt>PacketTransformerChain</tt> is a meta <tt>PacketTransformer</tt>
     * that applies all transformers present in this engine chain. The class
     * respects the order of the engine chain for outgoing packets and reverses
     * it for incoming packets.
     */
    private class PacketTransformerChain
        implements PacketTransformer
    {
        private final boolean isRtp;

        public PacketTransformerChain(boolean isRtp)
        {
            this.isRtp = isRtp;
        }
        /**
         * Transforms a specific packet.
         *
         * @param pkt the packet to be transformed
         * @return the transformed packet
         */
        public RawPacket transform(RawPacket pkt)
        {
            if( engineChain == null)
            {
                return pkt;
            }

            for (TransformEngine engine : engineChain)
            {
                PacketTransformer pTransformer = isRtp
                    ? engine.getRTPTransformer()
                    : engine.getRTCPTransformer();

                //the packet transformer may be null if for example the engine
                //only does RTP transformations and this is an RTCP transformer.
                if( pTransformer != null)
                    pkt = pTransformer.transform(pkt);
            }

            return pkt;
        }

        /**
         * Reverse-transforms a specific packet (i.e. transforms a transformed
         * packet back).
         *
         * @param pkt the transformed packet to be restored
         * @return the restored packet
         */
        public RawPacket reverseTransform(RawPacket pkt)
        {
            if( engineChain == null)
            {
                return pkt;
            }

            for (int i = engineChain.size() - 1 ; i >= 0; i--)
            {
                TransformEngine engine = engineChain.get(i);

                PacketTransformer pTransformer = isRtp
                ? engine.getRTPTransformer()
                : engine.getRTCPTransformer();

                //the packet transformer may be null if for example the engine
                //only does RTP transformations and this is an RTCP transformer.
                if( pTransformer != null)
                    pkt = pTransformer.reverseTransform(pkt);
            }

            return pkt;
        }
    }


}