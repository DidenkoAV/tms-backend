package com.test.system.dto.testcase.testrail;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a section in TestRail XML export.
 * Sections can contain test cases and nested subsections.
 */
@Data
public class TestRailSectionXml {

    @JacksonXmlProperty(localName = "name")
    private String name;

    @JacksonXmlProperty(localName = "description")
    private String description;

    @JacksonXmlElementWrapper(localName = "cases")
    @JacksonXmlProperty(localName = "case")
    private List<TestRailCaseXml> cases = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "sections")
    @JacksonXmlProperty(localName = "section")
    private List<TestRailSectionXml> sections = new ArrayList<>();
}

