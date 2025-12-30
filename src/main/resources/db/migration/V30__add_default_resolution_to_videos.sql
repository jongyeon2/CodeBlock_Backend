ALTER TABLE video
ADD COLUMN default_resolution VARCHAR(10) NOT NULL DEFAULT '1080p' COMMENT '기본 재생 해상도';

ALTER TABLE preview_video
ADD COLUMN default_resolution VARCHAR(10) NOT NULL DEFAULT '1080p' COMMENT '기본 재생 해상도';