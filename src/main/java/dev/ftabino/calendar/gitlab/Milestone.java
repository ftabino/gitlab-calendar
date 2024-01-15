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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * A minimal representation of a GitLab milestone.
 *
 * @author Francesco A. Tabino
 */
public record Milestone(
        int id,
        String title,
        @JsonProperty("due_date")
        LocalDate dueDate,
        State state,
        @JsonProperty("web_url")
        String url) {


    /**
     * State of the milestone according to GitLab.
     *
     */
    enum State {
        @JsonProperty("active")
        ACTIVE,
        @JsonProperty("closed")
        CLOSED
    }

}
