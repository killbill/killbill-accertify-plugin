/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.accertify.client;

import java.io.IOException;
import java.util.Map;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public abstract class XmlMapperProvider {

    public static XmlMapper get() {
        final XmlMapper mapper = new XmlMapper();

        final SimpleModule m = new SimpleModule("accertify", new Version(1, 0, 0, null, null, null));
        m.addSerializer(RequestDataCollection.class, new RequestDataCollectionSerializer(RequestDataCollection.class));
        mapper.registerModule(m);

        // Mostly to help tests and debugging
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

        return mapper;
    }

    private static final class RequestDataCollectionSerializer extends StdSerializer<RequestDataCollection> {

        protected RequestDataCollectionSerializer(final Class t) {
            super(t);
        }

        @Override
        public void serialize(final RequestDataCollection values, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
            // See https://github.com/FasterXML/jackson-dataformat-xml/issues/216
            ((ToXmlGenerator) jgen).setNextName(new QName(null, values.getChildElementName()));
            jgen.writeStartObject();
            for (final Map<String, Object> value : values) {
                for (final String key : value.keySet()) {
                    jgen.writeObjectField(key, value.get(key));
                }
            }
            jgen.writeEndObject();
        }
    }
}
