package com.scalefocus.blogservice.service.implementation;

import com.scalefocus.blogservice.dto.BlogPostDTO;
import com.scalefocus.blogservice.dto.CreateBlogPostDTO;
import com.scalefocus.blogservice.exceptions.BlogPostCreationException;
import com.scalefocus.blogservice.exceptions.BlogPostNotFoundException;
import com.scalefocus.blogservice.exceptions.TagNotFoundException;
import com.scalefocus.blogservice.mapper.BlogPostMapper;
import com.scalefocus.blogservice.model.BlogPost;
import com.scalefocus.blogservice.model.specifications.BlogPostSpecification;
import com.scalefocus.blogservice.model.Tag;
import com.scalefocus.blogservice.repository.BlogPostRepository;
import com.scalefocus.blogservice.repository.TagRepository;
import com.scalefocus.blogservice.service.BlogPostService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Service
public class BlogPostServiceImpl implements BlogPostService {
    private static final Logger logger = LoggerFactory.getLogger(BlogPostServiceImpl.class);

    private static final String BLOG_POST_NOT_FOUND_MESSAGE = "Blog post with ID: {} not found";

    private static final BlogPostMapper blogPostMapper = BlogPostMapper.INSTANCE;

    private final BlogPostRepository blogPostRepository;

    private final TagRepository tagRepository;


    public List<BlogPostDTO> getBlogPosts() {
        logger.debug("Fetching all blog posts");
        return blogPostMapper.blogPostsToBlogPostDTOs(blogPostRepository.findAll());
    }

    public Page<BlogPostDTO> getFilteredBlogPosts(String tag, String parity, Integer summaryLimit, Pageable pageable) {
        logger.info("Filtering blog posts with tag: {}, parity: {}, summaryLimit: {}", tag, parity, summaryLimit);
        if(summaryLimit !=null && summaryLimit<5){
            logger.error("Invalid summaryLimit: {}", summaryLimit);
            throw new IllegalArgumentException("summaryLimit must be at least 5");
        }

        if (parity != null && !parity.equalsIgnoreCase("even") && !parity.equalsIgnoreCase("odd")) {
            logger.error("Invalid parity value: {}", parity);
            throw new IllegalArgumentException("Invalid value for parity. It must be 'even' or 'odd'.");
        }

        Specification<BlogPost> spec = Specification.where(null);

        if (tag != null && summaryLimit != null) {
            spec = BlogPostSpecification.hasTagAndSummary(tag, summaryLimit);
        } else if (parity != null && summaryLimit != null) {
            spec = BlogPostSpecification.hasParityAndSummary(parity, summaryLimit);
        } else if (summaryLimit != null) {
            spec = BlogPostSpecification.hasSummaryLimit(summaryLimit);
        } else if (parity != null) {
            spec = BlogPostSpecification.hasParity(parity);
        } else if (tag != null) {
            spec = BlogPostSpecification.hasTag(tag);
        }

        Page<BlogPost> blogPosts = blogPostRepository.findAll(spec, pageable);
        logger.debug("Found {} blog posts after filtering", blogPosts.getTotalElements());

        return blogPosts.map(post -> {
            String summaryText = summaryLimit != null ? getSummary(post.getText(), summaryLimit) : post.getText();
            return new BlogPostDTO(post.getBlogId(), post.getTitle(), summaryText, post.getAuthor(), blogPostMapper.tagsToTagDTOs(post.getTags()));
        });
    }

    public BlogPostDTO getBlogPostById(Long id) {
        logger.info("Fetching blog post with ID: {}", id);
        return blogPostRepository.findById(id)
                .map(blogPostMapper::blogPostToBlogPostDTO)
                .orElseThrow(() -> {logger.error(BLOG_POST_NOT_FOUND_MESSAGE, id);
                    return new BlogPostNotFoundException(id);});
    }

    public BlogPostDTO addBlogPost(CreateBlogPostDTO blogPostDTO, String authorUsername) {
        logger.info("Adding a new blog post with title: {}", blogPostDTO.title());
        try {
            BlogPost blogPost = blogPostMapper.createBlogPostDTOToBlogPost(blogPostDTO);
            blogPost.setAuthor(authorUsername);
            BlogPost savedBlogPost = blogPostRepository.save(blogPost);
            logger.info("Blog post saved with ID: {}", savedBlogPost.getBlogId());
            return blogPostMapper.blogPostToBlogPostDTO(savedBlogPost);
        } catch (Exception e) {
            logger.error("Failed to create blog post", e);
            throw new BlogPostCreationException("Failed to create blog post");
        }
    }

