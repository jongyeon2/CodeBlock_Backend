package com.studyblock.domain.community.entity;

import com.studyblock.domain.community.enums.ContentStatus;
import com.studyblock.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>();

    @Builder
    public Comment(Post post, User user, String content, Comment parentComment) {
        this.post = post;
        this.user = user;
        this.content = content;
        this.parentComment = parentComment;
        this.createdAt = LocalDateTime.now();
        this.status = ContentStatus.ACTIVE;
    }

    // Business methods
    public void delete() {
        this.status = ContentStatus.DELETED;
    }

    public void restore() {
        this.status = ContentStatus.ACTIVE;
    }

    public void updateContent(String newContent) {
        this.content = newContent;
    }

    public boolean isActive() {
        return this.status == ContentStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return this.status == ContentStatus.DELETED;
    }

    public boolean isReply() {
        return this.parentComment != null;
    }

    public void addReply(Comment reply) {
        this.replies.add(reply);
    }
}
