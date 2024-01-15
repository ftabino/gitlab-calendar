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

package dev.ftabino.calendar.gitlab;

import dev.ftabino.calendar.release.Release;
import dev.ftabino.calendar.release.ReleaseSchedule;
import dev.ftabino.calendar.release.ReleaseScheduleSource;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ReleaseScheduleSource} for projects managed on GitLab.
 *
 * @author Francesco A. Tabino
 */
class GitLabReleaseScheduleSource implements ReleaseScheduleSource {

    private final Map<Integer, Page<Milestone>> earlierMilestones = new HashMap<>();

    private final GitLabOperations gitLab;

    public GitLabReleaseScheduleSource(GitLabOperations gitLab) {
        this.gitLab = gitLab;
    }

    @Override
    public List<ReleaseSchedule> get() {
        return collectContent(gitLab.getProjects())
                .stream()
                .map(this::createReleaseSchedule)
                .toList();
    }

    private ReleaseSchedule createReleaseSchedule(Project project) {
        Page<Milestone> page = gitLab.getMilestones(project, earlierMilestones.get(project.id()));
        earlierMilestones.put(project.id(), page);
        List<Release> releases = getReleases(project, page);
        return new ReleaseSchedule(project.name(), releases);
    }

    private List<Release> getReleases(Project project, Page<Milestone> page) {
        return collectContent(page)
                .stream()
                .filter(this::hasReleaseDate)
                .map((Milestone milestone) -> createRelease(project, milestone))
                .toList();
    }

    private <T> List<T> collectContent(Page<T> page) {
        List<T> content = new ArrayList<>();
        while (page != null) {
            content.addAll(page.getContent());
            page = page.next();
        }
        return content;
    }

    private boolean hasReleaseDate(Milestone milestone) {
        return milestone.dueDate() != null;
    }

    private Release createRelease(Project project, Milestone milestone) {
        try {
            return new Release(project.name(), milestone.title(),
                    milestone.dueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    getStatus(milestone),
                    URI.create(milestone.url()).toURL());
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Release.Status getStatus(Milestone milestone) {
        return (milestone.state() == Milestone.State.ACTIVE) ? Release.Status.OPEN : Release.Status.CLOSED;
    }

}
