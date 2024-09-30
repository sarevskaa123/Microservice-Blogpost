package com.scalefocus.blogservice.service;

import com.scalefocus.blogservice.dto.ResponseTagDTO;

import java.util.List;


public interface TagService {
    ResponseTagDTO createTag(String tagName);
    List<ResponseTagDTO> getAllTags();
    void deleteTag(String tagName);
}
