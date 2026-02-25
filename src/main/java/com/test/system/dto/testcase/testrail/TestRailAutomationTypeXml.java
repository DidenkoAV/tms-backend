package com.test.system.dto.testcase.testrail;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * Represents automation type in TestRail XML export.
 */
@Data
public class TestRailAutomationTypeXml {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "value")
    private String value;
}

