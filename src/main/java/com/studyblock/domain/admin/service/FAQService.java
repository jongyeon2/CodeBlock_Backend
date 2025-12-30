package com.studyblock.domain.admin.service;

import com.studyblock.domain.admin.dto.NoticeResponse;
import com.studyblock.domain.admin.repository.AdminPostRepository;
import com.studyblock.domain.community.entity.Board;
import com.studyblock.domain.community.entity.Post;
import com.studyblock.domain.community.enums.BoardType;
import com.studyblock.domain.community.repository.BoardRepository;
import com.studyblock.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FAQService {

    private final AdminPostRepository adminPostRepository;
    private final BoardRepository boardRepository;

    // FAQ 등록
    @Transactional
    public Post createFAQ(User user, String title, String originalContent) {

        Board board = boardRepository.findByType(BoardType.FAQ.getValue());

        if (board == null) {
            throw new IllegalStateException("FAQ 보드가 존재하지 않습니다.");
        }

        // 중복 체크
        if (adminPostRepository.existsByTitleAndBoardId(title, board.getId())) {
            throw new IllegalArgumentException("같은 제목의 FAQ가 이미 존재합니다.");
        }

        Post post = new Post(board, user, title, originalContent, null);
        return adminPostRepository.save(post);
    }

    // FAQ 조회
    public List<NoticeResponse> getFAQList() {
        List<Post> noticeList = adminPostRepository.findByBoardType(BoardType.FAQ.getValue());

        return noticeList.stream()
                .map(notice -> NoticeResponse.builder()
                        .id(notice.getId())
                        .title(notice.getTitle())
                        .originalContent(notice.getOriginalContent())
                        .editedContent(notice.getEditedContent())
                        .imageUrl(notice.getImageUrl())
                        .created_at(notice.getCreatedAt())
                        .updated_at(notice.getUpdatedAt())
                        .status(notice.getStatus())
                        .is_edited(notice.getIsEdited())
                        .build())
                .collect(Collectors.toList());
    }

    // FAQ 수정
    @Transactional
    public Post editFAQ(Long id, String editContent) {
        Post post = adminPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        post.edit(editContent, null);

        return post;
    }

    // FAQ 비활성화
    @Transactional
    public void deleteFAQ(Long id) {
        Post post = adminPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다."));
        post.delete();
        adminPostRepository.save(post);
    }
}
