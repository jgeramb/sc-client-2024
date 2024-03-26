/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.admin.PlayerJoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.admin.PreparedRoomResponse;
import de.teamgruen.sc.sdk.protocol.exceptions.DeserializationException;
import de.teamgruen.sc.sdk.protocol.exceptions.SerializationException;
import de.teamgruen.sc.sdk.protocol.responses.ErrorPacket;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.LeftPacket;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;
import lombok.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PacketSerializationUtil {

    private static final ObjectMapper XML_MAPPER = new XmlMapper();
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("<(\\w+)(.+)?>");
    private static final List<Class<? extends XMLProtocolPacket>> INCOMING_PACKET_TYPES;

    static {
        INCOMING_PACKET_TYPES = List.of(
                ErrorPacket.class,
                JoinedRoomResponse.class,
                RoomPacket.class,
                LeftPacket.class,
                // admin
                PreparedRoomResponse.class,
                PlayerJoinedRoomResponse.class
        );
    }

    /**
     * Serializes a packet to XML.
     *
     * @param packet the packet to serialize
     * @return The XML representation of the packet
     * @throws SerializationException if the packet could not be serialized
     */
    public static String serialize(@NonNull Object packet) throws SerializationException {
        try {
            return XML_MAPPER.writeValueAsString(packet).strip();
        } catch (JsonProcessingException ex) {
            throw new SerializationException(ex);
        }
    }

    /**
     * Deserializes an XML string to a packet.
     *
     * @param xml the XML to deserialize
     * @return The deserialized packet
     * @throws IllegalArgumentException if no root tag is provided
     * @throws DeserializationException if the XML could not be deserialized
     */
    public static XMLProtocolPacket deserializeXML(String rootTag, @NonNull String xml) throws DeserializationException {
        if(rootTag == null)
            throw new IllegalArgumentException("Root tag must not be null");

        try {
            final Class<? extends XMLProtocolPacket> packetType = INCOMING_PACKET_TYPES.stream()
                    .filter(type -> rootTag.equals(getRootTag(type)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown packet type: " + rootTag));

            return XML_MAPPER.readValue(xml, packetType);
        } catch (IllegalArgumentException | IOException ex) {
            throw new DeserializationException(ex);
        }
    }

    /**
     * Gets the root tag of a packet class.
     *
     * @param clazz the packet class
     * @throws NullPointerException if the class does not provide a root tag annotation
     * @return The root tag of the packet class
     */
    private static String getRootTag(@NonNull Class<? extends XMLProtocolPacket> clazz) throws NullPointerException {
        return Objects.requireNonNull(clazz.getAnnotation(JacksonXmlRootElement.class)).localName();
    }

    /**
     * Parses the tag name of an XML string.
     *
     * @param xml the XML to parse
     * @return The tag name of the XML object
     */
    public static String parseXMLTagName(@NonNull String xml) {
        final Matcher matcher = XML_TAG_PATTERN.matcher(xml);

        if (matcher.find())
            return matcher.group(1);

        return null;
    }

}
