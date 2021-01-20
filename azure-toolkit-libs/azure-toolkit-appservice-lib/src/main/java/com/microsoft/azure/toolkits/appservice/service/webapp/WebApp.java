/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.service.webapp;

import com.azure.core.exception.AzureException;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.AppServicePlan;
import com.azure.resourcemanager.appservice.models.WebApp.DefinitionStages;
import com.azure.resourcemanager.appservice.models.WebApp.Update;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.microsoft.azure.toolkits.appservice.AzureAppService;
import com.microsoft.azure.toolkits.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkits.appservice.entity.WebAppEntity;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.PublishingProfile;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.service.AbstractAppServiceCreator;
import com.microsoft.azure.toolkits.appservice.service.AbstractAppServiceUpdater;
import com.microsoft.azure.toolkits.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkits.appservice.service.IWebApp;
import com.microsoft.azure.toolkits.appservice.service.IWebAppDeploymentSlot;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class WebApp implements IWebApp {

    private WebAppEntity entity;
    private AzureAppService azureAppService;
    private AzureResourceManager azureClient;
    private com.azure.resourcemanager.appservice.models.WebApp webAppClient;

    public WebApp(WebAppEntity entity, AzureAppService azureAppService) {
        this.entity = entity;
        this.azureAppService = azureAppService;
        this.azureClient = azureAppService.getAzureResourceManager();
    }

    @Override
    public WebAppCreator create() {
        return new WebAppCreator();
    }

    @Override
    public void start() {
        getWebAppClient().start();
    }

    @Override
    public void stop() {
        getWebAppClient().stop();
    }

    @Override
    public void restart() {
        getWebAppClient().restart();
    }

    @Override
    public void delete() {
        azureClient.webApps().deleteById(getWebAppClient().id());
    }

    @Override
    public void deploy(File file) {
        final String fileExtensionName = FilenameUtils.getExtension(file.getName());
        switch (StringUtils.lowerCase(fileExtensionName)) {
            case "jar":
                deploy(DeployType.JAR, file);
                break;
            case "war":
                deploy(DeployType.WAR, file);
                break;
            case "ear":
                deploy(DeployType.EAR, file);
                break;
            case "zip":
                deploy(DeployType.ZIP, file);
                break;
            default:
                throw new AzureException("Unsupported file type, please set the deploy type.");
        }
    }

    @Override
    public WebAppEntity entity() {
        this.entity = WebAppEntity.createFromWebAppBase(getWebAppClient());
        return entity;
    }

    public void deploy(DeployType deployType, File target) {
        getWebAppClient().deploy(com.azure.resourcemanager.appservice.models.DeployType.fromString(deployType.getValue()), target);
    }

    @Override
    public boolean exists() {
        return getWebAppClient(true) != null;
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        final com.azure.resourcemanager.appservice.models.PublishingProfile publishingProfile = getWebAppClient().getPublishingProfile();
        return PublishingProfile.createFromServiceModel(publishingProfile);
    }

    @Override
    public Runtime getRuntime() {
        return null;
    }

    @Override
    public WebAppUpdater update() {
        return new WebAppUpdater();
    }

    @Override
    public IWebAppDeploymentSlot deploymentSlot(String slotName) {
        return null;
    }

    @Override
    public List<IWebAppDeploymentSlot> deploymentSlots() {
        return null;
    }

    private WebApp updateWebAppClient(com.azure.resourcemanager.appservice.models.WebApp webApp) {
        this.webAppClient = webApp;
        this.entity = WebAppEntity.createFromWebAppBase(webApp);
        return this;
    }

    private com.azure.resourcemanager.appservice.models.WebApp getWebAppClient() {
        return getWebAppClient(false);
    }

    synchronized com.azure.resourcemanager.appservice.models.WebApp getWebAppClient(boolean force) {
        if (webAppClient == null || force) {
            this.webAppClient = StringUtils.isEmpty(entity.getId()) ?
                    azureClient.webApps().getById(entity.getId()) :
                    azureClient.webApps().getByResourceGroup(entity.getResourceGroup(), entity.getName());
            this.entity = WebAppEntity.createFromWebAppBase(webAppClient);
        }
        return webAppClient;
    }


    public class WebAppCreator extends AbstractAppServiceCreator<WebApp> {

        @Override
        public WebApp commit() {
            final DefinitionStages.Blank blank = WebApp.this.azureClient.webApps().define(getName().get());
            final Runtime runtime = getRuntime();
            final AppServicePlan appServicePlan = null;
            final ResourceGroup resourceGroup = null;
            final DefinitionStages.WithCreate withCreate;
            switch (runtime.getOperatingSystem()) {
                case LINUX:
                    withCreate = createLinuxWebApp(blank, resourceGroup, appServicePlan, runtime);
                    break;
                case WINDOWS:
                    withCreate = createWindowsWebApp(blank, resourceGroup, appServicePlan, runtime);
                    break;
                case DOCKER:
                    withCreate = createDockerWebApp(blank, resourceGroup, appServicePlan, runtime);
                    break;
                default:
                    throw new RuntimeException();
            }
            if (getAppSettings() != null) {
                withCreate.withAppSettings(getAppSettings().get());
            }
            return new WebApp(WebAppEntity.createFromWebAppBase(withCreate.create()), azureAppService);
        }

        DefinitionStages.WithCreate createWindowsWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                        Runtime runtime) {
            return (DefinitionStages.WithCreate) blank.withExistingWindowsPlan(appServicePlan).withExistingResourceGroup(resourceGroup).withJavaVersion(null).withWebContainer(null);
        }

        DefinitionStages.WithCreate createLinuxWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                      Runtime runtime) {
            return blank.withExistingLinuxPlan(appServicePlan).withExistingResourceGroup(resourceGroup).withBuiltInImage(null);
        }

        DefinitionStages.WithCreate createDockerWebApp(DefinitionStages.Blank blank, ResourceGroup resourceGroup, AppServicePlan appServicePlan,
                                                       Runtime runtime) {
            return null;
        }
    }

    public class WebAppUpdater extends AbstractAppServiceUpdater<WebApp> {

        public static final String CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS = "Can not update the operation system for existing app service";

        @Override
        public WebApp commit() {
            Update update = getWebAppClient().update();
            if (getAppServicePlan() != null && getAppServicePlan().isPresent()) {
                update = updateAppServicePlan(update, getAppServicePlan().get());
            }
            if (getRuntime() != null && getRuntime().isPresent()) {
                update = updateRuntime(update, getRuntime().get());
            }
            if (getAppSettings() != null) {
                // todo: enhance app settings update, as now we could only add new app settings but can not remove existing values
                update.withAppSettings(getAppSettings().get());
            }
            WebApp.this.webAppClient = update.apply();
            return WebApp.this;
        }

        private Update updateAppServicePlan(Update update, AppServicePlanEntity newServicePlan) {
            final AppServicePlanEntity currentServicePlan = azureAppService.appServicePlan(getWebAppClient().appServicePlanId()).entity();
            if (Objects.equals(currentServicePlan, newServicePlan)) {
                return update;
            }
            final AppServicePlan newPlanServiceModel = null;
            if (newPlanServiceModel == null) {
                throw new RuntimeException("Target app service plan not exists");
            }
            return update.withExistingAppServicePlan(newPlanServiceModel);
        }

        private Update updateRuntime(Update update, Runtime newRuntime) {
            final Runtime current = WebApp.this.getRuntime();
            if (current.getOperatingSystem() != newRuntime.getOperatingSystem()) {
                throw new RuntimeException(CAN_NOT_UPDATE_EXISTING_APP_SERVICE_OS);
            }
            switch (newRuntime.getOperatingSystem()) {
                case LINUX:
                    // todo:
                    return update.withBuiltInImage(null);
                case WINDOWS:
                    return (Update) update.withJavaVersion(null).withWebContainer(null);
                case DOCKER:
                    return null;
                default:
                    throw new RuntimeException();
            }
        }
    }

}
