/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.springcloud.config;

import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;

import java.io.File;
import java.util.List;
import java.util.Map;

@Builder
@Getter
@Setter
public class SpringCloudDeploymentConfig {
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;
    private Boolean enablePersistentStorage;
    private Map<String, String> environment;
    private List<File> artifacts;

    public Boolean isEnablePersistentStorage() {
        return BooleanUtils.isTrue(enablePersistentStorage);
    }

    public ScaleSettings getScaleSettings() {
        return ScaleSettings.builder()
            .capacity(instanceCount)
            .cpu(cpu)
            .memoryInGB(memoryInGB)
            .build();
    }

    public String getJavaVersion() {
        return AzureSpringCloudConfigUtils.normalize(runtimeVersion);
    }
}
