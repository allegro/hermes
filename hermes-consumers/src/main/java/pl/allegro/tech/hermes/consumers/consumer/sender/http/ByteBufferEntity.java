package pl.allegro.tech.hermes.consumers.consumer.sender.http;

/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.Args;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/*
 *  CAUTION!
 *  This implementation comes from PR that was merged directly into hc.core5
 *  which is not backwards compatible with http-components version 4. We need
 *  entire http stack and not just the core so we cannot use core5 yet. This
 *  is the last implementation that works on version 4. If it is good enough
 *  for hc.core5 then it should be ok for out use case.
 *
 *  PR: https://github.com/apache/httpcore/pull/13
 *
* */

/**
 * An entity that delivers the contents of a {@link ByteBuffer}.
 */
@NotThreadSafe
public class ByteBufferEntity extends AbstractHttpEntity implements Cloneable {

    private final ByteBuffer buffer;

    private class ByteBufferInputStream extends InputStream {

        ByteBufferInputStream() {
            buffer.position(0);
        }

        public int read() throws IOException {
            if (!buffer.hasRemaining()) {
                return -1;
            }
            return buffer.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len) throws IOException {
            if (!buffer.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buffer.remaining());
            buffer.get(bytes, off, len);
            return len;
        }
    }

    public ByteBufferEntity(final ByteBuffer buffer, final ContentType contentType) {
        super();
        Args.notNull(buffer, "Source byte buffer");
        this.buffer = buffer;
        if (contentType != null) {
            setContentType(contentType.toString());
        }
    }

    public ByteBufferEntity(final ByteBuffer buffer) {
        this(buffer, null);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return buffer.limit();
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return new ByteBufferInputStream();
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        Args.notNull(outstream, "Output stream");
        WritableByteChannel channel = Channels.newChannel(outstream);
        channel.write(buffer);
        outstream.flush();
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
