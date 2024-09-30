package com.scalefocus.blogservice.service.implementation;

import com.scalefocus.blogservice.dto.ResponseTagDTO;
import com.scalefocus.blogservice.exceptions.TagAlreadyExistsException;
import com.scalefocus.blogservice.exceptions.TagNotFoundException;
import com.scalefocus.blogservice.mapper.BlogPostMapper;
import com.scalefocus.blogservice.model.BlogPost;
import com.scalefocus.blogservice.model.Tag;
import com.scalefocus.blogservice.repository.TagRepository;
import com.scalefocus.blogservice.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

@RequiredArgsConstructor
@Service
public class TagServiceImpl implements TagService {
    private static final Logger logger = LoggerFactory.getLogger(TagServiceImpl.class);
    private static final BlogPostMapper blogPostMapper = BlogPostMapper.INSTANCE;
    private final TagRepository tagRepository;

    public ResponseTagDTO createTag(String tagName) {
        logger.info("Creating new tag: {}", tagName);
        tagRepository.findByTagName(tagName).ifPresent(s ->  {
            logger.error("Tag with name '{}' already exists", tagName);
            throw new TagAlreadyExistsException(tagName);
        });

        Tag newTag = tagRepository.save(Tag.builder().tagName(tagName).build());
        logger.info("Tag created with ID: {}", newTag.getTagId());
        return blogPostMapper.tagToTagDTO(newTag);
    }

    @Override
    public List<ResponseTagDTO> getAllTags() {
        logger.info("Fetching all tags");
        return blogPostMapper.tagsToTagDTOs(tagRepository.findAll());
    }

    @Override
    public void deleteTag(String tagName) {
        logger.info("Deleting tag: {}", tagName);
        Tag tag = tagRepository.findByTagName(tagName).orElseThrow(() ->  {
            logger.error("Tag with name '{}' not found", tagName);
            return new TagNotFoundException(tagName);
        });

        for (BlogPost blogPost : tag.getBlogPosts()) {
            blogPost.getTags().remove(tag);
        }

        tagRepository.delete(tag);
        logger.info("Tag '{}' deleted successfully", tagName);
    }
}
