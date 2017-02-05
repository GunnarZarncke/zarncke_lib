package de.zarncke.lib.tmpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class TemplateFactory
{
    private static Charset ISO_8859_15 = Charset.forName("ISO-8859-15");

    public Template forFile(File parsedFile) throws IOException
    {
        return forFile(parsedFile, ISO_8859_15);
    }

    public Template forFile(File parsedFile, Charset encoding) throws IOException
    {
        CharsetDecoder decoder = ISO_8859_15.newDecoder();

        FileInputStream fis = new FileInputStream(parsedFile);
        FileChannel fc = fis.getChannel();

        // Get the file's size and then map it into memory
        int sz = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);

        // Decode the file into a char buffer
        CharBuffer cb = decoder.decode(bb);
        fc.close();

        return new Template(cb);
    }

}
