package cocodas.prier.board.post;

import cocodas.prier.board.post.post.request.PostRequestDto;
import cocodas.prier.board.post.post.PostService;
import cocodas.prier.board.post.post.response.PostDetailResponseDto;
import cocodas.prier.board.post.post.response.PostListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {

    private final PostService postService;

    // 모든 게시글 조회하기 & 검색 키워드 게시글 조회하기
    @GetMapping("/posts")
    @ResponseStatus(HttpStatus.OK)
    public PostListResponseDto allOrSearchPosts(@RequestHeader("Authorization") String authorizationHeader,
                                                      @RequestParam(name = "search", required = false) String keyword) {
        String token = authorizationHeader.replace("Bearer ", "");
        if (keyword == null || keyword.isEmpty()) {
            return postService.allPostList(token);
        } else {
            return postService.searchPostsByKeyword(token, keyword);
        }
    }

    // 특정 게시글 조회하기
    @GetMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public PostDetailResponseDto getPostByPostId(@RequestHeader("Authorization") String authorizationHeader,
                                                 @PathVariable(name = "postId") Long postId) {
        String token = authorizationHeader.replace("Bearer ", "");
        return postService.findByPostId(token, postId);
    }

    // 내가 작성한 게시글 모두 조회하기
    @GetMapping("/posts/my")
    @ResponseStatus(HttpStatus.OK)
    public PostListResponseDto myPostList(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        return postService.myPostList(token);
    }

    // 내가 좋아요 누른 게시글 모두 조회하기
    @GetMapping("/posts/like/my")
    @ResponseStatus(HttpStatus.OK)
    public PostListResponseDto pushLikePost(@RequestHeader("Authorization") String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        return postService.pushLikePost(token);
    }

    // 게시글 작성하기
    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public void addPost(@RequestHeader("Authorization") String authorizationHeader,
                        @RequestPart("dto") PostRequestDto postRequestDto,
                        @RequestParam(name = "media", required = false) MultipartFile[] media) {
        String token = authorizationHeader.replace("Bearer ", "");
        postService.addPost(token, postRequestDto, media);
    }

    // 게시글 수정하기
    @PutMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public void updatePost(@RequestHeader("Authorization") String authorizationHeader,
                           @RequestPart("dto") PostRequestDto postRequestDto,
                           @RequestParam(name = "media", required = false) MultipartFile[] media,
                           @PathVariable(name = "postId") Long postId) {
        String token = authorizationHeader.replace("Bearer ", "");
        postService.updatePost(token, postRequestDto, postId, media);
    }

    // 게시글 삭제하기
    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public void deletePost(@RequestHeader("Authorization") String authorizationHeader,
                           @PathVariable(name = "postId") Long postId) {
        String token = authorizationHeader.replace("Bearer ", "");
        postService.deletePost(token, postId);
    }
}
