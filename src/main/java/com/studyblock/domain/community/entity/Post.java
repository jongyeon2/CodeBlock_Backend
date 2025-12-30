package com.studyblock.domain.community.entity;

import com.studyblock.domain.community.enums.ContentStatus;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "original_content", nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "edited_content", columnDefinition = "TEXT")
    private String editedContent;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_edited", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean isEdited = false;

    @Column(name = "hit")
    private Long hit = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.ACTIVE;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    //조회수 증가
    public void incrementHits(){
        this.hit++;
    }

    // 자유게시판 전용
    @Builder
    public Post(Board board, User user, String title, String originalContent, String imageUrl, Long hit) {
        this.board = board;
        this.user = user;
        this.title = title;
        this.originalContent = originalContent;
        this.createdAt = LocalDateTime.now();
        this.isEdited = false;
        this.status = ContentStatus.ACTIVE;
        this.imageUrl = imageUrl;
        this.hit = 0L;
    }
    //공지사항, FAQ 전용
    @Builder
    public Post(Board board, User user, String title, String originalContent, String imageUrl) {
        this.board = board;
        this.user = user;
        this.title = title;
        this.originalContent = originalContent;
        this.createdAt = LocalDateTime.now();
        this.isEdited = false;
        this.status = ContentStatus.ACTIVE;
        this.imageUrl = imageUrl;
    }

    // Business methods
    public void edit(String newContent, String imageUrl) {
        this.editedContent = newContent;
        this.imageUrl = imageUrl;
        this.isEdited = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void delete() {
        this.status = ContentStatus.DELETED;
    }

    public void restore() {
        this.status = ContentStatus.ACTIVE;
    }

    public String getCurrentContent() {
        return isEdited && editedContent != null ? editedContent : originalContent;
    }

    public boolean isActive() {
        return this.status == ContentStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == ContentStatus.DELETED;
    }
}
