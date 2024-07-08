package cocodas.prier.user;

import cocodas.prier.board.comment.PostComment;
import cocodas.prier.board.post.like.Likes;
import cocodas.prier.board.post.post.Post;
import cocodas.prier.orders.orders.Orders;
import cocodas.prier.point.pointTransaction.PointTransaction;
import cocodas.prier.point.kakao.KakaoPay;
import cocodas.prier.project.feedback.response.Response;
import cocodas.prier.project.project.Project;
import cocodas.prier.project.comment.ProjectComment;
import cocodas.prier.quest.Quest;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Users {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String email;
    private String nickname;
    private String intro;
    private String belonging;
    @Enumerated(EnumType.STRING)
    private Rank tier;
    private String blogUrl;
    private String githubUrl;
    private String figmaUrl;
    private String notionUrl;
    private String metadata = "userProfile.svg";
    private String s3Key = "0af06fa9-a078-4fe3-9df8-0c1e85368fba";
    private LocalDateTime lastLogoutAt;
    private Integer balance;

    // 카카오 로그인을 사용할 때 필요한 Builder
    @Builder
    public Users(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
        this.tier = Rank.ROOKIE;
        this.balance = 0;
    }

    public void updateNickName(String nickname) {
        this.nickname = nickname;
    }

    public void updateIntro(String intro) {
        this.intro = intro;
    }

    public void updateBelonging(String belonging) {
        this.belonging = belonging;
    }

    public void updateBlog(String blogUrl) {
        this.blogUrl = blogUrl;
    }

    public void updateGithub(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public void updateFigma(String figmaUrl) {
        this.figmaUrl = figmaUrl;
    }

    public void updateNotion(String notionUrl) {
        this.notionUrl = notionUrl;
    }

    public void updateMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void updateS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public void updateLastLogoutAt(LocalDateTime lastLogoutAt) {
        this.lastLogoutAt = lastLogoutAt;
    }

    public void updateBalance(int amount) {
        if (this.balance == null) {
            this.balance = 0;
        }
        this.balance += amount;
    }

    // 테이블 연관관계 설정
    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostComment> postComments = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Likes> likes = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectComment> projectComments = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Response> responses = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PointTransaction> pointTransaction = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Orders> orders = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quest> quests = new ArrayList<>();

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KakaoPay> kakaoPayList = new ArrayList<>();
}