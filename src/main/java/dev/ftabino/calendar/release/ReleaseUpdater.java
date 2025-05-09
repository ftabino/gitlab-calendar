/*
 * Copyright (c) 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.ftabino.calendar.release;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code ReleaseUpdater} updates the known {@link Release Releases}.
 *
 * @author Francesco A. Tabino
 */
@Component
class ReleaseUpdater {

    private static final Logger log = LoggerFactory.getLogger(ReleaseUpdater.class);

    private final List<ReleaseScheduleSource> releaseScheduleSources;

    private final ReleaseRepository releaseRepository;

    ReleaseUpdater(List<ReleaseScheduleSource> releaseScheduleSources, ReleaseRepository releaseRepository) {
        this.releaseScheduleSources = releaseScheduleSources;
        this.releaseRepository = releaseRepository;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    void updateReleases() {
        log.info("[GITLAB] Updating releases");
        List<Release> releases =
                getReleaseSchedulesByProject().values()
                                              .stream()
                                              .flatMap((releaseSchedule) -> releaseSchedule.releases().stream())
                                              .toList();
        updateReleases(releases);
        log.info("[GITLAB] Releases updated");
    }

    private Map<String, ReleaseSchedule> getReleaseSchedulesByProject() {
        Map<String, ReleaseSchedule> schedulesByProject = new HashMap<>();
        this.releaseScheduleSources.stream()
                                   .map(ReleaseScheduleSource::get)
                                   .flatMap(List::stream)
                                   .forEach((releaseSchedule) -> collect(schedulesByProject, releaseSchedule));
        return schedulesByProject;
    }

    private void collect(Map<String, ReleaseSchedule> schedulesByProject, ReleaseSchedule schedule) {
        ReleaseSchedule existing = schedulesByProject.putIfAbsent(schedule.project(), schedule);
        if (existing != null) {
            existing.releases().addAll(schedule.releases());
        }
    }

    private void updateReleases(List<Release> releases) {
        this.releaseRepository.set(releases);
    }

}