    public String getSummary(String text, int summaryLimit) {
        return text.length() > summaryLimit ? text.substring(0, summaryLimit - 4) + " ..." : text;
    }

    public BlogPostDTO updateBlogPost(Long id, BlogPostDTO blogPostDTO) {
        logger.info("Updating blog post with ID: {}", id);
        return blogPostRepository.findById(id)
                .map(blogPost -> applyUpdatesIfNeeded(blogPost, blogPostDTO))
                .orElseThrow(() -> {
                    logger.error(BLOG_POST_NOT_FOUND_MESSAGE, id);
                    return new BlogPostNotFoundException(id);
                });
    }

    private BlogPostDTO applyUpdatesIfNeeded(BlogPost blogPost, BlogPostDTO updatedBlogPostDTO) {
        boolean updated = false;

        if (updatedBlogPostDTO.getTitle() != null && !updatedBlogPostDTO.getTitle().isEmpty()
                && !updatedBlogPostDTO.getTitle().equals(blogPost.getTitle())) {
            blogPost.setTitle(updatedBlogPostDTO.getTitle());
            updated = true;
            logger.info("Updated blog post title to '{}'", updatedBlogPostDTO.getTitle());
        }

        if (updatedBlogPostDTO.getText() != null && !updatedBlogPostDTO.getText().isEmpty()
                && !updatedBlogPostDTO.getText().equals(blogPost.getText())) {
            blogPost.setText(updatedBlogPostDTO.getText());
            updated = true;
            logger.info("Updated blog post text");
        }

        if (updatedBlogPostDTO.getTags() != null && !updatedBlogPostDTO.getTags().isEmpty()) {
            List<Tag> newTags = updatedBlogPostDTO.getTags().stream()
                    .map(tagDTO -> tagRepository.findByTagName(tagDTO.tagName())
                            .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagDTO.tagName()).build())))
                    .toList();

            if (!newTags.equals(blogPost.getTags())) {
                blogPost.setTags(newTags);
                updated = true;
                logger.info("Updated blog post tags");
            }
        }

        if (updated) {
            blogPostRepository.save(blogPost);
            logger.info("Blog post with ID: {} updated successfully", blogPost.getBlogId());
        }

        return blogPostMapper.blogPostToBlogPostDTO(blogPost);
    }

    public BlogPostDTO addTagToBlogPost(Long blogPostId, String tagName) {
        logger.info("Adding tag '{}' to blog post with ID: {}", tagName, blogPostId);
        return blogPostRepository.findById(blogPostId).map(blogPost -> {
            Tag tag = tagRepository.findByTagName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder().tagName(tagName).build()));
            blogPost.getTags().add(tag);
            blogPostRepository.save(blogPost);
            logger.info("Tag '{}' added to blog post with ID: {}", tagName, blogPostId);
            return blogPostMapper.blogPostToBlogPostDTO(blogPost);
        }).orElseThrow(() -> {
            logger.error(BLOG_POST_NOT_FOUND_MESSAGE, blogPostId);
            return new BlogPostNotFoundException(blogPostId);
        });

    }

    public BlogPostDTO removeTagFromBlogPost(Long blogPostId, String tagName) {
        logger.info("Removing tag '{}' from blog post with ID: {}", tagName, blogPostId);
        BlogPost blogPost = blogPostRepository.findById(blogPostId)
                .orElseThrow(() -> {
            logger.error(BLOG_POST_NOT_FOUND_MESSAGE, blogPostId);
            return new BlogPostNotFoundException(blogPostId);
        });

        Tag tag = tagRepository.findByTagName(tagName)
                .orElseThrow(() -> {
                    logger.error("Tag '{}' not found", tagName);
                    return new TagNotFoundException(tagName);
                });

        if (blogPost.getTags().contains(tag)) {
            blogPost.getTags().remove(tag);
            blogPostRepository.save(blogPost);
            logger.info("Tag '{}' removed from blog post with ID: {}", tagName, blogPostId);
        } else {
            logger.warn("Tag '{}' was not associated with blog post ID: {}", tagName, blogPostId);
        }

        return blogPostMapper.blogPostToBlogPostDTO(blogPost);
    }

    @Override
    public void deleteBlogPost(Long id) {
        logger.info("Deleting blog post with ID: {}", id);
        BlogPost blogPost = blogPostRepository.findById(id).orElseThrow(() -> {
            logger.error(BLOG_POST_NOT_FOUND_MESSAGE, id);
            return new BlogPostNotFoundException(id);
        });

        for (Tag tag : blogPost.getTags()) {
            tag.getBlogPosts().remove(blogPost);
        }

        blogPostRepository.delete(blogPost);
        logger.info("Blog post with ID: {} deleted successfully", id);
    }
}
