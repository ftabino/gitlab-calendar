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

/**
 * Operations that can be performed against the GitLab API.
 *
 * @author Francesco A. Tabino
 */
sealed interface GitLabOperations permits GitLabTemplate {

    /**
     * Get a list of all visible projects across GitLab for the authenticated token
     *
     * @return the page of projects
     */
    Page<Project> getProjects();

    /**
     * Returns the milestones in the given {@code project}.
     *
     * @param project         the project
     * @param earlierResponse the first page of an earlier response that can be used to
     *                        perform conditional requests, or {@code null}.
     * @return the page of milestones
     */
    Page<Milestone> getMilestones(Project project, Page<Milestone> earlierResponse);

}
