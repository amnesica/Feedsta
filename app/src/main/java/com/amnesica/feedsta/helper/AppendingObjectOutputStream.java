package com.amnesica.feedsta.helper;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Helper class to append to an ObjectOutputStream
 * source: https://stackoverflow.com/questions/1194656/appending-to-an-objectoutputstream
 */
class AppendingObjectOutputStream extends ObjectOutputStream {

    AppendingObjectOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    @Override
    protected void writeStreamHeader() throws IOException {
        //  do not write a header, but reset:
        //  this line added after another question
        //  showed a problem with the original
        reset();
    }

}