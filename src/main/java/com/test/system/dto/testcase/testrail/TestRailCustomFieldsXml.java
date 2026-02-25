package com.test.system.dto.testcase.testrail;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * Represents custom fields in TestRail XML export.
 */
@Data
public class TestRailCustomFieldsXml {

    @JacksonXmlProperty(localName = "automation_type")
    private TestRailAutomationTypeXml automationType;

    @JacksonXmlProperty(localName = "testclass")
    private String testClass;

    @JacksonXmlProperty(localName = "testmethod")
    private String testMethod;

    @JacksonXmlProperty(localName = "scenario")
    private String scenario;

    @JacksonXmlProperty(localName = "preconds")
    private String preconditions;

    @JacksonXmlProperty(localName = "steps")
    private String steps;
}

