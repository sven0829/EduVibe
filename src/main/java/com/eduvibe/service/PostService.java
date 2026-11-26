
package com.eduvibe.service;

import com.eduvibe.dao.PostDAO;
import com.eduvibe.model.Post;

import java.sql.SQLException;
import java.util.List;

public class PostService {

    private final PostDAO postDAO;

    public PostService() {
        this.postDAO = new PostDAO();
    }

    public List<Post> getHomeFeed(int userId)
            throws SQLException {
        validateUserId(userId);
        return postDAO.findHomeFeed(userId);
    }

    // FOLLOWING FEED
    public List<Post> getFollowingFeed(int userId)
            throws SQLException {
        validateUserId(userId);
        return postDAO.findFollowingFeed(userId);
    }

    public List<Post> getAcademicFeed(int userId)
            throws SQLException {
        validateUserId(userId);
        return postDAO.findByCategory(userId, "academic");
    }

    public List<Post> getEntertainmentFeed(int userId)
            throws SQLException {
        validateUserId(userId);
        return postDAO.findByCategory(userId, "entertainment");
    }

    /** Academic Reels: category academic AND is_reel = 1. */
    public List<Post> getAcademicReels(int userId) throws SQLException {
        validateUserId(userId);
        return postDAO.findReelsByCategory(userId, "academic");
    }

    /** Entertainment Reels: category entertainment AND is_reel = 1. */
    public List<Post> getEntertainmentReels(int userId) throws SQLException {
        validateUserId(userId);
        return postDAO.findReelsByCategory(userId, "entertainment");
    }

    public List<Post> getFollowingReels(int userId, String category) throws SQLException {
        validateUserId(userId);
        return postDAO.findFollowingReelsByCategory(userId, category);
    }

    /** Administrator correction of a post's category. Returns false when the post does not exist. */
    public boolean changeCategory(int postId, String category) throws SQLException {
        return postDAO.updateCategory(postId, category);
    }

    /** An author corrects the category of their own post. Returns false when it is not theirs. */
    public boolean changeOwnCategory(int postId, int userId, String category) throws SQLException {
        return postDAO.updateCategoryForOwner(postId, userId, category);
    }

    public List<Post> getReels(int userId)
            throws SQLException {
        validateUserId(userId);
        return postDAO.findReels(userId);
    }

    public boolean createPost(
            int userId,
            String category,
            String title,
            String description,
            String mediaType,
            String mediaPath,
            boolean isReel
    ) throws SQLException {

        validateUserId(userId);

        if (!com.eduvibe.util.ContentFilter.isClean(title)
                || !com.eduvibe.util.ContentFilter.isClean(description)) {
            throw new IllegalArgumentException(
                    com.eduvibe.util.ContentFilter.MESSAGE);
        }

        return postDAO.createPost(
                userId, category, title, description,
                mediaType, mediaPath, isReel
        );
    }

    // EDIT OWN POST
    public boolean updatePost(
            int postId,
            int userId,
            String title,
            String description
    ) throws SQLException {

        validateUserId(userId);

        if (postId <= 0) {
            throw new IllegalArgumentException("Invalid post ID.");
        }

        if (!com.eduvibe.util.ContentFilter.isClean(title)
                || !com.eduvibe.util.ContentFilter.isClean(description)) {
            throw new IllegalArgumentException(
                    com.eduvibe.util.ContentFilter.MESSAGE);
        }

        return postDAO.updatePost(
                postId, userId, title, description
        );
    }

    // DELETE OWN POST
    public boolean deletePost(
            int postId,
            int userId
    ) throws SQLException {

        validateUserId(userId);

        if (postId <= 0) {
            throw new IllegalArgumentException("Invalid post ID.");
        }

        return postDAO.deletePost(postId, userId);
    }

    private void validateUserId(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "A valid logged-in user is required."
            );
        }
    }
}
