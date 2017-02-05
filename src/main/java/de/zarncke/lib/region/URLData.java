package de.zarncke.lib.region;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;

import de.zarncke.lib.data.HasData;
import de.zarncke.lib.err.TunnelException;
import de.zarncke.lib.io.IOTools;

/**
 * This HasData reads from an URL.
 */
public class URLData implements HasData, Serializable
{
	private static final long serialVersionUID = 1L;
    /**
     * the data providing url
     */
    private final URL url;

    /**
     * the connection (may be null)
     */
	private transient WeakReference<URLConnection> conn = null;

    /**
     * Create for url
     *
     * @param url
     *            is the url from which to gather the data
     */
    public URLData(final URL url)
    {
        if ( url == null )
        {
            throw new IllegalArgumentException("URL may not be null!");
        }

        this.url = url;
    }

    /**
     * get the (connected) connection
     */
    protected URLConnection getConnection() throws IOException
    {
        URLConnection uc = null;
        if ( this.conn != null )
        {
            uc = this.conn.get();
        }
        if ( uc == null )
        {
            uc = this.url.openConnection();
            uc.connect();
			this.conn = new WeakReference<URLConnection>(uc);
        }
        return uc;
    }

    /**
     * get the bytes
     *
     * @return the byte array
     */
    public Region asRegion()
    {
        try
        {
            byte[] ba;
            if ( "file".equals(this.url.getProtocol()) )
            {
                String path = this.url.getPath();
                if ( this.url.getAuthority() != null )
                {
                    path = this.url.getAuthority() + path;
                }
                // TODO may be large, consider incremental region
                ba = IOTools.getAllBytes(new File(path));
            }
            else
            {
                ba = IOTools.getAllBytes(getConnection().getInputStream());
            }
            return RegionUtil.asRegion(ba);
        }
        catch ( IOException ioe )
        {
			throw new TunnelException(ioe);
        }
    }

    /**
     * get the length of the array
     *
     * @return length of array
     */
    public int length()
    {
        try
        {
            return getConnection().getContentLength();
        }
        catch ( IOException ioe )
        {
            throw new TunnelException(ioe);
        }
    }


    public int compareTo(final Object obj)
    {
        // todo: by date?
        return this.url.toExternalForm().compareTo(((URLData) obj).url.toExternalForm());
    }

    /**
     * @return true if same url or length and all bytes are equal
     */
    @Override
    public boolean equals(final Object obj)
    {
        try
        {
			return obj instanceof URLData ? this.url.toExternalForm().equals(((URLData) obj).url.toExternalForm())
					: obj instanceof HasData ? asRegion()
                    .equals(((HasData) obj).asRegion()) : false;
        }
        catch ( IOException e )
        {
            return false;
		}
    }

    /**
     * the hashcode over the url
     */
    @Override
    public int hashCode()
    {
		return this.url.toExternalForm().hashCode();
    }

    /**
     * print info and the first 16 bytes
     *
     * @return String
     */
    @Override
    public String toString()
    {
        return "data@" + this.url;
    }
}
