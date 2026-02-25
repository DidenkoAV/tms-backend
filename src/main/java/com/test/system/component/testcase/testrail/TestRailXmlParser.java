package com.test.system.component.testcase.testrail;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.test.system.dto.testcase.testrail.TestRailSuiteXml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for TestRail XML export files.
 * Uses Jackson XML mapper to deserialize XML into Java objects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestRailXmlParser {

    private static final String LOG_PREFIX = "[TestRailXmlParser]";

    private final XmlMapper xmlMapper;

    /**
     * Parse TestRail XML from input stream.
     *
     * @param inputStream the XML input stream
     * @return parsed TestRail suite
     * @throws IOException if parsing fails
     */
    public TestRailSuiteXml parse(InputStream inputStream) throws IOException {
        log.debug("{} Parsing TestRail XML", LOG_PREFIX);

        try {
            TestRailSuiteXml suite = xmlMapper.readValue(inputStream, TestRailSuiteXml.class);
            log.info("{} Successfully parsed TestRail suite: {}", LOG_PREFIX, suite.getName());
            return suite;

        } catch (IOException e) {
            log.error("{} Failed to parse TestRail XML", LOG_PREFIX, e);
            throw new IOException("Failed to parse TestRail XML: " + e.getMessage(), e);
        }
    }

    /**
     * Parse TestRail XML from byte array.
     *
     * @param xmlBytes the XML byte array
     * @return parsed TestRail suite
     * @throws IOException if parsing fails
     */
    public TestRailSuiteXml parse(byte[] xmlBytes) throws IOException {
        log.debug("{} Parsing TestRail XML from byte array", LOG_PREFIX);

        try {
            TestRailSuiteXml suite = xmlMapper.readValue(xmlBytes, TestRailSuiteXml.class);
            log.info("{} Successfully parsed TestRail suite: {}", LOG_PREFIX, suite.getName());
            return suite;

        } catch (IOException e) {
            log.error("{} Failed to parse TestRail XML", LOG_PREFIX, e);
            throw new IOException("Failed to parse TestRail XML: " + e.getMessage(), e);
        }
    }
}

