package com.tavelvcommerce.commentservice.service;

import com.tavelvcommerce.commentservice.dto.CommentDto;
import com.tavelvcommerce.commentservice.entitiy.Comment;
import com.tavelvcommerce.commentservice.entitiy.User;
import com.tavelvcommerce.commentservice.entitiy.Video;
import com.tavelvcommerce.commentservice.repository.CommentRepository;
import com.tavelvcommerce.commentservice.repository.UserRepository;
import com.tavelvcommerce.commentservice.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CommentDto.CommentCreateResponseDto createComment(String commentId, String videoId, String userId, String content) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is empty");
        }

        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Content is empty");
        }

        if (content.length() > 140) {
            throw new IllegalArgumentException("Content should not exceed 140 characters");
        }

        User user = userRepository.findByUserId(userId).orElseThrow(() -> new NoSuchElementException("User not found"));

        Video video = videoRepository.findByVideoId(videoId).orElseThrow(() -> new NoSuchElementException("Video not found"));

        Comment comment = Comment.builder()
                .commentId(commentId)
                .video(video)
                .user(user)
                .content(content)
                .createdAt(Timestamp.valueOf(LocalDateTime.now()))
                .build();

        try {
            commentRepository.save(comment);
        } catch (Exception e) {
            log.error("Failed to save comment: {}", e.getMessage());
            throw new RuntimeException("Failed to save comment");
        }

        CommentDto.CommentCreateResponseDto commentCreateResponseDto = CommentDto.CommentCreateResponseDto.builder()
                .commentId(comment.getCommentId())
                .createdAt(String.valueOf(comment.getCreatedAt()))
                .build();

        return commentCreateResponseDto;
    }

    @Override
    @Transactional
    public CommentDto.CommentUpdateResponseDto updateComment(String commentId, String userId, String content) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is empty");
        }

        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Content is empty");
        }

        if (content.length() > 140) {
            throw new IllegalArgumentException("Content should not exceed 140 characters");
        }

        Comment comment = commentRepository.findCommentByCommentIdAndUserId(commentId, userId).orElseThrow(() -> new NoSuchElementException("Comment not found"));

        try {
            comment.updateContent(content);
        } catch (Exception e) {
            log.error("Failed to update comment: {}", e.getMessage());
            throw new RuntimeException("Failed to update comment");
        }

        CommentDto.CommentUpdateResponseDto commentUpdateResponseDto = CommentDto.CommentUpdateResponseDto.builder()
                .commentId(comment.getCommentId())
                .updatedAt(String.valueOf(comment.getUpdatedAt()))
                .build();

        return commentUpdateResponseDto;
    }

    @Override
    public void userDeleteComment(String commentId,  String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID is empty");
        }

        Comment comment = commentRepository.findCommentByCommentIdAndUserId(commentId, userId).orElseThrow(() -> new NoSuchElementException("Comment not found"));

        try {
            commentRepository.delete(comment);
        } catch (Exception e) {
            log.error("Failed to delete comment: {}", e.getMessage());
            throw new RuntimeException("Failed to delete comment");
        }
    }

    @Override
    public void sellerDeleteComment(String commentId, String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) {
            throw new IllegalArgumentException("Seller ID is empty");
        }

        Comment comment = commentRepository.findCommentByCommentId(commentId).orElseThrow(() -> new NoSuchElementException("Comment not found"));

        if (!comment.getVideo().getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Seller ID does not match");
        }

        try {
            commentRepository.delete(comment);
        } catch (Exception e) {
            log.error("Failed to delete comment: {}", e.getMessage());
            throw new RuntimeException("Failed to delete comment");
        }
    }

    @Override
    @Transactional
    public Page<CommentDto.CommentResponseDto> sellerVideoGetComments(String videoId, String sellerId, int page, int size) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sortBy);

        Page<Comment> commentPage = commentRepository.findCommentsByVideoIdAndSellerId(videoId, sellerId, pageable);

        Page<CommentDto.CommentResponseDto> commentResponseDtoPage = commentPage.map(
                comment -> CommentDto.CommentResponseDto.builder()
                        .commentId(comment.getCommentId())
                        .userId(comment.getUser().getUserId())
                        .nickname(comment.getUser().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .build()
        );

        return commentResponseDtoPage;
    }

    @Override
    @Transactional
    public Page<CommentDto.CommentResponseDto> userVideoGetComments(String videoId, int page, int size) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sortBy);

        Page<Comment> commentPage = commentRepository.findCommentsByVideoId(videoId, pageable);

        Page<CommentDto.CommentResponseDto> commentResponseDtoPage = commentPage.map(
                comment -> CommentDto.CommentResponseDto.builder()
                        .commentId(comment.getCommentId())
                        .userId(comment.getUser().getUserId())
                        .nickname(comment.getUser().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .build()
        );

        return commentResponseDtoPage;
    }

    @Override
    @Transactional
    public Page<CommentDto.CommentResponseDto> userGetComments(String userId, int page, int size) {
        Sort sortBy = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sortBy);

        Page<Comment> commentPage = commentRepository.findCommentsByUserId(userId, pageable);

        Page<CommentDto.CommentResponseDto> commentResponseDtoPage = commentPage.map(
                comment -> CommentDto.CommentResponseDto.builder()
                        .commentId(comment.getCommentId())
                        .videoId(comment.getVideo().getVideoId())
                        .userId(comment.getUser().getUserId())
                        .nickname(comment.getUser().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .updatedAt(comment.getUpdatedAt())
                        .build()
        );

        return commentResponseDtoPage;
    }
}
