/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.config.service.kubernetes;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.kubernetes.client.Watcher;
import org.eclipse.jkube.kit.common.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.config.service.kubernetes.KubernetesClientUtil.doDeleteAndWait;

@SuppressWarnings("unused")
@EnableKubernetesMockClient(crud = true)
class KubernetesClientUtilTest {

  private KubernetesClient kubernetesClient;
  private KubernetesMockServer mockServer;

  @Test
  void doDeleteAndWait_withExistingResource_shouldDeleteAndReachWaitLimit() {
    // Given
    final CustomResourceDefinitionContext context = new CustomResourceDefinitionContext.Builder()
      .withGroup("org.eclipse.jkube")
      .withVersion("v1beta1")
      .withPlural("crds")
      .withKind("JKubeCustomResource")
      .build();
    mockServer.expectCustomResource(context);
    final GenericKubernetesResource existingResource = kubernetesClient
      .genericKubernetesResources("org.eclipse.jkube/v1beta1", "JKubeCustomResource")
      .inNamespace("namespace")
      .resource(new GenericKubernetesResourceBuilder()
        .withApiVersion("org.eclipse.jkube/v1beta1")
        .withKind("JKubeCustomResource")
        .withNewMetadata().withName("name").endMetadata()
        .build())
      .create();
    // When
    doDeleteAndWait(kubernetesClient, existingResource, "namespace",  2L);
    // Then
    assertThat(kubernetesClient.genericKubernetesResources(context).inNamespace("namespace").withName("name").get())
      .isNull();
  }

  @Test
  void applicableNamespace_whenNamespaceProvidedViaConfiguration_shouldReturnProvidedNamespace() {
    // Given
    String namespaceViaPluginConfigOrJkubeNamespaceProperty = "ns1";

    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(null, namespaceViaPluginConfigOrJkubeNamespaceProperty, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo(namespaceViaPluginConfigOrJkubeNamespaceProperty);
  }

  @Test
  void applicableNamespace_whenNamespaceInResourceMetadata_shouldReturnProvidedNamespace() {
    // Given
    Pod pod = new PodBuilder()
        .withNewMetadata().withNamespace("test").endMetadata()
        .build();

    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(pod, null, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo("test");
  }

  @Test
  void applicableNamespace_whenNamespaceProvidedViaKubernetesClient_shouldReturnProvidedNamespace() {
    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(null, null, null,
      ClusterConfiguration.from(kubernetesClient.getConfiguration()).build());

    // Then
    assertThat(resolvedNamespace).isEqualTo(kubernetesClient.getNamespace());
  }

  @Test
  void applicableNamespace_whenNamespaceProvidedViaResourceConfiguration_shouldReturnProvidedNamespace() {
    // Given
    ResourceConfig resourceConfig = ResourceConfig.builder().namespace("ns1").build();

    // When
    String resolvedNamespace = KubernetesClientUtil.applicableNamespace(null, null, resourceConfig, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo("ns1");
  }

  @Test
  void resolveFallbackNamespace_whenNamespaceProvidedViaResourceConfiguration_shouldReturnProvidedNamespace() {
    // Given
    ResourceConfig resourceConfig = ResourceConfig.builder().namespace("ns1").build();

    // When
    String resolvedNamespace = KubernetesClientUtil.resolveFallbackNamespace(resourceConfig, null);

    // Then
    assertThat(resolvedNamespace).isEqualTo(resourceConfig.getNamespace());
  }

  @Test
  void resolveFallbackNamespace_whenNamespaceProvidedViaKubernetesClient_shouldReturnProvidedNamespace() {
    // When
    String resolvedNamespace = KubernetesClientUtil
      .resolveFallbackNamespace(null, ClusterConfiguration.from(kubernetesClient.getConfiguration()).build());

    // Then
    assertThat(resolvedNamespace).isEqualTo(kubernetesClient.getNamespace());
  }

  @Test
  void getPodStatusMessagePostfix_whenActionIsDeleted_shouldReturnPodDeletedMessage() {
    // Given
    Watcher.Action action = Watcher.Action.DELETED;

    // When
    String messagePostfix = KubernetesClientUtil.getPodStatusMessagePostfix(action);

    // Then
    assertThat(messagePostfix).isEqualTo(": Pod Deleted");
  }

  @Test
  void getPodStatusMessagePostfix_whenActionIsError_shouldReturnErrorMessage() {
    // Given
    Watcher.Action action = Watcher.Action.ERROR;

    // When
    String messagePostfix = KubernetesClientUtil.getPodStatusMessagePostfix(action);

    // Then
    assertThat(messagePostfix).isEqualTo(": Error");
  }

  @Test
  void getPodCondition_whenPodConditionIsReadyAndTrue_shouldReturnReady() {
    // Given
    Pod pod = new PodBuilder()
      .withNewStatus()
      .addNewCondition()
      .withType("ready")
      .withStatus("True")
      .endCondition()
      .endStatus()
      .build();

    // When
    String condition = KubernetesClientUtil.getPodCondition(pod);

    // Then
    assertThat(condition).isEqualTo("ready");
  }

  @Test
  void getPodCondition_whenPodConditionIsReadyAndFalse_shouldReturnEmptyString() {
    // Given
    Pod pod = new PodBuilder()
      .withNewStatus()
      .addNewCondition()
      .withType("ready")
      .withStatus("False")
      .endCondition()
      .endStatus()
      .build();

    // When
    String condition = KubernetesClientUtil.getPodCondition(pod);

    // Then
    assertThat(condition).isEmpty();
  }

  @Test
  void getPodCondition_whenPodConditionIsNotReady_shouldReturnEmptyString() {
    // Given
    Pod pod = new PodBuilder()
      .withNewStatus()
      .addNewCondition()
      .withType("notready")
      .withStatus("True")
      .endCondition()
      .endStatus()
      .build();

    // When
    String condition = KubernetesClientUtil.getPodCondition(pod);

    // Then
    assertThat(condition).isEmpty();
  }

}
