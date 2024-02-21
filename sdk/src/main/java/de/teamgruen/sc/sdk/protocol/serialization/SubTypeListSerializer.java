package de.teamgruen.sc.sdk.protocol.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import de.teamgruen.sc.sdk.protocol.exceptions.SerializationException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class SubTypeListSerializer<T, S> extends StdSerializer<T> {

    private final String listFieldName;
    private final Map<Class<? extends S>, String> subTypes;

    protected SubTypeListSerializer(Class<T> type, String listFieldName, Map<Class<? extends S>, String> subTypes) {
        super(type);

        this.listFieldName = listFieldName;
        this.subTypes = subTypes;
    }

    @Override
    public void serialize(T type, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws SerializationException {
        try {
            jsonGenerator.writeStartObject();

            for (Field field : type.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                final Object fieldValue = field.get(type);

                if (field.getName().equals(this.listFieldName)) {
                    boolean wrap = field.getAnnotation(JacksonXmlElementWrapper.class).useWrapping();

                    if(wrap) {
                        jsonGenerator.writeFieldName(this.listFieldName);
                        jsonGenerator.writeStartObject();
                    }

                    for (Object subType : (List<?>) fieldValue) {
                        jsonGenerator.writeFieldName(this.subTypes.get(subType.getClass()));
                        serializerProvider.defaultSerializeValue(subType, jsonGenerator);
                    }

                    if(wrap)
                        jsonGenerator.writeEndObject();
                } else {
                    jsonGenerator.writeFieldName(field.getName());
                    serializerProvider.defaultSerializeValue(fieldValue, jsonGenerator);
                }
            }

            jsonGenerator.writeEndObject();
        } catch (Throwable cause) {
            throw new SerializationException(cause);
        }
    }

}
