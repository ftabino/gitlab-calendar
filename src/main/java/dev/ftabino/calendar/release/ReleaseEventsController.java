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

import dev.ftabino.calendar.release.Release.Status;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Controller for exposing {@link Release Releases} as Full Calendar events.
 *
 * @author Francesco A. Tabino
 */
@RestController
@RequestMapping("/releases")
class ReleaseEventsController {

    private final ReleaseRepository releaseRepository;

    ReleaseEventsController(ReleaseRepository releaseRepository) {
        this.releaseRepository = releaseRepository;
    }

    private static Function<Release, Map<String, Object>> releaseEvent() {
        return (release) -> {
            Map<String, Object> event = new HashMap<>();
            event.put("title", release.project() + " " + release.name());
            event.put("allDay", true);
            event.put("start", release.date());
            if (release.url() != null) {
                event.put("url", release.url());
            }
            if (release.status() == Status.CLOSED) {
                event.put("backgroundColor", "#6db33f");
            } else if (release.isOverdue()) {
                event.put("backgroundColor", "#d14");
            }
            return event;
        };
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    List<Map<String, Object>> releases(@RequestParam String start, @RequestParam String end) {
        LocalDate startDate = LocalDate.parse(start, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        LocalDate endDate = LocalDate.parse(end, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return this.releaseRepository.findAllInPeriod(startDate, endDate)
                                     .stream()
                                     .map(releaseEvent())
                                     .toList();
    }

    @ExceptionHandler
    ResponseEntity<String> handleDateParseException(ParseException exc) {
        return ResponseEntity.badRequest().body(exc.getMessage());
    }

}
