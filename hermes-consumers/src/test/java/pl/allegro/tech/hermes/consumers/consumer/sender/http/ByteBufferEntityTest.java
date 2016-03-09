package pl.allegro.tech.hermes.consumers.consumer.sender.http;/*
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

import org.apache.http.Consts;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Unit tests for {@link ByteBufferEntity}.
 *
 */
public class ByteBufferEntityTest {

    @Test
    public void testBasics() throws Exception {
        final ByteBuffer bytes = ByteBuffer.wrap("Message content".getBytes(Consts.ASCII));
        final ByteBufferEntity httpentity = new ByteBufferEntity(bytes);

        Assert.assertEquals(bytes.capacity(), httpentity.getContentLength());
        Assert.assertNotNull(httpentity.getContent());
        Assert.assertTrue(httpentity.isRepeatable());
        Assert.assertFalse(httpentity.isStreaming());
    }


    @Test(expected=IllegalArgumentException.class)
    public void testIllegalConstructorNullByteArray() throws Exception {
        new ByteBufferEntity(null);
    }

    @Test
    public void testWriteTo() throws Exception {
        final ByteBuffer bytes = ByteBuffer.wrap("Message content".getBytes(Consts.ASCII));
        final ByteBufferEntity httpentity = new ByteBufferEntity(bytes);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpentity.writeTo(out);
        byte[] bytes2 = out.toByteArray();
        Assert.assertNotNull(bytes2);
        Assert.assertEquals(bytes.capacity(), bytes2.length);
        bytes.position(0);
        for (int i = 0; i < bytes2.length; i++) {
            Assert.assertEquals(bytes.get(i), bytes2[i]);
        }

        out = new ByteArrayOutputStream();
        httpentity.writeTo(out);
        bytes2 = out.toByteArray();
        Assert.assertNotNull(bytes2);
        Assert.assertEquals(bytes.capacity(), bytes2.length);
        bytes.position(0);
        for (int i = 0; i < bytes.capacity(); i++) {
            Assert.assertEquals(bytes.get(i), bytes2[i]);
        }

        try {
            httpentity.writeTo(null);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }
}