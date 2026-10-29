package com.ikaansh.bookstore.dto;

import java.time.Instant;
import java.util.List;

/** The one error format returned by every failing endpoint. */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path, List<String> details) { }
