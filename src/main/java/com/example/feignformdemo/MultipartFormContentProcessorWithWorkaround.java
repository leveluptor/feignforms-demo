package com.example.feignformdemo;

/*
 * Copyright 2017 Artem Labazin <xxlabaza@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import feign.RequestTemplate;
import feign.codec.Encoder;
import feign.form.ContentProcessor;
import feign.form.ContentType;
import feign.form.multipart.ByteArrayWriter;
import feign.form.multipart.DelegateWriter;
import feign.form.multipart.ManyFilesWriter;
import feign.form.multipart.Output;
import feign.form.multipart.ParameterWriter;
import feign.form.multipart.SingleFileWriter;
import feign.form.multipart.Writer;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static feign.form.ContentType.MULTIPART;
import static lombok.AccessLevel.PRIVATE;

/**
 * Copy-pasted the whole class feign.form.MultipartFormContentProcessor from dependency io.github.openfeign.form:feign-form:3.0.0.
 * Thing that was changed is marked with FIXME comments.
 *
 *
 * ----- Original Javadoc below -----
 *
 * @author Artem Labazin <xxlabaza@gmail.com>
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MultipartFormContentProcessorWithWorkaround implements ContentProcessor {

    List<Writer> writers;

    Writer defaultPerocessor;

    /**
     * Constructor with specific delegate encoder.
     *
     * @param delegate specific delegate encoder for cases, when this processor couldn't handle request parameter.
     */
    public MultipartFormContentProcessorWithWorkaround(Encoder delegate) {
        writers = new ArrayList<Writer>(6);
        addWriter(new ByteArrayWriter());
        addWriter(new SingleFileWriter());
        addWriter(new ManyFilesWriter());
        addWriter(new ParameterWriter());

        defaultPerocessor = new DelegateWriter(delegate);
    }

    @Override
    public void process (RequestTemplate template, Charset charset, Map<String, Object> data) throws Exception {
        val boundary = Long.toHexString(System.currentTimeMillis());
        val output = new Output(charset);

        for (val entry : data.entrySet()) {
            val writer = findApplicableWriter(entry.getValue());
            writer.write(output, boundary, entry.getKey(), entry.getValue());
        }

        output.write("--").write(boundary).write("--").write(CRLF);

        val contentTypeHeaderValue = new StringBuilder()
                .append(getSupportedContentType().getHeader())
                .append("; charset=").append(charset.name())
                .append("; boundary=").append(boundary)
                .toString();

        template.header(CONTENT_TYPE_HEADER, contentTypeHeaderValue);
        template.body(output.toByteArray(), null); // FIXME in original library there was a charset instead of null

        output.close();
    }

    @Override
    public ContentType getSupportedContentType () {
        return MULTIPART;
    }

    /**
     * Adds {@link Writer} instance in runtime.
     *
     * @param writer additional writer.
     */
    public final void addWriter (Writer writer) {
        writers.add(writer);
    }

    private Writer findApplicableWriter (Object value) {
        for (val writer : writers) {
            if (writer.isApplicable(value)) {
                return writer;
            }
        }
        return defaultPerocessor;
    }
}
