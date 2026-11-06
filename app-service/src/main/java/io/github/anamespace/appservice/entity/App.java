package io.github.anamespace.appservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "apps")
public class App {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String slug;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 5000)
    private String description;

    @Column(name = "developer_id", nullable = false)
    private Long developerId;

    @Column(name = "logo_file_key")
    private String logoFileKey;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppStatus status = AppStatus.DRAFT;

    @Column(name = "first_published_at")
    private Instant firstPublishedAt;

    @Column(name = "cached_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal cachedRating = BigDecimal.ZERO;

    @Column(name = "cached_rating_count", nullable = false)
    private int cachedRatingCount = 0;

    @Column(name = "download_count", nullable = false)
    private long downloadCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "app_categories",
            joinColumns = @JoinColumn(name = "app_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    public App() {
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        if (cachedRating == null) {
            cachedRating = BigDecimal.ZERO;
        }
        if (status == null) {
            status = AppStatus.DRAFT;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDeveloperId() {
        return developerId;
    }

    public void setDeveloperId(Long developerId) {
        this.developerId = developerId;
    }

    public String getLogoFileKey() {
        return logoFileKey;
    }

    public void setLogoFileKey(String logoFileKey) {
        this.logoFileKey = logoFileKey;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public AppStatus getStatus() {
        return status;
    }

    public void setStatus(AppStatus status) {
        this.status = status;
    }

    public Instant getFirstPublishedAt() {
        return firstPublishedAt;
    }

    public void setFirstPublishedAt(Instant firstPublishedAt) {
        this.firstPublishedAt = firstPublishedAt;
    }

    public BigDecimal getCachedRating() {
        return cachedRating;
    }

    public void setCachedRating(BigDecimal cachedRating) {
        this.cachedRating = cachedRating;
    }

    public int getCachedRatingCount() {
        return cachedRatingCount;
    }

    public void setCachedRatingCount(int cachedRatingCount) {
        this.cachedRatingCount = cachedRatingCount;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }
}
