package cocodas.prier.project.project;

import cocodas.prier.point.pointTransaction.PointTransactionService;
import cocodas.prier.point.pointTransaction.TransactionType;
import cocodas.prier.project.feedback.question.Question;
import cocodas.prier.project.feedback.question.QuestionService;
import cocodas.prier.project.media.ProjectMediaService;
import cocodas.prier.project.project.dto.*;
import cocodas.prier.project.tag.projecttag.ProjectTagService;
import cocodas.prier.user.UserProfileService;
import cocodas.prier.user.UserRepository;
import cocodas.prier.user.Users;
import cocodas.prier.user.kakao.jwt.JwtTokenProvider;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    private final JwtTokenProvider jwtTokenProvider;
    private final ProjectTagService projectTagService;
    private final QuestionService questionService;
    private final ProjectMediaService projectMediaService;
    private final PointTransactionService pointTransactionService;
    private final UserProfileService userProfileService;

    @Transactional
    public Long createProject(ProjectForm form,
                                MultipartFile mainImage,
                                MultipartFile[] contentImages,
                                String token) {

        //유저 찾기
        Users user = getUsersByToken(token);

        Project project = buildProject(form, user);
        project.setStatus(ProjectStatus.values()[form.getStatus()]);
        Project savedProject = projectRepository.save(project);

        handleProjectTags(form, project);
        handleProjectQuestions(form, project);
        handleProjectMedia(mainImage, contentImages, project);

        user.getProjects().add(project);
        return savedProject.getProjectId();
    }

    private Users getUsersByToken(String token) {
        Long userId = jwtTokenProvider.getUserIdFromJwt(token);
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저"));
        log.info("유저 정보: " + user.getNickname());
        return user;
    }

    @Transactional
    public Long handleCreateProject(ProjectForm form, MultipartFile mainImage, MultipartFile[] contentImages, String token) {
        try {
            validateProjectForm(form);
            return createProject(form, mainImage, contentImages, token);
        } catch (IllegalArgumentException e) {
            log.error("Project creation failed: {}", e.getMessage());
            return -1L;
        }
    }

    private void validateProjectForm(ProjectForm form) {
        if (form.getTitle() == null || form.getIntroduce() == null || form.getGoal() == null ||
                form.getStartDate() == null || form.getEndDate() == null || form.getStatus() < 0 || form.getStatus() > 2 ||
                form.getTeamName() == null || form.getTeamDescription() == null || form.getTeamMate() == null ||
                form.getLink() == null || form.getQuestion() == null || form.getType() == null) {
            log.error("입력 값 중 null이 포함되어 있습니다.");
            throw new IllegalArgumentException("입력 필드 중 필수 값이 누락되었습니다.");
        }
    }

    private Project buildProject(ProjectForm form, Users user) {
        Project project = Project.builder()
                .title(form.getTitle())
                .introduce(form.getIntroduce())
                .goal(form.getGoal())
                .devStartDate(form.getStartDate())
                .devEndDate(form.getEndDate())
                .link(form.getLink())
                .teamName(form.getTeamName())
                .teamDescription(form.getTeamDescription())
                .teamMate(form.getTeamMate())
                .users(user)
                .build();
        return project;
    }

    private void handleProjectTags(ProjectForm form, Project project) {
        if (form.getTags() != null) {
            projectTagService.linkTagsToProject(project, form.getTags());
        }
    }

    private void handleProjectQuestions(ProjectForm form, Project project) {
        if (form.getQuestion() != null && form.getType() != null) {
            questionService.createQuestion(project, form.getQuestion(), form.getType());
        }
    }

    private void handleProjectMedia(MultipartFile mainImage, MultipartFile[] contentImages, Project project) {
        try {
            projectMediaService.createMainImage(project, mainImage);
        } catch (IOException e) {
            log.error("메인 이미지 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("메인 이미지 업로드 실패", e);
        }
        if (contentImages != null) {
            try {
                projectMediaService.createContentImage(project, contentImages);
            } catch (IOException e) {
                log.error("내용 이미지 업로드 실패: {}", e.getMessage());
                throw new RuntimeException("내용 이미지 업로드 실패", e);
            }
        }
    }

    @Transactional
    public String deleteProject(String token, Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 프로젝트"));

        Users user = getUsersByToken(token);
        if (!user.equals(project.getUsers())) {
            return "잘못된 사용자, 삭제 실패";
        }

        projectMediaService.deleteImage(project);
        projectRepository.deleteById(projectId);
        return "프로젝트 삭제 완료";
    }

    @Transactional
    public String updateProject(Long projectId,
                                ProjectForm projectForm,
                                MultipartFile mainImage,
                                MultipartFile[] contentImages,
                                String token) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 프로젝트"));

        Users user = getUsersByToken(token);
        if (!user.equals(project.getUsers())) {
            return "잘못된 사용자, 수정 실패";
        }


        project.setTitle(projectForm.getTitle());
        project.setIntroduce(projectForm.getIntroduce());
        project.setGoal(projectForm.getGoal());
        project.setDevStartDate(projectForm.getStartDate());
        project.setDevEndDate(projectForm.getEndDate());
        project.setStatus(ProjectStatus.values()[projectForm.getStatus()]);
        project.setTeamName(projectForm.getTeamName());
        project.setTeamDescription(projectForm.getTeamDescription());
        project.setTeamMate(projectForm.getTeamMate());
        project.setLink(projectForm.getLink());

        // 태그 업데이트
        if (projectForm.getTags() != null) {
            log.info(projectForm.getTags()[0]);
            projectTagService.updateProjectTags(project, projectForm.getTags());
        }

        // 질문 업데이트
        if (projectForm.getQuestion() != null && projectForm.getType() != null) {
            questionService.updateQuestions(project, projectForm.getQuestion(), projectForm.getQuestionId(), projectForm.getType());
        }

        // 미디어 파일 업데이트

        try {
            projectMediaService.updateMainImage(project, projectForm.getDeleteMainImage(), mainImage);
        } catch (IOException e) {
            log.error("메인 이미지 업데이트 실패: {}", e.getMessage());
            throw new RuntimeException("메인 이미지 업데이트 실패", e);
        }




        try {
            projectMediaService.updateContentImages(project, projectForm.getDeleteImages(), contentImages);
        } catch (IOException e) {
            log.error("내용 이미지 업데이트 실패: {}", e.getMessage());
            throw new RuntimeException("내용 이미지 업데이트 실패", e);
        }


        project.setUpdatedAt(LocalDateTime.now());
        return "프로젝트 업데이트 완료";
    }

    public ProjectDetailDto getProjectDetail(Long projectId, String token) {

        Users user = getUsersByToken(token);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 프로젝트"));

        boolean isMine = true;

        if (!project.getUsers().equals(user)) {
            isMine = false;
        }

        return new ProjectDetailDto(
                project.getProjectId(),
                project.getTitle(),
                project.getIntroduce(),
                project.getGoal(),
                project.getDevStartDate(),
                project.getDevEndDate(),
                project.getStatus(),
                project.getTeamName(),
                project.getTeamDescription(),
                project.getTeamMate(),
                project.getLink(),
                isMine,
                project.getFeedbackEndAt(),
                questionService.getProjectQuestions(project),
                projectMediaService.getProjectDetailMedia(project),
                projectTagService.getProjectTags(project),
                calculateScore(project),
                getFeedbackAmount(project),
                userProfileService.getProfile(project.getUsers().getUserId()).getS3Key()
        );
    }

    // $$$ 피드백 상세보기에서 필요한 프로젝트 정보들 (마지막에 feedbackAmount는 프로젝트 댓글 수, 프로젝트 피드백 수, 이 둘의 합 이렇게 3개 반환)
    public ProjectDetailForFeedbackPageDto getFeedbackPageDetail(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 프로젝트"));

        return new ProjectDetailForFeedbackPageDto(
                projectId,
                project.getTitle(),
                project.getTeamName(),
                project.getIntroduce(),
                projectMediaService.getProjectDetailMedia(project).get(0),
                project.getGoal(),
                project.getLink(),
                getFeedbackAmount(project));
    }


    public Page<ProjectDto> getAllProjects(Integer filter, Pageable pageable) {
        Sort sort = Sort.by("createdAt").descending();  // 기본 정렬: 최신순

        if (filter != null) {
            if (filter == 1) { // 인기순
                sort = Sort.by("calculatedScore").descending();
            } else if (filter == 2) { // 등록순
                sort = Sort.by("createdAt").ascending();
            }
        }

        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return projectRepository.findAll(pageable).map(project -> {
            Float score = project.getCalculatedScore();
            return new ProjectDto(
                    project.getProjectId(),
                    project.getUsers().getUserId(),
                    project.getTitle(),
                    project.getTeamName(),
                    projectMediaService.getMainImageUrl(project),
                    project.getDevStartDate(),
                    project.getStatus(),
                    project.getLink(),
                    projectTagService.getProjectTags(project),
                    score,
                    userProfileService.getProfile(project.getUsers().getUserId()).getS3Key(),
                    false
            );
        });
    }

    // 나의 프로젝트 조회
    public Page<ProjectDto> getMyProjects(String token, int filter, int page) {
        Users user = getUsersByToken(token);

        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("users"), user));  // 사용자 기반 필터

            // 진행 중인 프로젝트 필터링
            if (filter == 2) {
                predicates.add(cb.equal(root.get("status"), ProjectStatus.DEVELOPING));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 페이지 및 정렬 조건 설정
        Pageable pageable;
        if (filter == 1) {  // 등록순 (오래된 순)
            pageable = PageRequest.of(page, 5, Sort.by("createdAt").ascending());
        } else {  // 기본 필터 및 진행 중인 프로젝트 필터: 최신순
            pageable = PageRequest.of(page, 5, Sort.by("createdAt").descending());
        }

        Page<Project> projects = projectRepository.findAll(spec, pageable);

        return projects.map(project -> new ProjectDto(
                project.getProjectId(),
                project.getUsers().getUserId(),
                project.getTitle(),
                project.getTeamName(),
                projectMediaService.getMainImageUrl(project),
                project.getDevStartDate(),
                project.getStatus(),
                project.getLink(),
                projectTagService.getProjectTags(project),
                calculateScore(project),
                userProfileService.getProfile(project.getUsers().getUserId()).getS3Key(),
                true
        ));
    }

    // 유저 프로젝트 조회
    public Page<ProjectDto> getUserProjects(String token, Long userId, int filter, Pageable pageable) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저"));

        Users loginUser = getUsersByToken(token);

        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("users"), user));  // 사용자 기반 필터

            // 진행 중인 프로젝트 필터링
            if (filter == 2) {
                predicates.add(cb.equal(root.get("status"), ProjectStatus.DEVELOPING));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort;
        if (filter == 1) {  // 등록순 (오래된 순)
            sort = Sort.by("createdAt").ascending();
        } else {  // 기본 필터 및 진행 중인 프로젝트 필터: 최신순
            sort = Sort.by("createdAt").descending();
        }

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Page<Project> projects = projectRepository.findAll(spec, sortedPageable);

        return projects.map(project -> {
            // 로그에 프로젝트 제목과 상태를 기록
            log.info("Project 제목: {}, Project 상태: {}", project.getTitle(), project.getStatus());

            return new ProjectDto(
                    project.getProjectId(),
                    project.getUsers().getUserId(),
                    project.getTitle(),
                    project.getTeamName(),
                    projectMediaService.getMainImageUrl(project),
                    project.getDevStartDate(),
                    project.getStatus(),
                    project.getLink(),
                    projectTagService.getProjectTags(project),
                    calculateScore(project),
                    userProfileService.getProfile(project.getUsers().getUserId()).getS3Key(),
                    project.getUsers().equals(loginUser)
            );
        });
    }

    // %%% 마이페이지 최근 프로젝트 & 피드백 개수 조회
    public MyPageProjectDto getRecentProject(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 유저"));

        Project project = projectRepository.findMyRecentProject(user)
                .orElse(null);

        if (project == null) {
            return null;
        }

        return MyPageProjectDto.builder()
                .projectId(project.getProjectId())
                .title(project.getTitle())
                .teamName(project.getTeamName())
                .score(calculateScore(project))
                .feedbackAmount(getFeedbackAmount(project)[2])
                .build();
    }


    public Page<ProjectDto> getSearchedProjects(String keyword, Pageable pageable) {
        return projectRepository.findByKeyword(keyword, pageable)
                .map(project -> new ProjectDto(
                        project.getProjectId(),
                        project.getUsers().getUserId(),
                        project.getTitle(),
                        project.getTeamName(),
                        projectMediaService.getMainImageUrl(project),
                        project.getDevStartDate(),
                        project.getStatus(),
                        project.getLink(),
                        projectTagService.getProjectTags(project),
                        calculateScore(project),
                        userProfileService.getProfile(project.getUsers().getUserId()).getS3Key(),
                        false
                ));
    }

    @Transactional
    public String extendFeedback(Long projectId, Integer weeks, String token) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 프로젝트"));

        Users user = getUsersByToken(token);
        if (!user.equals(project.getUsers())) {
            return "잘못된 사용자, 요청 실패";
        }

        pointTransactionService.decreasePoints(user, weeks * 250, TransactionType.FEEDBACK_EXTENSION);
        project.addFeedbackEndAt(weeks);

        log.info(user.getNickname() + " " + weeks * 250 + "포인트 차감 완료 " + weeks + "주 연장 완료");
        return user.getNickname() + " " + weeks * 250 + "포인트 차감 완료 " + weeks + "주 연장 완료";
    }

    @Transactional
    public Float calculateScore(Project project) {
        int commentsAmount = project.getProjectComments().size();
        float averageScore = project.getScore() / commentsAmount;

        averageScore = Math.min(5, Math.round(averageScore * 2) / 2.0f);

        project.setCalculatedScore(averageScore);
        projectRepository.save(project);

        return averageScore;
    }

    //프로젝트 댓글 수, 프로젝트 피드백 수, 이 둘의 합 이렇게 3개 반환
    public int[] getFeedbackAmount(Project project) {
        List<Question> feedbackQuestions = project.getFeedbackQuestions();
        AtomicInteger maxValue = new AtomicInteger(Integer.MIN_VALUE);
        feedbackQuestions.forEach(q -> {
            if (maxValue.get() < q.getResponses().size()) {
                maxValue.set(q.getResponses().size());
            }
        });
        return new int[]{project.getProjectComments().size(), maxValue.get(), project.getProjectComments().size() + maxValue.get()};
    }
}