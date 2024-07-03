package cocodas.prier.project.project;

import cocodas.prier.project.project.dto.MyPageProjectDto;
import cocodas.prier.project.project.dto.ProjectDetailDto;
import cocodas.prier.project.project.dto.ProjectDto;
import cocodas.prier.project.project.dto.ProjectForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    private static String getToken(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new RuntimeException("JWT Token is missing");
        }

        return auth.substring(7);
    }

    // 프로젝트 생성
    @PostMapping
    public ResponseEntity<Long> createProject(@RequestPart("form") ProjectForm form,
                                                @RequestParam(name = "mainImage", required = false) MultipartFile mainImage,
                                                @RequestParam(name = "contentImages", required = false) MultipartFile[] contentImages,
                                                @RequestHeader("Authorization") String auth) {

        String token = getToken(auth);
        Long result = projectService.handleCreateProject(form, mainImage, contentImages, token);
        return ResponseEntity.ok(result);
    }

    // 프로젝트 삭제
    @DeleteMapping("/{projectId}")
    public ResponseEntity<String> deleteProject(@PathVariable Long projectId,
                                                @RequestHeader("Authorization") String auth) {

        String token = getToken(auth);
        String result = projectService.deleteProject(token, projectId);

        return ResponseEntity.ok(result);
    }

    // 프로젝트 수정
    // 질문은 수정 못하게 프론트에서 막아줘야함.
    @PutMapping("/{projectId}")
    public ResponseEntity<String> updateProject(@PathVariable Long projectId,
                                                @RequestPart("form") ProjectForm form,
                                                @RequestParam(name = "mainImage", required = false) MultipartFile mainImage,
                                                @RequestParam(value = "contentImages", required = false) MultipartFile[] contentImages,
                                                @RequestHeader("Authorization") String auth) {
        String token = getToken(auth);
        String result = projectService.updateProject(projectId, form, mainImage, contentImages, token);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailDto> getProjectDetail(@PathVariable Long projectId,
                                                             @RequestHeader("Authorization") String auth) {

        try {
            String token = getToken(auth);
            ProjectDetailDto result = projectService.getProjectDetail(projectId, token);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }



    }

    // 프로젝트 목록 조회
    @GetMapping
    public ResponseEntity<Page<ProjectDto>> getSearchedProjects(
            @RequestParam(value = "search", required = false) String keyword,
            @RequestParam(name = "filter", required = false) Integer filter,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        Pageable pageable = PageRequest.of(page, 8);  // 페이지 크기와 페이지 번호 설정
        Page<ProjectDto> projectPage;


        if (keyword != null && !keyword.isEmpty()) {
            projectPage = projectService.getSearchedProjects(keyword, pageable);
        } else {
            projectPage = projectService.getAllProjects(filter, pageable);
        }

        return ResponseEntity.ok(projectPage);
    }

    // 나의 프로젝트 조회
    @GetMapping("/my-projects")
    public ResponseEntity<Page<ProjectDto>> getMyProjects(
            @RequestHeader("Authorization") String auth,
            @RequestParam("filter") Integer filter,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        String token = getToken(auth);
        return ResponseEntity.ok(projectService.getMyProjects(token, filter, page));
    }

    // 유저 프로젝트 조회
    @GetMapping("/user-projects")
    public ResponseEntity<Page<ProjectDto>> getUserProjects(
            @RequestHeader("Authorization") String auth,
            @RequestParam("userId") Long userId,
            @RequestParam("filter") Integer filter,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        String token = getToken(auth);


        Pageable pageable = PageRequest.of(page, 5);  // 페이지 크기와 페이지 번호 설정
        return ResponseEntity.ok(projectService.getUserProjects(token, userId, filter, pageable));
    }

    // 나의 최근 프로젝트, 피드백 개수
    @GetMapping("/recent-project")
    public ResponseEntity<MyPageProjectDto> getMyRecentProject(@RequestHeader("Authorization") String auth,
                                                               @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(projectService.getRecentProject(userId));
    }


    // 프로젝트 피드백 기간 연장
    @PostMapping("/{projectId}/extend")
    public ResponseEntity<String> extendFeedback(@PathVariable Long projectId,
                                                 @RequestParam Integer weeks,
                                                 @RequestHeader("Authorization") String auth) {
        String token = getToken(auth);
        String result = projectService.extendFeedback(projectId, weeks, token);

        return ResponseEntity.ok(result);
    }

}
