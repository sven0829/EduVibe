package com.eduvibe.dao;

import com.eduvibe.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * com.eduvibe.dao.RewardCatalogSeeder  (NEW FILE)
 *
 * Makes sure the reward catalogue is not tiny. When the rewards table has fewer than
 * MIN_REWARDS rows, the starter catalogue below (digital frames, banners, themes and
 * badges; points only, no monetary value) is added. A reward is inserted only if no reward
 * with the same name exists, nothing is ever changed or deleted, and a row the database
 * rejects is simply skipped. Runs at most once per app start.
 */
public final class RewardCatalogSeeder {

    private static final int MIN_REWARDS = 30;
    private static boolean done = false;

    /** {name, description, reward_type, rarity, point_cost} */
    private static final String[][] CATALOG = {
        {"Silver Ring", "A clean silver ring around your avatar.", "avatar_frame", "common", "60"},
        {"Sky Ring", "A light-blue frame for a fresh look.", "avatar_frame", "common", "60"},
        {"Leaf Frame", "A green frame for nature lovers.", "avatar_frame", "common", "60"},
        {"Cloud Frame", "A soft white frame.", "avatar_frame", "common", "60"},
        {"Pencil Frame", "A yellow frame for note-takers.", "avatar_frame", "common", "60"},
        {"Sunset Frame", "Warm orange tones around your photo.", "avatar_frame", "uncommon", "180"},
        {"Neon Pulse Frame", "A bright neon outline that stands out.", "avatar_frame", "uncommon", "180"},
        {"Mint Fresh Frame", "A cool mint-green frame.", "avatar_frame", "uncommon", "180"},
        {"Cherry Frame", "A bold red-pink frame.", "avatar_frame", "uncommon", "180"},
        {"Ocean Wave Frame", "A deep teal frame.", "avatar_frame", "uncommon", "180"},
        {"Royal Violet Frame", "A rich purple frame fit for a scholar.", "avatar_frame", "rare", "380"},
        {"Ice Crystal Frame", "A frosty blue-white frame.", "avatar_frame", "rare", "380"},
        {"Ember Frame", "A glowing orange-red frame.", "avatar_frame", "rare", "380"},
        {"Emerald Frame", "A shining green frame.", "avatar_frame", "rare", "380"},
        {"Aurora Frame", "Shifting colours like the northern lights.", "avatar_frame", "epic", "800"},
        {"Nebula Frame", "A frame of deep-space purple and pink.", "avatar_frame", "epic", "800"},
        {"Solar Flare Frame", "A fiery gold-orange frame.", "avatar_frame", "epic", "800"},
        {"Golden Crown Frame", "The gold frame reserved for top students.", "avatar_frame", "legendary", "1800"},
        {"Diamond Halo Frame", "A brilliant frame that shines above the rest.", "avatar_frame", "legendary", "1800"},
        {"Campus Morning Banner", "A calm blue banner for your profile.", "banner", "common", "70"},
        {"Library Banner", "A cozy banner for bookworms.", "banner", "common", "70"},
        {"Notebook Banner", "A clean banner inspired by notebook paper.", "banner", "common", "70"},
        {"Green Field Banner", "A fresh green banner.", "banner", "common", "70"},
        {"Rainy Day Banner", "A quiet grey-blue banner.", "banner", "common", "70"},
        {"Study Night Banner", "A deep navy banner for night owls.", "banner", "uncommon", "160"},
        {"Coffee Break Banner", "A warm brown banner for caffeine-powered study.", "banner", "uncommon", "160"},
        {"Peach Banner", "A soft peach banner.", "banner", "uncommon", "160"},
        {"Lavender Banner", "A gentle lavender banner.", "banner", "uncommon", "160"},
        {"Teal Wave Banner", "A flowing teal banner.", "banner", "uncommon", "160"},
        {"Galaxy Banner", "A purple space banner for big thinkers.", "banner", "rare", "420"},
        {"Cherry Blossom Banner", "A soft pink banner full of spring colours.", "banner", "rare", "420"},
        {"Northern Lights Banner", "A green and violet glow.", "banner", "rare", "420"},
        {"Volcano Banner", "A hot red-orange banner.", "banner", "rare", "420"},
        {"Golden Hour Banner", "A glowing gold banner that grabs attention.", "banner", "epic", "900"},
        {"Cosmic Dream Banner", "A swirl of purple, pink and blue.", "banner", "epic", "900"},
        {"Thunderstorm Banner", "A dramatic dark-blue banner.", "banner", "epic", "900"},
        {"Honor Roll Banner", "The legendary banner for the best of the best.", "banner", "legendary", "2000"},
        {"Hall of Fame Banner", "A royal gold and crimson banner.", "banner", "legendary", "2000"},
        {"Classic Light Theme", "A bright, clean profile tint.", "theme", "common", "75"},
        {"Ocean Theme", "A cool blue profile tint.", "theme", "common", "75"},
        {"Sand Theme", "A warm neutral profile tint.", "theme", "common", "75"},
        {"Sky Theme", "A light sky-blue profile tint.", "theme", "common", "75"},
        {"Mint Theme", "A fresh mint profile tint.", "theme", "common", "75"},
        {"Forest Theme", "A calm green profile tint.", "theme", "uncommon", "170"},
        {"Sunrise Theme", "A warm orange profile tint.", "theme", "uncommon", "170"},
        {"Berry Theme", "A sweet purple-red profile tint.", "theme", "uncommon", "170"},
        {"Lemon Theme", "A cheerful yellow profile tint.", "theme", "uncommon", "170"},
        {"Slate Theme", "A cool grey-blue profile tint.", "theme", "uncommon", "170"},
        {"Midnight Theme", "A deep indigo profile tint.", "theme", "rare", "400"},
        {"Rose Theme", "An elegant pink profile tint.", "theme", "rare", "400"},
        {"Jade Theme", "A rich green profile tint.", "theme", "rare", "400"},
        {"Crimson Theme", "A bold red profile tint.", "theme", "rare", "400"},
        {"Amethyst Theme", "A rich violet profile tint.", "theme", "epic", "850"},
        {"Sapphire Theme", "A brilliant deep-blue profile tint.", "theme", "epic", "850"},
        {"Twilight Theme", "A purple-orange dusk profile tint.", "theme", "epic", "850"},
        {"Golden Theme", "A shining gold profile tint.", "theme", "legendary", "1900"},
        {"Platinum Theme", "A radiant silver-white profile tint.", "theme", "legendary", "1900"},
        {"Newbie Badge", "Show that you just joined the community.", "badge", "common", "50"},
        {"Bookworm Badge", "For those who always have a book nearby.", "badge", "common", "50"},
        {"Early Bird Badge", "For students who start their day early.", "badge", "common", "50"},
        {"Notetaker Badge", "For students with great notes.", "badge", "common", "50"},
        {"Curious Mind Badge", "For students who always ask why.", "badge", "common", "50"},
        {"Doodler Badge", "For students who sketch in the margins.", "badge", "common", "50"},
        {"Library Regular Badge", "For frequent library visitors.", "badge", "common", "50"},
        {"Night Owl Badge", "For students who study late.", "badge", "uncommon", "150"},
        {"Team Player Badge", "For helpful classmates.", "badge", "uncommon", "150"},
        {"Quiz Whiz Badge", "For quick thinkers.", "badge", "uncommon", "150"},
        {"Group Study Badge", "For students who study together.", "badge", "uncommon", "150"},
        {"Focus Badge", "For students who stay on task.", "badge", "uncommon", "150"},
        {"Problem Solver Badge", "For students who crack hard questions.", "badge", "uncommon", "150"},
        {"Lab Rat Badge", "For science lovers.", "badge", "uncommon", "150"},
        {"Top Commenter Badge", "For people who join every conversation.", "badge", "rare", "350"},
        {"Creator Badge", "For students who love to post.", "badge", "rare", "350"},
        {"Mentor Badge", "For students who help others learn.", "badge", "rare", "350"},
        {"Debate Champ Badge", "For sharp, respectful debaters.", "badge", "rare", "350"},
        {"Idea Spark Badge", "For students full of ideas.", "badge", "rare", "350"},
        {"Math Master Badge", "For number lovers.", "badge", "rare", "350"},
        {"Streak Master Badge", "For consistent daily learners.", "badge", "epic", "750"},
        {"Scholar Supreme Badge", "A rare badge for outstanding students.", "badge", "epic", "750"},
        {"Study Titan Badge", "For students who never quit.", "badge", "epic", "750"},
        {"Brainstorm Badge", "For bright, creative thinkers.", "badge", "epic", "750"},
        {"Campus Legend Badge", "The rarest badge in EduVibe.", "badge", "legendary", "2200"},
        {"Valedictorian Badge", "The ultimate badge for the top of the class.", "badge", "legendary", "2200"},
    };

    private RewardCatalogSeeder() { }

    public static synchronized void seedIfSparse() {
        if (done) {
            return;
        }
        done = true;

        try (Connection c = DatabaseConnection.getConnection()) {

            int count = 0;
            try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM rewards");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            if (count >= MIN_REWARDS) {
                return;
            }

            String sql = """
                INSERT INTO rewards (name, description, reward_type, rarity, point_cost, is_active)
                SELECT ?, ?, ?, ?, ?, 1 FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE name = ?)
                """;

            for (String[] r : CATALOG) {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, r[0]);
                    ps.setString(2, r[1]);
                    ps.setString(3, r[2]);
                    ps.setString(4, r[3]);
                    ps.setInt(5, Integer.parseInt(r[4]));
                    ps.setString(6, r[0]);
                    ps.executeUpdate();
                } catch (SQLException skip) {
                    // this database does not accept this value: leave the row out
                }
            }
        } catch (SQLException ignored) {
            done = false;      // try again next time (e.g. database was not ready)
        }
    }
}
