package com.studyblock.domain.community.entity;

import com.studyblock.domain.community.enums.BoardType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "TINYINT")
    private Integer type;

    @Builder
    public Board(String name, Integer type) {
        this.name = name;
        this.type = type;
    }

    // Business methods
    public void updateName(String name) {
        this.name = name;
    }

    public BoardType getBoardType() {
        return BoardType.fromValue(this.type);
    }

    public boolean isNoticeBoard() {
        return this.type == BoardType.NOTICE.getValue();
    }

    public boolean isFaqBoard() {
        return this.type == BoardType.FAQ.getValue();
    }

}

