package com.scalefocus.blogservice.dto;

import java.time.LocalDateTime;

public record ResponseTagDTO(Long tagId, String tagName, LocalDateTime timeCreated) {
}
