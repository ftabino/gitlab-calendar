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

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Central class for interacting with GitLab's REST API.
 *
 * @author Francesco A. Tabino
 */
final class GitLabTemplate implements GitLabOperations {

    private final RestClient restClient;

    private final LinkParser linkParser;

    /**
     * Creates a new {@code GitLabTemplate} that will use the given {@code url} as a root URL,
     * and the given {@code linkParser} to parse links from responses' {@code Link} header.
     * It will use a {@link RestClient} created from the given {@code builder}.
     *
     * @param url        the root uri
     * @param linkParser the link parser
     * @param builder    the restClient builder
     */
    GitLabTemplate(String url, String token, LinkParser linkParser, RestClient.Builder builder) {
        if (StringUtils.hasText(url)) {
            builder.baseUrl(url);
        }
        restClient = builder.defaultHeader("Authorization", "Bearer " + token)
                            .build();

        this.linkParser = linkParser;
    }

    @Override
    public Page<Project> getProjects() {
        var builder = UriComponentsBuilder.fromUriString("/api/v4/projects");
        var uri = builder.encode().toUriString();
        return new PageSupplier<>(uri, null, Project[].class).get();
    }

    @Override
    public Page<Milestone> getMilestones(Project project, Page<Milestone> earlierResponse) {
        var builder = UriComponentsBuilder.fromUriString("/api/v4/projects/{id}/milestones");
        var uri = builder.encode()
                         .buildAndExpand(project.id())
                         .toUriString();
        return new PageSupplier<>(uri, earlierResponse, Milestone[].class).get();
    }

    private class PageSupplier<T> implements Supplier<Page<T>> {

        private final String url;
        private final Page<T> earlierResponse;
        private final Class<T[]> type;
        private Page<T> page;

        PageSupplier(String url, Page<T> earlierResponse, Class<T[]> type) {
            this.url = url;
            this.earlierResponse = earlierResponse;
            this.type = type;
        }

        @Override
        public Page<T> get() {
            if (page == null) {
                page = getPage(url, type);
            }
            return page;
        }

        private Page<T> getPage(String url, Class<T[]> type) {
            if (!StringUtils.hasText(url)) {
                return null;
            }

            ResponseEntity<T[]> response =
                    restClient.get()
                              .uri(url)
                              .headers(httpHeaders -> {
                                  if (earlierResponse != null && (earlierResponse.next() != null || earlierResponse
                                          .getContent()
                                          .size() != 100)) {
                                      httpHeaders.setIfNoneMatch(earlierResponse.getEtag());
                                  }
                              })
                              .retrieve()
                              .toEntity(type);

            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED && earlierResponse != null) {
                Page<T> nextEarlierResponse = earlierResponse.next();
                return new StandardPage<>(earlierResponse.getContent(), url, earlierResponse.getEtag(),
                        new PageSupplier<>((nextEarlierResponse != null) ? nextEarlierResponse.getUrl() : null, nextEarlierResponse, type));
            } else {
                return new StandardPage<>(List.of(Objects.requireNonNull(response.getBody())), url,
                        response.getHeaders().getETag(),
                        new PageSupplier<>(getNextUrl(response), null, type));
            }

        }

        private String getNextUrl(ResponseEntity<?> response) {
            return GitLabTemplate.this.linkParser.parse(response.getHeaders().getFirst("Link")).get("next");
        }

    }

}
