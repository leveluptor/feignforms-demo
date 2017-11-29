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
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.form.ContentProcessor;
import feign.form.ContentType;
import feign.form.UrlencodedFormContentProcessor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;

/**
 * Copy-pasted the whole class feign.form.FormEncoder from dependency io.github.openfeign.form:feign-form:3.0.0.
 * Thing that was changed is marked with FIXME comments.
 *
 *
 * ----- Original Javadoc below -----
 *
 * @author Artem Labazin <xxlabaza@gmail.com>
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class FormEncoderWithWorkaround implements Encoder {

    private static final String CONTENT_TYPE_HEADER;

    private static final Pattern CHARSET_PATTERN;

    private static final Charset DEFAULT_CHARSET;

    static {
        CONTENT_TYPE_HEADER = "Content-Type";
        CHARSET_PATTERN = Pattern.compile("(?<=charset=)([\\w\\-]+)");
        DEFAULT_CHARSET = Charset.forName("UTF-8");
    }

    Encoder delegate;

    Map<ContentType, ContentProcessor> processors;

    /**
     * Constructor with the default Feign's encoder as a delegate.
     */
    public FormEncoderWithWorkaround() {
        this(new Encoder.Default());
    }

    /**
     * Constructor with specified delegate encoder.
     *
     * @param delegate  delegate encoder, if this encoder couldn't encode object.
     */
    public FormEncoderWithWorkaround(Encoder delegate) {
        this.delegate = delegate;

        val list = asList(new MultipartFormContentProcessorWithWorkaround(delegate),
                new UrlencodedFormContentProcessor()); // FIXME in original library there was a MultipartFormContentProcessor instead of MultipartFormContentProcessorWithWorkaround

        processors = new HashMap<ContentType, ContentProcessor>(list.size(), 1.F);
        for (ContentProcessor processor : list) {
            processors.put(processor.getSupportedContentType(), processor);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void encode (Object object, Type bodyType, RequestTemplate template) throws EncodeException {
        val contentTypeValue = getContentTypeValue(template.headers());
        val contentType = ContentType.of(contentTypeValue);
        if (!MAP_STRING_WILDCARD.equals(bodyType) || !processors.containsKey(contentType)) {
            delegate.encode(object, bodyType, template);
            return;
        }

        val charset = getCharset(contentTypeValue);
        val data = (Map<String, Object>) object;
        try {
            processors.get(contentType).process(template, charset, data);
        } catch (Exception ex) {
            throw new EncodeException(ex.getMessage());
        }
    }

    /**
     * Returns {@link ContentProcessor} for specific {@link ContentType}.
     *
     * @param type a type for content processor search.
     *
     * @return {@link ContentProcessor} instance for specified type or null.
     */
    protected final ContentProcessor getContentProcessor (ContentType type) {
        return processors.get(type);
    }

    private String getContentTypeValue (Map<String, Collection<String>> headers) {
        for (val entry : headers.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase(CONTENT_TYPE_HEADER)) {
                continue;
            }
            for (val contentTypeValue : entry.getValue()) {
                if (contentTypeValue == null) {
                    continue;
                }
                return contentTypeValue;
            }
        }
        return null;
    }

    private Charset getCharset (String contentTypeValue) {
        val matcher = CHARSET_PATTERN.matcher(contentTypeValue);
        return matcher.find()
                ? Charset.forName(matcher.group(1))
                : DEFAULT_CHARSET;
    }
}
