package com.spectory.Post.service;

import com.spectory.Config.JsonWebTokenProvider;
import com.spectory.Message;
import com.spectory.Post.domain.Post;
import com.spectory.Post.domain.PostRepository;
import com.spectory.Post.dto.PostDeleteRequestDto;
import com.spectory.Post.dto.PostModifyRequestDto;
import com.spectory.User.domain.User;
import com.spectory.User.domain.UserRepository;
import com.spectory.Post.dto.PostListResponseDto;
import com.spectory.Post.dto.PostSaveRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class PostService {
    private final JsonWebTokenProvider jsonWebTokenProvider;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageUploadService imageUploadService;
    @Transactional
    public Long save(PostSaveRequestDto requestDto, MultipartFile image) throws Exception {
        try {
            Optional<User> writer = userRepository.findById(requestDto.getUserIdx());
            if (requestDto.isEmpty() == true) {
                throw new Exception(Message.MISSING_ARGUMENT);
            }
            if (image != null) {
                try {
                    String fileURL = imageUploadService.imageUpload(image);
                    requestDto.setPicture(fileURL);
                } catch (Exception e) {
                    throw new Exception(Message.IMAGE_UPLOAD_FAIL);
                }
            }
            return postRepository.save(requestDto.toEntity(writer.get())).getPostId();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Transactional
    public Long modify(Long postId, PostModifyRequestDto postModifyRequestDto) {
        Optional<Post> post = postRepository.findById(postId);
        post.get().modify(postModifyRequestDto.getTitle(), postModifyRequestDto.getStartDate(), postModifyRequestDto.getEndDate(),
                postModifyRequestDto.getSituation(), postModifyRequestDto.getAction(), postModifyRequestDto.getLearned(),
                postModifyRequestDto.getPicture(),postModifyRequestDto.getRates(),postModifyRequestDto.getTags());
        return post.get().getPostId();
    }

    @Transactional
    public List<PostListResponseDto> findAll(Long userIdx) {
        Optional<User> writer = userRepository.findById(userIdx);
        List<Post> allPost = postRepository.findByWriter(writer.get());

        List<PostListResponseDto> rtnList = new ArrayList<PostListResponseDto>();
        for (Post p : allPost){
            rtnList.add(new PostListResponseDto(p.getPostId(),p.getType(),p.getTitle(),p.getStartDate(),p.getEndDate()
            ,p.getPicture(),p.getRates(),p.getTags()));
        }

        return rtnList;
    }

    public Optional<Post> getDetail(Long postIdx) throws Exception {
        Optional<Post> post = postRepository.findById(postIdx);
        if (post.isEmpty()) {
            throw new Exception(Message.INVALID_POST);
        } else {
            return post;
        }
    }

    @Transactional
    public boolean deletePost(PostDeleteRequestDto postDeleteRequestDto, Long postIdx) throws Exception {
        if (postDeleteRequestDto.getToken().isEmpty()) {
            throw new Exception(Message.MISSING_ARGUMENT); // 토큰 누락 시 바로 에러 처리.
        }
        if (jsonWebTokenProvider.validateToken(postDeleteRequestDto.getToken()) == false) {
            throw new Exception(Message.TOKEN_AUTHENTICATION_FAIL); // 토큰 인증 실패 시
        } else {
            Optional<Post> post = postRepository.findById(postIdx);
            if (post.isEmpty()) {
                throw new Exception(Message.INVALID_POST);
            } else {
                try {
                    postRepository.deleteById(postIdx);
                    return true;
                } catch (Exception e) {
                    throw new Exception(Message.DELETE_USER_FAIL);
                }
            }
        }
    }

    @Transactional
    public boolean deleteAllPost(PostDeleteRequestDto postDeleteRequestDto, Long userIdx) throws Exception {
        if (postDeleteRequestDto.getToken().isEmpty()) {
            throw new Exception(Message.MISSING_ARGUMENT);
        }
        if (jsonWebTokenProvider.validateToken(postDeleteRequestDto.getToken()) == false) {
            throw new Exception(Message.TOKEN_AUTHENTICATION_FAIL);
        }
        try {
            Optional<User> user = userRepository.findById(userIdx);
            List<Post> postList = postRepository.findByWriter(user.get());
            postRepository.deleteAll(postList);
            return true;
        } catch (Exception e) {
            throw new Exception(Message.DELETE_POST_FAIL);
        }
    }
}
