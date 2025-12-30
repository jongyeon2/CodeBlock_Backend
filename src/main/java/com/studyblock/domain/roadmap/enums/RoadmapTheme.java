package com.studyblock.domain.roadmap.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 로드맵 직군별 색상 테마
 * 프론트엔드와 공유되는 디자인 시스템
 */
@Getter
@RequiredArgsConstructor
public enum RoadmapTheme {
    BACKEND(
            "backend",
            "Server",       // Lucide 아이콘
            "#3B82F6",      // blue-500
            "#DBEAFE",      // blue-50
            "#93C5FD",      // blue-300
            "#60A5FA"       // blue-400
    ),
    FRONTEND(
            "frontend",
            "Palette",      // Lucide 아이콘
            "#A855F7",      // purple-500
            "#FAF5FF",      // purple-50
            "#D8B4FE",      // purple-300
            "#C084FC"       // purple-400
    ),
    FULLSTACK(
            "fullstack",
            "Zap",          // Lucide 아이콘
            "#14B8A6",      // teal-500
            "#F0FDFA",      // teal-50
            "#5EEAD4",      // teal-300
            "#2DD4BF"       // teal-400
    ),
    DATA(
            "data",
            "BarChart3",    // Lucide 아이콘
            "#10B981",      // emerald-500
            "#ECFDF5",      // emerald-50
            "#6EE7B7",      // emerald-300
            "#34D399"       // emerald-400
    );

    private final String jobId;
    private final String icon;
    private final String color;
    private final String lightColor;
    private final String borderColor;
    private final String hoverBorderColor;

    /**
     * jobId로 테마 찾기
     */
    public static RoadmapTheme fromJobId(String jobId) {
        for (RoadmapTheme theme : values()) {
            if (theme.jobId.equals(jobId)) {
                return theme;
            }
        }
        throw new IllegalArgumentException("존재하지 않는 직군입니다: " + jobId);
    }

    /**
     * 테마가 존재하는지 확인
     */
    public static boolean exists(String jobId) {
        for (RoadmapTheme theme : values()) {
            if (theme.jobId.equals(jobId)) {
                return true;
            }
        }
        return false;
    }
}
