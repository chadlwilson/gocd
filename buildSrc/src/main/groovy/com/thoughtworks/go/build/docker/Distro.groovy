/*
 * Copyright 2022 ThoughtWorks, Inc.
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

package com.thoughtworks.go.build.docker

import com.thoughtworks.go.build.OperatingSystem
import org.gradle.api.Project

enum Distro implements DistroBehavior {

  alpine{
    @Override
    OperatingSystem getOperatingSystem() {
      OperatingSystem.alpine_linux
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '3.13', releaseName: '3.13', eolDate: parseDate('2022-11-01'), continueToBuild: true),
        new DistroVersion(version: '3.14', releaseName: '3.14', eolDate: parseDate('2023-05-01')),
        new DistroVersion(version: '3.15', releaseName: '3.15', eolDate: parseDate('2023-11-01')),
        new DistroVersion(version: '3.16', releaseName: '3.16', eolDate: parseDate('2024-05-23')),
      ]
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      return [
        'adduser -D -u ${UID} -s /bin/bash -G root go'
      ]
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion distroVersion) {
      return [
        'apk --no-cache upgrade',
        // procps and gcompat are needed for tanuki wrapper
        'apk add --no-cache nss git mercurial subversion openssh-client bash curl procps gcompat'
      ]
    }
  },

  centos{
    @Override
    String getBaseImageRegistry(DistroVersion distroVersion) {
      distroVersion.version >= "8" ? "quay.io/centos" : super.baseImageRegistry
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion distroVersion) {
      def commands = ['yum update -y']

      String git = gitPackageFor(distroVersion)
      commands.add("yum install --assumeyes ${git} mercurial subversion openssh-clients bash unzip curl procps ${versionBelow8(distroVersion) ? 'sysvinit-tools coreutils' : 'procps-ng coreutils-single'}")

      if (versionBelow8(distroVersion)) {
        commands.add("cp /opt/rh/${git}/enable /etc/profile.d/${git}.sh")
      }

      commands.add('yum clean all')

      return commands
    }

    private boolean versionBelow8(DistroVersion distroVersion) {
      distroVersion.version < "8"
    }

    String gitPackageFor(DistroVersion distroVersion) {
      return versionBelow8(distroVersion) ? "rh-git218" : "git"
    }

    @Override
    Map<String, String> getEnvironmentVariables(DistroVersion distroVersion) {
      def vars = super.getEnvironmentVariables(distroVersion)

      if (versionBelow8(distroVersion)) {
        String git = gitPackageFor(distroVersion)
        return vars + [
          BASH_ENV: "/opt/rh/${git}/enable",
          ENV     : "/opt/rh/${git}/enable"
        ] as Map<String, String>
      } else {
        return vars
      }
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '7', releaseName: '7', eolDate: parseDate('2024-06-01'), installPrerequisitesCommands: ['yum install --assumeyes centos-release-scl-rh']),
        new DistroVersion(version: '8', releaseName: 'stream8', eolDate: parseDate('2024-05-31'), installPrerequisitesCommands: ['yum install --assumeyes glibc-langpack-en'])
      ]
    }
  },

  debian{
    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion distroVersion) {
      return [
        'apt-get update',
        'apt-get install -y git subversion mercurial openssh-client bash unzip curl locales procps sysvinit-utils coreutils',
        'apt-get autoclean',
        'echo \'en_US.UTF-8 UTF-8\' > /etc/locale.gen && /usr/sbin/locale-gen'
      ]
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '9', releaseName: 'stretch-slim', eolDate: parseDate('2022-06-30'), continueToBuild: true),
        // No EOL-LTS specified for buster release. Checkout https://wiki.debian.org/DebianReleases for more info
        new DistroVersion(version: '10', releaseName: 'buster-slim', eolDate: parseDate('2024-06-01')),
        new DistroVersion(version: '11', releaseName: 'bullseye-slim', eolDate: parseDate('2026-08-15')),
      ]
    }
  },

  ubuntu{
    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion distroVersion) {
      return debian.getInstallPrerequisitesCommands(distroVersion)
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: '18.04', releaseName: 'bionic', eolDate: parseDate('2023-04-01')),
        new DistroVersion(version: '20.04', releaseName: 'focal', eolDate: parseDate('2030-04-01')),
        new DistroVersion(version: '22.04', releaseName: 'jammy', eolDate: parseDate('2032-04-01')),
      ]
    }
  },

  docker{
    @Override
    OperatingSystem getOperatingSystem() {
      return alpine.getOperatingSystem()
    }

    @Override
    boolean isPrivilegedModeSupport() {
      return true
    }

    @Override
    List<DistroVersion> getSupportedVersions() {
      return [
        new DistroVersion(version: 'dind', releaseName: 'dind', eolDate: parseDate('2099-01-01'))
      ]
    }

    @Override
    List<String> getCreateUserAndGroupCommands() {
      return alpine.getCreateUserAndGroupCommands()
    }

    @Override
    List<String> getInstallPrerequisitesCommands(DistroVersion distroVersion) {
      return alpine.getInstallPrerequisitesCommands(distroVersion) +
        [
          'apk add --no-cache sudo',
        ]
    }

    @Override
    List<String> getInstallJavaCommands(Project project) {
      return [
        // Workaround for https://github.com/docker-library/docker/commit/75e26edc9ea7fff4aa3212fafa5966f4d6b00022
        // which causes a clash with glibc, which is installed later due to being needed for Tanuki Java Wrapper (and
        // thus used by the particular Adoptium builds we are using Alpine Adoptium builds seemingly can't co-exist happily).
        // We could avoid doing this once https://github.com/containerd/containerd/issues/5824 is fixed and makes its
        // way to the relevant docker:dind image version.
        'apk del --purge libc6-compat'
      ] + alpine.getInstallJavaCommands(project)
    }

    @Override
    Map<String, String> getEnvironmentVariables(DistroVersion distroVersion) {
      return alpine.getEnvironmentVariables(distroVersion)
    }
  }

  static Date parseDate(String date) {
    return Date.parse("yyyy-MM-dd", date)
  }

  GString projectName(DistroVersion distroVersion) {
    return "${name()}-${distroVersion.version}"
  }
}
