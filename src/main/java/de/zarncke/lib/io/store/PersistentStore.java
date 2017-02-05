package de.zarncke.lib.io.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import de.zarncke.lib.err.CantHappenException;

// TODO this store works but is incomplete in so far as it only always appends new objects without ever discarding old ones.
public class PersistentStore implements Closeable
{
    private int mappedSize;

    private MappedByteBuffer buffer;

    private final FileChannel backingChannel;

    PersistentStore(final File backingFile) throws IOException
    {
        // Open the file and then get a channel from the stream
        RandomAccessFile raf = new RandomAccessFile(backingFile, "rw");
        this.backingChannel = raf.getChannel();

        // Get the file's size and then map it into memory
        ensureMapping(Math.max(8, this.backingChannel.size()));

        if ( this.mappedSize <= 8 )
        {
            //this.buffer.limit(4);
            IntBuffer asIntBuffer = this.buffer.asIntBuffer();
            asIntBuffer.put(0);
            asIntBuffer.put(0);
        }
        this.buffer.position(8);
    }

    private void ensureMapping(final long capacity) throws IOException
    {
        if ( capacity > this.mappedSize )
        {
            this.mappedSize = (int) capacity;
            int pos = 0;
            if ( this.buffer != null )
            {
                pos = this.buffer.position();
                this.buffer = null;
            }
            this.buffer = this.backingChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.mappedSize);
            this.buffer.position(pos);
        }
    }

    public static PersistentStore init(final File backingFile) throws IOException
    {
        return new PersistentStore(backingFile);
    }

    public Serializable get()
    {
        this.buffer.position(0);
        IntBuffer intBuffer = this.buffer.asIntBuffer();
        int pos = intBuffer.get();
        int len = intBuffer.get();
        if ( len == 0 )
        {
            return null;
        }
        this.buffer.position(pos);
        byte[] bytes = new byte[len];
        //System.out.println("pos=" + pos + " len=" + len + " data=" + Arrays.toString(bytes));
        this.buffer.get(bytes);
        return deserializeObject(bytes);
    }

    public void put(final Serializable obj) throws IOException
    {
        byte[] bytes = serializeObject(obj);

        int pos = this.buffer.position();
        int len = bytes.length;
        ensureMapping(pos + len);
        this.buffer.put(bytes);
        this.buffer.force();
        this.buffer.position(0);
        IntBuffer intBuffer = this.buffer.asIntBuffer();
        intBuffer.put(pos);
        intBuffer.put(len);
        //System.out.println("pos=" + pos + " len=" + len + " data=" + Arrays.toString(bytes));
        this.buffer.force();
        this.buffer.position(pos + len);
    }

    private byte[] serializeObject(final Serializable obj)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try
        {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
        }
        catch ( IOException e )
        {
            throw new CantHappenException("IOException in memory cant happen", e);
        }
        return baos.toByteArray();
    };

    private Serializable deserializeObject(final byte[] bytes)
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try
        {
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (Serializable) ois.readObject();
        }
        catch ( ClassNotFoundException e )
        {
            throw new RuntimeException("cannot deserialize object (unknown class?)", e);
        }
        catch ( IOException e )
        {
            throw new RuntimeException("cannot deserialize object (invalid data?)", e);
        }
    };

	@Override
	public void close() throws IOException {
		this.backingChannel.close();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}
}
