package com.studyblock.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditNoticeRequest {
    private String editedContent;
    private Boolean removeImage; // 이미지 제거 여부
}
