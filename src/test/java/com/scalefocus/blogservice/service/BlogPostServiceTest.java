package com.scalefocus.blogservice.service;

import com.scalefocus.blogservice.dto.BlogPostDTO;
import com.scalefocus.blogservice.dto.CreateBlogPostDTO;
import com.scalefocus.blogservice.exceptions.BlogPostCreationException;
import com.scalefocus.blogservice.exceptions.BlogPostNotFoundException;
import com.scalefocus.blogservice.exceptions.TagNotFoundException;
import com.scalefocus.blogservice.mapper.BlogPostMapper;
import com.scalefocus.blogservice.model.BlogPost;
import com.scalefocus.blogservice.model.Tag;
import com.scalefocus.blogservice.repository.BlogPostRepository;
import com.scalefocus.blogservice.repository.TagRepository;
import com.scalefocus.blogservice.service.implementation.BlogPostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Assertions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BlogPostServiceTest {

    private BlogPostRepository blogPostRepository;
    private TagRepository tagRepository;
    private BlogPostServiceImpl blogPostService = new BlogPostServiceImpl(null, null);
    private final BlogPostMapper blogPostMapper = BlogPostMapper.INSTANCE;


    @BeforeEach
    public void setUp() {
        blogPostRepository = mock(BlogPostRepository.class);
        tagRepository = mock(TagRepository.class);
        blogPostService = new BlogPostServiceImpl(blogPostRepository, tagRepository);
    }

    @Test
    void testGetBlogPostById() {
        BlogPost blogPost = BlogPost.builder()
                .title("Test Title")
                .text("Test Content")
                .build();
        blogPost.setBlogId(1L);

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));

        BlogPostDTO result = blogPostService.getBlogPostById(1L);

        Assertions.assertEquals("Test Title", result.getTitle());
        Assertions.assertEquals("Test Content", result.getText());
    }

    @Test
    void testGetBlogPostById_NotFound() {
        when(blogPostRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(BlogPostNotFoundException.class, () -> blogPostService.getBlogPostById(1L));
    }

    @Test
    void testGetFilteredBlogPosts_WithTagOnly() {
        String tag = "Tag1";
        Pageable pageable = PageRequest.of(0, 10);
        BlogPost blogPost = BlogPost.builder().title("Test title 1").text("Test content 1").build();
        blogPost.setTags(List.of(Tag.builder().tagName(tag).build()));
        Page<BlogPost> blogPostPage = new PageImpl<>(List.of(blogPost));

        when(blogPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(blogPostPage);

        Page<BlogPostDTO> result = blogPostService.getFilteredBlogPosts(tag, null, null, pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("Test title 1", result.getContent().getFirst().getTitle());
    }

    @Test
    void testGetFilteredBlogPosts_WithParityOnly() {
        String parity = "even";
        Pageable pageable = PageRequest.of(0, 10);
        BlogPost blogPost = BlogPost.builder().title("Test title 1").text("Test content 1").build();
        blogPost.setTags(List.of(Tag.builder().tagName("Tag1").build(), Tag.builder().tagName("Tag2").build()));
        Page<BlogPost> blogPostPage = new PageImpl<>(List.of(blogPost));

        when(blogPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(blogPostPage);

        Page<BlogPostDTO> result = blogPostService.getFilteredBlogPosts(null, parity, null, pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("Test title 1", result.getContent().getFirst().getTitle());
    }

    @Test
    void testGetFilteredBlogPosts_WithSummaryLimitOnly() {
        int summaryLimit = 10;
        Pageable pageable = PageRequest.of(0, 10);
        BlogPost blogPost = BlogPost.builder().title("Test title 1").text("Test content that is long").build();
        Page<BlogPost> blogPostPage = new PageImpl<>(List.of(blogPost));

        when(blogPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(blogPostPage);

        Page<BlogPostDTO> result = blogPostService.getFilteredBlogPosts(null, null, summaryLimit, pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("Test c ...", result.getContent().getFirst().getText());
    }

    @Test
    void testGetFilteredBlogPosts_WithParityAndSummaryLimit() {
        String parity = "even";
        int summaryLimit = 10;
        Pageable pageable = PageRequest.of(0, 10);
        BlogPost blogPost = BlogPost.builder().title("Test title 1").text("Test content that is long").build();
        blogPost.setTags(List.of(Tag.builder().tagName("Tag1").build(), Tag.builder().tagName("Tag2").build()));
        Page<BlogPost> blogPostPage = new PageImpl<>(List.of(blogPost));

        when(blogPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(blogPostPage);

        Page<BlogPostDTO> result = blogPostService.getFilteredBlogPosts(null, parity, summaryLimit, pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("Test c ...", result.getContent().getFirst().getText());
    }

    @Test
    void testGetFilteredBlogPosts_WithTagAndSummaryLimit() {
        String tag = "Tag1";
        int summaryLimit = 10;
        Pageable pageable = PageRequest.of(0, 10);
        BlogPost blogPost = BlogPost.builder().title("Test title 1").text("Test content that is long").build();
        blogPost.setTags(List.of(Tag.builder().tagName(tag).build()));
        Page<BlogPost> blogPostPage = new PageImpl<>(List.of(blogPost));

        when(blogPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(blogPostPage);

        Page<BlogPostDTO> result = blogPostService.getFilteredBlogPosts(tag, null, summaryLimit, pageable);

        Assertions.assertEquals(1, result.getTotalElements());
        Assertions.assertEquals("Test c ...", result.getContent().getFirst().getText());
    }


    @Test
    void testGetFilteredBlogPosts_NoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        BlogPost blogPost1 = BlogPost.builder().title("Test title 1").text("Test content 1").build();
        BlogPost blogPost2 = BlogPost.builder().title("Test title 2").text("Test content 2").build();
        Page<BlogPost> blogPostPage = new PageImpl<>(List.of(blogPost1, blogPost2));

        when(blogPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(blogPostPage);

        Page<BlogPostDTO> result = blogPostService.getFilteredBlogPosts(null, null, null, pageable);

        Assertions.assertEquals(2, result.getTotalElements());
        Assertions.assertEquals("Test title 1", result.getContent().get(0).getTitle());
        Assertions.assertEquals("Test title 2", result.getContent().get(1).getTitle());
    }

    @Test
    void testAddBlogPost() {
        CreateBlogPostDTO createBlogPostDTO = new CreateBlogPostDTO("Test title", "Test content", new ArrayList<>());
        BlogPost blogPost = blogPostMapper.createBlogPostDTOToBlogPost(createBlogPostDTO);
        blogPost.setAuthor("testuser");

        when(blogPostRepository.save(any(BlogPost.class))).thenReturn(blogPost);

        BlogPostDTO result = blogPostService.addBlogPost(createBlogPostDTO,"testuser");
        verify(blogPostRepository, times(1)).save(any(BlogPost.class));

        BlogPost savedBlogPost = blogPostRepository.save(blogPost);
        Assertions.assertNotNull(savedBlogPost);

        Assertions.assertEquals(createBlogPostDTO.title(), savedBlogPost.getTitle());
        Assertions.assertEquals(createBlogPostDTO.text(), savedBlogPost.getText());
        Assertions.assertEquals("testuser", savedBlogPost.getAuthor());
        Assertions.assertNotNull(result);
    }

    @Test
    void testAddBlogPostFailure() {
        CreateBlogPostDTO blogPostDTO = new CreateBlogPostDTO("Test title", "Test content", new ArrayList<>());

        when(blogPostRepository.save(any(BlogPost.class))).thenThrow(new RuntimeException("DB error"));

        BlogPostCreationException exception = Assertions.assertThrows(BlogPostCreationException.class, () -> blogPostService.addBlogPost(blogPostDTO, "testuser"));

        Assertions.assertEquals("Failed to create blog post", exception.getMessage());
    }

    @Test
    void testGetBlogPosts() {
        BlogPost blogPost = BlogPost.builder().title("Test title").text("Test content").build();

        BlogPost blogPost2 = BlogPost.builder().title("Test title2").text("Test content2").build();

        BlogPost blogPost3 = BlogPost.builder().title("Test title3").text("Test content3").build();


        when(blogPostRepository.findAll()).thenReturn(Arrays.asList(blogPost, blogPost2, blogPost3));
        List<BlogPostDTO> blogPosts = blogPostService.getBlogPosts();
        Assertions.assertEquals(3, blogPosts.size());
    }

    @Test
    void testUpdateBlogPost() {
        BlogPost blogPost = BlogPost.builder().title("Test title").text("Test content").build();

        blogPost.setBlogId(1L);

        BlogPostDTO updatedBlogPostDTO = new BlogPostDTO(1L, "Updated title", "Updated content", Collections.emptyList());

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));

        BlogPostDTO result = blogPostService.updateBlogPost(1L, updatedBlogPostDTO);
        Assertions.assertNotNull(result);
        verify(blogPostRepository, times(1)).save(blogPost);

        Assertions.assertEquals("Updated title", result.getTitle());
        Assertions.assertEquals("Updated content", result.getText());

    }

    @Test
    void testAddTagToBlogPost() {
        BlogPost blogPost = BlogPost.builder()
                .title("Test title")
                .text("Test content")
                .build();
        blogPost.setBlogId(1L);
        blogPost.setTags(new ArrayList<>());

        Tag newTag = Tag.builder().tagName("New Tag").build();

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));
        when(tagRepository.findByTagName("New Tag")).thenReturn(Optional.of(newTag));

        BlogPostDTO result = blogPostService.addTagToBlogPost(1L, "New Tag");

        verify(blogPostRepository, times(1)).save(blogPost);
        Assertions.assertTrue(blogPost.getTags().contains(newTag));
        Assertions.assertEquals("Test title", result.getTitle());
        Assertions.assertEquals("Test content", result.getText());
        Assertions.assertTrue(result.getTags().stream().anyMatch(tagDTO -> tagDTO.tagName().equals("New Tag")));
    }

    @Test
    void testRemoveTagFromBlogPost() {
        BlogPost blogPost = BlogPost.builder()
                .title("Test title")
                .text("Test content")
                .build();
        blogPost.setBlogId(1L);

        Tag tag1 = Tag.builder().tagName("Tag1").build();
        Tag tag2 = Tag.builder().tagName("Tag2").build();

        List<Tag> tags = new ArrayList<>(Arrays.asList(tag1, tag2));
        blogPost.setTags(tags);

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));
        when(tagRepository.findByTagName("Tag1")).thenReturn(Optional.of(tag1));

        BlogPostDTO result = blogPostService.removeTagFromBlogPost(1L, "Tag1");

        verify(blogPostRepository, times(1)).save(blogPost);
        Assertions.assertFalse(blogPost.getTags().contains(tag1));
        Assertions.assertEquals("Test title", result.getTitle());
        Assertions.assertEquals("Test content", result.getText());
        Assertions.assertEquals(1, result.getTags().size());
        Assertions.assertTrue(result.getTags().stream().noneMatch(tagDTO -> tagDTO.tagName().equals("Tag1")));
    }


    @Test
    void testDeleteBlogPost() {
        Tag tag1 = Tag.builder().tagName("Tag 1").build();
        Tag tag2 = Tag.builder().tagName("Tag 2").build();
        BlogPost blogPost = BlogPost.builder().title("Test title").text("Test content").build();

        blogPost.setBlogId(1L);
        blogPost.setTags(new ArrayList<>(Arrays.asList(tag1, tag2)));
        tag1.setBlogPosts(new ArrayList<>(Collections.singletonList(blogPost)));
        tag2.setBlogPosts(new ArrayList<>(Collections.singletonList(blogPost)));


        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));
        blogPostService.deleteBlogPost(1L);
        verify(blogPostRepository, times(1)).delete(blogPost);
        Assertions.assertFalse(tag1.getBlogPosts().contains(blogPost));
        Assertions.assertFalse(tag2.getBlogPosts().contains(blogPost));
    }

    @Test
    void testDeleteBlogPostError() {
        when(blogPostRepository.findById(1L)).thenReturn(Optional.empty());
        Assertions.assertThrows(BlogPostNotFoundException.class, () -> blogPostService.deleteBlogPost(1L));
        verify(blogPostRepository, never()).delete(any(BlogPost.class));
    }

    @Test
    void testGetFilteredBlogPosts_InvalidParity() {
        String invalidParity = "invalid";
        Pageable pageable = PageRequest.of(0, 10);

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> blogPostService.getFilteredBlogPosts(null, invalidParity, null, pageable));

        Assertions.assertEquals("Invalid value for parity. It must be 'even' or 'odd'.", exception.getMessage());
    }

    @Test
    void testUpdateBlogPost_NoChanges() {
        BlogPost blogPost = BlogPost.builder().title("Test title").text("Test content").build();
        blogPost.setBlogId(1L);

        BlogPostDTO identicalBlogPostDTO = new BlogPostDTO(1L, "Test title", "Test content", Collections.emptyList());

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));

        BlogPostDTO result = blogPostService.updateBlogPost(1L, identicalBlogPostDTO);

        Assertions.assertEquals("Test title", result.getTitle());
        Assertions.assertEquals("Test content", result.getText());
        verify(blogPostRepository, never()).save(any(BlogPost.class));
    }

    @Test
    void testAddNonExistingTagToBlogPost() {
        BlogPost blogPost = BlogPost.builder()
                .title("Test title")
                .text("Test content")
                .tags(new ArrayList<>())
                .build();
        blogPost.setBlogId(1L);

        String newTagName = "NewTag";

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));
        when(tagRepository.findByTagName(newTagName)).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(i -> i.getArguments()[0]);
        when(blogPostRepository.save(blogPost)).thenReturn(blogPost);

        BlogPostDTO result = blogPostService.addTagToBlogPost(1L, newTagName);

        verify(tagRepository, times(1)).save(any(Tag.class));
        Assertions.assertTrue(result.getTags().stream().anyMatch(tagDTO -> tagDTO.tagName().equals(newTagName)));
    }

    @Test
    void testRemoveNonExistingTagFromBlogPost() {
        BlogPost blogPost = BlogPost.builder().title("Test title").text("Test content").build();
        blogPost.setBlogId(1L);

        String nonExistingTagName = "NonExistingTag";

        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(blogPost));
        when(tagRepository.findByTagName(nonExistingTagName)).thenReturn(Optional.empty());

        TagNotFoundException exception = Assertions.assertThrows(TagNotFoundException.class,
                () -> blogPostService.removeTagFromBlogPost(1L, nonExistingTagName));

        Assertions.assertEquals("Tag with name " + nonExistingTagName + " not found", exception.getMessage());
    }

    @Test
    void testAddTagToNonExistingBlogPost() {
        String tagName = "NewTag";
        Long nonExistingBlogPostId = 999L;

        when(blogPostRepository.findById(nonExistingBlogPostId)).thenReturn(Optional.empty());

        BlogPostNotFoundException exception = Assertions.assertThrows(
                BlogPostNotFoundException.class,
                () -> blogPostService.addTagToBlogPost(nonExistingBlogPostId, tagName)
        );

        Assertions.assertEquals("Blogpost with id " + nonExistingBlogPostId + " not found", exception.getMessage());
    }

    @Test
    void testRemoveTagFromNonExistingBlogPost() {
        String tagName = "Tag1";
        Long nonExistingBlogPostId = 999L;

        when(blogPostRepository.findById(nonExistingBlogPostId)).thenReturn(Optional.empty());

        BlogPostNotFoundException exception = Assertions.assertThrows(
                BlogPostNotFoundException.class,
                () -> blogPostService.removeTagFromBlogPost(nonExistingBlogPostId, tagName)
        );

        Assertions.assertEquals("Blogpost with id " + nonExistingBlogPostId + " not found", exception.getMessage());
    }

    @Test
    void testUpdateNonExistingBlogPost() {
        Long nonExistingBlogPostId = 999L;
        BlogPostDTO updatedBlogPostDTO = new BlogPostDTO(nonExistingBlogPostId, "Updated title", "Updated content", Collections.emptyList());

        when(blogPostRepository.findById(nonExistingBlogPostId)).thenReturn(Optional.empty());

        BlogPostNotFoundException exception = Assertions.assertThrows(
                BlogPostNotFoundException.class,
                () -> blogPostService.updateBlogPost(nonExistingBlogPostId, updatedBlogPostDTO)
        );

        Assertions.assertEquals("Blogpost with id " + nonExistingBlogPostId + " not found", exception.getMessage());
    }

    @Test
    void testGetFilteredBlogPosts_InvalidSummaryLimit() {
        int invalidSummaryLimit = 4;
        Pageable pageable = PageRequest.of(0, 10);

        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> blogPostService.getFilteredBlogPosts(null, null, invalidSummaryLimit, pageable)
        );

        Assertions.assertEquals("summaryLimit must be at least 5", exception.getMessage());
    }

}
