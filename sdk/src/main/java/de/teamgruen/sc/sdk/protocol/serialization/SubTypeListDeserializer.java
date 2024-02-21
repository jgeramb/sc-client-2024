package de.teamgruen.sc.sdk.protocol.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import de.teamgruen.sc.sdk.protocol.exceptions.DeserializationException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public abstract class SubTypeListDeserializer<R, S> extends StdDeserializer<R> {

    private final String listFieldName;
    private final Map<String, Class<? extends S>> subTypes;

    protected SubTypeListDeserializer(Class<R> result, String listFieldName, Map<String, Class<? extends S>> subTypes) {
        super(result);

        this.listFieldName = listFieldName;
        this.subTypes = subTypes;
    }

    @Override
    public R deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws DeserializationException {
        final R instance = this.getNewInstance();

        try {
            final Field listField = instance.getClass().getDeclaredField(this.listFieldName);
            listField.setAccessible(true);

            final Object list = listField.get(instance);
            final Method addMethod = list.getClass().getMethod("add", Object.class);
            final boolean wrap = listField.getAnnotation(JacksonXmlElementWrapper.class).useWrapping();

            if (wrap) {
                jsonParser.nextFieldName();
                jsonParser.nextToken();
            }

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                final Class<? extends S> fieldClass = this.subTypes.get(jsonParser.getText());

                jsonParser.nextToken();

                addMethod.invoke(list, deserializationContext.readValue(jsonParser, fieldClass));
            }

            if(wrap)
                jsonParser.nextToken();
        } catch (Throwable cause) {
            throw new DeserializationException(cause);
        }

        return instance;
    }

    public abstract R getNewInstance();

}
