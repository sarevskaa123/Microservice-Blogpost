package com.scalefocus.blogservice.mapper;

import com.scalefocus.blogservice.dto.BlogPostDTO;
import com.scalefocus.blogservice.dto.CreateBlogPostDTO;
import com.scalefocus.blogservice.dto.ResponseTagDTO;
import com.scalefocus.blogservice.model.BlogPost;
import com.scalefocus.blogservice.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BlogPostMapper {

    BlogPostMapper INSTANCE = Mappers.getMapper(BlogPostMapper.class);

    BlogPostDTO blogPostToBlogPostDTO(BlogPost blogPost);

    BlogPost blogPostDTOToBlogPost(BlogPostDTO blogPostDTO);

    BlogPost createBlogPostDTOToBlogPost(CreateBlogPostDTO createBlogPostDTO);

    @Mapping(target = "author", source = "author")
    BlogPost createBlogPostToDTO(BlogPost blogPost);

    @Mapping(target = "timeCreated", source = "timeCreated")
    ResponseTagDTO tagToTagDTO(Tag tag);

    @Mapping(target = "timeCreated", source = "timeCreated")
    Tag tagDTOToTag(ResponseTagDTO responseTagDTO);

    List<BlogPostDTO> blogPostsToBlogPostDTOs(List<BlogPost> blogPosts);

    List<BlogPost> blogPostDTOsToBlogPosts(List<BlogPostDTO> blogPostDTOs);

    List<ResponseTagDTO> tagsToTagDTOs(List<Tag> tags);

    List<Tag> tagDTOsToTags(List<ResponseTagDTO> responseTagDTOS);
}


