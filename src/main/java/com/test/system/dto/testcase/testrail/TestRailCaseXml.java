package com.test.system.dto.testcase.testrail;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * Represents a test case in TestRail XML export.
 */
@Data
public class TestRailCaseXml {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "template")
    private String template;

    @JacksonXmlProperty(localName = "type")
    private String type;

    @JacksonXmlProperty(localName = "priority")
    private String priority;

    @JacksonXmlProperty(localName = "estimate")
    private String estimate;

    @JacksonXmlProperty(localName = "references")
    private String references;

    @JacksonXmlProperty(localName = "custom")
    private TestRailCustomFieldsXml custom;

    @JacksonXmlProperty(localName = "is_converted")
    private String isConverted;
}

