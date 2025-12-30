package com.studyblock.domain.upload.enums;

/**
 * 이미지 타입별 폴더 분리를 위한 enum
 */
public enum ImageType {
    THUMBNAIL("thumbnails"),
    PROFILE("profiles"), 
    BANNER("banners"),
    LECTURE("lectures"),
    COURSE("courses"),
    NOTICE("notices"),
    POST("posts");

    private final String folderName;

    ImageType(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }

    /**
     * 이미지 타입에 따른 S3 폴더 경로 생성
     * @return S3 폴더 경로 (예: images/thumbnails/courses)
     */
    public String getS3Path() {
        return "images/" + folderName;
    }

    /**
     * 날짜별 폴더 구조를 포함한 전체 경로 생성
     * @param year 년도
     * @param month 월
     * @param day 일
     * @return 전체 S3 경로 (예: images/thumbnails/courses/2024/01/15)
     */
    public String getS3PathWithDate(int year, int month, int day) {
        return String.format("images/%s/%d/%02d/%02d", folderName, year, month, day);
    }
}


