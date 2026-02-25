package com.test.system.dto.testcase.testrail;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Root element for TestRail XML export.
 * Represents a test suite with nested sections and test cases.
 */
@Data
@JacksonXmlRootElement(localName = "suite")
public class TestRailSuiteXml {

    @JacksonXmlProperty(localName = "id")
    private String id;

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "description")
    private String description;

    @JacksonXmlElementWrapper(localName = "sections")
    @JacksonXmlProperty(localName = "section")
    private List<TestRailSectionXml> sections = new ArrayList<>();
}

