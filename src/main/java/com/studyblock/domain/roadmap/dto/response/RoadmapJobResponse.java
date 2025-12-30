package com.studyblock.domain.roadmap.dto.response;

import com.studyblock.domain.roadmap.entity.RoadmapJob;
import com.studyblock.domain.roadmap.enums.RoadmapTheme;
import lombok.Builder;

@Builder
public record RoadmapJobResponse(
        Long id,
        String jobId,
        String title,
        String description,
        String icon,
        String color,
        String lightColor,
        String borderColor,
        String hoverBorderColor
) {
    public static RoadmapJobResponse from(RoadmapJob job) {
        // jobId를 기반으로 테마(아이콘, 색상) 자동 매핑
        RoadmapTheme theme = RoadmapTheme.fromJobId(job.getJobId());

        return RoadmapJobResponse.builder()
                .id(job.getId())
                .jobId(job.getJobId())
                .title(job.getTitle())
                .description(job.getDescription())
                .icon(theme.getIcon())
                .color(theme.getColor())
                .lightColor(theme.getLightColor())
                .borderColor(theme.getBorderColor())
                .hoverBorderColor(theme.getHoverBorderColor())
                .build();
    }
}
