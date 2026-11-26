
package com.eduvibe.service;

import com.eduvibe.dao.LikeDAO;

import java.sql.SQLException;

public class LikeService {

    private final LikeDAO likeDAO;

    public LikeService() {
        this.likeDAO = new LikeDAO();
    }

    public boolean hasUserLiked(int postId, int userId)
            throws SQLException {

        validateIds(postId, userId);

        return likeDAO.hasUserLiked(postId, userId);
    }

    public int getLikeCount(int postId) throws SQLException {

        if (postId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid post ID."
            );
        }

        return likeDAO.getLikeCount(postId);
    }

    public boolean toggleLike(int postId, int userId)
            throws SQLException {

        validateIds(postId, userId);

        return likeDAO.toggleLike(postId, userId);
    }

    private void validateIds(int postId, int userId) {

        if (postId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid post ID."
            );
        }

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid user ID."
            );
        }
    }
}
