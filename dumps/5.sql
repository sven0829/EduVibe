USE eduvibe;

SELECT post_id, COUNT(*) AS total_likes
FROM post_likes
GROUP BY post_id;