/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.deploymentslot;

import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.microsoft.azure.toolkits.appservice.AzureAppService;
import com.microsoft.azure.toolkits.appservice.entity.WebAppDeploymentSlotEntity;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlotCreator;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class WebAppDeploymentSlot implements IWebAppDeploymentSlot {

    private AzureAppService azureAppService;
    private WebAppDeploymentSlotEntity slot;

    private DeploymentSlot deploymentSlotClient;
    private AzureResourceManager azureClient;

    public WebAppDeploymentSlot(WebAppDeploymentSlotEntity deploymentSlot, AzureAppService azureAppService) {
        this.slot = deploymentSlot;
        this.azureAppService = azureAppService;
        this.azureClient = azureAppService.getAzureResourceManager();
    }

    @Override
    public IWebAppDeploymentSlotCreator create() {
        return new WebAppDeploymentSlotCreator();
    }

    @Override
    public void start() {
        getDeploymentSlotClient().start();
    }

    @Override
    public void stop() {
        getDeploymentSlotClient().stop();
    }

    @Override
    public void restart() {
        getDeploymentSlotClient().restart();
    }

    @Override
    public void delete() {
        getDeploymentSlotClient().parent().deploymentSlots().deleteByName(slot.getName());
    }

    @Override
    public void deploy(File file) {

    }

    @Override
    public void deploy(DeployType deployType, File file) {

    }

    @Override
    public boolean exists() {
        return getDeploymentSlotService(true) != null;
    }

    @Override
    public WebAppDeploymentSlotEntity entity() {
        this.slot = WebAppDeploymentSlotEntity.createFromServiceModel(getDeploymentSlotClient());
        return slot;
    }

    private com.azure.resourcemanager.appservice.models.DeploymentSlot getDeploymentSlotClient() {
        return getDeploymentSlotService(false);
    }

    private synchronized com.azure.resourcemanager.appservice.models.DeploymentSlot getDeploymentSlotService(boolean force) {
        if (deploymentSlotClient == null || force) {
            final WebApp webAppService = StringUtils.isNotEmpty(slot.getId()) ?
                    azureClient.webApps().getById(slot.getId().substring(0, slot.getId().indexOf("/slots"))) :
                    azureClient.webApps().getByResourceGroup(slot.getResourceGroup(), slot.getWebappName());
            this.deploymentSlotClient = StringUtils.isNotEmpty(slot.getId()) ? webAppService.deploymentSlots().getById(slot.getId()) :
                    webAppService.deploymentSlots().getByName(slot.getName());
            this.slot = WebAppDeploymentSlotEntity.createFromServiceModel(this.deploymentSlotClient);
        }
        return deploymentSlotClient;
    }

    @Getter
    public class WebAppDeploymentSlotCreator implements IWebAppDeploymentSlotCreator {
        public static final String CONFIGURATION_SOURCE_NEW = "new";
        public static final String CONFIGURATION_SOURCE_PARENT = "parent";
        private static final String CONFIGURATION_SOURCE_DOES_NOT_EXISTS = "Target slot configuration source does not exists in current web app";

        private String name;
        private String configurationSource = CONFIGURATION_SOURCE_PARENT;
        private Optional<Map<String, String>> appSettings = null;

        @Override
        public IWebAppDeploymentSlotCreator withName(String name) {
            this.name = name;
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withAppSettings(Map<String, String> appSettings) {
            this.appSettings = Optional.of(appSettings);
            return this;
        }

        @Override
        public IWebAppDeploymentSlotCreator withConfigurationSource(String configurationSource) {
            this.configurationSource = configurationSource;
            return this;
        }

        @Override
        public WebAppDeploymentSlot commit() {
            final DeploymentSlot.DefinitionStages.Blank blank = deploymentSlotClient.parent().deploymentSlots().define(getName());
            final DeploymentSlot.DefinitionStages.WithCreate withCreate;
            switch (StringUtils.lowerCase(configurationSource)) {
                case CONFIGURATION_SOURCE_NEW:
                    withCreate = blank.withBrandNewConfiguration();
                    break;
                case CONFIGURATION_SOURCE_PARENT:
                    withCreate = blank.withConfigurationFromParent();
                    break;
                default:
                    final DeploymentSlot deploymentSlot = deploymentSlotClient.parent().deploymentSlots().getByName(configurationSource);
                    if (deploymentSlot == null) {
                        throw new RuntimeException(CONFIGURATION_SOURCE_DOES_NOT_EXISTS);
                    }
                    withCreate = blank.withConfigurationFromDeploymentSlot(deploymentSlot);
                    break;
            }
            if (appSettings != null) {
                withCreate.withAppSettings(appSettings.get());
            }
            return WebAppDeploymentSlot.this;
        }
    }
}
