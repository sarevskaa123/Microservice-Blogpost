package com.scalefocus.blogservice.model.specifications;

import com.scalefocus.blogservice.model.BlogPost;
import org.springframework.data.jpa.domain.Specification;

public class BlogPostSpecification {

    private BlogPostSpecification() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Specification<BlogPost> hasTag(String tag) {
        return (root, query, criteriaBuilder) -> tag == null ? null : criteriaBuilder.equal(root.join("tags").get("tagName"), tag);
    }

    public static Specification<BlogPost> hasParity(String parity) {
        return (root, query, criteriaBuilder) -> {
            if (parity == null) {
                return null;
            }
            boolean isEven = parity.equalsIgnoreCase("even");
            return isEven ? criteriaBuilder.equal(criteriaBuilder.mod(criteriaBuilder.size(root.get("tags")), 2), 0)
                    : criteriaBuilder.notEqual(criteriaBuilder.mod(criteriaBuilder.size(root.get("tags")), 2), 0);
        };
    }

    public static Specification<BlogPost> hasSummaryLimit(Integer summaryLimit) {
        return (root, query, criteriaBuilder) -> summaryLimit == null ? null : criteriaBuilder.isTrue(criteriaBuilder.literal(true));
    }

    public static Specification<BlogPost> hasTagAndSummary(String tag, Integer summaryLimit) {
        return Specification.where(hasTag(tag)).and(hasSummaryLimit(summaryLimit));
    }

    public static Specification<BlogPost> hasParityAndSummary(String parity, Integer summaryLimit) {
        return Specification.where(hasParity(parity)).and(hasSummaryLimit(summaryLimit));
    }

}
