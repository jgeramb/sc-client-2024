/*
 * Copyright (c) 2024 Justus Geramb (https://www.justix.dev)
 * All Rights Reserved.
 */

package de.teamgruen.sc.sdk.protocol.data.scores;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import de.teamgruen.sc.sdk.protocol.data.Team;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Winner {

    @JacksonXmlProperty(isAttribute = true)
    private Team team;
    @JacksonXmlProperty(isAttribute = true)
    private boolean regular;
    @JacksonXmlProperty(isAttribute = true)
    private String reason;

}
