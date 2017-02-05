package de.zarncke.lib.www;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.sys.Headquarters;
import de.zarncke.lib.sys.mbean.JmxHeadquarters;
import de.zarncke.lib.time.JavaClock;
import de.zarncke.lib.value.Default;
import de.zarncke.lib.www.WebService.Content;
import de.zarncke.lib.www.WebService.Result;

//import de.zarncke.lib.util.Misc;

/**
 * Standalone http server based on Simple. Does nothing, but can be derived to provide custom content.
 */
public class WebServer
{
    public static class FileContent implements Content
    {
		private final File root;

        public FileContent(final File root)
        {
            this.root = root;
        }

		@Override
		public void addName(final Object it, final Map m)
 {
			//
		}

        @Override
		public List<Object> children(final Object base)
        {
            return L.e();
        }

        @Override
		public Result get(final String path) throws IOException
        {
            Result r = new Result();
            File loc = new File(this.root, path);
            if ( loc.exists() )
            {
                r.obj = IOTools.getAllBytes(loc);
                r.code = 200;
            }
            else
            {
                r.obj = "absent".getBytes();
                r.code = 404;
            }
            return r;
        }

        @Override
		public Date getCreationDate(final Object it)
        {
			return new Date(JavaClock.getTheClock().getCurrentTimeMillis());
        }

        @Override
		public String getCursor(final Object it)
        {
            return "";
        }

        @Override
		public Object getMime(final Object it)
        {
            return "text/html";
        }

        @Override
		public String getPath(final Object it)
        {
            return null;
        }

        @Override
		public Object getProp(final String path)
        {
            return null;
        }

        @Override
		public Result putOrPost(final String path, final String ctIn, final byte[] bin, final boolean isPost)
        {
            Result r = new Result();
            r.code = 5000;
            return r;
        }

        @Override
		public void stopAll()
        {}
    }

    private static final int DEFAULT_PORT = 2607;

    private Connection connection;

    private final String docRoot;

    private final int port;

    WebService service;

    public WebServer(final String docRoot)
    {
        this(DEFAULT_PORT, docRoot);
    }

    public WebServer(final int port, final String docRoot)
    {
        this.port = port;
        this.docRoot = docRoot;
    }

    public static void main(final String[] command) throws Exception
    {
		Context.setFromNowOn(Default.of(new Headquarters(), Headquarters.class), Context.INHERITED);
		Headquarters.HEADQUARTERS.get().registerShutdownHook();
		JmxHeadquarters.startOrRestartHeadquartersMBean();
		int port = 8080;
		if (command.length >= 1) {
			port = Integer.parseInt(command[0]);
		}
		String root = ".";
		if (command.length >= 2) {
			root = command[1];
		}
		WebServer server = new WebServer(port, root);
        server.start();
    }

    public void start() throws IOException
    {
        String base = "http://localhost:" + this.port + "/";
        this.service = new WebService(base, this.docRoot, getContent());

        this.connection = new SocketConnection(this.service);

        SocketAddress address = new InetSocketAddress(this.port);
        this.connection.connect(address);
    }

    protected Content getContent()
    {
        return new FileContent(new File(this.docRoot));
    }

    public void stop() throws IOException
    {
        this.connection.close();
        this.service.stop();
    }

    public int getPort()
    {
        return this.port;
    }
}