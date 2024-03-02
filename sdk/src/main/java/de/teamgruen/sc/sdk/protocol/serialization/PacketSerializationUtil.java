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
import java.util.Objects;
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

    /**
     * Serializes a packet to XML.
     *
     * @param packet the packet to serialize
     * @return The XML representation of the packet
     * @throws SerializationException if the packet could not be serialized
     */
    public static String serialize(Object packet) throws SerializationException {
        try {
            return XML_MAPPER.writeValueAsString(packet).strip();
        } catch (JsonProcessingException ex) {
            throw new SerializationException(ex);
        }
    }

    /**
     * Deserializes an XML string to a list of packets.
     *
     * @param xml the XML to deserialize
     * @return The deserialized packets
     * @throws DeserializationException if the XML could not be deserialized
     */
    public static LinkedList<XMLProtocolPacket> deserialize(String xml) throws DeserializationException {
        if(xml == null)
            throw new IllegalArgumentException("XML cannot be null");

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

    /**
     * Deserializes an XML string to a packet.
     *
     * @param xml the XML to deserialize
     * @return The deserialized packet
     * @throws DeserializationException if the XML could not be deserialized
     */
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

    /**
     * Gets the root tag of a packet class.
     *
     * @param clazz the packet class
     * @throws NullPointerException if the class does not provide a root tag annotation
     * @return The root tag of the packet class
     */
    private static String getRootTag(Class<? extends XMLProtocolPacket> clazz) throws NullPointerException {
        return Objects.requireNonNull(clazz.getAnnotation(JacksonXmlRootElement.class)).localName();
    }

    /**
     * Parses the tag name of an XML string.
     *
     * @param xml the XML to parse
     * @return The tag name of the XML object
     */
    public static String parseXMLTagName(String xml) {
        final Matcher matcher = XML_TAG_PATTERN.matcher(xml);

        if (matcher.find())
            return matcher.group(1);

        return null;
    }

}
