package com.alejandro.mtoexplorer.api.dto;

public record UnrecognizedDto(String master, String kind, String value, String ep, String sheet,
                              Integer firstRow, Integer occurrences) {
}
