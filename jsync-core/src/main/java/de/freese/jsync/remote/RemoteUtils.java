// Created: 04.09.20
package de.freese.jsync.remote;

/**
 * @author Thomas Freese
 */
public final class RemoteUtils
{
    // /**
    // *
    // */
    // private static final byte[] EOL = new byte[]
    // {
    // 0x45, 0x4F, 0x4C
    // };

    /**
     *
     */
    public static final int STATUS_ERROR = 500;

    /**
     *
     */
    public static final int STATUS_OK = 200;

    // /**
    // * @return int
    // */
    // public static int getLengthOfEOL()
    // {
    // return EOL.length;
    // }

    // /**
    // * @param buffer {@link ByteBuffer}
    // * @return boolean
    // */
    // public static boolean isEOL(final ByteBuffer buffer)
    // {
    // if ((buffer.limit() - buffer.position()) < 3)
    // {
    // // Buffer hat keine 3 Bytes mehr.
    // return false;
    // }
    //
    // int index = buffer.position();
    //
    // byte e = buffer.get(index);
    // byte o = buffer.get(index + 1);
    // byte l = buffer.get(index + 2);
    //
    // return (EOL[0] == e) && (EOL[1] == o) && (EOL[2] == l);
    // }

    // /**
    // * @param buffer {@link ByteBuffer}
    // * @return boolean
    // */
    // public static boolean isResponseOK(final ByteBuffer buffer)
    // {
    // if ((buffer.limit() - buffer.position()) < 4)
    // {
    // // Buffer hat keine 4 Bytes mehr.
    // return false;
    // }
    //
    // return buffer.getInt() == 200;
    // }

    // /**
    // * @param buffer {@link ByteBuffer}
    // */
    // public static void writeEOL(final ByteBuffer buffer)
    // {
    // buffer.put(EOL);
    // }

    // /**
    // * @param buffer {@link ByteBuffer}
    // */
    // public static void writeResponseERROR(final ByteBuffer buffer)
    // {
    // buffer.putInt(STATUS_ERROR);
    // }

    // /**
    // * @param buffer {@link ByteBuffer}
    // */
    // public static void writeResponseOK(final ByteBuffer buffer)
    // {
    // buffer.putInt(STATUS_OK);
    // }

    /**
     * Erzeugt eine neue Instanz von {@link RemoteUtils}
     */
    private RemoteUtils()
    {
        super();
    }
}
