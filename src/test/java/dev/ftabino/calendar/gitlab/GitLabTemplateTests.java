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

import dev.ftabino.calendar.test.TestMethodResponseCreator;
import dev.ftabino.calendar.test.TestMethodResponseTestExecutionListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * Tests for {@link GitLabTemplate}.
 *
 * @author Francesco A. Tabino
 */
@RestClientTest
@TestExecutionListeners(mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS, listeners = TestMethodResponseTestExecutionListener.class)
@ContextConfiguration(classes = GitLabTemplateTests.TemplateConfiguration.class)
class GitLabTemplateTests {

    private final Project project = new Project(18, "Gitlab Calendar", false, false);

    @Autowired
    private GitLabTemplate gitLab;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private TestMethodResponseCreator testMethodResponse;

    @Autowired
    RestTemplateBuilder restTemplateBuilder;

    @Test
    void shouldGetProjects() {
        server.expect(requestTo("/api/v4/projects"))
              .andRespond(testMethodResponse);
        var page = gitLab.getProjects();
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().getFirst().name()).isEqualTo("Gitlab Calendar");
        server.verify();
    }

    @Test
    void shouldGetMilestones() {
        server.expect(requestTo("/api/v4/projects/18/milestones"))
              .andRespond(testMethodResponse);
        var page = gitLab.getMilestones(project, null);
        assertThat(page.getContent()).hasSize(2);
        server.verify();
        assertThat(page.getContent().getFirst().state()).isEqualTo(Milestone.State.CLOSED);
        assertThat(page.getContent().getLast().state()).isEqualTo(Milestone.State.ACTIVE);
    }

    /**
     * Test configuration for {@link GitLabTemplate}.
     */
    @Configuration
    static class TemplateConfiguration {

        @Bean
        GitLabTemplate gitLabTemplate(RestTemplateBuilder restTemplateBuilder) {
            return new GitLabTemplate("<GITLAB_URL>", "<GITLAB_TOKEN>", new RegexLinkParser(), restTemplateBuilder);
        }

    }

}
