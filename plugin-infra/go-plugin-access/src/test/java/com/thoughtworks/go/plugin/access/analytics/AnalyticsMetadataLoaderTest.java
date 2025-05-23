/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.access.analytics;

import com.thoughtworks.go.plugin.access.common.PluginMetadataChangeListener;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class AnalyticsMetadataLoaderTest {

    private AnalyticsExtension extension;
    private AnalyticsPluginInfoBuilder infoBuilder;
    private AnalyticsMetadataStore metadataStore;
    private PluginManager pluginManager;

    @BeforeEach
    public void setUp() {
        extension = mock(AnalyticsExtension.class);
        infoBuilder = mock(AnalyticsPluginInfoBuilder.class);
        metadataStore = mock(AnalyticsMetadataStore.class);
        pluginManager = mock(PluginManager.class);
    }

    @Test
    public void shouldBeAPluginChangeListener() {
        AnalyticsMetadataLoader analyticsMetadataLoader = new AnalyticsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        verify(pluginManager).addPluginChangeListener(eq(analyticsMetadataLoader));
    }

    @Test
    public void onPluginLoaded_shouldAddPluginInfoToMetadataStore() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AnalyticsMetadataLoader metadataLoader = new AnalyticsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        AnalyticsPluginInfo pluginInfo = new AnalyticsPluginInfo(descriptor, null, null, null);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(true);
        when(infoBuilder.pluginInfoFor(descriptor)).thenReturn(pluginInfo);

        metadataLoader.pluginLoaded(descriptor);

        verify(metadataStore).setPluginInfo(pluginInfo);
    }

    @Test
    public void onPluginLoad_shouldNotifyPluginMetadataLoadListeners() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AnalyticsMetadataLoader metadataLoader = new AnalyticsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        PluginMetadataChangeListener pluginMetadataChangeListener = mock(PluginMetadataChangeListener.class);
        AnalyticsPluginInfo pluginInfo = new AnalyticsPluginInfo(descriptor, null, null, null);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(true);
        when(infoBuilder.pluginInfoFor(descriptor)).thenReturn(pluginInfo);

        metadataLoader.registerListeners(pluginMetadataChangeListener);
        metadataLoader.pluginLoaded(descriptor);

        InOrder inOrder = inOrder(metadataStore, pluginMetadataChangeListener);

        inOrder.verify(metadataStore).setPluginInfo(pluginInfo);
        inOrder.verify(pluginMetadataChangeListener).onPluginMetadataCreate(descriptor.id());
    }

    @Test
    public void onPluginLoaded_shouldIgnoreNonAnalyticsPlugins() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AnalyticsMetadataLoader metadataLoader = new AnalyticsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(false);

        metadataLoader.pluginLoaded(descriptor);

        verifyNoMoreInteractions(infoBuilder);
        verifyNoMoreInteractions(metadataStore);
    }

    @Test
    public void onPluginUnloaded_shouldRemoveTheCorrespondingPluginInfoFromStore() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AnalyticsMetadataLoader metadataLoader = new AnalyticsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        AnalyticsPluginInfo pluginInfo = new AnalyticsPluginInfo(descriptor, null, null, null);

        metadataStore.setPluginInfo(pluginInfo);

        metadataLoader.pluginUnLoaded(descriptor);

        verify(metadataStore).remove(descriptor.id());
    }

    @Test
    public void onPluginUnLoaded_shouldNotifyPluginMetadataLoadListeners() {
        GoPluginDescriptor descriptor =  GoPluginDescriptor.builder().id("plugin1").build();
        AnalyticsMetadataLoader metadataLoader = new AnalyticsMetadataLoader(pluginManager, metadataStore, infoBuilder, extension);
        AnalyticsPluginInfo pluginInfo = new AnalyticsPluginInfo(descriptor, null, null, null);
        PluginMetadataChangeListener pluginMetadataChangeListener = mock(PluginMetadataChangeListener.class);

        when(extension.canHandlePlugin(descriptor.id())).thenReturn(true);

        metadataStore.setPluginInfo(pluginInfo);

        metadataLoader.registerListeners(pluginMetadataChangeListener);
        metadataLoader.pluginUnLoaded(descriptor);

        InOrder inOrder = inOrder(metadataStore, pluginMetadataChangeListener);

        inOrder.verify(metadataStore).remove(descriptor.id());
        inOrder.verify(pluginMetadataChangeListener).onPluginMetadataRemove(descriptor.id());
    }
}
