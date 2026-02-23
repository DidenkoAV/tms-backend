package com.test.system.component.jira;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Converts Jira ADF (Atlassian Document Format) to Markdown.
 */
@Component
public class JiraAdfConverter {

    /**
     * Extracts Markdown text from ADF JSON structure.
     */
    @SuppressWarnings("unchecked")
    public String extractMarkdown(Map<String, Object> adf) {
        StringBuilder sb = new StringBuilder();

        Object contentObj = adf.get("content");
        if (!(contentObj instanceof List<?> contentList)) {
            return "";
        }

        for (Object blockObj : contentList) {
            if (!(blockObj instanceof Map<?, ?> blockRaw)) continue;
            Map<?, ?> block = blockRaw;

            Object typeObj = block.get("type");
            String type = typeObj instanceof String s ? s : null;
            if (type == null) continue;

            switch (type) {
                case "paragraph" -> appendParagraph(sb, block);
                case "bulletList" -> appendBulletList(sb, block);
                case "orderedList" -> appendOrderedList(sb, block);
                case "inlineCard" -> appendInlineCard(sb, block);
            }
        }

        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private void appendParagraph(StringBuilder sb, Map<?, ?> block) {
        Object innerContentObj = block.get("content");
        if (!(innerContentObj instanceof List<?> innerContent)) return;

        for (Object innerObj : innerContent) {
            if (!(innerObj instanceof Map<?, ?> innerRaw)) continue;
            Map<?, ?> inner = innerRaw;

            Object textObj = inner.get("text");
            if (!(textObj instanceof String text)) continue;

            text = applyMarks(text, inner.get("marks"));
            sb.append(text);
        }
        sb.append("\n\n");
    }

    @SuppressWarnings("unchecked")
    private void appendBulletList(StringBuilder sb, Map<?, ?> block) {
        Object listItemsObj = block.get("content");
        if (!(listItemsObj instanceof List<?> listItems)) return;

        for (Object itemObj : listItems) {
            if (!(itemObj instanceof Map<?, ?> itemRaw)) continue;
            Map<?, ?> item = itemRaw;
            Object itemContentObj = item.get("content");
            if (itemContentObj instanceof List<?> itemContent) {
                sb.append("- ");
                sb.append(extractMarkdown(Map.of("content", itemContent)));
                sb.append("\n");
            }
        }
        sb.append("\n");
    }

    @SuppressWarnings("unchecked")
    private void appendOrderedList(StringBuilder sb, Map<?, ?> block) {
        Object listItemsObj = block.get("content");
        if (!(listItemsObj instanceof List<?> listItems)) return;

        int index = 1;
        for (Object itemObj : listItems) {
            if (!(itemObj instanceof Map<?, ?> itemRaw)) continue;
            Map<?, ?> item = itemRaw;
            Object itemContentObj = item.get("content");
            if (itemContentObj instanceof List<?> itemContent) {
                sb.append(index++).append(". ");
                sb.append(extractMarkdown(Map.of("content", itemContent)));
                sb.append("\n");
            }
        }
        sb.append("\n");
    }

    private void appendInlineCard(StringBuilder sb, Map<?, ?> block) {
        Object attrsObj = block.get("attrs");
        if (!(attrsObj instanceof Map<?, ?> attrsRaw)) return;
        Map<?, ?> attrs = attrsRaw;
        Object urlObj = attrs.get("url");
        if (urlObj instanceof String url) {
            sb.append("[").append(url).append("](").append(url).append(")\n\n");
        }
    }

    @SuppressWarnings("unchecked")
    private String applyMarks(String text, Object marksObj) {
        if (!(marksObj instanceof List<?> marks)) return text;

        for (Object markObj : marks) {
            if (!(markObj instanceof Map<?, ?> markRaw)) continue;
            Map<?, ?> mark = markRaw;
            Object markTypeObj = mark.get("type");
            if (!(markTypeObj instanceof String markType)) continue;

            if ("strong".equals(markType)) {
                text = "**" + text + "**";
            } else if ("em".equals(markType)) {
                text = "_" + text + "_";
            }
        }
        return text;
    }
}

