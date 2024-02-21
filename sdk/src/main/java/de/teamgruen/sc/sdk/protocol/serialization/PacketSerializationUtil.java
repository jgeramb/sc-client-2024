package de.teamgruen.sc.sdk.protocol.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import de.teamgruen.sc.sdk.protocol.XMLProtocolPacket;
import de.teamgruen.sc.sdk.protocol.exceptions.DeserializationException;
import de.teamgruen.sc.sdk.protocol.exceptions.SerializationException;
import de.teamgruen.sc.sdk.protocol.responses.JoinedRoomResponse;
import de.teamgruen.sc.sdk.protocol.room.LeftPacket;
import de.teamgruen.sc.sdk.protocol.room.RoomPacket;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PacketSerializationUtil {

    private static final ObjectMapper XML_MAPPER = new XmlMapper();
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("<(\\w+)(.+)?>");
    private static final List<Class<? extends XMLProtocolPacket>> INCOMING_PACKET_TYPES;

    static {
        INCOMING_PACKET_TYPES = List.of(
                JoinedRoomResponse.class,
                RoomPacket.class,
                LeftPacket.class
        );
    }

    public static String serialize(Object packet) throws SerializationException {
        try {
            return XML_MAPPER.writeValueAsString(packet).strip();
        } catch (JsonProcessingException ex) {
            throw new SerializationException(ex);
        }
    }

    public static LinkedList<XMLProtocolPacket> deserialize(String xml) throws DeserializationException {
        LinkedList<XMLProtocolPacket> packets = new LinkedList<>();

        while(!xml.isBlank()) {
            final String rootTag = parseXMLTagName(xml);

            if (rootTag == null)
                throw new IllegalArgumentException("No valid XML tag found");

            final int closeTagPosition = xml.indexOf("</" + rootTag + ">");
            final String packetXml = xml.substring(0, closeTagPosition == -1 ? xml.length() : (closeTagPosition + rootTag.length() + 3));

            packets.add(deserializeXML(rootTag, packetXml));

            // remove the packet from the xml
            xml = xml.substring(packetXml.length()).strip();
        }

        return packets;
    }

    private static XMLProtocolPacket deserializeXML(String rootTag, String xml) throws DeserializationException {
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

    private static String getRootTag(Class<? extends XMLProtocolPacket> clazz) {
        JacksonXmlRootElement annotation = clazz.getAnnotation(JacksonXmlRootElement.class);

        if (annotation == null)
            return null;

        return annotation.localName();
    }

    private static String parseXMLTagName(String xml) {
        final Matcher matcher = XML_TAG_PATTERN.matcher(xml);

        if (matcher.find())
            return matcher.group(1);

        return null;
    }

}
