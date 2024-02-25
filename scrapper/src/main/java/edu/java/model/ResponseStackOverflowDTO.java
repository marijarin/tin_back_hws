package edu.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record ResponseStackOverflowDTO(
    List<Item> items
) {
    public record Item(@JsonProperty("creation_date") OffsetDateTime creationDate) {
    }
}
