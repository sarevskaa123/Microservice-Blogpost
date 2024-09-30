package com.scalefocus.blogservice.service;

import com.scalefocus.blogservice.dto.BlogPostDTO;
import com.scalefocus.blogservice.dto.CreateBlogPostDTO;

import java.util.List;


public interface BlogPostService {

    List<BlogPostDTO> getBlogPosts();

    BlogPostDTO addBlogPost(CreateBlogPostDTO blogPostDTO, String authorUsername);

    String getSummary(String text, int summaryLimit);

    BlogPostDTO updateBlogPost(Long id, BlogPostDTO blogPostDTO);

    BlogPostDTO addTagToBlogPost(Long blogPostId, String tagName);

    BlogPostDTO removeTagFromBlogPost(Long blogPostId, String tagName);

    void deleteBlogPost(Long id);

    BlogPostDTO getBlogPostById(Long id);

}