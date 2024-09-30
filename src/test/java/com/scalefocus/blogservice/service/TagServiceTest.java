package com.scalefocus.blogservice.service;


import com.scalefocus.blogservice.dto.ResponseTagDTO;
import com.scalefocus.blogservice.exceptions.TagAlreadyExistsException;
import com.scalefocus.blogservice.exceptions.TagNotFoundException;
import com.scalefocus.blogservice.model.BlogPost;
import com.scalefocus.blogservice.model.Tag;
import com.scalefocus.blogservice.repository.TagRepository;
import com.scalefocus.blogservice.service.implementation.TagServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TagServiceTest {
    private TagRepository tagRepository;
    private TagServiceImpl tagService;

    @BeforeEach
    public void setUp() {
        tagRepository = mock(TagRepository.class);
        tagService = new TagServiceImpl(tagRepository);
    }

    @Test
    void testAddTag() {

        Tag savedTag = new Tag();
        savedTag.setTagId(1L);
        savedTag.setTagName("New Tag Test");
        savedTag.setTimeCreated(LocalDateTime.now());

        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

        ResponseTagDTO result = tagService.createTag("New Tag Test");

        verify(tagRepository, times(1)).save(any(Tag.class));
        Assertions.assertNotNull(result);
        Assertions.assertEquals("New Tag Test", result.tagName());
    }

    @Test
    void testAddTag_TagAlreadyExists() {
        Tag existingTag = new Tag();
        existingTag.setTagName("Existing Tag");

        when(tagRepository.findByTagName("Existing Tag")).thenReturn(Optional.of(existingTag));

        TagAlreadyExistsException exception = assertThrows(TagAlreadyExistsException.class, () -> tagService.createTag("Existing Tag"));

        Assertions.assertEquals("Tag with name Existing Tag already exists", exception.getMessage());
        verify(tagRepository, times(0)).save(any(Tag.class));
    }

    @Test
    void testGetAllTags() {
        Tag tag1 = Tag.builder().tagName("Tag1").build();
        Tag tag2 = Tag.builder().tagName("Tag2").build();

        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));

        List<ResponseTagDTO> tags = tagService.getAllTags();

        verify(tagRepository, times(1)).findAll();

        Assertions.assertEquals(2, tags.size());
        Assertions.assertEquals("Tag1", tags.get(0).tagName());
        Assertions.assertEquals("Tag2", tags.get(1).tagName());
    }

    @Test
    void testDeleteBlogPost() {
        BlogPost blogPost = BlogPost.builder().title("Test title").text("Test content").build();


        blogPost.setBlogId(1L);
        BlogPost blogPost2 = BlogPost.builder().title("Test title 2").text("Test content 2").build();

        blogPost2.setBlogId(2L);

        Tag tag = Tag.builder().tagName("Tag 1").build();
        tag.setBlogPosts(new ArrayList<>(Arrays.asList(blogPost, blogPost2)));
        blogPost.setTags(new ArrayList<>(Collections.singletonList(tag)));
        blogPost2.setTags(new ArrayList<>(Collections.singletonList(tag)));

        when(tagRepository.findByTagName("Tag 1")).thenReturn(Optional.of(tag));
        tagService.deleteTag("Tag 1");
        verify(tagRepository, times(1)).delete(tag);
        Assertions.assertFalse(blogPost.getTags().contains(tag));
        Assertions.assertFalse(blogPost2.getTags().contains(tag));
    }

    @Test
    void testDeleteBlogPostError() {
        when(tagRepository.findByTagName("Tag 1")).thenReturn(Optional.empty());
        Assertions.assertThrows(TagNotFoundException.class, () -> tagService.deleteTag("Tag 1"));
        verify(tagRepository, never()).delete(any(Tag.class));
    }

}
