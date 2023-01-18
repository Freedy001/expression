package com.freedy.expression.standard;

import lombok.Data;

@Data
public class AttachInfo {
    @CMDParameter(value = "-p", helpText = "target vm pid")
    private String pid;
    @CMDParameter(value = "-ap", helpText = "java agent jar file path")
    private String agentPath;
    @CMDParameter(value = "-arg", helpText = "java agent args")
    private String agentArg;
    @CMDParameter(value = "-n", helpText = "target application name")
    private String name;
}
