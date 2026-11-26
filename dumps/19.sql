-- OPTIONAL reward catalog: 36 new digital rewards (frames, banners, themes, badges).
-- Safe to run more than once: a reward is added only if no reward with the same name exists.
-- Nothing is deleted or changed. Rewards have no monetary value (points only).
--
-- BEFORE RUNNING, check your column values with:
--   SELECT DISTINCT reward_type, rarity FROM rewards;
-- This file uses reward_type: avatar_frame, banner, theme, badge
--       and rarity: common, uncommon, rare, epic, legendary.
-- If reward_type or rarity is an ENUM that does not allow these, MySQL will say "Data truncated";
-- then send me the result of:  SHOW COLUMNS FROM rewards;
USE eduvibe;

INSERT INTO rewards (name, description, reward_type, rarity, point_cost, is_active)
SELECT v.name, v.description, v.reward_type, v.rarity, v.point_cost, 1
FROM (
    SELECT 'Silver Ring' AS name, 'A clean silver ring around your avatar.' AS description, 'avatar_frame' AS reward_type, 'common' AS rarity, 60 AS point_cost
    UNION ALL SELECT 'Sky Ring', 'A light-blue frame for a fresh look.', 'avatar_frame', 'common', 80
    UNION ALL SELECT 'Leaf Frame', 'A green frame for nature lovers.', 'avatar_frame', 'common', 90
    UNION ALL SELECT 'Sunset Frame', 'Warm orange tones around your photo.', 'avatar_frame', 'uncommon', 180
    UNION ALL SELECT 'Neon Pulse Frame', 'A bright neon outline that stands out.', 'avatar_frame', 'uncommon', 220
    UNION ALL SELECT 'Royal Violet Frame', 'A rich purple frame fit for a scholar.', 'avatar_frame', 'rare', 380
    UNION ALL SELECT 'Aurora Frame', 'Shifting colours like the northern lights.', 'avatar_frame', 'epic', 800
    UNION ALL SELECT 'Golden Crown Frame', 'The gold frame reserved for top students.', 'avatar_frame', 'legendary', 1800
    UNION ALL SELECT 'Campus Morning Banner', 'A calm blue banner for your profile.', 'banner', 'common', 70
    UNION ALL SELECT 'Library Banner', 'A cozy banner for bookworms.', 'banner', 'common', 90
    UNION ALL SELECT 'Study Night Banner', 'A deep navy banner for night owls.', 'banner', 'uncommon', 160
    UNION ALL SELECT 'Coffee Break Banner', 'A warm brown banner for caffeine-powered study.', 'banner', 'uncommon', 200
    UNION ALL SELECT 'Galaxy Banner', 'A purple space banner for big thinkers.', 'banner', 'rare', 420
    UNION ALL SELECT 'Cherry Blossom Banner', 'A soft pink banner full of spring colours.', 'banner', 'rare', 450
    UNION ALL SELECT 'Golden Hour Banner', 'A glowing gold banner that grabs attention.', 'banner', 'epic', 900
    UNION ALL SELECT 'Honor Roll Banner', 'The legendary banner for the best of the best.', 'banner', 'legendary', 2000
    UNION ALL SELECT 'Classic Light Theme', 'A bright, clean profile tint.', 'theme', 'common', 75
    UNION ALL SELECT 'Ocean Theme', 'A cool blue profile tint.', 'theme', 'common', 95
    UNION ALL SELECT 'Forest Theme', 'A calm green profile tint.', 'theme', 'uncommon', 170
    UNION ALL SELECT 'Sunrise Theme', 'A warm orange profile tint.', 'theme', 'uncommon', 210
    UNION ALL SELECT 'Midnight Theme', 'A deep indigo profile tint.', 'theme', 'rare', 400
    UNION ALL SELECT 'Rose Theme', 'An elegant pink profile tint.', 'theme', 'rare', 430
    UNION ALL SELECT 'Amethyst Theme', 'A rich violet profile tint.', 'theme', 'epic', 850
    UNION ALL SELECT 'Golden Theme', 'A shining gold profile tint.', 'theme', 'legendary', 1900
    UNION ALL SELECT 'Newbie Badge', 'Show that you just joined the community.', 'badge', 'common', 50
    UNION ALL SELECT 'Bookworm Badge', 'For those who always have a book nearby.', 'badge', 'common', 65
    UNION ALL SELECT 'Early Bird Badge', 'For students who start their day early.', 'badge', 'common', 85
    UNION ALL SELECT 'Night Owl Badge', 'For students who study late.', 'badge', 'uncommon', 150
    UNION ALL SELECT 'Team Player Badge', 'For helpful classmates.', 'badge', 'uncommon', 190
    UNION ALL SELECT 'Quiz Whiz Badge', 'For quick thinkers.', 'badge', 'uncommon', 230
    UNION ALL SELECT 'Top Commenter Badge', 'For people who join every conversation.', 'badge', 'rare', 350
    UNION ALL SELECT 'Creator Badge', 'For students who love to post.', 'badge', 'rare', 390
    UNION ALL SELECT 'Mentor Badge', 'For students who help others learn.', 'badge', 'rare', 470
    UNION ALL SELECT 'Streak Master Badge', 'For consistent daily learners.', 'badge', 'epic', 750
    UNION ALL SELECT 'Scholar Supreme Badge', 'A rare badge for outstanding students.', 'badge', 'epic', 950
    UNION ALL SELECT 'Campus Legend Badge', 'The rarest badge in EduVibe.', 'badge', 'legendary', 2200
) v
WHERE NOT EXISTS (SELECT 1 FROM rewards r WHERE r.name = v.name);
