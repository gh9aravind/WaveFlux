-- ============================================================================
-- SoundSpot Production Database Schema
-- DBMS: PostgreSQL (v12+)
-- Target Capability: Music catalogs, custom user profiles & hierarchical playlist structures
-- ============================================================================

-- EnableUUID Extension for generating robust distributed IDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. ENUMS AND DOMAINS
-- ============================================================================
CREATE TYPE subscription_tier AS ENUM ('free', 'premium', 'family', 'admin');
CREATE TYPE track_vibe_genre AS ENUM ('synthwave', 'lofi', 'acoustic', 'classical', 'jazz', 'pop', 'rock', 'ambient');
CREATE TYPE playlist_visibility AS ENUM ('private', 'collaborative', 'public');

-- ============================================================================
-- 2. USER PROFILES AND ACCOUNTS
-- ============================================================================
CREATE TABLE user_profiles (
    user_id VARCHAR(128) PRIMARY KEY, -- Auth0 UID or Firebase SDK Identity Key
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(512),
    tier subscription_tier DEFAULT 'free'::subscription_tier,
    timezone VARCHAR(50) DEFAULT 'UTC',
    
    -- Audit & timestamp attributes
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE
);

-- Index user searches & login email scans
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_user_profiles_tier ON user_profiles(tier);

-- ============================================================================
-- 3. CHANNELS, ARTISTS & LABELS
-- ============================================================================
CREATE TABLE artists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    profile_image_url VARCHAR(512),
    website_url VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_artists_name_trgm ON artists (name);

-- ============================================================================
-- 4. ALBUMS AND RELEASES
-- ============================================================================
CREATE TABLE albums (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    artist_id UUID NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    cover_image_url VARCHAR(512),
    release_date DATE,
    barcode VARCHAR(50), -- UPC / EAN for distribution tracking
    label_name VARCHAR(255),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_albums_artist ON albums(artist_id);
CREATE INDEX idx_albums_release_date ON albums(release_date);

-- ============================================================================
-- 5. TRACKS & MUSIC METADATA
-- ============================================================================
CREATE TABLE tracks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    album_id UUID REFERENCES albums(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    artist_id UUID NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
    
    -- Music attributes for retrieval and curation
    genre track_vibe_genre NOT NULL,
    vibe_code VARCHAR(100) NOT NULL, -- e.g., 'synthwave', 'lofi_study', 'workout_pump'
    duration_seconds INT NOT NULL CHECK (duration_seconds > 0),
    track_number INT DEFAULT 1,
    disc_number INT DEFAULT 1,
    is_explicit BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- Storage coordinates targeting S3 and distributed via CloudFront
    s3_file_key VARCHAR(512) NOT NULL, -- e.g., 'audio/tracks/synthwave/fcc-3920.mp3'
    audio_codec VARCHAR(10) DEFAULT 'mp3' NOT NULL,
    bitrate_kbps INT DEFAULT 320 NOT NULL,
    
    -- Stats caching for fast listing (updated via background workers)
    play_count BIGINT DEFAULT 0 NOT NULL,
    favorite_count BIGINT DEFAULT 0 NOT NULL,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_tracks_vibe ON tracks(vibe_code);
CREATE INDEX idx_tracks_genre ON tracks(genre);
CREATE INDEX idx_tracks_artist ON tracks(artist_id);
CREATE INDEX idx_tracks_album ON tracks(album_id);

-- ============================================================================
-- 6. PLAYLIST STRUCTURES
-- ============================================================================
CREATE TABLE playlists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creator_id VARCHAR(128) NOT NULL REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(512),
    visibility playlist_visibility DEFAULT 'pubic'::playlist_visibility NOT NULL,
    
    follower_count INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_playlists_creator ON playlists(creator_id);
CREATE INDEX idx_playlists_visibility ON playlists(visibility);

-- Junction table mapping tracks -> values inside custom User Playlists
CREATE TABLE playlist_tracks (
    playlist_id UUID REFERENCES playlists(id) ON DELETE CASCADE,
    track_id UUID REFERENCES tracks(id) ON DELETE CASCADE,
    position INT NOT NULL, -- Ordered position sorting inside the playlist
    added_by VARCHAR(128) REFERENCES user_profiles(user_id) ON DELETE SET NULL,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    PRIMARY KEY (playlist_id, track_id),
    CONSTRAINT unique_playlist_position UNIQUE (playlist_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_playlist_tracks_playlist ON playlist_tracks(playlist_id);

-- Playlists followers / subscribers linkage table
CREATE TABLE playlist_followers (
    playlist_id UUID REFERENCES playlists(id) ON DELETE CASCADE,
    user_id VARCHAR(128) REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    followed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    PRIMARY KEY (playlist_id, user_id)
);

CREATE INDEX idx_playlist_followers_user ON playlist_followers(user_id);

-- ============================================================================
-- 7. USER INTERACTION MATRICES (LIKES, FAVORITES)
-- ============================================================================
CREATE TABLE user_favorite_tracks (
    user_id VARCHAR(128) REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    track_id UUID REFERENCES tracks(id) ON DELETE CASCADE,
    favorited_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    PRIMARY KEY (user_id, track_id)
);

CREATE INDEX idx_user_fav_tracks_user ON user_favorite_tracks(user_id);
CREATE INDEX idx_user_fav_tracks_track ON user_favorite_tracks(track_id);

-- ============================================================================
-- 8. AUTOMATIC RE-CALCULATION TRIGGERS & PROCEDURES (DATA CONSISTENCY)
-- ============================================================================

-- Automatically update modified updated_at timestamps
CREATE OR REPLACE FUNCTION update_modified_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attaching triggers to core functional entities
CREATE TRIGGER update_user_profiles_timestamp BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_modified_timestamp();
CREATE TRIGGER update_artists_timestamp BEFORE UPDATE ON artists FOR EACH ROW EXECUTE FUNCTION update_modified_timestamp();
CREATE TRIGGER update_albums_timestamp BEFORE UPDATE ON albums FOR EACH ROW EXECUTE FUNCTION update_modified_timestamp();
CREATE TRIGGER update_tracks_timestamp BEFORE UPDATE ON tracks FOR EACH ROW EXECUTE FUNCTION update_modified_timestamp();
CREATE TRIGGER update_playlists_timestamp BEFORE UPDATE ON playlists FOR EACH ROW EXECUTE FUNCTION update_modified_timestamp();

-- Automatically keep track dynamic like-counts cached on tracks table
CREATE OR REPLACE FUNCTION increment_track_favorites()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE tracks SET favorite_count = favorite_count + 1 WHERE id = NEW.track_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION decrement_track_favorites()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE tracks SET favorite_count = GREATEST(0, favorite_count - 1) WHERE id = OLD.track_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_fav_track_added AFTER INSERT ON user_favorite_tracks FOR EACH ROW EXECUTE FUNCTION increment_track_favorites();
CREATE TRIGGER tr_fav_track_removed AFTER DELETE ON user_favorite_tracks FOR EACH ROW EXECUTE FUNCTION decrement_track_favorites();
