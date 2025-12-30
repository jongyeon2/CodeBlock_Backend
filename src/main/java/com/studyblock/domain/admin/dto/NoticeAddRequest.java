package com.studyblock.domain.admin.dto;

import com.studyblock.domain.community.entity.Board;
import com.studyblock.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NoticeAddRequest {

    private String title;
    private String originalContent;
    private String imageUrl;

}
