package cocodas.prier.user;

import cocodas.prier.aws.AwsS3Service;
import cocodas.prier.project.comment.ProjectCommentService;
import cocodas.prier.project.comment.dto.MyPageCommentDto;
import cocodas.prier.project.feedback.response.ResponseService;
import cocodas.prier.project.project.ProjectService;
import cocodas.prier.project.project.dto.MyPageProjectDto;
import cocodas.prier.user.dto.NotificationDto;
import cocodas.prier.quest.Quest;
import cocodas.prier.statics.keywordAi.KeywordsService;
import cocodas.prier.statics.keywordAi.dto.response.KeyWordResponseDto;
import cocodas.prier.statics.objective.ObjectiveResponseService;
import cocodas.prier.user.kakao.jwt.JwtTokenProvider;
import cocodas.prier.user.response.MyPageResponseDto;
import cocodas.prier.user.response.OhterMyPageResponseDto;
import cocodas.prier.user.response.ProfileImgDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final ProjectService projectService;

    private final KeywordsService keywordsService;

    private final ObjectiveResponseService objectiveResponseService;

    private final ProjectCommentService projectCommentService;

    private final JwtTokenProvider jwtTokenProvider;

    private final AwsS3Service awsS3Service;

    private final UserProfileService userProfileService;

    @Value("${profile.default.s3key}")
    private String defaultProfileS3Key;

    private Long findUserIdByJwt(String token) {
        return jwtTokenProvider.getUserIdFromJwt(token);
    }

    private Users findUserExist(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found!"));
    }

    // 나의 마이페이지 보기
    public MyPageResponseDto viewMyPage(String token) {

        Long userId = findUserIdByJwt(token);

        Users users = findUserExist(userId);

        // 퀘스트
        Quest quests = users.getQuests()
                .stream()
                .filter(quest -> quest.getCreatedAt().equals(LocalDate.now()))
                .findFirst()
                .orElse(null);

        // 프로필
        ProfileImgDto profile = userProfileService.getProfile(userId);

        // 최신 프로젝트 가져오기
        MyPageProjectDto nowProject = projectService.getRecentProject(userId);
        Long projectId = null;
        String teamName = null;
        String projectTitle = null;
        Integer feedbackAmount = null;
        Float score = null;
        String percentageStr = null;
        List<KeyWordResponseDto> keywordByProjectId = null;
        List<MyPageCommentDto> projectComments = projectCommentService.getProjectComments(userId);

        if (nowProject != null) {
            projectId = nowProject.getProjectId();
            teamName = nowProject.getTeamName();
            projectTitle = nowProject.getTitle();
            feedbackAmount = nowProject.getFeedbackAmount();
            score = nowProject.getScore();

            Double percentage = objectiveResponseService.calculateFeedbackPercentage(projectId);
            percentageStr = String.format("%.2f", percentage);

            keywordByProjectId = keywordsService.getKeywordByProjectId(projectId);
        }

        return new MyPageResponseDto(
                users.getNickname(),
                users.getBelonging(),
                users.getTier(),
                users.getGithubUrl(),
                users.getNotionUrl(),
                users.getBlogUrl(),
                users.getFigmaUrl(),
                users.getEmail(),
                users.getIntro(),
                quests.getFirst(),
                quests.getSecond(),
                quests.getThird(),
                projectId,
                teamName,
                projectTitle,
                feedbackAmount,
                score,
                percentageStr,
                keywordByProjectId,
                projectComments,
                profile
        );
    }

    // 다른 사람의 마이페이지 보기
    public OhterMyPageResponseDto viewOtherMyPage(String token, Long otherUserId) {
        Long myUserId = findUserIdByJwt(token);

        Users myUsers = findUserExist(myUserId);

        Users otherUsers = findUserExist(otherUserId);

        // 퀘스트
        Quest quests = otherUsers.getQuests()
                .stream()
                .filter(quest -> quest.getCreatedAt().equals(LocalDate.now()))
                .findFirst()
                .orElse(null);

        // ======================= 여기까지가 나에 대한 정보 =======================

        // 프로필
        ProfileImgDto profile = userProfileService.getProfile(myUserId);

        // 최신 프로젝트 가져오기
        MyPageProjectDto nowProject = projectService.getRecentProject(otherUserId);
        Long projectId = null;
        String teamName = null;
        String projectTitle = null;
        Integer feedbackAmount = null;
        Float score = null;
        String percentageStr = null;
        List<KeyWordResponseDto> keywordByProjectId = null;
        List<MyPageCommentDto> projectComments = null;

        if (nowProject != null) {
            projectId = nowProject.getProjectId();
            teamName = nowProject.getTeamName();
            projectTitle = nowProject.getTitle();
            feedbackAmount = nowProject.getFeedbackAmount();
            score = nowProject.getScore();

            Double percentage = objectiveResponseService.calculateFeedbackPercentage(projectId);
            percentageStr = String.format("%.2f", percentage);

            keywordByProjectId = keywordsService.getKeywordByProjectId(projectId);

            projectComments = projectCommentService.getProjectComments(otherUserId);
        }

        return new OhterMyPageResponseDto(
                otherUsers.getNickname(),
                otherUsers.getBelonging(),
                otherUsers.getTier(),
                otherUsers.getGithubUrl(),
                otherUsers.getNotionUrl(),
                otherUsers.getBlogUrl(),
                otherUsers.getFigmaUrl(),
                otherUsers.getEmail(),
                otherUsers.getIntro(),
                awsS3Service.getPublicUrl(otherUsers.getS3Key()),
                quests != null && quests.getFirst(),
                quests != null && quests.getSecond(),
                quests != null && quests.getThird(),
                projectId,
                teamName,
                projectTitle,
                feedbackAmount,
                score,
                percentageStr,
                keywordByProjectId,
                projectComments,
                profile
        );
    }

    // 닉네임 수정하기
    @Transactional
    public void newNickName(String token, String newNickname) {
        Long userId = findUserIdByJwt(token);
        Users user = findUserExist(userId);

        user.updateNickName(newNickname);
    }

    // 소속 수정하기
    @Transactional
    public void newBelonging(String token, String newBelonging) {
        Long userId = findUserIdByJwt(token);
        Users user = findUserExist(userId);

        user.updateBelonging(newBelonging);
    }

    // 자기소개 수정하기
    @Transactional
    public void newIntro(String token, String newIntro) {
        Long userId = findUserIdByJwt(token);
        Users user = findUserExist(userId);

        user.updateIntro(newIntro);
    }

    // 블로그 링크 수정하기
    @Transactional
    public void newBlogUrl(String token, String newBlogUrl) {
        Long userId = findUserIdByJwt(token);
        Users user = findUserExist(userId);

        user.updateBlog(newBlogUrl);
    }

    // 깃헙 링크 수정하기
    @Transactional
    public void newGithubUrl(String token, String newGithubUrl) {
        Long userId = findUserIdByJwt(token);
        Users user = findUserExist(userId);

        user.updateGithub(newGithubUrl);
    }

    // 피그마 주소 수정하기
    @Transactional
    public void newFigmaUrl(String token, String newFigmaUrl) {
        Long userId = findUserIdByJwt(token);
        Users user = findUserExist(userId);

        user.updateFigma(newFigmaUrl);
    }

    // 노션 주소 수정하기
    @Transactional
    public void newNotionUrl(String token, String newNotionUrl) {
        Long userId = findUserIdByJwt(token);
        Users user = findUserExist(userId);

        user.updateNotion(newNotionUrl);
    }

    // 마이페이지 프로필 수정하기
    @Transactional
    public void newProfileImg(String token, MultipartFile file) throws IOException {
        Long userId = findUserIdByJwt(token);
        if (file != null) {
            saveMedia(userId, file);
        }
    }

    private void saveMedia(Long userId, MultipartFile file) throws IOException {
        String key = awsFileUploadAndGetKey(file);
        Users users = findUserExist(userId);

        users.updateMetadata(file.getOriginalFilename());
        users.updateS3Key(key);
    }

    private String awsFileUploadAndGetKey(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("profile-", file.getOriginalFilename());
        file.transferTo(tempFile);

        String key = awsS3Service.uploadFile(tempFile);
        tempFile.delete();
        return key;
    }

    @Transactional
    public void deleteProfileImg(String token) {
        Long userId = findUserIdByJwt(token);
        Users users = findUserExist(userId);

        users.updateMetadata("userProfile.svg");
        users.updateS3Key(defaultProfileS3Key);
    }

    @Transactional
    public void updateLastLogoutAt(String token, String logoutAt) {
        Long userId = findUserIdByJwt(token);
        Users users = findUserExist(userId);

        LocalDateTime lastLogoutAt = LocalDateTime.parse(logoutAt, DateTimeFormatter.ISO_DATE_TIME);
        users.updateLastLogoutAt(lastLogoutAt);
    }

    // $$ 천승환, 이소은 -> 사용자 프로필 사진 가져가라
    // ㅋㅋ 오키 고맙다.
    public ProfileImgDto getProfile(Long userId) {
        return userProfileService.getProfile(userId);
    }
}