package com.test.system.client;

/**
 * Jira REST API endpoints.
 */
public enum JiraApiEndpoint {
    
    MYSELF("/rest/api/3/myself"),
    CREATE_ISSUE("/rest/api/3/issue"),
    GET_ISSUE("/rest/api/3/issue/%s"),
    UPLOAD_ATTACHMENT("/rest/api/3/issue/%s/attachments"),
    CREATE_METADATA("/rest/api/3/issue/createmeta?projectKeys=%s&expand=projects.issuetypes");

    private final String path;

    JiraApiEndpoint(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String format(Object... args) {
        return String.format(path, args);
    }
}

